package com.winhmm.myspotify.exception;

/*
    Các import trong file, nhóm theo thư viện (đọc lướt, gặp ở hàm nào thì quay lại xem):
        org.slf4j...                       Logger, LoggerFactory: ghi log
        org.springframework.dao...         lỗi database đã được Spring "dịch" sang loại dễ hiểu
                                           (DataIntegrityViolation, OptimisticLockingFailure)
        org.springframework.http...        HttpStatus, HttpStatusCode, ResponseEntity: tạo response HTTP
                                           HttpMessageNotReadableException: lỗi đọc JSON
        org.springframework.mail...        MailException: lỗi gửi email của JavaMail
        org.springframework.security...    lỗi xác thực (401) và phân quyền (403)
        org.springframework.validation...  FieldError, ObjectError: chi tiết lỗi validation
        org.springframework.web...         lỗi trong quá trình Spring xử lý request
                                           (validation, tham số sai kiểu, thiếu file, file quá lớn...)
        java.util.Map                      body JSON { "message": "..." }
*/
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mail.MailException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.Map;

/*
    =====================================================================
    GLOBALEXCEPTIONHANDLER - XỬ LÝ LỖI TẬP TRUNG CHO TOÀN ỨNG DỤNG
    =====================================================================

    (Exception là gì, throw/catch, checked và unchecked: xem EmailSendException.
     Thứ tự đọc package exception: xem EmailSendException.)

    -----------------------------------------------------
    Class này làm gì?
    -----------------------------------------------------
    Khi Controller hoặc Service ném ra 1 exception (lỗi), Spring tìm trong class này
    hàm @ExceptionHandler phù hợp để chuyển lỗi đó thành response HTTP.

    Nhờ vậy:
    - Controller và Service chỉ cần "throw new IllegalArgumentException(...)",
      không phải tự viết try/catch và tự tạo response lỗi ở từng API.
    - Mọi lỗi trả về cùng 1 định dạng: { "message": "..." }
      → frontend chỉ cần 1 cách đọc lỗi: error.response.data.message (axios).
    - Không lộ chi tiết kỹ thuật (tên class Java, câu SQL...) ra ngoài.

    Nếu không có class này, Spring Boot dùng cách xử lý lỗi mặc định:
    lỗi nghiệp vụ như "Email already exists" thường thành 500 và message bị ẩn
    → frontend không biết lý do thật để báo cho người dùng.

    -----------------------------------------------------
    Đường đi của 1 lỗi (ví dụ: đăng ký bằng email đã có tài khoản)
    -----------------------------------------------------
        1. Frontend gửi: POST /api/auth/register  { "username": "...", "email": "a@gmail.com", ... }
        2. AuthController.register gọi AuthService.register.
        3. AuthService thấy email đã tồn tại (tài khoản đã xác thực):
               throw new IllegalArgumentException("Email already exists");
        4. Lỗi bay ngược ra khỏi AuthService (transaction rollback), ra khỏi AuthController.
           Không ai bắt → tới DispatcherServlet (bộ điều phối mọi request của Spring MVC).
        5. DispatcherServlet tìm trong class này hàm @ExceptionHandler khớp → handleIllegalArgument.
        6. Hàm trả về ResponseEntity → Spring đổi thành response:
               HTTP 400
               { "message": "Email already exists" }
        7. Frontend đọc: error.response.status = 400
                         error.response.data.message = "Email already exists"

    -----------------------------------------------------
    @RestControllerAdvice
    -----------------------------------------------------
    = @ControllerAdvice + @ResponseBody
    - @ControllerAdvice: các hàm @ExceptionHandler trong class này áp dụng cho MỌI Controller
      (không cần viết lại ở từng Controller). Spring tự tìm class này lúc khởi động,
      giống cách nó tự tìm các class @Service, @RestController.
    - @ResponseBody: giá trị hàm trả về được ghi thẳng vào body response.
      Jackson đổi Map thành JSON: Map.of("message", "abc") → { "message": "abc" }

    -----------------------------------------------------
    Bảng tóm tắt
    -----------------------------------------------------
        Loại lỗi                                Mã    Ví dụ
        IllegalArgumentException                400   Email đã tồn tại, sai OTP
        MethodArgumentNotValidException         400   Thiếu trường, sai định dạng (@Valid)
        HttpMessageNotReadableException         400   JSON sai cú pháp, role không tồn tại
        MissingServletRequestPartException      400   Upload thiếu file
        MethodArgumentTypeMismatchException     400   /users/abc/lock (id phải là số)
        AuthenticationException                 401   Chưa đăng nhập
        AccessDeniedException                   403   Sai role
        DataIntegrityViolationException         409   Trùng dữ liệu do 2 request cùng lúc
        OptimisticLockingFailureException       409   Dữ liệu vừa bị người khác sửa
        MaxUploadSizeExceededException          413   File quá lớn
        EmailSendException                      503   Gmail lỗi khi gửi OTP
        MailException                           503   Dự phòng
        Lỗi chuẩn của Spring (ErrorResponse)    giữ   404, 405, 415...
        Mọi lỗi khác                            500   NullPointerException...

    Ý nghĩa các nhóm mã:
        4xx: lỗi phía người gọi (gửi sai, chưa đăng nhập, không đủ quyền...) → sửa request rồi gửi lại.
        5xx: lỗi phía server (code lỗi, dịch vụ ngoài hỏng...) → người dùng không làm gì sai.

    -----------------------------------------------------
    Spring chọn hàm xử lý nào?
    -----------------------------------------------------
    Luôn chọn hàm khớp CỤ THỂ NHẤT với loại lỗi, không phụ thuộc thứ tự viết trong file.
    "Cụ thể nhất" = gần nhất trên cây kế thừa (ít bậc cha nhất).

    Ví dụ 1: EmailSendException → RuntimeException → Exception
        handleEmailSend (EmailSendException) : cách 0 bậc  ← được chọn
        handleOther     (Exception)          : cách 2 bậc

    Ví dụ 2: @PreAuthorize từ chối ném AuthorizationDeniedException
             AuthorizationDeniedException → AccessDeniedException → RuntimeException → Exception
        handleAccessDenied (AccessDeniedException) : cách 1 bậc  ← được chọn
        handleOther        (Exception)             : cách 3 bậc

    Vì vậy handleOther (bắt Exception, cha của gần như mọi lỗi) nằm giữa file cũng không sao:
    handleOptimisticLock và handleEmailSend viết phía sau nó vẫn được chọn đúng.

    -----------------------------------------------------
    Lỗi nào không đi qua class này?
    -----------------------------------------------------
    Class này chỉ bắt lỗi xảy ra BÊN TRONG DispatcherServlet: từ lúc Spring đọc request
    (JSON, form-data, tham số URL), chạy Controller → Service → Repository, tới lúc trả kết quả.

    Filter chạy TRƯỚC DispatcherServlet, nên lỗi ở filter không tới đây:
        Không gửi token, token sai hoặc hết hạn   → 401 "Unauthorized, please login"
                                                     (SecurityConfig, authenticationEntryPoint)
        Đăng nhập ở máy khác, phiên cũ bị thay     → 401 "Your session has ended. Your account
                                                     may have been logged in on another device"
        Gọi API quá nhiều lần                      → 429 "Too many requests. Please try again in N seconds"
                                                     (RateLimitFilter)
    Các chỗ đó tự ghi JSON cùng định dạng { "message": "..." },
    nên frontend vẫn đọc lỗi theo 1 cách duy nhất.
*/
@RestControllerAdvice
public class GlobalExceptionHandler {

