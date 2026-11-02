package com.winhmm.myspotify.dto.auth;

/*
    -----------------------------------------------------
    Bài học số 1 của package dto.auth: nên đọc file này trước tiên
    -----------------------------------------------------
    Package dto.auth có 6 file, đọc theo đúng thứ tự người dùng đi qua khi dùng web:
        1. RegisterRequest       → đăng ký tài khoản          (file này, giải thích kỹ nhất)
        2. VerifyOtpRequest      → nhập mã OTP để kích hoạt tài khoản
        3. LoginRequest          → đăng nhập
        4. LoginResponse         → kết quả trả về khi đăng nhập thành công
        5. ForgotPasswordRequest → quên mật khẩu, xin mã OTP
        6. ResetPasswordRequest  → nhập mã OTP và mật khẩu mới
    Những khái niệm giải thích ở đây (DTO, validation, getter, setter, Jackson...)
    xuất hiện lại ở cả 5 file còn lại. Các file đó chỉ nhắc ngắn gọn và trỏ về file này.

    -----------------------------------------------------
    Dòng package
    -----------------------------------------------------
    package com.winhmm.myspotify.dto.auth;
    = "địa chỉ nhà" của class, luôn trùng với thư mục chứa file:
        src/main/java/com/winhmm/myspotify/dto/auth/RegisterRequest.java
    - dto : thư mục chứa các DTO (giải thích DTO là gì ở bên dưới).
    - auth: viết tắt của authentication (xác thực), nhóm các DTO dùng cho đăng ký, đăng nhập, OTP.
    Dấu chấm trong tên package tương ứng với dấu gạch chéo giữa các thư mục.

    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    import = mượn class ở chỗ khác về dùng trong file này.
    Không import thì mỗi lần dùng phải viết tên đầy đủ, VD jakarta.validation.constraints.NotBlank, rất dài.

    Cả 4 dòng import bên dưới đều lấy từ jakarta.validation.constraints.
    Đây là bộ quy tắc kiểm tra dữ liệu chuẩn của Java, tên chính thức là Jakarta Bean Validation.
    - Bộ chuẩn này chỉ định nghĩa các annotation (@NotBlank, @Email...) và ý nghĩa của chúng.
    - Phần thật sự chạy kiểm tra là thư viện Hibernate Validator.
      Trong Spring Boot, thư viện này được thêm vào project bằng dependency spring-boot-starter-validation.

    - Email   : giá trị phải đúng định dạng email.
    - NotBlank: giá trị không được để trống.
    - Pattern : giá trị phải khớp 1 mẫu (biểu thức chính quy, regex).
    - Size    : độ dài giá trị phải nằm trong khoảng cho phép.
    (Giải thích chi tiết từng cái ngay tại thuộc tính dùng nó.)
*/
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/*
    =====================================================
    REGISTERREQUEST - DỮ LIỆU ĐĂNG KÝ TÀI KHOẢN
    =====================================================
    Dùng cho: POST /api/auth/register (UC01, xem AuthController)

    JSON frontend gửi lên:
        {
            "username": "maychieu",
            "email": "maychieu@gmail.com",
            "password": "matkhau123"
        }

    -----------------------------------------------------
    DTO là gì?
    -----------------------------------------------------
    DTO (Data Transfer Object, "đối tượng chuyên chở dữ liệu") là class chỉ dùng để chứa dữ liệu
    đi vào (request) hoặc đi ra (response) qua API.
    DTO không có logic nghiệp vụ, không đọc ghi database. Bên trong chỉ có:
    thuộc tính để chứa dữ liệu, getter để đọc, setter để ghi.

    Có 2 loại:
    - Request DTO : dữ liệu frontend gửi lên backend (file này, tên kết thúc bằng Request).
    - Response DTO: dữ liệu backend trả về frontend (VD LoginResponse, tên kết thúc bằng Response).

    Vì sao không nhận thẳng entity User?
    - Entity User có nhiều trường frontend không được phép gửi lên:
      role, accountStatus, accessStatus, artistRequestStatus...
      Nếu nhận thẳng entity, kẻ gian có thể gửi kèm "role": "ADMIN" để tự thăng quyền.
    - DTO chỉ có đúng 3 trường cần cho đăng ký → trường lạ gửi lên bị bỏ qua.
      VD gửi { "username": "...", "email": "...", "password": "...", "role": "ADMIN" }
      → "role" không có chỗ để chứa trong RegisterRequest → bị bỏ qua hoàn toàn.

    -----------------------------------------------------
    Hành trình của dữ liệu đăng ký
    -----------------------------------------------------
        Frontend gửi JSON
          → Jackson đổi JSON thành đối tượng RegisterRequest
          → @Valid kiểm tra các quy tắc trên từng thuộc tính
               sai → dừng ngay, trả 400 kèm message, không chạy tiếp
          → AuthController.register lấy dữ liệu bằng getter
          → AuthService.register xử lý nghiệp vụ (email trùng, username trùng, băm mật khẩu, gửi OTP)

    -----------------------------------------------------
    JSON được chuyển thành đối tượng thế nào?
    -----------------------------------------------------
    Thư viện Jackson (có sẵn trong Spring Boot) làm việc này. Nó làm tương đương các dòng code sau:
        RegisterRequest request = new RegisterRequest();     // 1. tạo đối tượng rỗng
        request.setUsername("maychieu");                     // 2. gọi setter cho từng key trong JSON
        request.setEmail("maychieu@gmail.com");
        request.setPassword("matkhau123");

    Bước 1 cần constructor không tham số.
    Class này không viết constructor nào, nên Java tự tạo sẵn 1 constructor không tham số
    (gọi là constructor mặc định). Nếu sau này tự viết 1 constructor có tham số,
    Java sẽ không tự tạo constructor mặc định nữa, Jackson không tạo được đối tượng và báo lỗi.

    Bước 2 cần setter, và tên setter phải khớp với key trong JSON:
        key "username" → Jackson tìm hàm setUsername (thêm "set", viết hoa chữ cái đầu của key).
    Key trong JSON viết sai, VD "userName" hoặc "user_name" → không tìm thấy setter
    → thuộc tính username giữ nguyên null → bị @NotBlank chặn với lỗi "Username is required".

    -----------------------------------------------------
    Validation (kiểm tra dữ liệu) hoạt động thế nào?
    -----------------------------------------------------
    Các annotation đặt trên thuộc tính (@NotBlank, @Email, @Size, @Pattern) là các quy tắc kiểm tra.
    Annotation là "nhãn dán" bắt đầu bằng @. Bản thân nhãn không chạy gì cả,
    Spring đọc nhãn để biết phải kiểm tra gì.

    Các quy tắc này chỉ có tác dụng khi Controller ghi @Valid trước @RequestBody:
        public ResponseEntity<...> register(@Valid @RequestBody RegisterRequest request)
    Quên @Valid → mọi quy tắc trong file này bị bỏ qua, dữ liệu sai vẫn lọt vào Service.

    Khi dữ liệu vi phạm quy tắc:
    1. Spring dừng ngay, không chạy code trong Controller và Service.
    2. Spring ném lỗi MethodArgumentNotValidException.
    3. GlobalExceptionHandler bắt lỗi, trả 400 kèm message ghi trong annotation:
           { "message": "Username must be between 5 and 20 characters" }

    Dữ liệu sai nhiều chỗ cùng lúc (VD vừa thiếu username vừa sai email):
    Spring thu thập đủ mọi lỗi, nhưng GlobalExceptionHandler chỉ trả về 1 message (lỗi đứng đầu danh sách)
    để thông báo gọn. Thứ tự các lỗi trong danh sách không cố định,
    nên không đoán trước được lần này frontend sẽ nhận message nào.
    → Frontend nên kiểm tra dữ liệu ngay trên form trước khi gửi,
      backend kiểm tra lại lần nữa để chặn trường hợp ai đó gửi thẳng bằng Postman.

    -----------------------------------------------------
    Ý nghĩa từng annotation
    -----------------------------------------------------
        Annotation        Ý nghĩa                                         Giá trị null
        @NotBlank         không được null, rỗng "" hoặc chỉ toàn dấu cách  báo lỗi
        @Email            phải đúng định dạng email                       bỏ qua, coi như hợp lệ
        @Size(min, max)   độ dài chuỗi nằm trong khoảng [min, max]        bỏ qua, coi như hợp lệ
        @Pattern(regexp)  chuỗi phải khớp biểu thức chính quy (regex)     bỏ qua, coi như hợp lệ

    Vì sao thuộc tính nào cũng có @NotBlank đứng đầu?
    Vì @Email, @Size, @Pattern đều bỏ qua giá trị null (coi như hợp lệ).
    Chỉ dùng @Email mà không có @NotBlank thì frontend bỏ trống email vẫn lọt qua.
    → Luôn ghép @NotBlank (bắt buộc phải có) với các quy tắc kiểm tra hình thức.

    message = "...": câu thông báo trả về cho frontend khi vi phạm quy tắc.
    Viết tiếng Anh, ngắn gọn, cố định, để frontend so sánh được và tự dịch sang tiếng Việt.
*/
/*
    -----------------------------------------------------
    public class RegisterRequest
    -----------------------------------------------------
    - public: class công khai, các package khác (controller, service) dùng được.
    - class : khai báo 1 class, tức là 1 "khuôn" để tạo ra các đối tượng.
    - RegisterRequest: tên class, phải trùng với tên file RegisterRequest.java.

    Các thuộc tính đều là private:
    chỉ code bên trong class này đọc ghi trực tiếp được, bên ngoài phải đi qua getter và setter.
    Cách giấu dữ liệu này gọi là đóng gói (encapsulation), 1 nguyên tắc cơ bản của lập trình hướng đối tượng.
*/
public class RegisterRequest {
    /*
        -----------------------------------------------------
        username: tên đăng nhập
        -----------------------------------------------------
        private String username;
        - private : chỉ dùng được bên trong class này.
        - String  : kiểu chuỗi ký tự (chữ).
        - username: tên thuộc tính, cũng là tên key trong JSON.

        3 quy tắc, kiểm tra cùng lúc:

        1. @NotBlank(message = "Username is required")
           Bắt buộc phải có. null, "" hay "   " đều báo lỗi "Username is required".

        2. @Size(min = 5, max = 20, message = "...")
           Dài từ 5 đến 20 ký tự (tính cả 2 đầu: 5 và 20 đều hợp lệ).
           min = 5, max = 20, message = "..." là các tham số có tên:
           ghi rõ tên từng tham số nên thứ tự viết không quan trọng.
           Cột username trong bảng users dài tối đa 50 ký tự (length = 50 trong entity User),
           giới hạn 20 ở đây chặt hơn nên chắc chắn lưu vừa.

        3. @Pattern(regexp = "^[a-zA-Z0-9_.]+$", message = "...")
           Chỉ được dùng chữ cái tiếng Anh, chữ số, dấu gạch dưới và dấu chấm.
           regexp là viết tắt của regular expression (biểu thức chính quy, gọi tắt là regex):
           1 chuỗi mô tả "mẫu" mà giá trị phải khớp.

           Đọc regex ^[a-zA-Z0-9_.]+$ từng phần:
               ^          : bắt đầu chuỗi
               [ ... ]    : 1 ký tự thuộc danh sách trong ngoặc vuông
                 a-z      : chữ thường từ a đến z
                 A-Z      : chữ hoa từ A đến Z
                 0-9      : chữ số từ 0 đến 9
                 _ .      : dấu gạch dưới, dấu chấm
                            (trong ngoặc vuông, dấu chấm chỉ là dấu chấm bình thường)
               +          : ký tự ở ngay trước được lặp lại 1 hoặc nhiều lần
               $          : kết thúc chuỗi
           → Từ đầu tới cuối chuỗi, mọi ký tự đều phải thuộc danh sách trên.
           Nhờ có ^ và $, chỉ cần 1 ký tự lạ ở bất kỳ đâu là không khớp.

           Ví dụ:
               "maychieu"     → hợp lệ
               "may.chieu_99" → hợp lệ
               "may chieu"    → không hợp lệ (có dấu cách)
               "mâychiều"     → không hợp lệ (chữ có dấu tiếng Việt không nằm trong a-z)
               "may@chieu"    → không hợp lệ (dấu @ không có trong danh sách)
           Không cho dấu cách và ký tự đặc biệt để username dùng an toàn ở mọi nơi
           (hiển thị, đường dẫn URL, tìm kiếm...).

        Trùng username với người khác: annotation không kiểm tra được (phải hỏi database),
        nên AuthService kiểm tra và báo "Username already exists".
    */
    @NotBlank(message = "Username is required")
    @Size(min = 5, max = 20, message = "Username must be between 5 and 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_.]+$",
            message = "Username can only contain letters, numbers, dot and underscore")
    private String username;

