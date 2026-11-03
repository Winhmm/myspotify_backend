package com.winhmm.myspotify.mapper;

/*
    Các import trong file:
        dto.song...        5 DTO mà mapper tạo ra (ArtistSummary, GenreResponse, PageResponse,
                           SongManageResponse, SongResponse)
        entity...          3 entity mà mapper đọc vào (Genre, Song, User)
        enums...           để so sánh trạng thái (AccountStatus, Role, SongStatus)
        Page               1 trang kết quả do Spring Data trả về (xem toPageResponse)
        LocalDateTime      lấy giờ hiện tại để tính released
        ArrayList, List    danh sách
        Comparator         sắp xếp danh sách
        Function           "1 hàm được truyền vào như 1 giá trị" (xem toPageResponse)
*/
import com.winhmm.myspotify.dto.song.ArtistSummary;
import com.winhmm.myspotify.dto.song.GenreResponse;
import com.winhmm.myspotify.dto.song.PageResponse;
import com.winhmm.myspotify.dto.song.SongManageResponse;
import com.winhmm.myspotify.dto.song.SongResponse;
import com.winhmm.myspotify.entity.Genre;
import com.winhmm.myspotify.entity.Song;
import com.winhmm.myspotify.entity.User;
import com.winhmm.myspotify.enums.AccountStatus;
import com.winhmm.myspotify.enums.Role;
import com.winhmm.myspotify.enums.SongStatus;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/*
    =====================================================================
    SONGMAPPER - CHUYỂN ENTITY SONG THÀNH DTO TRẢ VỀ FRONTEND
    =====================================================================

    (Mapper là gì, vì sao dùng static, vì sao constructor private: xem UserMapper.
     Thứ tự đọc package mapper: xem UserMapper.)

    Các method:
        toArtistSummary(user)      : User → ArtistSummary          (id và nghệ danh)
        toGenreResponse(genre)     : Genre → GenreResponse         (id và tên thể loại)
        toSongResponse(song)       : Song → SongResponse           (cho người nghe)
        toManageResponse(song)     : Song → SongManageResponse     (cho Artist, Admin)
        toPageResponse(page, ...)  : Page<Song> → PageResponse<T>  (1 trang kết quả)
    Và 3 hàm phụ private (chỉ dùng trong file này): genreId, genreName, activeFeaturedArtists.

    =====================================================================
    MAPPER LÀ GÌ, VÌ SAO CẦN?
    =====================================================================

    Service làm việc với entity Song (đại diện 1 dòng trong bảng songs).
    Frontend cần DTO (SongResponse, SongManageResponse): chỉ chứa đúng những gì cần hiển thị.
    Mapper là "người phiên dịch" giữa 2 bên.

        Database ──► Repository ──► Service ──► Controller ──► SongMapper ──► JSON
                       (Song)       (Song)       (Song)          (DTO)

    Ngoại lệ: 2 Service gọi SongMapper ngay bên trong:
    - ListeningHistoryService: phải ghép dữ liệu từ 2 database (lượt nghe trong MongoDB,
      bài hát trong MySQL) thành ListeningHistoryResponse.
    - RecommendationService: trả thẳng danh sách SongResponse (bài gợi ý).

    Vì sao gom vào 1 class riêng?
    - 3 controller (SongController, ArtistSongController, AdminSongController),
      GenreController và 2 Service ở trên đều cần chuyển Song → DTO.
      Viết 1 lần ở đây, mọi nơi dùng chung, không lặp code.
    - Sửa cách hiển thị (VD thêm trường mới vào JSON) → chỉ sửa 1 chỗ.

    =====================================================================
    VÌ SAO TOÀN METHOD STATIC?
    =====================================================================

    Mapper không lưu trạng thái gì (không có thuộc tính, không cần database):
    nhận Song vào, trả DTO ra, như 1 hàm toán học.
    → static: gọi thẳng SongMapper.toSongResponse(song), không cần new SongMapper()
      hay để Spring tiêm vào.

    =====================================================================
    VÌ SAO KHÔNG DÙNG THƯ VIỆN MAPSTRUCT, MODELMAPPER?
    =====================================================================

    Các thư viện đó tự sinh code chuyển đổi, gọn hơn khi có rất nhiều entity.
    Module Song chỉ có vài kiểu chuyển, và có logic riêng (lọc người feat bị khóa,
    tính sẵn released), viết tay dễ đọc và dễ kiểm soát hơn.

    =====================================================================
    MAPPER ĐỌC DỮ LIỆU LAZY: VÌ SAO KHÔNG BỊ LỖI?
    =====================================================================

    Trong entity Song, 3 quan hệ đều là LAZY (chỉ tải khi thật sự dùng tới):
        artist           (@ManyToOne, LAZY)   → người đăng bài
        genre            (@ManyToOne, LAZY)   → thể loại
        featuredArtists  (@ManyToMany, LAZY)  → danh sách người feat
    Khi Repository trả về Song, 3 thứ này CHƯA được tải, Hibernate chỉ đặt sẵn "chỗ giữ chỗ" (proxy).
    Mapper gọi song.getArtist().getArtistName() hay duyệt song.getFeaturedArtists()
    → lúc đó Hibernate mới chạy thêm câu SELECT để lấy dữ liệu.

    Muốn chạy được câu SELECT đó, kết nối tới database (session của Hibernate) phải còn mở.
    Mapper được gọi trong Controller, tức là SAU khi Service đã chạy xong. Vì sao vẫn được?
    → Spring Boot mặc định bật spring.jpa.open-in-view=true: giữ session mở suốt cả request,
      tới khi trả JSON xong mới đóng. (Nếu không tự cấu hình dòng này, lúc khởi động
      console có 1 dòng WARN "spring.jpa.open-in-view is enabled by default..." nhắc về nó.)
    Nếu sau này đặt spring.jpa.open-in-view=false, mọi chỗ gọi mapper ngoài transaction
    sẽ lỗi LazyInitializationException → 500. Khi đó phải tải sẵn dữ liệu trong Service
    (VD JOIN FETCH, @EntityGraph) hoặc gọi mapper bên trong Service có @Transactional.

    -----------------------------------------------------
    Số câu truy vấn khi lấy 1 trang 20 bài (GET /api/songs)
    -----------------------------------------------------
        1 câu   : SELECT 20 bài trong bảng songs
        1 câu   : COUNT tổng số bài (để tính totalPages)
        20 câu  : mỗi bài 1 câu lấy danh sách feat (featuredArtists)
        vài câu : lấy nghệ danh và tên thể loại, mỗi Artist hoặc thể loại KHÁC NHAU 1 câu
                  (đã tải 1 lần thì Hibernate nhớ lại, không tải lần 2 trong cùng request)
    Kiểu "1 câu lấy danh sách + N câu lấy chi tiết từng phần tử" gọi là vấn đề N+1.
    Với vài chục bài mỗi trang thì vẫn nhanh, chạy tốt.
    Khi dữ liệu lớn có thể tối ưu bằng @EntityGraph hoặc @BatchSize (chưa cần làm bây giờ).
*/
public class SongMapper {

