package com.winhmm.myspotify.enums;

/*
    ===================================================================
    SongStatus - Các TRẠNG THÁI của 1 bài hát.
    ===================================================================

    ---------- ENUM LÀ GÌ? ----------

    Enum = kiểu dữ liệu chỉ nhận MỘT TRONG MỘT SỐ GIÁ TRỊ CỐ ĐỊNH.
    So với dùng String "PENDING":
    - Gõ sai tên (VD SongStatus.PENDNG) → compiler báo lỗi NGAY, không đợi chạy mới phát hiện.
    - IntelliJ gợi ý đủ các giá trị khi gõ SongStatus.
    - Không thể gán giá trị lạ như "ABC".

    ---------- VÒNG ĐỜI 1 BÀI HÁT ----------

        Artist đăng bài ──► PENDING ──Admin duyệt──► APPROVED ──tới giờ──► người nghe thấy
                              │                         │    ▲
                        Admin từ chối            Artist ẩn  │ Artist hiện lại
                              ▼                         ▼    │
                          REJECTED ◄──Admin gỡ bài─── HIDDEN
                              │                         ▲
                              │      (Admin gỡ bài APPROVED cũng về REJECTED)
                      Artist sửa bài ──► PENDING (tự gửi duyệt lại)

        Artist xóa bài (ở bất kỳ trạng thái nào) ──► DELETED

    ---------- AI ĐƯỢC CHUYỂN TRẠNG THÁI NÀO? ----------

        Từ          → Sang       Ai làm          Ở đâu
        (bài mới)   → PENDING    Artist đăng     ArtistSongService.uploadSong
        PENDING     → APPROVED   Admin duyệt     AdminSongService.approveSong
        PENDING     → REJECTED   Admin từ chối   AdminSongService.rejectSong
        APPROVED    → HIDDEN     Artist ẩn       ArtistSongService.hideSong
        HIDDEN      → APPROVED   Artist hiện     ArtistSongService.showSong
        APPROVED    → REJECTED   Admin gỡ        AdminSongService.removeSong
        HIDDEN      → REJECTED   Admin gỡ        AdminSongService.removeSong
        REJECTED    → PENDING    Artist sửa bài  ArtistSongService.resubmitIfRejected
        (bất kỳ)    → DELETED    Artist xóa      ArtistSongService.deleteSong

    Mọi chuyển đổi không có trong bảng → Service ném lỗi 400.
    VD: duyệt bài đã APPROVED → "Only pending songs can be approved".

    ---------- LƯU Ý KHI SỬA ENUM NÀY ----------

    Entity Song dùng @Enumerated(EnumType.STRING) → database lưu TÊN, VD chữ "APPROVED".
    - Đổi thứ tự các giá trị: AN TOÀN.
    - Thêm giá trị mới: AN TOÀN (cột status là VARCHAR(20), tên mới phải ≤ 20 ký tự).
    - ĐỔI TÊN giá trị (VD APPROVED → PUBLISHED): NGUY HIỂM.
      Dữ liệu cũ trong database vẫn là "APPROVED" → Hibernate không đọc được → lỗi.
      Muốn đổi tên phải chạy thêm SQL: UPDATE songs SET status = 'PUBLISHED' WHERE status = 'APPROVED';

    Frontend nhận các giá trị này dạng chuỗi trong JSON ("status": "PENDING"),
    và Admin lọc bằng ?status=PENDING (phải viết hoa đúng tên).
*/
public enum SongStatus {
    /*
        Chờ Admin duyệt. Trạng thái mặc định của mọi bài mới đăng.
        Người nghe KHÔNG thấy. Artist vẫn sửa, đổi ảnh, đổi lịch, xóa được.
    */
    PENDING,

    /*
        Đã được Admin duyệt.
        - releaseAt <= hiện tại → người nghe thấy ("Đã phát hành").
        - releaseAt  > hiện tại → chưa thấy, tự hiện khi tới giờ ("Đã lên lịch").
        Artist có thể ẩn bài, Admin có thể gỡ bài.
    */
    APPROVED,

    /*
        Bị Admin từ chối (khi đang chờ duyệt) hoặc bị gỡ (khi đã duyệt).
        Luôn kèm rejectReason để Artist biết lý do.
        Người nghe KHÔNG thấy. Artist sửa bài (tên, feat, ảnh bìa) → tự về PENDING.
    */
    REJECTED,

    /*
        Artist tự ẩn bài đã được duyệt.
        Người nghe KHÔNG thấy, nhưng bài giữ nguyên lượt nghe, file, lịch phát hành.
        Artist hiện lại → về APPROVED ngay, KHÔNG cần Admin duyệt lại.
        Admin vẫn gỡ được bài đang ẩn (chuyển sang REJECTED).
    */
    HIDDEN,

    /*
        Artist đã xóa bài (XÓA MỀM).
        - Bản ghi vẫn còn trong database (giữ lịch sử), nhưng file nhạc và ảnh bìa
          đã bị xóa hẳn khỏi ổ đĩa.
        - Mọi thao tác của Artist / Admin coi như bài "không tồn tại".
        - Không thể khôi phục (vì file đã mất).
        - Admin vẫn xem được trong danh sách khi lọc ?status=DELETED.
    */
    DELETED
}