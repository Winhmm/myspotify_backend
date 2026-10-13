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
    - Tăng lượt nghe + ghi lịch sử nghe (UC24).
    - Trang của 1 Artist (UC25).

    Chỉ trả về bài "đã phát hành" (xem PUBLISHED_CONDITION trong SongRepository).
    Mọi truy vấn truyền LocalDateTime.now() → bài hẹn lịch tự xuất hiện đúng giờ.
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
    private final ListeningHistoryService listeningHistoryService;

    public SongService(SongRepository songRepository,
                       UserRepository userRepository,
                       StringRedisTemplate redisTemplate,
                       ListeningHistoryService listeningHistoryService) {
        this.songRepository = songRepository;
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
        this.listeningHistoryService = listeningHistoryService;
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

    /* ================= UC24: TĂNG LƯỢT NGHE + GHI LỊCH SỬ ================= */

    /*
        Frontend gọi khi bắt đầu phát bài.

        1. Chỉ xử lý bài ĐÃ PHÁT HÀNH.
        2. Chống nghe ảo bằng Redis:
               SET play:<songId>:<email> 1 NX EX 30
           Lần đầu trong 30 giây → được tính. Đã có key → bỏ qua.
           Redis lỗi → vẫn tính (lượt nghe không phải dữ liệu bảo mật).
        3. Được tính thì:
           - Tăng play_count trong MySQL (1 câu UPDATE, nhiều người nghe vẫn đếm đúng).
           - Ghi lịch sử vào MongoDB.
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

            /*
                Ghi lịch sử theo userId (không đổi), không theo email (có thể đổi).

                ifPresent(user -> ...): chỉ chạy NẾU tìm thấy User. Tương đương:
                    Optional<User> result = userRepository.findByEmail(email);
                    if (result.isPresent()) {
                        listeningHistoryService.record(result.get().getId(), songId);
                    }

                Chỉ ghi khi lượt nghe được tính → nghe lại liên tục không làm lịch sử bị lặp.
            */
            userRepository.findByEmail(email)
                    .ifPresent(user -> listeningHistoryService.record(user.getId(), songId));
        }
    }

    /* ================= UC25: TRANG ARTIST ================= */

    /*
        Không phải Artist / đã bị khóa → báo "Artist does not exist".
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