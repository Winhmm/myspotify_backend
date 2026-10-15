package com.winhmm.myspotify.dto.song;

import java.time.LocalDateTime;

/*
    =====================================================================
    ListeningHistoryResponse - 1 BÀI TRONG MỤC "NGHE GẦN ĐÂY"
    =====================================================================

    API: GET /api/users/me/history → trả về mảng tối đa 10 phần tử kiểu này.

    Gồm 2 phần:
    - song    : thông tin bài hát (dùng lại SongResponse của người nghe).
    - playedAt: lần nghe gần nhất của người dùng với bài này.

    JSON:
    {
        "song": {
            "id": 15,
            "title": "Chiều Mưa",
            "audioUrl": "/uploads/songs/audio/3f2a9c1e.mp3",
            "artistName": "Mây Chiều",
            ...
        },
        "playedAt": "2026-10-12T22:55:10"
    }

    "song" có cấu trúc giống hệt 1 phần tử trong GET /api/songs
    → frontend dùng lại được component hiển thị bài hát đã có.

    Dữ liệu đến từ 2 database:
    - playedAt lấy từ MongoDB (collection listening_history).
    - song lấy từ MySQL (bảng songs), luôn là thông tin mới nhất
      (Artist đổi tên bài → lịch sử hiển thị tên mới).

    Được tạo trong ListeningHistoryService.getRecentSongs.
    DTO response → chỉ có getter, không có setter.
*/
public class ListeningHistoryResponse {
    private SongResponse song;
    private LocalDateTime playedAt;

    public ListeningHistoryResponse(SongResponse song, LocalDateTime playedAt) {
        this.song = song;
        this.playedAt = playedAt;
    }

    public SongResponse getSong() { return song; }
    public LocalDateTime getPlayedAt() { return playedAt; }
}
