package com.winhmm.myspotify.dto.lyrics;

/*
    File này không có dòng import nào: chỉ dùng Long và String,
    2 class có sẵn trong gói java.lang mà Java tự import cho mọi file.
*/

/*
    =====================================================================
    LYRICLINE - 1 CÂU HÁT TRONG LỜI BÀI HÁT
    =====================================================================

    Ví dụ JSON:
        { "timeMs": 27800, "text": "Ngày xưa lúc thơ bé, ta thường nhìn mưa hát" }

    -----------------------------------------------------
    Class này được dùng ở đâu?
    -----------------------------------------------------
    Artist gửi lời lên dạng 1 chuỗi dài (LyricsRequest.content).
    LyricsService đọc từng dòng của chuỗi đó, mỗi dòng tạo ra 1 đối tượng LyricLine:
        "[00:27.80] Ngày xưa lúc thơ bé"  →  new LyricLine(27800L, "Ngày xưa lúc thơ bé")
    Danh sách các LyricLine được trả về frontend trong trường "lines" của LyricsResponse.

    Class này không phải entity, không có bảng riêng trong database.
    Database chỉ lưu nguyên chuỗi lời (bảng song_lyrics), mỗi lần trả về thì tách lại thành từng câu.

    -----------------------------------------------------
    timeMs: thời điểm câu hát bắt đầu, đơn vị mili giây
    -----------------------------------------------------
    1 giây = 1000 mili giây. VD 27800 ms = 27,8 giây.
    - Dùng mili giây vì thẻ <audio> của trình duyệt có audio.currentTime (đơn vị giây, có phần lẻ),
      frontend chỉ cần so currentTime * 1000 với timeMs để biết đang tới câu nào:
          câu đang hát = câu cuối cùng có timeMs <= audio.currentTime * 1000
      VD đang ở giây 30,5 → 30500 → câu 27800 đã bắt đầu, câu 32900 chưa tới → đang hát câu 27800.
    - Số nguyên dễ so sánh hơn chuỗi "[00:27.80]", không phải tự tách phút, giây ở frontend.
    - Lời không có thời gian → timeMs = null.
    - Kiểu Long (không phải long) để chứa được giá trị null:
        long: kiểu nguyên thủy, luôn có giá trị, không thể null (mặc định là 0).
        Long: kiểu đối tượng bọc ngoài long, có thể null.
      Nếu dùng long, lời không có thời gian sẽ bị hiểu nhầm thành "câu bắt đầu ở giây 0".

    -----------------------------------------------------
    text: nội dung câu hát
    -----------------------------------------------------
    Đã bỏ phần thời gian và dấu cách thừa ở 2 đầu.
    - Có thể là chuỗi rỗng "" nếu dòng đó chỉ có thời gian, VD "[01:02.00]".
      Đây là cách LRC đánh dấu đoạn nhạc không lời, frontend có thể hiện "♪".

    -----------------------------------------------------
    Không có setter: tạo 1 lần bằng constructor rồi chỉ đọc
    -----------------------------------------------------
    Đối tượng tạo xong thì không thay đổi được nữa, gọi là đối tượng bất biến (immutable).
    - 2 thuộc tính có từ khóa final: chỉ được gán đúng 1 lần (trong constructor).
      Viết thêm dòng gán lại giá trị ở chỗ khác → IntelliJ báo lỗi ngay, không compile được.
    - Không có setter, nên code bên ngoài cũng không có cách nào sửa.
    Vì sao làm vậy? Câu hát tách ra rồi thì chỉ để đọc và trả về, không ai cần sửa.
    Không sửa được thì không lo bị sửa nhầm ở đâu đó.

    Jackson vẫn tạo JSON bình thường, vì Jackson chỉ cần getter để đọc giá trị
    (class này chỉ dùng để trả về, Jackson không bao giờ phải tạo nó từ JSON).
*/
public class LyricLine {
    /*
        private final:
        - private: chỉ dùng được bên trong class này.
        - final  : gán đúng 1 lần trong constructor, sau đó không đổi được.
    */
    private final Long timeMs;
    private final String text;

    /*
        -----------------------------------------------------
        Constructor: nơi duy nhất gán giá trị cho 2 thuộc tính
        -----------------------------------------------------
        LyricsService gọi: new LyricLine(timeMs, text)
        - Lời có thời gian    : new LyricLine(27800L, "Ngày xưa lúc thơ bé")
        - Lời không thời gian : new LyricLine(null, "Ngày xưa lúc thơ bé")
        Chữ L sau 27800 nghĩa là số kiểu long (Java tự đổi thành Long khi truyền vào).

        this.timeMs = timeMs: gán tham số vào thuộc tính cùng tên (xem RegisterRequest).
    */
    public LyricLine(Long timeMs, String text) {
        this.timeMs = timeMs;
        this.text = text;
    }

    /*
        -----------------------------------------------------
        Getter
        -----------------------------------------------------
        - Jackson gọi để tạo JSON: getTimeMs() → "timeMs", getText() → "text".
        - LyricsService cũng gọi, VD line.getTimeMs() != null để biết lời có thời gian hay không.
    */
    public Long getTimeMs() { return timeMs; }
    public String getText() { return text; }
}