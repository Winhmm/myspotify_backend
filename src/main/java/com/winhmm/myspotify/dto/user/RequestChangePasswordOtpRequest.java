package com.winhmm.myspotify.dto.user;

/*
    -----------------------------------------------------
    Dòng import
    -----------------------------------------------------
    (Các khái niệm cơ bản: xem RegisterRequest. Thứ tự đọc package dto.user: xem UpdateProfileRequest.)

    - NotBlank: không được null, rỗng hoặc chỉ toàn dấu cách.
*/
import jakarta.validation.constraints.NotBlank;

/*
    =====================================================
    REQUESTCHANGEPASSWORDOTPREQUEST - ĐỔI MẬT KHẨU (BƯỚC 1: XÁC NHẬN MẬT KHẨU HIỆN TẠI)
    =====================================================

    API: POST /api/users/me/change-password/request-otp   (UC09, xem UserController)

    JSON frontend gửi lên:
        {
            "currentPassword": "matkhau123"
        }

    Kết quả: 200 { "message": "OTP code has been sent to your email" }

    -----------------------------------------------------
    Quy trình đổi mật khẩu gồm 2 bước
    -----------------------------------------------------
        Bước 1 (file này)             : gửi mật khẩu hiện tại → backend gửi mã OTP tới email của tài khoản.
        Bước 2 (ChangePasswordRequest): gửi mã OTP + mật khẩu mới → đổi mật khẩu.

    Vì sao cần cả mật khẩu hiện tại lẫn OTP? Bảo vệ 2 lớp:
    - Người mượn máy, hoặc kẻ gian lấy được token đang đăng nhập sẵn, không biết mật khẩu
      → bị chặn ở bước 1.
    - Kẻ gian biết mật khẩu (VD bị lộ) nhưng không vào được hộp thư email → bị chặn ở bước 2.

    So với quên mật khẩu (ForgotPasswordRequest trong dto.auth):
    - Quên mật khẩu: chưa đăng nhập, nên phải gửi email để backend biết là tài khoản nào.
    - Đổi mật khẩu : đã đăng nhập, backend biết tài khoản nhờ token → không cần gửi email.

    -----------------------------------------------------
    Backend làm gì? (UserService.requestChangePasswordOtp)
    -----------------------------------------------------
    1. Lấy tài khoản đang đăng nhập (từ token).
    2. So mật khẩu hiện tại với mật khẩu đã băm trong database bằng BCrypt (passwordEncoder.matches).
       Sai → 400 "Password does not match".
    3. Tạo mã OTP 6 chữ số (mục đích CHANGE_PASSWORD), hạn 5 phút, gửi về email hiện tại.
       Các mã CHANGE_PASSWORD cũ chưa dùng bị vô hiệu, chỉ mã mới nhất dùng được.

    Lỗi khác có thể gặp:
        400 "Please wait 60 seconds before requesting a new OTP"  → xin mã liên tục (chống spam)
        503 "Could not send OTP email, please try again later"    → gửi email thất bại
*/
public class RequestChangePasswordOtpRequest {
    /*
        -----------------------------------------------------
        currentPassword: mật khẩu hiện tại (dạng gốc, chưa băm)
        -----------------------------------------------------
        - @NotBlank: bắt buộc phải có → "Current password is required".

        Vì sao không có @Size(min = 6, max = 72) như lúc đăng ký?
        Mật khẩu này không được lưu, chỉ dùng để so với mật khẩu đã lưu.
        Gõ sai thì Service báo "Password does not match", không cần kiểm tra độ dài.
        @Size chỉ cần cho mật khẩu sắp được lưu (password trong RegisterRequest, newPassword
        trong ChangePasswordRequest), để mật khẩu mới luôn đủ mạnh và băm được bằng BCrypt.

        Mật khẩu gốc chỉ tồn tại trong bộ nhớ lúc xử lý request, không được lưu hay ghi log.
        Database chỉ chứa chuỗi đã băm. BCrypt là băm 1 chiều: không giải ngược ra mật khẩu gốc được,
        chỉ kiểm tra được "mật khẩu này băm ra có khớp với chuỗi đã lưu không".
    */
    @NotBlank(message = "Current password is required")
    private String currentPassword;

    /*
        -----------------------------------------------------
        Getter và setter
        -----------------------------------------------------
        - Setter: Jackson gọi khi đổi JSON thành đối tượng ("currentPassword" → setCurrentPassword).
        - Getter: UserController gọi request.getCurrentPassword() để truyền xuống UserService.
        Cách đọc từng dòng: xem RegisterRequest.
    */
    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
}