package com.winhmm.myspotify.service;

import com.winhmm.myspotify.entity.OtpVerification;
import com.winhmm.myspotify.entity.User;
import com.winhmm.myspotify.enums.AccountStatus;
import com.winhmm.myspotify.enums.ArtistRequestStatus;
import com.winhmm.myspotify.enums.OtpPurpose;
import com.winhmm.myspotify.enums.Role;
import com.winhmm.myspotify.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/*
    =====================================================
    USERSERVICE - NGHIỆP VỤ TÀI KHOẢN CÁ NHÂN (UC07 - UC14, UC19)
    =====================================================

    Các chức năng:
        getProfile(userId)                → lấy User theo id (UC07, dùng chung)
        getByEmail(email)                 → lấy User đang đăng nhập từ email trong token
        updateProfile(...)                → sửa họ tên (UC08)
        requestChangePasswordOtp(...)     → đổi mật khẩu bước 1: kiểm tra mật khẩu, gửi OTP (UC09)
        changePassword(...)               → đổi mật khẩu bước 2: kiểm tra OTP, lưu mật khẩu mới (UC09)
        updateAvatar(...)                 → đổi ảnh đại diện (UC10)
        disableAccount(...)               → tự vô hiệu hóa tài khoản (UC11)
        deleteAccount(...)                → tự xóa tài khoản, xóa mềm (UC12)
        requestUpdateEmailOtp(...)        → đổi email bước 1: gửi OTP tới email mới (UC13)
        updateEmail(...)                  → đổi email bước 2: kiểm tra OTP, cập nhật email (UC13)
        requestBecomeArtist(...)          → gửi yêu cầu trở thành Artist (UC14)
        updateArtistProfile(...)          → Artist sửa nghệ danh và giới thiệu (UC19)

    Được gọi từ UserController. Mọi tài khoản đã đăng nhập đều dùng được,
    một số chức năng giới hạn role ngay trong Service (UC14 chỉ USER, UC19 chỉ ARTIST).

    -----------------------------------------------------
    Tham số userId đến từ đâu?
    -----------------------------------------------------
    Luôn do UserController lấy từ token (email → tìm User → lấy id),
    không bao giờ lấy từ URL hay body.
    → Người dùng chỉ thao tác được trên tài khoản của chính mình.

    -----------------------------------------------------
    Khi nào dùng @Transactional?
    -----------------------------------------------------
    Chỉ các hàm có gửi hoặc kiểm tra OTP, vì chúng ghi nhiều thứ vào database cùng lúc
    (vô hiệu hóa OTP cũ, lưu OTP mới, đổi mật khẩu...) và cần rollback toàn bộ khi có lỗi.
    Ví dụ: gửi email OTP thất bại → OTP vừa tạo bị hủy, các OTP cũ được khôi phục.

    Các hàm còn lại chỉ có 1 lần save(), bản thân save() đã tự chạy trong 1 transaction.

    -----------------------------------------------------
    Những thay đổi buộc đăng nhập lại
    -----------------------------------------------------
    Đổi mật khẩu, đổi email, vô hiệu hóa, xóa tài khoản → xóa phiên trong Redis
    → token hiện tại mất hiệu lực ngay (401) → frontend chuyển về trang đăng nhập.
*/
@Service
public class UserService {
    private static final String AVATAR_FOLDER = "avatars";

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;
    private final FileStorageService fileStorageService;

    public UserService(UserRepository userRepository, OtpService otpService,
                       PasswordEncoder passwordEncoder, SessionService sessionService,
                       FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.otpService = otpService;
        this.passwordEncoder = passwordEncoder;
        this.sessionService = sessionService;
        this.fileStorageService = fileStorageService;
    }

    /*
        =====================================================
        CÁC HÀM DÙNG CHUNG
        =====================================================
    */

