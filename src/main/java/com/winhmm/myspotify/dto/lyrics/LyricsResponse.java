package com.winhmm.myspotify.dto.lyrics;

import java.time.LocalDateTime;
import java.util.List;

/*
    =====================================================================
    LYRICSRESPONSE - LỜI BÀI HÁT TRẢ VỀ FRONTEND
    =====================================================================

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

    Các trường:
    - synced   : true = mọi câu có thời gian (chạy chữ theo nhạc được),
                 false = lời tĩnh hoặc chưa có lời.
    - content  : lời dạng văn bản LRC, đã được chuẩn hóa (bỏ dòng trống, đưa thời gian về dạng [mm:ss.xx]).
                 Frontend dùng để điền lại vào ô nhập khi Artist bấm sửa lời.
    - lines    : lời đã tách sẵn từng câu. Trình phát nhạc dùng mảng này để hiện và tô sáng câu đang hát.
    - updatedAt: lần lưu gần nhất, null nếu chưa có lời.

    Vì sao bài chưa có lời vẫn trả 200?
    - Người nghe mở bài nào frontend cũng gọi API lời. Phần lớn bài có thể chưa có lời,
      trả 400 thì frontend phải bắt lỗi mỗi lần, console đầy lỗi đỏ.
    - Frontend chỉ cần: lines.length === 0 → hiện "Chưa có lời cho bài hát này".

    5 trường nên dùng constructor, không cần Builder.
*/
public class LyricsResponse {
    private final Long songId;
    private final boolean synced;
    private final String content;
    private final List<LyricLine> lines;
    private final LocalDateTime updatedAt;

    public LyricsResponse(Long songId, boolean synced, String content,
                          List<LyricLine> lines, LocalDateTime updatedAt) {
        this.songId = songId;
        this.synced = synced;
        this.content = content;
        this.lines = lines;
        this.updatedAt = updatedAt;
    }

    public Long getSongId() { return songId; }
    public boolean isSynced() { return synced; }
    public String getContent() { return content; }
    public List<LyricLine> getLines() { return lines; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
