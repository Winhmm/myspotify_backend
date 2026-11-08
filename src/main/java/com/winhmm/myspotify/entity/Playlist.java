package com.winhmm.myspotify.entity;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    (Entity là gì, các annotation cơ bản: xem User. @ManyToOne và LAZY: xem OtpVerification.
     @PrePersist, @PreUpdate: xem Song. Thứ tự đọc package entity: xem User.
     Mọi khái niệm đó cũng được giải thích lại ngay trong file này.)

    Dòng package ở trên cho biết file này nằm ở thư mục src/main/java/com/winhmm/myspotify/entity.
    Class ở package khác muốn dùng Playlist thì phải import com.winhmm.myspotify.entity.Playlist.

    import = mượn class hoặc annotation của thư viện khác để dùng trong file này.
    - jakarta.persistence.*: mượn tất cả annotation của JPA (dấu * = lấy hết trong package đó),
                             gồm @Entity, @Table, @Id, @Column, @ManyToOne, @PrePersist...
    - LocalDateTime        : kiểu ngày giờ của Java (có ngày và giờ, không kèm múi giờ),
                             dùng cho createdAt, updatedAt.
*/
import jakarta.persistence.*;

import java.time.LocalDateTime;

/*
    =====================================================================
    PLAYLIST - ENTITY ÁNH XẠ BẢNG playlists (MYSQL)
    =====================================================================

    Mỗi dòng của bảng = 1 playlist do 1 tài khoản tạo.

    Các cột:
        id          : khóa chính, tự tăng
        user_id     : người tạo playlist (khóa ngoại trỏ tới users.id)
        name        : tên playlist, tối đa 100 ký tự
        description : mô tả, tối đa 300 ký tự, có thể để trống
        is_public   : true = công khai, false = riêng tư (mặc định khi tạo)
        cover_url   : ảnh bìa do người tạo tự tải lên, có thể để trống
        created_at  : thời điểm tạo
        updated_at  : thời điểm sửa gần nhất (đổi tên, đổi ảnh, thêm hoặc bớt bài)

    Ví dụ dữ liệu:
        id | user_id | name           | description          | is_public | created_at
        3  | 6       | Chill buổi tối | Nghe trước khi ngủ   | 1         | 2026-10-27 22:30:00
        4  | 6       | Nhạc chạy bộ   | NULL                 | 0         | 2026-10-28 07:10:00
        → Tài khoản id 6 có 2 playlist: 1 công khai, 1 riêng tư.

    =====================================================================
    ENTITY LÀ GÌ? (ĐỌC TRƯỚC NẾU CHƯA HỌC SPRING)
    =====================================================================

    Java làm việc với object, còn MySQL làm việc với bảng và dòng.
    Entity là 1 class Java được "ánh xạ" (map) với 1 bảng trong MySQL:
        1 class        ↔ 1 bảng      (class Playlist ↔ bảng playlists)
        1 thuộc tính   ↔ 1 cột       (name ↔ cột name, coverUrl ↔ cột cover_url)
        1 object       ↔ 1 dòng      (tạo new Playlist() rồi lưu = thêm 1 dòng vào bảng)

    Ai làm việc chuyển đổi này?
    - JPA (Jakarta Persistence API): bộ quy tắc và annotation chuẩn của Java để ánh xạ class với bảng.
      JPA chỉ là bản mô tả "phải làm gì", tự nó không chạy được.
    - Hibernate: thư viện làm thật theo các quy tắc của JPA. Spring Boot dùng sẵn Hibernate.
      Khi Service gọi playlistRepository.save(playlist), Hibernate tự viết câu INSERT hoặc UPDATE
      rồi gửi xuống MySQL. Mình không phải tự viết SQL.

    Annotation (chữ bắt đầu bằng @) giống "nhãn dán" lên class, thuộc tính hoặc hàm.
    Tự nó không chạy gì, nhưng thư viện (Hibernate, Spring) đọc nhãn để biết phải làm gì.
    VD dán @Column(nullable = false) lên name → Hibernate biết cột name không được để trống.

    Bảng tương đương trong MySQL (gần đúng):
        CREATE TABLE playlists (
            id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
            user_id     BIGINT       NOT NULL,
            name        VARCHAR(100) NOT NULL,
            description VARCHAR(300),
            is_public   BIT          NOT NULL,
            cover_url   VARCHAR(255),
            created_at  DATETIME(6)  NOT NULL,
            updated_at  DATETIME(6)  NOT NULL,
            FOREIGN KEY (user_id) REFERENCES users (id)
        );
    Bảng có thể do Hibernate tự tạo lúc ứng dụng khởi động (khi cấu hình spring.jpa.hibernate.ddl-auto=update),
    hoặc tạo bằng tay với câu SQL như trên. Dù cách nào, tên cột trong bảng phải khớp với entity.

    =====================================================================
    ẢNH BÌA
    =====================================================================

    Cột cover_url chỉ lưu ảnh người tạo tự tải lên.
    - Có ảnh riêng       → hiển thị ảnh đó.
    - Không có ảnh riêng → hiển thị ảnh bìa của bài đầu tiên trong playlist
                           (bài còn nghe được và có ảnh bìa).

    Ảnh "tự động" không lưu vào cột này mà được tính mỗi lần trả kết quả (trong PlaylistService).
    Nhờ vậy khi bài đầu tiên bị bớt khỏi playlist, hoặc bị ẩn, thì ảnh đại diện tự đổi theo,
    không phải cập nhật lại database.

    =====================================================================
    DANH SÁCH BÀI HÁT NẰM Ở ĐÂU?
    =====================================================================

    Không nằm trong bảng này mà nằm ở bảng playlist_songs (entity PlaylistSong).

    Class này cố ý không khai báo danh sách bài (List<PlaylistSong>), để đơn giản:
    - Muốn lấy bài thì hỏi PlaylistSongRepository.
    - Khi chỉ cần tên playlist (VD trang "Playlist của tôi"), Hibernate không phải tải kèm cả danh sách bài.

    -----------------------------------------------------
    @Entity và @Table
    -----------------------------------------------------
    - @Entity: đánh dấu class này là entity → Hibernate quản lý, lưu được xuống database.
      Thiếu @Entity thì Playlist chỉ là class Java bình thường, gọi save(...) sẽ lỗi.
    - @Table(name = "playlists"): tên bảng trong MySQL.
      Không ghi thì Hibernate tự đặt tên bảng theo tên class (playlist, không có chữ s).

    Class này không tự viết constructor nào → Java tự tạo sẵn 1 constructor rỗng: new Playlist().
    Hibernate cần đúng constructor rỗng này để tạo object mỗi khi đọc 1 dòng từ database.
*/
@Entity
@Table(name = "playlists")
public class Playlist {
    /*
        -----------------------------------------------------
        id: khóa chính, MySQL tự tăng
        -----------------------------------------------------
        - @Id: thuộc tính này là khóa chính (primary key): mỗi dòng có 1 id riêng, không bao giờ trùng.
        - @GeneratedValue(strategy = GenerationType.IDENTITY): mình không tự đặt id,
          mà để MySQL tự tăng (AUTO_INCREMENT): playlist đầu tiên id 1, tiếp theo id 2...
          Vì vậy lúc mới new Playlist(), id đang là null.
          Lưu xong, Hibernate lấy id MySQL vừa cấp gán ngược vào object.
        - Long (L hoa) chứ không phải long: Long cho phép null (chưa lưu thì chưa có id),
          long (l thường) là kiểu số nguyên thủy, không chứa được null.

        Đặt @Id trên thuộc tính (không đặt trên getter) → Hibernate đọc, ghi thẳng vào thuộc tính
        (gọi là field access), không đi qua getter, setter.
        Nhờ vậy id, createdAt, updatedAt không có setter mà Hibernate vẫn gán được.
    */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
        -----------------------------------------------------
        owner: người tạo playlist
        -----------------------------------------------------
        Quan hệ nhiều - một: 1 tài khoản có nhiều playlist (giống OtpVerification.user),
        mỗi playlist chỉ thuộc về 1 tài khoản.

