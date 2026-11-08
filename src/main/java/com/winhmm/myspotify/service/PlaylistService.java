package com.winhmm.myspotify.service;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    - 2 DTO trả về: PlaylistDetailResponse (chi tiết), PlaylistSummaryResponse (1 ô trong danh sách).
    - 4 entity: Playlist, PlaylistSong, Song, User.
    - AccountStatus: trạng thái tài khoản (ACTIVE, DISABLED...), dùng để ẩn playlist của tài khoản bị khóa.
    - PlaylistMapper: đổi entity sang DTO.
    - 4 repository: đọc, ghi các bảng playlists, playlist_songs, songs, users.
    - PageRequest: tạo yêu cầu "lấy trang 0, 1 dòng" (lấy ảnh bìa của bài đầu tiên).
    - AccessDeniedException: lỗi "không có quyền" của Spring Security → 403.
    - Service, Transactional: 2 annotation của Spring (giải thích bên dưới).
    - MultipartFile: file ảnh bìa tải lên.
    - LocalDateTime: giờ hiện tại (so với giờ phát hành của bài).
    - ArrayList, HashMap, LinkedHashSet, List, Map, Set: các kiểu danh sách của Java.
*/
import com.winhmm.myspotify.dto.playlist.PlaylistDetailResponse;
import com.winhmm.myspotify.dto.playlist.PlaylistSummaryResponse;
import com.winhmm.myspotify.entity.Playlist;
import com.winhmm.myspotify.entity.PlaylistSong;
import com.winhmm.myspotify.entity.Song;
import com.winhmm.myspotify.entity.User;
import com.winhmm.myspotify.enums.AccountStatus;
import com.winhmm.myspotify.mapper.PlaylistMapper;
import com.winhmm.myspotify.repository.PlaylistRepository;
import com.winhmm.myspotify.repository.PlaylistSongRepository;
import com.winhmm.myspotify.repository.SongRepository;
import com.winhmm.myspotify.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
    =====================================================================
    PLAYLISTSERVICE - NGHIỆP VỤ PLAYLIST
    =====================================================================

    (Service là gì, @Service, constructor injection: xem SessionService.
     @Transactional: xem AuthService. Tất cả cũng được giải thích lại bên dưới.)

    Service là tầng xử lý nghiệp vụ, nằm giữa Controller và Repository:
        Controller (nhận request) → Service (kiểm tra quy tắc, quyết định làm gì) → Repository (đọc, ghi database)
    Mọi quy tắc của playlist (ai được sửa, tối đa bao nhiêu bài, bài nào được thêm...) đều nằm ở đây.

    Các chức năng:
        createPlaylist(...)       → tạo playlist (kèm ít nhất 1 bài, ảnh bìa nếu có)
        getMyPlaylists(...)       → playlist của tôi (cả riêng tư lẫn công khai)
        getPublicPlaylists(...)   → playlist công khai của 1 tài khoản (trang cá nhân người đó)
        getPlaylistDetail(...)    → chi tiết 1 playlist kèm danh sách bài
        updatePlaylist(...)       → sửa tên, mô tả, công khai hay riêng tư
        updateCover(...)          → đổi ảnh bìa riêng
        removeCover(...)          → gỡ ảnh bìa riêng (quay về ảnh bài đầu tiên)
        addSong(...)              → thêm 1 bài
        removeSong(...)           → bỏ 1 bài
        deletePlaylist(...)       → xóa playlist

    Được gọi từ PlaylistController. Mọi tài khoản đã đăng nhập đều dùng được (USER, ARTIST, ADMIN).

    =====================================================================
    QUY TẮC CHUNG
    =====================================================================

    1. Mỗi tài khoản tối đa 50 playlist, mỗi playlist tối đa 200 bài.
    2. Tạo playlist bắt buộc có ít nhất 1 bài. Sau này bỏ hết bài vẫn được (playlist rỗng).
    3. Chỉ thêm được bài đang phát hành.
    4. Chỉ hiện và chỉ đếm bài đang phát hành (bài bị ẩn, bị gỡ tự ẩn, phát hành lại thì tự hiện lại).
    5. Bài xếp theo thời điểm thêm, thêm trước đứng trước.
    6. Ảnh bìa: ảnh riêng → không có thì ảnh bài đầu tiên → vẫn không có thì null.
    7. Chỉ chủ playlist được sửa, đổi ảnh, thêm bỏ bài, xóa. Người khác gọi → 403.
    8. Người khác xem:
       - Không thấy playlist riêng tư. Mở thẳng bằng id → "This playlist is private".
       - Không thấy playlist của tài khoản không còn ACTIVE (bị khóa, vô hiệu hóa, đã xóa)
         → "Playlist does not exist".

    =====================================================================
    BÁO LỖI THẾ NÀO?
    =====================================================================

    Gặp chỗ sai, Service "ném" (throw) 1 exception, VD:
        throw new IllegalArgumentException("Playlist does not exist");
    → Hàm dừng ngay tại đó, các dòng phía sau không chạy.
    → Spring chuyển exception tới GlobalExceptionHandler, nơi đổi exception thành response:
        IllegalArgumentException         → 400 {"message": "<câu báo lỗi>"}
        AccessDeniedException            → 403 {"message": "You do not have permission to access this resource"}
        DataIntegrityViolationException  → 409 {"message": "Data already exists"}  (database chặn dữ liệu trùng)
    Nhờ vậy Service chỉ cần ném lỗi, không phải tự viết response.

    =====================================================================
    VÌ SAO SERVICE TRẢ THẲNG DTO?
    =====================================================================

    Ở module Song, Service trả entity, Controller gọi SongMapper đổi sang DTO.
    Ở đây Service trả luôn DTO (giống ListeningHistoryService), vì để tạo DTO
    còn phải hỏi database thêm: đếm số bài, tìm ảnh bìa dự phòng, lấy danh sách bài.
    Hỏi database là việc của Service, Controller không nên làm.

    =====================================================================
    SERVICE VÀ CONSTRUCTOR INJECTION
    =====================================================================

    @Service: đánh dấu class này là 1 service. Khi ứng dụng khởi động, Spring tự tạo đúng 1 object
    PlaylistService (gọi là bean) và giữ lại, ai cần thì Spring đưa object đó cho.
    Mình không bao giờ tự viết new PlaylistService(...).

    Constructor injection (tiêm qua constructor):
    - PlaylistService cần 5 thứ: 4 repository và FileStorageService.
    - Chỉ cần khai báo chúng làm tham số của constructor. Lúc tạo PlaylistService,
      Spring tự tìm 5 bean đó và truyền vào (gọi là "tiêm" phụ thuộc, dependency injection).
    - final: gán 1 lần trong constructor, sau đó không đổi được → không ai lỡ tay gán null.
    Controller cũng nhận PlaylistService theo đúng cách này.

    =====================================================================
    TRANSACTION LÀ GÌ?
    =====================================================================

    Transaction (giao dịch) = gom nhiều câu ghi database thành 1 khối "tất cả hoặc không gì cả":
    - Mọi câu chạy thành công → lưu hết (commit).
    - 1 câu lỗi giữa chừng → hủy hết những gì đã ghi trong khối (rollback), database như chưa có gì xảy ra.

    Đặt @Transactional lên 1 hàm public → Spring mở transaction khi bắt đầu hàm,
    commit khi hàm chạy xong, rollback nếu hàm ném RuntimeException (VD IllegalArgumentException).
    Lưu ý: chỉ có tác dụng khi hàm được gọi từ class khác (ở đây PlaylistController gọi sang).

    Trong file này:
    - createPlaylist, addSong, removeSong, deletePlaylist: có @Transactional vì ghi nhiều bảng
      hoặc nhiều dòng cùng lúc.
    - updatePlaylist, updateCover, removeCover: chỉ lưu 1 dòng (1 lần save), save có sẵn transaction riêng
      → không cần. Riêng 2 hàm ảnh bìa còn cố ý không dùng (giải thích ở updateCover).
    - Các hàm chỉ đọc: không cần.
