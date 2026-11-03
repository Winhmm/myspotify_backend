package com.winhmm.myspotify.repository;

import com.winhmm.myspotify.entity.Song;
import com.winhmm.myspotify.enums.SongStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Collection;

/*
    =====================================================================
    SONGREPOSITORY - TRUY CẬP BẢNG SONGS (MYSQL)
    =====================================================================

    (Repository là gì, vì sao là interface, JpaRepository tặng sẵn những gì,
     cách Spring đọc tên method: xem GenreRepository.
     Thứ tự đọc package repository: xem GenreRepository.)

    Các truy vấn:
        Người nghe : findPublished, findPublishedByGenre, searchPublished, findPublishedById,
                     findPublishedByArtist, incrementPlayCount
        Artist     : findByArtistIdAndStatusNotOrderByCreatedAtDesc, findByIdAndArtistId
        Admin      : findByStatusNot, findByStatus
        Lịch sử nghe, gợi ý: findPublishedByIds

    Repository chỉ chứa truy vấn bài hát. Truy vấn người dùng nằm ở UserRepository.

    Được dùng ở: SongService, ArtistSongService, AdminSongService, LyricsService,
    ListeningHistoryService, RecommendationService.

    =====================================================================
    2 CÁCH VIẾT TRUY VẤN
    =====================================================================

    Cách 1 - Đặt tên method theo quy ước (derived query):
        Spring đọc tên method rồi tự sinh câu truy vấn (chi tiết: xem GenreRepository).
        VD: findByStatus(SongStatus status, Pageable pageable)
            → SELECT * FROM songs WHERE status = ? (kèm phân trang)
        Các từ khóa hay dùng: findBy, And, Or, Not, OrderBy...Desc, Top10, Containing, IgnoreCase.
        Hợp với truy vấn đơn giản.

    Cách 2 - Tự viết JPQL bằng @Query:
        Dùng khi điều kiện phức tạp (JOIN nhiều bảng, OR lồng nhau...),
        đặt tên method theo quy ước sẽ dài không đọc nổi.
        Khi có @Query, Spring KHÔNG đọc tên method nữa → tên đặt tùy ý, miễn dễ hiểu
        (VD findPublished, searchPublished không theo quy ước nào cả).

    =====================================================================
    JPQL KHÁC SQL THẾ NÀO?
    =====================================================================

    JPQL (Java Persistence Query Language) viết theo tên class và tên thuộc tính Java,
    không phải tên bảng và tên cột:

        JPQL:  SELECT s FROM Song s WHERE s.releaseAt <= :now
        SQL :  SELECT * FROM songs s WHERE s.release_at <= ?

        - "Song" là tên class (không phải bảng "songs").
        - "s" là tên gọi tắt (alias) đặt cho Song trong câu truy vấn, giống biến trong vòng lặp.
        - "SELECT s": lấy cả object Song (không phải liệt kê từng cột).
        - "s.releaseAt" là thuộc tính Java (không phải cột "release_at").
        - "s.artist.accountStatus": đi theo quan hệ như gọi getter trong Java
          (s.getArtist().getAccountStatus()) → Hibernate tự JOIN sang bảng users.

    Hibernate dịch JPQL sang SQL đúng của MySQL.
    Lợi ích: đổi tên cột trong @Column không phải sửa câu truy vấn.
    Viết sai tên thuộc tính trong JPQL → ứng dụng báo lỗi ngay lúc khởi động (giống derived query).

    =====================================================================
    THAM SỐ :now, :keyword VÀ CHỐNG SQL INJECTION
    =====================================================================

    Dấu hai chấm là tham số có tên, được điền bằng @Param("now") ở tham số method:
        @Query("... WHERE s.releaseAt <= :now")
        Page<Song> findPublished(@Param("now") LocalDateTime now, ...)
                                  ↑ chữ "now" trong @Param phải trùng chữ sau dấu hai chấm

    Hibernate gửi truy vấn tới MySQL dưới dạng PreparedStatement:
        - Câu SQL gửi đi chỉ có dấu ?, VD: ... WHERE LOWER(title) LIKE ?
        - Giá trị keyword được gửi riêng, sau khi MySQL đã hiểu xong cấu trúc câu lệnh.
    → MySQL không bao giờ coi keyword là một phần của câu lệnh.
      keyword = "'; DROP TABLE songs; --" chỉ được coi là chuỗi cần tìm, không chạy được.
    (Tuyệt đối không ghép chuỗi kiểu "... LIKE '%" + keyword + "%'": đó chính là lỗ hổng SQL injection.)

    Vì sao truyền :now từ Java thay vì dùng CURRENT_TIMESTAMP của MySQL?
    - Giờ do Java tính (LocalDateTime.now()) cùng nguồn với giờ lưu vào database lúc tạo bài
      → không lệch khi múi giờ MySQL khác múi giờ ứng dụng.
    - Khi test có thể truyền 1 thời điểm cố định → kết quả kiểm soát được.

    =====================================================================
    PHÂN TRANG: Pageable VÀ Page
    =====================================================================

    Pageable pageable (tham số): gói thông tin "lấy trang nào, mỗi trang mấy bài, sắp xếp ra sao".
        Do Service tạo, VD trong SongService:
            PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "releaseAt"))
            → trang 0 (trang đầu), 20 bài, mới phát hành trước.
        Spring tự thêm vào cuối câu SQL: ORDER BY release_at DESC LIMIT 20 OFFSET 0
        (trang 2 → OFFSET 40: bỏ qua 40 bài đầu).
        Câu @Query không cần viết ORDER BY hay LIMIT, Pageable lo hết.
        → Cùng 1 câu truy vấn, Service khác nhau sắp xếp khác nhau được:
          SongService sắp theo releaseAt (mới nhất), RecommendationService sắp theo playCount (nghe nhiều nhất).

    Page<Song> (kiểu trả về): 1 trang kết quả, gồm danh sách bài + thông tin tổng
        (tổng số bài, tổng số trang...). Đổi sang JSON bằng SongMapper.toPageResponse.
        Để biết tổng, Spring chạy 2 câu SQL:
            1. Lấy 20 bài của trang hiện tại.
            2. SELECT COUNT(...) để biết tổng số bài → tính totalPages.
        (Riêng trang đầu mà có ít hơn 20 bài, Spring biết luôn tổng số nên bỏ qua câu đếm.)
*/
public interface SongRepository extends JpaRepository<Song, Long> {
    /*
        =====================================================================
        ĐIỀU KIỆN "BÀI ĐÃ PHÁT HÀNH"
        =====================================================================

        Người nghe chỉ thấy bài thỏa đủ 3 điều kiện (UC20 - UC25):
        1. s.status = APPROVED : Admin đã duyệt, Artist không ẩn.
        2. s.releaseAt <= :now : đã tới giờ phát hành.
        3. Artist chính ACTIVE và vẫn có role ARTIST.
           → Artist chính bị khóa, xóa tài khoản, hoặc bị thu hồi quyền
             → mọi bài của họ tự ẩn, không cần sửa từng bài. Mở khóa lại → bài tự hiện lại.

        -----------------------------------------------------
        Vì sao viết thành 1 hằng số String?
        -----------------------------------------------------
        Điều kiện này được dùng trong 6 câu truy vấn bên dưới.
        Viết 1 lần, ghép vào bằng dấu +:
            @Query("SELECT s FROM Song s WHERE " + PUBLISHED_CONDITION)
        Sau này đổi quy tắc "đã phát hành" (VD thêm điều kiện) → chỉ sửa 1 chỗ, 6 câu cùng đổi theo.

        - Biến khai báo trong interface tự động là public static final (hằng số), không cần ghi ra.
        - Giá trị trong annotation (như @Query) bắt buộc là hằng số biết trước lúc biên dịch.
          PUBLISHED_CONDITION được ghép từ các chuỗi cố định → là hằng số → ghép vào @Query được.
        - Mỗi đoạn chuỗi kết thúc bằng 1 dấu cách ("... APPROVED ") để khi ghép lại các từ không dính nhau
          (thiếu dấu cách → "APPROVEDAND" → lỗi cú pháp lúc khởi động).

        Giá trị enum trong JPQL viết đầy đủ tên package:
            com.winhmm.myspotify.enums.SongStatus.APPROVED
        để Hibernate biết chắc đó là hằng số enum Java, không phải tên 1 thuộc tính hay 1 chuỗi.
    */
    String PUBLISHED_CONDITION =
            "s.status = com.winhmm.myspotify.enums.SongStatus.APPROVED "
                    + "AND s.releaseAt <= :now "
                    + "AND s.artist.accountStatus = com.winhmm.myspotify.enums.AccountStatus.ACTIVE "
                    + "AND s.artist.role = com.winhmm.myspotify.enums.Role.ARTIST";

