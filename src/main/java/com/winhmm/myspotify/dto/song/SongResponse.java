package com.winhmm.myspotify.dto.song;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    (Các khái niệm cơ bản: xem RegisterRequest. Thứ tự đọc package dto.song: xem UploadSongRequest.)

    - LocalDateTime: kiểu ngày giờ của Java, dùng cho releaseAt.
    - List         : danh sách, dùng cho featuredArtists.
    ArtistSummary nằm cùng package dto.song nên không cần import.
*/
import java.time.LocalDateTime;
import java.util.List;

/*
    =====================================================================
    SONGRESPONSE - THÔNG TIN BÀI HÁT CHO NGƯỜI NGHE (UC20 - UC25)
    =====================================================================

    Dùng ở: mọi API trong SongController (danh sách, tìm kiếm, chi tiết, trang nghệ sĩ),
    3 API gợi ý trong RecommendationController,
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
        "canvasUrl": "/uploads/songs/canvas/8d1f2a7c.mp4",
        "durationSeconds": 215,
        "playCount": 1024,
        "releaseAt": "2026-10-12T16:30:00",
        "artistId": 5,
        "artistName": "Mây Chiều",
        "featuredArtists": [ { "id": 12, "artistName": "Sóng Nhỏ" } ],
        "genreId": 24,
        "genreName": "Chill"
    }

    -----------------------------------------------------
    Ý nghĩa từng trường
    -----------------------------------------------------
    - id             : id bài hát.
    - title          : tên bài.
    - audioUrl       : đường dẫn file nhạc. Là đường dẫn tương đối, frontend ghép thêm địa chỉ server:
                       "http://157.66.100.87:8080" + audioUrl → đặt vào thẻ <audio>.
    - coverUrl       : đường dẫn ảnh bìa, null nếu Artist chưa có ảnh → frontend hiện ảnh mặc định.
    - canvasUrl      : video lặp (Canvas) của bài, null nếu Artist chưa đăng
                       → frontend hiện ảnh bìa (coverUrl) thay thế.
    - durationSeconds: thời lượng, đơn vị giây (215 → frontend hiển thị "3:35").
    - playCount      : tổng lượt nghe.
    - releaseAt      : thời điểm phát hành.
    - artistId, artistName: Artist chính (người đăng bài).
    - featuredArtists: danh sách nghệ sĩ feat (mỗi phần tử là 1 ArtistSummary).
                       Nghệ sĩ feat bị khóa tài khoản thì tự bị ẩn khỏi danh sách.
                       Rỗng [] → frontend không hiện chữ "ft.".
    - genreId, genreName: thể loại của bài (chọn lúc đăng bài, không đổi được).

    Vì sao để phẳng artistId, artistName, genreId, genreName thay vì lồng object
    { "artist": { "id": 5, "name": "..." } }?
    Frontend đọc thẳng song.artistName, song.genreName, ngắn gọn hơn song.artist.name.

    Vì sao kiểu Integer, Long (chữ hoa) mà không phải int, long?
    Giữ đúng kiểu của entity Song, và kiểu đối tượng có thể null (VD bài cũ chưa có thể loại → genreId = null).

    LocalDateTime → JSON: Spring Boot tự chuyển sang chuỗi chuẩn ISO "2026-10-12T16:30:00"
    (không phải dạng số như 1760261400000), frontend dùng new Date(...) đọc được ngay.

    =====================================================================
    BUILDER PATTERN - VÌ SAO DÙNG?
    =====================================================================

    Vấn đề: class có 13 trường. Nếu dùng constructor thường:
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
    → Đọc là hiểu ngay, không thể nhầm thứ tự, trường nào chưa cần thì bỏ qua (giữ giá trị null).

    Cách hoạt động:
    1. SongResponse.builder() → tạo 1 đối tượng Builder rỗng.
    2. .id(15L) → gán vào Builder, trả về chính Builder đó (return this)
       → nên gọi nối tiếp .title(...).durationSeconds(...) được (gọi là "method chaining").
    3. .build() → lấy hết giá trị trong Builder, tạo ra SongResponse thật.

    Giống đi mua bánh mì: người bán (Builder) ghi lần lượt từng món bạn gọi
    ("thêm pate", "thêm chả", "không hành"), gọi xong mới làm ra 1 ổ bánh hoàn chỉnh (build).

    Builder được dùng duy nhất trong SongMapper.toSongResponse(song).
    Project tự viết Builder bằng tay (không dùng thư viện Lombok) để thấy rõ từng bước hoạt động.
*/
public class SongResponse {
    /*
        13 thuộc tính, đều private. Ý nghĩa từng trường: xem phần "Ý nghĩa từng trường" ở trên.
    */
    private Long id;
    private String title;
    private String audioUrl;
    private String coverUrl;
    private String canvasUrl;
    private Integer durationSeconds;
    private Long playCount;
    private LocalDateTime releaseAt;
    private Long artistId;
    private String artistName;
    private List<ArtistSummary> featuredArtists;
    private Long genreId;
    private String genreName;

