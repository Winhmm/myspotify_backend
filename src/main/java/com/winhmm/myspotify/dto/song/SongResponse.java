package com.winhmm.myspotify.dto.song;

import java.time.LocalDateTime;
import java.util.List;

/*
    =====================================================================
    SongResponse - THÔNG TIN BÀI HÁT CHO NGƯỜI NGHE (UC20 - UC25)
    =====================================================================

    Dùng ở: mọi API trong SongController (danh sách, tìm kiếm, chi tiết, trang nghệ sĩ)
    và phần "song" trong ListeningHistoryResponse.

    So với SongManageResponse (bản cho Artist, Admin), bản này không có:
        status, released, rejectReason, createdAt, updatedAt.
    Vì sao?
    - Người nghe chỉ thấy bài đã phát hành → status luôn là APPROVED, gửi đi là thừa.
    - rejectReason, ngày tạo, ngày sửa là thông tin quản lý nội bộ, không nên lộ ra.
    → Nguyên tắc: mỗi loại người dùng có 1 DTO riêng, chỉ chứa đúng những gì họ cần.

    JSON:
    {
        "id": 15,
        "title": "Chiều Mưa",
        "audioUrl": "/uploads/songs/audio/3f2a9c1e.mp3",
        "coverUrl": "/uploads/songs/covers/9b1c4d7f.jpg",
        "durationSeconds": 215,
        "playCount": 1024,
        "releaseAt": "2026-10-12T16:30:00",
        "artistId": 5,
        "artistName": "Mây Chiều",
        "featuredArtists": [ { "id": 12, "artistName": "Sóng Nhỏ" } ]
    }

    LocalDateTime → JSON: Spring Boot tự chuyển sang chuỗi chuẩn ISO "2026-10-12T16:30:00"
    (không phải dạng số như 1760261400000), frontend dùng new Date(...) đọc được ngay.

    =====================================================================
    BUILDER PATTERN - VÌ SAO DÙNG?
    =====================================================================

    Vấn đề: class có 10 trường. Nếu dùng constructor thường:
        new SongResponse(15L, "Chiều Mưa", "/uploads/...", "/uploads/...", 215, 1024L,
                         time, 5L, "Mây Chiều", list);
    → Đọc vào không biết số 215 là gì, 1024L là gì.
    → Truyền nhầm thứ tự 2 tham số cùng kiểu (VD artistId và playCount đều là Long)
      → trình biên dịch không báo lỗi, dữ liệu sai âm thầm.

    Giải pháp: Builder, gọi tên từng trường khi gán:
        SongResponse response = SongResponse.builder()
                .id(15L)
                .title("Chiều Mưa")
                .durationSeconds(215)
                .playCount(1024L)
                .artistId(5L)
                .build();
    → Đọc là hiểu ngay, không thể nhầm thứ tự, trường nào chưa cần thì bỏ qua.

    Cách hoạt động:
    1. SongResponse.builder() → tạo 1 đối tượng Builder rỗng.
    2. .id(15L) → gán vào Builder, trả về chính Builder đó (return this)
       → nên gọi nối tiếp .title(...).durationSeconds(...) được (gọi là "method chaining").
    3. .build() → lấy hết giá trị trong Builder, tạo ra SongResponse thật.

    Builder được dùng duy nhất trong SongMapper.toSongResponse(song).
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