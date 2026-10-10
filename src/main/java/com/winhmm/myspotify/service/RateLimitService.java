package com.winhmm.myspotify.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/*
    Giới hạn số request (rate limiting) bằng Redis.

    Cách hoạt động (fixed window):
    - Mỗi tài khoản / IP có 1 bộ đếm trong Redis, VD: rate:user:phong@gmail.com.
    - Mỗi request → tăng bộ đếm lên 1 (lệnh INCR).
    - Request đầu tiên của cửa sổ → đặt thời hạn 60 giây cho bộ đếm.
    - Bộ đếm vượt giới hạn → chặn.
    - Hết 60 giây → Redis tự xóa bộ đếm → đếm lại từ đầu.
*/
@Service
public class RateLimitService {
    private final StringRedisTemplate redisTemplate;
    private final int windowSeconds;
    private final int userRequests;
    private final int authRequests;

    public RateLimitService (
            StringRedisTemplate redisTemplate,
            @Value("${app.rate-limit.window-seconds}") int windowSeconds,
            @Value("${app.rate-limit.user-requests}") int userRequests,
            @Value("${app.rate-limit.auth-requests}") int authRequests
    ) {
        this.redisTemplate = redisTemplate;
        this.windowSeconds = windowSeconds;
        this.userRequests = userRequests;
        this.authRequests = authRequests;
    }

    /*
        Ghi nhận 1 request và cho biết có được phép đi tiếp không.

        1. INCR key → tăng bộ đếm lên 1, trả về giá trị mới
           (key chưa có → Redis tự tạo với giá trị 1).
        2. Giá trị = 1 → đây là request đầu tiên của cửa sổ → đặt thời hạn.
        3. Giá trị <= limit → cho qua (true), ngược lại → chặn (false).
    */
    public boolean tryAcquire(String key, int limit) {
        Long count = redisTemplate.opsForValue().increment(key);

        if(count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
        }

        return count != null && count <= limit;
    }

    /*
        Còn bao nhiêu giây thì bộ đếm được xóa (để báo cho frontend thử lại sau).
        Tương đương lệnh Redis: TTL key.
    */
    public long getRetryAfterSeconds(String key) {
        Long ttl = redisTemplate.getExpire(key);
        if(ttl == null || ttl < 1) {
            return 1;
        } else {
            return ttl;
        }
    }

    public int getUserRequests() { return userRequests; }
    public int getAuthRequests() { return authRequests; }
}
