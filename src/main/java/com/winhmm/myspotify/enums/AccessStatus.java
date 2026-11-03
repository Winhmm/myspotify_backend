package com.winhmm.myspotify.enums;

/*
    =====================================================================
    ACCESSSTATUS - QUYỀN VÀO WEB
    =====================================================================

    (Enum là gì, cách lưu vào MySQL: xem giải thích chi tiết trong Role.
     Thứ tự đọc package enums: xem Role.)

    Lưu ở cột users.access_status.

    Web chỉ dành cho những người được chủ dự án cho phép
    (tránh người lạ dùng, vì liên quan bản quyền nhạc).
    Đây là "lớp cửa" thứ hai, độc lập với AccountStatus:
        AccountStatus: tài khoản đã xác thực, có bị khóa, bị xóa không (hệ thống tự quản lý).
        AccessStatus : chủ dự án có cho người này vào web không (chủ dự án tự quyết).
    Đăng nhập được khi cả 2 đều đạt: ACTIVE và ACCEPT.

    -----------------------------------------------------
    Chỉ chủ dự án đổi được, bằng SQL
    -----------------------------------------------------
    Không có API nào đổi giá trị này, kể cả API của Admin (Admin cũng phải ACCEPT mới vào được).
        Cho phép  : UPDATE users SET access_status = 'ACCEPT' WHERE email = 'nam@gmail.com';
        Thu hồi   : UPDATE users SET access_status = 'REFUSE' WHERE email = 'nam@gmail.com';
    Thu hồi có tác dụng ngay từ request tiếp theo:
    CustomUserDetailsService coi tài khoản REFUSE là "disabled" → 401.

    Không trả về frontend (UserProfileResponse không có trường này):
    đây là thông tin quản trị nội bộ, frontend không cần biết.
*/
public enum AccessStatus {
    /*
        ACCEPT: được đăng nhập và dùng web (nếu tài khoản cũng đang ACTIVE).
    */
    ACCEPT,

    /*
        REFUSE: không đăng nhập được. Mặc định của mọi tài khoản mới đăng ký
        (gán trong entity User và AuthService.register).
        Vẫn đăng ký, nhận và nhập OTP bình thường, chỉ bị chặn ở bước đăng nhập:
        400 "Account has not been approved yet".
    */
    REFUSE
}