package com.winhmm.myspotify.mapper;

import com.winhmm.myspotify.dto.song.ArtistSummary;
import com.winhmm.myspotify.dto.song.PageResponse;
import com.winhmm.myspotify.dto.song.SongManageResponse;
import com.winhmm.myspotify.dto.song.SongResponse;
import com.winhmm.myspotify.entity.Song;
import com.winhmm.myspotify.entity.User;
import com.winhmm.myspotify.enums.AccountStatus;
import com.winhmm.myspotify.enums.Role;
import com.winhmm.myspotify.enums.SongStatus;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/*
    ===================================================================
    SongMapper - Chuyển ENTITY Song → DTO trả về frontend.
    ===================================================================

    ---------- MAPPER LÀ GÌ, VÌ SAO CẦN? ----------

    Service làm việc với entity Song (đại diện bảng trong database).
    Frontend cần DTO (SongResponse, SongManageResponse): chỉ chứa đúng những gì cần hiển thị.
    Mapper là "người phiên dịch" giữa 2 bên.

        Database ──► Repository ──► Service ──► Controller ──► SongMapper ──► JSON
                       (Song)       (Song)       (Song)        (DTO)

    Vì sao gom vào 1 class riêng?
    - 3 controller (SongController, ArtistSongController, AdminSongController) đều cần
      chuyển Song → DTO. Viết 1 lần ở đây, cả 3 dùng chung, không lặp code.
    - Sửa cách hiển thị (VD thêm trường mới vào JSON) → chỉ sửa 1 chỗ.

    ---------- VÌ SAO TOÀN METHOD STATIC? ----------

    Mapper không lưu trạng thái gì (không có thuộc tính, không cần database):
    nhận Song vào, trả DTO ra, như 1 hàm toán học.
    → static: gọi thẳng SongMapper.toSongResponse(song), không cần new SongMapper()
      hay để Spring tiêm vào.

    ---------- 3 CÁCH CHUYỂN ----------

        toArtistSummary(user)      : User → ArtistSummary          (id + nghệ danh)
        toSongResponse(song)       : Song → SongResponse           (cho người nghe)
        toManageResponse(song)     : Song → SongManageResponse     (cho Artist, Admin)
        toPageResponse(page, ...)  : Page<Song> → PageResponse<T>  (1 trang kết quả)
*/
public class SongMapper {
    /*
        Constructor private → không ai tạo được đối tượng SongMapper (new SongMapper() sẽ lỗi).
        Đây là cách viết chuẩn cho "class tiện ích" chỉ chứa method static
        (giống java.lang.Math: không ai new Math(), chỉ gọi Math.max(...)).
    */
    private SongMapper() {}

    /*
        User → ArtistSummary (chỉ id + nghệ danh).

        Dùng ở:
        - activeFeaturedArtists() bên dưới: danh sách nghệ sĩ feat của mỗi bài.
        - ArtistSongController.searchArtists(): kết quả tìm nghệ sĩ để feat.

        KHÔNG đưa email, họ tên thật, trạng thái tài khoản ra ngoài → bảo vệ thông tin cá nhân.
    */
    public static ArtistSummary toArtistSummary(User user) {
        return new ArtistSummary(user.getId(), user.getArtistName());
    }

    /*
        Lấy danh sách nghệ sĩ feat ĐANG HOẠT ĐỘNG của bài hát.

        1. LỌC: người bị khóa / bị thu hồi quyền Artist → ẩn tên, không đưa vào danh sách.
           Bài hát vẫn hiển thị bình thường (vì Artist CHÍNH vẫn hoạt động),
           chỉ ẩn tên người feat có vấn đề.
           VD: bài "Chiều Mưa" feat Sóng Nhỏ và Vầng Trăng, Vầng Trăng bị khóa
               → hiển thị "Chiều Mưa - Mây Chiều ft. Sóng Nhỏ".

        2. SẮP XẾP theo id: featuredArtists là Set (HashSet), không giữ thứ tự cố định,
           mỗi lần đọc database có thể trả về thứ tự khác.
           Không sắp xếp → lần này hiện "ft. A, B", lần sau "ft. B, A" → frontend nhấp nháy.
           Comparator.comparing(ArtistSummary::getId) = "so sánh 2 phần tử theo id, nhỏ trước".

        private: chỉ dùng nội bộ trong mapper.

        Trả về List rỗng (không phải null) khi không có ai feat
        → JSON là [] → frontend không cần kiểm tra null.
    */
    private static List<ArtistSummary> activeFeaturedArtists(Song song) {
        List<ArtistSummary> result = new ArrayList<>();

        for(User user : song.getFeaturedArtists()) {
            if(user.getAccountStatus() == AccountStatus.ACTIVE && user.getRole() == Role.ARTIST) {
                result.add(toArtistSummary(user));
            }
        }

        result.sort(Comparator.comparing(ArtistSummary::getId));
        return result;
    }

