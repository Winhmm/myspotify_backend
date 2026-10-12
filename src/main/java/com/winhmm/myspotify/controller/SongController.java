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
    ===================================================================
    API dành cho NGƯỜI NGHE (UC20 - UC25).
    ===================================================================

    ---------- 1. AI ĐƯỢC GỌI? ----------

    KHÔNG có @PreAuthorize → không giới hạn role:
        USER, ARTIST, ADMIN đều dùng được (Artist và Admin cũng là người nghe).

    Nhưng VẪN PHẢI ĐĂNG NHẬP:
        Trong SecurityConfig, /api/songs/** không nằm trong danh sách permitAll(),
        nên rơi vào anyRequest().authenticated()
        → không có token / token sai / phiên hết hạn → 401.
        Muốn cho khách chưa đăng nhập xem danh sách bài:
        thêm "/api/songs/**" vào permitAll() trong SecurityConfig
        (nhưng khi đó API /play không có email để chống spam lượt nghe).

    ---------- 2. CHỈ THẤY BÀI "ĐÃ PHÁT HÀNH" ----------

    Mọi API trong file này chỉ trả bài thỏa đủ 3 điều kiện
    (PUBLISHED_CONDITION trong SongRepository):
        1. status = APPROVED     : Admin đã duyệt, Artist không ẩn.
        2. releaseAt <= hiện tại : đã tới giờ phát hành.
        3. Artist chính đang ACTIVE và vẫn có role ARTIST.
    → Bài chờ duyệt, bị từ chối, bị ẩn, chưa tới lịch, hoặc của Artist bị khóa:
      người nghe hoàn toàn không thấy.
    → Bài hẹn lịch 20h tự xuất hiện lúc 20h, không cần chạy tác vụ hẹn giờ nào,
      vì điều kiện so sánh releaseAt với thời gian hiện tại MỖI LẦN truy vấn.

    ---------- 3. DTO RIÊNG CHO NGƯỜI NGHE ----------

    Trả về SongResponse (bản rút gọn), KHÔNG có status, rejectReason, createdAt...
    vì người nghe không cần và không nên biết thông tin quản lý nội bộ.
    (Artist và Admin dùng SongManageResponse, đầy đủ hơn.)

    Nghệ sĩ feat bị khóa → tự ẩn tên khỏi danh sách featuredArtists,
    nhưng bài hát vẫn hiển thị bình thường.

    ---------- 4. NGHE NHẠC (UC23) KHÔNG CẦN API RIÊNG ----------

    Frontend đặt audioUrl vào thẻ audio:
        <audio src="http://<server>/uploads/songs/audio/abc.mp3" controls></audio>
    File được phục vụ trực tiếp bởi WebMvcConfig (/uploads/**):
        - Không cần token (SecurityConfig cho phép /uploads/** công khai).
        - Hỗ trợ tua nhạc: trình duyệt chỉ xin đúng đoạn cần nghe (HTTP 206 Partial Content).
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
        UC20 - Danh sách bài đã phát hành.
        ---------------------------------------------------------------

        VÍ DỤ GỌI:
            GET /api/songs                    → trang đầu, 20 bài
            GET /api/songs?page=1&size=10     → trang thứ hai, 10 bài mỗi trang

        Sắp xếp: mới phát hành lên đầu (releaseAt giảm dần, cài trong SongService).
        Phân trang an toàn: page âm → 0, size ngoài khoảng 1 - 50 → tự chỉnh lại.

        JSON TRẢ VỀ:
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
            - durationSeconds đổi sang "3:35" để hiển thị.
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
        UC21 - Tìm kiếm.
        ---------------------------------------------------------------

        VÍ DỤ GỌI:
            GET /api/songs/search?keyword=may chieu&page=0&size=20

        Tìm keyword (KHÔNG phân biệt hoa thường, chỉ cần khớp 1 phần) trong:
            - Tên bài hát.               VD: "chiều" khớp "Chiều Mưa"
            - Nghệ danh Artist chính.    VD: "may"   khớp "Mây Chiều"
            - Nghệ danh Artist feat.     (chỉ tính người đang hoạt động)

        1 bài khớp nhiều điều kiện (VD có 2 người feat cùng chứa keyword)
        → vẫn chỉ xuất hiện 1 lần (DISTINCT trong câu truy vấn).

        keyword rỗng hoặc không gửi (defaultValue = "")
            → trả danh sách bình thường như UC20, không báo lỗi.

        Định dạng JSON trả về giống hệt UC20.

        THỨ TỰ ĐƯỜNG DẪN:
            "/search" là đường dẫn cố định, còn "/{songId}" bên dưới là khuôn có biến.
            Spring ưu tiên đường dẫn cố định, nên "/search" KHÔNG bị hiểu nhầm
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
        UC22 - Chi tiết 1 bài hát.
        ---------------------------------------------------------------

        VÍ DỤ GỌI:
            GET /api/songs/15

        JSON TRẢ VỀ: 1 SongResponse (cùng cấu trúc với 1 phần tử trong "content" của UC20).

        Bài chưa phát hành / bị ẩn / bị gỡ / Artist bị khóa / không tồn tại
            → đều báo CHUNG: 400 "Song does not exist".
        Cố ý KHÔNG phân biệt từng trường hợp:
            nếu báo "bài đang chờ duyệt", người ngoài sẽ biết bài đó tồn tại
            và đoán được kế hoạch phát hành của Artist (lộ thông tin).
    */
    @GetMapping("/{songId}")
    public ResponseEntity<SongResponse> getSong(@PathVariable Long songId) {
        return ResponseEntity.ok(SongMapper.toSongResponse(songService.getPublishedSong(songId)));
    }

    /*
        ---------------------------------------------------------------
        UC24 - Tăng lượt nghe.
        ---------------------------------------------------------------

        VÍ DỤ GỌI:
            POST /api/songs/15/play      (không cần body)

        Frontend gọi 1 LẦN khi người dùng bắt đầu phát bài, VD:
            audio.addEventListener("play", () => axios.post(`/api/songs/${id}/play`));

        Dùng POST (không phải GET) vì đây là hành động TẠO RA THAY ĐỔI
        (ghi nhận thêm 1 lượt nghe). Quy ước: GET chỉ đọc, không được làm thay đổi dữ liệu.

        CHỐNG TĂNG LƯỢT NGHE ẢO:
            Cùng 1 tài khoản nghe cùng 1 bài chỉ được tính 1 lượt trong 30 giây
            (Redis lệnh SET NX, xem SongService.play).
            authentication.getName() (email) dùng để phân biệt từng người nghe.

        Luôn trả 200 { "message": "Play recorded" }, KỂ CẢ KHI lượt đó bị bỏ qua
        vì nghe lại quá nhanh. Không báo "bạn nghe quá nhanh" để kẻ gian
        không dò được quy luật chống spam.

        CHỐNG RACE CONDITION:
            Lượt nghe tăng bằng 1 câu SQL: UPDATE ... SET play_count = play_count + 1.
            1000 người nghe cùng lúc vẫn đếm đủ 1000, không bị mất lượt nào.

        LỖI CÓ THỂ GẶP:
            400 "Song does not exist" → bài chưa phát hành / không tồn tại
    */
    @PostMapping("/{songId}/play")
    public ResponseEntity<Map<String, String>> play(Authentication authentication,
                                                    @PathVariable Long songId) {
        songService.play(songId, authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Play recorded"));
    }

    /*
        ---------------------------------------------------------------
        UC25 - Trang của 1 Artist: thông tin + bài đã phát hành.
        ---------------------------------------------------------------

        VÍ DỤ GỌI:
            GET /api/songs/artists/5?page=0&size=20

        Gọi Service 2 lần:
        1. getArtist(artistId): lấy thông tin nghệ sĩ.
           Không phải Artist / đã bị khóa / không tồn tại → 400 "Artist does not exist".
        2. getArtistSongs(...): bài đã phát hành mà Artist này là
           Artist CHÍNH hoặc ĐƯỢC FEAT (giống Spotify: trang nghệ sĩ có cả bài hát chung).

        Gộp 2 kết quả vào ArtistSongsResponse bằng constructor (DTO này không dùng Builder).

        JSON TRẢ VỀ:
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

        ĐƯỜNG DẪN:
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