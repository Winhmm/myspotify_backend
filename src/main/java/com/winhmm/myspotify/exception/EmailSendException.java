package com.winhmm.myspotify.exception;

/*
    =====================================================================
    PACKAGE EXCEPTION - THỨ TỰ ĐỌC
    =====================================================================

    1. EmailSendException      (file này): exception tự viết, rất ngắn.
                                Đọc trước để hiểu exception là gì, throw/catch, checked và unchecked.
    2. GlobalExceptionHandler : nơi đổi mọi exception thành response HTTP { "message": "..." }
                                gửi về frontend.

    =====================================================================
    EXCEPTION LÀ GÌ? (ĐỌC NẾU MỚI HỌC)
    =====================================================================

    -----------------------------------------------------
    Exception = 1 object báo "có chuyện, không làm tiếp được"
    -----------------------------------------------------
    Khi code gặp tình huống không thể chạy tiếp (sai OTP, email đã tồn tại, Gmail lỗi...),
    ta tạo 1 object exception mô tả lỗi rồi "ném" nó đi bằng từ khóa throw:

        throw new IllegalArgumentException("Email already exists");

    - new IllegalArgumentException(...) : tạo object lỗi, kèm message (câu mô tả lỗi).
    - throw                              : ném lỗi đi. Các dòng phía sau throw trong hàm đó KHÔNG chạy nữa.

    -----------------------------------------------------
    Lỗi "bay ngược" lên hàm đã gọi
    -----------------------------------------------------
    Lỗi bị ném ra sẽ thoát khỏi hàm hiện tại, quay về hàm đã gọi nó, rồi hàm đã gọi hàm đó...
    cứ thế đi ngược lên, tới khi gặp 1 chỗ bắt lỗi (catch).

    Ví dụ thật trong dự án khi đăng ký mà Gmail đang lỗi (đọc từ dưới lên = đường lỗi bay ngược):

        GlobalExceptionHandler.handleEmailSend  ← (6) Spring đưa lỗi tới đây → trả 503 cho frontend
        AuthController.register                 ← (5) không bắt, lỗi bay tiếp
        AuthService.register                    ← (4) không bắt, lỗi bay tiếp
        OtpService.generateAndSend              ← (3) không bắt, lỗi bay tiếp
        EmailService.sendOtp                    ← (2) BẮT lỗi gốc, ném lại thành EmailSendException
        mailSender.send(message)                ← (1) lỗi gốc xảy ra ở đây (sai mật khẩu ứng dụng, mất mạng...)

    -----------------------------------------------------
    try/catch: bắt lỗi
    -----------------------------------------------------
        try {
            // code có thể bị lỗi
        } catch (Exception e) {
            // chỉ chạy khi code trong try bị lỗi; e là object lỗi vừa bắt được
        }

    Ví dụ trong EmailService.sendOtp:
        try {
            ...
            mailSender.send(message);                     // (1) có thể lỗi
        } catch (Exception e) {                           // (2) bắt mọi loại lỗi
            log.error("Could not send OTP email to {}: {}", toEmail, e.getMessage());
            throw new EmailSendException("Could not send OTP email, please try again later", e);
        }
    → Bắt lỗi gốc (khó hiểu, đủ loại), ghi log, rồi ném ra 1 loại lỗi duy nhất của dự án: EmailSendException.

    -----------------------------------------------------
    Cây kế thừa của exception trong Java
    -----------------------------------------------------
        Throwable
        ├── Error                               lỗi nghiêm trọng của JVM (hết bộ nhớ...), không bắt
        └── Exception                           CHECKED: trình biên dịch bắt buộc phải xử lý
            ├── IOException, SQLException...    (checked)
            └── RuntimeException                UNCHECKED: không bắt buộc xử lý
                ├── IllegalArgumentException    Service dùng cho lỗi nghiệp vụ (→ 400)
                ├── NullPointerException        lỗi code (→ 500)
                └── EmailSendException          file này (→ 503)

    Checked (con của Exception nhưng KHÔNG phải con của RuntimeException):
        Hàm nào ném ra (hoặc gọi 1 hàm có thể ném ra) loại này thì BẮT BUỘC phải chọn 1 trong 2:
        - Bọc trong try/catch.
        - Hoặc khai báo "throws" ở tên hàm, VD: public void register(...) throws IOException
        Không làm → IntelliJ báo đỏ, không biên dịch được.

    Unchecked (con của RuntimeException):
        Không bắt buộc gì cả. Cứ throw, lỗi tự bay ngược lên tới khi có chỗ bắt
        (trong dự án này: GlobalExceptionHandler bắt tất cả).

    =====================================================================
    EMAILSENDEXCEPTION - LỖI GỬI EMAIL
    =====================================================================

    Được ném ra khi EmailService không gửi được email OTP
    (Gmail lỗi, sai mật khẩu ứng dụng, mất kết nối, quá thời gian chờ...).
    Chỉ có đúng 1 chỗ tạo ra lỗi này: khối catch trong EmailService.sendOtp (xem ở trên).

    Lưu ý: gửi tới 1 email không có thật (VD gõ nhầm abc@gmial.com) thường KHÔNG gây lỗi này.
    Gmail vẫn nhận thư và báo gửi thành công, sau đó mới gửi thư "không tìm thấy người nhận"
    về hộp thư của MySpotify. Người dùng chỉ đơn giản là không nhận được mã.

    -----------------------------------------------------
    Vì sao phải kế thừa RuntimeException? Lý do 1: rollback
    -----------------------------------------------------
    Transaction (giao dịch): gom nhiều câu lệnh SQL thành 1 khối "làm hết hoặc không làm gì".
    - Hàm có @Transactional chạy xong bình thường → COMMIT: mọi thay đổi được lưu thật vào MySQL.
    - Hàm ném lỗi ra ngoài                       → ROLLBACK: mọi thay đổi trong hàm bị hủy hết.

    Nhưng quy tắc mặc định của @Transactional chỉ ROLLBACK khi lỗi là RuntimeException (hoặc Error).
    Nếu là checked exception, transaction vẫn COMMIT
    → tài khoản và mã OTP vẫn được lưu dù email không gửi được.

    Kế thừa RuntimeException thì khi gửi email lỗi, những gì vừa làm đều bị hủy:

        API gặp lỗi gửi email                              Bị hủy (rollback)
        POST /api/auth/register (email mới)                tài khoản vừa tạo + mã OTP vừa tạo
        POST /api/auth/register (email UNVERIFIED cũ)      username, mật khẩu vừa ghi đè + mã OTP vừa tạo
        POST /api/auth/forgot-password                     mã OTP vừa tạo
        POST /api/users/me/change-password/request-otp     mã OTP vừa tạo
        POST /api/users/me/email/request-otp               mã OTP vừa tạo

    Hệ quả có lợi cho người dùng:
    - Đăng ký mới bị lỗi: không còn tài khoản "rác" → đăng ký lại ngay bằng cùng email.
    - Không bị tính thời gian chờ 60 giây (OtpService tính từ mã OTP mới nhất,
      mà mã đó đã bị hủy) → bấm gửi lại được ngay.
    - Trước khi tạo mã mới, OtpService đánh dấu các mã cũ là "đã dùng".
      Bước này cũng bị hủy → mã cũ (nếu còn trong 5 phút) vẫn dùng được.

    -----------------------------------------------------
    Lý do 2: không phải viết "throws" ở khắp nơi
    -----------------------------------------------------
    Nếu là checked exception, mọi hàm trên đường lỗi bay ngược đều phải khai báo throws:
        OtpService.createAndSend, cả 2 hàm OtpService.generateAndSend,
        AuthService.register, AuthService.forgotPassword,
        UserService.requestChangePasswordOtp, UserService.requestUpdateEmailOtp,
        và cả các hàm Controller gọi chúng.
    Unchecked thì không cần gì, code gọn hơn.

    -----------------------------------------------------
    Vì sao cần class riêng, không dùng luôn RuntimeException?
    -----------------------------------------------------
    Để GlobalExceptionHandler nhận ra ĐÚNG loại lỗi này và trả:
        HTTP 503
        { "message": "Could not send OTP email, please try again later" }
    Nếu ném RuntimeException chung chung, nó rơi vào handleOther → 500 "Internal server error",
    frontend không biết là do email, người dùng tưởng web bị hỏng.

    Vì sao không dùng IllegalArgumentException (→ 400)?
    400 nghĩa là "người dùng gửi sai". Ở đây người dùng không sai gì,
    lỗi do dịch vụ bên ngoài (Gmail) → 503 đúng ý nghĩa hơn (xem handleEmailSend).

    -----------------------------------------------------
    Cú pháp "extends"
    -----------------------------------------------------
    public class EmailSendException extends RuntimeException
    - extends: kế thừa. EmailSendException là 1 loại RuntimeException,
      nên tự có sẵn mọi hàm của cha mà không phải viết lại:
          getMessage()       : lấy message
          getCause()         : lấy lỗi gốc
          printStackTrace()  : in đường đi của lỗi ra console
    - Class này chỉ cần viết thêm 1 constructor, phần còn lại thừa hưởng từ RuntimeException.
*/
public class EmailSendException extends RuntimeException {

