package com.winhmm.myspotify.dto.song;

/*
    -----------------------------------------------------
    Package dto.song: thứ tự nên đọc
    -----------------------------------------------------
    Package dto.song có 11 file. Các khái niệm cơ bản (package, import, DTO, annotation,
    validation, getter, setter, Jackson) đã giải thích chi tiết trong RegisterRequest (package dto.auth).

    Nhóm 1 - Request DTO (dữ liệu Artist, Admin gửi lên):
        1. UploadSongRequest    → Artist đăng bài (file này, đọc trước)
        2. UpdateSongRequest    → Artist sửa tên bài và danh sách feat
        3. UpdateReleaseRequest → Artist đổi lịch phát hành
        4. RejectSongRequest    → Admin từ chối hoặc gỡ bài, kèm lý do

    Nhóm 2 - Response DTO (dữ liệu backend trả về):
        5. GenreResponse            → 1 thể loại nhạc (response đơn giản nhất)
        6. ArtistSummary            → id và nghệ danh của 1 nghệ sĩ
        7. SongResponse             → bài hát cho người nghe (giải thích Builder pattern)
        8. SongManageResponse       → bài hát đầy đủ cho Artist và Admin
        9. PageResponse<T>          → 1 trang kết quả (giải thích generic <T>)
        10. ArtistSongsResponse     → trang của 1 nghệ sĩ (DTO lồng nhau)
        11. ListeningHistoryResponse → 1 bài trong mục "Nghe gần đây"

    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    - NotBlank, NotNull, Size: annotation kiểm tra dữ liệu (Jakarta Bean Validation, xem RegisterRequest).
    - DateTimeFormat         : annotation của Spring, cho biết chuỗi ngày giờ gửi lên có định dạng nào.
    - MultipartFile          : đối tượng Spring tạo ra cho mỗi file được upload (nhạc, ảnh).
    - LocalDateTime          : kiểu ngày giờ của Java, không kèm múi giờ.
    - ArrayList, List        : danh sách của Java.
      List là interface (khuôn chung cho mọi loại danh sách),
      ArrayList là 1 loại danh sách cụ thể dùng để tạo đối tượng: new ArrayList<>().
*/
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/*
    =====================================================================
    UPLOADSONGREQUEST - DỮ LIỆU ARTIST GỬI LÊN KHI ĐĂNG BÀI (UC26)
    =====================================================================

    API: POST /api/artist/songs   (Content-Type: multipart/form-data)
    Xem ArtistSongController.uploadSong.

    =====================================================================
    VÌ SAO KHÔNG DÙNG JSON?
    =====================================================================

    JSON chỉ chứa chữ, không chứa được file nhị phân (mp3, jpg).
    multipart/form-data chia request thành nhiều "phần", mỗi phần là 1 trường chữ hoặc 1 file
    → gửi được cả tên bài lẫn file nhạc trong cùng 1 request.
    Giống cái phong bì có nhiều ngăn: ngăn này đựng tờ giấy ghi tên bài, ngăn kia đựng đĩa nhạc.

    =====================================================================
    CÁC TRƯỜNG (POSTMAN: BODY → FORM-DATA)
    =====================================================================

        KEY                TYPE   BẮT BUỘC   VÍ DỤ
        title              Text   Có         Chiều Mưa
        genreId            Text   Có         24   (id lấy từ GET /api/genres, chọn 1 lần, đăng xong không đổi được)
        audio              File   Có         chieu-mua.mp3
        cover              File   Không      bia.jpg
        featuredArtistIds  Text   Không      12   (lặp lại key để feat nhiều người:
        featuredArtistIds  Text   Không      13    Spring gom thành danh sách [12, 13])
        releaseAt          Text   Không      2026-12-24T20:00:00

    Frontend (JavaScript):
        const form = new FormData();
        form.append("title", "Chiều Mưa");
        form.append("genreId", 24);
        form.append("audio", fileInput.files[0]);
        form.append("featuredArtistIds", 12);
        form.append("featuredArtistIds", 13);
        axios.post("/api/artist/songs", form);   // axios tự đặt Content-Type multipart

    Video lặp (Canvas) không gửi ở đây, mà đăng riêng sau khi có bài (PUT /api/artist/songs/{songId}/canvas).

    =====================================================================
    SPRING ĐIỀN DỮ LIỆU THẾ NÀO?
    =====================================================================

    Controller khai báo: @Valid @ModelAttribute UploadSongRequest request
    - @ModelAttribute: lấy dữ liệu từ form-data (khác @RequestBody lấy từ JSON).
      Người làm việc này là Spring (data binding), không phải Jackson.

    1. Spring tạo đối tượng bằng constructor rỗng mặc định
       (Java tự sinh vì class không khai báo constructor nào).
    2. Với mỗi trường trong form-data, gọi setter trùng tên:
         key "title" → setTitle("Chiều Mưa")
         key "audio" → setAudio(file)
       Mọi giá trị trong form-data đều là chữ, Spring tự đổi sang đúng kiểu của thuộc tính:
         "24" → Long 24, "2026-12-24T20:00:00" → LocalDateTime.
       Đổi không được (VD genreId = "abc") → 400 "Invalid value for genreId"
       (GlobalExceptionHandler xử lý).
    3. @Valid kiểm tra các annotation bên dưới.

    → DTO request bắt buộc phải có setter (khác DTO response chỉ có getter).
    → Tên key trong form-data phải đúng tên thuộc tính.
      Gửi key sai, VD "song_title" → Spring không tìm thấy setter nào → title = null
      → bị @NotBlank chặn → 400 "Title is required".

    =====================================================================
    @NotNull, @NotEmpty, @NotBlank KHÁC NHAU THẾ NÀO?
    =====================================================================

        Giá trị gửi lên   @NotNull   @NotEmpty   @NotBlank
        null              ✗ chặn     ✗ chặn      ✗ chặn
        ""                ✓ qua      ✗ chặn      ✗ chặn
        "   "             ✓ qua      ✓ qua       ✗ chặn

    - @NotBlank: chỉ dùng cho chuỗi (String), chặn cả chuỗi toàn dấu cách → dùng cho title.
    - @NotNull : dùng cho mọi kiểu đối tượng → dùng cho genreId (Long) và audio (MultipartFile),
                 vì 2 kiểu này không phải chuỗi, không có khái niệm "toàn dấu cách".
    - @NotEmpty: dùng cho chuỗi hoặc danh sách, chỉ chặn null và rỗng. File này không dùng.

    =====================================================================
    KIỂM TRA 2 LỚP
    =====================================================================

    Lớp 1 (DTO, ở đây): kiểm tra hình thức: có gửi không, dài quá không, quá 5 người không.
    Lớp 2 (Service)   : kiểm tra nghiệp vụ, cần đọc file hoặc database:
                        file có phải nhạc thật không, dung lượng, id thể loại và id feat có tồn tại không,
                        lịch có ở tương lai không...
*/
public class UploadSongRequest {
    /*
        -----------------------------------------------------
        title: tên bài hát
        -----------------------------------------------------
        - @NotBlank: bắt buộc, không được rỗng hay toàn dấu cách → "Title is required".
        - @Size(max = 150): tối đa 150 ký tự → "Title must not exceed 150 characters".
          Khớp với độ dài cột title trong bảng songs (length = 150 trong entity Song).
          Không có quy tắc này thì tên dài hơn 150 ký tự sẽ gây lỗi lúc lưu MySQL (lỗi 500).
    */
    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must not exceed 150 characters")
    private String title;