            users                 playlists
            ┌────┐  1     nhiều   ┌────┬─────────┐
            │ id │◄───────────────│ id │ user_id │
            └────┘                └────┴─────────┘

        - @ManyToOne: "nhiều playlist - một User".
          Trong Java, thuộc tính có kiểu User (cả 1 object tài khoản),
          nhưng trong MySQL chỉ lưu id của tài khoản vào cột user_id (khóa ngoại).
          Hibernate tự đổi qua lại: lưu thì lấy owner.getId() ghi vào user_id,
          đọc thì dùng user_id để tìm ra User.
        - @JoinColumn(name = "user_id", nullable = false): tên cột khóa ngoại là user_id, bắt buộc có giá trị
          (không có playlist nào "vô chủ").
        - Tên thuộc tính trong Java là owner (chủ sở hữu) cho dễ hiểu,
          còn tên cột trong database vẫn là user_id (@JoinColumn(name = "user_id")).
          Tên thuộc tính và tên cột không bắt buộc giống nhau.

        - fetch = FetchType.LAZY ("lười"): đọc playlist chưa đọc kèm người tạo.
          Hibernate đặt vào owner 1 object "đại diện" (gọi là proxy), mới chỉ biết id.
          Gọi playlist.getOwner().getId() → có ngay, không cần hỏi database.
          Gọi playlist.getOwner().getUsername() lần đầu → lúc đó Hibernate mới chạy thêm 1 câu SELECT vào bảng users.
          Ngược lại với LAZY là EAGER: lúc nào đọc playlist cũng đọc luôn người tạo, kể cả khi không cần.
          Mặc định của @ManyToOne là EAGER, nên phải ghi rõ LAZY.

          Khi cần cả người tạo (VD hiện tên chủ playlist), PlaylistRepository dùng
          "JOIN FETCH p.owner" để lấy cả 2 trong 1 câu SQL,
          thay vì 1 câu cho playlist rồi thêm 1 câu cho mỗi người tạo.

