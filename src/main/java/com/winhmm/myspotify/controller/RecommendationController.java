package com.winhmm.myspotify.controller;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    (package, import, annotation là gì: xem giải thích chi tiết trong GenreController.)

    - SongResponse         : DTO bài hát trả cho frontend.
    - RecommendationService: Service gợi ý bài hát.
    - ResponseEntity       : toàn bộ response HTTP (mã trạng thái, header, body).
    - GetMapping, PathVariable, RequestParam, RestController: annotation của Spring MVC.
    - List                 : kiểu danh sách của Java.
*/
import com.winhmm.myspotify.dto.song.SongResponse;
import com.winhmm.myspotify.service.RecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/*
    =====================================================================
    RECOMMENDATIONCONTROLLER - API GỢI Ý BÀI HÁT
    =====================================================================

    Các API trong class:
        GET /api/songs/{songId}/recommendations          → bài đề xuất sau 1 bài (danh sách phát tiếp)
        GET /api/songs/{songId}/recommendations/artist   → bài của cùng nghệ sĩ
        GET /api/recommendations/genres?genreIds=1,24    → bài theo danh sách thể loại (AI Python)

    2 API đầu tính thẳng từ MySQL, API thứ 3 hỏi dịch vụ AI Python
    (lý do: xem phần đầu file RecommendationService).

    Mỗi request đi qua đầy đủ các bước bảo mật (JWT, Redis, rate limit...)
    như mô tả trong comment đầu file AdminSongController.

    =====================================================================
    AI ĐƯỢC GỌI?
    =====================================================================

    Không có @PreAuthorize → mọi role (USER, ARTIST, ADMIN) đều dùng được.
    Nhưng phải đăng nhập: các đường dẫn này không nằm trong permitAll() của SecurityConfig
    → không có token, token sai hoặc phiên hết hạn → 401.

    =====================================================================
    FRONTEND KHÔNG GỌI THẲNG PYTHON
    =====================================================================

    Áp dụng cho API gợi ý theo thể loại (API duy nhất còn hỏi Python):

        React ──GET──► Spring Boot (8080) ──POST──► Python AI (127.0.0.1:8000)

    - Python chỉ nghe ở 127.0.0.1 của VPS, từ internet không gọi tới được.
    - Mọi request vẫn được kiểm tra JWT, rate limit như các API khác.
    - Python chỉ trả về id bài hát. Spring Boot lấy thông tin đầy đủ từ MySQL
      và chỉ giữ lại bài đã phát hành.
    - Frontend dùng GET (chỉ đọc dữ liệu). Việc gọi sang Python bằng POST là chuyện
      bên trong backend, frontend không cần biết.

    =====================================================================
    JSON TRẢ VỀ (CHUNG CHO CẢ 3 API)
    =====================================================================

    1 mảng các bài hát, cùng cấu trúc với 1 phần tử trong "content" của GET /api/songs:
        [
            {
                "id": 7,
                "title": "Phố Đêm",
                "audioUrl": "/uploads/songs/audio/3f2a9c1e.mp3",
                "coverUrl": "/uploads/songs/covers/9b1c4d7f.jpg",
                "canvasUrl": null,
                "durationSeconds": 198,
                "playCount": 530,
                "releaseAt": "2026-10-10T20:00:00",
                "artistId": 5,
                "artistName": "Mây Chiều",
                "featuredArtists": [],
                "genreId": 24,
                "genreName": "Chill"
            },
            ...
        ]
    → Frontend dùng lại được component hiển thị bài hát đã có.

    - Mảng rỗng [] là bình thường (VD hệ thống chưa có bài nào khác).
    - Dịch vụ AI tạm lỗi → API thể loại tự dùng phương án dự phòng (bài nhiều lượt nghe nhất),
      frontend vẫn nhận mảng bài hát như bình thường, không có lỗi.
*/
/*
    -----------------------------------------------------
    Annotation trên class
    -----------------------------------------------------
    - @RestController: class này là controller, giá trị return được đổi thành JSON
      (xem giải thích chi tiết trong GenreController).
    - Không có @RequestMapping chung: 3 API thuộc 2 nhóm đường dẫn khác nhau
      (/api/songs/... và /api/recommendations/...), nên mỗi hàm tự ghi đường dẫn đầy đủ
      (giống LyricsController).
    Biến recommendationService và constructor: tiêm phụ thuộc (xem GenreController).
*/
@RestController
public class RecommendationController {
    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    /*
        ---------------------------------------------------------------
        Bài đề xuất sau 1 bài (danh sách phát tiếp)
        ---------------------------------------------------------------
        Ví dụ gọi:
            GET /api/songs/15/recommendations                          → 5 bài (mặc định)
            GET /api/songs/15/recommendations?limit=10                 → 10 bài
            GET /api/songs/15/recommendations?limit=10&excludeIds=7,22 → 10 bài, không có bài 7 và 22

        Dùng cho:
        - Mục "Bài hát đề xuất" trên trang chi tiết bài hát (limit=5).
        - Danh sách phát tiếp khi nghe hết bài (limit=10).

        Thứ tự kết quả (chi tiết: xem phần đầu file RecommendationService):
            tối đa 3 bài của cùng nghệ sĩ (ưu tiên cùng thể loại)
            → bài cùng thể loại của nghệ sĩ khác
            → bài nhiều lượt nghe nhất của nghệ sĩ khác (khi vẫn còn thiếu).

        Tham số:
        - @PathVariable Long songId: id bài đang nghe, lấy từ đường dẫn (/api/songs/15/... → 15).
          Gửi chữ, VD /api/songs/abc/recommendations → 400 "Invalid parameter: songId".
        - @RequestParam(defaultValue = "5") int limit: số bài muốn lấy.
          Không gửi → 5. Tự giới hạn trong khoảng 1 đến 20.
        - @RequestParam(required = false) List<Long> excludeIds: các bài không muốn nhận lại,
          VD các bài vừa phát xong. Không bắt buộc.
          Spring tự tách "7,22" theo dấu phẩy thành [7, 22] (giống genreIds ở API thể loại).
          Gửi chữ, VD excludeIds=abc → 400 "Invalid parameter: excludeIds".

        Không bao giờ có chính bài đang nghe trong kết quả.

        Lỗi có thể gặp:
            400 "Song does not exist" → bài không tồn tại hoặc chưa phát hành
    */
    @GetMapping("/api/songs/{songId}/recommendations")
    public ResponseEntity<List<SongResponse>> getSimilarSongs(@PathVariable Long songId,
                                                              @RequestParam(defaultValue = "5") int limit,
                                                              @RequestParam(required = false) List<Long> excludeIds) {
        return ResponseEntity.ok(recommendationService.getSimilarSongs(songId, limit, excludeIds));
    }

