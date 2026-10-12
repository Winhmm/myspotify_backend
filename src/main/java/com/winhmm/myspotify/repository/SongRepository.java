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

/*
    ===================================================================
    SongRepository - Truy cập bảng "songs" trong database.
    ===================================================================

    ---------- REPOSITORY LÀ GÌ? ----------

    Repository là lớp DUY NHẤT nói chuyện trực tiếp với database.
    Service gọi Repository, không bao giờ tự viết SQL.

    ---------- VÌ SAO LÀ INTERFACE, KHÔNG CÓ CODE BÊN TRONG? ----------

    Mình chỉ KHAI BÁO method, Spring Data JPA TỰ VIẾT phần code thực thi khi ứng dụng khởi động.

    extends JpaRepository<Song, Long>:
        - Song: entity mà repository này quản lý.
        - Long: kiểu của khóa chính (Song.id).
    Được tặng sẵn hàng chục method, không cần khai báo:
        save(song)          → INSERT (bài mới) hoặc UPDATE (bài đã có)
        findById(15L)       → SELECT ... WHERE id = 15, trả Optional<Song>
        findAll()           → SELECT toàn bộ
        delete(song)        → DELETE
        count()             → đếm số dòng

    ---------- 2 CÁCH VIẾT TRUY VẤN ----------

    CÁCH 1 - Đặt tên method theo quy ước (derived query):
        Spring đọc TÊN method rồi tự sinh câu truy vấn.
        VD: findByStatus(SongStatus status, Pageable pageable)
            → SELECT * FROM songs WHERE status = ? (kèm phân trang)
        Các từ khóa hay dùng: findBy, And, Or, Not, OrderBy...Desc, Top10, Containing, IgnoreCase.
        Hợp với truy vấn ĐƠN GIẢN.

    CÁCH 2 - Tự viết JPQL bằng @Query:
        Dùng khi điều kiện phức tạp (JOIN nhiều bảng, OR lồng nhau...),
        đặt tên method theo quy ước sẽ dài không đọc nổi.

    ---------- JPQL KHÁC SQL THẾ NÀO? ----------

    JPQL (Java Persistence Query Language) viết theo TÊN CLASS và TÊN THUỘC TÍNH Java,
    không phải tên bảng và tên cột:

        JPQL:  SELECT s FROM Song s WHERE s.releaseAt <= :now
        SQL :  SELECT * FROM songs s WHERE s.release_at <= ?

        - "Song" là tên class (không phải bảng "songs").
        - "s.releaseAt" là thuộc tính Java (không phải cột "release_at").
        - "s.artist.accountStatus": đi theo quan hệ như gọi getter trong Java
          → Hibernate tự JOIN sang bảng users.
    Hibernate dịch JPQL sang SQL đúng của MySQL.
    Lợi ích: đổi tên cột trong @Column không phải sửa câu truy vấn.

    ---------- THAM SỐ :now, :keyword ----------

    Dấu hai chấm = tham số có tên, được điền bằng @Param("now") ở tham số method.
    Hibernate tự "escape" giá trị → CHỐNG SQL INJECTION:
    keyword = "'; DROP TABLE songs; --" chỉ được coi là chuỗi cần tìm, không chạy được.
    (Tuyệt đối không ghép chuỗi kiểu "... LIKE '%" + keyword + "%'".)

    Vì sao truyền :now từ Java thay vì dùng CURRENT_TIMESTAMP của MySQL?
    - Giờ do Java tính (LocalDateTime.now()) cùng múi giờ với giờ lưu vào database lúc tạo bài
      → không lệch khi múi giờ MySQL khác múi giờ ứng dụng.
    - Khi test có thể truyền 1 thời điểm cố định → kết quả kiểm soát được.
*/
public interface SongRepository extends JpaRepository<Song, Long> {
    /*
        ---------- ĐIỀU KIỆN "BÀI ĐÃ PHÁT HÀNH" ----------

        Người nghe chỉ thấy bài thỏa ĐỦ 3 điều kiện (UC20 - UC25):
        1. s.status = APPROVED     : Admin đã duyệt, Artist không ẩn.
        2. s.releaseAt <= :now     : đã tới giờ phát hành.
        3. Artist chính ACTIVE và vẫn có role ARTIST.
           → Artist chính bị khóa / xóa tài khoản / bị thu hồi quyền → MỌI bài của họ tự ẩn,
             không cần sửa từng bài. Mở khóa lại → bài tự hiện lại.

        Viết thành hằng số String rồi ghép vào các @Query bên dưới
        → 4 truy vấn dùng chung 1 điều kiện, sửa 1 chỗ là cả 4 thay đổi theo.
        (Hằng số khai báo trong interface tự động là public static final.)

        Enum trong JPQL phải ghi ĐẦY ĐỦ tên package:
            com.winhmm.myspotify.enums.SongStatus.APPROVED
        (JPQL không hiểu lệnh import của Java.)

        Biến "s" là bí danh (alias) của Song, được khai báo trong "FROM Song s"
        của từng câu truy vấn ghép điều kiện này vào.
    */
    String PUBLISHED_CONDITION =
            "s.status = com.winhmm.myspotify.enums.SongStatus.APPROVED "
                    + "AND s.releaseAt <= :now "
                    + "AND s.artist.accountStatus = com.winhmm.myspotify.enums.AccountStatus.ACTIVE "
                    + "AND s.artist.role = com.winhmm.myspotify.enums.Role.ARTIST";