    /*
        -----------------------------------------------------
        genreId: id thể loại nhạc
        -----------------------------------------------------
        - Long: id trong database là số nguyên lớn, kiểu Long để có thể null khi không gửi.
        - @NotNull: bắt buộc phải chọn thể loại → "Genre is required".
        Id không tồn tại (VD 999): DTO không biết được (phải hỏi database),
        nên ArtistSongService kiểm tra và báo "Genre does not exist".
        Danh sách thể loại để frontend hiện ô chọn: GET /api/genres (GenreController).
    */
    @NotNull(message = "Genre is required")
    private Long genreId;

    /*
        -----------------------------------------------------
        audio: file nhạc
        -----------------------------------------------------
        MultipartFile: đối tượng Spring tạo cho mỗi file upload, có các hàm:
            getOriginalFilename() → tên file gốc trên máy người gửi, VD "chieu-mua.mp3"
            getSize()             → kích thước, đơn vị byte
            getContentType()      → loại file trình duyệt khai báo, VD "audio/mpeg"
            getInputStream()      → luồng để đọc nội dung file
        - @NotNull: bắt buộc phải có file nhạc → "Audio file is required".
        File có phải nhạc thật không, có đọc được thời lượng không: FileStorageService kiểm tra
        (đổi đuôi .txt thành .mp3 cũng bị phát hiện).
    */
    @NotNull(message = "Audio file is required")
    private MultipartFile audio;

