package com.winhmm.myspotify.repository;

import com.winhmm.myspotify.document.ListeningHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

/*
    ===================================================================
    ListeningHistoryRepository - Truy cập collection listening_history (MongoDB).
    ===================================================================

    extends MongoRepository<ListeningHistory, String>:
        ListeningHistory = document quản lý, String = kiểu của _id.
    Dùng giống hệt JpaRepository: save(), findById(), count()...
    Spring tự biết: extends JpaRepository → MySQL, extends MongoRepository → MongoDB.

    Log khởi động có dòng "Multiple Spring Data modules found..." là bình thường.
*/
public interface ListeningHistoryRepository extends MongoRepository<ListeningHistory, String> {
    /*
        Lịch sử của 1 người, mới nhất trước, có phân trang.
        Tương đương lệnh MongoDB:
            db.listening_history.find({ userId: 5 }).sort({ playedAt: -1 }).skip(0).limit(20)
        Dùng đúng index idx_user_played_at → nhanh dù có hàng triệu document.
    */
    Page<ListeningHistory> findByUserIdOrderByPlayedAtDesc(Long userId, Pageable pageable);
}
