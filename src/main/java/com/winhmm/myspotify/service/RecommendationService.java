package com.winhmm.myspotify.service;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    - RecommendClient : gọi sang dịch vụ AI (Python).
    - SongResponse    : DTO bài hát trả cho frontend (cùng cấu trúc với GET /api/songs).
    - Song            : entity bài hát.
    - SongMapper      : đổi Song sang SongResponse.
    - SongRepository  : đọc bảng songs trong MySQL.
    - Logger, LoggerFactory: ghi log (dòng thông báo trong app.log).
    - PageRequest, Pageable, Sort: lấy "trang đầu, N bài, sắp xếp theo cột nào".
    - Service         : đánh dấu class Service.
    - RestClientException: lỗi khi gọi HTTP sang server khác (Python tắt, quá thời gian chờ...).
    - LocalDateTime   : thời điểm hiện tại, dùng để lọc bài đã tới giờ phát hành.
    - Các class còn lại trong java.util: danh sách, Map, tập hợp... (giải thích tại chỗ dùng).
    - Supplier        : "1 đoạn code trả về giá trị", truyền vào hàm như 1 tham số (giải thích ở askPython).
*/
import com.winhmm.myspotify.client.RecommendClient;
import com.winhmm.myspotify.dto.song.SongResponse;
import com.winhmm.myspotify.entity.Song;
import com.winhmm.myspotify.mapper.SongMapper;
import com.winhmm.myspotify.repository.SongRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/*
    =====================================================================
    RECOMMENDATIONSERVICE - GỢI Ý BÀI HÁT BẰNG AI
    =====================================================================

    Các chức năng:
        getSimilarSongs(...) → bài hát tương tự 1 bài     (Python: /recommend)
        getArtistSongs(...)  → bài liên quan nghệ sĩ      (Python: /recommend/artist)
        getGenreSongs(...)   → bài theo danh sách thể loại (Python: /recommend/genres)

    =====================================================================
    CÁC BƯỚC CHUNG CỦA MỌI CHỨC NĂNG
    =====================================================================

    1. Hỏi Python → nhận danh sách id bài hát, VD [7, 22, 3, 41].

    2. Hỏi MySQL thông tin các bài đó (1 câu truy vấn cho cả danh sách).
       Dùng findPublishedByIds có sẵn → chỉ lấy bài đã phát hành.
       Vì sao phải lọc lại? Python đọc thẳng bảng songs nên có thể gợi ý cả bài
       đang chờ duyệt, bị ẩn, bị gỡ, hoặc của Artist bị khóa. Người nghe không được thấy các bài đó.

    3. Sắp xếp đúng thứ tự Python gợi ý (MySQL trả về không theo thứ tự đó),
       bỏ bài trùng, bỏ chính bài đang nghe, lấy đủ số lượng yêu cầu.

    4. Đổi sang SongResponse rồi trả về.

    =====================================================================
    PHƯƠNG ÁN DỰ PHÒNG (FALLBACK)
    =====================================================================

    Gợi ý bài hát là tính năng phụ. Python lỗi thì không được làm hỏng trang nghe nhạc.

    Khi nào dùng dự phòng?
    - Python đang tắt, bị lỗi, hoặc trả lời chậm quá thời gian chờ (mặc định 3 giây).
    - Python trả lời được, nhưng sau khi lọc ở bước 2 không còn bài nào
      (VD toàn gợi ý bài đã bị ẩn).

    Dự phòng trả gì? Các bài nhiều lượt nghe nhất, đã phát hành:
    - Bài tương tự  → cùng thể loại với bài đang nghe.
    - Theo nghệ sĩ  → của cùng nghệ sĩ (nếu không có thì cùng thể loại).
    - Theo thể loại → thuộc các thể loại được yêu cầu.

    Frontend không phân biệt được kết quả từ AI hay từ dự phòng: cấu trúc JSON giống hệt.
    Khi Python lỗi, app.log có dòng cảnh báo bắt đầu bằng "Recommend service error"
    để biết mà kiểm tra dịch vụ Python.
*/
@Service
public class RecommendationService {
    /*
        log: dùng để ghi dòng cảnh báo vào app.log khi Python lỗi (giống ListeningHistoryService).
    */
    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    /*
        Số bài tối đa mỗi lần gợi ý. Frontend gửi limit lớn hơn → tự giảm về 20.
        static final: hằng số dùng chung, không đổi được.
    */
    private static final int MAX_LIMIT = 20;

