package com.winhmm.myspotify.service;

import org.springframework.stereotype.Service;

@Service
public class EmailService {
    /*
        Hiện tại chỉ in OTP ra console để test bằng Postman,
        sau này sẽ thay bằng gửi email thật.
    */
    public void sendOtp(String toEmail, String otpCode) {
        System.out.println("=== OTP gửi tới " + toEmail + ": " + otpCode + " ===");
    }
}
