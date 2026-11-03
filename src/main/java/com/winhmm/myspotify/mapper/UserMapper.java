package com.winhmm.myspotify.mapper;

import com.winhmm.myspotify.dto.user.UserProfileResponse;
import com.winhmm.myspotify.entity.User;

/*
    =====================================================
    PACKAGE MAPPER - THỨ TỰ ĐỌC
    =====================================================

    1. UserMapper (file này): ngắn, chỉ 1 hàm. Đọc trước để hiểu mapper là gì.
    2. SongMapper           : dài hơn, có lọc dữ liệu, tính toán, phân trang, generic.

    =====================================================
    USERMAPPER - CHUYỂN ENTITY USER SANG DTO
    =====================================================

    Gom việc chuyển User (entity) sang UserProfileResponse (DTO) về 1 chỗ duy nhất.

    -----------------------------------------------------
    Entity và DTO khác nhau thế nào? (đọc nếu mới học)
    -----------------------------------------------------
    - Entity User: đại diện 1 dòng trong bảng users, có ĐỦ mọi cột
      (kể cả password đã băm, access_status...). Dùng để đọc và ghi database.
    - DTO UserProfileResponse: chỉ chứa những gì frontend cần hiển thị.
      Spring (Jackson) đổi DTO thành JSON trả về.
    - Mapper: "người phiên dịch", đọc dữ liệu từ entity rồi tạo DTO.

        Database ──► UserRepository ──► UserService ──► UserController ──► UserMapper ──► JSON
                       (User)            (User)          (User)            (DTO)

    -----------------------------------------------------
    Được dùng ở đâu?
    -----------------------------------------------------
        UserController:
            GET  /api/users/me/profile          xem thông tin cá nhân
            PUT  /api/users/me/profile          sửa họ tên
            POST /api/users/me/avatar           đổi ảnh đại diện
            PUT  /api/users/me/artist-profile   sửa nghệ danh, bio (Artist)
        AdminController:
            GET  /api/admin/users               danh sách mọi tài khoản
            GET  /api/admin/artist-requests     danh sách yêu cầu làm Artist đang chờ duyệt

    -----------------------------------------------------
    Vì sao tách ra class riêng?
    -----------------------------------------------------
    Trước đây hàm chuyển đổi này bị copy y hệt ở UserController và AdminController.
    Tách ra đây theo nguyên tắc Single Responsibility (chữ S trong SOLID):
    - Controller chỉ lo nhận request và trả response.
    - Mapper chỉ lo chuyển đổi dữ liệu.
    → Thêm 1 trường mới vào UserProfileResponse thì chỉ sửa đúng file này.

    -----------------------------------------------------
    Vì sao không trả thẳng entity User ra ngoài?
    -----------------------------------------------------
    Nếu Controller trả về User, Jackson đổi MỌI trường có getter thành JSON:
        { "id": 6, "username": "winhmm", "password": "$2a$10$Xk9...", "accessStatus": "ACCEPT", ... }
    - Lộ mật khẩu đã băm (password): kẻ gian có thể mang về máy dò thử.
    - Lộ thông tin quản trị nội bộ (accessStatus, xem enum AccessStatus).
    - Sau này thêm cột mới vào bảng users → cột đó tự lộ ra JSON mà không ai để ý.
    Dùng DTO thì chỉ trường nào được liệt kê trong mapper mới ra ngoài: an toàn mặc định.

    -----------------------------------------------------
    Vì sao toàn hàm static?
    -----------------------------------------------------
    Mapper không lưu trạng thái gì (không có thuộc tính, không cần database):
    nhận User vào, trả DTO ra, như 1 hàm toán học.
    → Gọi thẳng UserMapper.toUserProfileResponse(user), không cần new UserMapper()
      hay để Spring tiêm vào. Giống cách dùng SongMapper.

    static nghĩa là hàm thuộc về CLASS, không thuộc về 1 object cụ thể:
        Không static: UserMapper mapper = new UserMapper();  mapper.toUserProfileResponse(user);
        Có static   : UserMapper.toUserProfileResponse(user);
*/
public class UserMapper {

