package com.winhmm.myspotify.dto.auth;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    (package, import, DTO, annotation, validation là gì: xem giải thích chi tiết trong RegisterRequest.)

    - Email   : giá trị phải đúng định dạng email.
    - NotBlank: không được null, rỗng hoặc chỉ toàn dấu cách.
    - Pattern : giá trị phải khớp 1 biểu thức chính quy (regex).
    - Size    : độ dài giá trị phải nằm trong khoảng cho phép.
*/
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/*
    =====================================================
    RESETPASSWORDREQUEST - ĐẶT LẠI MẬT KHẨU (BƯỚC 2: NHẬP OTP VÀ MẬT KHẨU MỚI)
    =====================================================
    Dùng cho: POST /api/auth/reset-password (UC05, xem AuthController)

    JSON frontend gửi lên:
        {
            "email": "maychieu@gmail.com",
            "otpCode": "731044",
            "newPassword": "matkhaumoi456"
        }

    Đây là bước 2 của quy trình quên mật khẩu. Bước 1 là ForgotPasswordRequest
    (xin mã OTP gửi về email).

    -----------------------------------------------------
    Backend làm gì? (AuthService.resetPassword)
    -----------------------------------------------------
    1. Tìm tài khoản theo email. Không có → 400 "Email does not exist".
    2. Kiểm tra mã OTP mục đích RESET_PASSWORD: còn hạn 5 phút, chưa sai quá 5 lần, có khớp không.
       Mã OTP đăng ký (mục đích REGISTER) không dùng được ở đây, vì mỗi mã gắn với 1 mục đích.
    3. Băm mật khẩu mới bằng BCrypt rồi lưu.
    4. Xóa phiên đăng nhập trong Redis → mọi thiết bị đang đăng nhập bị đăng xuất.
       Lý do: người dùng thường đặt lại mật khẩu vì nghi bị lộ.
       Nếu kẻ gian đang dùng tài khoản thì sẽ bị đá ra ngay.

    File này là tổ hợp của 2 file đã học:
    - email, otpCode : giống hệt VerifyOtpRequest.
    - newPassword    : cùng quy tắc với password trong RegisterRequest.
*/
public class ResetPasswordRequest {
    /*
        -----------------------------------------------------
        email: email của tài khoản cần đặt lại mật khẩu
        -----------------------------------------------------
        Phải gửi lại email (dù đã gửi ở bước 1), vì backend không lưu "ai đang ở bước 2".
        Mỗi request là độc lập, request nào cũng phải tự mang đủ thông tin.
        Frontend giữ sẵn email từ bước 1, người dùng không phải gõ lại.

        - @NotBlank: bắt buộc phải có → "Email is required".
        - @Email   : đúng định dạng email → "Invalid email".
    */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email")
    private String email;

    /*
        -----------------------------------------------------
        otpCode: mã OTP 6 chữ số nhận được qua email
        -----------------------------------------------------
        Quy tắc giống hệt VerifyOtpRequest:
        - @NotBlank: bắt buộc phải có → "OTP code is required".
        - @Pattern(regexp = "^\\d{6}$"): đúng 6 chữ số → "OTP code must be 6 digits".
        Vì sao dùng String thay vì int, và cách đọc regex ^\d{6}$: xem VerifyOtpRequest.
    */
    @NotBlank(message = "OTP code is required")
    @Pattern(regexp = "^\\d{6}$", message = "OTP code must be 6 digits")
    private String otpCode;

    /*
        -----------------------------------------------------
        newPassword: mật khẩu mới (dạng gốc, chưa băm)
        -----------------------------------------------------
        - @NotBlank: bắt buộc phải có → "New password is required".
        - @Size(min = 6, max = 72): dài từ 6 đến 72 ký tự
          → "New password must be between 6 and 72 characters".
          Vì sao giới hạn 6 và 72: xem thuộc tính password trong RegisterRequest.

        Quy tắc phải giống hệt lúc đăng ký. Nếu ở đây lỏng hơn (VD cho 3 ký tự),
        người dùng sẽ lách được quy tắc mật khẩu bằng cách đăng ký xong rồi "quên mật khẩu".

        Tên key trong JSON là "newPassword" (chữ P viết hoa), khớp với setNewPassword.
    */
    @NotBlank(message = "New password is required")
    @Size(min = 6, max = 72, message = "New password must be between 6 and 72 characters")
    private String newPassword;

    /*
        -----------------------------------------------------
        Getter và setter
        -----------------------------------------------------
        - Setter: Jackson gọi khi đổi JSON thành đối tượng
          ("email" → setEmail, "otpCode" → setOtpCode, "newPassword" → setNewPassword).
        - Getter: AuthController gọi request.getEmail(), request.getOtpCode(), request.getNewPassword()
          để truyền xuống AuthService.resetPassword.
        Cách đọc từng dòng: xem RegisterRequest.
    */
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}