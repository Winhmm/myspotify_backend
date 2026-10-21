package com.winhmm.myspotify.controller;

import com.winhmm.myspotify.dto.lyrics.LyricsRequest;
import com.winhmm.myspotify.dto.lyrics.LyricsResponse;
import com.winhmm.myspotify.service.LyricsService;
import com.winhmm.myspotify.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/*
    =====================================================================
    LYRICSCONTROLLER - API LỜI BÀI HÁT
    =====================================================================

    Các API trong class:
        GET    /api/songs/{songId}/lyrics          → người nghe xem lời (mọi tài khoản đã đăng nhập)
        GET    /api/artist/songs/{songId}/lyrics   → Artist xem lời bài của mình (để sửa)
        PUT    /api/artist/songs/{songId}/lyrics   → Artist thêm mới hoặc thay toàn bộ lời
        DELETE /api/artist/songs/{songId}/lyrics   → Artist xóa lời

    =====================================================================
    VÌ SAO TẠO CONTROLLER RIÊNG?
    =====================================================================

    - Gom mọi API về lời vào 1 file, dễ tìm.
    - Không phải sửa SongController và ArtistSongController đã chạy ổn.

    Vì vậy class này không có @RequestMapping chung:
    mỗi method tự ghi đường dẫn đầy đủ, vì có 2 nhóm đường dẫn khác nhau
    (/api/songs/... cho người nghe và /api/artist/... cho Artist).

    =====================================================================
    PHÂN QUYỀN
    =====================================================================

    - API người nghe: không có @PreAuthorize. Chỉ cần đăng nhập
      (SecurityConfig bắt buộc đăng nhập với mọi đường dẫn ngoài danh sách công khai).
    - API Artist: @PreAuthorize("hasRole('ARTIST')") đặt trên từng method
      (không đặt trên class như ArtistSongController, vì class này còn API cho người nghe).
      USER hoặc ADMIN gọi → 403.
    - Artist chỉ đụng được bài của mình: LyricsService tìm bài bằng findByIdAndArtistId.
*/
@RestController
public class LyricsController {
    private final LyricsService lyricsService;
    private final UserService userService;

    public LyricsController(LyricsService lyricsService, UserService userService) {
        this.lyricsService = lyricsService;
        this.userService = userService;
    }

    /*
        Lấy id người đang đăng nhập từ email trong token
        (giống hàm me trong ArtistSongController).
    */
    private Long me(Authentication authentication) {
        return userService.getByEmail(authentication.getName()).getId();
    }

    /*
        ---------------------------------------------------------------
        Người nghe xem lời
        ---------------------------------------------------------------
        Ví dụ gọi:
            GET /api/songs/9/lyrics

        Bài đã phát hành, có lời → 200 với "lines" là danh sách câu.
        Bài đã phát hành, chưa có lời → 200 với "lines": [].
        Bài chưa phát hành, bị ẩn, không tồn tại → 400 "Song does not exist".

        Frontend tô sáng câu đang hát:
            Câu đang hát = câu cuối cùng có timeMs <= audio.currentTime * 1000.
    */
    @GetMapping("/api/songs/{songId}/lyrics")
    public ResponseEntity<LyricsResponse> getLyrics(@PathVariable Long songId) {
        return ResponseEntity.ok(lyricsService.getPublishedLyrics(songId));
    }

    /*
        ---------------------------------------------------------------
        Artist xem lời bài của mình
        ---------------------------------------------------------------
        Ví dụ gọi:
            GET /api/artist/songs/9/lyrics

        Khác API người nghe: xem được cả bài chưa phát hành (chờ duyệt, bị từ chối, đang ẩn).
        Frontend dùng khi mở hộp thoại lời: điền "content" vào ô nhập để Artist sửa tiếp.
    */
    @PreAuthorize("hasRole('ARTIST')")
    @GetMapping("/api/artist/songs/{songId}/lyrics")
    public ResponseEntity<LyricsResponse> getMyLyrics(Authentication authentication,
                                                      @PathVariable Long songId) {
        return ResponseEntity.ok(lyricsService.getMyLyrics(me(authentication), songId));
    }

    /*
        ---------------------------------------------------------------
        Artist thêm mới hoặc thay toàn bộ lời
        ---------------------------------------------------------------
        Ví dụ gọi (Postman: Body → raw → JSON):
            PUT /api/artist/songs/9/lyrics
            {
                "content": "[00:27.80] Ngày xưa lúc thơ bé, ta thường nhìn mưa hát\n[00:32.90] Và khi cơn mưa như nặng hạt"
            }

        Dùng PUT vì gọi nhiều lần vẫn ra cùng kết quả: lần nào cũng thay toàn bộ lời cũ.
        Không cần API riêng cho "thêm" và "sửa".

        @Valid kiểm tra LyricsRequest: không rỗng, tối đa 10.000 ký tự.
        LyricsService kiểm tra phần còn lại (thời gian, thứ tự, số dòng...).

        JSON trả về: LyricsResponse của lời vừa lưu (đã chuẩn hóa).
    */
    @PreAuthorize("hasRole('ARTIST')")
    @PutMapping("/api/artist/songs/{songId}/lyrics")
    public ResponseEntity<LyricsResponse> saveLyrics(Authentication authentication,
                                                     @PathVariable Long songId,
                                                     @Valid @RequestBody LyricsRequest request) {
        LyricsResponse response = lyricsService.saveLyrics(me(authentication), songId, request.getContent());
        return ResponseEntity.ok(response);
    }

    /*
        ---------------------------------------------------------------
        Artist xóa lời
        ---------------------------------------------------------------
        Ví dụ gọi:
            DELETE /api/artist/songs/9/lyrics      (không cần body)

        JSON trả về: { "message": "Lyrics have been deleted" }
        Bài chưa có lời → 400 "Song has no lyrics".
    */
    @PreAuthorize("hasRole('ARTIST')")
    @DeleteMapping("/api/artist/songs/{songId}/lyrics")
    public ResponseEntity<Map<String, String>> deleteLyrics(Authentication authentication,
                                                            @PathVariable Long songId) {
        lyricsService.deleteLyrics(me(authentication), songId);
        return ResponseEntity.ok(Map.of("message", "Lyrics have been deleted"));
    }
}