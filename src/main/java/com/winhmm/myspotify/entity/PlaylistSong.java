package com.winhmm.myspotify.entity;

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

    =====================================================================
    VÌ SAO KHÔNG DÙNG @ManyToMany NHƯ BẢNG song_featured_artists?
    =====================================================================

    Playlist và bài hát là quan hệ nhiều - nhiều:
    1 playlist có nhiều bài, 1 bài nằm trong nhiều playlist.

    @ManyToMany chỉ tạo được bảng trung gian có đúng 2 cột (playlist_id, song_id).
    Ở đây cần thêm cột added_at để sắp xếp bài theo thời gian thêm vào,
    nên phải viết bảng trung gian thành 1 entity riêng, gồm 2 quan hệ @ManyToOne.

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

    =====================================================================
    BÀI HÁT BỊ ẨN HOẶC BỊ GỠ THÌ SAO?
    =====================================================================

    Dòng trong bảng này vẫn giữ nguyên. Khi xem playlist, chỉ lấy những bài còn nghe được
    (điều kiện PUBLISHED_CONDITION của SongRepository).
    → Bài bị ẩn tự biến mất khỏi playlist, được mở lại thì tự hiện lại đúng vị trí cũ.

    Lưu ý khi xóa hẳn 1 bài hát bằng SQL: phải xóa các dòng trong bảng này trước
    (DELETE FROM playlist_songs WHERE song_id = ...), nếu không sẽ bị lỗi khóa ngoại.
*/
@Entity
@Table(
        name = "playlist_songs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_playlist_songs_playlist_song", columnNames = {"playlist_id", "song_id"})
        }
)
public class PlaylistSong {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
        Quan hệ nhiều - một: 1 playlist có nhiều dòng PlaylistSong.
    */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id", nullable = false)
    private Playlist playlist;

    /*
        Quan hệ nhiều - một: 1 bài hát có thể nằm trong nhiều playlist (nhiều dòng PlaylistSong).
    */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id", nullable = false)
    private Song song;

    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    @PrePersist
    protected void onCreate() {
        addedAt = LocalDateTime.now();
    }

    protected PlaylistSong() {}

    public PlaylistSong(Playlist playlist, Song song) {
        this.playlist = playlist;
        this.song = song;
    }

    public Long getId() { return id; }
    public Playlist getPlaylist() { return playlist; }
    public Song getSong() { return song; }
    public LocalDateTime getAddedAt() { return addedAt; }
}