    /*
        =====================================================================
        ĐIỀU KIỆN "ARTIST FEAT ĐANG HOẠT ĐỘNG"
        =====================================================================

        1. Tài khoản đang ACTIVE.
        2. Vẫn có role ARTIST.

        Áp dụng cho f (người feat) trong searchPublished: tìm theo tên người feat
        chỉ tính người đang hoạt động. Khớp đúng bộ lọc activeFeaturedArtists trong SongMapper
        → người bị khóa không hiện trong danh sách feat, cũng không làm bài hiện ra khi tìm tên họ.
    */
    String ACTIVE_FEATURED_CONDITION =
            "f.accountStatus = com.winhmm.myspotify.enums.AccountStatus.ACTIVE "
                    + "AND f.role = com.winhmm.myspotify.enums.Role.ARTIST";

    /*
        =====================================================================
        NGƯỜI NGHE (UC20 - UC25)
        =====================================================================
    */

    /*
        ---------------------------------------------------------------
        UC20 - Danh sách bài đã phát hành, có phân trang
        ---------------------------------------------------------------
        Dùng ở:
        - SongService.getPublishedSongs (GET /api/songs, không lọc thể loại).
        - RecommendationService: bài nghe nhiều nhất (khi bài đang nghe không có thể loại).

        Câu SQL Hibernate sinh ra, gần tương đương:
            SELECT s.* FROM songs s
            JOIN users a ON a.id = s.artist_id                    ← do s.artist.accountStatus
            WHERE s.status = 'APPROVED' AND s.release_at <= ?
              AND a.account_status = 'ACTIVE' AND a.role = 'ARTIST'
            ORDER BY s.release_at DESC LIMIT 20 OFFSET 0          ← do Pageable
        (Phân trang Pageable và Page: xem phần đầu file.)
        Index idx_songs_status_release (status, release_at) trong entity Song khớp 2 điều kiện đầu.
    */
    @Query("SELECT s FROM Song s WHERE " + PUBLISHED_CONDITION)
    Page<Song> findPublished(@Param("now") LocalDateTime now, Pageable pageable);

