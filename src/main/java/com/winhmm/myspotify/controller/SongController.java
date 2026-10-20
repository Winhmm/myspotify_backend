package com.winhmm.myspotify.controller;

import com.winhmm.myspotify.dto.song.ArtistSongsResponse;
import com.winhmm.myspotify.dto.song.PageResponse;
import com.winhmm.myspotify.dto.song.SongResponse;
import com.winhmm.myspotify.entity.User;
import com.winhmm.myspotify.mapper.SongMapper;
import com.winhmm.myspotify.service.SongService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/*
    =====================================================================
    SONGCONTROLLER - API NGƯỜI NGHE (UC20 - UC25)
    =====================================================================

    Các API trong class:
        GET  /api/songs                          → danh sách bài đã phát hành
        GET  /api/songs/search?keyword=          → tìm kiếm bài
        GET  /api/songs/{songId}                 → chi tiết 1 bài
        POST /api/songs/{songId}/play            → ghi nhận 1 lượt nghe
        GET  /api/songs/artists/{artistId}       → trang của 1 Artist

    Mỗi request đi qua đầy đủ các bước bảo mật (JWT, Redis, rate limit...)
    như mô tả trong comment đầu file AdminSongController.

    =====================================================================
    AI ĐƯỢC GỌI?
    =====================================================================

    Không có @PreAuthorize → không giới hạn role:
    USER, ARTIST, ADMIN đều dùng được (Artist và Admin cũng là người nghe).

    Nhưng vẫn phải đăng nhập:
    trong SecurityConfig, /api/songs/** không nằm trong danh sách permitAll(),
    nên rơi vào anyRequest().authenticated()
    → không có token, token sai hoặc phiên hết hạn → 401.

    Muốn cho khách chưa đăng nhập xem danh sách bài: thêm "/api/songs/**" vào permitAll().
    Nhưng khi đó phải sửa thêm API /play, vì khách không có tài khoản thì
    tham số authentication là null → authentication.getName() gây lỗi 500,
    và cũng không còn email để chống nghe ảo hay ghi lịch sử nghe.

    =====================================================================
    CHỈ THẤY BÀI "ĐÃ PHÁT HÀNH"
    =====================================================================

    Mọi API trong file này chỉ trả bài thỏa đủ 3 điều kiện
    (PUBLISHED_CONDITION trong SongRepository):
        1. status = APPROVED     : Admin đã duyệt, Artist không ẩn.
        2. releaseAt <= hiện tại : đã tới giờ phát hành.
        3. Artist chính đang ACTIVE và vẫn có role ARTIST.
    → Bài chờ duyệt, bị từ chối, bị ẩn, chưa tới lịch, hoặc của Artist bị khóa:
      người nghe hoàn toàn không thấy.
    → Bài hẹn lịch 20h tự xuất hiện lúc 20h, không cần chạy tác vụ hẹn giờ nào,
      vì điều kiện so sánh releaseAt với thời gian hiện tại ở mỗi lần truy vấn.
    → "Thời gian hiện tại" là giờ của máy chủ (LocalDateTime.now()),
      nên server phải chạy múi giờ Asia/Ho_Chi_Minh thì lịch mới đúng giờ Việt Nam.

    =====================================================================
    DTO RIÊNG CHO NGƯỜI NGHE
    =====================================================================

    Trả về SongResponse (bản rút gọn), không có status, rejectReason, createdAt...
    vì người nghe không cần và không nên biết thông tin quản lý nội bộ.
    (Artist và Admin dùng SongManageResponse, đầy đủ hơn.)

    Nghệ sĩ feat bị khóa → tự ẩn tên khỏi danh sách featuredArtists,
    nhưng bài hát vẫn hiển thị bình thường.

    =====================================================================
    NGHE NHẠC (UC23) KHÔNG CẦN API RIÊNG
    =====================================================================

    Frontend đặt audioUrl vào thẻ audio:
        <audio src="http://<server>/uploads/songs/audio/abc.mp3" controls></audio>

    File được phục vụ trực tiếp bởi WebMvcConfig (/uploads/**):
    - Không cần token (SecurityConfig cho phép /uploads/** công khai).
    - Hỗ trợ tua nhạc: trình duyệt chỉ xin đúng đoạn cần nghe (HTTP 206 Partial Content).
    - Không bị tính vào rate limit (RateLimitFilter bỏ qua /uploads/**),
      vì phát và tua 1 bài gửi rất nhiều request nhỏ.

    =====================================================================
    LỊCH SỬ NGHE
    =====================================================================

    Mỗi lượt nghe được tính qua POST /api/songs/{songId}/play cũng được lưu vào MongoDB.
    Xem lịch sử: GET /api/users/me/history (ListeningHistoryController).
*/
@RestController
@RequestMapping("/api/songs")
public class SongController {
    private final SongService songService;

