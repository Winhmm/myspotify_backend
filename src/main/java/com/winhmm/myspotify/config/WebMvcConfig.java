package com.winhmm.myspotify.config;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    - Value: annotation đọc giá trị từ application.properties.
    - Configuration: annotation đánh dấu class cấu hình.
    - ResourceHandlerRegistry: "sổ đăng ký" để khai báo URL nào trỏ tới thư mục nào.
    - WebMvcConfigurer: interface để tùy chỉnh thêm cho Spring MVC.
    - Paths: class tiện ích của Java để làm việc với đường dẫn file và thư mục.
*/
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
    Biến 1 thư mục trên ổ đĩa thành 1 đường dẫn URL, để frontend mở được file nhạc, ảnh, video.

    Ví dụ trên VPS (app.upload-dir=/home/myspotify/uploads):

        File trên ổ đĩa : /home/myspotify/uploads/songs/audio/3f2a9c1e.mp3
        Database lưu    : /uploads/songs/audio/3f2a9c1e.mp3
        Frontend mở     : http://157.66.100.87:8080/uploads/songs/audio/3f2a9c1e.mp3

    Frontend chỉ cần ghép địa chỉ server với đường dẫn trong database:
        <audio src="http://157.66.100.87:8080/uploads/songs/audio/3f2a9c1e.mp3">
        <img   src="http://157.66.100.87:8080/uploads/songs/covers/7b1d0a44.jpg">

    -----------------------------------------------------
    Vì sao cần class này?
    -----------------------------------------------------
    Mặc định Spring Boot chỉ phục vụ file tĩnh nằm trong project
    (thư mục src/main/resources/static), tức là file có sẵn lúc đóng gói JAR.
    File người dùng upload thì được lưu ra 1 thư mục bên ngoài JAR (FileStorageService ghi vào),
    nên phải khai báo thêm để Spring biết đường mà lấy.

    Vì sao không lưu file vào src/main/resources/static?
    - Khi chạy bằng file JAR, thư mục đó nằm bên trong JAR, không ghi thêm file vào được.
    - Mỗi lần deploy JAR mới sẽ mất hết file cũ.
    → Lưu ra thư mục riêng (app.upload-dir) là cách đúng.

    -----------------------------------------------------
    Ai phục vụ file thật sự?
    -----------------------------------------------------
    Không có Controller nào cả. Spring MVC có sẵn 1 class tên ResourceHttpRequestHandler,
    chuyên trả file tĩnh. Class này tự lo nhiều việc rất quan trọng:

    1. Tự đoán Content-Type theo đuôi file:
       .mp3 → audio/mpeg, .jpg → image/jpeg, .png → image/png, .mp4 → video/mp4.
       Nhờ đó trình duyệt biết đây là nhạc để phát, là ảnh để hiển thị.

    2. Hỗ trợ tải từng đoạn (HTTP Range) → đây là thứ giúp tua nhạc được:
       Khi người dùng kéo thanh tua tới giữa bài, thẻ <audio> gửi:
           GET /uploads/songs/audio/3f2a9c1e.mp3
           Range: bytes=2500000-
       Server chỉ trả về đoạn từ byte 2.500.000 tới cuối file:
           HTTP/1.1 206 Partial Content
           Content-Range: bytes 2500000-4999999/5000000
       → Không cần tải lại cả bài, tua tới đâu phát tới đó.
       (206 Partial Content = "trả về 1 phần nội dung".)

    3. Chống truy cập trái phép ra ngoài thư mục:
       Request kiểu /uploads/../application.properties (dùng ".." để đi ngược lên thư mục cha)
       bị từ chối → kẻ xấu không đọc được file cấu hình chứa mật khẩu.

    4. Trả 404 Not Found nếu file không tồn tại.

    -----------------------------------------------------
    Liên quan tới các file khác
    -----------------------------------------------------
    - FileStorageService: lưu file vào {app.upload-dir}/... và trả về đường dẫn /uploads/...
      để lưu vào database.
    - SecurityConfig: permitAll cho /uploads/**, vì thẻ <audio> và <img> của HTML
      không gửi được header Authorization → nếu bắt đăng nhập thì không phát được nhạc.
    - RateLimitFilter: bỏ qua /uploads/, vì phát và tua 1 bài tạo ra rất nhiều request nhỏ.
    - CorsConfig: expose Content-Range, Accept-Ranges cho trường hợp JavaScript cần đọc.

    Lưu ý (không phải lỗi): vì /uploads/** công khai, ai có đúng đường link
    thì mở được file mà không cần đăng nhập. Tên file là UUID ngẫu nhiên
    (ví dụ 3f2a9c1e-8b7d-4e21-9c3a-1f5b6d7e8a90.mp3) nên gần như không thể đoán ra,
    nhưng nếu link bị chia sẻ ra ngoài thì người khác vẫn nghe được.
    Với project học tập, cách này là chấp nhận được.

    -----------------------------------------------------
    @Configuration và implements WebMvcConfigurer
    -----------------------------------------------------
    - @Configuration: Spring tạo đối tượng của class này khi khởi động.
    - implements WebMvcConfigurer: class này "ký hợp đồng" với Spring MVC.

    WebMvcConfigurer giống 1 phiếu góp ý gửi cho Spring MVC:
    Spring Boot đã tự cấu hình sẵn Spring MVC (Jackson để đổi object sang JSON,
    file tĩnh mặc định, xử lý lỗi...). Muốn thêm hoặc chỉnh 1 phần nhỏ thì implement interface này
    và ghi đè (override) đúng hàm cần chỉnh. Phần còn lại Spring Boot vẫn giữ nguyên.
    Interface này có nhiều hàm (addCorsMappings, addInterceptors...),
    nhưng tất cả đều có sẵn phần thân rỗng, nên chỉ cần override hàm mình cần.

    Lưu ý: không gắn thêm @EnableWebMvc vào class này.
    @EnableWebMvc sẽ tắt toàn bộ cấu hình tự động của Spring Boot cho Spring MVC,
    nhiều thứ đang chạy sẽ bị hỏng theo.
*/
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    /*
        -----------------------------------------------------
        Đọc đường dẫn thư mục upload từ application.properties
        -----------------------------------------------------
        @Value("${app.upload-dir}"): khi tạo đối tượng WebMvcConfig, Spring tìm dòng
        app.upload-dir=... trong application.properties và gán giá trị vào biến uploadDir.

        Cú pháp ${tên_key}: lấy giá trị của key đó.

        Giá trị hiện tại:
        - Máy local : app.upload-dir=uploads                   (thư mục uploads trong project)
        - VPS       : app.upload-dir=/home/myspotify/uploads    (đường dẫn tuyệt đối)

        Nếu quên khai báo key này, ứng dụng không khởi động được và báo lỗi
        "Could not resolve placeholder 'app.upload-dir'" → biết ngay mà sửa.

        - private: chỉ dùng bên trong class này.
        - String: kiểu chuỗi.
        - Không có final: vì Spring gán giá trị sau khi đã tạo đối tượng
          (gọi là tiêm qua field), biến final thì không gán lại được.
    */
    @Value("${app.upload-dir}")
    private String uploadDir;

    /*
        =====================================================
        KHAI BÁO URL /UPLOADS/** TRỎ TỚI THƯ MỤC UPLOAD
        =====================================================

        @Override: hàm này ghi đè hàm addResourceHandlers có sẵn trong WebMvcConfigurer.
        Nếu gõ sai tên hàm, trình biên dịch báo lỗi ngay nhờ @Override.

        Spring MVC tự gọi hàm này 1 lần khi khởi động, và truyền vào registry
        (sổ đăng ký). Mình ghi vào sổ: "URL nào → thư mục nào".

        void: hàm không trả về gì.
    */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        /*
            -----------------------------------------------------
            Bước 1: Đổi đường dẫn thư mục thành dạng URL "file:..."
            -----------------------------------------------------
            Dòng code nối nhiều hàm liên tiếp, đọc từ trái sang phải.
            Ví dụ với máy local Windows, project ở ổ D thư mục myspotify, uploadDir = "uploads":

            1. Paths.get(uploadDir)
               → tạo đối tượng Path đại diện cho đường dẫn "uploads" (đường dẫn tương đối).

            2. .toAbsolutePath()
               → đổi thành đường dẫn tuyệt đối: D:/myspotify/uploads
               (Windows hiển thị bằng dấu gạch ngược, ở đây viết dấu / cho dễ đọc)
               Đường dẫn tương đối được tính từ thư mục đang chạy ứng dụng
               (khi chạy trong IntelliJ là thư mục project).
               Trên VPS uploadDir đã là đường dẫn tuyệt đối nên giữ nguyên.

            3. .toUri()
               → đổi thành dạng URI: file:///D:/myspotify/uploads/
               Trên VPS: file:///home/myspotify/uploads/

            4. .toString()
               → đổi URI thành chuỗi String để dùng ở bước sau.

            Vì sao phải có tiền tố "file:"?
            Spring dùng tiền tố để biết tìm file ở đâu:
            - "file:"      → tìm trên ổ đĩa của máy.
            - "classpath:" → tìm bên trong project (src/main/resources, tức là bên trong JAR).
            File upload nằm trên ổ đĩa nên phải là "file:".
        */
        String location = Paths.get(uploadDir).toAbsolutePath().toUri().toString();

        /*
            -----------------------------------------------------
            Bước 2: Đảm bảo đường dẫn kết thúc bằng dấu "/"
            -----------------------------------------------------
            toUri() chỉ tự thêm "/" ở cuối nếu thư mục đã tồn tại lúc đó.
            Lần chạy đầu tiên, chưa ai upload gì, thư mục có thể chưa được tạo
            → kết quả là "file:///home/myspotify/uploads" (thiếu "/").

            Vì sao thiếu "/" lại hỏng?
            Spring ghép phần đường dẫn của URL vào sau location, giống cách trình duyệt
            ghép đường dẫn tương đối:
            - Có "/"  : file:///home/myspotify/uploads/ + songs/a.mp3
                        → file:///home/myspotify/uploads/songs/a.mp3   (đúng)
            - Thiếu "/": "uploads" bị hiểu là 1 file chứ không phải thư mục,
                        nên Spring ghép vào thư mục cha
                        → file:///home/myspotify/songs/a.mp3           (sai, không tìm thấy → 404)

            - !location.endsWith("/"): "nếu location không kết thúc bằng /". Dấu ! nghĩa là "không".
            - location += "/": viết tắt của location = location + "/", tức nối thêm "/" vào cuối.
        */
        if (!location.endsWith("/")) {
            location += "/";
        }

        /*
            -----------------------------------------------------
            Bước 3: Đăng ký URL → thư mục
            -----------------------------------------------------
            3 hàm được nối liên tiếp bằng dấu chấm (mỗi hàm trả về chính đối tượng cấu hình,
            nên gọi tiếp được hàm sau):

            - addResourceHandler("/uploads/**"): mẫu URL được xử lý.
              "**" nghĩa là khớp mọi thứ phía sau, kể cả nhiều cấp thư mục:
              /uploads/avatars/a.png, /uploads/songs/audio/b.mp3, /uploads/songs/canvas/c.mp4...

            - addResourceLocations(location): thư mục chứa file thật.

            Cách ghép: bỏ phần "/uploads/" ở đầu URL, phần còn lại ghép vào sau location.
                URL  : /uploads/songs/audio/3f2a9c1e.mp3
                Phần còn lại: songs/audio/3f2a9c1e.mp3
                File : /home/myspotify/uploads/songs/audio/3f2a9c1e.mp3

            - setCachePeriod(3600): trình duyệt được giữ file trong bộ nhớ đệm (cache) 3600 giây = 1 giờ.
              Spring gửi kèm header:
                  Cache-Control: max-age=3600
                  Last-Modified: <thời điểm file được sửa lần cuối>
              Trong 1 giờ, mở lại cùng ảnh bìa thì trình duyệt lấy luôn từ cache, không gọi server.
              Hết 1 giờ, trình duyệt hỏi lại "file có thay đổi không?",
              nếu không đổi, server chỉ trả 304 Not Modified (không gửi lại nội dung) → tiết kiệm băng thông.

              Có sợ cache cũ không? Không, vì mỗi lần đổi ảnh bìa hay file nhạc,
              FileStorageService lưu file mới với tên UUID mới → URL mới → trình duyệt tải file mới.
        */
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
    }
}