    /*
        ---------------------------------------------------------------
        UC20 (lọc theo thể loại) - Bài đã phát hành của 1 thể loại, có phân trang
        ---------------------------------------------------------------
        Giống findPublished, thêm điều kiện s.genre.id = :genreId.
        Dùng ở:
        - SongService.getPublishedSongs (GET /api/songs?genreId=3).
        - RecommendationService: bài nghe nhiều nhất cùng thể loại.

        s.genre.id: đọc thẳng cột genre_id của bảng songs, Hibernate không cần JOIN sang bảng genres.

        Câu đếm (cho totalPages) Spring tự sinh: SELECT COUNT(s) ... cùng điều kiện WHERE.
        Không cần viết countQuery riêng vì câu này không có DISTINCT hay LEFT JOIN.

        genreId không tồn tại → không bài nào khớp → trang rỗng, không báo lỗi.
    */
    @Query("SELECT s FROM Song s WHERE s.genre.id = :genreId AND " + PUBLISHED_CONDITION)
    Page<Song> findPublishedByGenre(@Param("genreId") Long genreId,
                                    @Param("now") LocalDateTime now,
                                    Pageable pageable);

    /*
        ---------------------------------------------------------------
        UC21 - Tìm kiếm bài đã phát hành theo keyword
        ---------------------------------------------------------------
        Dùng ở SongService.searchSongs (GET /api/songs/search?keyword=may).
        keyword rỗng → SongService trả danh sách bình thường, không gọi tới hàm này.
        keyword được trim() (bỏ dấu cách 2 đầu) trước khi truyền vào.

        Tìm keyword trong 3 chỗ (chỉ cần khớp 1 trong 3):
        - Tên bài hát.
        - Nghệ danh Artist chính.
        - Nghệ danh Artist feat (chỉ tính người đang hoạt động).

        Các thành phần câu truy vấn:

        LEFT JOIN s.featuredArtists f
            - Ghép mỗi bài với từng người feat của nó (qua bảng song_featured_artists).
            - LEFT JOIN (không phải JOIN thường): bài không có ai feat vẫn được giữ lại,
              khi đó f = NULL. Dùng JOIN thường → bài không feat ai bị loại khỏi kết quả,
              tìm theo tên bài sẽ không ra.

            VD bài 15 feat 2 người (id 12, 13), bài 16 không feat ai. Sau LEFT JOIN:
                s = 15, f = 12
                s = 15, f = 13
                s = 16, f = NULL

        LOWER(...) LIKE LOWER(CONCAT('%', :keyword, '%'))
            - CONCAT('%', 'may', '%') → '%may%'.
            - LIKE '%may%': chứa "may" ở bất kỳ vị trí nào ("Mây Chiều", "Bài Hát May Mắn").
            - LOWER 2 vế: đổi hết về chữ thường → không phân biệt hoa thường.
            - "may" khớp được "Mây" (không phân biệt dấu) là nhờ collation mặc định
              của MySQL 8 (utf8mb4_0900_ai_ci), không phải nhờ LOWER.
              (ai = accent insensitive: không phân biệt dấu, ci = case insensitive: không phân biệt hoa thường)

        OR (LOWER(f.artistName) LIKE ... AND ACTIVE_FEATURED_CONDITION)
            - Dấu ngoặc quan trọng: điều kiện "đang hoạt động" chỉ gắn với vế tìm theo người feat,
              không ảnh hưởng 2 vế tìm theo tên bài và tên Artist chính.
            - Cả 3 vế OR được bọc chung trong 1 cặp ngoặc rồi mới AND với PUBLISHED_CONDITION:
                  WHERE <đã phát hành> AND ( <tên bài> OR <tên Artist chính> OR (<tên feat> AND <feat hoạt động>) )
              Thiếu cặp ngoặc ngoài → AND được tính trước OR → bài CHƯA phát hành cũng lọt vào kết quả.

        DISTINCT s
            - Bài feat 2 người → LEFT JOIN tạo ra 2 dòng cho cùng 1 bài (VD bài 15 ở trên).
            - DISTINCT gộp lại → mỗi bài chỉ xuất hiện 1 lần.

        countQuery (câu đếm riêng cho phân trang):
            - Mặc định Spring tự sinh câu đếm từ câu chính, nhưng với DISTINCT và JOIN
              câu tự sinh có thể đếm sai hoặc lỗi.
            - COUNT(DISTINCT s): đếm số bài khác nhau (không phải số dòng sau JOIN)
              → totalElements chính xác.
            - Điều kiện WHERE phải giống hệt câu chính, nếu không số trang sẽ lệch với dữ liệu.

        Cú pháp @Query(value = "...", countQuery = "..."):
            Khi annotation có từ 2 thuộc tính trở lên, phải ghi rõ tên từng thuộc tính.
            (Chỉ có 1 thuộc tính value thì viết tắt được: @Query("...").)

        Giới hạn đã biết:
            Ký tự '%' và '_' trong keyword có nghĩa đặc biệt với LIKE
            ('%' = chuỗi bất kỳ, '_' = 1 ký tự bất kỳ). Tìm "100%" sẽ khớp rộng hơn mong đợi.
            Không gây lỗi bảo mật (vẫn chống SQL injection), chỉ làm kết quả rộng hơn.
            Muốn chính xác tuyệt đối thì escape 2 ký tự này trong Service trước khi truyền vào.
            (Khác UserRepository dùng Containing: Spring tự escape 2 ký tự này.)
    */
    @Query(value = "SELECT DISTINCT s FROM Song s LEFT JOIN s.featuredArtists f "
            + "WHERE " + PUBLISHED_CONDITION + " AND ("
            + "LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR LOWER(s.artist.artistName) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR (LOWER(f.artistName) LIKE LOWER(CONCAT('%', :keyword, '%')) AND " + ACTIVE_FEATURED_CONDITION + "))",
            countQuery = "SELECT COUNT(DISTINCT s) FROM Song s LEFT JOIN s.featuredArtists f "
                    + "WHERE " + PUBLISHED_CONDITION + " AND ("
                    + "LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')) "
                    + "OR LOWER(s.artist.artistName) LIKE LOWER(CONCAT('%', :keyword, '%')) "
                    + "OR (LOWER(f.artistName) LIKE LOWER(CONCAT('%', :keyword, '%')) AND " + ACTIVE_FEATURED_CONDITION + "))")
    Page<Song> searchPublished(@Param("keyword") String keyword,
                               @Param("now") LocalDateTime now,
                               Pageable pageable);

