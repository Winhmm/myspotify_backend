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
    ===================================================================
    ArtistSongService - Nghiệp vụ của ARTIST với bài hát của mình.
    ===================================================================

    - Đăng bài (UC26).
    - Xem bài của tôi (UC28).
    - Sửa bài, đổi ảnh bìa, đổi lịch, ẩn / hiện, xóa bài (UC29 - UC33).
    - Tìm nghệ sĩ để feat (UC37).

    ---------- 2 LỚP KIỂM TRA Ở MỌI THAO TÁC ----------

    1. getArtist(artistId): người đang đăng nhập PHẢI là Artist ACTIVE.
       Vì sao kiểm tra lại khi Controller đã có @PreAuthorize("hasRole('ARTIST')")?
       → Token JWT có thể còn hạn trong lúc Admin vừa khóa tài khoản hoặc hạ role.
         Kiểm tra lại từ database đảm bảo dùng trạng thái MỚI NHẤT.

    2. getMySong(artistId, songId): bài PHẢI thuộc về chính Artist đó.
       → Artist không thể đụng vào bài của người khác.

    ---------- QUẢN LÝ FILE: KHÔNG ĐỂ LẠI FILE RÁC ----------

    File (nhạc, ảnh) nằm trên ổ đĩa, database chỉ lưu đường dẫn.
    Hai nơi này KHÔNG chung 1 transaction: database lỗi không tự xóa file đã ghi.
    → Mọi chỗ ghi file đều có try / catch để tự dọn file khi bước sau thất bại.
*/
@Service
public class ArtistSongService {
    /*
        Tối đa 5 nghệ sĩ feat mỗi bài.
    */
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

    /* ================= UC26: TẢI LÊN BÀI HÁT ================= */

    /*
        CÁC BƯỚC:
        1. Kiểm tra người đăng là Artist đang hoạt động.
        2. Kiểm tra lịch phát hành (nếu có phải ở tương lai).
        3. Kiểm tra danh sách nghệ sĩ feat.
        4. Lưu file nhạc → đọc thời lượng (file giả thì báo lỗi).
        5. Lưu ảnh bìa (nếu có).
        6. Lưu bài hát vào database với trạng thái PENDING (chờ Admin duyệt).

        VÌ SAO KIỂM TRA (1-3) TRƯỚC KHI LƯU FILE (4-5)?
        → Kiểm tra dữ liệu chỉ tốn vài mili giây, lưu file 100MB tốn nhiều giây và ổ đĩa.
          Dữ liệu sai thì báo lỗi ngay, khỏi tốn công ghi file rồi lại xóa.

        CHỐNG FILE RÁC (bước 4 trở đi):
        Bất kỳ bước nào lỗi (file nhạc giả, ảnh lỗi, database lỗi...)
        → khối catch xóa các file đã lưu → không để lại file mồ côi trên ổ đĩa.

        VÌ SAO saveAudio NẰM NGOÀI try?
        → saveAudio lỗi thì chưa có file nào được ghi, không có gì phải dọn.
          Chỉ sau khi saveAudio thành công (đã có file trên ổ đĩa) mới vào vùng cần bảo vệ.

        catch (RuntimeException exception) ... throw exception;
        → Bắt lỗi chỉ để DỌN FILE, rồi ném lại nguyên lỗi đó
          → GlobalExceptionHandler vẫn nhận đúng lỗi gốc và trả message chính xác.
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
            /*
                Đọc thời lượng = kiểm tra file có phải nhạc thật không.
                File .txt đổi đuôi thành .mp3 → thư viện đọc nhạc báo lỗi
                → "Invalid or corrupted audio file".
            */
            int duration = fileStorageService.getAudioDuration(audioUrl);

            /*
                Ảnh bìa không bắt buộc.
                isEmpty(): Postman / form gửi key "cover" nhưng không chọn file → bỏ qua.
            */
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

