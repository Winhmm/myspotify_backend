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
    =====================================================================
    AdminSongService - NGHIỆP VỤ CỦA ADMIN VỚI BÀI HÁT (UC34 - UC36)
    =====================================================================

    Các chức năng:
        getSongs(...)    → danh sách bài, lọc theo trạng thái (UC34)
        approveSong(...) → duyệt bài (UC35)
        rejectSong(...)  → từ chối bài đang chờ duyệt (UC35)
        removeSong(...)  → gỡ bài đã duyệt (UC36)

    Được gọi từ AdminSongController.

    =====================================================================
    SERVICE LÀM GÌ?
    =====================================================================

    Service chứa quy tắc nghiệp vụ: "được làm gì, khi nào, với điều kiện gì".
    VD: "chỉ duyệt được bài đang PENDING", "Artist bị khóa thì không duyệt".

    Kiến trúc 3 lớp:
        Controller : nhận request, trả response, không chứa if/else nghiệp vụ.
        Service    : kiểm tra quy tắc, quyết định thay đổi gì (file này).
        Repository : đọc và ghi database.

    @Service: báo cho Spring tạo 1 đối tượng duy nhất của class này (bean),
    rồi tiêm vào AdminSongController qua constructor.

    =====================================================================
    CÁCH BÁO LỖI
    =====================================================================

    Vi phạm quy tắc → throw new IllegalArgumentException("thông báo")
    → GlobalExceptionHandler bắt lại → trả 400 { "message": "thông báo" }.
    Service không cần biết gì về HTTP, chỉ cần ném lỗi kèm thông báo rõ ràng.

    Ghi trùng với Artist (Artist sửa hoặc xóa bài đúng lúc Admin thao tác)
    → Hibernate ném ObjectOptimisticLockingFailureException (nhờ @Version trong Song)
    → GlobalExceptionHandler trả 409.

    =====================================================================
    VÌ SAO KHÔNG CÓ @Transactional?
    =====================================================================

    Mỗi method chỉ đọc 1 bài rồi lưu 1 bài. Mỗi lần gọi repository
    (findById, save) tự chạy trong 1 transaction riêng do Spring Data quản lý.

    Trường hợp có người khác sửa bài ở giữa lúc đọc và lúc lưu đã được
    @Version (Optimistic Locking) xử lý, nên không cần gom vào 1 transaction lớn.
*/
@Service
public class AdminSongService {
    private static final int MAX_PAGE_SIZE = 50;

    private final SongRepository songRepository;

    public AdminSongService(SongRepository songRepository) {
        this.songRepository = songRepository;
    }

    /*
        ---------------------------------------------------------------
        UC34 - Danh sách bài hát
        ---------------------------------------------------------------

        - status = null     → mọi bài trừ DELETED.
        - status có giá trị → chỉ bài ở trạng thái đó (VD PENDING để duyệt).

        Math.max(page, 0): page âm (VD -5) → 0.
        Math.min(Math.max(size, 1), 50): ép size vào khoảng 1 - 50.

        Trả về Page<Song> (entity), Controller dùng SongMapper chuyển sang DTO.
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

    /*
        ---------------------------------------------------------------
        UC35 - Duyệt bài đang chờ duyệt
        ---------------------------------------------------------------
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
        ---------------------------------------------------------------
        UC35 - Từ chối bài đang chờ duyệt (bắt buộc có lý do)
        ---------------------------------------------------------------

        .trim() bỏ dấu cách thừa ở 2 đầu: "  Ảnh mờ  " → "Ảnh mờ".
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

    /*
        ---------------------------------------------------------------
        UC36 - Gỡ bài đã duyệt (kể cả bài Artist đang ẩn, bắt buộc có lý do)
        ---------------------------------------------------------------

        Điều kiện báo lỗi: bài KHÔNG phải APPROVED và cũng KHÔNG phải HIDDEN.
        Phải dùng && (và), không dùng || (hoặc):
            1 bài chỉ có 1 trạng thái, nên "!= APPROVED || != HIDDEN" luôn đúng
            → bài nào cũng bị báo lỗi, không gỡ được bài nào.
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

    /*
        ---------------------------------------------------------------
        Hàm hỗ trợ: lấy bài theo id cho Admin
        ---------------------------------------------------------------
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