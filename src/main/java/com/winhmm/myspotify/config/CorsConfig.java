package com.winhmm.myspotify.config;

/*
    -----------------------------------------------------
    Dòng package ở trên
    -----------------------------------------------------
    package = "địa chỉ nhà" của class. File này nằm trong thư mục
    src/main/java/com/winhmm/myspotify/config nên package là com.winhmm.myspotify.config.

    Vì package này nằm bên trong com.winhmm.myspotify (package của MyspotifyApplication),
    Spring tự quét thấy class này khi khởi động (component scan).
    Nếu để class ở package khác, ví dụ com.abc.config, Spring sẽ không thấy và CORS không chạy.

    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    import = mượn class ở chỗ khác về dùng trong file này.
    Không import thì mỗi lần dùng phải viết tên đầy đủ,
    ví dụ org.springframework.web.cors.CorsConfiguration, rất dài.

    - Bean, Configuration: 2 annotation của Spring dùng để khai báo cấu hình và tạo đối tượng.
    - CorsConfiguration: đối tượng chứa các luật CORS (origin nào, method nào, header nào...).
    - CorsConfigurationSource: interface có nghĩa là "nguồn cấu hình CORS".
      Khi có request tới, Spring hỏi nó: "URL này thì dùng luật CORS nào?".
    - UrlBasedCorsConfigurationSource: class có sẵn của Spring, cài đặt interface trên,
      cho phép gắn luật CORS theo mẫu URL (ví dụ "/**" là mọi URL).
    - List: kiểu danh sách của Java. List.of(...) tạo nhanh 1 danh sách cố định.
*/
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
    Origin là gì?
    -----------------------------------------------------
    Origin = giao thức + domain + port. Chỉ cần khác 1 trong 3 là khác origin.

        http://localhost:5173       → giao thức http, domain localhost, port 5173
        http://157.66.100.87:8080   → giao thức http, domain 157.66.100.87, port 8080

    2 địa chỉ trên khác domain và khác port → khác origin.

    Một vài ví dụ khác:
        http://localhost:5173  và  http://localhost:8080   → khác port      → khác origin
        http://myspotify.vn    và  https://myspotify.vn    → khác giao thức → khác origin
        http://localhost:5173/a và http://localhost:5173/b → chỉ khác đường dẫn → cùng origin

    -----------------------------------------------------
    Same-Origin Policy - luật an toàn của trình duyệt
    -----------------------------------------------------
    Trình duyệt có 1 luật gọi là Same-Origin Policy (chính sách cùng nguồn gốc):
    JavaScript của trang web ở origin A không được tự ý đọc dữ liệu trả về từ origin B.

    Vì sao lại có luật này?
    Giả sử bạn đang đăng nhập Facebook, rồi vào 1 trang web lạ.
    Nếu không có luật này, JavaScript của trang lạ có thể gọi API của Facebook
    (trình duyệt tự gửi kèm cookie đăng nhập của bạn) rồi đọc tin nhắn riêng của bạn.
    Same-Origin Policy ngăn chuyện đó: trang lạ gửi được request nhưng không đọc được kết quả.

    Với MySpotify:
    - Frontend chạy ở http://localhost:5173 (máy bạn frontend).
    - Backend chạy ở http://157.66.100.87:8080 (VPS).
    → Khác origin → mặc định trình duyệt không cho JavaScript của frontend đọc dữ liệu từ backend.

    -----------------------------------------------------
    CORS là gì?
    -----------------------------------------------------
    CORS (Cross-Origin Resource Sharing - chia sẻ tài nguyên giữa các origin)
    là cách backend "ký giấy cho phép" gửi trình duyệt, thông qua các header trong response:

        Access-Control-Allow-Origin: http://localhost:5173

    Trình duyệt đọc header này, thấy origin của trang web có trong "giấy phép"
    → giao dữ liệu cho JavaScript. Không thấy → báo lỗi CORS trong Console:

        Access to XMLHttpRequest at 'http://157.66.100.87:8080/api/songs'
        from origin 'http://localhost:5173' has been blocked by CORS policy

    -----------------------------------------------------
    Những điều rất hay bị hiểu nhầm
    -----------------------------------------------------
    1. CORS chỉ do trình duyệt kiểm tra.
       Postman, curl, app Android/iOS không quan tâm tới CORS.
       → Test bằng Postman chạy được nhưng frontend báo lỗi CORS là chuyện bình thường
         nếu cấu hình CORS sai.

    2. CORS không phải là cơ chế bảo vệ server.
       Kẻ xấu dùng Postman vẫn gọi được API, CORS không chặn được.
       Server vẫn phải dựa vào JWT, phân quyền và rate limit để bảo vệ dữ liệu.
       CORS chỉ bảo vệ người dùng đang mở trình duyệt.

    3. Với request đơn giản (xem phần dưới), request vẫn tới được server và server vẫn xử lý.
       Trình duyệt chỉ "giấu" kết quả, không cho JavaScript đọc.

    -----------------------------------------------------
    Request đơn giản và preflight
    -----------------------------------------------------
    Request đơn giản: method là GET, HEAD hoặc POST, không có header đặc biệt,
    Content-Type chỉ là 1 trong 3 loại: text/plain, multipart/form-data,
    application/x-www-form-urlencoded.
    → Trình duyệt gửi thẳng request, rồi kiểm tra header CORS trong response.

    Các request còn lại gọi là request "phức tạp", ví dụ:
    - Có header Authorization (gửi JWT). Hầu hết API của MySpotify đều có header này.
    - Method là PUT, PATCH, DELETE.
    - Content-Type là application/json.

    Với request phức tạp, trước khi gửi request thật, trình duyệt tự gửi
    1 request OPTIONS để hỏi trước (gọi là preflight - "bay thử"):

        OPTIONS /api/songs/5
        Origin: http://localhost:5173
        Access-Control-Request-Method: PUT
        Access-Control-Request-Headers: authorization, content-type

    Nghĩa là: "Tôi là trang ở localhost:5173, tôi muốn gửi PUT kèm 2 header này, có được không?"

    Server trả lời cho phép:

        HTTP/1.1 200 OK
        Access-Control-Allow-Origin: http://localhost:5173
        Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE,OPTIONS,HEAD
        Access-Control-Allow-Headers: authorization, content-type
        Access-Control-Allow-Credentials: true
        Access-Control-Max-Age: 3600

    Trình duyệt thấy được phép → mới gửi request PUT thật.
    Nếu preflight bị từ chối → request PUT thật không bao giờ được gửi đi.

    Mẹo debug: mở tab Network của trình duyệt, sẽ thấy 2 dòng cho cùng 1 URL:
    1 dòng OPTIONS (preflight) và 1 dòng PUT (request thật).

    -----------------------------------------------------
    Class này được dùng thế nào?
    -----------------------------------------------------
    1. Khi khởi động, Spring thấy @Configuration → tạo đối tượng CorsConfig,
       gọi hàm corsConfigurationSource() (vì có @Bean), cất kết quả vào container.
    2. SecurityConfig gọi .cors(Customizer.withDefaults()).
       Spring Security tìm bean CorsConfigurationSource này và tạo ra 1 CorsFilter.
    3. CorsFilter được đặt gần như đầu tiên trong chuỗi filter bảo mật.
       Với mỗi request:
       - Nếu là preflight hợp lệ → CorsFilter tự trả lời 200 kèm các header cho phép
         và dừng luôn, request OPTIONS không đi tiếp vào JwtAuthenticationFilter hay Controller.
       - Nếu là request thật → CorsFilter gắn thêm header Access-Control-Allow-Origin...
         vào response, rồi cho request đi tiếp.
       - Nếu origin hoặc method không được phép → trả 403 "Invalid CORS request".

    Vì sao cấu hình CORS bên trong Spring Security mà không cấu hình ở Spring MVC?
    Vì CorsFilter đứng trước các filter khác, nên cả những response lỗi do filter trả về
    như 401 (chưa đăng nhập) hay 429 (gọi quá nhiều) cũng có header CORS.
    Nhờ vậy frontend đọc được nội dung lỗi {"message": "..."}.
    Nếu chỉ cấu hình CORS ở Spring MVC, response 401 và 429 sẽ thiếu header CORS,
    trình duyệt chỉ báo "lỗi CORS" chung chung, frontend không biết lỗi thật là gì.

    -----------------------------------------------------
    @Configuration
    -----------------------------------------------------
    Annotation đặt trên class, báo cho Spring biết: "class này chứa cấu hình,
    bên trong có các hàm @Bean, hãy gọi chúng khi khởi động để tạo đối tượng".