        Chỉ owner được sửa, xóa, thêm bớt bài (PlaylistService kiểm tra).
    */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    /*
        -----------------------------------------------------
        name và description
        -----------------------------------------------------
        name: tên playlist, VARCHAR(100), bắt buộc.
        description: mô tả, VARCHAR(300), có thể NULL.
        DTO tạo và sửa playlist nên có @Size khớp 2 con số này (giống cách dto.user khớp với User).

        @Column dùng để mô tả cột:
        - nullable = false: cột NOT NULL, không được để trống.
          Không ghi → mặc định nullable = true (được để trống), như description.
        - length = 100: cột VARCHAR(100), tối đa 100 ký tự.
          Không ghi → mặc định 255.
        - Không ghi name → tên cột lấy theo tên thuộc tính (name → cột name).
    */
    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 300)
    private String description;

    /*
        -----------------------------------------------------
        isPublic: playlist công khai hay riêng tư
        -----------------------------------------------------
        - true : người khác xem được (trên trang cá nhân của chủ playlist).
        - false: chỉ chủ playlist xem được. Mặc định khi tạo (= false).
        - name = "is_public": cột trong MySQL; boolean thành 0 hoặc 1.

        Vì sao thuộc tính tên isPublic mà không phải public?
        public là từ khóa của Java (public class, public void...), không đặt làm tên biến được.

        Cặp getter, setter theo quy ước của boolean: isPublic() và setPublic(...).
        Hibernate dùng field access (xem phần id ở trên) nên tên thuộc tính trong câu JPQL là isPublic:
            ... WHERE p.isPublic = true   (xem PlaylistRepository.findPublicPlaylists)

        boolean (b thường) chứ không phải Boolean: playlist lúc nào cũng phải là công khai hoặc riêng tư,
        không có trạng thái "chưa biết" (null).
        "= false" ở cuối: giá trị ban đầu khi new Playlist().
    */
    @Column(name = "is_public", nullable = false)
    private boolean isPublic = false;

    /*
        coverUrl: đường dẫn ảnh bìa người tạo tự tải lên, NULL nếu chưa có
        (xem phần "Ảnh bìa" ở trên).
        VD giá trị: "/uploads/playlists/7c1e2b9a-....jpg"
        (file thật nằm trên ổ đĩa của server, database chỉ lưu đường dẫn, không lưu cả bức ảnh).
    */
    @Column(name = "cover_url", length = 255)
    private String coverUrl;

    /*
        -----------------------------------------------------
        createdAt và updatedAt
        -----------------------------------------------------
        createdAt: thời điểm tạo. updatable = false → chỉ ghi 1 lần lúc INSERT,
                   các câu UPDATE sau này không đụng tới cột này (xem Song).
        updatedAt: thời điểm sửa gần nhất.
        Cả 2 do onCreate(), onUpdate(), markUpdated() bên dưới tự gán, không có setter
        → không ai sửa tay được.
    */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /*
        -----------------------------------------------------
        @PrePersist và @PreUpdate: hàm Hibernate tự gọi
        -----------------------------------------------------
        @PrePersist: chạy trước INSERT (lần đầu lưu playlist) → gán ngày tạo và ngày sửa.
        @PreUpdate : chạy trước UPDATE (mỗi lần lưu thay đổi) → gán lại ngày sửa.
        (Giải thích chi tiết: xem Song.)

        Mình không bao giờ tự gọi 2 hàm này. Hibernate tự gọi đúng lúc,
        nên không thể quên gán ngày giờ.
        protected: code bên ngoài (Service) không gọi được, Hibernate vẫn gọi được.
    */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /*
        -----------------------------------------------------
        markUpdated(): đánh dấu playlist vừa thay đổi
        -----------------------------------------------------
        Dùng khi thêm hoặc bớt bài.

        Vì sao cần hàm này?
        Thêm bài = thêm 1 dòng vào bảng playlist_songs, không đổi cột nào của bảng playlists.
        Hibernate chỉ chạy UPDATE (và gọi @PreUpdate) khi thấy đối tượng có thay đổi
        → nếu không làm gì, updatedAt của playlist vẫn là giờ cũ.

        markUpdated() tự đổi updatedAt → Hibernate thấy playlist có thay đổi → chạy UPDATE khi lưu.
        Khác onUpdate(): hàm này public, Service gọi chủ động; onUpdate() chỉ Hibernate tự gọi.
    */
    public void markUpdated() {
        updatedAt = LocalDateTime.now();
    }

    /*
        -----------------------------------------------------
        Getter và setter
        -----------------------------------------------------
        - Getter (getXxx, isXxx): hàm đọc giá trị. Setter (setXxx): hàm gán giá trị.
        - Các thuộc tính đều private: bên ngoài không đọc, ghi trực tiếp được,
          chỉ đi qua getter, setter → class tự quyết định cái gì được đổi (gọi là đóng gói).

        Có setter: owner, name, description, isPublic, coverUrl (những gì được phép đổi).
        Không có setter: id (MySQL tự cấp), createdAt, updatedAt (tự gán ở các hàm trên).
    */
    public Long getId() { return id; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}