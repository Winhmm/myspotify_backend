package com.winhmm.myspotify.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/*
    =====================================================================
    ListeningHistory - 1 LƯỢT NGHE CỦA 1 NGƯỜI DÙNG (MONGODB)
    =====================================================================

    Các field:
        _id, userId, songId, playedAt

    Collection: listening_history (trong database myspotify_db trên MongoDB Atlas).

    =====================================================================
    @Document THAY CHO @Entity
    =====================================================================

    Với MySQL ta dùng @Entity. Với MongoDB dùng @Document:
        1 class      ↔ 1 collection   (ListeningHistory ↔ listening_history)
        1 đối tượng  ↔ 1 document     (1 lượt nghe)
        1 thuộc tính ↔ 1 field

    Không cần tạo collection trước, không có ddl-auto:
    ghi document đầu tiên là collection tự xuất hiện.

    Document trong MongoDB trông như sau:
        {
            "_id": ObjectId("6710a3f2c9e77b1a2d3e4f50"),
            "userId": 5,
            "songId": 15,
            "playedAt": ISODate("2026-10-12T15:55:10Z"),
            "_class": "com.winhmm.myspotify.document.ListeningHistory"
        }
    _class do Spring tự thêm, để biết đọc document này thành class nào.

    =====================================================================
    VÌ SAO 1 DOCUMENT CHO MỖI LƯỢT NGHE?
    =====================================================================

    Không gom thành 1 document cho mỗi người kiểu { userId: 5, history: [ ... ] }, vì:
    - 1 document tối đa 16MB, mảng lịch sử tăng mãi sẽ chạm giới hạn.
    - Mỗi lần nghe phải sửa cả document lớn, chậm dần theo thời gian.
    - TTL index (tự xóa sau 90 ngày) chỉ hoạt động trên từng document.
    Quy tắc thiết kế MongoDB: không nhét mảng tăng không giới hạn vào 1 document.

    =====================================================================
    CHỈ LƯU ID, KHÔNG LƯU TÊN BÀI
    =====================================================================

    - Artist đổi tên bài → lịch sử tự hiển thị tên mới (đọc từ MySQL lúc hiển thị).
    - Bài bị ẩn, gỡ, xóa → lúc hiển thị tự bỏ qua.
    Đánh đổi: hiển thị lịch sử phải đọc thêm MySQL (xem ListeningHistoryService).

    =====================================================================
    2 INDEX
    =====================================================================

    Spring tự tạo khi khởi động, nhờ spring.data.mongodb.auto-index-creation=true
    trong application.properties (mặc định tắt, thiếu dòng đó thì không có index nào).

    1. idx_user_played_at (userId tăng dần, playedAt giảm dần):
       Phục vụ truy vấn "lịch sử của user X, mới nhất trước".
       Không có index → MongoDB phải đọc toàn bộ collection (COLLSCAN), rất chậm
       khi có hàng triệu lượt nghe.

    2. idx_played_at_ttl: TTL index (Time To Live).
       MongoDB tự xóa document khi playedAt đã quá 90 ngày.
       → Không cần viết code dọn dẹp, collection không phình to mãi.
       Tác vụ dọn của MongoDB chạy khoảng 60 giây 1 lần, nên có thể xóa trễ vài phút.

    Kiểm tra trên Atlas: Data Explorer → listening_history → tab Indexes.
*/
@Document(collection = "listening_history")
@CompoundIndex(name = "idx_user_played_at", def = "{'userId': 1, 'playedAt': -1}")
public class ListeningHistory {
    @Id
    private String id;

    private Long userId;

    private Long songId;

    @Indexed(name = "idx_played_at_ttl", expireAfter = "90d")
    private LocalDateTime playedAt;

    public ListeningHistory() {}

    public ListeningHistory(Long userId, Long songId, LocalDateTime playedAt) {
        this.userId = userId;
        this.songId = songId;
        this.playedAt = playedAt;
    }

    public String getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getSongId() { return songId; }
    public LocalDateTime getPlayedAt() { return playedAt; }
}