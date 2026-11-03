package com.winhmm.myspotify.entity;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    (Entity là gì, các annotation cơ bản: xem User. Quan hệ và LAZY: xem OtpVerification, Song.
     Thứ tự đọc package entity: xem User.)

    - jakarta.persistence.*: mượn tất cả annotation của JPA.
    - LocalDateTime        : kiểu ngày giờ của Java, dùng cho updatedAt.
*/
import jakarta.persistence.*;

import java.time.LocalDateTime;

/*
    =====================================================================
    SONGLYRICS - ENTITY ÁNH XẠ BẢNG song_lyrics (MYSQL)
    =====================================================================

    Mỗi dòng của bảng = lời của 1 bài hát.

    Các cột:
        id          : khóa chính, tự tăng
        song_id     : bài hát sở hữu lời này (khóa ngoại trỏ tới songs.id), không trùng
        content     : toàn bộ lời bài hát dạng văn bản (kiểu TEXT)
        updated_at  : thời điểm lưu gần nhất

    Ví dụ dữ liệu:
        id | song_id | content                                          | updated_at
        1  | 15      | [00:27.80] Ngày xưa lúc thơ bé...\n[00:32.90]... | 2026-10-20 21:10:00
    (\n là ký tự xuống dòng: mỗi câu 1 dòng.)

    =====================================================================
    LỜI ĐƯỢC LƯU THẾ NÀO?
    =====================================================================

    Lưu nguyên cả bài trong 1 ô content, mỗi câu 1 dòng, theo chuẩn LRC
    (chuẩn phổ biến của file lời nhạc có thời gian):

        [00:27.80] Ngày xưa lúc thơ bé, ta thường nhìn mưa hát
        [00:32.90] Và khi cơn mưa như nặng hạt, lại có bóng nước chợt tan
        [00:38.10] Mẹ bảo ta rằng bong bóng mưa

        [00:27.80] nghĩa là phút 00, giây 27, phần trăm giây 80 → 27,8 giây.

    Lời không có thời gian thì mỗi dòng chỉ có chữ:

        Ngày xưa lúc thơ bé, ta thường nhìn mưa hát
        Và khi cơn mưa như nặng hạt, lại có bóng nước chợt tan

    Vì sao lưu cả bài trong 1 ô, không tách mỗi câu thành 1 dòng trong bảng?
    - Đơn giản: lưu, sửa, xóa lời chỉ là đọc ghi đúng 1 dòng.
    - Artist luôn sửa cả bài 1 lần (dán lại hoặc đánh mốc lại), không sửa lẻ từng câu.
    - LRC là chuẩn chung, frontend hay ứng dụng khác đều đọc được.
    Khi trả về frontend, LyricsService tách content thành từng câu (timeMs + text).

    =====================================================================
    VÌ SAO TÁCH BẢNG RIÊNG, KHÔNG THÊM CỘT VÀO BẢNG songs?
    =====================================================================

    - Lời có thể dài vài nghìn ký tự. Các truy vấn danh sách bài hát (trang chủ, tìm kiếm)
      không cần lời, để riêng thì không phải đọc kèm khối chữ lớn này.
    - Bảng songs giữ nguyên, không phải sửa entity Song, SongMapper hay các DTO đã có.
    - Bài chưa có lời → không có dòng nào trong bảng này.

    =====================================================================
    LƯU VÀ SỬA LỜI (LyricsService.saveLyrics)
    =====================================================================

        SongLyrics lyrics = songLyricsRepository.findBySongId(songId).orElse(new SongLyrics());
        lyrics.setSong(song);
        lyrics.setContent(...);
        songLyricsRepository.save(lyrics);

    - Bài đã có lời → tìm thấy dòng cũ (có id) → save() chạy UPDATE.
    - Bài chưa có lời → new SongLyrics() (id = null) → save() chạy INSERT.
    → 1 hàm dùng cho cả "thêm lời" lẫn "sửa lời".
*/
@Entity
@Table(name = "song_lyrics")
public class SongLyrics {
    /*
        id: khóa chính, MySQL tự tăng (xem User).
    */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
        -----------------------------------------------------
        Bài hát sở hữu lời này
        -----------------------------------------------------
        Quan hệ một - một (@OneToOne): 1 bài hát có tối đa 1 lời, 1 lời thuộc đúng 1 bài.

