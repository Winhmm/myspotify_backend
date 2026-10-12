package com.winhmm.myspotify.service;

import com.winhmm.myspotify.entity.Song;
import com.winhmm.myspotify.entity.User;
import com.winhmm.myspotify.enums.AccountStatus;
import com.winhmm.myspotify.enums.Role;
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

/*
    ===================================================================
    SongService - Nghiệp vụ của NGƯỜI NGHE (mọi tài khoản đã đăng nhập).
    ===================================================================

    - Danh sách bài đã phát hành (UC20).
    - Tìm kiếm bài (UC21).
    - Xem chi tiết bài (UC22).
    - Tăng lượt nghe (UC24).
    - Trang của 1 Artist (UC25).

    Chỉ trả về bài "đã phát hành" (xem PUBLISHED_CONDITION trong SongRepository).
    Mọi truy vấn truyền LocalDateTime.now() vào → bài hẹn lịch tự xuất hiện đúng giờ,
    không cần tác vụ hẹn giờ nào chạy nền.
*/
@Service
public class SongService {
    private static final int MAX_PAGE_SIZE = 50;

    /*
        Cùng 1 tài khoản nghe cùng 1 bài → chỉ tính 1 lượt trong 30 giây.
        Duration.ofSeconds(30): kiểu dữ liệu "khoảng thời gian" của Java,
        rõ nghĩa hơn số 30 trơn (30 giây? 30 phút?).
    */
    private static final Duration PLAY_COOLDOWN = Duration.ofSeconds(30);

    private final SongRepository songRepository;
    private final UserRepository userRepository;

    /*
        StringRedisTemplate: công cụ Spring cung cấp để gửi lệnh tới Redis
        (key và value đều là chuỗi). Dùng chung với SessionService, RateLimitService.
    */
    private final StringRedisTemplate redisTemplate;

    public SongService(SongRepository songRepository,
                       UserRepository userRepository,
                       StringRedisTemplate redisTemplate) {
        this.songRepository = songRepository;
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
    }

    /* ================= UC20: DANH SÁCH BÀI ================= */

    /*
        1 trang bài đã phát hành, mới phát hành trước.
    */
    public Page<Song> getPublishedSongs(int page, int size) {
        return songRepository.findPublished(LocalDateTime.now(), pageable(page, size));
    }

    /* ================= UC21: TÌM KIẾM ================= */

    /*
        Tìm theo tên bài, nghệ danh Artist chính, nghệ danh Artist feat.

        Từ khóa rỗng hoặc toàn dấu cách → trả danh sách bình thường (như UC20),
        không báo lỗi: người dùng xóa hết ô tìm kiếm thì thấy lại danh sách đầy đủ.

        keyword.trim(): "  may chieu  " → "may chieu" (bỏ dấu cách thừa người dùng lỡ gõ).
    */
    public Page<Song> searchSongs(String keyword, int page, int size) {
        if(keyword == null || keyword.isBlank()) {
            return getPublishedSongs(page, size);
        }

        return songRepository.searchPublished(keyword.trim(), LocalDateTime.now(), pageable(page, size));
    }

    /* ================= UC22: CHI TIẾT BÀI ================= */

    /*
        Bài chưa phát hành / bị ẩn / bị gỡ / Artist bị khóa / không tồn tại
        → đều báo chung "Song does not exist" (không lộ trạng thái bài cho người ngoài).

        public vì play() bên dưới và SongController đều dùng.
    */
    public Song getPublishedSong(Long songId) {
        return songRepository.findPublishedById(songId, LocalDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException("Song does not exist"));
    }

    /* ================= UC24: TĂNG LƯỢT NGHE ================= */

