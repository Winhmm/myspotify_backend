package com.winhmm.myspotify.repository;

import com.winhmm.myspotify.document.ListeningHistory;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/*
    =====================================================
    LISTENINGHISTORYREPOSITORY - TRUY CẬP COLLECTION LISTENING_HISTORY (MONGODB)
    =====================================================
    Lưu và đọc lịch sử nghe nhạc trong MongoDB.
    Chỉ được dùng trong ListeningHistoryService.

    extends MongoRepository<ListeningHistory, String>:
    - ListeningHistory: document mà repository này quản lý.
    - String: kiểu của khóa chính. MongoDB tự sinh _id dạng chuỗi ObjectId,
      VD "6712f0a3c9e77b2d4a1f5e10", không phải số tự tăng như MySQL.

    Cách đặt tên method giống hệt JpaRepository của MySQL,
    Spring Data tự dịch sang câu truy vấn của MongoDB thay vì SQL.
    Có sẵn save() để ghi 1 lượt nghe mới.
*/
public interface ListeningHistoryRepository extends MongoRepository<ListeningHistory, String> {
    /*
        =====================================================
        100 LƯỢT NGHE GẦN NHẤT CỦA 1 NGƯỜI
        =====================================================
        Tên method được Spring tách thành từng phần:
            findTop100           → giới hạn 100 document
            ByUserId             → điều kiện userId = ?
            OrderByPlayedAtDesc  → sắp xếp thời điểm nghe giảm dần (mới nhất trước)

        Câu truy vấn MongoDB tương đương:
            db.listening_history.find({ userId: 5 }).sort({ playedAt: -1 }).limit(100)

        Vì sao lấy 100, trong khi chỉ cần hiển thị 10 bài?
        Nếu người dùng nghe đi nghe lại vài bài, 10 lượt gần nhất có thể chỉ gồm 2 đến 3 bài khác nhau.
        Lấy 100 lượt gần như chắc chắn đủ 10 bài khác nhau, mà vẫn rất nhẹ (100 document nhỏ).
        ListeningHistoryService lọc trùng, giữ thứ tự, rồi lấy 10 bài đầu.

        Truy vấn này cần index { userId: 1, playedAt: -1 } trên MongoDB Atlas để chạy nhanh.
        Spring Data MongoDB mặc định không tự tạo index từ annotation,
        nên phải tạo tay trên Atlas (tab Indexes của collection listening_history).
    */
    List<ListeningHistory> findTop100ByUserIdOrderByPlayedAtDesc(Long userId);
}