    /*
        -----------------------------------------------------
        cover: ảnh bìa (không bắt buộc)
        -----------------------------------------------------
        Không có annotation nào → không gửi cũng được, cover = null.
        Không có ảnh bìa thì frontend hiển thị ảnh mặc định (coverUrl = null).
        Đổi ảnh bìa sau: PUT /api/artist/songs/{songId}/cover.
    */
    private MultipartFile cover;

    /*
        -----------------------------------------------------
        featuredArtistIds: danh sách id nghệ sĩ feat (không bắt buộc)
        -----------------------------------------------------
        List<Long>: danh sách các số Long, VD [12, 13].
        Trong form-data, lặp lại cùng 1 key nhiều lần → Spring gom thành 1 danh sách.

        - @Size(max = 5): với danh sách, @Size đếm số phần tử (không phải số ký tự).
          Quá 5 người → "Maximum 5 featured artists".
        - = new ArrayList<>(): giá trị ban đầu là danh sách rỗng.
          Không gửi key featuredArtistIds → Spring không gọi setter → giữ danh sách rỗng
          → Service không phải lo trường hợp null (tránh lỗi NullPointerException).
          <> (diamond): Java tự hiểu kiểu bên trong là Long, không cần viết new ArrayList<Long>().

        Id có tồn tại không, có phải Artist đang hoạt động không, có trùng không, có tự feat mình không:
        ArtistSongService kiểm tra.
    */
    @Size(max = 5, message = "Maximum 5 featured artists")
    private List<Long> featuredArtistIds = new ArrayList<>();

    /*
        -----------------------------------------------------
        releaseAt: lịch phát hành (không bắt buộc)
        -----------------------------------------------------
        - Không gửi (null) → phát hành ngay khi Admin duyệt.
        - Có gửi → bài chỉ hiện với người nghe từ thời điểm này (phải ở tương lai, Service kiểm tra).
        Thời gian theo giờ máy chủ (server chạy múi giờ Asia/Ho_Chi_Minh thì là giờ Việt Nam).

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME):
        Form-data chỉ gửi chữ. Annotation này cho Spring biết chuỗi có dạng ISO:
            "2026-12-24T20:00:00" (năm-tháng-ngày, chữ T, giờ:phút:giây)
        để đổi thành LocalDateTime.
        Gửi sai dạng, VD "24/12/2026 20:00" → 400 "Invalid value for releaseAt".
        (Vì sao DTO dùng JSON thì không cần annotation này: xem UpdateReleaseRequest.)
    */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime releaseAt;

    /*
        -----------------------------------------------------
        Getter và setter
        -----------------------------------------------------
        - Setter: Spring gọi khi điền dữ liệu từ form-data.
        - Getter: ArtistSongController gọi request.getTitle(), request.getAudio()...
          để truyền xuống ArtistSongService.uploadSong.
        Cách đọc từng dòng: xem RegisterRequest.

        Setter của featuredArtistIds có thêm 1 lớp bảo vệ:
            this.featuredArtistIds = (featuredArtistIds != null) ? featuredArtistIds : new ArrayList<>();
        Đây là toán tử 3 ngôi: điều_kiện ? giá_trị_nếu_đúng : giá_trị_nếu_sai.
        - Tham số khác null → dùng tham số.
        - Tham số là null   → dùng danh sách rỗng mới.
        Viết dài tương đương:
            if (featuredArtistIds != null) {
                this.featuredArtistIds = featuredArtistIds;
            } else {
                this.featuredArtistIds = new ArrayList<>();
            }
        → Dù ai gọi setter với null, thuộc tính vẫn không bao giờ là null.
    */
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Long getGenreId() { return genreId; }
    public void setGenreId(Long genreId) { this.genreId = genreId; }

    public MultipartFile getAudio() { return audio; }
    public void setAudio(MultipartFile audio) { this.audio = audio; }

    public MultipartFile getCover() { return cover; }
    public void setCover(MultipartFile cover) { this.cover = cover; }

    public List<Long> getFeaturedArtistIds() { return featuredArtistIds; }
    public void setFeaturedArtistIds(List<Long> featuredArtistIds) {
        this.featuredArtistIds = (featuredArtistIds != null) ? featuredArtistIds : new ArrayList<>();
    }

    public LocalDateTime getReleaseAt() { return releaseAt; }
    public void setReleaseAt(LocalDateTime releaseAt) { this.releaseAt = releaseAt; }
}