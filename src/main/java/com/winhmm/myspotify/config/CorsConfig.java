package com.winhmm.myspotify.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/*
    =====================================================
    CORSCONFIG - CHO PHÉP FRONTEND Ở DOMAIN KHÁC GỌI API
    =====================================================

    -----------------------------------------------------
    CORS là gì?
    -----------------------------------------------------
    Trình duyệt có 1 luật an toàn gọi là Same-Origin Policy:
    trang web ở origin A không được tự ý đọc dữ liệu từ API ở origin B.

    Origin = giao thức + domain + port. Chỉ cần khác 1 trong 3 là khác origin.
    Ví dụ:
    - Frontend: http://localhost:5173
    - Backend : http://157.66.100.87:8080
    → Khác domain và khác port → khác origin → trình duyệt chặn,
      trừ khi backend trả về các header cho phép (Access-Control-Allow-Origin...).

    CORS (Cross-Origin Resource Sharing) là cách backend nói với trình duyệt:
    "Tôi cho phép origin này gọi tôi."

    -----------------------------------------------------
    Những điều cần nhớ
    -----------------------------------------------------
    - CORS chỉ do trình duyệt kiểm tra. Postman và curl không quan tâm tới CORS.
      → Test bằng Postman chạy được nhưng frontend báo lỗi CORS là chuyện bình thường
        nếu cấu hình CORS sai.
    - CORS bảo vệ người dùng trình duyệt, không phải cơ chế bảo vệ server.
      Server vẫn phải dựa vào JWT và phân quyền để bảo vệ dữ liệu.

    -----------------------------------------------------
    Preflight là gì?
    -----------------------------------------------------
    Với request "phức tạp" (có header Authorization, method PUT/DELETE/PATCH,
    hoặc Content-Type là application/json), trình duyệt gửi trước 1 request OPTIONS để hỏi:

        OPTIONS /api/songs/5
        Origin: http://localhost:5173
        Access-Control-Request-Method: PUT
        Access-Control-Request-Headers: authorization, content-type

    Server trả lời cho phép:

        Access-Control-Allow-Origin: http://localhost:5173
        Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD
        Access-Control-Allow-Headers: authorization, content-type
        Access-Control-Max-Age: 3600

    Sau đó trình duyệt mới gửi request PUT thật.
    Vì request OPTIONS không mang token, SecurityConfig phải permitAll cho OPTIONS.

    -----------------------------------------------------
    Class này được dùng thế nào?
    -----------------------------------------------------
    SecurityConfig gọi .cors(Customizer.withDefaults()),
    Spring Security tự tìm bean CorsConfigurationSource bên dưới và áp dụng.
*/
@Configuration
public class CorsConfig {
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        /*
            -----------------------------------------------------
            Origin nào được phép gọi
            -----------------------------------------------------
            "*" = mọi origin đều được phép.

            Vì sao dùng setAllowedOriginPatterns thay vì setAllowedOrigins?
            - setAllowedOrigins("*") kết hợp với setAllowCredentials(true)
              là cấu hình không hợp lệ, Spring sẽ báo lỗi khi có request.
            - setAllowedOriginPatterns("*") thì Spring trả lại đúng origin của request
              (ví dụ http://localhost:5173) thay vì dấu "*" → hợp lệ.

            Lưu ý: cấu hình này tiện lúc đang phát triển.
            Khi frontend có domain thật, nên đổi thành danh sách cụ thể,
            ví dụ List.of("https://myspotify.vn", "http://localhost:5173").
        */
        config.setAllowedOriginPatterns(List.of("*"));
        /*
            -----------------------------------------------------
            Method nào được phép dùng
            -----------------------------------------------------
            - GET, POST, PUT, PATCH, DELETE: các thao tác thông thường của API.
            - OPTIONS: dùng cho preflight.
            - HEAD: giống GET nhưng chỉ lấy header, không lấy body
              (thẻ audio đôi khi dùng để kiểm tra kích thước file).
        */
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        /*
            -----------------------------------------------------
            Header nào frontend được gửi lên
            -----------------------------------------------------
            "*" = mọi header. Quan trọng nhất là:
            - Authorization: chứa JWT.
            - Content-Type: application/json hoặc multipart/form-data (khi upload file).
        */
        config.setAllowedHeaders(List.of("*"));
         /*
            -----------------------------------------------------
            Header nào JavaScript được đọc trong response
            -----------------------------------------------------
            Mặc định, JavaScript chỉ đọc được vài header cơ bản (Content-Type, Cache-Control...).
            Muốn JavaScript đọc thêm header nào thì phải khai báo ở đây ("expose"):

            - Authorization: hiện project trả token trong body nên chưa dùng, để sẵn không ảnh hưởng.
            - Content-Disposition: chứa tên file khi tải file về.
            - Content-Range: cho biết đoạn dữ liệu đang trả về, ví dụ "bytes 0-1023/5000000".
            - Accept-Ranges: báo server hỗ trợ tải từng đoạn → tua nhạc được.
            - Content-Length: kích thước dữ liệu trả về.
            - Retry-After: khi bị 429, cho frontend biết phải chờ bao nhiêu giây.
        */
        config.setExposedHeaders(List.of(
                "Authorization",
                "Content-Disposition",
                "Content-Range",
                "Accept-Ranges",
                "Content-Length",
                "Retry-After"
        ));
        /*
            -----------------------------------------------------
            Cho phép gửi kèm thông tin xác thực
            -----------------------------------------------------
            true = cho phép trình duyệt gửi kèm cookie khi gọi API.
            Cần thiết nếu frontend dùng axios với withCredentials: true,
            nếu không trình duyệt sẽ báo lỗi CORS.
        */
        config.setAllowCredentials(true);
        /*
            -----------------------------------------------------
            Thời gian trình duyệt nhớ kết quả preflight
            -----------------------------------------------------
            3600 giây = 1 giờ. Trong 1 giờ, trình duyệt không gửi lại request OPTIONS
            cho cùng 1 URL → giảm số request, frontend chạy nhanh hơn.
        */
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