    /*
        ---------- ĐIỀU KIỆN "ARTIST FEAT ĐANG HOẠT ĐỘNG" ----------

        1. Tài khoản đang ACTIVE.
        2. Vẫn có role ARTIST.

        Dùng khi tìm kiếm theo tên Artist feat (UC21):
        → Artist feat bị khóa thì tên của họ KHÔNG được tính khi tìm.
          VD: tìm "vầng trăng" không ra bài mà Vầng Trăng (đã bị khóa) được feat.
        → Bài hát vẫn hiển thị bình thường khi tìm theo tên bài hoặc Artist chính,
          chỉ ẩn tên người bị khóa (SongMapper.activeFeaturedArtists lọc tương tự).

        Biến "f" là bí danh của từng người feat, khai báo trong "LEFT JOIN s.featuredArtists f".
    */
    String ACTIVE_FEATURED_CONDITION =
            "f.accountStatus = com.winhmm.myspotify.enums.AccountStatus.ACTIVE "
                    + "AND f.role = com.winhmm.myspotify.enums.Role.ARTIST";

    /* ================= NGƯỜI NGHE (UC20 - UC25) ================= */

    /*
        UC20 - Danh sách bài đã phát hành, có phân trang.

        Pageable pageable: gói thông tin phân trang + sắp xếp do Service tạo,
            VD PageRequest.of(0, 20, Sort.by(DESC, "releaseAt")).
        Spring tự thêm vào câu SQL: ORDER BY release_at DESC LIMIT 20 OFFSET 0.

        Trả về Page<Song>: Spring chạy 2 câu SQL:
            1. Lấy 20 bài của trang hiện tại.
            2. SELECT COUNT(...) để biết tổng số bài → tính totalPages.
    */
    @Query("SELECT s FROM Song s WHERE " + PUBLISHED_CONDITION)
    Page<Song> findPublished(@Param("now") LocalDateTime now, Pageable pageable);