    /*
        -----------------------------------------------------
        Constructor (hàm khởi tạo)
        -----------------------------------------------------
        - Tên trùng tên class, không có kiểu trả về (không có void).
        - Chạy khi viết: new EmailSendException("...", e)

        2 tham số:
        - message: nội dung lỗi trả về cho frontend.
                   Luôn là câu cố định "Could not send OTP email, please try again later" (EmailService truyền vào).
                   Không dùng message gốc của JavaMail vì nó chứa chi tiết kỹ thuật
                   (tên máy chủ smtp.gmail.com, cổng, mã lỗi...), người dùng đọc không hiểu
                   và không nên lộ ra ngoài.
        - cause:   lỗi gốc từ JavaMail (VD MailAuthenticationException khi sai mật khẩu ứng dụng).
                   Throwable là kiểu cha cao nhất (xem cây ở trên) → nhận được mọi loại lỗi.

        super(message, cause):
        - super(...) gọi constructor của class cha, ở đây là RuntimeException(String message, Throwable cause).
        - RuntimeException tự lưu 2 giá trị này lại, sau đó đọc ra bằng:
              e.getMessage() → "Could not send OTP email, please try again later"
                               (GlobalExceptionHandler.handleEmailSend dùng để trả cho frontend)
              e.getCause()   → lỗi gốc từ JavaMail

        Vì sao giữ lại cause?
        Nếu sau này có chỗ ghi log cả object lỗi, VD log.error("...", e), log sẽ in thêm dòng
            Caused by: org.springframework.mail.MailAuthenticationException: Authentication failed
        → biết ngay nguyên nhân thật. Bỏ cause đi thì mất dấu vết này.
        (Hiện tại EmailService đã ghi log lỗi gốc trước khi ném, nên đây là thông tin dự phòng.)

        Vì sao chỉ có 1 constructor?
        Chỉ có 1 chỗ tạo lỗi này (EmailService), và chỗ đó luôn có đủ message lẫn lỗi gốc.
    */
    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}