*/
@Service
public class PlaylistService {

    /*
        Các giới hạn (hằng số, đổi 1 chỗ là áp dụng mọi nơi):
        - Mỗi tài khoản tối đa 50 playlist.
        - Mỗi playlist tối đa 200 bài.
        - Ảnh bìa riêng lưu ở thư mục {uploadDir}/playlists/.

        private static final:
        - private: chỉ dùng trong class này.
        - static : thuộc về class, không phải từng object.
        - final  : không đổi được sau khi gán.
        Hằng số theo quy ước viết hoa, nối bằng dấu gạch dưới (MAX_SONGS_PER_PLAYLIST).
    */
    private static final int MAX_PLAYLISTS_PER_USER = 50;
    private static final int MAX_SONGS_PER_PLAYLIST = 200;
    private static final String COVER_FOLDER = "playlists";

    private final PlaylistRepository playlistRepository;
    private final PlaylistSongRepository playlistSongRepository;
    private final SongRepository songRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public PlaylistService(PlaylistRepository playlistRepository,
                           PlaylistSongRepository playlistSongRepository,
                           SongRepository songRepository,
                           UserRepository userRepository,
                           FileStorageService fileStorageService) {
        this.playlistRepository = playlistRepository;
        this.playlistSongRepository = playlistSongRepository;
        this.songRepository = songRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    /*
        =====================================================================
        TẠO PLAYLIST
        =====================================================================

        Các bước:
        1. Lấy tài khoản người tạo.
        2. Đã có 50 playlist → báo lỗi.
        3. Kiểm tra danh sách bài (bỏ trùng, tối đa 200, mọi bài phải đang phát hành).
        4. Lưu ảnh bìa (nếu có).
        5. Lưu playlist, rồi lưu từng bài theo đúng thứ tự gửi lên.
        6. Trả về chi tiết playlist vừa tạo.

        Bước 1 đến 3 chỉ đọc database. Sai ở đây thì báo lỗi ngay, chưa lưu file ảnh nào.

        -----------------------------------------------------
        Vì sao có @Transactional?
        -----------------------------------------------------
        Hàm ghi nhiều dòng: 1 dòng playlists + nhiều dòng playlist_songs.
        Lỗi giữa chừng (VD lưu bài thứ 3 bị lỗi) → rollback toàn bộ, không để lại playlist "dở dang".

        -----------------------------------------------------
        Khối try / catch dọn file ảnh
        -----------------------------------------------------
        try { ... } catch (RuntimeException exception) { ... }:
        chạy các lệnh trong try, nếu có lỗi thì nhảy vào catch.

        @Transactional chỉ hủy được thay đổi trong MySQL, không xóa được file ảnh đã ghi xuống ổ đĩa.
        → Lỗi lúc lưu database thì khối catch tự xóa ảnh bìa vừa lưu, rồi ném lại đúng lỗi cũ
          (throw exception) → GlobalExceptionHandler vẫn trả đúng mã lỗi như bình thường.
        coverUrl = null (không gửi ảnh) → delete(null) không làm gì.
        (Cách làm giống ArtistSongService.uploadSong.)

        -----------------------------------------------------
        Tham số
        -----------------------------------------------------
        - ownerId    : id người đang đăng nhập (Controller lấy từ token, không lấy từ frontend).
        - name       : tên playlist (đã kiểm tra @NotBlank, tối đa 100 ký tự).
        - description: mô tả, có thể null.
        - isPublic   : true, false hoặc null (null coi như false, tức riêng tư).
        - songIds    : danh sách id bài, theo thứ tự muốn xếp.
        - coverFile  : ảnh bìa, có thể null.
    */
    @Transactional
    public PlaylistDetailResponse createPlaylist(Long ownerId, String name, String description,
                                                 Boolean isPublic, List<Long> songIds,
                                                 MultipartFile coverFile) {
        User owner = getUser(ownerId);

        if (playlistRepository.countByOwnerId(ownerId) >= MAX_PLAYLISTS_PER_USER) {
            throw new IllegalArgumentException(
                    "Maximum " + MAX_PLAYLISTS_PER_USER + " playlists per account");
        }

        List<Song> songs = loadPlayableSongs(songIds);

        /*
            Có gửi ảnh bìa thì lưu xuống ổ đĩa. Từ đây trở đi, lỗi gì cũng phải xóa ảnh này.
            coverFile.isEmpty(): Postman có tạo key cover nhưng không chọn file → coi như không gửi.
            saveImage tự kiểm tra định dạng (JPG, PNG, WEBP), dung lượng (tối đa 2MB), chữ ký file,
            rồi trả về đường dẫn, VD "/uploads/playlists/7c1e2b9a-....jpg".
        */
        String coverUrl = null;
        if (coverFile != null && !coverFile.isEmpty()) {
            coverUrl = fileStorageService.saveImage(coverFile, COVER_FOLDER);
        }

        try {
            /*
                Lưu playlist trước để có id (playlist_songs cần playlist_id).
                name.trim(): bỏ dấu cách thừa 2 đầu ("  Chill  " → "Chill").
                Boolean.TRUE.equals(isPublic): true chỉ khi isPublic đúng là true,
                null hay false đều ra false (giải thích: xem SongService.play).
                Viết isPublic == true sẽ lỗi NullPointerException khi isPublic là null.
            */
            Playlist playlist = new Playlist();
            playlist.setOwner(owner);
            playlist.setName(name.trim());
            playlist.setDescription(cleanDescription(description));
            playlist.setPublic(Boolean.TRUE.equals(isPublic));
            playlist.setCoverUrl(coverUrl);
            Playlist saved = playlistRepository.save(playlist);

            /*
                Tạo 1 dòng PlaylistSong cho mỗi bài, giữ đúng thứ tự trong danh sách.
                saveAll: lưu cả danh sách. Dòng nào lưu trước có id nhỏ hơn
                → các bài thêm cùng lúc vẫn hiện đúng thứ tự đã chọn (ORDER BY addedAt, id).
                addedAt được entity PlaylistSong tự gán lúc lưu (@PrePersist).
            */
            List<PlaylistSong> items = new ArrayList<>();
            for (Song song : songs) {
                items.add(new PlaylistSong(saved, song));
            }
            playlistSongRepository.saveAll(items);

            return buildDetail(saved, ownerId);
        } catch (RuntimeException exception) {
            fileStorageService.delete(coverUrl);
            throw exception;
        }
    }

    /*
        =====================================================================
        PLAYLIST CỦA TÔI
        =====================================================================

        Mọi playlist của người đang đăng nhập (cả riêng tư lẫn công khai), mới tạo trước.
        Mỗi playlist kèm số bài đang phát hành và ảnh bìa đã tính sẵn (xem toSummaries).
    */
    public List<PlaylistSummaryResponse> getMyPlaylists(Long ownerId) {
        return toSummaries(playlistRepository.findMyPlaylists(ownerId));
    }

    /*
        =====================================================================
        PLAYLIST CÔNG KHAI CỦA 1 TÀI KHOẢN
        =====================================================================

        Dùng khi xem trang cá nhân của 1 người: chỉ playlist công khai,
        và chỉ khi tài khoản đó còn ACTIVE (điều kiện nằm sẵn trong câu truy vấn findPublicPlaylists).

        Tài khoản không tồn tại, bị khóa, hoặc không có playlist công khai nào
        → trả về danh sách rỗng [], không báo lỗi.
        Xem trang của chính mình cũng chỉ thấy playlist công khai (đúng như người khác nhìn thấy).
        Muốn xem đủ cả riêng tư → dùng "Playlist của tôi".
    */
    public List<PlaylistSummaryResponse> getPublicPlaylists(Long userId) {
        return toSummaries(playlistRepository.findPublicPlaylists(userId));
    }

    /*
        =====================================================================
        CHI TIẾT 1 PLAYLIST
        =====================================================================

        Các bước:
        1. Tìm playlist (kèm người tạo). Không có → "Playlist does not exist".
        2. Người xem là chủ playlist → luôn xem được.
        3. Người xem là người khác:
           - Tài khoản người tạo không còn ACTIVE → "Playlist does not exist"
             (coi như không còn, giống bài hát của Artist bị khóa).
           - Playlist riêng tư → "This playlist is private".
        4. Lấy các bài đang phát hành, đổi sang DTO.

        -----------------------------------------------------
        orElseThrow(() -> new IllegalArgumentException(...))
        -----------------------------------------------------
        findWithOwnerById trả về Optional (hộp có thể rỗng, xem PlaylistRepository).
        orElseThrow: hộp có playlist → lấy ra; hộp rỗng → ném lỗi.
        () -> new IllegalArgumentException("..."): lambda, tức 1 hàm ngắn viết tại chỗ,
        chỉ chạy khi hộp rỗng (lúc cần ném lỗi mới tạo lỗi).

        -----------------------------------------------------
        .equals(viewerId) thay vì ==
        -----------------------------------------------------
        So 2 số Long bằng equals, không dùng ==.
        Long là object: == so 2 object có phải cùng 1 object trong bộ nhớ không,
        với số lớn (từ 128 trở lên) có thể ra false dù cùng giá trị.
        equals so giá trị → luôn đúng.
    */
    public PlaylistDetailResponse getPlaylistDetail(Long viewerId, Long playlistId) {
        Playlist playlist = playlistRepository.findWithOwnerById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("Playlist does not exist"));

        boolean isOwner = playlist.getOwner().getId().equals(viewerId);

        if (!isOwner) {
            if (playlist.getOwner().getAccountStatus() != AccountStatus.ACTIVE) {
                throw new IllegalArgumentException("Playlist does not exist");
            }
            if (!playlist.isPublic()) {
                throw new IllegalArgumentException("This playlist is private");
            }
        }

        return buildDetail(playlist, viewerId);
    }