    /*
        UC21 - Tìm kiếm bài đã phát hành theo keyword.

        Tìm keyword trong 3 chỗ (chỉ cần khớp 1 trong 3):
        - Tên bài hát.
        - Nghệ danh Artist chính.
        - Nghệ danh Artist feat (chỉ tính người đang hoạt động).

        CÁC THÀNH PHẦN CÂU TRUY VẤN:

        LEFT JOIN s.featuredArtists f
            - Ghép mỗi bài với từng người feat của nó (qua bảng song_featured_artists).
            - LEFT JOIN (không phải JOIN thường): bài KHÔNG có ai feat vẫn được giữ lại,
              khi đó f = NULL. Dùng JOIN thường → bài không feat ai bị loại khỏi kết quả,
              tìm theo tên bài sẽ không ra.

        LOWER(...) LIKE LOWER(CONCAT('%', :keyword, '%'))
            - CONCAT('%', 'may', '%') → '%may%'.
            - LIKE '%may%': chứa "may" ở BẤT KỲ VỊ TRÍ nào ("Mây Chiều", "Bài Hát May Mắn").
            - LOWER 2 vế: đổi hết về chữ thường → không phân biệt hoa thường.

        DISTINCT s
            - Bài feat 2 người → LEFT JOIN tạo ra 2 dòng cho cùng 1 bài.
            - DISTINCT gộp lại → mỗi bài chỉ xuất hiện 1 lần.

        countQuery (câu đếm riêng cho phân trang):
            - Mặc định Spring tự sinh câu đếm từ câu chính, nhưng với DISTINCT + JOIN
              câu tự sinh có thể đếm sai hoặc lỗi.
            - COUNT(DISTINCT s): đếm số BÀI khác nhau (không phải số dòng sau JOIN)
              → totalElements chính xác.
            - Điều kiện WHERE phải GIỐNG HỆT câu chính, nếu không số trang sẽ lệch với dữ liệu.

        GIỚI HẠN ĐÃ BIẾT:
            Ký tự '%' và '_' trong keyword có nghĩa đặc biệt với LIKE
            ('%' = chuỗi bất kỳ, '_' = 1 ký tự bất kỳ). Tìm "100%" sẽ khớp rộng hơn mong đợi.
            Không gây lỗi bảo mật (vẫn chống SQL injection), chỉ làm kết quả rộng hơn.
            Muốn chính xác tuyệt đối thì escape 2 ký tự này trong Service trước khi truyền vào.
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
        UC22 - Lấy 1 bài ĐÃ PHÁT HÀNH theo id.

        Khác findById (có sẵn của JpaRepository): findById trả về bài ở BẤT KỲ trạng thái nào.
        Người nghe phải dùng method này → bài chưa phát hành coi như không tồn tại.

        Optional<Song>: hộp có thể chứa 1 bài hoặc rỗng.
        - Tìm thấy → Optional chứa bài.
        - Không thấy (không tồn tại / chưa phát hành / bị ẩn...) → Optional rỗng.
        Service xử lý bằng .orElseThrow(...) → ném lỗi "Song does not exist".
        Dùng Optional thay vì trả null → buộc người gọi phải xử lý trường hợp không tìm thấy,
        tránh quên kiểm tra null rồi bị NullPointerException.

        Cũng được dùng trong SongService.play: chỉ tăng lượt nghe cho bài đã phát hành.
    */
    @Query("SELECT s FROM Song s WHERE s.id = :songId AND " + PUBLISHED_CONDITION)
    Optional<Song> findPublishedById(@Param("songId") Long songId,
                                     @Param("now") LocalDateTime now);

