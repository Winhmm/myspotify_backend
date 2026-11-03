package com.winhmm.myspotify.repository;

/*
    =====================================================================
    PLAYLISTREPOSITORY - FILE GIỮ CHỖ, CHƯA DÙNG
    =====================================================================

    (Thứ tự đọc package repository: xem GenreRepository.)

    -----------------------------------------------------
    Hiện tại file này chưa làm gì
    -----------------------------------------------------
    Chức năng Playlist chưa làm, file này chỉ giữ chỗ và ghi sẵn thiết kế:
    - Là 1 class rỗng, chưa phải repository thật (chưa extends JpaRepository, chưa có method nào).
    - Không có annotation nào → Spring không tạo bean, không Service nào dùng tới.
    → Không ảnh hưởng gì khi chạy ứng dụng.

    Khi làm chức năng Playlist sẽ đổi thành interface, giống các repository khác:
        public interface PlaylistRepository extends JpaRepository<Playlist, Long> { ... }
    (class → interface: Spring Data chỉ tự sinh code cho interface, xem GenreRepository.)
    Bảng playlist_songs (entity PlaylistSong) sẽ cần thêm 1 repository riêng.

    =====================================================================
    THIẾT KẾ DỰ KIẾN
    =====================================================================

    Có sẵn từ JpaRepository: save, findById, delete...

    Viết thêm:
        countByOwnerId(...)     → đếm số playlist của 1 tài khoản (giới hạn 50)
        findWithOwnerById(...)  → 1 playlist kèm người tạo (xem chi tiết)
        findByOwner(...)        → mọi playlist của 1 tài khoản (trang "Playlist của tôi")
        findPublicByOwner(...)  → playlist công khai của 1 tài khoản (trang cá nhân người khác)

    =====================================================================
    JOIN FETCH LÀ GÌ?
    =====================================================================

    Field owner trong Playlist là LAZY: đọc playlist thì chưa đọc người tạo.
    Nếu sau đó gọi playlist.getOwner().getUsername() cho từng playlist,
    Hibernate chạy thêm 1 câu SELECT vào bảng users cho mỗi người tạo khác nhau
    (cùng 1 người thì chỉ tải 1 lần trong 1 request).
    → Danh sách 20 playlist của 20 người khác nhau = 1 + 20 câu SQL (gọi là lỗi "N + 1",
      xem thêm phần "Số câu truy vấn" trong SongMapper).

    JOIN FETCH p.owner: lấy luôn người tạo trong cùng 1 câu SQL (JOIN bảng users)
    → 20 playlist vẫn chỉ 1 câu SQL. VD:
        @Query("SELECT p FROM Playlist p JOIN FETCH p.owner WHERE p.id = :id")
    Khác JOIN thường (như trong SongRepository): JOIN thường chỉ dùng bảng users để lọc điều kiện,
    JOIN FETCH còn nạp luôn dữ liệu người tạo vào object Playlist.
*/
public class PlaylistRepository {
}