package com.winhmm.myspotify.repository;

import com.winhmm.myspotify.entity.Song;
import com.winhmm.myspotify.enums.SongStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Collection;

/*
    =====================================================================
    SongRepository - TRUY CẬP BẢNG songs (MYSQL)
    =====================================================================

    Các truy vấn:
        Người nghe : findPublished, searchPublished, findPublishedById,
                     findPublishedByArtist, incrementPlayCount
        Artist     : findByArtistIdAndStatusNotOrderByCreatedAtDesc, findByIdAndArtistId
        Admin      : findByStatusNot, findByStatus
        Lịch sử nghe: findPublishedByIds

    Repository chỉ chứa truy vấn bài hát. Truy vấn người dùng nằm ở UserRepository.

    =====================================================================
    REPOSITORY LÀ GÌ?
    =====================================================================

    Repository là lớp duy nhất nói chuyện trực tiếp với database.
    Service gọi Repository, không bao giờ tự viết SQL.

    =====================================================================
    VÌ SAO LÀ INTERFACE, KHÔNG CÓ CODE BÊN TRONG?
    =====================================================================

    Mình chỉ khai báo method, Spring Data JPA tự viết phần code thực thi khi ứng dụng khởi động.

    extends JpaRepository<Song, Long>:
        - Song: entity mà repository này quản lý.
        - Long: kiểu của khóa chính (Song.id).

    Được tặng sẵn nhiều method, không cần khai báo:
        save(song)          → INSERT (bài mới) hoặc UPDATE (bài đã có id)
        findById(15L)       → SELECT ... WHERE id = 15, trả Optional<Song>
        findAllById(ids)    → SELECT ... WHERE id IN (...)
        findAll()           → SELECT toàn bộ
        delete(song)        → DELETE
        count()             → đếm số dòng

    =====================================================================
    2 CÁCH VIẾT TRUY VẤN
    =====================================================================

    Cách 1 - Đặt tên method theo quy ước (derived query):
        Spring đọc tên method rồi tự sinh câu truy vấn.
        VD: findByStatus(SongStatus status, Pageable pageable)
            → SELECT * FROM songs WHERE status = ? (kèm phân trang)
        Các từ khóa hay dùng: findBy, And, Or, Not, OrderBy...Desc, Top10, Containing, IgnoreCase.
        Hợp với truy vấn đơn giản.

    Cách 2 - Tự viết JPQL bằng @Query:
        Dùng khi điều kiện phức tạp (JOIN nhiều bảng, OR lồng nhau...),
        đặt tên method theo quy ước sẽ dài không đọc nổi.

    =====================================================================
    JPQL KHÁC SQL THẾ NÀO?
    =====================================================================

    JPQL (Java Persistence Query Language) viết theo tên class và tên thuộc tính Java,
    không phải tên bảng và tên cột:

        JPQL:  SELECT s FROM Song s WHERE s.releaseAt <= :now
        SQL :  SELECT * FROM songs s WHERE s.release_at <= ?

        - "Song" là tên class (không phải bảng "songs").
        - "s.releaseAt" là thuộc tính Java (không phải cột "release_at").
        - "s.artist.accountStatus": đi theo quan hệ như gọi getter trong Java
          → Hibernate tự JOIN sang bảng users.

    Hibernate dịch JPQL sang SQL đúng của MySQL.
    Lợi ích: đổi tên cột trong @Column không phải sửa câu truy vấn.

    =====================================================================
    THAM SỐ :now, :keyword VÀ CHỐNG SQL INJECTION
    =====================================================================

    Dấu hai chấm là tham số có tên, được điền bằng @Param("now") ở tham số method.

    Hibernate gửi truy vấn tới MySQL dưới dạng PreparedStatement:
        - Câu SQL gửi đi chỉ có dấu ?, VD: ... WHERE LOWER(title) LIKE ?
        - Giá trị keyword được gửi riêng, sau khi MySQL đã hiểu xong cấu trúc câu lệnh.
    → MySQL không bao giờ coi keyword là một phần của câu lệnh.
      keyword = "'; DROP TABLE songs; --" chỉ được coi là chuỗi cần tìm, không chạy được.
    (Tuyệt đối không ghép chuỗi kiểu "... LIKE '%" + keyword + "%'": đó chính là lỗ hổng SQL injection.)

    Vì sao truyền :now từ Java thay vì dùng CURRENT_TIMESTAMP của MySQL?
    - Giờ do Java tính (LocalDateTime.now()) cùng nguồn với giờ lưu vào database lúc tạo bài
      → không lệch khi múi giờ MySQL khác múi giờ ứng dụng.
    - Khi test có thể truyền 1 thời điểm cố định → kết quả kiểm soát được.
*/
public interface SongRepository extends JpaRepository<Song, Long> {
    /*
        =====================================================================
        ĐIỀU KIỆN "BÀI ĐÃ PHÁT HÀNH"
        =====================================================================

        Người nghe chỉ thấy bài thỏa đủ 3 điều kiện (UC20 - UC25):
        1. s.status = APPROVED : Admin đã duyệt, Artist không ẩn.
        2. s.releaseAt <= :now : đã tới giờ phát hành.
        3. Artist chính ACTIVE và vẫn có role ARTIST.
           → Artist chính bị khóa, xóa tài khoản, hoặc bị thu hồi quyền
             → mọi bài của họ tự ẩn, không cần sửa từng bài. Mở khóa lại → bài tự hiện lại.
    */
    String PUBLISHED_CONDITION =
            "s.status = com.winhmm.myspotify.enums.SongStatus.APPROVED "
                    + "AND s.releaseAt <= :now "
                    + "AND s.artist.accountStatus = com.winhmm.myspotify.enums.AccountStatus.ACTIVE "
                    + "AND s.artist.role = com.winhmm.myspotify.enums.Role.ARTIST";