    /*
        Constructor private: cấm new SongMapper(), vì mọi hàm đều static (giải thích chi tiết: xem UserMapper).
    */
    private SongMapper() {}

    /*
        ---------------------------------------------------------------
        User → ArtistSummary (chỉ id và nghệ danh)
        ---------------------------------------------------------------

        Dùng ở:
        - activeFeaturedArtists() bên dưới: danh sách nghệ sĩ feat của mỗi bài.
        - ArtistSongController.searchArtists() (GET /api/artist/search-artists?keyword=...):
          kết quả tìm nghệ sĩ để feat, viết gọn bằng method reference:
              .map(SongMapper::toArtistSummary)   // = .map(user -> SongMapper.toArtistSummary(user))

        JSON 1 phần tử: { "id": 12, "artistName": "Sơn Tùng" }

        Không đưa email, họ tên thật, trạng thái tài khoản ra ngoài → bảo vệ thông tin cá nhân.
        Đặc biệt quan trọng vì Artist A tìm được Artist B: A chỉ cần biết id (để gửi lên khi feat)
        và nghệ danh (để hiển thị), không cần biết email của B.

        Chỉ 2 trường nên dùng constructor thường, không cần Builder.
    */
    public static ArtistSummary toArtistSummary(User user) {
        return new ArtistSummary(user.getId(), user.getArtistName());
    }

