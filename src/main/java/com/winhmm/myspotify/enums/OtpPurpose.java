package com.winhmm.myspotify.enums;

/*
    =====================================================================
    OTPPURPOSE - MỤC ĐÍCH CỦA MÃ OTP
    =====================================================================

    (Enum là gì, cách lưu vào MySQL: xem giải thích chi tiết trong Role.
     Thứ tự đọc package enums: xem Role.)

    Lưu ở cột otp_verifications.purpose (xem entity OtpVerification).
    Mỗi mã OTP gửi đi đều gắn với đúng 1 mục đích.

    =====================================================================
    4 MỤC ĐÍCH, MỖI MỤC ĐÍCH 1 QUY TRÌNH 2 BƯỚC
    =====================================================================

        Mục đích          Bước 1: xin mã                          Bước 2: nhập mã
        REGISTER          AuthService.register                    AuthService.verifyOtp
        RESET_PASSWORD    AuthService.forgotPassword              AuthService.resetPassword
        CHANGE_PASSWORD   UserService.requestChangePasswordOtp    UserService.changePassword
        CHANGE_EMAIL      UserService.requestUpdateEmailOtp       UserService.updateEmail

    Mã luôn gửi về email hiện tại của tài khoản, trừ CHANGE_EMAIL gửi tới email mới.

    =====================================================================
    VÌ SAO PHẢI PHÂN BIỆT MỤC ĐÍCH?
    =====================================================================

    1. An toàn: mã của mục đích này không dùng được cho mục đích khác.
       VD kẻ gian có được mã xác thực đăng ký (REGISTER) cũng không dùng nó để đặt lại mật khẩu được,
       vì bước 2 chỉ tìm mã có đúng mục đích RESET_PASSWORD.
    2. Không đè lên nhau: mỗi mục đích có "mã mới nhất" riêng.
       Xin mã đổi mật khẩu không làm mất hiệu lực mã đổi email đang chờ nhập, và ngược lại.
       Thời gian chờ 60 giây giữa 2 lần xin mã cũng tính riêng theo từng mục đích.

    OtpService tìm mã theo cả tài khoản và mục đích:
        findFirstByUserAndPurposeAndVerifiedFalseOrderByIdDesc(user, purpose)
        → "mã mới nhất, chưa hết hiệu lực, của tài khoản này, cho mục đích này".

    Frontend không bao giờ gửi mục đích lên: mỗi API đã tự biết mình dùng mục đích nào
    (VD API đổi email luôn dùng CHANGE_EMAIL). Enum này chỉ dùng bên trong backend, không có trong JSON.
*/
public enum OtpPurpose {
    /*
        REGISTER: xác thực email khi đăng ký.
        Nhập đúng → tài khoản UNVERIFIED thành ACTIVE.
    */
    REGISTER,

    /*
        RESET_PASSWORD: quên mật khẩu (chưa đăng nhập).
        Nhập đúng kèm mật khẩu mới → đặt lại mật khẩu, đăng xuất khỏi mọi thiết bị.
    */
    RESET_PASSWORD,

    /*
        CHANGE_PASSWORD: đổi mật khẩu khi đang đăng nhập.
        Chỉ được xin mã sau khi nhập đúng mật khẩu hiện tại (bảo vệ 2 lớp).
    */
    CHANGE_PASSWORD,

    /*
        CHANGE_EMAIL: đổi email.
        Mã gửi tới email mới; email mới được giữ tạm ở cột new_email của mã OTP,
        nhập đúng mã mới chép sang bảng users.
    */
    CHANGE_EMAIL
}