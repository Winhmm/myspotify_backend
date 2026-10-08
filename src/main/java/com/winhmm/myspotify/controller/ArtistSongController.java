package com.winhmm.myspotify.controller;

import com.winhmm.myspotify.dto.song.ArtistSummary;
import com.winhmm.myspotify.dto.song.SongManageResponse;
import com.winhmm.myspotify.dto.song.UploadSongRequest;
import com.winhmm.myspotify.entity.Song;
import com.winhmm.myspotify.mapper.SongMapper;
import com.winhmm.myspotify.service.ArtistSongService;
import com.winhmm.myspotify.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/*
    API dành cho ARTIST (UC26 - UC33, UC37).
    @PreAuthorize("hasRole('ARTIST')") ở đầu class → áp dụng cho mọi API bên dưới.
*/
@PreAuthorize("hasRole('ARTIST')")
@RestController
@RequestMapping("/api/artist")
@Validated
public class ArtistSongController {
    private final ArtistSongService artistSongService;
    private final UserService userService;

    public ArtistSongController(ArtistSongService artistSongService, UserService userService) {
        this.artistSongService = artistSongService;
        this.userService = userService;
    }

    /*
        Lấy id của Artist đang đăng nhập (email trong token → id).
    */
    private Long me(Authentication authentication) {
        return userService.getByEmail(authentication.getName()).getId();
    }

    /*
        UC26 - Tải lên bài hát.

        @ModelAttribute: gom các trường của multipart/form-data vào 1 DTO
        (giống @RequestBody nhưng dành cho form-data thay vì JSON).
        @Valid: kiểm tra các annotation @NotBlank, @Size... trong DTO.
    */
    @PostMapping(value = "/songs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SongManageResponse> uploadSong(Authentication authentication,
                                                         @Valid @ModelAttribute UploadSongRequest request) {
        Song song = artistSongService.uploadSong(
                me(authentication),
                request.getTitle(),
                request.getAudio(),
                request.getCover(),
                request.getFeaturedArtistIds(),
                request.getReleaseAt());

        return ResponseEntity.ok(SongMapper.toManageResponse(song));
    }

    /*
        UC28 - Bài hát của tôi.
    */
    @GetMapping("/songs")
    public ResponseEntity<List<SongManageResponse>> getMySongs(Authentication authentication) {
        List<SongManageResponse> result = artistSongService.getMySongs(me(authentication))
                .stream()
                .map(SongMapper::toManageResponse)
                .toList();
        return ResponseEntity.ok(result);
    }

    /*
        UC37 - Tìm nghệ sĩ để feat.
    */
    @GetMapping("/search-artists")
    public ResponseEntity<List<ArtistSummary>> searchArtists(Authentication authentication,
                                                             @RequestParam("keyword") String keyword) {
        List<ArtistSummary> result = artistSongService.searchArtists(me(authentication), keyword)
                .stream()
                .map(SongMapper::toArtistSummary)
                .toList();

        return ResponseEntity.ok(result);
    }
}
