package com.winhmm.myspotify.entity;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    (Entity là gì, các annotation cơ bản: xem User. @ManyToOne và LAZY: xem OtpVerification.
     @ManyToMany: xem Song. Thứ tự đọc package entity: xem User.
     Các khái niệm @Entity, @Id, @Column, @ManyToOne, LAZY đã giải thích lại trong Playlist, nên đọc Playlist trước.)

    - jakarta.persistence.*: mượn tất cả annotation của JPA.
    - LocalDateTime        : kiểu ngày giờ của Java, dùng cho addedAt.
*/
import jakarta.persistence.*;

import java.time.LocalDateTime;

/*
    =====================================================================
    PLAYLISTSONG - ENTITY ÁNH XẠ BẢNG playlist_songs (MYSQL)
    =====================================================================

    Mỗi dòng của bảng = 1 bài hát nằm trong 1 playlist.

    Các cột:
        id          : khóa chính, tự tăng
        playlist_id : playlist chứa bài (khóa ngoại trỏ tới playlists.id)
        song_id     : bài hát (khóa ngoại trỏ tới songs.id)
        added_at    : thời điểm bài được thêm vào playlist

    Ví dụ playlist 3 có 2 bài:
        ┌────┬─────────────┬─────────┬─────────────────────┐
        │ id │ playlist_id │ song_id │ added_at            │
        ├────┼─────────────┼─────────┼─────────────────────┤
        │ 10 │ 3           │ 15      │ 2026-10-27 22:31:00 │
        │ 11 │ 3           │ 7       │ 2026-10-27 22:35:00 │
        └────┴─────────────┴─────────┴─────────────────────┘

    Thứ tự bài trong playlist = thứ tự thêm vào: sắp xếp theo added_at, rồi tới id
    (2 bài thêm cùng 1 thời điểm thì bài có id nhỏ hơn đứng trước).

    Bảng tương đương trong MySQL (gần đúng):
        CREATE TABLE playlist_songs (
            id          BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
            playlist_id BIGINT      NOT NULL,
            song_id     BIGINT      NOT NULL,
            added_at    DATETIME(6) NOT NULL,
            CONSTRAINT uk_playlist_songs_playlist_song UNIQUE (playlist_id, song_id),
            FOREIGN KEY (playlist_id) REFERENCES playlists (id),
            FOREIGN KEY (song_id) REFERENCES songs (id)
        );

    =====================================================================
    VÌ SAO KHÔNG DÙNG @ManyToMany NHƯ BẢNG song_featured_artists?
    =====================================================================

    Playlist và bài hát là quan hệ nhiều - nhiều:
    1 playlist có nhiều bài, 1 bài nằm trong nhiều playlist.

    MySQL không lưu thẳng được quan hệ nhiều - nhiều, phải dùng 1 bảng ở giữa (bảng trung gian),
    mỗi dòng ghép 1 playlist với 1 bài.

    @ManyToMany chỉ tạo được bảng trung gian có đúng 2 cột (playlist_id, song_id).
    Ở đây cần thêm cột added_at để sắp xếp bài theo thời gian thêm vào,
    nên phải viết bảng trung gian thành 1 entity riêng, gồm 2 quan hệ @ManyToOne.

        playlists              playlist_songs (entity này)           songs
        ┌────┐   1      nhiều  ┌─────────────┬─────────┬──────────┐  nhiều  1   ┌────┐
        │ id │◄────────────────│ playlist_id │ song_id │ added_at │────────────►│ id │
        └────┘                 └─────────────┴─────────┴──────────┘             └────┘

    =====================================================================
    UNIQUE (playlist_id, song_id)
    =====================================================================

    1 bài chỉ được nằm 1 lần trong 1 playlist.
    - PlaylistService kiểm tra trước, báo lỗi "Song is already in this playlist".
    - Ràng buộc UNIQUE trong database là lớp bảo vệ thứ hai: 2 request thêm cùng 1 bài
      đúng cùng lúc đều qua bước kiểm tra, nhưng database chặn request thứ hai
      → DataIntegrityViolationException → GlobalExceptionHandler trả 409 "Data already exists".
    - Ràng buộc này cũng tự tạo index bắt đầu bằng playlist_id
      → lấy bài của 1 playlist (WHERE playlist_id = ?) rất nhanh.
      (Index giống mục lục của quyển sách: MySQL tra mục lục để tìm thẳng tới các dòng cần,
       không phải đọc lần lượt cả bảng.)

    @UniqueConstraint(name = ..., columnNames = {"playlist_id", "song_id"}):
    - Là UNIQUE trên cặp 2 cột cùng lúc (không phải từng cột riêng lẻ).
      Bài 15 vẫn nằm được trong playlist 3 và playlist 4; chỉ cặp (3, 15) không được lặp lại.
    - columnNames ghi tên cột trong database (playlist_id), không phải tên thuộc tính Java (playlist).
    - name: đặt tên rõ ràng cho ràng buộc, khớp với bảng đã tạo trên VPS.
    - Ghi trong @Table(uniqueConstraints = { ... }) vì ràng buộc này thuộc về cả bảng,
      không thuộc về riêng 1 cột nào.
      Dấu { } : mảng, có thể ghi nhiều ràng buộc, cách nhau bằng dấu phẩy. Ở đây chỉ có 1.

    =====================================================================
    BÀI HÁT BỊ ẨN HOẶC BỊ GỠ THÌ SAO?
    =====================================================================

    Dòng trong bảng này vẫn giữ nguyên. Khi xem playlist, chỉ lấy những bài còn nghe được
    (điều kiện PUBLISHED_CONDITION của SongRepository).
    → Bài bị ẩn tự biến mất khỏi playlist, được mở lại thì tự hiện lại đúng vị trí cũ.

    Lưu ý khi xóa hẳn 1 bài hát bằng SQL: phải xóa các dòng trong bảng này trước
    (DELETE FROM playlist_songs WHERE song_id = ...), nếu không sẽ bị lỗi khóa ngoại.
    (Khóa ngoại song_id đang trỏ tới bài đó, MySQL không cho xóa bài khi vẫn còn dòng trỏ tới nó.)
*/
@Entity
@Table(
        name = "playlist_songs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_playlist_songs_playlist_song", columnNames = {"playlist_id", "song_id"})
        }
)
public class PlaylistSong {
    /*
        id: khóa chính riêng, MySQL tự tăng (giải thích @Id, @GeneratedValue: xem Playlist).
        Dù cặp (playlist_id, song_id) đã không trùng, vẫn dùng 1 cột id đơn giản làm khóa chính:
        Repository chỉ cần JpaRepository<PlaylistSong, Long>, không phải viết class khóa ghép.
    */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
        Quan hệ nhiều - một: 1 playlist có nhiều dòng PlaylistSong.
        Cột playlist_id trỏ tới playlists.id, bắt buộc.
        LAZY: đọc 1 dòng PlaylistSong chưa đọc kèm playlist (giải thích LAZY: xem phần owner trong Playlist).
    */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id", nullable = false)
    private Playlist playlist;