    /*
        ---------------------------------------------------------------
        Genre → GenreResponse (id và tên thể loại)
        ---------------------------------------------------------------
        Dùng ở GenreController (GET /api/genres):
            songService.getGenres().stream().map(SongMapper::toGenreResponse).toList()
        JSON: [ { "id": 1, "name": "Pop" }, { "id": 2, "name": "Rap" }, ... ]

        Vì sao đặt trong SongMapper, không tạo GenreMapper riêng?
        Thể loại chỉ dùng kèm bài hát và chỉ có 1 hàm chuyển đổi rất ngắn,
        tách thêm 1 file riêng chưa đáng.
    */
    public static GenreResponse toGenreResponse(Genre genre) {
        return new GenreResponse(genre.getId(), genre.getName());
    }

    /*
        ---------------------------------------------------------------
        Thể loại của 1 bài: id và tên
        ---------------------------------------------------------------
        Mọi bài đều có thể loại (bài mới bắt buộc chọn, bài cũ đã gán bằng câu UPDATE),
        nhưng cột genre_id trong database vẫn cho phép NULL.
        → Kiểm tra null trước khi đọc, để lỡ có bài thiếu thể loại cũng không lỗi
          NullPointerException (500), chỉ trả genreId, genreName = null.

        Vì sao phải kiểm tra? Nếu genre là null mà vẫn gọi genre.getId()
        → gọi hàm trên 1 thứ không tồn tại → NullPointerException → 500 cho cả danh sách,
          chỉ vì 1 bài lỗi.

        Toán tử 3 ngôi:  điều kiện ? giá trị nếu đúng : giá trị nếu sai
            genre != null ? genre.getId() : null
            → có thể loại thì lấy id, không có thì null.

        2 hàm này là private static:
        - private: chỉ dùng trong file này (hàm phụ), bên ngoài không gọi được.
        - static : được gọi từ các hàm static khác (toSongResponse, toManageResponse).
                   Hàm static chỉ gọi được hàm static khác, vì không có object nào để gọi hàm thường.

        Về số câu truy vấn: genre.getId() không cần tải thể loại (Hibernate đã biết sẵn id,
        vì cột genre_id nằm ngay trong dòng songs). genre.getName() mới cần tải.
    */
    private static Long genreId(Song song) {
        Genre genre = song.getGenre();
        return genre != null ? genre.getId() : null;
    }

    private static String genreName(Song song) {
        Genre genre = song.getGenre();
        return genre != null ? genre.getName() : null;
    }

    /*
        ---------------------------------------------------------------
        Danh sách nghệ sĩ feat đang hoạt động của 1 bài
        ---------------------------------------------------------------
        Trong database, 1 bài có thể feat nhiều người (bảng song_featured_artists).
        Nhưng chỉ hiện những người còn ĐẠT 2 điều kiện:
            accountStatus == ACTIVE   (không bị khóa, không tự vô hiệu hóa, không bị xóa)
            role == ARTIST            (chưa bị Admin hạ xuống USER)

        Ví dụ: bài 15 của Artist A feat Artist B (id 12) và Artist C (id 13).
            Admin khóa B → JSON của bài 15 chỉ còn: "featuredArtists": [ { "id": 13, ... } ]
            Admin mở khóa B → B tự hiện lại, không cần Artist A sửa bài.
        Dòng (15, 12) trong bảng song_featured_artists KHÔNG bị xóa, chỉ bị ẩn khi hiển thị.
        Giống cách bài của Artist bị khóa tự ẩn khỏi người nghe (PUBLISHED_CONDITION trong SongRepository).
        Cùng điều kiện cũng được dùng khi tìm kiếm theo tên người feat (ACTIVE_FEATURED_CONDITION),
        nên tìm kiếm và hiển thị luôn khớp nhau.

        -----------------------------------------------------
        Đọc từng dòng
        -----------------------------------------------------
        List<ArtistSummary> result = new ArrayList<>();
            → tạo danh sách rỗng để chứa kết quả.
              <> (diamond): Java tự hiểu kiểu bên trong là ArtistSummary, không cần viết lại.

        for (User user : song.getFeaturedArtists())
            → vòng lặp for-each: lần lượt lấy từng người feat (kiểu User) trong Set.
              Đây là lúc Hibernate chạy câu SELECT tải danh sách feat (LAZY, xem phần đầu file).

        user.getAccountStatus() == AccountStatus.ACTIVE && user.getRole() == Role.ARTIST
            → so sánh enum bằng == (an toàn, vì mỗi giá trị enum chỉ có đúng 1 object, xem enum Role).
              && : cả 2 vế đều đúng.

        result.add(toArtistSummary(user));
            → đạt điều kiện thì đổi sang ArtistSummary (chỉ id và nghệ danh) rồi thêm vào danh sách.

        result.sort(Comparator.comparing(ArtistSummary::getId));
            → sắp xếp tăng dần theo id.
              Comparator.comparing(ArtistSummary::getId): "so sánh 2 phần tử bằng cách so getId() của chúng".
            Vì sao phải sắp xếp? featuredArtists là Set (HashSet) → KHÔNG có thứ tự cố định.
            Không sắp xếp thì cùng 1 bài, mỗi lần gọi API có thể ra thứ tự khác nhau
            (lần này B trước C, lần sau C trước B) → giao diện bị "nhảy" lung tung.
            Sắp theo id → thứ tự luôn giống nhau giữa các lần gọi.
            (id nhỏ hơn = tài khoản tạo sớm hơn; đây KHÔNG phải thứ tự Artist chọn lúc feat,
             vì bảng song_featured_artists không lưu thứ tự.)
    */
    private static List<ArtistSummary> activeFeaturedArtists(Song song) {
        List<ArtistSummary> result = new ArrayList<>();

        for(User user : song.getFeaturedArtists()) {
            if(user.getAccountStatus() == AccountStatus.ACTIVE && user.getRole() == Role.ARTIST) {
                result.add(toArtistSummary(user));
            }
        }

        result.sort(Comparator.comparing(ArtistSummary::getId));
        return result;
    }

