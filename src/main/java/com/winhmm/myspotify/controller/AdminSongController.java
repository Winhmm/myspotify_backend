package com.winhmm.myspotify.controller;

import com.winhmm.myspotify.dto.song.PageResponse;
import com.winhmm.myspotify.dto.song.RejectSongRequest;
import com.winhmm.myspotify.dto.song.SongManageResponse;
import com.winhmm.myspotify.enums.SongStatus;
import com.winhmm.myspotify.mapper.SongMapper;
import com.winhmm.myspotify.service.AdminSongService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/*
    =====================================================================
    ADMINSONGCONTROLLER - API ADMIN QUẢN LÝ BÀI HÁT (UC34 - UC36)
    =====================================================================

    Các API trong class:
        GET  /api/admin/songs                    → danh sách bài, lọc theo trạng thái
        PUT  /api/admin/songs/{songId}/approve   → duyệt bài
        PUT  /api/admin/songs/{songId}/reject    → từ chối bài đang chờ duyệt
        PUT  /api/admin/songs/{songId}/remove    → gỡ bài đã duyệt

    Chỉ tài khoản role ADMIN gọi được (@PreAuthorize ở đầu class).

    Phần bên dưới mô tả hành trình đầy đủ của 1 request, lấy API duyệt bài làm ví dụ.

    Mọi API cần đăng nhập trong project (không riêng API Admin) đều đi qua
    các bước (2) đến (9) giống hệt như vậy, chỉ khác bước (10) kiểm tra role.

    =====================================================================
    HÀNH TRÌNH CHI TIẾT CỦA MỘT REQUEST
    =====================================================================

    Ví dụ:
    Admin đăng nhập vào MySpotify và nhấn nút "Duyệt" bài hát ID = 15.

    API:
    PUT /api/admin/songs/15/approve

    Mục tiêu:
    - Xác thực người gửi request là ai.
    - Kiểm tra phiên đăng nhập còn hợp lệ hay không.
    - Kiểm tra người dùng có quyền ADMIN hay không.
    - Nếu hợp lệ, thực hiện nghiệp vụ duyệt bài hát.
    - Trả kết quả về frontend.

    =====================================================================
    (1) ADMIN ĐĂNG NHẬP VÀ NHẬN JWT (CHỈ LÀM 1 LẦN, TRƯỚC MỌI REQUEST KHÁC)
    =====================================================================

    Bước 1:
    Frontend gửi email và mật khẩu đến POST /api/auth/login.

    Bước 2:
    AuthService.login kiểm tra:
    - Email có tồn tại không.
    - Mật khẩu có đúng không: passwordEncoder.matches(mật khẩu gửi lên, mật khẩu đã hash).
    - Tài khoản có đang ACTIVE không (chưa xác thực, bị khóa, đã xóa → từ chối).
    Sai email hoặc sai mật khẩu đều báo chung "Email or password is incorrect",
    để kẻ gian không dò được email nào có tồn tại trong hệ thống.

    Đây là lần duy nhất mật khẩu được kiểm tra. Các request sau chỉ dùng JWT.

    Bước 3:
    SessionService.createSession(email):
    - Sinh mã phiên ngẫu nhiên (UUID), VD "3f2a9c1e-8b7d-...".
    - Lưu vào Redis: session:admin@gmail.com → 3f2a9c1e-8b7d-...
      (tự xóa sau đúng thời hạn của token).
    - Đăng nhập ở thiết bị mới → mã phiên mới ghi đè mã cũ → thiết bị cũ bị đăng xuất.

    Bước 4:
    JwtUtil.generateToken(user, sessionId) tạo JWT chứa đúng 4 thông tin:
        sub (subject)    : email
        jti (JWT ID)     : mã phiên vừa tạo ở bước 3
        iat (issued at)  : thời điểm tạo
        exp (expiration) : thời điểm hết hạn
    rồi ký bằng khóa bí mật app.jwt.secret (chỉ server biết).

    Bước 5:
    Backend trả về frontend:
    {
        "token": "<JWT>",
        "userId": 1,
        "username": "admin",
        "role": "ADMIN"
    }

    Frontend lưu token lại, gửi kèm trong mọi request sau.
    role trong JSON chỉ để frontend biết hiện hay ẩn menu Admin.
    Backend không tin role này, mà đọc lại role từ database ở mỗi request (bước 6).

    ---------- JWT là gì? ----------

    JWT gồm 3 phần, nối bằng dấu chấm:  Header.Payload.Signature

    - Header và Payload chỉ được encode bằng Base64URL, không được mã hóa.
      → Ai có token cũng giải ra đọc được email bên trong (thử dán token vào jwt.io).
      → Tuyệt đối không đưa mật khẩu hay dữ liệu bí mật vào JWT.

    - Signature (chữ ký) tạo từ Header + Payload + khóa bí mật.
      → Kẻ gian sửa email trong payload thành email khác
        thì chữ ký không còn khớp → server phát hiện ngay ở bước (3).
      → Chữ ký chống sửa, không chống đọc.

    =====================================================================
    (2) FRONTEND GỬI REQUEST DUYỆT BÀI HÁT
    =====================================================================

    Khi Admin nhấn nút "Duyệt", frontend gửi:

        PUT /api/admin/songs/15/approve
        Authorization: Bearer <JWT>

    Trong đó:
    - PUT: phương thức HTTP dùng cho yêu cầu cập nhật (đổi trạng thái bài).
    - /api/admin/songs/15/approve: endpoint duyệt bài hát ID = 15.
    - Authorization: header chứa thông tin xác thực.
    - Bearer: cho biết giá trị theo sau là access token.
    - <JWT>: token backend đã cấp khi đăng nhập.

    Frontend không gửi lại mật khẩu trong mỗi request, chỉ gửi JWT.

    =====================================================================
    (3) JwtAuthenticationFilter NHẬN VÀ KIỂM TRA JWT
    =====================================================================

    Filter là đoạn code chạy trước Controller ở mỗi request.
    JwtAuthenticationFilter kế thừa OncePerRequestFilter
    → được đảm bảo chạy đúng 1 lần cho mỗi request.

    Bước 3.1: Đọc header Authorization.

        String header = request.getHeader("Authorization");

    Header phải bắt đầu bằng "Bearer " → cắt bỏ 7 ký tự đầu, lấy phần token:
        String token = header.substring(7);

    Không có header, hoặc không bắt đầu bằng "Bearer "
    → filter không xác thực request này, nhưng vẫn cho request đi tiếp.
    Filter không tự chặn, việc chặn là của bước (9).
    (Nhờ vậy các API công khai như /api/auth/login vẫn đi qua filter bình thường.)

    Bước 3.2: Kiểm tra token.

        jwtUtil.validateToken(token)

    Bên trong, thư viện jjwt đọc token và dùng khóa bí mật để kiểm tra:
    - Đúng cấu trúc Header.Payload.Signature.
    - Chữ ký khớp (token không bị sửa).
    - Chưa hết hạn (exp còn ở tương lai).
    Sai bất kỳ điều nào → trả false → filter không xác thực, request đi tiếp.

    =====================================================================
    (4) LẤY EMAIL VÀ SESSION ID TỪ JWT
    =====================================================================

    Token hợp lệ → đọc 2 thông tin:

        String email = jwtUtil.getEmailFromToken(token);         // từ trường sub
        String sessionId = jwtUtil.getSessionIdFromToken(token); // từ trường jti

    - email    : tài khoản mà token đại diện, dùng để tìm User trong database.
    - sessionId: mã phiên của lần đăng nhập đã tạo ra token này,
                 dùng để đối chiếu với Redis ở bước (5).

    Ví dụ:
        email     = admin@gmail.com
        sessionId = 3f2a9c1e-8b7d-...

    Chỉ đọc 2 giá trị này sau khi token đã qua bước kiểm tra chữ ký,
    nên chắc chắn chúng do chính server tạo ra, không bị ai sửa.

    =====================================================================
    (5) SessionService KIỂM TRA PHIÊN ĐĂNG NHẬP TRONG REDIS
    =====================================================================

        sessionService.isValidSession(email, sessionId)

    Lấy mã phiên hiện tại trong Redis (lệnh GET session:admin@gmail.com)
    rồi so với sessionId trong token.

    Ví dụ:
        Token: sessionId = 3f2a9c1e...
        Redis: session:admin@gmail.com → 3f2a9c1e...

    Khớp → phiên hợp lệ → đi tiếp bước (6).

    Không khớp, hoặc Redis không có key, xảy ra khi:
    - Tài khoản đã đăng nhập ở thiết bị khác (mã phiên bị ghi đè).
    - Đã đăng xuất, đổi mật khẩu, đặt lại mật khẩu, đổi email (key bị xóa).
    - Bị Admin khóa tài khoản (key bị xóa).
    → Filter không xác thực request, và ghi chú vào request:
        request.setAttribute(SESSION_ERROR,
            "Your session has ended. Your account may have been logged in on another device");
      Bước (9) đọc ghi chú này để trả đúng thông báo cho frontend.

    Vì sao JWT hợp lệ vẫn cần kiểm tra Redis?
    JWT tự nó không thể "thu hồi": token còn hạn thì chữ ký vẫn đúng.
    Redis giữ "phiên nào đang được phép dùng". Xóa key trong Redis
    → mọi token cũ của tài khoản đó mất hiệu lực ngay, dù chưa hết hạn.
    Đây là cách project làm được đăng nhập 1 thiết bị và đăng xuất tức thì.

    Redis lỗi (mất kết nối...) → lỗi bị khối catch trong filter bắt lại
    → request coi như chưa đăng nhập → trả 401 (không phải 500).

    =====================================================================
    (6) CustomUserDetailsService TẢI USER TỪ DATABASE
    =====================================================================

    Phiên hợp lệ → filter gọi:

        userDetailsService.loadUserByUsername(email)

    Bước 6.1:
    UserRepository.findByEmail(email) tìm User trong MySQL. Ví dụ bảng users:

        id = 1
        email = admin@gmail.com
        password = <mật khẩu đã hash bằng BCrypt>
        role = ADMIN
        account_status = ACTIVE

    Bước 6.2:
    Chuyển entity User thành đối tượng UserDetails (dạng mà Spring Security hiểu):

        org.springframework.security.core.userdetails.User
            .withUsername(user.getEmail())
            .password(user.getPassword())
            .roles(user.getRole().name())
            .disabled(user.getAccountStatus() != AccountStatus.ACTIVE)
            .build();

    Code ghi tên đầy đủ org.springframework.security.core.userdetails.User
    vì project có entity com.winhmm.myspotify.entity.User trùng tên.
    Đây là class User của Spring Security, không phải entity User.

    Ý nghĩa:
    - withUsername(email): Spring Security gọi là "username", nhưng project dùng email.
      Vì vậy về sau authentication.getName() trả về email.

    - password(...): bắt buộc khai báo khi tạo UserDetails, nhưng trong luồng JWT
      không được dùng tới (mật khẩu chỉ kiểm tra 1 lần lúc đăng nhập, bước 1).

    - roles("ADMIN"): Spring tự thêm tiền tố → authority "ROLE_ADMIN".
      role USER → "ROLE_USER", role ARTIST → "ROLE_ARTIST".

    - disabled(...): accountStatus khác ACTIVE (UNVERIFIED, DISABLED, DELETED)
      → tài khoản bị đánh dấu không hoạt động.

    Không tìm thấy User (VD có người xóa thẳng dòng đó khỏi MySQL trong lúc phiên Redis vẫn còn)
    → ném UsernameNotFoundException → khối catch trong filter bắt
    → request coi như chưa đăng nhập.
    Trường hợp đổi email không tới được bước này: phiên của email cũ đã bị xóa,
    request bị chặn từ bước (5).

    =====================================================================
    (7) KIỂM TRA TÀI KHOẢN VÀ THIẾT LẬP AUTHENTICATION
    =====================================================================

        if (userDetails.isEnabled()) { ... }

    Tài khoản bị khóa, chưa xác thực hoặc đã xóa → isEnabled() = false
    → không xác thực, dù token và phiên đều hợp lệ.

    Tài khoản đang hoạt động → tạo Authentication:

        new UsernamePasswordAuthenticationToken(
            userDetails,                    // ai: thông tin người dùng
            null,                           // mật khẩu: không cần, đã xác thực bằng JWT
            userDetails.getAuthorities()    // quyền: [ROLE_ADMIN]
        )

    Constructor 3 tham số này tạo Authentication ở trạng thái đã xác thực.

    Rồi lưu vào SecurityContext của request hiện tại:

        SecurityContextHolder.getContext().setAuthentication(authentication);

    Từ thời điểm này, mọi phần phía sau đều biết:
    - Người gửi request là ai (email).
    - Đã xác thực.
    - Có những quyền gì (ROLE_ADMIN).
    Controller lấy lại thông tin này qua tham số Authentication authentication.

    Lưu ý: filter này không kiểm tra có phải ADMIN hay không.
    Nó chỉ trả lời "người này là ai, có quyền gì". Việc chặn theo quyền ở bước (10).

    =====================================================================
    (8) RateLimitFilter KIỂM TRA GIỚI HẠN REQUEST
    =====================================================================

    SecurityConfig đặt RateLimitFilter ngay sau JwtAuthenticationFilter
    (addFilterAfter), nên lúc này filter đã biết request của tài khoản nào.

    Cách đếm (Redis INCR, cửa sổ 60 giây):
    - /api/auth/** (đăng nhập, đăng ký, OTP): đếm theo IP, tối đa 10 request mỗi phút.
    - API khác, đã đăng nhập : đếm theo email, tối đa 100 request mỗi phút.
    - API khác, chưa đăng nhập: đếm theo IP,    tối đa 100 request mỗi phút.

    Vượt giới hạn → trả ngay 429 Too Many Requests, kèm header Retry-After (số giây phải chờ),
    request không đi tiếp tới Controller.

    Lưu ý: rate limit đếm trước bước kiểm tra quyền, nên cả request bị 401 hay 403
    cũng tính vào giới hạn (chống kẻ gian gửi dồn dập để dò quyền).

    Không đếm: request OPTIONS (trình duyệt tự gửi để hỏi CORS), /uploads/** (file nhạc, ảnh).

    =====================================================================
    (9) SecurityConfig KIỂM TRA QUY TẮC TRUY CẬP
    =====================================================================

    Cuối chuỗi filter, Spring Security áp dụng quy tắc đã khai báo trong SecurityConfig:
    - /api/auth/**, /uploads/**, Swagger... nằm trong permitAll() → ai cũng vào được.
    - Mọi đường dẫn còn lại (anyRequest().authenticated()) → bắt buộc đã xác thực.

    /api/admin/songs/15/approve thuộc nhóm thứ hai.

    Chưa có Authentication (token sai, hết hạn, phiên hết, tài khoản bị khóa...)
    → authenticationEntryPoint trong SecurityConfig trả 401:
        - Có ghi chú SESSION_ERROR từ bước (5) → message phiên đã kết thúc.
        - Không có → "Unauthorized, please login".
    → Request dừng ở đây, không tới Controller.

    Đã có Authentication → đi tiếp.
    Đã đăng nhập không có nghĩa được gọi mọi API, còn phải qua bước (10).

    =====================================================================
    (10) DispatcherServlet CHỌN METHOD, @PreAuthorize KIỂM TRA QUYỀN ADMIN
    =====================================================================

    Qua hết các filter, request tới DispatcherServlet của Spring MVC.
    DispatcherServlet so URL và HTTP method với các @RequestMapping, @PutMapping
    → tìm ra method approveSong(...) của class này.

    Ngay trước khi method chạy, Spring kiểm tra @PreAuthorize("hasRole('ADMIN')")
    đặt ở đầu class (áp dụng cho mọi method trong class).
    hasRole('ADMIN') kiểm tra Authentication có authority "ROLE_ADMIN" không.

    Có ROLE_ADMIN → cho phép chạy approveSong().

    Không có (VD chỉ có ROLE_USER hoặc ROLE_ARTIST)
    → ném AccessDeniedException ngay trong lúc gọi method controller
    → GlobalExceptionHandler.handleAccessDenied bắt lại → trả 403:
        { "message": "You do not have permission to access this resource" }

    Request chưa đăng nhập không bao giờ tới được bước này,
    vì đã bị chặn 401 ở bước (9).

    @PreAuthorize hoạt động được nhờ @EnableMethodSecurity trong SecurityConfig.
    Thiếu annotation đó thì @PreAuthorize bị bỏ qua hoàn toàn (ai cũng gọi được API Admin).

    Role luôn mới nhất:
    Role được đọc lại từ database ở mỗi request (bước 6), không lấy từ JWT,
    và project không có bộ nhớ đệm (cache) nào giữ role cũ.
    → Admin bị hạ xuống USER → ngay request kế tiếp đã nhận ROLE_USER → bị 403.

    =====================================================================
    (11) CONTROLLER GỌI SERVICE
    =====================================================================

    Method approveSong(@PathVariable Long songId) chạy:
    - @PathVariable lấy số 15 từ đường dẫn "/{songId}/approve" và đổi sang Long.
      Gửi "/abc/approve" → không đổi được → 400 "Invalid parameter: songId".
    - Gọi adminSongService.approveSong(15).

    Controller chỉ tiếp nhận request và chuyển cho Service,
    không chứa if/else nghiệp vụ.

    =====================================================================
    (12) SERVICE XỬ LÝ NGHIỆP VỤ VÀ DATABASE
    =====================================================================

    AdminSongService.approveSong(15):
    1. songRepository.findById(15): không có bài, hoặc bài đã DELETED
       → lỗi "Song does not exist".
    2. Bài phải đang PENDING → nếu không: "Only pending songs can be approved".
    3. Artist chính phải ACTIVE và còn role ARTIST → nếu không: "Artist account is not active".
    4. releaseAt đang null (Artist muốn phát hành ngay) → gán bằng thời điểm duyệt.
       releaseAt có giá trị (Artist đã hẹn lịch) → giữ nguyên.
    5. Xóa lý do từ chối cũ (rejectReason = null).
    6. Đổi trạng thái → APPROVED.
    7. songRepository.save(song) → Hibernate chạy câu:
           UPDATE songs SET ..., version = version + 1 WHERE id = 15 AND version = <version lúc đọc>
       Artist sửa hoặc xóa bài đúng lúc Admin duyệt → version đã đổi → không khớp dòng nào
       → ObjectOptimisticLockingFailureException → 409 (Optimistic Locking, xem Song.version).

    Lỗi nghiệp vụ (bước 1 đến 3) được ném dạng IllegalArgumentException
    → GlobalExceptionHandler trả 400 { "message": "..." }.

    =====================================================================
    (13) SongMapper CHUYỂN ENTITY THÀNH DTO
    =====================================================================

        SongMapper.toManageResponse(song)

    Chuyển entity Song → SongManageResponse, gồm:
        id, title, audioUrl, coverUrl, durationSeconds, playCount,
        status, released, releaseAt, rejectReason,
        artistId, artistName, featuredArtists, createdAt, updatedAt.

    released được tính sẵn: APPROVED và releaseAt <= bây giờ
    → frontend biết bài "Đã phát hành" hay "Đã lên lịch" mà không cần tự so thời gian.

    Vì sao không trả thẳng entity Song?
    - Song có quan hệ LAZY tới User (artist, featuredArtists). Trả thẳng entity:
      + Jackson gặp đối tượng "proxy" của Hibernate (User chưa được tải thật)
        → dễ báo lỗi khi chuyển sang JSON.
      + Nếu chuyển được, JSON sẽ kèm cả object User: email, họ tên, trạng thái tài khoản...
        (mật khẩu thì không, vì trường password trong User đã có @JsonIgnore chặn lại).
    - DTO chỉ chứa đúng những gì frontend cần, đổi database không làm vỡ frontend.

    =====================================================================
    (14) ResponseEntity VÀ JACKSON TRẢ JSON
    =====================================================================

        ResponseEntity.ok(dto) → HTTP 200 OK.

    Jackson đọc các getter của DTO (getTitle() → "title", isReleased() → "released")
    và tạo JSON:

    {
        "id": 15,
        "title": "Chiều Mưa",
        "status": "APPROVED",
        "released": true,
        "releaseAt": "2026-10-14T00:05:00",
        "rejectReason": null,
        "artistId": 5,
        "artistName": "Mây Chiều",
        "featuredArtists": [ { "id": 12, "artistName": "Sóng Nhỏ" } ],
        ...
    }

    Frontend nhận HTTP 200 và JSON, cập nhật giao diện (VD đổi nhãn bài thành "Đã phát hành").

    =====================================================================
    (15) TÓM TẮT LUỒNG XỬ LÝ
    =====================================================================

    Frontend gửi JWT
        -> JwtAuthenticationFilter kiểm tra chữ ký, hạn dùng của JWT
        -> Lấy email và sessionId từ JWT
        -> SessionService đối chiếu sessionId với Redis
        -> CustomUserDetailsService tải User (kèm role mới nhất) từ MySQL
        -> Tài khoản ACTIVE → lưu Authentication vào SecurityContext
        -> RateLimitFilter đếm request (vượt → 429)
        -> SecurityConfig: chưa xác thực → 401
        -> DispatcherServlet chọn method approveSong
        -> @PreAuthorize: không phải ADMIN → 403
        -> Controller gọi Service
        -> Service kiểm tra nghiệp vụ, lưu MySQL (lỗi nghiệp vụ → 400, xung đột → 409)
        -> SongMapper chuyển entity thành DTO
        -> ResponseEntity.ok → HTTP 200
        -> Jackson chuyển DTO thành JSON
        -> Frontend nhận kết quả

    Mọi lỗi đều trả về cùng 1 dạng JSON: { "message": "..." }
    → frontend luôn đọc lỗi bằng error.response.data.message.
*/
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/admin/songs")
public class AdminSongController {
    private final AdminSongService adminSongService;