    /*
        =====================================================================
        SỬA THÔNG TIN PLAYLIST
        =====================================================================

        Sửa tên, mô tả, công khai hay riêng tư (thay toàn bộ thông tin cũ, xem UpdatePlaylistRequest).

        Các bước:
        1. Lấy playlist, kiểm tra người gọi là chủ (getOwnedPlaylist).
        2. Gán thông tin mới. Mô tả rỗng hoặc toàn dấu cách → null (xóa mô tả).
        3. save → Hibernate thấy có thay đổi → chạy UPDATE.
           @PreUpdate trong entity tự cập nhật updatedAt.
           Gửi lên giống hệt thông tin cũ → không có gì thay đổi → Hibernate không chạy UPDATE,
           updatedAt giữ nguyên.
        4. Trả về chi tiết playlist sau khi sửa.

        Đổi từ công khai sang riêng tư: người khác đang mở playlist này sẽ nhận "This playlist is private"
        ở lần tải lại tiếp theo.
    */
    public PlaylistDetailResponse updatePlaylist(Long ownerId, Long playlistId, String name,
                                                 String description, Boolean isPublic) {
        Playlist playlist = getOwnedPlaylist(ownerId, playlistId);

        playlist.setName(name.trim());
        playlist.setDescription(cleanDescription(description));
        playlist.setPublic(Boolean.TRUE.equals(isPublic));

        Playlist saved = playlistRepository.save(playlist);
        return buildDetail(saved, ownerId);
    }

