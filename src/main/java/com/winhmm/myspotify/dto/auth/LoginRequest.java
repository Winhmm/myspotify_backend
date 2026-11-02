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
    LOGINREQUEST - DỮ LIỆU ĐĂNG NHẬP
    =====================================================
    Dùng cho: POST /api/auth/login (UC03, xem AuthController)

    JSON frontend gửi lên:
        {
            "email": "maychieu@gmail.com",
            "password": "matkhau123"
        }

    Đăng nhập thành công → backend trả về LoginResponse (có token).
    Đăng nhập thất bại   → 400 kèm message (xem danh sách lỗi trong AuthController).

    -----------------------------------------------------
    Đăng nhập bằng email, không phải username
    -----------------------------------------------------
    Project chọn đăng nhập bằng email. Username chỉ để hiển thị.
    Sau khi đăng nhập, email cũng là thứ được ghi vào token (trường sub),
    và authentication.getName() trong các Controller trả về chính email này.

    -----------------------------------------------------
    Lưu ý về bảo mật khi gửi mật khẩu
    -----------------------------------------------------
    Mật khẩu được gửi dạng chữ bình thường trong body JSON.
    - Phải gửi bằng POST trong body, không bao giờ đặt trên URL (VD /login?password=...),
      vì URL bị lưu trong lịch sử trình duyệt và log của máy chủ.
    - Trên đường truyền, chỉ HTTPS mới mã hóa nội dung request.
      Server hiện chạy http://157.66.100.87:8080 (chưa có HTTPS), nên mật khẩu đi trên mạng
      ở dạng đọc được. Với project học tập thì chấp nhận được,
      khi chạy thật cho nhiều người dùng thì cần gắn tên miền và bật HTTPS.
*/
public class LoginRequest {
    /*
        -----------------------------------------------------
        email: email đăng nhập
        -----------------------------------------------------
        - @NotBlank: bắt buộc phải có → "Email is required".
        - @Email   : đúng định dạng email → "Invalid email".
        Ý nghĩa 2 annotation: xem RegisterRequest.

        Email sai định dạng bị chặn ngay ở đây, không cần hỏi database.
        Email đúng định dạng nhưng không tồn tại: AuthService báo "Email or password is incorrect"
        (giải thích ở thuộc tính password bên dưới).
    */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email")
    private String email;

    /*
        -----------------------------------------------------
        password: mật khẩu
        -----------------------------------------------------
        Chỉ có @NotBlank (bắt buộc phải có), không có @Size như RegisterRequest. Vì sao?
        - Lúc đăng nhập chỉ cần so mật khẩu gửi lên với mật khẩu đã lưu.
          Mật khẩu sai thì kiểu gì cũng sai, dài hay ngắn không quan trọng.
        - Nếu thêm @Size(min = 6), mật khẩu 3 ký tự sẽ nhận lỗi "Password must be between 6 and 72 characters".
          Như vậy là lộ quy tắc đặt mật khẩu cho kẻ gian đang dò.
          Không có @Size thì mọi mật khẩu sai đều nhận chung 1 lỗi "Email or password is incorrect".

        Vì sao sai email và sai mật khẩu báo cùng 1 message?
        Nếu báo riêng "Email không tồn tại" và "Sai mật khẩu", kẻ gian thử nhiều email
        sẽ biết email nào đã đăng ký trong hệ thống. Báo chung thì không đoán được.

        Cách so mật khẩu (trong AuthService.login):
            passwordEncoder.matches(mật khẩu gửi lên, mật khẩu đã băm trong database)
        BCrypt băm lại mật khẩu gửi lên theo đúng "muối" đã dùng lúc đăng ký rồi so 2 kết quả.
        Không thể giải ngược mật khẩu đã băm về mật khẩu gốc.
    */
    @NotBlank(message = "Password is required")
    private String password;

    /*
        -----------------------------------------------------
        Getter và setter
        -----------------------------------------------------
        - Setter: Jackson gọi khi đổi JSON thành đối tượng ("email" → setEmail, "password" → setPassword).
        - Getter: AuthController gọi request.getEmail(), request.getPassword()
          để truyền xuống AuthService.login.
        Cách đọc từng dòng: xem RegisterRequest.
    */
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}