package com.winhmm.myspotify.config;

import com.winhmm.myspotify.security.CustomUserDetailsService;
import com.winhmm.myspotify.security.JwtAuthenticationFilter;
import com.winhmm.myspotify.security.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        /*
                            Các API không cần đăng nhập:
                            - /api/auth/**: API đăng nhập, đăng ký.
                            - /uploads/**: API tải file tĩnh (ảnh, nhạc).
                            - /actuator/health: API kiểm tra sức khỏe ứng dụng.
                            - /swagger-ui/**, /v3/api-docs/**: API tài liệu Swagger.
                            - /error: API lỗi mặc định của Spring Boot.
                         */
                        .requestMatchers("/api/auth/**", "/uploads/**", "/actuator/health",
                                "/swagger-ui/**", "/v3/api-docs/**", "/error").permitAll()
                        /*
                            Các API còn lại cần đăng nhập.
                        */
                        .anyRequest().authenticated()
                )
                /*
                    Nếu chưa đăng nhập mà truy cập API cần đăng nhập → trả về 401 Unauthorized.
                */
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                /*
                    Thêm JwtAuthenticationFilter vào trước UsernamePasswordAuthenticationFilter để:
                    1. Lấy token từ header.
                    2. Token hợp lệ → tìm User → báo cho Spring "request này của User X".
                    3. Không có token / token sai → bỏ qua, Spring sẽ tự chặn nếu API cần đăng nhập.
                */
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil, userDetailsService),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}