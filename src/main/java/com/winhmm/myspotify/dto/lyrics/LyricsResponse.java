package com.winhmm.myspotify.dto.lyrics;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    - LocalDateTime: kiểu ngày giờ của Java (năm, tháng, ngày, giờ, phút, giây), không kèm múi giờ.
    - List         : kiểu danh sách của Java.
    LyricLine nằm cùng package dto.lyrics nên không cần import.
    (Class cùng package dùng được ngay, chỉ class ở package khác mới phải import.)
*/
import java.time.LocalDateTime;
import java.util.List;

/*
    =====================================================================
    LYRICSRESPONSE - LỜI BÀI HÁT TRẢ VỀ FRONTEND
    =====================================================================
    Dùng cho cả 3 API trả lời bài hát (xem LyricsController):
        GET /api/songs/{songId}/lyrics          → người nghe xem lời
        GET /api/artist/songs/{songId}/lyrics   → Artist xem lời bài của mình để sửa
        PUT /api/artist/songs/{songId}/lyrics   → Artist lưu lời, trả về lời vừa lưu

    Ví dụ JSON (lời có thời gian):
        {
            "songId": 9,
            "synced": true,
            "content": "[00:27.80] Ngày xưa lúc thơ bé, ta thường nhìn mưa hát\n[00:32.90] Và khi cơn mưa...",
            "lines": [
                { "timeMs": 27800, "text": "Ngày xưa lúc thơ bé, ta thường nhìn mưa hát" },
                { "timeMs": 32900, "text": "Và khi cơn mưa như nặng hạt, lại có bóng nước chợt tan" }
            ],
            "updatedAt": "2026-10-21T12:40:00"
        }

    Bài chưa có lời (vẫn trả 200, không báo lỗi):
        { "songId": 9, "synced": false, "content": "", "lines": [], "updatedAt": null }

    -----------------------------------------------------
    Các trường
    -----------------------------------------------------
    - songId   : id bài hát.
    - synced   : true  = mọi câu có thời gian (chạy chữ theo nhạc được),
                 false = lời tĩnh hoặc chưa có lời.
    - content  : lời dạng văn bản LRC, đã được chuẩn hóa: bỏ dòng trống, bỏ dòng thông tin
                 như [ar:...] [ti:...], đưa mọi thời gian về cùng dạng [mm:ss.xx].
                 Frontend dùng để điền lại vào ô nhập khi Artist bấm sửa lời.
    - lines    : lời đã tách sẵn từng câu (mỗi phần tử là 1 LyricLine).
                 Trình phát nhạc dùng mảng này để hiện và tô sáng câu đang hát.
    - updatedAt: lần lưu gần nhất, null nếu chưa có lời.

    Vì sao trả cả content lẫn lines, trông như trùng nhau?
    - content dành cho ô nhập lời của Artist (sửa dạng văn bản, giữ nguyên định dạng LRC).
    - lines dành cho trình phát nhạc (đã tách sẵn, frontend không phải tự đọc định dạng LRC).

    -----------------------------------------------------
    Vì sao bài chưa có lời vẫn trả 200?
    -----------------------------------------------------
    - Người nghe mở bài nào frontend cũng gọi API lời. Phần lớn bài có thể chưa có lời,
      trả 400 thì frontend phải bắt lỗi mỗi lần, console đầy lỗi đỏ.
    - Frontend chỉ cần: lines.length === 0 → hiện "Chưa có lời cho bài hát này".

    -----------------------------------------------------
    Các kiểu dữ liệu được đổi sang JSON thế nào?
    -----------------------------------------------------
        Java                          JSON
        Long songId                   số: 9
        boolean synced                true hoặc false
        String content                chuỗi trong dấu nháy kép (ký tự xuống dòng thành \n)
        List<LyricLine> lines         mảng [ ... ], mỗi LyricLine thành 1 object { "timeMs", "text" }
        LocalDateTime updatedAt       chuỗi dạng "năm-tháng-ngàyTgiờ:phút:giây", VD "2026-10-21T12:40:00"
                                      (giờ của máy chủ, không kèm múi giờ)
        giá trị null                  null

    List<LyricLine> là 1 đối tượng chứa nhiều đối tượng khác bên trong.
    Jackson tự đi vào từng phần tử, gọi getter của LyricLine để tạo object con.

    -----------------------------------------------------
    Đối tượng bất biến, tạo bằng constructor
    -----------------------------------------------------
    Giống LyricLine: mọi thuộc tính là final, không có setter, tạo 1 lần rồi chỉ đọc.
    5 trường nên dùng constructor, không cần Builder (Builder dành cho DTO nhiều trường như SongResponse).
*/
public class LyricsResponse {
    /*
        -----------------------------------------------------
        5 thuộc tính, đều private final (gán 1 lần trong constructor)
        -----------------------------------------------------
        - Long songId             : Long để đồng bộ với kiểu id của entity Song.
        - boolean synced          : boolean (chữ b thường), kiểu nguyên thủy chỉ có true hoặc false,
                                    không bao giờ null. Bài chưa có lời thì là false.
        - String content          : chuỗi LRC đã chuẩn hóa. Bài chưa có lời thì là "" (không phải null),
                                    để frontend điền thẳng vào ô nhập mà không phải kiểm tra null.
        - List<LyricLine> lines   : danh sách câu. Bài chưa có lời thì là danh sách rỗng [] (không phải null),
                                    để frontend dùng lines.length mà không bị lỗi.
        - LocalDateTime updatedAt : thời điểm lưu gần nhất, lấy từ cột updated_at của bảng song_lyrics.
                                    Bài chưa có lời thì là null (chưa từng lưu).
    */
    private final Long songId;
    private final boolean synced;
    private final String content;
    private final List<LyricLine> lines;
    private final LocalDateTime updatedAt;

    /*
        -----------------------------------------------------
        Constructor 5 tham số
        -----------------------------------------------------
        Được gọi trong LyricsService.toResponse(...):
        - Bài chưa có lời:
              new LyricsResponse(song.getId(), false, "", new ArrayList<>(), null)
        - Bài đã có lời:
              new LyricsResponse(song.getId(), synced, lyrics.getContent(), lines, lyrics.getUpdatedAt())

        Phải truyền đủ 5 giá trị theo đúng thứ tự khai báo: songId, synced, content, lines, updatedAt.
        Constructor dài nên xuống dòng giữa danh sách tham số cho dễ đọc, Java không quan tâm xuống dòng.
    */
    public LyricsResponse(Long songId, boolean synced, String content,
                          List<LyricLine> lines, LocalDateTime updatedAt) {
        this.songId = songId;
        this.synced = synced;
        this.content = content;
        this.lines = lines;
        this.updatedAt = updatedAt;
    }

    /*
        -----------------------------------------------------
        Getter (Jackson gọi để tạo JSON)
        -----------------------------------------------------
        getSongId() → "songId", getContent() → "content", getLines() → "lines",
        getUpdatedAt() → "updatedAt".

        isSynced() thay vì getSynced():
        Với thuộc tính kiểu boolean (nguyên thủy), quy ước đặt tên getter là is + tên thuộc tính.
        Đọc lên giống câu hỏi "is synced?" (có đồng bộ không?).
        Jackson hiểu quy ước này: isSynced() → bỏ chữ "is" → key "synced" trong JSON.
    */
    public Long getSongId() { return songId; }
    public boolean isSynced() { return synced; }
    public String getContent() { return content; }
    public List<LyricLine> getLines() { return lines; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}