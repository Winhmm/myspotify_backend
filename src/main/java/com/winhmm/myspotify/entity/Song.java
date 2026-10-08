package com.winhmm.myspotify.entity;

import com.winhmm.myspotify.enums.SongStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "songs",
        indexes = {
                @Index(name = "idx_songs_status_release", columnList = "status, release_at"),
                @Index(name = "idx_songs_artist_status", columnList = "artist_id, status")
        }
)
public class Song {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
        Artist CHÍNH - người đăng bài (quan hệ nhiều - một).
        - Nhiều bài hát thuộc về 1 Artist.
        - Cột artist_id trong bảng songs trỏ tới users.id.
        - Chỉ Artist chính có quyền sửa / ẩn / xóa / đổi lịch bài hát.
    */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = false)
    private User artist;

    /*
        Các Artist FEAT. (quan hệ nhiều - nhiều).
        - 1 bài hát có thể có nhiều Artist feat. (tối đa 5).
        - 1 Artist có thể được feat. trong nhiều bài hát.
        - Bảng trung gian song_featured_artists lưu mối quan hệ này:
            song_id   → songs.id
            artist_id → users.id
        - Artist feat. chỉ được ghi tên, KHÔNG có quyền quản lý bài.
        - Dùng Set (không phải List) → không feat. trùng 1 người 2 lần.
    */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "song_featured_artists",
            joinColumns = @JoinColumn(name = "song_id"),
            inverseJoinColumns = @JoinColumn(name = "artist_id")
    )
    private Set<User> featuredArtists = new HashSet<>();

    /*
        Tên bài hát.
    */
    @Column(nullable = false, length = 150)
    private String title;

    /*
        Đường dẫn file nhạc (.mp3 tối đa 20MB, .wav tối đa 100MB).
    */
    @Column(name = "audio_url", nullable = false, length = 255)
    private String audioUrl;

    /*
        Đường dẫn ảnh bìa (không bắt buộc, JPG / PNG / WEBP tối đa 2MB).
    */
    @Column(name = "cover_url", length = 255)
    private String coverUrl;

    /*
        Thời lượng bài hát (giây).
        Backend tự đọc từ file nhạc khi tải lên.
    */
    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds;

    /*
        Lượt nghe, mặc định 0, tăng 1 mỗi lần bài được phát (UC24).
    */
    @Column(name = "play_count", nullable = false)
    private Long playCount = 0L;

    /*
        Trạng thái bài hát, mặc định PENDING (chờ Admin duyệt).
    */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SongStatus status = SongStatus.PENDING;

    /*
        Thời điểm phát hành:
        - NULL khi chờ duyệt → phát hành ngay sau khi Admin duyệt.
        - Có giá trị khi chờ duyệt → lịch phát hành Artist đã chọn (UC27).
        - Khi Admin duyệt: nếu đang NULL → gán = thời điểm duyệt.
        → Mọi bài APPROVED đều có releaseAt.
    */
    @Column(name = "release_at")
    private LocalDateTime releaseAt;

    /*
        Lý do bị từ chối (UC35) hoặc bị gỡ (UC36).
        Được xóa (NULL) khi bài được duyệt thành công.
    */
    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    /*
        Thời điểm tải lên (tự gán, không bao giờ thay đổi).
    */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /*
        Thời điểm sửa gần nhất (tự gán).
        Tăng lượt nghe không làm thay đổi cột này.
    */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /*
        @PrePersist: Hibernate tự gọi ngay trước khi INSERT bài hát mới.
        → Gán createdAt và updatedAt = thời điểm hiện tại.
    */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /*
        @PreUpdate: Hibernate tự gọi ngay trước khi UPDATE bài hát.
        → Gán updatedAt = thời điểm hiện tại.
    */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /*
        Không có setter cho id, createdAt, updatedAt
        → các trường này do database / Hibernate tự quản lý.
    */
    public Long getId() { return id; }

    public User getArtist() { return artist; }
    public void setArtist(User artist) { this.artist = artist; }

    public Set<User> getFeaturedArtists() { return featuredArtists; }
    public void setFeaturedArtists(Set<User> featuredArtists) { this.featuredArtists = featuredArtists; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public Long getPlayCount() { return playCount; }
    public void setPlayCount(Long playCount) { this.playCount = playCount; }

    public SongStatus getStatus() { return status; }
    public void setStatus(SongStatus status) { this.status = status; }

    public LocalDateTime getReleaseAt() { return releaseAt; }
    public void setReleaseAt(LocalDateTime releaseAt) { this.releaseAt = releaseAt; }

    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