    /*
        ---------------------------------------------------------------
        UC22 - Lấy 1 bài đã phát hành theo id
        ---------------------------------------------------------------
        Khác findById có sẵn: findById trả về MỌI bài (kể cả PENDING, HIDDEN, DELETED),
        hàm này chỉ trả bài người nghe được phép thấy.

        Dùng ở:
        - SongService.getPublishedSong: xem chi tiết 1 bài (GET /api/songs/{songId}),
          và kiểm tra trước khi tính lượt nghe (POST /api/songs/{songId}/play).
        - RecommendationService: bài đang nghe phải là bài công khai.

        Optional rỗng (bài không tồn tại, chưa duyệt, bị ẩn, chưa tới giờ, Artist bị khóa...)
        → Service ném lỗi "Song does not exist" cho MỌI trường hợp, không nói rõ lý do
          → người ngoài không dò được bài nào đang chờ duyệt hay bị ẩn.
    */
    @Query("SELECT s FROM Song s WHERE s.id = :songId AND " + PUBLISHED_CONDITION)
    Optional<Song> findPublishedById(@Param("songId") Long songId,
                                     @Param("now") LocalDateTime now);

    /*
        ---------------------------------------------------------------
        UC25 - Bài đã phát hành mà Artist đó là Artist chính hoặc được feat
        ---------------------------------------------------------------
        Dùng ở:
        - SongService.getArtistSongs: trang cá nhân của 1 Artist (GET /api/songs/artists/{artistId}).
        - RecommendationService: bài nghe nhiều nhất của cùng Artist.

        s.artist.id = :artistId OR f.id = :artistId
            → Artist đó đăng bài (Artist chính), HOẶC có mặt trong danh sách feat.
            VD Artist 12 được Artist 7 mời feat bài 15 → bài 15 cũng hiện trên trang của Artist 12.

        LEFT JOIN, DISTINCT, countQuery: cùng lý do như searchPublished.
            (Bài của chính Artist 12 mà feat 2 người → 2 dòng sau JOIN → DISTINCT gộp lại.)

        Không cần ACTIVE_FEATURED_CONDITION ở đây: f.id = :artistId chính là Artist của trang đang xem,
        và SongService.getArtist đã kiểm tra Artist đó đang ACTIVE, còn role ARTIST trước khi gọi hàm này.
    */
    @Query(value = "SELECT DISTINCT s FROM Song s LEFT JOIN s.featuredArtists f "
            + "WHERE " + PUBLISHED_CONDITION
            + " AND (s.artist.id = :artistId OR f.id = :artistId)",
            countQuery = "SELECT COUNT(DISTINCT s) FROM Song s LEFT JOIN s.featuredArtists f "
                    + "WHERE " + PUBLISHED_CONDITION
                    + " AND (s.artist.id = :artistId OR f.id = :artistId)")
    Page<Song> findPublishedByArtist(@Param("artistId") Long artistId,
                                     @Param("now") LocalDateTime now,
                                     Pageable pageable);

