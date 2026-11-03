package com.winhmm.myspotify.entity;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    (Entity là gì, các annotation cơ bản: xem User. @ManyToOne và LAZY: xem OtpVerification.
     @PrePersist, @PreUpdate: xem Song. Thứ tự đọc package entity: xem User.)

    - jakarta.persistence.*: mượn tất cả annotation của JPA.
    - LocalDateTime        : kiểu ngày giờ của Java, dùng cho createdAt, updatedAt.
*/
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

    Ví dụ dữ liệu:
        id | user_id | name           | description          | is_public | created_at
        3  | 6       | Chill buổi tối | Nghe trước khi ngủ   | 1         | 2026-10-27 22:30:00
        4  | 6       | Nhạc chạy bộ   | NULL                 | 0         | 2026-10-28 07:10:00
        → Tài khoản id 6 có 2 playlist: 1 công khai, 1 riêng tư.

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
    /*
        id: khóa chính, MySQL tự tăng (xem User).
    */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
        -----------------------------------------------------
        owner: người tạo playlist
        -----------------------------------------------------
        Quan hệ nhiều - một: 1 tài khoản có nhiều playlist (giống OtpVerification.user).
        - Tên thuộc tính trong Java là owner (chủ sở hữu) cho dễ hiểu,
          còn tên cột trong database vẫn là user_id (@JoinColumn(name = "user_id")).
          Tên thuộc tính và tên cột không bắt buộc giống nhau.
        - LAZY: đọc playlist chưa đọc kèm người tạo.
          Khi cần cả người tạo (VD hiện tên chủ playlist), PlaylistRepository dùng
          "JOIN FETCH p.owner" để lấy cả 2 trong 1 câu SQL,
          thay vì 1 câu cho playlist rồi thêm 1 câu cho mỗi người tạo.

        Chỉ owner được sửa, xóa, thêm bớt bài (PlaylistService kiểm tra).
    */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    /*
        name: tên playlist, VARCHAR(100), bắt buộc.
        description: mô tả, VARCHAR(300), có thể NULL.
        DTO tạo và sửa playlist nên có @Size khớp 2 con số này (giống cách dto.user khớp với User).
    */
    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 300)
    private String description;

    /*
        -----------------------------------------------------
        isPublic: playlist công khai hay riêng tư
        -----------------------------------------------------
        - true : người khác xem được (trên trang cá nhân của chủ playlist).
        - false: chỉ chủ playlist xem được. Mặc định khi tạo (= false).
        - name = "is_public": cột trong MySQL; boolean thành 0 hoặc 1.

        Vì sao thuộc tính tên isPublic mà không phải public?
        public là từ khóa của Java (public class, public void...), không đặt làm tên biến được.

        Cặp getter, setter theo quy ước của boolean: isPublic() và setPublic(...).
        Hibernate dùng field access (xem User) nên tên thuộc tính trong câu JPQL là isPublic:
            ... WHERE p.isPublic = true   (xem PlaylistRepository.findPublicByOwner)
    */
    @Column(name = "is_public", nullable = false)
    private boolean isPublic = false;

    /*
        coverUrl: đường dẫn ảnh bìa người tạo tự tải lên, NULL nếu chưa có
        (xem phần "Ảnh bìa" ở trên).
    */
    @Column(name = "cover_url", length = 255)
    private String coverUrl;

    /*
        createdAt: thời điểm tạo. updatable = false → chỉ ghi 1 lần lúc INSERT (xem Song).
        updatedAt: thời điểm sửa gần nhất.
        Cả 2 do onCreate(), onUpdate(), markUpdated() bên dưới tự gán, không có setter.
    */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /*
        @PrePersist: chạy trước INSERT → gán ngày tạo và ngày sửa.
        @PreUpdate : chạy trước UPDATE → gán lại ngày sửa.
        (Giải thích chi tiết: xem Song.)
    */
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
        -----------------------------------------------------
        markUpdated(): đánh dấu playlist vừa thay đổi
        -----------------------------------------------------
        Dùng khi thêm hoặc bớt bài.

        Vì sao cần hàm này?
        Thêm bài = thêm 1 dòng vào bảng playlist_songs, không đổi cột nào của bảng playlists.
        Hibernate chỉ chạy UPDATE (và gọi @PreUpdate) khi thấy đối tượng có thay đổi
        → nếu không làm gì, updatedAt của playlist vẫn là giờ cũ.

        markUpdated() tự đổi updatedAt → Hibernate thấy playlist có thay đổi → chạy UPDATE khi lưu.
        Khác onUpdate(): hàm này public, Service gọi chủ động; onUpdate() chỉ Hibernate tự gọi.
    */
    public void markUpdated() {
        updatedAt = LocalDateTime.now();
    }

    /*
        -----------------------------------------------------
        Getter và setter
        -----------------------------------------------------
        Có setter: owner, name, description, isPublic, coverUrl (những gì được phép đổi).
        Không có setter: id (MySQL tự cấp), createdAt, updatedAt (tự gán ở các hàm trên).
    */
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