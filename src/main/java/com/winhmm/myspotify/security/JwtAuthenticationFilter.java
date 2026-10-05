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
    Chạy trước Controller ở mỗi request:
    1. Lấy token từ header "Authorization: Bearer <token>".
    2. Token hợp lệ + mã phiên khớp Redis → tìm User → báo cho Spring "request này của User X".
    3. Không có token / token sai / phiên không khớp → bỏ qua,
       Spring sẽ tự chặn nếu API cần đăng nhập.
*/
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /*
        Tên "ghi chú" gắn vào request khi phiên không hợp lệ.

        SecurityConfig đọc ghi chú này để trả message rõ ràng cho frontend.
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

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            if (jwtUtil.validateToken(token)) {
                try {
                    String email = jwtUtil.getEmailFromToken(token);
                    String sessionId = jwtUtil.getSessionIdFromToken(token);

                    /*
                        Mã phiên trong token khác với Redis:
                        - Đã đăng nhập ở thiết bị khác (phiên bị ghi đè), hoặc
                        - Đã đăng xuất / đổi mật khẩu (phiên bị xóa).
                        → Ghi chú lại, KHÔNG xác thực request này.
                    */
                    if (!sessionService.isValidSession(email, sessionId)) {
                        request.setAttribute(SESSION_ERROR,
                                "Your session has ended. Your account may have been logged in on another device");
                    } else {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                        if (userDetails.isEnabled()) {
                            SecurityContextHolder.getContext().setAuthentication(
                                    new UsernamePasswordAuthenticationToken(
                                            userDetails, null, userDetails.getAuthorities()));
                        }
                    }
                } catch (Exception e) {
                    // Không tìm thấy User / Redis lỗi → coi như chưa đăng nhập.
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}