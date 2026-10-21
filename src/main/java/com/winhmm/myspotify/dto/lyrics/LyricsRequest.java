package com.winhmm.myspotify.dto.lyrics;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/*
    =====================================================================
    LYRICSREQUEST - DỮ LIỆU ARTIST GỬI LÊN KHI LƯU LỜI BÀI HÁT
    =====================================================================

    Ví dụ JSON:
        {
            "content": "[00:27.80] Ngày xưa lúc thơ bé, ta thường nhìn mưa hát\n[00:32.90] Và khi cơn mưa..."
        }

    "\n" là ký tự xuống dòng: mỗi câu hát nằm trên 1 dòng.

    content nhận 2 kiểu:
    - Có thời gian (chuẩn LRC): mọi dòng bắt đầu bằng [phút:giây.phần trăm giây].
      Dùng cho cả 2 cách của frontend:
        + Artist nhớ thời gian → dán thẳng lời có sẵn [mm:ss.xx].
        + Artist không nhớ → vừa nghe vừa bấm "Gắn mốc", frontend tự ghép thành chuỗi LRC rồi gửi.
    - Không có thời gian: mỗi dòng chỉ có chữ (lời tĩnh, không chạy theo nhạc).
*/
public class LyricsRequest {
    @NotBlank(message = "Lyrics must not be empty")
    @Size(max = 10000, message = "Lyrics must not exceed 10000 characters")
    private String content;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