    /*
        =====================================================================
        ĐỔI ẢNH BÌA RIÊNG
        =====================================================================

        Thứ tự an toàn (giống UserService.updateAvatar, ArtistSongService.updateCover):
            1. Lưu ảnh mới xuống ổ đĩa.
            2. Lưu playlist với đường dẫn ảnh mới.
            3. Lưu playlist thành công → mới xóa ảnh cũ.
        Bước 2 lỗi → xóa ảnh mới vừa lưu, ảnh cũ còn nguyên, playlist vẫn hiển thị bình thường.
        Nếu xóa ảnh cũ trước mà bước 2 lỗi → playlist trỏ tới 1 file không còn → ảnh bị vỡ.

        Vì sao hàm này không có @Transactional?
        Không có @Transactional thì save tự mở 1 transaction riêng và commit ngay khi save xong
        → lỗi database (nếu có) xảy ra ngay trong khối try, khối catch bắt được để xóa ảnh mới.
        Có @Transactional thì việc ghi thật xuống MySQL dời tới lúc hàm kết thúc
        → lỗi lúc đó nằm ngoài khối try, không dọn được ảnh mới.

        Chưa có ảnh riêng (oldCoverUrl = null) → delete(null) không làm gì.
        coverFile rỗng (có key cover nhưng không chọn file) → saveImage báo "Image file is required".
    */
    public PlaylistDetailResponse updateCover(Long ownerId, Long playlistId, MultipartFile coverFile) {
        Playlist playlist = getOwnedPlaylist(ownerId, playlistId);

        String oldCoverUrl = playlist.getCoverUrl();
        String newCoverUrl = fileStorageService.saveImage(coverFile, COVER_FOLDER);

        Playlist saved;
        try {
            playlist.setCoverUrl(newCoverUrl);
            saved = playlistRepository.save(playlist);
        } catch (RuntimeException exception) {
            fileStorageService.delete(newCoverUrl);
            throw exception;
        }

        fileStorageService.delete(oldCoverUrl);
        return buildDetail(saved, ownerId);
    }

