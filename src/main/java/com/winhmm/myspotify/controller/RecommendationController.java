package com.winhmm.myspotify.controller;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    (package, import, annotation là gì: xem giải thích chi tiết trong GenreController.)

    - SongResponse         : DTO bài hát trả cho frontend.
    - RecommendationService: Service gợi ý bài hát bằng AI.
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
    RECOMMENDATIONCONTROLLER - API GỢI Ý BÀI HÁT BẰNG AI
    =====================================================================

    Các API trong class:
        GET /api/songs/{songId}/recommendations          → bài hát tương tự 1 bài
        GET /api/songs/{songId}/recommendations/artist   → bài liên quan nghệ sĩ của 1 bài
        GET /api/recommendations/genres?genreIds=1,24    → bài theo danh sách thể loại

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

    - Mảng rỗng [] là bình thường (VD hệ thống chưa có bài nào khác cùng thể loại).
    - Dịch vụ AI tạm lỗi → backend tự dùng phương án dự phòng (bài nhiều lượt nghe nhất),
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
        Bài hát tương tự 1 bài
        ---------------------------------------------------------------
        Ví dụ gọi:
            GET /api/songs/15/recommendations             → 5 bài (mặc định)
            GET /api/songs/15/recommendations?limit=10    → 10 bài

        Dùng cho mục "Bài hát tương tự" dưới trình phát nhạc.

        Tham số:
        - @PathVariable Long songId: id bài đang nghe, lấy từ đường dẫn (/api/songs/15/... → 15).
          Gửi chữ, VD /api/songs/abc/recommendations → 400 "Invalid parameter: songId".
        - @RequestParam(defaultValue = "5") int limit: số bài muốn lấy.
          Không gửi → 5. Tự giới hạn trong khoảng 1 đến 20.

        Không bao giờ có chính bài đang nghe trong kết quả.

        Lỗi có thể gặp:
            400 "Song does not exist" → bài không tồn tại hoặc chưa phát hành
    */
    @GetMapping("/api/songs/{songId}/recommendations")
    public ResponseEntity<List<SongResponse>> getSimilarSongs(@PathVariable Long songId,
                                                              @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(recommendationService.getSimilarSongs(songId, limit));
    }

    /*
        ---------------------------------------------------------------
        Bài liên quan tới nghệ sĩ của 1 bài
        ---------------------------------------------------------------
        Ví dụ gọi:
            GET /api/songs/15/recommendations/artist?limit=5

        Dùng cho mục "Cùng nghệ sĩ" trên trang chi tiết bài hát.
        Tham số và lỗi giống API bài hát tương tự ở trên.
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