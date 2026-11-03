package com.winhmm.myspotify.repository;

import com.winhmm.myspotify.entity.User;
import com.winhmm.myspotify.enums.AccountStatus;
import com.winhmm.myspotify.enums.ArtistRequestStatus;
import com.winhmm.myspotify.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/*
    =====================================================
    USERREPOSITORY - TRUY CẬP BẢNG USERS (MYSQL)
    =====================================================

    (Repository là gì, vì sao là interface, cách Spring đọc tên method: xem GenreRepository.
     Tự viết JPQL bằng @Query: xem SongRepository.
     Thứ tự đọc package repository: xem GenreRepository.)

    Lớp duy nhất đọc và ghi trực tiếp bảng users.
    Service gọi Repository, không tự viết SQL.

    extends JpaRepository<User, Long>:
    - User: entity mà repository này quản lý.
    - Long: kiểu của khóa chính (User.id).
    Được tặng sẵn và đang được dùng trong dự án:
        save(user)         → lưu tài khoản mới hoặc thay đổi (AuthService, UserService, AdminService)
        findById(id)       → lấy 1 tài khoản theo id (UserService, AdminService, SongService...)
        findAll()          → mọi tài khoản (AdminService, GET /api/admin/users)
        findAllById(ids)   → nhiều tài khoản theo danh sách id, trong 1 câu SQL
                             (ArtistSongService: kiểm tra danh sách người được feat)

    Interface không có code bên trong: Spring Data JPA đọc tên method
    rồi tự sinh câu truy vấn khi ứng dụng khởi động (derived query).

    -----------------------------------------------------
    3 kiểu trả về trong file này
    -----------------------------------------------------
        Optional<User>  : 0 hoặc 1 kết quả (xem giải thích Optional trong SongLyricsRepository)
        boolean         : chỉ cần biết CÓ hay KHÔNG, không cần lấy dữ liệu
        List<User>      : 0, 1 hoặc nhiều kết quả. Không có → danh sách rỗng, không bao giờ null.
*/
public interface UserRepository extends JpaRepository<User, Long> {

    /*
        -----------------------------------------------------
        Tìm tài khoản theo email
        -----------------------------------------------------
        SQL tương đương: SELECT * FROM users WHERE email = ?
        Optional vì cột email có UNIQUE → nhiều nhất 1 tài khoản.

        Method được gọi nhiều nhất dự án, vì email là "tên đăng nhập" và nằm trong token:
        - AuthService: đăng ký (email đã có chưa), xác thực OTP, đăng nhập, quên và đặt lại mật khẩu.
        - CustomUserDetailsService: MỖI request có token, đọc lại tài khoản từ email trong token
          để biết role và trạng thái mới nhất (bị khóa giữa chừng là chặn được ngay).
        - UserService, SongService: đổi email trong token thành tài khoản.
    */
    Optional<User> findByEmail(String email);

    /*
        -----------------------------------------------------
        Email đã được dùng chưa? (đổi email)
        -----------------------------------------------------
        existsBy...: trả true hoặc false.
        SQL gần tương đương: SELECT 1 FROM users WHERE email = ? LIMIT 1

        Vì sao không dùng findByEmail(...).isPresent()?
        Kết quả như nhau, nhưng exists nhẹ hơn: MySQL chỉ cần biết có dòng nào khớp,
        không phải đọc cả dòng, Hibernate không phải tạo object User.

        Dùng ở UserService khi đổi email (requestUpdateEmailOtp, updateEmail):
        email mới đã thuộc tài khoản khác → 400 "Email already exists".
        Tính cả tài khoản UNVERIFIED, DISABLED, DELETED: email của tài khoản đã xóa vẫn bị giữ
        (xem enum AccountStatus).
    */
    boolean existsByEmail(String email);

