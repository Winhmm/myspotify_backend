package com.winhmm.myspotify.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/*
    =====================================================
    PACKAGE SERVICE - THỨ TỰ ĐỌC
    =====================================================

    14 file, đọc từ dễ tới khó (file sau dùng lại kiến thức của file trước):
        Nhóm Redis và email (nhỏ, được các Service khác dùng chung):
             1. SessionService          (file này): phiên đăng nhập, Redis cơ bản
             2. RateLimitService        : đếm request bằng Redis
             3. EmailService            : gửi email
             4. OtpService              : tạo, gửi, kiểm tra mã OTP, @Transactional
        Nhóm tài khoản:
             5. AuthService             : đăng ký, xác thực OTP, đăng nhập, quên mật khẩu
             6. UserService             : thông tin cá nhân, đổi mật khẩu, đổi email, xin làm Artist
             7. AdminService            : Admin quản lý tài khoản
        Nhóm bài hát:
             8. FileStorageService      : lưu, kiểm tra, xóa file nhạc, ảnh, video
             9. ArtistSongService       : Artist quản lý bài của mình
            10. AdminSongService        : Admin duyệt, từ chối, gỡ bài
            11. SongService             : người nghe xem, tìm, nghe nhạc
            12. ListeningHistoryService : lịch sử nghe (MongoDB, @Async)
            13. LyricsService           : lời bài hát (regex)
            14. RecommendationService   : gợi ý bài hát bằng AI (gọi sang Python)

    =====================================================
    SERVICE LÀ GÌ? (ĐỌC NẾU MỚI HỌC)
    =====================================================

    Kiến trúc 3 lớp của dự án:
        Controller : nhận request, lấy dữ liệu từ request, gọi Service, trả response.
        Service    : chứa quy tắc nghiệp vụ "được làm gì, khi nào, với điều kiện gì" (package này).
        Repository : đọc và ghi database.

    Những điểm chung của mọi file trong package:
    - @Service: Spring tự tạo đúng 1 object của class lúc khởi động (gọi là bean),
      rồi tự truyền vào nơi cần dùng qua constructor (constructor injection):
          public AuthService(UserRepository userRepository, OtpService otpService, ...)
          → Spring thấy constructor cần UserRepository, OtpService... thì tự tìm bean tương ứng truyền vào.
          → Không bao giờ phải tự viết new AuthService(...).
    - Thuộc tính private final: gán 1 lần trong constructor, sau đó không đổi được nữa.
    - Vi phạm quy tắc → throw new IllegalArgumentException("message")
      → GlobalExceptionHandler trả 400 { "message": "..." } cho frontend.
    - @Transactional (chỉ có ở vài hàm): gom nhiều thao tác database thành 1 khối
      "làm hết hoặc không làm gì" (giải thích chi tiết trong AuthService và OtpService).

    =====================================================
    REDIS LÀ GÌ? (ĐỌC NẾU MỚI HỌC)
    =====================================================

    Redis là 1 database lưu dữ liệu trong RAM, dạng "key → value" (giống 1 Map khổng lồ):
        session:a@gmail.com    →  "3f2a9c1e-8b7d-..."
        rate:user:a@gmail.com  →  "57"
    - Rất nhanh (đọc ghi khoảng 1 ms), hợp với dữ liệu được đọc ở MỌI request.
    - Mỗi key đặt được thời hạn sống (TTL): hết hạn Redis tự xóa, không cần code dọn dẹp.
    - Theo tài liệu user.md, Redis của dự án không lưu xuống ổ đĩa: khởi động lại là mất hết.
      Vì vậy chỉ lưu dữ liệu tạm (phiên đăng nhập, bộ đếm). Dữ liệu quan trọng vẫn ở MySQL.
      Mất phiên → mọi người phải đăng nhập lại, không mất dữ liệu quan trọng nào.

    Các loại key dự án đang dùng:
        session:<email>          phiên đăng nhập            (SessionService)
        rate:...                 bộ đếm rate limit          (RateLimitService)
        otp-attempt:<id mã>      số lần nhập OTP            (OtpService)
        play:<songId>:<email>    chống đếm trùng lượt nghe  (SongService)

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
        Spring tự tạo và truyền vào qua constructor
        (Spring Boot đọc cấu hình spring.data.redis.* để biết Redis nằm ở đâu).

        Cách dùng trong file này:
            redisTemplate.opsForValue()           → nhóm lệnh làm việc với value dạng chuỗi
                .set(key, value, thờiHạn)         ~ lệnh Redis: SET key value EX số_giây
                .get(key)                         ~ lệnh Redis: GET key (key không tồn tại → null)
            redisTemplate.delete(key)             ~ lệnh Redis: DEL key
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
           UUID.randomUUID() tạo mã mới, .toString() đổi thành chuỗi để lưu.
        2. Lưu vào Redis, ghi đè phiên cũ (nếu có), tự xóa sau expirationMs.
           Tương đương lệnh Redis: SET session:<email> <mã phiên> EX <số giây>
        3. Trả mã phiên về cho AuthService để ghi vào token.

        Duration.ofMillis(expirationMs): đổi số mili giây thành kiểu Duration (1 khoảng thời gian),
        VD 86400000 ms → 24 giờ. Redis tự xóa key sau đúng khoảng thời gian này.
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