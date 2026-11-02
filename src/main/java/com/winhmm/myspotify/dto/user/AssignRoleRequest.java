package com.winhmm.myspotify.dto.user;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    (Các khái niệm cơ bản: xem RegisterRequest. Thứ tự đọc package dto.user: xem UpdateProfileRequest.)

    - Role   : enum role của tài khoản, gồm 3 giá trị USER, ARTIST, ADMIN (package enums).
    - NotNull: không được null (dùng cho mọi kiểu đối tượng, không riêng chuỗi).
*/
import com.winhmm.myspotify.enums.Role;
import jakarta.validation.constraints.NotNull;

/*
    =====================================================
    ASSIGNROLEREQUEST - ADMIN GÁN ROLE CHO 1 TÀI KHOẢN (UC18)
    =====================================================

    API: PUT /api/admin/users/{userId}/role   (xem AdminController.assignRole)

    Ví dụ: nâng tài khoản id 7 lên ADMIN
        PUT /api/admin/users/7/role
        {
            "role": "ADMIN"
        }

    Kết quả: 200 { "message": "Role has been assigned successfully" }

    Ai được gọi? Chỉ tài khoản ADMIN (@PreAuthorize("hasRole('ADMIN')") trên AdminController).
    Người khác gọi → 403 "You do not have permission to access this resource".

    Id tài khoản nằm trên đường dẫn (/users/7/role), không nằm trong DTO.
    Quy ước REST: đường dẫn cho biết "thao tác trên tài khoản nào",
    body chỉ chứa "dữ liệu muốn thay đổi".

    =====================================================
    ENUM TRONG JSON
    =====================================================

    Thuộc tính role có kiểu Role (enum), không phải String.
    - Jackson tự đổi chuỗi trong JSON sang hằng số enum cùng tên: "ADMIN" → Role.ADMIN.
    - Chuỗi không có trong enum (VD "SUPERADMIN") → Jackson không đổi được
      → lỗi HttpMessageNotReadableException → GlobalExceptionHandler trả 400 "Invalid request data".
    → Không bao giờ có role lạ lọt vào Service.

    Vì sao không dùng String?
    Dùng String thì phải tự viết code kiểm tra "chuỗi này có phải USER, ARTIST hay ADMIN không",
    quên kiểm tra là lưu nhầm role lạ vào database. Dùng enum thì Jackson kiểm tra hộ,
    và IntelliJ báo lỗi ngay nếu trong code gõ sai tên (VD Role.ADMNI).

    =====================================================
    QUY TẮC CỦA SERVICE (AdminService.assignRole)
    =====================================================

    DTO chỉ kiểm tra "có gửi role không". Các quy tắc nghiệp vụ nằm ở Service:
    1. Không gán ARTIST ở đây
       → 400 "ARTIST role can only be granted by approving an artist request".
       Artist bắt buộc có nghệ danh, mà DTO này không có trường nghệ danh.
       Muốn thành Artist phải đi đúng quy trình gửi yêu cầu (ArtistRequest) và Admin duyệt.
    2. Không đổi role của ADMIN → 400 "Cannot change role of an admin account".
    3. Không gán cho tài khoản DELETED hoặc UNVERIFIED
       → 400 "Cannot assign role to a deleted or unverified account".
    4. Không gán trùng role hiện tại → 400 "User already has this role".
    5. Hạ 1 ARTIST xuống role khác, hoặc người đang có yêu cầu làm Artist PENDING
       → xóa nghệ danh, bio, đưa artistRequestStatus về NONE.

    → Thực tế chỉ có 2 giá trị dùng được:
        "ADMIN" → nâng 1 tài khoản lên Admin.
        "USER"  → hạ 1 Artist về người nghe thường (thu hồi quyền Artist).

    Role mới có tác dụng ngay ở request tiếp theo của người đó, không cần đăng nhập lại:
    mỗi request, JwtAuthenticationFilter đọc role hiện tại từ MySQL (không đọc role ghi trong token).
*/
public class AssignRoleRequest {
    /*
        -----------------------------------------------------
        role: role mới muốn gán
        -----------------------------------------------------
        - @NotNull: bắt buộc phải có → "Role must not be null".
          Không gửi trường role, hoặc gửi "role": null → role = null → bị chặn.

        Vì sao @NotNull mà không phải @NotBlank?
        @NotBlank chỉ dùng được cho chuỗi (kiểm tra "rỗng", "toàn dấu cách").
        Role là enum, không có khái niệm "toàn dấu cách" → dùng @NotNull.
        Đặt @NotBlank lên 1 thuộc tính enum sẽ bị lỗi khi chạy (không có bộ kiểm tra nào cho kiểu này).
        (Bảng so sánh @NotNull, @NotEmpty, @NotBlank: xem UploadSongRequest trong dto.song.)

        Service còn kiểm tra role == null 1 lần nữa ("Role is required"),
        phòng khi Controller quên ghi @Valid.
    */
    @NotNull(message = "Role must not be null")
    private Role role;

    /*
        -----------------------------------------------------
        Getter và setter
        -----------------------------------------------------
        - Setter: Jackson gọi khi đổi JSON thành đối tượng ("role" → setRole(Role.ADMIN)).
          Lúc gọi setter, chuỗi "ADMIN" đã được Jackson đổi thành Role.ADMIN.
        - Getter: AdminController gọi request.getRole() để truyền xuống AdminService.assignRole.
        Cách đọc từng dòng: xem RegisterRequest.
    */
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}