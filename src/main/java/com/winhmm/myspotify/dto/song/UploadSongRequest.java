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
    ===================================================================
    UploadSongRequest - Dữ liệu Artist GỬI LÊN khi đăng bài (UC26).
    ===================================================================

    API: POST /api/artist/songs   (Content-Type: multipart/form-data)

    ---------- VÌ SAO KHÔNG DÙNG JSON? ----------

    JSON chỉ chứa chữ, không chứa được file nhị phân (mp3, jpg).
    multipart/form-data chia request thành nhiều "phần", mỗi phần là 1 trường chữ hoặc 1 file
    → gửi được cả tên bài lẫn file nhạc trong cùng 1 request.

    ---------- CÁC TRƯỜNG (Postman: Body → form-data) ----------

        KEY                TYPE   BẮT BUỘC   VÍ DỤ
        title              Text   Có         Chiều Mưa
        audio              File   Có         chieu-mua.mp3
        cover              File   Không      bia.jpg
        featuredArtistIds  Text   Không      12   (lặp lại key để feat nhiều người:
        featuredArtistIds  Text   Không      13    Spring gom thành danh sách [12, 13])
        releaseAt          Text   Không      2026-12-24T20:00:00

    ---------- SPRING ĐIỀN DỮ LIỆU THẾ NÀO? ----------

    Controller khai báo: @Valid @ModelAttribute UploadSongRequest request
    1. Spring tạo đối tượng bằng constructor rỗng mặc định (Java tự sinh vì class không khai báo constructor nào).
    2. Với mỗi trường trong form-data, gọi setter TRÙNG TÊN:
         key "title" → setTitle("Chiều Mưa")
         key "audio" → setAudio(file)
    3. @Valid kiểm tra các annotation bên dưới.

    → DTO REQUEST BẮT BUỘC PHẢI CÓ SETTER (khác DTO response chỉ có getter).
    → Tên key trong form-data phải TRÙNG tên thuộc tính.
      Gửi key "Title" (viết hoa) hoặc "song_title" → Spring không tìm thấy setter → title = null
      → bị @NotBlank chặn → 400 "Title is required".

    ---------- @NotNull / @NotEmpty / @NotBlank KHÁC NHAU THẾ NÀO? ----------

        Giá trị gửi lên   @NotNull   @NotEmpty   @NotBlank
        null              ✗ chặn     ✗ chặn      ✗ chặn
        ""                ✓ qua      ✗ chặn      ✗ chặn
        "   "             ✓ qua      ✓ qua       ✗ chặn

    - @NotBlank: chỉ dùng cho String, chặn cả chuỗi toàn dấu cách → dùng cho title.
    - @NotNull : dùng cho mọi kiểu object → dùng cho audio (MultipartFile không phải String).

    ---------- KIỂM TRA 2 LỚP ----------

    Lớp 1 (DTO, ở đây)  : kiểm tra hình thức: có gửi không, dài quá không, quá 5 người không.
    Lớp 2 (Service)     : kiểm tra nghiệp vụ, cần đọc file hoặc database:
                          file có phải nhạc thật không, dung lượng, id feat có tồn tại không,
                          lịch có ở tương lai không...
*/
public class UploadSongRequest {
    /*
        Tên bài hát.
        - @NotBlank: không được thiếu / rỗng / toàn dấu cách.
        - @Size(max = 150): khớp độ dài cột title (length = 150) trong entity Song.
          Không chặn ở đây → database báo lỗi "Data too long" khó hiểu hơn nhiều.
        Service còn gọi title.trim() để bỏ dấu cách thừa ở 2 đầu.
    */
    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must not exceed 150 characters")
    private String title;

    /*
        File nhạc (MP3 hoặc WAV).

        MultipartFile: đối tượng Spring tạo cho mỗi file tải lên, có:
            getOriginalFilename() → tên file gốc trên máy người dùng, VD "chieu-mua.mp3"
            getSize()             → dung lượng (byte)
            getContentType()      → loại file trình duyệt khai báo, VD "audio/mpeg"
            getInputStream()      → đọc nội dung file

        @NotNull chỉ chặn trường hợp KHÔNG GỬI key "audio".
        Gửi key "audio" nhưng không chọn file → audio là file rỗng (không phải null)
        → Service kiểm tra thêm file.isEmpty() → "Audio file is required".

        Đuôi file, dung lượng, file có phải nhạc thật không → kiểm tra trong FileStorageService.
    */
    @NotNull(message = "Audio file is required")
    private MultipartFile audio;

    /*
        Ảnh bìa: KHÔNG bắt buộc → không có annotation kiểm tra.
        Không gửi → null → bài không có ảnh bìa.
        Có gửi → Service kiểm tra định dạng JPG / PNG / WEBP, tối đa 2MB, chữ ký file thật.
    */
    private MultipartFile cover;

    /*
        Danh sách id nghệ sĩ feat.

        @Size(max = 5) trên List: đếm SỐ PHẦN TỬ trong danh sách (không phải độ dài chuỗi).
        Đây là chặn sơ bộ. Service còn kiểm tra kỹ hơn:
            - Loại id trùng: [12, 12, 13] → {12, 13} rồi mới đếm.
            - Bỏ giá trị rỗng (ô featuredArtistIds để trống → null).
            - Không chứa chính mình, mỗi id phải là Artist đang ACTIVE.

        = new ArrayList<>(): giá trị mặc định là danh sách RỖNG thay vì null
        → Không gửi trường này → featuredArtistIds = [] → không bị NullPointerException.
    */
    @Size(max = 5, message = "Maximum 5 featured artists")
    private List<Long> featuredArtistIds = new ArrayList<>();

    /*
        Lịch phát hành, VD "2026-12-24T20:00:00" (giờ Việt Nam).
        Không gửi → null → phát hành ngay khi Admin duyệt.

        @DateTimeFormat(iso = ISO.DATE_TIME):
        - Form-data gửi lên chỉ là CHUỖI. Annotation này báo cho Spring biết
          chuỗi có dạng "năm-tháng-ngàyTgiờ:phút:giây" để đổi sang LocalDateTime.
        - Thiếu annotation này → Spring không biết đổi thế nào → lỗi.
        - Gửi sai dạng, VD "24/12/2026" → 400 "Invalid value for releaseAt"
          (GlobalExceptionHandler bắt lỗi binding).

        Vì sao UpdateReleaseRequest KHÔNG cần annotation này?
        → Xem giải thích trong UpdateReleaseRequest.java.

        "Phải ở tương lai" kiểm tra trong Service, vì cần so với thời gian hiện tại.
    */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime releaseAt;

    /*
        Getter + setter.
        - Setter: Spring dùng để ĐIỀN dữ liệu từ form-data vào.
        - Getter: Controller dùng để LẤY dữ liệu ra truyền cho Service.
    */
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public MultipartFile getAudio() { return audio; }
    public void setAudio(MultipartFile audio) { this.audio = audio; }

    public MultipartFile getCover() { return cover; }
    public void setCover(MultipartFile cover) { this.cover = cover; }

    public List<Long> getFeaturedArtistIds() { return featuredArtistIds; }
    public void setFeaturedArtistIds(List<Long> featuredArtistIds) { this.featuredArtistIds = featuredArtistIds; }

    public LocalDateTime getReleaseAt() { return releaseAt; }
    public void setReleaseAt(LocalDateTime releaseAt) { this.releaseAt = releaseAt; }
}