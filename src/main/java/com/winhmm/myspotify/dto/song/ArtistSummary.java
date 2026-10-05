package com.winhmm.myspotify.dto.song;

/*
    Thông tin rút gọn của 1 nghệ sĩ.

    Dùng cho: danh sách nghệ sĩ feat., kết quả tìm nghệ sĩ (UC37).
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
