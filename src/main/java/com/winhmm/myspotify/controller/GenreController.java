package com.winhmm.myspotify.controller;

/*
    -----------------------------------------------------
    Bài học số 1: nên đọc file này trước tiên
    -----------------------------------------------------
    GenreController là controller ngắn nhất của project, nên được dùng làm bài học đầu tiên.
    Những khái niệm được giải thích kỹ ở đây (package, import, @RestController, @RequestMapping,
    constructor, ResponseEntity, generic...) xuất hiện lại ở mọi controller khác.
    Các controller khác chỉ nhắc ngắn gọn và trỏ về file này.

    -----------------------------------------------------
    Dòng package
    -----------------------------------------------------
    package com.winhmm.myspotify.controller;
    = "địa chỉ nhà" của class, luôn trùng với thư mục chứa file:
        src/main/java/com/winhmm/myspotify/controller/GenreController.java

    Package này nằm bên trong com.winhmm.myspotify, là package của MyspotifyApplication
    (class có @SpringBootApplication và hàm main). Khi khởi động, Spring quét mọi class
    trong package đó và các package con, thấy @RestController thì tự tạo đối tượng.
    Để class ra ngoài package này, Spring sẽ không thấy và API không chạy (404).

    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    import = mượn class ở chỗ khác về dùng trong file này.
    Không import thì phải viết tên đầy đủ mỗi lần dùng, VD org.springframework.http.ResponseEntity.

    - GenreResponse : DTO (Data Transfer Object), đối tượng chở dữ liệu trả về cho frontend,
                      chỉ có id và name.
    - SongMapper    : class chứa các hàm đổi entity (dữ liệu trong database) sang DTO.
    - SongService   : tầng Service, nơi chứa nghiệp vụ của người nghe.
    - ResponseEntity: đối tượng đại diện cho toàn bộ response HTTP (mã trạng thái, header, body).
    - GetMapping, RequestMapping, RestController: annotation của Spring MVC (giải thích bên dưới).
    - List          : kiểu danh sách của Java.

    Annotation là gì?
    Là "nhãn dán" bắt đầu bằng @, đặt trước class, hàm, biến hoặc tham số.
    Bản thân nhãn không chạy gì cả, nhưng Spring đọc nhãn để biết phải làm gì với chỗ đó.
    VD thấy @GetMapping trên 1 hàm → Spring hiểu "hàm này xử lý request GET".
*/

