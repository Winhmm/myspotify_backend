package com.winhmm.myspotify.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtp(String toEmail, String otpCode) {
        System.out.println("=== OTP gửi tới " + toEmail + ": " + otpCode + " ===");

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
    }
}
