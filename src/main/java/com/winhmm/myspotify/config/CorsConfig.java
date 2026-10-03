package com.winhmm.myspotify.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/*
    Cấu hình CORS dùng chung cho toàn bộ ứng dụng (Spring Security tự động dùng bean này).

    - Cho phép mọi origin (frontend chạy ở đâu cũng gọi được).
    - Cho phép mọi method thường dùng, kể cả PATCH và OPTIONS (preflight).
    - Cho phép mọi header, bao gồm Authorization khi gửi JWT.
    - Cho phép credentials (axios withCredentials: true vẫn chạy).
    - Expose một số header để JavaScript đọc được (tải file, tua nhạc).
    - Áp dụng cho mọi đường dẫn: /api/**, /uploads/**, ...
    - Cache kết quả preflight trong 3600 giây.
*/
@Configuration
public class CorsConfig {
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of(
                "Authorization",
                "Content-Disposition",
                "Content-Range",
                "Accept-Ranges",
                "Content-Length"
        ));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

