package com.winhmm.myspotify.dto.song;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    (Các khái niệm cơ bản: xem RegisterRequest. Thứ tự đọc package dto.song: xem UploadSongRequest.)

    - SongStatus   : enum trạng thái bài hát (PENDING, APPROVED, REJECTED, HIDDEN, DELETED).
    - LocalDateTime: kiểu ngày giờ của Java.
    - List         : danh sách, dùng cho featuredArtists.
*/
import com.winhmm.myspotify.enums.SongStatus;

import java.time.LocalDateTime;
import java.util.List;

/*
    =====================================================================
    SONGMANAGERESPONSE - THÔNG TIN BÀI HÁT ĐẦY ĐỦ CHO ARTIST VÀ ADMIN
    =====================================================================

    Dùng ở:
    - ArtistSongController: "Bài hát của tôi" (UC28) và kết quả sau mỗi thao tác
      (đăng, sửa, đổi ảnh, đổi video Canvas, đổi lịch, ẩn, hiện).
    - AdminSongController : danh sách bài (UC34), kết quả duyệt, từ chối, gỡ.

    Có thêm so với SongResponse (bản cho người nghe):
        status, released, rejectReason, createdAt, updatedAt.
    13 trường còn lại giống hệt SongResponse (ý nghĩa từng trường: xem SongResponse).

    -----------------------------------------------------
    5 trường thêm
    -----------------------------------------------------
    - status      : trạng thái bài, kiểu enum SongStatus.
                    Jackson tự chuyển enum thành chuỗi theo tên, VD SongStatus.REJECTED → "REJECTED".
    - released    : bài đã thật sự phát hành chưa (true hoặc false), backend tính sẵn.
    - rejectReason: lý do Admin từ chối hoặc gỡ bài, null nếu không bị từ chối.
    - createdAt   : thời điểm Artist đăng bài.
    - updatedAt   : thời điểm sửa gần nhất.

    =====================================================================
    FRONTEND HIỂN THỊ NHÃN TRẠNG THÁI
    =====================================================================

        status     released   → Nhãn hiển thị
        PENDING    false      → "Chờ duyệt"
        APPROVED   false      → "Đã lên lịch"   (đã duyệt, chưa tới giờ phát hành)
        APPROVED   true       → "Đã phát hành"
        REJECTED   false      → "Bị từ chối"    (kèm rejectReason)
        HIDDEN     false      → "Đã ẩn"
        DELETED    false      → "Đã xóa"        (chỉ Admin thấy, khi lọc status=DELETED)

    Vì sao cần thêm "released" khi đã có "status"?
    - Bài APPROVED có 2 tình trạng khác nhau: đã tới giờ phát hành hay chưa.
    - Backend tính sẵn (released = APPROVED và releaseAt <= giờ server, trong SongMapper)
      → frontend không phải tự so thời gian,
      tránh sai lệch do đồng hồ máy người dùng chạy sai.

    =====================================================================
    JSON
    =====================================================================

    {
        "id": 15,
        "title": "Chiều Mưa",
        "audioUrl": "/uploads/songs/audio/3f2a9c1e.mp3",
        "coverUrl": "/uploads/songs/covers/9b1c4d7f.jpg",
        "canvasUrl": "/uploads/songs/canvas/8d1f2a7c.mp4",
        "durationSeconds": 215,
        "playCount": 0,
        "status": "REJECTED",
        "released": false,
        "releaseAt": null,
        "rejectReason": "Ảnh bìa không phù hợp",
        "artistId": 5,
        "artistName": "Mây Chiều",
        "featuredArtists": [ { "id": 12, "artistName": "Sóng Nhỏ" } ],
        "genreId": 24,
        "genreName": "Chill",
        "createdAt": "2026-10-12T16:30:00",
        "updatedAt": "2026-10-12T17:05:00"
    }

    =====================================================================
    BUILDER
    =====================================================================

    Class có 18 trường → càng cần Builder hơn SongResponse
    (giải thích chi tiết Builder pattern xem trong SongResponse.java,
    cấu trúc code ở đây giống hệt, chỉ thêm 5 trường).

        SongManageResponse response = SongManageResponse.builder()
                .id(1L)
                .status(SongStatus.PENDING)
                .released(false)
                .build();

    Được dùng duy nhất trong SongMapper.toManageResponse(song).
*/
public class SongManageResponse {
    /*
        18 thuộc tính, đều private.

        released là boolean (chữ b thường, kiểu nguyên thủy): luôn là true hoặc false, không bao giờ null.
        Các trường còn lại dùng kiểu đối tượng (Long, Integer, String...) nên có thể null,
        VD releaseAt = null (chưa đặt lịch), rejectReason = null (không bị từ chối).
    */
    private Long id;
    private String title;
    private String audioUrl;
    private String coverUrl;
    private String canvasUrl;
    private Integer durationSeconds;
    private Long playCount;
    private SongStatus status;
    private boolean released;
    private LocalDateTime releaseAt;
    private String rejectReason;
    private Long artistId;
    private String artistName;
    private List<ArtistSummary> featuredArtists;
    private Long genreId;
    private String genreName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /*
        Constructor private: chỉ Builder gọi được, chép từng giá trị từ Builder sang
        (giải thích chi tiết: xem SongResponse).
    */
    private SongManageResponse(Builder builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.audioUrl = builder.audioUrl;
        this.coverUrl = builder.coverUrl;
        this.canvasUrl = builder.canvasUrl;
        this.durationSeconds = builder.durationSeconds;
        this.playCount = builder.playCount;
        this.status = builder.status;
        this.released = builder.released;
        this.releaseAt = builder.releaseAt;
        this.rejectReason = builder.rejectReason;
        this.artistId = builder.artistId;
        this.artistName = builder.artistName;
        this.featuredArtists = builder.featuredArtists;
        this.genreId = builder.genreId;
        this.genreName = builder.genreName;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
    }

