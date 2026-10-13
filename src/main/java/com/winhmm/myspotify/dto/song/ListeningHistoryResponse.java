package com.winhmm.myspotify.dto.song;

import java.time.LocalDateTime;

/*
    ===================================================================
    ListeningHistoryResponse - 1 dòng trong mục "Nghe gần đây".
    ===================================================================

    Gồm 2 phần:
    - song    : thông tin bài hát (dùng lại SongResponse của người nghe).
    - playedAt: lúc người dùng bắt đầu nghe.

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

    Dữ liệu đến từ 2 database:
    - playedAt lấy từ MongoDB (collection listening_history).
    - song lấy từ MySQL (bảng songs), luôn là thông tin MỚI NHẤT
      (Artist đổi tên bài → lịch sử hiển thị tên mới).
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
