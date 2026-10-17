package com.winhmm.myspotify.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/*
    =====================================================
    WEBMVCCONFIG - PHỤC VỤ FILE ĐÃ UPLOAD QUA URL
    =====================================================

    -----------------------------------------------------
    Class này làm gì?
    -----------------------------------------------------

    Biến 1 thư mục trên ổ đĩa thành 1 đường dẫn URL, để frontend mở được file nhạc và ảnh.
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