            /*
                Bài mới (id = null) → save() chạy INSERT.
                @PrePersist trong Song tự gán createdAt, updatedAt.
                Hibernate tự chèn các dòng feat vào bảng song_featured_artists.
            */
            return songRepository.save(song);
        } catch (RuntimeException exception) {
            /*
                delete(null) đã an toàn sẵn → không cần kiểm tra coverUrl có null không
                (lỗi xảy ra trước khi lưu ảnh thì coverUrl vẫn là null).
            */
            fileStorageService.delete(audioUrl);
            fileStorageService.delete(coverUrl);
            throw exception;
        }
    }

    /* ================= UC28: BÀI HÁT CỦA TÔI ================= */

    /*
        Mọi trạng thái trừ DELETED, mới tải lên trước.
        Không gọi getArtist() vì chỉ ĐỌC bài của chính mình, không thay đổi gì.
    */
    public List<Song> getMySongs(Long artistId) {
        return songRepository.findByArtistIdAndStatusNotOrderByCreatedAtDesc(artistId, SongStatus.DELETED);
    }

    /* ================= UC37: TÌM NGHỆ SĨ ĐỂ FEAT ================= */

    /*
        Tìm Artist đang hoạt động theo nghệ danh (tối đa 10 người).

        keyword rỗng → trả danh sách rỗng ngay, không truy vấn database
        (tránh trả về 10 nghệ sĩ bất kỳ khi người dùng chưa gõ gì).
        List.of(): danh sách rỗng không sửa được, gọn hơn new ArrayList<>().

        Tên method dài là derived query, Spring đọc tên để tự sinh truy vấn:
            findTop10By                  → LIMIT 10
            RoleAnd...                   → role = 'ARTIST'
            AccountStatusAnd...          → account_status = 'ACTIVE'
            ArtistNameContainingIgnoreCase → LOWER(artist_name) LIKE LOWER('%keyword%')
            AndIdNot                     → id <> artistId (loại chính mình)
    */
    public List<User> searchArtists(Long artistId, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        return userRepository.findTop10ByRoleAndAccountStatusAndArtistNameContainingIgnoreCaseAndIdNot(
                Role.ARTIST, AccountStatus.ACTIVE, keyword.trim(), artistId);
    }

    /* ================= UC29: SỬA TÊN BÀI + FEAT ================= */

    /*
        Sửa tên bài và THAY THẾ toàn bộ danh sách feat.

        - Không giới hạn trạng thái: sửa được bài PENDING, APPROVED, HIDDEN, REJECTED.
          (Bài đã phát hành vẫn sửa tên được, VD sửa lỗi chính tả.)
        - Bài REJECTED → sửa xong tự về PENDING (gửi duyệt lại).

        getArtist(artistId): chỉ để KIỂM TRA, không cần giá trị trả về.
    */
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
        THỨ TỰ AN TOÀN:
        1. Lưu ảnh MỚI lên ổ đĩa.
        2. Cập nhật bài với đường dẫn ảnh mới, lưu database.
        3. Lưu thành công → mới XÓA ảnh CŨ (tránh đầy ổ đĩa).

        Nếu bước 2 lỗi (VD 409 do xung đột):
        → catch xóa ảnh MỚI vừa lưu, ảnh CŨ còn nguyên, bài vẫn hiển thị ảnh cũ bình thường.

        Vì sao không xóa ảnh cũ trước?
        → Xóa trước mà bước 2 lỗi thì bài trỏ tới 1 file không còn tồn tại → ảnh vỡ.

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
        } catch (RuntimeException e) {
            fileStorageService.delete(newCoverUrl);
            throw e;
        }
    }

    /* ================= UC31: ĐỔI LỊCH PHÁT HÀNH ================= */

    /*
        QUY TẮC:
        - Bài ĐÃ PHÁT HÀNH (kể cả đang ẩn) → không đổi lịch được nữa.
          Chặn kiểu lách luật: ẩn bài đã phát hành, đổi lịch sang năm sau, hiện lại
          → bài biến mất khỏi trang người nghe mà không qua Admin.
        - Lịch mới (nếu có) phải ở tương lai.
        - releaseAt = null nghĩa là "phát hành ngay":
            + Bài PENDING            → để null, Admin duyệt xong sẽ gán = lúc duyệt.
            + Bài APPROVED / HIDDEN  → đã duyệt rồi → gán = bây giờ.
              (KHÔNG để null: bài đã duyệt mà releaseAt = null
               sẽ không bao giờ hiện cho người nghe.)
            + Bài REJECTED           → để null, như PENDING.

        Đổi lịch KHÔNG gửi duyệt lại bài REJECTED (khác sửa tên / ảnh),
        vì lịch không phải lý do bị từ chối.
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

    /*
        Ẩn bài: APPROVED → HIDDEN.
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
        Hiện lại bài đang ẩn: HIDDEN → APPROVED.
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

    /* ================= UC33: XÓA BÀI ================= */

    /*
        XÓA MỀM (soft delete):
        - Đổi trạng thái DELETED, GIỮ bản ghi trong database (lịch sử, lượt nghe cũ).
        - Xóa HẲN file nhạc và ảnh bìa khỏi ổ đĩa (không chiếm dung lượng).

        THỨ TỰ: lưu trạng thái DELETED TRƯỚC, xóa file SAU.
        → Lưu database lỗi (VD 409 vì Admin đang duyệt bài này) → hàm dừng ở save(),
          file KHÔNG bị xóa → bài vẫn nguyên vẹn, nghe được bình thường.
        → Ngược lại (xóa file trước, lưu sau): lưu lỗi thì bài vẫn APPROVED
          nhưng file đã mất → người nghe bấm phát bị lỗi.

        Xóa được ở MỌI trạng thái (PENDING, APPROVED, REJECTED, HIDDEN).
        Không khôi phục được vì file đã mất.
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
        Dùng ở MỌI thao tác thay đổi dữ liệu (xem giải thích "2 lớp kiểm tra" ở đầu class).
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
        Lịch phát hành (nếu có) phải ở TƯƠNG LAI.
        - releaseAt = null → "phát hành ngay sau khi duyệt" → hợp lệ, bỏ qua kiểm tra.
        - !releaseAt.isAfter(now) = "không ở sau bây giờ" = quá khứ hoặc đúng bây giờ → lỗi.
    */
    private void checkFutureTime(LocalDateTime releaseAt) {
        if (releaseAt != null && !releaseAt.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Release time must be in the future");
        }
    }

    /*
        Kiểm tra và tải danh sách nghệ sĩ feat từ danh sách id frontend gửi lên.

        QUY TẮC:
        - Tối đa 5 người, không trùng nhau.
        - Không chứa chính Artist chính.
        - Mỗi người phải tồn tại, có role ARTIST và đang ACTIVE.

        CÁC BƯỚC:
        1. Danh sách rỗng / null → trả tập rỗng (bài không feat ai).
        2. new HashSet<>(ids): Set tự loại id trùng. VD [12, 12, 13] → {12, 13}.
        3. uniqueIds.remove(null): bỏ giá trị rỗng
           (form-data gửi ô featuredArtistIds để trống → Spring đổi thành null).
           Thiếu bước này → findAllById(null) báo lỗi khó hiểu.
        4. Kiểm tra số lượng SAU KHI loại trùng (gửi [12, 12, 12, 12, 12, 12] vẫn hợp lệ = 1 người).
        5. Không tự feat chính mình.
        6. findAllById(uniqueIds): 1 câu SELECT ... WHERE id IN (12, 13) lấy tất cả cùng lúc
           (nhanh hơn gọi findById từng người).
           Mỗi người phải là Artist ACTIVE.
        7. So số người tìm được với số id gửi lên:
           findAllById BỎ QUA id không tồn tại (không báo lỗi),
           nên gửi [12, 999] chỉ tìm được 1 người → phát hiện có id sai.
    */
    private Set<User> loadFeaturedArtists(Long artistId, List<Long> ids) {
        Set<User> result = new HashSet<>();

        if (ids == null || ids.isEmpty()) {
            return result;
        }

        Set<Long> uniqueIds = new HashSet<>(ids);

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

        if (result.size() != uniqueIds.size()) {
            throw new IllegalArgumentException("Some featured artists do not exist");
        }

        return result;
    }

    /*
        Lấy bài hát của CHÍNH Artist đang đăng nhập.

        findByIdAndArtistId: tìm theo CẢ id bài VÀ id Artist.
        - Không tìm thấy, hoặc là bài của người khác → báo "Song does not exist".
          (Cố ý không báo "bạn không có quyền": tránh lộ việc bài đó có tồn tại.)
        - Bài đã xóa (DELETED) → cũng coi như không tồn tại.
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
        = đã được duyệt (APPROVED hoặc HIDDEN) VÀ releaseAt <= bây giờ.

        Tính cả HIDDEN: bài đang ẩn vẫn tính theo lịch
        → ẩn bài không giúp lách luật đổi lịch (xem updateReleaseSchedule).

        Khác "released" trong SongMapper (chỉ tính APPROVED):
        SongMapper trả lời "người nghe CÓ ĐANG THẤY không" (bài ẩn thì không thấy),
        còn hàm này trả lời "bài ĐÃ TỪNG tới giờ phát hành chưa".
    */
    private boolean isReleased(Song song) {
        return wasApproved(song)
                && song.getReleaseAt() != null
                && !song.getReleaseAt().isAfter(LocalDateTime.now());
    }

    /*
        Bài bị từ chối mà Artist đã sửa → tự gửi duyệt lại, xóa lý do từ chối cũ.
        Bài ở trạng thái khác → không làm gì.
        Dùng trong updateSong và updateCover.
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