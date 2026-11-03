package com.winhmm.myspotify.service;

import com.winhmm.myspotify.entity.Genre;
import com.winhmm.myspotify.entity.Song;
import com.winhmm.myspotify.entity.User;
import com.winhmm.myspotify.enums.AccountStatus;
import com.winhmm.myspotify.enums.Role;
import com.winhmm.myspotify.repository.GenreRepository;
import com.winhmm.myspotify.repository.SongRepository;
import com.winhmm.myspotify.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/*
    =====================================================================
    SongService - NGHIỆP VỤ CỦA NGƯỜI NGHE (UC20 - UC25)
    =====================================================================

    Các chức năng:
        getPublishedSongs(...) → danh sách bài đã phát hành, lọc được theo thể loại (UC20)
        getGenres()            → danh sách thể loại nhạc
        searchSongs(...)       → tìm kiếm bài (UC21)
        getPublishedSong(...)  → chi tiết 1 bài (UC22)
        play(...)              → tăng lượt nghe và ghi lịch sử nghe (UC24)
        getArtist(...)         → thông tin 1 Artist cho trang nghệ sĩ (UC25)
        getArtistSongs(...)    → bài của 1 Artist cho trang nghệ sĩ (UC25)

    Được gọi từ SongController và GenreController. Mọi tài khoản đã đăng nhập đều dùng được.

    =====================================================================
    CHỈ TRẢ BÀI "ĐÃ PHÁT HÀNH"
    =====================================================================

    Mọi truy vấn dùng điều kiện PUBLISHED_CONDITION trong SongRepository
    (APPROVED, đã tới giờ phát hành, Artist chính đang hoạt động).

    Mọi truy vấn truyền LocalDateTime.now() vào
    → bài hẹn lịch tự xuất hiện đúng giờ, không cần tác vụ hẹn giờ nào chạy nền.
    LocalDateTime.now() là giờ của máy chủ, nên server phải chạy múi giờ
    Asia/Ho_Chi_Minh thì lịch mới đúng giờ Việt Nam.
*/
@Service
public class SongService {
    private static final int MAX_PAGE_SIZE = 50;

    /*
        Cùng 1 tài khoản nghe cùng 1 bài → chỉ tính 1 lượt trong 30 giây.
    */
    private static final Duration PLAY_COOLDOWN = Duration.ofSeconds(30);

    private final SongRepository songRepository;
    private final UserRepository userRepository;
    private final GenreRepository genreRepository;

    /*
        StringRedisTemplate: công cụ Spring cung cấp để gửi lệnh tới Redis
        (key và value đều là chuỗi). Dùng chung với SessionService, RateLimitService.
    */
    private final StringRedisTemplate redisTemplate;
    private final ListeningHistoryService listeningHistoryService;

    public SongService(SongRepository songRepository,
                       UserRepository userRepository,
                       GenreRepository genreRepository,
                       StringRedisTemplate redisTemplate,
                       ListeningHistoryService listeningHistoryService) {
        this.songRepository = songRepository;
        this.userRepository = userRepository;
        this.genreRepository = genreRepository;
        this.redisTemplate = redisTemplate;
        this.listeningHistoryService = listeningHistoryService;
    }


    /*
        ---------------------------------------------------------------
        UC20 - Danh sách bài đã phát hành
        ---------------------------------------------------------------
        1 trang bài đã phát hành, mới phát hành trước.

        genreId:
        - null       → mọi thể loại (như trước khi có phần thể loại).
        - có giá trị → chỉ bài thuộc thể loại đó (trang Genres của frontend).
    */
    public Page<Song> getPublishedSongs(Long genreId, int page, int size) {
        if(genreId == null) {
            return songRepository.findPublished(LocalDateTime.now(), pageable(page, size));
        }

        return songRepository.findPublishedByGenre(genreId, LocalDateTime.now(), pageable(page, size));
    }

    /*
        ---------------------------------------------------------------
        Danh sách thể loại nhạc
        ---------------------------------------------------------------
        Mọi thể loại, xếp theo tên A → Z. Chỉ có khoảng 25 dòng nên không phân trang.
    */
    public List<Genre> getGenres() {
        return genreRepository.findAllByOrderByNameAsc();
    }

    /*
        ---------------------------------------------------------------
        UC21 - Tìm kiếm
        ---------------------------------------------------------------
        Tìm theo tên bài, nghệ danh Artist chính, nghệ danh Artist feat.

        Từ khóa rỗng hoặc toàn dấu cách → trả danh sách bình thường (như UC20),
        không báo lỗi: người dùng xóa hết ô tìm kiếm thì thấy lại danh sách đầy đủ.

        keyword.trim(): "  may chieu  " → "may chieu" (bỏ dấu cách thừa người dùng lỡ gõ).
    */
    public Page<Song> searchSongs(String keyword, int page, int size) {
        if(keyword == null || keyword.isBlank()) {
            return getPublishedSongs(null, page, size);
        }

        return songRepository.searchPublished(keyword.trim(), LocalDateTime.now(), pageable(page, size));
    }

