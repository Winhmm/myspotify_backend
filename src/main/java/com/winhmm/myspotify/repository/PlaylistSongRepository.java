package com.winhmm.myspotify.repository;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    - PlaylistSong : entity mà repository này làm việc cùng (bảng playlist_songs).
    - Pageable     : "yêu cầu phân trang": lấy trang mấy, mỗi trang bao nhiêu dòng
                     (dùng ở findPlayableCoverUrls để chỉ lấy 1 dòng).
    - JpaRepository, Query, Param: xem PlaylistRepository.
    - Modifying    : annotation @Modifying, đánh dấu câu truy vấn có thay đổi dữ liệu (UPDATE, DELETE).
    - LocalDateTime: kiểu ngày giờ, dùng cho tham số now (giờ hiện tại).
    - List, Optional: xem PlaylistRepository.
*/
import com.winhmm.myspotify.entity.PlaylistSong;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/*
    =====================================================================
    PLAYLISTSONGREPOSITORY - TRUY CẬP BẢNG PLAYLIST_SONGS (MYSQL)
    =====================================================================

    (Repository là gì, derived query, JPQL, @Query, @Param, JOIN FETCH: xem PlaylistRepository,
     nên đọc file đó trước.)

    Mỗi dòng trong bảng playlist_songs = 1 bài nằm trong 1 playlist:
        id   playlist_id   song_id   added_at
        1    3             15        2026-11-04 08:00:00
        2    3             22        2026-11-04 08:00:00
        3    3             9         2026-11-05 21:10:00

    Có sẵn từ JpaRepository<PlaylistSong, Long>: save, saveAll, delete...

    Viết thêm:
        existsByPlaylistIdAndSongId(...)  → bài đã có trong playlist chưa (thêm bài)
        countByPlaylistId(...)            → playlist đang có bao nhiêu dòng (giới hạn 200)
        findByPlaylistIdAndSongId(...)    → tìm 1 dòng để bỏ bài
        findPlayableSongs(...)            → các bài đang phát hành của playlist, theo thứ tự thêm
        countPlayableSongs(...)           → đếm số bài đang phát hành
        findPlayableCoverUrls(...)        → ảnh bìa của bài đầu tiên (làm ảnh bìa dự phòng)
        deleteAllByPlaylistId(...)        → xóa mọi dòng của 1 playlist (khi xóa playlist)

    Chỉ được dùng trong PlaylistService.

    =====================================================================
    "BÀI ĐANG PHÁT HÀNH" TRONG PLAYLIST
    =====================================================================

    Playlist giữ lại mọi bài đã thêm, kể cả bài sau đó bị ẩn, bị gỡ, bị xóa, hoặc Artist bị khóa.
    Nhưng khi hiển thị và đếm, chỉ tính bài đang phát hành:
        ... AND SongRepository.PUBLISHED_CONDITION
    (dùng lại đúng điều kiện của người nghe: APPROVED, đã tới giờ, Artist chính ACTIVE và còn role ARTIST).
    → Bài phát hành lại (Artist hiện lại bài, Admin mở khóa Artist...) tự xuất hiện lại trong playlist,
      không ai phải thêm lại.

    PUBLISHED_CONDITION là 1 hằng số kiểu String nằm trong SongRepository, nội dung là:
        s.status = APPROVED                   → Admin đã duyệt bài
        AND s.releaseAt <= :now               → đã tới giờ phát hành
        AND s.artist.accountStatus = ACTIVE   → tài khoản Artist chính còn hoạt động
        AND s.artist.role = ARTIST            → tài khoản đó vẫn còn là Artist
    Viết 1 lần ở SongRepository, các nơi khác nối chuỗi vào để dùng lại
    → sau này đổi điều kiện chỉ cần sửa 1 chỗ.

    SongRepository.PUBLISHED_CONDITION viết theo tên gọi tắt s (s.status, s.releaseAt, s.artist...),
    nên các câu bên dưới đều đặt tên gọi tắt cho bài hát là s: JOIN ps.song s.
    Có chỗ trống :now → hàm nào dùng điều kiện này cũng phải có tham số @Param("now").
*/
public interface PlaylistSongRepository extends JpaRepository<PlaylistSong, Long> {

    /*
        -----------------------------------------------------
        Bài đã có trong playlist chưa?
        -----------------------------------------------------
        Derived query: exists + By + PlaylistId + And + SongId.
        - exists       → trả về true (có ít nhất 1 dòng) hoặc false (không có dòng nào).
        - PlaylistId   → playlist.id (đi theo quan hệ playlist, giống SongId trong SongLyricsRepository).
        - SongId       → song.id.
        - And          → 2 điều kiện phải đúng cùng lúc.
        SQL tương đương: SELECT 1 FROM playlist_songs WHERE playlist_id = ? AND song_id = ? LIMIT 1
    */
    boolean existsByPlaylistIdAndSongId(Long playlistId, Long songId);

    /*
        -----------------------------------------------------
        Playlist đang có bao nhiêu dòng?
        -----------------------------------------------------
        SQL tương đương: SELECT COUNT(*) FROM playlist_songs WHERE playlist_id = ?
        Đếm tất cả dòng, kể cả bài không còn phát hành (vẫn chiếm 1 chỗ trong giới hạn 200).
    */
    long countByPlaylistId(Long playlistId);

    /*
        -----------------------------------------------------
        Tìm đúng 1 dòng (playlist, bài) để bỏ bài khỏi playlist
        -----------------------------------------------------
        Optional vì cặp (playlist_id, song_id) là UNIQUE → nhiều nhất 1 dòng.
        Không có dòng nào → Optional rỗng → Service báo "Song is not in this playlist".
    */
    Optional<PlaylistSong> findByPlaylistIdAndSongId(Long playlistId, Long songId);

