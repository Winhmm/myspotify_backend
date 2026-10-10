package com.winhmm.myspotify.config;

import com.winhmm.myspotify.security.CustomUserDetailsService;
import com.winhmm.myspotify.security.JwtAuthenticationFilter;
import com.winhmm.myspotify.security.JwtUtil;
import com.winhmm.myspotify.security.RateLimitFilter;
import com.winhmm.myspotify.service.RateLimitService;
import com.winhmm.myspotify.service.SessionService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final SessionService sessionService;
    private final RateLimitService rateLimitService;

    public SecurityConfig(JwtUtil jwtUtil,
                          CustomUserDetailsService userDetailsService,
                          SessionService sessionService,
                          RateLimitService rateLimitService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.sessionService = sessionService;
        this.rateLimitService = rateLimitService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**", "/uploads/**", "/actuator/health",
                                "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
                                "/error").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        /*
                            Chưa đăng nhập / token sai / phiên không hợp lệ → 401.

                            Nếu filter có ghi chú "phiên không hợp lệ" → trả message đó,
                            để frontend biết vì sao bị đăng xuất.
                        */
                        .authenticationEntryPoint((request, response, authException) -> {
                            Object sessionError = request.getAttribute(JwtAuthenticationFilter.SESSION_ERROR);
                            String message = (sessionError != null)
                                    ? sessionError.toString()
                                    : "Unauthorized, please login";
                            writeJsonError(response, 401, message);
                        })
                        /*
                            Đã đăng nhập nhưng không đủ quyền → 403.
                        */
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeJsonError(response, 403, "You do not have permission to access this resource"))
                )
                /*
                    1. JWT filter: đọc token → biết request của tài khoản nào.
                */
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil, userDetailsService, sessionService),
                        UsernamePasswordAuthenticationFilter.class)

                /*
                    2. MỚI - Rate limit filter: chạy ngay sau JWT filter
                       → đã biết email → đếm theo tài khoản.
                */
                .addFilterAfter(new RateLimitFilter(rateLimitService),
                        JwtAuthenticationFilter.class);

        return http.build();
    }

    /*
        Ghi lỗi dạng JSON, cùng format với GlobalExceptionHandler: { "message": "..." }
    */
    private void writeJsonError(HttpServletResponse response, int status,
                                String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}