    /*
        ---------------------------------------------------------------
        UC22 - Chi tiết 1 bài đã phát hành
        ---------------------------------------------------------------
        Bài chưa phát hành, bị ẩn, bị gỡ, Artist bị khóa, hoặc không tồn tại
        → đều báo chung "Song does not exist" (không lộ trạng thái bài cho người ngoài).

        public vì cả SongController và hàm play() bên dưới đều dùng.
    */
    public Song getPublishedSong(Long songId) {
        return songRepository.findPublishedById(songId, LocalDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException("Song does not exist"));
    }

    /*
        ---------------------------------------------------------------
        UC24 - Tăng lượt nghe và ghi lịch sử nghe
        ---------------------------------------------------------------
        Frontend gọi POST /api/songs/{songId}/play khi người dùng bắt đầu nghe 1 bài.
        Tham số email: SongController truyền authentication.getName() (email trong token).

        Các bước:
        1. getPublishedSong(songId): bài phải đang phát hành, không thì 400 "Song does not exist".
           Chỉ gọi để kiểm tra, không dùng kết quả trả về.
        2. Chống đếm trùng bằng Redis:
               setIfAbsent("play:15:a@gmail.com", "1", 30 giây)   ~ lệnh Redis: SET key 1 NX EX 30
               NX = chỉ ghi khi key CHƯA tồn tại.
           - Key chưa có → ghi được → trả true  → lần nghe đầu trong 30 giây → tính lượt.
           - Key đã có  → không ghi → trả false → vừa nghe trong 30 giây → không tính.
           Sau 30 giây Redis tự xóa key → lần sau lại được tính.
           Chống: bấm play liên tục, tua đi tua lại, F5 trang để tăng lượt nghe ảo.
        3. Được tính:
           - songRepository.incrementPlayCount: tăng play_count bằng 1 câu UPDATE (xem SongRepository).
           - Ghi lịch sử nghe vào MongoDB (ListeningHistoryService.record, chạy ở thread riêng).

        -----------------------------------------------------
        Đọc từng dòng
        -----------------------------------------------------
        Boolean firstTime: Boolean (chữ B hoa) là kiểu object, có thể null.
        Boolean.TRUE.equals(firstTime): ra true chỉ khi firstTime đúng là true.
            Viết if (firstTime) mà firstTime là null → NullPointerException.
            Để Boolean.TRUE đứng trước → firstTime là null cũng không lỗi, chỉ ra false.

        catch (Exception e) { shouldCount = true; }: Redis lỗi → vẫn tính lượt (fail-open).
            Đếm dư vài lượt còn hơn làm hỏng chức năng nghe nhạc.

        userRepository.findByEmail(email).ifPresent(user -> ...):
            ifPresent: Optional có giá trị thì chạy đoạn code trong ngoặc, rỗng thì bỏ qua.
            user -> listeningHistoryService.record(user.getId(), songId): lambda nhận user rồi gọi record.
            Phải đổi email thành id, vì MongoDB lưu userId (số), còn token chỉ có email.

        API luôn trả 200 "Play recorded", kể cả khi không được tính lượt (nghe lại trong 30 giây),
        để không lộ quy luật chống spam (xem tài liệu test, mục "Giới hạn đã biết").
    */
    public void play(Long songId, String email) {
        getPublishedSong(songId);

        boolean shouldCount;
        try {
            Boolean firstTime = redisTemplate.opsForValue()
                    .setIfAbsent("play:" + songId + ":" + email, "1", PLAY_COOLDOWN);
            shouldCount = Boolean.TRUE.equals(firstTime);
        } catch (Exception e) {
            shouldCount = true;
        }

        if(shouldCount) {
            songRepository.incrementPlayCount(songId);
            userRepository.findByEmail(email)
                    .ifPresent(user -> listeningHistoryService.record(user.getId(), songId));
        }
    }

    /*
        ---------------------------------------------------------------
        UC25 - Lấy thông tin Artist cho trang nghệ sĩ
        ---------------------------------------------------------------
        Không phải Artist, đã bị khóa, hoặc không tồn tại → báo "Artist does not exist".
        Ẩn trang của Artist bị khóa: khớp với việc mọi bài của họ cũng tự ẩn
        (điều kiện Artist ACTIVE trong PUBLISHED_CONDITION).
    */
    public User getArtist(Long artistId) {
        User artist = userRepository.findById(artistId)
                .orElseThrow(() -> new IllegalArgumentException("Artist does not exist"));

        if(artist.getRole() != Role.ARTIST || artist.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Artist does not exist");
        }

        return artist;
    }

    /*
        ---------------------------------------------------------------
        UC25 - Bài của 1 Artist cho trang nghệ sĩ
        ---------------------------------------------------------------
        Bài đã phát hành mà Artist này là Artist chính hoặc được feat.
    */
    public Page<Song> getArtistSongs(Long artistId, int page, int size) {
        return songRepository.findPublishedByArtist(artistId, LocalDateTime.now(), pageable(page, size));
    }

    /*
        ---------------------------------------------------------------
        Hàm hỗ trợ: phân trang an toàn, mới phát hành lên đầu
        ---------------------------------------------------------------
        - page < 0 → 0.
        - size ngoài khoảng 1 - 50 → tự chỉnh lại.

        Sắp xếp theo "releaseAt" (ngày phát hành), khác Admin sắp theo "createdAt" (ngày tải lên):
        người nghe quan tâm bài nào mới ra, Admin quan tâm bài nào mới gửi.
    */
    private Pageable pageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "releaseAt"));
    }
}