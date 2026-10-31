package com.winhmm.myspotify.document;

/*
    -----------------------------------------------------
    Dòng package
    -----------------------------------------------------
    File nằm ở src/main/java/com/winhmm/myspotify/document/ListeningHistory.java
    nên package là com.winhmm.myspotify.document.

    Vì sao đặt trong thư mục document, không đặt chung thư mục entity?
    - entity  : class ánh xạ bảng MySQL (User, Song, Genre...), dùng annotation của JPA.
    - document: class ánh xạ collection MongoDB, dùng annotation của Spring Data MongoDB.
    Tách riêng thì nhìn tên thư mục là biết class thuộc database nào,
    và không dùng nhầm annotation của bên này cho bên kia.

    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    - Id           : đánh dấu thuộc tính làm khóa chính (field _id) của document.
    - CompoundIndex: khai báo 1 index gồm nhiều field (index kép).
    - Indexed      : khai báo index trên 1 field (ở đây dùng làm TTL index, tự xóa dữ liệu cũ).
    - Document     : đánh dấu class này ánh xạ tới 1 collection của MongoDB.
    - LocalDateTime: kiểu ngày giờ của Java (năm, tháng, ngày, giờ, phút, giây),
                     không kèm thông tin múi giờ.

    Lưu ý dễ nhầm với Id:
    Entity MySQL dùng jakarta.persistence.Id (của JPA).
    Document MongoDB dùng org.springframework.data.annotation.Id (của Spring Data), như dòng import bên dưới.
    2 annotation trùng tên Id, IntelliJ gợi ý cả 2, khi import phải chọn đúng gói.
    MongoDB không dùng JPA, nên không dùng annotation của JPA ở đây.
*/
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/*
    =====================================================================
    LISTENINGHISTORY - 1 LƯỢT NGHE CỦA 1 NGƯỜI DÙNG (MONGODB)
    =====================================================================

    Mỗi lần 1 người nghe 1 bài (và lượt nghe đó được tính),
    backend lưu 1 document vào MongoDB. Class này mô tả hình dạng của document đó.

    Các field:
        _id      : mã của document, MongoDB tự sinh
        userId   : id người nghe (cột id của bảng users bên MySQL)
        songId   : id bài hát (cột id của bảng songs bên MySQL)
        playedAt : thời điểm nghe

    Collection: listening_history.
    Database  : myspotify_db ở máy local, myspotify_prod trên VPS.
                Tên database nằm trong chuỗi kết nối spring.mongodb.uri của từng file cấu hình
                (đoạn giữa ".mongodb.net/" và dấu "?").

    =====================================================================
    MONGODB LÀ GÌ? SO SÁNH VỚI MYSQL
    =====================================================================

    MySQL lưu dữ liệu dạng bảng, gồm hàng và cột, giống 1 trang Excel.
    MongoDB lưu dữ liệu dạng document, trông giống 1 object JSON.

        MySQL               MongoDB
        database            database
        bảng (table)        collection
        1 hàng (row)        1 document
        1 cột (column)      1 field
        khóa chính id       khóa chính _id
        câu lệnh SQL        câu lệnh dạng JSON, VD db.listening_history.find({ userId: 5 })

    Những khác biệt quan trọng với người mới:
    - MySQL bắt buộc tạo bảng và khai báo cột trước (CREATE TABLE),
      mỗi hàng phải theo đúng các cột đó.
    - MongoDB không bắt buộc cấu trúc cố định: về lý thuyết mỗi document có thể có field khác nhau.
      Class Java này chính là "quy ước" để mọi document lượt nghe có cùng 1 hình dạng.
    - MongoDB không có khóa ngoại (foreign key) và không JOIN được với bảng MySQL.
      userId = 5 ở đây chỉ là 1 con số, MongoDB không kiểm tra user 5 có tồn tại hay không.
      Muốn biết user 5 hay bài 15 là gì thì phải tự hỏi thêm MySQL bằng code Java.

    =====================================================================
    VÌ SAO LỊCH SỬ NGHE LƯU Ở MONGODB, KHÔNG LƯU Ở MYSQL?
    =====================================================================

    Lịch sử nghe có đặc điểm khác hẳn dữ liệu còn lại của project:
    - Ghi rất nhiều: mỗi lượt nghe là 1 bản ghi mới, số lượng tăng nhanh hơn mọi bảng khác.
    - Chỉ thêm mới, không bao giờ sửa: lượt nghe đã xảy ra thì không thay đổi nữa.
    - Không cần giữ mãi: lịch sử cũ hơn 90 ngày là bỏ được.
    - Không cần ràng buộc chặt chẽ với bảng khác.

    MongoDB hợp với kiểu dữ liệu này: ghi nhanh, và có sẵn TTL index để tự xóa dữ liệu cũ.
    MySQL không có TTL, muốn xóa dữ liệu cũ phải tự viết tác vụ dọn dẹp chạy định kỳ.
    Ngoài ra, đây cũng là dịp để project dùng thêm 1 loại database khác ngoài MySQL.

    =====================================================================
    @Document THAY CHO @Entity
    =====================================================================

    Với MySQL ta dùng @Entity (của JPA, do Hibernate xử lý).
    Với MongoDB dùng @Document (do Spring Data MongoDB xử lý):
        1 class      ↔ 1 collection   (ListeningHistory ↔ listening_history)
        1 đối tượng  ↔ 1 document     (1 lượt nghe)
        1 thuộc tính ↔ 1 field        (thuộc tính userId ↔ field "userId")

    Không cần tạo collection trước, cũng không có ddl-auto như bên MySQL.
    Collection tự được tạo khi Spring tạo index lúc khởi động, hoặc khi ghi document đầu tiên.

    Document thật trong MongoDB trông như sau
    (xem trên Atlas: Data Explorer → chọn database → listening_history):
        {
            "_id": ObjectId("6710a3f2c9e77b1a2d3e4f50"),
            "userId": 5,
            "songId": 15,
            "playedAt": ISODate("2026-10-12T08:55:10Z"),
            "_class": "com.winhmm.myspotify.document.ListeningHistory"
        }

    - _id: khóa chính bắt buộc của mọi document trong MongoDB.
      Thuộc tính id trong Java (có @Id) được lưu thành field _id.
    - _class: Spring tự thêm, ghi tên đầy đủ của class Java,
      để khi đọc lên biết phải đổi document này thành đối tượng của class nào.
      Mình không khai báo field này trong class, cũng không cần dùng tới.
    - Các field còn lại lấy đúng tên thuộc tính Java: userId, songId, playedAt.

    =====================================================================
    THỜI GIAN ĐƯỢC LƯU THEO GIỜ UTC
    =====================================================================

    Ở document trên: người dùng nghe lúc 15:55:10 giờ Việt Nam,
    nhưng Atlas hiện "2026-10-12T08:55:10Z", lệch 7 tiếng. Đây không phải lỗi.

    - MongoDB luôn lưu ngày giờ theo UTC (giờ quốc tế). Chữ Z ở cuối nghĩa là UTC.
      Việt Nam là UTC+7, nên 15:55 giờ Việt Nam = 08:55 UTC.
    - LocalDateTime trong Java không kèm múi giờ. Khi lưu, Spring hiểu giá trị đó
      theo múi giờ của máy chủ (Asia/Ho_Chi_Minh) rồi đổi sang UTC.
    - Khi đọc lên, Spring đổi ngược từ UTC về múi giờ máy chủ → lại thành 15:55.
    → Trong code Java luôn thấy giờ Việt Nam. Chỉ khi nhìn thẳng trên Atlas mới thấy giờ UTC.

    Vì vậy máy chủ phải giữ cố định múi giờ Asia/Ho_Chi_Minh.
    Nếu đổi múi giờ máy chủ, giờ đọc ra từ dữ liệu cũ sẽ bị lệch theo.

    =====================================================================
    VÌ SAO 1 DOCUMENT CHO MỖI LƯỢT NGHE?
    =====================================================================

    Có thể gom lịch sử của mỗi người vào 1 document duy nhất, kiểu:
        { "userId": 5, "history": [ { "songId": 15, ... }, { "songId": 7, ... }, ... ] }
    Nhưng project không làm vậy, vì:
    - 1 document tối đa 16MB. Mảng lịch sử tăng mãi thì sớm muộn chạm giới hạn này.
    - Mỗi lần nghe phải sửa cả 1 document ngày càng lớn → càng lúc càng chậm.
    - TTL index (tự xóa sau 90 ngày) xóa nguyên cả document, không xóa được từng phần tử
      nằm bên trong mảng. Gom chung thì không tự xóa lượt nghe cũ được.
    Quy tắc thiết kế MongoDB: không nhét 1 mảng tăng không giới hạn vào 1 document.

    =====================================================================
    CHỈ LƯU ID, KHÔNG LƯU TÊN BÀI
    =====================================================================

    Document chỉ có songId, không có tên bài, tên nghệ sĩ, ảnh bìa...
    - Artist đổi tên bài → lịch sử tự hiển thị tên mới (đọc từ MySQL lúc hiển thị).
    - Bài bị ẩn, bị gỡ, bị xóa → lúc hiển thị tự bỏ qua.
    Nếu lưu sẵn tên bài vào đây, đổi tên bài thì lịch sử vẫn hiện tên cũ.
    Đánh đổi: muốn hiển thị lịch sử phải hỏi thêm MySQL (xem ListeningHistoryService).

    =====================================================================
    INDEX LÀ GÌ?
    =====================================================================

    Index giống phần mục lục ở cuối sách giáo khoa.
    Muốn tìm chữ "đạo hàm": không có mục lục thì phải lật từng trang từ đầu tới cuối,
    có mục lục thì tra ra ngay số trang.

    Không có index, muốn tìm lượt nghe của user 5, MongoDB phải đọc lần lượt
    toàn bộ document trong collection (gọi là COLLSCAN, viết tắt của collection scan).
    Có 1 triệu lượt nghe là phải đọc đủ 1 triệu document, chỉ để lấy vài chục cái.

    Có index, MongoDB tra index để nhảy thẳng tới đúng các document cần.
    Đánh đổi: index chiếm thêm dung lượng, và mỗi lần ghi document mới
    MongoDB phải cập nhật thêm index (chậm hơn 1 chút, không đáng kể).

    Mọi collection luôn có sẵn 1 index trên _id (tên là _id_), MongoDB tự tạo.
    Class này khai báo thêm 2 index (giải thích ở @CompoundIndex và @Indexed bên dưới).
    → Trên Atlas (Data Explorer → listening_history → tab Indexes) sẽ thấy 3 index:
        _id_, idx_user_played_at, idx_played_at_ttl

    -----------------------------------------------------
    Ai tạo 2 index này?
    -----------------------------------------------------
    Spring tự tạo khi ứng dụng khởi động, nhờ dòng sau trong file cấu hình:
        spring.data.mongodb.auto-index-creation=true
    Mặc định tính năng này tắt. Thiếu dòng đó thì 2 annotation index bên dưới bị bỏ qua:
    ứng dụng vẫn chạy, nhưng truy vấn chậm và lịch sử cũ không bao giờ tự xóa.

    Mỗi lần khởi động Spring đều gọi lệnh tạo index.
    Index giống hệt đã tồn tại thì MongoDB bỏ qua, không tạo trùng.

    =====================================================================
    CLASS NÀY ĐƯỢC DÙNG Ở ĐÂU?
    =====================================================================

    Ghi (mỗi lượt nghe được tính):
        Frontend gọi POST /api/songs/{songId}/play
        → SongService.play: chống đếm trùng 30 giây bằng Redis, tăng play_count trong MySQL
        → ListeningHistoryService.record(userId, songId)
          chạy ở thread riêng (@Async), API /play trả kết quả ngay, không chờ MongoDB
        → historyRepository.save(new ListeningHistory(userId, songId, LocalDateTime.now()))
        → MongoDB thêm 1 document mới.
        Ghi lỗi (VD mất kết nối Atlas) → chỉ ghi log cảnh báo, người dùng vẫn nghe nhạc bình thường.

    Đọc (mục "Nghe gần đây"):
        Frontend gọi GET /api/users/me/history
        → ListeningHistoryService.getRecentSongs(userId)
        → historyRepository.findTop100ByUserIdOrderByPlayedAtDesc(userId)
          tương đương lệnh MongoDB:
              db.listening_history.find({ userId: 5 }).sort({ playedAt: -1 }).limit(100)
        → lọc trùng, hỏi MySQL thông tin các bài, lấy ra 10 bài khác nhau.

    ListeningHistoryRepository extends MongoRepository<ListeningHistory, String>:
    - ListeningHistory: class document mà repository quản lý (chính là class này).
    - String          : kiểu dữ liệu của khóa chính (thuộc tính id bên dưới).
*/
/*
    -----------------------------------------------------
    @Document(collection = "listening_history")
    -----------------------------------------------------
    Báo cho Spring Data MongoDB: đối tượng của class này được lưu vào collection listening_history.

    collection = "...": tên collection.
    Nếu không ghi, Spring tự đặt tên theo tên class, đổi chữ cái đầu thành chữ thường
    → "listeningHistory". Ghi rõ ra để tên collection theo kiểu chữ thường, nối bằng dấu gạch dưới,
    giống cách đặt tên bảng bên MySQL (songs, song_featured_artists...).

    -----------------------------------------------------
    @CompoundIndex: index kép trên 2 field userId và playedAt
    -----------------------------------------------------
    Compound index = 1 index gồm nhiều field, sắp xếp theo thứ tự field ghi trong def.

    - name = "idx_user_played_at": tên index, hiện trên Atlas. "idx" là viết tắt của index.
    - def  = "{'userId': 1, 'playedAt': -1}": định nghĩa index, viết dạng JSON trong 1 chuỗi Java.
      + Trong chuỗi Java đã dùng dấu " ở 2 đầu, nên bên trong dùng dấu ' cho tên field.
      + 1  = sắp xếp tăng dần.
      + -1 = sắp xếp giảm dần.
      → Index sắp xếp theo userId tăng dần trước. Trong cùng 1 userId, sắp xếp playedAt
        giảm dần (lượt nghe mới nhất đứng đầu).

    Hình dung index như 1 danh sách đã xếp sẵn:
        userId 5, 15:55   ← mới nhất của user 5
        userId 5, 15:40
        userId 5, 09:12
        userId 6, 20:30   ← mới nhất của user 6
        userId 6, 18:05
        ...

    Truy vấn "100 lượt nghe mới nhất của user 5" chỉ cần:
    1. Nhảy tới chỗ bắt đầu của userId 5 trong index.
    2. Đọc lần lượt 100 dòng từ đó trở đi. Các dòng đã xếp sẵn mới nhất trước,
       MongoDB không phải sắp xếp lại.

    Thứ tự field trong def rất quan trọng, giống danh bạ điện thoại xếp theo họ rồi mới tới tên:
    - Tìm theo họ (userId): dùng được index.
    - Tìm theo họ + tên (userId + playedAt): dùng được index.
    - Chỉ tìm theo tên (chỉ playedAt): không tận dụng được index này.

    @CompoundIndex đặt trên class (không đặt trên 1 thuộc tính), vì index liên quan tới nhiều field.
*/
@Document(collection = "listening_history")
@CompoundIndex(name = "idx_user_played_at", def = "{'userId': 1, 'playedAt': -1}")
public class ListeningHistory {
    /*
        -----------------------------------------------------
        id: khóa chính của document (field _id trong MongoDB)
        -----------------------------------------------------
        @Id: thuộc tính này là khóa chính, được lưu thành field _id.

        Vì sao kiểu String, không phải Long như entity MySQL?
        - MySQL dùng số tự tăng: 1, 2, 3...
        - MongoDB mặc định dùng ObjectId: 1 mã 12 byte, hiển thị thành 24 ký tự
          gồm chữ số và chữ cái a đến f, VD "6710a3f2c9e77b1a2d3e4f50".
          ObjectId được ghép từ thời điểm tạo, 1 phần ngẫu nhiên và 1 bộ đếm,
          nên gần như không bao giờ trùng, kể cả khi nhiều máy cùng ghi 1 lúc.
        - Khai báo kiểu String cho dễ dùng trong Java.
          Spring tự đổi qua lại giữa chuỗi trong Java và ObjectId trong MongoDB.

        Lúc tạo đối tượng mới bằng new ListeningHistory(...), id đang là null.
        Khi gọi save(), MongoDB sinh ObjectId mới, Spring gán ngược lại vào thuộc tính id.
        → Không bao giờ tự đặt id bằng tay.

        - private: chỉ dùng được bên trong class này, bên ngoài đọc qua getter.
    */
    @Id
    private String id;

