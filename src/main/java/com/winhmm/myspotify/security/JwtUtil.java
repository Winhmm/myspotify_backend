package com.winhmm.myspotify.security;

import com.winhmm.myspotify.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/*
    =====================================================
    JWTUTIL - TẠO VÀ KIỂM TRA TOKEN
    =====================================================

    Class này có 2 nhiệm vụ:
    - Tạo token (giống "in vé"): generateToken() được gọi 1 lần khi người dùng
      đăng nhập thành công (trong AuthService.login).
    - Kiểm tra token (giống "soát vé"): validateToken(), getEmailFromToken(),
      getSessionIdFromToken() được gọi ở mỗi request, bên trong JwtAuthenticationFilter.

    @Component: Spring tự tạo 1 đối tượng JwtUtil duy nhất khi khởi động,
    rồi tiêm (inject) vào các class cần dùng như AuthService, SecurityConfig.

    -----------------------------------------------------
    Cấu trúc của 1 JWT
    -----------------------------------------------------
    Một JWT gồm 3 phần, ngăn cách bằng dấu chấm: header.payload.signature

    Ví dụ:
        eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhQGdtYWlsLmNvbSJ9.kT3x9...

    - Header: cho biết thuật toán ký. Giải mã ra: { "alg": "HS256" }
    - Payload: phần dữ liệu, mỗi dữ liệu gọi là 1 "claim". Project này lưu 4 claim:
        + sub (subject)    : email của người dùng → biết token này của ai.
        + jti (JWT ID)     : mã phiên đăng nhập (chuỗi UUID), cũng được lưu trong Redis.
        + iat (issued at)  : thời điểm tạo token.
        + exp (expiration) : thời điểm token hết hạn.
      Giải mã ra:
        { "sub": "a@gmail.com", "jti": "3f2c8a...", "iat": 1760000000, "exp": 1760086400 }
    - Signature: chữ ký = HMAC-SHA256(header + payload, secret).

    -----------------------------------------------------
    Token có an toàn không?
    -----------------------------------------------------
    - Payload chỉ được mã hóa Base64, đây không phải mã hóa bí mật.
      Ai cầm token cũng đọc được nội dung (dán vào trang jwt.io là thấy email).
      → Không bao giờ bỏ mật khẩu hoặc dữ liệu nhạy cảm vào token.
    - Nhưng không ai sửa được payload:
      hacker đổi email trong payload thành email của Admin thì chữ ký không còn khớp.
      Muốn tạo lại chữ ký đúng phải có secret, mà secret chỉ nằm trên server.
      → Server luôn phát hiện được token bị sửa.

    -----------------------------------------------------
    Vì sao không bỏ role vào token?
    -----------------------------------------------------
    - Token sống 7 ngày và không sửa được sau khi đã gửi cho client.
    - Nếu role nằm trong token: Admin hạ 1 người từ ADMIN xuống USER,
      nhưng token cũ của người đó vẫn ghi ADMIN
      → họ vẫn dùng được quyền Admin cho tới khi token hết hạn.
    - Project này chỉ để email trong token. Role được đọc lại từ MySQL ở mỗi request
      (xem CustomUserDetailsService) → đổi role là có hiệu lực ngay ở request tiếp theo.

    -----------------------------------------------------
    Vì sao cần thêm jti (mã phiên)?
    -----------------------------------------------------
    - JWT bình thường có 1 nhược điểm: đã phát ra thì không thu hồi được cho tới khi hết hạn.
    - Project lưu jti vào Redis với key session:{email}.
      Mỗi request, filter so jti trong token với giá trị trong Redis:
        + Khớp → token còn hiệu lực.
        + Không khớp hoặc key không tồn tại → token đã bị thu hồi → 401.
    - Muốn "đá" 1 người ra khỏi hệ thống, chỉ cần xóa hoặc ghi đè key Redis đó.
*/
@Component
public class JwtUtil {
    private final SecretKey key;
    private final long expirationMs;

    /*
        -----------------------------------------------------
        Constructor: đọc cấu hình và tạo khóa
        -----------------------------------------------------
    */
    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expiration-ms}") long expirationMs) {
        /*
            secret.getBytes(UTF_8): chuyển chuỗi secret thành mảng byte.

            Keys.hmacShaKeyFor(...): biến mảng byte thành khóa HMAC mà thư viện jjwt dùng được.
        */
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /*
        =====================================================
        TẠO TOKEN
        =====================================================

        Được gọi trong AuthService.login, sau khi đã:
        1. Kiểm tra email và mật khẩu đúng.
        2. Kiểm tra tài khoản đang ACTIVE.
        3. Gọi SessionService.createSession(email) → sinh UUID mới và ghi vào Redis
           (ghi đè UUID cũ → thiết bị đăng nhập trước đó bị đá ra).
    */
    public String generateToken(User user, String sessionId) {
        return Jwts.builder()
                .subject(user.getEmail())
                .id(sessionId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    /*
        =====================================================
        KIỂM TRA TOKEN HỢP LỆ
        =====================================================
    */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /*
        =====================================================
        LẤY EMAIL TỪ TOKEN
        =====================================================

        Chỉ gọi hàm này sau khi validateToken() đã trả về true.
        Nếu gọi với token sai, hàm sẽ ném exception thay vì trả về null.
    */
    public String getEmailFromToken(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /*
        =====================================================
        LẤY MÃ PHIÊN TỪ TOKEN
        =====================================================

        JwtAuthenticationFilter dùng giá trị này để so với Redis (key session:{email}).
        Giống getEmailFromToken, chỉ gọi sau khi validateToken() trả về true.
    */
    public String getSessionIdFromToken(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token)
                .getPayload()
                .getId();
    }
}