        So với @ManyToOne (VD Song.artist):
        - @ManyToOne: nhiều dòng được phép trỏ tới cùng 1 bài.
        - @OneToOne : chỉ 1 dòng được trỏ tới 1 bài.
        Trong database cả 2 đều chỉ là 1 cột khóa ngoại. Điểm khác nằm ở unique = true bên dưới:
        cột song_id không được trùng → database chặn trường hợp 1 bài có 2 lời.

        @JoinColumn(name = "song_id", nullable = false, unique = true):
        - song_id      : tên cột khóa ngoại trỏ tới songs.id.
        - nullable     : lời bắt buộc thuộc về 1 bài.
        - unique = true: mỗi bài chỉ có tối đa 1 dòng trong bảng này.
          Tự tạo index → findBySongId (tìm lời theo bài) rất nhanh.

        fetch = FetchType.LAZY: đọc lời chưa đọc kèm bài hát ngay (xem OtpVerification).
        @OneToOne mặc định là EAGER, nên phải ghi rõ LAZY.

        Quan hệ chỉ khai báo ở phía này. Entity Song không có thuộc tính lyrics,
        muốn lấy lời của 1 bài thì hỏi SongLyricsRepository.findBySongId(songId).

        Xóa hẳn 1 bài bằng SQL thì phải xóa dòng lời của bài đó trước (lỗi khóa ngoại),
        giống bảng playlist_songs (xem PlaylistSong).
    */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id", nullable = false, unique = true)
    private Song song;

    /*
        -----------------------------------------------------
        Toàn bộ lời bài hát (chuẩn LRC hoặc chữ thường)
        -----------------------------------------------------
        columnDefinition = "TEXT": tự ghi kiểu cột SQL thay vì để Hibernate chọn.
        - Không ghi gì: Hibernate tạo VARCHAR(255), quá ngắn cho 1 bài hát.
        - TEXT của MySQL chứa tối đa 65.535 byte.
          LyricsRequest giới hạn 10.000 ký tự; mỗi ký tự tối đa 4 byte (bảng mã utf8mb4, mặc định của MySQL 8)
          → khoảng 40.000 byte, nằm gọn trong TEXT, kể cả lời toàn chữ có dấu hay emoji.
    */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /*
        -----------------------------------------------------
        Thời điểm lưu gần nhất
        -----------------------------------------------------
        Giá trị do hàm onSave() bên dưới tự gán. Không có setter.
    */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /*
        -----------------------------------------------------
        Lifecycle callback (xem giải thích chi tiết trong Song)
        -----------------------------------------------------
        1 hàm gắn cả 2 annotation:
        - @PrePersist: chạy trước INSERT (lần đầu thêm lời).
        - @PreUpdate : chạy trước UPDATE (sửa lời).
        → Lần nào lưu cũng cập nhật updatedAt, không cần 2 hàm riêng như Song.
        Lưu lại đúng nội dung cũ (không thay đổi gì) thì Hibernate không chạy UPDATE,
        nên updatedAt cũng không đổi.
    */
    @PrePersist
    @PreUpdate
    protected void onSave() {
        updatedAt = LocalDateTime.now();
    }

    /*
        -----------------------------------------------------
        Getter và setter
        -----------------------------------------------------
        - setSong, setContent: LyricsService dùng khi lưu lời.
        - Không có setId, setUpdatedAt: MySQL và onSave() tự gán.
        Constructor rỗng: Java tự tạo, dùng ở new SongLyrics() trong LyricsService và cho Hibernate.
    */
    public Long getId() { return id; }

    public Song getSong() { return song; }
    public void setSong(Song song) { this.song = song; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
}