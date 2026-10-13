package com.winhmm.myspotify.controller;

import com.winhmm.myspotify.dto.song.ListeningHistoryResponse;
import com.winhmm.myspotify.dto.song.PageResponse;
import com.winhmm.myspotify.service.ListeningHistoryService;
import com.winhmm.myspotify.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/*
    ===================================================================
    API "Nghe gần đây" - lịch sử nghe của người đang đăng nhập.
    ===================================================================

    VD: GET /api/users/me/history?page=0&size=20

    - Mọi role dùng được, chỉ cần đăng nhập.
    - "me": luôn là lịch sử của CHÍNH người gọi (lấy từ token),
      không xem được lịch sử người khác bằng cách đổi id trên URL.
    - Lịch sử quá 90 ngày tự bị MongoDB xóa.

    JSON:
    {
        "content": [
            { "song": { "id": 15, "title": "Chiều Mưa", ... }, "playedAt": "2026-10-12T22:55:10" }
        ],
        "page": 0, "size": 20, "totalElements": 1, "totalPages": 1
    }
*/
@RestController
@RequestMapping("/api/users/me/history")
public class ListeningHistoryController {
    private final ListeningHistoryService listeningHistoryService;
    private final UserService userService;

    public ListeningHistoryController(ListeningHistoryService listeningHistoryService,
                                      UserService userService) {
        this.listeningHistoryService = listeningHistoryService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<ListeningHistoryResponse>> getMyHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = userService.getByEmail(authentication.getName()).getId();
        return ResponseEntity.ok(listeningHistoryService.getMyHistory(userId, page, size));
    }
}