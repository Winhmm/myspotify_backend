package com.winhmm.myspotify;

import com.winhmm.myspotify.service.RateLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/*
    Test thử RateLimitService với Redis thật.
    Giới hạn 5 request → request thứ 6, 7 phải bị chặn.
*/
@SpringBootTest
class RateLimitServiceTest {

    @Autowired
    private RateLimitService rateLimitService;

    @Test
    void testLimit() {
        String key = "rate:test:" + System.currentTimeMillis();   // key mới mỗi lần chạy

        for (int i = 1; i <= 7; i++) {
            boolean allowed = rateLimitService.tryAcquire(key, 5);
            System.out.println("Request " + i + ": " + (allowed ? "cho qua" : "BỊ CHẶN"));
        }

        System.out.println("Thử lại sau: " + rateLimitService.getRetryAfterSeconds(key) + " giây");
    }
}
