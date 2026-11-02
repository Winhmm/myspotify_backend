package com.winhmm.myspotify.dto.song;

/*
    -----------------------------------------------------
    Dòng import
    -----------------------------------------------------
    - LocalDateTime: kiểu ngày giờ của Java, dùng cho playedAt.
    SongResponse nằm cùng package dto.song nên không cần import.
*/
import java.time.LocalDateTime;

/*
    =====================================================================
    LISTENINGHISTORYRESPONSE - 1 BÀI TRONG MỤC "NGHE GẦN ĐÂY"
    =====================================================================

    API: GET /api/users/me/history (xem ListeningHistoryController)
    → trả về mảng tối đa 10 phần tử kiểu này, bài nghe gần nhất đứng đầu.

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
    Đây cũng là 1 DTO lồng nhau: thuộc tính song có kiểu SongResponse,
    Jackson tự gọi getter của SongResponse để tạo object "song" bên trong (xem ArtistSongsResponse).

    Vì sao lồng song thay vì để phẳng (id, title, ..., playedAt cùng 1 cấp)?
    - Dùng lại nguyên SongResponse, không phải khai báo lại 13 trường.
    - Tách rõ "thông tin bài hát" và "thông tin lượt nghe".

    Dữ liệu đến từ 2 database:
    - playedAt lấy từ MongoDB (collection listening_history).
    - song lấy từ MySQL (bảng songs), luôn là thông tin mới nhất
      (Artist đổi tên bài → lịch sử hiển thị tên mới).

    Được tạo trong ListeningHistoryService.getRecentSongs:
        new ListeningHistoryResponse(SongMapper.toSongResponse(song), playedAt)
    DTO response → chỉ có getter, không có setter.
*/
public class ListeningHistoryResponse {
    /*
        - song    : bài hát, kiểu SongResponse (1 DTO bên trong DTO này).
        - playedAt: thời điểm nghe gần nhất, theo giờ máy chủ.
                    MongoDB lưu theo giờ UTC, Spring tự đổi về giờ máy chủ khi đọc lên
                    (xem ListeningHistory trong package document).
    */
    private SongResponse song;
    private LocalDateTime playedAt;

    /*
        Constructor: gán đủ 2 giá trị khi tạo đối tượng (xem GenreResponse).
    */
    public ListeningHistoryResponse(SongResponse song, LocalDateTime playedAt) {
        this.song = song;
        this.playedAt = playedAt;
    }

    /*
        Getter: Jackson gọi để tạo JSON: getSong() → "song", getPlayedAt() → "playedAt".
    */
    public SongResponse getSong() { return song; }
    public LocalDateTime getPlayedAt() { return playedAt; }
}