*/
@Configuration
public class CorsConfig {
    /*
        =====================================================
        TẠO BEAN CORSCONFIGURATIONSOURCE
        =====================================================

        @Bean: Spring gọi hàm này đúng 1 lần khi khởi động, lấy đối tượng được return
        và cất vào container (kho đối tượng của Spring). Ai cần thì Spring lấy ra đưa cho.

        - public: hàm công khai, Spring gọi được.
        - CorsConfigurationSource: kiểu dữ liệu trả về của hàm.
        - corsConfigurationSource: tên hàm, cũng là tên của bean trong container.
          Nên giữ nguyên tên này: một số phiên bản Spring Security
          tìm bean CORS theo đúng tên "corsConfigurationSource".
        - (): hàm không nhận tham số nào.
    */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        /*
            -----------------------------------------------------
            Tạo đối tượng chứa luật CORS
            -----------------------------------------------------
            new CorsConfiguration(): tạo 1 đối tượng rỗng, chưa cho phép gì cả.
            Các dòng bên dưới lần lượt "điền luật" vào đối tượng này.

            Cú pháp: KiểuDữLiệu tênBiến = new KiểuDữLiệu();
        */
        CorsConfiguration config = new CorsConfiguration();

        /*
            -----------------------------------------------------
            Origin nào được phép gọi
            -----------------------------------------------------
            List.of("*"): tạo danh sách có 1 phần tử là "*".
            "*" là mẫu khớp với mọi origin → frontend chạy ở đâu cũng gọi được.

            Vì sao dùng setAllowedOriginPatterns thay vì setAllowedOrigins?
            - setAllowedOrigins("*") kết hợp với setAllowCredentials(true) (ở dưới)
              là cấu hình không hợp lệ theo chuẩn CORS, Spring sẽ ném lỗi
              IllegalArgumentException ngay khi có request tới.
            - setAllowedOriginPatterns("*") thì Spring không trả về dấu "*",
              mà trả lại đúng origin của request, ví dụ:
                  Access-Control-Allow-Origin: http://localhost:5173
              → hợp lệ, và trình duyệt chấp nhận.

            Lưu ý về an toàn:
            - Hiện tại token được gửi qua header Authorization, không qua cookie,
              nên cho phép mọi origin không gây nguy hiểm: trang web lạ không có token để gửi.
            - Nếu sau này đổi sang lưu token trong cookie HttpOnly (như bạn frontend đề xuất),
              thì phải đổi "*" thành danh sách origin cụ thể, ví dụ:
                  List.of("https://myspotify.vn", "http://localhost:5173")
              Vì khi dùng cookie, trang web lạ có thể lợi dụng trình duyệt tự gửi kèm cookie.
        */
        config.setAllowedOriginPatterns(List.of("*"));

