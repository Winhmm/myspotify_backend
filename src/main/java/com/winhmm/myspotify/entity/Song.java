package com.winhmm.myspotify.entity;

import com.winhmm.myspotify.enums.SongStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/*
    ===================================================================
    Song - ENTITY ánh xạ bảng "songs" trong database.
    ===================================================================

    ---------- ENTITY LÀ GÌ? ----------

    Entity = class Java đại diện cho 1 bảng trong database.
        - 1 class    ↔ 1 bảng       (Song ↔ songs)
        - 1 đối tượng ↔ 1 dòng      (new Song() ↔ 1 bài hát)
        - 1 thuộc tính ↔ 1 cột      (title ↔ cột title)

    Hibernate (thư viện JPA mà Spring Data JPA dùng bên dưới) tự viết câu SQL giúp mình:
        songRepository.save(song)      → INSERT INTO songs (...) VALUES (...)   (bài mới)
                                       → UPDATE songs SET ... WHERE id = ?      (bài đã có)
        songRepository.findById(15L)   → SELECT ... FROM songs WHERE id = 15
    → Mình làm việc với object Java, không phải tự viết SQL cho các thao tác cơ bản.

    ---------- TỰ TẠO BẢNG ----------

    application.properties có spring.jpa.hibernate.ddl-auto=update
    → Khi ứng dụng khởi động, Hibernate đọc các annotation trong class này
      rồi tự tạo bảng "songs" (nếu chưa có) hoặc thêm cột mới (VD cột version).
    → "update" KHÔNG xóa cột cũ, không đổi kiểu cột đã có. Đổi tên thuộc tính
      → Hibernate tạo cột mới, cột cũ vẫn nằm đó (phải tự xóa bằng SQL).

    ---------- CÁC ANNOTATION Ở ĐẦU CLASS ----------

    @Entity
        → Báo cho Hibernate: class này là 1 bảng, hãy quản lý nó.

    @Table(name = "songs", indexes = {...})
        → Tên bảng là "songs" (không ghi thì Hibernate dùng tên class "song").
        → indexes: tạo chỉ mục (index) để TĂNG TỐC truy vấn.

    ---------- INDEX LÀ GÌ, VÌ SAO CẦN 2 INDEX NÀY? ----------

    Không có index: tìm "các bài APPROVED" → MySQL phải đọc TỪNG DÒNG của cả bảng.
    10 bài thì không sao, 1 triệu bài thì rất chậm.
    Có index: giống mục lục cuối sách, MySQL nhảy thẳng tới những dòng cần tìm.

    1. idx_songs_status_release (status, release_at)
       Phục vụ mọi truy vấn của NGƯỜI NGHE:
           WHERE status = 'APPROVED' AND release_at <= now
       Đây là truy vấn chạy nhiều nhất cả hệ thống (mỗi lần mở trang chủ).

    2. idx_songs_artist_status (artist_id, status)
       Phục vụ "Bài hát của tôi" của ARTIST:
           WHERE artist_id = 5 AND status <> 'DELETED'

    Index nhiều cột (composite index): thứ tự cột QUAN TRỌNG.
        Index (status, release_at) dùng được cho:
            WHERE status = ?                       ✓
            WHERE status = ? AND release_at <= ?   ✓
            WHERE release_at <= ?                  ✗ (thiếu cột đầu tiên)
        Quy tắc "tiền tố bên trái" (leftmost prefix), giống danh bạ xếp theo Họ rồi Tên:
        tìm theo Họ được, tìm theo Họ + Tên được, chỉ có Tên thì không tra nhanh được.

    Đánh đổi: index làm INSERT / UPDATE chậm đi một chút (phải cập nhật cả mục lục),
    nên chỉ tạo index cho những truy vấn thật sự dùng nhiều.
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
    /*
        Khóa chính (primary key) của bảng.

        @Id: đánh dấu đây là khóa chính, mỗi bài hát có 1 id duy nhất.

        @GeneratedValue(strategy = GenerationType.IDENTITY):
            Để MySQL tự tăng id (AUTO_INCREMENT): bài đầu id = 1, bài sau id = 2...
            Mình KHÔNG tự gán id. Bài mới tạo bằng new Song() có id = null,
            sau khi save() xong Hibernate mới điền id do MySQL sinh ra.

        Dùng Long (không phải long) vì bài chưa lưu cần id = null
        (long nguyên thủy không chứa được null, mặc định là 0 → dễ nhầm với id thật).
    */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
        ---------- ARTIST CHÍNH (người đăng bài) ----------

        Quan hệ NHIỀU - MỘT (@ManyToOne):
        - Nhiều bài hát thuộc về 1 Artist.
        - Trong database: bảng songs có cột artist_id trỏ tới users.id (khóa ngoại).
        - Trong Java: song.getArtist() trả về luôn object User, không phải con số id.

        @JoinColumn(name = "artist_id", nullable = false):
        - Tên cột khóa ngoại là artist_id.
        - nullable = false: bài hát BẮT BUỘC phải có chủ.

        fetch = FetchType.LAZY (tải lười):
        - Khi đọc 1 Song từ database, Hibernate CHƯA đọc User ngay.
        - Chỉ khi code gọi song.getArtist().getArtistName() thì mới chạy thêm câu
          SELECT ... FROM users WHERE id = ?.
        - Lợi ích: nhiều chỗ chỉ cần thông tin bài, không cần thông tin Artist
          → không tốn thêm truy vấn.
        - Ngược lại EAGER (tải ngay): luôn đọc kèm User dù không dùng tới.
        - Lưu ý: tải lười chỉ hoạt động khi kết nối database còn mở.
          Project đang để spring.jpa.open-in-view = true (mặc định)
          → kết nối mở suốt request → SongMapper trong Controller vẫn đọc được artist.
          Đừng tắt cài đặt này nếu chưa sửa lại cách tải dữ liệu.

        Quyền: chỉ Artist chính được sửa / ẩn / xóa / đổi lịch bài.
        Không có cascade: xóa bài KHÔNG xóa User, lưu bài KHÔNG lưu User.
    */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = false)
    private User artist;

    /*
        ---------- CÁC ARTIST FEAT ----------

        Quan hệ NHIỀU - NHIỀU (@ManyToMany):
        - 1 bài hát có thể có nhiều Artist feat (tối đa 5, kiểm tra trong Service).
        - 1 Artist có thể được feat trong nhiều bài hát.

        Database KHÔNG lưu được danh sách trong 1 ô, nên cần BẢNG TRUNG GIAN:

            songs                song_featured_artists          users
            ┌────┬──────────┐    ┌─────────┬───────────┐    ┌────┬───────────┐
            │ id │ title    │    │ song_id │ artist_id │    │ id │ artist_name│
            ├────┼──────────┤    ├─────────┼───────────┤    ├────┼───────────┤
            │ 15 │ Chiều Mưa│◄───│   15    │    12     │───►│ 12 │ Sóng Nhỏ  │
            │    │          │◄───│   15    │    13     │───►│ 13 │ Đom Đóm   │
            └────┴──────────┘    └─────────┴───────────┘    └────┴───────────┘
            → Bài 15 feat 2 người: id 12 và id 13.

        @JoinTable(...):
        - name = "song_featured_artists"       : tên bảng trung gian (Hibernate tự tạo).
        - joinColumns = song_id                : cột trỏ về bảng songs (phía class này).
        - inverseJoinColumns = artist_id       : cột trỏ về bảng users (phía bên kia).

        Thao tác trong Java rất đơn giản:
            song.setFeaturedArtists(Set.of(user12, user13));
            songRepository.save(song);
        → Hibernate tự xóa các dòng cũ của bài 15 trong bảng trung gian
          rồi chèn các dòng mới. Không cần viết SQL nào.

        Dùng Set (không phải List):
        - Set không chứa phần tử trùng → không thể feat 1 người 2 lần.
        - (Service cũng đã loại id trùng trước khi tải User, Set là lớp bảo vệ thứ hai.)

        = new HashSet<>(): giá trị mặc định là tập rỗng thay vì null
        → bài không feat ai vẫn gọi song.getFeaturedArtists() an toàn, không NullPointerException.

        Artist feat chỉ được ghi tên, KHÔNG có quyền quản lý bài.
        Artist feat bị khóa → SongMapper tự ẩn tên họ, bài hát vẫn hiển thị bình thường.
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

        @Column(nullable = false, length = 150):
        - nullable = false → cột NOT NULL trong database.
        - length = 150     → kiểu VARCHAR(150).
        DTO (UploadSongRequest, UpdateSongRequest) đã chặn @Size(max = 150) trước
        → lỗi được báo rõ ràng từ sớm, không đợi tới lúc database từ chối.
    */
    @Column(nullable = false, length = 150)
    private String title;

    /*
        Đường dẫn file nhạc, VD "/uploads/songs/audio/3f2a9c1e-....mp3".

        Database chỉ lưu ĐƯỜNG DẪN, file thật nằm trên ổ đĩa server.
        Vì sao không lưu file vào database?
        - File nhạc nặng (tới 100MB) → database phình to, sao lưu chậm, truy vấn chậm.
        - Phục vụ file từ ổ đĩa nhanh hơn và hỗ trợ tua nhạc (HTTP 206) dễ dàng.

        Giới hạn: .mp3 tối đa 20MB, .wav tối đa 100MB (kiểm tra trong FileStorageService).
    */
    @Column(name = "audio_url", nullable = false, length = 255)
    private String audioUrl;

    /*
        Đường dẫn ảnh bìa. KHÔNG bắt buộc (không có nullable = false → cho phép NULL).
        JPG / PNG / WEBP, tối đa 2MB.
    */
    @Column(name = "cover_url", length = 255)
    private String coverUrl;

    /*
        Thời lượng bài hát (giây), VD 215 = 3 phút 35 giây.
        Backend TỰ ĐỌC từ file nhạc khi tải lên (FileStorageService.getAudioDuration),
        Artist không tự nhập → không thể khai sai.
    */
    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds;

    /*
        Lượt nghe, mặc định 0.

        updatable = false: Hibernate KHÔNG đưa cột này vào câu UPDATE khi save(song).
        → Chỉ câu incrementPlayCount trong SongRepository
          (UPDATE songs SET play_count = play_count + 1 WHERE id = ?) mới đổi được lượt nghe.

        VÌ SAO? Chống RACE CONDITION "ghi đè mất lượt nghe":
            t1  Artist mở trang sửa bài: đọc play_count = 100
            t2  5 người nghe → database tăng lên 105
            t3  Artist bấm lưu tên mới → nếu save() ghi cả play_count = 100
                → MẤT 5 lượt nghe!
        Có updatable = false → câu UPDATE của Artist không đụng tới play_count → không mất.

        Lúc INSERT bài mới vẫn ghi giá trị 0 như bình thường (insertable mặc định = true).

        Không có setter cho trường này: gọi setPlayCount rồi save() cũng KHÔNG có tác dụng,
        nên bỏ hẳn setter để không ai dùng nhầm.
    */
    @Column(name = "play_count", nullable = false, updatable = false)
    private Long playCount = 0L;

    /*
        Trạng thái bài hát, mặc định PENDING (chờ Admin duyệt).

        @Enumerated(EnumType.STRING): lưu TÊN enum vào database, VD chữ "PENDING".
        Nếu dùng EnumType.ORDINAL (mặc định khi không ghi), sẽ lưu SỐ THỨ TỰ:
            PENDING = 0, APPROVED = 1, REJECTED = 2, HIDDEN = 3, DELETED = 4
        Nguy hiểm: lỡ chèn thêm giá trị mới vào giữa enum (VD SCHEDULED sau PENDING)
        → mọi số thứ tự phía sau bị lệch → bài APPROVED (1) bỗng thành SCHEDULED (1).
        STRING an toàn hơn nhiều, nhìn database cũng dễ hiểu.

        length = 20 → VARCHAR(20), đủ chứa tên enum dài nhất.
    */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SongStatus status = SongStatus.PENDING;

    /*
        Thời điểm phát hành (giờ Việt Nam):

        - Đang chờ duyệt, releaseAt = NULL    → phát hành ngay sau khi Admin duyệt.
        - Đang chờ duyệt, releaseAt có giá trị → lịch phát hành Artist đã hẹn.
        - Khi Admin duyệt: nếu đang NULL → backend gán = thời điểm duyệt.
        → Mọi bài APPROVED đều có releaseAt (quy tắc quan trọng: truy vấn người nghe
          dùng "release_at <= now", bài APPROVED mà releaseAt NULL sẽ không bao giờ hiện).

        Không có nullable = false → cho phép NULL (cần cho trường hợp "phát hành ngay").

        Bài hẹn 20h tự xuất hiện lúc 20h mà không cần tác vụ hẹn giờ nào,
        vì truy vấn người nghe so release_at với thời gian hiện tại MỖI LẦN chạy.
    */
    @Column(name = "release_at")
    private LocalDateTime releaseAt;

    /*
        Lý do bị từ chối (UC35) hoặc bị gỡ (UC36). Tối đa 255 ký tự.
        - Có giá trị khi status = REJECTED.
        - Bị xóa (về NULL) khi Artist sửa bài để gửi duyệt lại, hoặc khi Admin duyệt thành công.
    */
    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    /*
        Thời điểm tải lên.
        updatable = false: chỉ ghi lúc INSERT, câu UPDATE không bao giờ đụng tới
        → ngày tạo không thể bị sửa, kể cả khi code lỡ gán giá trị mới.
        Giá trị do hàm onCreate() bên dưới tự gán.
    */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /*
        Thời điểm sửa gần nhất, do onCreate() / onUpdate() tự gán.

        Tăng lượt nghe KHÔNG làm thay đổi cột này, vì incrementPlayCount là câu UPDATE
        viết thẳng (bulk update), không đi qua cơ chế entity của Hibernate
        → @PreUpdate không được gọi. Đúng ý: có người nghe không có nghĩa là bài bị "sửa".
    */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /*
        ---------- OPTIMISTIC LOCKING: CHỐNG "LOST UPDATE" ----------

        VẤN ĐỀ: 2 người sửa CÙNG 1 bài CÙNG LÚC.
            t1  Admin đọc bài 15: status = PENDING
            t2  Artist đọc bài 15: status = PENDING
            t3  Artist xóa bài: ghi status = DELETED, xóa file nhạc trên ổ đĩa
            t4  Admin duyệt:    ghi status = APPROVED   ← GHI ĐÈ!
            → Bài đã xóa "sống lại", hiện cho người nghe nhưng file nhạc đã mất.

        GIẢI PHÁP: @Version. Mỗi bài có 1 con số "phiên bản".
            t1  Admin đọc:  version = 3
            t2  Artist đọc: version = 3
            t3  Artist ghi: UPDATE ... SET ..., version = 4 WHERE id = 15 AND version = 3
                            → khớp 1 dòng ✓
            t4  Admin ghi:  UPDATE ... SET ..., version = 4 WHERE id = 15 AND version = 3
                            → version trong database đã là 4 → KHÔNG KHỚP dòng nào ✗
                            → Hibernate ném ObjectOptimisticLockingFailureException
                            → GlobalExceptionHandler trả 409:
                              "This data was changed by someone else, please reload and try again"

        Hibernate tự làm hết: tự thêm "AND version = ?" vào mọi câu UPDATE,
        tự tăng version sau mỗi lần ghi thành công. Mình không viết thêm code nào.

        Gọi là "optimistic" (lạc quan) vì giả định xung đột HIẾM khi xảy ra:
        không khóa bài khi đọc, chỉ kiểm tra lúc ghi. Khác "pessimistic" (bi quan):
        khóa bài ngay khi đọc, người khác phải đợi → an toàn hơn nhưng chậm hơn nhiều.

        Lưu ý: incrementPlayCount (bulk update) KHÔNG tăng version
        → người nghe không gây xung đột với Admin / Artist. Đúng ý.

        Dùng long (nguyên thủy) thay vì Long:
        - Không bao giờ null, bài mới bắt đầu từ 0.
        - Spring Data dùng version để đoán "bài mới hay bài cũ" khi save():
          nếu version là Long và đang null → coi là bài mới → INSERT (sai với bài cũ).
          Dùng long → Spring Data chuyển sang dựa vào id → luôn đoán đúng.

        Không có setter: chỉ Hibernate được đổi version.
        Kiểm chứng bằng test: SongOptimisticLockTest.
    */
    @Version
    @Column(nullable = false)
    private long version;

    /*
        ---------- LIFECYCLE CALLBACK: HÀM HIBERNATE TỰ GỌI ----------

        @PrePersist: Hibernate tự gọi ngay TRƯỚC KHI INSERT bài hát mới.
        → Gán createdAt và updatedAt = thời điểm hiện tại.
        → Service không phải nhớ gán ngày tạo mỗi khi tạo bài.

        protected: chỉ Hibernate (và class con) gọi, code bên ngoài không gọi nhầm.
    */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /*
        @PreUpdate: Hibernate tự gọi ngay TRƯỚC KHI UPDATE bài hát (qua save()).
        → Gán updatedAt = thời điểm hiện tại.
    */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /*
        ---------- GETTER / SETTER ----------

        Chỉ có GETTER (không có setter) cho:
            id, playCount, createdAt, updatedAt, version
        → Những trường này do database / Hibernate tự quản lý, code không được tự đổi.
        Các trường còn lại có setter để Service cập nhật.

        Hibernate đọc / ghi trực tiếp vào thuộc tính (vì @Id đặt trên thuộc tính),
        nên không cần setter mới nạp được dữ liệu từ database.
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