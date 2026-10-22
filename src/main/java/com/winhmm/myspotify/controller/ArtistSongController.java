package com.winhmm.myspotify.controller;

import com.winhmm.myspotify.dto.song.*;
import com.winhmm.myspotify.entity.Song;
import com.winhmm.myspotify.mapper.SongMapper;
import com.winhmm.myspotify.service.ArtistSongService;
import com.winhmm.myspotify.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/*
    =====================================================================
    ARTISTSONGCONTROLLER - API ARTIST QUẢN LÝ BÀI HÁT CỦA MÌNH (UC26 - UC33, UC37)
    =====================================================================

    Các API trong class:
        POST   /api/artist/songs                    → đăng bài mới
        GET    /api/artist/songs                    → bài hát của tôi
        GET    /api/artist/search-artists?keyword=  → tìm nghệ sĩ để feat
        PUT    /api/artist/songs/{songId}           → sửa tên bài và danh sách feat
        PUT    /api/artist/songs/{songId}/cover     → đổi ảnh bìa
        PUT    /api/artist/songs/{songId}/canvas    → đăng hoặc thay video lặp (Canvas)
        DELETE /api/artist/songs/{songId}/canvas    → gỡ video lặp (Canvas)
        PUT    /api/artist/songs/{songId}/release   → đổi lịch phát hành
        PUT    /api/artist/songs/{songId}/hide      → ẩn bài
        PUT    /api/artist/songs/{songId}/show      → hiện lại bài đã ẩn
        DELETE /api/artist/songs/{songId}           → xóa bài

    Mỗi request đi qua đầy đủ các bước bảo mật (JWT, Redis, rate limit...)
    như mô tả trong comment đầu file AdminSongController.

    =====================================================================
    VÒNG ĐỜI CỦA MỘT BÀI HÁT
    =====================================================================

        Artist đăng bài ──► PENDING ──Admin duyệt──► APPROVED ──tới giờ──► người nghe thấy
                              │                         │    ▲
                        Admin từ chối            Artist ẩn  │ Artist hiện lại
                              ▼                         ▼    │
                          REJECTED ◄──Admin gỡ bài─── HIDDEN
                              │
                      Artist sửa bài ──► PENDING (tự gửi duyệt lại)

        Artist xóa bài (bất kỳ lúc nào) ──► DELETED (xóa luôn file nhạc, ảnh bìa và video Canvas)

    =====================================================================
    CÁC ANNOTATION Ở ĐẦU CLASS
    =====================================================================

    @PreAuthorize("hasRole('ARTIST')")
        - Áp dụng cho mọi API bên dưới: chỉ tài khoản role ARTIST mới gọi được.
        - USER hoặc ADMIN gọi vào → 403 "You do not have permission to access this resource".
        - Muốn trở thành ARTIST: User gửi yêu cầu kèm nghệ danh (UC14), Admin duyệt
          (approveArtistRequest). Admin không gán thẳng role ARTIST được,
          nhờ vậy Artist nào cũng có nghệ danh.

    @RequestMapping("/api/artist")
        - Mọi API bắt đầu bằng /api/artist, VD /api/artist/songs.
        - Lưu ý 2 đường dẫn dễ nhầm:
            /api/artist/...        : Artist quản lý bài của mình (file này, cần role ARTIST).
            /api/songs/artists/... : người nghe xem trang của 1 Artist (SongController).

    @Validated
        - Bật kiểm tra annotation đặt trực tiếp lên tham số method,
          VD @RequestParam @NotBlank String keyword.
        - Khác @Valid: @Valid đặt trước 1 DTO (object) để kiểm tra các trường bên trong.
        - Hiện chưa có tham số nào dùng kiểu đó, nên annotation này chưa có tác dụng.
          Giữ lại để sau này thêm ràng buộc cho @RequestParam thì dùng được ngay.

    =====================================================================
    BẢO MẬT: ARTIST CHỈ ĐỤNG ĐƯỢC BÀI CỦA MÌNH
    =====================================================================

    - Controller lấy id Artist từ token (hàm me bên dưới),
      không bao giờ nhận artistId từ frontend gửi lên.
      (Nếu nhận, kẻ gian chỉ cần sửa artistId trong request là giả làm người khác.)

    - Service tìm bài bằng findByIdAndArtistId(songId, artistId):
      bài của người khác → không tìm thấy → báo "Song does not exist".
      → Artist A không thể sửa hay xóa bài của Artist B, dù đoán được songId.
      → Báo "không tồn tại" thay vì "không có quyền" để không lộ việc bài đó có tồn tại.

    - Mọi API thay đổi dữ liệu, Service còn kiểm tra lại từ database:
      tài khoản phải là ARTIST đang ACTIVE, nếu không → 400 "Only active artists can manage songs".
      Đây là lớp bảo vệ thứ hai bên cạnh @PreAuthorize và bước kiểm tra tài khoản ở filter.

    =====================================================================
    KHI NÀO DÙNG JSON, KHI NÀO DÙNG FORM-DATA?
    =====================================================================

    - API có gửi file (đăng bài, đổi ảnh bìa, đăng video Canvas)
      → multipart/form-data. JSON chỉ chứa chữ, không gửi được file nhị phân (mp3, jpg).
    - API chỉ có chữ (sửa tên, đổi lịch)
      → JSON (application/json), dùng @RequestBody.
    - API không cần dữ liệu gửi lên (ẩn, hiện, xóa, gỡ Canvas) → không có body.
*/
@PreAuthorize("hasRole('ARTIST')")
@RestController
@RequestMapping("/api/artist")
@Validated
public class ArtistSongController {
    private final ArtistSongService artistSongService;
    private final UserService userService;

