package com.winhmm.myspotify.dto.auth;

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
    Ý nghĩa từng trường
    -----------------------------------------------------
    1. token:
       - JWT do backend tạo khi đăng nhập thành công, sống 24 giờ.
       - Chứa email (sub) và mã phiên (jti), không chứa mật khẩu hay role.
       - Frontend lưu lại và gửi kèm ở mọi request sau:
             Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
       - Token mất hiệu lực sớm hơn 24 giờ khi: đăng xuất, đăng nhập ở thiết bị khác,
         đổi mật khẩu, đổi email, bị Admin khóa.

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

    -----------------------------------------------------
    Enum Role chuyển thành JSON thế nào?
    -----------------------------------------------------
    Jackson tự chuyển enum thành chuỗi theo tên: Role.ARTIST → "ARTIST".

    Jackson đọc các getter để tạo JSON: getToken() → "token", getUserId() → "userId"...
*/
public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
    private Role role;

    public LoginResponse(String token, Long userId, String username, Role role) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}

