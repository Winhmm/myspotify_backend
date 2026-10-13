package com.winhmm.myspotify.service;

import com.winhmm.myspotify.document.ListeningHistory;
import com.winhmm.myspotify.dto.song.ListeningHistoryResponse;
import com.winhmm.myspotify.dto.song.PageResponse;
import com.winhmm.myspotify.entity.Song;
import com.winhmm.myspotify.mapper.SongMapper;
import com.winhmm.myspotify.repository.ListeningHistoryRepository;
import com.winhmm.myspotify.repository.SongRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
    ===================================================================
    ListeningHistoryService - Lịch sử nghe nhạc (MongoDB).
    ===================================================================

    1. record(...)       : ghi 1 lượt nghe (gọi từ SongService.play).
    2. getMyHistory(...) : đọc lịch sử 1 người, mới nhất trước, có phân trang.

    LÀM VIỆC VỚI 2 DATABASE:

        MongoDB (listening_history)          MySQL (songs)
        ┌────────┬────────┬──────────┐       ┌────┬───────────┐
        │ userId │ songId │ playedAt │       │ id │ title     │
        │   5    │   15   │ 22:55    │ ────► │ 15 │ Chiều Mưa │
        │   5    │    7   │ 22:40    │ ────► │  7 │ Phố Đêm   │
        └────────┴────────┴──────────┘       └────┴───────────┘

    MongoDB và MySQL không JOIN được với nhau.
    → Java tự ghép: đọc MongoDB lấy songId, đọc MySQL lấy thông tin bài, ghép lại.
    Gọi là "application-side join" (ghép dữ liệu ở tầng ứng dụng).
*/
@Service
public class ListeningHistoryService {
    private static final Logger log = LoggerFactory.getLogger(ListeningHistoryService.class);
    private static final int MAX_PAGE_SIZE = 50;

    private final ListeningHistoryRepository historyRepository;
    private final SongRepository songRepository;

    public ListeningHistoryService(ListeningHistoryRepository historyRepository,
                                   SongRepository songRepository) {
        this.historyRepository = historyRepository;
        this.songRepository = songRepository;
    }

    /* ================= GHI LỊCH SỬ ================= */

    /*
        Ghi 1 lượt nghe vào MongoDB.
        Tương đương: db.listening_history.insertOne({ userId: 5, songId: 15, playedAt: ... })

        MongoDB lỗi (mất mạng, Atlas bảo trì...) → chỉ ghi log cảnh báo, KHÔNG ném lỗi.
        Lịch sử là tính năng phụ, không được làm hỏng chức năng chính là phát nhạc.
    */
    public void record(Long userId, Long songId) {
        try {
            historyRepository.save(new ListeningHistory(userId, songId, LocalDateTime.now()));
        } catch (Exception e) {
            log.warn("Could not save listening history (user {}, song {}): {}",
                    userId, songId, e.getMessage());
        }
    }

    /* ================= ĐỌC LỊCH SỬ ================= */

    /*
        CÁC BƯỚC:
        1. Phân trang an toàn (page >= 0, size 1 - 50).
        2. Đọc MongoDB: 1 trang lịch sử của user.
        3. Gom songId khác nhau (Set tự loại trùng: nghe 1 bài 3 lần → chỉ hỏi MySQL 1 lần).
        4. Đọc MySQL 1 lần: các bài ĐÃ PHÁT HÀNH trong danh sách id.
        5. Đưa vào Map<id, Song> để tra nhanh.
        6. Ghép theo đúng thứ tự thời gian. Bài không còn phát hành → bỏ qua.

        Giới hạn: bài bị bỏ qua vẫn tính trong totalElements, nên 1 trang có thể
        ít hơn size (xin 20, nhận 18). Chấp nhận được với "Nghe gần đây".
    */
    public PageResponse<ListeningHistoryResponse> getMyHistory(Long userId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        Page<ListeningHistory> historyPage = historyRepository.findByUserIdOrderByPlayedAtDesc(
                userId, PageRequest.of(safePage, safeSize));

        Set<Long> songIds = new HashSet<>();
        for (ListeningHistory history : historyPage.getContent()) {
            songIds.add(history.getSongId());
        }

        /*
            Danh sách rỗng → không gọi MySQL ("WHERE id IN ()" rỗng không hợp lệ).
        */
        Map<Long, Song> songsById = new HashMap<>();
        if (!songIds.isEmpty()) {
            for (Song song : songRepository.findPublishedByIds(songIds, LocalDateTime.now())) {
                songsById.put(song.getId(), song);
            }
        }

        List<ListeningHistoryResponse> content = new ArrayList<>();
        for (ListeningHistory history : historyPage.getContent()) {
            Song song = songsById.get(history.getSongId());

            if (song != null) {
                content.add(new ListeningHistoryResponse(
                        SongMapper.toSongResponse(song), history.getPlayedAt()));
            }
        }

        return new PageResponse<>(
                content,
                historyPage.getNumber(),
                historyPage.getSize(),
                historyPage.getTotalElements(),
                historyPage.getTotalPages()
        );
    }
}