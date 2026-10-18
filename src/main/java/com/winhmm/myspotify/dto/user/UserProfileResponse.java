package com.winhmm.myspotify.dto.user;

import com.winhmm.myspotify.enums.AccountStatus;
import com.winhmm.myspotify.enums.ArtistRequestStatus;
import com.winhmm.myspotify.enums.Role;

/*
    =====================================================
    USERPROFILERESPONSE - THÔNG TIN TÀI KHOẢN TRẢ VỀ CHO FRONTEND
    =====================================================

    Dùng cho:
    - UserController: xem và sửa thông tin cá nhân, đổi avatar, sửa hồ sơ Artist.
    - AdminController: danh sách tài khoản, danh sách yêu cầu làm Artist.

    JSON backend trả về:
        {
            "id": 6,
            "username": "maychieu",
            "email": "maychieu@gmail.com",
            "fullName": "Trần Mây Chiều",
            "avatarUrl": "/uploads/avatars/3f2c8a1e.jpg",
            "accountStatus": "ACTIVE",
            "role": "ARTIST",
            "artistRequestStatus": "APPROVED",
            "artistName": "Mây Chiều",
            "bio": "Ballad nhẹ nhàng cho buổi chiều."
        }

    -----------------------------------------------------
    Vì sao không trả thẳng entity User?
    -----------------------------------------------------
    Entity User có trường password (chuỗi đã băm BCrypt).
    Dù đã băm, lộ chuỗi băm vẫn giúp kẻ gian thử dò mật khẩu trên máy của họ.
    DTO này liệt kê rõ những trường được phép trả ra, không có password.

    -----------------------------------------------------
    3 trạng thái độc lập của 1 tài khoản
    -----------------------------------------------------
    - accountStatus: tài khoản có dùng được không.
        UNVERIFIED : đã đăng ký, chưa xác thực OTP.
        ACTIVE     : đang hoạt động.
        DISABLED   : bị Admin khóa, hoặc tự vô hiệu hóa.
        DELETED    : đã xóa mềm.
    - role: quyền trong hệ thống.
        USER   : người nghe.
        ARTIST : nghệ sĩ, được đăng bài hát.
        ADMIN  : quản trị viên.
    - artistRequestStatus: tiến trình yêu cầu làm Artist.
        NONE     : chưa gửi yêu cầu.
        PENDING  : đang chờ Admin duyệt.
        APPROVED : đã được duyệt (role đã thành ARTIST).
        REJECTED : bị từ chối, được gửi lại.

    Ví dụ: 1 Artist bị khóa có role = ARTIST, accountStatus = DISABLED, artistRequestStatus = APPROVED.

    -----------------------------------------------------
    Các trường có thể null
    -----------------------------------------------------
    - fullName  : chưa cập nhật họ tên.
    - avatarUrl : chưa đổi ảnh đại diện → frontend hiển thị ảnh mặc định.
    - artistName, bio : chưa từng gửi yêu cầu làm Artist, hoặc bị từ chối.

    -----------------------------------------------------
    Vì sao chỉ có getter, không có setter?
    -----------------------------------------------------
    Đây là DTO trả về (response): backend tạo bằng constructor rồi gửi đi, không ai sửa nữa.
    Jackson chỉ cần getter để đọc giá trị và tạo JSON:
    getUsername() → "username", getRole() → "role"...
    Enum được chuyển thành chuỗi theo tên: Role.ARTIST → "ARTIST".
*/
public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String avatarUrl;
    private AccountStatus accountStatus;
    private Role role;
    private ArtistRequestStatus artistRequestStatus;
    private String artistName;
    private String bio;

    public UserProfileResponse(Long id, String username, String email, String fullName,
                               String avatarUrl, AccountStatus accountStatus, Role role,
                               ArtistRequestStatus artistRequestStatus, String artistName, String bio) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
        this.accountStatus = accountStatus;
        this.role = role;
        this.artistRequestStatus = artistRequestStatus;
        this.artistName = artistName;
        this.bio = bio;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getAvatarUrl() { return avatarUrl; }
    public AccountStatus getAccountStatus() { return accountStatus; }
    public Role getRole() { return role; }
    public ArtistRequestStatus getArtistRequestStatus() { return artistRequestStatus; }
    public String getArtistName() { return artistName; }
    public String getBio() { return bio; }
}
