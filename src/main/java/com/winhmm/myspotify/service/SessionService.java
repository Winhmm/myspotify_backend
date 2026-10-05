package com.winhmm.myspotify.service;

/*
    Quản lý phiên đăng nhập trong Redis.
    Mục đích: 1 tài khoản chỉ đăng nhập được trên 1 thiết bị / trình duyệt tại 1 thời điểm.

    Dữ liệu lưu trong Redis:
        key:   session:<email>          VD: session:phong@gmail.com
        value: mã phiên (UUID)          VD: 3f2a9c1e-8b7d-4e21-...
        tự xóa sau đúng thời hạn của token (24 giờ)

    Đăng nhập ở thiết bị mới → mã phiên mới ghi đè mã cũ
    → token của thiết bị cũ không còn khớp → bị đăng xuất.
*/

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class SessionService {
    /*
        Tiền tố để đặt tên Redis key.
    */
    private static final String KEY_PREFIX = "session:";

    /*
        StringRedisTemplate: công cụ Spring cung cấp sẵn để gửi lệnh tới Redis.

        Spring tự tạo và tự đưa vào qua constructor (giống UserRepository).
    */
    private final StringRedisTemplate redisTemplate;

    /*
        Thời hạn phiên = thời hạn token (lấy cùng giá trị app.jwt.expiration-ms).
    */
    private final long expirationMs;

    public SessionService(StringRedisTemplate redisTemplate,
                          @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.redisTemplate = redisTemplate;
        this.expirationMs = expirationMs;
    }

    /*
        Tạo key Redis cho 1 email.
        VD: "phong@gmail.com" → "session:phong@gmail.com"
    */
    private String key(String email) {
        return KEY_PREFIX + email;
    }

    /*
        Đăng nhập → tạo phiên mới.

        1. Sinh mã phiên ngẫu nhiên (UUID, không thể đoán được).
        2. Lưu vào Redis, ghi đè phiên cũ (nếu có), tự xóa sau expirationMs.
           Tương đương lệnh: SET session:<email> <mã phiên> EX <số giây>
        3. Trả mã phiên về để ghi vào token.
    */
    public String createSession(String email) {
        /*
            Tạo session ID.
        */
        String sessionId = UUID.randomUUID().toString();

        /*
            Lưu session vào Redis.
        */
        redisTemplate.opsForValue().set(key(email), sessionId, Duration.ofMillis(expirationMs));

        /*
            Cấp cho Redis và JWT.
        */
        return sessionId;
    }

    /*
        Mỗi request → kiểm tra phiên trong token còn hợp lệ không.

        Tương đương lệnh: GET session:<email>
        - Giống mã phiên trong token → hợp lệ (true).
        - Khác (đã đăng nhập ở thiết bị khác) → không hợp lệ (false).
        - Không có (đã đăng xuất / hết hạn) → không hợp lệ (false).

        SessionId trong JWT có phải session hiện tại của tài khoản này không?
    */
    public boolean isValidSession(String email, String sessionId) {
        if(sessionId == null) {
            return false;
        }

        /*
            SessionId trong JWT có phải session hiện tại của tài khoản này không?
        */
        String currentSessionId = redisTemplate.opsForValue().get(key(email));

        /*
            So sánh với JWT
        */
        return sessionId.equals(currentSessionId);
    }

    /*
        Đăng xuất / đổi mật khẩu / bị khóa... → xóa phiên.
        Tương đương lệnh: DEL session:<email>
        → Mọi token cũ của tài khoản này mất hiệu lực ngay lập tức.
    */
    public void deleteSession(String email) {
        redisTemplate.delete(key(email));
    }
}