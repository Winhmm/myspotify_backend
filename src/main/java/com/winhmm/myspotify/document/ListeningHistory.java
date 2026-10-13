package com.winhmm.myspotify.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/*
    ===================================================================
    ListeningHistory - 1 LƯỢT NGHE của 1 người dùng, lưu trong MongoDB.
    ===================================================================

    @Document thay cho @Entity của MySQL:
        1 class    ↔ 1 collection   (ListeningHistory ↔ listening_history)
        1 đối tượng ↔ 1 document    (1 lượt nghe)
        1 thuộc tính ↔ 1 field
    Không cần tạo collection trước: ghi document đầu tiên là collection tự xuất hiện.

    Document trong MongoDB trông như sau:
        {
            "_id": ObjectId("6710a3f2c9e77b1a2d3e4f50"),
            "userId": 5,
            "songId": 15,
            "playedAt": ISODate("2026-10-12T15:55:10Z"),
            "_class": "com.winhmm.myspotify.document.ListeningHistory"
        }
    (_class do Spring tự thêm, để biết đọc document thành class nào.)

    VÌ SAO 1 DOCUMENT CHO MỖI LƯỢT NGHE (không gom mảng theo từng người)?
    - 1 document tối đa 16MB, mảng lịch sử tăng mãi sẽ chạm giới hạn.
    - TTL index (tự xóa sau 90 ngày) chỉ hoạt động trên từng document.

    CHỈ LƯU ID, KHÔNG LƯU TÊN BÀI:
    - Artist đổi tên bài → lịch sử tự hiển thị tên mới (đọc từ MySQL lúc hiển thị).
    - Bài bị ẩn / gỡ / xóa → lúc hiển thị tự bỏ qua.

    2 INDEX (Spring tự tạo nhờ auto-index-creation=true):
    1. idx_user_played_at (userId tăng, playedAt giảm): tăng tốc truy vấn
       "lịch sử của user X, mới nhất trước".
    2. idx_played_at_ttl: TTL INDEX, MongoDB tự xóa document quá 90 ngày
       (tác vụ dọn chạy khoảng 60 giây 1 lần, có thể trễ vài phút).
*/
@Document(collection = "listening_history")
@CompoundIndex(name = "idx_user_played_at", def = "{'userId': 1, 'playedAt': -1}")
public class ListeningHistory {
    /*
        Khóa chính (_id). MongoDB tự sinh ObjectId khi lưu.
        Lưu ý import: org.springframework.data.annotation.Id, KHÔNG phải jakarta.persistence.Id.
    */
    @Id
    private String id;

    /*
        id người nghe (= users.id bên MySQL). Dùng id, không dùng email,
        vì người dùng có thể đổi email.
        MongoDB không có khóa ngoại, không tự kiểm tra user có tồn tại không.
    */
    private Long userId;

    /*
        id bài hát (= songs.id bên MySQL).
    */
    private Long songId;

    /*
        Thời điểm bắt đầu nghe. Document tự bị xóa sau 90 ngày tính từ giá trị này.
    */
    @Indexed(name = "idx_played_at_ttl", expireAfter = "90d")
    private LocalDateTime playedAt;

    /*
        Constructor rỗng: Spring cần để tạo đối tượng khi đọc từ MongoDB.
    */
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