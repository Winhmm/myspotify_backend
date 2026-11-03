package com.winhmm.myspotify.repository;

import com.winhmm.myspotify.entity.SongLyrics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


/*
    =====================================================================
    SONGLYRICSREPOSITORY - TRUY VẤN BẢNG song_lyrics
    =====================================================================

    (Repository là gì, JpaRepository, cách Spring đọc tên method: xem GenreRepository.
     Thứ tự đọc package repository: xem GenreRepository.)

    Đọc và ghi lời bài hát. Mỗi bài có tối đa 1 dòng lời
    (cột song_id có UNIQUE, quan hệ @OneToOne trong entity SongLyrics).

    Chỉ được dùng trong LyricsService:
        findBySongId (khai báo bên dưới) → xem lời, lưu lời, xóa lời
        save(lyrics)   (có sẵn)          → thêm mới hoặc cập nhật lời
        delete(lyrics) (có sẵn)          → xóa lời

    extends JpaRepository<SongLyrics, Long>: quản lý entity SongLyrics, khóa chính kiểu Long.
*/
public interface SongLyricsRepository extends JpaRepository<SongLyrics, Long> {

    /*
        -----------------------------------------------------
        Tìm lời của 1 bài theo id bài hát
        -----------------------------------------------------
        Câu SQL tương đương:
            SELECT * FROM song_lyrics WHERE song_id = ?

        -----------------------------------------------------
        Spring hiểu "SongId" thế nào?
        -----------------------------------------------------
        Entity SongLyrics KHÔNG có thuộc tính tên songId, chỉ có thuộc tính song (kiểu Song).
        Spring tự tách:
            SongId → song . id   (thuộc tính song, rồi đi tiếp vào thuộc tính id của Song)
        Giống viết lyrics.getSong().getId() trong Java.
        Hibernate thấy id của song chính là cột song_id nằm ngay trong bảng song_lyrics
        → so thẳng cột đó, không cần JOIN sang bảng songs.

        -----------------------------------------------------
        Optional<SongLyrics>: "có thể có, có thể không"
        -----------------------------------------------------
        Bài chưa có lời → không tìm thấy dòng nào.
        Thay vì trả null (dễ quên kiểm tra → NullPointerException), trả về Optional:
        1 "hộp" có thể chứa 1 SongLyrics hoặc rỗng. Người gọi BẮT BUỘC phải mở hộp:

            .orElse(new SongLyrics())       có thì lấy, không có thì tạo mới (lưu lời: thêm hoặc sửa)
            .orElse(null)                   có thì lấy, không có thì null   (xem lời: chưa có lời)
            .orElseThrow(() -> new IllegalArgumentException("Song has no lyrics"))
                                            có thì lấy, không có thì ném lỗi → 400 (xóa lời)
        Cả 3 cách trên đều được dùng trong LyricsService.

        Vì sao Optional mà không phải List?
        song_id là UNIQUE → mỗi bài nhiều nhất 1 dòng lời. Trả về Optional nói rõ "0 hoặc 1 kết quả".
        (Nếu lỡ có 2 dòng khớp, Spring sẽ báo lỗi thay vì âm thầm lấy 1 dòng.)

        Lưu ý: hàm này không kiểm tra bài có được xem không (đã phát hành chưa, của ai...).
        LyricsService kiểm tra bài trước (qua SongRepository), rồi mới gọi hàm này.
    */
    Optional<SongLyrics> findBySongId(Long songId);
}