        /*
            -----------------------------------------------------
            Method nào được phép dùng
            -----------------------------------------------------
            Giá trị này được trả về trong header Access-Control-Allow-Methods của preflight.

            - GET: lấy dữ liệu (danh sách bài hát, thông tin cá nhân...).
            - POST: tạo mới (đăng ký, upload bài hát...).
            - PUT: cập nhật toàn bộ (sửa thông tin bài hát...).
            - PATCH: cập nhật 1 phần. Project hiện chưa dùng, để sẵn không ảnh hưởng.
            - DELETE: xóa.
            - OPTIONS: method của preflight.
            - HEAD: giống GET nhưng chỉ lấy header, không lấy body
              (ví dụ để biết kích thước file trước khi tải).

            Method không có trong danh sách (ví dụ TRACE) sẽ bị CorsFilter từ chối.
        */
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));

        /*
            -----------------------------------------------------
            Header nào frontend được gửi lên
            -----------------------------------------------------
            Giá trị này dùng để trả lời câu hỏi Access-Control-Request-Headers của preflight.

            "*" = mọi header đều được. Với "*", Spring trả lại đúng danh sách header
            mà trình duyệt đã hỏi. Quan trọng nhất với MySpotify là:
            - Authorization: chứa JWT, dạng "Bearer eyJhbGciOi...".
            - Content-Type: application/json (gửi JSON) hoặc multipart/form-data (upload file).
        */
        config.setAllowedHeaders(List.of("*"));

        /*
            -----------------------------------------------------
            Header nào JavaScript được đọc trong response
            -----------------------------------------------------
            Mặc định, với request khác origin, JavaScript chỉ đọc được vài header cơ bản:
            Cache-Control, Content-Language, Content-Length, Content-Type, Expires,
            Last-Modified, Pragma.
            Muốn JavaScript (axios, fetch) đọc thêm header nào thì phải "expose" ở đây.
            Giá trị này được trả về trong header Access-Control-Expose-Headers.

            - Authorization: hiện project trả token trong body (LoginResponse) nên chưa dùng,
              để sẵn không ảnh hưởng.
            - Content-Disposition: chứa tên file khi tải file về.
            - Content-Range: cho biết đoạn dữ liệu đang trả về, ví dụ "bytes 0-1023/5000000"
              (đoạn từ byte 0 tới 1023 của file 5.000.000 byte).
            - Accept-Ranges: giá trị "bytes" nghĩa là server hỗ trợ tải từng đoạn file.
            - Content-Length: kích thước dữ liệu trả về. Trình duyệt mới đã cho đọc sẵn,
              khai báo thêm cũng không sao.
            - Retry-After: khi bị 429 (gọi quá nhiều), RateLimitFilter gửi header này
              để cho frontend biết phải chờ bao nhiêu giây.

            Lưu ý: thẻ <audio> tự xử lý việc tua nhạc, không cần expose.
            Danh sách này chỉ cần khi chính code JavaScript tự đọc các header đó.
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
            Cho phép gửi kèm thông tin xác thực (credentials)
            -----------------------------------------------------
            true → server trả về header Access-Control-Allow-Credentials: true.

            Credentials ở đây là cookie (hoặc kiểu đăng nhập HTTP Basic của trình duyệt).
            Nếu frontend gọi axios với withCredentials: true (hoặc fetch với credentials: "include")
            mà server không trả header này → trình duyệt báo lỗi CORS.

            Hiện tại project gửi token qua header Authorization nên chưa thực sự cần,
            nhưng để true giúp frontend dù cấu hình withCredentials hay không đều chạy được.
        */
        config.setAllowCredentials(true);

        /*
            -----------------------------------------------------
            Thời gian trình duyệt nhớ kết quả preflight
            -----------------------------------------------------
            3600L: chữ L ở cuối nghĩa là số kiểu long (số nguyên lớn),
            vì hàm setMaxAge nhận tham số kiểu Long.

            3600 giây = 1 giờ → server trả header Access-Control-Max-Age: 3600.
            Trong 1 giờ, trình duyệt không gửi lại request OPTIONS cho cùng URL và cùng method
            → giảm 1 nửa số request, frontend chạy nhanh hơn.
            Mỗi trình duyệt có giới hạn tối đa riêng (ví dụ Chrome tối đa 2 giờ).
        */
        config.setMaxAge(3600L);

        /*
            -----------------------------------------------------
            Gắn luật CORS vào URL
            -----------------------------------------------------
            new UrlBasedCorsConfigurationSource(): tạo 1 "bảng tra cứu" URL → luật CORS.

            registerCorsConfiguration("/**", config): đăng ký luật config cho mọi URL.
            - "/*"  : khớp 1 cấp, ví dụ /abc, không khớp /api/songs.
            - "/**" : khớp mọi cấp, ví dụ /api/songs, /api/songs/5/play, /uploads/songs/audio/a.mp3.

            Nếu muốn, có thể gắn luật khác nhau cho từng nhóm URL, ví dụ:
                source.registerCorsConfiguration("/api/admin/**", adminConfig);
            Project chỉ cần 1 luật chung cho tất cả.

            return source: trả bảng tra cứu cho Spring, Spring cất vào container.
        */
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}