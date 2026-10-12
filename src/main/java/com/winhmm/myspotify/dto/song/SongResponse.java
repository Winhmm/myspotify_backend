package com.winhmm.myspotify.dto.song;

import java.time.LocalDateTime;
import java.util.List;

/*
    ===================================================================
    SongResponse - Thông tin bài hát cho NGƯỜI NGHE (UC20 - UC25).
    ===================================================================

    DÙNG Ở: mọi API trong SongController (danh sách, tìm kiếm, chi tiết, trang nghệ sĩ).

    So với SongManageResponse (bản cho Artist / Admin), bản này KHÔNG CÓ:
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

    ===================================================================
    BUILDER PATTERN - VÌ SAO DÙNG?
    ===================================================================

    VẤN ĐỀ: class có 10 trường. Nếu dùng constructor thường:
        new SongResponse(15L, "Chiều Mưa", "/uploads/...", "/uploads/...", 215, 1024L,
                         time, 5L, "Mây Chiều", list);
    → Đọc vào không biết số 215 là gì, 1024L là gì.
    → Truyền nhầm thứ tự 2 tham số cùng kiểu (VD artistId và playCount đều là Long)
      → compiler KHÔNG báo lỗi, dữ liệu sai âm thầm.

    GIẢI PHÁP: Builder, gọi tên từng trường khi gán:
        SongResponse response = SongResponse.builder()
                .id(15L)
                .title("Chiều Mưa")
                .durationSeconds(215)
                .playCount(1024L)
                .artistId(5L)
                .build();
    → Đọc là hiểu ngay, không thể nhầm thứ tự, trường nào chưa cần thì bỏ qua.

    CÁCH HOẠT ĐỘNG:
    1. SongResponse.builder() → tạo 1 đối tượng Builder rỗng.
    2. .id(15L) → gán vào Builder, trả về CHÍNH Builder đó (return this)
       → nên gọi nối tiếp .title(...).durationSeconds(...) được (gọi là "method chaining").
    3. .build() → lấy hết giá trị trong Builder, tạo ra SongResponse thật.

    Builder được dùng duy nhất trong SongMapper.toSongResponse(song).
*/
public class SongResponse {
    /*
        id bài hát (= songs.id). Dùng cho URL chi tiết /api/songs/{id} và API /play.
    */
    private Long id;

    /*
        Tên bài hát.
    */
    private String title;

    /*
        Đường dẫn file nhạc, VD "/uploads/songs/audio/3f2a9c1e.mp3".
        Frontend ghép với địa chỉ server rồi đặt vào thẻ <audio src="...">.
        Tên file là UUID ngẫu nhiên (không phải tên gốc) → không đoán được, không trùng.
    */
    private String audioUrl;

    /*
        Đường dẫn ảnh bìa. null → bài không có ảnh bìa → frontend hiển thị ảnh mặc định.
    */
    private String coverUrl;

    /*
        Thời lượng tính bằng GIÂY, VD 215 → frontend hiển thị "3:35".
        Backend tự đọc từ file nhạc khi tải lên, Artist không tự nhập (tránh nhập sai).
        Dùng Integer (không phải int) cho khớp kiểu trong entity Song.
    */
    private Integer durationSeconds;

    /*
        Tổng lượt nghe.
    */
    private Long playCount;

    /*
        Thời điểm phát hành. Frontend có thể hiển thị "Phát hành ngày 12/10/2026".
    */
    private LocalDateTime releaseAt;

    /*
        Artist CHÍNH (người đăng bài): id + nghệ danh.
        Trả 2 trường phẳng thay vì 1 object lồng → frontend đọc gọn: song.artistName.
    */
    private Long artistId;
    private String artistName;

    /*
        Danh sách nghệ sĩ feat (đã lọc bỏ người bị khóa, sắp xếp theo id).
        Không feat với ai → mảng rỗng [] (không phải null) → frontend không cần kiểm tra null.
    */
    private List<ArtistSummary> featuredArtists;

    /*
        Constructor PRIVATE → bên ngoài KHÔNG gọi được new SongResponse(...).
        Cách duy nhất để tạo SongResponse là qua Builder → luôn rõ ràng, không nhầm thứ tự.

        Builder là class lồng bên trong SongResponse nên vẫn gọi được constructor private này.
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
        static → gọi thẳng qua tên class, không cần có đối tượng SongResponse trước.
    */
    public static Builder builder() {
        return new Builder();
    }

    /*
        Getter: Jackson dùng để tạo JSON (getTitle() → "title").
        Không có setter → SongResponse tạo xong không sửa được (immutable).
    */
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
        Builder: "nháp" để gom từng giá trị trước khi tạo SongResponse thật.

        public static class:
        - static: Builder không cần 1 đối tượng SongResponse có sẵn mới tạo được
          (đúng ý: dùng Builder để TẠO ra SongResponse).
        - Nằm bên trong SongResponse → được truy cập constructor private ở trên.

        Mỗi method gán 1 trường rồi trả về chính Builder (return this)
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

        /*
            Bước cuối: tạo SongResponse thật từ các giá trị đã gom.
            Trường nào chưa gán → giữ giá trị mặc định null.
        */
        public SongResponse build() {
            return new SongResponse(this);
        }
    }
}