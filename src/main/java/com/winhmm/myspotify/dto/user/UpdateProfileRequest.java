package com.winhmm.myspotify.dto.user;

/*
    -----------------------------------------------------
    Package dto.user: thứ tự nên đọc
    -----------------------------------------------------
    Package dto.user có 9 file, phục vụ 2 controller:
    - UserController : người dùng tự quản lý tài khoản của mình (đường dẫn /api/users/me/...).
    - AdminController: Admin quản lý tài khoản của người khác (đường dẫn /api/admin/...).
    Các khái niệm cơ bản (package, import, DTO, annotation, validation, getter, setter, Jackson)
    đã giải thích chi tiết trong RegisterRequest (package dto.auth).

    Nhóm 1 - Request DTO của người dùng (UserController):
        1. UpdateProfileRequest            → sửa họ tên (file này, đọc trước)
        2. RequestChangePasswordOtpRequest → đổi mật khẩu, bước 1: nhập mật khẩu hiện tại để nhận OTP
        3. ChangePasswordRequest           → đổi mật khẩu, bước 2: nhập OTP và mật khẩu mới
        4. RequestUpdateEmailOtpRequest    → đổi email, bước 1: nhập email mới để nhận OTP
        5. UpdateEmailRequest              → đổi email, bước 2: nhập OTP
        6. AccountActionRequest            → tự vô hiệu hóa hoặc tự xóa tài khoản (nhập mật khẩu xác nhận)
        7. ArtistRequest                   → gửi yêu cầu làm Artist, sửa hồ sơ Artist

    Nhóm 2 - Request DTO của Admin (AdminController):
        8. AssignRoleRequest → gán role cho 1 tài khoản (giải thích enum trong JSON)

    Nhóm 3 - Response DTO:
        9. UserProfileResponse → thông tin tài khoản trả về frontend (không có mật khẩu)

    -----------------------------------------------------
    Điểm chung của nhóm 1: không có id, không có email của người dùng
    -----------------------------------------------------
    Backend tự biết "ai đang gọi" nhờ token đăng nhập:
        token → email ghi trong token → tìm User trong MySQL → id
    (xem hàm me(authentication) trong UserController).
    → Người dùng chỉ sửa được tài khoản của chính mình.
      Có cố gửi thêm "id": 99 trong JSON thì Jackson cũng bỏ qua, vì DTO không có thuộc tính id.

    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    - NotBlank: không được null, rỗng hoặc chỉ toàn dấu cách.
    - Size    : độ dài chuỗi nằm trong khoảng cho phép.
*/
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/*
    =====================================================
    UPDATEPROFILEREQUEST - SỬA HỌ TÊN (UC08)
    =====================================================

    API: PUT /api/users/me/profile   (xem UserController.updateProfile)

    JSON frontend gửi lên:
        {
            "fullName": "Trần Mây Chiều"
        }

    Kết quả: 200, trả về UserProfileResponse đã có họ tên mới
    → frontend cập nhật giao diện luôn, không cần gọi lại GET /api/users/me/profile.

    -----------------------------------------------------
    Vì sao DTO chỉ có đúng 1 trường?
    -----------------------------------------------------
    API này chỉ cho sửa họ tên. Các thông tin khác đổi qua API riêng, vì cần bảo vệ kỹ hơn:
    - username     : không cho đổi (là định danh cố định của tài khoản).
    - email        : cần OTP gửi tới email mới (RequestUpdateEmailOtpRequest, UpdateEmailRequest).
    - mật khẩu     : cần mật khẩu hiện tại và OTP (RequestChangePasswordOtpRequest, ChangePasswordRequest).
    - ảnh đại diện : gửi file qua form-data (POST /api/users/me/avatar), không dùng DTO.
    - role, trạng thái tài khoản: chỉ Admin đổi được.

    Nếu dùng chung 1 DTO "sửa mọi thứ" có cả email, role..., chỉ cần quên kiểm tra 1 trường
    là người dùng tự nâng mình lên ADMIN được.
    Mỗi API 1 DTO nhỏ, chỉ chứa đúng trường được phép sửa → không có cách nào lách.

    -----------------------------------------------------
    Spring điền dữ liệu thế nào?
    -----------------------------------------------------
    Controller khai báo: @Valid @RequestBody UpdateProfileRequest request
    1. Jackson tạo đối tượng bằng constructor rỗng mặc định
       (Java tự sinh vì class không khai báo constructor nào).
    2. Key "fullName" trong JSON → Jackson gọi setFullName("Trần Mây Chiều").
    3. @Valid kiểm tra 2 annotation của fullName. Vi phạm → 400, không chạy vào Service.
    4. Controller gọi request.getFullName() → truyền xuống UserService.updateProfile.
*/
public class UpdateProfileRequest {
    /*
        -----------------------------------------------------
        fullName: họ tên mới
        -----------------------------------------------------
        - @NotBlank: bắt buộc, không được rỗng hay toàn dấu cách → "Full name is required".
          Hệ quả: đã đặt họ tên rồi thì không xóa về trống được, chỉ đổi sang tên khác.
          (Tài khoản mới đăng ký luôn có fullName = null, vì lúc đăng ký không hỏi họ tên.)
        - @Size(max = 100): tối đa 100 ký tự → "Full name must not exceed 100 characters".
          Khớp với độ dài cột full_name (length = 100) trong entity User.
          Không có quy tắc này thì tên dài hơn 100 ký tự sẽ gây lỗi lúc lưu MySQL.

        Không có @Pattern: họ tên có thể chứa chữ có dấu, dấu cách, dấu gạch ngang...
        Rất khó viết 1 quy tắc đúng cho mọi tên người, nên chỉ giới hạn độ dài.

        Lưu ý: Service lưu nguyên văn, không cắt dấu cách thừa ở 2 đầu.
        Gửi "  Mây Chiều  " → lưu đúng "  Mây Chiều  ".
        → Frontend nên gọi .trim() trước khi gửi.
    */
    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;

    /*
        -----------------------------------------------------
        Getter và setter
        -----------------------------------------------------
        - Setter: Jackson gọi khi đổi JSON thành đối tượng ("fullName" → setFullName).
          Tên key trong JSON phải đúng là "fullName". Gửi sai tên, VD "full_name"
          → Jackson không tìm thấy setter → bỏ qua → fullName = null → 400 "Full name is required".
        - Getter: UserController gọi request.getFullName().
        Cách đọc từng dòng: xem RegisterRequest.
    */
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
}