    /*
        ---------------------------------------------------------------
        Bài của cùng nghệ sĩ
        ---------------------------------------------------------------
        Ví dụ gọi:
            GET /api/songs/15/recommendations/artist?limit=5

        Các bài khác của nghệ sĩ (tính cả bài được feat), bài cùng thể loại xếp trước.
        Nghệ sĩ không có bài nào khác → bài cùng thể loại, nhiều lượt nghe nhất.
        API "Bài đề xuất" ở trên đã gồm bài của cùng nghệ sĩ, nên frontend có thể không cần API này nữa.
        Vẫn giữ lại để không làm hỏng chỗ nào còn gọi tới.
        Tham số songId, limit và lỗi giống API ở trên (không có excludeIds).
    */
    @GetMapping("/api/songs/{songId}/recommendations/artist")
    public ResponseEntity<List<SongResponse>> getArtistSongs(@PathVariable Long songId,
                                                             @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(recommendationService.getArtistSongs(songId, limit));
    }

    /*
        ---------------------------------------------------------------
        Bài theo danh sách thể loại
        ---------------------------------------------------------------
        Ví dụ gọi:
            GET /api/recommendations/genres?genreIds=1,24             → 10 bài (mặc định)
            GET /api/recommendations/genres?genreIds=1&genreIds=24    → cách viết khác, cùng kết quả
            GET /api/recommendations/genres?genreIds=24&limit=5

        Dùng cho trang Genres, hoặc màn hình chọn thể loại yêu thích.
        Danh sách id thể loại: GET /api/genres (GenreController).

        @RequestParam(required = false) List<Long> genreIds:
        - Spring tự tách "1,24" theo dấu phẩy thành danh sách [1, 24].
        - required = false: không gửi cũng không bị Spring chặn,
          để RecommendationService tự kiểm tra và trả message rõ ràng.
        - Gửi chữ, VD genreIds=abc → 400 "Invalid parameter: genreIds".

        Lỗi có thể gặp:
            400 "Genre ids are required"        → không gửi genreIds
            400 "At most 10 genres are allowed" → gửi quá 10 thể loại
    */
    @GetMapping("/api/recommendations/genres")
    public ResponseEntity<List<SongResponse>> getGenreSongs(@RequestParam(required = false) List<Long> genreIds,
                                                            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(recommendationService.getGenreSongs(genreIds, limit));
    }
}