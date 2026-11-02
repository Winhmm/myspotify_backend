package com.winhmm.myspotify.dto.user;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    (Các khái niệm cơ bản: xem RegisterRequest. Thứ tự đọc package dto.user: xem UpdateProfileRequest.)

    - NotBlank: không được null, rỗng hoặc chỉ toàn dấu cách.
    - Pattern : giá trị phải khớp 1 biểu thức chính quy (regex).
*/
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/*
    =====================================================
    UPDATEEMAILREQUEST - ĐỔI EMAIL (BƯỚC 2: NHẬP OTP)
    =====================================================

    API: PUT /api/users/me/email   (UC13, xem UserController)

    JSON frontend gửi lên:
        {
            "otpCode": "118204"
        }

    Đây là bước 2. Bước 1 là RequestUpdateEmailOtpRequest (gửi email mới để nhận OTP).

    -----------------------------------------------------
    Vì sao không gửi lại email mới?
    -----------------------------------------------------
    Email mới đã được lưu trong bản ghi OTP ở bước 1. Backend lấy email từ đó.

    Nếu cho gửi lại email ở bước 2, kẻ gian có thể:
        bước 1: nhận OTP ở email A của mình (chứng minh sở hữu A)
        bước 2: gửi mã đó kèm email B của người khác
        → đổi được sang email B mà chưa bao giờ chứng minh sở hữu B.
    Lấy email từ bản ghi OTP → email được đổi chắc chắn là email đã nhận mã.

    -----------------------------------------------------
    Backend làm gì? (UserService.updateEmail)
    -----------------------------------------------------
    1. Kiểm tra mã OTP mục đích CHANGE_EMAIL (lỗi giống ChangePasswordRequest:
       "Otp code does not exist", "Otp code has expired", "Otp code does not match",
       "Too many wrong attempts, please request a new OTP").
    2. Kiểm tra lại email mới chưa bị ai dùng → 400 "Email already exists".
       Có thể có người vừa đăng ký đúng email đó trong lúc chờ nhập OTP.
    3. Xóa phiên đăng nhập của email cũ trong Redis.
    4. Đổi email trong bảng users.

    Kết quả: 200 { "message": "Email has been updated, please login again" }

    Vì sao phải đăng nhập lại?
    Token hiện tại ghi email cũ. Mỗi request, JwtAuthenticationFilter kiểm tra phiên theo email trong token.
    Phiên của email cũ đã bị xóa → token cũ không dùng được nữa (401).
    Frontend: thành công → xóa token, chuyển về trang đăng nhập để đăng nhập bằng email mới.
*/
public class UpdateEmailRequest {
    /*
        -----------------------------------------------------
        otpCode: mã OTP 6 chữ số nhận được ở email mới
        -----------------------------------------------------
        Quy tắc giống hệt VerifyOtpRequest (dto.auth) và ChangePasswordRequest:
        - @NotBlank: bắt buộc phải có → "OTP code is required".
        - @Pattern(regexp = "^\\d{6}$"): đúng 6 chữ số → "OTP code must be 6 digits".
        Cách đọc regex và vì sao dùng String thay vì int: xem VerifyOtpRequest.

        DTO này chỉ có 1 trường, vì mọi thông tin khác backend đã biết:
        tài khoản (từ token) và email mới (từ bản ghi OTP).
    */
    @NotBlank(message = "OTP code is required")
    @Pattern(regexp = "^\\d{6}$", message = "OTP code must be 6 digits")
    private String otpCode;

    /*
        -----------------------------------------------------
        Getter và setter
        -----------------------------------------------------
        - Setter: Jackson gọi khi đổi JSON thành đối tượng ("otpCode" → setOtpCode).
        - Getter: UserController gọi request.getOtpCode() để truyền xuống UserService.updateEmail.
        Cách đọc từng dòng: xem RegisterRequest.
    */
    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }
}