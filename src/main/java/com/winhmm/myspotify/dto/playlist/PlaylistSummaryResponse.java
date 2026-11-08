package com.winhmm.myspotify.dto.playlist;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    - LocalDateTime: kiểu ngày giờ, dùng cho createdAt, updatedAt.
*/
import java.time.LocalDateTime;

/*
    =====================================================================
    PLAYLISTSUMMARYRESPONSE - 1 PLAYLIST TRONG DANH SÁCH (KHÔNG KÈM BÀI HÁT)
    =====================================================================

    Dùng ở:
    - GET /api/playlists/me              : playlist của tôi
    - GET /api/playlists/users/{userId}  : playlist công khai của 1 người

    Chỉ có thông tin tóm tắt để hiện 1 ô playlist (ảnh, tên, người tạo, số bài).
    Muốn xem danh sách bài → frontend gọi GET /api/playlists/{id} (PlaylistDetailResponse).
    Vì sao tách 2 DTO? Danh sách 50 playlist mà kèm cả bài hát của từng playlist
    thì JSON rất nặng, trong khi màn hình danh sách không cần tới bài hát.

    Vì sao không trả thẳng entity Playlist về frontend?
    - Entity có owner là cả object User (có email, mật khẩu đã băm, trạng thái tài khoản...)
      → trả thẳng là lộ thông tin của người tạo.
    - Một số giá trị frontend cần không nằm trong entity (songCount, ảnh bìa dự phòng).
    → Tự chọn đúng những trường cần trả trong DTO này.

    JSON:
    {
        "id": 3,
        "name": "Nhạc chill cuối tuần",
        "coverUrl": "/uploads/playlists/7c1e2b9a.jpg",
        "isPublic": true,
        "ownerId": 6,
        "ownerName": "winhmm",
        "songCount": 12,
        "createdAt": "2026-11-04T08:00:00",
        "updatedAt": "2026-11-05T21:10:00"
    }

    -----------------------------------------------------
    Ý nghĩa từng trường
    -----------------------------------------------------
    - id        : id playlist.
    - name      : tên playlist.
    - coverUrl  : ảnh bìa đã tính sẵn: ảnh riêng, không có thì ảnh bài đầu tiên,
                  vẫn không có thì null → frontend hiện ảnh mặc định.
    - isPublic  : true = công khai, false = riêng tư.
    - ownerId   : id người tạo (frontend dùng để mở trang cá nhân người đó).
    - ownerName : tên tài khoản (username) người tạo.
    - songCount : số bài đang phát hành (bài bị ẩn, bị gỡ không tính).
    - createdAt : lúc tạo playlist.
    - updatedAt : lần thay đổi gần nhất (sửa thông tin, đổi ảnh, thêm hoặc bỏ bài).

    -----------------------------------------------------
    Vì sao getter là getIsPublic, không phải isPublic?
    -----------------------------------------------------
    Jackson đặt tên trường JSON theo tên getter:
        getIsPublic() → "isPublic"     (đúng ý)
        isPublic()    → "public"       (mất chữ "is", frontend dễ nhầm)

    =====================================================================
    BUILDER LÀ GÌ?
    =====================================================================

    Class có 9 trường. Nếu dùng constructor thường:
        new PlaylistSummaryResponse(3L, "Chill", null, true, 6L, "winhmm", 12, t1, t2)
    → nhìn vào không biết số 12 là gì, true là gì, đổi chỗ 2 tham số cùng kiểu cũng không ai phát hiện.

    Dùng Builder, mỗi giá trị đi kèm tên của nó:
        PlaylistSummaryResponse.builder()
                .id(3L)
                .name("Chill")
                .songCount(12)
                ...
                .build();
    → đọc là hiểu, thứ tự gọi không quan trọng.
    Cách làm giống SongResponse (module Song).
*/
public class PlaylistSummaryResponse {
    private Long id;
    private String name;
    private String coverUrl;
    private boolean isPublic;
    private Long ownerId;
    private String ownerName;
    private long songCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /*
        Constructor private: bên ngoài không gọi new PlaylistSummaryResponse(...) được,
        bắt buộc tạo qua PlaylistSummaryResponse.builder()...build().
        Constructor nhận vào 1 Builder và chép từng giá trị Builder đang giữ sang object thật.
    */
    private PlaylistSummaryResponse(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.coverUrl = builder.coverUrl;
        this.isPublic = builder.isPublic;
        this.ownerId = builder.ownerId;
        this.ownerName = builder.ownerName;
        this.songCount = builder.songCount;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
    }

    /*
        builder(): hàm static (gọi thẳng bằng tên class, không cần object có sẵn)
        → PlaylistSummaryResponse.builder() trả về 1 Builder mới, rỗng.
    */
    public static Builder builder() {
        return new Builder();
    }

    /*
        Chỉ có getter (để Jackson đổi ra JSON), không có setter: tạo xong là cố định.
    */
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCoverUrl() { return coverUrl; }
    public boolean getIsPublic() { return isPublic; }
    public Long getOwnerId() { return ownerId; }
    public String getOwnerName() { return ownerName; }
    public long getSongCount() { return songCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    /*
        -----------------------------------------------------
        Builder: "người lắp ráp" PlaylistSummaryResponse
        -----------------------------------------------------
        Builder giữ tạm các giá trị trong lúc lắp ráp.
        Mỗi hàm gán 1 trường rồi trả lại chính Builder (return this) → gọi nối tiếp được:
            builder().id(3L).name("Chill")...
        build() tạo ra object PlaylistSummaryResponse thật (gọi constructor private ở trên).

        static class: class lồng bên trong, tạo được mà không cần có PlaylistSummaryResponse trước.
        Vì nằm bên trong nên Builder gọi được constructor private của PlaylistSummaryResponse.
    */
    public static class Builder {
        private Long id;
        private String name;
        private String coverUrl;
        private boolean isPublic;
        private Long ownerId;
        private String ownerName;
        private long songCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder coverUrl(String coverUrl) { this.coverUrl = coverUrl; return this; }
        public Builder isPublic(boolean isPublic) { this.isPublic = isPublic; return this; }
        public Builder ownerId(Long ownerId) { this.ownerId = ownerId; return this; }
        public Builder ownerName(String ownerName) { this.ownerName = ownerName; return this; }
        public Builder songCount(long songCount) { this.songCount = songCount; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public PlaylistSummaryResponse build() {
            return new PlaylistSummaryResponse(this);
        }
    }
}