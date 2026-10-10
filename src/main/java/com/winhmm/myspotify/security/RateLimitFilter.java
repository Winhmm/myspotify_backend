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
    Giới hạn số request vào backend.
    - /api/auth/** (đăng nhập, đăng ký, OTP...): đếm theo IP, giới hạn thấp
      → chống dò mật khẩu, chống spam email OTP.
    - Các API khác:
        + Đã đăng nhập → đếm theo email (mỗi tài khoản 1 bộ đếm).
        + Chưa đăng nhập → đếm theo IP.

    Không gắn @Component: filter này được tạo bằng "new" trong SecurityConfig.
    (Nếu gắn @Component, Spring Boot sẽ tự đăng ký thêm 1 lần nữa ở ngoài

    Spring Security → chạy trước JWT filter → không biết tài khoản nào → đếm sai).
*/
public class RateLimitFilter extends OncePerRequestFilter {
    private final RateLimitService rateLimitService;

    public RateLimitFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    /*
        Những request không đếm:
        - OPTIONS: trình duyệt tự gửi để hỏi CORS.
        - /uploads/**: file nhạc, ảnh. Phát nhạc / tua bài gửi rất nhiều request nhỏ.
        - /actuator/**, Swagger: kiểm tra hệ thống / tài liệu API.
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

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String ip = request.getRemoteAddr();

        String key;
        int limit;

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

        boolean allowed;
        try {
            allowed = rateLimitService.tryAcquire(key, limit);
        } catch (Exception e) {
            allowed = true;
        }

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

        filterChain.doFilter(request, response);
    }
}