    /*
        =====================================================================
        GỠ ẢNH BÌA RIÊNG
        =====================================================================

        Xóa ảnh riêng → playlist quay về dùng ảnh bìa của bài đầu tiên (hoặc ảnh mặc định của frontend).
        Chưa có ảnh riêng → "Playlist has no custom cover".

        Thứ tự: lưu playlist (coverUrl = null) trước, thành công mới xóa file
        → lỗi lúc lưu thì file vẫn còn, playlist không bị trỏ tới file đã mất.
    */
    public PlaylistDetailResponse removeCover(Long ownerId, Long playlistId) {
        Playlist playlist = getOwnedPlaylist(ownerId, playlistId);

        String oldCoverUrl = playlist.getCoverUrl();
        if (oldCoverUrl == null) {
            throw new IllegalArgumentException("Playlist has no custom cover");
        }

        playlist.setCoverUrl(null);
        Playlist saved = playlistRepository.save(playlist);

        fileStorageService.delete(oldCoverUrl);
        return buildDetail(saved, ownerId);
    }

    /*
        =====================================================================
        THÊM 1 BÀI VÀO PLAYLIST
        =====================================================================

        Các bước:
        1. Lấy playlist, kiểm tra người gọi là chủ.
        2. Bài phải đang phát hành (findPublishedById của SongRepository). Không → "Song does not exist"
           (bài không tồn tại, chưa duyệt, chưa tới giờ phát hành, bị ẩn, Artist bị khóa... đều chung 1 câu).
        3. Bài đã có trong playlist → "Song is already in this playlist".
        4. Playlist đã có 200 dòng → "Maximum 200 songs per playlist".
        5. Lưu dòng mới. addedAt tự là giờ hiện tại → bài mới nằm cuối playlist.
        6. markUpdated() → updatedAt của playlist đổi theo (giải thích: xem Playlist.markUpdated).
           Playlist này đang được Hibernate quản lý trong transaction, nên chỉ cần đổi giá trị,
           lúc hàm kết thúc Hibernate tự chạy UPDATE. Vẫn gọi save cho dễ đọc.
        7. Trả về chi tiết playlist sau khi thêm.

        2 request thêm cùng 1 bài đúng cùng lúc: cả 2 đều qua bước 3,
        nhưng ràng buộc UNIQUE (playlist_id, song_id) chặn request thứ hai → 409 "Data already exists".

        @Transactional: ghi 2 bảng (thêm dòng playlist_songs, cập nhật updated_at của playlists).
    */
    @Transactional
    public PlaylistDetailResponse addSong(Long ownerId, Long playlistId, Long songId) {
        Playlist playlist = getOwnedPlaylist(ownerId, playlistId);

        Song song = songRepository.findPublishedById(songId, LocalDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException("Song does not exist"));

        if (playlistSongRepository.existsByPlaylistIdAndSongId(playlistId, songId)) {
            throw new IllegalArgumentException("Song is already in this playlist");
        }

        if (playlistSongRepository.countByPlaylistId(playlistId) >= MAX_SONGS_PER_PLAYLIST) {
            throw new IllegalArgumentException(
                    "Maximum " + MAX_SONGS_PER_PLAYLIST + " songs per playlist");
        }

        playlistSongRepository.save(new PlaylistSong(playlist, song));

        playlist.markUpdated();
        playlistRepository.save(playlist);

        return buildDetail(playlist, ownerId);
    }