    /*
        UC25 - Bài đã phát hành mà Artist đó là Artist CHÍNH hoặc ĐƯỢC FEAT.

        (s.artist.id = :artistId OR f.id = :artistId):
            - s.artist.id = :artistId → bài do Artist này đăng.
            - f.id = :artistId        → bài Artist này được feat.
        LEFT JOIN + DISTINCT + countQuery: lý do giống hệt searchPublished ở trên.

        Giống Spotify: trang nghệ sĩ có cả bài của họ và bài họ hát chung.
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
        UC24 - Tăng lượt nghe bằng 1 câu UPDATE duy nhất.

        ---------- VÌ SAO KHÔNG LÀM "ĐỌC - CỘNG - GHI" TRONG JAVA? ----------

        Cách sai (bị RACE CONDITION):
            Song song = findById(15);                       // đọc: 100
            song.setPlayCount(song.getPlayCount() + 1);     // cộng: 101
            save(song);                                     // ghi: 101
        2 người nghe CÙNG LÚC:
            Người A đọc 100, người B đọc 100
            A ghi 101, B ghi 101
            → 2 lượt nghe nhưng chỉ tăng 1. Gọi là "lost update".

        Cách đúng (file này):
            UPDATE songs SET play_count = play_count + 1 WHERE id = 15
        Phép cộng xảy ra BÊN TRONG MySQL, trong 1 câu lệnh. MySQL tự khóa dòng đó
        trong lúc cập nhật → 1000 người nghe cùng lúc vẫn tăng đủ 1000.

        ---------- CÁC ANNOTATION ----------

        @Modifying: báo cho Spring đây là câu UPDATE / DELETE, không phải SELECT
            (thiếu annotation này, Spring tưởng là SELECT rồi báo lỗi).
        @Transactional: câu UPDATE bắt buộc chạy trong 1 transaction (giao dịch).
            Transaction = nhóm thao tác "thành công hết hoặc không có gì xảy ra".

        Trả về int: số dòng bị cập nhật (1 = thành công, 0 = không có bài này).

        ---------- ĐẶC ĐIỂM CỦA BULK UPDATE ----------

        Câu UPDATE viết thẳng này KHÔNG đi qua entity Song, nên:
        - KHÔNG gọi @PreUpdate → updatedAt không đổi (nghe nhạc không phải "sửa bài").
        - KHÔNG tăng version   → người nghe không gây xung đột Optimistic Locking với Admin / Artist.
        - Kết hợp updatable = false ở cột play_count: Admin / Artist save() bài
          cũng không ghi đè lượt nghe. Hai cơ chế bảo vệ lẫn nhau.
    */
    @Modifying
    @Transactional
    @Query("UPDATE Song s SET s.playCount = s.playCount + 1 WHERE s.id = :songId")
    int incrementPlayCount(@Param("songId") Long songId);

    /* ================= ARTIST (UC28 - UC33) ================= */

    /*
        UC28 - Bài hát của tôi: mọi trạng thái trừ DELETED, mới tải lên trước.

        Derived query: Spring đọc TÊN method rồi tự sinh truy vấn:
            findBy                → SELECT ... WHERE
            ArtistId              → artist.id = ?           (tham số 1: artistId)
            And                   → AND
            StatusNot             → status <> ?             (tham số 2: status)
            OrderByCreatedAtDesc  → ORDER BY created_at DESC
        Gọi: findByArtistIdAndStatusNotOrderByCreatedAtDesc(5L, SongStatus.DELETED)
        → SELECT * FROM songs WHERE artist_id = 5 AND status <> 'DELETED' ORDER BY created_at DESC

        Tận dụng index idx_songs_artist_status (artist_id, status) → nhanh.
        Trả List (không phân trang) vì 1 Artist thường chỉ có vài chục bài.
    */
    List<Song> findByArtistIdAndStatusNotOrderByCreatedAtDesc(Long artistId, SongStatus status);

    /*
        Tìm bài theo id VÀ phải thuộc về Artist đó.
            → SELECT * FROM songs WHERE id = ? AND artist_id = ?

        Đây là chốt BẢO MẬT quan trọng nhất của module Artist:
        Artist A gửi id bài của Artist B → điều kiện artist_id không khớp → Optional rỗng
        → Service báo "Song does not exist".
        → Artist không thể sửa / ẩn / xóa bài của người khác dù đoán được id.
    */
    Optional<Song> findByIdAndArtistId(Long id, Long artistId);

    /* ================= ADMIN (UC34) ================= */

    /*
        UC34 - Xem mọi bài trừ 1 trạng thái (Service truyền DELETED).
            → SELECT * FROM songs WHERE status <> 'DELETED' (kèm phân trang, sắp xếp)
    */
    Page<Song> findByStatusNot(SongStatus status, Pageable pageable);

    /*
        UC34 - Lọc đúng 1 trạng thái, VD chỉ bài PENDING đang chờ duyệt.
            → SELECT * FROM songs WHERE status = 'PENDING' (kèm phân trang, sắp xếp)
    */
    Page<Song> findByStatus(SongStatus status, Pageable pageable);
}