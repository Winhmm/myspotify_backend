package com.winhmm.myspotify.service;

import com.winhmm.myspotify.entity.OtpVerification;
import com.winhmm.myspotify.entity.User;
import com.winhmm.myspotify.enums.OtpPurpose;
import com.winhmm.myspotify.repository.OtpVerificationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class OtpService {
    private final OtpVerificationRepository otpVerificationRepository;
    private final EmailService emailService;

    public OtpService(OtpVerificationRepository otpVerificationRepository, EmailService emailService) {
        this.otpVerificationRepository = otpVerificationRepository;
        this.emailService = emailService;
    }

    /*
        Gửi OTP tới email hiện tại của User.

        Dùng cho: đăng ký, quên mật khẩu, đổi mật khẩu.
    */
    public void generateAndSend(User user, OtpPurpose purpose) {
        createAndSend(user, purpose, null, user.getEmail());
    }

    /*
        Tách thành 2 hàm để phân chia nhiệm vụ:

        1. generateAndSend():
        - Xác định các thông tin cần dùng, ví dụ email nào sẽ nhận OTP.

        2. createAndSend():
        - Là hàm private xử lý công việc thực tế:
        tạo OTP → lưu OTP vào database → gửi email.

        generateAndSend() = quyết định gọi như thế nào.
        createAndSend() = thực hiện việc tạo và gửi OTP.
    */
    public void generateAndSend(User user, OtpPurpose purpose, String newEmail) {
        createAndSend(user, purpose, newEmail, newEmail);
    }

    private void createAndSend(User user, OtpPurpose purpose, String newEmail, String toEmail) {
        String otpCode = String.format("%06d", new Random().nextInt(1_000_000));

        OtpVerification otp = new OtpVerification();
        otp.setUser(user);
        otp.setOtpCode(otpCode);
        otp.setPurpose(purpose);
        otp.setNewEmail(newEmail);
        otp.setExpiredAt(LocalDateTime.now().plusMinutes(5));
        otp.setVerified(false);

        otpVerificationRepository.save(otp);
        emailService.sendOtp(toEmail, otpCode);
    }

    public OtpVerification verifyAndConsume(User user, String otpCode, OtpPurpose purpose) {
        OtpVerification otp = otpVerificationRepository
                .findFirstByUserAndPurposeAndVerifiedFalseOrderByIdDesc(user, purpose)
                .orElseThrow(() -> new IllegalArgumentException("Otp code does not exist"));

        if (otp.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Otp code has expired");
        }

        if (!otp.getOtpCode().equals(otpCode)) {
            throw new IllegalArgumentException("Otp code does not match");
        }

        otp.setVerified(true);
        return otpVerificationRepository.save(otp);
    }
}
