package com.winhmm.myspotify.service;

import com.winhmm.myspotify.entity.Song;
import com.winhmm.myspotify.entity.User;
import com.winhmm.myspotify.enums.AccountStatus;
import com.winhmm.myspotify.enums.Role;
import com.winhmm.myspotify.enums.SongStatus;
import com.winhmm.myspotify.repository.SongRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/*
    Các chức năng của ADMIN với bài hát:
    - Xem danh sách bài, lọc theo trạng thái (UC34).
    - Duyệt bài (UC35).
    - Từ chối bài chờ duyệt (UC35).
    - Gỡ bài đã duyệt (UC36).
*/
@Service
public class AdminSongService {
    private static final int MAX_PAGE_SIZE = 50;

    private final SongRepository songRepository;

    public AdminSongService(SongRepository songRepository) {
        this.songRepository = songRepository;
    }

    /* ================= UC34: DANH SÁCH BÀI HÁT ================= */

    /*
        - status = null → mọi bài trừ DELETED.
        - status có giá trị → chỉ bài ở trạng thái đó.
        - Mới nhất lên đầu. page < 0 hoặc size ngoài khoảng 1 - 50 → tự chỉnh lại.
    */
    public Page<Song> getSongs(SongStatus status, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        if(status == null) {
            return songRepository.findByStatusNot(SongStatus.DELETED, pageable);
        }

        return songRepository.findByStatus(status, pageable);
    }

    /* ================= UC35: DUYỆT / TỪ CHỐI ================= */

    /*
        Duyệt bài chờ duyệt:
        - Artist đã bị khóa / thu hồi quyền → không duyệt.
        - releaseAt = null → phát hành ngay (gán = bây giờ).
        - releaseAt có giá trị → giữ lịch Artist đã chọn
          (nếu giờ đó đã qua trong lúc chờ duyệt → bài hiện ngay).
        - Xóa lý do từ chối cũ (nếu có).
    */
    public Song approveSong(Long songId) {
        Song song = getSong(songId);

        if(song.getStatus() != SongStatus.PENDING) {
            throw new IllegalArgumentException("Only pending songs can be approved");
        }

        User artist = song.getArtist();
        if(artist.getAccountStatus() != AccountStatus.ACTIVE || artist.getRole() != Role.ARTIST) {
            throw new IllegalArgumentException("Artist account is not active");
        }

        if(song.getReleaseAt() == null) {
            song.setReleaseAt(LocalDateTime.now());
        }

        song.setRejectReason(null);
        song.setStatus(SongStatus.APPROVED);

        return songRepository.save(song);
    }

    /*
        Từ chối bài chờ duyệt (bắt buộc có lý do).
    */
    public Song rejectSong(Long songId, String reason) {
        Song song = getSong(songId);

        if(song.getStatus() != SongStatus.PENDING) {
            throw new IllegalArgumentException("Only pending songs can be rejected");
        }

        song.setStatus(SongStatus.REJECTED);
        song.setRejectReason(reason.trim());

        return songRepository.save(song);
    }

    /* ================= UC36: GỠ BÀI ================= */

    /*
        Gỡ bài đã duyệt (kể cả bài Artist đang ẩn), bắt buộc có lý do.
        Bài chuyển sang REJECTED → Artist thấy lý do, sửa bài sẽ gửi duyệt lại.
    */
    public Song removeSong(Long songId, String reason) {
        Song song = getSong(songId);

        if(song.getStatus() != SongStatus.APPROVED && song.getStatus() != SongStatus.HIDDEN) {
            throw new IllegalArgumentException("Only approved songs can be removed");
        }

        song.setStatus(SongStatus.REJECTED);
        song.setRejectReason(reason.trim());

        return songRepository.save(song);
    }

    /* ================= HÀM HỖ TRỢ ================= */

    /*
        Bài không tồn tại hoặc đã bị Artist xóa → coi như không tồn tại.
    */
    private Song getSong(Long songId) {
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new IllegalArgumentException("Song does not exist"));

        if(song.getStatus() == SongStatus.DELETED) {
            throw new IllegalArgumentException("Song does not exist");
        }

        return song;
    }
}