    /*
        ---------------------------------------------------------------
        Song → SongResponse (cho người nghe)
        ---------------------------------------------------------------
        Dùng ở:
        - SongController: danh sách bài, tìm kiếm, chi tiết 1 bài, trang của 1 Artist.
        - ListeningHistoryService: lịch sử nghe (GET /api/users/me/history).
        - RecommendationService: bài gợi ý.

        Người nghe chỉ thấy bài ĐÃ PHÁT HÀNH, nên KHÔNG có các trường quản lý:
        status, released, rejectReason, createdAt, updatedAt.
        (Ẩn status để người nghe không dò được bài nào đang chờ duyệt, bị từ chối...)

        -----------------------------------------------------
        Cú pháp Builder: SongResponse.builder() ... .build()
        -----------------------------------------------------
        SongResponse có 13 trường. Dùng constructor thì phải truyền 13 giá trị đúng thứ tự,
        rất dễ nhầm (xem lưu ý trong UserMapper). Builder cho phép gán theo TÊN trường:
            SongResponse.builder()        → tạo 1 "người lắp ráp" (Builder) rỗng
                .id(song.getId())         → gán id, trả lại chính Builder đó
                .title(song.getTitle())   →   ... nên gọi nối tiếp được (method chaining)
                ...
                .build()                  → lắp xong, tạo ra object SongResponse thật
        Builder được viết tay trong SongResponse (dự án không dùng Lombok).

        -----------------------------------------------------
        Từng trường
        -----------------------------------------------------
            id, title             : lấy thẳng từ Song
            audioUrl              : "/uploads/songs/audio/3f2a....mp3"
            coverUrl              : "/uploads/songs/covers/9b1c....jpg", null nếu không có ảnh bìa
            canvasUrl             : "/uploads/songs/canvas/....mp4", null nếu không có video Canvas
                                    (3 đường dẫn trên là tương đối, frontend ghép thêm địa chỉ server)
            durationSeconds       : thời lượng (giây), VD 215 = 3 phút 35 giây
            playCount             : số lượt nghe
            releaseAt             : thời điểm phát hành
            artistId, artistName  : Artist chính, lấy qua song.getArtist()
                                    (gửi riêng id và tên, không gửi cả object User → không lộ email...)
            featuredArtists       : danh sách feat đã lọc và sắp xếp (hàm ở trên)
            genreId, genreName    : thể loại, có thể null (2 hàm phụ ở trên)
    */
    public static SongResponse toSongResponse(Song song) {
        return SongResponse.builder()
                .id(song.getId())
                .title(song.getTitle())
                .audioUrl(song.getAudioUrl())
                .coverUrl(song.getCoverUrl())
                .canvasUrl(song.getCanvasUrl())
                .durationSeconds(song.getDurationSeconds())
                .playCount(song.getPlayCount())
                .releaseAt(song.getReleaseAt())
                .artistId(song.getArtist().getId())
                .artistName(song.getArtist().getArtistName())
                .featuredArtists(activeFeaturedArtists(song))
                .genreId(genreId(song))
                .genreName(genreName(song))
                .build();
    }

