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

    (Repository là gì, cách Spring đọc tên method: xem GenreRepository.
     Optional: xem SongLyricsRepository.
     Thứ tự đọc package repository: xem GenreRepository.)

    Chỉ được dùng trong OtpService.
    Ngoài method tự khai báo bên dưới, còn có sẵn save() từ JpaRepository
    để lưu mã mới và đánh dấu mã đã dùng.

    Không có hàm xóa: mọi mã OTP (kể cả đã dùng, đã hết hạn) được giữ lại trong bảng
    để tra cứu lịch sử. Mã "hết hiệu lực" chỉ đơn giản là verified = true.
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

        -----------------------------------------------------
        Vài điểm mới so với các repository trước
        -----------------------------------------------------
        - First: giống Top1. Có nhiều dòng khớp vẫn chỉ lấy dòng đầu tiên (sau khi sắp xếp).
          Không có First, nếu có 2 mã cũ chưa dùng thì Spring báo lỗi "nhiều hơn 1 kết quả".
        - VerifiedFalse: điều kiện "bằng false" viết thẳng vào tên, KHÔNG cần tham số.
          Vì vậy method chỉ có 2 tham số (user, purpose) dù có 3 điều kiện.
          (Tương tự có VerifiedTrue, IsNull, IsNotNull... cũng không cần tham số.)
        - Truyền đối tượng User (không phải userId): Spring tự lấy user.getId() để so với cột user_id.
        - OrderByIdDesc: id tự tăng (1, 2, 3...) → mã tạo sau luôn có id lớn hơn
          → sắp id giảm dần = mã mới nhất đứng đầu.

        Vì sao trả về Optional?
        Người dùng chưa từng yêu cầu mã, hoặc mọi mã đều đã dùng → không có kết quả.
        OtpService xử lý từng trường hợp (xem bên dưới).

        -----------------------------------------------------
        Dùng ở 3 chỗ trong OtpService
        -----------------------------------------------------
        1. Kiểm tra thời gian chờ 60 giây trước khi gửi mã mới:
           có mã chưa dùng được tạo chưa tới 60 giây
           → 400 "Please wait 60 seconds before requesting a new OTP".
        2. Vô hiệu hóa lần lượt các mã cũ:
           gọi lặp lại, mỗi lần lấy mã mới nhất còn verified = false, đánh dấu verified = true rồi lưu,
           tới khi hàm trả về Optional rỗng (không còn mã cũ nào) → chỉ mã sắp tạo còn dùng được.
        3. Lấy mã để so khi người dùng nhập OTP (verifyAndConsume):
           không có mã → 400 "Otp code does not exist".

        Tham số purpose (REGISTER, RESET_PASSWORD...) tách riêng từng loại mã:
        mã đăng ký không dùng được để đặt lại mật khẩu, và ngược lại (xem enum OtpPurpose).

        -----------------------------------------------------
        Index
        -----------------------------------------------------
        Index idx_otp_user_purpose_verified (user_id, purpose, verified) trong entity
        khớp đúng với điều kiện WHERE này → truy vấn nhanh dù bảng có nhiều dòng.
        (MySQL InnoDB còn tự gắn khóa chính id vào cuối mọi index,
         nên ORDER BY id DESC cũng tận dụng được luôn index này.)
    */
    Optional<OtpVerification> findFirstByUserAndPurposeAndVerifiedFalseOrderByIdDesc(User user, OtpPurpose purpose);
}