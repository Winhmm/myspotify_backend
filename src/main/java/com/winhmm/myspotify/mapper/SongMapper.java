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
    =====================================================================
    SongMapper - CHUYỂN ENTITY Song THÀNH DTO TRẢ VỀ FRONTEND
    =====================================================================

    Các method:
        toArtistSummary(user)      : User → ArtistSummary          (id và nghệ danh)
        toSongResponse(song)       : Song → SongResponse           (cho người nghe)
        toManageResponse(song)     : Song → SongManageResponse     (cho Artist, Admin)
        toPageResponse(page, ...)  : Page<Song> → PageResponse<T>  (1 trang kết quả)

    =====================================================================
    MAPPER LÀ GÌ, VÌ SAO CẦN?
    =====================================================================

    Service làm việc với entity Song (đại diện 1 dòng trong bảng songs).
    Frontend cần DTO (SongResponse, SongManageResponse): chỉ chứa đúng những gì cần hiển thị.
    Mapper là "người phiên dịch" giữa 2 bên.

        Database ──► Repository ──► Service ──► Controller ──► SongMapper ──► JSON
                       (Song)       (Song)       (Song)          (DTO)

    Ngoại lệ: ListeningHistoryService gọi SongMapper ngay trong Service,
    vì Service đó phải ghép dữ liệu từ 2 database (MongoDB và MySQL) thành DTO.

    Vì sao gom vào 1 class riêng?
    - 3 controller (SongController, ArtistSongController, AdminSongController) và
      ListeningHistoryService đều cần chuyển Song → DTO.
      Viết 1 lần ở đây, mọi nơi dùng chung, không lặp code.
    - Sửa cách hiển thị (VD thêm trường mới vào JSON) → chỉ sửa 1 chỗ.

    =====================================================================
    VÌ SAO TOÀN METHOD STATIC?
    =====================================================================

    Mapper không lưu trạng thái gì (không có thuộc tính, không cần database):
    nhận Song vào, trả DTO ra, như 1 hàm toán học.
    → static: gọi thẳng SongMapper.toSongResponse(song), không cần new SongMapper()
      hay để Spring tiêm vào.

    =====================================================================
    VÌ SAO KHÔNG DÙNG THƯ VIỆN MAPSTRUCT, MODELMAPPER?
    =====================================================================

    Các thư viện đó tự sinh code chuyển đổi, gọn hơn khi có rất nhiều entity.
    Module Song chỉ có vài kiểu chuyển, và có logic riêng (lọc người feat bị khóa,
    tính sẵn released), viết tay dễ đọc và dễ kiểm soát hơn.
*/
public class SongMapper {
    private SongMapper() {}

    /*
        ---------------------------------------------------------------
        User → ArtistSummary (chỉ id và nghệ danh)
        ---------------------------------------------------------------

        Dùng ở:
        - activeFeaturedArtists() bên dưới: danh sách nghệ sĩ feat của mỗi bài.
        - ArtistSongController.searchArtists(): kết quả tìm nghệ sĩ để feat.

        Không đưa email, họ tên thật, trạng thái tài khoản ra ngoài → bảo vệ thông tin cá nhân.
    */
    public static ArtistSummary toArtistSummary(User user) {
        return new ArtistSummary(user.getId(), user.getArtistName());
    }

    /*
        ---------------------------------------------------------------
        Danh sách nghệ sĩ feat đang hoạt động của 1 bài
        ---------------------------------------------------------------
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
        ---------------------------------------------------------------
        Song → SongResponse (cho người nghe)
        ---------------------------------------------------------------
    */
    public static SongResponse toSongResponse(Song song) {
        return SongResponse.builder()
                .id(song.getId())
                .title(song.getTitle())
                .audioUrl(song.getAudioUrl())
                .coverUrl(song.getCoverUrl())
                .canvasUrl(song.getCanvasUrl())
                .durationSeconds(song.getDurationSeconds())
                .playCount(song.getPlayCount())
                .releaseAt(song.getReleaseAt())
                .artistId(song.getArtist().getId())
                .artistName(song.getArtist().getArtistName())
                .featuredArtists(activeFeaturedArtists(song))
                .build();
    }

    /*
        ---------------------------------------------------------------
        Song → SongManageResponse (cho Artist và Admin, đầy đủ thông tin quản lý)
        ---------------------------------------------------------------
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
                .canvasUrl(song.getCanvasUrl())
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
        ---------------------------------------------------------------
        Page<Song> → PageResponse<T> (1 trang kết quả trả về frontend)
        ---------------------------------------------------------------
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