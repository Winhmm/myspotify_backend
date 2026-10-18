package com.winhmm.myspotify.service;

import com.winhmm.myspotify.entity.OtpVerification;
import com.winhmm.myspotify.entity.User;
import com.winhmm.myspotify.enums.OtpPurpose;
import com.winhmm.myspotify.repository.OtpVerificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.time.Duration;

/*
    =====================================================
    OTPSERVICE - TẠO, GỬI VÀ KIỂM TRA MÃ OTP
    =====================================================

    Các chức năng:
        generateAndSend(user, purpose)           → gửi mã tới email hiện tại của User
        generateAndSend(user, purpose, newEmail) → gửi mã tới email mới (đổi email)
        verifyAndConsume(user, otpCode, purpose) → kiểm tra mã, đúng thì đánh dấu đã dùng

    Được gọi từ AuthService (đăng ký, quên mật khẩu) và UserService (đổi mật khẩu, đổi email).

    -----------------------------------------------------
    Các quy tắc bảo mật
    -----------------------------------------------------
    1. Mã gồm 6 chữ số ngẫu nhiên, sinh bằng SecureRandom (không đoán trước được).
    2. Hết hạn sau 5 phút.
    3. Chờ 60 giây giữa 2 lần yêu cầu mã (cùng User, cùng mục đích) → chống spam email.
    4. Mỗi mã chỉ được nhập sai tối đa 5 lần → chống dò mã.
       Có 1.000.000 mã khả dĩ, 5 lần thử chỉ có xác suất đoán trúng 0,0005%.
    5. Mỗi mã chỉ dùng được 1 lần.
    6. Có mã mới thì mọi mã cũ cùng mục đích bị vô hiệu hóa → chỉ mã mới nhất dùng được.
    7. Mã của mục đích này không dùng được cho mục đích khác.

    -----------------------------------------------------
    Dữ liệu được lưu ở đâu?
    -----------------------------------------------------
    - MySQL (bảng otp_verifications): mã OTP, mục đích, hạn dùng, đã dùng hay chưa.
    - Redis (key otp-attempt:{id của mã}): số lần đã nhập sai.
*/
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

    /*
        StringRedisTemplate: công cụ gửi lệnh tới Redis, dùng để đếm số lần nhập sai.
        MAX_FAILED_ATTEMPTS: số lần được nhập (cả đúng lẫn sai) cho mỗi mã.
    */
    private final StringRedisTemplate redisTemplate;
    private static final int MAX_FAILED_ATTEMPTS = 5;

    public OtpService(OtpVerificationRepository otpVerificationRepository,
                      EmailService emailService,
                      StringRedisTemplate redisTemplate) {
        this.otpVerificationRepository = otpVerificationRepository;
        this.emailService = emailService;
        this.redisTemplate = redisTemplate;
    }

    /*
        =====================================================
        GỬI OTP TỚI EMAIL HIỆN TẠI
        =====================================================
        Dùng cho: đăng ký (REGISTER), quên mật khẩu (RESET_PASSWORD), đổi mật khẩu (CHANGE_PASSWORD).
        newEmail = null vì các mục đích này không cần lưu email mới.

        -----------------------------------------------------
        Vì sao có 2 hàm generateAndSend và 1 hàm createAndSend?
        -----------------------------------------------------
        - 2 hàm public generateAndSend(): quyết định gửi tới email nào.
        - 1 hàm private createAndSend(): làm việc thật sự (kiểm tra, tạo, lưu, gửi).
        Nơi gọi chỉ cần chọn đúng hàm, không phải tự truyền 4 tham số.

        -----------------------------------------------------
        @Transactional ở đây làm gì?
        -----------------------------------------------------
        - Nơi gọi đã có transaction (AuthService.register...) → dùng chung transaction đó.
        - Nơi gọi chưa có → tự mở transaction mới.
        → Vô hiệu hóa mã cũ, lưu mã mới, gửi email luôn cùng thành công hoặc cùng bị hủy.

        Đặt ở hàm public vì Spring không áp dụng @Transactional cho hàm private.
    */
    @Transactional
    public void generateAndSend(User user, OtpPurpose purpose) {
        createAndSend(user, purpose, null, user.getEmail());
    }

    /*
        =====================================================
        GỬI OTP TỚI EMAIL MỚI
        =====================================================
        Dùng cho: đổi email (CHANGE_EMAIL).
        - Lưu newEmail vào bản ghi OTP, để bước 2 lấy ra cập nhật.
        - Gửi mã tới newEmail (không phải email hiện tại),
          để chứng minh người dùng sở hữu email mới.
    */
    @Transactional
    public void generateAndSend(User user, OtpPurpose purpose, String newEmail) {
        createAndSend(user, purpose, newEmail, newEmail);
    }

    /*
        =====================================================
        TẠO, LƯU VÀ GỬI 1 MÃ OTP
        =====================================================
        Tham số:
        - user: chủ của mã.
        - purpose: mục đích của mã.
        - newEmail: email mới cần lưu (chỉ cho CHANGE_EMAIL, còn lại null).
        - toEmail: địa chỉ nhận mã.
    */
    private void createAndSend(User user, OtpPurpose purpose, String newEmail, String toEmail) {
        LocalDateTime now = LocalDateTime.now();

        /*
            -----------------------------------------------------
            Bước 1: Chống spam, chờ 60 giây giữa 2 lần gửi
            -----------------------------------------------------

            Lấy mã mới nhất chưa dùng (cùng User, cùng mục đích).
            Bảng không có cột created_at, nên tính thời điểm tạo:
                createdAt = expiredAt - 5 phút

            Ví dụ: mã trước tạo lúc 14:00:00 (hết hạn 14:05:00).
            - Yêu cầu mã mới lúc 14:00:40: 14:00:00 + 60 giây = 14:01:00, vẫn sau 14:00:40 → chặn.
            - Yêu cầu mã mới lúc 14:01:10: 14:01:00 đã qua → cho phép.

            Bị chặn → IllegalArgumentException → nơi gọi rollback → không có gì thay đổi.
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
            -----------------------------------------------------
            Bước 2: Vô hiệu hóa toàn bộ mã cũ chưa dùng
            -----------------------------------------------------

            Cùng User, cùng mục đích → đặt verified = true.
            Nhờ vậy chỉ mã mới nhất dùng được, mã cũ trong hộp thư trở thành vô dụng.

            Vòng lặp: lấy mã chưa dùng mới nhất → vô hiệu hóa → lấy tiếp, đến khi hết.
            Hibernate tự ghi thay đổi xuống database trước mỗi lần truy vấn tiếp theo,
            nên mã vừa vô hiệu hóa không bị lấy lại lần nữa.
            Thường chỉ có 0 hoặc 1 mã cũ, vòng lặp chạy rất ít lần.
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
            -----------------------------------------------------
            Bước 3: Sinh mã 6 chữ số
            -----------------------------------------------------
            - RANDOM.nextInt(1_000_000): số ngẫu nhiên từ 0 đến 999999.
              (1_000_000 là cách viết số 1000000 có dấu gạch dưới cho dễ đọc.)
            - String.format("%06d", ...): đủ 6 chữ số, thiếu thì thêm số 0 ở đầu.
              VD: 7215 → "007215", 482913 → "482913".
        */
        String otpCode = String.format("%06d", RANDOM.nextInt(1_000_000));

        /*
            -----------------------------------------------------
            Bước 4: Lưu mã vào database, rồi gửi email
            -----------------------------------------------------
            Lưu trước, gửi sau: nếu gửi email lỗi (EmailSendException),
            transaction rollback → mã vừa lưu bị hủy, các mã cũ ở bước 2 được khôi phục.
        */
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

    /*
        =====================================================
        KIỂM TRA MÃ OTP VÀ ĐÁNH DẤU ĐÃ DÙNG
        =====================================================

        Trả về bản ghi OTP đã dùng, để nơi gọi đọc thêm thông tin nếu cần
        (VD UserService.updateEmail lấy newEmail).

        Mọi lỗi đều ném IllegalArgumentException → 400.
        Nơi gọi có @Transactional nên khi lỗi, mọi thay đổi trong MySQL bị hủy.
    */
    public OtpVerification verifyAndConsume(User user, String otpCode, OtpPurpose purpose) {
        /*
            -----------------------------------------------------
            Bước 1: Lấy mã mới nhất chưa dùng
            -----------------------------------------------------

            Không có → chưa yêu cầu mã, hoặc mã đã dùng rồi → "Otp code does not exist".
        */
        OtpVerification otp = otpVerificationRepository
                .findFirstByUserAndPurposeAndVerifiedFalseOrderByIdDesc(user, purpose)
                .orElseThrow(() -> new IllegalArgumentException("Otp code does not exist"));

        /*
            -----------------------------------------------------
            Bước 2: Kiểm tra hạn dùng
            -----------------------------------------------------

            expiredAt trước thời điểm hiện tại → đã hết hạn.
        */
        if(otp.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Otp code has expired");
        }

        /*
            -----------------------------------------------------
            Bước 3: Giới hạn số lần nhập (tối đa 5 lần cho mỗi mã)
            -----------------------------------------------------

            Key Redis theo id của mã, VD otp-attempt:13 → mỗi mã có bộ đếm riêng.

            Vì sao tăng bộ đếm trước rồi mới kiểm tra (chống race condition)?
            - INCR là lệnh nguyên tử: 20 request gửi cùng lúc thì Redis trả về
              lần lượt 1, 2, 3, ..., 20, không bao giờ có 2 request nhận cùng 1 số.
            - Chỉ 5 request nhận số từ 1 đến 5 mới được so mã, các request còn lại bị chặn.

            Cách "đọc bộ đếm → kiểm tra → rồi mới tăng" bị lỗi:
            nhiều request cùng đọc được 0 trước khi request nào kịp tăng
            → tất cả cùng lọt qua bước kiểm tra → kẻ gian thử được nhiều hơn 5 lần.

            Vì sao lưu ở Redis, không lưu trong MySQL?
            Mỗi lần nhập sai, hàm ném lỗi → transaction rollback.
            Nếu lưu bộ đếm trong MySQL, số lần sai cũng bị rollback về như cũ → không bao giờ đạt 5.
            Redis không bị ảnh hưởng bởi rollback.

            Lần đếm đầu tiên (attempts == 1) → đặt thời hạn 5 phút cho key,
            bằng thời hạn của mã, để Redis tự dọn dẹp.

            Redis không phản hồi → attempts == null → coi như vượt giới hạn (an toàn hơn cho qua).
        */
        String attemptKey = "otp-attempt:" + otp.getId();
        Long attempts = redisTemplate.opsForValue().increment(attemptKey);

        if(attempts != null && attempts == 1) {
            redisTemplate.expire(attemptKey, Duration.ofMinutes(OTP_EXPIRE_MINUTES));
        }

        if(attempts == null || attempts > MAX_FAILED_ATTEMPTS) {
            throw new IllegalArgumentException("Too many wrong attempts, please request a new OTP");
        }

        if(!otp.getOtpCode().equals(otpCode)) {
            throw new IllegalArgumentException("Otp code does not match");
        }

        redisTemplate.delete(attemptKey);
        otp.setVerified(true);
        return otpVerificationRepository.save(otp);
    }
}
