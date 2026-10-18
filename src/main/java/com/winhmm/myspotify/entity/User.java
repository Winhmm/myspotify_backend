package com.winhmm.myspotify.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.winhmm.myspotify.enums.AccountStatus;
import com.winhmm.myspotify.enums.ArtistRequestStatus;
import com.winhmm.myspotify.enums.Role;
import jakarta.persistence.*;

/*
    =====================================================
    USER - BẢNG USERS (TÀI KHOẢN NGƯỜI DÙNG)
    =====================================================

    -----------------------------------------------------
    Entity là gì?
    -----------------------------------------------------
    Entity là class Java đại diện cho 1 bảng trong database.
    Mỗi đối tượng User tương ứng với 1 dòng trong bảng users.
    Hibernate (JPA) tự chuyển qua lại giữa đối tượng Java và dòng dữ liệu:
    - userRepository.save(user)   → INSERT hoặc UPDATE
    - userRepository.findById(6)  → SELECT ... WHERE id = 6

    - @Entity: đánh dấu class này là entity.
    - @Table(name = "users"): tên bảng trong database.
      Không đặt tên bảng là "user" vì đó là từ khóa trong nhiều hệ quản trị database.

    -----------------------------------------------------
    Index
    -----------------------------------------------------
    Index giống mục lục của sách: giúp database tìm nhanh mà không phải đọc hết bảng.
    - idx_users_artist_request_status: Admin lọc danh sách yêu cầu làm Artist đang chờ
      (WHERE artist_request_status = 'PENDING').
    - Cột username và email có unique = true → database tự tạo index duy nhất,
      nên đăng nhập bằng email vẫn tìm rất nhanh.

    -----------------------------------------------------
    Một tài khoản có 3 trạng thái độc lập
    -----------------------------------------------------
    - accountStatus      : tài khoản có dùng được không (AccountStatus).
    - role               : quyền trong hệ thống (Role).
    - artistRequestStatus: tiến trình xin làm Artist (ArtistRequestStatus).
    Ví dụ: Artist bị khóa → role = ARTIST, accountStatus = DISABLED, artistRequestStatus = APPROVED.

    -----------------------------------------------------
    Lưu ý về VPS
    -----------------------------------------------------
    VPS dùng ddl-auto=validate: Hibernate chỉ kiểm tra bảng có khớp với entity không,
    không tự sửa bảng. Thêm hoặc sửa field ở đây thì phải tự chạy ALTER TABLE trên VPS
    trước khi deploy, nếu không ứng dụng sẽ không khởi động được.
*/
@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_artist_request_status", columnList = "artist_request_status")
        }
)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @JsonIgnore
    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", length = 20, nullable = false)
    private AccountStatus accountStatus = AccountStatus.UNVERIFIED;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "artist_request_status", length = 20, nullable = false)
    private ArtistRequestStatus artistRequestStatus = ArtistRequestStatus.NONE;

    @Column(name = "artist_name", length = 100)
    private String artistName;

    @Column(length = 255)
    private String bio;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public AccountStatus getAccountStatus() { return accountStatus; }
    public void setAccountStatus(AccountStatus accountStatus) { this.accountStatus = accountStatus; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public ArtistRequestStatus getArtistRequestStatus() { return artistRequestStatus; }
    public void setArtistRequestStatus(ArtistRequestStatus artistRequestStatus) { this.artistRequestStatus = artistRequestStatus; }

    public String getArtistName() { return artistName; }
    public void setArtistName(String artistName) { this.artistName = artistName; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
