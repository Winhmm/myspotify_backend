package com.winhmm.myspotify.dto.song;

/*
    =====================================================================
    ARTISTSUMMARY - THÔNG TIN RÚT GỌN CỦA 1 NGHỆ SĨ
    =====================================================================

    Chỉ gồm 2 trường: id và nghệ danh.
    Cấu trúc giống GenreResponse (thuộc tính private, constructor, chỉ có getter, không có setter).

    Dùng ở đâu:
    1. Danh sách nghệ sĩ feat trong mỗi bài hát (SongResponse, SongManageResponse):
         "featuredArtists": [ { "id": 12, "artistName": "Sóng Nhỏ" } ]
    2. Kết quả tìm nghệ sĩ để feat (UC37):
         GET /api/artist/search-artists?keyword=may
         → [ { "id": 5, "artistName": "Mây Chiều" } ]

    Vì sao chỉ có 2 trường?
    - Frontend chỉ cần hiển thị tên và có id để bấm vào xem trang nghệ sĩ
      (GET /api/songs/artists/{id}).
    - Không lộ email, họ tên thật, trạng thái tài khoản của nghệ sĩ
      → bảo vệ thông tin cá nhân. (Nguyên tắc: chỉ trả đúng những gì frontend cần.)
    - JSON gọn → tải nhanh, nhất là khi 1 trang có 20 bài, mỗi bài vài người feat.

    Được tạo bởi SongMapper.toArtistSummary(user):
        new ArtistSummary(user.getId(), user.getArtistName())
    Nghệ sĩ là 1 tài khoản trong bảng users (role ARTIST), nên id ở đây chính là id của User.

    Tên trường là "id" (không phải "artistId") vì đối tượng này đã là "1 nghệ sĩ",
    gọi artist.id là đủ rõ nghĩa ở frontend.
*/
public class ArtistSummary {
    /*
        - id        : id tài khoản của nghệ sĩ (bảng users).
        - artistName: nghệ danh (không phải username, không phải họ tên thật).
    */
    private Long id;
    private String artistName;

    /*
        Constructor: gán đủ 2 giá trị khi tạo đối tượng (xem GenreResponse).
    */
    public ArtistSummary(Long id, String artistName) {
        this.id = id;
        this.artistName = artistName;
    }

    /*
        Getter: Jackson gọi để tạo JSON: getId() → "id", getArtistName() → "artistName".
    */
    public Long getId() { return id; }
    public String getArtistName() { return artistName; }
}