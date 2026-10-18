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

    Chấp nhận nhược điểm này vì chỉ có 5 API gửi OTP bị ảnh hưởng,
    và người dùng ít khi gọi các API này.
    Thời gian chờ tối đa được giới hạn bằng cấu hình spring.mail.properties.mail.smtp.*
    trong file properties, để request không bị treo khi Gmail không phản hồi.
*/
@Service
public class EmailService {
    private final JavaMailSender mailSender;
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

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