    /*
        ---------------------------------------------------------------
        UC24 - Tăng lượt nghe bằng 1 câu UPDATE duy nhất
        ---------------------------------------------------------------
        Dùng ở SongService.play (POST /api/songs/{songId}/play), sau khi Redis xác nhận
        người này chưa nghe bài đó trong 30 giây gần nhất.

        Câu SQL tương đương:
            UPDATE songs SET play_count = play_count + 1 WHERE id = ?

        -----------------------------------------------------
        Vì sao không đọc bài lên, cộng 1, rồi save()?
        -----------------------------------------------------
        Cách đó bị mất lượt khi 2 người bấm play cùng lúc:
            Người A đọc: play_count = 100        Người B đọc: play_count = 100
            A cộng 1, lưu 101                    B cộng 1, lưu 101
            → 2 lượt nghe nhưng chỉ tăng 1.
        Câu UPDATE "play_count = play_count + 1" để MySQL tự đọc và cộng ngay trong 1 bước,
        MySQL khóa dòng đó trong lúc cập nhật → người sau phải đợi người trước xong
        → 2 lượt nghe tăng đúng 2 (xem kịch bản E2E-04 trong tài liệu test).

        Thêm 2 lợi ích:
        - Không đụng tới cột version (@Version) của bài → lượt nghe không làm Artist hay Admin
          đang sửa bài bị lỗi 409 "This data was changed by someone else".
        - Cột play_count có updatable = false trong entity Song: save() bình thường KHÔNG BAO GIỜ
          ghi đè play_count. Câu UPDATE này là đường duy nhất thay đổi lượt nghe.

        -----------------------------------------------------
        3 annotation
        -----------------------------------------------------
        @Query("UPDATE ..."): JPQL cũng viết được câu UPDATE (và DELETE), không chỉ SELECT.

        @Modifying: báo cho Spring "câu này THAY ĐỔI dữ liệu, không phải đọc".
            Thiếu → Spring chạy như câu SELECT → lỗi lúc gọi.

        @Transactional: câu thay đổi dữ liệu bắt buộc phải chạy trong 1 transaction.
            SongService.play không có @Transactional, nên đặt ở đây để hàm này tự mở transaction riêng.
            Thiếu → lỗi TransactionRequiredException → 500.

        Kiểu trả về int: số dòng bị UPDATE. Bài tồn tại → 1, không tồn tại → 0.
        (SongService không dùng tới giá trị này, vì đã kiểm tra bài tồn tại trước khi gọi.)

        Lưu ý: câu UPDATE chạy thẳng xuống MySQL, bỏ qua các object Song Hibernate đang giữ.
        Nếu ngay sau đó, trong cùng request, đọc lại song.getPlayCount() từ object cũ
        thì vẫn thấy số cũ. SongService.play không đọc lại nên không ảnh hưởng.
    */
    @Modifying
    @Transactional
    @Query("UPDATE Song s SET s.playCount = s.playCount + 1 WHERE s.id = :songId")
    int incrementPlayCount(@Param("songId") Long songId);