    /*
        -----------------------------------------------------
        UC07 - Lấy User theo id
        -----------------------------------------------------
        Dùng cho xem thông tin cá nhân, và là bước đầu tiên của mọi chức năng bên dưới.
        Không có → "User does not exist".
    */
    public User getProfile(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));
    }

    /*
        -----------------------------------------------------
        Lấy User đang đăng nhập từ email trong token
        -----------------------------------------------------
        UserController gọi hàm này với authentication.getName() (email trong token)
        để lấy id, rồi truyền id vào các hàm khác.

        Trường hợp không tìm thấy rất hiếm, vì JwtAuthenticationFilter đã kiểm tra
        email tồn tại trước khi cho request đi qua.
    */
    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));
    }

    /*
        -----------------------------------------------------
        Kiểm tra mật khẩu hiện tại
        -----------------------------------------------------
        Dùng trước các thao tác quan trọng: đổi mật khẩu, vô hiệu hóa, xóa tài khoản.
        → Người mượn máy lúc tài khoản đang đăng nhập sẵn không làm được các việc này.

        passwordEncoder.matches(mậtKhẩuGốc, chuỗiĐãBăm):
        BCrypt lấy "muối" có sẵn trong chuỗi đã băm, băm lại mật khẩu vừa nhập rồi so sánh.
    */
    private void checkPassword(User user, String rawPassword) {
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new IllegalArgumentException("Password does not match");
        }
    }

    /*
        -----------------------------------------------------
        Kiểm tra tài khoản đang ACTIVE
        -----------------------------------------------------
        Thực tế tài khoản không ACTIVE đã bị JwtAuthenticationFilter chặn từ trước (401).
        Đây là lớp kiểm tra dự phòng, để Service tự đúng
        dù sau này được gọi từ chỗ khác không đi qua filter.
    */
    private void checkActive(User user) {
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Account is not active");
        }
    }

    /*
        =====================================================
        UC08 - CẬP NHẬT THÔNG TIN CÁ NHÂN
        =====================================================

        Chỉ sửa họ tên. Độ dài đã được kiểm tra ở UpdateProfileRequest (tối đa 100 ký tự).
        Trả về User sau khi lưu, để Controller trả thông tin mới cho frontend.
    */
    public User updateProfile(Long userId, String fullName) {
        User user = getProfile(userId);
        user.setFullName(fullName);
        return userRepository.save(user);
    }

    /*
        =====================================================
        UC09 - ĐỔI MẬT KHẨU (BƯỚC 1: XÁC NHẬN MẬT KHẨU, GỬI OTP)
        =====================================================

        1. Kiểm tra mật khẩu hiện tại. Sai → "Password does not match".
        2. Tạo và gửi OTP mục đích CHANGE_PASSWORD tới email hiện tại.

        Bảo vệ 2 lớp:
        - Không biết mật khẩu hiện tại → bị chặn ở bước 1.
        - Biết mật khẩu nhưng không vào được hộp thư → bị chặn ở bước 2.

        Lỗi có thể gặp từ OtpService:
        - Chưa đủ 60 giây kể từ mã trước → "Please wait 60 seconds...".
        - Gửi email thất bại → 503, OTP vừa tạo bị hủy (rollback).
    */
    @Transactional
    public void requestChangePasswordOtp(Long userId, String currentPassword) {
        User user = getProfile(userId);
        checkPassword(user, currentPassword);
        otpService.generateAndSend(user, OtpPurpose.CHANGE_PASSWORD);
    }

    /*
        =====================================================
        UC09 - ĐỔI MẬT KHẨU (BƯỚC 2: KIỂM TRA OTP, LƯU MẬT KHẨU MỚI)
        =====================================================

        1. Kiểm tra OTP mục đích CHANGE_PASSWORD (hạn dùng, số lần sai, có khớp không).
        2. Băm mật khẩu mới bằng BCrypt rồi lưu.
        3. Xóa phiên trong Redis → đăng xuất khỏi mọi thiết bị, kể cả thiết bị đang dùng.
           Frontend chuyển về trang đăng nhập để đăng nhập bằng mật khẩu mới.

        Không kiểm tra lại mật khẩu cũ ở bước này, vì đã kiểm tra ở bước 1
        (OTP chỉ được gửi khi mật khẩu cũ đúng).
    */
    @Transactional
    public void changePassword(Long userId, String otpCode, String newPassword) {
        User user = getProfile(userId);

        otpService.verifyAndConsume(user, otpCode, OtpPurpose.CHANGE_PASSWORD);

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        /*
            Đổi mật khẩu → đăng xuất khỏi mọi thiết bị (kể cả thiết bị hiện tại).
            Frontend chuyển về trang đăng nhập để đăng nhập bằng mật khẩu mới.
        */
        sessionService.deleteSession(user.getEmail());
    }

    /*
        =====================================================
        UC10 - CẬP NHẬT ẢNH ĐẠI DIỆN
        =====================================================

        -----------------------------------------------------
        Luồng xử lý
        -----------------------------------------------------
        1. Ghi nhớ đường dẫn ảnh cũ (có thể null nếu chưa từng đổi ảnh).
        2. FileStorageService.saveImage() kiểm tra và lưu ảnh mới:
           - Chỉ nhận JPG, PNG, WEBP thật (kiểm tra chữ ký file), tối đa 2MB.
           - Đặt tên ngẫu nhiên bằng UUID, lưu vào {uploadDir}/avatars/.
           - Trả về đường dẫn, VD "/uploads/avatars/3f2c8a1e.jpg".
           Ảnh không hợp lệ → báo lỗi tại đây, chưa có gì thay đổi.
        3. Lưu đường dẫn mới vào database.
        4. Lưu thành công → xóa ảnh cũ trên ổ đĩa (tránh đầy ổ đĩa vì ảnh không còn dùng).
           Lưu thất bại → xóa ảnh mới vừa lưu, giữ nguyên ảnh cũ, rồi ném lại lỗi.

        -----------------------------------------------------
        Vì sao không dùng @Transactional mà tự viết try/catch?
        -----------------------------------------------------

        @Transactional chỉ hủy được thay đổi trong database,
        không xóa được file đã ghi xuống ổ đĩa.
        Nếu lưu database lỗi mà không tự xóa ảnh mới, ổ đĩa sẽ có file "mồ côi"
        (không bản ghi nào trỏ tới). Khối catch dọn dẹp file đó.

        Vì sao xóa ảnh cũ sau khi lưu database, không xóa trước?
        Nếu xóa trước mà lưu database lỗi, người dùng mất luôn ảnh cũ.
        Xóa sau thì trường hợp xấu nhất chỉ là còn sót 1 file cũ trên ổ đĩa.

        fileStorageService.delete(null) không làm gì, nên người chưa có ảnh cũ vẫn chạy bình thường.
    */
    public User updateAvatar(Long userId, MultipartFile file) {
        User user = getProfile(userId);

        String oldAvatarUrl = user.getAvatarUrl();
        String newAvatarUrl = fileStorageService.saveImage(file, AVATAR_FOLDER);

        try {
            user.setAvatarUrl(newAvatarUrl);
            User saved = userRepository.save(user);

            fileStorageService.delete(oldAvatarUrl);
            return saved;
        } catch (RuntimeException e) {
            fileStorageService.delete(newAvatarUrl);
            throw e;
        }
    }

    /*
        =====================================================
        UC13 - CẬP NHẬT EMAIL (BƯỚC 1: GỬI OTP TỚI EMAIL MỚI)
        =====================================================

        1. Email mới phải khác email hiện tại.
           equalsIgnoreCase: so sánh không phân biệt hoa thường,
           "MayChieu@Gmail.com" và "maychieu@gmail.com" được coi là giống nhau.
        2. Email mới chưa được tài khoản nào dùng
           (kể cả tài khoản DELETED, vì tài khoản đã xóa vẫn giữ email).
        3. Tạo OTP mục đích CHANGE_EMAIL:
           - Lưu email mới vào bản ghi OTP (cột new_email).
           - Gửi mã tới email mới, để chứng minh người dùng sở hữu email đó.
           Bảng users chưa thay đổi gì ở bước này.
    */
    @Transactional
    public void requestUpdateEmailOtp(Long userId, String newEmail) {
        User user = getProfile(userId);

        if (newEmail.equalsIgnoreCase(user.getEmail())) {
            throw new IllegalArgumentException("New email must be different from current email");
        }

        if (userRepository.existsByEmail(newEmail)) {
            throw new IllegalArgumentException("Email already exists");
        }

        otpService.generateAndSend(user, OtpPurpose.CHANGE_EMAIL, newEmail);
    }

    /*
        =====================================================
        UC13 - CẬP NHẬT EMAIL (BƯỚC 2: KIỂM TRA OTP, ĐỔI EMAIL)
        =====================================================

        1. Kiểm tra OTP mục đích CHANGE_EMAIL.
           verifyAndConsume trả về bản ghi OTP, trong đó có email mới (otp.getNewEmail()).
           Email mới lấy từ database, không lấy từ request
           → không thể nhận mã ở email A rồi đổi sang email B.
        2. Kiểm tra lại email mới chưa bị ai dùng.
           Trong lúc người dùng mở hộp thư lấy mã, có thể có người khác vừa đăng ký email đó.
        3. Xóa phiên của email cũ trong Redis.
           Phải xóa trước khi đổi email, vì key Redis là session:<email cũ>.
           Đổi email xong mới xóa thì sẽ xóa nhầm key session:<email mới> (không tồn tại),
           còn phiên cũ vẫn sống.
        4. Đổi email trong database.

        Token hiện tại chứa email cũ và phiên cũ đã bị xóa
        → token mất hiệu lực → frontend chuyển về trang đăng nhập để đăng nhập bằng email mới.
    */
    @Transactional
    public void updateEmail(Long userId, String otpCode) {
        User user = getProfile(userId);

        OtpVerification otp = otpService.verifyAndConsume(user, otpCode, OtpPurpose.CHANGE_EMAIL);

        if (userRepository.existsByEmail(otp.getNewEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        /*
            Xóa phiên của EMAIL CŨ trước khi đổi
            (key Redis là session:<email cũ>).
        */
        sessionService.deleteSession(user.getEmail());

        user.setEmail(otp.getNewEmail());
        userRepository.save(user);
    }

    /*
        =====================================================
        UC11 - VÔ HIỆU HÓA TÀI KHOẢN
        =====================================================

        1. Tài khoản phải đang ACTIVE.
        2. Mật khẩu phải đúng (xác nhận chính chủ).
        3. Chuyển sang DISABLED.
        4. Xóa phiên trong Redis → đăng xuất ngay.

        Sau khi vô hiệu hóa:
        - Không đăng nhập được: "Account has been disabled".
        - Nếu là Artist: mọi bài hát tự ẩn khỏi người nghe.
        - Muốn dùng lại: liên hệ Admin mở khóa (AdminService.unlockAccount).
    */
    public void disableAccount(Long userId, String currentPassword) {
        User user = getProfile(userId);
        checkActive(user);
        checkPassword(user, currentPassword);

        user.setAccountStatus(AccountStatus.DISABLED);
        userRepository.save(user);

        sessionService.deleteSession(user.getEmail());
    }

    /*
        =====================================================
        UC12 - XÓA TÀI KHOẢN (XÓA MỀM)
        =====================================================

        Giống UC11, chỉ khác trạng thái cuối là DELETED.

        Xóa mềm: không xóa dòng trong bảng users, chỉ đổi trạng thái.
        - Giữ nguyên dữ liệu liên quan (bài hát, OTP...) không bị lỗi khóa ngoại.
        - Email và username vẫn bị giữ, không dùng để đăng ký tài khoản mới được.
        - Admin không mở lại được (chỉ mở khóa tài khoản DISABLED).
    */
    public void deleteAccount(Long userId, String currentPassword) {
        User user = getProfile(userId);
        checkActive(user);
        checkPassword(user, currentPassword);

        user.setAccountStatus(AccountStatus.DELETED);
        userRepository.save(user);

        sessionService.deleteSession(user.getEmail());
    }

    /*
        =====================================================
        UC14 - GỬI YÊU CẦU TRỞ THÀNH ARTIST
        =====================================================

        1. Tài khoản phải ACTIVE.
        2. Chỉ role USER được gửi.
           ARTIST thì đã là nghệ sĩ rồi, ADMIN không được kiêm nghệ sĩ.
        3. artistRequestStatus phải là:
           - NONE (chưa gửi bao giờ), hoặc
           - REJECTED (từng bị từ chối, được gửi lại).
           Đang PENDING hoặc đã APPROVED → báo lỗi, tránh gửi trùng.
        4. Lưu nghệ danh, bio, chuyển artistRequestStatus sang PENDING.
           Role vẫn là USER cho tới khi Admin duyệt.

        Bước tiếp theo: Admin duyệt (AdminService.approveArtistRequest)
        hoặc từ chối (AdminService.rejectArtistRequest).
    */
    public void requestBecomeArtist(Long userId, String artistName, String bio) {
        User user = getProfile(userId);
        checkActive(user);

        if (user.getRole() != Role.USER) {
            throw new IllegalArgumentException("Only users with role USER can request to become an artist");
        }

        if (user.getArtistRequestStatus() != ArtistRequestStatus.NONE
                && user.getArtistRequestStatus() != ArtistRequestStatus.REJECTED) {
            throw new IllegalArgumentException("An artist request already exists or has been approved");
        }

        user.setArtistName(artistName);
        user.setBio(bio);
        user.setArtistRequestStatus(ArtistRequestStatus.PENDING);

        userRepository.save(user);
    }

    /*
        =====================================================
        UC19 - CẬP NHẬT HỒ SƠ ARTIST
        =====================================================

        Chỉ role ARTIST được sửa nghệ danh và giới thiệu.
        USER đang có yêu cầu PENDING không sửa được ở đây:
        muốn đổi nghệ danh thì chờ Admin xử lý xong yêu cầu.

        Nghệ danh mới hiện ngay trên mọi bài hát của Artist,
        vì bài hát chỉ lưu artist_id, tên được lấy từ bảng users mỗi lần truy vấn.
    */
    public User updateArtistProfile(Long userId, String artistName, String bio) {
        User user = getProfile(userId);

        if (user.getRole() != Role.ARTIST) {
            throw new IllegalArgumentException("Only artists can update artist profile");
        }

        user.setArtistName(artistName);
        user.setBio(bio);

        return userRepository.save(user);
    }
}