    /*
        =====================================================================
        BỎ 1 BÀI KHỎI PLAYLIST
        =====================================================================

        Các bước:
        1. Lấy playlist, kiểm tra người gọi là chủ.
        2. Tìm dòng (playlist, bài). Không có → "Song is not in this playlist".
           Không kiểm tra bài còn phát hành hay không: bài đã bị ẩn vẫn bỏ được.
        3. Xóa dòng đó (chỉ xóa bài khỏi playlist, bài hát gốc không bị ảnh hưởng).
        4. markUpdated() → updatedAt đổi theo.
        5. Trả về chi tiết playlist sau khi bỏ.

        Bỏ hết bài vẫn được, playlist thành rỗng (quy tắc 2).

        Dòng vừa delete chưa bị xóa thật trong MySQL ngay (Hibernate gom lại, ghi lúc cần).
        Khi buildDetail hỏi lại danh sách bài, Hibernate tự ghi lệnh xóa xuống trước rồi mới đọc
        → danh sách trả về đã không còn bài vừa bỏ.

        @Transactional: ghi 2 bảng (xóa dòng playlist_songs, cập nhật updated_at của playlists).
    */
    @Transactional
    public PlaylistDetailResponse removeSong(Long ownerId, Long playlistId, Long songId) {
        Playlist playlist = getOwnedPlaylist(ownerId, playlistId);

        PlaylistSong item = playlistSongRepository.findByPlaylistIdAndSongId(playlistId, songId)
                .orElseThrow(() -> new IllegalArgumentException("Song is not in this playlist"));

        playlistSongRepository.delete(item);

        playlist.markUpdated();
        playlistRepository.save(playlist);

        return buildDetail(playlist, ownerId);
    }

