package com.winhmm.myspotify.entity;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    (Entity là gì, các annotation @Entity, @Id, @Column, @Enumerated: xem giải thích chi tiết trong User.
     Thứ tự đọc package entity: xem User.)

    - OtpPurpose           : enum mục đích của mã OTP (REGISTER, RESET_PASSWORD, CHANGE_PASSWORD, CHANGE_EMAIL).
    - jakarta.persistence.*: mượn tất cả annotation của JPA.
    - LocalDateTime        : kiểu ngày giờ của Java, dùng cho expiredAt.
*/
import com.winhmm.myspotify.enums.OtpPurpose;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/*
    =====================================================================
    OTPVERIFICATION - ENTITY ÁNH XẠ BẢNG otp_verifications (MÃ XÁC THỰC)
    =====================================================================

    Mỗi dòng là 1 mã OTP đã gửi cho người dùng.

    Ví dụ dữ liệu:
        id | user_id | otp_code | purpose         | new_email          | expired_at          | verified
        12 |    6    | 482913   | REGISTER        | null               | 2026-10-18 14:05:00 | true
        13 |    6    | 529301   | CHANGE_PASSWORD | null               | 2026-10-18 15:20:00 | false
        14 |    6    | 118204   | CHANGE_EMAIL    | new@gmail.com      | 2026-10-18 16:00:00 | false

    Dùng ở 4 nơi, phân biệt bằng cột purpose:
        REGISTER        → xác thực email khi đăng ký (VerifyOtpRequest)
        RESET_PASSWORD  → quên mật khẩu (ForgotPasswordRequest, ResetPasswordRequest)
        CHANGE_PASSWORD → đổi mật khẩu khi đã đăng nhập (ChangePasswordRequest)
        CHANGE_EMAIL    → đổi email (RequestUpdateEmailOtpRequest, UpdateEmailRequest)
    Mã của mục đích này không dùng được cho mục đích khác.

    -----------------------------------------------------
    Vòng đời của 1 mã OTP
    -----------------------------------------------------
    1. Tạo mới: verified = false, expired_at = hiện tại + 5 phút.
    2. Kết thúc theo 1 trong 3 cách:
       - Người dùng nhập đúng            → verified = true.
       - Người dùng yêu cầu mã mới        → mã cũ bị đặt verified = true (vô hiệu hóa).
       - Quá 5 phút mà không dùng         → vẫn verified = false nhưng bị từ chối vì hết hạn.
    Vì vậy verified = true nên hiểu là "mã đã hết hiệu lực" (đã dùng hoặc đã bị thay),
    không chỉ là "đã nhập đúng".

    Số lần nhập sai (tối đa 5) không lưu ở bảng này mà đếm trong Redis (key otp-attempt:{id}),
    vì Redis tăng số đếm an toàn khi nhiều request đến cùng lúc (xem OtpRaceConditionTest).

    -----------------------------------------------------
    Index
    -----------------------------------------------------
    idx_otp_user_purpose_verified (user_id, purpose, verified):
    khớp đúng với truy vấn được dùng nhiều nhất trong OtpService:
        WHERE user_id = ? AND purpose = ? AND verified = false ORDER BY id DESC LIMIT 1
    → tìm mã mới nhất còn dùng được rất nhanh, dù bảng có nhiều dòng.
    Truy vấn này được Spring Data tự sinh từ tên hàm trong OtpVerificationRepository:
        findFirstByUserAndPurposeAndVerifiedFalseOrderByIdDesc(user, purpose)

    -----------------------------------------------------
    Vì sao không có cột created_at?
    -----------------------------------------------------
    Thời điểm tạo được tính ngược từ expired_at: created_at = expired_at - 5 phút
    (dùng để kiểm tra thời gian chờ 60 giây giữa 2 lần gửi).
    Lưu ý: nếu sau này đổi thời hạn OTP (OTP_EXPIRE_MINUTES), các mã tạo trước khi đổi
    sẽ bị tính sai thời điểm tạo trong vài phút. Không ảnh hưởng đáng kể.
*/
@Entity
@Table(
        name = "otp_verifications",
        indexes = {
                @Index(name = "idx_otp_user_purpose_verified", columnList = "user_id, purpose, verified")
        }
)
public class OtpVerification {
    /*
        id: khóa chính, MySQL tự tăng (xem User).
        Id càng lớn = mã càng mới → "ORDER BY id DESC" lấy được mã mới nhất.
    */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
        -----------------------------------------------------
        user: mã OTP này của tài khoản nào
        -----------------------------------------------------
        Đây là quan hệ đầu tiên trong package (đọc kỹ, Song và Playlist dùng lại y hệt).