    /*
        -----------------------------------------------------
        Username đã được dùng chưa? (đăng ký)
        -----------------------------------------------------
        SQL gần tương đương: SELECT 1 FROM users WHERE username = ? LIMIT 1
        Dùng ở AuthService.register → "Username already exists".

        Lưu ý: kiểm tra trước rồi mới INSERT vẫn có khe hở rất nhỏ khi 2 người đăng ký
        cùng username đúng cùng lúc (cả 2 đều thấy "chưa có"). Ràng buộc UNIQUE trên cột username
        trong MySQL là lớp chặn cuối → người thứ hai nhận 409 "Data already exists"
        (xem handleDataIntegrity trong GlobalExceptionHandler).
    */
    boolean existsByUsername(String username);

    /*
        -----------------------------------------------------
        Danh sách tài khoản theo tiến trình xin làm Artist
        -----------------------------------------------------
        SQL tương đương: SELECT * FROM users WHERE artist_request_status = ?
        Dùng ở AdminService.getPendingArtistRequests, truyền PENDING
        → GET /api/admin/artist-requests: Admin xem ai đang chờ duyệt làm Artist.

        Truyền enum ArtistRequestStatus, Spring tự đổi thành chuỗi 'PENDING' khi so với cột
        (vì cột dùng @Enumerated(EnumType.STRING), xem entity User).
        Index idx_users_artist_request_status trên cột này (khai báo trong entity User)
        giúp MySQL không phải đọc hết bảng users.
    */
    List<User> findByArtistRequestStatus(ArtistRequestStatus status);

    /*
        -----------------------------------------------------
        UC37 - Tìm nghệ sĩ để feat khi Artist đăng bài
        -----------------------------------------------------
        API: GET /api/artist/search-artists?keyword=son
        ArtistSongService.searchArtists gọi:
            findTop10ByRoleAndAccountStatusAndArtistNameContainingIgnoreCaseAndIdNot(
                    Role.ARTIST, AccountStatus.ACTIVE, keyword.trim(), artistId)

        Tên method được Spring tách thành từng phần
        (mỗi điều kiện "ăn" 1 tham số, theo đúng thứ tự từ trái sang phải):
            findTop10                           → LIMIT 10
            ByRole                              → WHERE role = ?                 ← tham số 1 (ARTIST)
            AndAccountStatus                    → AND account_status = ?         ← tham số 2 (ACTIVE)
            AndArtistNameContainingIgnoreCase   → AND artist_name chứa keyword,  ← tham số 3 (keyword)
                                                  không phân biệt hoa thường
            AndIdNot                            → AND id <> ?                    ← tham số 4 (id của mình)

        Câu SQL Spring sinh ra, gần tương đương:
            SELECT * FROM users
            WHERE role = 'ARTIST' AND account_status = 'ACTIVE'
              AND UPPER(artist_name) LIKE UPPER('%son%') ESCAPE '\'
              AND id <> 7
            LIMIT 10

        - Containing  : Spring tự thêm % 2 đầu keyword → tìm "chứa" ở bất kỳ vị trí nào.
                        Spring còn tự escape (vô hiệu hóa) ký tự đặc biệt % và _ trong keyword
                        → tìm "100%" vẫn chính xác (khác searchPublished trong SongRepository).
        - IgnoreCase  : Spring đổi cả 2 vế sang chữ HOA rồi mới so → "son" khớp "Sơn Tùng", "SON".
                        ("son" khớp "Sơn" là nhờ MySQL không phân biệt dấu, giống giải thích trong SongRepository.)
        - IdNot       : loại chính mình ra, vì không được tự feat chính mình.
        - Role + AccountStatus: chỉ tìm Artist đang hoạt động (người bị khóa hay bị hạ role không feat được).

        Chỉ lấy 10 người để danh sách gợi ý gọn, frontend gõ thêm chữ để lọc tiếp.
        keyword rỗng: ArtistSongService trả danh sách rỗng luôn, không gọi tới hàm này.

        Tên method rất dài nhưng vẫn đọc được từng phần.
        Nếu điều kiện phức tạp hơn nữa (OR lồng nhau, JOIN...) thì nên chuyển sang @Query
        (xem SongRepository).
    */
    List<User> findTop10ByRoleAndAccountStatusAndArtistNameContainingIgnoreCaseAndIdNot(
            Role role, AccountStatus accountStatus, String keyword, Long excludeId);
}