    /*
        =====================================================================
        ĐIỀU KIỆN "ARTIST FEAT ĐANG HOẠT ĐỘNG"
        =====================================================================

        1. Tài khoản đang ACTIVE.
        2. Vẫn có role ARTIST.
    */
    String ACTIVE_FEATURED_CONDITION =
            "f.accountStatus = com.winhmm.myspotify.enums.AccountStatus.ACTIVE "
                    + "AND f.role = com.winhmm.myspotify.enums.Role.ARTIST";

    /*
        =====================================================================
        NGƯỜI NGHE (UC20 - UC25)
        =====================================================================
    */

    /*
        ---------------------------------------------------------------
        UC20 - Danh sách bài đã phát hành, có phân trang
        ---------------------------------------------------------------

        Pageable pageable: gói thông tin phân trang và sắp xếp do Service tạo,
            VD PageRequest.of(0, 20, Sort.by(DESC, "releaseAt")).
        Spring tự thêm vào câu SQL: ORDER BY release_at DESC LIMIT 20 OFFSET 0.

        Trả về Page<Song>: Spring chạy 2 câu SQL:
            1. Lấy 20 bài của trang hiện tại.
            2. SELECT COUNT(...) để biết tổng số bài → tính totalPages.
        (Riêng trang đầu mà có ít hơn 20 bài, Spring biết luôn tổng số nên bỏ qua câu đếm.)
    */
    @Query("SELECT s FROM Song s WHERE " + PUBLISHED_CONDITION)
    Page<Song> findPublished(@Param("now") LocalDateTime now, Pageable pageable);

    /*
        ---------------------------------------------------------------
        UC21 - Tìm kiếm bài đã phát hành theo keyword
        ---------------------------------------------------------------

        Tìm keyword trong 3 chỗ (chỉ cần khớp 1 trong 3):
        - Tên bài hát.
        - Nghệ danh Artist chính.
        - Nghệ danh Artist feat (chỉ tính người đang hoạt động).

        Các thành phần câu truy vấn:

        LEFT JOIN s.featuredArtists f
            - Ghép mỗi bài với từng người feat của nó (qua bảng song_featured_artists).
            - LEFT JOIN (không phải JOIN thường): bài không có ai feat vẫn được giữ lại,
              khi đó f = NULL. Dùng JOIN thường → bài không feat ai bị loại khỏi kết quả,
              tìm theo tên bài sẽ không ra.

        LOWER(...) LIKE LOWER(CONCAT('%', :keyword, '%'))
            - CONCAT('%', 'may', '%') → '%may%'.
            - LIKE '%may%': chứa "may" ở bất kỳ vị trí nào ("Mây Chiều", "Bài Hát May Mắn").
            - LOWER 2 vế: đổi hết về chữ thường → không phân biệt hoa thường.
            - "may" khớp được "Mây" (không phân biệt dấu) là nhờ collation mặc định
              của MySQL 8 (utf8mb4_0900_ai_ci), không phải nhờ LOWER.

        DISTINCT s
            - Bài feat 2 người → LEFT JOIN tạo ra 2 dòng cho cùng 1 bài.
            - DISTINCT gộp lại → mỗi bài chỉ xuất hiện 1 lần.

        countQuery (câu đếm riêng cho phân trang):
            - Mặc định Spring tự sinh câu đếm từ câu chính, nhưng với DISTINCT và JOIN
              câu tự sinh có thể đếm sai hoặc lỗi.
            - COUNT(DISTINCT s): đếm số bài khác nhau (không phải số dòng sau JOIN)
              → totalElements chính xác.
            - Điều kiện WHERE phải giống hệt câu chính, nếu không số trang sẽ lệch với dữ liệu.

        Giới hạn đã biết:
            Ký tự '%' và '_' trong keyword có nghĩa đặc biệt với LIKE
            ('%' = chuỗi bất kỳ, '_' = 1 ký tự bất kỳ). Tìm "100%" sẽ khớp rộng hơn mong đợi.
            Không gây lỗi bảo mật (vẫn chống SQL injection), chỉ làm kết quả rộng hơn.
            Muốn chính xác tuyệt đối thì escape 2 ký tự này trong Service trước khi truyền vào.
            (Khác UserRepository dùng Containing: Spring tự escape 2 ký tự này.)
    */
    @Query(value = "SELECT DISTINCT s FROM Song s LEFT JOIN s.featuredArtists f "
            + "WHERE " + PUBLISHED_CONDITION + " AND ("
            + "LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR LOWER(s.artist.artistName) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR (LOWER(f.artistName) LIKE LOWER(CONCAT('%', :keyword, '%')) AND " + ACTIVE_FEATURED_CONDITION + "))",
            countQuery = "SELECT COUNT(DISTINCT s) FROM Song s LEFT JOIN s.featuredArtists f "
                    + "WHERE " + PUBLISHED_CONDITION + " AND ("
                    + "LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')) "
                    + "OR LOWER(s.artist.artistName) LIKE LOWER(CONCAT('%', :keyword, '%')) "
                    + "OR (LOWER(f.artistName) LIKE LOWER(CONCAT('%', :keyword, '%')) AND " + ACTIVE_FEATURED_CONDITION + "))")
    Page<Song> searchPublished(@Param("keyword") String keyword,
                               @Param("now") LocalDateTime now,
                               Pageable pageable);

