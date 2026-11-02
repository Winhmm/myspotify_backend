package com.winhmm.myspotify.dto.auth;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    (package, import, DTO, annotation, validation là gì: xem giải thích chi tiết trong RegisterRequest.)

    - Email   : giá trị phải đúng định dạng email.
    - NotBlank: không được null, rỗng hoặc chỉ toàn dấu cách.
    - Pattern : giá trị phải khớp 1 biểu thức chính quy (regex).
*/
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/*
    =====================================================
    VERIFYOTPREQUEST - DỮ LIỆU XÁC THỰC MÃ OTP ĐĂNG KÝ
    =====================================================
    Dùng cho: POST /api/auth/verify-otp (UC02, xem AuthController)

    JSON frontend gửi lên:
        {
            "email": "maychieu@gmail.com",
            "otpCode": "482913"
        }

    -----------------------------------------------------
    OTP là gì?
    -----------------------------------------------------
    OTP (One-Time Password, mật khẩu dùng 1 lần) là mã 6 chữ số
    backend gửi tới email của người đăng ký.
    Người dùng nhập đúng mã → chứng minh họ thật sự sở hữu email đó → tài khoản được kích hoạt.

    Quy tắc của mã OTP (kiểm tra trong OtpService, không phải ở DTO này):
    - Hết hạn sau 5 phút.
    - Sai tối đa 5 lần cho mỗi mã.
    - Dùng xong là hết hiệu lực, không dùng lại được.

    -----------------------------------------------------
    Vì sao phải gửi kèm email?
    -----------------------------------------------------
    Người dùng lúc này chưa đăng nhập, chưa có token, backend không biết họ là ai.
    Email cho backend biết cần kiểm tra mã OTP của tài khoản nào.
    Frontend thường giữ sẵn email từ bước đăng ký, người dùng chỉ cần gõ mã.

    -----------------------------------------------------
    DTO kiểm tra hình thức, Service kiểm tra nghiệp vụ
    -----------------------------------------------------
    - DTO này chỉ kiểm tra "trông có đúng không": có gửi không, có đúng 6 chữ số không.
      Không cần hỏi database.
    - Mã có khớp không, còn hạn không, đã sai mấy lần: OtpService kiểm tra (phải đọc database và Redis).
    Mã sai hình thức (VD "12ab") bị chặn ngay ở DTO, đỡ tốn 1 lần truy vấn database.
*/
public class VerifyOtpRequest {
    /*
        -----------------------------------------------------
        email: email của tài khoản cần kích hoạt
        -----------------------------------------------------
        - @NotBlank: bắt buộc phải có → "Email is required".
        - @Email   : đúng định dạng email → "Invalid email".
        Ý nghĩa 2 annotation: xem RegisterRequest.

        Email không tồn tại trong hệ thống: AuthService báo "Email does not exist".
    */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email")
    private String email;

    /*
        -----------------------------------------------------
        otpCode: mã OTP 6 chữ số
        -----------------------------------------------------
        Vì sao kiểu String, không phải int?
        - Mã OTP có thể bắt đầu bằng số 0, VD "012345".
          Lưu bằng int thì số 0 ở đầu bị mất: 012345 thành 12345 (chỉ còn 5 chữ số).
        - Mã OTP là 1 dãy ký tự để so khớp, không dùng để cộng trừ nhân chia.
        → Dữ liệu chỉ gồm chữ số nhưng không dùng để tính toán thì nên dùng String
          (giống số điện thoại, mã bưu điện).
        Frontend phải gửi kèm dấu nháy kép: "otpCode": "012345", không phải "otpCode": 12345.

        1. @NotBlank(message = "OTP code is required"): bắt buộc phải có.

        2. @Pattern(regexp = "^\\d{6}$", message = "OTP code must be 6 digits"):
           Phải gồm đúng 6 chữ số, không hơn, không kém.

           Đọc regex ^\d{6}$:
               ^    : bắt đầu chuỗi
               \d   : 1 chữ số (từ 0 đến 9), d là viết tắt của digit (chữ số)
               {6}  : ký tự ngay trước lặp lại đúng 6 lần
               $    : kết thúc chuỗi
           → Từ đầu tới cuối là đúng 6 chữ số.

           Vì sao trong code viết "\\d" (2 dấu gạch ngược) mà regex chỉ có \d (1 dấu)?
           Trong chuỗi Java, dấu gạch ngược là ký tự đặc biệt (VD \n là xuống dòng).
           Muốn chuỗi chứa đúng 1 dấu gạch ngược thì phải viết 2 dấu: "\\".
           → Chuỗi Java "^\\d{6}$" sau khi đọc vào bộ nhớ chính là regex ^\d{6}$.

           Ví dụ:
               "482913"  → hợp lệ
               "012345"  → hợp lệ
               "48291"   → không hợp lệ (chỉ 5 chữ số)
               "4829130" → không hợp lệ (7 chữ số)
               "48 291 3"→ không hợp lệ (có dấu cách)
               "12ab56"  → không hợp lệ (có chữ cái)

           @Pattern bỏ qua giá trị null, nên vẫn cần @NotBlank ở trên để bắt trường hợp không gửi mã.
    */
    @NotBlank(message = "OTP code is required")
    @Pattern(regexp = "^\\d{6}$", message = "OTP code must be 6 digits")
    private String otpCode;

    /*
        -----------------------------------------------------
        Getter và setter
        -----------------------------------------------------
        - Setter: Jackson gọi khi đổi JSON thành đối tượng ("otpCode" → setOtpCode).
        - Getter: AuthController gọi request.getEmail(), request.getOtpCode() để truyền xuống AuthService.
        Cách đọc từng dòng: xem RegisterRequest.

        Tên key trong JSON phải là "otpCode" (chữ C viết hoa) để khớp với setOtpCode.
        Gửi "otp_code" hoặc "otpcode" thì Jackson không tìm thấy setter
        → otpCode là null → lỗi "OTP code is required".
    */
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }
}