package com.winhmm.myspotify.enums;

/*
    =====================================================================
    ACCOUNTSTATUS - TÀI KHOẢN CÓ DÙNG ĐƯỢC KHÔNG
    =====================================================================

    (Enum là gì, cách lưu vào MySQL và đổi sang JSON: xem giải thích chi tiết trong Role.
     Thứ tự đọc package enums: xem Role.)

    Lưu ở cột users.account_status. Trả về frontend trong UserProfileResponse ("accountStatus").

    =====================================================================
    VÒNG ĐỜI CỦA 1 TÀI KHOẢN
    =====================================================================

        (đăng ký) ─────────────────────────────────────► UNVERIFIED
        UNVERIFIED ──(nhập đúng OTP)───────────────────► ACTIVE
        ACTIVE     ──(tự vô hiệu hóa, hoặc Admin khóa)─► DISABLED
        DISABLED   ──(Admin mở khóa)───────────────────► ACTIVE
        ACTIVE     ──(tự xóa tài khoản)────────────────► DELETED   (vĩnh viễn)

    Ai đổi, ở đâu:
        → UNVERIFIED : AuthService.register (tạo tài khoản mới)
        UNVERIFIED → ACTIVE   : AuthService.verifyOtp (POST /api/auth/verify-otp)
        ACTIVE     → DISABLED : UserService.disableAccount (PUT /api/users/me/disable)
                                AdminService.lockAccount   (PUT /api/admin/users/{userId}/lock)
        DISABLED   → ACTIVE   : AdminService.unlockAccount (PUT /api/admin/users/{userId}/unlock)
        ACTIVE     → DELETED  : UserService.deleteAccount  (PUT /api/users/me/delete)

    =====================================================================
    ĐĂNG NHẬP THEO TỪNG TRẠNG THÁI (AuthService.login)
    =====================================================================

    Mật khẩu đúng rồi mới xét tới trạng thái:
        UNVERIFIED → 400 "Account is not verified yet"
        DISABLED   → 400 "Account has been disabled"
        DELETED    → 400 "Account has been deleted"
        ACTIVE     → xét tiếp quyền vào web (AccessStatus), ACCEPT mới đăng nhập được.

    Người đang đăng nhập mà tài khoản đổi khỏi ACTIVE (VD bị Admin khóa):
    mỗi request, CustomUserDetailsService đánh dấu tài khoản không ACTIVE là "disabled"
    → request không được xác thực → 401. Ngoài ra phiên trong Redis cũng bị xóa ngay lúc khóa.

    Ảnh hưởng tới bài hát: Artist không ACTIVE thì toàn bộ bài tự ẩn khỏi người nghe
    (điều kiện PUBLISHED_CONDITION trong SongRepository), mở khóa lại thì tự hiện lại.
*/
public enum AccountStatus {
    /*
        UNVERIFIED: đã đăng ký, chưa nhập mã OTP xác thực email. Mặc định khi tạo new User().
        - Không đăng nhập được.
        - Đăng ký lại bằng đúng email này vẫn được (ghi đè username, mật khẩu, gửi OTP mới),
          vì có thể lần trước gõ sai hoặc không nhận được mã.
    */
    UNVERIFIED,

    /*
        ACTIVE: đang hoạt động bình thường. Trạng thái duy nhất được đăng nhập và dùng web.
        Nhiều chức năng kiểm tra ACTIVE trước khi cho làm, VD quên mật khẩu, gửi yêu cầu làm Artist,
        đăng bài (Artist).
    */
    ACTIVE,

    /*
        DISABLED: tạm khóa, có 2 nguồn:
        - Người dùng tự vô hiệu hóa (phải nhập mật khẩu xác nhận, AccountActionRequest).
        - Admin khóa (không khóa được tài khoản ADMIN).
        Muốn dùng lại: Admin mở khóa → ACTIVE. Dữ liệu (bài hát, playlist...) vẫn giữ nguyên.
    */
    DISABLED,

    /*
        DELETED: đã xóa mềm (soft delete): dòng trong bảng users vẫn còn, chỉ đổi trạng thái.
        - Không đăng nhập được, Admin cũng không mở khóa được (chỉ mở khóa DISABLED).
        - Email và username vẫn bị giữ, không đăng ký tài khoản mới bằng chúng được.
        Vì sao không xóa hẳn dòng? Bài hát, mã OTP... đều trỏ tới users.id bằng khóa ngoại,
        xóa hẳn sẽ lỗi khóa ngoại hoặc mất dữ liệu liên quan.
    */
    DELETED
}