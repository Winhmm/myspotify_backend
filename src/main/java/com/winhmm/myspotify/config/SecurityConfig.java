package com.winhmm.myspotify.config;

import com.winhmm.myspotify.security.CustomUserDetailsService;
import com.winhmm.myspotify.security.JwtAuthenticationFilter;
import com.winhmm.myspotify.security.JwtUtil;
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

    public SecurityConfig(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                /*
                    Dùng bean corsConfigurationSource trong CorsConfig.
                */
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        /*
                            Cho phép mọi request preflight của trình duyệt.
                        */
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        /*
                            Các API không cần đăng nhập:
                            - /api/auth/**: đăng nhập, đăng ký, OTP, quên mật khẩu.
                            - /uploads/**: file tĩnh (ảnh, nhạc).
                            - /actuator/health: kiểm tra sức khỏe ứng dụng.
                            - Swagger: tài liệu API.
                            - /error: trang lỗi mặc định của Spring Boot.
                        */
                        .requestMatchers("/api/auth/**", "/uploads/**", "/actuator/health",
                                "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
                                "/error").permitAll()
                        /*
                            Các API còn lại cần đăng nhập.
                        */
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        /*
                            Chưa đăng nhập / token sai / token hết hạn → 401.
                        */
                        .authenticationEntryPoint((request, response, authException) ->
                                writeJsonError(response, 401, "Unauthorized, please login"))
                        /*
                            Đã đăng nhập nhưng không đủ quyền (VD: USER gọi API ADMIN) → 403.
                        */
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeJsonError(response, 403, "You do not have permission to access this resource"))
                )
                /*
                    JwtAuthenticationFilter chạy trước UsernamePasswordAuthenticationFilter:
                    1. Lấy token từ header.
                    2. Token hợp lệ → tìm User → báo cho Spring "request này của User X".
                    3. Không có token / token sai → bỏ qua, Spring sẽ tự chặn nếu API cần đăng nhập.
                */
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil, userDetailsService),
                        UsernamePasswordAuthenticationFilter.class);

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