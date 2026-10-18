package com.winhmm.myspotify.entity;

import com.winhmm.myspotify.enums.SongStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/*
    =====================================================================
    Song - ENTITY ÁNH XẠ BẢNG songs (MYSQL)
    =====================================================================

    Các cột chính:
        id, artist_id, title, audio_url, cover_url, duration_seconds, play_count,
        status, release_at, reject_reason, created_at, updated_at, version

    Bảng phụ: song_featured_artists (danh sách Artist feat của mỗi bài).

    =====================================================================
    ENTITY LÀ GÌ?
    =====================================================================

    Entity là class Java đại diện cho 1 bảng trong database.
        - 1 class     ↔ 1 bảng   (Song ↔ songs)
        - 1 đối tượng ↔ 1 dòng   (new Song() ↔ 1 bài hát)
        - 1 thuộc tính ↔ 1 cột   (title ↔ cột title)

    Hibernate (thư viện JPA mà Spring Data JPA dùng bên dưới) tự viết câu SQL giúp mình:
        songRepository.save(song)    → INSERT INTO songs (...) VALUES (...)   (bài mới)
                                     → UPDATE songs SET ... WHERE id = ?      (bài đã có)
        songRepository.findById(15L) → SELECT ... FROM songs WHERE id = 15
    → Mình làm việc với object Java, không phải tự viết SQL cho các thao tác cơ bản.

    =====================================================================
    TỰ TẠO BẢNG
    =====================================================================

    application.properties có spring.jpa.hibernate.ddl-auto=update
    → Khi ứng dụng khởi động, Hibernate đọc các annotation trong class này
      rồi tự tạo bảng songs (nếu chưa có) hoặc thêm cột mới (VD cột version).
    → "update" không xóa cột cũ, không đổi kiểu cột đã có.
      Đổi tên thuộc tính → Hibernate tạo cột mới, cột cũ vẫn nằm đó (phải tự xóa bằng SQL).

    =====================================================================
    CÁC ANNOTATION Ở ĐẦU CLASS
    =====================================================================

    @Entity
        → Báo cho Hibernate: class này là 1 bảng, hãy quản lý nó.

    @Table(name = "songs", indexes = {...})
        → Tên bảng là "songs" (không ghi thì Hibernate đặt theo tên class).
        → indexes: tạo chỉ mục (index) để tăng tốc truy vấn.

    =====================================================================
    INDEX LÀ GÌ, VÌ SAO CẦN 2 INDEX NÀY?
    =====================================================================

    Không có index: tìm "các bài APPROVED" → MySQL phải đọc từng dòng của cả bảng.
    10 bài thì không sao, 1 triệu bài thì rất chậm.
    Có index: giống mục lục cuối sách, MySQL nhảy thẳng tới những dòng cần tìm.

    1. idx_songs_status_release (status, release_at)
       Phục vụ mọi truy vấn của người nghe:
           WHERE status = 'APPROVED' AND release_at <= now
       Đây là truy vấn chạy nhiều nhất cả hệ thống (mỗi lần mở trang chủ).
       Admin lọc theo trạng thái (WHERE status = 'PENDING') cũng dùng được index này.

    2. idx_songs_artist_status (artist_id, status)
       Phục vụ "Bài hát của tôi" của Artist:
           WHERE artist_id = 5 AND status <> 'DELETED'

    Index nhiều cột (composite index): thứ tự cột quan trọng.
        Index (status, release_at) dùng được cho:
            WHERE status = ?                       ✓
            WHERE status = ? AND release_at <= ?   ✓
            WHERE release_at <= ?                  ✗ (thiếu cột đầu tiên)
        Quy tắc "tiền tố bên trái" (leftmost prefix), giống danh bạ xếp theo Họ rồi Tên:
        tìm theo Họ được, tìm theo Họ + Tên được, chỉ có Tên thì không tra nhanh được.

    Đánh đổi: index làm INSERT, UPDATE chậm đi một chút (phải cập nhật cả mục lục),
    nên chỉ tạo index cho những truy vấn thật sự dùng nhiều.

    Kiểm tra index trong MySQL: SHOW INDEX FROM songs;
*/
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
        ---------------------------------------------------------------
        Artist chính (người đăng bài)
        ---------------------------------------------------------------
        Quan hệ nhiều - một (@ManyToOne):
        - Nhiều bài hát thuộc về 1 Artist.
        - Trong database: bảng songs có cột artist_id trỏ tới users.id (khóa ngoại).
        - Trong Java: song.getArtist() trả về luôn object User, không phải con số id.

        @JoinColumn(name = "artist_id", nullable = false):
        - Tên cột khóa ngoại là artist_id.
        - nullable = false: bài hát bắt buộc phải có chủ.

        fetch = FetchType.LAZY (tải lười):
        - Khi đọc 1 Song từ database, Hibernate chưa đọc User ngay,
          chỉ tạo 1 đối tượng giả (proxy) đã biết sẵn id của Artist.
        - getArtist().getId(): chưa chạy SQL.
        - getArtist().getArtistName(): lúc này mới chạy thêm câu
          SELECT ... FROM users WHERE id = ?.
        - Lợi ích: nhiều chỗ chỉ cần thông tin bài, không cần thông tin Artist
          → không tốn thêm truy vấn.
        - Ngược lại EAGER (tải ngay): luôn đọc kèm User dù không dùng tới.

        Quyền: chỉ Artist chính được sửa, ẩn, xóa, đổi lịch bài.
        Không có cascade: xóa bài không xóa User, lưu bài không lưu User.
    */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = false)
    private User artist;

    /*
        ---------------------------------------------------------------
        Các Artist feat
        ---------------------------------------------------------------
        Quan hệ nhiều - nhiều (@ManyToMany):
        - 1 bài hát có thể có nhiều Artist feat (tối đa 5, kiểm tra trong Service).
        - 1 Artist có thể được feat trong nhiều bài hát.

        Database không lưu được danh sách trong 1 ô, nên cần bảng trung gian:

            songs                 song_featured_artists          users
            ┌────┬───────────┐    ┌─────────┬───────────┐    ┌────┬─────────────┐
            │ id │ title     │    │ song_id │ artist_id │    │ id │ artist_name │
            ├────┼───────────┤    ├─────────┼───────────┤    ├────┼─────────────┤
            │ 15 │ Chiều Mưa │◄───│   15    │    12     │───►│ 12 │ Sóng Nhỏ    │
            │    │           │◄───│   15    │    13     │───►│ 13 │ Đom Đóm     │
            └────┴───────────┘    └─────────┴───────────┘    └────┴─────────────┘
            → Bài 15 feat 2 người: id 12 và id 13.

        @JoinTable(...):
        - name = "song_featured_artists" : tên bảng trung gian (Hibernate tự tạo).
        - joinColumns = song_id          : cột trỏ về bảng songs (phía class này).
        - inverseJoinColumns = artist_id : cột trỏ về bảng users (phía bên kia).
    */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "song_featured_artists",
            joinColumns = @JoinColumn(name = "song_id"),
            inverseJoinColumns = @JoinColumn(name = "artist_id")
    )
    private Set<User> featuredArtists = new HashSet<>();

    /*
        ---------------------------------------------------------------
        Tên bài hát
        ---------------------------------------------------------------
    */
    @Column(nullable = false, length = 150)
    private String title;

    /*
        ---------------------------------------------------------------
        Đường dẫn file nhạc
        ---------------------------------------------------------------

        Giới hạn: .mp3 tối đa 20MB, .wav tối đa 100MB (kiểm tra trong FileStorageService).
    */
    @Column(name = "audio_url", nullable = false, length = 255)
    private String audioUrl;

    /*
        ---------------------------------------------------------------
        Đường dẫn ảnh bìa
        ---------------------------------------------------------------
        Không bắt buộc (không có nullable = false → cho phép NULL).
        JPG, PNG, WEBP, tối đa 2MB.
    */
    @Column(name = "cover_url", length = 255)
    private String coverUrl;

    /*
        ---------------------------------------------------------------
        Thời lượng bài hát (giây)
        ---------------------------------------------------------------
    */
    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds;

    /*
        ---------------------------------------------------------------
        Lượt nghe, mặc định 0
        ---------------------------------------------------------------
    */
    @Column(name = "play_count", nullable = false, updatable = false)
    private Long playCount = 0L;

    /*
        ---------------------------------------------------------------
        Trạng thái bài hát, mặc định PENDING (chờ Admin duyệt)
        ---------------------------------------------------------------
    */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SongStatus status = SongStatus.PENDING;

    /*
        ---------------------------------------------------------------
        Thời điểm phát hành
        ---------------------------------------------------------------
    */
    @Column(name = "release_at")
    private LocalDateTime releaseAt;

    /*
        ---------------------------------------------------------------
        Lý do bị từ chối (UC35) hoặc bị gỡ (UC36)
        ---------------------------------------------------------------
    */
    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    /*
        ---------------------------------------------------------------
        Thời điểm tải lên
        ---------------------------------------------------------------
    */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /*
        ---------------------------------------------------------------
        Thời điểm sửa gần nhất
        ---------------------------------------------------------------
    */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /*
        =====================================================================
        OPTIMISTIC LOCKING: CHỐNG "LOST UPDATE"
        =====================================================================

        Vấn đề: 2 người sửa cùng 1 bài cùng lúc.
            t1  Admin đọc bài 15: status = PENDING
            t2  Artist đọc bài 15: status = PENDING
            t3  Artist xóa bài: ghi status = DELETED, xóa file nhạc trên ổ đĩa
            t4  Admin duyệt:    ghi status = APPROVED   ← ghi đè!
            → Bài đã xóa "sống lại", hiện cho người nghe nhưng file nhạc đã mất.

        Giải pháp: @Version. Mỗi bài có 1 con số "phiên bản".
            t1  Admin đọc:  version = 3
            t2  Artist đọc: version = 3
            t3  Artist ghi: UPDATE ... SET ..., version = 4 WHERE id = 15 AND version = 3
                            → khớp 1 dòng ✓
            t4  Admin ghi:  UPDATE ... SET ..., version = 4 WHERE id = 15 AND version = 3
                            → version trong database đã là 4 → không khớp dòng nào ✗
                            → Hibernate ném ObjectOptimisticLockingFailureException
                            → GlobalExceptionHandler trả 409:
                              "This data was changed by someone else, please reload and try again"

        Hibernate tự làm hết: tự thêm "AND version = ?" vào mọi câu UPDATE,
        tự tăng version sau mỗi lần ghi thành công. Mình không viết thêm code nào.

        Gọi là "optimistic" (lạc quan) vì giả định xung đột hiếm khi xảy ra:
        không khóa bài khi đọc, chỉ kiểm tra lúc ghi. Khác "pessimistic" (bi quan):
        khóa bài ngay khi đọc, người khác phải đợi → an toàn hơn nhưng chậm hơn nhiều.

        Lưu ý: incrementPlayCount (bulk update) không tăng version
        → người nghe không gây xung đột với Admin, Artist. Đúng ý.

        Dùng long (nguyên thủy) thay vì Long:
        - Không bao giờ null, bài mới bắt đầu từ 0.
        - Spring Data dựa vào version để đoán "bài mới hay bài cũ" khi save():
          nếu version là Long và đang null → coi là bài mới → chạy INSERT.
          Dùng long → Spring Data chuyển sang dựa vào id (id null mới là bài mới) → luôn đoán đúng.

        Cột version được thêm sau khi bảng songs đã có dữ liệu:
        ddl-auto=update thêm cột mới với giá trị NULL cho mọi bài cũ, mà kiểu long
        không nhận được NULL → đọc bài cũ sẽ lỗi. Vì vậy đã chạy 1 lần (cả local và VPS):
            UPDATE songs SET version = 0 WHERE version IS NULL;

        Không có setter: chỉ Hibernate được đổi version.
        Kiểm chứng bằng test: SongOptimisticLockTest.
    */
    @Version
    @Column(nullable = false)
    private long version;

    /*
        =====================================================================
        LIFECYCLE CALLBACK: HÀM HIBERNATE TỰ GỌI
        =====================================================================

        @PrePersist: Hibernate tự gọi ngay trước khi INSERT bài hát mới.
        → Gán createdAt và updatedAt bằng thời điểm hiện tại.
        → Service không phải nhớ gán ngày tạo mỗi khi tạo bài.

        protected: chỉ Hibernate (và class con) gọi, code bên ngoài không gọi nhầm.
    */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /*
        @PreUpdate: Hibernate tự gọi ngay trước khi UPDATE bài hát (qua save()).
        → Gán updatedAt bằng thời điểm hiện tại.
    */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

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

    public SongStatus getStatus() { return status; }
    public void setStatus(SongStatus status) { this.status = status; }

    public LocalDateTime getReleaseAt() { return releaseAt; }
    public void setReleaseAt(LocalDateTime releaseAt) { this.releaseAt = releaseAt; }

    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public long getVersion() { return version; }
}