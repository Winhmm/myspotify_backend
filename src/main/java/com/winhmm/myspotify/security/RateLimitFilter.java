package com.winhmm.myspotify.security;

import com.winhmm.myspotify.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/*
    =====================================================
    RATELIMITFILTER - GIỚI HẠN SỐ REQUEST VÀO BACKEND
    =====================================================

    (Filter là gì, OncePerRequestFilter: xem JwtAuthenticationFilter.
     Thứ tự đọc package security: xem JwtUtil.)

    Rate limit = giới hạn tốc độ: mỗi người chỉ được gửi tối đa N request trong 1 khoảng thời gian.
    Chống: dò mật khẩu (thử hàng nghìn mật khẩu), spam gửi OTP (tốn email), gọi API liên tục làm server quá tải.

    Chạy ngay sau JwtAuthenticationFilter ở mỗi request (thứ tự được đặt trong SecurityConfig):
        .addFilterAfter(new RateLimitFilter(rateLimitService), JwtAuthenticationFilter.class)
    Phần đếm và quyết định cho qua hay chặn nằm trong RateLimitService.

    -----------------------------------------------------
    Đếm theo gì?
    -----------------------------------------------------
    - /api/auth/** (đăng nhập, đăng ký, OTP, quên mật khẩu):
      đếm theo IP, giới hạn thấp (10 request mỗi 60 giây)
      → chống dò mật khẩu, chống spam email OTP.
      Đếm theo IP vì lúc này người gọi chưa đăng nhập, chưa biết là ai.
    - Các API khác (giới hạn 100 request mỗi 60 giây):
        + Đã đăng nhập → đếm theo email, mỗi tài khoản 1 bộ đếm riêng.
          Công bằng hơn đếm theo IP, vì nhiều người có thể dùng chung 1 IP
          (cùng wifi trường học, quán cà phê).
        + Chưa đăng nhập → đếm theo IP.

    Các con số đọc từ application.properties (RateLimitService):
        app.rate-limit.window-seconds=60     độ dài 1 "cửa sổ" đếm (giây)
        app.rate-limit.user-requests=100     giới hạn API thường
        app.rate-limit.auth-requests=10      giới hạn API /api/auth/...

    Vượt giới hạn → 429 Too Many Requests, kèm header Retry-After (số giây phải chờ).

    -----------------------------------------------------
    Cách đếm trong Redis (RateLimitService.tryAcquire)
    -----------------------------------------------------
    Mỗi bộ đếm là 1 key Redis chứa 1 con số:
        Request đầu tiên : INCR key → 1, đồng thời đặt hạn 60 giây cho key (EXPIRE)
        Request thứ 2    : INCR key → 2
        ...
        Request thứ 101  : INCR key → 101 > 100 → chặn
        Hết 60 giây      : Redis tự xóa key → request sau bắt đầu đếm lại từ 1
    INCR (tăng 1) chạy nguyên khối trong Redis: 2 request tới cùng lúc vẫn đếm đúng, không bị trùng số.

    Cách này gọi là "cửa sổ cố định" (fixed window): đơn giản, chỉ 1 key cho mỗi người.
    Đánh đổi: ai gửi 100 request cuối cửa sổ cũ và 100 request đầu cửa sổ mới
    thì trong vài giây gửi được gần 200. Với dự án này chấp nhận được.

    -----------------------------------------------------
    Vì sao phải chạy sau JwtAuthenticationFilter?
    -----------------------------------------------------
    Để đếm theo email, filter cần biết request của ai.
    Thông tin đó chỉ có sau khi JwtAuthenticationFilter đọc token
    và đặt người dùng vào SecurityContext.

    -----------------------------------------------------
    Vì sao không gắn @Component?
    -----------------------------------------------------
    Filter này được tạo bằng new trong SecurityConfig.
    Nếu gắn @Component, Spring Boot sẽ tự đăng ký thêm 1 lần nữa ở tầng servlet,
    bên ngoài Spring Security. Bản thứ hai đó chạy trước JWT filter,
    nên không biết request của tài khoản nào → mọi request bị đếm theo IP, và bị đếm 2 lần.

    -----------------------------------------------------
    Khi Redis bị lỗi
    -----------------------------------------------------
    Cho request đi qua (fail-open), không chặn.
    Lý do: rate limit là lớp bảo vệ phụ. Redis sập mà chặn hết request
    thì cả hệ thống ngừng hoạt động, thiệt hại lớn hơn việc tạm thời không giới hạn.
    (Ngược với JwtAuthenticationFilter: xác thực là lớp bảo vệ chính, Redis lỗi thì không cho vào.)
*/
public class RateLimitFilter extends OncePerRequestFilter {
    private final RateLimitService rateLimitService;

