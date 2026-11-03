package com.winhmm.myspotify.service;

import com.winhmm.myspotify.exception.EmailSendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/*
    =====================================================
    EMAILSERVICE - GỬI EMAIL OTP
    =====================================================

    Gửi mã OTP tới email người dùng cho các chức năng:
    - Đăng ký tài khoản (REGISTER).
    - Quên mật khẩu (RESET_PASSWORD).
    - Đổi mật khẩu (CHANGE_PASSWORD).
    - Đổi email (CHANGE_EMAIL), mã được gửi tới email mới.

    Được gọi từ OtpService.createAndSend(), sau khi đã lưu mã OTP vào database.

    -----------------------------------------------------
    Vì sao gửi đồng bộ?
    -----------------------------------------------------
    Gửi đồng bộ nghĩa là API chờ Gmail gửi xong mới trả response.
    - Ưu điểm: gửi lỗi thì báo lỗi ngay cho người dùng, và hủy (rollback)
      những gì vừa lưu vào database (tài khoản mới, mã OTP).
    - Nhược điểm: API mất khoảng 2 đến 5 giây.

    Chấp nhận nhược điểm này vì chỉ có 4 API gửi OTP bị ảnh hưởng,
    và người dùng ít khi gọi các API này:
        POST /api/auth/register
        POST /api/auth/forgot-password
        POST /api/users/me/change-password/request-otp
        POST /api/users/me/email/request-otp
    Thời gian chờ tối đa được giới hạn bằng cấu hình spring.mail.properties.mail.smtp.*
    trong file properties, để request không bị treo khi Gmail không phản hồi.
    Theo tài liệu user.md, cả 3 mốc đều là 5000 ms (5 giây):
        connectiontimeout : chờ kết nối tới Gmail
        timeout           : chờ Gmail trả lời
        writetimeout      : chờ gửi xong nội dung thư
    Quá 5 giây → JavaMail ném lỗi → EmailSendException → 503.

    -----------------------------------------------------
    Gửi qua Gmail cần cấu hình gì?
    -----------------------------------------------------
    Trong application.properties (đã gitignore, không đưa lên GitHub):
        spring.mail.host=smtp.gmail.com
        spring.mail.username=<địa chỉ Gmail dùng để gửi>
        spring.mail.password=<mật khẩu ứng dụng 16 ký tự, KHÔNG phải mật khẩu Gmail thường>
    Spring Boot đọc các dòng này và tự tạo sẵn bean JavaMailSender, class này chỉ việc nhận về dùng.
    Sai mật khẩu ứng dụng → mọi lần gửi đều lỗi 503 (log có dòng "Could not send OTP email to ...").
*/
@Service
public class EmailService {
    /*
        - mailSender: công cụ gửi email của Spring (Spring Boot tạo sẵn từ cấu hình spring.mail.*).
        - log: ghi lỗi gửi email vào log (giải thích Logger: xem GlobalExceptionHandler).
        - fromEmail: địa chỉ người gửi, lấy từ spring.mail.username.
          @Value đặt thẳng trên thuộc tính (field injection): Spring gán giá trị ngay sau khi tạo object.
          Gmail chỉ cho gửi bằng đúng tài khoản đã đăng nhập SMTP, nên dùng lại spring.mail.username.
    */
    private final JavaMailSender mailSender;
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /*
        =====================================================
        GỬI 1 EMAIL CHỨA MÃ OTP
        =====================================================
        Tham số:
        - toEmail: địa chỉ nhận (email hiện tại, hoặc email mới khi đổi email).
        - otpCode: mã 6 chữ số do OtpService sinh ra.

        Các bước:
        1. new SimpleMailMessage(): 1 email dạng chữ thường (không HTML, không file đính kèm).
        2. setFrom("MySpotify <" + fromEmail + ">"): dạng "Tên hiển thị <địa chỉ>"
           → hộp thư người nhận hiện tên "MySpotify" thay vì địa chỉ Gmail trần trụi.
        3. setTo, setSubject, setText: người nhận, tiêu đề, nội dung.
           "\n" là ký tự xuống dòng, "\n\n" là xuống dòng 2 lần (cách 1 dòng trống).
        4. mailSender.send(message): kết nối tới Gmail và gửi, chờ tới khi gửi xong (đồng bộ).

        Email người dùng nhận được:
            Tiêu đề: MySpotify - Mã xác thực OTP

            Xin chào,

            Mã OTP của bạn là: 482913

            Mã có hiệu lực trong 5 phút và chỉ sử dụng được 1 lần.
            ...
        Lưu ý: chữ "5 phút" viết cứng trong nội dung thư. Sau này đổi OTP_EXPIRE_MINUTES
        trong OtpService thì nhớ sửa cả câu này.

        Bất kỳ lỗi nào (sai mật khẩu ứng dụng, mất mạng, quá thời gian chờ...) → khối catch:
        - Ghi log lỗi gốc (để người quản trị biết nguyên nhân thật).
        - Ném EmailSendException với message gọn → transaction của nơi gọi rollback → 503.
        Chi tiết luồng lỗi: xem EmailSendException và GlobalExceptionHandler.handleEmailSend.
    */
    public void sendOtp(String toEmail, String otpCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("MySpotify <" + fromEmail + ">");
            message.setTo(toEmail);
            message.setSubject("MySpotify - Mã xác thực OTP");
            message.setText(
                    "Xin chào,\n\n"
                            + "Mã OTP của bạn là: " + otpCode + "\n\n"
                            + "Mã có hiệu lực trong 5 phút và chỉ sử dụng được 1 lần.\n\n"
                            + "Nếu bạn không yêu cầu mã này, hãy bỏ qua email.\n\n"
                            + "MySpotify"
            );

            mailSender.send(message);
        } catch (Exception e) {
            log.error("Could not send OTP email to {}: {}", toEmail, e.getMessage());
            throw new EmailSendException("Could not send OTP email, please try again later", e);
        }
    }
}