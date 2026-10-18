package com.winhmm.myspotify.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/*
    =====================================================
    SESSIONSERVICE - QUẢN LÝ PHIÊN ĐĂNG NHẬP TRONG REDIS
    =====================================================

    Mục đích:
    1. Mỗi tài khoản chỉ đăng nhập được trên 1 thiết bị tại 1 thời điểm.
    2. Thu hồi token ngay lập tức khi cần (JWT thông thường không làm được việc này).

    -----------------------------------------------------
    Dữ liệu lưu trong Redis
    -----------------------------------------------------
        key   : session:<email>          VD: session:maychieu@gmail.com
        value : mã phiên (UUID)          VD: 3f2a9c1e-8b7d-4e21-a9c3-5d6e7f8a9b0c
        tự xóa sau đúng thời hạn của token (24 giờ)

    Mã phiên này cũng được ghi vào JWT (claim "jti").
    Mỗi request, JwtAuthenticationFilter so mã phiên trong token với Redis:
    - Khớp → token còn hiệu lực.
    - Không khớp hoặc không có key → token đã bị thu hồi → 401.

    -----------------------------------------------------
    Khi nào phiên bị thay đổi?
    -----------------------------------------------------
    - Đăng nhập (createSession)  : ghi đè mã cũ → thiết bị cũ bị đăng xuất.
    - deleteSession được gọi khi : đăng xuất, đổi mật khẩu, đặt lại mật khẩu,
                                   đổi email, tự vô hiệu hóa, tự xóa tài khoản,
                                   bị Admin khóa.

    -----------------------------------------------------
    Vì sao dùng Redis, không dùng MySQL?
    -----------------------------------------------------
    Phiên được đọc ở mọi request. Redis lưu dữ liệu trong RAM nên đọc rất nhanh (khoảng 1 ms),
    và tự xóa key khi hết hạn, không cần viết code dọn dẹp.
*/
@Service
public class SessionService {
    /*
        Tiền tố của key Redis, để phân biệt với các loại key khác
        (rate:..., otp-attempt:..., play:...).
    */
    private static final String KEY_PREFIX = "session:";

    /*
        StringRedisTemplate: công cụ Spring cung cấp sẵn để gửi lệnh tới Redis
        (key và value đều là chuỗi).
        Spring tự tạo và truyền vào qua constructor.
    */
    private final StringRedisTemplate redisTemplate;

    /*
        Thời hạn phiên = thời hạn token (cùng giá trị app.jwt.expiration-ms, mặc định 24 giờ).
        Phiên và token hết hạn cùng lúc, không cái nào sống lâu hơn cái nào.
    */
    private final long expirationMs;

    public SessionService(StringRedisTemplate redisTemplate,
                          @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.redisTemplate = redisTemplate;
        this.expirationMs = expirationMs;
    }

    /*
        Tạo key Redis cho 1 email.
        VD: "maychieu@gmail.com" → "session:maychieu@gmail.com"
    */
    private String key(String email) {
        return KEY_PREFIX + email;
    }

    /*
        =====================================================
        TẠO PHIÊN MỚI (KHI ĐĂNG NHẬP)
        =====================================================

        1. Sinh mã phiên ngẫu nhiên bằng UUID (36 ký tự, gần như không thể đoán được).
        2. Lưu vào Redis, ghi đè phiên cũ (nếu có), tự xóa sau expirationMs.
           Tương đương lệnh Redis: SET session:<email> <mã phiên> EX <số giây>
        3. Trả mã phiên về cho AuthService để ghi vào token.
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
        =====================================================
        KIỂM TRA PHIÊN CÒN HỢP LỆ KHÔNG (MỖI REQUEST)
        =====================================================

        Được gọi từ JwtAuthenticationFilter ở mỗi request có token.
        Câu hỏi cần trả lời: mã phiên trong token có phải phiên hiện tại của tài khoản không?

        Tương đương lệnh Redis: GET session:<email>
        - Giống mã phiên trong token → hợp lệ (true).
        - Khác (đã đăng nhập ở thiết bị khác) → không hợp lệ (false).
        - Không có key (đã đăng xuất, bị khóa, hết hạn) → không hợp lệ (false).

        sessionId.equals(currentSessionId): nếu currentSessionId là null,
        equals trả về false, không gây lỗi.
        Token không có jti (sessionId == null) → false ngay.
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
        =====================================================
        XÓA PHIÊN (THU HỒI TOKEN)
        =====================================================

        Tương đương lệnh Redis: DEL session:<email>
        → Mọi token của tài khoản này mất hiệu lực ngay lập tức, dù còn hạn.
        Xóa key không tồn tại cũng không lỗi.
    */
    public void deleteSession(String email) {
        redisTemplate.delete(key(email));
    }
}


