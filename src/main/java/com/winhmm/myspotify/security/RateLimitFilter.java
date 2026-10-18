package com.winhmm.myspotify.security;

import com.winhmm.myspotify.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/*
    =====================================================
    RATELIMITFILTER - GIỚI HẠN SỐ REQUEST VÀO BACKEND
    =====================================================

    Chạy ngay sau JwtAuthenticationFilter ở mỗi request (thứ tự được đặt trong SecurityConfig).
    Phần đếm và quyết định cho qua hay chặn nằm trong RateLimitService.

    -----------------------------------------------------
    Đếm theo gì?
    -----------------------------------------------------
    - /api/auth/** (đăng nhập, đăng ký, OTP, quên mật khẩu):
      đếm theo IP, giới hạn thấp (10 request mỗi 60 giây)
      → chống dò mật khẩu, chống spam email OTP.
      Đếm theo IP vì lúc này người gọi chưa đăng nhập, chưa biết là ai.
    - Các API khác (giới hạn 100 request mỗi 60 giây):
        + Đã đăng nhập → đếm theo email, mỗi tài khoản 1 bộ đếm riêng.
          Công bằng hơn đếm theo IP, vì nhiều người có thể dùng chung 1 IP
          (cùng wifi trường học, quán cà phê).
        + Chưa đăng nhập → đếm theo IP.

    Vượt giới hạn → 429 Too Many Requests, kèm header Retry-After (số giây phải chờ).

    -----------------------------------------------------
    Vì sao phải chạy sau JwtAuthenticationFilter?
    -----------------------------------------------------
    Để đếm theo email, filter cần biết request của ai.
    Thông tin đó chỉ có sau khi JwtAuthenticationFilter đọc token
    và đặt người dùng vào SecurityContext.

    -----------------------------------------------------
    Vì sao không gắn @Component?
    -----------------------------------------------------
    Filter này được tạo bằng new trong SecurityConfig.
    Nếu gắn @Component, Spring Boot sẽ tự đăng ký thêm 1 lần nữa ở tầng servlet,
    bên ngoài Spring Security. Bản thứ hai đó chạy trước JWT filter,
    nên không biết request của tài khoản nào → mọi request bị đếm theo IP, và bị đếm 2 lần.

    -----------------------------------------------------
    Khi Redis bị lỗi
    -----------------------------------------------------
    Cho request đi qua (fail-open), không chặn.
    Lý do: rate limit là lớp bảo vệ phụ. Redis sập mà chặn hết request
    thì cả hệ thống ngừng hoạt động, thiệt hại lớn hơn việc tạm thời không giới hạn.
*/
public class RateLimitFilter extends OncePerRequestFilter {
    private final RateLimitService rateLimitService;

    public RateLimitFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    /*
        =====================================================
        NHỮNG REQUEST KHÔNG ĐẾM
        =====================================================

        shouldNotFilter trả true → filter bỏ qua request đó, không chạy doFilterInternal.

        - OPTIONS: trình duyệt tự gửi để hỏi CORS (preflight), không phải người dùng gọi.
        - /uploads/: file nhạc và ảnh. Phát và tua 1 bài gửi rất nhiều request nhỏ
          (mỗi lần tua là 1 request lấy 1 đoạn file), đếm vào sẽ nhanh chóng vượt giới hạn.
        - /actuator/: kiểm tra server còn sống không.
        - /swagger-ui, /v3/api-docs: trang tài liệu API, tải nhiều file tĩnh.
    */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || path.startsWith("/uploads/")
                || path.startsWith("/actuator/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }

    /*
        =====================================================
        ĐẾM VÀ QUYẾT ĐỊNH CHO QUA HAY CHẶN
        =====================================================
    */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        /*
            -----------------------------------------------------
            Bước 1: Lấy đường dẫn và IP của người gửi
            -----------------------------------------------------

            getRemoteAddr(): IP của máy kết nối trực tiếp tới backend.

            Lưu ý: hiện tại frontend gọi thẳng vào cổng 8080 nên đây là IP thật của người dùng.
            Nếu sau này đặt Nginx phía trước backend, getRemoteAddr() sẽ luôn là IP của Nginx
            (127.0.0.1) → mọi người dùng chung 1 bộ đếm. Khi đó phải đọc IP thật từ
            header X-Forwarded-For do Nginx gửi kèm.
        */
        String path = request.getRequestURI();
        String ip = request.getRemoteAddr();

        String key;
        int limit;

        /*
            -----------------------------------------------------
            Bước 2: Chọn bộ đếm (key Redis) và giới hạn
            -----------------------------------------------------

            - /api/auth/... → key rate:auth:<IP>, giới hạn auth-requests (10).
            - Đã đăng nhập  → key rate:user:<email>, giới hạn user-requests (100).
            - Chưa đăng nhập → key rate:ip:<IP>, giới hạn user-requests (100).

            Kiểm tra "đã đăng nhập" gồm 3 điều kiện:
            - auth != null: có thông tin xác thực.
            - auth.isAuthenticated(): đã được xác thực.
            - Không phải AnonymousAuthenticationToken: Spring gắn nhãn "khách" (anonymous)
              cho request chưa đăng nhập, nhãn này cũng có isAuthenticated() = true,
              nên phải loại riêng.
            Lưu ý: ở vị trí filter hiện tại, nhãn "khách" thường chưa được gắn,
            điều kiện thứ ba là để an toàn nếu sau này thứ tự filter thay đổi.
        */
        if(path.startsWith("/api/auth/")) {
            key = "rate:auth:" + ip;
            limit = rateLimitService.getAuthRequests();
        } else {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if(auth != null && auth.isAuthenticated()
                    && !(auth instanceof AnonymousAuthenticationToken)) {
                key = "rate:user:" + auth.getName();
            } else {
                key = "rate:ip:" + ip;
            }
            limit = rateLimitService.getUserRequests();
        }

        /*
            -----------------------------------------------------
            Bước 3: Đếm request
            -----------------------------------------------------

            Redis lỗi (mất kết nối...) → cho qua (fail-open), xem giải thích ở đầu file.
        */
        boolean allowed;
        try {
            allowed = rateLimitService.tryAcquire(key, limit);
        } catch (Exception e) {
            allowed = true;
        }

        /*
            -----------------------------------------------------
            Bước 4: Vượt giới hạn → trả 429 và dừng
            -----------------------------------------------------

            - setStatus(429): Too Many Requests.
            - Header Retry-After: số giây còn lại đến khi bộ đếm được xóa.
              Frontend đọc được header này nhờ CorsConfig đã expose "Retry-After".
            - Body JSON cùng định dạng với các lỗi khác: { "message": "..." }.

            Phải tự ghi response ở đây vì lỗi xảy ra trong filter,
            trước khi tới Controller, nên GlobalExceptionHandler không xử lý được.

            return: không gọi filterChain.doFilter → request không đi tiếp vào Controller.
        */
        if(!allowed) {
            long retryAfter = rateLimitService.getRetryAfterSeconds(key);

            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"message\":\"Too many requests. Please try again in "
                            + retryAfter + " seconds\"}"
            );
            /*
                Không cho request vào Controller
            */
            return;
        }

        /*
            -----------------------------------------------------
            Bước 5: Chưa vượt giới hạn → cho đi tiếp
            -----------------------------------------------------
        */
        filterChain.doFilter(request, response);
    }
}
