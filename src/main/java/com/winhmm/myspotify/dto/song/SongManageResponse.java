package com.winhmm.myspotify.dto.song;

import com.winhmm.myspotify.enums.SongStatus;

import java.time.LocalDateTime;
import java.util.List;

/*
    Thông tin bài hát ĐẦY ĐỦ, trả về cho ARTIST (UC28) và ADMIN (UC34).

    Frontend dựa vào status + released để hiển thị:
    - PENDING                    → "Chờ duyệt"
    - APPROVED + released = false  → "Đã lên lịch"
    - APPROVED + released = true   → "Đã phát hành"
    - REJECTED                   → "Bị từ chối" (kèm rejectReason)
    - HIDDEN                     → "Đã ẩn"

    Tạo bằng Builder:
        SongManageResponse response = SongManageResponse.builder()
                .id(1L)
                .status(SongStatus.PENDING)
                .build();
*/
public class SongManageResponse {
    private Long id;
    private String title;
    private String audioUrl;
    private String coverUrl;
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
