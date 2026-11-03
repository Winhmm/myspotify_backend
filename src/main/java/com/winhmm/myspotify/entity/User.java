package com.winhmm.myspotify.entity;

/*
    -----------------------------------------------------
    Package entity: thứ tự nên đọc
    -----------------------------------------------------
    Package entity có 7 file, mỗi file là 1 bảng trong MySQL.

        1. User            → bảng users (file này, đọc trước: giải thích entity và các annotation cơ bản)
        2. Genre           → bảng genres (entity đơn giản nhất, chỉ đọc không ghi)
        3. OtpVerification → bảng otp_verifications (quan hệ @ManyToOne đầu tiên)
        4. Song            → bảng songs (nhiều quan hệ, @ManyToMany, @Version, lifecycle callback)
        5. SongLyrics      → bảng song_lyrics (quan hệ @OneToOne, cột kiểu TEXT)
        6. Playlist        → bảng playlists (thuộc tính boolean tên isPublic)
        7. PlaylistSong    → bảng playlist_songs (bảng trung gian viết thành entity riêng)

    Lịch sử nghe nhạc không nằm ở đây mà nằm trong MongoDB (document ListeningHistory, package document).

    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    - JsonIgnore: annotation của Jackson, bảo Jackson bỏ qua 1 thuộc tính khi đổi đối tượng thành JSON.
      (Spring Boot 4 dùng Jackson 3, nhưng các annotation vẫn nằm ở gói com.fasterxml.jackson.annotation.)
    - AccessStatus, AccountStatus, ArtistRequestStatus, Role: 4 enum trạng thái của tài khoản (package enums).
    - jakarta.persistence.*: mượn tất cả annotation của JPA (Entity, Table, Id, Column, Enumerated...).
      Dấu * nghĩa là "tất cả class trong gói này", đỡ phải viết 10 dòng import.

    JPA (Jakarta Persistence API) là bộ quy tắc chung để ánh xạ class Java ↔ bảng database.
    Hibernate là thư viện làm theo bộ quy tắc đó (giống @NotBlank là quy tắc, Hibernate Validator là người kiểm tra).
    Spring Data JPA (các Repository) chạy Hibernate ở bên dưới.
*/
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.winhmm.myspotify.enums.AccessStatus;
import com.winhmm.myspotify.enums.AccountStatus;
import com.winhmm.myspotify.enums.ArtistRequestStatus;
import com.winhmm.myspotify.enums.Role;
import jakarta.persistence.*;