    /*
        Số thể loại tối đa trong 1 lần gợi ý theo thể loại.
        Chặn trường hợp frontend gửi danh sách quá dài.
    */
    private static final int MAX_GENRES = 10;

    private final RecommendClient recommendClient;
    private final SongRepository songRepository;

    /*
        Constructor: Spring tự tạo RecommendClient và SongRepository rồi truyền vào
        (tiêm phụ thuộc, xem giải thích chi tiết trong GenreController).
    */
    public RecommendationService(RecommendClient recommendClient, SongRepository songRepository) {
        this.recommendClient = recommendClient;
        this.songRepository = songRepository;
    }

    /*
        =====================================================================
        BÀI HÁT TƯƠNG TỰ 1 BÀI
        =====================================================================
        Dùng cho mục "Bài hát tương tự" dưới trình phát nhạc.

        1. Bài gốc phải đã phát hành, nếu không → 400 "Song does not exist".
        2. Hỏi Python POST /recommend.
           Xin gấp đôi số bài cần (askCount), vì 1 phần có thể bị lọc ở bước tiếp theo
           (bài chưa phát hành, trùng chính bài đang nghe).
        3. Lọc, sắp xếp, lấy đủ limit bài (loadPublishedInOrder).
        4. Không còn bài nào → dự phòng: bài cùng thể loại, nhiều lượt nghe nhất.
    */
    public List<SongResponse> getSimilarSongs(Long songId, int limit) {
        Song song = findPublishedSong(songId);
        int safeLimit = safeLimit(limit);
        int askCount = safeLimit * 2;

        /*
            () -> recommendClient.similarSongs(songId, askCount) là 1 lambda:
            "đoạn code gọi Python", được truyền vào askPython để chạy bên trong try-catch.
        */
        List<Long> ids = askPython("similar", () -> recommendClient.similarSongs(songId, askCount));
        List<Song> songs = loadPublishedInOrder(ids, songId, safeLimit);

        if (songs.isEmpty()) {
            songs = topSongsSameGenre(song, safeLimit);
        }
        return toResponses(songs);
    }

    /*
        =====================================================================
        BÀI LIÊN QUAN TỚI NGHỆ SĨ CỦA 1 BÀI
        =====================================================================
        Dùng cho mục "Cùng nghệ sĩ" trên trang chi tiết bài hát.

        Các bước giống getSimilarSongs, chỉ khác:
        - Gọi Python POST /recommend/artist.
        - Dự phòng: bài nhiều lượt nghe nhất của cùng nghệ sĩ.
          Nghệ sĩ chỉ có đúng 1 bài (chính bài đang nghe) → dùng tiếp dự phòng cùng thể loại.
    */
    public List<SongResponse> getArtistSongs(Long songId, int limit) {
        Song song = findPublishedSong(songId);
        int safeLimit = safeLimit(limit);
        int askCount = safeLimit * 2;

        List<Long> ids = askPython("artist", () -> recommendClient.artistSongs(songId, askCount));
        List<Song> songs = loadPublishedInOrder(ids, songId, safeLimit);

        if (songs.isEmpty()) {
            songs = topSongsSameArtist(song, safeLimit);
        }
        if (songs.isEmpty()) {
            songs = topSongsSameGenre(song, safeLimit);
        }
        return toResponses(songs);
    }