    /*
        ---------------------------------------------------------------
        UC22 - Lấy 1 bài đã phát hành theo id
        ---------------------------------------------------------------
    */
    @Query("SELECT s FROM Song s WHERE s.id = :songId AND " + PUBLISHED_CONDITION)
    Optional<Song> findPublishedById(@Param("songId") Long songId,
                                     @Param("now") LocalDateTime now);

    /*
        ---------------------------------------------------------------
        UC25 - Bài đã phát hành mà Artist đó là Artist chính hoặc được feat
        ---------------------------------------------------------------
    */
    @Query(value = "SELECT DISTINCT s FROM Song s LEFT JOIN s.featuredArtists f "
            + "WHERE " + PUBLISHED_CONDITION
            + " AND (s.artist.id = :artistId OR f.id = :artistId)",
            countQuery = "SELECT COUNT(DISTINCT s) FROM Song s LEFT JOIN s.featuredArtists f "
                    + "WHERE " + PUBLISHED_CONDITION
                    + " AND (s.artist.id = :artistId OR f.id = :artistId)")
    Page<Song> findPublishedByArtist(@Param("artistId") Long artistId,
                                     @Param("now") LocalDateTime now,
                                     Pageable pageable);

    /*
        ---------------------------------------------------------------
        UC24 - Tăng lượt nghe bằng 1 câu UPDATE duy nhất
        ---------------------------------------------------------------
    */
    @Modifying
    @Transactional
    @Query("UPDATE Song s SET s.playCount = s.playCount + 1 WHERE s.id = :songId")
    int incrementPlayCount(@Param("songId") Long songId);

    /*
        =====================================================================
        ARTIST (UC28 - UC33)
        =====================================================================
    */

    /*
        ---------------------------------------------------------------
        UC28 - Bài hát của tôi: mọi trạng thái trừ DELETED, mới tải lên trước
        ---------------------------------------------------------------
    */
    List<Song> findByArtistIdAndStatusNotOrderByCreatedAtDesc(Long artistId, SongStatus status);

    /*
        ---------------------------------------------------------------
        UC29 - UC33 - Tìm bài theo id và phải thuộc về Artist đó
        ---------------------------------------------------------------
    */
    Optional<Song> findByIdAndArtistId(Long id, Long artistId);

    /*
        =====================================================================
        ADMIN (UC34)
        =====================================================================
    */

    /*
        ---------------------------------------------------------------
        UC34 - Mọi bài trừ 1 trạng thái (Service truyền DELETED)
        ---------------------------------------------------------------
    */
    Page<Song> findByStatusNot(SongStatus status, Pageable pageable);

    /*
        ---------------------------------------------------------------
        UC34 - Lọc đúng 1 trạng thái, VD chỉ bài PENDING đang chờ duyệt
        ---------------------------------------------------------------
    */
    Page<Song> findByStatus(SongStatus status, Pageable pageable);

    /*
        =====================================================================
        LỊCH SỬ NGHE
        =====================================================================
    */

    /*
        ---------------------------------------------------------------
        Lấy nhiều bài đã phát hành theo danh sách id, trong 1 câu truy vấn
        ---------------------------------------------------------------
    */
    @Query("SELECT s FROM Song s WHERE s.id IN :songIds AND " + PUBLISHED_CONDITION)
    List<Song> findPublishedByIds(@Param("songIds") Collection<Long> songIds,
                                  @Param("now") LocalDateTime now);
}