package com.winhmm.myspotify.repository;

import com.winhmm.myspotify.document.ListeningHistory;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ListeningHistoryRepository extends MongoRepository<ListeningHistory, String> {
    /*
        Nếu người dùng nghe đi nghe lại vài bài, 10 lượt gần nhất có thể chỉ gồm 2 đến 3 bài khác nhau.

        Lấy 100 lượt gần như chắc chắn đủ 10 bài khác nhau, mà vẫn rất nhẹ (100 document nhỏ, có index).
    */
    List<ListeningHistory> findTop100ByUserIdOrderByPlayedAtDesc(long userId);
}