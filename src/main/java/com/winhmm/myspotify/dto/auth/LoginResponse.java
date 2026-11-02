package com.winhmm.myspotify.dto.auth;

/*
    -----------------------------------------------------
    Dòng import
    -----------------------------------------------------
    (package, import, DTO là gì: xem giải thích chi tiết trong RegisterRequest.)

    - Role: enum quyền của tài khoản, nằm trong package enums của project.
      Enum là kiểu dữ liệu chỉ có 1 số giá trị cố định. Role chỉ có 3 giá trị: USER, ARTIST, ADMIN.
      Gõ sai (VD Role.ADMINN) thì IntelliJ báo lỗi ngay, an toàn hơn dùng chuỗi "ADMIN".

    File này không import gì của jakarta.validation:
    đây là dữ liệu backend tự tạo ra để trả về, không phải dữ liệu người dùng gửi lên,
    nên không cần kiểm tra.
*/
import com.winhmm.myspotify.enums.Role;

/*
    =====================================================
    LOGINRESPONSE - KẾT QUẢ TRẢ VỀ KHI ĐĂNG NHẬP THÀNH CÔNG
    =====================================================
    Dùng cho: POST /api/auth/login (UC03)

    JSON backend trả về:
        {
            "token": "eyJhbGciOiJIUzI1NiJ9...",
            "userId": 6,
            "username": "maychieu",
            "role": "ARTIST"
        }

    -----------------------------------------------------
    Response DTO khác request DTO thế nào?
    -----------------------------------------------------
        Request DTO (VD LoginRequest)              Response DTO (file này)
        frontend gửi lên backend                   backend trả về frontend
        Jackson đổi JSON → đối tượng               Jackson đổi đối tượng → JSON
        Jackson cần constructor rỗng + setter      Jackson chỉ cần getter
        có annotation kiểm tra (@NotBlank...)      không cần kiểm tra

    -----------------------------------------------------
    Đối tượng này được tạo ở đâu?
    -----------------------------------------------------
    Cuối hàm AuthService.login, sau khi mật khẩu đúng và đã tạo token:
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getRole());
    AuthController nhận lại và trả về: ResponseEntity.ok(response).

    -----------------------------------------------------
    Đối tượng được chuyển thành JSON thế nào?
    -----------------------------------------------------
    Jackson đọc các getter để tạo JSON, tên key lấy từ tên getter
    (bỏ chữ "get", viết thường chữ cái đầu):
        getToken()    → "token"
        getUserId()   → "userId"
        getUsername() → "username"
        getRole()     → "role"
    Enum được đổi thành chuỗi theo tên: Role.ARTIST → "ARTIST".

    -----------------------------------------------------
    Vì sao dùng DTO mà không trả Map như các API khác trong AuthController?
    -----------------------------------------------------
    - Các API khác chỉ trả 1 câu thông báo { "message": "..." }, dùng Map.of là đủ.
    - API này trả 4 trường, frontend cần cấu trúc cố định.
      DTO ghi rõ có những trường nào, kiểu gì. Gõ sai tên thì IntelliJ báo lỗi ngay,
      còn Map gõ sai khóa (VD "tokn") vẫn chạy bình thường, tới lúc frontend lỗi mới biết.

    -----------------------------------------------------
    Ý nghĩa từng trường
    -----------------------------------------------------
    1. token:
       - JWT do backend tạo khi đăng nhập thành công.
       - Thời hạn lấy từ app.jwt.expiration-ms trong file cấu hình, đơn vị mili giây
         (VD 86400000 = 24 giờ, 604800000 = 7 ngày).
       - Chứa email (sub), mã phiên (jti), thời điểm tạo (iat) và thời điểm hết hạn (exp).
         Không chứa mật khẩu hay role.
       - Frontend lưu lại và gửi kèm ở mọi request sau:
             Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
       - Token mất hiệu lực sớm hơn thời hạn khi: đăng xuất, đăng nhập ở thiết bị khác,
         đổi mật khẩu, đặt lại mật khẩu, đổi email, bị Admin khóa.

    2. userId:
       - Id của người dùng trong database.
       - Frontend dùng để so sánh, ví dụ: bài hát có artistId bằng userId
         thì hiện nút "Sửa" cho chính Artist đó.

    3. username:
       - Tên đăng nhập, để hiển thị ngay trên giao diện (VD "Xin chào, maychieu")
         mà không cần gọi thêm API.

    4. role:
       - Quyền hiện tại: USER, ARTIST hoặc ADMIN.
       - Frontend dựa vào đây để hiện hoặc ẩn menu tương ứng.
       - Chỉ đúng tại thời điểm đăng nhập. Admin đổi role sau đó thì giá trị này cũ đi,
         gọi GET /api/users/me/profile để lấy role mới nhất.
       - Ẩn menu ở frontend chỉ để giao diện gọn. Bảo mật thật nằm ở backend:
         dù frontend hiện nhầm nút, backend vẫn trả 403 nếu không đủ quyền.
*/
public class LoginResponse {
    /*
        -----------------------------------------------------
        4 thuộc tính
        -----------------------------------------------------
        - String token   : chuỗi JWT.
        - Long userId    : id người dùng.
          Long (chữ L hoa) là kiểu đối tượng, có thể null.
          long (chữ l thường) là kiểu nguyên thủy, không thể null.
          Dùng Long cho đồng bộ với kiểu id của entity User.
        - String username: tên đăng nhập.
        - Role role      : quyền, kiểu enum Role.

        private: bên ngoài không đọc trực tiếp được, phải gọi getter (Jackson cũng gọi getter).
    */
    private String token;
    private Long userId;
    private String username;
    private Role role;

    /*
        -----------------------------------------------------
        Constructor 4 tham số
        -----------------------------------------------------
        Constructor là hàm đặc biệt: trùng tên với class, không có kiểu trả về,
        chạy đúng 1 lần khi tạo đối tượng bằng new.

        new LoginResponse(token, user.getId(), user.getUsername(), user.getRole())
        → truyền đủ 4 giá trị theo đúng thứ tự khai báo, tạo xong là có đủ dữ liệu ngay.

        this.token = token:
        - this.token: thuộc tính của đối tượng.
        - token     : tham số vừa được truyền vào.
        → Gán giá trị tham số vào thuộc tính. 3 dòng còn lại tương tự.

        Class đã tự khai báo constructor này, nên Java không tự tạo constructor không tham số nữa
        (xem RegisterRequest). Không sao, vì Jackson chỉ đổi đối tượng này thành JSON,
        không bao giờ phải tạo nó từ JSON.
    */
    public LoginResponse(String token, Long userId, String username, Role role) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    /*
        -----------------------------------------------------
        Getter và setter
        -----------------------------------------------------
        - Getter: Jackson gọi để tạo JSON trả về (bắt buộc phải có, thiếu getter nào
          thì JSON thiếu trường đó).
        - Setter: hiện không nơi nào dùng tới, vì mọi giá trị đã được gán trong constructor.
          Để lại cũng không ảnh hưởng gì.
        Cách đọc từng dòng: xem RegisterRequest.
    */
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}