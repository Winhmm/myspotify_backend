package com.winhmm.myspotify.dto.auth;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    (package, import, DTO, annotation, validation là gì: xem giải thích chi tiết trong RegisterRequest.)

    - Email   : giá trị phải đúng định dạng email.
    - NotBlank: không được null, rỗng hoặc chỉ toàn dấu cách.
*/
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/*
    =====================================================
    FORGOTPASSWORDREQUEST - QUÊN MẬT KHẨU (BƯỚC 1: XIN MÃ OTP)
    =====================================================
    Dùng cho: POST /api/auth/forgot-password (UC04, xem AuthController)

    JSON frontend gửi lên:
        {
            "email": "maychieu@gmail.com"
        }

    -----------------------------------------------------
    Quy trình quên mật khẩu gồm 2 bước
    -----------------------------------------------------
        Bước 1 (file này)      : gửi email → backend gửi mã OTP tới email đó.
        Bước 2 (ResetPasswordRequest): gửi email + mã OTP + mật khẩu mới → đặt lại mật khẩu.

    Vì sao không cho đặt lại mật khẩu ngay ở bước 1?
    Ai cũng biết email của người khác. Nếu chỉ cần email là đổi được mật khẩu,
    kẻ gian sẽ chiếm được mọi tài khoản. Mã OTP gửi vào hộp thư chứng minh
    người yêu cầu thật sự là chủ email.

    -----------------------------------------------------
    Backend làm gì sau khi nhận email? (AuthService.forgotPassword)
    -----------------------------------------------------
    1. Tìm tài khoản theo email. Không có → 400 "Email does not exist".
    2. Tài khoản phải đang hoạt động (ACTIVE). Chưa xác thực, bị khóa, đã xóa → 400 "Account is not active".
    3. Tạo mã OTP mới (mục đích RESET_PASSWORD), gửi tới email.
       Các mã RESET_PASSWORD cũ chưa dùng bị vô hiệu, chỉ mã mới nhất dùng được.

    Chống spam:
    - Phải chờ 60 giây giữa 2 lần xin mã → 400 "Please wait 60 seconds before requesting a new OTP".
    - Rate limit: tối đa 10 request mỗi phút cho mỗi IP trên mọi API /api/auth/**.
*/
public class ForgotPasswordRequest {
    /*
        -----------------------------------------------------
        email: email của tài khoản quên mật khẩu
        -----------------------------------------------------
        DTO chỉ có đúng 1 thuộc tính, vì bước này chỉ cần biết gửi mã OTP tới email nào.

        - @NotBlank: bắt buộc phải có → "Email is required".
        - @Email   : đúng định dạng email → "Invalid email".
        Ý nghĩa 2 annotation: xem RegisterRequest.

        Lưu ý thiết kế: message "Email does not exist" (ở AuthService) cho người ngoài biết
        1 email có đăng ký trong hệ thống hay không. Đây là đánh đổi có chủ đích:
        người dùng biết ngay mình gõ sai email, thay vì chờ mãi 1 email không bao giờ tới.
        (Khác với đăng nhập, nơi sai email và sai mật khẩu báo chung 1 message, xem LoginRequest.)
    */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email")
    private String email;

    /*
        -----------------------------------------------------
        Getter và setter
        -----------------------------------------------------
        - Setter: Jackson gọi khi đổi JSON thành đối tượng ("email" → setEmail).
        - Getter: AuthController gọi request.getEmail() để truyền xuống AuthService.forgotPassword.
        Cách đọc từng dòng: xem RegisterRequest.
    */
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}