import com.winhmm.myspotify.dto.song.GenreResponse;
import com.winhmm.myspotify.mapper.SongMapper;
import com.winhmm.myspotify.service.SongService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/*
    =====================================================================
    GENRECONTROLLER - API THỂ LOẠI NHẠC
    =====================================================================

    API trong class:
        GET /api/genres   → danh sách 25 thể loại, xếp theo tên A → Z

    Ai được gọi:
    - Không có @PreAuthorize → mọi role (USER, ARTIST, ADMIN).
    - Phải đăng nhập (/api/genres không nằm trong permitAll() của SecurityConfig).

    Dùng ở 2 chỗ trên frontend:
    - Artist đăng bài: ô chọn thể loại (bắt buộc chọn 1).
    - Người nghe: trang Genres, bấm vào 1 thể loại → GET /api/songs?genreId=<id>.

    Chỉ có API đọc. Thêm thể loại mới: chạy câu INSERT trong MySQL (không có API Admin).

    Vì sao gọi SongService mà không tạo GenreService riêng?
    - Chỉ có 1 chức năng (lấy danh sách), tạo cả 1 Service riêng thì thừa.
    - Thể loại là 1 phần của module Song, SongService là nghiệp vụ của người nghe.
    - Vẫn giữ đúng kiến trúc 3 lớp: Controller → Service → Repository
      (Controller không gọi thẳng Repository).

    JSON trả về:
        [
            { "id": 1,  "name": "Acoustic" },
            { "id": 2,  "name": "Ballad" },
            ...
        ]
*/
/*
    -----------------------------------------------------
    Controller là gì? Kiến trúc 3 tầng của project
    -----------------------------------------------------
    Backend chia làm 3 tầng, mỗi tầng làm 1 việc, giống 1 nhà hàng:

        Frontend (khách)
           │  gửi request HTTP, VD GET /api/genres
           ▼
        Controller (phục vụ bàn) : nhận yêu cầu, kiểm tra dữ liệu gửi lên, trả kết quả
           ▼
        Service (đầu bếp)        : xử lý nghiệp vụ, quyết định đúng sai
           ▼
        Repository (kho)         : đọc ghi database (MySQL, MongoDB)

    Phục vụ bàn không tự nấu ăn, đầu bếp không ra đón khách.
    Tách như vậy thì mỗi phần ngắn gọn, dễ đọc, dễ sửa.

    -----------------------------------------------------
    @RestController
    -----------------------------------------------------
    Đặt trên class, có 2 tác dụng:
    1. Báo cho Spring: "đây là controller, hãy tạo 1 đối tượng của class này khi khởi động".
       Spring tự tạo, mình không bao giờ viết new GenreController().
    2. Giá trị return của mọi hàm trong class được tự đổi thành JSON rồi gửi cho frontend.
       Việc đổi Java object thành JSON do thư viện Jackson làm (Spring Boot tích hợp sẵn).

    @RestController = @Controller + @ResponseBody.
    (@Controller đứng một mình thì hàm phải trả về tên 1 trang HTML, dùng cho web kiểu cũ.)

    -----------------------------------------------------
    @RequestMapping("/api/genres")
    -----------------------------------------------------
    Đường dẫn chung cho mọi API trong class.
    Đường dẫn đầy đủ của 1 API = đường dẫn của class + đường dẫn ghi trên hàm.
    VD: class "/api/genres" + hàm @GetMapping (không ghi gì) → GET /api/genres.

    -----------------------------------------------------
    Request đi tới hàm getGenres() như thế nào?
    -----------------------------------------------------
    1. Frontend gửi: GET http://157.66.100.87:8080/api/genres, kèm header Authorization.
    2. Tomcat (máy chủ web có sẵn bên trong Spring Boot) nhận request.
    3. Request đi qua chuỗi filter bảo mật trong SecurityConfig (CORS, JWT, rate limit...).
    4. DispatcherServlet (bộ điều phối của Spring MVC) tra bảng:
       "GET + /api/genres" → hàm getGenres() của GenreController.
    5. Spring gọi getGenres(), lấy giá trị return, Jackson đổi thành JSON, gửi về frontend.

    -----------------------------------------------------
    Các method HTTP dùng trong project
    -----------------------------------------------------
        Method   Annotation       Ý nghĩa                      Ví dụ
        GET      @GetMapping      lấy dữ liệu                  xem danh sách bài hát
        POST     @PostMapping     tạo mới hoặc 1 hành động     đăng ký, đăng bài, ghi lượt nghe
        PUT      @PutMapping      cập nhật                     sửa tên bài, duyệt bài
        DELETE   @DeleteMapping   xóa                          xóa bài, xóa lời bài hát

    -----------------------------------------------------
    Khi có lỗi thì sao?
    -----------------------------------------------------
    Controller trong project không viết try-catch.
    Service gặp lỗi nghiệp vụ thì ném exception, VD:
        throw new IllegalArgumentException("Song does not exist");
    Exception bay ngược lên qua Controller, Spring chuyển nó cho GlobalExceptionHandler,
    nơi đổi exception thành response lỗi, VD: 400 { "message": "Song does not exist" }.
    Nhờ vậy controller ngắn gọn, và mọi lỗi đều có chung 1 dạng JSON.
*/
@RestController
@RequestMapping("/api/genres")
public class GenreController {
    /*
        -----------------------------------------------------
        Biến songService và constructor (tiêm phụ thuộc)
        -----------------------------------------------------
        private final SongService songService;
        - SongService: kiểu của biến (class Service chứa nghiệp vụ).
        - songService: tên biến (chữ s thường, quy ước đặt tên biến trong Java).
        - private    : chỉ dùng được bên trong class này.
        - final      : chỉ được gán đúng 1 lần (trong constructor), sau đó không đổi được nữa
                       → tránh vô tình gán nhầm sang đối tượng khác.

        public GenreController(SongService songService) { ... } là constructor:
        - Constructor là hàm đặc biệt, trùng tên với class, không có kiểu trả về,
          chạy đúng 1 lần khi đối tượng được tạo ra.
        - Khi khởi động, Spring tạo SongService trước, rồi gọi constructor này
          và truyền đối tượng SongService vào.
        - Kỹ thuật này gọi là tiêm phụ thuộc (Dependency Injection - DI):
          class cần gì thì khai báo trong constructor, Spring tự "tiêm" vào.
        - Class chỉ có 1 constructor nên không cần ghi @Autowired, Spring tự hiểu.

        this.songService = songService:
        - this.songService: biến của class (khai báo ở trên).
        - songService     : tham số vừa được Spring truyền vào.
        → Lưu lại vào biến của class để các hàm khác dùng được.

        Vì sao không tự viết new SongService()?
        SongService lại cần các Repository, Redis, ListeningHistoryService...
        Tự new thì phải tự tạo cả chuỗi đối tượng đó và tự cấu hình kết nối database.
        Spring lo hết, và cả ứng dụng chỉ dùng chung 1 đối tượng SongService (tiết kiệm bộ nhớ).
    */
    private final SongService songService;