    /*
        ---------------------------------------------------------------
        Song → SongManageResponse (cho Artist và Admin, đầy đủ thông tin quản lý)
        ---------------------------------------------------------------
        Dùng ở:
        - ArtistSongController: "Bài hát của tôi", và kết quả trả về sau khi đăng, sửa, ẩn, hiện...
        - AdminSongController : danh sách bài để duyệt, kết quả sau khi duyệt, từ chối, gỡ bài.

        So với SongResponse, có thêm:
            status        : PENDING, APPROVED, REJECTED, HIDDEN, DELETED (xem enum SongStatus)
            released      : đã phát hành chưa (true hoặc false), tính ngay bên dưới
            rejectReason  : lý do bị từ chối hoặc bị gỡ, null nếu không bị
            createdAt     : lúc đăng bài
            updatedAt     : lần sửa gần nhất

        -----------------------------------------------------
        Tính released
        -----------------------------------------------------
        "Đã phát hành" không lưu trong database mà tính ra mỗi lần đọc (xem enum SongStatus):
            song.getStatus() == SongStatus.APPROVED                  đã được duyệt
            && song.getReleaseAt() != null                            đã có thời điểm phát hành
            && !song.getReleaseAt().isAfter(LocalDateTime.now())      thời điểm đó đã tới

        - isAfter(now): releaseAt có SAU bây giờ không (còn ở tương lai không)?
          Thêm dấu ! (phủ định) → "KHÔNG ở tương lai" = releaseAt <= bây giờ.
          Khớp đúng điều kiện s.releaseAt <= :now trong PUBLISHED_CONDITION của SongRepository.
        - Vì sao kiểm tra releaseAt != null trước?
          Bài PENDING chưa đặt lịch thì releaseAt = null. Gọi null.isAfter(...) → NullPointerException.
          && dừng ngay khi gặp vế sai, nên khi releaseAt là null thì vế thứ 3 không bao giờ chạy.
          (Bài APPROVED luôn có releaseAt: lúc duyệt, nếu chưa đặt lịch thì AdminSongService
           gán releaseAt = lúc duyệt. Kiểm tra null ở đây chỉ để an toàn tuyệt đối.)

        Ví dụ (bây giờ là 20:00 ngày 03/11/2026):
            status      releaseAt            released   Frontend hiển thị
            PENDING     null                 false      "Chờ duyệt"
            APPROVED    03/11/2026 08:00     true       "Đã phát hành"
            APPROVED    10/11/2026 20:00     false      "Đã lên lịch" (tới giờ tự thành true)
            HIDDEN      01/11/2026 08:00     false      "Đã ẩn"

        Lưu ý: released chỉ xét bài, không xét Artist. Người nghe còn cần Artist đang ACTIVE
        và còn role ARTIST mới thấy bài (PUBLISHED_CONDITION).
        Nhưng ai xem được SongManageResponse? Artist (đang đăng nhập được thì chắc chắn ACTIVE và ARTIST)
        và Admin (Admin xem thêm accountStatus ở danh sách tài khoản nếu cần).

        boolean (chữ thường) chứ không phải Boolean: luôn có giá trị true hoặc false, không bao giờ null.
    */
    public static SongManageResponse toManageResponse(Song song) {
        boolean released = song.getStatus() == SongStatus.APPROVED
                && song.getReleaseAt() != null
                && !song.getReleaseAt().isAfter(LocalDateTime.now());

        return SongManageResponse.builder()
                .id(song.getId())
                .title(song.getTitle())
                .audioUrl(song.getAudioUrl())
                .coverUrl(song.getCoverUrl())
                .canvasUrl(song.getCanvasUrl())
                .durationSeconds(song.getDurationSeconds())
                .playCount(song.getPlayCount())
                .status(song.getStatus())
                .released(released)
                .releaseAt(song.getReleaseAt())
                .rejectReason(song.getRejectReason())
                .artistId(song.getArtist().getId())
                .artistName(song.getArtist().getArtistName())
                .featuredArtists(activeFeaturedArtists(song))
                .genreId(genreId(song))
                .genreName(genreName(song))
                .createdAt(song.getCreatedAt())
                .updatedAt(song.getUpdatedAt())
                .build();
    }

