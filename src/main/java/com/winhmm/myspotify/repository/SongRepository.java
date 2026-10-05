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

public interface SongRepository extends JpaRepository<Song, Long> {
    /*
        Điều kiện "bài đã phát hành" - người nghe thấy được (UC20 - UC25):
        1. Bài đã được Admin duyệt (APPROVED).
        2. Đã tới giờ phát hành (releaseAt <= hiện tại).
        3. Artist CHÍNH đang hoạt động và vẫn còn là Artist.
           → Artist chính bị khóa / xóa / thu hồi quyền → bài tự ẩn.
    */
    String PUBLISHED_CONDITION =
            "s.status = com.winhmm.myspotify.enums.SongStatus.APPROVED "
                    + "AND s.releaseAt <= :now "
                    + "AND s.artist.accountStatus = com.winhmm.myspotify.enums.AccountStatus.ACTIVE "
                    + "AND s.artist.role = com.winhmm.myspotify.enums.Role.ARTIST";

    /*
        Điều kiện "Artist feat. đang hoạt động":
        1. Tài khoản đang ACTIVE.
        2. Vẫn còn role ARTIST.

        Dùng khi TÌM KIẾM theo tên Artist feat. (UC21):
        → Artist feat. bị khóa thì tên của họ KHÔNG được tính khi tìm.
        (Bài hát vẫn hiển thị bình thường, chỉ ẩn tên người đó.)
    */
    String ACTIVE_FEATURED_CONDITION =
            "f.accountStatus = com.winhmm.myspotify.enums.AccountStatus.ACTIVE "
                    + "AND f.role = com.winhmm.myspotify.enums.Role.ARTIST";

    /* ================= NGƯỜI NGHE (UC20 - UC25) ================= */

    /*
        UC20 - Danh sách bài đã phát hành (có phân trang).
    */
    @Query("SELECT s FROM Song s WHERE " + PUBLISHED_CONDITION)
    Page<Song> findPublished(@Param("now") LocalDateTime now, Pageable pageable);

    /*
        UC21 - Tìm kiếm bài đã phát hành theo keyword.

        Tìm keyword trong:
        - Tên bài hát.
        - Nghệ danh Artist chính.
        - Nghệ danh Artist feat. (chỉ tính người đang hoạt động).

        - LEFT JOIN: lấy cả những bài KHÔNG có ai feat.
        - LOWER + LIKE '%keyword%': không phân biệt hoa thường,
          keyword chỉ cần xuất hiện một phần trong tên.
        - DISTINCT: 1 bài có nhiều người feat. trùng keyword → chỉ lấy 1 lần.
        - countQuery: câu đếm tổng số kết quả, dùng cho phân trang.
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
        UC22, UC23 - Lấy 1 bài đã phát hành theo id.
        Bài chưa phát hành / không tồn tại → Optional rỗng.
    */
    @Query("SELECT s FROM Song s WHERE s.id = :songId AND " + PUBLISHED_CONDITION)
    Optional<Song> findPublishedById(@Param("songId") Long songId,
                                     @Param("now") LocalDateTime now);

    /*
        UC25 - Bài đã phát hành mà Artist đó là Artist chính hoặc được feat.
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
        Nhiều người nghe cùng lúc vẫn đếm đúng.

        @Modifying: báo cho Spring đây là câu UPDATE / DELETE, không phải SELECT.
        @Transactional: câu UPDATE bắt buộc phải chạy trong 1 transaction.
    */
    @Modifying
    @Transactional
    @Query("UPDATE Song s SET s.playCount = s.playCount + 1 WHERE s.id = :songId")
    int incrementPlayCount(@Param("songId") Long songId);

    /* ================= ARTIST (UC28 - UC33) ================= */

    /*
        UC28 - Bài hát của tôi: mọi trạng thái trừ DELETED, mới nhất lên đầu.
    */
    List<Song> findByArtistIdAndStatusNotOrderByCreatedAtDesc(Long artistId, SongStatus status);

    /*
        Tìm bài theo id VÀ phải thuộc về Artist đó.
        → Artist không thể sửa / ẩn / xóa bài của người khác.
    */
    Optional<Song> findByIdAndArtistId(Long id, Long artistId);

    /* ================= ADMIN (UC34) ================= */

    /*
        UC34 - Xem toàn bộ bài (trừ DELETED).
    */
    Page<Song> findByStatusNot(SongStatus status, Pageable pageable);

    /*
        UC34 - Lọc theo 1 trạng thái.
    */
    Page<Song> findByStatus(SongStatus status, Pageable pageable);
}