    /*
        =====================================================================
        BÀI THEO DANH SÁCH THỂ LOẠI
        =====================================================================
        Dùng cho trang Genres, hoặc màn hình chọn thể loại yêu thích.

        1. Kiểm tra danh sách thể loại:
           - Không gửi hoặc rỗng → 400 "Genre ids are required".
           - Bỏ phần tử null và id trùng (VD [1, 1, 24] → [1, 24]).
           - Quá 10 thể loại → 400 "At most 10 genres are allowed".
        2. Hỏi Python POST /recommend/genres.
        3. Lọc, sắp xếp, lấy đủ limit bài. Không loại bài nào (không có "bài đang nghe").
        4. Không còn bài nào → dự phòng: bài nhiều lượt nghe nhất thuộc các thể loại đó.

        Id thể loại không tồn tại thì không báo lỗi, chỉ đơn giản là không có bài nào thuộc thể loại đó.
    */
    public List<SongResponse> getGenreSongs(List<Long> genreIds, int limit) {
        if (genreIds == null || genreIds.isEmpty()) {
            throw new IllegalArgumentException("Genre ids are required");
        }

        /*
            Đọc chuỗi stream từ trên xuống:
            - filter(Objects::nonNull): giữ phần tử khác null.
              Objects::nonNull là cách viết gọn của id -> id != null.
            - distinct(): bỏ phần tử trùng.
            - toList()  : gom lại thành List mới.
        */
        List<Long> cleanGenreIds = genreIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (cleanGenreIds.isEmpty()) {
            throw new IllegalArgumentException("Genre ids are required");
        }
        if (cleanGenreIds.size() > MAX_GENRES) {
            throw new IllegalArgumentException("At most " + MAX_GENRES + " genres are allowed");
        }

        int safeLimit = safeLimit(limit);
        int askCount = safeLimit * 2;

        List<Long> ids = askPython("genres", () -> recommendClient.genreSongs(cleanGenreIds, askCount));

        /*
            Tham số thứ 2 là null: không cần loại bài nào ra khỏi kết quả.
        */
        List<Song> songs = loadPublishedInOrder(ids, null, safeLimit);

        if (songs.isEmpty()) {
            songs = topSongsInGenres(cleanGenreIds, safeLimit);
        }
        return toResponses(songs);
    }

    /*
        =====================================================================
        CÁC HÀM PHỤ (PRIVATE)
        =====================================================================
    */

    /*
        -----------------------------------------------------
        Hỏi Python, lỗi thì trả danh sách rỗng thay vì ném lỗi ra ngoài
        -----------------------------------------------------
        Tham số:
        - apiName: tên ngắn của API ("similar", "artist", "genres"), chỉ dùng để ghi log.
        - call   : đoạn code gọi Python, kiểu Supplier<List<Long>>.

        Supplier<List<Long>> là gì?
        - Là 1 "đoạn code chưa chạy", khi gọi call.get() thì đoạn code đó mới chạy
          và trả về List<Long>.
        - Nhờ vậy 3 chức năng ở trên dùng chung 1 khối try-catch này, mỗi chức năng chỉ truyền vào
          đoạn code gọi Python của mình, không phải viết lại try-catch 3 lần.

        try-catch:
        - try  : chạy thử call.get() (gọi Python).
        - catch: nếu ném RestClientException (Python tắt, quá thời gian chờ, trả mã lỗi...)
                 → ghi 1 dòng cảnh báo vào log, trả về danh sách rỗng.
                 Danh sách rỗng → hàm gọi sẽ dùng phương án dự phòng.

        log.warn("... ({}): {}", apiName, e.getMessage()):
        Mỗi cặp {} được thay lần lượt bằng các giá trị phía sau, VD:
            Recommend service error (similar): I/O error on POST request ... Connection refused
    */
    private List<Long> askPython(String apiName, Supplier<List<Long>> call) {
        try {
            return call.get();
        } catch (RestClientException e) {
            log.warn("Recommend service error ({}): {}", apiName, e.getMessage());
            return List.of();
        }
    }

