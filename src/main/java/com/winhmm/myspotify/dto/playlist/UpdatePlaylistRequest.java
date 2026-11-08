package com.winhmm.myspotify.dto.playlist;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    - NotBlank, NotNull, Size: các annotation kiểm tra dữ liệu (giải thích ở từng thuộc tính).
*/
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/*
    =====================================================================
    UPDATEPLAYLISTREQUEST - DỮ LIỆU GỬI LÊN KHI SỬA THÔNG TIN PLAYLIST
    =====================================================================

    (DTO là gì: xem CreatePlaylistRequest.)

    API: PUT /api/playlists/{playlistId}
    Body: JSON (raw → JSON trong Postman), VD:
        {
            "name": "Nhạc chill tối thứ 7",
            "description": "Nghe lúc đọc sách",
            "isPublic": true
        }

    Sửa được 3 thứ: tên, mô tả, công khai hay riêng tư.
    Ảnh bìa có API riêng (PUT, DELETE /api/playlists/{playlistId}/cover),
    bài hát cũng có API riêng (thêm, bỏ từng bài).

    -----------------------------------------------------
    Gửi đủ cả 3 trường mỗi lần
    -----------------------------------------------------
    API này thay toàn bộ thông tin cũ bằng thông tin mới (giống màn hình "Sửa thông tin" của Spotify:
    hiện sẵn tên và mô tả cũ, người dùng sửa chỗ nào thì sửa, rồi bấm Lưu gửi lên cả 3).
    - description không gửi, gửi null hoặc gửi "" → xóa mô tả.
    - Muốn giữ mô tả cũ → gửi lại đúng mô tả cũ.

    -----------------------------------------------------
    JSON được đổi thành object này thế nào?
    -----------------------------------------------------
    Controller nhận bằng @Valid @RequestBody UpdatePlaylistRequest:
    - @RequestBody: Spring dùng thư viện Jackson đọc chuỗi JSON trong body,
      tạo object UpdatePlaylistRequest rồi gọi setter theo tên key:
          "name"        → setName(...)
          "description" → setDescription(...)
          "isPublic"    → setIsPublic(...)
      JSON viết sai cú pháp (thiếu dấu ngoặc, thiếu dấu phẩy...) → 400 "Invalid request data".
    - @Valid: kiểm tra các annotation bên dưới. Sai → 400 kèm message.

    Khác CreatePlaylistRequest (form-data, @ModelAttribute): API này không có file nên dùng JSON,
    gọn hơn cho frontend.
*/
public class UpdatePlaylistRequest {

    /*
        Tên mới: bắt buộc, tối đa 100 ký tự (giống lúc tạo).
    */
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    /*
        Mô tả mới: không bắt buộc, tối đa 300 ký tự.
        null hoặc toàn dấu cách → Service lưu null (xóa mô tả).
    */
    @Size(max = 300, message = "Description must not exceed 300 characters")
    private String description;

    /*
        Công khai hay riêng tư: bắt buộc gửi true hoặc false.
        - Boolean (B hoa) + @NotNull: không gửi → null → @NotNull báo "isPublic is required".
          Nếu dùng boolean (b thường), không gửi sẽ tự thành false
          → playlist công khai lặng lẽ bị chuyển thành riêng tư, người dùng không hề biết.
        - Getter, setter tên getIsPublic, setIsPublic → key JSON là "isPublic"
          (lý do: xem CreatePlaylistRequest).
        Gửi "isPublic": "abc" → 400 "Invalid request data".
    */
    @NotNull(message = "isPublic is required")
    private Boolean isPublic;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
}