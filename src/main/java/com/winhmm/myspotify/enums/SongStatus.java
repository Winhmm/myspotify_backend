package com.winhmm.myspotify.enums;

/*
    =====================================================================
    SONGSTATUS - TRẠNG THÁI BÀI HÁT
    =====================================================================

    (Enum là gì, cách lưu vào MySQL và đổi sang JSON: xem giải thích chi tiết trong Role.
     Thứ tự đọc package enums: xem Role.)

    Lưu ở cột songs.status (có index cùng release_at, xem entity Song).
    Trả về cho Artist và Admin trong SongManageResponse ("status").
    Admin lọc được theo trạng thái: GET /api/admin/songs?status=PENDING
    (gửi giá trị không có trong enum, VD ?status=ABC → 400 "Invalid parameter: status").

    =====================================================================
    VÒNG ĐỜI CỦA 1 BÀI HÁT
    =====================================================================

        (Artist đăng bài) ─────────────────────────► PENDING
        PENDING   ──(Admin duyệt)──────────────────► APPROVED
        PENDING   ──(Admin từ chối, kèm lý do)─────► REJECTED
        REJECTED  ──(Artist sửa bài)───────────────► PENDING   (tự gửi duyệt lại)
        APPROVED  ──(Artist ẩn)────────────────────► HIDDEN
        HIDDEN    ──(Artist hiện lại)──────────────► APPROVED
        APPROVED  ──(Admin gỡ, kèm lý do)──────────► REJECTED
        HIDDEN    ──(Admin gỡ, kèm lý do)──────────► REJECTED
        (mọi trạng thái trừ DELETED) ──(Artist xóa)► DELETED   (không khôi phục)

    Ai đổi, ở đâu:
        → PENDING             : ArtistSongService.uploadSong (đăng bài mới)
        PENDING  → APPROVED   : AdminSongService.approveSong
                                (Artist phải còn ACTIVE và role ARTIST;
                                 chưa đặt lịch thì releaseAt = lúc duyệt → phát hành ngay)
        PENDING  → REJECTED   : AdminSongService.rejectSong (bắt buộc có lý do)
        APPROVED, HIDDEN → REJECTED : AdminSongService.removeSong (gỡ bài, bắt buộc có lý do)
        REJECTED → PENDING    : ArtistSongService.resubmitIfRejected, tự chạy khi Artist sửa bài
                                (đổi tên, feat, ảnh bìa, video Canvas), lý do cũ bị xóa
        APPROVED → HIDDEN     : ArtistSongService.hideSong
        HIDDEN   → APPROVED   : ArtistSongService.showSong
        bất kỳ   → DELETED    : ArtistSongService.deleteSong (xóa luôn file nhạc, ảnh, video trên ổ đĩa)

    Vì sao "gỡ bài" (UC36) dùng lại REJECTED, không có trạng thái REMOVED riêng?
    Với Artist, bị gỡ hay bị từ chối đều cùng 1 ý: "bài có vấn đề, đọc lý do rồi sửa".
    Sửa xong bài tự quay về PENDING chờ duyệt lại, dùng chung 1 quy trình.

    =====================================================================
    "ĐÃ PHÁT HÀNH" KHÔNG PHẢI 1 TRẠNG THÁI
    =====================================================================

    Không có giá trị RELEASED hay SCHEDULED trong enum. "Đã phát hành" được tính ra mỗi lần đọc:
        status = APPROVED  và  releaseAt <= bây giờ
        (với người nghe còn thêm: Artist chính đang ACTIVE và vẫn có role ARTIST)
    - APPROVED + releaseAt ở tương lai → "Đã lên lịch", tới giờ là tự phát hành.
    - SongManageResponse có trường released (true hoặc false) tính sẵn cho frontend.

    Vì sao không lưu thành 1 trạng thái riêng?
    Nếu lưu SCHEDULED rồi chờ tới giờ đổi thành RELEASED, phải có 1 tác vụ chạy ngầm
    liên tục kiểm tra đồng hồ để cập nhật database. Tính ra lúc đọc thì không cần gì thêm,
    và không bao giờ bị trễ.

    =====================================================================
    AI THẤY BÀI Ở TRẠNG THÁI NÀO?
    =====================================================================

        Người nghe          : chỉ bài đã phát hành (xem điều kiện ở trên).
        Artist (bài của mình): mọi trạng thái trừ DELETED ("Bài hát của tôi").
        Admin               : mặc định mọi trạng thái trừ DELETED;
                              lọc ?status=DELETED mới thấy bài đã xóa.
*/
public enum SongStatus {
    /*
        PENDING: chờ Admin duyệt. Mặc định khi đăng bài (xem entity Song).
        Người nghe chưa thấy. Admin duyệt (APPROVED) hoặc từ chối (REJECTED).
    */
    PENDING,

    /*
        APPROVED: đã được duyệt.
        Tới releaseAt thì người nghe thấy. Artist ẩn được (HIDDEN), Admin gỡ được (REJECTED).
        Đã phát hành rồi thì Artist không đổi lịch được nữa.
    */
    APPROVED,

    /*
        REJECTED: bị Admin từ chối, hoặc bị gỡ sau khi đã duyệt.
        Luôn kèm lý do (cột reject_reason) để Artist biết phải sửa gì.
        Artist sửa bài → tự về PENDING.
    */
    REJECTED,

    /*
        HIDDEN: Artist tự ẩn bài đã duyệt. Người nghe không thấy.
        Artist hiện lại được bất cứ lúc nào (về APPROVED), không cần Admin duyệt lại.
    */
    HIDDEN,

    /*
        DELETED: Artist đã xóa bài (xóa mềm: dòng trong bảng songs vẫn còn, chỉ đổi trạng thái).
        File nhạc, ảnh bìa, video Canvas bị xóa khỏi ổ đĩa → không khôi phục được.
        Vì sao giữ lại dòng? Playlist, lời bài hát, danh sách feat... trỏ tới songs.id bằng khóa ngoại.
        Các chức năng nghe, sửa, duyệt đều coi bài DELETED như không tồn tại → "Song does not exist"
        (chỉ danh sách của Admin, khi lọc ?status=DELETED, mới còn thấy).
    */
    DELETED
}