    /*
        -----------------------------------------------------
        Tìm bài đã phát hành theo id
        -----------------------------------------------------
        Không có, hoặc chưa phát hành, bị ẩn, bị gỡ, Artist bị khóa
        → 400 "Song does not exist" (cùng message với GET /api/songs/{songId}).
    */
    private Song findPublishedSong(Long songId) {
        return songRepository.findPublishedById(songId, LocalDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException("Song does not exist"));
    }

    /*
        -----------------------------------------------------
        Giới hạn số bài trong khoảng 1 đến 20
        -----------------------------------------------------
        Math.max(limit, 1): nhỏ hơn 1 (VD 0, -5) → thành 1.
        Math.min(..., MAX_LIMIT): lớn hơn 20 (VD 1000) → thành 20.
    */
    private int safeLimit(int limit) {
        return Math.min(Math.max(limit, 1), MAX_LIMIT);
    }

    /*
        -----------------------------------------------------
        Đổi danh sách id thành danh sách bài đã phát hành, giữ đúng thứ tự
        -----------------------------------------------------
        Ví dụ:
            ids từ Python   : [22, 15, 7, 22, 41]       (15 là bài đang nghe, 41 đang bị ẩn)
            MySQL trả về    : bài 7, bài 15, bài 22     (không có 41 vì chưa phát hành,
                                                         thứ tự có thể khác thứ tự trong ids)
            Kết quả         : [bài 22, bài 7]           (giữ thứ tự Python, bỏ 15, bỏ 22 lặp lại,
                                                         bỏ 41)

        Tham số:
        - ids      : danh sách id Python gợi ý.
        - excludeId: id bài cần loại ra (bài đang nghe). null = không loại bài nào.
        - limit    : số bài tối đa trong kết quả.

        Các bước:
        1. Danh sách rỗng → trả rỗng luôn, không hỏi MySQL
           (câu IN () với danh sách rỗng cũng không có ý nghĩa).
        2. Hỏi MySQL 1 lần cho cả danh sách, rồi cho vào Map<id, Song> để tra nhanh theo id.
        3. Duyệt ids theo đúng thứ tự Python:
           - new LinkedHashSet<>(ids): bỏ id trùng nhưng vẫn giữ thứ tự xuất hiện
             (HashSet thường thì làm mất thứ tự).
           - id.equals(excludeId): là bài đang nghe → bỏ qua (continue: chuyển sang id tiếp theo).
           - songsById.get(id) == null: bài không có trong kết quả MySQL (chưa phát hành) → bỏ qua.
           - Đủ limit bài → dừng (break).
    */
    private List<Song> loadPublishedInOrder(List<Long> ids, Long excludeId, int limit) {
        if (ids.isEmpty()) {
            return List.of();
        }

        Map<Long, Song> songsById = new HashMap<>();
        for (Song song : songRepository.findPublishedByIds(ids, LocalDateTime.now())) {
            songsById.put(song.getId(), song);
        }

        List<Song> result = new ArrayList<>();
        for (Long id : new LinkedHashSet<>(ids)) {
            if (id.equals(excludeId)) {
                continue;
            }
            Song song = songsById.get(id);
            if (song == null) {
                continue;
            }
            result.add(song);
            if (result.size() == limit) {
                break;
            }
        }
        return result;
    }

    /*
        -----------------------------------------------------
        Dự phòng: bài cùng thể loại, nhiều lượt nghe nhất
        -----------------------------------------------------
        - Lấy trang đầu, limit + 1 bài (dư 1 bài, phòng trường hợp chính bài đang nghe
          nằm trong danh sách và bị loại ra).
        - Bài cũ chưa có thể loại (genre = null) → lấy bài nhiều lượt nghe nhất trong toàn hệ thống.
        - Loại chính bài đang nghe, rồi cắt còn đúng limit bài.

        song.getGenre().getId(): genre là quan hệ LAZY, nhưng lấy id thì Hibernate
        không cần tải thêm dữ liệu (id đã có sẵn trong cột genre_id của bảng songs).
    */
    private List<Song> topSongsSameGenre(Song song, int limit) {
        Pageable top = topByPlayCount(limit + 1);
        LocalDateTime now = LocalDateTime.now();

        List<Song> candidates;
        if (song.getGenre() != null) {
            candidates = songRepository.findPublishedByGenre(song.getGenre().getId(), now, top).getContent();
        } else {
            candidates = songRepository.findPublished(now, top).getContent();
        }
        return excludeAndLimit(candidates, song.getId(), limit);
    }