    public ArtistSongController(ArtistSongService artistSongService, UserService userService) {
        this.artistSongService = artistSongService;
        this.userService = userService;
    }

    /*
        Lấy id của Artist đang đăng nhập.

        Authentication authentication:
        - Spring tự truyền vào bất kỳ method controller nào khai báo tham số kiểu này.
        - Đối tượng này do JwtAuthenticationFilter tạo sau khi kiểm tra token hợp lệ,
          lưu trong SecurityContextHolder suốt request.
        - authentication.getName() trả về email
          (CustomUserDetailsService dùng .withUsername(user.getEmail())).

        Email → tìm User trong database → lấy id.
        Vì sao không ghi thẳng id vào token?
        - Token hiện chỉ chứa email (sub) và mã phiên (jti).
        - Tra id từ email vẫn nhanh vì cột email có UNIQUE → MySQL tự tạo index.

        private: hàm nội bộ, chỉ dùng trong class này.
    */
    private Long me(Authentication authentication) {
        return userService.getByEmail(authentication.getName()).getId();
    }

    /*
    ---------------------------------------------------------------
    UC26 - Tải lên bài hát
    ---------------------------------------------------------------

    Ví dụ gọi (Postman: Body → form-data):
        POST /api/artist/songs
        Content-Type: multipart/form-data

        KEY                TYPE    VALUE
        title              Text    Chiều Mưa
        genreId            Text    24                      (bắt buộc, id từ GET /api/genres)
        audio              File    chieu-mua.mp3
        cover              File    bia.jpg                 (không bắt buộc)
        featuredArtistIds  Text    12                      (không bắt buộc)
        featuredArtistIds  Text    13                      (lặp key để feat nhiều người)
        releaseAt          Text    2026-12-24T20:00:00     (không bắt buộc)

    consumes = MediaType.MULTIPART_FORM_DATA_VALUE
        → API này chỉ nhận multipart/form-data.
        → Gửi JSON vào → 415 Unsupported Media Type.

    @ModelAttribute UploadSongRequest request
        → Spring đọc từng trường của form-data rồi gọi setter tương ứng:
          setTitle("Chiều Mưa"), setGenreId(24), setAudio(file), setCover(file),
          setFeaturedArtistIds([12, 13]), setReleaseAt(...).
        → Giống @RequestBody nhưng dành cho form-data.
        → releaseAt đổi từ chuỗi sang LocalDateTime nhờ @DateTimeFormat trong DTO.

    @Valid → kiểm tra trong DTO:
        @NotBlank và @Size(max = 150) cho title,
        @NotNull cho genreId và audio, @Size(max = 5) cho featuredArtistIds.

    Service làm gì:
        1. Kiểm tra người đăng là Artist đang hoạt động.
        2. Lịch phát hành (nếu có) phải ở tương lai.
        3. Kiểm tra danh sách feat (tối đa 5, không tự feat mình, phải là Artist ACTIVE).
        4. Kiểm tra thể loại có tồn tại (sai id → 400 "Genre does not exist").
        5. Lưu file nhạc → đọc thời lượng (file giả, VD .txt đổi đuôi .mp3 → báo lỗi).
        6. Lưu ảnh bìa (kiểm tra chữ ký file, chặn PDF đổi đuôi .png).
        7. Lưu bài với trạng thái PENDING.
        Từ bước 5, bước nào lỗi → xóa các file vừa lưu, không để lại file rác trên ổ đĩa.

    Thể loại chỉ chọn được lúc đăng bài, sau đó không đổi được (API sửa bài không nhận genreId).

    JSON trả về: SongManageResponse với "status": "PENDING".
*/
    @PostMapping(value = "/songs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SongManageResponse> uploadSong(Authentication authentication,
                                                         @Valid @ModelAttribute UploadSongRequest request) {
        Song song = artistSongService.uploadSong(
                me(authentication),
                request.getTitle(),
                request.getGenreId(),
                request.getAudio(),
                request.getCover(),
                request.getFeaturedArtistIds(),
                request.getReleaseAt());

        return ResponseEntity.ok(SongMapper.toManageResponse(song));
    }