    /*
        ---------------------------------------------------------------
        Page<Song> → PageResponse<T> (1 trang kết quả trả về frontend)
        ---------------------------------------------------------------
        Page<Song> là kết quả phân trang của Spring Data, VD songRepository.findPublished(now, pageable):
            page.getContent()        : danh sách bài của trang này (List<Song>), VD 20 bài
            page.getNumber()         : số trang hiện tại, ĐẾM TỪ 0 (trang đầu là 0)
            page.getSize()           : số bài tối đa mỗi trang (VD 20)
            page.getTotalElements()  : tổng số bài khớp điều kiện, trên mọi trang (VD 45)
            page.getTotalPages()     : tổng số trang (45 bài, 20 bài/trang → 3 trang)

        JSON frontend nhận được (VD GET /api/songs?page=0&size=20):
            {
                "content": [ { bài 1 }, { bài 2 }, ... ],
                "page": 0,
                "size": 20,
                "totalElements": 45,
                "totalPages": 3
            }
        Frontend dùng totalPages để vẽ nút chuyển trang, hết trang khi page + 1 == totalPages.

        Vì sao không trả thẳng Page ra JSON?
        JSON của Page do Spring tự sinh có rất nhiều trường thừa (pageable, sort, first, last...)
        và Spring khuyến cáo không dùng trực tiếp vì cấu trúc có thể đổi giữa các phiên bản.
        PageResponse tự định nghĩa → JSON gọn, cố định, frontend yên tâm dùng.

        -----------------------------------------------------
        Generic: <T>, PageResponse<T>, Function<Song, T>
        -----------------------------------------------------
        Cùng 1 trang bài hát nhưng cần đổi sang 2 kiểu DTO khác nhau:
            Người nghe: PageResponse<SongResponse>
            Admin     : PageResponse<SongManageResponse>
        Thay vì viết 2 hàm gần giống hệt nhau, dùng generic: T là "1 kiểu bất kỳ, quyết định lúc gọi".

        public static <T> PageResponse<T> toPageResponse(Page<Song> page, Function<Song, T> converter)
            - <T> đứng trước kiểu trả về: khai báo "hàm này có 1 kiểu tạm tên T".
            - Function<Song, T>: 1 HÀM được truyền vào như 1 tham số, nhận Song, trả về T.
              converter.apply(song) = chạy hàm đó với song.
            - Kiểu trả về PageResponse<T>: trang chứa các phần tử kiểu T.

        Khi gọi, truyền vào hàm chuyển đổi mong muốn bằng method reference (Class::tênHàm):
            SongController (người nghe):
                SongMapper.toPageResponse(page, SongMapper::toSongResponse)
                → T = SongResponse, converter.apply(song) chính là toSongResponse(song)
            AdminSongController:
                SongMapper.toPageResponse(page, SongMapper::toManageResponse)
                → T = SongManageResponse, converter.apply(song) chính là toManageResponse(song)
        SongMapper::toSongResponse là cách viết gọn của: song -> SongMapper.toSongResponse(song)

        -----------------------------------------------------
        Đọc từng dòng
        -----------------------------------------------------
        List<T> content = new ArrayList<>();          → danh sách rỗng chứa DTO
        for (Song song : page.getContent())           → duyệt từng bài trong trang
            content.add(converter.apply(song));       → đổi bài đó sang DTO rồi thêm vào
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                                  page.getTotalElements(), page.getTotalPages());
            → đóng gói danh sách DTO cùng 4 thông tin phân trang.
              Thứ tự tham số khớp constructor của PageResponse
              (content, page, size, totalElements, totalPages).
              Các số khác kiểu nhau một phần (int, long), nhưng page, size, totalPages
              cùng kiểu int, nên vẫn phải cẩn thận đúng thứ tự.
    */
    public static <T> PageResponse<T> toPageResponse(Page<Song> page, Function<Song, T> converter) {
        List<T> content = new ArrayList<>();

        for(Song song : page.getContent()) {
            content.add(converter.apply(song));
        }

        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}