package com.winhmm.myspotify.repository;

import com.winhmm.myspotify.document.ListeningHistory;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/*
    =====================================================
    LISTENINGHISTORYREPOSITORY - TRUY CẬP COLLECTION LISTENING_HISTORY (MONGODB)
    =====================================================

    (Repository là gì, cách Spring đọc tên method: xem GenreRepository.
     Thứ tự đọc package repository: xem GenreRepository.)

    Lưu và đọc lịch sử nghe nhạc trong MongoDB.
    Chỉ được dùng trong ListeningHistoryService.

    -----------------------------------------------------
    MongoDB khác MySQL thế nào? (đọc nếu mới học)
    -----------------------------------------------------
        MySQL                           MongoDB
        bảng (table)                    collection   (listening_history)
        1 dòng (row)                    1 document   (giống 1 object JSON)
        cột cố định, tạo bảng trước     mỗi document tự mang các field của nó
        khóa ngoại, JOIN                không có khóa ngoại
    1 document trong collection này:
        { "_id": "6712f0a3c9e77b2d4a1f5e10", "userId": 6, "songId": 15, "playedAt": ... }
    userId, songId chỉ là con số, MongoDB không biết chúng trỏ tới bảng users, songs bên MySQL.
    Muốn lấy thông tin bài hát → ListeningHistoryService phải sang MySQL hỏi thêm
    (SongRepository.findPublishedByIds).

    Vì sao lịch sử nghe để MongoDB, không để MySQL?
    Mỗi lượt nghe = 1 document mới → ghi rất nhiều, dữ liệu đơn giản, không cần JOIN,
    và cần tự xóa dữ liệu cũ sau 90 ngày (TTL index, xem document ListeningHistory).

    -----------------------------------------------------
    extends MongoRepository<ListeningHistory, String>
    -----------------------------------------------------
    - MongoRepository: bản dành cho MongoDB, tương tự JpaRepository của MySQL.
    - ListeningHistory: document mà repository này quản lý (class có @Document, không phải @Entity).
    - String: kiểu của khóa chính. MongoDB tự sinh _id dạng chuỗi ObjectId,
      VD "6712f0a3c9e77b2d4a1f5e10", không phải số tự tăng như MySQL.

    Cách đặt tên method giống hệt JpaRepository của MySQL,
    Spring Data tự dịch sang câu truy vấn của MongoDB thay vì SQL.
    Có sẵn save() để ghi 1 lượt nghe mới:
        historyRepository.save(new ListeningHistory(userId, songId, LocalDateTime.now()))
        → MongoDB thêm 1 document, tự sinh _id.
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
        (So với SQL: find({...}) ~ WHERE, sort({ playedAt: -1 }) ~ ORDER BY played_at DESC,
         limit(100) ~ LIMIT 100. Số -1 nghĩa là giảm dần, 1 là tăng dần.)

        Lưu ý: ByUserId ở đây là field userId có thật trong document (kiểu Long),
        không phải đi theo quan hệ như SongId trong SongLyricsRepository.

        Vì sao lấy 100, trong khi chỉ cần hiển thị 10 bài?
        Nếu người dùng nghe đi nghe lại vài bài, 10 lượt gần nhất có thể chỉ gồm 2 đến 3 bài khác nhau.
        Lấy 100 lượt gần như chắc chắn đủ 10 bài khác nhau, mà vẫn rất nhẹ (100 document nhỏ).
        ListeningHistoryService lọc trùng, giữ thứ tự, rồi lấy 10 bài đầu.

        Truy vấn này dùng index idx_user_played_at { userId: 1, playedAt: -1 } để chạy nhanh:
        khớp đúng điều kiện (userId) lẫn thứ tự sắp xếp (playedAt giảm dần).
        Index được khai báo bằng @CompoundIndex trong document ListeningHistory,
        Spring tự tạo trên MongoDB lúc khởi động nhờ cấu hình:
            spring.data.mongodb.auto-index-creation=true
        (Mặc định cấu hình này tắt. Thiếu dòng đó thì index không được tạo:
         ứng dụng vẫn chạy nhưng truy vấn chậm dần khi dữ liệu nhiều.
         Kiểm tra trên Atlas: Data Explorer → listening_history → tab Indexes.)
    */
    List<ListeningHistory> findTop100ByUserIdOrderByPlayedAtDesc(Long userId);
}