    /*
        -----------------------------------------------------
        userId: id của người nghe
        -----------------------------------------------------
        Trùng với cột id của bảng users bên MySQL.

        Vì sao lưu id, không lưu email?
        Email đổi được (UC13 đổi email), còn id thì không bao giờ đổi.
        Lưu email thì đổi email xong sẽ mất hết lịch sử cũ.

        Long (chữ L hoa) thay vì long (chữ l thường):
        - long: kiểu nguyên thủy, không thể null.
        - Long: kiểu đối tượng, có thể null.
        Dùng Long cho đồng bộ với kiểu id của entity User bên MySQL.
        Trong MongoDB, giá trị này được lưu thành số nguyên 64 bit (Atlas ghi kiểu Int64).
    */
    private Long userId;

    /*
        -----------------------------------------------------
        songId: id của bài hát được nghe
        -----------------------------------------------------
        Trùng với cột id của bảng songs bên MySQL.
        Chỉ lưu id, không lưu tên bài (lý do: xem phần "Chỉ lưu id, không lưu tên bài" ở trên).
    */
    private Long songId;

    /*
        -----------------------------------------------------
        playedAt: thời điểm nghe, kèm TTL index tự xóa sau 90 ngày
        -----------------------------------------------------
        Giá trị do ListeningHistoryService gán bằng LocalDateTime.now() lúc ghi lượt nghe.
        Lưu trong MongoDB theo giờ UTC (xem phần "Thời gian được lưu theo giờ UTC" ở trên).

        @Indexed(...): tạo index trên 1 field duy nhất là playedAt.
        - name = "idx_played_at_ttl": tên index, hiện trên Atlas.
        - expireAfter = "90d": biến index này thành TTL index (Time To Live, "thời gian sống").

        TTL index hoạt động thế nào?
        - MongoDB có 1 tác vụ chạy ngầm, khoảng 60 giây chạy 1 lần.
        - Mỗi lần chạy, nó xóa các document có playedAt + 90 ngày < thời điểm hiện tại.
          VD nghe ngày 01/01 → khoảng ngày 01/04 document tự bị xóa.
        - Vì 60 giây mới chạy 1 lần, document có thể bị xóa trễ vài phút. Không sao.
        → Không cần viết code dọn dẹp, collection không phình to mãi.

        Cách viết thời gian trong expireAfter: số + đơn vị.
            "30s" = 30 giây, "10m" = 10 phút, "12h" = 12 giờ, "90d" = 90 ngày.

        TTL index chỉ hoạt động khi field có kiểu ngày giờ trong MongoDB.
        LocalDateTime được Spring lưu thành kiểu ngày giờ (ISODate) nên dùng được.
        Nếu lưu thời gian dạng chuỗi (VD "2026-10-12 15:55") thì TTL không xóa gì cả.

        Muốn đổi thời gian giữ lịch sử, VD từ 90 ngày sang 30 ngày:
        sửa "90d" thành "30d" thôi là chưa đủ.
        MongoDB không cho tạo index trùng tên nhưng khác cấu hình, Spring sẽ báo lỗi,
        và index cũ (90 ngày) vẫn giữ nguyên.
        Phải vào Atlas → listening_history → tab Indexes → xóa (Drop) idx_played_at_ttl,
        rồi khởi động lại ứng dụng để Spring tạo index mới.
    */
    @Indexed(name = "idx_played_at_ttl", expireAfter = "90d")
    private LocalDateTime playedAt;