    /*
        -----------------------------------------------------
        Logger: ghi log (nhật ký hoạt động của server)
        -----------------------------------------------------
        Dùng thay cho System.out.println, vì log có:
        - Mức độ: error (lỗi), warn (cảnh báo), info (thông tin), debug (chi tiết để dò lỗi).
        - Thời gian, tên class ghi log → biết lỗi xảy ra lúc nào, ở đâu.

        - SLF4J (org.slf4j) chỉ là "giao diện" chung để ghi log.
          Spring Boot dùng Logback làm phần chạy thật bên dưới, mình không cần cấu hình gì thêm.
        - LoggerFactory.getLogger(GlobalExceptionHandler.class): tạo logger mang tên class này,
          mỗi dòng log sẽ ghi kèm "GlobalExceptionHandler".
        - private: chỉ dùng trong class này.
          static : 1 logger dùng chung cho cả class, không tạo mới mỗi lần.
          final  : gán 1 lần, không đổi.

        2 cách ghi log trong file này:
            log.error("Lỗi gửi email: {}", e.getMessage());
                → {} là chỗ trống, được thay bằng tham số phía sau.
            log.error("Lỗi không mong muốn", e);
                → tham số cuối là 1 exception → in thêm cả stack trace
                  (danh sách từng dòng code lỗi đã đi qua, xem handleOther).

        Xem log ở đâu?
        - Chạy trong IntelliJ: tab Run ở dưới màn hình.
        - Trên VPS: file app.log, xem trực tiếp bằng: tail -f /home/myspotify/app.log
    */
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /*
        -----------------------------------------------------
        Hàm phụ: tạo body lỗi { "message": ... }
        -----------------------------------------------------
        - Map<String, String>: cặp khóa → giá trị, cả 2 đều là chuỗi.
          Map.of("message", "abc") tạo Map có 1 cặp: "message" → "abc".
          Jackson đổi thành JSON: { "message": "abc" }

        - Map.of KHÔNG cho giá trị null (ném NullPointerException),
          nên message null thì dùng câu dự phòng fallback.

        - message != null ? message : fallback
          Toán tử 3 ngôi (điều kiện ? giá trị khi đúng : giá trị khi sai), viết gọn của:
              if (message != null) return message; else return fallback;

        Khi nào message có thể null? Khi lấy message từ nơi khác mà không chắc chắn có, VD:
            throw new IllegalArgumentException();   // không truyền message → getMessage() trả null
        Vì vậy:
        - Hàm nào lấy message từ object lỗi (getMessage, getDefaultMessage, getDetail) → dùng body(...).
        - Hàm nào trả câu cố định tự viết (VD "File is required") → dùng thẳng Map.of(...),
          vì câu cố định không bao giờ null.
    */
    private Map<String, String> body(String message, String fallback) {
        return Map.of("message", message != null ? message : fallback);
    }

