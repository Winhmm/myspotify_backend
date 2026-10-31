package com.winhmm.myspotify.config;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    Class của project:
    - CustomUserDetailsService: tìm User trong MySQL theo email, đổi sang dạng Spring Security hiểu.
    - JwtAuthenticationFilter: filter đọc token và "dán nhãn" request này của ai.
    - JwtUtil: tạo và kiểm tra JWT.
    - RateLimitFilter: filter đếm số lượt gọi API.
    - RateLimitService: phần đếm bằng Redis mà RateLimitFilter dùng.
    - SessionService: quản lý mã phiên đăng nhập trong Redis.

    Class của Java và Spring:
    - HttpServletResponse: đối tượng response, dùng để ghi lỗi trả về cho client.
    - Bean, Configuration: annotation khai báo cấu hình và tạo đối tượng.
    - HttpMethod: danh sách các method HTTP (GET, POST, OPTIONS...).
    - Customizer: tiện ích cấu hình, Customizer.withDefaults() = "dùng cấu hình mặc định".
    - EnableMethodSecurity: bật @PreAuthorize.
    - HttpSecurity: đối tượng dùng để cấu hình bảo mật cho các request web.
    - EnableWebSecurity: bật Spring Security cho web.
    - SessionCreationPolicy: các chế độ dùng HttpSession (ở đây chọn STATELESS).
    - BCryptPasswordEncoder, PasswordEncoder: băm và so sánh mật khẩu.
    - SecurityFilterChain: chuỗi filter bảo mật, kết quả cuối cùng của việc cấu hình.
    - UsernamePasswordAuthenticationFilter: filter đăng nhập bằng form có sẵn của Spring,
      project chỉ mượn làm mốc vị trí để đặt filter JWT.
    - IOException: lỗi khi đọc ghi dữ liệu, có thể xảy ra lúc ghi response.
*/
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

    Class này giống "bảng nội quy" của 1 tòa nhà. Nó quy định:
    1. Thứ tự các filter: ai kiểm tra token, ai đếm số lượt gọi API.
    2. URL nào ai cũng vào được, URL nào phải đăng nhập.
    3. Trả lỗi gì khi bị chặn: 401 hay 403.
    4. Dùng thuật toán gì để băm mật khẩu (BCrypt).

    -----------------------------------------------------
    Filter là gì?
    -----------------------------------------------------
    Filter là "trạm kiểm soát" mà request phải đi qua trước khi tới Controller.
    Mỗi filter làm 1 việc, xong thì chuyển request cho filter tiếp theo
    (bằng lệnh filterChain.doFilter), hoặc chặn luôn và tự trả response.

    Spring Security thực chất là 1 chuỗi khoảng 15 filter xếp theo thứ tự.
    Class này dùng để bật, tắt, thêm filter vào chuỗi đó.

    -----------------------------------------------------
    Các annotation trên class
    -----------------------------------------------------
    - @Configuration: báo cho Spring biết class này chứa cấu hình,
      các hàm có @Bean sẽ được Spring gọi khi khởi động để tạo đối tượng.
    - @EnableWebSecurity: bật Spring Security cho các request web.
    - @EnableMethodSecurity: bật @PreAuthorize trên Controller và Service.
      Thiếu annotation này thì mọi @PreAuthorize("hasRole(...)") bị bỏ qua
      → tài khoản USER cũng gọi được API của Admin. Rất nguy hiểm.

    -----------------------------------------------------
    Đường đi của 1 request (rút gọn)
    -----------------------------------------------------
    Request
      → CorsFilter                  : kiểm tra origin của frontend, trả lời preflight
      → JwtAuthenticationFilter     : dán nhãn "request này của ai" (filter tự viết)
      → RateLimitFilter             : đếm số lượt gọi, vượt giới hạn → 429 (filter tự viết)
      → AnonymousAuthenticationFilter : chưa có nhãn → gắn nhãn "khách" (anonymous)
      → ExceptionTranslationFilter  : chuyển lỗi bảo mật thành response 401 hoặc 403
      → AuthorizationFilter         : kiểm tra luật URL (permitAll hay authenticated)
      → DispatcherServlet → @PreAuthorize → Controller → Service

    Ngoài các filter trên, Spring Security còn vài filter khác (ghi header bảo mật,
    dọn dẹp sau request...), không cần quan tâm khi mới học.

    "Nhãn" ở đây là đối tượng Authentication được đặt vào SecurityContextHolder.
    Có nhãn = đã đăng nhập. Trong Controller lấy ra bằng tham số Authentication authentication,
    authentication.getName() trả về email của người đang gọi API.

    -----------------------------------------------------
    Phân biệt 401 và 403
    -----------------------------------------------------
    - 401 Unauthorized: "Tôi không biết bạn là ai."
      Request là "khách" nhưng gọi API cần đăng nhập.
    - 403 Forbidden: "Tôi biết bạn là ai, nhưng bạn không được vào đây."
      Đã đăng nhập nhưng role không đủ quyền.

    Ví dụ dễ nhớ ở tòa nhà:
    - Không đeo thẻ nhân viên → bảo vệ chặn ở cổng → 401.
    - Có thẻ nhân viên nhưng đi vào phòng giám đốc → bị chặn ở cửa phòng → 403.

    ExceptionTranslationFilter quyết định: khi bị từ chối, nếu request là "khách" → 401,
    nếu đã đăng nhập → 403.
