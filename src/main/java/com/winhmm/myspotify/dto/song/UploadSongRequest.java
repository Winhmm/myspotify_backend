package com.winhmm.myspotify.dto.song;

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
    UploadSongRequest - DỮ LIỆU ARTIST GỬI LÊN KHI ĐĂNG BÀI (UC26)
    =====================================================================

    API: POST /api/artist/songs   (Content-Type: multipart/form-data)

    =====================================================================
    VÌ SAO KHÔNG DÙNG JSON?
    =====================================================================

    JSON chỉ chứa chữ, không chứa được file nhị phân (mp3, jpg).
    multipart/form-data chia request thành nhiều "phần", mỗi phần là 1 trường chữ hoặc 1 file
    → gửi được cả tên bài lẫn file nhạc trong cùng 1 request.

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

    =====================================================================
    SPRING ĐIỀN DỮ LIỆU THẾ NÀO?
    =====================================================================

    Controller khai báo: @Valid @ModelAttribute UploadSongRequest request
    1. Spring tạo đối tượng bằng constructor rỗng mặc định
       (Java tự sinh vì class không khai báo constructor nào).
    2. Với mỗi trường trong form-data, gọi setter trùng tên:
         key "title" → setTitle("Chiều Mưa")
         key "audio" → setAudio(file)
    3. @Valid kiểm tra các annotation bên dưới.

    → DTO request bắt buộc phải có setter (khác DTO response chỉ có getter).
    → Tên key trong form-data phải trùng tên thuộc tính, phân biệt hoa thường.
      Gửi key "Title" (viết hoa) hoặc "song_title" → Spring không tìm thấy setter → title = null
      → bị @NotBlank chặn → 400 "Title is required".

    =====================================================================
    @NotNull, @NotEmpty, @NotBlank KHÁC NHAU THẾ NÀO?
    =====================================================================

        Giá trị gửi lên   @NotNull   @NotEmpty   @NotBlank
        null              ✗ chặn     ✗ chặn      ✗ chặn
        ""                ✓ qua      ✗ chặn      ✗ chặn
        "   "             ✓ qua      ✓ qua       ✗ chặn

    - @NotBlank: chỉ dùng cho String, chặn cả chuỗi toàn dấu cách → dùng cho title.
    - @NotNull : dùng cho mọi kiểu object → dùng cho audio (MultipartFile không phải String).

    =====================================================================
    KIỂM TRA 2 LỚP
    =====================================================================

    Lớp 1 (DTO, ở đây): kiểm tra hình thức: có gửi không, dài quá không, quá 5 người không.
    Lớp 2 (Service)   : kiểm tra nghiệp vụ, cần đọc file hoặc database:
                        file có phải nhạc thật không, dung lượng, id feat có tồn tại không,
                        lịch có ở tương lai không...
*/
public class UploadSongRequest {
    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must not exceed 150 characters")
    private String title;

    @NotNull(message = "Genre is required")
    private Long genreId;

    @NotNull(message = "Audio file is required")
    private MultipartFile audio;

    private MultipartFile cover;

    @Size(max = 5, message = "Maximum 5 featured artists")
    private List<Long> featuredArtistIds = new ArrayList<>();

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime releaseAt;

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