    /*
        -----------------------------------------------------
        Constructor private, nhận vào 1 Builder
        -----------------------------------------------------
        - private: code bên ngoài class không gọi được new SongResponse(...).
          → Cách duy nhất để tạo SongResponse là đi qua Builder (hàm build() bên dưới).
          Class Builder nằm bên trong SongResponse nên vẫn gọi được constructor private này.
        - Tham số builder: Builder đã được gán đủ giá trị.
        - Mỗi dòng chép 1 giá trị từ Builder sang SongResponse:
            this.id = builder.id;
            this.id   : thuộc tính của SongResponse đang được tạo.
            builder.id: thuộc tính cùng tên bên trong Builder.
    */
    private SongResponse(Builder builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.audioUrl = builder.audioUrl;
        this.coverUrl = builder.coverUrl;
        this.canvasUrl = builder.canvasUrl;
        this.durationSeconds = builder.durationSeconds;
        this.playCount = builder.playCount;
        this.releaseAt = builder.releaseAt;
        this.artistId = builder.artistId;
        this.artistName = builder.artistName;
        this.featuredArtists = builder.featuredArtists;
        this.genreId = builder.genreId;
        this.genreName = builder.genreName;
    }

    /*
        -----------------------------------------------------
        Điểm bắt đầu: SongResponse.builder()
        -----------------------------------------------------
        - static: gọi thẳng qua tên class SongResponse.builder(), không cần có sẵn 1 đối tượng SongResponse
          (lúc này chưa có đối tượng nào, vì đang chuẩn bị tạo).
        - Trả về 1 Builder rỗng (mọi thuộc tính đều null) để gán dần từng giá trị.
    */
    public static Builder builder() {
        return new Builder();
    }

    /*
        -----------------------------------------------------
        Getter (Jackson gọi để tạo JSON)
        -----------------------------------------------------
        Tên key trong JSON lấy theo tên getter: getAudioUrl() → "audioUrl"...
        Không có setter: tạo xong qua Builder thì không sửa được nữa.
    */
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getAudioUrl() { return audioUrl; }
    public String getCoverUrl() { return coverUrl; }
    public String getCanvasUrl() { return canvasUrl; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public Long getPlayCount() { return playCount; }
    public LocalDateTime getReleaseAt() { return releaseAt; }
    public Long getArtistId() { return artistId; }
    public String getArtistName() { return artistName; }
    public List<ArtistSummary> getFeaturedArtists() { return featuredArtists; }
    public Long getGenreId() { return genreId; }
    public String getGenreName() { return genreName; }

    /*
        =====================================================================
        CLASS BUILDER (CLASS LỒNG BÊN TRONG SONGRESPONSE)
        =====================================================================

        public static class Builder: 1 class được viết bên trong class SongResponse.
        - public: bên ngoài dùng được (SongMapper gọi SongResponse.builder().id(...)...).
        - static: Builder tồn tại độc lập, không cần có sẵn 1 đối tượng SongResponse mới tạo được Builder.
          (Đúng ý đồ: Builder có trước, SongResponse được tạo sau, từ Builder.)
        - Tên đầy đủ khi gọi từ bên ngoài là SongResponse.Builder.

        Builder có đúng 13 thuộc tính giống hệt SongResponse, đóng vai "tờ giấy nháp":
        gán dần từng giá trị vào đây, đủ rồi mới tạo SongResponse thật.
    */
    public static class Builder {
        private Long id;
        private String title;
        private String audioUrl;
        private String coverUrl;
        private String canvasUrl;
        private Integer durationSeconds;
        private Long playCount;
        private LocalDateTime releaseAt;
        private Long artistId;
        private String artistName;
        private List<ArtistSummary> featuredArtists;
        private Long genreId;
        private String genreName;

        /*
            -----------------------------------------------------
            Các hàm gán giá trị
            -----------------------------------------------------
            Đọc 1 dòng:
                public Builder id(Long id) { this.id = id; return this; }
                - Kiểu trả về là Builder (không phải void như setter thông thường).
                - Tên hàm đặt đúng bằng tên thuộc tính, không có chữ "set" → đọc tự nhiên: .id(15L).
                - this.id = id: gán giá trị vào thuộc tính của Builder.
                - return this: trả về chính Builder này
                  → gọi tiếp được hàm khác ngay sau dấu chấm: .id(15L).title("Chiều Mưa")...

            Viết dài tương đương:
                public Builder id(Long id) {
                    this.id = id;
                    return this;
                }
        */
        public Builder id(Long id) { this.id = id; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder audioUrl(String audioUrl) { this.audioUrl = audioUrl; return this; }
        public Builder coverUrl(String coverUrl) { this.coverUrl = coverUrl; return this; }
        public Builder canvasUrl(String canvasUrl) { this.canvasUrl = canvasUrl; return this; }
        public Builder durationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; return this; }
        public Builder playCount(Long playCount) { this.playCount = playCount; return this; }
        public Builder releaseAt(LocalDateTime releaseAt) { this.releaseAt = releaseAt; return this; }
        public Builder artistId(Long artistId) { this.artistId = artistId; return this; }
        public Builder artistName(String artistName) { this.artistName = artistName; return this; }
        public Builder featuredArtists(List<ArtistSummary> featuredArtists) { this.featuredArtists = featuredArtists; return this; }
        public Builder genreId(Long genreId) { this.genreId = genreId; return this; }
        public Builder genreName(String genreName) { this.genreName = genreName; return this; }

        /*
            -----------------------------------------------------
            build(): tạo SongResponse thật từ Builder
            -----------------------------------------------------
            new SongResponse(this): gọi constructor private ở trên,
            truyền vào chính Builder này (this) để constructor chép từng giá trị sang.
            Đây là bước cuối của chuỗi: SongResponse.builder()...build().
        */
        public SongResponse build() {
            return new SongResponse(this);
        }
    }
}