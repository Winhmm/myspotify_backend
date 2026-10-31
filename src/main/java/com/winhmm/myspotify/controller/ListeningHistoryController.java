package com.winhmm.myspotify.controller;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    (package, import, annotation là gì: xem giải thích chi tiết trong GenreController.)

    - ListeningHistoryResponse: DTO của 1 dòng lịch sử, gồm bài hát (song) và thời điểm nghe (playedAt).
    - ListeningHistoryService : Service đọc và ghi lịch sử nghe trong MongoDB.
    - UserService             : Service về tài khoản, ở đây dùng để đổi email thành id.
    - ResponseEntity          : toàn bộ response HTTP (mã trạng thái, header, body).
    - Authentication          : thông tin người đang đăng nhập (của Spring Security).
    - GetMapping, RequestMapping, RestController: annotation của Spring MVC.
    - List                    : kiểu danh sách của Java.
*/

import com.winhmm.myspotify.dto.song.ListeningHistoryResponse;
import com.winhmm.myspotify.service.ListeningHistoryService;
import com.winhmm.myspotify.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/*
    =====================================================================
    LISTENINGHISTORYCONTROLLER - API "NGHE GẦN ĐÂY"
    =====================================================================

    API trong class:
        GET /api/users/me/history   → 10 bài khác nhau được nghe gần nhất

    Mỗi request đi qua đầy đủ các bước bảo mật (JWT, Redis, rate limit...)
    như mô tả trong comment đầu file AdminSongController.

    =====================================================================
    AI ĐƯỢC GỌI?
    =====================================================================

    Không có @PreAuthorize → mọi role (USER, ARTIST, ADMIN) đều dùng được.
    Nhưng phải đăng nhập: /api/users/** không nằm trong permitAll() của SecurityConfig
    → không có token, token sai hoặc phiên hết hạn → 401.

    "me" trong đường dẫn: luôn là lịch sử của chính người gọi, lấy từ token.
    URL không chứa userId → không ai xem được lịch sử người khác bằng cách đổi số trên URL.

    =====================================================================
    DỮ LIỆU ĐẾN TỪ ĐÂU?
    =====================================================================

    Lịch sử được ghi tự động mỗi khi frontend gọi POST /api/songs/{songId}/play
    và lượt nghe đó được tính (không nghe lại cùng bài trong 30 giây).
    Frontend không cần gọi API nào khác để lưu lịch sử.

    MongoDB lưu từng lượt nghe. API này lọc lại thành từng bài
    (xem ListeningHistoryService.getRecentSongs).

    =====================================================================
    VÍ DỤ GỌI VÀ JSON TRẢ VỀ
    =====================================================================

    Ví dụ gọi:
        GET /api/users/me/history      (không có tham số)

    JSON trả về là 1 mảng, tối đa 10 phần tử, mới nghe nhất ở đầu:
    [
        {
            "song": {
                "id": 15,
                "title": "Chiều Mưa",
                "audioUrl": "/uploads/songs/audio/3f2a9c1e.mp3",
                "coverUrl": "/uploads/songs/covers/9b1c4d7f.jpg",
                "canvasUrl": null,
                "durationSeconds": 215,
                "playCount": 1024,
                "releaseAt": "2026-10-01T20:00:00",
                "artistId": 5,
                "artistName": "Mây Chiều",
                "featuredArtists": [ { "id": 12, "artistName": "Sóng Nhỏ" } ],
                "genreId": 24,
                "genreName": "Chill"
            },
            "playedAt": "2026-10-14T15:05:10"
        },
        {
            "song": { "id": 7, "title": "Phố Đêm", ... },
            "playedAt": "2026-10-14T14:50:42"
        }
    ]

    "song" có cấu trúc giống hệt 1 phần tử trong GET /api/songs
    → frontend dùng lại được component hiển thị bài hát đã có.

    =====================================================================
    NHỮNG ĐIỀU FRONTEND CẦN BIẾT
    =====================================================================

    1. Mỗi bài chỉ xuất hiện 1 lần.
       Nghe "Chiều Mưa" 3 lần → chỉ 1 dòng, playedAt là lần nghe gần nhất.
       Nghe lại 1 bài cũ → bài đó nhảy lên đầu danh sách.

    2. Tối đa 10 bài. Nghe chưa tới 10 bài khác nhau → trả ít hơn.

    3. Bài không còn phát hành tự bị bỏ qua.
       Bài bị ẩn, bị gỡ, bị xóa, hoặc Artist bị khóa → không xuất hiện.
       Bài được hiện lại hoặc Artist được mở khóa → xuất hiện lại.

    4. Lịch sử tự xóa sau 90 ngày (TTL index trong MongoDB).

    5. Thời gian playedAt.
       Dạng "năm-tháng-ngàyTgiờ:phút:giây", là giờ của máy chủ, không kèm múi giờ.
       Server chạy múi giờ Asia/Ho_Chi_Minh thì đây là giờ Việt Nam.
       Hiển thị kiểu "15 phút trước": tính khoảng cách giữa playedAt và giờ hiện tại.

    6. Chưa nghe bài nào → [] → frontend hiển thị "Bạn chưa nghe bài nào".

    7. Sau khi người dùng phát 1 bài, muốn danh sách cập nhật thì gọi lại API này.

    =====================================================================
    LỖI CÓ THỂ GẶP
    =====================================================================

        401 → chưa đăng nhập hoặc phiên hết hạn
        429 → gọi quá nhiều lần trong 1 phút
        500 → MongoDB lỗi khi đọc lịch sử (mất kết nối Atlas...)

    Khác với lúc ghi: MongoDB lỗi khi ghi lịch sử (trong API /play) chỉ ghi log,
    không báo lỗi cho người dùng, vì ghi lịch sử là việc phụ.
    Còn API này có nhiệm vụ chính là đọc lịch sử, đọc không được thì phải báo lỗi.
*/
/*
    -----------------------------------------------------
    Annotation trên class
    -----------------------------------------------------
    - @RestController: class này là controller, giá trị return được đổi thành JSON
      (xem giải thích chi tiết trong GenreController).
    - @RequestMapping("/api/users/me/history"): ghi luôn đường dẫn đầy đủ ở class,
      vì class chỉ có 1 API. Hàm bên dưới chỉ cần @GetMapping, không ghi thêm gì.
*/
@RestController
@RequestMapping("/api/users/me/history")
public class ListeningHistoryController {
    /*
        -----------------------------------------------------
        2 dependency và constructor
        -----------------------------------------------------
        Class cần 2 Service, nên khai báo 2 biến private final
        và constructor nhận 2 tham số. Khi khởi động, Spring tự tạo 2 Service
        rồi truyền vào (tiêm phụ thuộc, xem GenreController).

        - listeningHistoryService: đọc lịch sử nghe từ MongoDB.
        - userService            : đổi email (trong token) thành id người dùng.

        Thứ tự tham số trong constructor không quan trọng,
        Spring tìm đối tượng theo kiểu dữ liệu, không theo vị trí.
    */
    private final ListeningHistoryService listeningHistoryService;
    private final UserService userService;

