package com.winhmm.myspotify.repository;

import com.winhmm.myspotify.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/*
    =====================================================================
    PACKAGE REPOSITORY - THỨ TỰ ĐỌC
    =====================================================================

    Đọc từ dễ tới khó, file sau dùng lại kiến thức của file trước:
        1. GenreRepository            (file này): repository là gì, JpaRepository,
                                       cách Spring đọc tên method (derived query)
        2. SongLyricsRepository       : Optional, tìm theo thuộc tính của quan hệ (song.id)
        3. UserRepository             : exists, List, tên method dài nhiều điều kiện
                                       (Top10, Containing, IgnoreCase, Not)
        4. OtpVerificationRepository  : findFirst, OrderBy, truyền cả entity làm tham số
        5. SongRepository             : tự viết JPQL bằng @Query, phân trang Page/Pageable,
                                       @Modifying (câu UPDATE), countQuery
        6. ListeningHistoryRepository : repository cho MongoDB (database khác hẳn MySQL)
        7. PlaylistRepository         : file giữ chỗ, chưa dùng (chờ làm chức năng Playlist)

    =====================================================================
    GENREREPOSITORY - TRUY CẬP BẢNG GENRES (MYSQL)
    =====================================================================

    Đọc bảng genres (danh sách thể loại: Pop, Rap, Ballad...).

    Được dùng ở:
    - SongService.getGenres()        → GET /api/genres: frontend hiện danh sách thể loại để chọn.
    - ArtistSongService (getGenre)   → findById(genreId) khi Artist đăng hoặc sửa bài,
                                       kiểm tra thể loại có tồn tại không ("Genre does not exist").
    Không có API thêm, sửa, xóa thể loại: thể loại do chủ dự án thêm thẳng vào database bằng SQL.

    =====================================================================
    REPOSITORY LÀ GÌ? (ĐỌC NẾU MỚI HỌC)
    =====================================================================

    Repository là lớp duy nhất nói chuyện trực tiếp với database.
    Service gọi Repository, không bao giờ tự viết SQL.

        Controller ──► Service ──► Repository ──► MySQL
                                   (file này)

    Lợi ích: mọi câu truy vấn của 1 bảng nằm gọn 1 chỗ.
    Service chỉ việc gọi genreRepository.findAllByOrderByNameAsc(),
    không cần biết bên dưới là MySQL hay database khác, câu SQL viết thế nào.

    =====================================================================
    VÌ SAO LÀ INTERFACE, KHÔNG CÓ CODE BÊN TRONG?
    =====================================================================

    Mình chỉ KHAI BÁO method (tên, tham số, kiểu trả về), không viết thân hàm.
    Spring Data JPA tự viết phần code thực thi khi ứng dụng khởi động:

        1. Spring tìm mọi interface kế thừa JpaRepository trong dự án.
        2. Với mỗi interface, Spring tự tạo 1 object thật (proxy) có đủ code cho mọi method.
        3. Object đó được đăng ký làm bean → tiêm vào Service qua constructor:
               public SongService(..., GenreRepository genreRepository) { ... }
        → Không cần viết "implements GenreRepository" ở đâu cả,
          cũng không cần đặt annotation @Repository.

    Nếu đặt tên method sai (VD findAllByOrderByNamAsc, thiếu chữ "e"),
    ứng dụng báo lỗi ngay lúc khởi động, kiểu:
        No property 'nam' found for type 'Genre'
    → Biết sai ngay, không phải đợi tới lúc có người gọi API.

    -----------------------------------------------------
    extends JpaRepository<Genre, Long>
    -----------------------------------------------------
    2 kiểu trong dấu <> (generic):
    - Genre: entity mà repository này quản lý (bảng genres).
    - Long : kiểu của khóa chính (Genre.id).

    Được tặng sẵn nhiều method, không cần khai báo:
        save(genre)          → INSERT (chưa có id) hoặc UPDATE (đã có id)
        findById(3L)         → SELECT ... WHERE id = 3, trả Optional<Genre> (xem SongLyricsRepository)
        findAllById(ids)     → SELECT ... WHERE id IN (...)
        findAll()            → SELECT toàn bộ bảng
        existsById(3L)       → có dòng id = 3 không (true hoặc false)
        count()              → đếm số dòng
        delete(genre)        → DELETE
    VD ArtistSongService gọi genreRepository.findById(genreId) mà file này không hề khai báo findById.

    =====================================================================
    DERIVED QUERY: SPRING ĐỌC TÊN METHOD RỒI TỰ SINH CÂU TRUY VẤN
    =====================================================================

    Tên method được tách thành từng phần theo quy ước:
        find...By      → SELECT (chữ giữa find và By là tùy ý: findAllBy, findGenresBy... đều được)
        exists...By    → có hay không (trả boolean)
        count...By     → đếm
        Sau By         → điều kiện WHERE, tên thuộc tính Java viết hoa chữ đầu: ByName, ByEmail...
        And, Or        → nối nhiều điều kiện
        OrderBy...Asc  → sắp xếp tăng dần, Desc → giảm dần
    Các repository sau sẽ gặp thêm: First, Top10, Not, Containing, IgnoreCase, True, False...

    Lưu ý: dùng tên THUỘC TÍNH trong class Java (name, artistName...),
    không phải tên cột trong database (artist_name).
*/
public interface GenreRepository extends JpaRepository<Genre, Long> {

    /*
        -----------------------------------------------------
        Mọi thể loại, sắp xếp theo tên A → Z
        -----------------------------------------------------
        Tên method được Spring tách thành từng phần:
            findAll        → lấy tất cả
            By             → (không có điều kiện nào phía sau → không có WHERE)
            OrderByNameAsc → ORDER BY name ASC (theo thuộc tính name, tăng dần)

        Câu SQL tương đương:
            SELECT * FROM genres ORDER BY name ASC

        Vì sao không dùng findAll() có sẵn?
        findAll() không hứa trước thứ tự (thường theo id, tức thứ tự lúc thêm vào database).
        Frontend cần danh sách xếp theo tên để người dùng dễ tìm: "Ballad, EDM, Pop, Rap..."

        Thứ tự chữ cái do MySQL quyết định theo collation (bộ quy tắc so sánh chữ) của cột,
        mặc định MySQL 8 không phân biệt hoa thường và không phân biệt dấu
        → "ballad" và "Ballad" đứng cạnh nhau, "Ấn tượng" xếp gần nhóm chữ "A".

        Trả về List<Genre>: không có thể loại nào → danh sách rỗng [], không bao giờ null.
    */
    List<Genre> findAllByOrderByNameAsc();
}