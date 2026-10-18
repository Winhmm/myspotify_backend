package com.winhmm.myspotify.controller;

import com.winhmm.myspotify.dto.user.*;
import com.winhmm.myspotify.entity.User;
import com.winhmm.myspotify.mapper.UserMapper;
import com.winhmm.myspotify.service.SessionService;
import com.winhmm.myspotify.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/*
    =====================================================
    USERCONTROLLER - API TÀI KHOẢN CÁ NHÂN (UC06 - UC14, UC19)
    =====================================================

    Các API trong class:
        POST /api/users/logout                          → đăng xuất
        GET  /api/users/me/profile                      → xem thông tin cá nhân
        PUT  /api/users/me/profile                      → sửa họ tên
        POST /api/users/me/change-password/request-otp  → đổi mật khẩu (bước 1: gửi OTP)
        PUT  /api/users/me/change-password              → đổi mật khẩu (bước 2: nhập OTP)
        POST /api/users/me/avatar                       → đổi ảnh đại diện
        POST /api/users/me/email/request-otp            → đổi email (bước 1: gửi OTP tới email mới)
        PUT  /api/users/me/email                        → đổi email (bước 2: nhập OTP)
        PUT  /api/users/me/disable                      → tự vô hiệu hóa tài khoản
        PUT  /api/users/me/delete                       → tự xóa tài khoản (xóa mềm)
        POST /api/users/me/artist-request               → gửi yêu cầu trở thành Artist
        PUT  /api/users/me/artist-profile               → sửa nghệ danh và giới thiệu (chỉ Artist)

    -----------------------------------------------------
    Ai được gọi?
    -----------------------------------------------------
    Mọi tài khoản đã đăng nhập: USER, ARTIST, ADMIN.
    - Không có @PreAuthorize → không giới hạn role.
    - Nhưng "/api/users/**" không nằm trong permitAll() của SecurityConfig
      → bắt buộc có token hợp lệ, nếu không → 401.
    Một số API giới hạn role ở tầng Service:
    - Gửi yêu cầu làm Artist: chỉ role USER.
    - Sửa hồ sơ Artist: chỉ role ARTIST.

    -----------------------------------------------------
    "me" nghĩa là gì? Vì sao URL không có userId?
    -----------------------------------------------------
    Mọi API đều thao tác trên tài khoản của chính người đang đăng nhập.
    Id người dùng được lấy từ token (qua email), không lấy từ URL hay body.

    Nếu thiết kế URL dạng PUT /api/users/{userId}/profile,
    kẻ gian chỉ cần đổi số 6 thành số 7 là sửa được hồ sơ người khác
    (lỗi bảo mật IDOR - Insecure Direct Object Reference).
    Dùng "/me" và lấy id từ token thì không có cách nào giả mạo,
    vì token có chữ ký, không sửa được.

    -----------------------------------------------------
    Tham số Authentication
    -----------------------------------------------------
    Spring tự truyền vào thông tin người đang đăng nhập.
    Thông tin này do JwtAuthenticationFilter đặt vào SecurityContext khi kiểm tra token.
    authentication.getName() trả về email của người đang đăng nhập.

    -----------------------------------------------------
    Chuyển entity sang DTO
    -----------------------------------------------------
    Các API trả thông tin cá nhân dùng UserMapper.toUserProfileResponse()
    (dùng chung với AdminController), để không trả trường password ra ngoài.
*/
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final SessionService sessionService;

    public UserController(UserService userService, SessionService sessionService) {
        this.userService = userService;
        this.sessionService = sessionService;
    }

    /*
        -----------------------------------------------------
        Lấy id của người đang đăng nhập
        -----------------------------------------------------
        authentication.getName() = email trong token
        → tìm User theo email → lấy id.

        Hàm private, dùng chung cho mọi API bên dưới.
        Email không còn trong database (rất hiếm) → 400 "User does not exist".
    */
    private Long me(Authentication authentication) {
        return userService.getByEmail(authentication.getName()).getId();
    }

    /*
        =====================================================
        UC06 - ĐĂNG XUẤT
        =====================================================

        Ví dụ gọi:
            POST /api/users/logout      (không cần body, chỉ cần header Authorization)

        Xóa phiên trong Redis (key session:{email}).
        → Token hiện tại hết hiệu lực ngay lập tức, dù vẫn còn hạn 24 giờ.
        → Ai lấy được token này cũng không dùng được nữa (401).

        Kết quả: 200 { "message": "Logout successful" }

        Frontend: xóa token đã lưu và chuyển về trang đăng nhập.
        Backend không xóa được token nằm trong trình duyệt, chỉ làm nó mất hiệu lực.
    */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(Authentication authentication) {
        sessionService.deleteSession(authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }

    /*
        =====================================================
        UC07 - XEM THÔNG TIN CÁ NHÂN
        =====================================================

        Ví dụ gọi:
            GET /api/users/me/profile

        Kết quả: 200
            {
                "id": 6,
                "username": "maychieu",
                "email": "maychieu@gmail.com",
                "fullName": "Trần Mây Chiều",
                "avatarUrl": "/uploads/avatars/3f2c8a1e.jpg",
                "accountStatus": "ACTIVE",
                "role": "ARTIST",
                "artistRequestStatus": "APPROVED",
                "artistName": "Mây Chiều",
                "bio": "Ballad nhẹ nhàng cho buổi chiều."
            }

        Frontend:
        - Gọi API này khi mở ứng dụng để lấy role mới nhất
          (role trong response đăng nhập có thể đã cũ nếu Admin vừa đổi).
        - fullName, avatarUrl, artistName, bio có thể null → hiển thị giá trị mặc định.
        - artistRequestStatus = PENDING → hiện "Yêu cầu đang chờ duyệt".
    */
    @GetMapping("/me/profile")
    public ResponseEntity<UserProfileResponse> getProfile(Authentication authentication) {
        User user = userService.getProfile(me(authentication));
        return ResponseEntity.ok(UserMapper.toUserProfileResponse(user));
    }

    /*
        =====================================================
        UC08 - CẬP NHẬT THÔNG TIN CÁ NHÂN
        =====================================================

        Ví dụ gọi:
            PUT /api/users/me/profile
            {
                "fullName": "Trần Mây Chiều"
            }

        Chỉ cho sửa họ tên.
        - Username không cho đổi (là định danh cố định của tài khoản).
        - Email đổi qua API riêng có xác thực OTP (UC13).
        - Mật khẩu đổi qua API riêng có xác thực OTP (UC09).

        Kết quả: 200, trả về thông tin cá nhân mới (cùng cấu trúc với UC07)
        → frontend cập nhật giao diện luôn, không cần gọi lại GET.

        Lỗi có thể gặp:
            400 "Full name is required"
            400 "Full name must not exceed 100 characters"
    */
    @PutMapping("/me/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(Authentication authentication,
                                                             @Valid @RequestBody UpdateProfileRequest request) {
        User user = userService.updateProfile(me(authentication), request.getFullName());
        return ResponseEntity.ok(UserMapper.toUserProfileResponse(user));
    }

    /*
        =====================================================
        UC09 - ĐỔI MẬT KHẨU (BƯỚC 1: XÁC NHẬN MẬT KHẨU HIỆN TẠI)
        =====================================================

        Ví dụ gọi:
            POST /api/users/me/change-password/request-otp
            {
                "currentPassword": "matkhau123"
            }

        -----------------------------------------------------
        Vì sao cần cả mật khẩu hiện tại lẫn OTP?
        -----------------------------------------------------
        Bảo vệ 2 lớp:
        - Người mượn máy (token đang đăng nhập sẵn) không biết mật khẩu → bị chặn ở bước 1.
        - Người biết mật khẩu nhưng không vào được hộp thư email → bị chặn ở bước 2.

        Luồng xử lý (UserService.requestChangePasswordOtp):
        1. So mật khẩu hiện tại bằng BCrypt. Sai → 400 "Password does not match".
        2. Tạo OTP (mục đích CHANGE_PASSWORD) và gửi về email hiện tại.

        Kết quả: 200 { "message": "OTP code has been sent to your email" }

        Lỗi có thể gặp:
            400 "Current password is required"
            400 "Password does not match"
            400 "Please wait 60 seconds before requesting a new OTP"
            503 "Could not send OTP email, please try again later"
    */
    @PostMapping("/me/change-password/request-otp")
    public ResponseEntity<Map<String, String>> requestChangePasswordOtp(Authentication authentication,
                                                                        @Valid @RequestBody RequestChangePasswordOtpRequest request) {
        userService.requestChangePasswordOtp(me(authentication), request.getCurrentPassword());
        return ResponseEntity.ok(Map.of("message", "OTP code has been sent to your email"));
    }

    /*
        =====================================================
        UC09 - ĐỔI MẬT KHẨU (BƯỚC 2: NHẬP OTP VÀ MẬT KHẨU MỚI)
        =====================================================

        Ví dụ gọi:
            PUT /api/users/me/change-password
            {
                "otpCode": "529301",
                "newPassword": "matkhaumoi456"
            }

        Luồng xử lý (UserService.changePassword):
        1. Kiểm tra OTP mục đích CHANGE_PASSWORD.
        2. Băm mật khẩu mới bằng BCrypt rồi lưu.
        3. Xóa phiên trong Redis → đăng xuất khỏi mọi thiết bị, kể cả thiết bị đang dùng.

        Kết quả: 200 { "message": "Password has been changed successfully" }

        Lỗi có thể gặp:
            400 "OTP code must be 6 digits"
            400 "New password must be between 6 and 72 characters"
            400 "Otp code does not exist" / "Otp code has expired" / "Otp code does not match"
            400 "Too many wrong attempts, please request a new OTP"

        Frontend: thành công → xóa token, chuyển về trang đăng nhập.
        Nếu không chuyển, request tiếp theo cũng sẽ nhận 401 vì phiên đã bị xóa.
    */
    @PutMapping("/me/change-password")
    public ResponseEntity<Map<String, String>> changePassword(Authentication authentication,
                                                              @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(me(authentication), request.getOtpCode(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password has been changed successfully"));
    }

    /*
        =====================================================
        UC10 - CẬP NHẬT ẢNH ĐẠI DIỆN
        =====================================================

        Ví dụ gọi (Postman: Body → form-data):
            POST /api/users/me/avatar
            Key: file    Type: File    Value: chọn ảnh từ máy

        -----------------------------------------------------
        Vì sao dùng multipart/form-data thay vì JSON?
        -----------------------------------------------------
        JSON chỉ chứa văn bản, không gửi trực tiếp file nhị phân (ảnh, nhạc) được.
        multipart/form-data chia request thành nhiều "phần", mỗi phần có thể là 1 file.

        - consumes = MULTIPART_FORM_DATA_VALUE: API này chỉ nhận multipart/form-data.
          Gửi JSON → 415 Unsupported Media Type.
        - @RequestParam("file"): lấy phần có tên "file" trong form.
          Thiếu → 400 "File is required".

        -----------------------------------------------------
        Luồng xử lý (UserService.updateAvatar)
        -----------------------------------------------------
        1. FileStorageService.saveImage() kiểm tra và lưu ảnh:
           - Chỉ nhận JPG, PNG, WEBP thật (kiểm tra vài byte đầu của file,
             không chỉ tin vào đuôi file, vì đuôi file đổi được dễ dàng).
           - Tối đa 2MB.
           - Đặt tên file mới bằng UUID ngẫu nhiên, lưu vào thư mục avatars.
        2. Lưu đường dẫn mới vào database.
        3. Lưu thành công → xóa ảnh cũ trên ổ đĩa (tránh đầy ổ).
           Lưu thất bại → xóa ảnh mới vừa lưu, giữ nguyên ảnh cũ.

        Kết quả: 200, trả về thông tin cá nhân mới, có avatarUrl mới.

        Lỗi có thể gặp:
            400 "File is required"
            400 → file không phải ảnh hợp lệ hoặc vượt quá 2MB
            413 "File is too large" → vượt giới hạn upload của server

        Frontend: hiển thị ảnh bằng địa chỉ server + avatarUrl,
        ví dụ http://157.66.100.87:8080/uploads/avatars/3f2c8a1e.jpg
    */
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserProfileResponse> updateAvatar(Authentication authentication,
                                                            @RequestParam("file") MultipartFile file) {
        User user = userService.updateAvatar(me(authentication), file);
        return ResponseEntity.ok(UserMapper.toUserProfileResponse(user));
    }

    /*
        =====================================================
        UC13 - CẬP NHẬT EMAIL (BƯỚC 1: GỬI OTP TỚI EMAIL MỚI)
        =====================================================

        Ví dụ gọi:
            POST /api/users/me/email/request-otp
            {
                "newEmail": "maychieu.new@gmail.com"
            }

        -----------------------------------------------------
        Vì sao gửi OTP tới email mới?
        -----------------------------------------------------
        Để chứng minh người dùng thật sự sở hữu email mới.
        Nếu không kiểm tra, người dùng gõ nhầm 1 ký tự sẽ mất tài khoản
        (OTP quên mật khẩu sau này sẽ gửi tới 1 email không ai đọc).

        Luồng xử lý (UserService.requestUpdateEmailOtp):
        1. Email mới phải khác email hiện tại (không phân biệt hoa thường).
        2. Email mới chưa được tài khoản nào dùng.
        3. Tạo OTP (mục đích CHANGE_EMAIL), lưu email mới vào bản ghi OTP,
           rồi gửi mã tới email mới.
           Email mới được lưu tạm trong OTP, chưa đổi trong bảng users.

        Kết quả: 200 { "message": "OTP code has been sent to your new email" }

        Lỗi có thể gặp:
            400 "Invalid email"
            400 "New email must be different from current email"
            400 "Email already exists"
            400 "Please wait 60 seconds before requesting a new OTP"
            503 "Could not send OTP email, please try again later"
    */
    @PostMapping("/me/email/request-otp")
    public ResponseEntity<Map<String, String>> requestUpdateEmailOtp(Authentication authentication,
                                                                     @Valid @RequestBody RequestUpdateEmailOtpRequest request) {
        userService.requestUpdateEmailOtp(me(authentication), request.getNewEmail());
        return ResponseEntity.ok(Map.of("message", "OTP code has been sent to your new email"));
    }

    /*
        =====================================================
        UC13 - CẬP NHẬT EMAIL (BƯỚC 2: NHẬP OTP)
        =====================================================

        Ví dụ gọi:
            PUT /api/users/me/email
            {
                "otpCode": "118204"
            }

        Body không cần gửi lại email mới, vì email mới đã được lưu trong bản ghi OTP ở bước 1.
        → Người dùng không thể đổi sang 1 email khác với email đã nhận OTP.

        Luồng xử lý (UserService.updateEmail):
        1. Kiểm tra OTP mục đích CHANGE_EMAIL.
        2. Kiểm tra lại email mới chưa bị ai dùng
           (có thể có người vừa đăng ký email đó trong lúc chờ nhập OTP).
        3. Xóa phiên của email cũ trong Redis (key session:{email cũ}).
        4. Đổi email trong database.

        Kết quả: 200 { "message": "Email has been updated, please login again" }

        Vì sao phải đăng nhập lại?
        Token hiện tại chứa email cũ. Phiên của email cũ đã bị xóa,
        nên token cũ không dùng được nữa (401).

        Lỗi có thể gặp:
            400 "OTP code must be 6 digits"
            400 "Otp code does not exist" / "Otp code has expired" / "Otp code does not match"
            400 "Email already exists"

        Frontend: thành công → xóa token, chuyển về trang đăng nhập để đăng nhập bằng email mới.
    */
    @PutMapping("/me/email")
    public ResponseEntity<Map<String, String>> updateEmail(Authentication authentication,
                                                           @Valid @RequestBody UpdateEmailRequest request) {
        userService.updateEmail(me(authentication), request.getOtpCode());
        return ResponseEntity.ok(Map.of("message", "Email has been updated, please login again"));
    }

    /*
        =====================================================
        UC11 - VÔ HIỆU HÓA TÀI KHOẢN
        =====================================================

        Ví dụ gọi:
            PUT /api/users/me/disable
            {
                "password": "matkhau123"
            }

        Người dùng tự tạm khóa tài khoản của mình.
        Phải nhập lại mật khẩu để xác nhận, tránh người mượn máy khóa tài khoản giúp.

        Luồng xử lý (UserService.disableAccount):
        1. Tài khoản phải đang ACTIVE.
        2. Mật khẩu phải đúng.
        3. Chuyển accountStatus sang DISABLED.
        4. Xóa phiên trong Redis → đăng xuất ngay.

        Sau khi vô hiệu hóa:
        - Không đăng nhập được: 400 "Account has been disabled".
        - Nếu là Artist: toàn bộ bài hát tự ẩn khỏi người nghe
          (điều kiện phát hành yêu cầu Artist phải ACTIVE).
        - Muốn dùng lại: liên hệ Admin mở khóa (PUT /api/admin/users/{userId}/unlock).

        Kết quả: 200 { "message": "Account has been disabled" }

        Lỗi có thể gặp:
            400 "Password is required"
            400 "Password does not match"
    */
    @PutMapping("/me/disable")
    public ResponseEntity<Map<String, String>> disableAccount(Authentication authentication,
                                                              @Valid @RequestBody AccountActionRequest request) {
        userService.disableAccount(me(authentication), request.getPassword());
        return ResponseEntity.ok(Map.of("message", "Account has been disabled"));
    }

    /*
        =====================================================
        UC12 - XÓA TÀI KHOẢN (XÓA MỀM)
        =====================================================

        Ví dụ gọi:
            PUT /api/users/me/delete
            {
                "password": "matkhau123"
            }

        -----------------------------------------------------
        Xóa mềm (soft delete) là gì?
        -----------------------------------------------------
        Không xóa dòng dữ liệu khỏi bảng users,
        mà chỉ đổi accountStatus sang DELETED.

        Vì sao không xóa hẳn?
        - Bài hát, lượt nghe, OTP... đều liên kết với user_id.
          Xóa hẳn User sẽ làm lỗi khóa ngoại hoặc mất dữ liệu liên quan.
        - Giữ lại để tra cứu khi cần (khiếu nại, thống kê).

        Dùng PUT thay vì DELETE vì đây thực chất là cập nhật trạng thái,
        và cần gửi body chứa mật khẩu (DELETE thường không gửi body).

        Luồng xử lý giống UC11, chỉ khác trạng thái là DELETED.

        Sau khi xóa:
        - Không đăng nhập được: 400 "Account has been deleted".
        - Admin không mở khóa được (chỉ mở khóa tài khoản DISABLED).
        - Email và username vẫn bị giữ, không dùng để đăng ký tài khoản mới được.

        Kết quả: 200 { "message": "Account has been deleted" }

        Lỗi có thể gặp:
            400 "Password is required"
            400 "Password does not match"
    */
    @PutMapping("/me/delete")
    public ResponseEntity<Map<String, String>> deleteAccount(Authentication authentication,
                                                             @Valid @RequestBody AccountActionRequest request) {
        userService.deleteAccount(me(authentication), request.getPassword());
        return ResponseEntity.ok(Map.of("message", "Account has been deleted"));
    }

    /*
        =====================================================
        UC14 - GỬI YÊU CẦU TRỞ THÀNH ARTIST
        =====================================================

        Ví dụ gọi:
            POST /api/users/me/artist-request
            {
                "artistName": "Mây Chiều",
                "bio": "Ballad nhẹ nhàng cho buổi chiều."
            }

        -----------------------------------------------------
        Quy trình trở thành Artist
        -----------------------------------------------------
        1. User gửi yêu cầu kèm nghệ danh  → artistRequestStatus = PENDING
        2. Admin xem danh sách chờ duyệt    → GET /api/admin/artist-requests
        3a. Admin duyệt                      → role = ARTIST, artistRequestStatus = APPROVED
        3b. Admin từ chối                    → artistRequestStatus = REJECTED, xóa nghệ danh và bio
                                              → User được gửi lại yêu cầu mới

        Luồng xử lý (UserService.requestBecomeArtist):
        1. Tài khoản phải ACTIVE.
        2. Chỉ role USER được gửi (ARTIST rồi thì không cần, ADMIN thì không được).
        3. artistRequestStatus phải là NONE (chưa gửi bao giờ) hoặc REJECTED (từng bị từ chối).
           Đang PENDING hoặc đã APPROVED → báo lỗi, tránh gửi trùng.
        4. Lưu nghệ danh, bio, chuyển artistRequestStatus sang PENDING.

        Kết quả: 200 { "message": "Artist request has been submitted" }

        Lỗi có thể gặp:
            400 "Artist name is required"
            400 "Only users with role USER can request to become an artist"
            400 "An artist request already exists or has been approved"
    */
    @PostMapping("/me/artist-request")
    public ResponseEntity<Map<String, String>> requestBecomeArtist(Authentication authentication,
                                                                   @Valid @RequestBody ArtistRequest request) {
        userService.requestBecomeArtist(me(authentication), request.getArtistName(), request.getBio());
        return ResponseEntity.ok(Map.of("message", "Artist request has been submitted"));
    }

    /*
        =====================================================
        UC19 - CẬP NHẬT HỒ SƠ ARTIST
        =====================================================

        Ví dụ gọi:
            PUT /api/users/me/artist-profile
            {
                "artistName": "Mây Chiều Official",
                "bio": "Ballad và lofi."
            }

        Dùng chung DTO ArtistRequest với UC14 vì có cùng 2 trường và cùng quy tắc kiểm tra.

        Chỉ role ARTIST được dùng (kiểm tra trong UserService.updateArtistProfile).
        Nghệ danh mới hiện ngay trên mọi bài hát của Artist,
        vì bài hát chỉ lưu artist_id, còn tên được lấy từ bảng users mỗi lần truy vấn.

        Kết quả: 200, trả về thông tin cá nhân mới.

        Lỗi có thể gặp:
            400 "Artist name is required"
            400 "Only artists can update artist profile"
    */
    @PutMapping("/me/artist-profile")
    public ResponseEntity<UserProfileResponse> updateArtistProfile(Authentication authentication,
                                                                   @Valid @RequestBody ArtistRequest request) {
        User user = userService.updateArtistProfile(me(authentication), request.getArtistName(), request.getBio());
        return ResponseEntity.ok(UserMapper.toUserProfileResponse(user));
    }
}