    /*
        =====================================================================
        XÓA PLAYLIST
        =====================================================================

        Xóa hẳn (không phải xóa mềm như tài khoản hay bài hát): playlist không còn cần lưu lại để làm gì.

        Các bước:
        1. Lấy playlist, kiểm tra người gọi là chủ.
        2. Xóa mọi dòng của playlist trong bảng playlist_songs (phải xóa trước, vì khóa ngoại).
        3. Xóa playlist.
        4. flush(): bắt Hibernate gửi ngay câu DELETE xuống MySQL.
           Bình thường Hibernate để tới cuối hàm mới gửi. Gửi ngay thì nếu có lỗi,
           lỗi xảy ra luôn ở đây, trước bước 5 → ảnh bìa chưa bị xóa oan.
        5. Xóa file ảnh bìa riêng (nếu có) khỏi ổ đĩa. Không có ảnh riêng → delete(null) không làm gì.
        Bài hát trong playlist không bị ảnh hưởng gì.

        @Transactional: bước 2 và 3 phải cùng thành công hoặc cùng bị hủy.
        Câu DELETE của bước 2 (@Modifying) cũng bắt buộc phải chạy trong transaction.
    */
    @Transactional
    public void deletePlaylist(Long ownerId, Long playlistId) {
        Playlist playlist = getOwnedPlaylist(ownerId, playlistId);
        String coverUrl = playlist.getCoverUrl();

        playlistSongRepository.deleteAllByPlaylistId(playlistId);
        playlistRepository.delete(playlist);
        playlistRepository.flush();

        fileStorageService.delete(coverUrl);
    }

    /*
        =====================================================================
        CÁC HÀM PHỤ (PRIVATE)
        =====================================================================

        private: chỉ dùng bên trong class này, Controller không gọi được.
        Tách ra để các hàm chính ngắn, dễ đọc, và không lặp lại cùng 1 đoạn code nhiều lần.
    */

