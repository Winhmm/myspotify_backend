package com.winhmm.myspotify.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/*
    Cho phép truy cập file đã upload qua đường dẫn /uploads/**.
    VD: file lưu tại {uploadDir}/avatars/a.jpg → GET /uploads/avatars/a.jpg
    Spring tự hỗ trợ Range request (HTTP 206) → frontend tua nhạc được.
*/
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Value("${app.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadDir).toAbsolutePath().toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
    }
}
