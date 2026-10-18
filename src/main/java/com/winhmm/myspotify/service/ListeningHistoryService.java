package com.winhmm.myspotify.service;

import com.winhmm.myspotify.document.ListeningHistory;
import com.winhmm.myspotify.dto.song.ListeningHistoryResponse;
import com.winhmm.myspotify.entity.Song;
import com.winhmm.myspotify.mapper.SongMapper;
import com.winhmm.myspotify.repository.ListeningHistoryRepository;
import com.winhmm.myspotify.repository.SongRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
    =====================================================================
    ListeningHistoryService - LỊCH SỬ NGHE NHẠC (MONGODB)
    =====================================================================

    Các chức năng:
        record(...)         → ghi 1 lượt nghe (gọi từ SongService.play)
        getRecentSongs(...) → 10 bài khác nhau được nghe gần nhất của 1 người
                              (gọi từ ListeningHistoryController)

    =====================================================================
    LÀM VIỆC VỚI 2 DATABASE
    =====================================================================

        MongoDB (listening_history)          MySQL (songs)
        ┌────────┬────────┬──────────┐       ┌────┬───────────┐
        │ userId │ songId │ playedAt │       │ id │ title     │
        │   5    │   15   │ 22:55    │ ────► │ 15 │ Chiều Mưa │
        │   5    │    7   │ 22:40    │ ────► │  7 │ Phố Đêm   │
        └────────┴────────┴──────────┘       └────┴───────────┘

    MongoDB và MySQL là 2 hệ thống riêng, không JOIN với nhau được.
    → Java tự ghép: đọc MongoDB lấy songId, đọc MySQL lấy thông tin bài, rồi ghép lại.
    Gọi là "application-side join" (ghép dữ liệu ở tầng ứng dụng).
*/
@Service
public class ListeningHistoryService {
    private static final Logger log = LoggerFactory.getLogger(ListeningHistoryService.class);

    private static final int RECENT_SONG_LIMIT = 10;

    private final ListeningHistoryRepository historyRepository;
    private final SongRepository songRepository;

    public ListeningHistoryService(ListeningHistoryRepository historyRepository,
                                   SongRepository songRepository) {
        this.historyRepository = historyRepository;
        this.songRepository = songRepository;
    }

    /*
        ---------------------------------------------------------------
        Ghi 1 lượt nghe vào MongoDB (bất đồng bộ)
        ---------------------------------------------------------------
        @Async: ghi lịch sử ở thread riêng, API /play trả response ngay
        mà không chờ MongoDB Atlas phản hồi.
        Ghi lỗi thì chỉ log cảnh báo, không ảnh hưởng tới việc nghe nhạc.
    */
    @Async
    public void record(Long userId, Long songId) {
        try {
            historyRepository.save(new ListeningHistory(userId, songId, LocalDateTime.now()));
        } catch (Exception e) {
            log.warn("Could not save listening history (user {}, song {}): {}",
                    userId, songId, e.getMessage());
        }
    }

    /*
        ---------------------------------------------------------------
        10 bài khác nhau được nghe gần nhất
        ---------------------------------------------------------------
        Giới hạn: chỉ xét 100 lượt nghe gần nhất. Nếu 100 lượt đó chỉ gồm 6 bài khác nhau
        (nghe đi nghe lại rất nhiều), kết quả có 6 bài dù trước đó từng nghe bài khác.
        Với mục "Nghe gần đây", như vậy là hợp lý.
    */
    public List<ListeningHistoryResponse> getRecentSongs(Long userId) {
        /*
            Bước 1: 100 lượt nghe gần nhất (MongoDB).
        */
        List<ListeningHistory> recentPlays = historyRepository.findTop100ByUserIdOrderByPlayedAtDesc(userId);

        /*
            Bước 2: lọc trùng, giữ lần nghe gần nhất của mỗi bài, giữ thứ tự.
        */
        Map<Long, LocalDateTime> lastPlayedAt = new LinkedHashMap<>();

        for(ListeningHistory play : recentPlays) {
            lastPlayedAt.putIfAbsent(play.getSongId(), play.getPlayedAt());
        }

        if(lastPlayedAt.isEmpty()) {
            return List.of();
        }

        /*
            Bước 3: hỏi MySQL 1 lần, lấy thông tin các bài còn đang phát hành,
            rồi đưa vào Map<id, Song> để tra nhanh theo id.

            lastPlayedAt.keySet(): tập các songId đã lọc trùng ở bước 2.
        */
        Map<Long, Song> songsById = new HashMap<>();
        for(Song song : songRepository.findPublishedByIds(lastPlayedAt.keySet(), LocalDateTime.now())) {
            songsById.put(song.getId(), song);
        }

        /*
            Bước 4: lấy theo đúng thứ tự, bỏ bài không còn phát hành, đủ 10 thì dừng.
            Map.Entry: 1 cặp (key, value) trong Map, ở đây là (songId, playedAt).
        */
        List<ListeningHistoryResponse> result = new ArrayList<>();

        for(Map.Entry<Long, LocalDateTime> entry : lastPlayedAt.entrySet()) {
            Song song = songsById.get(entry.getKey());

            if(song != null) {
                result.add(new ListeningHistoryResponse(SongMapper.toSongResponse(song), entry.getValue()));

                if(result.size() == RECENT_SONG_LIMIT) {
                    break;
                }
            }
        }

        return result;
    }
}