package com.winhmm.myspotify.exception;

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
    =====================================================
    GLOBALEXCEPTIONHANDLER - XỬ LÝ LỖI TẬP TRUNG CHO TOÀN ỨNG DỤNG
    =====================================================

    -----------------------------------------------------
    Class này làm gì?
    -----------------------------------------------------
    Khi Controller hoặc Service ném ra 1 exception (lỗi), Spring tìm trong class này
    hàm @ExceptionHandler phù hợp để chuyển lỗi đó thành response HTTP.

    Nhờ vậy:
    - Controller và Service chỉ cần "throw new IllegalArgumentException(...)",
      không phải tự viết try/catch và tự tạo response lỗi ở từng API.
    - Mọi lỗi trả về cùng 1 định dạng: { "message": "..." }
      → frontend chỉ cần 1 cách đọc lỗi: response.data.message.
    - Không lộ chi tiết kỹ thuật (tên class Java, câu SQL...) ra ngoài.

    @RestControllerAdvice: áp dụng cho mọi Controller, kết quả trả về được chuyển thành JSON.

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

    -----------------------------------------------------
    Spring chọn hàm xử lý nào?
    -----------------------------------------------------
    Luôn chọn hàm khớp cụ thể nhất với loại lỗi, không phụ thuộc thứ tự viết trong file.
    Ví dụ: EmailSendException là con của RuntimeException, con của Exception.
    Spring chọn handleEmailSend, không chọn handleOther (bắt Exception).

    -----------------------------------------------------
    Lỗi nào không đi qua class này?
    -----------------------------------------------------
    401 và 403 xảy ra trong filter (trước khi tới Controller), ví dụ không gửi token
    hoặc token sai, được SecurityConfig tự xử lý và trả cùng định dạng { "message": "..." }.
    429 do RateLimitFilter tự trả.
*/
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /*
        Tạo body lỗi { "message": ... }.
        Map.of không cho giá trị null (ném NullPointerException),
        nên message null thì dùng câu dự phòng fallback.
    */
    private Map<String, String> body(String message, String fallback) {
        return Map.of("message", message != null ? message : fallback);
    }

    /*
        -----------------------------------------------------
        Lỗi nghiệp vụ → 400
        -----------------------------------------------------
        Service ném IllegalArgumentException kèm message khi vi phạm quy tắc nghiệp vụ:
        email đã tồn tại, sai OTP, tài khoản bị khóa...
        Message được trả nguyên văn cho frontend hiển thị.
    */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(body(e.getMessage(), "Invalid request data"));
    }

    /*
        -----------------------------------------------------
        Lỗi validation từ @Valid → 400
        -----------------------------------------------------
        Xảy ra khi dữ liệu trong DTO vi phạm @NotBlank, @Email, @Size, @Pattern...
        Có thể có nhiều lỗi cùng lúc, chỉ lấy lỗi đầu tiên để message ngắn gọn.

        Trường hợp đặc biệt: lỗi đổi kiểu dữ liệu (isBindingFailure), ví dụ
        releaseAt = "abc" hoặc featuredArtistIds = "xyz" khi gửi form-data.
        Message gốc của Spring rất dài và lộ chi tiết Java,
        nên thay bằng message gọn: "Invalid value for releaseAt".
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
        Ví dụ:
        - JSON sai cú pháp: thiếu dấu ngoặc, thiếu dấu phẩy.
        - Giá trị enum không tồn tại: "role": "SUPERADMIN".
        - Sai kiểu: "fullName": 123 thay vì chuỗi... (một số trường hợp).
    */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleNotReadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(Map.of("message", "Invalid request data"));
    }

    /*
        -----------------------------------------------------
        Upload thiếu file → 400
        -----------------------------------------------------
        Gọi API upload (avatar, bài hát) nhưng form-data không có key "file".
    */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, String>> handleMissingFile(MissingServletRequestPartException e) {
        return ResponseEntity.badRequest().body(Map.of("message", "File is required"));
    }

    /*
        -----------------------------------------------------
        Tham số trên URL sai kiểu → 400
        -----------------------------------------------------
        Ví dụ: /api/admin/users/abc/lock, trong khi userId phải là số.
        e.getName() trả về tên tham số bị sai, VD "userId".
    */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest().body(Map.of("message", "Invalid parameter: " + e.getName()));
    }

    /*
        -----------------------------------------------------
        Chưa đăng nhập → 401
        -----------------------------------------------------
        Phần lớn lỗi 401 đã được SecurityConfig xử lý trong filter.
        Hàm này bắt những trường hợp hiếm khi lỗi xác thực xảy ra bên trong Controller.
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
        @PreAuthorize từ chối ném ra AuthorizationDeniedException (con của AccessDeniedException)
        bên trong Controller. Nếu không có hàm này, lỗi sẽ rơi vào handleOther và thành 500.
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
        Ví dụ: 2 người đăng ký cùng username đúng cùng lúc.
        Cả 2 đều qua bước kiểm tra existsByUsername trong Service,
        nhưng ràng buộc UNIQUE trong database chặn người thứ hai.
        Không trả message gốc vì nó chứa câu SQL và tên bảng.
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
        File vượt giới hạn upload của server (spring.servlet.multipart.max-file-size).
        Lỗi xảy ra trước khi tới Controller, ngay khi Spring đọc request.
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
        Hiện tại EmailService đã bắt mọi lỗi và đổi thành EmailSendException,
        nên hàm này không được gọi tới.
        Giữ lại phòng trường hợp sau này có chỗ gọi mailSender trực tiếp
        mà không đi qua EmailService.
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
        1. Lỗi chuẩn của Spring (ErrorResponse), ví dụ:
           - Sai URL → 404.
           - Sai method (gọi GET cho API chỉ nhận POST) → 405.
           - Sai Content-Type (gửi JSON cho API upload) → 415.
           - Thiếu tham số bắt buộc → 400.
           → Giữ đúng mã lỗi và message của Spring.
        2. Lỗi không mong muốn (NullPointerException, mất kết nối database...)
           → 500, ghi log đầy đủ để debug (xem trong app.log trên VPS),
             nhưng chỉ trả "Internal server error", không lộ chi tiết ra ngoài.
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
        Optimistic locking: entity Song có cột version.
        Mỗi lần lưu, Hibernate kiểm tra version có còn như lúc đọc không.
        Khác → có người vừa sửa xen vào → ném lỗi thay vì ghi đè mất thay đổi của họ.

        Ví dụ: Admin duyệt bài đúng lúc Artist xóa bài.
        Frontend nên tải lại dữ liệu mới nhất rồi thử lại.
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
        Lỗi đã được ghi log trong EmailService, ở đây chỉ trả message cho frontend.
        Transaction đã rollback: tài khoản mới hoặc mã OTP vừa tạo đều bị hủy.
    */
    @ExceptionHandler(EmailSendException.class)
    public ResponseEntity<Map<String, String>> handleEmailSend(EmailSendException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", e.getMessage()));
    }
}
