package com.winhmm.myspotify.repository;

/*
    =====================================================================
    PLAYLISTREPOSITORY - ĐỌC VÀ GHI BẢNG playlists
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
    Hibernate chạy thêm 1 câu SELECT vào bảng users cho mỗi playlist
    → 20 playlist = 1 + 20 câu SQL (gọi là lỗi "N + 1").

    JOIN FETCH p.owner: lấy luôn người tạo trong cùng 1 câu SQL (JOIN bảng users)
    → 20 playlist vẫn chỉ 1 câu SQL.
*/
public class PlaylistRepository {
}
