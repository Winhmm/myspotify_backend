package com.winhmm.myspotify.exception;

/*
    =====================================================
    EMAILSENDEXCEPTION - LỖI GỬI EMAIL
    =====================================================

    Được ném ra khi EmailService không gửi được email OTP
    (Gmail lỗi, sai mật khẩu ứng dụng, mất kết nối, quá thời gian chờ...).

    -----------------------------------------------------
    Vì sao phải kế thừa RuntimeException?
    -----------------------------------------------------
    Mặc định, @Transactional chỉ rollback khi gặp RuntimeException (hoặc Error).
    Nếu dùng Exception thường (checked exception), transaction vẫn commit
    → tài khoản và mã OTP vẫn được lưu dù email không gửi được.

    Kế thừa RuntimeException thì khi gửi email lỗi:
    - Đăng ký: tài khoản vừa tạo bị hủy → người dùng đăng ký lại được ngay với cùng email.
    - Gửi lại OTP: mã OTP vừa tạo bị hủy → không bị tính thời gian chờ 60 giây.

    -----------------------------------------------------
    Vì sao cần class riêng?
    -----------------------------------------------------
    Để GlobalExceptionHandler nhận ra đúng loại lỗi này
    và trả mã 503 kèm message rõ ràng, thay vì lỗi 500 chung chung.
*/
public class EmailSendException extends RuntimeException {

    /*
        message: nội dung lỗi trả về cho frontend.
        cause: lỗi gốc từ JavaMail, giữ lại để ghi log khi cần kiểm tra.
    */
    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
