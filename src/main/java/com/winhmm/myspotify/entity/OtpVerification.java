package com.winhmm.myspotify.entity;

import com.winhmm.myspotify.enums.OtpPurpose;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/*
    =====================================================
    OTPVERIFICATION - BẢNG OTP_VERIFICATIONS (MÃ XÁC THỰC)
    =====================================================

    Mỗi dòng là 1 mã OTP đã gửi cho người dùng.

    Ví dụ dữ liệu:
        id | user_id | otp_code | purpose         | new_email          | expired_at          | verified
        12 |    6    | 482913   | REGISTER        | null               | 2026-10-18 14:05:00 | true
        13 |    6    | 529301   | CHANGE_PASSWORD | null               | 2026-10-18 15:20:00 | false
        14 |    6    | 118204   | CHANGE_EMAIL    | new@gmail.com      | 2026-10-18 16:00:00 | false

    -----------------------------------------------------
    Vòng đời của 1 mã OTP
    -----------------------------------------------------
    1. Tạo mới: verified = false, expired_at = hiện tại + 5 phút.
    2. Kết thúc theo 1 trong 3 cách:
       - Người dùng nhập đúng            → verified = true.
       - Người dùng yêu cầu mã mới        → mã cũ bị đặt verified = true (vô hiệu hóa).
       - Quá 5 phút mà không dùng         → vẫn verified = false nhưng bị từ chối vì hết hạn.

    -----------------------------------------------------
    Index
    -----------------------------------------------------
    idx_otp_user_purpose_verified (user_id, purpose, verified):
    khớp đúng với truy vấn được dùng nhiều nhất trong OtpService:
        WHERE user_id = ? AND purpose = ? AND verified = false ORDER BY id DESC LIMIT 1
    → tìm mã mới nhất còn dùng được rất nhanh, dù bảng có nhiều dòng.

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
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
        Mã OTP này của tài khoản nào.

        - @ManyToOne: nhiều mã OTP thuộc về 1 User (1 người có thể nhận nhiều mã theo thời gian).
        - @JoinColumn(name = "user_id"): cột khóa ngoại trỏ tới users.id.
        - fetch = FetchType.LAZY: khi lấy 1 OtpVerification từ database,
          Hibernate chưa lấy thông tin User ngay, chỉ lấy khi thật sự gọi otp.getUser().
          Tiết kiệm 1 câu truy vấn vì OtpService thường không cần đọc lại User
          (đã có sẵn User từ trước khi tìm mã).
    */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "otp_code", nullable = false, length = 10)
    private String otpCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OtpPurpose purpose;

    @Column(name = "new_email", length = 100)
    private String newEmail;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    @Column(nullable = false)
    private boolean verified = false;

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