    /*
        =====================================================================
        ARTIST (UC28 - UC33)
        =====================================================================
    */

    /*
        ---------------------------------------------------------------
        UC28 - Bài hát của tôi: mọi trạng thái trừ DELETED, mới tải lên trước
        ---------------------------------------------------------------
        Dùng ở ArtistSongService.getMySongs (GET /api/artist/songs), truyền:
            artistId = id lấy từ token, status = DELETED

        Tên method được Spring tách thành từng phần:
            findBy               → SELECT
            ArtistId             → WHERE artist_id = ?     (thuộc tính artist, rồi id, giống SongId
                                                             trong SongLyricsRepository)
            AndStatusNot         → AND status <> ?         (Not = "khác")
            OrderByCreatedAtDesc → ORDER BY created_at DESC (mới đăng lên trước)

        Trả về List (không phân trang): mỗi Artist chỉ có vài chục bài, trả hết 1 lần cho gọn.
        Index idx_songs_artist_status (artist_id, status) trong entity Song khớp điều kiện này.
    */
    List<Song> findByArtistIdAndStatusNotOrderByCreatedAtDesc(Long artistId, SongStatus status);

    /*
        ---------------------------------------------------------------
        UC29 - UC33 - Tìm bài theo id và phải thuộc về Artist đó
        ---------------------------------------------------------------
        SQL tương đương: SELECT * FROM songs WHERE id = ? AND artist_id = ?

        Dùng mỗi khi Artist sửa, ẩn, hiện, xóa, đổi ảnh, đổi lịch, sửa lời 1 bài
        (getMySong trong ArtistSongService và LyricsService).

        Đây là lớp bảo vệ quyền sở hữu: artistId lấy từ token, không phải từ frontend gửi lên.
            Artist 7 gọi DELETE /api/artist/songs/15, mà bài 15 là của Artist 9
            → WHERE id = 15 AND artist_id = 7 → không có dòng nào → Optional rỗng
            → "Song does not exist" (không nói "không có quyền" để không lộ bài 15 có tồn tại).
        Bài đã DELETED: hàm này vẫn trả về, Service tự kiểm tra thêm và coi như không tồn tại.
    */
    Optional<Song> findByIdAndArtistId(Long id, Long artistId);

