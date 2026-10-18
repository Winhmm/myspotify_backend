package com.winhmm.myspotify.repository;

import com.winhmm.myspotify.entity.OtpVerification;
import com.winhmm.myspotify.entity.User;
import com.winhmm.myspotify.enums.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/*
    =====================================================
    OTPVERIFICATIONREPOSITORY - TRUY CẬP BẢNG OTP_VERIFICATIONS (MYSQL)
    =====================================================

    Chỉ được dùng trong OtpService.
    Ngoài method tự khai báo bên dưới, còn có sẵn save() từ JpaRepository
    để lưu mã mới và đánh dấu mã đã dùng.
*/
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {
    /*
        =====================================================
        LẤY MÃ OTP MỚI NHẤT CÒN DÙNG ĐƯỢC
        =====================================================

        Tên method được Spring tách thành từng phần:
            findFirst         → LIMIT 1 (chỉ lấy 1 kết quả)
            ByUser            → WHERE user_id = ?
            AndPurpose        → AND purpose = ?
            AndVerifiedFalse  → AND verified = false   (chưa dùng, chưa bị vô hiệu hóa)
            OrderByIdDesc     → ORDER BY id DESC        (id lớn nhất = mã tạo gần nhất)

        Câu SQL tương đương:
            SELECT * FROM otp_verifications
            WHERE user_id = ? AND purpose = ? AND verified = false
            ORDER BY id DESC LIMIT 1

        Truyền đối tượng User (không phải userId): Spring tự lấy user.getId() để so với cột user_id.

        Dùng ở 3 chỗ trong OtpService:
        1. Kiểm tra thời gian chờ 60 giây trước khi gửi mã mới.
        2. Vô hiệu hóa lần lượt các mã cũ.
        3. Lấy mã để so khi người dùng nhập OTP.

        Index idx_otp_user_purpose_verified (user_id, purpose, verified) trong entity
        khớp đúng với điều kiện WHERE này → truy vấn nhanh dù bảng có nhiều dòng.
    */
    Optional<OtpVerification> findFirstByUserAndPurposeAndVerifiedFalseOrderByIdDesc(User user, OtpPurpose purpose);
}
