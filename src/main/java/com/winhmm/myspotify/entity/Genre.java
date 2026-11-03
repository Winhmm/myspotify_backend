package com.winhmm.myspotify.entity;

/*
    -----------------------------------------------------
    Dòng import
    -----------------------------------------------------
    (Entity là gì, các annotation @Entity, @Id, @Column: xem giải thích chi tiết trong User.
     Thứ tự đọc package entity: xem User.)

    - jakarta.persistence.*: mượn tất cả annotation của JPA (Entity, Table, Id, Column...).
*/
import jakarta.persistence.*;

/*
    =====================================================================
    GENRE - ENTITY ÁNH XẠ BẢNG genres (MYSQL)
    =====================================================================

    Mỗi dòng của bảng = 1 thể loại nhạc, VD Pop, Ballad, Rap, Chill...

    Các cột:
        id   : khóa chính, tự tăng
        name : tên thể loại, không trùng, tối đa 50 ký tự

    Ví dụ dữ liệu:
        id | name
        1  | Acoustic
        2  | Ballad
        24 | Chill

    Đây là entity đơn giản nhất của project: 2 thuộc tính, không có quan hệ, không có setter.

    =====================================================================
    DỮ LIỆU CÓ SẴN, KHÔNG CÓ API THÊM SỬA XÓA
    =====================================================================

    - 25 thể loại được thêm 1 lần bằng câu INSERT (chạy tay trong MySQL).
    - Muốn thêm thể loại mới sau này: chạy thêm 1 câu INSERT, không phải sửa code, VD:
          INSERT INTO genres (name) VALUES ('Lofi');
      (Không ghi id, MySQL tự cấp.)
    - Vì vậy class này chỉ có getter, không có setter:
      backend chỉ đọc bảng genres, không bao giờ ghi.
    - Đọc ở đâu: GET /api/genres (GenreController), và lúc Artist đăng bài
      (ArtistSongService tìm thể loại theo genreId, không có → "Genre does not exist").

    =====================================================================
    VÌ SAO DÙNG BẢNG MÀ KHÔNG DÙNG ENUM?
    =====================================================================

    Nếu dùng enum Genre { POP, BALLAD, ... } với @Enumerated(EnumType.STRING):
    - Hibernate tạo cột kiểu enum('POP','BALLAD',...) của MySQL.
    - Thêm 1 thể loại = sửa code, build lại, và tự chạy ALTER TABLE
      (trên VPS vì ddl-auto=validate, ở máy local vì ddl-auto=update không sửa kiểu cột đã có).
    Dùng bảng: thêm thể loại chỉ là thêm 1 dòng dữ liệu.

    Quy tắc chọn: danh sách cố định, gắn với logic code (VD Role, SongStatus) → enum.
    Danh sách có thể thêm bớt theo thời gian, code không cần biết từng giá trị (thể loại) → bảng.

    =====================================================================
    QUAN HỆ VỚI BÀI HÁT
    =====================================================================

    1 thể loại có nhiều bài, 1 bài có đúng 1 thể loại.
    Quan hệ được khai báo ở phía Song (Song.genre, cột songs.genre_id).
    Class này không cần biết danh sách bài hát của mình.
    Muốn lấy bài theo thể loại thì hỏi SongRepository (GET /api/songs?genreId=24).
*/
@Entity
@Table(name = "genres")
public class Genre {
    /*
        id: khóa chính, MySQL tự tăng (giải thích @Id, @GeneratedValue: xem User).
        Đây chính là genreId mà frontend gửi lên khi đăng bài (UploadSongRequest).
    */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
        name: tên thể loại.
        - nullable = false: bắt buộc có tên.
        - unique = true   : không 2 thể loại trùng tên (INSERT trùng sẽ bị MySQL chặn).
        - length = 50     : VARCHAR(50).
    */
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    /*
        -----------------------------------------------------
        Chỉ có getter
        -----------------------------------------------------
        Không có setter, vậy Hibernate gán giá trị vào id, name bằng cách nào khi đọc từ database?
        → @Id đặt trên thuộc tính nên Hibernate dùng field access:
          ghi thẳng vào thuộc tính private (bằng cơ chế reflection của Java), không cần setter.
        Code của mình thì không có cách nào sửa id, name → đúng ý "chỉ đọc".

        Constructor rỗng: Java tự tạo (class không khai báo constructor nào), Hibernate dùng để tạo đối tượng.
    */
    public Long getId() { return id; }
    public String getName() { return name; }
}