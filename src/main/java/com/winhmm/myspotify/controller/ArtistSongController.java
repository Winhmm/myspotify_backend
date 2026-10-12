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
    ===================================================================
    API dành cho ARTIST quản lý bài hát của mình (UC26 - UC33, UC37).
    ===================================================================

    ---------- 1. VÒNG ĐỜI 1 BÀI HÁT ----------

        Artist đăng bài ──► PENDING ──Admin duyệt──► APPROVED ──tới giờ──► người nghe thấy
                              │                         │    ▲
                        Admin từ chối            Artist ẩn  │ Artist hiện lại
                              ▼                         ▼    │
                          REJECTED ◄──Admin gỡ bài─── HIDDEN
                              │
                      Artist sửa bài ──► PENDING (tự gửi duyệt lại)

        Artist xóa bài (bất kỳ lúc nào) ──► DELETED (xóa luôn file nhạc và ảnh bìa)

    ---------- 2. CÁC ANNOTATION Ở ĐẦU CLASS ----------

    @PreAuthorize("hasRole('ARTIST')")
        → Áp dụng cho MỌI API bên dưới: chỉ tài khoản role ARTIST mới gọi được.
        → USER / ADMIN gọi vào → 403.
        → Muốn trở thành ARTIST: User gửi yêu cầu kèm nghệ danh (UC14),
          Admin duyệt (approveArtistRequest). Admin KHÔNG gán thẳng role ARTIST được,
          nhờ vậy Artist nào cũng có nghệ danh.

    @RequestMapping("/api/artist")
        → Mọi API bắt đầu bằng /api/artist, VD: /api/artist/songs.
        → Lưu ý tên dễ nhầm:
            /api/artist/...       : Artist QUẢN LÝ bài của mình (file này, cần role ARTIST).
            /api/songs/artists/...: người nghe XEM trang của 1 Artist (SongController).

    @Validated
        → Bật kiểm tra annotation đặt TRỰC TIẾP lên tham số method,
          VD: @RequestParam @NotBlank String keyword.
        → Khác @Valid: @Valid dùng cho DTO (object), @Validated cho tham số đơn lẻ.
        → Hiện tại chưa có tham số nào dùng kiểu đó, nên annotation này chưa có tác dụng.
          Giữ lại để sau này thêm ràng buộc cho @RequestParam thì dùng được ngay.

    ---------- 3. BẢO MẬT: ARTIST CHỈ ĐỤNG ĐƯỢC BÀI CỦA MÌNH ----------

        - Controller lấy id Artist từ TOKEN (hàm me bên dưới),
          KHÔNG BAO GIỜ nhận artistId từ frontend gửi lên.
          (Nếu nhận, kẻ gian chỉ cần sửa artistId trong request là giả làm người khác.)
        - Service tìm bài bằng findByIdAndArtistId(songId, artistId):
          bài của người khác → không tìm thấy → báo "Song does not exist".
        → Artist A không thể sửa / xóa bài của Artist B, dù đoán được songId.
        → Báo "không tồn tại" thay vì "không có quyền" để không lộ việc bài đó có tồn tại.

    ---------- 4. KHI NÀO DÙNG JSON, KHI NÀO DÙNG FORM-DATA? ----------

        API có gửi FILE (đăng bài, đổi ảnh bìa)
            → multipart/form-data. JSON chỉ chứa chữ, không gửi được file nhị phân (mp3, jpg).
        API chỉ có chữ (sửa tên, đổi lịch)
            → JSON (application/json), dùng @RequestBody.
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
        - Spring TỰ TRUYỀN vào bất kỳ method controller nào khai báo tham số kiểu này.
        - Đối tượng này được JwtAuthenticationFilter tạo ra sau khi kiểm tra token hợp lệ,
          lưu trong SecurityContextHolder trong suốt request.
        - authentication.getName() = "username" trong UserDetails = EMAIL
          (CustomUserDetailsService dùng .withUsername(user.getEmail())).

        Email → tìm User trong database → lấy id.
        Vì sao không ghi thẳng id vào token?
        - Token hiện chỉ chứa email (subject) và mã phiên (jti).
        - Tra id từ email vẫn nhanh vì cột email có UNIQUE → MySQL tự tạo index.

        private: hàm nội bộ, chỉ dùng trong class này.
    */
    private Long me(Authentication authentication) {
        return userService.getByEmail(authentication.getName()).getId();
    }

    /*
        ---------------------------------------------------------------
        UC26 - Tải lên bài hát.
        ---------------------------------------------------------------

        VÍ DỤ GỌI (Postman: Body → form-data):
            POST /api/artist/songs
            Content-Type: multipart/form-data

            KEY                TYPE    VALUE
            title              Text    Chiều Mưa
            audio              File    chieu-mua.mp3
            cover              File    bia.jpg                 (không bắt buộc)
            featuredArtistIds  Text    12                      (không bắt buộc)
            featuredArtistIds  Text    13                      (lặp key để feat nhiều người)
            releaseAt          Text    2026-12-24T20:00:00     (không bắt buộc)

        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
            → API này CHỈ nhận multipart/form-data.
            → Gửi JSON vào → 415 Unsupported Media Type.

        @ModelAttribute UploadSongRequest request
            → Spring đọc TỪNG trường của form-data rồi gọi setter tương ứng:
              setTitle("Chiều Mưa"), setAudio(file), setCover(file),
              setFeaturedArtistIds([12, 13]), setReleaseAt(...).
            → Giống @RequestBody nhưng dành cho form-data.
            → releaseAt đổi từ chuỗi sang LocalDateTime nhờ @DateTimeFormat trong DTO.

        @Valid → kiểm tra trong DTO:
            @NotBlank title, @Size(max = 150) title,
            @NotNull audio, @Size(max = 5) featuredArtistIds.

        SERVICE LÀM GÌ:
            1. Kiểm tra người đăng là Artist đang hoạt động.
            2. Lịch phát hành (nếu có) phải ở tương lai.
            3. Kiểm tra danh sách feat (tối đa 5, không tự feat mình, phải là Artist ACTIVE).
            4. Lưu file nhạc → đọc thời lượng (file giả, VD .txt đổi đuôi .mp3 → báo lỗi).
            5. Lưu ảnh bìa (kiểm tra chữ ký file, chặn PDF đổi đuôi .png).
            6. Lưu bài với trạng thái PENDING.
            Bước nào lỗi → xóa các file vừa lưu, không để lại file rác trên ổ đĩa.

        JSON TRẢ VỀ: SongManageResponse với "status": "PENDING".

        LỖI CÓ THỂ GẶP:
            400 "Title is required" / "Audio file is required"
            400 "Only MP3 or WAV files are allowed"
            400 "MP3 file must not exceed 20MB" / "WAV file must not exceed 100MB"
            400 "Invalid or corrupted audio file"
            400 "Only JPG, PNG or WEBP images are allowed" / "Image must not exceed 2MB"
            400 "Release time must be in the future"
            400 "Maximum 5 featured artists" / "You cannot feature yourself"
            413 "File is too large" (vượt giới hạn multipart trong application.properties)
            415 (gửi JSON thay vì form-data)
    */
    @PostMapping(value = "/songs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SongManageResponse> uploadSong(Authentication authentication,
                                                         @Valid @ModelAttribute UploadSongRequest request) {
        Song song = artistSongService.uploadSong(
                me(authentication),
                request.getTitle(),
                request.getAudio(),
                request.getCover(),
                request.getFeaturedArtistIds(),
                request.getReleaseAt());

        return ResponseEntity.ok(SongMapper.toManageResponse(song));
    }

    /*
        ---------------------------------------------------------------
        UC28 - Bài hát của tôi.
        ---------------------------------------------------------------

        VÍ DỤ GỌI:
            GET /api/artist/songs

        Trả về MỌI bài của Artist ở mọi trạng thái (trừ DELETED), mới tải lên trước,
        KHÔNG phân trang (1 Artist thường chỉ có vài chục bài).

        .stream().map(SongMapper::toManageResponse).toList()
            - stream(): biến List<Song> thành "dòng chảy" để xử lý lần lượt từng phần tử.
            - map(...): biến MỖI Song thành 1 SongManageResponse.
            - toList(): gom kết quả thành List<SongManageResponse> (danh sách không sửa được).

            Tương đương vòng for quen thuộc:
                List<SongManageResponse> result = new ArrayList<>();
                for (Song s : songs) {
                    result.add(SongMapper.toManageResponse(s));
                }

        JSON TRẢ VỀ: 1 MẢNG (không có content / page như các API phân trang):
            [
                { "id": 15, "title": "Chiều Mưa", "status": "APPROVED", "released": true, ... },
                { "id": 14, "title": "Phố Đêm",   "status": "REJECTED",
                  "rejectReason": "Ảnh bìa không phù hợp", ... }
            ]

        Frontend dựa vào status + released để hiển thị nhãn:
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
        UC37 - Tìm nghệ sĩ để feat.
        ---------------------------------------------------------------

        VÍ DỤ GỌI:
            GET /api/artist/search-artists?keyword=may

        Dùng cho ô tìm kiếm khi Artist chọn người feat lúc đăng / sửa bài:
        gõ "may" → hiện tối đa 10 Artist có nghệ danh chứa "may"
        (không phân biệt hoa thường: "Mây Chiều" vẫn khớp nếu gõ "mây").

        - Tự loại chính mình khỏi kết quả (không ai tự feat mình).
        - Chỉ trả Artist đang ACTIVE.
        - keyword rỗng hoặc toàn dấu cách → trả mảng rỗng [].
        - Không gửi tham số keyword → 400 (Spring báo thiếu tham số bắt buộc,
          vì @RequestParam mặc định required = true).

        JSON TRẢ VỀ: chỉ id + nghệ danh, KHÔNG có email hay thông tin cá nhân:
            [ { "id": 12, "artistName": "Mây Chiều" }, { "id": 19, "artistName": "Mây Xanh" } ]

        Frontend lấy "id" của người được chọn, gửi vào featuredArtistIds khi đăng / sửa bài.
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
        UC29 - Sửa tên bài hát và danh sách feat.
        ---------------------------------------------------------------

        VÍ DỤ GỌI:
            PUT /api/artist/songs/15
            Content-Type: application/json
            Body: { "title": "Chiều Mưa (Acoustic)", "featuredArtistIds": [12, 13] }

        Dùng @RequestBody (JSON) vì API này không có file.

        featuredArtistIds là danh sách MỚI HOÀN TOÀN, thay thế danh sách cũ:
            Đang feat [12, 13], gửi [12]  → còn feat 12.
            Gửi [] hoặc bỏ trống trường này → không còn feat với ai.

        Bài đang REJECTED mà Artist sửa → tự chuyển về PENDING và xóa lý do từ chối cũ,
        để Admin duyệt lại.

        LỖI CÓ THỂ GẶP:
            400 "Title is required" / "Title must not exceed 150 characters"
            400 "Song does not exist"  (sai id, bài của người khác, hoặc đã xóa)
            400 "Maximum 5 featured artists" / "You cannot feature yourself"
            400 "Some featured artists do not exist"
            409 (Admin thao tác trên bài này đúng lúc Artist bấm lưu)
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
        UC30 - Đổi ảnh bìa.
        ---------------------------------------------------------------

        VÍ DỤ GỌI (Postman: Body → form-data):
            PUT /api/artist/songs/15/cover
            KEY: cover   TYPE: File   VALUE: bia-moi.jpg

        @RequestParam("cover") MultipartFile cover
            → API chỉ có 1 trường nên lấy thẳng file, không cần tạo DTO.
            → MultipartFile: đối tượng Spring tạo cho mỗi file tải lên,
              có tên file gốc, kích thước, loại file, và nội dung để đọc.
            → Thiếu key "cover" → 400 "File is required"
              (MissingServletRequestPartException trong GlobalExceptionHandler).

        THỨ TỰ AN TOÀN TRONG SERVICE:
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
        UC31 - Đổi lịch phát hành.
        ---------------------------------------------------------------

        VÍ DỤ GỌI:
            PUT /api/artist/songs/15/release
            Body: { "releaseAt": "2026-12-24T20:00:00" }   → lên lịch 20h ngày 24/12
            Body: { "releaseAt": null }                    → phát hành ngay

        Định dạng thời gian: "năm-tháng-ngàyTgiờ:phút:giây", giờ Việt Nam, không kèm múi giờ.
        Gửi sai định dạng (VD "24/12/2026") → Jackson không đọc được → 400 "Invalid request data".

        Không có @Valid vì UpdateReleaseRequest không có annotation kiểm tra nào.
        Việc kiểm tra nằm trong Service, vì cần so với thời gian hiện tại và trạng thái bài:
            - Thời gian phải ở tương lai.
            - Bài đã phát hành (kể cả đang ẩn) → không đổi lịch được nữa.
              (Chặn kiểu lách luật: ẩn bài đã phát hành rồi đổi lịch sang năm sau.)
            - releaseAt = null với bài đã duyệt → gán = bây giờ (phát hành ngay).

        LỖI CÓ THỂ GẶP:
            400 "Release time must be in the future"
            400 "Cannot change the schedule of a released song"
            400 "Song does not exist"
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
        UC32 - Ẩn bài (APPROVED → HIDDEN).
        ---------------------------------------------------------------

        VÍ DỤ GỌI:
            PUT /api/artist/songs/15/hide      (không cần body)

        Bài ẩn: người nghe không thấy nữa (không có trong danh sách, tìm kiếm,
        trang Artist), nhưng GIỮ NGUYÊN lượt nghe, file nhạc, ảnh bìa, lịch phát hành.
        Khác với xóa: ẩn có thể hiện lại, xóa thì mất file vĩnh viễn.

        LỖI CÓ THỂ GẶP:
            400 "Only approved songs can be hidden" → bài đang PENDING / REJECTED / đã ẩn
            400 "Song does not exist"
    */
    @PutMapping("/songs/{songId}/hide")
    public ResponseEntity<SongManageResponse> hideSong(Authentication authentication,
                                                       @PathVariable Long songId) {
        return ResponseEntity.ok(SongMapper.toManageResponse(
                artistSongService.hideSong(me(authentication), songId)));
    }

    /*
        ---------------------------------------------------------------
        UC32 - Hiện lại bài đã ẩn (HIDDEN → APPROVED).
        ---------------------------------------------------------------

        VÍ DỤ GỌI:
            PUT /api/artist/songs/15/show      (không cần body)

        Không cần Admin duyệt lại, vì bài đã được duyệt trước khi bị ẩn.

        LỖI CÓ THỂ GẶP:
            400 "Only hidden songs can be shown again"
            400 "Song does not exist"
    */
    @PutMapping("/songs/{songId}/show")
    public ResponseEntity<SongManageResponse> showSong(Authentication authentication,
                                                       @PathVariable Long songId) {
        return ResponseEntity.ok(SongMapper.toManageResponse(
                artistSongService.showSong(me(authentication), songId)));
    }

    /*
        ---------------------------------------------------------------
        UC33 - Xóa bài.
        ---------------------------------------------------------------

        VÍ DỤ GỌI:
            DELETE /api/artist/songs/15

        XÓA MỀM (soft delete):
            - Bản ghi VẪN CÒN trong database, chỉ đổi status = DELETED.
            - File nhạc và ảnh bìa bị xóa HẲN khỏi ổ đĩa (không chiếm dung lượng).
        Vì sao không xóa hẳn bản ghi?
            - Giữ lịch sử (bài từng tồn tại, từng có bao nhiêu lượt nghe).
            - Tránh lỗi khóa ngoại nếu sau này có bảng khác trỏ tới bài (playlist, lịch sử nghe).

        Trả về Map.of("message", "...") → Jackson chuyển thành JSON:
            { "message": "Song has been deleted" }
        Map.of tạo Map KHÔNG SỬA ĐƯỢC, đủ dùng cho 1 thông báo đơn giản.
        Không trả SongManageResponse vì bài đã xóa, frontend không cần hiển thị nữa.

        LỖI CÓ THỂ GẶP:
            400 "Song does not exist"  (sai id, bài của người khác, hoặc đã xóa trước đó)
            409 (Admin đang duyệt bài này đúng lúc Artist bấm xóa)
    */
    @DeleteMapping("/songs/{songId}")
    public ResponseEntity<Map<String, String>> deleteSong(Authentication authentication,
                                                          @PathVariable Long songId) {
        artistSongService.deleteSong(me(authentication), songId);
        return ResponseEntity.ok(Map.of("message", "Song has been deleted"));
    }
}