package com.winhmm.myspotify.service;

import com.winhmm.myspotify.document.ListeningHistory;
import com.winhmm.myspotify.dto.song.ListeningHistoryResponse;
import com.winhmm.myspotify.entity.Song;
import com.winhmm.myspotify.mapper.SongMapper;
import com.winhmm.myspotify.repository.ListeningHistoryRepository;
import com.winhmm.myspotify.repository.SongRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    2 chức năng:
    1. record(...)         : ghi 1 lượt nghe (gọi từ SongService.play).
    2. getRecentSongs(...) : 10 bài khác nhau được nghe gần nhất của 1 người.

    =====================================================================
    LÀM VIỆC VỚI 2 DATABASE
    =====================================================================

        MongoDB (listening_history)          MySQL (songs)
        ┌────────┬────────┬──────────┐       ┌────┬───────────┐
        │ userId │ songId │ playedAt │       │ id │ title     │
        │   5    │   15   │ 22:55    │ ────► │ 15 │ Chiều Mưa │
        │   5    │    7   │ 22:40    │ ────► │  7 │ Phố Đêm   │
        └────────┴────────┴──────────┘       └────┴───────────┘

    MongoDB và MySQL không JOIN với nhau được.
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

    public void record(Long userId, Long songId) {
        try {
            historyRepository.save(new ListeningHistory(userId, songId, LocalDateTime.now()));
        } catch (Exception e) {
            log.warn("Could not save listening history (user {}, song {}): {}",
                    userId, songId, e.getMessage());
        }
    }

    public List<ListeningHistoryResponse> getRecentSongs(Long userId) {
        List<ListeningHistory> recentPlays = historyRepository.findTop100ByUserIdOrderByPlayedAtDesc(userId);

        Map<Long, LocalDateTime> lastPlayedAt = new LinkedHashMap<>();

        for(ListeningHistory play : recentPlays) {
            lastPlayedAt.putIfAbsent(play.getSongId(), play.getPlayedAt());
        }

        if(lastPlayedAt.isEmpty()) {
            return List.of();
        }

        /*
            Chọc vào bảng song trong MySQL để lấy thêm thông tin bài hát
        */
        Map<Long, Song> songsById = new HashMap<>();
        for(Song song : songRepository.findPublishedByIds(lastPlayedAt.keySet(), LocalDateTime.now())) {
            songsById.put(song.getId(), song);
        }

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