    public SongController(SongService songService) {
        this.songService = songService;
    }

    /*
        ---------------------------------------------------------------
        UC20 - Danh sách bài đã phát hành
        ---------------------------------------------------------------
        Ví dụ gọi:
            GET /api/songs                    → trang đầu, 20 bài
            GET /api/songs?page=1&size=10     → trang thứ hai, 10 bài mỗi trang

        Sắp xếp: mới phát hành lên đầu (releaseAt giảm dần, cài trong SongService).
        Phân trang an toàn: page âm → 0, size ngoài khoảng 1 - 50 → tự chỉnh lại.

        JSON trả về:
            {
                "content": [
                    {
                        "id": 15,
                        "title": "Chiều Mưa",
                        "audioUrl": "/uploads/songs/audio/3f2a9c1e.mp3",
                        "coverUrl": "/uploads/songs/covers/9b1c4d7f.jpg",
                        "durationSeconds": 215,
                        "playCount": 1024,
                        "releaseAt": "2026-10-12T16:30:00",
                        "artistId": 5,
                        "artistName": "Mây Chiều",
                        "featuredArtists": [ { "id": 12, "artistName": "Sóng Nhỏ" } ]
                    }
                ],
                "page": 0,
                "size": 20,
                "totalElements": 135,
                "totalPages": 7
            }

        Frontend dùng:
            - totalPages để vẽ nút chuyển trang (1, 2, 3, ..., 7).
            - audioUrl ghép với địa chỉ server để phát nhạc.
            - coverUrl = null → hiển thị ảnh bìa mặc định.
            - durationSeconds đổi sang "3:35" để hiển thị.
            - featuredArtists rỗng [] → không hiện chữ "ft.".
    */
    @GetMapping
    public ResponseEntity<PageResponse<SongResponse>> getSongs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(SongMapper.toPageResponse(
                songService.getPublishedSongs(page, size), SongMapper::toSongResponse));
    }

    /*
        ---------------------------------------------------------------
        UC21 - Tìm kiếm
        ---------------------------------------------------------------
        Ví dụ gọi:
            GET /api/songs/search?keyword=may chieu&page=0&size=20

        Tìm keyword (chỉ cần khớp 1 phần) trong:
            - Tên bài hát.               VD: "chiều" khớp "Chiều Mưa"
            - Nghệ danh Artist chính.    VD: "mây"   khớp "Mây Chiều"
            - Nghệ danh Artist feat      (chỉ tính người đang hoạt động)

        Khớp không phân biệt:
            - Hoa thường: câu truy vấn dùng LOWER() cho cả 2 vế.
            - Có dấu hay không dấu ("may" khớp "Mây Chiều"): nhờ collation mặc định của MySQL 8
              (utf8mb4_0900_ai_ci, "ai" = accent insensitive). Nếu bảng dùng collation khác,
              tìm không dấu có thể không ra kết quả.

        1 bài khớp nhiều điều kiện (VD có 2 người feat cùng chứa keyword)
        → vẫn chỉ xuất hiện 1 lần (DISTINCT trong câu truy vấn).

        keyword rỗng hoặc không gửi (defaultValue = "")
        → trả danh sách bình thường như UC20, không báo lỗi.
        (Người dùng xóa hết ô tìm kiếm thì thấy lại danh sách đầy đủ.)

        JSON trả về: giống hệt UC20.

        Frontend: keyword có dấu cách hoặc tiếng Việt có dấu thì trình duyệt hay axios
        tự mã hóa (VD "may chieu" → "may%20chieu"), backend tự giải mã, không cần làm gì thêm.

        Thứ tự đường dẫn:
            "/search" là đường dẫn cố định, còn "/{songId}" bên dưới là khuôn có biến.
            Spring ưu tiên đường dẫn cố định, nên "/search" không bị hiểu nhầm
            thành songId = "search".
    */
    @GetMapping("/search")
    public ResponseEntity<PageResponse<SongResponse>> searchSongs(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(SongMapper.toPageResponse(
                songService.searchSongs(keyword, page, size), SongMapper::toSongResponse));
    }

    /*
        ---------------------------------------------------------------
        UC22 - Chi tiết 1 bài hát
        ---------------------------------------------------------------
        Ví dụ gọi:
            GET /api/songs/15

        JSON trả về: 1 SongResponse (cùng cấu trúc với 1 phần tử trong "content" của UC20).

        Bài chưa phát hành, bị ẩn, bị gỡ, Artist bị khóa, hoặc không tồn tại
        → đều báo chung: 400 "Song does not exist".
        Cố ý không phân biệt từng trường hợp:
        nếu báo "bài đang chờ duyệt", người ngoài sẽ biết bài đó tồn tại
        và đoán được kế hoạch phát hành của Artist (lộ thông tin).

        Lỗi có thể gặp:
            400 "Song does not exist"
            400 "Invalid parameter: songId" → id không phải số, VD /api/songs/abc
    */
    @GetMapping("/{songId}")
    public ResponseEntity<SongResponse> getSong(@PathVariable Long songId) {
        return ResponseEntity.ok(SongMapper.toSongResponse(songService.getPublishedSong(songId)));
    }

    /*
        ---------------------------------------------------------------
        UC24 - Ghi nhận 1 lượt nghe (tăng lượt nghe và lưu lịch sử nghe)
        ---------------------------------------------------------------
        Ví dụ gọi:
            POST /api/songs/15/play      (không cần body)

        Frontend gọi 1 lần khi người dùng bắt đầu phát bài, VD:
            audio.addEventListener("play", () => axios.post(`/api/songs/${id}/play`));
        Lưu ý: sự kiện "play" của thẻ audio cũng chạy khi bấm tạm dừng rồi phát tiếp.
        Không sao, backend đã chống đếm trùng (xem bên dưới).

        Dùng POST (không phải GET) vì đây là hành động tạo ra thay đổi.
        Quy ước: GET chỉ đọc, không được làm thay đổi dữ liệu.

        Backend làm 3 việc (trong SongService.play):

        1. Kiểm tra bài đã phát hành. Chưa phát hành → 400 "Song does not exist".

        2. Chống tăng lượt nghe ảo (Redis):
           Cùng 1 tài khoản nghe cùng 1 bài chỉ được tính 1 lượt trong 30 giây.
           authentication.getName() (email) dùng để phân biệt từng người nghe.

        3. Nếu lượt nghe được tính:
           - MySQL  : play_count + 1 bằng 1 câu UPDATE duy nhất,
                      1000 người nghe cùng lúc vẫn đếm đủ 1000 (chống race condition).
           - MongoDB: ghi 1 dòng lịch sử nghe (xem ListeningHistoryController).
                      MongoDB lỗi → chỉ ghi log, không làm hỏng việc phát nhạc.

        Luôn trả 200 { "message": "Play recorded" }, kể cả khi lượt đó bị bỏ qua
        vì nghe lại quá nhanh. Không báo "bạn nghe quá nhanh" để kẻ gian
        không dò được quy luật chống spam.
    */
    @PostMapping("/{songId}/play")
    public ResponseEntity<Map<String, String>> play(Authentication authentication,
                                                    @PathVariable Long songId) {
        songService.play(songId, authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Play recorded"));
    }

    /*
        ---------------------------------------------------------------
        UC25 - Trang của 1 Artist: thông tin và bài đã phát hành
        ---------------------------------------------------------------
        Ví dụ gọi:
            GET /api/songs/artists/5?page=0&size=20

        Gọi Service 2 lần:
        1. getArtist(artistId): lấy thông tin nghệ sĩ.
           Không phải Artist, đã bị khóa, hoặc không tồn tại → 400 "Artist does not exist".
        2. getArtistSongs(...): bài đã phát hành mà Artist này là
           Artist chính hoặc được feat (giống Spotify: trang nghệ sĩ có cả bài hát chung).

        Gộp 2 kết quả vào ArtistSongsResponse bằng constructor (DTO này không dùng Builder).

        JSON trả về:
            {
                "artistId": 5,
                "artistName": "Mây Chiều",
                "bio": "Ballad nhẹ nhàng cho buổi chiều.",
                "avatarUrl": "/uploads/avatars/7c2e.jpg",
                "songs": {
                    "content": [ ... ],
                    "page": 0,
                    "size": 20,
                    "totalElements": 12,
                    "totalPages": 1
                }
            }

        Frontend:
            - avatarUrl hoặc bio có thể null → hiển thị ảnh mặc định, ẩn phần giới thiệu.
            - Chuyển trang bài hát thì gọi lại cùng API với page khác.
              Phần thông tin nghệ sĩ được trả lại giống hệt, có thể bỏ qua.

        Đường dẫn:
            Đặt dưới /api/songs/artists/... (không phải /api/artists/...)
            để không nhầm với /api/artist/... (API quản lý của Artist trong ArtistSongController).
    */
    @GetMapping("/artists/{artistId}")
    public ResponseEntity<ArtistSongsResponse> getArtistPage(
            @PathVariable Long artistId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User artist = songService.getArtist(artistId);

        PageResponse<SongResponse> songs = SongMapper.toPageResponse(
                songService.getArtistSongs(artistId, page, size), SongMapper::toSongResponse);

        return ResponseEntity.ok(new ArtistSongsResponse(
                artist.getId(), artist.getArtistName(), artist.getBio(), artist.getAvatarUrl(), songs));
    }
}

