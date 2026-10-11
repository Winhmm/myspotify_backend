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
    Các chức năng của NGƯỜI NGHE (mọi tài khoản đã đăng nhập):
    - Danh sách bài đã phát hành (UC20).
    - Tìm kiếm bài (UC21).
    - Xem chi tiết bài (UC22).
    - Tăng lượt nghe (UC24).
    - Trang của 1 Artist (UC25).

    Chỉ trả về bài "đã phát hành" (xem PUBLISHED_CONDITION trong SongRepository).
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
    private final StringRedisTemplate redisTemplate;

    public SongService(SongRepository songRepository,
                       UserRepository userRepository,
                       StringRedisTemplate redisTemplate) {
        this.songRepository = songRepository;
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
    }

    /* ================= UC20: DANH SÁCH BÀI ================= */

    public Page<Song> getPublishedSongs(int page, int size) {
        return songRepository.findPublished(LocalDateTime.now(), pageable(page, size));
    }

    /* ================= UC21: TÌM KIẾM ================= */

    /*
        Từ khóa rỗng → trả danh sách bình thường (như UC20).
    */
    public Page<Song> searchSongs(String keyword, int page, int size) {
        if(keyword == null || keyword.isBlank()) {
            return getPublishedSongs(page, size);
        }

        return songRepository.searchPublished(keyword.trim(), LocalDateTime.now(), pageable(page, size));
    }

    /* ================= UC22: CHI TIẾT BÀI ================= */

    /*
        Bài chưa phát hành / bị ẩn / bị gỡ / không tồn tại → đều báo "không tồn tại".
    */
    public Song getPublishedSong(Long songId) {
        return songRepository.findPublishedById(songId, LocalDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException("Song does not exist"));
    }

    /* ================= UC24: TĂNG LƯỢT NGHE ================= */

    /*
        Frontend gọi khi bắt đầu phát bài.

        Chống tăng lượt nghe ảo bằng Redis:
            key:   play:<songId>:<email>     VD: play:15:phong@gmail.com
            Tương đương lệnh: SET key 1 NX EX 30
            (NX = chỉ ghi nếu key CHƯA có)
        - Ghi được (lần đầu trong 30 giây) → tăng lượt nghe.
        - Key đã có (vừa nghe xong) → bỏ qua, không tính thêm.
        - Redis lỗi → vẫn tăng lượt nghe, không để chức năng phát nhạc bị lỗi theo.
    */
    public void play(Long songId, String email) {
        getPublishedSong(songId);

        /*
            true: cho phép tăng lượt nghe.
            false: bỏ qua, không tăng lượt nghe.
        */
        boolean shouldCount;
        try {
            /*
                Tạo key Redis và kiểm tra.
            */
            Boolean firstTime = redisTemplate.opsForValue()
                    .setIfAbsent("play:" + songId + ":" + email, "1", PLAY_COOLDOWN);
            /*
                Nếu lần đầu tiên thì gán bằng true.
            */
            shouldCount = Boolean.TRUE.equals(firstTime);
        } catch (Exception e) {
            /*
                Redis lỗi: vẫn tăng lượt nghe để tránh ảnh hưởng chức năng phát nhạc.
            */
            shouldCount = true;
        }

        if(shouldCount) {
            songRepository.incrementPlayCount(songId);
        }
    }

    /* ================= UC25: TRANG ARTIST ================= */

     /*
        Lấy Artist để hiển thị trang của họ.
        Không phải Artist / đã bị khóa → báo "không tồn tại".
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
        Bài đã phát hành mà Artist là Artist chính hoặc được feat.
    */
     public Page<Song> getArtistSongs(Long artistId, int page, int size) {
         return songRepository.findPublishedByArtist(artistId, LocalDateTime.now(), pageable(page, size));
     }

    /* ================= HÀM HỖ TRỢ ================= */

    /*
        Phân trang an toàn, mới phát hành lên đầu.
        page < 0 → 0. size ngoài khoảng 1 - 50 → tự chỉnh lại.
    */
    private Pageable pageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "releaseAt"));
    }
}
