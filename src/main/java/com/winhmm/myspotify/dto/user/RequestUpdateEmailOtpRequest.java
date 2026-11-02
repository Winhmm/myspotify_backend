package com.winhmm.myspotify.dto.user;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    (Các khái niệm cơ bản: xem RegisterRequest. Thứ tự đọc package dto.user: xem UpdateProfileRequest.)

    - Email   : giá trị phải đúng định dạng email.
    - NotBlank: không được null, rỗng hoặc chỉ toàn dấu cách.
    - Size    : độ dài chuỗi nằm trong khoảng cho phép.
*/
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/*
    =====================================================
    REQUESTUPDATEEMAILOTPREQUEST - ĐỔI EMAIL (BƯỚC 1: GỬI OTP TỚI EMAIL MỚI)
    =====================================================

    API: POST /api/users/me/email/request-otp   (UC13, xem UserController)

    JSON frontend gửi lên:
        {
            "newEmail": "maychieu.new@gmail.com"
        }

    Kết quả: 200 { "message": "OTP code has been sent to your new email" }

    -----------------------------------------------------
    Quy trình đổi email gồm 2 bước
    -----------------------------------------------------
        Bước 1 (file này)          : gửi email mới → backend gửi mã OTP tới email mới.
        Bước 2 (UpdateEmailRequest): gửi mã OTP → backend đổi email.

    Vì sao gửi OTP tới email mới, không phải email cũ?
    Để chứng minh người dùng thật sự sở hữu email mới.
    Nếu không kiểm tra, người dùng gõ nhầm 1 ký tự sẽ mất tài khoản:
    sau này OTP quên mật khẩu sẽ gửi tới 1 email không ai đọc.

    -----------------------------------------------------
    Backend làm gì? (UserService.requestUpdateEmailOtp)
    -----------------------------------------------------
    1. Email mới phải khác email hiện tại → 400 "New email must be different from current email".
       So sánh không phân biệt hoa thường: "MayChieu@gmail.com" và "maychieu@gmail.com" coi là trùng.
    2. Email mới chưa được tài khoản nào dùng → 400 "Email already exists".
       Tính cả tài khoản chưa xác thực và tài khoản đã xóa mềm (email của họ vẫn bị giữ).
    3. Tạo mã OTP (mục đích CHANGE_EMAIL), lưu email mới vào bản ghi OTP
       (cột new_email của bảng otp_verifications), rồi gửi mã tới email mới.
       Bảng users chưa đổi gì: người dùng vẫn đăng nhập bằng email cũ cho tới khi xong bước 2.

    Lỗi khác có thể gặp:
        400 "Please wait 60 seconds before requesting a new OTP"  → xin mã liên tục (chống spam)
        503 "Could not send OTP email, please try again later"    → gửi email thất bại

    -----------------------------------------------------
    Lưu ý thiết kế
    -----------------------------------------------------
    Đổi mật khẩu phải nhập mật khẩu hiện tại (RequestChangePasswordOtpRequest),
    còn đổi email thì chỉ cần đang đăng nhập.
    Người lấy được token đang đăng nhập (VD mượn máy) có thể đổi email sang email của họ,
    rồi dùng "quên mật khẩu" để chiếm luôn tài khoản.
    Muốn chặt hơn: thêm trường currentPassword vào DTO này và kiểm tra giống bước 1 đổi mật khẩu.
    Với project học tập, cách hiện tại vẫn chạy đúng.
*/
public class RequestUpdateEmailOtpRequest {
    /*
        -----------------------------------------------------
        newEmail: email mới muốn đổi sang
        -----------------------------------------------------
        - @NotBlank: bắt buộc phải có → "New email is required".
        - @Email   : đúng định dạng email → "Invalid email".
          Chỉ kiểm tra hình thức cơ bản: "maychieu@gmail" (thiếu .com) vẫn lọt qua
          (xem RegisterRequest). Email có thật hay không thì mã OTP ở bước 2 sẽ chứng minh.
        - @Size(max = 100): tối đa 100 ký tự → "New email must not exceed 100 characters".
          Khớp với độ dài cột email (length = 100) trong entity User, giống quy tắc lúc đăng ký.

        Vì sao có cả @NotBlank lẫn @Email?
        @Email bỏ qua giá trị null (coi như hợp lệ). Thiếu @NotBlank thì không gửi email mới vẫn lọt qua.

        Tên key trong JSON là "newEmail" (không phải "email"), khớp với setNewEmail.
    */
    @NotBlank(message = "New email is required")
    @Email(message = "Invalid email")
    @Size(max = 100, message = "New email must not exceed 100 characters")
    private String newEmail;

    /*
        -----------------------------------------------------
        Getter và setter
        -----------------------------------------------------
        - Setter: Jackson gọi khi đổi JSON thành đối tượng ("newEmail" → setNewEmail).
        - Getter: UserController gọi request.getNewEmail() để truyền xuống UserService.
        Cách đọc từng dòng: xem RegisterRequest.
    */
    public String getNewEmail() { return newEmail; }
    public void setNewEmail(String newEmail) { this.newEmail = newEmail; }
}