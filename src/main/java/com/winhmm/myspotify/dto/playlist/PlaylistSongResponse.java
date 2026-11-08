package com.winhmm.myspotify.dto.playlist;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    - SongResponse : DTO bài hát có sẵn của module Song (dùng lại, không viết mới).
    - LocalDateTime: kiểu ngày giờ, dùng cho addedAt.
*/
import com.winhmm.myspotify.dto.song.SongResponse;

import java.time.LocalDateTime;

/*
    =====================================================================
    PLAYLISTSONGRESPONSE - 1 BÀI TRONG PLAYLIST
    =====================================================================

    Dùng trong danh sách "songs" của PlaylistDetailResponse.

    Gồm 2 phần:
    - song   : thông tin bài hát, dùng lại SongResponse có sẵn
               (cùng cấu trúc với GET /api/songs → frontend dùng lại được component hiển thị bài).
    - addedAt: lúc bài được thêm vào playlist (frontend hiện "Ngày thêm" như Spotify).

    Cách làm giống ListeningHistoryResponse (song + playedAt): bọc SongResponse bên trong,
    không chép lại từng trường của bài hát.

    JSON:
    {
        "song": { "id": 15, "title": "Chiều Mưa", "artistName": "Mây Chiều", ... },
        "addedAt": "2026-11-04T08:00:00"
    }

    -----------------------------------------------------
    Object Java được đổi thành JSON thế nào?
    -----------------------------------------------------
    Controller trả object về, Spring dùng thư viện Jackson đổi thành JSON:
    - Mỗi getter public thành 1 key: getSong() → "song", getAddedAt() → "addedAt"
      (bỏ chữ "get", chữ cái đầu viết thường).
    - Giá trị là object (SongResponse) → thành 1 khối { ... } lồng bên trong.
    - LocalDateTime → chuỗi dạng "năm-tháng-ngàyTgiờ:phút:giây".
    Không có getter thì không xuất hiện trong JSON.

    Chỉ 2 trường nên dùng constructor thường, không cần Builder.
    Không có setter: tạo xong là cố định, chỉ để Jackson đọc getter ra JSON.
*/
public class PlaylistSongResponse {
    private SongResponse song;
    private LocalDateTime addedAt;

    public PlaylistSongResponse(SongResponse song, LocalDateTime addedAt) {
        this.song = song;
        this.addedAt = addedAt;
    }

    public SongResponse getSong() { return song; }
    public LocalDateTime getAddedAt() { return addedAt; }
}