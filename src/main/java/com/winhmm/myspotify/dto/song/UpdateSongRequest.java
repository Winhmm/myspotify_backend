package com.winhmm.myspotify.dto.song;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

/*
    =====================================================================
    UpdateSongRequest - ARTIST SỬA TÊN BÀI VÀ DANH SÁCH FEAT (UC29)
    =====================================================================

    API: PUT /api/artist/songs/{songId}   (Content-Type: application/json)

    Dùng JSON (không phải form-data) vì API này không có file.

    JSON frontend gửi lên:
    {
        "title": "Chiều Mưa (Acoustic)",
        "featuredArtistIds": [12, 13]
    }

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

    =====================================================================
    QUY TẮC CỦA featuredArtistIds
    =====================================================================

    Là danh sách mới hoàn toàn, thay thế danh sách cũ:
        Đang feat [12, 13], gửi [12] → còn feat 12.
        Không feat với ai → gửi [] hoặc bỏ trống trường featuredArtistIds.

    Vì sao bắt gửi lại cả title khi chỉ muốn đổi feat?
    → Đơn giản cho cả 2 phía: frontend luôn gửi đủ form sửa bài, backend luôn cập nhật đủ,
      không phải phân biệt "trường nào có gửi, trường nào không".

    Kiểm tra giống UploadSongRequest (xem giải thích chi tiết ở file đó).
*/
public class UpdateSongRequest {
    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must not exceed 150 characters")
    private String title;

    @Size(max = 5, message = "Maximum 5 featured artists")
    private List<Long> featuredArtistIds = new ArrayList<>();

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<Long> getFeaturedArtistIds() { return featuredArtistIds; }
    public void setFeaturedArtistIds(List<Long> featuredArtistIds) {
        this.featuredArtistIds = (featuredArtistIds != null) ? featuredArtistIds : new ArrayList<>();
    }
}