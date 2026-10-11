package com.winhmm.myspotify.controller;

import com.winhmm.myspotify.dto.song.ArtistSongsResponse;
import com.winhmm.myspotify.dto.song.PageResponse;
import com.winhmm.myspotify.dto.song.SongResponse;
import com.winhmm.myspotify.entity.User;
import com.winhmm.myspotify.mapper.SongMapper;
import com.winhmm.myspotify.service.SongService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/*
    API dành cho NGƯỜI NGHE (UC20 - UC25).
    Không có @PreAuthorize → mọi tài khoản đã đăng nhập (USER, ARTIST, ADMIN) đều dùng được.

    UC23 (nghe nhạc) không cần API riêng:
    frontend phát trực tiếp từ audioUrl, VD: http://<server>/uploads/songs/audio/abc.mp3
*/
@RestController
@RequestMapping("/api/songs")
public class SongController {
    private final SongService songService;

    public SongController(SongService songService) {
        this.songService = songService;
    }

    /*
        UC20 - Danh sách bài đã phát hành.
        VD: GET /api/songs?page=0&size=20
    */
    @GetMapping
    public ResponseEntity<PageResponse<SongResponse>> getSongs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(SongMapper.toPageResponse(
                songService.getPublishedSongs(page, size), SongMapper::toSongResponse));
    }

    /*
        UC21 - Tìm kiếm.
        VD: GET /api/songs/search?keyword=son tung&page=0&size=20
    */
    @GetMapping("/search")
    public ResponseEntity<PageResponse<SongResponse>> searchSongs(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(SongMapper.toPageResponse(
                songService.searchSongs(keyword, page, size), SongMapper::toSongResponse));
    }

    /*
        UC22 - Chi tiết bài hát.
    */
    @GetMapping("/{songId}")
    public ResponseEntity<SongResponse> getSong(@PathVariable Long songId) {
        return ResponseEntity.ok(SongMapper.toSongResponse(songService.getPublishedSong(songId)));
    }

    /*
        UC24 - Tăng lượt nghe (frontend gọi khi bắt đầu phát bài).
    */
    @PostMapping("/{songId}/play")
    public ResponseEntity<Map<String, String>> play(Authentication authentication,
                                                    @PathVariable Long songId) {
        songService.play(songId, authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Play recorded"));
    }

    /*
        UC25 - Trang của 1 Artist: thông tin + bài đã phát hành.
        VD: GET /api/songs/artists/5?page=0&size=20
    */
    @GetMapping("/artists/{artistId}")
    public ResponseEntity<ArtistSongsResponse> getArtistPage(
            @PathVariable Long artistId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User artist = songService.getArtist(artistId);

        PageResponse<SongResponse> songs = SongMapper.toPageResponse(
                songService.getArtistSongs(artistId, page, size), SongMapper::toSongResponse);

        return ResponseEntity.ok(new ArtistSongsResponse(
                artist.getId(), artist.getArtistName(), artist.getBio(), artist.getAvatarUrl(), songs));
    }
}
