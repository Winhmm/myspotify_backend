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

    (Token là gì, thứ tự đọc package security và đường đi của 1 request: xem JwtUtil.)

    -----------------------------------------------------
    Filter là gì?
    -----------------------------------------------------
    Filter là đoạn code chạy trước khi request tới Controller.
    Nhiều filter nối với nhau thành 1 chuỗi (filter chain), request đi qua lần lượt từng cái:
        Request → Filter 1 → Filter 2 → ... → Controller → Response
    Mỗi filter tự quyết: cho đi tiếp (gọi filterChain.doFilter) hoặc dừng lại và tự trả response.

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
      API cần đăng nhập mà request là "khách" → 401
      (trả bởi authenticationEntryPoint trong SecurityConfig).
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
    Kế thừa (extends) xong chỉ cần viết lại 1 hàm doFilterInternal, phần còn lại có sẵn.

    -----------------------------------------------------
    Vì sao không gắn @Component?
    -----------------------------------------------------
    - Nếu gắn @Component, Spring Boot tự đăng ký filter này thêm 1 lần nữa
      ở tầng servlet (bên ngoài Spring Security) → mỗi request chạy filter 2 lần.
    - Project tạo filter bằng từ khóa new trong SecurityConfig
      → filter chỉ nằm trong chuỗi filter của Spring Security. Đây là cách làm đúng:
            .addFilterBefore(new JwtAuthenticationFilter(jwtUtil, userDetailsService, sessionService),
                             UsernamePasswordAuthenticationFilter.class)
        (đặt trước filter đăng nhập bằng form có sẵn của Spring, tức là chạy rất sớm trong chuỗi.)
    - Cũng vì không phải @Component nên các dependency phải truyền qua constructor
      (SecurityConfig truyền vào), không tự inject được.

    -----------------------------------------------------
    Mỗi request có token tốn những gì?
    -----------------------------------------------------
    - Đọc token (không cần mạng, chỉ tính toán).
    - 1 lần đọc Redis (so mã phiên).
    - 1 câu SELECT vào MySQL (đọc User mới nhất).
    Đổi lại: khóa tài khoản, đổi role, đăng xuất... đều có hiệu lực ngay ở request tiếp theo.
