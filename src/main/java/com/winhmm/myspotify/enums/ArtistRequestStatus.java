package com.winhmm.myspotify.enums;

/*
    =====================================================================
    ARTISTREQUESTSTATUS - TIẾN TRÌNH XIN LÀM ARTIST
    =====================================================================

    (Enum là gì, cách lưu vào MySQL và đổi sang JSON: xem giải thích chi tiết trong Role.
     Thứ tự đọc package enums: xem Role.)

    Lưu ở cột users.artist_request_status (có index, để Admin lọc nhanh các yêu cầu PENDING).
    Trả về frontend trong UserProfileResponse ("artistRequestStatus").

    =====================================================================
    VÒNG ĐỜI CỦA 1 YÊU CẦU
    =====================================================================

        NONE      ──(USER gửi yêu cầu)──────► PENDING
        PENDING   ──(Admin duyệt)───────────► APPROVED  (role thành ARTIST)
        PENDING   ──(Admin từ chối)─────────► REJECTED  (xóa nghệ danh, bio)
        REJECTED  ──(gửi lại yêu cầu mới)───► PENDING
        PENDING, APPROVED ──(Admin đổi role)► NONE

    Ai đổi, ở đâu:
        NONE, REJECTED → PENDING  : UserService.requestBecomeArtist  (POST /api/users/me/artist-request)
        PENDING        → APPROVED : AdminService.approveArtistRequest (PUT /api/admin/artist-requests/{userId}/approve)
        PENDING        → REJECTED : AdminService.rejectArtistRequest  (PUT /api/admin/artist-requests/{userId}/reject)
        PENDING, APPROVED → NONE  : AdminService.assignRole, khi Admin đổi role của người đang chờ duyệt
                                    hoặc hạ 1 Artist xuống (nghệ danh và bio cũng bị xóa).

    Vì sao cần enum này khi đã có Role?
    Role chỉ cho biết "hiện tại là gì" (USER hay ARTIST). Enum này cho biết "đang ở bước nào":
    1 USER có thể chưa xin (NONE), đang chờ (PENDING) hay vừa bị từ chối (REJECTED).
    Frontend dựa vào đó để hiện nút "Đăng ký làm nghệ sĩ", chữ "Đang chờ duyệt"...
*/
public enum ArtistRequestStatus {
    /*
        NONE: chưa gửi yêu cầu nào. Mặc định khi đăng ký.
        Cũng là trạng thái sau khi Admin đổi role (hạ Artist hoặc đổi role người đang chờ duyệt).
    */
    NONE,

    /*
        PENDING: đã gửi yêu cầu kèm nghệ danh và bio, đang chờ Admin duyệt.
        Admin xem danh sách này ở GET /api/admin/artist-requests.
        Đang PENDING thì không gửi thêm yêu cầu được (tránh gửi trùng).
    */
    PENDING,

    /*
        APPROVED: đã được duyệt, role đã chuyển thành ARTIST.
        Không gửi yêu cầu được nữa (đã là Artist rồi).
    */
    APPROVED,

    /*
        REJECTED: bị Admin từ chối. Nghệ danh và bio đã gửi bị xóa.
        Được gửi lại yêu cầu mới (quay về PENDING).
    */
    REJECTED
}