    public ListeningHistoryController(ListeningHistoryService listeningHistoryService,
                                      UserService userService) {
        this.listeningHistoryService = listeningHistoryService;
        this.userService = userService;
    }

    /*
        =====================================================================
        LẤY 10 BÀI NGHE GẦN ĐÂY CỦA CHÍNH MÌNH
        =====================================================================

        @GetMapping (không ghi đường dẫn): dùng luôn đường dẫn của class,
        tức là GET /api/users/me/history.

        Authentication authentication:
        Spring tự truyền vào thông tin người đang đăng nhập
        (do JwtAuthenticationFilter đặt vào SecurityContext khi kiểm tra token).
        authentication.getName() trả về email.
        Tham số này không cần annotation nào: Spring thấy kiểu Authentication là tự hiểu.

        Kiểu trả về ResponseEntity<List<ListeningHistoryResponse>>:
        response HTTP có body là danh sách các ListeningHistoryResponse
        (cách đọc generic < >: xem GenreController).

        -----------------------------------------------------
        Dòng Long userId = userService.getByEmail(authentication.getName()).getId();
        -----------------------------------------------------
        Đọc từ trong ra ngoài:
        1. authentication.getName()         → "lan@gmail.com"
        2. userService.getByEmail("lan@...") → đối tượng User tìm được trong MySQL
           (không có → Service ném lỗi → 400 "User does not exist").
        3. .getId()                          → id của User đó, VD 7.
        4. Gán vào biến userId.

        Long (chữ L hoa) và long (chữ l thường) khác nhau thế nào?
        - long: kiểu số nguyên nguyên thủy, luôn có giá trị, không thể null.
        - Long: kiểu đối tượng bọc ngoài long, có thể null.
        Id trong entity dùng Long, vì đối tượng mới tạo (chưa lưu database) có id = null.

        Từng bước:
        1. userService.getByEmail(email) → tìm User trong MySQL → .getId() lấy id.
           Cần id vì MongoDB lưu lịch sử theo userId, không theo email
           (email có thể đổi, id thì không bao giờ đổi).
        2. listeningHistoryService.getRecentSongs(userId) → danh sách tối đa 10 bài.
        3. ResponseEntity.ok(...) → HTTP 200, Jackson đổi danh sách thành mảng JSON.
    */
    @GetMapping
    public ResponseEntity<List<ListeningHistoryResponse>> getMyHistory(Authentication authentication) {
        Long userId = userService.getByEmail(authentication.getName()).getId();
        return ResponseEntity.ok(listeningHistoryService.getRecentSongs(userId));
    }
}