*/
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /*
        Tên "giấy nhắn" dán vào request khi phiên đăng nhập không còn hợp lệ (xem bước 5).
        SecurityConfig đọc lại đúng tên này để chọn message 401.
        public static final: hằng số dùng chung giữa 2 class
        → không ai phải gõ lại chuỗi "sessionError" bằng tay (gõ sai 1 chữ là không khớp).
    */
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

        throws ServletException, IOException: khai báo theo đúng hàm gốc của OncePerRequestFilter
        (filterChain.doFilter có thể ném 2 loại lỗi này).

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

        Kết quả theo từng trường hợp:
            Không gửi token                    → khách, không có giấy nhắn  → 401 "Unauthorized, please login"
            Token sai chữ ký hoặc hết hạn      → khách, không có giấy nhắn  → 401 "Unauthorized, please login"
            Phiên Redis không khớp             → khách, CÓ giấy nhắn        → 401 "Your session has ended..."
            Tài khoản không ACTIVE hoặc REFUSE → khách, không có giấy nhắn  → 401 "Unauthorized, please login"
            Mọi thứ hợp lệ                     → dán nhãn                    → vào được Controller
        (Mã 401 chỉ xảy ra với API cần đăng nhập. API công khai như /api/auth/... vẫn chạy với tư cách khách.)

        Bị Admin khóa, tự vô hiệu hóa, tự xóa tài khoản: phiên Redis đều bị xóa ngay lúc đó
        → rơi vào dòng "Phiên Redis không khớp" trước. Dòng "không ACTIVE hoặc REFUSE"
        chủ yếu bắt trường hợp chủ dự án đổi access_status sang REFUSE bằng SQL (không xóa phiên).
    */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        /*
            -----------------------------------------------------
            Bước 1: Lấy header Authorization
            -----------------------------------------------------
            Frontend gửi:  Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIi...
            Không gửi header này → getHeader trả null.
        */
        String header = request.getHeader("Authorization");

        /*
            Chỉ xử lý khi header tồn tại và bắt đầu bằng "Bearer ".
            "Bearer" (người cầm giữ): quy ước chuẩn cho token, nghĩa là "ai cầm token này thì là người đó".
            Kiểm tra header != null TRƯỚC: nếu header là null mà gọi header.startsWith(...)
            → NullPointerException. && dừng ngay khi vế trái sai nên an toàn.
        */
        if (header != null && header.startsWith("Bearer ")) {
            /*
                -----------------------------------------------------
                Bước 2: Cắt lấy token
                -----------------------------------------------------
                "Bearer " dài đúng 7 ký tự (6 chữ cái + 1 dấu cách, vị trí 0 tới 6).
                substring(7): lấy từ ký tự thứ 7 tới hết → chỉ còn phần token.
                    "Bearer eyJhbGci..."  →  "eyJhbGci..."
            */
            String token = header.substring(7);

            /*
                -----------------------------------------------------
                Bước 3: Kiểm tra chữ ký và hạn dùng
                -----------------------------------------------------
                Sai chữ ký, hết hạn, sai định dạng → false → bỏ qua, request là "khách"
                (chi tiết từng trường hợp: xem JwtUtil.validateToken).
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
                        - Tự vô hiệu hóa hoặc tự xóa tài khoản → key bị xóa.
                        - Admin khóa tài khoản → key bị xóa.
                        - Hết hạn: key tự biến mất cùng lúc token hết hạn (cùng thời gian sống).

                        Khi đó: dán giấy nhắn SESSION_ERROR vào request và không xác thực.
                        Kết quả: API cần đăng nhập sẽ trả 401 kèm message
                        "Your session has ended. Your account may have been logged in on another device".

                        request.setAttribute(tên, giá trị): gắn 1 giá trị vào request,
                        giống dán 1 tờ giấy nhắn lên hồ sơ. Các bước sau trong cùng request
                        đọc lại được bằng request.getAttribute(tên). Request kết thúc → giấy nhắn mất.

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
                            Lấy role và trạng thái MỚI NHẤT (không tin vào thông tin cũ),
                            xem CustomUserDetailsService.
                        */
                        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                        /*
                            -----------------------------------------------------
                            Bước 7: Dán nhãn vào SecurityContext
                            -----------------------------------------------------
                            Chỉ dán nhãn khi isEnabled() = true, tức tài khoản ACTIVE và ACCEPT.
                            Tài khoản UNVERIFIED, DISABLED, DELETED, hoặc bị đổi sang REFUSE
                            → không dán nhãn → là "khách".

                            SecurityContextHolder là gì?
                            - Là "cái hộp" giữ thông tin người đang gửi request hiện tại.
                            - Mỗi request chạy trên 1 thread riêng và có hộp riêng,
                              nên request của người A không lẫn với request của người B.
                            - Hộp được dọn sạch khi request kết thúc. Vì cấu hình STATELESS,
                              Spring không cất nhãn vào session để dùng lại
                              → request sau phải gửi token lại từ đầu.

                            UsernamePasswordAuthenticationToken là "cái nhãn" bỏ vào hộp, gồm 3 phần:
                            - principal = userDetails: người đang gọi (email, role).
                            - credentials = null: không cần giữ mật khẩu, token đã chứng minh danh tính.
                            - authorities = userDetails.getAuthorities(): danh sách quyền, ví dụ [ROLE_ADMIN].
                              @PreAuthorize("hasRole(...)") đọc danh sách này để kiểm tra role.
                            Dùng constructor 3 tham số thì nhãn tự được đánh dấu "đã xác thực".
                            (Tên class có chữ "UsernamePassword" nhưng ở đây không liên quan mật khẩu,
                             chỉ là loại nhãn có sẵn tiện dùng nhất của Spring.)

                            SecurityContextHolder.getContext().setAuthentication(...):
                            lấy hộp của request hiện tại, bỏ nhãn vào.

                            Sau bước này, Controller lấy được người đang gọi bằng:
                            - authentication.getName() → email (cách các Controller trong dự án đang dùng)
                            - hoặc @AuthenticationPrincipal UserDetails userDetails
                        */
                        if (userDetails.isEnabled()) {
                            SecurityContextHolder.getContext().setAuthentication(
                                    new UsernamePasswordAuthenticationToken(
                                            userDetails, null, userDetails.getAuthorities()));
                        }
                    }
                } catch (Exception e) {}
                /*
                    -----------------------------------------------------
                    catch rỗng: lỗi gì cũng coi như "khách"
                    -----------------------------------------------------
                    Bắt mọi lỗi ở bước 4 đến 7, VD:
                    - Không tìm thấy User trong MySQL (UsernameNotFoundException).
                    - Redis hoặc MySQL mất kết nối.
                    Bắt rồi bỏ qua → request đi tiếp với tư cách "khách" → API cần đăng nhập trả 401,
                    thay vì để lỗi văng ra làm hỏng cả request.

                    Lưu ý khi dò lỗi: khác RateLimitFilter (Redis lỗi thì vẫn cho qua),
                    ở đây Redis lỗi thì KHÔNG xác thực được ai → mọi API cần đăng nhập đều 401.
                    Vì an toàn: không chắc phiên còn hợp lệ thì không cho vào.
                    catch rỗng nên không ghi log: thấy mọi người đột nhiên bị 401
                    thì nên kiểm tra Redis và MySQL trên VPS trước.
                */
            }
        }

        /*
            -----------------------------------------------------
            Bước 8: Chuyển request sang filter tiếp theo
            -----------------------------------------------------
            Luôn chạy, dù đã dán nhãn hay chưa (filter này không chặn, xem đầu file).
            Filter tiếp theo là RateLimitFilter (thứ tự đặt trong SecurityConfig).
            Quên dòng này → request dừng ở đây, frontend nhận response rỗng.
        */
        filterChain.doFilter(request, response);
    }
}