    /*
        ---------------------------------------------------------------
        UC28 - Bài hát của tôi
        ---------------------------------------------------------------
        Ví dụ gọi:
            GET /api/artist/songs

        Trả về mọi bài của Artist ở mọi trạng thái (trừ DELETED), mới tải lên trước,
        không phân trang (1 Artist thường chỉ có vài chục bài).

        .stream().map(SongMapper::toManageResponse).toList()
            - stream(): biến List<Song> thành "dòng chảy" để xử lý lần lượt từng phần tử.
            - map(...): biến mỗi Song thành 1 SongManageResponse.
            - toList(): gom kết quả thành List<SongManageResponse> (danh sách không sửa được).

            Tương đương vòng for quen thuộc:
                List<SongManageResponse> result = new ArrayList<>();
                for (Song s : songs) {
                    result.add(SongMapper.toManageResponse(s));
                }

        JSON trả về là 1 mảng (không có content, page như các API phân trang):
            [
                { "id": 15, "title": "Chiều Mưa", "status": "APPROVED", "released": true, ... },
                { "id": 14, "title": "Phố Đêm",   "status": "REJECTED",
                  "rejectReason": "Ảnh bìa không phù hợp", ... }
            ]

        Frontend dựa vào status và released để hiển thị nhãn:
            PENDING                    → "Chờ duyệt"
            APPROVED + released=false  → "Đã lên lịch"
            APPROVED + released=true   → "Đã phát hành"
            REJECTED                   → "Bị từ chối" (kèm rejectReason)
            HIDDEN                     → "Đã ẩn"
    */
    @GetMapping("/songs")
    public ResponseEntity<List<SongManageResponse>> getMySongs(Authentication authentication) {
        List<SongManageResponse> result = artistSongService.getMySongs(me(authentication))
                .stream()
                .map(SongMapper::toManageResponse)
                .toList();
        return ResponseEntity.ok(result);
    }

