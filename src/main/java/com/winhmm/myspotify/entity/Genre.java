package com.winhmm.myspotify.entity;

import jakarta.persistence.*;

/*
    =====================================================================
    GENRE - ENTITY ÁNH XẠ BẢNG genres (MYSQL)
    =====================================================================

    Mỗi dòng của bảng = 1 thể loại nhạc, VD Pop, Ballad, Rap, Chill...

    Các cột:
        id   : khóa chính, tự tăng
        name : tên thể loại, không trùng, tối đa 50 ký tự

    =====================================================================
    DỮ LIỆU CÓ SẴN, KHÔNG CÓ API THÊM SỬA XÓA
    =====================================================================

    - 25 thể loại được thêm 1 lần bằng câu INSERT (chạy tay trong MySQL).
    - Muốn thêm thể loại mới sau này: chạy thêm 1 câu INSERT, không phải sửa code.
    - Vì vậy class này chỉ có getter, không có setter:
      backend chỉ đọc bảng genres, không bao giờ ghi.

    =====================================================================
    VÌ SAO DÙNG BẢNG MÀ KHÔNG DÙNG ENUM?
    =====================================================================

    Nếu dùng enum Genre { POP, BALLAD, ... } với @Enumerated(EnumType.STRING):
    - Hibernate tạo cột kiểu enum('POP','BALLAD',...) của MySQL.
    - Thêm 1 thể loại = sửa code, build lại, và chạy ALTER TABLE trên VPS
      (vì VPS dùng ddl-auto=validate).
    Dùng bảng: thêm thể loại chỉ là thêm 1 dòng dữ liệu.

    =====================================================================
    QUAN HỆ VỚI BÀI HÁT
    =====================================================================

    1 thể loại có nhiều bài, 1 bài có đúng 1 thể loại.
    Quan hệ được khai báo ở phía Song (Song.genre, cột songs.genre_id).
    Class này không cần biết danh sách bài hát của mình.
*/
@Entity
@Table(name = "genres")
public class Genre {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    public Long getId() { return id; }
    public String getName() { return name; }
}
