package com.winhmm.myspotify.security;

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
    2. Token hợp lệ → tìm User → báo cho Spring "request này của User X".
    3. Không có token / token sai → bỏ qua, Spring sẽ tự chặn nếu API cần đăng nhập.
*/
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if(header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            if(jwtUtil.validateToken(token)) {
                try {
                    String email = jwtUtil.getEmailFromToken(token);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                    if(userDetails.isEnabled()) {
                        SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities()));
                    }
                } catch (Exception e) {
                    // Không tìm thấy User → coi như chưa đăng nhập.
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}