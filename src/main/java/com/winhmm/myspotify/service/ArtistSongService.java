package com.winhmm.myspotify.service;

import com.winhmm.myspotify.entity.Song;
import com.winhmm.myspotify.entity.User;
import com.winhmm.myspotify.enums.AccountStatus;
import com.winhmm.myspotify.enums.Role;
import com.winhmm.myspotify.enums.SongStatus;
import com.winhmm.myspotify.repository.SongRepository;
import com.winhmm.myspotify.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
    Các chức năng của ARTIST với bài hát:
    - Đăng bài (UC26).
    - Xem bài của tôi (UC28).
    - Tìm nghệ sĩ để feat. (UC37).

    Mọi thao tác đều kiểm tra bài hát phải thuộc về Artist đang đăng nhập.
*/
@Service
public class ArtistSongService {
    private static final int MAX_FEATURED_ARTISTS = 5;
    private static final String COVER_FOLDER = "songs/covers";

    private final SongRepository songRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public ArtistSongService(SongRepository songRepository,
                             UserRepository userRepository,
                             FileStorageService fileStorageService) {
        this.songRepository = songRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    /* ================= UC26: TẢI LÊN BÀI HÁT ================= */

    /*
        Các bước:
        1. Kiểm tra người đăng là Artist đang hoạt động.
        2. Kiểm tra lịch phát hành (nếu có phải ở tương lai).
        3. Kiểm tra danh sách nghệ sĩ feat.
        4. Lưu file nhạc → đọc thời lượng (file giả thì xóa file và báo lỗi).
        5. Lưu ảnh bìa (nếu có).
        6. Lưu bài hát vào database với trạng thái PENDING (chờ Admin duyệt).
    */
    public Song uploadSong(Long artistId, String title, MultipartFile audioFile,
                           MultipartFile coverFile, List<Long> featuredArtistIds,
                           LocalDateTime releaseAt) {
        User artist = getArtist(artistId);

        checkFutureTime(releaseAt);
        Set<User> featuredArtists = loadFeaturedArtists(artistId, featuredArtistIds);

        /*
            Lưu file nhạc rồi đọc thời lượng.
            File không phải nhạc thật → xóa file vừa lưu, báo lỗi.
        */
        String audioUrl = fileStorageService.saveAudio(audioFile);
        int duration;
        try {
            duration = fileStorageService.getAudioDuration(audioUrl);
        } catch (IllegalArgumentException e) {
            fileStorageService.delete(audioUrl);
            throw e;
        }

        /*
            Lưu ảnh bìa (không bắt buộc).
            Ảnh lỗi → xóa file nhạc vừa lưu để không để lại file rác.
        */
        String coverUrl = null;
        if (coverFile != null && !coverFile.isEmpty()) {
            try {
                coverUrl = fileStorageService.saveImage(coverFile, COVER_FOLDER);
            } catch (IllegalArgumentException e) {
                fileStorageService.delete(audioUrl);
                throw e;
            }
        }

        Song song = new Song();
        song.setArtist(artist);
        song.setTitle(title.trim());
        song.setAudioUrl(audioUrl);
        song.setCoverUrl(coverUrl);
        song.setDurationSeconds(duration);
        song.setFeaturedArtists(featuredArtists);
        song.setReleaseAt(releaseAt);
        song.setStatus(SongStatus.PENDING);

        return songRepository.save(song);
    }

    /* ================= UC28: BÀI HÁT CỦA TÔI ================= */

    /*
        Mọi trạng thái trừ DELETED, mới nhất lên đầu.
    */
    public List<Song> getMySongs(Long artistId) {
        return songRepository.findByArtistIdAndStatusNotOrderByCreatedAtDesc(artistId, SongStatus.DELETED);
    }

    /* ================= UC37: TÌM NGHỆ SĨ ĐỂ FEAT. ================= */

    /*
        Tìm Artist đang hoạt động theo nghệ danh (tối đa 10 người).
        Chính mình đã được loại ngay trong câu truy vấn (AndIdNot).
    */
    public List<User> searchArtists(Long artistId, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        return userRepository.findTop10ByRoleAndAccountStatusAndArtistNameContainingIgnoreCaseAndIdNot(
                Role.ARTIST, AccountStatus.ACTIVE, keyword.trim(), artistId);
    }

    /* ================= HÀM HỖ TRỢ ================= */

    /*
        Lấy Artist theo id, kiểm tra đúng role ARTIST và đang ACTIVE.
    */
    private User getArtist(Long artistId) {
        User artist = userRepository.findById(artistId)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));

        if (artist.getRole() != Role.ARTIST || artist.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Only active artists can manage songs");
        }

        return artist;
    }

    /*
        Lịch phát hành (nếu có) phải ở tương lai.
        releaseAt = null → phát hành ngay sau khi duyệt → hợp lệ.
    */
    private void checkFutureTime(LocalDateTime releaseAt) {
        if (releaseAt != null && !releaseAt.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Release time must be in the future");
        }
    }

    /*
        Kiểm tra và lấy danh sách nghệ sĩ feat.:
        - Tối đa 5 người, không trùng nhau.
        - Không chứa chính Artist chính.
        - Mỗi người phải tồn tại, có role ARTIST và đang ACTIVE.
    */
    private Set<User> loadFeaturedArtists(Long artistId, List<Long> ids) {
        Set<User> result = new HashSet<>();

        if (ids == null || ids.isEmpty()) {
            return result;
        }

        /*
            Set tự loại bỏ id trùng, VD: [12, 12, 13] → {12, 13}
        */
        Set<Long> uniqueIds = new HashSet<>(ids);

        if (uniqueIds.size() > MAX_FEATURED_ARTISTS) {
            throw new IllegalArgumentException("Maximum " + MAX_FEATURED_ARTISTS + " featured artists");
        }

        if (uniqueIds.contains(artistId)) {
            throw new IllegalArgumentException("You cannot feature yourself");
        }

        for (User user : userRepository.findAllById(uniqueIds)) {
            if (user.getRole() != Role.ARTIST || user.getAccountStatus() != AccountStatus.ACTIVE) {
                throw new IllegalArgumentException("Featured artist is not available: " + user.getId());
            }

            result.add(user);
        }

        /*
            Số người tìm được ít hơn số id gửi lên → có id không tồn tại.
        */
        if (result.size() != uniqueIds.size()) {
            throw new IllegalArgumentException("Some featured artists do not exist");
        }

        return result;
    }
}
