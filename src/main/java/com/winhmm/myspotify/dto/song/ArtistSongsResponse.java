package com.winhmm.myspotify.dto.song;

/*
    ===================================================================
    ArtistSongsResponse - Dữ liệu cho trang của một nghệ sĩ (UC25).
    ===================================================================

    API: GET /api/songs/artists/{artistId}?page=0&size=20

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

    LỒNG DTO: trường songs có kiểu PageResponse<SongResponse>
    → Jackson tự chuyển lồng nhiều tầng: ArtistSongsResponse → PageResponse → SongResponse
      → ArtistSummary. Không cần viết code chuyển JSON nào.

    Chuyển sang trang 2 bài hát: gọi lại cùng API với page = 1.
    Phần thông tin nghệ sĩ được trả lại (giống hệt), frontend có thể bỏ qua.

    Tạo bằng constructor trong SongController.getArtistPage(...)
    (5 trường, không cần Builder).
*/
public class ArtistSongsResponse {
    private Long artistId;
    private String artistName;
    private String bio;
    private String avatarUrl;
    private PageResponse<SongResponse> songs;

    public ArtistSongsResponse(Long artistId, String artistName, String bio,
                               String avatarUrl, PageResponse<SongResponse> songs) {
        this.artistId = artistId;
        this.artistName = artistName;
        this.bio = bio;
        this.avatarUrl = avatarUrl;
        this.songs = songs;
    }

    public Long getArtistId() { return artistId; }
    public String getArtistName() { return artistName; }
    public String getBio() { return bio; }
    public String getAvatarUrl() { return avatarUrl; }
    public PageResponse<SongResponse> getSongs() { return songs; }
}