*/
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    /*
        -----------------------------------------------------
        Các dependency (đối tượng mà class này cần dùng)
        -----------------------------------------------------
        - private: chỉ dùng bên trong class này.
        - final: gán 1 lần trong constructor, sau đó không đổi được nữa
          → tránh vô tình gán nhầm giá trị khác.

        Vì sao SecurityConfig cần 4 đối tượng này?
        Vì 2 filter tự viết (JwtAuthenticationFilter, RateLimitFilter) được tạo bằng new
        ở bên dưới, mà filter lại cần các đối tượng này để làm việc,
        nên SecurityConfig nhận chúng từ Spring rồi truyền tiếp cho filter.
    */
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final SessionService sessionService;
    private final RateLimitService rateLimitService;

    /*
        -----------------------------------------------------
        Constructor - tiêm dependency qua constructor
        -----------------------------------------------------
        Khi tạo đối tượng SecurityConfig, Spring nhìn vào constructor, thấy cần 4 tham số,
        nên tự tìm trong container 4 đối tượng có kiểu tương ứng và truyền vào:
        - JwtUtil                  (có @Component)
        - CustomUserDetailsService (có @Service)
        - SessionService           (có @Service)
        - RateLimitService         (có @Service)

        Class chỉ có 1 constructor nên không cần ghi @Autowired, Spring tự hiểu.

        this.jwtUtil = jwtUtil:
        - this.jwtUtil là biến của class (khai báo ở trên).
        - jwtUtil (không có this) là tham số vừa được truyền vào.
        → Gán tham số vào biến của class để các hàm khác dùng được.
    */
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

        Tham số HttpSecurity http: do Spring tự tạo và truyền vào.
        Đây là đối tượng để cấu hình, mỗi hàm .xxx() bật, tắt hoặc chỉnh 1 tính năng.

        throws Exception: hàm http.build() ở cuối có thể ném lỗi,
        nên hàm này khai báo "có thể ném lỗi" để Spring tự xử lý.

        -----------------------------------------------------
        Cách đọc đoạn code bên dưới
        -----------------------------------------------------
        1. Nối hàm bằng dấu chấm:
           http.csrf(...).cors(...).sessionManagement(...)...
           Mỗi hàm cấu hình xong lại trả về chính đối tượng http,
           nên có thể gọi tiếp hàm sau. Viết xuống dòng cho dễ đọc, nhưng thực chất là 1 câu lệnh dài.

        2. Lambda: csrf -> csrf.disable()
           Đọc là: "nhận đối tượng cấu hình CSRF, đặt tên là csrf, rồi gọi csrf.disable() trên nó".
           Có thể hiểu lambda là 1 hàm viết gọn, không cần tên:
               tham_số -> việc_cần_làm
           Tên tham số tùy ý đặt (csrf, s, auth, ex...), chỉ cần dùng thống nhất.
           Từ Spring Security 7 (đi cùng Spring Boot 4), cách viết lambda này là cách viết chuẩn.
    */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                /*
                    -----------------------------------------------------
                    Tắt CSRF
                    -----------------------------------------------------
                    CSRF (Cross-Site Request Forgery - giả mạo request từ trang khác)
                    là kiểu tấn công lợi dụng việc trình duyệt tự động gửi cookie.

                    Ví dụ: bạn đang đăng nhập ngân hàng (trình duyệt đang giữ cookie đăng nhập),
                    rồi vào 1 trang web độc hại. Trang đó tự gửi request chuyển tiền tới ngân hàng,
                    trình duyệt tự kèm cookie → ngân hàng tưởng chính bạn gửi.

                    Mặc định Spring Security bật chống CSRF: mọi request POST, PUT, DELETE
                    phải kèm 1 mã CSRF bí mật, thiếu là bị chặn 403.

                    Project này gửi token qua header Authorization.
                    Trình duyệt không bao giờ tự gắn header này vào request,
                    nên trang web độc hại không giả mạo được → tắt CSRF là an toàn.
                    Nếu không tắt, mọi request POST, PUT, DELETE từ frontend và Postman đều bị 403.

                    Nếu sau này đổi sang lưu token trong cookie (HttpOnly) thì phải xem xét bật lại,
                    vì lúc đó trình duyệt sẽ tự gửi kèm cookie giống ví dụ ngân hàng ở trên.
                */
                .csrf(csrf -> csrf.disable())
                /*
                    -----------------------------------------------------
                    Bật CORS
                    -----------------------------------------------------
                    Customizer.withDefaults(): Spring tự tìm bean CorsConfigurationSource
                    (đã khai báo trong CorsConfig) và dùng cấu hình đó để tạo CorsFilter.

                    CorsFilter được đặt gần như đầu chuỗi, nên kể cả response lỗi 401, 403, 429
                    do các filter phía sau trả về cũng có header CORS
                    → frontend đọc được nội dung lỗi thay vì chỉ thấy "lỗi CORS".
                */
                .cors(Customizer.withDefaults())
                /*
                    -----------------------------------------------------
                    Không dùng HttpSession
                    -----------------------------------------------------
                    HttpSession là cách các web truyền thống nhớ người đăng nhập:
                    server lưu thông tin trong RAM, gửi cho trình duyệt cookie JSESSIONID,
                    các request sau trình duyệt gửi lại cookie đó để server nhận ra.

                    STATELESS (không trạng thái): Spring không tạo HttpSession và không gửi cookie JSESSIONID.
                    → Server không tự nhớ ai đã đăng nhập giữa các request.
                    → Mỗi request phải tự mang token theo trong header Authorization.
                    → Mỗi request bắt đầu với SecurityContext trống,
                      JwtAuthenticationFilter đọc token và dán nhãn lại từ đầu,
                      xử lý xong request thì nhãn bị xóa.

                    Lợi ích: server không tốn RAM lưu session, chạy nhiều server cùng lúc cũng được.

                    Project vẫn lưu mã phiên trong Redis, nhưng đó là do mình tự quản lý
                    để thu hồi token (đăng xuất, đăng nhập nơi khác...),
                    hoàn toàn tách biệt với HttpSession của Spring.

                    s -> s.sessionCreationPolicy(...): s là đối tượng cấu hình session.
                */
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                /*
                    -----------------------------------------------------
                    Luật phân quyền theo URL
                    -----------------------------------------------------
                    auth là đối tượng để khai báo luật. Mỗi luật gồm 2 phần:
                    - requestMatchers(...): URL nào (và method nào) áp dụng luật này.
                    - permitAll() hoặc authenticated(): luật là gì.

                    Spring kiểm tra từ trên xuống dưới, gặp luật đầu tiên khớp thì dùng luật đó,
                    không xét các luật phía sau nữa.
                    Vì vậy luật cụ thể phải đặt trước, luật chung (anyRequest) đặt cuối cùng.
                    Nếu đặt anyRequest().authenticated() lên đầu, mọi URL kể cả đăng nhập đều bị khóa.

                    - permitAll(): ai cũng vào được, kể cả "khách".
                    - authenticated(): phải đăng nhập (có nhãn), chưa quan tâm role.

                    Ký hiệu trong mẫu URL:
                    - "**": khớp mọi thứ phía sau, kể cả nhiều cấp.
                      Ví dụ "/api/auth/**" khớp /api/auth/login, /api/auth/register/verify...

                    Luật 1: mọi request OPTIONS đều được phép.
                    Request OPTIONS là preflight: trước khi gửi request thật có header Authorization,
                    trình duyệt tự gửi 1 request OPTIONS (không có token) để hỏi
                    "tôi có được gửi không?" (xem giải thích trong CorsConfig).
                    Thực ra CorsFilter đã tự trả lời preflight hợp lệ và dừng luôn,
                    request không đi tới đây. Luật này là lớp phòng hờ cho các request OPTIONS khác,
                    để chúng không bị biến thành 401.

                    Luật 2: các URL công khai, không cần token:
                    - /api/auth/**: đăng ký, xác thực OTP, đăng nhập, quên mật khẩu.
                      Người dùng chưa có token mới cần gọi các API này,
                      bắt đăng nhập ở đây là vô lý (giống bắt có chìa khóa mới được lấy chìa khóa).
                    - /uploads/**: file nhạc, ảnh, video. Thẻ <audio>, <img>, <video> của HTML
                      không gửi được header Authorization, nên phải để công khai.
                    - /actuator/health: kiểm tra server còn chạy hay không, trả về {"status":"UP"}.
                      Chỉ mở đúng health, các endpoint actuator khác (nếu có) vẫn phải đăng nhập.
                    - /swagger-ui/**, /swagger-ui.html, /v3/api-docs/**: trang tài liệu API.
                    - /error: trang lỗi mặc định của Spring.
                      Khi có lỗi (ví dụ 404 không tìm thấy), Tomcat chuyển nội bộ request sang /error
                      để tạo nội dung lỗi, và Spring Security kiểm tra luật thêm 1 lần cho /error.
                      Nếu chặn /error, mọi lỗi (400, 404, 500...) của "khách" đều bị biến thành 401,
                      rất khó debug.

                    Luật 3: mọi URL còn lại phải đăng nhập.
                    Ở đây chỉ kiểm tra "đã đăng nhập chưa". Kiểm tra role (USER, ARTIST, ADMIN)
                    được làm riêng bằng @PreAuthorize trên từng Controller,
                    ví dụ AdminController có @PreAuthorize("hasRole('ADMIN')").

                    Một hệ quả thú vị: gọi 1 URL không tồn tại (ví dụ GET /api/abc)
                    - Không có token → 401, vì luật 3 chặn trước khi Spring biết URL đó không tồn tại.
                    - Có token hợp lệ → 404.
                */
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**", "/uploads/**", "/actuator/health",
                                "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
                                "/error").permitAll()
                        .anyRequest().authenticated()
                )
                /*
                    -----------------------------------------------------
                    Tự quy định nội dung lỗi 401 và 403
                    -----------------------------------------------------
                    Khi AuthorizationFilter từ chối 1 request, nó ném ra lỗi bảo mật.
                    ExceptionTranslationFilter (đứng ngay trước) bắt lỗi đó:
                    - Request là "khách"   → gọi authenticationEntryPoint → 401.
                    - Request đã đăng nhập → gọi accessDeniedHandler     → 403.

                    Mặc định Spring trả lỗi không có nội dung rõ ràng.
                    Ở đây tự viết để trả JSON có dạng {"message": "..."},
                    giống hệt dạng lỗi của GlobalExceptionHandler
                    → frontend chỉ cần đọc error.response.data.message cho mọi loại lỗi.

                    ex -> ex.xxx(...).yyy(...): ex là đối tượng cấu hình xử lý lỗi.
                */
                .exceptionHandling(ex -> ex
                        /*
                            -----------------------------------------------------
                            Xử lý lỗi 401: chưa đăng nhập
                            -----------------------------------------------------
                            Được gọi khi request là "khách" nhưng gọi API cần đăng nhập.
                            Request bị coi là "khách" khi JwtAuthenticationFilter không dán nhãn, vì:
                            - Không gửi token.
                            - Token sai chữ ký, sai định dạng hoặc hết hạn.
                            - Phiên đã bị thu hồi (đăng nhập nơi khác, đăng xuất, đổi mật khẩu...).
                            - Tài khoản không ở trạng thái ACTIVE (chưa xác thực OTP, bị khóa, đã xóa).
                            - Tài khoản bị chủ dự án đổi access_status sang REFUSE.

                            Lambda có 3 tham số, Spring tự truyền vào:
                            - request: request bị chặn, dùng để đọc giấy nhắn.
                            - response: dùng để ghi lỗi trả về.
                            - authException: lỗi Spring tạo ra (không dùng tới).
                            Khi lambda có nhiều tham số thì đặt chúng trong ngoặc: (a, b, c) -> {...}
                            Khi thân lambda có nhiều dòng thì đặt trong dấu { }.

                            -----------------------------------------------------
                            Giấy nhắn SESSION_ERROR
                            -----------------------------------------------------
                            request.getAttribute(tên): đọc 1 "giấy nhắn" được gắn vào request.
                            Giấy nhắn chỉ tồn tại trong suốt 1 request, request xong là mất.

                            JwtAuthenticationFilter.SESSION_ERROR là hằng số có giá trị "sessionError".
                            Khi token đúng nhưng mã phiên trong token không khớp với Redis
                            (đã bị đăng nhập ở thiết bị khác), JwtAuthenticationFilter để lại giấy nhắn:
                                request.setAttribute(SESSION_ERROR, "Your session has ended...")
                            Dùng hằng số chung thay vì gõ tay chuỗi "sessionError" ở 2 nơi
                            → tránh gõ sai chính tả làm 2 bên không "nói chuyện" được với nhau.

                            Object: kiểu dữ liệu chung nhất của Java, vì getAttribute có thể trả về
                            bất kỳ loại dữ liệu gì. Không có giấy nhắn thì trả về null.

                            -----------------------------------------------------
                            Cách chọn message (toán tử 3 ngôi)
                            -----------------------------------------------------
                            Cú pháp: điều_kiện ? giá_trị_nếu_đúng : giá_trị_nếu_sai

                            - Có giấy nhắn (sessionError != null)
                              → message = nội dung giấy nhắn "Your session has ended...",
                                để frontend báo cho người dùng biết vì sao bị đăng xuất.
                              .toString(): đổi Object về String.
                            - Không có giấy nhắn
                              → message chung "Unauthorized, please login".

                            Viết dài tương đương:
                                String message;
                                if (sessionError != null) {
                                    message = sessionError.toString();
                                } else {
                                    message = "Unauthorized, please login";
                                }
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
                            Được gọi khi request đã đăng nhập nhưng bị từ chối ở tầng filter.

                            Thân lambda chỉ có 1 dòng nên không cần dấu { }.

                            Khi nào handler này thật sự chạy?
                            Hiện tại luật URL ở trên chỉ có permitAll và authenticated,
                            mà người đã đăng nhập thì luôn qua được authenticated
                            → ở tầng filter gần như không bao giờ có lỗi 403.

                            Lỗi 403 thường gặp nhất là từ @PreAuthorize, ví dụ:
                            tài khoản USER gọi GET /api/admin/users,
                            AdminController có @PreAuthorize("hasRole('ADMIN')") → bị từ chối.
                            Lỗi này xảy ra bên trong Controller, nên được
                            GlobalExceptionHandler.handleAccessDenied bắt trước
                            (trả 403 với cùng message), không đi tới handler ở đây.

                            Vậy handler này để làm gì? Là lớp phòng hờ:
                            nếu sau này thêm luật kiểu .requestMatchers("/api/admin/**").hasRole("ADMIN")
                            ngay trong SecurityConfig, lỗi 403 sẽ do handler này trả về.
                        */
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeJsonError(response, 403, "You do not have permission to access this resource"))
                )
                /*
                    -----------------------------------------------------
                    Gắn filter JWT
                    -----------------------------------------------------
                    addFilterBefore(filterMới, FilterMốc.class): đặt filter mới đứng ngay trước filter mốc.

                    new JwtAuthenticationFilter(jwtUtil, userDetailsService, sessionService):
                    tự tạo đối tượng filter và truyền 3 dependency nó cần qua constructor.

                    UsernamePasswordAuthenticationFilter.class:
                    - .class nghĩa là "chính class đó" (không phải 1 đối tượng), dùng làm mốc vị trí.
                    - Đây là filter đăng nhập bằng form có sẵn của Spring.
                      Project không bật đăng nhập bằng form nên filter này không chạy,
                      chỉ mượn vị trí của nó làm mốc.
                    Vị trí này nằm trước các bước kiểm tra quyền (AuthorizationFilter, @PreAuthorize)
                    → đảm bảo nhãn được dán xong trước khi bị kiểm tra.

                    Vì sao filter được tạo bằng new mà không gắn @Component?
                    Nếu gắn @Component (tức là biến filter thành bean), Spring Boot sẽ tự đăng ký
                    filter đó thêm 1 lần nữa ở tầng Tomcat, bên ngoài chuỗi bảo mật
                    → filter nằm ở 2 nơi, dễ chạy sai thứ tự hoặc chạy 2 lần.
                    Tạo bằng new thì filter chỉ nằm đúng 1 chỗ trong chuỗi bảo mật.
                    (Chi tiết hơn xem comment trong JwtAuthenticationFilter.)
                */
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil, userDetailsService, sessionService),
                        UsernamePasswordAuthenticationFilter.class)

                /*
                    -----------------------------------------------------
                    Gắn filter giới hạn số lượt gọi (rate limit)
                    -----------------------------------------------------
                    addFilterAfter(filterMới, FilterMốc.class): đặt filter mới đứng ngay sau filter mốc.
                    Mốc ở đây là JwtAuthenticationFilter vừa được thêm ở trên
                    → RateLimitFilter chạy ngay sau khi nhãn được dán.

                    Vì sao phải đứng sau filter JWT?
                    - Lúc này đã biết request của ai → đếm số lượt theo email.
                    - Đếm theo email công bằng hơn đếm theo IP, vì nhiều người
                      có thể dùng chung 1 IP (ví dụ cùng wifi trường học).
                    - Request của "khách" (chưa đăng nhập) thì đếm theo IP.

                    Giới hạn hiện tại (đọc từ application.properties, key app.rate-limit.*):
                    - /api/auth/**: 10 lượt mỗi 60 giây, đếm theo IP
                      (chống dò mật khẩu và spam email OTP).
                    - Các API khác: 100 lượt mỗi 60 giây, đếm theo email (hoặc IP nếu là khách).
                    - Vượt giới hạn → 429 Too Many Requests, kèm header Retry-After.
                    - Không đếm: OPTIONS, /uploads/, /actuator/, Swagger (xem shouldNotFilter).
                */
                .addFilterAfter(new RateLimitFilter(rateLimitService),
                        JwtAuthenticationFilter.class);

        /*
            -----------------------------------------------------
            Đóng gói cấu hình
            -----------------------------------------------------
            http.build(): gom tất cả cấu hình ở trên thành 1 đối tượng SecurityFilterChain,
            bên trong là danh sách filter đã xếp đúng thứ tự.
            return: trả đối tượng đó cho Spring. Từ giờ mọi request đều đi qua chuỗi filter này.
        */
        return http.build();
    }

    /*
        =====================================================
        GHI LỖI DẠNG JSON
        =====================================================

        Hàm phụ dùng chung cho cả lỗi 401 và 403 ở trên, tránh viết lặp code.

        - private: chỉ dùng trong class này.
        - void: không trả về gì, chỉ ghi dữ liệu vào response.
        - Tham số:
          + response: đối tượng response để ghi lỗi.
          + status: mã lỗi HTTP (401 hoặc 403).
          + message: nội dung lỗi.
        - throws IOException: getWriter().write(...) có thể ném lỗi khi ghi dữ liệu
          (ví dụ client đã ngắt kết nối giữa chừng).

        Ví dụ response được tạo ra:
            HTTP/1.1 401
            Content-Type: application/json;charset=UTF-8

            {"message":"Unauthorized, please login"}
    */
    private void writeJsonError(HttpServletResponse response, int status,
                                String message) throws IOException {
        /*
            -----------------------------------------------------
            Từng dòng làm gì
            -----------------------------------------------------
            1. setStatus(status): đặt mã trạng thái HTTP, ví dụ 401.

            2. setContentType("application/json"): báo cho client biết body là JSON,
               axios sẽ tự đổi chuỗi JSON thành object JavaScript.

            3. setCharacterEncoding("UTF-8"): dùng bảng mã UTF-8 để ghi chữ.
               Phải đặt trước khi gọi getWriter(), nếu không sẽ không có tác dụng.
               Message hiện tại là tiếng Anh, nhưng nếu sau này viết tiếng Việt có dấu
               thì thiếu dòng này sẽ bị lỗi font kiểu "Ä‘Äƒng nháº­p".

            4. getWriter().write(...): lấy "cây bút" của response và viết nội dung body.

               "{\"message\":\"" + message + "\"}"
               - Trong chuỗi Java, dấu " dùng để mở và đóng chuỗi.
                 Muốn có ký tự " nằm bên trong chuỗi thì phải viết \" (thêm dấu \ phía trước).
               - Dấu + dùng để nối chuỗi.
               Với message = "Unauthorized, please login", kết quả là:
                   {"message":"Unauthorized, please login"}

               Lưu ý: tự ghép chuỗi JSON như vậy chỉ an toàn khi message không chứa dấu "
               (nếu có, JSON sẽ bị hỏng). Hiện tại mọi message đều là chữ cố định do mình viết,
               nên không có vấn đề.
        */
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }

    /*
        =====================================================
        BEAN BĂM MẬT KHẨU
        =====================================================

        @Bean: Spring gọi hàm này 1 lần khi khởi động, cất đối tượng BCryptPasswordEncoder vào container.
        Class nào cần (AuthService, UserService) chỉ cần khai báo PasswordEncoder trong constructor,
        Spring tự truyền đúng đối tượng này vào.

        - Kiểu trả về là interface PasswordEncoder, không phải BCryptPasswordEncoder
          → nơi dùng chỉ biết "đây là bộ băm mật khẩu", không phụ thuộc thuật toán cụ thể.
          Sau này muốn đổi thuật toán (ví dụ Argon2) chỉ cần sửa đúng 1 dòng ở đây.

        -----------------------------------------------------
        Được dùng ở đâu?
        -----------------------------------------------------
        - passwordEncoder.encode(matKhauGoc): băm mật khẩu trước khi lưu vào database
          (đăng ký, đặt lại mật khẩu, đổi mật khẩu).
        - passwordEncoder.matches(matKhauNhapVao, banBamTrongDatabase): kiểm tra mật khẩu
          khi đăng nhập hoặc khi đổi mật khẩu (nhập mật khẩu cũ).

        -----------------------------------------------------
        Vì sao chọn BCrypt?
        -----------------------------------------------------
        - Băm 1 chiều: từ bản băm không tính ngược ra mật khẩu gốc được.
          Lỡ database bị lộ, kẻ xấu cũng không biết mật khẩu thật.
        - Có "muối" (salt) ngẫu nhiên: cùng mật khẩu "123456", mỗi lần băm ra 1 kết quả khác nhau
          → 2 người trùng mật khẩu vẫn có bản băm khác nhau.
          Vì vậy không so sánh được bằng cách băm lại rồi dùng equals,
          bắt buộc phải dùng hàm matches().
        - Chậm có chủ đích (mặc định 2^10 = 1024 vòng lặp): 1 lần đăng nhập không đáng kể,
          nhưng kẻ xấu muốn thử hàng tỷ mật khẩu thì mất rất nhiều thời gian.

        Bản băm có dạng chuỗi 60 ký tự, ví dụ:
            $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
            - $2a$ : phiên bản thuật toán BCrypt.
            - 10$  : độ khó (2^10 vòng lặp).
            - 22 ký tự tiếp theo: muối.
            - Phần còn lại: kết quả băm.
    */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}