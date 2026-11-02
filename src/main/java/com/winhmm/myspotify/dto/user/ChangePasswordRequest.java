package com.winhmm.myspotify.dto.user;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    (Các khái niệm cơ bản: xem RegisterRequest. Thứ tự đọc package dto.user: xem UpdateProfileRequest.)

    - NotBlank: không được null, rỗng hoặc chỉ toàn dấu cách.
    - Pattern : giá trị phải khớp 1 biểu thức chính quy (regex).
    - Size    : độ dài chuỗi nằm trong khoảng cho phép.
*/
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/*
    =====================================================
    CHANGEPASSWORDREQUEST - ĐỔI MẬT KHẨU (BƯỚC 2: NHẬP OTP VÀ MẬT KHẨU MỚI)
    =====================================================

    API: PUT /api/users/me/change-password   (UC09, xem UserController)

    JSON frontend gửi lên:
        {
            "otpCode": "529301",
            "newPassword": "matkhaumoi456"
        }

    Đây là bước 2. Bước 1 là RequestChangePasswordOtpRequest (nhập mật khẩu hiện tại để nhận OTP).

    - Không gửi email: backend biết tài khoản nhờ token.
    - Không gửi lại mật khẩu hiện tại: đã kiểm tra ở bước 1.
      Mã OTP chỉ được tạo ra khi bước 1 thành công → có mã OTP đúng nghĩa là đã qua bước 1.

    -----------------------------------------------------
    Backend làm gì? (UserService.changePassword)
    -----------------------------------------------------
    1. Kiểm tra mã OTP mục đích CHANGE_PASSWORD của tài khoản đang đăng nhập
       (OtpService.verifyAndConsume):
           Không có mã nào đang chờ dùng → 400 "Otp code does not exist"
           Quá 5 phút                    → 400 "Otp code has expired"
           Sai quá 5 lần                 → 400 "Too many wrong attempts, please request a new OTP"
           Sai mã                        → 400 "Otp code does not match"
       Mã OTP của mục đích khác (VD mã đổi email, mục đích CHANGE_EMAIL) không dùng được ở đây.
       Dùng xong, mã bị đánh dấu đã dùng, không dùng lại lần 2 được.
    2. Băm mật khẩu mới bằng BCrypt rồi lưu vào database.
    3. Xóa phiên đăng nhập trong Redis → đăng xuất khỏi mọi thiết bị, kể cả thiết bị đang dùng.

    Kết quả: 200 { "message": "Password has been changed successfully" }
    Frontend: thành công → xóa token, chuyển về trang đăng nhập để đăng nhập bằng mật khẩu mới.

    So với ResetPasswordRequest (quên mật khẩu, dto.auth): giống hệt, chỉ thiếu trường email
    (vì ở đây người dùng đã đăng nhập).
*/
public class ChangePasswordRequest {
    /*
        -----------------------------------------------------
        otpCode: mã OTP 6 chữ số nhận được qua email
        -----------------------------------------------------
        Quy tắc giống hệt VerifyOtpRequest (dto.auth):
        - @NotBlank: bắt buộc phải có → "OTP code is required".
        - @Pattern(regexp = "^\\d{6}$"): đúng 6 chữ số → "OTP code must be 6 digits".
        Cách đọc regex ^\d{6}$ và vì sao viết "\\d" trong code: xem VerifyOtpRequest.

        Nhắc nhanh vì sao dùng String thay vì int:
        mã OTP có thể bắt đầu bằng số 0 ("012345"), lưu bằng int thì mất số 0 ở đầu (12345).
        Frontend phải gửi kèm dấu nháy kép: "otpCode": "012345".
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
          Vì sao 6 và 72: xem thuộc tính password trong RegisterRequest
          (72 là giới hạn của thuật toán băm BCrypt).

        Quy tắc phải giống hệt lúc đăng ký (RegisterRequest) và lúc quên mật khẩu (ResetPasswordRequest).
        Nếu ở đây lỏng hơn (VD cho 3 ký tự), người dùng sẽ lách được quy tắc mật khẩu
        bằng cách đăng ký xong rồi "đổi mật khẩu".

        Backend không bắt mật khẩu mới phải khác mật khẩu cũ:
        gửi lại đúng mật khẩu cũ vẫn thành công.
    */
    @NotBlank(message = "New password is required")
    @Size(min = 6, max = 72, message = "New password must be between 6 and 72 characters")
    private String newPassword;

    /*
        -----------------------------------------------------
        Getter và setter
        -----------------------------------------------------
        - Setter: Jackson gọi khi đổi JSON thành đối tượng
          ("otpCode" → setOtpCode, "newPassword" → setNewPassword).
        - Getter: UserController gọi request.getOtpCode(), request.getNewPassword()
          để truyền xuống UserService.changePassword.
        Cách đọc từng dòng: xem RegisterRequest.
    */
    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}