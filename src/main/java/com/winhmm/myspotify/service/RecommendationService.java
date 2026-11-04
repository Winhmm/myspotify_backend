package com.winhmm.myspotify.service;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    - RecommendClient : gọi sang dịch vụ AI (Python), giờ chỉ còn dùng cho gợi ý theo thể loại.
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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/*
    =====================================================================
    RECOMMENDATIONSERVICE - GỢI Ý BÀI HÁT
    =====================================================================

    Các chức năng:
        getSimilarSongs(...) → bài đề xuất sau 1 bài, dùng làm danh sách phát tiếp   (tính từ MySQL)
        getArtistSongs(...)  → bài của cùng nghệ sĩ                                  (tính từ MySQL)
        getGenreSongs(...)   → bài theo danh sách thể loại                           (Python: /recommend/genres)

    =====================================================================
    VÌ SAO 2 CHỨC NĂNG ĐẦU KHÔNG HỎI PYTHON NỮA?
    =====================================================================

    Trước đây getSimilarSongs hỏi Python POST /recommend, getArtistSongs hỏi POST /recommend/artist.
    Nhưng 2 API đó của Python tính từ 1 file CSV riêng (so theo genre, energy, tempo trong file),
    không xét nghệ sĩ của bài trong MySQL → kết quả không theo đúng nghệ sĩ và thể loại.
    Backend chỉ dùng phương án dự phòng khi Python trả danh sách rỗng,
    nên Python trả id nào (miễn bài đó có trong MySQL) là hiện bài đó, dù sai nghệ sĩ.

    Giờ 2 chức năng này tính thẳng từ MySQL theo quy tắc ở phần dưới.
    - Không gọi Python, nên Python tắt cũng không ảnh hưởng.
    - RecommendClient vẫn giữ nguyên các hàm similarSongs, artistSongs
      (không ai gọi tới, để dành nếu sau này bên Python sửa lại).
    - getGenreSongs vẫn hỏi Python như cũ.

    =====================================================================
    QUY TẮC ĐỀ XUẤT PHÁT TIẾP (GETSIMILARSONGS)
    =====================================================================

    Mục tiêu: nghe hết 1 bài thì phát tiếp bài liên quan:
    vài bài của cùng nghệ sĩ trước, rồi chuyển sang nghệ sĩ khác cùng thể loại.

    Ví dụ: đang nghe "Chiều Mưa" của Mây Chiều, thể loại Chill, xin 6 bài (limit = 6).

    Bước 1. Tối đa 3 bài khác của Mây Chiều.
            - Tính cả bài Mây Chiều chỉ được feat.
            - Bài cùng thể loại Chill xếp trước, rồi tới bài thể loại khác.
              Trong mỗi nhóm, bài nhiều lượt nghe hơn xếp trước.
            - Mây Chiều có 10 bài thì cũng chỉ lấy 3 (đủ 3 bài là chuyển sang bước 2).
            - Mây Chiều chỉ có 1 bài khác thì lấy 1 bài đó rồi chuyển luôn sang bước 2.

    Bước 2. Còn thiếu thì lấy bài Chill của nghệ sĩ khác, nhiều lượt nghe nhất trước.
            Không lấy thêm bài nào của Mây Chiều (kể cả bài có feat Mây Chiều),
            để giữ đúng giới hạn tối đa 3 bài của cùng nghệ sĩ.

    Bước 3. Vẫn còn thiếu (thể loại Chill ít bài) thì lấy bài nhiều lượt nghe nhất
            của nghệ sĩ khác, thể loại nào cũng được.
            Có bước này để danh sách phát tiếp không bị hết giữa chừng.

    Kết quả: [Mây Chiều 1, Mây Chiều 2, Mây Chiều 3, Chill A, Chill B, Chill C]

    Bài cũ chưa có thể loại (genre = null): bỏ qua bước 2 (không biết thể loại nào là "tương tự"),
    bước 1 không có bài nào được ưu tiên, bước 3 vẫn chạy bình thường.

    Kết quả luôn cố định với cùng dữ liệu (không chọn ngẫu nhiên), nên dễ kiểm tra bằng Postman.

    -----------------------------------------------------
    Frontend dùng kết quả này thế nào khi phát hết bài?
    -----------------------------------------------------
    1. Người dùng bấm phát bài X → gọi GET /api/songs/X/recommendations?limit=10,
       lưu kết quả làm "danh sách chờ".
    2. Bài đang phát kết thúc → phát bài đầu tiên của danh sách chờ, rồi bỏ nó khỏi danh sách.
       Không gọi lại API cho từng bài, nếu không thì bài nào cũng được thêm 3 bài
       của nghệ sĩ đó và giới hạn 3 bài mất tác dụng.
    3. Danh sách chờ hết → gọi lại API với bài vừa phát xong,
       kèm excludeIds là các bài đã phát để không bị lặp lại.

    =====================================================================
    "ỨNG VIÊN" LÀ GÌ?
    =====================================================================

    Mỗi bước hỏi MySQL tối đa 50 bài nhiều lượt nghe nhất (CANDIDATE_POOL) làm ứng viên,
    rồi lọc tiếp bằng Java (bỏ bài đã có, bài cần loại, bài của cùng nghệ sĩ...).
    Vì sao không hỏi đúng số bài cần? Vì 1 phần ứng viên sẽ bị lọc bỏ,
    hỏi dư ra để sau khi lọc vẫn còn đủ bài.

    =====================================================================
    PHƯƠNG ÁN DỰ PHÒNG CỦA GỢI Ý THEO THỂ LOẠI (GETGENRESONGS)
    =====================================================================

    Gợi ý bài hát là tính năng phụ. Python lỗi thì không được làm hỏng trang nghe nhạc.

    Khi nào dùng dự phòng?
    - Python đang tắt, bị lỗi, hoặc trả lời chậm quá thời gian chờ
      (cấu hình app.recommend.timeout-ms, không đặt thì mặc định 3000 ms = 3 giây).
    - Python trả lời được, nhưng sau khi lọc không còn bài nào
      (VD toàn gợi ý bài đã bị ẩn).

    Dự phòng trả gì? Các bài nhiều lượt nghe nhất, đã phát hành, thuộc các thể loại được yêu cầu.

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

    /*
        Số bài tối đa của cùng nghệ sĩ trong danh sách đề xuất phát tiếp (bước 1).
    */
    private static final int SAME_ARTIST_LIMIT = 3;

    /*
        Số ứng viên tối đa mỗi lần hỏi MySQL (xem phần "Ứng viên là gì?" ở đầu file).
    */
    private static final int CANDIDATE_POOL = 50;

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
        BÀI ĐỀ XUẤT SAU 1 BÀI (DANH SÁCH PHÁT TIẾP)
        =====================================================================
        Dùng cho mục "Bài hát đề xuất" trên trang chi tiết bài hát,
        và cho danh sách phát tiếp khi nghe hết 1 bài.
        Quy tắc 3 bước: xem phần "Quy tắc đề xuất phát tiếp" ở đầu file.

        Tham số:
        - songId    : bài đang nghe (bài gốc).
        - limit     : số bài muốn lấy (tự giới hạn trong khoảng 1 đến 20).
        - excludeIds: các bài không muốn nhận lại (VD các bài vừa phát xong), có thể null.

        -----------------------------------------------------
        skipIds: tập các id "không được chọn nữa"
        -----------------------------------------------------
        Set (tập hợp) không chứa phần tử trùng, và contains(id) kiểm tra rất nhanh.
        Ban đầu gồm: chính bài đang nghe + các id trong excludeIds.
        Mỗi khi chọn được 1 bài, id bài đó cũng được thêm vào → không bao giờ chọn trùng.

        -----------------------------------------------------
        Đọc từng bước trong code
        -----------------------------------------------------
        1. Bài gốc phải đã phát hành, nếu không → 400 "Song does not exist".
        2. Bước 1: lấy ứng viên là các bài của nghệ sĩ (findPublishedByArtist có sẵn,
           tính cả bài được feat), xếp bài cùng thể loại lên trước (sameGenreFirst),
           rồi chọn tối đa 3 bài.
           Math.min(SAME_ARTIST_LIMIT, safeLimit): frontend chỉ xin 2 bài thì chỉ lấy 2, không lấy 3.
        3. Cho tất cả ứng viên của nghệ sĩ vào skipIds (kể cả bài không được chọn),
           để bước 2, 3 không chọn thêm bài nào của nghệ sĩ này.
           Nhờ vậy bài "nghệ sĩ khác feat Mây Chiều" cũng bị loại ở bước 2, 3.
        4. Bước 2: còn thiếu và bài gốc có thể loại → bài cùng thể loại, khác nghệ sĩ.
        5. Bước 3: vẫn còn thiếu → bài nhiều lượt nghe nhất toàn hệ thống, khác nghệ sĩ.
        6. Đổi sang SongResponse rồi trả về.

        song.getArtist().getId(), song.getGenre().getId(): artist và genre là quan hệ LAZY,
        nhưng lấy id thì Hibernate không cần tải thêm dữ liệu
        (id đã có sẵn trong cột artist_id, genre_id của bảng songs).
    */
    public List<SongResponse> getSimilarSongs(Long songId, int limit, List<Long> excludeIds) {
        Song song = findPublishedSong(songId);
        int safeLimit = safeLimit(limit);
        LocalDateTime now = LocalDateTime.now();
        Long artistId = song.getArtist().getId();

        Set<Long> skipIds = new HashSet<>();
        skipIds.add(songId);
        if (excludeIds != null) {
            for (Long excludeId : excludeIds) {
                if (excludeId != null) {
                    skipIds.add(excludeId);
                }
            }
        }

        List<Song> result = new ArrayList<>();

        /*
            Bước 1: tối đa 3 bài của cùng nghệ sĩ.
            Tham số skipArtistId = null: không loại nghệ sĩ nào (đang cần chính bài của nghệ sĩ này).
        */
        List<Song> artistSongs = songRepository.findPublishedByArtist(
                artistId, now, topByPlayCount(CANDIDATE_POOL)).getContent();
        int sameArtistLimit = Math.min(SAME_ARTIST_LIMIT, safeLimit);
        addSongs(result, sameGenreFirst(artistSongs, song), skipIds, null, sameArtistLimit);

        for (Song artistSong : artistSongs) {
            skipIds.add(artistSong.getId());
        }

        /*
            Bước 2: bài cùng thể loại của nghệ sĩ khác.
            result.size() < safeLimit: chỉ chạy khi danh sách còn thiếu.
        */
        if (result.size() < safeLimit && song.getGenre() != null) {
            List<Song> genreSongs = songRepository.findPublishedByGenre(
                    song.getGenre().getId(), now, topByPlayCount(CANDIDATE_POOL)).getContent();
            addSongs(result, genreSongs, skipIds, artistId, safeLimit);
        }

        /*
            Bước 3: bài nhiều lượt nghe nhất của nghệ sĩ khác, thể loại nào cũng được.
        */
        if (result.size() < safeLimit) {
            List<Song> popularSongs = songRepository.findPublished(
                    now, topByPlayCount(CANDIDATE_POOL)).getContent();
            addSongs(result, popularSongs, skipIds, artistId, safeLimit);
        }

        return toResponses(result);
    }

    /*
        =====================================================================
        BÀI CỦA CÙNG NGHỆ SĨ
        =====================================================================
        API GET /api/songs/{songId}/recommendations/artist.
        Mục "Bài hát đề xuất" (getSimilarSongs) đã gồm bài của cùng nghệ sĩ,
        nên frontend có thể không cần API này nữa. Vẫn giữ lại để không làm hỏng chỗ nào còn gọi tới.

        1. Bài gốc phải đã phát hành, nếu không → 400 "Song does not exist".
        2. Các bài khác của cùng nghệ sĩ (tính cả bài được feat),
           bài cùng thể loại xếp trước, rồi tới bài nhiều lượt nghe. Lấy tối đa limit bài.
           Ở đây không giới hạn 3 bài, vì API này chỉ để xem bài của nghệ sĩ.
        3. Nghệ sĩ chỉ có đúng 1 bài (chính bài đang nghe)
           → dự phòng: bài cùng thể loại, nhiều lượt nghe nhất.
    */
    public List<SongResponse> getArtistSongs(Long songId, int limit) {
        Song song = findPublishedSong(songId);
        int safeLimit = safeLimit(limit);

        List<Song> artistSongs = songRepository.findPublishedByArtist(
                song.getArtist().getId(), LocalDateTime.now(), topByPlayCount(CANDIDATE_POOL)).getContent();

        Set<Long> skipIds = new HashSet<>();
        skipIds.add(songId);

        List<Song> result = new ArrayList<>();
        addSongs(result, sameGenreFirst(artistSongs, song), skipIds, null, safeLimit);

        if (result.isEmpty()) {
            result = topSongsSameGenre(song, safeLimit);
        }
        return toResponses(result);
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
           Xin gấp đôi số bài cần (askCount), vì 1 phần có thể bị lọc ở bước tiếp theo.
        3. Lọc, sắp xếp, lấy đủ limit bài (loadPublishedInOrder).
           Không loại bài nào (không có "bài đang nghe").
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

        /*
            () -> recommendClient.genreSongs(cleanGenreIds, askCount) là 1 lambda:
            "đoạn code gọi Python", được truyền vào askPython để chạy bên trong try-catch.
        */
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
        Thêm ứng viên vào kết quả cho tới khi đủ số bài
        -----------------------------------------------------
        Tham số:
        - result      : danh sách kết quả đang dựng dần (hàm này thêm bài vào thẳng danh sách đó).
        - candidates  : các ứng viên, đã xếp theo thứ tự ưu tiên.
        - skipIds     : các id không được chọn (xem getSimilarSongs).
        - skipArtistId: bỏ qua bài có Artist chính là người này. null = không bỏ qua nghệ sĩ nào.
        - maxSize     : kết quả đủ bao nhiêu bài thì dừng.

        Duyệt từng ứng viên theo thứ tự:
        - Kết quả đã đủ maxSize bài → dừng hẳn (break).
        - Id nằm trong skipIds → bỏ qua (continue: chuyển sang ứng viên tiếp theo).
        - Artist chính là skipArtistId → bỏ qua.
          candidate.getArtist().getId().equals(null) luôn trả false → skipArtistId = null thì không bỏ bài nào.
        - Còn lại → thêm vào kết quả, và thêm id vào skipIds để không bị chọn lại ở bước sau.

        Hàm trả về void (không trả gì), vì nó thêm thẳng vào danh sách result được truyền vào:
        result ở đây và result trong getSimilarSongs là cùng 1 danh sách
        (Java truyền "địa chỉ" của object List, không sao chép ra danh sách mới).
    */
    private void addSongs(List<Song> result, List<Song> candidates, Set<Long> skipIds,
                          Long skipArtistId, int maxSize) {
        for (Song candidate : candidates) {
            if (result.size() >= maxSize) {
                break;
            }
            if (skipIds.contains(candidate.getId())) {
                continue;
            }
            if (candidate.getArtist().getId().equals(skipArtistId)) {
                continue;
            }
            result.add(candidate);
            skipIds.add(candidate.getId());
        }
    }

    /*
        -----------------------------------------------------
        Xếp bài cùng thể loại với bài gốc lên trước
        -----------------------------------------------------
        Ví dụ bài gốc thể loại Chill, songs (đã xếp theo lượt nghe giảm dần):
            [Rap 900 lượt, Chill 500 lượt, Pop 300 lượt, Chill 100 lượt]
        Kết quả:
            [Chill 500 lượt, Chill 100 lượt, Rap 900 lượt, Pop 300 lượt]
        Trong mỗi nhóm vẫn giữ nguyên thứ tự lượt nghe như ban đầu.

        Cách làm: chia làm 2 danh sách (cùng thể loại, khác thể loại),
        rồi nối danh sách khác thể loại vào cuối danh sách cùng thể loại (addAll).
    */
    private List<Song> sameGenreFirst(List<Song> songs, Song seed) {
        List<Song> sameGenre = new ArrayList<>();
        List<Song> otherGenre = new ArrayList<>();

        for (Song song : songs) {
            if (isSameGenre(song, seed)) {
                sameGenre.add(song);
            } else {
                otherGenre.add(song);
            }
        }

        sameGenre.addAll(otherGenre);
        return sameGenre;
    }

    /*
        -----------------------------------------------------
        2 bài có cùng thể loại không?
        -----------------------------------------------------
        Bài nào chưa có thể loại (genre = null) → coi như không cùng thể loại với bài nào.
        Phải kiểm tra null trước khi gọi getGenre().getId(),
        nếu không sẽ lỗi NullPointerException (gọi hàm trên 1 thứ không tồn tại).
        So 2 id kiểu Long bằng equals, không dùng ==:
        Long là object, == so 2 object có phải cùng 1 object trong bộ nhớ không,
        với số từ 128 trở lên có thể ra false dù cùng giá trị. equals so giá trị → luôn đúng.
    */
    private boolean isSameGenre(Song a, Song b) {
        if (a.getGenre() == null || b.getGenre() == null) {
            return false;
        }
        return a.getGenre().getId().equals(b.getGenre().getId());
    }

    /*
        -----------------------------------------------------
        Hỏi Python, lỗi thì trả danh sách rỗng thay vì ném lỗi ra ngoài
        -----------------------------------------------------
        Tham số:
        - apiName: tên ngắn của API (VD "genres"), chỉ dùng để ghi log.
        - call   : đoạn code gọi Python, kiểu Supplier<List<Long>>.

        Supplier<List<Long>> là gì?
        - Là 1 "đoạn code chưa chạy", khi gọi call.get() thì đoạn code đó mới chạy
          và trả về List<Long>.
        - Nhờ vậy khối try-catch này dùng chung được cho mọi API của Python,
          mỗi chỗ gọi chỉ truyền vào đoạn code gọi Python của mình.
          Hiện chỉ còn getGenreSongs dùng, sau này gợi ý cho playlist cũng dùng lại được.

        try-catch:
        - try  : chạy thử call.get() (gọi Python).
        - catch: nếu ném RestClientException (Python tắt, quá thời gian chờ, trả mã lỗi...)
                 → ghi 1 dòng cảnh báo vào log, trả về danh sách rỗng.
                 Danh sách rỗng → hàm gọi sẽ dùng phương án dự phòng.

        log.warn("... ({}): {}", apiName, e.getMessage()):
        Mỗi cặp {} được thay lần lượt bằng các giá trị phía sau, VD:
            Recommend service error (genres): I/O error on POST request ... Connection refused
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
            ids từ Python   : [22, 15, 7, 22, 41]       (15 là bài cần loại, 41 đang bị ẩn)
            MySQL trả về    : bài 7, bài 15, bài 22     (không có 41 vì chưa phát hành,
                                                         thứ tự có thể khác thứ tự trong ids)
            Kết quả         : [bài 22, bài 7]           (giữ thứ tự Python, bỏ 15, bỏ 22 lặp lại,
                                                         bỏ 41)

        Tham số:
        - ids      : danh sách id Python gợi ý.
        - excludeId: id bài cần loại ra. null = không loại bài nào.
        - limit    : số bài tối đa trong kết quả.

        Các bước:
        1. Danh sách rỗng → trả rỗng luôn, không hỏi MySQL
           (câu IN () với danh sách rỗng cũng không có ý nghĩa).
        2. Hỏi MySQL 1 lần cho cả danh sách, rồi cho vào Map<id, Song> để tra nhanh theo id.
        3. Duyệt ids theo đúng thứ tự Python:
           - new LinkedHashSet<>(ids): bỏ id trùng nhưng vẫn giữ thứ tự xuất hiện
             (HashSet thường thì làm mất thứ tự).
           - id.equals(excludeId): là bài cần loại → bỏ qua (continue: chuyển sang id tiếp theo).
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
        Dùng trong getArtistSongs khi nghệ sĩ không có bài nào khác.

        - Lấy trang đầu, limit + 1 bài (dư 1 bài, phòng trường hợp chính bài đang nghe
          nằm trong danh sách và bị loại ra).
        - Bài cũ chưa có thể loại (genre = null) → lấy bài nhiều lượt nghe nhất trong toàn hệ thống.
        - Loại chính bài đang nghe, rồi cắt còn đúng limit bài.
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
        - Sort.by(Sort.Order.desc("playCount"), Sort.Order.desc("id")):
          ORDER BY play_count DESC, id DESC.
          "playCount", "id" là tên thuộc tính Java trong entity Song, không phải tên cột MySQL.
        - Vì sao thêm id DESC? Nhiều bài có cùng lượt nghe (VD bài mới đều 0 lượt)
          thì MySQL không hứa trả theo thứ tự nào, mỗi lần gọi có thể khác nhau.
          Thêm id: cùng lượt nghe thì bài mới hơn (id lớn hơn) đứng trước → kết quả luôn cố định.
    */
    private Pageable topByPlayCount(int size) {
        return PageRequest.of(0, size, Sort.by(Sort.Order.desc("playCount"), Sort.Order.desc("id")));
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