    /*
        Quan hệ nhiều - một: 1 bài hát có thể nằm trong nhiều playlist (nhiều dòng PlaylistSong).
        Cột song_id trỏ tới songs.id, bắt buộc.
        Khi xem playlist, PlaylistSongRepository dùng "JOIN FETCH ps.song" để lấy luôn bài hát
        trong cùng 1 câu SQL (tránh mỗi bài 1 câu SQL riêng).
    */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id", nullable = false)
    private Song song;

    /*
        addedAt: thời điểm thêm bài vào playlist.
        updatable = false: chỉ ghi 1 lần lúc INSERT. Do onCreate() bên dưới tự gán.
    */
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    /*
        @PrePersist: Hibernate tự gọi ngay trước khi INSERT (xem Song, xem Playlist).
        → Mỗi lần thêm 1 bài vào playlist, addedAt tự là giờ hiện tại.
    */
    @PrePersist
    protected void onCreate() {
        addedAt = LocalDateTime.now();
    }

    /*
        -----------------------------------------------------
        2 constructor
        -----------------------------------------------------
        Constructor = hàm đặc biệt, cùng tên với class, chạy khi gọi new PlaylistSong(...),
        dùng để tạo object và gán giá trị ban đầu.

        1. protected PlaylistSong() {}: constructor rỗng, dành cho Hibernate.
           Hibernate bắt buộc phải có constructor không tham số để tạo đối tượng khi đọc từ database.
           Vì class đã tự viết constructor có tham số (số 2), Java không tự tạo constructor rỗng nữa
           → phải tự viết. Để protected: Hibernate dùng được, còn code của mình
           (ở package khác như service) không gọi được new PlaylistSong() rỗng.

        2. public PlaylistSong(Playlist playlist, Song song): constructor để code dùng:
               playlistSongRepository.save(new PlaylistSong(playlist, song));
           Bắt buộc truyền đủ playlist và bài hát → không thể tạo ra 1 dòng thiếu thông tin.
           Không cần truyền id (MySQL cấp) và addedAt (onCreate() gán).
           this.playlist = playlist: this.playlist là thuộc tính của object,
           playlist (không có this) là tham số vừa truyền vào. Cùng tên nên phải có this để phân biệt.

        Vì sao không có setter?
        1 dòng PlaylistSong không bao giờ bị sửa: muốn "đổi" thì xóa dòng cũ, thêm dòng mới.
        Bớt bài khỏi playlist = xóa dòng (playlistSongRepository.delete(...)).
    */
    protected PlaylistSong() {}

    public PlaylistSong(Playlist playlist, Song song) {
        this.playlist = playlist;
        this.song = song;
    }

    /*
        Getter: Service đọc bài hát (getSong()), thời điểm thêm (getAddedAt())...
    */
    public Long getId() { return id; }
    public Playlist getPlaylist() { return playlist; }
    public Song getSong() { return song; }
    public LocalDateTime getAddedAt() { return addedAt; }
}