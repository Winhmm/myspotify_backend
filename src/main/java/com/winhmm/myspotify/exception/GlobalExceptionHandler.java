package com.winhmm.myspotify.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mail.MailException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /*
        Tạo body lỗi, tránh lỗi Map.of khi message bị null.
    */
    private Map<String, String> body(String message, String fallback) {
        return Map.of("message", message != null ? message : fallback);
    }

    /*
        Lỗi nghiệp vụ trong service (email trùng, sai OTP, ...) → 400.
    */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(body(e.getMessage(), "Invalid request"));
    }

    /*
        Lỗi validation từ @Valid (@NotBlank, @Email, ...) → 400, lấy message lỗi đầu tiên.
    */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ResponseEntity.badRequest().body(body(message, "Invalid request data"));
    }

    /*
        JSON sai định dạng, hoặc gửi role không tồn tại (vd "SUPERADMIN") → 400.
    */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleNotReadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(Map.of("message", "Invalid request data"));
    }

    /*
        Gọi API upload nhưng không gửi kèm file (thiếu key "file") → 400.
    */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, String>> handleMissingFile(MissingServletRequestPartException e) {
        return ResponseEntity.badRequest().body(Map.of("message", "File is required"));
    }

    /*
        Tham số trên URL sai kiểu, vd /api/admin/users/abc/lock (userId phải là số) → 400.
    */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest().body(Map.of("message", "Invalid parameter: " + e.getName()));
    }

    /*
        Chưa đăng nhập / sai thông tin đăng nhập (Spring Security) → 401.
    */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthentication(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Unauthorized, please login"));
    }

    /*
        Đã đăng nhập nhưng không đủ quyền (bị @PreAuthorize chặn) → 403.
    */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "You do not have permission to access this resource"));
    }

    /*
        Vi phạm ràng buộc UNIQUE trong Database
        (vd 2 người đăng ký cùng username đúng cùng lúc) → 409.
    */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrity(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "Data already exists"));
    }

    /*
        File tải lên vượt quá kích thước cho phép → 413.
    */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxSize(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
                .body(Map.of("message", "File is too large"));
    }

    /*
        Gửi email thất bại (sai cấu hình Gmail, mất kết nối, ...) → 503.
    */
    @ExceptionHandler(MailException.class)
    public ResponseEntity<Map<String, String>> handleMail(MailException e) {
        log.error("Lỗi gửi email: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", "Could not send email, please try again later"));
    }

    /*
        Bắt mọi lỗi còn lại:
        - Lỗi chuẩn của Spring (sai URL → 404, sai method → 405,
          sai Content-Type → 415, thiếu tham số → 400, ...) → giữ đúng status của nó.
        - Lỗi không mong muốn (NullPointerException, ...) → 500, ghi log để debug.
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
}
