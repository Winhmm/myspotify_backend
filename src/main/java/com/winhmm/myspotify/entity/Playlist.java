package com.winhmm.myspotify.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/*
    =====================================================================
    PLAYLIST - ENTITY ÁNH XẠ BẢNG playlists (MYSQL)
    =====================================================================

    Mỗi dòng của bảng = 1 playlist do 1 tài khoản tạo.

    Các cột:
        id          : khóa chính, tự tăng
        user_id     : người tạo playlist (khóa ngoại trỏ tới users.id)
        name        : tên playlist, tối đa 100 ký tự
        description : mô tả, tối đa 300 ký tự, có thể để trống
        is_public   : true = công khai, false = riêng tư (mặc định khi tạo)
        cover_url   : ảnh bìa do người tạo tự tải lên, có thể để trống
        created_at  : thời điểm tạo
        updated_at  : thời điểm sửa gần nhất (đổi tên, đổi ảnh, thêm hoặc bớt bài)

    =====================================================================
    ẢNH BÌA
    =====================================================================

    Cột cover_url chỉ lưu ảnh người tạo tự tải lên.
    - Có ảnh riêng       → hiển thị ảnh đó.
    - Không có ảnh riêng → hiển thị ảnh bìa của bài đầu tiên trong playlist
                           (bài còn nghe được và có ảnh bìa).

    Ảnh "tự động" không lưu vào cột này mà được tính mỗi lần trả kết quả (trong PlaylistService).
    Nhờ vậy khi bài đầu tiên bị bớt khỏi playlist, hoặc bị ẩn, thì ảnh đại diện tự đổi theo,
    không phải cập nhật lại database.

    =====================================================================
    DANH SÁCH BÀI HÁT NẰM Ở ĐÂU?
    =====================================================================

    Không nằm trong bảng này mà nằm ở bảng playlist_songs (entity PlaylistSong).

    Class này cố ý không khai báo danh sách bài (List<PlaylistSong>), để đơn giản:
    - Muốn lấy bài thì hỏi PlaylistSongRepository.
    - Khi chỉ cần tên playlist (VD trang "Playlist của tôi"), Hibernate không phải tải kèm cả danh sách bài.
*/
@Entity
@Table(name = "playlists")
public class Playlist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 300)
    private String description;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic = false;

    @Column(name = "cover_url", length = 255)
    private String coverUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /*
        Đánh dấu playlist vừa thay đổi (dùng khi thêm hoặc bớt bài).
        Đổi updatedAt → Hibernate thấy playlist có thay đổi → UPDATE khi lưu.
    */
    public void markUpdated() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