    /*
        -----------------------------------------------------
        Lấy tài khoản theo id
        -----------------------------------------------------
        Gần như luôn có (id lấy từ token). Không có → "User does not exist".
    */
    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));
    }

    /*
        -----------------------------------------------------
        Lấy playlist mà người gọi là chủ (dùng cho mọi thao tác sửa, xóa)
        -----------------------------------------------------
        1. Không có playlist id đó → 400 "Playlist does not exist".
        2. Có nhưng của người khác → 403.
           AccessDeniedException là lỗi "không có quyền" của Spring Security,
           GlobalExceptionHandler đổi thành 403 "You do not have permission to access this resource"
           (câu trong ngoặc của exception chỉ để đọc code cho rõ, không gửi về frontend).

        Vì sao 403 mà không phải 400?
        403 Forbidden = "biết bạn là ai, nhưng bạn không được làm việc này" → đúng nghĩa nhất.
        Frontend bình thường không hiện nút sửa cho người khác (isOwner = false),
        nên lỗi này chỉ xảy ra khi ai đó tự gọi API.
    */
    private Playlist getOwnedPlaylist(Long ownerId, Long playlistId) {
        Playlist playlist = playlistRepository.findWithOwnerById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("Playlist does not exist"));

        if (!playlist.getOwner().getId().equals(ownerId)) {
            throw new AccessDeniedException("You are not the owner of this playlist");
        }
        return playlist;
    }

    /*
        -----------------------------------------------------
        Kiểm tra danh sách bài khi tạo playlist
        -----------------------------------------------------
        1. Bỏ id null và id trùng, giữ nguyên thứ tự.
           LinkedHashSet: Set không chứa phần tử trùng, và nhớ thứ tự thêm vào
           (HashSet thường thì làm mất thứ tự). VD [15, 22, 15, 9] → [15, 22, 9].
        2. Sau khi bỏ trùng mà rỗng → "At least 1 song is required".
           Quá 200 bài → "Maximum 200 songs per playlist".
        3. Hỏi MySQL 1 lần cho cả danh sách (findPublishedByIds, chỉ lấy bài đang phát hành),
           cho vào Map<id, Song> để tra nhanh theo id.
           Map: bảng tra cứu "khóa → giá trị" (ở đây id bài → bài). songsById.get(15) → bài có id 15,
           không có thì trả về null.
           Không hỏi từng bài 1 lần (200 bài = 200 câu SQL), mà gom thành 1 câu WHERE id IN (...).
        4. Duyệt lại theo đúng thứ tự ban đầu. Bài nào không có trong Map
           (không tồn tại, chưa phát hành, bị ẩn, Artist bị khóa...) → "Song does not exist: <id>".
           Kèm id để frontend biết chính xác bài nào có vấn đề.
    */
    private List<Song> loadPlayableSongs(List<Long> songIds) {
        Set<Long> uniqueIds = new LinkedHashSet<>();
        if (songIds != null) {
            for (Long songId : songIds) {
                if (songId != null) {
                    uniqueIds.add(songId);
                }
            }
        }

        if (uniqueIds.isEmpty()) {
            throw new IllegalArgumentException("At least 1 song is required");
        }
        if (uniqueIds.size() > MAX_SONGS_PER_PLAYLIST) {
            throw new IllegalArgumentException(
                    "Maximum " + MAX_SONGS_PER_PLAYLIST + " songs per playlist");
        }

        Map<Long, Song> songsById = new HashMap<>();
        for (Song song : songRepository.findPublishedByIds(uniqueIds, LocalDateTime.now())) {
            songsById.put(song.getId(), song);
        }

        List<Song> result = new ArrayList<>();
        for (Long songId : uniqueIds) {
            Song song = songsById.get(songId);
            if (song == null) {
                throw new IllegalArgumentException("Song does not exist: " + songId);
            }
            result.add(song);
        }
        return result;
    }

    /*
        -----------------------------------------------------
        Dọn mô tả: rỗng hoặc toàn dấu cách → null, có chữ → bỏ dấu cách thừa 2 đầu
        -----------------------------------------------------
        VD: null → null, "   " → null, "  Nghe lúc làm bài  " → "Nghe lúc làm bài".
        isBlank(): chuỗi rỗng hoặc chỉ toàn dấu cách.
        Lưu null thay vì chuỗi rỗng → frontend chỉ cần kiểm tra 1 trường hợp (description == null).
    */
    private String cleanDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }

    /*
        -----------------------------------------------------
        Dựng chi tiết 1 playlist
        -----------------------------------------------------
        Lấy các bài đang phát hành (đúng thứ tự thêm), rồi giao cho PlaylistMapper.
        Dùng chung cho xem chi tiết, tạo, sửa, đổi ảnh, thêm bài, bỏ bài.
    */
    private PlaylistDetailResponse buildDetail(Playlist playlist, Long viewerId) {
        List<PlaylistSong> items = playlistSongRepository.findPlayableSongs(playlist.getId(), LocalDateTime.now());
        boolean isOwner = playlist.getOwner().getId().equals(viewerId);
        return PlaylistMapper.toDetailResponse(playlist, items, isOwner);
    }

    /*
        -----------------------------------------------------
        Đổi danh sách playlist thành danh sách tóm tắt
        -----------------------------------------------------
        Mỗi playlist cần thêm 2 thông tin phải hỏi database:
        - songCount: đếm bài đang phát hành (1 câu COUNT).
        - coverUrl : ảnh riêng, không có thì ảnh bài đầu tiên (1 câu SELECT, chỉ khi không có ảnh riêng).
        → Tối đa khoảng 2 câu SQL cho mỗi playlist. Mỗi tài khoản tối đa 50 playlist nên vẫn nhanh.

        now: lấy giờ 1 lần, dùng chung cho mọi playlist trong danh sách
        → mọi playlist được tính theo cùng 1 thời điểm.
    */
    private List<PlaylistSummaryResponse> toSummaries(List<Playlist> playlists) {
        LocalDateTime now = LocalDateTime.now();
        List<PlaylistSummaryResponse> result = new ArrayList<>();

        for (Playlist playlist : playlists) {
            long songCount = playlistSongRepository.countPlayableSongs(playlist.getId(), now);
            String coverUrl = resolveCover(playlist, now);
            result.add(PlaylistMapper.toSummaryResponse(playlist, songCount, coverUrl));
        }
        return result;
    }

    /*
        -----------------------------------------------------
        Ảnh bìa cho danh sách: ảnh riêng → ảnh bài đầu tiên → null
        -----------------------------------------------------
        PageRequest.of(0, 1): trang đầu, 1 dòng → chỉ lấy ảnh của bài đầu tiên (LIMIT 1).
        covers rỗng (playlist không còn bài phát hành) → null.
        covers có 1 phần tử null (bài đầu không có ảnh) → get(0) trả null → cũng là null.
        covers.isEmpty() ? null : covers.get(0): toán tử 3 ngôi (rỗng thì null, không thì phần tử đầu).
    */
    private String resolveCover(Playlist playlist, LocalDateTime now) {
        if (playlist.getCoverUrl() != null) {
            return playlist.getCoverUrl();
        }

        List<String> covers = playlistSongRepository.findPlayableCoverUrls(
                playlist.getId(), now, PageRequest.of(0, 1));
        return covers.isEmpty() ? null : covers.get(0);
    }
}