    public GenreController(SongService songService) {
        this.songService = songService;
    }

    /*
        =====================================================================
        LẤY DANH SÁCH THỂ LOẠI
        =====================================================================

        @GetMapping (không ghi đường dẫn): dùng luôn đường dẫn của class → GET /api/genres.

        -----------------------------------------------------
        Đọc dòng khai báo hàm
        -----------------------------------------------------
            public ResponseEntity<List<GenreResponse>> getGenres()

        - public: Spring gọi được hàm này từ bên ngoài class.
        - ResponseEntity<List<GenreResponse>>: kiểu trả về, đọc từ ngoài vào trong:
          + ResponseEntity<...>  : toàn bộ response HTTP (mã trạng thái + header + body).
          + List<GenreResponse>  : phần body là 1 danh sách các GenreResponse.
          Dấu < > gọi là generic, cho biết "bên trong chứa kiểu gì".
          List<GenreResponse> là danh sách chỉ chứa GenreResponse,
          lỡ bỏ kiểu khác vào thì IntelliJ báo lỗi ngay lúc viết code.
        - getGenres: tên hàm. Đặt tên gì cũng được,
          vì Spring tìm hàm theo @GetMapping và đường dẫn, không theo tên hàm.
        - (): không nhận tham số nào, vì API này không cần dữ liệu gì từ frontend.

        -----------------------------------------------------
        Đọc thân hàm
        -----------------------------------------------------
        List<GenreResponse> result = ...;
            Khai báo biến result kiểu List<GenreResponse>, gán bằng kết quả của vế phải.

        Từng bước của vế phải:
        1. songService.getGenres() → List<Genre> (entity), đã xếp theo tên A → Z.
        2. .stream()   → biến danh sách thành "dòng chảy" để xử lý lần lượt từng phần tử.
        3. .map(SongMapper::toGenreResponse) → đổi từng Genre thành GenreResponse (DTO, chỉ có id và name).
           SongMapper::toGenreResponse là cách viết gọn của: genre -> SongMapper.toGenreResponse(genre)
        4. .toList()   → gom kết quả lại thành List (danh sách không sửa được).
           Ngắn gọn hơn .collect(Collectors.toList()) ở AdminController, có từ Java 16.
        5. ResponseEntity.ok(result) → HTTP 200, Jackson đổi danh sách thành mảng JSON.

        return ResponseEntity.ok(result);
            ok(...) tạo response có mã 200 OK và body là result.
            Một số cách tạo response khác (project dùng chủ yếu ok):
                ResponseEntity.status(HttpStatus.CREATED).body(...)  → 201 Created
                ResponseEntity.badRequest().body(...)                → 400 Bad Request

        Vì sao không trả thẳng List<Genre>?
        Giữ đúng quy ước của project: Controller luôn trả DTO, không trả entity,
        để sau này entity Genre có thêm cột hay quan hệ mới cũng không làm đổi JSON của frontend.
    */
    @GetMapping
    public ResponseEntity<List<GenreResponse>> getGenres() {
        List<GenreResponse> result = songService.getGenres()
                .stream()
                .map(SongMapper::toGenreResponse)
                .toList();

        return ResponseEntity.ok(result);
    }
}