    /*
        -----------------------------------------------------
        email: địa chỉ email
        -----------------------------------------------------
        Dùng để đăng nhập và nhận mã OTP.

        1. @NotBlank(message = "Email is required"): bắt buộc phải có.

        2. @Email(message = "Invalid email"): phải có dạng phần_tên@tên_miền.
           "maychieu@gmail.com" → hợp lệ.
           "maychieu.gmail.com" → không hợp lệ (thiếu @).
           "may chieu@gmail.com"→ không hợp lệ (có dấu cách).
           Lưu ý: @Email chỉ kiểm tra hình thức cơ bản. "maychieu@gmail" (thiếu .com) vẫn lọt qua,
           và email viết đúng định dạng nhưng không có thật cũng lọt qua.
           Email có thật và đúng là của người đăng ký hay không được kiểm tra bằng mã OTP:
           không nhận được mã thì không kích hoạt được tài khoản.

        3. @Size(max = 100, message = "Email must not exceed 100 characters"):
           Tối đa 100 ký tự, khớp với độ dài cột email trong bảng users (length = 100 trong entity User).
           Không có quy tắc này thì email dài hơn 100 ký tự sẽ gây lỗi lúc lưu vào MySQL (lỗi 500).
           Chỉ có max, không có min: không ghi min thì mặc định min = 0.

        Email đã có người dùng: AuthService kiểm tra và báo "Email already exists".
    */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    /*
        -----------------------------------------------------
        password: mật khẩu (dạng gốc, chưa băm)
        -----------------------------------------------------
        1. @NotBlank(message = "Password is required"): bắt buộc phải có.

        2. @Size(min = 6, max = 72, message = "Password must be between 6 and 72 characters"):
           - Ít nhất 6 ký tự: mật khẩu quá ngắn rất dễ bị đoán.
           - Nhiều nhất 72 ký tự: thuật toán băm BCrypt (dùng trong SecurityConfig)
             chỉ xử lý được tối đa 72 byte.
             Lưu ý: 72 byte không hẳn là 72 ký tự. Chữ tiếng Anh và chữ số chiếm 1 byte,
             còn chữ có dấu tiếng Việt chiếm 2 đến 3 byte. Mật khẩu toàn chữ có dấu, dài gần 72 ký tự
             có thể vượt 72 byte, khi đó Spring Security báo lỗi lúc băm và trả 400.
             Trường hợp này rất hiếm, vì hầu như không ai đặt mật khẩu dài như vậy.

        Mật khẩu gốc chỉ nằm trong đối tượng này trong lúc xử lý 1 request.
        AuthService băm bằng passwordEncoder.encode(...) rồi mới lưu, database không bao giờ chứa mật khẩu gốc.
    */
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 72, message = "Password must be between 6 and 72 characters")
    private String password;

    /*
        -----------------------------------------------------
        Getter và setter
        -----------------------------------------------------
        Thuộc tính là private nên bên ngoài không đọc ghi trực tiếp được, phải qua 2 loại hàm:
        - Getter (get...): trả về giá trị của thuộc tính.
          AuthController gọi request.getUsername() để lấy username truyền xuống Service.
        - Setter (set...): gán giá trị mới cho thuộc tính.
          Jackson gọi setUsername(...) khi đổi JSON thành đối tượng.

        Đọc dòng getter:
            public String getUsername() { return username; }
            - public     : bên ngoài gọi được.
            - String     : kiểu dữ liệu trả về, cùng kiểu với thuộc tính.
            - getUsername: tên hàm theo quy ước get + tên thuộc tính (viết hoa chữ cái đầu).
            - ()         : không nhận tham số.
            - return username;: trả về giá trị của thuộc tính username.

        Đọc dòng setter:
            public void setUsername(String username) { this.username = username; }
            - void                   : hàm không trả về gì.
            - (String username)      : nhận vào 1 tham số kiểu String, tên là username.
            - this.username = username;
                this.username: thuộc tính của đối tượng (khai báo ở trên).
                username     : tham số vừa được truyền vào.
              2 cái trùng tên, nên phải có this. để chỉ rõ đâu là thuộc tính.
              → Gán giá trị tham số vào thuộc tính.

        Mỗi hàm viết gọn trên 1 dòng, tương đương cách viết nhiều dòng:
            public String getUsername() {
                return username;
            }

        Phải đặt tên đúng quy ước get và set, vì Jackson và Spring dựa vào tên hàm để tìm.
        Đặt tên khác, VD layUsername(), thì Jackson không nhận ra.
    */
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}