package com.winhmm.myspotify.dto.user;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    (Các khái niệm cơ bản: xem RegisterRequest. Thứ tự đọc package dto.user: xem UpdateProfileRequest.)

    3 enum trạng thái của tài khoản (package enums), ý nghĩa từng giá trị xem bên dưới:
    - AccountStatus      : tài khoản có dùng được không.
    - ArtistRequestStatus: tiến trình yêu cầu làm Artist.
    - Role               : quyền trong hệ thống.
*/
import com.winhmm.myspotify.enums.AccountStatus;
import com.winhmm.myspotify.enums.ArtistRequestStatus;
import com.winhmm.myspotify.enums.Role;

/*
    =====================================================
    USERPROFILERESPONSE - THÔNG TIN TÀI KHOẢN TRẢ VỀ CHO FRONTEND
    =====================================================

    Dùng cho:
    - UserController (trả về 1 đối tượng):
        GET  /api/users/me/profile         → xem thông tin cá nhân (UC07)
        PUT  /api/users/me/profile         → kết quả sau khi sửa họ tên (UC08)
        POST /api/users/me/avatar          → kết quả sau khi đổi ảnh đại diện (UC10)
        PUT  /api/users/me/artist-profile  → kết quả sau khi sửa hồ sơ Artist (UC19)
    - AdminController (trả về 1 mảng):
        GET /api/admin/users               → danh sách tất cả tài khoản (UC15)
        GET /api/admin/artist-requests     → danh sách yêu cầu làm Artist đang chờ duyệt (UC17)

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
    1. Mật khẩu: entity User có trường password (chuỗi đã băm BCrypt).
       Dù đã băm, lộ chuỗi băm vẫn giúp kẻ gian thử dò mật khẩu trên máy của họ.
       Entity đã có @JsonIgnore trên password, nhưng đó chỉ là 1 annotation, lỡ tay xóa là lộ ngay.
       DTO này không có trường password ngay từ đầu → không có cách nào lộ.
    2. Trường nội bộ: entity có accessStatus (quyền vào web, chỉ chủ dự án đổi bằng SQL).
       Frontend không cần biết, nên DTO không đưa vào.
    3. Tách biệt: sau này entity User thêm cột mới, JSON trả về vẫn giữ nguyên,
       frontend không bị ảnh hưởng.

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
        NONE     : chưa gửi yêu cầu, hoặc bị Admin đổi role khi đang là Artist
                   hay đang chờ duyệt (xem AssignRoleRequest).
        PENDING  : đang chờ Admin duyệt.
        APPROVED : đã được duyệt (role đã thành ARTIST).
        REJECTED : bị từ chối, được gửi lại.

    Ví dụ: 1 Artist bị khóa có role = ARTIST, accountStatus = DISABLED, artistRequestStatus = APPROVED.

    -----------------------------------------------------
    Các trường có thể null
    -----------------------------------------------------
    - fullName  : chưa cập nhật họ tên (tài khoản mới đăng ký luôn là null, vì đăng ký không hỏi họ tên).
    - avatarUrl : chưa đổi ảnh đại diện → frontend hiển thị ảnh mặc định.
    - artistName, bio : chưa từng gửi yêu cầu làm Artist, bị từ chối, hoặc bị Admin đổi role.
    - bio       : còn có thể null ngay cả với Artist, vì bio không bắt buộc (xem ArtistRequest).

    -----------------------------------------------------
    Vì sao chỉ có getter, không có setter?
    -----------------------------------------------------
    Đây là DTO trả về (response): backend tạo bằng constructor rồi gửi đi, không ai sửa nữa.
    Jackson chỉ cần getter để đọc giá trị và tạo JSON:
    getUsername() → "username", getRole() → "role"...
    Enum được chuyển thành chuỗi theo tên: Role.ARTIST → "ARTIST".
*/
public class UserProfileResponse {
    /*
        10 thuộc tính, đều private. Ý nghĩa: xem JSON và các phần giải thích ở trên.
        - id, username, email: định danh của tài khoản (username và email không trùng với ai).
        - avatarUrl: đường dẫn tương đối, frontend ghép thêm địa chỉ server
          (VD "http://157.66.100.87:8080" + avatarUrl) để hiển thị ảnh.
    */
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

    /*
        -----------------------------------------------------
        Constructor 10 tham số
        -----------------------------------------------------
        Được gọi duy nhất trong UserMapper.toUserProfileResponse(user):
            new UserProfileResponse(user.getId(), user.getUsername(), user.getEmail(),
                                    user.getFullName(), user.getAvatarUrl(), ...)

        Phải truyền đúng thứ tự khai báo. Có nhiều tham số cùng kiểu String nằm cạnh nhau
        (username, email, fullName, avatarUrl), truyền nhầm thứ tự thì trình biên dịch không báo lỗi,
        dữ liệu bị đổi chỗ âm thầm (VD email hiện vào ô họ tên).
        Ở đây vẫn dùng constructor thường vì chỉ có đúng 1 chỗ tạo đối tượng (UserMapper), dễ kiểm soát.
        Sau này nếu thêm nhiều trường, có thể chuyển sang Builder như SongResponse (dto.song).

        Constructor dài nên xuống dòng giữa danh sách tham số cho dễ đọc.
    */
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

    /*
        Getter: Jackson gọi để tạo JSON, tên key lấy theo tên getter (bỏ "get", viết thường chữ cái đầu):
        getFullName() → "fullName", getArtistRequestStatus() → "artistRequestStatus"...
    */
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