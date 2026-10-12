package com.winhmm.myspotify.dto.song;

/*
    ===================================================================
    ArtistSummary - Thông tin rút gọn của 1 nghệ sĩ.
    ===================================================================

    Chỉ gồm 2 trường: id và nghệ danh.

    DÙNG Ở ĐÂU:
    1. Danh sách nghệ sĩ feat trong mỗi bài hát:
         "featuredArtists": [ { "id": 12, "artistName": "Sóng Nhỏ" } ]
    2. Kết quả tìm nghệ sĩ để feat (UC37):
         GET /api/artist/search-artists?keyword=may
         → [ { "id": 5, "artistName": "Mây Chiều" } ]

    VÌ SAO CHỈ CÓ 2 TRƯỜNG?
    - Frontend chỉ cần hiển thị tên và có id để bấm vào xem trang nghệ sĩ.
    - Không lộ email, họ tên thật, trạng thái tài khoản của nghệ sĩ
      → bảo vệ thông tin cá nhân. (Nguyên tắc: chỉ trả đúng những gì frontend cần).
    - JSON gọn → tải nhanh, nhất là khi 1 trang có 20 bài, mỗi bài vài người feat.

    Được tạo bởi SongMapper.toArtistSummary(user).

    DTO response → chỉ có getter, không có setter (tạo xong không sửa được).
*/
public class ArtistSummary {
    private Long id;
    private String artistName;

    public ArtistSummary(Long id, String artistName) {
        this.id = id;
        this.artistName = artistName;
    }

    public Long getId() { return id; }
    public String getArtistName() { return artistName; }
}
