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
    ===================================================================
    AdminSongService - Nghiệp vụ của ADMIN với bài hát.
    ===================================================================

    - Xem danh sách bài, lọc theo trạng thái (UC34).
    - Duyệt bài (UC35).
    - Từ chối bài chờ duyệt (UC35).
    - Gỡ bài đã duyệt (UC36).

    ---------- SERVICE LÀM GÌ? ----------

    Service chứa QUY TẮC NGHIỆP VỤ: "được làm gì, khi nào, với điều kiện gì".
    VD: "chỉ duyệt được bài đang PENDING", "Artist bị khóa thì không duyệt".
    Controller chỉ gọi Service, Repository chỉ đọc / ghi. Mọi if/else nghiệp vụ nằm ở đây.

    @Service: báo cho Spring tạo 1 đối tượng duy nhất của class này (bean),
              rồi tiêm vào AdminSongController qua constructor.

    ---------- CÁCH BÁO LỖI ----------

    Vi phạm quy tắc → throw new IllegalArgumentException("thông báo").
    → GlobalExceptionHandler bắt lại → trả 400 { "message": "thông báo" }.
    Service không cần biết gì về HTTP, chỉ cần ném lỗi kèm thông báo rõ ràng.

    Ghi trùng với Artist (Artist sửa / xóa bài đúng lúc Admin thao tác)
    → Hibernate ném ObjectOptimisticLockingFailureException (nhờ @Version trong Song)
    → GlobalExceptionHandler trả 409.
*/
@Service
public class AdminSongService {
    /*
        Số bài tối đa mỗi trang. static final = hằng số, viết HOA theo quy ước Java.
        Đặt tên thay vì viết số 50 trực tiếp trong code (gọi là tránh "magic number"):
        đọc là hiểu ý nghĩa, muốn đổi chỉ sửa 1 chỗ.
    */
    private static final int MAX_PAGE_SIZE = 50;

    private final SongRepository songRepository;

    public AdminSongService(SongRepository songRepository) {
        this.songRepository = songRepository;
    }

    /* ================= UC34: DANH SÁCH BÀI HÁT ================= */

    /*
        Lấy 1 trang bài hát cho Admin.

        - status = null       → mọi bài trừ DELETED.
        - status có giá trị   → chỉ bài ở trạng thái đó (VD PENDING để duyệt).

        PHÂN TRANG AN TOÀN (không tin tham số frontend gửi lên):
            Math.max(page, 0)                   : page âm (VD -5) → 0.
            Math.min(Math.max(size, 1), 50)     : ép size vào khoảng 1 - 50.
                size = 0      → 1
                size = 20     → 20
                size = 100000 → 50   (chống kéo cả database trong 1 lần)

        PageRequest.of(trang, số bài, cách sắp xếp):
            Sort.by(Sort.Direction.DESC, "createdAt"): mới tải lên trước.
            "createdAt" là tên THUỘC TÍNH Java (không phải tên cột created_at).
            Admin thấy bài vừa gửi lên ở đầu danh sách.
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
        Duyệt bài đang chờ duyệt.

        CÁC BƯỚC:
        1. Lấy bài (không tồn tại / đã xóa → lỗi).
        2. Bài phải đang PENDING.
        3. Artist chính phải còn ACTIVE và còn role ARTIST.
           (Artist bị khóa SAU KHI gửi bài → không duyệt, tránh phát hành bài
            của người đang bị xử lý vi phạm.)
        4. releaseAt:
           - null (Artist muốn phát hành ngay) → gán = thời điểm duyệt.
             BẮT BUỘC: truy vấn người nghe dùng "release_at <= now",
             bài APPROVED mà releaseAt null sẽ KHÔNG BAO GIỜ hiện ra.
           - Có giá trị (Artist đã hẹn lịch) → giữ nguyên.
             Nếu giờ hẹn đã trôi qua trong lúc chờ duyệt → bài hiện ngay sau khi duyệt.
        5. Xóa lý do từ chối cũ (bài từng bị từ chối rồi Artist sửa và gửi lại).
        6. Đổi trạng thái → APPROVED và lưu.

        song.getArtist(): quan hệ LAZY → Hibernate chạy thêm 1 câu SELECT lấy User ở đây.

        songRepository.save(song): bài đã có id → Hibernate chạy UPDATE
            ... WHERE id = ? AND version = ?
        Trả về bài đã lưu để Controller chuyển sang DTO.
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
        Từ chối bài đang chờ duyệt (bắt buộc có lý do).

        reason đã được @NotBlank trong RejectSongRequest đảm bảo không rỗng.
        .trim() bỏ dấu cách thừa ở 2 đầu: "  Ảnh mờ  " → "Ảnh mờ".

        Sau khi bị từ chối: Artist sửa bài (tên, feat, ảnh bìa) → tự về PENDING
        (ArtistSongService.resubmitIfRejected).
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

        Chỉ áp dụng APPROVED hoặc HIDDEN. Vì sao gỡ được cả bài đang ẩn?
        → Nếu không, Artist có thể "né" bị gỡ bằng cách ẩn bài trước,
          đợi Admin bỏ qua rồi hiện lại.

        Bài chuyển sang REJECTED (không phải DELETED):
        - Người nghe không thấy nữa ngay lập tức.
        - Artist thấy lý do, có thể sửa và gửi duyệt lại.
        - File nhạc và lượt nghe vẫn giữ nguyên.
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
        Lấy bài theo id cho Admin.
        - Không tồn tại → lỗi.
        - Artist đã xóa (DELETED) → cũng coi như không tồn tại:
          file nhạc đã bị xóa khỏi ổ đĩa, không được duyệt / gỡ bài này nữa.

        findById trả Optional<Song>:
            .orElseThrow(() -> new IllegalArgumentException(...))
            → có bài thì lấy ra, rỗng thì ném lỗi.
            "() -> ..." là lambda: chỉ tạo đối tượng lỗi KHI THẬT SỰ cần (Optional rỗng).

        private: chỉ dùng nội bộ trong class.
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