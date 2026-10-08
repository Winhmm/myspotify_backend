package com.winhmm.myspotify.dto.auth;

import com.winhmm.myspotify.enums.Role;

/*
    LoginResponse là DTO dùng để chứa các thông tin Backend trả về cho Frontend
    sau khi người dùng đăng nhập thành công.

    Response này gồm 4 thông tin chính:
    1. token:
    - Là JWT được Backend tạo sau khi đăng nhập thành công.

    - Frontend cần lưu token này và gửi kèm trong các request tiếp theo
    để Backend biết người gửi request đã đăng nhập và xác thực được User.

    2. userId:
    - Là ID của User trong Database.

    - Frontend cần userId để biết chính xác tài khoản nào đang đăng nhập
    và có thể sử dụng ID này khi cần tham chiếu đến User.

    3. username:
    - Là tên của User.

    - Trả về username để Frontend có thể sử dụng ngay sau khi đăng nhập,
    ví dụ hiển thị tên người dùng trên giao diện.

    4. role:
    - Là quyền của User trong hệ thống.

    - Ví dụ: USER, ARTIST, ADMIN.

    - Frontend có thể dựa vào role để hiển thị hoặc ẩn những chức năng
    tương ứng với quyền của người dùng.

    Ví dụ sau khi đăng nhập thành công, Backend có thể trả về:
    {
        "token": "eyJhbGciOiJIUzI1NiJ9...",
        "userId": 1,
        "username": "phong123",
        "role": "USER"
    }
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

