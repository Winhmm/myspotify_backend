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
    PACKAGE SECURITY - THỨ TỰ ĐỌC
    =====================================================

        1. JwtUtil                  (file này): token là gì, tạo và kiểm tra token
        2. CustomUserDetailsService : đổi User trong MySQL thành "người dùng" mà Spring Security hiểu
        3. JwtAuthenticationFilter  : mỗi request, dùng 2 file trên để biết "request này của ai"
        4. RateLimitFilter          : giới hạn số request mỗi người được gửi
    Nơi ghép 4 file lại với nhau: SecurityConfig (package config).

    -----------------------------------------------------
    Đường đi của 1 request có token (đọc nếu mới học)
    -----------------------------------------------------
        Frontend gửi: GET /api/users/me/profile
                      Header: Authorization: Bearer eyJhbGciOi...
            │
            ▼
        JwtAuthenticationFilter   đọc token (JwtUtil) → so phiên với Redis (SessionService)
                                  → đọc User từ MySQL (CustomUserDetailsService) → "dán nhãn" người gọi
            │
            ▼
        RateLimitFilter           đếm số request của người đó, quá giới hạn → 429
            │
            ▼
        AuthorizationFilter       API cần đăng nhập mà chưa có nhãn → 401 (luật trong SecurityConfig)
            │
            ▼
        @PreAuthorize             sai role → 403
            │
            ▼
        Controller → Service → Repository → MySQL

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
    Token là gì? Vì sao cần?
    -----------------------------------------------------
    HTTP không "nhớ" ai đã đăng nhập: mỗi request là độc lập.
    Sau khi đăng nhập đúng, server đưa cho frontend 1 chuỗi token (giống "vé vào cửa").
    Mỗi request sau, frontend gửi kèm vé trong header:
        Authorization: Bearer <token>
    Server soát vé → biết người gọi là ai, không cần gửi lại mật khẩu.
    Frontend tự cất token (VD trong localStorage) và gắn vào mọi request.

    JWT (JSON Web Token) là 1 loại token phổ biến. Thư viện dùng: jjwt 0.12.6 (pom.xml).

    -----------------------------------------------------
    Cấu trúc của 1 JWT
    -----------------------------------------------------
    Một JWT gồm 3 phần, ngăn cách bằng dấu chấm: header.payload.signature

    Ví dụ:
        eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhQGdtYWlsLmNvbSJ9.kT3x9...

    - Header: cho biết thuật toán ký. Giải mã ra, VD: { "alg": "HS256" }
    - Payload: phần dữ liệu, mỗi dữ liệu gọi là 1 "claim". Project này lưu 4 claim:
        + sub (subject)    : email của người dùng → biết token này của ai.
        + jti (JWT ID)     : mã phiên đăng nhập (chuỗi UUID), cũng được lưu trong Redis.
        + iat (issued at)  : thời điểm tạo token.
        + exp (expiration) : thời điểm token hết hạn.
      Giải mã ra:
        { "sub": "a@gmail.com", "jti": "3f2c8a...", "iat": 1760000000, "exp": 1760086400 }
        (iat và exp tính bằng số GIÂY kể từ 01/01/1970, cách máy tính hay lưu thời gian.
         1760086400 - 1760000000 = 86400 giây = 24 giờ.)
    - Signature: chữ ký = HMAC-SHA(header + payload, secret).

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
    - Vì vậy secret (app.jwt.secret) phải giữ bí mật tuyệt đối: chỉ nằm trong file cấu hình
      trên server (đã gitignore), không đưa lên GitHub, không gửi cho ai.
      Lộ secret = ai cũng tự in được "vé" giả của bất kỳ tài khoản nào.

    -----------------------------------------------------
    Vì sao không bỏ role vào token?
    -----------------------------------------------------
    - Token sống tới khi hết hạn (app.jwt.expiration-ms, theo tài liệu user.md là 86400000 ms = 24 giờ)
      và không sửa được sau khi đã gửi cho client.
    - Nếu role nằm trong token: Admin hạ 1 Artist xuống USER,
      nhưng token cũ của người đó vẫn ghi ARTIST
      → họ vẫn đăng bài được cho tới khi token hết hạn.
    - Project này chỉ để email trong token. Role được đọc lại từ MySQL ở mỗi request
      (xem CustomUserDetailsService) → đổi role là có hiệu lực ngay ở request tiếp theo.

    -----------------------------------------------------
    Vì sao cần thêm jti (mã phiên)?
    -----------------------------------------------------
    - JWT bình thường có 1 nhược điểm: đã phát ra thì không thu hồi được cho tới khi hết hạn.
    - Project lưu jti vào Redis với key session:{email} (xem SessionService).
      Mỗi request, filter so jti trong token với giá trị trong Redis:
        + Khớp → token còn hiệu lực.
        + Không khớp hoặc key không tồn tại → token đã bị thu hồi → 401.
    - Muốn "đá" 1 người ra khỏi hệ thống, chỉ cần xóa hoặc ghi đè key Redis đó.
    - Key Redis sống đúng bằng hạn token (cùng đọc app.jwt.expiration-ms),
      token hết hạn thì key cũng tự biến mất.