    /*
        -----------------------------------------------------
        Lỗi nghiệp vụ → 400
        -----------------------------------------------------
        Service ném IllegalArgumentException kèm message khi vi phạm quy tắc nghiệp vụ.
        Message được trả nguyên văn cho frontend hiển thị. Ví dụ:
            "Email already exists"                                 (đăng ký)
            "Email or password is incorrect"                       (đăng nhập)
            "Otp code does not match"                              (nhập sai OTP)
            "Please wait 60 seconds before requesting a new OTP"   (gửi lại OTP quá nhanh)
            "Song does not exist"                                  (bài không có hoặc không phải của mình)
        Vì message hiện thẳng cho người dùng, khi viết throw trong Service
        chỉ nên ghi câu ngắn gọn, dễ hiểu, không chứa thông tin nhạy cảm.

        Lưu ý: sai mật khẩu khi đăng nhập trả 400, không phải 401.
        Vì AuthService.login tự kiểm tra mật khẩu rồi ném IllegalArgumentException,
        không đi qua cơ chế xác thực của Spring Security (xem handleAuthentication).

        -----------------------------------------------------
        Cú pháp của 1 hàm xử lý lỗi
        -----------------------------------------------------
        @ExceptionHandler(IllegalArgumentException.class)
            → "hàm này xử lý lỗi loại IllegalArgumentException (và mọi class con của nó)".
            IllegalArgumentException.class: object đại diện cho class đó (cách Java chỉ tên 1 class).

        public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e)
            - Tham số e: chính object lỗi vừa bị ném, Spring tự truyền vào.
              e.getMessage() lấy message đã truyền lúc throw.
            - Kiểu trả về ResponseEntity<Map<String, String>>:
              ResponseEntity = toàn bộ response HTTP (mã trạng thái + header + body).
              <Map<String, String>> = kiểu của body (generic: nói rõ bên trong chứa gì).

        ResponseEntity.badRequest().body(...)
            - badRequest()  : bắt đầu tạo response với mã 400 (Bad Request).
            - .body(...)    : gắn body vào và hoàn tất, trả về ResponseEntity.
            Viết đầy đủ tương đương: ResponseEntity.status(HttpStatus.BAD_REQUEST).body(...)
            Các hàm tắt khác trong file: internalServerError() = 500.
            Mã không có hàm tắt thì dùng status(HttpStatus.XXX), VD status(HttpStatus.CONFLICT) = 409.
    */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(body(e.getMessage(), "Invalid request data"));
    }

    /*
        -----------------------------------------------------
        Lỗi validation từ @Valid → 400
        -----------------------------------------------------
        Xảy ra khi dữ liệu trong DTO vi phạm @NotBlank, @Email, @Size, @Pattern, @NotNull...
        Hàm Controller KHÔNG chạy, Spring ném lỗi này ngay khi kiểm tra xong.

        Ví dụ:
            POST /api/auth/register  { "username": "ab", "email": "a@gmail.com", "password": "123456" }
            → @Size(min = 5, max = 20) trên username bị vi phạm
            → 400 { "message": "Username must be between 5 and 20 characters" }

        -----------------------------------------------------
        Đọc lỗi từng bước
        -----------------------------------------------------
        e.getBindingResult()   : kết quả kiểm tra, chứa danh sách tất cả lỗi.
        .getAllErrors()        : List<ObjectError>, mỗi phần tử là 1 lỗi.
        .get(0)                : lấy lỗi đầu tiên (luôn có ít nhất 1 lỗi, nếu không đã chẳng ném).

        Có thể có nhiều lỗi cùng lúc, VD username quá ngắn VÀ email sai định dạng.
        Chỉ lấy 1 lỗi để message ngắn gọn; người dùng sửa xong gửi lại sẽ thấy lỗi tiếp theo.
        Lưu ý: thứ tự các lỗi không cố định, lần này có thể báo username, lần sau báo email.

        2 loại lỗi trong danh sách:
        - ObjectError: lỗi chung của cả object (kiểu cha).
        - FieldError : lỗi gắn với 1 trường cụ thể (kiểu con của ObjectError),
                       có thêm getField() cho biết tên trường, VD "username".
        Mọi annotation trong DTO của dự án đều đặt trên trường → thực tế luôn là FieldError.

        -----------------------------------------------------
        Trường hợp đặc biệt: lỗi đổi kiểu dữ liệu (isBindingFailure)
        -----------------------------------------------------
        Khi gửi form-data (đăng bài: POST /api/artist/songs), mọi giá trị gửi lên đều là chữ.
        Spring phải tự đổi chữ sang đúng kiểu của trường trong DTO:
            genreId   = "3"                → Long 3                          OK
            genreId   = "abc"              → không đổi được sang Long        LỖI
            releaseAt = "2026-12-01T20:00" → LocalDateTime                   OK
            releaseAt = "abc"              → không đổi được                  LỖI
        Lỗi đổi kiểu → fieldError.isBindingFailure() = true.
        Message gốc của Spring rất dài và lộ chi tiết Java, VD:
            Failed to convert property value of type 'java.lang.String'
            to required type 'java.time.LocalDateTime' for property 'releaseAt'; ...
        nên thay bằng message gọn: "Invalid value for releaseAt".

        (Gửi JSON thì sai kiểu không vào đây mà vào handleNotReadable, vì Jackson báo lỗi trước.)

        -----------------------------------------------------
        Cú pháp: error instanceof FieldError fieldError && fieldError.isBindingFailure()
        -----------------------------------------------------
        - error instanceof FieldError: kiểm tra error có thật sự là FieldError không.
        - fieldError (tên biến viết ngay sau): nếu đúng, Java tự ép kiểu và gán vào biến mới này
          (pattern matching, có từ Java 16). Viết kiểu cũ phải 2 bước:
              if (error instanceof FieldError) {
                  FieldError fieldError = (FieldError) error;
                  ...
              }
        - && fieldError.isBindingFailure(): chỉ chạy khi vế trái đúng (&& dừng sớm nếu vế trái sai),
          nên lúc này fieldError chắc chắn đã có giá trị.

        -----------------------------------------------------
        Trường hợp thường: lỗi từ annotation
        -----------------------------------------------------
        error.getDefaultMessage(): message viết trong annotation của DTO,
            VD @NotBlank(message = "Email is required") → "Email is required".
        Dùng body(...) để phòng message null → "Invalid request data".

        Vì sao cần hàm riêng, không để handleOther lo?
        Lỗi này cũng là 1 ErrorResponse (xem handleOther). Không có hàm này, handleOther vẫn trả 400
        nhưng message chung chung "Invalid request content." → người dùng không biết sai ở đâu.
    */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        ObjectError error = e.getBindingResult().getAllErrors().get(0);

        if (error instanceof FieldError fieldError && fieldError.isBindingFailure()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid value for " + fieldError.getField()));
        }

        return ResponseEntity.badRequest().body(body(error.getDefaultMessage(), "Invalid request data"));
    }

    /*
        -----------------------------------------------------
        JSON không đọc được → 400
        -----------------------------------------------------
        Với @RequestBody, Jackson đổi JSON thành object DTO. Đổi không được → lỗi này.
        Ví dụ:
        - JSON sai cú pháp: thiếu dấu ngoặc, thiếu dấu phẩy, thừa dấu phẩy cuối.
              { "fullName": "Winhmm"          ← thiếu }
        - Không gửi body cho API cần body (Postman chọn "none").
        - Giá trị enum không tồn tại:
              PUT /api/admin/users/5/role          { "role": "SUPERADMIN" }
        - Sai kiểu dữ liệu:
              PUT /api/artist/songs/9/release      { "releaseAt": "abc" }
              PUT /api/artist/songs/9              { "title": "...", "featuredArtistIds": ["abc"] }
        → 400 { "message": "Invalid request data" }

        Vì sao không trả message gốc?
        Message của Jackson chứa tên class Java, VD:
            Cannot deserialize value of type `com.winhmm.myspotify.enums.Role` from String "SUPERADMIN"...
        Vừa khó hiểu với người dùng, vừa lộ cấu trúc code ra ngoài.
    */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleNotReadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(Map.of("message", "Invalid request data"));
    }

    /*
        -----------------------------------------------------
        Upload thiếu file → 400
        -----------------------------------------------------
        API nhận file bằng @RequestParam("tên") MultipartFile, nhưng form-data không có key đó
        (hoặc gõ sai tên key):
            POST /api/users/me/avatar           cần key "file"
            PUT  /api/artist/songs/9/cover      cần key "cover"
            PUT  /api/artist/songs/9/canvas     cần key "canvas"
        → 400 { "message": "File is required" }

        Đăng bài (POST /api/artist/songs) thiếu file nhạc thì KHÔNG vào đây:
        file nhạc nằm trong DTO UploadSongRequest với @NotNull
        → vào handleValidation → "Audio file is required".

        Không có hàm này, handleOther vẫn trả 400 nhưng message là "Required part 'file' is not present."
        → thay bằng câu gọn, thống nhất cho mọi API upload.
    */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, String>> handleMissingFile(MissingServletRequestPartException e) {
        return ResponseEntity.badRequest().body(Map.of("message", "File is required"));
    }

    /*
        -----------------------------------------------------
        Tham số trên URL sai kiểu → 400
        -----------------------------------------------------
        Tham số trên URL (@PathVariable, @RequestParam) luôn là chữ, Spring phải đổi sang đúng kiểu.
        Đổi không được → lỗi này. Ví dụ:
            PUT /api/admin/users/abc/lock         userId phải là số      → "Invalid parameter: userId"
            GET /api/songs?page=abc               page phải là số        → "Invalid parameter: page"
            GET /api/admin/songs?status=ABC       status phải là SongStatus (PENDING, APPROVED...)
                                                                         → "Invalid parameter: status"
        e.getName() trả về tên tham số bị sai.
    */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest().body(Map.of("message", "Invalid parameter: " + e.getName()));
    }

    /*
        -----------------------------------------------------
        Chưa đăng nhập → 401 (dự phòng)
        -----------------------------------------------------
        AuthenticationException: lỗi xác thực của Spring Security
        (con của nó: BadCredentialsException, DisabledException...).

        Phần lớn lỗi 401 đã được SecurityConfig xử lý trong filter (xem phần đầu file),
        request chưa đăng nhập không bao giờ tới được Controller.
        Đăng nhập sai mật khẩu cũng không vào đây (AuthService ném IllegalArgumentException → 400).
        Hàm này chỉ để phòng những trường hợp hiếm khi lỗi xác thực xảy ra bên trong Controller,
        tránh để nó rơi vào handleOther thành 500.
    */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthentication(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Unauthorized, please login"));
    }

    /*
        -----------------------------------------------------
        Không đủ quyền → 403
        -----------------------------------------------------
        401 và 403 khác nhau thế nào?
        - 401 Unauthorized: chưa biết bạn là ai (chưa đăng nhập, token sai).
        - 403 Forbidden   : biết bạn là ai, nhưng bạn không có quyền làm việc này.

        Ví dụ: tài khoản USER gọi GET /api/admin/users
        → AdminController có @PreAuthorize("hasRole('ADMIN')") → từ chối
        → ném AuthorizationDeniedException (con của AccessDeniedException)
        → 403 { "message": "You do not have permission to access this resource" }

        Vì sao lỗi này tới được đây (không phải do filter xử lý)?
        @PreAuthorize kiểm tra ngay trước khi hàm Controller chạy, tức là đã ở bên trong DispatcherServlet.
        Nếu không có hàm này, lỗi sẽ rơi vào handleOther (bắt Exception) và thành 500.
        Message giống hệt accessDeniedHandler trong SecurityConfig → frontend thấy 1 kiểu duy nhất.
    */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "You do not have permission to access this resource"));
    }

    /*
        -----------------------------------------------------
        Vi phạm ràng buộc trong database → 409
        -----------------------------------------------------
        409 Conflict: request hợp lệ nhưng xung đột với dữ liệu đang có.

        Ví dụ: 2 người đăng ký cùng username đúng cùng lúc (cách nhau vài mili giây).
            Người A: existsByUsername("winhmm") → false (chưa có) → INSERT
            Người B: existsByUsername("winhmm") → false (A chưa kịp lưu) → INSERT
            → Cả 2 đều qua bước kiểm tra trong Service,
              nhưng cột username có ràng buộc UNIQUE trong MySQL → INSERT của B bị từ chối.
            → B nhận 409 { "message": "Data already exists" }, A đăng ký thành công.

        Lỗi gốc từ MySQL (SQLException) được Spring "dịch" thành DataIntegrityViolationException,
        nên ở đây không cần biết database là MySQL hay loại khác.

        Không trả message gốc vì nó chứa câu SQL và tên bảng, VD:
            Duplicate entry 'winhmm' for key 'users.UK...' ... insert into users ...

        Lưu ý: lỗi này còn xảy ra khi vi phạm các ràng buộc khác (chuỗi dài hơn độ dài cột, khóa ngoại).
        Những trường hợp đó gần như không xảy ra vì DTO đã có @Size khớp độ dài cột,
        nên message "Data already exists" đúng với trường hợp phổ biến nhất là trùng dữ liệu.
    */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrity(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "Data already exists"));
    }

    /*
        -----------------------------------------------------
        File quá lớn → 413
        -----------------------------------------------------
        Giới hạn upload cấu hình trong application.properties:
            spring.servlet.multipart.max-file-size=100MB       mỗi file tối đa 100MB
            spring.servlet.multipart.max-request-size=110MB    cả request (mọi file + chữ) tối đa 110MB
        Vượt 1 trong 2 → 413 { "message": "File is too large" }

        Lỗi xảy ra trước khi tới Controller, ngay khi Spring đọc phần form-data của request.
        Vẫn tới được đây vì bước đọc form-data nằm bên trong DispatcherServlet.

        HttpStatus.CONTENT_TOO_LARGE: mã 413, tên mới của PAYLOAD_TOO_LARGE
        (tên cũ vẫn chạy nhưng bị gạch ngang trong IntelliJ, báo "đã lỗi thời").

        Lưu ý khi test: file lớn hơn giới hạn RẤT nhiều (VD gửi file 1GB),
        server có thể ngắt kết nối giữa chừng → Postman báo lỗi kết nối (socket hang up, ECONNRESET)
        thay vì hiện 413. Đó là cách Tomcat tự bảo vệ, không phải lỗi code.
    */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxSize(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
                .body(Map.of("message", "File is too large"));
    }

    /*
        -----------------------------------------------------
        Lỗi gửi email từ JavaMail → 503 (dự phòng)
        -----------------------------------------------------
        MailException: lỗi gửi email của Spring, VD:
        - MailAuthenticationException: sai email hoặc mật khẩu ứng dụng Gmail.
        - MailSendException          : không kết nối được tới Gmail, gửi thất bại.

        Hiện tại EmailService đã bắt mọi lỗi và đổi thành EmailSendException,
        nên hàm này không được gọi tới.
        Giữ lại phòng trường hợp sau này có chỗ gọi mailSender trực tiếp
        mà không đi qua EmailService.

        log.error("Lỗi gửi email: {}", e.getMessage()): ghi lỗi vào log để người quản trị biết
        (EmailService không chạy nên phải tự ghi ở đây). Người dùng chỉ thấy câu chung chung.
    */
    @ExceptionHandler(MailException.class)
    public ResponseEntity<Map<String, String>> handleMail(MailException e) {
        log.error("Lỗi gửi email: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", "Could not send email, please try again later"));
    }

    /*
        -----------------------------------------------------
        Mọi lỗi còn lại
        -----------------------------------------------------
        @ExceptionHandler(Exception.class): Exception là cha của gần như mọi lỗi
        → lỗi nào không có hàm riêng ở trên đều rơi vào đây (lưới an toàn cuối cùng).
        Chia làm 2 nhóm:

        1. Lỗi chuẩn của Spring (ErrorResponse) → giữ đúng mã lỗi và message của Spring
        ---------------------------------------------------------------------------
        ErrorResponse là 1 interface. Các lỗi "có sẵn mã HTTP" của Spring đều implements nó,
        tức là tự biết mình nên trả mã nào và câu mô tả nào.
        Ví dụ (đã đăng nhập; câu mô tả do Spring tự đặt, có thể khác chút tùy phiên bản và cấu hình):
            GET  /api/abcxyz                        (sai URL)
                → 404 { "message": "No static resource api/abcxyz." }
            GET  /api/auth/login                    (API chỉ nhận POST)
                → 405 { "message": "Method 'GET' is not supported." }
            POST /api/users/me/avatar gửi JSON      (API chỉ nhận form-data)
                → 415 { "message": "Content-Type 'application/json' is not supported." }
            GET  /api/artist/search-artists         (thiếu ?keyword=)
                → 400 { "message": "Required parameter 'keyword' is not present." }

        Đọc từng dòng:
            if (e instanceof ErrorResponse errorResponse)
                → e có phải ErrorResponse không? Phải thì ép kiểu, gán vào biến errorResponse
                  (pattern matching, giống handleValidation).
            errorResponse.getStatusCode()
                → mã HTTP Spring đã chọn, VD 404. Kiểu HttpStatusCode
                  (interface chung; HttpStatus là enum các mã chuẩn và implements nó).
            errorResponse.getBody().getDetail()
                → getBody() trả về ProblemDetail (object mô tả lỗi theo chuẩn RFC 9457),
                  getDetail() lấy câu mô tả, VD "Method 'GET' is not supported."
            ResponseEntity.status(status).body(body(detail, "Request error"))
                → trả đúng mã đó, message là câu mô tả (null thì dùng "Request error").

        2. Lỗi không mong muốn → 500
        ---------------------------------------------------------------------------
        VD NullPointerException (quên kiểm tra null), mất kết nối database, lỗi đọc ghi file...
        Đây là lỗi do code hoặc do server, người dùng không làm gì sai.
        - log.error("Lỗi không mong muốn", e): truyền cả object e → log in đầy đủ stack trace, VD:
              ERROR ... GlobalExceptionHandler : Lỗi không mong muốn
              java.lang.NullPointerException: Cannot invoke "String.length()" because "x" is null
                  at com.winhmm.myspotify.service.SongService.getPublishedSong(SongService.java:120)
                  at com.winhmm.myspotify.controller.SongController.getSong(SongController.java:95)
                  ...
          Dòng "at ..." đầu tiên thuộc package com.winhmm.myspotify chính là chỗ cần sửa
          (tên file và số dòng nằm trong ngoặc).
        - Frontend chỉ nhận { "message": "Internal server error" }, không lộ chi tiết ra ngoài.
        - ResponseEntity.internalServerError(): hàm tắt cho mã 500.

        Lưu ý cho sau này: ArtistSongController có @Validated ở đầu class.
        Nếu thêm annotation kiểm tra thẳng lên tham số, VD @RequestParam @NotBlank String keyword,
        lỗi vi phạm sẽ là ConstraintViolationException: không có hàm riêng, cũng không phải
        ErrorResponse → rơi vào nhóm 2 → 500. Khi đó cần viết thêm 1 hàm @ExceptionHandler riêng
        trả 400. (Hiện chưa có tham số nào như vậy nên chưa xảy ra.)
    */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleOther(Exception e) {
        if (e instanceof ErrorResponse errorResponse) {
            HttpStatusCode status = errorResponse.getStatusCode();
            String detail = errorResponse.getBody().getDetail();
            return ResponseEntity.status(status).body(body(detail, "Request error"));
        }

        log.error("Lỗi không mong muốn", e);
        return ResponseEntity.internalServerError()
                .body(Map.of("message", "Internal server error"));
    }

    /*
        -----------------------------------------------------
        Dữ liệu vừa bị người khác thay đổi → 409
        -----------------------------------------------------
        Optimistic locking (khóa lạc quan): entity Song có cột version (@Version).
        Mỗi lần lưu, Hibernate tăng version thêm 1, và chỉ cập nhật nếu version
        vẫn còn như lúc đọc. Khác → có người vừa sửa xen vào → ném lỗi thay vì ghi đè mất thay đổi của họ.

        Ví dụ: Admin duyệt bài đúng lúc Artist xóa bài (bài số 9, đang có version = 3).
            Admin đọc bài 9 (version 3)         Artist đọc bài 9 (version 3)
            Artist lưu trước:
                UPDATE songs SET status = 'DELETED',  version = 4 WHERE id = 9 AND version = 3
                → sửa được 1 dòng → thành công
            Admin lưu sau:
                UPDATE songs SET status = 'APPROVED', version = 4 WHERE id = 9 AND version = 3
                → sửa được 0 dòng (version đã là 4) → Hibernate biết có người sửa xen vào
                → ném ObjectOptimisticLockingFailureException (con của OptimisticLockingFailureException)
                → Admin nhận 409 { "message": "This data was changed by someone else, please reload and try again" }
        Nếu không có version, bài đã xóa sẽ bị "sống lại" thành APPROVED.

        "Lạc quan" vì không khóa dữ liệu khi đọc (tin rằng hiếm khi 2 người sửa cùng lúc),
        chỉ kiểm tra lúc lưu. Xảy ra xung đột thì báo lỗi.
        Frontend nên tải lại dữ liệu mới nhất rồi thử lại.
        (Có test riêng cho trường hợp này: SongOptimisticLockTest.)
    */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, String>> handleOptimisticLock(OptimisticLockingFailureException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "This data was changed by someone else, please reload and try again"));
    }

    /*
        -----------------------------------------------------
        Gửi email OTP thất bại → 503
        -----------------------------------------------------
        503 Service Unavailable: không phải lỗi do người dùng nhập sai (400),
        cũng không phải lỗi code backend (500), mà do dịch vụ bên ngoài (Gmail) đang không dùng được.
        → Frontend có thể hiện: "Không gửi được email, vui lòng thử lại sau".

        Ví dụ: đăng ký đúng lúc Gmail lỗi (hoặc mật khẩu ứng dụng Gmail trên VPS bị sai)
            POST /api/auth/register
            → 503 { "message": "Could not send OTP email, please try again later" }

        Thứ tự xảy ra (chi tiết xem EmailSendException):
            1. EmailService bắt lỗi gốc, ghi log, ném EmailSendException.
            2. Lỗi bay qua các hàm @Transactional → ROLLBACK (tài khoản mới hoặc mã OTP vừa tạo bị hủy).
            3. Lỗi tới hàm này → trả 503.
        Khi hàm này chạy thì rollback đã xong, ở đây chỉ việc trả message cho frontend.

        - Không ghi log ở đây vì EmailService đã ghi rồi (ghi 2 lần chỉ làm rối log).
        - e.getMessage(): message truyền lúc tạo lỗi trong EmailService.
          Dùng thẳng Map.of (không cần body(...)) vì message luôn là câu cố định, không bao giờ null.
    */
    @ExceptionHandler(EmailSendException.class)
    public ResponseEntity<Map<String, String>> handleEmailSend(EmailSendException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", e.getMessage()));
    }
}