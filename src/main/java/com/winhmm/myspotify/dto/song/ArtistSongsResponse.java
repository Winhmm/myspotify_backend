package com.winhmm.myspotify.dto.song;

/*
    UC25 - Trang của 1 Artist: thông tin nghệ sĩ + danh sách bài hát (có phân trang).

    JSON:
    {
        "artistId": 12,
        "artistName": "Trang Phan",
        "bio": "Ca sĩ indie pop...",
        "avatarUrl": "/uploads/avatars/abc.jpg",
        "songs": { "content": [...], "page": 0, ... }
    }
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