*/
@Component
public class JwtUtil {

    /*
        2 thuộc tính, gán 1 lần trong constructor, không đổi (final):
        - key         : khóa bí mật dùng để ký và kiểm tra chữ ký.
        - expirationMs: thời gian sống của token, đơn vị mili giây (1 giây = 1000 ms).
    */
    private final SecretKey key;
    private final long expirationMs;

    /*
        -----------------------------------------------------
        Constructor: đọc cấu hình và tạo khóa
        -----------------------------------------------------
        @Value("${app.jwt.secret}"): Spring đọc giá trị của key app.jwt.secret
        trong application.properties rồi truyền vào tham số secret.
        Tương tự @Value("${app.jwt.expiration-ms}") cho thời hạn token.
        Thiếu 1 trong 2 dòng cấu hình này → ứng dụng không khởi động được (báo lỗi ngay).

        Constructor chỉ chạy 1 lần lúc khởi động (JwtUtil là bean duy nhất),
        nên khóa được tạo 1 lần rồi dùng lại cho mọi request.
    */
    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expiration-ms}") long expirationMs) {
        /*
            secret.getBytes(UTF_8): chuyển chuỗi secret thành mảng byte.

            Keys.hmacShaKeyFor(...): biến mảng byte thành khóa HMAC mà thư viện jjwt dùng được.
            - Secret phải dài ít nhất 32 byte (256 bit), VD 32 ký tự chữ thường không dấu.
              Ngắn hơn → jjwt ném WeakKeyException ngay lúc khởi động → ứng dụng không chạy.
              (Quy định bảo mật: khóa ngắn quá thì dễ bị dò ra.)
            - Độ dài secret còn quyết định thuật toán ký (xem generateToken).
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
        2. Kiểm tra tài khoản đang ACTIVE và được phép vào web (ACCEPT).
        3. Gọi SessionService.createSession(email) → sinh UUID mới và ghi vào Redis
           (ghi đè UUID cũ → thiết bị đăng nhập trước đó bị đá ra).
        Token trả về cho frontend trong LoginResponse.

        -----------------------------------------------------
        Đọc từng dòng (cú pháp builder: gọi nối tiếp, cuối cùng compact() tạo ra kết quả)
        -----------------------------------------------------
        Jwts.builder()            → bắt đầu tạo 1 token
        .subject(user.getEmail()) → claim "sub" = email
        .id(sessionId)            → claim "jti" = mã phiên (UUID vừa lưu vào Redis)
        .issuedAt(new Date())     → claim "iat" = bây giờ
                                    (new Date() = thời điểm hiện tại; jjwt dùng kiểu Date cũ của Java)
        .expiration(new Date(System.currentTimeMillis() + expirationMs))
                                  → claim "exp" = bây giờ + thời hạn
                                    System.currentTimeMillis(): số mili giây từ 01/01/1970 tới hiện tại
        .signWith(key)            → ký bằng khóa bí mật. jjwt tự chọn thuật toán theo độ dài khóa:
                                    32 đến 47 byte → HS256, 48 đến 63 byte → HS384, từ 64 byte → HS512
        .compact()                → ghép 3 phần thành chuỗi "header.payload.signature"
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

        Jwts.parser()             → tạo bộ đọc token
        .verifyWith(key)          → "hãy kiểm tra chữ ký bằng khóa này"
        .build()                  → hoàn tất bộ đọc
        .parseSignedClaims(token) → đọc token: tách 3 phần, tính lại chữ ký và so,
                                    kiểm tra exp. Có vấn đề gì là ném exception.

        Các trường hợp ném exception (đều bị bắt → trả false):
            Token hết hạn (exp đã qua)           → ExpiredJwtException
            Payload bị sửa, ký bằng secret khác  → SignatureException
            Chuỗi không đúng dạng JWT (VD "abc") → MalformedJwtException
            Chuỗi rỗng                           → IllegalArgumentException
        Không cần phân biệt lý do: sai kiểu nào cũng là "vé không hợp lệ".

        try/catch biến "ném lỗi" thành true/false cho dễ dùng: filter chỉ cần viết
            if (jwtUtil.validateToken(token)) { ... }
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

        .getPayload()  → phần payload (danh sách claim)
        .getSubject()  → claim "sub", tức email

        Chỉ gọi hàm này sau khi validateToken() đã trả về true.
        Nếu gọi với token sai, hàm sẽ ném exception thay vì trả về null.

        Ghi chú: mỗi request, filter gọi validateToken, getEmailFromToken, getSessionIdFromToken
        → token được đọc và kiểm tra chữ ký 3 lần. Có thể gộp thành 1 lần đọc,
        nhưng việc này rất nhanh (vài micro giây) nên giữ 3 hàm riêng cho dễ hiểu.
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

        .getId() → claim "jti", tức mã phiên (UUID).

        JwtAuthenticationFilter dùng giá trị này để so với Redis (key session:{email}).
        Giống getEmailFromToken, chỉ gọi sau khi validateToken() trả về true.
        Token cũ không có jti → trả null → SessionService.isValidSession coi là không hợp lệ.
    */
    public String getSessionIdFromToken(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token)
                .getPayload()
                .getId();
    }
}