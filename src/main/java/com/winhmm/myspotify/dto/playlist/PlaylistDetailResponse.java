package com.winhmm.myspotify.dto.playlist;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    - LocalDateTime: kiểu ngày giờ, dùng cho createdAt, updatedAt.
    - List         : danh sách bài hát trong playlist.
*/
import java.time.LocalDateTime;
import java.util.List;

/*
    =====================================================================
    PLAYLISTDETAILRESPONSE - CHI TIẾT 1 PLAYLIST (KÈM DANH SÁCH BÀI)
    =====================================================================

    (Nên đọc PlaylistSummaryResponse trước: Builder, lý do getter tên getIsXxx đều giải thích ở đó.)

    Dùng ở:
    - GET    /api/playlists/{playlistId}                 : xem chi tiết
    - POST   /api/playlists                              : trả về playlist vừa tạo
    - PUT    /api/playlists/{playlistId}                 : trả về playlist sau khi sửa thông tin
    - PUT    /api/playlists/{playlistId}/cover           : sau khi đổi ảnh bìa
    - DELETE /api/playlists/{playlistId}/cover           : sau khi gỡ ảnh bìa
    - POST   /api/playlists/{playlistId}/songs/{songId}  : sau khi thêm bài
    - DELETE /api/playlists/{playlistId}/songs/{songId}  : sau khi bỏ bài
    → Sửa xong là frontend có ngay dữ liệu mới để vẽ lại màn hình, không phải gọi thêm API xem chi tiết.

    So với PlaylistSummaryResponse, có thêm:
        description, hasCustomCover, isOwner, songs.

    JSON:
    {
        "id": 3,
        "name": "Nhạc chill cuối tuần",
        "description": "Nghe lúc làm bài",
        "coverUrl": "/uploads/songs/covers/9b1c4d7f.jpg",
        "hasCustomCover": false,
        "isPublic": true,
        "ownerId": 6,
        "ownerName": "winhmm",
        "isOwner": true,
        "songCount": 2,
        "createdAt": "2026-11-04T08:00:00",
        "updatedAt": "2026-11-04T08:00:00",
        "songs": [
            { "song": { "id": 15, "title": "Chiều Mưa", ... }, "addedAt": "2026-11-04T08:00:00" },
            { "song": { "id": 22, "title": "Phố Đêm", ... },   "addedAt": "2026-11-04T08:00:00" }
        ]
    }

    -----------------------------------------------------
    Ý nghĩa các trường mới
    -----------------------------------------------------
    - description    : mô tả, null nếu không có.
    - hasCustomCover : true = playlist có ảnh bìa riêng.
                       false = coverUrl đang là ảnh của bài đầu tiên (hoặc null).
                       Frontend dùng để quyết định có hiện nút "Gỡ ảnh bìa" không.
    - isOwner        : người đang xem có phải chủ playlist không.
                       true → frontend hiện các nút sửa, xóa, thêm bài, bỏ bài.
                       (Frontend ẩn nút chỉ để giao diện gọn. Dù ai đó tự gọi API,
                        backend vẫn kiểm tra quyền chủ playlist trong PlaylistService.)
    - songs          : các bài đang phát hành, thêm trước đứng trước.
    - songCount      : số phần tử của songs.

    Các trường còn lại giống PlaylistSummaryResponse (xem giải thích bên đó,
    kể cả lý do getter tên getIsPublic, getIsOwner, getHasCustomCover).

    Có 13 trường nên dùng Builder.
*/
public class PlaylistDetailResponse {
    private Long id;
    private String name;
    private String description;
    private String coverUrl;
    private boolean hasCustomCover;
    private boolean isPublic;
    private Long ownerId;
    private String ownerName;
    private boolean isOwner;
    private long songCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PlaylistSongResponse> songs;

    /*
        Constructor private: chỉ Builder gọi được (giống PlaylistSummaryResponse).
    */
    private PlaylistDetailResponse(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.description = builder.description;
        this.coverUrl = builder.coverUrl;
        this.hasCustomCover = builder.hasCustomCover;
        this.isPublic = builder.isPublic;
        this.ownerId = builder.ownerId;
        this.ownerName = builder.ownerName;
        this.isOwner = builder.isOwner;
        this.songCount = builder.songCount;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.songs = builder.songs;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCoverUrl() { return coverUrl; }
    public boolean getHasCustomCover() { return hasCustomCover; }
    public boolean getIsPublic() { return isPublic; }
    public Long getOwnerId() { return ownerId; }
    public String getOwnerName() { return ownerName; }
    public boolean getIsOwner() { return isOwner; }
    public long getSongCount() { return songCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<PlaylistSongResponse> getSongs() { return songs; }

    /*
        Builder: cách dùng giống PlaylistSummaryResponse.Builder.
    */
    public static class Builder {
        private Long id;
        private String name;
        private String description;
        private String coverUrl;
        private boolean hasCustomCover;
        private boolean isPublic;
        private Long ownerId;
        private String ownerName;
        private boolean isOwner;
        private long songCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<PlaylistSongResponse> songs;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder coverUrl(String coverUrl) { this.coverUrl = coverUrl; return this; }
        public Builder hasCustomCover(boolean hasCustomCover) { this.hasCustomCover = hasCustomCover; return this; }
        public Builder isPublic(boolean isPublic) { this.isPublic = isPublic; return this; }
        public Builder ownerId(Long ownerId) { this.ownerId = ownerId; return this; }
        public Builder ownerName(String ownerName) { this.ownerName = ownerName; return this; }
        public Builder isOwner(boolean isOwner) { this.isOwner = isOwner; return this; }
        public Builder songCount(long songCount) { this.songCount = songCount; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder songs(List<PlaylistSongResponse> songs) { this.songs = songs; return this; }

        public PlaylistDetailResponse build() {
            return new PlaylistDetailResponse(this);
        }
    }
}