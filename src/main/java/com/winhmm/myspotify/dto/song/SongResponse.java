package com.winhmm.myspotify.dto.song;

import java.time.LocalDateTime;
import java.util.List;

/*
    Thông tin bài hát trả về cho người nghe (UC20 - UC25).

    Không có status, rejectReason... vì người nghe không cần biết.
*/
public class SongResponse {
    private Long id;
    private String title;
    private String audioUrl;
    private String coverUrl;
    private Integer durationSeconds;
    private Long playCount;
    private LocalDateTime releaseAt;
    private Long artistId;
    private String artistName;
    private List<ArtistSummary> featuredArtists;

    /*
        Constructor private → bên ngoài không gọi được new SongResponse(...),
        bắt buộc phải tạo qua Builder.
    */
    private SongResponse(Builder builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.audioUrl = builder.audioUrl;
        this.coverUrl = builder.coverUrl;
        this.durationSeconds = builder.durationSeconds;
        this.playCount = builder.playCount;
        this.releaseAt = builder.releaseAt;
        this.artistId = builder.artistId;
        this.artistName = builder.artistName;
        this.featuredArtists = builder.featuredArtists;
    }

    /*
        Điểm bắt đầu tạo đối tượng: SongResponse.builder()
    */
    public static Builder builder() {
        return new Builder();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getAudioUrl() { return audioUrl; }
    public String getCoverUrl() { return coverUrl; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public Long getPlayCount() { return playCount; }
    public LocalDateTime getReleaseAt() { return releaseAt; }
    public Long getArtistId() { return artistId; }
    public String getArtistName() { return artistName; }
    public List<ArtistSummary> getFeaturedArtists() { return featuredArtists; }

    /*
        Builder: gom từng giá trị, cuối cùng gọi build() để tạo SongResponse.
        Mỗi method gán xong trả về chính Builder (return this)
        → gọi nối tiếp được: .id(...).title(...).build()
    */
    public static class Builder {
        private Long id;
        private String title;
        private String audioUrl;
        private String coverUrl;
        private Integer durationSeconds;
        private Long playCount;
        private LocalDateTime releaseAt;
        private Long artistId;
        private String artistName;
        private List<ArtistSummary> featuredArtists;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder audioUrl(String audioUrl) { this.audioUrl = audioUrl; return this; }
        public Builder coverUrl(String coverUrl) { this.coverUrl = coverUrl; return this; }
        public Builder durationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; return this; }
        public Builder playCount(Long playCount) { this.playCount = playCount; return this; }
        public Builder releaseAt(LocalDateTime releaseAt) { this.releaseAt = releaseAt; return this; }
        public Builder artistId(Long artistId) { this.artistId = artistId; return this; }
        public Builder artistName(String artistName) { this.artistName = artistName; return this; }
        public Builder featuredArtists(List<ArtistSummary> featuredArtists) { this.featuredArtists = featuredArtists; return this; }

        public SongResponse build() {
            return new SongResponse(this);
        }
    }
}
