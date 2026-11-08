package com.winhmm.myspotify.controller;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    - 4 DTO: CreatePlaylistRequest, UpdatePlaylistRequest (dữ liệu gửi lên),
             PlaylistDetailResponse, PlaylistSummaryResponse (dữ liệu trả về).
    - PlaylistService: nơi xử lý nghiệp vụ. UserService: lấy tài khoản đang đăng nhập.
    - Valid: annotation @Valid, bật kiểm tra dữ liệu gửi lên.
    - MediaType: hằng số loại dữ liệu (form-data, JSON...).
    - ResponseEntity: response trả về (mã HTTP + body).
    - Authentication: thông tin người đang đăng nhập (lấy từ token).
    - org.springframework.web.bind.annotation.*: các annotation của Controller
      (@RestController, @GetMapping, @PathVariable, @RequestBody...).
    - MultipartFile: file ảnh bìa tải lên.
    - List, Map: danh sách playlist, response dạng {"message": "..."}.
*/
import com.winhmm.myspotify.dto.playlist.CreatePlaylistRequest;
import com.winhmm.myspotify.dto.playlist.PlaylistDetailResponse;
import com.winhmm.myspotify.dto.playlist.PlaylistSummaryResponse;
import com.winhmm.myspotify.dto.playlist.UpdatePlaylistRequest;
import com.winhmm.myspotify.service.PlaylistService;
import com.winhmm.myspotify.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/*
    =====================================================================
    PLAYLISTCONTROLLER - API PLAYLIST
    =====================================================================

    (Controller là gì, @RestController, @RequestMapping, ResponseEntity: xem các Controller đã có,
     VD GenreController, ArtistSongController. Tất cả cũng được giải thích lại bên dưới.)

    Các API:
        POST   /api/playlists                              → tạo playlist (form-data)
        GET    /api/playlists/me                           → playlist của tôi
        GET    /api/playlists/users/{userId}               → playlist công khai của 1 tài khoản
        GET    /api/playlists/{playlistId}                 → chi tiết 1 playlist
        PUT    /api/playlists/{playlistId}                 → sửa tên, mô tả, công khai (JSON)
        PUT    /api/playlists/{playlistId}/cover           → đổi ảnh bìa (form-data)
        DELETE /api/playlists/{playlistId}/cover           → gỡ ảnh bìa
        POST   /api/playlists/{playlistId}/songs/{songId}  → thêm 1 bài
        DELETE /api/playlists/{playlistId}/songs/{songId}  → bỏ 1 bài
        DELETE /api/playlists/{playlistId}                 → xóa playlist

    =====================================================================
    CONTROLLER LÀ GÌ?
    =====================================================================

    Controller là "quầy tiếp nhận": nhận request HTTP từ frontend (hoặc Postman),
    đọc dữ liệu gửi lên, gọi Service xử lý, rồi trả kết quả về.
    Controller không tự kiểm tra quy tắc nghiệp vụ (đó là việc của Service).

    1 request HTTP gồm:
    - Method (phương thức): GET (lấy), POST (tạo, hành động), PUT (sửa), DELETE (xóa).
    - URL: VD /api/playlists/3.
    - Header: VD Authorization: Bearer <token> (cho biết ai đang gọi).
    - Body (không bắt buộc): dữ liệu gửi kèm, dạng JSON hoặc form-data.
    Spring so method + URL với các annotation bên dưới để chọn đúng hàm chạy.

    -----------------------------------------------------
    @RestController và @RequestMapping
    -----------------------------------------------------
    - @RestController: class này là controller, và giá trị các hàm trả về
      được tự đổi thành JSON (bằng thư viện Jackson) rồi gửi cho frontend.
      Spring cũng tự tạo bean PlaylistController lúc khởi động (giống @Service).
    - @RequestMapping("/api/playlists"): đường dẫn chung cho cả class.
      Đường dẫn của từng hàm được nối vào sau: "/api/playlists" + "/me" = "/api/playlists/me".

    -----------------------------------------------------
    ResponseEntity
    -----------------------------------------------------
    ResponseEntity<T> = cả response: mã HTTP + body có kiểu T.
    ResponseEntity.ok(x): mã 200 OK, body là x (Jackson đổi x thành JSON).
    Lỗi (400, 403...) không trả ở đây mà do GlobalExceptionHandler trả khi Service ném exception.

    -----------------------------------------------------
    Ai được gọi?
    -----------------------------------------------------
    Không có @PreAuthorize → mọi tài khoản đã đăng nhập (USER, ARTIST, ADMIN) đều dùng được.
    Chưa đăng nhập → 401 (SecurityConfig: ngoài vài URL mở sẵn như /api/auth/**, /uploads/**,
    mọi URL khác đều phải đăng nhập).
    Ai được xem playlist nào, ai được sửa: PlaylistService kiểm tra.

    Postman: mọi API dưới đây đều cần tab Authorization → Bearer Token → dán token lấy từ API đăng nhập.

    -----------------------------------------------------
    "/me", "/users/{userId}" và "/{playlistId}" có bị nhầm nhau không?
    -----------------------------------------------------
    Không.
    - {playlistId} là "chỗ trống" chỉ khớp đúng 1 đoạn đường dẫn (không chứa dấu /).
      /users/5 có 2 đoạn → không khớp /{playlistId}, chỉ khớp /users/{userId}.
    - /me và /{playlistId} cùng 1 đoạn: Spring ưu tiên đường dẫn viết cố định ("/me")
      hơn đường dẫn có chỗ trống. GET /api/playlists/me → getMyPlaylists, GET /api/playlists/3 → getPlaylist.
    - GET /api/playlists/abc → 400 "Invalid parameter: playlistId" (playlistId phải là số).
*/
@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {
    private final PlaylistService playlistService;
    private final UserService userService;

    /*
        Constructor injection: Spring tự truyền 2 bean PlaylistService và UserService vào
        (giải thích chi tiết: xem phần "Service và constructor injection" trong PlaylistService).
    */
    public PlaylistController(PlaylistService playlistService, UserService userService) {
        this.playlistService = playlistService;
        this.userService = userService;
    }

    /*
        -----------------------------------------------------
        Lấy id người đang đăng nhập
        -----------------------------------------------------
        Authentication authentication: khai báo làm tham số của hàm API là Spring tự truyền vào.
        Đối tượng này do JwtAuthenticationFilter tạo ra sau khi kiểm tra token hợp lệ,
        authentication.getName() là email trong token.
        → email → tìm User → lấy id. (Giống hàm me trong ArtistSongController, UserController.)

        Không bao giờ nhận id người dùng từ frontend → không ai giả làm người khác được.
        private: hàm nội bộ, không có @GetMapping... nên không phải API.
    */
    private Long me(Authentication authentication) {
        return userService.getByEmail(authentication.getName()).getId();
    }

    /*
        ---------------------------------------------------------------
        Tạo playlist
        ---------------------------------------------------------------
        POST /api/playlists
        Body: form-data (các key: xem CreatePlaylistRequest)

        Postman: Body → form-data
            name      Text   Nhạc chill cuối tuần
            isPublic  Text   true
            songIds   Text   15
            songIds   Text   22
            cover     File   bia.jpg        (không bắt buộc)

        - @PostMapping không ghi đường dẫn → dùng luôn đường dẫn của class: /api/playlists.
        - consumes = MULTIPART_FORM_DATA_VALUE: chỉ nhận form-data.
          Gửi JSON → 415 "Content-Type 'application/json' is not supported."
        - @Valid @ModelAttribute: đọc form-data vào CreatePlaylistRequest rồi kiểm tra annotation.
          Sai → 400, hàm này không chạy.

        JSON trả về: PlaylistDetailResponse của playlist vừa tạo (kèm danh sách bài).

        Lỗi có thể gặp:
            400 "Name is required"                     thiếu tên
            400 "At least 1 song is required"          không chọn bài nào
            400 "Maximum 200 songs per playlist"       quá 200 bài
            400 "Song does not exist: 15"              bài 15 không tồn tại hoặc chưa phát hành
            400 "Maximum 50 playlists per account"     đã có 50 playlist
            400 "Only JPG, PNG or WEBP images are allowed", "Image must not exceed 2MB"   ảnh bìa sai
    */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PlaylistDetailResponse> createPlaylist(Authentication authentication,
                                                                 @Valid @ModelAttribute CreatePlaylistRequest request) {
        PlaylistDetailResponse response = playlistService.createPlaylist(
                me(authentication),
                request.getName(),
                request.getDescription(),
                request.getIsPublic(),
                request.getSongIds(),
                request.getCover());
        return ResponseEntity.ok(response);
    }

    /*
        ---------------------------------------------------------------
        Playlist của tôi
        ---------------------------------------------------------------
        GET /api/playlists/me      (không cần body)
        JSON trả về: mảng PlaylistSummaryResponse, mới tạo trước, gồm cả riêng tư lẫn công khai.
        Chưa có playlist nào → [] (mảng rỗng, không báo lỗi).
    */
    @GetMapping("/me")
    public ResponseEntity<List<PlaylistSummaryResponse>> getMyPlaylists(Authentication authentication) {
        return ResponseEntity.ok(playlistService.getMyPlaylists(me(authentication)));
    }

    /*
        ---------------------------------------------------------------
        Playlist công khai của 1 tài khoản
        ---------------------------------------------------------------
        GET /api/playlists/users/{userId}      VD /api/playlists/users/6

        Dùng ở trang cá nhân của 1 người. Chỉ có playlist công khai.
        Tài khoản không tồn tại, bị khóa, hoặc chưa có playlist công khai → [] (mảng rỗng).

        Không cần biết ai đang xem (ai xem cũng thấy giống nhau),
        nên hàm không có tham số Authentication. Vẫn phải đăng nhập mới gọi được (SecurityConfig).
    */
    @GetMapping("/users/{userId}")
    public ResponseEntity<List<PlaylistSummaryResponse>> getPublicPlaylists(@PathVariable Long userId) {
        return ResponseEntity.ok(playlistService.getPublicPlaylists(userId));
    }

    /*
        ---------------------------------------------------------------
        Chi tiết 1 playlist
        ---------------------------------------------------------------
        GET /api/playlists/{playlistId}      VD /api/playlists/3

        @PathVariable Long playlistId: lấy giá trị ở chỗ trống {playlistId} trong đường dẫn,
        VD /api/playlists/3 → playlistId = 3.
        Tên tham số Java (playlistId) phải trùng với tên trong dấu { }.

        JSON trả về: PlaylistDetailResponse.
        Lỗi có thể gặp:
            400 "Playlist does not exist"      không có playlist này, hoặc chủ playlist không còn ACTIVE
            400 "This playlist is private"     playlist riêng tư của người khác
    */
    @GetMapping("/{playlistId}")
    public ResponseEntity<PlaylistDetailResponse> getPlaylist(Authentication authentication,
                                                              @PathVariable Long playlistId) {
        return ResponseEntity.ok(playlistService.getPlaylistDetail(me(authentication), playlistId));
    }

    /*
        ---------------------------------------------------------------
        Sửa tên, mô tả, công khai hay riêng tư
        ---------------------------------------------------------------
        PUT /api/playlists/{playlistId}
        Postman: Body → raw → JSON
            {
                "name": "Nhạc chill tối thứ 7",
                "description": "Nghe lúc đọc sách",
                "isPublic": false
            }

        - @RequestBody: đọc JSON trong body thành object UpdatePlaylistRequest.
        - @Valid: kiểm tra các annotation trong UpdatePlaylistRequest.

        JSON trả về: PlaylistDetailResponse sau khi sửa.
        Lỗi có thể gặp:
            400 "Name is required"           thiếu tên hoặc tên toàn dấu cách
            400 "isPublic is required"       thiếu isPublic
            400 "Playlist does not exist"
            403                              playlist của người khác
    */
    @PutMapping("/{playlistId}")
    public ResponseEntity<PlaylistDetailResponse> updatePlaylist(Authentication authentication,
                                                                 @PathVariable Long playlistId,
                                                                 @Valid @RequestBody UpdatePlaylistRequest request) {
        PlaylistDetailResponse response = playlistService.updatePlaylist(
                me(authentication),
                playlistId,
                request.getName(),
                request.getDescription(),
                request.getIsPublic());
        return ResponseEntity.ok(response);
    }

    /*
        ---------------------------------------------------------------
        Đổi ảnh bìa
        ---------------------------------------------------------------
        PUT /api/playlists/{playlistId}/cover
        Postman: Body → form-data
            cover   File   bia-moi.jpg

        @RequestParam("cover") MultipartFile cover:
            → API chỉ có 1 trường nên lấy thẳng file, không cần tạo DTO (giống đổi ảnh bìa bài hát).
            → Thiếu key "cover" → 400 "File is required"
              (MissingServletRequestPartException trong GlobalExceptionHandler).

        JSON trả về: PlaylistDetailResponse, "coverUrl" là ảnh mới, "hasCustomCover": true.
        Lỗi có thể gặp:
            400 "File is required"                            thiếu key cover
            400 "Image file is required"                      có key nhưng chưa chọn file
            400 "Only JPG, PNG or WEBP images are allowed"
            400 "Image must not exceed 2MB"
            400 "Invalid or corrupted image file"             file giả (đổi đuôi .jpg) hoặc hỏng
            403                                               playlist của người khác
    */
    @PutMapping(value = "/{playlistId}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PlaylistDetailResponse> updateCover(Authentication authentication,
                                                              @PathVariable Long playlistId,
                                                              @RequestParam("cover") MultipartFile cover) {
        return ResponseEntity.ok(playlistService.updateCover(me(authentication), playlistId, cover));
    }

    /*
        ---------------------------------------------------------------
        Gỡ ảnh bìa
        ---------------------------------------------------------------
        DELETE /api/playlists/{playlistId}/cover      (không cần body)

        File ảnh bị xóa khỏi ổ đĩa. Playlist quay về dùng ảnh bìa của bài đầu tiên.
        JSON trả về: PlaylistDetailResponse, "hasCustomCover": false.
        Lỗi có thể gặp:
            400 "Playlist has no custom cover"     playlist chưa có ảnh riêng
            403                                    playlist của người khác
    */
    @DeleteMapping("/{playlistId}/cover")
    public ResponseEntity<PlaylistDetailResponse> removeCover(Authentication authentication,
                                                              @PathVariable Long playlistId) {
        return ResponseEntity.ok(playlistService.removeCover(me(authentication), playlistId));
    }

    /*
        ---------------------------------------------------------------
        Thêm 1 bài vào playlist
        ---------------------------------------------------------------
        POST /api/playlists/{playlistId}/songs/{songId}      VD /api/playlists/3/songs/15
        (không cần body)

        2 chỗ trống trong đường dẫn → 2 tham số @PathVariable, Spring ghép theo tên.
        Bài mới nằm cuối playlist.

        JSON trả về: PlaylistDetailResponse sau khi thêm.
        Lỗi có thể gặp:
            400 "Song does not exist"                bài không tồn tại hoặc chưa phát hành
            400 "Song is already in this playlist"   bài đã có trong playlist
            400 "Maximum 200 songs per playlist"     playlist đã đủ 200 bài
            403                                      playlist của người khác
            409 "Data already exists"                bấm thêm 2 lần đúng cùng lúc (lần thứ hai bị chặn)
    */
    @PostMapping("/{playlistId}/songs/{songId}")
    public ResponseEntity<PlaylistDetailResponse> addSong(Authentication authentication,
                                                          @PathVariable Long playlistId,
                                                          @PathVariable Long songId) {
        return ResponseEntity.ok(playlistService.addSong(me(authentication), playlistId, songId));
    }

    /*
        ---------------------------------------------------------------
        Bỏ 1 bài khỏi playlist
        ---------------------------------------------------------------
        DELETE /api/playlists/{playlistId}/songs/{songId}      VD /api/playlists/3/songs/15
        (không cần body)

        Chỉ bỏ bài khỏi playlist, bài hát gốc vẫn còn nguyên.
        JSON trả về: PlaylistDetailResponse sau khi bỏ.
        Lỗi có thể gặp:
            400 "Song is not in this playlist"     bài không nằm trong playlist
            403                                    playlist của người khác
    */
    @DeleteMapping("/{playlistId}/songs/{songId}")
    public ResponseEntity<PlaylistDetailResponse> removeSong(Authentication authentication,
                                                             @PathVariable Long playlistId,
                                                             @PathVariable Long songId) {
        return ResponseEntity.ok(playlistService.removeSong(me(authentication), playlistId, songId));
    }

    /*
        ---------------------------------------------------------------
        Xóa playlist
        ---------------------------------------------------------------
        DELETE /api/playlists/{playlistId}      (không cần body)

        Xóa hẳn playlist và danh sách bài của nó (bài hát gốc không bị ảnh hưởng).
        Service không trả gì (void) nên Controller tự trả 1 câu thông báo:
            Map.of("message", "Playlist deleted") → JSON {"message": "Playlist deleted"}
        Map.of(key, value): tạo nhanh 1 Map có 1 cặp khóa - giá trị (giống các API khác trong dự án).

        Lỗi có thể gặp:
            400 "Playlist does not exist"     không có, hoặc đã xóa rồi
            403                               playlist của người khác
    */
    @DeleteMapping("/{playlistId}")
    public ResponseEntity<Map<String, String>> deletePlaylist(Authentication authentication,
                                                              @PathVariable Long playlistId) {
        playlistService.deletePlaylist(me(authentication), playlistId);
        return ResponseEntity.ok(Map.of("message", "Playlist deleted"));
    }
}