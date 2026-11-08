package com.winhmm.myspotify.mapper;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    - 3 DTO trả về frontend: PlaylistDetailResponse, PlaylistSongResponse, PlaylistSummaryResponse.
    - 2 entity: Playlist, PlaylistSong.
    - ArrayList, List: tạo danh sách bài hát trả về.
    (SongMapper cùng package mapper nên không cần import.)
*/
import com.winhmm.myspotify.dto.playlist.PlaylistDetailResponse;
import com.winhmm.myspotify.dto.playlist.PlaylistSongResponse;
import com.winhmm.myspotify.dto.playlist.PlaylistSummaryResponse;
import com.winhmm.myspotify.entity.Playlist;
import com.winhmm.myspotify.entity.PlaylistSong;

import java.util.ArrayList;
import java.util.List;

/*
    =====================================================================
    PLAYLISTMAPPER - CHUYỂN ENTITY PLAYLIST THÀNH DTO TRẢ VỀ FRONTEND
    =====================================================================

    (Mapper là gì, vì sao static, vì sao constructor private: xem UserMapper.
     Cũng được giải thích lại ngay bên dưới.)

    Mapper = class chỉ làm 1 việc: chép dữ liệu từ entity sang DTO.
    Gom việc chép vào 1 chỗ → Service, Controller gọn, và mọi API trả về cùng 1 kiểu JSON.

    Các method:
        toSummaryResponse(...)       : Playlist → PlaylistSummaryResponse (1 ô trong danh sách)
        toDetailResponse(...)        : Playlist + các bài → PlaylistDetailResponse (chi tiết)
        toPlaylistSongResponse(...)  : PlaylistSong → PlaylistSongResponse (1 bài trong playlist)

    -----------------------------------------------------
    Vì sao toàn hàm static?
    -----------------------------------------------------
    Hàm static gọi thẳng bằng tên class, không cần tạo object:
        PlaylistMapper.toDetailResponse(playlist, items, true)
    Mapper không giữ dữ liệu gì riêng (chỉ nhận vào, chép ra), nên không cần object.

    private PlaylistMapper() {}: constructor private → không ai gọi new PlaylistMapper() được
    (tạo ra cũng vô ích vì mọi hàm đều static).

    Mapper không phải bean của Spring (không có @Service, @Component),
    Spring không quản lý class này, chỉ là class Java thường.

    -----------------------------------------------------
    Khác SongMapper ở chỗ nào?
    -----------------------------------------------------
    Một số giá trị không nằm sẵn trong entity Playlist mà phải hỏi database thêm:
        songCount  : đếm bài đang phát hành
        coverUrl   : không có ảnh riêng thì lấy ảnh bài đầu tiên
        songs      : danh sách bài đang phát hành
        isOwner    : phụ thuộc người đang xem là ai
    Mapper không được gọi database (chỉ chuyển đổi dữ liệu có sẵn),
    nên PlaylistService hỏi database trước, rồi truyền các giá trị đó vào đây làm tham số.
*/
public class PlaylistMapper {
    private PlaylistMapper() {}

    /*
        ---------------------------------------------------------------
        Playlist → PlaylistSummaryResponse (1 ô trong danh sách)
        ---------------------------------------------------------------
        Tham số:
        - playlist : entity, đã nạp sẵn owner (JOIN FETCH trong PlaylistRepository).
        - songCount: số bài đang phát hành, Service đếm sẵn.
        - coverUrl : ảnh bìa đã tính sẵn (ảnh riêng, hoặc ảnh bài đầu tiên, hoặc null).

        playlist.isPublic(): getter của boolean trong entity đặt tên isXxx (quy ước của Java).
        ownerName lấy username (tên tài khoản), không lấy email → không lộ email của người tạo.
    */
    public static PlaylistSummaryResponse toSummaryResponse(Playlist playlist, long songCount, String coverUrl) {
        return PlaylistSummaryResponse.builder()
                .id(playlist.getId())
                .name(playlist.getName())
                .coverUrl(coverUrl)
                .isPublic(playlist.isPublic())
                .ownerId(playlist.getOwner().getId())
                .ownerName(playlist.getOwner().getUsername())
                .songCount(songCount)
                .createdAt(playlist.getCreatedAt())
                .updatedAt(playlist.getUpdatedAt())
                .build();
    }

    /*
        ---------------------------------------------------------------
        Playlist + danh sách bài → PlaylistDetailResponse (chi tiết)
        ---------------------------------------------------------------
        Tham số:
        - playlist: entity, đã nạp sẵn owner.
        - items   : các dòng PlaylistSong của bài đang phát hành, đúng thứ tự thêm
                    (PlaylistSongRepository.findPlayableSongs).
        - isOwner : người đang xem có phải chủ playlist không (Service tính sẵn).

        -----------------------------------------------------
        Đọc từng bước
        -----------------------------------------------------
        1. Đổi từng PlaylistSong sang PlaylistSongResponse, giữ nguyên thứ tự.
           for (PlaylistSong item : items): vòng lặp "for each", lần lượt lấy từng phần tử của items,
           đặt tên là item.
        2. Tính ảnh bìa:
               playlist.getCoverUrl() != null → dùng ảnh riêng.
               Không có ảnh riêng và còn bài → ảnh bìa của bài đầu tiên
                   (bài đó không có ảnh thì vẫn là null).
               Không có ảnh riêng và không còn bài → null.
           items.get(0): phần tử đầu tiên của danh sách (đánh số từ 0).
           Phải kiểm tra !items.isEmpty() trước: danh sách rỗng mà gọi get(0) → lỗi IndexOutOfBoundsException.
           Dấu ! nghĩa là "không": !items.isEmpty() = danh sách không rỗng.
        3. hasCustomCover = playlist có ảnh riêng không (so trên ảnh riêng, không so trên ảnh đã tính).
        4. songCount = số bài trong danh sách (songs.size()).
    */
    public static PlaylistDetailResponse toDetailResponse(Playlist playlist, List<PlaylistSong> items, boolean isOwner) {
        List<PlaylistSongResponse> songs = new ArrayList<>();
        for (PlaylistSong item : items) {
            songs.add(toPlaylistSongResponse(item));
        }

        String coverUrl = playlist.getCoverUrl();
        if (coverUrl == null && !items.isEmpty()) {
            coverUrl = items.get(0).getSong().getCoverUrl();
        }

        return PlaylistDetailResponse.builder()
                .id(playlist.getId())
                .name(playlist.getName())
                .description(playlist.getDescription())
                .coverUrl(coverUrl)
                .hasCustomCover(playlist.getCoverUrl() != null)
                .isPublic(playlist.isPublic())
                .ownerId(playlist.getOwner().getId())
                .ownerName(playlist.getOwner().getUsername())
                .isOwner(isOwner)
                .songCount(songs.size())
                .createdAt(playlist.getCreatedAt())
                .updatedAt(playlist.getUpdatedAt())
                .songs(songs)
                .build();
    }

    /*
        ---------------------------------------------------------------
        PlaylistSong → PlaylistSongResponse (1 bài trong playlist)
        ---------------------------------------------------------------
        - Thông tin bài: dùng lại SongMapper.toSongResponse
          (người feat bị khóa tự bị ẩn, xem SongMapper).
        - addedAt: lúc thêm bài vào playlist.
    */
    public static PlaylistSongResponse toPlaylistSongResponse(PlaylistSong item) {
        return new PlaylistSongResponse(SongMapper.toSongResponse(item.getSong()), item.getAddedAt());
    }
}