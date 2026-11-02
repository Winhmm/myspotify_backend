package com.winhmm.myspotify.dto.lyrics;

/*
    -----------------------------------------------------
    Package dto.lyrics: nên đọc file này trước tiên
    -----------------------------------------------------
    Package dto.lyrics có 3 file, đọc theo đường đi của dữ liệu:
        1. LyricsRequest  → Artist gửi lời bài hát lên (file này)
        2. LyricLine      → 1 câu hát sau khi backend tách lời ra từng câu
        3. LyricsResponse → lời bài hát backend trả về frontend (chứa danh sách LyricLine)

    Các khái niệm cơ bản (package, import, DTO, annotation, validation, getter, setter, Jackson)
    đã giải thích chi tiết trong RegisterRequest (package dto.auth). 3 file ở đây chỉ giải thích thêm
    những điểm mới.

    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    - NotBlank: không được null, rỗng "" hoặc chỉ toàn dấu cách.
    - Size    : độ dài chuỗi nằm trong khoảng cho phép.
    Cả 2 thuộc bộ kiểm tra dữ liệu Jakarta Bean Validation (xem RegisterRequest).
*/
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/*
    =====================================================================
    LYRICSREQUEST - DỮ LIỆU ARTIST GỬI LÊN KHI LƯU LỜI BÀI HÁT
    =====================================================================
    Dùng cho: PUT /api/artist/songs/{songId}/lyrics (xem LyricsController.saveLyrics)

    Ví dụ JSON:
        {
            "content": "[00:27.80] Ngày xưa lúc thơ bé, ta thường nhìn mưa hát\n[00:32.90] Và khi cơn mưa..."
        }

    Toàn bộ lời bài hát nằm trong 1 chuỗi duy nhất là "content".
    "\n" là ký tự xuống dòng: mỗi câu hát nằm trên 1 dòng.
    Trong JSON không được xuống dòng thật bên trong 1 chuỗi, nên phải viết \n.
    Jackson đọc \n trong JSON thành 1 ký tự xuống dòng thật trong chuỗi Java.

    -----------------------------------------------------
    LRC là gì?
    -----------------------------------------------------
    LRC là định dạng lời bài hát phổ biến (file .lrc trên mạng, karaoke, Zing MP3...).
    Mỗi dòng gồm thời điểm câu hát bắt đầu, đặt trong ngoặc vuông, rồi tới nội dung câu:
        [00:27.80] Ngày xưa lúc thơ bé, ta thường nhìn mưa hát
         │  │  │
         │  │  └─ phần trăm giây: 80 → 0,80 giây
         │  └──── giây: 27
         └─────── phút: 00
    → Câu này bắt đầu ở giây thứ 27,8 của bài hát.
    Trình phát nhạc dựa vào thời điểm này để tô sáng đúng câu đang hát (chữ chạy theo nhạc).

    -----------------------------------------------------
    content nhận 2 kiểu
    -----------------------------------------------------
    - Có thời gian (chuẩn LRC): mọi dòng bắt đầu bằng [phút:giây.phần trăm giây].
      Dùng cho cả 2 cách của frontend:
        + Artist nhớ thời gian → dán thẳng lời có sẵn [mm:ss.xx].
        + Artist không nhớ → vừa nghe vừa bấm "Gắn mốc", frontend tự ghép thành chuỗi LRC rồi gửi.
    - Không có thời gian: mỗi dòng chỉ có chữ (lời tĩnh, không chạy theo nhạc).
    Trộn lẫn (dòng có, dòng không) thì bị từ chối (xem LyricsService).

    -----------------------------------------------------
    DTO kiểm tra hình thức, Service kiểm tra nội dung
    -----------------------------------------------------
    - DTO này chỉ kiểm tra 2 điều đơn giản: có gửi lời không, có quá dài không.
    - Mọi quy tắc còn lại cần đọc từng dòng và cần biết độ dài bài hát, nên nằm trong LyricsService:
        + Tối đa 300 dòng, mỗi dòng tối đa 200 ký tự.
        + Tất cả các dòng có thời gian, hoặc không dòng nào có.
        + Giây từ 00 đến 59, thời gian dòng sau lớn hơn dòng trước, không vượt quá độ dài bài.
      Sai → 400 kèm số dòng bị sai, VD "Line 5: time must be greater than the previous line".
*/
public class LyricsRequest {
    /*
        -----------------------------------------------------
        content: toàn bộ lời bài hát
        -----------------------------------------------------
        1. @NotBlank(message = "Lyrics must not be empty")
           Không gửi, gửi chuỗi rỗng, hoặc chỉ toàn dấu cách và xuống dòng → 400 "Lyrics must not be empty".
           (Muốn xóa lời thì dùng API DELETE riêng, không gửi chuỗi rỗng.)

        2. @Size(max = 10000, message = "Lyrics must not exceed 10000 characters")
           Tối đa 10.000 ký tự cho cả bài (tính cả dấu cách, ký tự xuống dòng và phần [mm:ss.xx]).
           Đủ dư cho 1 bài hát bình thường: 1 bài dài khoảng 60 câu,
           mỗi câu khoảng 60 ký tự kể cả thời gian → khoảng 3.600 ký tự.

           Vì sao vẫn cần giới hạn khi LyricsService đã giới hạn 300 dòng, mỗi dòng 200 ký tự?
           - Chặn sớm: chuỗi quá dài bị từ chối ngay, không phải đọc từng dòng.
           - Thực tế 10.000 ký tự là giới hạn chặt hơn: 300 dòng x 200 ký tự = 60.000 ký tự,
             sẽ bị @Size chặn trước khi tới LyricsService.
           - Lời được lưu vào cột kiểu TEXT trong bảng song_lyrics (tối đa 65.535 byte).
             1 ký tự chiếm tối đa 4 byte, nên 10.000 ký tự (cộng thêm phần backend chuẩn hóa thời gian)
             vẫn chắc chắn lưu vừa.
    */
    @NotBlank(message = "Lyrics must not be empty")
    @Size(max = 10000, message = "Lyrics must not exceed 10000 characters")
    private String content;

    /*
        -----------------------------------------------------
        Getter và setter
        -----------------------------------------------------
        - Setter: Jackson gọi khi đổi JSON thành đối tượng ("content" → setContent).
        - Getter: LyricsController gọi request.getContent() để truyền xuống LyricsService.saveLyrics.
        Class không viết constructor nào, nên Java tự tạo constructor không tham số cho Jackson dùng.
        Cách đọc từng dòng: xem RegisterRequest.
    */
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}