    public RateLimitFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    /*
        =====================================================
        NHỮNG REQUEST KHÔNG ĐẾM
        =====================================================

        shouldNotFilter trả true → filter bỏ qua request đó, không chạy doFilterInternal.
        (Hàm có sẵn trong OncePerRequestFilter, mặc định trả false = lọc mọi request.
         @Override để viết lại theo ý mình.)

        - OPTIONS: trình duyệt tự gửi để hỏi CORS (preflight), không phải người dùng gọi.
          VD frontend ở localhost:5173 gọi API ở 157.66.100.87:8080 (khác địa chỉ)
          → trước mỗi request thật, trình duyệt tự gửi 1 request OPTIONS hỏi "có cho phép không?".
          Đếm cả loại này thì mỗi lần bấm tốn 2 lượt.
        - /uploads/: file nhạc và ảnh. Phát và tua 1 bài gửi rất nhiều request nhỏ
          (mỗi lần tua là 1 request lấy 1 đoạn file), đếm vào sẽ nhanh chóng vượt giới hạn.
        - /actuator/: kiểm tra server còn sống không.
        - /swagger-ui, /v3/api-docs: trang tài liệu API, tải nhiều file tĩnh.

        - "OPTIONS".equalsIgnoreCase(request.getMethod()): so chuỗi không phân biệt hoa thường.
          Viết "OPTIONS".equalsIgnoreCase(...) (chuỗi cố định đứng trước) thay vì
          request.getMethod().equalsIgnoreCase("OPTIONS"): lỡ getMethod() là null cũng không lỗi.
        - getRequestURI(): đường dẫn không gồm phần ?... phía sau.
          VD /api/songs?page=0 → "/api/songs".
        - startsWith: đường dẫn bắt đầu bằng ... (VD "/uploads/songs/audio/3f2a.mp3" bắt đầu bằng "/uploads/").
        - || : "hoặc", chỉ cần khớp 1 điều kiện là bỏ qua.
    */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || path.startsWith("/uploads/")
                || path.startsWith("/actuator/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }

    /*
        =====================================================
        ĐẾM VÀ QUYẾT ĐỊNH CHO QUA HAY CHẶN
        =====================================================
    */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        /*
            -----------------------------------------------------
            Bước 1: Lấy đường dẫn và IP của người gửi
            -----------------------------------------------------

            getRemoteAddr(): IP của máy kết nối trực tiếp tới backend.

            Lưu ý: hiện tại frontend gọi thẳng vào cổng 8080 nên đây là IP thật của người dùng.
            Nếu sau này đặt Nginx phía trước backend, getRemoteAddr() sẽ luôn là IP của Nginx
            (127.0.0.1) → mọi người dùng chung 1 bộ đếm. Khi đó phải đọc IP thật từ
            header X-Forwarded-For do Nginx gửi kèm.
            (Chạy thử trên máy mình: IP thường là "0:0:0:0:0:0:0:1" hoặc "127.0.0.1", nghĩa là chính máy này.)
        */
        String path = request.getRequestURI();
        String ip = request.getRemoteAddr();

        /*
            Khai báo trước, gán giá trị ở bước 2 (mỗi nhánh if/else gán 1 lần).
            Java bắt buộc mọi nhánh đều phải gán, nếu không sẽ báo lỗi biên dịch khi dùng ở bước 3.
        */
        String key;
        int limit;

        /*
            -----------------------------------------------------
            Bước 2: Chọn bộ đếm (key Redis) và giới hạn
            -----------------------------------------------------

            - /api/auth/... → key rate:auth:<IP>, giới hạn auth-requests (10).
            - Đã đăng nhập  → key rate:user:<email>, giới hạn user-requests (100).
            - Chưa đăng nhập → key rate:ip:<IP>, giới hạn user-requests (100).

            VD key trong Redis:
                rate:auth:113.161.20.5       (máy có IP 113.161.20.5 gọi /api/auth/login)
                rate:user:a@gmail.com        (tài khoản a@gmail.com gọi API thường)
                rate:ip:113.161.20.5         (chưa đăng nhập, gọi API thường → sẽ nhận 401,
                                              nhưng vẫn bị đếm để chống gọi liên tục)

            Kiểm tra "đã đăng nhập" gồm 3 điều kiện:
            - auth != null: có thông tin xác thực.
            - auth.isAuthenticated(): đã được xác thực.
            - Không phải AnonymousAuthenticationToken: Spring gắn nhãn "khách" (anonymous)
              cho request chưa đăng nhập, nhãn này cũng có isAuthenticated() = true,
              nên phải loại riêng.
            Lưu ý: ở vị trí filter hiện tại, nhãn "khách" thường chưa được gắn,
            điều kiện thứ ba là để an toàn nếu sau này thứ tự filter thay đổi.

            auth.getName(): email (JwtAuthenticationFilter dán nhãn bằng UserDetails có username = email).
            !(auth instanceof AnonymousAuthenticationToken): dấu ! đảo ngược → "KHÔNG phải nhãn khách".
        */
        if(path.startsWith("/api/auth/")) {
            key = "rate:auth:" + ip;
            limit = rateLimitService.getAuthRequests();
        } else {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if(auth != null && auth.isAuthenticated()
                    && !(auth instanceof AnonymousAuthenticationToken)) {
                key = "rate:user:" + auth.getName();
            } else {
                key = "rate:ip:" + ip;
            }
            limit = rateLimitService.getUserRequests();
        }

        /*
            -----------------------------------------------------
            Bước 3: Đếm request
            -----------------------------------------------------
            rateLimitService.tryAcquire(key, limit): tăng bộ đếm thêm 1 rồi so với giới hạn.
            - true : chưa vượt → cho qua.
            - false: đã vượt → chặn.
            (Cách đếm: xem phần đầu file.)

            Redis lỗi (mất kết nối...) → cho qua (fail-open), xem giải thích ở đầu file.
            Khai báo boolean allowed ở ngoài try để dùng được ở bước 4
            (biến khai báo bên trong { } của try chỉ sống trong cặp ngoặc đó).
        */
        boolean allowed;
        try {
            allowed = rateLimitService.tryAcquire(key, limit);
        } catch (Exception e) {
            allowed = true;
        }

        /*
            -----------------------------------------------------
            Bước 4: Vượt giới hạn → trả 429 và dừng
            -----------------------------------------------------

            - getRetryAfterSeconds(key): đọc thời gian còn lại của key trong Redis (TTL),
              VD còn 37 giây nữa bộ đếm mới bị xóa → Retry-After: 37.
            - setStatus(429): Too Many Requests.
            - Header Retry-After: số giây còn lại đến khi bộ đếm được xóa.
              Frontend đọc được header này nhờ CorsConfig đã expose "Retry-After"
              (mặc định trình duyệt giấu các header lạ khỏi JavaScript khi gọi khác địa chỉ).
            - setContentType("application/json;charset=UTF-8"): báo frontend body là JSON.
            - Body JSON cùng định dạng với các lỗi khác: { "message": "..." }.
              Tự ghép chuỗi JSON bằng tay: \" là dấu ngoặc kép nằm BÊN TRONG chuỗi Java.
                  "{\"message\":\"Too many requests. Please try again in " + 37 + " seconds\"}"
                  → {"message":"Too many requests. Please try again in 37 seconds"}

            Phải tự ghi response ở đây vì lỗi xảy ra trong filter,
            trước khi tới Controller, nên GlobalExceptionHandler không xử lý được.

            return: không gọi filterChain.doFilter → request không đi tiếp vào Controller.
        */
        if(!allowed) {
            long retryAfter = rateLimitService.getRetryAfterSeconds(key);

            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"message\":\"Too many requests. Please try again in "
                            + retryAfter + " seconds\"}"
            );
            /*
                Không cho request vào Controller
            */
            return;
        }

        /*
            -----------------------------------------------------
            Bước 5: Chưa vượt giới hạn → cho đi tiếp
            -----------------------------------------------------
            Request đi tiếp tới các filter kiểm tra quyền của Spring Security, rồi tới Controller.
        */
        filterChain.doFilter(request, response);
    }
}