    /*
        -----------------------------------------------------
        Các bài đang phát hành của 1 playlist, theo thứ tự thêm vào
        -----------------------------------------------------
        Dùng khi xem chi tiết playlist (và khi trả kết quả sau mỗi lần sửa).

        - SELECT ps FROM PlaylistSong ps: lấy các object PlaylistSong, tên gọi tắt ps.
        - JOIN FETCH ps.song s: nạp luôn bài hát trong cùng câu SQL
          (song là LAZY, không fetch thì mỗi bài thêm 1 câu SELECT).
        - WHERE ps.playlist.id = :playlistId: chỉ dòng của playlist này.
          ps.playlist.id đọc thẳng cột playlist_id, không cần JOIN sang bảng playlists.
        - AND PUBLISHED_CONDITION: chỉ bài đang phát hành (xem phần đầu file).
        - ORDER BY ps.addedAt ASC: thêm trước đứng trước.
          Thêm ps.id ASC: các bài thêm cùng lúc (VD lúc tạo playlist) vẫn giữ đúng thứ tự đã chọn,
          vì dòng nào lưu trước thì có id nhỏ hơn.

        Lưu ý dấu cách: PUBLISHED_CONDITION kết thúc bằng chữ "ARTIST" không có dấu cách,
        nên đoạn nối tiếp phải bắt đầu bằng dấu cách: " ORDER BY ...".

        SQL Hibernate sinh ra (gần đúng):
            SELECT ps.*, s.* FROM playlist_songs ps
            JOIN songs s ON s.id = ps.song_id
            JOIN users a ON a.id = s.artist_id
            WHERE ps.playlist_id = ? AND s.status = 'APPROVED' AND s.release_at <= ?
              AND a.account_status = 'ACTIVE' AND a.role = 'ARTIST'
            ORDER BY ps.added_at ASC, ps.id ASC
    */
    @Query("SELECT ps FROM PlaylistSong ps JOIN FETCH ps.song s "
            + "WHERE ps.playlist.id = :playlistId AND " + SongRepository.PUBLISHED_CONDITION
            + " ORDER BY ps.addedAt ASC, ps.id ASC")
    List<PlaylistSong> findPlayableSongs(@Param("playlistId") Long playlistId,
                                         @Param("now") LocalDateTime now);

    /*
        -----------------------------------------------------
        Đếm số bài đang phát hành của 1 playlist
        -----------------------------------------------------
        Dùng ở danh sách playlist (mỗi playlist hiện "12 bài").
        COUNT(ps): đếm số dòng thỏa điều kiện, MySQL tự đếm, không phải tải từng bài về Java.
        JOIN thường (không FETCH): chỉ cần bảng songs để lọc điều kiện, không cần dữ liệu bài.
    */
    @Query("SELECT COUNT(ps) FROM PlaylistSong ps JOIN ps.song s "
            + "WHERE ps.playlist.id = :playlistId AND " + SongRepository.PUBLISHED_CONDITION)
    long countPlayableSongs(@Param("playlistId") Long playlistId,
                            @Param("now") LocalDateTime now);

    /*
        -----------------------------------------------------
        Ảnh bìa của bài đang phát hành đầu tiên (ảnh bìa dự phòng)
        -----------------------------------------------------
        Dùng ở danh sách playlist, cho playlist không có ảnh bìa riêng.

        - SELECT s.coverUrl: chỉ lấy 1 cột (đường dẫn ảnh bìa), không lấy cả bài.
          Kiểu trả về List<String>.
        - Pageable: Service truyền PageRequest.of(0, 1) → trang đầu tiên (số 0), mỗi trang 1 dòng
          → chỉ lấy dòng đầu tiên (SQL thêm LIMIT 1).
        - Bài đầu tiên không có ảnh bìa → danh sách có 1 phần tử null
          → playlist không có ảnh → frontend hiện ảnh mặc định.
        - Playlist không còn bài nào phát hành → danh sách rỗng.
    */
    @Query("SELECT s.coverUrl FROM PlaylistSong ps JOIN ps.song s "
            + "WHERE ps.playlist.id = :playlistId AND " + SongRepository.PUBLISHED_CONDITION
            + " ORDER BY ps.addedAt ASC, ps.id ASC")
    List<String> findPlayableCoverUrls(@Param("playlistId") Long playlistId,
                                       @Param("now") LocalDateTime now,
                                       Pageable pageable);

    /*
        -----------------------------------------------------
        Xóa mọi dòng của 1 playlist (khi xóa playlist)
        -----------------------------------------------------
        Phải xóa các dòng ở bảng playlist_songs trước, rồi mới xóa playlist:
        cột playlist_id là khóa ngoại trỏ tới playlists.id,
        xóa playlist khi vẫn còn dòng trỏ tới nó → MySQL báo lỗi khóa ngoại.

        SQL tương đương: DELETE FROM playlist_songs WHERE playlist_id = ?
        1 câu DELETE xóa hết, không phải tải từng dòng về rồi xóa từng cái.

        - @Modifying: bắt buộc với câu @Query có thay đổi dữ liệu (UPDATE, DELETE).
          Thiếu → Spring tưởng đây là câu SELECT và báo lỗi khi chạy
          (giống incrementPlayCount trong SongRepository).
        - Câu thay đổi dữ liệu phải chạy bên trong 1 transaction.
          Hàm gọi tới (PlaylistService.deletePlaylist) có @Transactional nên đã có sẵn.
        - Trả về int: số dòng đã xóa (VD playlist có 12 bài → 12).
    */
    @Modifying
    @Query("DELETE FROM PlaylistSong ps WHERE ps.playlist.id = :playlistId")
    int deleteAllByPlaylistId(@Param("playlistId") Long playlistId);
}