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
    Chuyển Song (entity) → DTO trả về frontend.
    Gom vào 1 chỗ để 3 controller dùng chung, không phải viết lại.

    Tất cả method đều là static → gọi thẳng SongMapper.toSongResponse(song),
    không cần tạo đối tượng SongMapper.
*/
public class SongMapper {
    /*
        Constructor private → không ai tạo được đối tượng SongMapper
        (class này chỉ chứa method static).
    */
    private SongMapper() {}

    /*
        User → ArtistSummary (id + nghệ danh).
    */
    public static ArtistSummary toArtistSummary(User user) {
        return new ArtistSummary(user.getId(), user.getArtistName());
    }

    /*
        Lấy danh sách nghệ sĩ feat đang hoạt động của bài hát.
        - Người bị khóa / bị thu hồi quyền Artist → ẩn tên, không đưa vào danh sách.
        - Sắp xếp theo id để thứ tự luôn cố định
          (Set không giữ thứ tự, mỗi lần lấy có thể khác nhau).
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
        Song → SongResponse (cho người nghe).
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
        Song → SongManageResponse (cho Artist và Admin).

        released = đã duyệt và đã tới giờ phát hành.
        → Frontend hiển thị "Đã phát hành" / "Đã lên lịch".
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
        Page<Song> (kết quả phân trang từ repository) → PageResponse<T> (trả về frontend).

        converter = cách chuyển 1 bài hát, truyền vào khi gọi. VD:
        - SongMapper.toPageResponse(page, SongMapper::toSongResponse)
        - SongMapper.toPageResponse(page, SongMapper::toManageResponse)

        "SongMapper::toSongResponse" nghĩa là:
        "dùng method toSongResponse để chuyển từng bài hát trong trang".
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