    /*
        -----------------------------------------------------
        Dự phòng: bài của cùng nghệ sĩ, nhiều lượt nghe nhất
        -----------------------------------------------------
        findPublishedByArtist có sẵn: bài nghệ sĩ đó là Artist chính hoặc được feat.
    */
    private List<Song> topSongsSameArtist(Song song, int limit) {
        List<Song> candidates = songRepository.findPublishedByArtist(
                song.getArtist().getId(), LocalDateTime.now(), topByPlayCount(limit + 1)).getContent();
        return excludeAndLimit(candidates, song.getId(), limit);
    }

    /*
        -----------------------------------------------------
        Dự phòng: bài nhiều lượt nghe nhất thuộc các thể loại được yêu cầu
        -----------------------------------------------------
        1. Mỗi thể loại lấy limit bài nhiều lượt nghe nhất, gom chung vào 1 danh sách.
           Mỗi bài chỉ có 1 thể loại, nên không bị trùng giữa các thể loại.
        2. Sắp xếp cả danh sách theo lượt nghe giảm dần:
           Comparator.comparing(Song::getPlayCount) → so sánh theo playCount tăng dần,
           .reversed() → đảo lại thành giảm dần.
        3. Lấy limit bài đầu tiên. Không có bài đang nghe nên không loại bài nào (null).

        Phải dùng new ArrayList<>() vì danh sách từ getContent() không sửa được,
        mà addAll và sort đều sửa danh sách.
    */
    private List<Song> topSongsInGenres(List<Long> genreIds, int limit) {
        LocalDateTime now = LocalDateTime.now();
        List<Song> candidates = new ArrayList<>();

        for (Long genreId : genreIds) {
            candidates.addAll(songRepository.findPublishedByGenre(genreId, now, topByPlayCount(limit)).getContent());
        }

        candidates.sort(Comparator.comparing(Song::getPlayCount).reversed());
        return excludeAndLimit(candidates, null, limit);
    }

    /*
        -----------------------------------------------------
        Trang đầu tiên, size bài, sắp xếp lượt nghe giảm dần
        -----------------------------------------------------
        PageRequest.of(0, size, Sort...):
        - 0   : trang đầu tiên (đánh số từ 0).
        - size: số bài.
        - Sort.by(Sort.Direction.DESC, "playCount"): ORDER BY play_count DESC.
          "playCount" là tên thuộc tính Java trong entity Song, không phải tên cột MySQL.
    */
    private Pageable topByPlayCount(int size) {
        return PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "playCount"));
    }

    /*
        -----------------------------------------------------
        Loại 1 bài ra khỏi danh sách và cắt còn limit bài
        -----------------------------------------------------
        - filter(...): giữ bài có id khác excludeId.
          !song.getId().equals(excludeId): dấu ! nghĩa là "không".
          excludeId là null thì equals luôn trả false → giữ lại mọi bài.
        - limit(limit): lấy tối đa limit phần tử đầu tiên.
    */
    private List<Song> excludeAndLimit(List<Song> songs, Long excludeId, int limit) {
        return songs.stream()
                .filter(song -> !song.getId().equals(excludeId))
                .limit(limit)
                .toList();
    }

    /*
        -----------------------------------------------------
        Đổi danh sách Song thành danh sách SongResponse
        -----------------------------------------------------
        SongMapper::toSongResponse là cách viết gọn của song -> SongMapper.toSongResponse(song).
        Nghệ sĩ feat bị khóa tự bị ẩn khỏi featuredArtists (SongMapper đã xử lý).
    */
    private List<SongResponse> toResponses(List<Song> songs) {
        return songs.stream()
                .map(SongMapper::toSongResponse)
                .toList();
    }
}