    /*
        =====================================================================
        ADMIN (UC34)
        =====================================================================
        Dùng ở AdminSongService.getSongs (GET /api/admin/songs?status=...&page=...&size=...).
        Pageable do AdminSongService tạo: sắp theo createdAt giảm dần (bài mới đăng lên trước).
        Admin thấy MỌI bài, không cần PUBLISHED_CONDITION (kể cả bài của Artist đang bị khóa).
    */

    /*
        ---------------------------------------------------------------
        UC34 - Mọi bài trừ 1 trạng thái (Service truyền DELETED)
        ---------------------------------------------------------------
        Không lọc ?status → danh sách mặc định: mọi bài trừ bài đã xóa.
        SQL tương đương: SELECT * FROM songs WHERE status <> 'DELETED' ORDER BY created_at DESC LIMIT ...
        Derived query cũng nhận Pageable được: Spring tự thêm ORDER BY, LIMIT và câu đếm.
    */
    Page<Song> findByStatusNot(SongStatus status, Pageable pageable);

    /*
        ---------------------------------------------------------------
        UC34 - Lọc đúng 1 trạng thái, VD chỉ bài PENDING đang chờ duyệt
        ---------------------------------------------------------------
        ?status=PENDING → SELECT * FROM songs WHERE status = 'PENDING' ...
        ?status=DELETED → cách duy nhất để Admin xem bài đã xóa.
    */
    Page<Song> findByStatus(SongStatus status, Pageable pageable);

    /*
        =====================================================================
        LỊCH SỬ NGHE
        =====================================================================
    */

    /*
        ---------------------------------------------------------------
        Lấy nhiều bài đã phát hành theo danh sách id, trong 1 câu truy vấn
        ---------------------------------------------------------------
        Dùng ở:
        - ListeningHistoryService: MongoDB chỉ lưu songId, phải sang MySQL lấy thông tin bài.
        - RecommendationService: dịch vụ gợi ý trả về danh sách id bài, phải lấy bài thật.

        s.id IN :songIds
            → WHERE id IN (15, 9, 22, ...): lấy 1 lần cả danh sách.
            Gọi findPublishedById lần lượt cho từng id thì 10 bài = 10 câu SQL, chậm hơn nhiều.

        Collection<Long>: kiểu cha chung của List và Set → truyền List hay Set đều được
            (ListeningHistoryService truyền keySet() của 1 Map, là 1 Set).

        Lưu ý khi dùng:
        - Kết quả KHÔNG theo thứ tự id truyền vào (MySQL trả theo thứ tự nó muốn).
          Cả 2 Service đều bỏ kết quả vào Map<id, Song> rồi duyệt lại theo thứ tự ban đầu
          (lịch sử: mới nghe trước; gợi ý: phù hợp nhất trước).
        - Bài không còn công khai (bị ẩn, bị xóa, Artist bị khóa...) tự bị loại khỏi kết quả
          nhờ PUBLISHED_CONDITION → lịch sử nghe và gợi ý không bao giờ hiện bài đã bị gỡ.
        - Danh sách id rỗng: cả 2 Service đều kiểm tra và trả kết quả rỗng trước, không gọi hàm này.
    */
    @Query("SELECT s FROM Song s WHERE s.id IN :songIds AND " + PUBLISHED_CONDITION)
    List<Song> findPublishedByIds(@Param("songIds") Collection<Long> songIds,
                                  @Param("now") LocalDateTime now);
}