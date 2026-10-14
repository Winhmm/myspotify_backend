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
    =====================================================================
    ArtistSongService - NGHIỆP VỤ CỦA ARTIST VỚI BÀI HÁT CỦA MÌNH
    =====================================================================

    Các chức năng:
        uploadSong(...)            → đăng bài (UC26)
        getMySongs(...)            → bài hát của tôi (UC28)
        updateSong(...)            → sửa tên và danh sách feat (UC29)
        updateCover(...)           → đổi ảnh bìa (UC30)
        updateReleaseSchedule(...) → đổi lịch phát hành (UC31)
        hideSong(...), showSong(...) → ẩn, hiện lại bài (UC32)
        deleteSong(...)            → xóa bài (UC33)
        searchArtists(...)         → tìm nghệ sĩ để feat (UC37)

    Được gọi từ ArtistSongController.

    =====================================================================
    2 LỚP KIỂM TRA Ở MỌI THAO TÁC THAY ĐỔI DỮ LIỆU
    =====================================================================

    1. getArtist(artistId): người đang đăng nhập phải là Artist đang ACTIVE.

       Controller đã có @PreAuthorize("hasRole('ARTIST')"), và filter đã chặn
       tài khoản bị khóa, vậy vì sao Service còn kiểm tra lại?
       → Phòng thủ nhiều lớp (defense in depth): Service tự đảm bảo quy tắc của mình,
         không phụ thuộc vào việc nơi gọi đã kiểm tra hay chưa.
         VD sau này có Service khác gọi vào, hoặc viết test gọi thẳng Service,
         quy tắc "chỉ Artist đang hoạt động mới quản lý bài" vẫn được giữ.

    2. getMySong(artistId, songId): bài phải thuộc về chính Artist đó.
       → Artist không thể đụng vào bài của người khác.

    =====================================================================
    QUẢN LÝ FILE: KHÔNG ĐỂ LẠI FILE RÁC
    =====================================================================

    File (nhạc, ảnh) nằm trên ổ đĩa, database chỉ lưu đường dẫn.
    Hai nơi này không chung 1 transaction: database lỗi không tự xóa file đã ghi,
    và xóa file thì không "hoàn tác" được.
    → Mọi chỗ ghi file đều có try / catch để tự dọn file khi bước sau thất bại.
    → Chỗ xóa file luôn đặt sau khi lưu database thành công.
*/
@Service
public class ArtistSongService {
    private static final int MAX_FEATURED_ARTISTS = 5;

    /*
        Thư mục con chứa ảnh bìa (bên trong thư mục upload gốc).
    */
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

    /*
        ---------------------------------------------------------------
        UC26 - Tải lên bài hát
        ---------------------------------------------------------------

        Các bước:
        1. Kiểm tra người đăng là Artist đang hoạt động.
        2. Kiểm tra lịch phát hành (nếu có phải ở tương lai).
        3. Kiểm tra danh sách nghệ sĩ feat.
        4. Lưu file nhạc → đọc thời lượng (file giả thì báo lỗi).
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
            Lưu file nhạc lên ổ đĩa. Từ đây trở đi, nếu có lỗi phải xóa file này.
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
        } catch(RuntimeException exception) {
            fileStorageService.delete(audioUrl);
            fileStorageService.delete(coverUrl);
            throw exception;
        }
    }

    /*
        ---------------------------------------------------------------
        UC28 - Bài hát của tôi
        ---------------------------------------------------------------

        Mọi trạng thái trừ DELETED, mới tải lên trước.
    */
    public List<Song> getMySongs(Long artistId) {
        return songRepository.findByArtistIdAndStatusNotOrderByCreatedAtDesc(artistId, SongStatus.DELETED);
    }

    /*
        ---------------------------------------------------------------
        UC37 - Tìm nghệ sĩ để feat
        ---------------------------------------------------------------
    */
    public List<User> searchArtists(Long artistId, String keyword) {
        if(keyword == null || keyword.isBlank()) {
            return List.of();
        }

        return userRepository.findTop10ByRoleAndAccountStatusAndArtistNameContainingIgnoreCaseAndIdNot(
                Role.ARTIST, AccountStatus.ACTIVE, keyword.trim(), artistId);
    }

    /*
        ---------------------------------------------------------------
        UC29 - Sửa tên bài và thay thế danh sách feat
        ---------------------------------------------------------------

        - Không giới hạn trạng thái: sửa được bài PENDING, APPROVED, HIDDEN, REJECTED.
          (Bài đã phát hành vẫn sửa tên được, VD sửa lỗi chính tả).

        - Bài REJECTED → sửa xong tự về PENDING (gửi duyệt lại).
    */
    public Song updateSong(Long artistId, Long songId, String title, List<Long> featuredArtistIds) {
        getArtist(artistId);
        Song song = getMySong(artistId, songId);

        song.setTitle(title.trim());
        song.setFeaturedArtists(loadFeaturedArtists(artistId, featuredArtistIds));
        resubmitIfRejected(song);

        return songRepository.save(song);
    }

