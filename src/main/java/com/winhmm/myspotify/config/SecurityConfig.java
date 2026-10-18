package com.winhmm.myspotify.config;

import com.winhmm.myspotify.security.CustomUserDetailsService;
import com.winhmm.myspotify.security.JwtAuthenticationFilter;
import com.winhmm.myspotify.security.JwtUtil;
import com.winhmm.myspotify.security.RateLimitFilter;
import com.winhmm.myspotify.service.RateLimitService;
import com.winhmm.myspotify.service.SessionService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

/*
    =====================================================
    SECURITYCONFIG - CẤU HÌNH BẢO MẬT CHO TOÀN BỘ ỨNG DỤNG
    =====================================================

    Class này giống "bảng nội quy" của tòa nhà. Nó quy định:
    1. Thứ tự các filter: ai kiểm tra token, ai đếm số lượt gọi API.
    2. URL nào ai cũng vào được, URL nào phải đăng nhập.
    3. Trả lỗi gì khi bị chặn: 401 hay 403.

    -----------------------------------------------------
    Các annotation trên class
    -----------------------------------------------------
    - @Configuration: báo cho Spring biết class này chứa cấu hình,
      các hàm có @Bean sẽ được Spring gọi khi khởi động để tạo đối tượng.
    - @EnableWebSecurity: bật Spring Security cho các request web.
    - @EnableMethodSecurity: bật @PreAuthorize trên Controller và Service.
      Thiếu annotation này thì mọi @PreAuthorize("hasRole(...)") bị bỏ qua
      → tài khoản USER cũng gọi được API của Admin.

    -----------------------------------------------------
    Đường đi của 1 request (rút gọn)
    -----------------------------------------------------
    Request
      → CorsFilter                  : kiểm tra domain frontend có được phép gọi không
      → JwtAuthenticationFilter     : dán nhãn "request này của ai"
      → RateLimitFilter             : đếm số lượt gọi, vượt giới hạn → 429
      → AnonymousAuthenticationFilter : chưa có nhãn → gắn nhãn "khách" (anonymous)
      → ExceptionTranslationFilter  : chuyển lỗi bảo mật thành response 401 hoặc 403
      → AuthorizationFilter         : kiểm tra luật URL (permitAll hay authenticated)
      → DispatcherServlet → @PreAuthorize → Controller → Service

    -----------------------------------------------------
    Phân biệt 401 và 403
    -----------------------------------------------------
    - 401 Unauthorized: "Tôi không biết bạn là ai."
      Request là "khách" nhưng gọi API cần đăng nhập.
    - 403 Forbidden: "Tôi biết bạn là ai, nhưng bạn không được vào đây."
      Đã đăng nhập nhưng role không đủ quyền.
    ExceptionTranslationFilter quyết định: khi bị từ chối, nếu request là "khách" → 401,
    nếu đã đăng nhập → 403.
*/
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final SessionService sessionService;
    private final RateLimitService rateLimitService;

    public SecurityConfig(JwtUtil jwtUtil,
                          CustomUserDetailsService userDetailsService,
                          SessionService sessionService,
                          RateLimitService rateLimitService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.sessionService = sessionService;
        this.rateLimitService = rateLimitService;
    }

    /*
        =====================================================
        CẤU HÌNH CHUỖI FILTER BẢO MẬT
        =====================================================

        @Bean: Spring gọi hàm này 1 lần khi khởi động và dùng kết quả
        (SecurityFilterChain) để xử lý mọi request.

        HttpSecurity là đối tượng để cấu hình, mỗi hàm .xxx() bật hoặc tắt 1 tính năng.
        Cú pháp s -> s.xxx() là lambda: "nhận đối tượng cấu hình s và gọi hàm xxx trên nó".
    */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                /*
                    -----------------------------------------------------
                    Tắt CSRF
                    -----------------------------------------------------
                    CSRF là kiểu tấn công lợi dụng việc trình duyệt tự động gửi cookie.
                    Ví dụ: bạn đang đăng nhập ngân hàng (có cookie), vào 1 trang web độc hại,
                    trang đó tự gửi request chuyển tiền tới ngân hàng, trình duyệt tự kèm cookie
                    → ngân hàng tưởng là bạn gửi.

                    Project này gửi token qua header Authorization.
                    Trình duyệt không bao giờ tự gắn header này vào request,
                    nên trang web độc hại không giả mạo được → tắt CSRF là an toàn.

                    Nếu sau này đổi sang lưu token trong cookie thì phải xem xét bật lại.
                */
                .csrf(csrf -> csrf.disable())
                /*
                    -----------------------------------------------------
                    Bật CORS
                    -----------------------------------------------------
                    Customizer.withDefaults(): Spring tự tìm bean CorsConfigurationSource
                    (đã khai báo trong CorsConfig) và dùng cấu hình đó.
                */
                .cors(Customizer.withDefaults())
                /*
                    -----------------------------------------------------
                    Không dùng HttpSession
                    -----------------------------------------------------
                    STATELESS: Spring không tạo HttpSession và không gửi cookie JSESSIONID.
                    → Server không tự nhớ ai đã đăng nhập giữa các request.
                    → Mỗi request phải tự mang token theo.

                    Project vẫn lưu mã phiên trong Redis, nhưng đó là do mình tự quản lý
                    để thu hồi token, hoàn toàn tách biệt với HttpSession của Spring.
                */
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                /*
                    -----------------------------------------------------
                    Luật phân quyền theo URL
                    -----------------------------------------------------
                    Spring kiểm tra từ trên xuống dưới, gặp luật đầu tiên khớp thì dùng luật đó.
                    Vì vậy luật cụ thể phải đặt trước, luật chung (anyRequest) đặt cuối cùng.

                    - permitAll(): ai cũng vào được, kể cả "khách".
                    - authenticated(): phải đăng nhập (có nhãn), chưa quan tâm role.

                    Luật 1: mọi request OPTIONS đều được phép.
                    Request OPTIONS là "preflight": trước khi gửi request thật có header Authorization,
                    trình duyệt tự gửi 1 request OPTIONS để hỏi server "tôi có được gửi không?".
                    Request OPTIONS không mang token, nên nếu chặn thì frontend luôn bị lỗi CORS.

                    Luật 2: các URL công khai, không cần token:
                    - /api/auth/**: đăng ký, đăng nhập, gửi OTP, quên mật khẩu.
                      Người dùng chưa có token mới cần gọi các API này.
                    - /uploads/**: file nhạc và ảnh. Thẻ audio và img của HTML
                      không gửi được header Authorization, nên phải để công khai.
                    - /actuator/health: kiểm tra server còn chạy hay không.
                    - /swagger-ui/**, /swagger-ui.html, /v3/api-docs/**: trang tài liệu API.
                    - /error: trang lỗi mặc định của Spring. Nếu chặn trang này,
                      mọi lỗi (400, 404, 500...) đều bị biến thành 401, rất khó debug.

                    Luật 3: mọi URL còn lại phải đăng nhập.
                    Ở đây chỉ kiểm tra "đã đăng nhập chưa". Kiểm tra role (USER, ARTIST, ADMIN)
                    được làm riêng bằng @PreAuthorize trên từng Controller.
                */
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**", "/uploads/**", "/actuator/health",
                                "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
                                "/error").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        /*
                            -----------------------------------------------------
                            Xử lý lỗi 401: chưa đăng nhập
                            -----------------------------------------------------
                            Được gọi khi request là "khách" nhưng gọi API cần đăng nhập.
                            Nguyên nhân có thể là:
                            - Không gửi token.
                            - Token sai chữ ký hoặc hết hạn.
                            - Phiên đã bị thu hồi (đăng nhập nơi khác, đăng xuất, đổi mật khẩu...).
                            - Tài khoản không còn ACTIVE.

                            Tham số của lambda:
                            - request: request bị chặn, dùng để đọc giấy nhắn.
                            - response: dùng để ghi lỗi trả về.
                            - authException: lỗi Spring tạo ra (không dùng tới).

                            Cách chọn message:
                            - Có giấy nhắn SESSION_ERROR (do JwtAuthenticationFilter để lại)
                              → trả "Your session has ended...", để frontend báo người dùng
                              biết vì sao bị đăng xuất.
                            - Không có → trả message chung "Unauthorized, please login".
                        */
                        .authenticationEntryPoint((request, response, authException) -> {
                            Object sessionError = request.getAttribute(JwtAuthenticationFilter.SESSION_ERROR);
                            String message = (sessionError != null)
                                    ? sessionError.toString()
                                    : "Unauthorized, please login";
                            writeJsonError(response, 401, message);
                        })
                        /*
                            -----------------------------------------------------
                            Xử lý lỗi 403: không đủ quyền
                            -----------------------------------------------------
                            Được gọi khi đã đăng nhập nhưng role không phù hợp.
                            Ví dụ: tài khoản USER gọi GET /api/admin/users,
                            Controller có @PreAuthorize("hasRole('ADMIN')") → bị từ chối → 403.

                            Lưu ý: nếu GlobalExceptionHandler có hàm bắt Exception.class,
                            lỗi từ @PreAuthorize có thể bị hàm đó bắt trước và trả 500.
                            Khi đó cần thêm 1 handler riêng cho AccessDeniedException.
                        */
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeJsonError(response, 403, "You do not have permission to access this resource"))
                )
                /*
                    -----------------------------------------------------
                    Gắn filter JWT
                    -----------------------------------------------------
                    addFilterBefore(filterMới, FilterMốc.class): đặt filter mới đứng ngay trước filter mốc.

                    UsernamePasswordAuthenticationFilter là filter đăng nhập bằng form có sẵn của Spring.
                    Project không dùng nó, chỉ mượn làm mốc vị trí.
                    Đặt filter JWT ở đây để đảm bảo nhãn được dán xong trước khi
                    các bước kiểm tra quyền (AuthorizationFilter, @PreAuthorize) chạy.

                    Filter được tạo bằng new (không phải @Component), xem lý do
                    trong comment của JwtAuthenticationFilter.
                */
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil, userDetailsService, sessionService),
                        UsernamePasswordAuthenticationFilter.class)

                /*
                    -----------------------------------------------------
                    Gắn filter giới hạn số lượt gọi (rate limit)
                    -----------------------------------------------------
                    addFilterAfter: đặt RateLimitFilter đứng ngay sau JwtAuthenticationFilter.

                    Vì sao phải đứng sau filter JWT?
                    - Lúc này đã biết request của ai → đếm số lượt theo email.
                    - Đếm theo email công bằng hơn đếm theo IP, vì nhiều người
                      có thể dùng chung 1 IP (ví dụ cùng wifi trường học).
                    - Request của "khách" (chưa đăng nhập) thì đếm theo IP.

                    Giới hạn hiện tại:
                    - /api/auth/**: 10 lượt mỗi 60 giây, đếm theo IP.
                    - Các API khác: 100 lượt mỗi 60 giây, đếm theo email (hoặc IP nếu là khách).
                    - Vượt giới hạn → 429 Too Many Requests.
                */
                .addFilterAfter(new RateLimitFilter(rateLimitService),
                        JwtAuthenticationFilter.class);

        return http.build();
    }

    /*
         =====================================================
         GHI LỖI DẠNG JSON
         =====================================================
     */
    private void writeJsonError(HttpServletResponse response, int status,
                                String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}