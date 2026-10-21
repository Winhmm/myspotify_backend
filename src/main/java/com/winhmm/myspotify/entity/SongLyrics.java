package com.winhmm.myspotify.entity;

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
*/
@Entity
@Table(name = "song_lyrics")
public class SongLyrics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
        ---------------------------------------------------------------
        Bài hát sở hữu lời này
        ---------------------------------------------------------------
        Quan hệ một - một (@OneToOne): 1 bài hát có tối đa 1 lời, 1 lời thuộc đúng 1 bài.
    */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id", nullable = false, unique = true)
    private Song song;

    /*
        ---------------------------------------------------------------
        Toàn bộ lời bài hát (chuẩn LRC hoặc chữ thường)
        ---------------------------------------------------------------
    */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /*
        ---------------------------------------------------------------
        Thời điểm lưu gần nhất
        ---------------------------------------------------------------
    */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }

    public Song getSong() { return song; }
    public void setSong(Song song) { this.song = song; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