    public AdminSongController(AdminSongService adminSongService) {
        this.adminSongService = adminSongService;
    }

    /*
        ---------------------------------------------------------------
        UC34 - Danh sách bài hát (có phân trang, lọc theo trạng thái).
        ---------------------------------------------------------------
        Ví dụ gọi:
        GET /api/admin/songs                          → mọi bài (trừ đã xóa), trang đầu
        GET /api/admin/songs?status=PENDING           → chỉ bài đang chờ duyệt
        GET /api/admin/songs?status=APPROVED&page=1   → bài đã duyệt, trang thứ 2

        Luồng xử lý:
        1. adminSongService.getSongs(...)
           → Page<Song>: bài của trang này + tổng số bài, tổng số trang.
           Sắp xếp mới tải lên trước (createdAt giảm dần) để bài chờ duyệt mới nhất nằm trên.

        2. SongMapper.toPageResponse(page, SongMapper::toManageResponse)
           → chuyển từng Song trong trang thành SongManageResponse,
             đóng gói cùng thông tin phân trang vào PageResponse.

        3. ResponseEntity.ok(...) → HTTP 200 kèm JSON:
           { "content": [...], "page": 0, "size": 20, "totalElements": 35, "totalPages": 2 }
    */
    @GetMapping
    public ResponseEntity<PageResponse<SongManageResponse>> getSongs(
            @RequestParam(required = false) SongStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        /*
            Nhận kết quả phân trang từ Service và phương thức chuyển đổi DTO.

            Chuyển từng Song thành SongManageResponse.

            Đóng gói danh sách DTO cùng thông tin phân trang vào PageResponse.
        */
        return ResponseEntity.ok(SongMapper.toPageResponse(
                /*
                    Gọi Service để lấy danh sách bài hát theo trạng thái status,
                    kết quả là một đối tượng Page<Song> chứa danh sách
                    bài hát và thông tin phân trang.

                    SongMapper::toManageResponse chuyển từng đối tượng Song thành SongManageResponse (DTO)
                    để trả dữ liệu phù hợp cho frontend.
                */
                adminSongService.getSongs(status, page, size),
                SongMapper::toManageResponse));
    }