    /*
        -----------------------------------------------------
        Constructor private: cấm tạo object
        -----------------------------------------------------
        - Mọi hàm đều static nên không bao giờ cần new UserMapper().
        - Nếu không viết constructor nào, Java tự tạo 1 constructor public rỗng
          → ai cũng new UserMapper() được (vô nghĩa).
        - Viết constructor private rỗng → chỉ code bên trong class này gọi được
          → bên ngoài viết new UserMapper() sẽ báo lỗi biên dịch.
        Đây là cách viết quen thuộc cho "class tiện ích" (utility class), VD Math, Collections của Java.
    */
    private UserMapper() {}

    /*
        -----------------------------------------------------
        User → UserProfileResponse
        -----------------------------------------------------
        Lấy đủ 10 trường frontend cần hiển thị:

            Trường                  Lấy từ                         VD giá trị
            id                      user.getId()                   6
            username                user.getUsername()             "winhmm"
            email                   user.getEmail()                "winhmm@gmail.com"
            fullName                user.getFullName()             "Nguyễn Văn A" (có thể null)
            avatarUrl               user.getAvatarUrl()            "/uploads/avatars/3f2a....jpg" (có thể null)
            accountStatus           user.getAccountStatus()        ACTIVE
            role                    user.getRole()                 USER
            artistRequestStatus     user.getArtistRequestStatus()  NONE
            artistName              user.getArtistName()           null nếu chưa xin làm Artist
            bio                     user.getBio()                  null nếu chưa xin làm Artist

        Bỏ ra ngoài: password (mật khẩu đã băm), accessStatus (quyền vào web, chỉ chủ dự án quản lý).

        JSON frontend nhận được (VD GET /api/users/me/profile):
            {
                "id": 6,
                "username": "winhmm",
                "email": "winhmm@gmail.com",
                "fullName": null,
                "avatarUrl": null,
                "accountStatus": "ACTIVE",
                "role": "USER",
                "artistRequestStatus": "NONE",
                "artistName": null,
                "bio": null
            }
        Enum được Jackson đổi thành chuỗi tên hằng ("ACTIVE", "USER"...).

        -----------------------------------------------------
        Lưu ý khi sửa: thứ tự tham số phải khớp constructor
        -----------------------------------------------------
        new UserProfileResponse(...) truyền 10 giá trị theo ĐÚNG thứ tự của constructor trong DTO:
            (id, username, email, fullName, avatarUrl, accountStatus, role, artistRequestStatus, artistName, bio)
        Có 6 tham số cùng kiểu String. Nếu lỡ đảo chỗ 2 tham số String, VD email và fullName,
        Java vẫn biên dịch được (vì cùng kiểu) nhưng JSON sẽ hiện họ tên ở chỗ email.
        → Khi thêm hoặc bớt trường, mở UserProfileResponse ra đối chiếu từng vị trí.
        (SongResponse có tới 13 trường nên dùng Builder, gọi theo tên trường, không lo nhầm vị trí,
         xem SongMapper.toSongResponse.)

        Được gọi thế nào?
        - UserController: UserMapper.toUserProfileResponse(user)
        - AdminController: trong stream, viết gọn bằng method reference:
              adminService.getAllUsers()
                      .stream()
                      .map(UserMapper::toUserProfileResponse)   // = .map(user -> UserMapper.toUserProfileResponse(user))
                      .collect(Collectors.toList());
          → đổi cả danh sách List<User> thành List<UserProfileResponse>.
    */
    public static UserProfileResponse toUserProfileResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatarUrl(),
                user.getAccountStatus(),
                user.getRole(),
                user.getArtistRequestStatus(),
                user.getArtistName(),
                user.getBio()
        );
    }
}