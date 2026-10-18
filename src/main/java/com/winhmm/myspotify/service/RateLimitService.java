package com.winhmm.myspotify.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/*
    =====================================================
    RATELIMITSERVICE - GIỚI HẠN SỐ REQUEST BẰNG REDIS
    =====================================================
    Được RateLimitFilter gọi ở mỗi request, để chống:
    - Dò mật khẩu: thử hàng nghìn mật khẩu liên tục vào API đăng nhập.
    - Spam API: gửi hàng loạt request làm quá tải server.

    -----------------------------------------------------
    Giới hạn hiện tại (cấu hình trong application.properties)
    -----------------------------------------------------
        app.rate-limit.window-seconds=60    → cửa sổ đếm 60 giây
        app.rate-limit.auth-requests=10     → /api/auth/**: 10 request mỗi cửa sổ, đếm theo IP
        app.rate-limit.user-requests=100    → API khác: 100 request mỗi cửa sổ,
                                              đếm theo email (hoặc theo IP nếu chưa đăng nhập)

    Key Redis do RateLimitFilter đặt:
        rate:auth:<IP>       VD: rate:auth:113.161.20.5
        rate:user:<email>    VD: rate:user:maychieu@gmail.com
        rate:ip:<IP>         (chưa đăng nhập, gọi API khác /api/auth/**)

    -----------------------------------------------------
    Cách hoạt động: cửa sổ cố định (fixed window)
    -----------------------------------------------------
    - Mỗi tài khoản hoặc IP có 1 bộ đếm trong Redis.
    - Mỗi request → tăng bộ đếm lên 1 (lệnh INCR).
    - Request đầu tiên của cửa sổ → đặt thời hạn 60 giây cho bộ đếm.
    - Bộ đếm vượt giới hạn → chặn, trả 429 kèm header Retry-After.
    - Hết 60 giây → Redis tự xóa bộ đếm → đếm lại từ đầu.

    Ví dụ với giới hạn 10:
        14:00:00  request 1   → bộ đếm = 1, đặt hết hạn lúc 14:01:00
        14:00:20  request 10  → bộ đếm = 10, vẫn cho qua
        14:00:25  request 11  → bộ đếm = 11 → chặn, Retry-After: 35
        14:01:00  Redis xóa bộ đếm
        14:01:02  request mới → bộ đếm = 1, cho qua

    Nhược điểm đã biết: dồn request vào cuối cửa sổ này và đầu cửa sổ sau
    thì có thể gửi tối đa gấp đôi giới hạn trong khoảng thời gian ngắn.
    Chấp nhận được với mục đích chống dò mật khẩu và spam.
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
        =====================================================
        GHI NHẬN 1 REQUEST VÀ QUYẾT ĐỊNH CHO QUA HAY CHẶN
        =====================================================

        1. INCR key → tăng bộ đếm lên 1, trả về giá trị mới.
           Key chưa có → Redis tự tạo với giá trị 1.
           INCR là lệnh nguyên tử: nhiều request cùng lúc vẫn đếm đúng, không bị trùng số.
        2. Giá trị = 1 → request đầu tiên của cửa sổ → đặt thời hạn windowSeconds.
        3. Giá trị nhỏ hơn hoặc bằng limit → cho qua (true), ngược lại → chặn (false).

        Redis không phản hồi (count == null) → trả false.
        RateLimitFilter có xử lý riêng khi Redis lỗi (cho request đi qua),
        để Redis sập không làm cả hệ thống ngừng hoạt động.
    */
    public boolean tryAcquire(String key, int limit) {
        Long count = redisTemplate.opsForValue().increment(key);

        if(count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
        }

        return count != null && count <= limit;
    }

    /*
        =====================================================
        CÒN BAO NHIÊU GIÂY THÌ ĐƯỢC GỬI LẠI
        =====================================================

        Tương đương lệnh Redis: TTL key (thời gian sống còn lại của key, tính bằng giây).
        Giá trị này được gửi về frontend trong header Retry-After của response 429,
        để frontend hiện "Vui lòng thử lại sau 35 giây".

        ttl null hoặc nhỏ hơn 1 (key vừa hết hạn, hoặc không có thời hạn) → trả 1 giây.
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
