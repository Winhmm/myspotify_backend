package com.winhmm.myspotify.controller;

import com.winhmm.myspotify.dto.song.PageResponse;
import com.winhmm.myspotify.dto.song.RejectSongRequest;
import com.winhmm.myspotify.dto.song.SongManageResponse;
import com.winhmm.myspotify.enums.SongStatus;
import com.winhmm.myspotify.mapper.SongMapper;
import com.winhmm.myspotify.service.AdminSongService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/*
    API dành cho ADMIN quản lý bài hát (UC34 - UC36).
*/
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/admin/songs")
public class AdminSongController {
    private final AdminSongService adminSongService;

    public AdminSongController(AdminSongService adminSongService) {
        this.adminSongService = adminSongService;
    }

    /*
        UC34 - Danh sách bài hát.
        VD: GET /api/admin/songs?status=PENDING&page=0&size=20
        status viết HOA: PENDING, APPROVED, REJECTED, HIDDEN, DELETED. Bỏ trống = mọi bài trừ DELETED.
    */
    @GetMapping
    public ResponseEntity<PageResponse<SongManageResponse>> getSongs(
            @RequestParam(required = false) SongStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(SongMapper.toPageResponse(
                adminSongService.getSongs(status, page, size),
                SongMapper::toManageResponse));
    }

    /*
        UC35 - Duyệt bài.
    */
    @PutMapping("/{songId}/approve")
    public ResponseEntity<SongManageResponse> approveSong(@PathVariable Long songId) {
        return ResponseEntity.ok(SongMapper.toManageResponse(adminSongService.approveSong(songId)));
    }

    /*
        UC35 - Từ chối bài chờ duyệt. Body: { "reason": "..." }
    */
    @PutMapping("/{songId}/reject")
    public ResponseEntity<SongManageResponse> rejectSong(@PathVariable Long songId,
                                                         @Valid @RequestBody RejectSongRequest request) {
        return ResponseEntity.ok(SongMapper.toManageResponse(
                adminSongService.rejectSong(songId, request.getReason())));
    }

    /*
        UC36 - Gỡ bài đã duyệt. Body: { "reason": "..." }
    */
    @PutMapping("/{songId}/remove")
    public ResponseEntity<SongManageResponse> removeSong(@PathVariable Long songId,
                                                         @Valid @RequestBody RejectSongRequest request) {
        return ResponseEntity.ok(SongMapper.toManageResponse(
                adminSongService.removeSong(songId, request.getReason())));
    }
}
