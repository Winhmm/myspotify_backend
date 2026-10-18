package com.winhmm.myspotify.security;

import com.winhmm.myspotify.service.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/*
    =====================================================
    JWTAUTHENTICATIONFILTER - XÁC ĐỊNH NGƯỜI GỬI REQUEST
    =====================================================

    -----------------------------------------------------
    Filter là gì?
    -----------------------------------------------------
    Filter là đoạn code chạy trước khi request tới Controller.
    Nhiều filter nối với nhau thành 1 chuỗi (filter chain), request đi qua lần lượt từng cái:
        Request → Filter 1 → Filter 2 → ... → Controller → Response

    -----------------------------------------------------
    Nhiệm vụ của filter này
    -----------------------------------------------------
    Trả lời đúng 1 câu hỏi: "Request này của ai?"
    - Token hợp lệ → gắn thông tin người dùng (email + role) vào SecurityContext.
      Có thể hiểu là "dán nhãn" lên request: "đây là a@gmail.com, role ADMIN".
    - Không có token, hoặc token không hợp lệ → không dán nhãn,
      request đi tiếp với tư cách "khách" (anonymous).

    -----------------------------------------------------
    Filter này không chặn request
    -----------------------------------------------------
    Dù token sai, filter vẫn cho request đi tiếp. Việc chặn do các bước sau quyết định:
    - AuthorizationFilter (theo luật URL trong SecurityConfig):
      API cần đăng nhập mà request là "khách" → 401.
    - @PreAuthorize trên Controller: đã đăng nhập nhưng sai role → 403.

    Vì sao thiết kế như vậy?
    - Các API công khai như /api/auth/login vẫn chạy bình thường,
      kể cả khi frontend lỡ gửi kèm 1 token cũ đã hết hạn.
    - Mỗi bước làm đúng 1 việc → dễ hiểu, dễ sửa.

    -----------------------------------------------------
    Vì sao kế thừa OncePerRequestFilter?
    -----------------------------------------------------
    Trong 1 request, Spring có thể chuyển tiếp nội bộ (forward), ví dụ khi có lỗi
    sẽ chuyển sang /error. Nếu dùng filter thường, filter có thể chạy 2 lần.
    OncePerRequestFilter đảm bảo filter chỉ chạy đúng 1 lần cho mỗi request.

    -----------------------------------------------------
    Vì sao không gắn @Component?
    -----------------------------------------------------
    - Nếu gắn @Component, Spring Boot tự đăng ký filter này thêm 1 lần nữa
      ở tầng servlet (bên ngoài Spring Security) → mỗi request chạy filter 2 lần.
    - Project tạo filter bằng từ khóa new trong SecurityConfig
      → filter chỉ nằm trong chuỗi filter của Spring Security. Đây là cách làm đúng.
    - Cũng vì không phải @Component nên các dependency phải truyền qua constructor
      (SecurityConfig truyền vào), không tự inject được.
*/
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    public static final String SESSION_ERROR = "sessionError";

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final SessionService sessionService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil,
                                   CustomUserDetailsService userDetailsService,
                                   SessionService sessionService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.sessionService = sessionService;
    }

    /*
        =====================================================
        XỬ LÝ MỖI REQUEST
        =====================================================

        Hàm này được Spring gọi tự động mỗi khi có request.

        Tham số:
        - request: thông tin request gửi lên (header, URL, body...).
        - response: dùng để ghi response trả về (filter này không dùng tới).
        - filterChain: chuỗi filter còn lại. Gọi filterChain.doFilter() để chuyển
          request sang filter tiếp theo.

        Tóm tắt các bước:
        1. Lấy header Authorization.
        2. Cắt lấy token.
        3. Kiểm tra chữ ký và hạn dùng.
        4. Đọc email và mã phiên.
        5. So mã phiên với Redis.
        6. Đọc người dùng từ MySQL và kiểm tra trạng thái.
        7. Dán nhãn vào SecurityContext.
        8. Chuyển request sang filter tiếp theo.
        Hỏng ở bước nào thì bỏ qua các bước còn lại, nhảy thẳng tới bước 8.
    */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        /*
            -----------------------------------------------------
            Bước 1: Lấy header Authorization
            -----------------------------------------------------
        */
        String header = request.getHeader("Authorization");

        /*
            Chỉ xử lý khi header tồn tại và bắt đầu bằng "Bearer ".
        */
        if (header != null && header.startsWith("Bearer ")) {
            /*
                -----------------------------------------------------
                Bước 2: Cắt lấy token
                -----------------------------------------------------
            */
            String token = header.substring(7);

            /*
                -----------------------------------------------------
                Bước 3: Kiểm tra chữ ký và hạn dùng
                -----------------------------------------------------
            */
            if (jwtUtil.validateToken(token)) {
                try {
                    /*
                        -----------------------------------------------------
                        Bước 4: Đọc email và mã phiên trong token
                        -----------------------------------------------------
                        - email: token này của ai (claim "sub").
                        - sessionId: mã phiên được tạo lúc đăng nhập (claim "jti").
                    */
                    String email = jwtUtil.getEmailFromToken(token);
                    String sessionId = jwtUtil.getSessionIdFromToken(token);

                    /*
                        -----------------------------------------------------
                        Bước 5: So mã phiên với Redis
                        -----------------------------------------------------
                        sessionService.isValidSession(email, sessionId) đọc key session:{email}
                        trong Redis và so với sessionId trong token.

                        Hai giá trị không khớp, hoặc key không tồn tại, khi:
                        - Đăng nhập ở thiết bị khác → Redis bị ghi đè bằng UUID mới.
                        - Đăng xuất → key bị xóa.
                        - Đổi mật khẩu hoặc đặt lại mật khẩu → key bị xóa.
                        - Đổi email → key bị xóa.
                        - Admin khóa tài khoản → key bị xóa.

                        Khi đó: dán giấy nhắn SESSION_ERROR vào request và không xác thực.
                        Kết quả: API cần đăng nhập sẽ trả 401 kèm message
                        "Your session has ended. Your account may have been logged in on another device".

                        Đây chính là cơ chế thu hồi token mà JWT thông thường không có.

                        Ví dụ thực tế:
                        - 9h00: đăng nhập trên laptop → Redis: session:a@gmail.com = "uuid-111"
                                token laptop chứa jti = "uuid-111"
                        - 9h30: đăng nhập trên điện thoại → Redis: session:a@gmail.com = "uuid-222"
                                token điện thoại chứa jti = "uuid-222"
                        - 9h31: laptop gọi API → token gửi "uuid-111", Redis đang là "uuid-222"
                                → không khớp → laptop nhận 401.
                    */
                    if (!sessionService.isValidSession(email, sessionId)) {
                        request.setAttribute(SESSION_ERROR,
                                "Your session has ended. Your account may have been logged in on another device");
                    } else {
                        /*
                            -----------------------------------------------------
                            Bước 6: Đọc người dùng mới nhất từ MySQL
                            -----------------------------------------------------
                        */
                        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                         /*
                            -----------------------------------------------------
                            Bước 7: Dán nhãn vào SecurityContext
                            -----------------------------------------------------
                            Chỉ dán nhãn khi tài khoản còn ACTIVE (isEnabled() = true).
                            Tài khoản UNVERIFIED, DISABLED, DELETED → không dán nhãn → là "khách".

                            SecurityContextHolder là gì?
                            - Là "cái hộp" giữ thông tin người đang gửi request hiện tại.
                            - Mỗi request chạy trên 1 thread riêng và có hộp riêng,
                              nên request của người A không lẫn với request của người B.
                            - Hộp được dọn sạch khi request kết thúc (vì cấu hình STATELESS)
                              → request sau phải gửi token lại từ đầu.

                            UsernamePasswordAuthenticationToken là "cái nhãn" bỏ vào hộp, gồm 3 phần:
                            - principal = userDetails: người đang gọi (email, role).
                            - credentials = null: không cần giữ mật khẩu, token đã chứng minh danh tính.
                            - authorities = userDetails.getAuthorities(): danh sách quyền, ví dụ [ROLE_ADMIN].
                              @PreAuthorize("hasRole(...)") đọc danh sách này để kiểm tra role.
                            Dùng constructor 3 tham số thì nhãn tự được đánh dấu "đã xác thực".

                            Sau bước này, Controller lấy được người đang gọi bằng:
                            - authentication.getName() → email
                            - hoặc @AuthenticationPrincipal UserDetails userDetails
                        */
                        if (userDetails.isEnabled()) {
                            SecurityContextHolder.getContext().setAuthentication(
                                    new UsernamePasswordAuthenticationToken(
                                            userDetails, null, userDetails.getAuthorities()));
                        }
                    }
                } catch (Exception e) {}
            }
        }
        filterChain.doFilter(request, response);
    }
}