    /*
        -----------------------------------------------------
        Constructor không tham số
        -----------------------------------------------------
        Spring Data MongoDB cần constructor này khi đọc dữ liệu lên:
        1. MongoDB trả về 1 document.
        2. Spring gọi new ListeningHistory() để tạo 1 đối tượng rỗng (mọi thuộc tính đều null).
        3. Spring ghi thẳng giá trị của từng field (_id, userId, songId, playedAt)
           vào thuộc tính tương ứng, kể cả thuộc tính private (dùng kỹ thuật reflection
           của Java, cho phép đọc và ghi thuộc tính private từ bên ngoài class).

        Nhờ Spring ghi thẳng vào thuộc tính, class này không cần setter.

        Khi class có nhiều constructor, Spring Data ưu tiên dùng constructor không tham số này.
        Code của mình không dùng tới nó.
    */
    public ListeningHistory() {}

    /*
        -----------------------------------------------------
        Constructor 3 tham số
        -----------------------------------------------------
        Dùng khi ghi 1 lượt nghe mới, trong ListeningHistoryService.record:
            new ListeningHistory(userId, songId, LocalDateTime.now())

        Không có tham số id, vì id do MongoDB tự sinh lúc save() (xem giải thích ở thuộc tính id).

        this.userId = userId:
        - this.userId: thuộc tính của đối tượng (khai báo ở trên).
        - userId      : tham số vừa được truyền vào.
        → Gán giá trị tham số vào thuộc tính. 2 dòng còn lại tương tự.
    */
    public ListeningHistory(Long userId, Long songId, LocalDateTime playedAt) {
        this.userId = userId;
        this.songId = songId;
        this.playedAt = playedAt;
    }

    /*
        -----------------------------------------------------
        Getter: cho code bên ngoài đọc giá trị
        -----------------------------------------------------
        Thuộc tính là private nên bên ngoài không đọc trực tiếp được,
        phải gọi getter, VD play.getSongId().
        ListeningHistoryService dùng getSongId() và getPlayedAt() để lọc trùng bài
        và hiển thị thời điểm nghe.

        Mỗi getter viết gọn trên 1 dòng, tương đương cách viết nhiều dòng:
            public Long getUserId() {
                return userId;
            }

        Vì sao chỉ có getter, không có setter?
        Lượt nghe đã xảy ra thì không bao giờ sửa. Không có setter thì code của mình
        không thể vô tình sửa lịch sử sau khi đã tạo đối tượng.
        Việc đọc dữ liệu từ MongoDB vẫn chạy bình thường, vì Spring ghi thẳng vào thuộc tính
        (xem constructor không tham số ở trên).
    */
    public String getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getSongId() { return songId; }
    public LocalDateTime getPlayedAt() { return playedAt; }
}


