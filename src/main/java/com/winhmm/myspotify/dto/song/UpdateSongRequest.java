package com.winhmm.myspotify.dto.song;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    (Các khái niệm cơ bản: xem RegisterRequest. Thứ tự đọc package dto.song: xem UploadSongRequest.)

    - NotBlank, Size: annotation kiểm tra dữ liệu.
    - ArrayList, List: danh sách của Java (List là khuôn chung, ArrayList là loại cụ thể để tạo đối tượng).
*/
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

/*
    =====================================================================
    UPDATESONGREQUEST - ARTIST SỬA TÊN BÀI VÀ DANH SÁCH FEAT (UC29)
    =====================================================================

    API: PUT /api/artist/songs/{songId}   (Content-Type: application/json)
    Xem ArtistSongController.updateSong.

    Dùng JSON (không phải form-data như UploadSongRequest) vì API này không có file.

    JSON frontend gửi lên:
    {
        "title": "Chiều Mưa (Acoustic)",
        "featuredArtistIds": [12, 13]
    }
    [12, 13] trong JSON là 1 mảng, Jackson đổi thành List<Long> gồm 12 và 13.

    Chỉ sửa được tên bài và danh sách feat.
    - Thể loại: chọn lúc đăng bài, không đổi được.
    - Ảnh bìa, video Canvas, lịch phát hành: có API riêng.
    - File nhạc: không đổi được (muốn đổi nhạc thì xóa bài, đăng bài mới).

    =====================================================================
    SPRING ĐIỀN DỮ LIỆU THẾ NÀO?
    =====================================================================

    Controller khai báo: @Valid @RequestBody UpdateSongRequest request
    1. Jackson tạo đối tượng bằng constructor rỗng mặc định.
    2. Với mỗi trường trong JSON, gọi setter trùng tên:
         "title"             → setTitle("Chiều Mưa (Acoustic)")
         "featuredArtistIds" → setFeaturedArtistIds([12, 13])
    3. @Valid kiểm tra annotation.

    Trường lạ trong JSON (VD "artistId": 99) → Jackson bỏ qua, không lỗi
    (Spring Boot tắt sẵn chế độ báo lỗi khi gặp trường lạ)
    → kẻ gian không thể đổi chủ bài bằng cách gửi thêm artistId.
    Id của Artist luôn được lấy từ token, không bao giờ lấy từ dữ liệu gửi lên.

    =====================================================================
    QUY TẮC CỦA featuredArtistIds
    =====================================================================

    Là danh sách mới hoàn toàn, thay thế danh sách cũ:
        Đang feat [12, 13], gửi [12] → còn feat 12.
        Không feat với ai → gửi [] hoặc bỏ trống trường featuredArtistIds.

    Vì sao bắt gửi lại cả title khi chỉ muốn đổi feat?
    → Đơn giản cho cả 2 phía: frontend luôn gửi đủ form sửa bài, backend luôn cập nhật đủ,
      không phải phân biệt "trường nào có gửi, trường nào không".

    Bài đang bị từ chối (REJECTED) mà Artist sửa → tự gửi duyệt lại (về PENDING).

    Kiểm tra giống UploadSongRequest (xem giải thích chi tiết ở file đó).
*/
public class UpdateSongRequest {
    /*
        -----------------------------------------------------
        title: tên bài mới
        -----------------------------------------------------
        Quy tắc giống hệt lúc đăng bài:
        - @NotBlank: bắt buộc → "Title is required".
        - @Size(max = 150): tối đa 150 ký tự (khớp cột title) → "Title must not exceed 150 characters".
        Không đổi tên thì gửi lại đúng tên cũ.
    */
    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must not exceed 150 characters")
    private String title;

    /*
        -----------------------------------------------------
        featuredArtistIds: danh sách id nghệ sĩ feat mới
        -----------------------------------------------------
        - @Size(max = 5): tối đa 5 phần tử → "Maximum 5 featured artists".
        - = new ArrayList<>(): JSON không có trường này → Jackson không gọi setter
          → giữ danh sách rỗng → Service hiểu là "không feat với ai".
        Giải thích chi tiết: xem thuộc tính cùng tên trong UploadSongRequest.
    */
    @Size(max = 5, message = "Maximum 5 featured artists")
    private List<Long> featuredArtistIds = new ArrayList<>();

    /*
        -----------------------------------------------------
        Getter và setter
        -----------------------------------------------------
        - Setter: Jackson gọi khi đổi JSON thành đối tượng.
        - Getter: ArtistSongController gọi request.getTitle(), request.getFeaturedArtistIds().

        Setter của featuredArtistIds: JSON gửi "featuredArtistIds": null → Jackson gọi setter với null
        → toán tử 3 ngôi đổi thành danh sách rỗng, thuộc tính không bao giờ là null
        (giải thích toán tử 3 ngôi: xem UploadSongRequest).
    */
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<Long> getFeaturedArtistIds() { return featuredArtistIds; }
    public void setFeaturedArtistIds(List<Long> featuredArtistIds) {
        this.featuredArtistIds = (featuredArtistIds != null) ? featuredArtistIds : new ArrayList<>();
    }
}