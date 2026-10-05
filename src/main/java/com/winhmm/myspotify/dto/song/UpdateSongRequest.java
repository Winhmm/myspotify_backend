package com.winhmm.myspotify.dto.song;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

/*
    UC29 - Artist sửa tên bài hát và danh sách nghệ sĩ feat.

    JSON frontend gửi lên:
    {
        "title": "Tên bài mới",
        "featuredArtistIds": [12, 13]
    }

    Không feat với ai → gửi [] hoặc bỏ trống trường featuredArtistIds.
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
    public void setFeaturedArtistIds(List<Long> featuredArtistIds) { this.featuredArtistIds = featuredArtistIds; }
}