        - @ManyToOne: nhiều mã OTP thuộc về 1 User (1 người có thể nhận nhiều mã theo thời gian).
          Đọc tên annotation từ phía class này: "nhiều (Many) OtpVerification → một (One) User".
        - @JoinColumn(name = "user_id", nullable = false): trong bảng có cột user_id (khóa ngoại)
          trỏ tới users.id, bắt buộc có giá trị.

        Trong database chỉ lưu con số user_id, nhưng trong Java thuộc tính có kiểu User:
            otp.getUser()         → đối tượng User đầy đủ
            otp.setUser(user)     → Hibernate tự lấy user.getId() để ghi vào cột user_id
        → Code làm việc với đối tượng, không phải tự quản lý con số id.

        - fetch = FetchType.LAZY (tải lười): khi lấy 1 OtpVerification từ database,
          Hibernate chưa lấy thông tin User ngay, chỉ lấy khi thật sự cần đọc dữ liệu của User
          (VD otp.getUser().getEmail()).
          Tiết kiệm 1 câu truy vấn vì OtpService thường không cần đọc lại User
          (đã có sẵn User từ trước khi tìm mã).
          Ngược lại FetchType.EAGER (tải ngay): luôn đọc kèm User dù không dùng tới.
          @ManyToOne mặc định là EAGER, nên phải ghi rõ LAZY.
    */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /*
        otpCode: mã 6 chữ số, lưu dạng chuỗi (giữ được số 0 ở đầu, VD "012345").
        length = 10: dư chỗ, phòng sau này đổi sang mã dài hơn.
        Mã được tạo bằng SecureRandom (ngẫu nhiên an toàn, khó đoán) trong OtpService.
    */
    @Column(name = "otp_code", nullable = false, length = 10)
    private String otpCode;

    /*
        purpose: mục đích của mã, lưu dạng chữ (VD "CHANGE_EMAIL").
        Giải thích @Enumerated(EnumType.STRING): xem accountStatus trong User.
        Không ghi name → tên cột là purpose.
    */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OtpPurpose purpose;

    /*
        newEmail: chỉ dùng cho mục đích CHANGE_EMAIL, các mục đích khác để null.
        Email mới được giữ tạm ở đây trong lúc chờ người dùng nhập OTP,
        nhập đúng thì mới chép sang bảng users (xem UpdateEmailRequest).
        length = 100: khớp với cột email của bảng users.
    */
    @Column(name = "new_email", length = 100)
    private String newEmail;

    /*
        expiredAt: thời điểm hết hạn = lúc tạo + 5 phút.
        LocalDateTime → cột kiểu DATETIME trong MySQL, theo giờ của máy chủ.
    */
    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    /*
        verified: mã đã hết hiệu lực chưa (xem "Vòng đời của 1 mã OTP" ở trên).
        - boolean (nguyên thủy): luôn là true hoặc false, không bao giờ null.
        - = false: mã mới tạo luôn chưa dùng.
        - Trong MySQL thành cột kiểu bit hoặc tinyint: 0 = false, 1 = true.
    */
    @Column(nullable = false)
    private boolean verified = false;

    /*
        -----------------------------------------------------
        Getter và setter
        -----------------------------------------------------
        OtpService dùng setter để tạo mã mới (setUser, setOtpCode, setPurpose, setExpiredAt...)
        và để vô hiệu hóa mã (setVerified(true)), rồi save().

        isVerified() thay vì getVerified(): thuộc tính boolean nguyên thủy đặt tên getter là is + tên.
    */
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }

    public OtpPurpose getPurpose() { return purpose; }
    public void setPurpose(OtpPurpose purpose) { this.purpose = purpose; }

    public String getNewEmail() { return newEmail; }
    public void setNewEmail(String newEmail) { this.newEmail = newEmail; }

    public LocalDateTime getExpiredAt() { return expiredAt; }
    public void setExpiredAt(LocalDateTime expiredAt) { this.expiredAt = expiredAt; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
}