    /*
        Điểm bắt đầu: SongManageResponse.builder() trả về 1 Builder rỗng (xem SongResponse).
    */
    public static Builder builder() {
        return new Builder();
    }

    /*
        -----------------------------------------------------
        Getter (Jackson gọi để tạo JSON)
        -----------------------------------------------------
        isReleased() thay vì getReleased():
        thuộc tính kiểu boolean nguyên thủy đặt tên getter là is + tên thuộc tính.
        Jackson bỏ chữ "is" → key "released" trong JSON (giống isSynced() trong LyricsResponse).
    */
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getAudioUrl() { return audioUrl; }
    public String getCoverUrl() { return coverUrl; }
    public String getCanvasUrl() { return canvasUrl; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public Long getPlayCount() { return playCount; }
    public SongStatus getStatus() { return status; }
    public boolean isReleased() { return released; }
    public LocalDateTime getReleaseAt() { return releaseAt; }
    public String getRejectReason() { return rejectReason; }
    public Long getArtistId() { return artistId; }
    public String getArtistName() { return artistName; }
    public List<ArtistSummary> getFeaturedArtists() { return featuredArtists; }
    public Long getGenreId() { return genreId; }
    public String getGenreName() { return genreName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    /*
        -----------------------------------------------------
        Class Builder lồng bên trong
        -----------------------------------------------------
        Giống hệt Builder của SongResponse:
        - 18 thuộc tính "nháp" giống SongManageResponse.
        - Mỗi hàm gán 1 giá trị rồi return this để gọi nối tiếp.
        - build() gọi new SongManageResponse(this) tạo đối tượng thật.
        Giải thích chi tiết từng dòng: xem SongResponse.
    */
    public static class Builder {
        private Long id;
        private String title;
        private String audioUrl;
        private String coverUrl;
        private String canvasUrl;
        private Integer durationSeconds;
        private Long playCount;
        private SongStatus status;
        private boolean released;
        private LocalDateTime releaseAt;
        private String rejectReason;
        private Long artistId;
        private String artistName;
        private List<ArtistSummary> featuredArtists;
        private Long genreId;
        private String genreName;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder audioUrl(String audioUrl) { this.audioUrl = audioUrl; return this; }
        public Builder coverUrl(String coverUrl) { this.coverUrl = coverUrl; return this; }
        public Builder canvasUrl(String canvasUrl) { this.canvasUrl = canvasUrl; return this; }
        public Builder durationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; return this; }
        public Builder playCount(Long playCount) { this.playCount = playCount; return this; }
        public Builder status(SongStatus status) { this.status = status; return this; }
        public Builder released(boolean released) { this.released = released; return this; }
        public Builder releaseAt(LocalDateTime releaseAt) { this.releaseAt = releaseAt; return this; }
        public Builder rejectReason(String rejectReason) { this.rejectReason = rejectReason; return this; }
        public Builder artistId(Long artistId) { this.artistId = artistId; return this; }
        public Builder artistName(String artistName) { this.artistName = artistName; return this; }
        public Builder featuredArtists(List<ArtistSummary> featuredArtists) { this.featuredArtists = featuredArtists; return this; }
        public Builder genreId(Long genreId) { this.genreId = genreId; return this; }
        public Builder genreName(String genreName) { this.genreName = genreName; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public SongManageResponse build() {
            return new SongManageResponse(this);
        }
    }
}