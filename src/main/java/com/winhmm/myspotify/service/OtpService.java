package com.winhmm.myspotify.service;

import com.winhmm.myspotify.entity.OtpVerification;
import com.winhmm.myspotify.entity.User;
import com.winhmm.myspotify.enums.OtpPurpose;
import com.winhmm.myspotify.repository.OtpVerificationRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class OtpService {
    private final OtpVerificationRepository otpVerificationRepository;
    private final EmailService emailService;
    private static final SecureRandom RANDOM = new SecureRandom();

    /*
        OTP hết hạn sau 5 phút.

        Phải chờ 60 giây mới được yêu cầu OTP mới.
    */
    private static final int OTP_EXPIRE_MINUTES = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 60;

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
        LocalDateTime now = LocalDateTime.now();

        /*
            Chống spam: OTP gần nhất được tạo chưa đủ 60 giây → báo lỗi.

            Thời điểm tạo = expiredAt - 5 phút.
        */
        Optional<OtpVerification> latest = otpVerificationRepository
                .findFirstByUserAndPurposeAndVerifiedFalseOrderByIdDesc(user, purpose);

        if(latest.isPresent()) {
            LocalDateTime createdAt = latest.get().getExpiredAt().minusMinutes(OTP_EXPIRE_MINUTES);
            if(createdAt.plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(now)) {
                throw new IllegalArgumentException(
                        "Please wait " + RESEND_COOLDOWN_SECONDS + " seconds before requesting a new OTP");
            }
        }

        /*
            Vô hiệu hóa toàn bộ OTP cũ chưa dùng (cùng User, cùng mục đích)
             → chỉ OTP mới nhất dùng được.

             verified = true nghĩa là "đã dùng / không còn dùng được".
        */
        Optional<OtpVerification> old = latest;
        while(old.isPresent()) {
            OtpVerification otp = old.get();
            otp.setVerified(true);
            otpVerificationRepository.save(otp);

            old = otpVerificationRepository
                    .findFirstByUserAndPurposeAndVerifiedFalseOrderByIdDesc(user, purpose);
        }

        /*
            Tạo OTP mới → lưu → gửi email.
        */
        String otpCode = String.format("%06d", RANDOM.nextInt(1_000_000));

        OtpVerification otp = new OtpVerification();
        otp.setUser(user);
        otp.setOtpCode(otpCode);
        otp.setPurpose(purpose);
        otp.setNewEmail(newEmail);
        otp.setExpiredAt(now.plusMinutes(OTP_EXPIRE_MINUTES));
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
