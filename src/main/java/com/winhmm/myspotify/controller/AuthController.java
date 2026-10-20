package com.winhmm.myspotify.controller;

/*
    =====================================================
    AUTHCONTROLLER - API XÁC THỰC TÀI KHOẢN (UC01 - UC05)
    =====================================================

    Các API trong class:
        POST /api/auth/register          → đăng ký, gửi OTP về email
        POST /api/auth/verify-otp        → xác thực OTP, kích hoạt tài khoản
        POST /api/auth/login             → đăng nhập, nhận token
        POST /api/auth/forgot-password   → quên mật khẩu, gửi OTP về email
        POST /api/auth/reset-password    → đặt lại mật khẩu bằng OTP

    -----------------------------------------------------
    Ai được gọi?
    -----------------------------------------------------
    Tất cả mọi người, kể cả người chưa đăng nhập.
    Trong SecurityConfig, "/api/auth/**" nằm trong permitAll(),
    vì người dùng chưa có token mới cần gọi các API này.
    Không có @PreAuthorize trên class → không giới hạn role.

    -----------------------------------------------------
    Bảo vệ chống dò mật khẩu và spam
    -----------------------------------------------------
    - Rate limit (RateLimitFilter): tối đa 10 request mỗi 60 giây cho mỗi IP.
      Vượt quá → 429 Too Many Requests.
      → Kẻ gian muốn thử 1 triệu mật khẩu sẽ mất gần 70 ngày cho 1 IP.
    - OTP (OtpService):
        + Chờ 60 giây giữa 2 lần yêu cầu mã mới.
        + Sai tối đa 5 lần cho mỗi mã.
        + Mã hết hạn sau 5 phút và chỉ dùng được 1 lần.

    -----------------------------------------------------
    Vai trò của Controller
    -----------------------------------------------------
    Controller chỉ làm 3 việc:
    1. Nhận JSON từ frontend, Spring tự chuyển thành đối tượng DTO (@RequestBody).
    2. Kiểm tra dữ liệu đầu vào (@Valid): thiếu trường, sai định dạng → 400 ngay,
       không chạy xuống Service.
    3. Gọi AuthService xử lý nghiệp vụ, rồi trả kết quả về frontend.
    Mọi quy tắc nghiệp vụ (email trùng, OTP sai, tài khoản bị khóa...) nằm trong AuthService.

    -----------------------------------------------------
    Định dạng lỗi
    -----------------------------------------------------
    Mọi lỗi đều trả về cùng 1 dạng: { "message": "..." }
    - Dữ liệu không hợp lệ (@Valid)              → 400, message lấy từ annotation trong DTO.
    - Lỗi nghiệp vụ (IllegalArgumentException)   → 400.
    - Gửi email OTP thất bại (EmailSendException) → 503.
    - Trùng dữ liệu do 2 request cùng lúc         → 409 "Data already exists".
    Tất cả được xử lý tập trung trong GlobalExceptionHandler.

    -----------------------------------------------------
    Các annotation trên class
    -----------------------------------------------------
    - @RestController: class này nhận request HTTP và trả về dữ liệu JSON
      (giá trị trả về của mỗi hàm được Spring tự chuyển thành JSON).
    - @RequestMapping("/api/auth"): mọi đường dẫn trong class đều bắt đầu bằng /api/auth.
*/
import com.winhmm.myspotify.dto.auth.*;
import com.winhmm.myspotify.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /*
        =====================================================
        UC01 - ĐĂNG KÝ TÀI KHOẢN
        =====================================================

        Ví dụ gọi:
            POST /api/auth/register
            {
                "username": "maychieu",
                "email": "maychieu@gmail.com",
                "password": "matkhau123"
            }

        -----------------------------------------------------
        Luồng xử lý (AuthService.register)
        -----------------------------------------------------
        Trường hợp 1: email chưa tồn tại
            → Tạo User mới: accountStatus = UNVERIFIED, role = USER.
            → Mật khẩu được băm bằng BCrypt trước khi lưu.
            → Tạo mã OTP (mục đích REGISTER) và gửi về email.

        Trường hợp 2: email đã tồn tại nhưng chưa xác thực (UNVERIFIED)
            Ví dụ: người dùng đăng ký hôm qua nhưng chưa nhập OTP, mã đã hết hạn.
            → Cập nhật username và mật khẩu mới.
            → Gửi lại OTP mới.
            Đây cũng chính là cách "gửi lại OTP đăng ký":
            frontend chỉ cần gọi lại API này với cùng thông tin.

        Trường hợp 3: email đã được xác thực
            → 400 "Email already exists".

        Gửi email thất bại → 503 và toàn bộ thay đổi bị hủy (rollback),
        người dùng đăng ký lại được ngay với cùng email.

        -----------------------------------------------------
        Kết quả
        -----------------------------------------------------
        200 { "message": "Registration successful, the account is awaiting OTP verification" }
        Thời gian phản hồi khoảng 2 đến 5 giây, vì phải chờ Gmail gửi xong email.

        Lỗi có thể gặp:
            400 "Username is required", "Invalid email", ...   → sai dữ liệu đầu vào
            400 "Email already exists"
            400 "Username already exists"
            400 "Please wait 60 seconds before requesting a new OTP"
            503 "Could not send OTP email, please try again later"

        Frontend: đăng ký thành công → chuyển sang màn hình nhập OTP,
        giữ lại email để gửi kèm ở bước xác thực.
    */
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request.getUsername(), request.getEmail(), request.getPassword());
        return ResponseEntity.ok(Map.of("message", "Registration successful, the account is awaiting OTP verification"));
    }

    /*
        =====================================================
        UC02 - XÁC THỰC OTP ĐĂNG KÝ
        =====================================================

        Ví dụ gọi:
            POST /api/auth/verify-otp
            {
                "email": "maychieu@gmail.com",
                "otpCode": "482913"
            }

        -----------------------------------------------------
        Luồng xử lý (AuthService.verifyOtp)
        -----------------------------------------------------
        1. Tìm User theo email. Không có → 400 "Email does not exist".
        2. Tài khoản phải đang UNVERIFIED. Đã kích hoạt rồi → 400 "Account is already verified".
        3. Kiểm tra OTP (OtpService.verifyAndConsume):
           - Lấy mã mới nhất chưa dùng.
           - Còn hạn 5 phút không.
           - Đã sai quá 5 lần chưa (đếm bằng Redis).
           - Mã có khớp không.
           - Khớp → đánh dấu mã đã dùng, không dùng lại được nữa.
        4. Chuyển accountStatus sang ACTIVE.

        -----------------------------------------------------
        Kết quả
        -----------------------------------------------------
        200 { "message": "Verification successful, the account has been activated" }

        Lỗi có thể gặp:
            400 "OTP code must be 6 digits"
            400 "Email does not exist"
            400 "Account is already verified"
            400 "Otp code does not exist"
            400 "Otp code has expired"
            400 "Otp code does not match"
            400 "Too many wrong attempts, please request a new OTP"

        Frontend: xác thực thành công không tự đăng nhập.
        Chuyển người dùng sang màn hình đăng nhập.
    */
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, String>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyOtp(request.getEmail(), request.getOtpCode());
        return ResponseEntity.ok(Map.of("message", "Verification successful, the account has been activated"));
    }

    /*
        =====================================================
        UC03 - ĐĂNG NHẬP
        =====================================================

        Ví dụ gọi:
            POST /api/auth/login
            {
                "email": "maychieu@gmail.com",
                "password": "matkhau123"
            }

        -----------------------------------------------------
        Luồng xử lý (AuthService.login)
        -----------------------------------------------------
        1. Tìm User theo email.
        2. So mật khẩu bằng BCrypt (passwordEncoder.matches).
        3. Kiểm tra trạng thái tài khoản: UNVERIFIED, DISABLED, DELETED → báo lỗi.
        4. Tạo phiên mới trong Redis (key session:{email}, giá trị là 1 UUID).
           Nếu đang đăng nhập ở thiết bị khác, phiên cũ bị ghi đè
           → thiết bị cũ bị đăng xuất ở request tiếp theo (401).
           → Mỗi tài khoản chỉ đăng nhập được ở 1 nơi tại 1 thời điểm.
        5. Tạo JWT chứa email (sub) và mã phiên (jti), sống 7 ngày
           (thời hạn lấy từ cấu hình JWT trong application.properties).

        -----------------------------------------------------
        Vì sao sai email và sai mật khẩu báo cùng 1 message?
        -----------------------------------------------------
        Nếu báo riêng "Email không tồn tại" và "Sai mật khẩu",
        kẻ gian thử nhiều email sẽ biết email nào đã đăng ký trong hệ thống.
        Báo chung "Email or password is incorrect" thì không đoán được.

        Các lỗi về trạng thái tài khoản (chưa xác thực, bị khóa, đã xóa) chỉ hiện
        sau khi nhập đúng mật khẩu → người ngoài không dò được trạng thái của tài khoản khác.

        -----------------------------------------------------
        Kết quả
        -----------------------------------------------------
        200
            {
                "token": "eyJhbGciOiJIUzI1NiJ9...",
                "userId": 6,
                "username": "maychieu",
                "role": "ARTIST"
            }

        Lỗi có thể gặp:
            400 "Email or password is incorrect"
            400 "Account is not verified yet"
            400 "Account has been disabled"
            400 "Account has been deleted"
            429 → đăng nhập quá 10 lần trong 1 phút từ cùng 1 IP

        -----------------------------------------------------
        Frontend cần làm gì?
        -----------------------------------------------------
        - Lưu token (localStorage hoặc bộ nhớ của ứng dụng).
        - Gửi kèm token ở mọi request sau:
              Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
        - Dùng role để hiện hoặc ẩn menu (Artist thấy "Quản lý bài hát", Admin thấy "Quản trị").
          Lưu ý: role trong response này chỉ đúng tại thời điểm đăng nhập.
          Nếu Admin đổi role sau đó, gọi GET /api/users/me/profile để lấy role mới nhất.
        - Nhận 401 ở bất kỳ API nào → xóa token, chuyển về trang đăng nhập.
    */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(response);
    }

    /*
        =====================================================
        UC04 - QUÊN MẬT KHẨU (BƯỚC 1: GỬI OTP)
        =====================================================

        Ví dụ gọi:
            POST /api/auth/forgot-password
            {
                "email": "maychieu@gmail.com"
            }

        -----------------------------------------------------
        Luồng xử lý (AuthService.forgotPassword)
        -----------------------------------------------------
        1. Tìm User theo email. Không có → 400 "Email does not exist".
        2. Tài khoản phải ACTIVE. Chưa xác thực, bị khóa hoặc đã xóa → 400 "Account is not active".
        3. Tạo OTP (mục đích RESET_PASSWORD) và gửi về email.
           Các mã RESET_PASSWORD cũ chưa dùng bị vô hiệu hóa, chỉ mã mới nhất dùng được.

        -----------------------------------------------------
        Kết quả
        -----------------------------------------------------
        200 { "message": "OTP code has been sent to your email" }

        Lỗi có thể gặp:
            400 "Email does not exist"
            400 "Account is not active"
            400 "Please wait 60 seconds before requesting a new OTP"
            503 "Could not send OTP email, please try again later"

        Lưu ý: message "Email does not exist" cho người ngoài biết 1 email có đăng ký hay không.
        Đây là sự đánh đổi có chủ đích: người dùng biết ngay mình gõ sai email,
        thay vì chờ mãi một email không bao giờ tới.

        Frontend: chuyển sang màn hình nhập OTP và mật khẩu mới,
        giữ lại email để gửi kèm ở bước 2.
    */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "OTP code has been sent to your email"));
    }

    /*
        =====================================================
        UC05 - ĐẶT LẠI MẬT KHẨU (BƯỚC 2: NHẬP OTP VÀ MẬT KHẨU MỚI)
        =====================================================

        Ví dụ gọi:
            POST /api/auth/reset-password
            {
                "email": "maychieu@gmail.com",
                "otpCode": "731044",
                "newPassword": "matkhaumoi456"
            }

        -----------------------------------------------------
        Luồng xử lý (AuthService.resetPassword)
        -----------------------------------------------------
        1. Tìm User theo email.
        2. Kiểm tra OTP mục đích RESET_PASSWORD (hạn dùng, số lần sai, có khớp không).
        3. Băm mật khẩu mới bằng BCrypt rồi lưu.
        4. Xóa phiên trong Redis → đăng xuất khỏi mọi thiết bị.
           Lý do: người dùng thường đặt lại mật khẩu vì nghi bị lộ.
           Nếu kẻ gian đang dùng tài khoản thì sẽ bị đá ra ngay.

        -----------------------------------------------------
        Kết quả
        -----------------------------------------------------
        200 { "message": "Password has been reset successfully" }

        Lỗi có thể gặp:
            400 "New password must be between 6 and 72 characters"
            400 "Email does not exist"
            400 "Otp code does not exist" / "Otp code has expired" / "Otp code does not match"
            400 "Too many wrong attempts, please request a new OTP"

        Frontend: thành công → chuyển về màn hình đăng nhập.
    */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getEmail(), request.getOtpCode(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password has been reset successfully"));
    }
}