    /*
        ---------------------------------------------------------------
        UC35 - Duyệt bài.
        ---------------------------------------------------------------
        Ví dụ gọi:
        PUT /api/admin/songs/15/approve (không cần body).

        Luồng xử lý:
        - Bài phải đang PENDING.

        - Artist chính phải còn ACTIVE và còn role ARTIST.

        - releaseAt đang null (Artist muốn phát hành ngay) → gán = thời điểm duyệt.

        - releaseAt có giá trị (Artist đã hẹn lịch) → giữ nguyên lịch đó.

        - Xóa lý do từ chối cũ (nếu bài từng bị từ chối rồi gửi lại).

        - Đổi trạng thái sang APPROVED.

        JSON trả về: 1 SongManageResponse với "status": "APPROVED".
        "released": true  → đã tới giờ, người nghe thấy ngay.
        "released": false → đang chờ tới lịch phát hành.
    */
    @PutMapping("/{songId}/approve")
    public ResponseEntity<SongManageResponse> approveSong(@PathVariable Long songId) {
        return ResponseEntity.ok(SongMapper.toManageResponse(adminSongService.approveSong(songId)));
    }

    /*
        ---------------------------------------------------------------
        UC35 - Từ chối bài đang chờ duyệt.
        ---------------------------------------------------------------
    */
    @PutMapping("/{songId}/reject")
    public ResponseEntity<SongManageResponse> rejectSong(@PathVariable Long songId,
                                                         @Valid @RequestBody RejectSongRequest request) {
        return ResponseEntity.ok(SongMapper.toManageResponse(
                adminSongService.rejectSong(songId, request.getReason())));
    }

    /*
        ---------------------------------------------------------------
        UC36 - Gỡ bài đã duyệt (kể cả bài Artist đang ẩn).
        ---------------------------------------------------------------
        Dùng lại RejectSongRequest vì cấu trúc giống hệt (chỉ cần 1 trường reason),
        không cần tạo thêm DTO mới.
    */
    @PutMapping("/{songId}/remove")
    public ResponseEntity<SongManageResponse> removeSong(@PathVariable Long songId,
                                                         @Valid @RequestBody RejectSongRequest request) {
        return ResponseEntity.ok(SongMapper.toManageResponse(
                adminSongService.removeSong(songId, request.getReason())));
    }
}