/*
    =====================================================================
    USER - ENTITY ÁNH XẠ BẢNG users (TÀI KHOẢN NGƯỜI DÙNG)
    =====================================================================

    -----------------------------------------------------
    Entity là gì?
    -----------------------------------------------------
    Entity là class Java đại diện cho 1 bảng trong database.
        - 1 class      ↔ 1 bảng  (User ↔ users)
        - 1 đối tượng  ↔ 1 dòng  (1 đối tượng User ↔ 1 tài khoản)
        - 1 thuộc tính ↔ 1 cột   (email ↔ cột email)

    Hibernate tự chuyển qua lại giữa đối tượng Java và dòng dữ liệu, tự viết câu SQL:
        userRepository.save(user)    → INSERT (tài khoản mới) hoặc UPDATE (tài khoản đã có)
        userRepository.findById(6L)  → SELECT ... FROM users WHERE id = 6
    → Mình làm việc với đối tượng Java, không phải tự viết SQL cho các thao tác cơ bản.

    - @Entity: đánh dấu class này là entity, Hibernate sẽ quản lý nó.
    - @Table(name = "users"): tên bảng trong database.
      Không đặt tên bảng là "user" vì đó là từ khóa trong nhiều hệ quản trị database.

    Ví dụ dữ liệu trong bảng users:
        id | username  | email              | role   | account_status | access_status | artist_name
        6  | maychieu  | maychieu@gmail.com | ARTIST | ACTIVE         | ACCEPT        | Mây Chiều
        7  | songnho   | songnho@gmail.com  | USER   | UNVERIFIED     | REFUSE        | NULL

    -----------------------------------------------------
    Entity khác DTO thế nào?
    -----------------------------------------------------
    - Entity (package này): khớp với bảng database, có đủ mọi cột, kể cả password.
    - DTO (package dto)   : khớp với JSON gửi đi hoặc nhận về, chỉ chứa trường cần thiết.
    Controller không bao giờ trả thẳng entity ra ngoài, mà đổi sang DTO trước
    (VD UserMapper đổi User → UserProfileResponse, bỏ password).

    -----------------------------------------------------
    Index
    -----------------------------------------------------
    Index giống mục lục của sách: giúp database tìm nhanh mà không phải đọc hết bảng.
    - idx_users_artist_request_status: Admin lọc danh sách yêu cầu làm Artist đang chờ
      (WHERE artist_request_status = 'PENDING').
    - Cột username và email có unique = true → database tự tạo index duy nhất,
      nên đăng nhập bằng email vẫn tìm rất nhanh.

    Đọc cú pháp @Table(...) bên dưới:
        @Table(
                name = "users",
                indexes = {
                        @Index(name = "idx_users_artist_request_status", columnList = "artist_request_status")
                }
        )
    - name = "users"   : tên bảng.
    - indexes = { ... }: danh sách index của bảng. Dấu { } trong annotation nghĩa là "mảng",
                         bên trong có thể ghi nhiều @Index, cách nhau bởi dấu phẩy.
    - @Index(...)      : 1 annotation nằm bên trong 1 annotation khác.
        + name       : tên index (tự đặt, thường bắt đầu bằng idx_ cho dễ nhận ra).
        + columnList : các cột của index, ghi theo tên cột trong database (artist_request_status),
                       không phải tên thuộc tính Java (artistRequestStatus).

    -----------------------------------------------------
    Hibernate theo dõi thay đổi thế nào? (dirty checking)
    -----------------------------------------------------
    Đối tượng đọc từ database (findById, findByEmail...) được Hibernate "nhớ" giá trị ban đầu.
    Khi lưu (save() hoặc khi transaction kết thúc), Hibernate so giá trị hiện tại với giá trị ban đầu:
    - Có thuộc tính khác đi → chạy UPDATE (gọi là "dirty", tức "bị bẩn", đã thay đổi).
    - Không khác gì        → không chạy câu SQL nào.
    Ví dụ:
        User user = userRepository.findById(6L).orElseThrow();   → SELECT, Hibernate nhớ fullName cũ
        user.setFullName("Trần Mây Chiều");                       → chỉ đổi trong bộ nhớ Java
        userRepository.save(user);                                → thấy fullName khác → UPDATE users ...
    Chưa gọi save() (và không nằm trong @Transactional) thì database không đổi gì.

    -----------------------------------------------------
    Một tài khoản có 4 trạng thái độc lập
    -----------------------------------------------------
    - accountStatus      : tài khoản có dùng được không (AccountStatus).
    - role               : quyền trong hệ thống (Role).
    - artistRequestStatus: tiến trình xin làm Artist (ArtistRequestStatus).
    - accessStatus       : có được chủ dự án cho vào web không (AccessStatus).
    Ví dụ: Artist bị khóa → role = ARTIST, accountStatus = DISABLED, artistRequestStatus = APPROVED.

    -----------------------------------------------------
    Quyền vào web (accessStatus)
    -----------------------------------------------------
    Web chỉ dành cho người được chủ dự án cho phép (tránh người lạ dùng, liên quan bản quyền nhạc).
    - Mọi tài khoản mới đăng ký đều là REFUSE → đăng ký, nhập OTP bình thường nhưng không đăng nhập được.
    - Chủ dự án cấp quyền bằng SQL:
          UPDATE users SET access_status = 'ACCEPT' WHERE email = 'nam@gmail.com';
    - Thu hồi quyền:
          UPDATE users SET access_status = 'REFUSE' WHERE email = 'nam@gmail.com';
      Người đang đăng nhập bị chặn ngay từ request tiếp theo (xem CustomUserDetailsService).
    - Không có API nào đổi được cột này, kể cả API của Admin.
      Admin cũng phải là ACCEPT mới đăng nhập được.

    -----------------------------------------------------
    Lưu ý về VPS
    -----------------------------------------------------
    - Máy local dùng ddl-auto=update: Hibernate tự tạo bảng, tự thêm cột mới theo entity.
    - VPS dùng ddl-auto=validate: Hibernate chỉ kiểm tra bảng có khớp với entity không,
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
    /*
        -----------------------------------------------------
        id: khóa chính
        -----------------------------------------------------
        - @Id: thuộc tính này là khóa chính (primary key), mỗi dòng 1 giá trị không trùng.
        - @GeneratedValue(strategy = GenerationType.IDENTITY): để MySQL tự cấp id
          (cột AUTO_INCREMENT: 1, 2, 3...). Code không bao giờ tự gán id.

        Kiểu Long (đối tượng) thay vì long (nguyên thủy):
        - new User() → id = null, nghĩa là "chưa lưu vào database".
        - Sau userRepository.save(user) → MySQL cấp id, Hibernate gán lại vào đối tượng (VD id = 8).
        Spring Data dựa vào id null hay không để chọn INSERT (mới) hay UPDATE (đã có).

        @Id đặt trên thuộc tính (field), nên Hibernate đọc ghi trực tiếp vào thuộc tính
        (gọi là field access), không đi qua getter, setter.
    */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
        -----------------------------------------------------
        username: tên đăng nhập hiển thị, không trùng, không đổi được
        -----------------------------------------------------
        @Column mô tả cột trong bảng:
        - nullable = false: cột NOT NULL, bắt buộc có giá trị.
        - unique = true   : không 2 dòng nào trùng username (database tự chặn).
        - length = 50     : cột VARCHAR(50), tối đa 50 ký tự.
        Không ghi name → tên cột trùng tên thuộc tính: username.

        RegisterRequest chỉ cho 5 đến 20 ký tự, nằm gọn trong 50.
    */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /*
        -----------------------------------------------------
        email: email đăng nhập, không trùng
        -----------------------------------------------------
        - VARCHAR(100), NOT NULL, UNIQUE. Khớp @Size(max = 100) trong RegisterRequest
          và RequestUpdateEmailOtpRequest.
        - Email trong token đăng nhập dùng để tìm lại User mỗi request (findByEmail).
    */
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /*
        -----------------------------------------------------
        password: mật khẩu đã băm BCrypt (không bao giờ lưu mật khẩu gốc)
        -----------------------------------------------------
        Ví dụ giá trị: $2a$10$N9qo8uLOickgx2ZMRZoMye... (chuỗi băm BCrypt dài 60 ký tự).
        - length = 255: dư chỗ, phòng sau này đổi sang thuật toán băm cho chuỗi dài hơn.
        - @JsonIgnore: nếu lỡ có chỗ nào đổi thẳng User thành JSON, Jackson sẽ bỏ qua thuộc tính này.
          Bình thường project đã đổi User sang DTO trước khi trả về (UserProfileResponse không có password),
          @JsonIgnore là lớp phòng hờ thứ hai.
    */
    @JsonIgnore
    @Column(nullable = false, length = 255)
    private String password;

    /*
        -----------------------------------------------------
        fullName: họ tên (có thể null)
        -----------------------------------------------------
        - name = "full_name": tên cột trong bảng.
          Java đặt tên kiểu camelCase (fullName), SQL quen kiểu snake_case (full_name).
          Spring Boot mặc định cũng tự đổi fullName → full_name, ghi rõ name để nhìn là biết tên cột.
        - Không có nullable = false → cho phép NULL (đăng ký không hỏi họ tên).
    */
    @Column(name = "full_name", length = 100)
    private String fullName;

    /*
        avatarUrl: đường dẫn ảnh đại diện, VD "/uploads/avatars/3f2c8a1e.jpg".
        NULL = chưa có ảnh → frontend hiện ảnh mặc định.
        Chỉ lưu đường dẫn, file ảnh thật nằm trên ổ đĩa server (FileStorageService).
    */
    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    /*
        -----------------------------------------------------
        accountStatus: tài khoản có dùng được không
        -----------------------------------------------------
        @Enumerated(EnumType.STRING): lưu enum vào database dưới dạng chữ, VD "ACTIVE".
        Cách còn lại là EnumType.ORDINAL: lưu số thứ tự (UNVERIFIED = 0, ACTIVE = 1, ...).
        Vì sao không dùng ORDINAL?
        - Nhìn số 1 trong database không biết nghĩa là gì.
        - Chèn thêm 1 giá trị vào giữa enum → số thứ tự các giá trị sau bị lệch
          → dữ liệu cũ bị hiểu sai hết mà không báo lỗi.

        = AccountStatus.UNVERIFIED: giá trị ban đầu khi tạo new User()
        (đăng ký xong, chưa nhập OTP). Service không cần gán lại.
        Đây là giá trị mặc định phía Java: chỉ có tác dụng khi tạo đối tượng bằng new User().
        Khi đọc 1 tài khoản có sẵn từ database, Hibernate ghi đè bằng giá trị thật trong cột.

        Lưu ý: Hibernate tạo cột này dạng enum('UNVERIFIED','ACTIVE',...) của MySQL.
        Thêm giá trị mới vào enum AccountStatus thì phải tự ALTER cột, cả trên VPS lẫn máy local
        (ddl-auto=update chỉ thêm cột mới, không sửa kiểu của cột đã có).
        Xem thêm phần "Vì sao dùng bảng mà không dùng enum" trong Genre. 3 cột enum bên dưới cũng vậy.
    */
    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", length = 20, nullable = false)
    private AccountStatus accountStatus = AccountStatus.UNVERIFIED;

    /*
        role: quyền trong hệ thống (USER, ARTIST, ADMIN), mặc định USER.
        Không ghi name → tên cột là role.
        Mỗi request, CustomUserDetailsService đọc role mới nhất từ cột này
        → Admin đổi role thì có tác dụng ngay, không cần đăng nhập lại.
    */
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Role role = Role.USER;

    /*
        artistRequestStatus: tiến trình xin làm Artist (NONE, PENDING, APPROVED, REJECTED), mặc định NONE.
        Cột này có index idx_users_artist_request_status (khai báo ở @Table phía trên).
    */
    @Enumerated(EnumType.STRING)
    @Column(name = "artist_request_status", length = 20, nullable = false)
    private ArtistRequestStatus artistRequestStatus = ArtistRequestStatus.NONE;

    /*
        Mặc định REFUSE: tài khoản tạo bằng new User() (đăng ký) luôn bị chặn
        cho tới khi chủ dự án đổi thành ACCEPT bằng SQL (xem phần "Quyền vào web" ở trên).
    */
    @Enumerated(EnumType.STRING)
    @Column(name = "access_status", length = 20, nullable = false)
    private AccessStatus accessStatus = AccessStatus.REFUSE;

    /*
        -----------------------------------------------------
        artistName, bio: hồ sơ Artist (có thể null)
        -----------------------------------------------------
        - artistName: nghệ danh, VARCHAR(100). Không có unique → 2 Artist trùng nghệ danh được.
        - bio       : giới thiệu, VARCHAR(255).
        Chỉ có giá trị khi đã gửi yêu cầu làm Artist (ArtistRequest).
        Bị từ chối hoặc bị Admin hạ role → Service đặt lại null.
        Bài hát không lưu nghệ danh, chỉ lưu artist_id → đổi nghệ danh ở đây là mọi bài đổi theo.
    */
    @Column(name = "artist_name", length = 100)
    private String artistName;

    @Column(length = 255)
    private String bio;

    /*
        -----------------------------------------------------
        Getter và setter
        -----------------------------------------------------
        Khác DTO response (chỉ có getter), entity cần cả setter:
        Service đọc User từ database, đổi giá trị bằng setter, rồi save() để ghi lại. VD:
            User user = userRepository.findById(6L).orElseThrow();
            user.setFullName("Trần Mây Chiều");
            userRepository.save(user);   → UPDATE users SET full_name = ... WHERE id = 6

        Hibernate không cần getter, setter (dùng field access, xem thuộc tính id),
        getter và setter ở đây là để code của mình dùng.
        Class không khai báo constructor nào → Java tự tạo constructor rỗng,
        đúng thứ Hibernate cần để tạo đối tượng khi đọc từ database.
        Cách đọc từng dòng getter, setter: xem RegisterRequest (package dto.auth).
    */
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

    public AccessStatus getAccessStatus() { return accessStatus; }
    public void setAccessStatus(AccessStatus accessStatus) { this.accessStatus = accessStatus; }

    public String getArtistName() { return artistName; }
    public void setArtistName(String artistName) { this.artistName = artistName; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}