    /*
        Song → SongResponse (cho NGƯỜI NGHE).

        Dùng Builder: gọi tên từng trường → đọc là hiểu, không nhầm thứ tự
        (xem giải thích Builder Pattern trong SongResponse.java).

        song.getArtist().getId() / getArtistName():
        - artist là quan hệ LAZY → lần đầu gọi getArtist().getArtistName()
          Hibernate mới chạy câu SELECT lấy User.
        - Chạy được trong Controller nhờ open-in-view = true (kết nối database mở suốt request).
    */
    public static SongResponse toSongResponse(Song song) {
        return SongResponse.builder()
                .id(song.getId())
                .title(song.getTitle())
                .audioUrl(song.getAudioUrl())
                .coverUrl(song.getCoverUrl())
                .durationSeconds(song.getDurationSeconds())
                .playCount(song.getPlayCount())
                .releaseAt(song.getReleaseAt())
                .artistId(song.getArtist().getId())
                .artistName(song.getArtist().getArtistName())
                .featuredArtists(activeFeaturedArtists(song))
                .build();
    }

    /*
        Song → SongManageResponse (cho ARTIST và ADMIN, đầy đủ thông tin quản lý).

        TÍNH SẴN "released" (đã phát hành chưa):
            released = bài APPROVED  VÀ  có releaseAt  VÀ  releaseAt KHÔNG ở sau thời điểm hiện tại

        !song.getReleaseAt().isAfter(now) nghĩa là "releaseAt <= now":
            - isAfter(now) = true  → còn ở tương lai → chưa phát hành.
            - Phủ định (!)         → đã tới hoặc đã qua giờ phát hành.
            Viết vậy thay vì isBefore(now) để tính cả trường hợp releaseAt ĐÚNG BẰNG now.

        Điều kiện này GIỐNG điều kiện PUBLISHED_CONDITION trong SongRepository
        (trừ phần kiểm tra Artist), nên "released = true" khớp với việc người nghe thấy bài.

        Vì sao tính ở backend? Đồng hồ điện thoại người dùng có thể chạy sai,
        dùng giờ server mới chính xác.

        Frontend hiển thị:
            APPROVED + released = false → "Đã lên lịch"
            APPROVED + released = true  → "Đã phát hành"
    */
    public static SongManageResponse toManageResponse(Song song) {
        boolean released = song.getStatus() == SongStatus.APPROVED
                && song.getReleaseAt() != null
                && !song.getReleaseAt().isAfter(LocalDateTime.now());

        return SongManageResponse.builder()
                .id(song.getId())
                .title(song.getTitle())
                .audioUrl(song.getAudioUrl())
                .coverUrl(song.getCoverUrl())
                .durationSeconds(song.getDurationSeconds())
                .playCount(song.getPlayCount())
                .status(song.getStatus())
                .released(released)
                .releaseAt(song.getReleaseAt())
                .rejectReason(song.getRejectReason())
                .artistId(song.getArtist().getId())
                .artistName(song.getArtist().getArtistName())
                .featuredArtists(activeFeaturedArtists(song))
                .createdAt(song.getCreatedAt())
                .updatedAt(song.getUpdatedAt())
                .build();
    }

    /*
        Page<Song> (kết quả phân trang từ Repository) → PageResponse<T> (trả về frontend).

        ---------- GENERIC METHOD <T> ----------

        "public static <T> PageResponse<T> ..." : <T> đứng trước kiểu trả về
        → đây là method generic: T được quyết định MỖI LẦN GỌI.
            toPageResponse(page, SongMapper::toSongResponse)   → T = SongResponse
            toPageResponse(page, SongMapper::toManageResponse) → T = SongManageResponse
        1 method dùng cho cả người nghe và Admin.

        ---------- Function<Song, T> converter ----------

        Function<A, B> = "1 hàm nhận A, trả về B" (interface có sẵn của Java).
        converter = CÁCH CHUYỂN 1 bài hát, được truyền vào như 1 tham số.
            converter.apply(song) → gọi hàm đó cho 1 bài cụ thể.

        "SongMapper::toSongResponse" là method reference, nghĩa là:
            "dùng method toSongResponse để chuyển từng bài hát trong trang".
        Tương đương lambda: song -> SongMapper.toSongResponse(song)

        Kiểu viết này gọi là "truyền hành vi" (behavior parameterization):
        phần lặp, đóng gói PageResponse viết 1 lần, phần "chuyển 1 bài thế nào" thay đổi được.

        ---------- CÁC THÔNG TIN LẤY TỪ Page ----------

            page.getContent()       → danh sách Song của trang này
            page.getNumber()        → số trang hiện tại (từ 0)
            page.getSize()          → số phần tử tối đa mỗi trang
            page.getTotalElements() → tổng số bài của mọi trang
            page.getTotalPages()    → tổng số trang

        ---------- GIỚI HẠN ĐÃ BIẾT: TRUY VẤN N+1 ----------

        artist và featuredArtists đều LAZY. Với 1 trang 20 bài:
            1 câu SELECT lấy 20 bài
            + tối đa 20 câu lấy Artist chính (mỗi bài 1 câu, Artist trùng thì Hibernate dùng lại)
            + 20 câu lấy danh sách feat (mỗi bài 1 câu)
        → khoảng 41 câu SQL cho 1 lần mở trang. Gọi là "vấn đề N+1".
        Với quy mô đồ án (vài trăm bài) thì không đáng kể.
        Khi dữ liệu lớn, cách sửa: dùng @EntityGraph hoặc JOIN FETCH trong SongRepository
        để lấy bài + Artist + feat trong ít câu truy vấn hơn.
    */
    public static <T> PageResponse<T> toPageResponse(Page<Song> page, Function<Song, T> converter) {
        List<T> content = new ArrayList<>();

        for(Song song : page.getContent()) {
            content.add(converter.apply(song));
        }

        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}