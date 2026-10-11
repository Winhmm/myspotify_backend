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
    - Sửa bài, đổi ảnh bìa, đổi lịch, ẩn / hiện, xóa bài (UC29 - UC33).
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
        4. Lưu file nhạc → đọc thời lượng (file giả thì báo lỗi).
        5. Lưu ảnh bìa (nếu có).
        6. Lưu bài hát vào database với trạng thái PENDING (chờ Admin duyệt).

        Từ bước 4 trở đi, bất kỳ bước nào lỗi (file nhạc giả, ảnh lỗi, database lỗi...)
        → xóa các file đã lưu, không để lại file rác trên ổ đĩa.
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
        String coverUrl = null;

        try {
            int duration = fileStorageService.getAudioDuration(audioUrl);

            if(coverFile != null && !coverFile.isEmpty()) {
                coverUrl = fileStorageService.saveImage(coverFile, COVER_FOLDER);
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
        } catch (RuntimeException exception) {
            /*
                delete(null) đã an toàn sẵn → không cần kiểm tra coverUrl có null không.
            */
            fileStorageService.delete(audioUrl);
            fileStorageService.delete(coverUrl);
            throw exception;
        }
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

    /* ================= UC29: SỬA TÊN BÀI + FEAT. ================= */

    public Song updateSong(Long artistId, Long songId, String title, List<Long> featuredArtistIds) {
        getArtist(artistId);
        Song song = getMySong(artistId, songId);

        song.setTitle(title.trim());
        song.setFeaturedArtists(loadFeaturedArtists(artistId, featuredArtistIds));
        resubmitIfRejected(song);

        return songRepository.save(song);
    }

    /* ================= UC30: ĐỔI ẢNH BÌA ================= */

    /*
        - Lưu ảnh mới → lưu bài thành công → xóa ảnh cũ (tránh đầy ổ đĩa).
        - Lưu bài lỗi → xóa ảnh mới vừa lưu, giữ nguyên ảnh cũ.
    */
    public Song updateCover(Long artistId, Long songId, MultipartFile coverFile) {
        getArtist(artistId);
        Song song = getMySong(artistId, songId);

        String oldCoverUrl = song.getCoverUrl();
        String newCoverUrl = fileStorageService.saveImage(coverFile, COVER_FOLDER);

        try {
            song.setCoverUrl(newCoverUrl);
            resubmitIfRejected(song);

            Song saved = songRepository.save(song);
            fileStorageService.delete(oldCoverUrl);

            return saved;
        } catch (RuntimeException e) {
            fileStorageService.delete(newCoverUrl);
            throw e;
        }
    }

    /* ================= UC31: ĐỔI LỊCH PHÁT HÀNH ================= */

    /*
        - Bài đã phát hành (kể cả đang bị ẩn) → không đổi được nữa.
        - releaseAt = null:
            + Bài PENDING            → phát hành ngay sau khi Admin duyệt.
            + Bài APPROVED / HIDDEN  → đã duyệt rồi → gán = bây giờ.
              (Không để null, vì bài đã duyệt mà releaseAt = null
               sẽ không bao giờ hiện cho người nghe.)
    */
    public Song updateReleaseSchedule(Long artistId, Long songId, LocalDateTime releaseAt) {
        getArtist(artistId);
        Song song = getMySong(artistId, songId);

        if(isReleased(song)) {
            throw new IllegalArgumentException("Cannot change the schedule of a released song");
        }

        checkFutureTime(releaseAt);

        if(releaseAt == null && wasApproved(song)) {
            song.setReleaseAt(LocalDateTime.now());
        } else {
            song.setReleaseAt(releaseAt);
        }

        return songRepository.save(song);
    }

    /* ================= UC32: ẨN / HIỆN BÀI ================= */

    public Song hideSong(Long artistId, Long songId) {
        getArtist(artistId);
        Song song = getMySong(artistId, songId);

        if(song.getStatus() != SongStatus.APPROVED) {
            throw new IllegalArgumentException("Only approved songs can be hidden");
        }

        song.setStatus(SongStatus.HIDDEN);
        return songRepository.save(song);
    }

    /*
        Hiện lại bài đang ẩn.
        Trả về APPROVED vì bài đã được duyệt từ trước, không cần duyệt lại.
    */
    public Song showSong(Long artistId, Long songId) {
        getArtist(artistId);
        Song song = getMySong(artistId, songId);

        if(song.getStatus() != SongStatus.HIDDEN) {
            throw new IllegalArgumentException("Only hidden songs can be shown again");
        }

        /*
            Trả về APPROVED nếu bài đã được duyệt từ trước, không cần duyệt lại.
        */
        song.setStatus(SongStatus.APPROVED);
        return songRepository.save(song);
    }

    /* ================= UC33: XÓA BÀI ================= */

    /*
        Xóa mềm: đổi trạng thái DELETED (giữ lại bản ghi trong database),
        nhưng xóa hẳn file nhạc và ảnh bìa để không chiếm ổ đĩa.

        File chỉ bị xóa sau khi lưu trạng thái DELETED thành công.
    */
    public void deleteSong(Long artistId, Long songId) {
        getArtist(artistId);
        Song song = getMySong(artistId, songId);

        song.setStatus(SongStatus.DELETED);
        songRepository.save(song);

        fileStorageService.delete(song.getAudioUrl());
        fileStorageService.delete(song.getCoverUrl());
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

        /*
            Bỏ giá trị rỗng (form gửi ô featuredArtistIds để trống → null).
        */
        uniqueIds.remove(null);

        if(uniqueIds.isEmpty()) {
            return result;
        }

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

    /*
        Lấy bài hát của chính Artist đang đăng nhập.
        - Không tìm thấy, hoặc là bài của người khác → báo "không tồn tại".
        - Bài đã xóa → coi như không tồn tại.
    */
    private Song getMySong(Long artistId, Long songId) {
        Song song = songRepository.findByIdAndArtistId(songId, artistId)
                .orElseThrow(() -> new IllegalArgumentException("Song does not exist"));

        if(song.getStatus() == SongStatus.DELETED) {
            throw new IllegalArgumentException("Song does not exist");
        }

        return song;
    }

    /*
        Bài đã tới giờ phát hành chưa?
        Bài đang ẩn vẫn tính theo lịch → ẩn bài không giúp lách luật đổi lịch.
    */
    private boolean isReleased(Song song) {
        return wasApproved(song)
                && song.getReleaseAt() != null
                && !song.getReleaseAt().isAfter(LocalDateTime.now());
    }

    /*
        Bài bị từ chối mà Artist đã sửa → gửi duyệt lại, xóa lý do từ chối cũ.
    */
    private void resubmitIfRejected(Song song) {
        if(song.getStatus() == SongStatus.REJECTED) {
            song.setStatus(SongStatus.PENDING);
            song.setRejectReason(null);
        }
    }

    /*
        Bài đã được Admin duyệt chưa?
        HIDDEN cũng tính là đã duyệt (chỉ ẩn được bài APPROVED).
    */
    private boolean wasApproved(Song song) {
        return song.getStatus() == SongStatus.APPROVED
                || song.getStatus() == SongStatus.HIDDEN;
    }
}
