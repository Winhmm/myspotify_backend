package com.winhmm.myspotify.repository;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    - Playlist     : entity mà repository này làm việc cùng (bảng playlists).
    - JpaRepository: interface của Spring Data JPA, có sẵn các hàm save, findById, delete...
    - Query        : annotation @Query, dùng để tự viết câu truy vấn.
    - Param        : annotation @Param, đặt tên cho tham số dùng trong câu truy vấn.
    - List         : danh sách nhiều phần tử, có thứ tự.
    - Optional     : "hộp" có thể chứa 1 giá trị hoặc rỗng (giải thích ở hàm findWithOwnerById).
*/
import com.winhmm.myspotify.entity.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/*
    =====================================================================
    PLAYLISTREPOSITORY - TRUY CẬP BẢNG PLAYLISTS (MYSQL)
    =====================================================================

    (Repository là gì, derived query: xem GenreRepository. JPQL, @Query, @Param: xem SongRepository.
     Các khái niệm đó cũng được giải thích lại bên dưới.)

    =====================================================================
    REPOSITORY LÀ GÌ?
    =====================================================================

    Ứng dụng chia làm nhiều tầng, mỗi tầng 1 việc:
        Controller  → nhận request từ frontend, trả JSON về
        Service     → xử lý nghiệp vụ (kiểm tra quyền, giới hạn, quy tắc)
        Repository  → đọc, ghi database
        Entity      → class ánh xạ với bảng
    Service muốn lưu hay tìm playlist thì gọi repository, không tự viết SQL.

    Điều đặc biệt: đây là interface, các hàm bên dưới không có thân hàm (không có code bên trong).
    Khi ứng dụng khởi động, Spring Data JPA tự tạo 1 class thật "làm hộ" interface này
    (tự viết code cho từng hàm), rồi tạo sẵn 1 object của class đó (gọi là bean).
    Service chỉ cần khai báo PlaylistRepository trong constructor là Spring tự đưa object đó vào
    (xem phần "Constructor injection" trong PlaylistService).

    Không cần ghi @Repository: Spring Data tự nhận ra mọi interface extends JpaRepository.

    -----------------------------------------------------
    extends JpaRepository<Playlist, Long>
    -----------------------------------------------------
    - Playlist: entity repository làm việc cùng.
    - Long    : kiểu của khóa chính (id trong Playlist là Long).
    Có sẵn, không cần viết:
        save(playlist)       thêm mới (id đang null) hoặc cập nhật (id đã có), trả về playlist đã lưu
        findById(id)         tìm theo id, trả về Optional<Playlist>
        delete(playlist)     xóa 1 dòng
        count(), findAll()   đếm, lấy tất cả

    =====================================================================
    2 CÁCH VIẾT THÊM HÀM
    =====================================================================

    1. Derived query: đặt tên hàm theo quy tắc, Spring đọc tên và tự sinh câu truy vấn.
           long countByOwnerId(Long ownerId);
               count   → đếm
               By      → phần điều kiện bắt đầu từ đây
               OwnerId → thuộc tính owner, rồi tới id của owner (owner.id)
           Spring tự sinh: SELECT COUNT(*) FROM playlists WHERE user_id = ?
       Viết sai tên thuộc tính (VD countByUserId, vì Playlist không có thuộc tính user)
       → ứng dụng báo lỗi ngay lúc khởi động, không chạy được.

    2. @Query: tự viết câu truy vấn bằng JPQL, dùng khi điều kiện phức tạp hoặc cần JOIN FETCH.
       Có @Query thì tên hàm đặt tùy ý, Spring không đọc tên hàm nữa.

    -----------------------------------------------------
    JPQL khác SQL thế nào?
    -----------------------------------------------------
    JPQL viết theo tên class và tên thuộc tính Java, không theo tên bảng, tên cột:
        JPQL: SELECT p FROM Playlist p WHERE p.isPublic = true
        SQL : SELECT * FROM playlists WHERE is_public = 1
    - Playlist p : class Playlist, đặt tên gọi tắt là p (giống biến tạm).
    - SELECT p   : lấy cả object Playlist (không phải từng cột).
    - p.isPublic : thuộc tính isPublic (Hibernate tự đổi thành cột is_public).
    - p.owner    : đi theo quan hệ owner (Hibernate tự đổi thành cột user_id, JOIN bảng users nếu cần).
    Hibernate dịch JPQL ra SQL của MySQL rồi mới gửi đi.

    -----------------------------------------------------
    :ownerId và @Param("ownerId")
    -----------------------------------------------------
    - :ownerId trong câu truy vấn là 1 "chỗ trống" có tên.
    - @Param("ownerId") Long ownerId: điền giá trị của tham số này vào chỗ trống :ownerId.
    Giá trị được gửi riêng cho MySQL, không ghép thẳng vào chuỗi câu truy vấn
    → không bị tấn công SQL injection (người dùng gửi chuỗi lạ để phá câu SQL).

    =====================================================================
    JOIN FETCH LÀ GÌ?
    =====================================================================

    Field owner trong Playlist là LAZY: đọc playlist thì chưa đọc người tạo.
    Mỗi playlist trả về frontend đều kèm tên người tạo (ownerName),
    nên lúc đổi sang DTO sẽ gọi playlist.getOwner().getUsername().
    Không có JOIN FETCH → Hibernate chạy thêm 1 câu SELECT vào bảng users
    cho mỗi người tạo khác nhau (lỗi "N + 1", xem thêm phần "Số câu truy vấn" trong SongMapper).
        VD: 20 playlist của 20 người khác nhau = 1 câu lấy playlist + 20 câu lấy người tạo = 21 câu.

    JOIN FETCH p.owner: lấy luôn người tạo trong cùng 1 câu SQL (JOIN bảng users)
    → bao nhiêu playlist cũng chỉ 1 câu SQL.

    Khác JOIN thường (như s.artist.accountStatus trong SongRepository):
    - JOIN thường : chỉ dùng bảng users để lọc điều kiện, không nạp dữ liệu người tạo vào object.
    - JOIN FETCH  : nạp luôn dữ liệu người tạo vào object Playlist, gọi getOwner() không cần SELECT thêm.

    =====================================================================
    CÁC HÀM TRONG FILE
    =====================================================================

    Có sẵn từ JpaRepository: save, findById, delete...

    Viết thêm:
        countByOwnerId(...)       → đếm số playlist của 1 tài khoản (giới hạn 50)
        findWithOwnerById(...)    → 1 playlist kèm người tạo (xem chi tiết, sửa, xóa, thêm bài)
        findMyPlaylists(...)      → mọi playlist của 1 tài khoản (trang "Playlist của tôi")
        findPublicPlaylists(...)  → playlist công khai của 1 tài khoản (trang cá nhân người khác)

    Chỉ được dùng trong PlaylistService.
*/
public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

    /*
        -----------------------------------------------------
        Đếm số playlist của 1 tài khoản
        -----------------------------------------------------
        Derived query: count + By + OwnerId (thuộc tính owner, rồi id của owner).
        SQL tương đương: SELECT COUNT(*) FROM playlists WHERE user_id = ?
        Trả về long (số nguyên), không bao giờ null (không có playlist nào thì ra 0).

        Dùng khi tạo playlist: đã có 50 cái → không cho tạo thêm.
        Đếm cả playlist riêng tư lẫn công khai.
    */
    long countByOwnerId(Long ownerId);

    /*
        -----------------------------------------------------
        Lấy 1 playlist theo id, kèm người tạo
        -----------------------------------------------------
        Dùng khi xem chi tiết, sửa, đổi ảnh bìa, thêm bỏ bài, xóa playlist.
        Cần người tạo để:
        - So với người đang đăng nhập: có phải chủ playlist không.
        - Hiện tên người tạo (ownerName).
        - Kiểm tra tài khoản người tạo còn ACTIVE không.

        Vì sao không dùng findById có sẵn?
        findById chỉ lấy playlist, owner vẫn LAZY → gọi getOwner().getUsername() sẽ tốn thêm 1 câu SELECT.
        Hàm này lấy cả 2 trong 1 câu.

        -----------------------------------------------------
        Optional<Playlist> là gì?
        -----------------------------------------------------
        Có thể không có playlist id đó. Thay vì trả về null (dễ quên kiểm tra → NullPointerException),
        Spring trả về 1 "hộp" Optional:
        - Tìm thấy  → hộp chứa playlist.
        - Không thấy → hộp rỗng.
        Service mở hộp bằng orElseThrow(...): có thì lấy ra, rỗng thì ném lỗi "Playlist does not exist".
    */
    @Query("SELECT p FROM Playlist p JOIN FETCH p.owner WHERE p.id = :playlistId")
    Optional<Playlist> findWithOwnerById(@Param("playlistId") Long playlistId);

    /*
        -----------------------------------------------------
        Mọi playlist của 1 tài khoản (trang "Playlist của tôi")
        -----------------------------------------------------
        Gồm cả riêng tư lẫn công khai. Playlist tạo sau đứng trước.

        - JOIN FETCH p.owner o: nạp người tạo, đặt tên gọi tắt là o.
        - WHERE o.id = :ownerId: chỉ playlist của tài khoản này.
        - ORDER BY p.createdAt DESC: sắp xếp theo thời điểm tạo, DESC = giảm dần → mới tạo đứng trước.
          (ASC = tăng dần, cũ đứng trước.)
          Thêm p.id DESC để 2 playlist tạo cùng 1 lúc vẫn có thứ tự cố định (id lớn hơn = tạo sau).

        Lưu ý dấu cách khi nối chuỗi: "... = :ownerId " + "ORDER BY ..." → cuối mỗi đoạn phải có dấu cách,
        nếu không 2 chữ dính nhau (":ownerIdORDER") và câu truy vấn bị lỗi lúc khởi động.

        Trả về List (không phân trang) vì mỗi tài khoản tối đa 50 playlist.
        Không có playlist nào → danh sách rỗng [] (không phải null).
    */
    @Query("SELECT p FROM Playlist p JOIN FETCH p.owner o "
            + "WHERE o.id = :ownerId "
            + "ORDER BY p.createdAt DESC, p.id DESC")
    List<Playlist> findMyPlaylists(@Param("ownerId") Long ownerId);

    /*
        -----------------------------------------------------
        Playlist công khai của 1 tài khoản (trang cá nhân của người đó)
        -----------------------------------------------------
        Người khác xem trang cá nhân của tài khoản này, chỉ thấy:
        - Playlist công khai (p.isPublic = true). Playlist riêng tư không hiện trong danh sách.
        - Của tài khoản đang ACTIVE. Tài khoản bị khóa, tự vô hiệu hóa, đã xóa
          → mọi playlist của họ tự ẩn với người khác (giống cách bài hát của Artist bị khóa tự ẩn).

        - p.isPublic: tên thuộc tính trong class Playlist (không phải tên cột is_public).
        - com.winhmm.myspotify.enums.AccountStatus.ACTIVE: trong JPQL, giá trị enum
          phải ghi đủ tên package (Hibernate không đọc được dòng import của file Java).
    */
    @Query("SELECT p FROM Playlist p JOIN FETCH p.owner o "
            + "WHERE o.id = :ownerId "
            + "AND p.isPublic = true "
            + "AND o.accountStatus = com.winhmm.myspotify.enums.AccountStatus.ACTIVE "
            + "ORDER BY p.createdAt DESC, p.id DESC")
    List<Playlist> findPublicPlaylists(@Param("ownerId") Long ownerId);
}

