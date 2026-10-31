package com.winhmm.myspotify.controller;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    (package, import, annotation là gì: xem giải thích chi tiết trong GenreController.)

    - LyricsRequest : DTO nhận dữ liệu Artist gửi lên khi lưu lời (chỉ có trường content).
    - LyricsResponse: DTO trả lời bài hát về cho frontend.
    - LyricsService : Service xử lý nghiệp vụ lời bài hát.
    - UserService   : dùng để đổi email trong token thành id người dùng.
    - Valid         : bật kiểm tra dữ liệu trong DTO (giải thích ở hàm saveLyrics).
    - ResponseEntity: toàn bộ response HTTP (mã trạng thái, header, body).
    - PreAuthorize  : kiểm tra quyền (role) trước khi chạy hàm.
    - Authentication: thông tin người đang đăng nhập.
    - org.springframework.web.bind.annotation.*:
      dấu * nghĩa là "mượn tất cả class trong package đó", gồm RestController, GetMapping,
      PutMapping, DeleteMapping, PathVariable, RequestBody... Viết gọn hơn import từng cái.
    - Map: kiểu dữ liệu dạng cặp "khóa → giá trị", dùng để trả { "message": "..." }.
*/

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
/*
    -----------------------------------------------------
    Annotation trên class
    -----------------------------------------------------
    - @RestController: class này là controller, giá trị return được đổi thành JSON
      (xem giải thích chi tiết trong GenreController).
    - Không có @RequestMapping: mỗi hàm tự ghi đường dẫn đầy đủ (xem lý do ở trên).

    -----------------------------------------------------
    2 dependency và constructor
    -----------------------------------------------------
    Spring tự tạo LyricsService, UserService rồi truyền vào constructor
    (tiêm phụ thuộc, xem GenreController). private final: chỉ gán 1 lần, không đổi được.
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
        -----------------------------------------------------
        Hàm phụ me: lấy id người đang đăng nhập
        -----------------------------------------------------
        Lấy id người đang đăng nhập từ email trong token
        (giống hàm me trong ArtistSongController).

        - private: hàm nội bộ, chỉ dùng trong class này, frontend không gọi được
          (không có @GetMapping hay @PutMapping nên cũng không phải API).
        - Long   : kiểu trả về (id người dùng).
        - Authentication authentication: nhận thông tin đăng nhập từ hàm gọi nó.

        Đọc thân hàm từ trong ra ngoài:
            authentication.getName()  → email trong token, VD "maychieu@gmail.com"
            userService.getByEmail()  → tìm User trong MySQL
            .getId()                  → lấy id của User đó

        Vì sao tách thành hàm riêng?
        3 API của Artist đều cần id, viết 1 lần dùng nhiều lần, đọc code cũng ngắn hơn:
            lyricsService.getMyLyrics(me(authentication), songId)
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

        -----------------------------------------------------
        @GetMapping("/api/songs/{songId}/lyrics") và @PathVariable
        -----------------------------------------------------
        - @GetMapping(...): hàm này xử lý request GET tới đường dẫn ghi trong ngoặc.
          Class không có @RequestMapping nên đây chính là đường dẫn đầy đủ.
        - {songId}: chỗ trống trong đường dẫn, nhận giá trị bất kỳ.
          GET /api/songs/9/lyrics → {songId} = "9".

        @PathVariable Long songId:
        - Lấy giá trị của chỗ trống {songId} gán vào tham số songId.
        - Tên tham số phải trùng tên trong dấu { } (đều là songId).
          Muốn đặt tên khác thì ghi rõ: @PathVariable("songId") Long id.
        - Spring tự đổi chuỗi "9" sang số Long 9.
          Gửi /api/songs/abc/lyrics → không đổi được → 400 "Invalid parameter: songId".

        Không có @PreAuthorize → mọi tài khoản đã đăng nhập đều xem được.
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

        -----------------------------------------------------
        @PreAuthorize("hasRole('ARTIST')") trên hàm
        -----------------------------------------------------
        - Ngay trước khi hàm chạy, Spring kiểm tra biểu thức trong ngoặc.
        - hasRole('ARTIST'): người gọi phải có quyền ROLE_ARTIST
          (Spring tự thêm tiền tố ROLE_, xem CustomUserDetailsService).
        - Không đủ quyền → hàm không chạy → 403
          "You do not have permission to access this resource".
        - Hoạt động được nhờ @EnableMethodSecurity trong SecurityConfig.

        Hàm có 2 tham số, Spring tự điền cả 2:
        - Authentication authentication: người đang đăng nhập.
        - @PathVariable Long songId     : số trong đường dẫn.
        Thứ tự tham số không quan trọng, Spring nhận ra theo kiểu và annotation.
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

        -----------------------------------------------------
        @Valid @RequestBody LyricsRequest request
        -----------------------------------------------------
        @RequestBody: lấy body JSON của request và đổi thành đối tượng Java.
            Body gửi lên: { "content": "[00:27.80] Ngày xưa..." }
            Jackson làm:  LyricsRequest request = new LyricsRequest();
                          request.setContent("[00:27.80] Ngày xưa...");
            Tên trường trong JSON phải trùng tên trường trong DTO (content).
            JSON sai cú pháp (thiếu dấu ngoặc, dấu phẩy...) → 400 "Invalid request data".

        @Valid: kiểm tra các annotation đặt trên trường của DTO, ngay trước khi hàm chạy:
            @NotBlank(message = "Lyrics must not be empty")
            @Size(max = 10000, message = "Lyrics must not exceed 10000 characters")
            Vi phạm → hàm không chạy → GlobalExceptionHandler trả 400 kèm đúng message đó.
            Thiếu @Valid thì các annotation trong DTO bị bỏ qua, dữ liệu sai vẫn lọt vào Service.

        Sau khi qua kiểm tra, lấy nội dung bằng getter: request.getContent().

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

        -----------------------------------------------------
        @DeleteMapping và Map.of
        -----------------------------------------------------
        - @DeleteMapping(...): hàm xử lý request DELETE (method HTTP dùng để xóa).
        - Kiểu trả về ResponseEntity<Map<String, String>>:
          body là 1 Map có khóa kiểu String và giá trị kiểu String.
        - Map.of("message", "Lyrics have been deleted"): tạo nhanh 1 Map có 1 cặp
          khóa "message" → giá trị "Lyrics have been deleted".
          Jackson đổi Map thành JSON: { "message": "Lyrics have been deleted" }.
          Map.of tạo Map không sửa được, đủ dùng cho 1 thông báo đơn giản, không cần tạo DTO riêng.
        - lyricsService.deleteLyrics(...) không trả gì (void), chỉ cần chạy xong không lỗi
          là xóa thành công. Lỗi thì Service ném exception, dòng return bên dưới không chạy tới.
    */
    @PreAuthorize("hasRole('ARTIST')")
    @DeleteMapping("/api/artist/songs/{songId}/lyrics")
    public ResponseEntity<Map<String, String>> deleteLyrics(Authentication authentication,
                                                            @PathVariable Long songId) {
        lyricsService.deleteLyrics(me(authentication), songId);
        return ResponseEntity.ok(Map.of("message", "Lyrics have been deleted"));
    }
}