    /*
        Frontend gọi khi bắt đầu phát bài.

        BƯỚC 1: getPublishedSong(songId)
            Chỉ tăng lượt nghe cho bài ĐÃ PHÁT HÀNH.
            Chặn kiểu gọi API /play cho bài chưa phát hành để "nuôi" lượt nghe trước.

        BƯỚC 2: CHỐNG TĂNG LƯỢT NGHE ẢO BẰNG REDIS
            key:   play:<songId>:<email>     VD: play:15:phong@gmail.com
            Tương đương lệnh Redis: SET play:15:phong@gmail.com 1 NX EX 30
                NX    = chỉ ghi nếu key CHƯA có
                EX 30 = tự xóa key sau 30 giây
            - Ghi được (lần đầu trong 30 giây) → setIfAbsent trả true  → tăng lượt nghe.
            - Key đã có (vừa nghe xong)        → setIfAbsent trả false → bỏ qua.
            - Sau 30 giây Redis tự xóa key → nghe lại được tính tiếp.
            Không có cơ chế này: 1 người gọi API liên tục là đẩy lượt nghe lên hàng nghìn.

            setIfAbsent là thao tác NGUYÊN TỬ (atomic): kiểm tra và ghi xảy ra trong 1 bước
            → 2 request cùng lúc của cùng 1 người, chỉ 1 request ghi được, không có khe hở
              cho race condition (khác với cách sai: GET kiểm tra rồi mới SET).

            Value "1" không quan trọng, chỉ cần key tồn tại.

        BƯỚC 3: Redis lỗi (mất kết nối, Redis tắt) → VẪN tăng lượt nghe.
            Lựa chọn "fail open" (lỗi thì cho qua): lượt nghe không phải dữ liệu bảo mật,
            thà đếm dư vài lượt còn hơn làm hỏng trải nghiệm nghe nhạc.
            (Ngược với OTP: Redis lỗi thì CHẶN, vì đó là bảo mật.)

            Boolean.TRUE.equals(firstTime) thay vì firstTime == true:
            setIfAbsent trả về Boolean (object, có thể null). So sánh kiểu này an toàn với null,
            còn "firstTime == true" sẽ ném NullPointerException nếu firstTime là null.

        BƯỚC 4: incrementPlayCount → UPDATE play_count = play_count + 1 trong MySQL
            → nhiều người nghe cùng lúc vẫn đếm đúng (xem SongRepository).
    */
    public void play(Long songId, String email) {
        getPublishedSong(songId);

        /*
            true : cho phép tăng lượt nghe.
            false: bỏ qua, không tăng lượt nghe.
        */
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
        }
    }

    /* ================= UC25: TRANG ARTIST ================= */

    /*
        Lấy thông tin Artist để hiển thị trang của họ.

        Không phải Artist / đã bị khóa / không tồn tại → báo "Artist does not exist".
        Ẩn trang của Artist bị khóa: khớp với việc mọi bài của họ cũng tự ẩn
        (điều kiện Artist ACTIVE trong PUBLISHED_CONDITION).

        Trả về entity User (không phải DTO), Controller tự lấy các trường cần thiết
        (id, artistName, bio, avatarUrl) để tạo ArtistSongsResponse.
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
        Bài đã phát hành mà Artist là Artist chính HOẶC được feat.
    */
    public Page<Song> getArtistSongs(Long artistId, int page, int size) {
        return songRepository.findPublishedByArtist(artistId, LocalDateTime.now(), pageable(page, size));
    }

    /* ================= HÀM HỖ TRỢ ================= */

    /*
        Phân trang an toàn, MỚI PHÁT HÀNH lên đầu.
        - page < 0 → 0.
        - size ngoài khoảng 1 - 50 → tự chỉnh lại.

        Sắp xếp theo "releaseAt" (ngày phát hành), khác Admin sắp theo "createdAt" (ngày tải lên):
        người nghe quan tâm bài nào MỚI RA, Admin quan tâm bài nào MỚI GỬI.
        VD: bài tải lên từ tháng trước nhưng hẹn phát hành hôm nay → người nghe thấy ở đầu.

        Hàm này giống AdminSongService.getSongs (phần ép page / size).
        Sau này có thêm module mới cũng cần phân trang, có thể tách ra 1 class tiện ích dùng chung.
    */
    private Pageable pageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "releaseAt"));
    }
}