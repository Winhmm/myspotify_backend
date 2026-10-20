package com.winhmm.myspotify.dto.song;

import com.winhmm.myspotify.enums.SongStatus;

import java.time.LocalDateTime;
import java.util.List;

/*
    =====================================================================
    SongManageResponse - THÔNG TIN BÀI HÁT ĐẦY ĐỦ CHO ARTIST VÀ ADMIN
    =====================================================================

    Dùng ở:
    - ArtistSongController: "Bài hát của tôi" (UC28) và kết quả sau mỗi thao tác
      (đăng, sửa, đổi ảnh, đổi lịch, ẩn, hiện).
    - AdminSongController : danh sách bài (UC34), kết quả duyệt, từ chối, gỡ.

    Có thêm so với SongResponse (bản cho người nghe):
        status, released, rejectReason, createdAt, updatedAt.

    =====================================================================
    FRONTEND HIỂN THỊ NHÃN TRẠNG THÁI
    =====================================================================

        status     released   → Nhãn hiển thị
        PENDING    false      → "Chờ duyệt"
        APPROVED   false      → "Đã lên lịch"   (đã duyệt, chưa tới giờ phát hành)
        APPROVED   true       → "Đã phát hành"
        REJECTED   false      → "Bị từ chối"    (kèm rejectReason)
        HIDDEN     false      → "Đã ẩn"

    Vì sao cần thêm "released" khi đã có "status"?
    - Bài APPROVED có 2 tình trạng khác nhau: đã tới giờ phát hành hay chưa.
    - Backend tính sẵn (so releaseAt với giờ server) → frontend không phải tự so thời gian,
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
        "createdAt": "2026-10-12T16:30:00",
        "updatedAt": "2026-10-12T17:05:00"
    }

    Enum SongStatus → JSON: Jackson tự chuyển thành chuỗi tên enum, VD "REJECTED".

    =====================================================================
    BUILDER
    =====================================================================

    Class có 16 trường → càng cần Builder hơn SongResponse
    (giải thích chi tiết Builder Pattern xem trong SongResponse.java).

        SongManageResponse response = SongManageResponse.builder()
                .id(1L)
                .status(SongStatus.PENDING)
                .released(false)
                .build();

    Được dùng duy nhất trong SongMapper.toManageResponse(song).
*/
public class SongManageResponse {
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

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
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

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
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public SongManageResponse build() {
            return new SongManageResponse(this);
        }
    }
}