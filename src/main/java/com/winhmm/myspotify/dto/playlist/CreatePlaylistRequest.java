package com.winhmm.myspotify.dto.playlist;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    - NotBlank, NotEmpty, Size: các annotation kiểm tra dữ liệu (Bean Validation), giải thích ở từng thuộc tính.
    - MultipartFile          : kiểu dữ liệu Spring dùng cho 1 file tải lên (ở đây là ảnh bìa).
    - ArrayList, List        : danh sách id bài hát.
*/
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/*
    =====================================================================
    CREATEPLAYLISTREQUEST - DỮ LIỆU GỬI LÊN KHI TẠO PLAYLIST
    =====================================================================

    (Các khái niệm cơ bản về DTO, @NotBlank, @Size: xem RegisterRequest.
     Gửi form-data có file: xem UploadSongRequest. Các khái niệm đó cũng được giải thích lại bên dưới.)

    =====================================================================
    DTO LÀ GÌ?
    =====================================================================

    DTO (Data Transfer Object) = class chỉ dùng để chở dữ liệu giữa frontend và backend.
    - Request DTO (như class này): chở dữ liệu frontend gửi lên.
    - Response DTO (như PlaylistDetailResponse): chở dữ liệu backend trả về.

    Vì sao không nhận thẳng entity Playlist?
    - Entity có những thuộc tính frontend không được tự đặt (id, owner, createdAt...).
      Nhận thẳng entity → người dùng gửi kèm "owner" của người khác là gây rối được.
    - DTO chỉ có đúng những thứ được phép gửi: tên, mô tả, công khai, danh sách bài, ảnh bìa.
    - Annotation kiểm tra dữ liệu (@NotBlank, @Size...) đặt ở DTO, entity không bị lẫn lộn.

    =====================================================================
    GỬI DỮ LIỆU BẰNG FORM-DATA
    =====================================================================

    API: POST /api/playlists
    Body: multipart/form-data (vì có thể kèm ảnh bìa, JSON không gửi được file).

    Controller nhận bằng @Valid @ModelAttribute CreatePlaylistRequest:
    - @ModelAttribute: Spring tạo 1 object CreatePlaylistRequest rỗng, rồi đọc từng key của form-data,
      gọi setter tương ứng (key "name" → setName, key "songIds" → setSongIds...).
      Tên key lấy theo tên setter: bỏ chữ "set", chữ cái đầu viết thường (setIsPublic → "isPublic").
    - @Valid: sau khi gán xong, kiểm tra các annotation bên dưới.
      Sai → Spring không chạy hàm trong Controller, mà ném MethodArgumentNotValidException
      → GlobalExceptionHandler.handleValidation trả 400 kèm message, VD {"message": "Name is required"}.
      Sai nhiều chỗ cùng lúc thì chỉ trả về 1 message (lỗi đầu tiên Spring tìm thấy).

    -----------------------------------------------------
    Các key trong Postman (Body → form-data)
    -----------------------------------------------------
        Key           Kiểu   Bắt buộc   VD giá trị
        name          Text   Có         Nhạc chill cuối tuần
        description   Text   Không      Nghe lúc làm bài
        isPublic      Text   Không      true   (không gửi → false, tức riêng tư)
        songIds       Text   Có         15     (thêm nhiều dòng songIds để chọn nhiều bài,
                                                hoặc 1 dòng "15,22,9")
        cover         File   Không      ảnh JPG, PNG, WEBP, tối đa 2MB

    Thứ tự bài trong playlist = thứ tự các songIds gửi lên.
    Tạo playlist bắt buộc có ít nhất 1 bài (giống Spotify gợi ý bài lúc tạo).
*/
public class CreatePlaylistRequest {

    /*
        Tên playlist: bắt buộc, tối đa 100 ký tự (khớp cột name VARCHAR(100) trong entity Playlist).
        - @NotBlank: không được null, rỗng "", hay toàn dấu cách "   ".
        - @Size(max = 100): tối đa 100 ký tự.
        - message = "...": câu báo lỗi trả về frontend khi sai.
    */
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    /*
        Mô tả: không bắt buộc, tối đa 300 ký tự (khớp cột description VARCHAR(300)).
        @Size bỏ qua giá trị null → không gửi cũng được.
    */
    @Size(max = 300, message = "Description must not exceed 300 characters")
    private String description;

    /*
        Công khai (true) hay riêng tư (false).

        Vì sao kiểu Boolean (chữ B hoa) và tên getter là getIsPublic?
        - Boolean cho phép null: không gửi key isPublic → null → Service coi là false (riêng tư).
          boolean (b thường) không chứa được null.
        - Spring đặt tên key theo tên setter: setIsPublic → key "isPublic".
          Nếu dùng boolean (chữ thường) với setter setPublic, key sẽ thành "public" (dễ nhầm).
        Gửi chữ không phải true hay false (VD "abc") → 400 "Invalid value for isPublic".
    */
    private Boolean isPublic;

    /*
        Danh sách id bài hát, theo đúng thứ tự muốn xếp trong playlist.
        - @NotEmpty: phải có ít nhất 1 phần tử (tạo playlist bắt buộc có ít nhất 1 bài).
          Khác @NotBlank (dùng cho chuỗi): @NotEmpty dùng cho danh sách.
        - @Size(max = 200): tối đa 200 bài mỗi playlist.
        - = new ArrayList<>(): không gửi key songIds → danh sách rỗng (không phải null)
          → @NotEmpty báo đúng message "At least 1 song is required".
        - List<Long>: Spring tự đổi chữ "15" thành số 15.
          Gửi chữ thay vì số (VD songIds = abc) → 400 "Invalid value for songIds".

        Service còn kiểm tra thêm: bỏ id trùng, mọi bài phải đang phát hành.
    */
    @NotEmpty(message = "At least 1 song is required")
    @Size(max = 200, message = "Maximum 200 songs per playlist")
    private List<Long> songIds = new ArrayList<>();

    /*
        Ảnh bìa riêng: không bắt buộc.
        Không gửi → playlist dùng ảnh bìa của bài đầu tiên (xem PlaylistMapper).
        Kiểm tra định dạng, dung lượng, chữ ký file do FileStorageService.saveImage làm.

        MultipartFile: Spring gói file tải lên thành object này,
        có tên file gốc (getOriginalFilename), dung lượng (getSize), loại file (getContentType)
        và nội dung để đọc (getInputStream).
    */
    private MultipartFile cover;

    /*
        Getter, setter: Spring dùng setter để gán dữ liệu form-data vào,
        Controller dùng getter để đọc ra rồi truyền cho Service.
    */
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }

    public List<Long> getSongIds() { return songIds; }

    /*
        Frontend lỡ gửi null → giữ danh sách rỗng, để @NotEmpty báo lỗi rõ ràng
        thay vì NullPointerException ở chỗ khác.
        (condition) ? a : b : toán tử 3 ngôi, đúng lấy a, sai lấy b.
    */
    public void setSongIds(List<Long> songIds) {
        this.songIds = (songIds != null) ? songIds : new ArrayList<>();
    }

    public MultipartFile getCover() { return cover; }
    public void setCover(MultipartFile cover) { this.cover = cover; }
}