    /*
        ---------------------------------------------------------------
        UC37 - Tìm nghệ sĩ để feat
        ---------------------------------------------------------------
        Ví dụ gọi:
            GET /api/artist/search-artists?keyword=may

        Dùng cho ô tìm kiếm khi Artist chọn người feat lúc đăng hoặc sửa bài:
        gõ "may" → hiện tối đa 10 Artist có nghệ danh chứa "may".

        Khớp không phân biệt:
        - Hoa thường: nhờ IgnoreCase trong tên method của UserRepository.
        - Có dấu hay không dấu ("may" khớp "Mây Chiều"): nhờ collation mặc định của MySQL 8
          (utf8mb4_0900_ai_ci, "ai" = accent insensitive). Nếu bảng dùng collation khác,
          tìm không dấu có thể không ra kết quả.

        - Tự loại chính mình khỏi kết quả (không ai tự feat mình).
        - Chỉ trả Artist đang ACTIVE.
        - keyword rỗng hoặc toàn dấu cách → trả mảng rỗng [].
        - Không gửi tham số keyword → 400 (Spring báo thiếu tham số bắt buộc,
          vì @RequestParam mặc định required = true).

        JSON trả về chỉ có id và nghệ danh, không có email hay thông tin cá nhân:
            [ { "id": 5, "artistName": "Mây Chiều" }, { "id": 19, "artistName": "Mây Xanh" } ]

        Frontend lấy "id" của người được chọn, gửi vào featuredArtistIds khi đăng hoặc sửa bài.
    */
    @GetMapping("/search-artists")
    public ResponseEntity<List<ArtistSummary>> searchArtists(Authentication authentication,
                                                             @RequestParam("keyword") String keyword) {
        List<ArtistSummary> result = artistSongService.searchArtists(me(authentication), keyword)
                .stream()
                .map(SongMapper::toArtistSummary)
                .toList();

        return ResponseEntity.ok(result);
    }

    /*
        ---------------------------------------------------------------
        UC29 - Sửa tên bài hát và danh sách feat
        ---------------------------------------------------------------
        Ví dụ gọi:
            PUT /api/artist/songs/15
            Content-Type: application/json
            Body: { "title": "Chiều Mưa (Acoustic)", "featuredArtistIds": [12, 13] }

        Dùng @RequestBody (JSON) vì API này không có file.

        featuredArtistIds là danh sách mới hoàn toàn, thay thế danh sách cũ:
            Đang feat [12, 13], gửi [12]   → còn feat 12.
            Gửi [] hoặc bỏ trống trường này → không còn feat với ai.

        Sửa được bài ở mọi trạng thái (trừ đã xóa), kể cả bài đã phát hành (VD sửa lỗi chính tả).

        Bài đang REJECTED mà Artist sửa → tự chuyển về PENDING và xóa lý do từ chối cũ,
        để Admin duyệt lại.
    */
    @PutMapping("/songs/{songId}")
    public ResponseEntity<SongManageResponse> updateSong(Authentication authentication,
                                                         @PathVariable Long songId,
                                                         @Valid @RequestBody UpdateSongRequest request) {
        Song song = artistSongService.updateSong(
                me(authentication), songId, request.getTitle(), request.getFeaturedArtistIds());

        return ResponseEntity.ok(SongMapper.toManageResponse(song));
    }

    /*
        ---------------------------------------------------------------
        UC30 - Đổi ảnh bìa
        ---------------------------------------------------------------
        Ví dụ gọi (Postman: Body → form-data):
            PUT /api/artist/songs/15/cover
            KEY: cover   TYPE: File   VALUE: bia-moi.jpg

        @RequestParam("cover") MultipartFile cover
            → API chỉ có 1 trường nên lấy thẳng file, không cần tạo DTO.
            → MultipartFile: đối tượng Spring tạo cho mỗi file tải lên,
              có tên file gốc, kích thước, loại file, và nội dung để đọc.
            → Thiếu key "cover" → 400 "File is required"
              (MissingServletRequestPartException trong GlobalExceptionHandler).

        Thứ tự an toàn trong Service:
            1. Lưu ảnh mới.
            2. Lưu bài với đường dẫn ảnh mới.
            3. Lưu bài thành công → mới xóa ảnh cũ.
            Bước 2 lỗi → xóa ảnh mới vừa lưu, ảnh cũ còn nguyên, bài vẫn hiển thị bình thường.

        Bài đang REJECTED mà đổi ảnh bìa → tự gửi duyệt lại (thường bị từ chối vì ảnh bìa).
    */
    @PutMapping(value = "/songs/{songId}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SongManageResponse> updateCover(Authentication authentication,
                                                          @PathVariable Long songId,
                                                          @RequestParam("cover") MultipartFile cover) {
        Song song = artistSongService.updateCover(me(authentication), songId, cover);
        return ResponseEntity.ok(SongMapper.toManageResponse(song));
    }

