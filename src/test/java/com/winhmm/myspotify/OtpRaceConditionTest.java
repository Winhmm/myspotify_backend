package com.winhmm.myspotify;

import com.winhmm.myspotify.entity.OtpVerification;
import com.winhmm.myspotify.entity.User;
import com.winhmm.myspotify.enums.OtpPurpose;
import com.winhmm.myspotify.repository.OtpVerificationRepository;
import com.winhmm.myspotify.repository.UserRepository;
import com.winhmm.myspotify.service.OtpService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
    Tái hiện race condition ở giới hạn 5 lần nhập sai OTP:
    20 luồng cùng nhập sai OTP ĐÚNG 1 THỜI ĐIỂM.

    Đúng: chỉ 5 lần được kiểm tra mã (báo "sai mã"), 15 lần còn lại bị chặn.
    Lỗi:  nhiều luồng cùng đọc bộ đếm = 0 trước khi ai kịp tăng → vượt quá 5 lần.
*/
@SpringBootTest
class OtpRaceConditionTest {

    private static final int THREADS = 20;

    @Autowired
    private OtpService otpService;

    @Autowired
    private OtpVerificationRepository otpRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void parallelWrongAttemptsAreLimited() throws Exception {
        User user = userRepository.findByEmail("trangphan@example.com").orElseThrow();

        /*
            Tạo sẵn 1 OTP (mã đúng là 123456) để test, không gửi email.
        */
        OtpVerification otp = new OtpVerification();
        otp.setUser(user);
        otp.setOtpCode("123456");
        otp.setPurpose(OtpPurpose.CHANGE_PASSWORD);
        otp.setExpiredAt(LocalDateTime.now().plusMinutes(5));
        otp.setVerified(false);
        OtpVerification savedOtp = otpRepository.save(otp);

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);

        /*
            "Vạch xuất phát": 20 luồng chuẩn bị xong thì đứng chờ ở start.await(),
            start.countDown() → cả 20 luồng chạy cùng lúc.
        */
        CountDownLatch start = new CountDownLatch(1);

        /*
            AtomicInteger: bộ đếm an toàn khi nhiều luồng cùng tăng
            (int thường sẽ bị chính race condition làm đếm sai).
        */
        AtomicInteger wrongCode = new AtomicInteger();
        AtomicInteger blocked = new AtomicInteger();

        try {
            List<Future<Object>> futures = new ArrayList<>();

            for (int i = 0; i < THREADS; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    try {
                        otpService.verifyAndConsume(user, "000000", OtpPurpose.CHANGE_PASSWORD);
                    } catch (IllegalArgumentException e) {
                        if (e.getMessage().startsWith("Too many")) {
                            blocked.incrementAndGet();
                        } else {
                            wrongCode.incrementAndGet();
                        }
                    }
                    return null;
                }));
            }

            start.countDown();

            for (Future<Object> future : futures) {
                future.get();
            }

            System.out.println("Số lần được kiểm tra mã (báo sai mã): " + wrongCode.get());
            System.out.println("Số lần bị chặn (quá 5 lần):          " + blocked.get());

            assertEquals(5, wrongCode.get(), "Chỉ được phép thử tối đa 5 lần");

        } finally {
            pool.shutdown();
            redisTemplate.delete(List.of(
                    "otp-fail:" + savedOtp.getId(),
                    "otp-attempt:" + savedOtp.getId()));
            otpRepository.delete(savedOtp);
        }
    }
}