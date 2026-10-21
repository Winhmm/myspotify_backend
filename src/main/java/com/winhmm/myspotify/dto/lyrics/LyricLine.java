package com.winhmm.myspotify.dto.lyrics;

/*
    =====================================================================
    LYRICLINE - 1 CÂU HÁT TRONG LỜI BÀI HÁT
    =====================================================================

    Ví dụ JSON:
        { "timeMs": 27800, "text": "Ngày xưa lúc thơ bé, ta thường nhìn mưa hát" }

    timeMs: thời điểm câu hát bắt đầu, đơn vị mili giây (27800 = 27,8 giây).
    - Dùng mili giây vì thẻ <audio> của trình duyệt có audio.currentTime (giây),
      frontend chỉ cần so currentTime * 1000 với timeMs để biết đang tới câu nào.
    - Lời không có thời gian → timeMs = null.
    - Kiểu Long (không phải long) để chứa được giá trị null.

    text: nội dung câu hát.
    - Có thể là chuỗi rỗng "" nếu dòng đó chỉ có thời gian, VD "[01:02.00]".
      Đây là cách LRC đánh dấu đoạn nhạc không lời, frontend có thể hiện "♪".

    Không có setter: tạo 1 lần bằng constructor rồi chỉ đọc.
*/
public class LyricLine {
    private final Long timeMs;
    private final String text;

    public LyricLine(Long timeMs, String text) {
        this.timeMs = timeMs;
        this.text = text;
    }

    public Long getTimeMs() { return timeMs; }
    public String getText() { return text; }
}