    /*
        ---------------------------------------------------------------
        Đăng hoặc thay video lặp (Canvas)
        ---------------------------------------------------------------
        Ví dụ gọi (Postman: Body → form-data):
            PUT /api/artist/songs/15/canvas
            KEY: canvas   TYPE: File   VALUE: canvas-8s.mp4

        Quy định video:
            - Định dạng MP4.
            - Dài từ 7 đến 10 giây (làm tròn tới 0.1 giây).
            - Tối đa 50MB.
            - Khuyến nghị: video dọc tỉ lệ 9:16 (VD 720 x 1280), không cần tiếng
              (frontend luôn phát ở chế độ tắt tiếng).

        @RequestParam("canvas") MultipartFile canvas
            → API chỉ có 1 trường nên lấy thẳng file, không cần tạo DTO (giống đổi ảnh bìa).
            → Thiếu key "canvas" → 400 "File is required".

        Frontend phát video:
            <video src="http://<server>/uploads/songs/canvas/abc.mp4" autoplay muted loop playsinline></video>
            - autoplay   : tự phát khi trang hiện ra.
            - loop       : hết video tự phát lại từ đầu.
            - muted      : bắt buộc, vì trình duyệt chặn tự phát video có tiếng.
            - playsinline: trên iPhone, video chạy ngay trong trang, không bật toàn màn hình.
            canvasUrl = null → hiện ảnh bìa như cũ.

        Lỗi có thể gặp:
            400 "File is required"                → thiếu key "canvas"
            400 "Only MP4 videos are allowed"     → gửi ảnh, file .mov...
            400 "Invalid or corrupted video file" → file giả mạo hoặc hỏng
            400 "Video must not exceed 50MB"
            400 "Canvas video must be between 7 and 10 seconds (current: 12.0s)"
            400 "Song does not exist"             → bài không phải của mình, hoặc đã xóa

        JSON trả về: SongManageResponse, có "canvasUrl" mới.
    */
    @PutMapping(value = "/songs/{songId}/canvas", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SongManageResponse> updateCanvas(Authentication authentication,
                                                           @PathVariable Long songId,
                                                           @RequestParam("canvas") MultipartFile canvas) {
        Song song = artistSongService.updateCanvas(me(authentication), songId, canvas);
        return ResponseEntity.ok(SongMapper.toManageResponse(song));
    }

    /*
        ---------------------------------------------------------------
        Gỡ video lặp (Canvas)
        ---------------------------------------------------------------
        Ví dụ gọi:
            DELETE /api/artist/songs/15/canvas      (không cần body)

        File video bị xóa khỏi ổ đĩa, "canvasUrl" trở về null
        → frontend quay lại hiện ảnh bìa.

        Lỗi có thể gặp:
            400 "Song has no canvas video" → bài chưa có Canvas

        JSON trả về: SongManageResponse với "canvasUrl": null.
    */
    @DeleteMapping("/songs/{songId}/canvas")
    public ResponseEntity<SongManageResponse> removeCanvas(Authentication authentication,
                                                           @PathVariable Long songId) {
        Song song = artistSongService.removeCanvas(me(authentication), songId);
        return ResponseEntity.ok(SongMapper.toManageResponse(song));
    }

    /*
        ---------------------------------------------------------------
        UC31 - Đổi lịch phát hành
        ---------------------------------------------------------------
        Ví dụ gọi:
            PUT /api/artist/songs/15/release
            Body: { "releaseAt": "2026-12-24T20:00:00" }   → lên lịch 20h ngày 24/12
            Body: { "releaseAt": null }                    → phát hành ngay

        Định dạng thời gian: "năm-tháng-ngàyTgiờ:phút:giây", giờ Việt Nam, không kèm múi giờ.
        Server phải chạy múi giờ Asia/Ho_Chi_Minh thì "giờ Việt Nam" mới đúng
        (backend so lịch với LocalDateTime.now(), là giờ của máy chủ).
        Gửi sai định dạng (VD "24/12/2026") → Jackson không đọc được → 400 "Invalid request data".

        Không có @Valid vì UpdateReleaseRequest không có annotation kiểm tra nào.
        Việc kiểm tra nằm trong Service, vì cần so với thời gian hiện tại và trạng thái bài:
            - Thời gian phải ở tương lai.
            - Bài đã phát hành (kể cả đang ẩn) → không đổi lịch được nữa.
              (Chặn kiểu lách luật: ẩn bài đã phát hành rồi đổi lịch sang năm sau.)
            - releaseAt = null với bài đã duyệt → gán bằng bây giờ (phát hành ngay).
    */
    @PutMapping("/songs/{songId}/release")
    public ResponseEntity<SongManageResponse> updateRelease(Authentication authentication,
                                                            @PathVariable Long songId,
                                                            @RequestBody UpdateReleaseRequest request) {
        Song song = artistSongService.updateReleaseSchedule(
                me(authentication), songId, request.getReleaseAt());

        return ResponseEntity.ok(SongMapper.toManageResponse(song));
    }

    /*
        ---------------------------------------------------------------
        UC32 - Ẩn bài (APPROVED → HIDDEN)
        ---------------------------------------------------------------
        Ví dụ gọi:
            PUT /api/artist/songs/15/hide      (không cần body)

        Bài ẩn: người nghe không thấy nữa (không có trong danh sách, tìm kiếm,
        trang Artist, "Nghe gần đây"), nhưng giữ nguyên lượt nghe, file nhạc,
        ảnh bìa, lịch phát hành.
        Khác với xóa: ẩn có thể hiện lại, xóa thì mất file vĩnh viễn.
    */
    @PutMapping("/songs/{songId}/hide")
    public ResponseEntity<SongManageResponse> hideSong(Authentication authentication,
                                                       @PathVariable Long songId) {
        return ResponseEntity.ok(SongMapper.toManageResponse(
                artistSongService.hideSong(me(authentication), songId)));
    }

    /*
        ---------------------------------------------------------------
        UC32 - Hiện lại bài đã ẩn (HIDDEN → APPROVED)
        ---------------------------------------------------------------
        Ví dụ gọi:
            PUT /api/artist/songs/15/show      (không cần body)

        Không cần Admin duyệt lại, vì bài đã được duyệt trước khi bị ẩn.
        Lịch phát hành giữ nguyên: bài ẩn khi chưa tới lịch → hiện lại vẫn chờ đúng lịch đó.
    */
    @PutMapping("/songs/{songId}/show")
    public ResponseEntity<SongManageResponse> showSong(Authentication authentication,
                                                       @PathVariable Long songId) {
        return ResponseEntity.ok(SongMapper.toManageResponse(
                artistSongService.showSong(me(authentication), songId)));
    }

    /*
        ---------------------------------------------------------------
        UC33 - Xóa bài
        ---------------------------------------------------------------
        Ví dụ gọi:
            DELETE /api/artist/songs/15

        Xóa mềm (soft delete):
            - Bản ghi vẫn còn trong database, chỉ đổi status = DELETED.
            - File nhạc, ảnh bìa và video Canvas bị xóa hẳn khỏi ổ đĩa (không chiếm dung lượng).

        Vì sao không xóa hẳn bản ghi?
            - Giữ lịch sử (bài từng tồn tại, từng có bao nhiêu lượt nghe).
            - Tránh lỗi khóa ngoại nếu sau này có bảng MySQL khác trỏ tới bài (VD playlist).
            - Lịch sử nghe (MongoDB) vẫn còn lưu songId của bài này. Khi hiển thị "Nghe gần đây",
              bài đã xóa không còn "đã phát hành" nên tự bị bỏ qua, người dùng không thấy.

        Xóa được ở mọi trạng thái (PENDING, APPROVED, REJECTED, HIDDEN).
        Không khôi phục được vì file đã mất.

        Trả về Map.of("message", "...") → Jackson chuyển thành JSON:
            { "message": "Song has been deleted" }
        Map.of tạo Map không sửa được, đủ dùng cho 1 thông báo đơn giản.
        Không trả SongManageResponse vì bài đã xóa, frontend không cần hiển thị nữa.
    */
    @DeleteMapping("/songs/{songId}")
    public ResponseEntity<Map<String, String>> deleteSong(Authentication authentication,
                                                          @PathVariable Long songId) {
        artistSongService.deleteSong(me(authentication), songId);
        return ResponseEntity.ok(Map.of("message", "Song has been deleted"));
    }
}