package com.winhmm.myspotify.dto.song;

/*
    File này không có dòng import nào:
    - Long, String có sẵn trong gói java.lang (Java tự import).
    - PageResponse, SongResponse nằm cùng package dto.song nên dùng được ngay.
*/

/*
    =====================================================================
    ARTISTSONGSRESPONSE - DỮ LIỆU CHO TRANG CỦA 1 NGHỆ SĨ (UC25)
    =====================================================================

    API: GET /api/songs/artists/{artistId}?page=0&size=20 (xem SongController.getArtistPage)

    Gộp 2 phần vào 1 response, để frontend chỉ gọi 1 API là vẽ được cả trang:
    1. Thông tin nghệ sĩ (phần đầu trang): nghệ danh, giới thiệu, ảnh đại diện.
    2. Danh sách bài hát (phần dưới, có phân trang): bài mà nghệ sĩ là Artist chính
       hoặc được feat, chỉ tính bài đã phát hành.

    JSON:
    {
        "artistId": 5,
        "artistName": "Mây Chiều",
        "bio": "Ballad nhẹ nhàng cho buổi chiều.",
        "avatarUrl": "/uploads/avatars/7c2e.jpg",
        "songs": {
            "content": [ ...SongResponse... ],
            "page": 0,
            "size": 20,
            "totalElements": 12,
            "totalPages": 1
        }
    }

    - bio, avatarUrl có thể null (nghệ sĩ chưa viết giới thiệu, chưa có ảnh)
      → frontend ẩn phần giới thiệu, hiện ảnh mặc định.

    =====================================================================
    DTO LỒNG NHAU
    =====================================================================

    Trường songs có kiểu PageResponse<SongResponse>: 1 DTO nằm bên trong 1 DTO khác.
    → Jackson tự chuyển lồng nhiều tầng, gọi getter của từng tầng:
        ArtistSongsResponse            → object ngoài cùng
          └── songs: PageResponse      → object "songs"
                └── content: SongResponse  → mỗi phần tử trong mảng "content"
                      └── featuredArtists: ArtistSummary → mỗi phần tử trong mảng "featuredArtists"
      Không cần viết code chuyển JSON nào.

    Chuyển sang trang 2 bài hát: gọi lại cùng API với page=1.
    Phần thông tin nghệ sĩ được trả lại (giống hệt), frontend có thể bỏ qua.

    Tạo bằng constructor trong SongController.getArtistPage(...)
    (5 trường, không cần Builder).

    DTO response → chỉ có getter, không có setter (tạo xong không sửa được).
*/
public class ArtistSongsResponse {
    /*
        - artistId  : id tài khoản của nghệ sĩ.
        - artistName: nghệ danh.
        - bio       : đoạn giới thiệu nghệ sĩ tự viết.
        - avatarUrl : đường dẫn ảnh đại diện (tương đối, frontend ghép thêm địa chỉ server).
        - songs     : 1 trang bài hát của nghệ sĩ, kiểu PageResponse<SongResponse>.
    */
    private Long artistId;
    private String artistName;
    private String bio;
    private String avatarUrl;
    private PageResponse<SongResponse> songs;

    /*
        -----------------------------------------------------
        Constructor 5 tham số
        -----------------------------------------------------
        Được gọi trong SongController.getArtistPage:
            new ArtistSongsResponse(artist.getId(), artist.getArtistName(), artist.getBio(),
                                    artist.getAvatarUrl(), songs)
        Phải truyền đúng thứ tự khai báo. 4 tham số đầu lấy từ entity User của nghệ sĩ,
        tham số cuối là trang bài hát đã đổi sang DTO bằng SongMapper.toPageResponse.

        Constructor dài nên xuống dòng giữa danh sách tham số cho dễ đọc.
    */
    public ArtistSongsResponse(Long artistId, String artistName, String bio,
                               String avatarUrl, PageResponse<SongResponse> songs) {
        this.artistId = artistId;
        this.artistName = artistName;
        this.bio = bio;
        this.avatarUrl = avatarUrl;
        this.songs = songs;
    }

    /*
        Getter: Jackson gọi để tạo JSON.
        getSongs() trả về PageResponse → Jackson tiếp tục gọi getter của PageResponse
        để tạo object "songs" lồng bên trong.
    */
    public Long getArtistId() { return artistId; }
    public String getArtistName() { return artistName; }
    public String getBio() { return bio; }
    public String getAvatarUrl() { return avatarUrl; }
    public PageResponse<SongResponse> getSongs() { return songs; }
}