    /*
        ---------------------------------------------------------------
        UC30 - Đổi ảnh bìa
        ---------------------------------------------------------------

        Thứ tự an toàn:
        1. Lưu ảnh mới lên ổ đĩa.
        2. Cập nhật bài với đường dẫn ảnh mới, lưu database.
        3. Lưu thành công → mới xóa ảnh cũ (tránh đầy ổ đĩa).

        Bài REJECTED đổi ảnh bìa → tự gửi duyệt lại (thường bị từ chối chính vì ảnh bìa).
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
        } catch(RuntimeException exception) {
            fileStorageService.delete(newCoverUrl);
            throw exception;
        }
    }

    /*
        ---------------------------------------------------------------
        UC31 - Đổi lịch phát hành
        ---------------------------------------------------------------
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

    /*
        ---------------------------------------------------------------
        UC32 - Ẩn bài (APPROVED → HIDDEN)
        ---------------------------------------------------------------

        Người nghe không thấy nữa, nhưng giữ nguyên lượt nghe, file, lịch phát hành.

        Bài chưa tới lịch cũng ẩn được (VD Artist đổi ý, tạm hoãn phát hành).
    */
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
        ---------------------------------------------------------------
        UC32 - Hiện lại bài đang ẩn (HIDDEN → APPROVED)
        ---------------------------------------------------------------

        Không cần Admin duyệt lại, vì bài đã được duyệt trước khi bị ẩn.

        Lịch phát hành giữ nguyên: bài ẩn khi chưa tới lịch → hiện lại vẫn chờ đúng lịch đó.
    */
    public Song showSong(Long artistId, Long songId) {
        getArtist(artistId);
        Song song = getMySong(artistId, songId);

        if(song.getStatus() != SongStatus.HIDDEN) {
            throw new IllegalArgumentException("Only hidden songs can be shown again");
        }

        song.setStatus(SongStatus.APPROVED);
        return songRepository.save(song);
    }

    /*
        ---------------------------------------------------------------
        UC33 - Xóa bài (xóa mềm)
        ---------------------------------------------------------------

        Đổi trạng thái DELETED, giữ bản ghi trong database (lịch sử, lượt nghe cũ).

        Xóa hẳn file nhạc và ảnh bìa khỏi ổ đĩa (không chiếm dung lượng).
    */
    public void deleteSong(Long artistId, Long songId) {
        getArtist(artistId);
        Song song = getMySong(artistId, songId);

        song.setStatus(SongStatus.DELETED);
        songRepository.save(song);

        fileStorageService.delete(song.getAudioUrl());
        fileStorageService.delete(song.getCoverUrl());
    }

    /*
        ---------------------------------------------------------------
        Hàm hỗ trợ: lấy Artist và kiểm tra đang hoạt động
        ---------------------------------------------------------------
    */
    private User getArtist(Long artistId) {
        User artist = userRepository.findById(artistId)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));

        if(artist.getRole() != Role.ARTIST || artist.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Only active artists can manage songs");
        }

        return artist;
    }

    /*
        ---------------------------------------------------------------
        Hàm hỗ trợ: lịch phát hành phải ở tương lai
        ---------------------------------------------------------------
    */
    private void checkFutureTime(LocalDateTime releaseAt) {
        if(releaseAt != null && !releaseAt.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Release time must be in the future");
        }
    }

    /*
        ---------------------------------------------------------------
        Hàm hỗ trợ: kiểm tra và tải danh sách nghệ sĩ feat
        ---------------------------------------------------------------

        Quy tắc:
        - Tối đa 5 người, không trùng nhau.
        - Không chứa chính Artist chính.
        - Mỗi người phải tồn tại, có role ARTIST và đang ACTIVE.
    */
    private Set<User> loadFeaturedArtists(Long artistId, List<Long> ids) {
        Set<User> result = new HashSet<>();

        if(ids == null || ids.isEmpty()) {
            return result;
        }

        Set<Long> uniqueIds = new HashSet<>(ids);

        uniqueIds.remove(null);

        if(uniqueIds.size() > MAX_FEATURED_ARTISTS) {
            throw new IllegalArgumentException("Maximum " + MAX_FEATURED_ARTISTS + " featured artists");
        }

        if(uniqueIds.contains(artistId)) {
            throw new IllegalArgumentException("You cannot feature yourself");
        }

        for (User user : userRepository.findAllById(uniqueIds)) {
            if(user.getRole() != Role.ARTIST || user.getAccountStatus() != AccountStatus.ACTIVE) {
                throw new IllegalArgumentException("Featured artist is not available: " + user.getId());
            }

            result.add(user);
        }

        if(result.size() != uniqueIds.size()) {
            throw new IllegalArgumentException("Some featured artists do not exist");
        }

        return result;
    }

    /*
        ---------------------------------------------------------------
        Hàm hỗ trợ: lấy bài hát của chính Artist đang đăng nhập
        ---------------------------------------------------------------
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
        ---------------------------------------------------------------
        Hàm hỗ trợ: bài đã tới giờ phát hành chưa?
        ---------------------------------------------------------------
    */
    private boolean isReleased(Song song) {
        return wasApproved(song)
                && song.getReleaseAt() != null
                && !song.getReleaseAt().isAfter(LocalDateTime.now());
    }

    /*
         ---------------------------------------------------------------
        Hàm hỗ trợ: bài bị từ chối mà Artist đã sửa → tự gửi duyệt lại
        ---------------------------------------------------------------
    */
    private void resubmitIfRejected(Song song) {
        if(song.getStatus() == SongStatus.REJECTED) {
            song.setStatus(SongStatus.PENDING);
            song.setRejectReason(null);
        }
    }

    /*
        ---------------------------------------------------------------
        Hàm hỗ trợ: bài đã được Admin duyệt chưa?
        ---------------------------------------------------------------
    */
    private boolean wasApproved(Song song) {
        return song.getStatus() == SongStatus.APPROVED
                || song.getStatus() == SongStatus.HIDDEN;
    }
}