package com.winhmm.myspotify.dto.song;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/*
    UC26 - Artist tải lên bài hát.

    Frontend gửi multipart/form-data (KHÔNG phải JSON, vì có file):
    - title             : tên bài hát (bắt buộc)
    - audio             : file .mp3 / .wav (bắt buộc)
    - cover             : ảnh bìa (không bắt buộc)
    - featuredArtistIds : id nghệ sĩ feat. (không bắt buộc, gửi nhiều dòng cùng tên)
    - releaseAt         : lịch phát hành (không bắt buộc, VD: 2026-12-24T20:00:00)

    Spring tự đọc từng trường trong form-data rồi gọi setter tương ứng.
*/
public class UploadSongRequest {
    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must not exceed 150 characters")
    private String title;

    @NotNull(message = "Audio file is required")
    private MultipartFile audio;

    private MultipartFile cover;

    @Size(max = 5, message = "Maximum 5 featured artists")
    private List<Long> featuredArtistIds = new ArrayList<>();

    /*
        @DateTimeFormat: cho Spring biết cách đổi chuỗi "2026-12-24T20:00:00" → LocalDateTime.
    */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime releaseAt;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public MultipartFile getAudio() { return audio; }
    public void setAudio(MultipartFile audio) { this.audio = audio; }

    public MultipartFile getCover() { return cover; }
    public void setCover(MultipartFile cover) { this.cover = cover; }

    public List<Long> getFeaturedArtistIds() { return featuredArtistIds; }
    public void setFeaturedArtistIds(List<Long> featuredArtistIds) { this.featuredArtistIds = featuredArtistIds; }

    public LocalDateTime getReleaseAt() { return releaseAt; }
    public void setReleaseAt(LocalDateTime releaseAt) { this.releaseAt = releaseAt; }
}
