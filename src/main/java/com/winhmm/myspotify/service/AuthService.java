package com.winhmm.myspotify.service;

import com.winhmm.myspotify.dto.auth.*;
import com.winhmm.myspotify.entity.User;
import com.winhmm.myspotify.enums.AccessStatus;
import com.winhmm.myspotify.enums.AccountStatus;
import com.winhmm.myspotify.enums.OtpPurpose;
import com.winhmm.myspotify.enums.Role;
import com.winhmm.myspotify.repository.UserRepository;
import com.winhmm.myspotify.security.JwtUtil;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/*
    =====================================================
    AUTHSERVICE - NGHIỆP VỤ XÁC THỰC TÀI KHOẢN (UC01 - UC05)
    =====================================================

    Các chức năng:
        register(...)        → đăng ký, gửi OTP (UC01)
        verifyOtp(...)       → xác thực OTP, kích hoạt tài khoản (UC02)
        login(...)           → đăng nhập, tạo phiên và token (UC03)
        forgotPassword(...)  → quên mật khẩu, gửi OTP (UC04)
        resetPassword(...)   → đặt lại mật khẩu bằng OTP (UC05)

    Được gọi từ AuthController. Người chưa đăng nhập cũng gọi được.

    -----------------------------------------------------
    Các thành phần được sử dụng
    -----------------------------------------------------
    - UserRepository : đọc và ghi bảng users.
    - OtpService     : tạo, gửi và kiểm tra mã OTP.
    - PasswordEncoder: băm mật khẩu và so mật khẩu bằng BCrypt (bean trong SecurityConfig).
    - JwtUtil        : tạo token.
    - SessionService : tạo và xóa phiên đăng nhập trong Redis.
    Tất cả được Spring tự truyền vào qua constructor.

    -----------------------------------------------------
    @Transactional là gì?
    -----------------------------------------------------
    Gom mọi thao tác database trong 1 hàm thành 1 khối:
    - Chạy hết không lỗi → lưu tất cả (commit).
    - Có lỗi RuntimeException giữa chừng → hủy tất cả (rollback), database như chưa có gì xảy ra.

    Ví dụ trong register(): đã lưu User, nhưng gửi email OTP thất bại
    → EmailSendException → rollback → User vừa tạo cũng bị hủy
    → người dùng đăng ký lại được ngay với cùng email.

    Lưu ý: rollback chỉ áp dụng cho MySQL. Thay đổi trong Redis (xóa phiên, bộ đếm OTP)
    không bị hủy theo.

    -----------------------------------------------------
    Cách báo lỗi
    -----------------------------------------------------
    Ném IllegalArgumentException kèm message
    → GlobalExceptionHandler trả 400 { "message": "..." } cho frontend.
*/
@Service
public class AuthService {
    private final UserRepository userRepository;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SessionService sessionService;

    public AuthService(UserRepository userRepository, OtpService otpService,
                       PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                       SessionService sessionService) {
        this.userRepository = userRepository;
        this.otpService = otpService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.sessionService = sessionService;
    }

    /*
        =====================================================
        UC01 - ĐĂNG KÝ
        =====================================================

        Có 3 trường hợp, tùy email đã tồn tại hay chưa.

        -----------------------------------------------------
        Trường hợp 1: email đã tồn tại và đã xác thực (ACTIVE, DISABLED, DELETED)
        -----------------------------------------------------
        → 400 "Email already exists".
        Tài khoản DELETED vẫn giữ email, nên không đăng ký lại được bằng email đó.

        -----------------------------------------------------
        Trường hợp 2: email đã tồn tại nhưng chưa xác thực (UNVERIFIED)
        -----------------------------------------------------
        Ví dụ: hôm qua đăng ký nhưng không nhập OTP, mã đã hết hạn.
        Cho phép "đăng ký lại": cập nhật username, mật khẩu mới, rồi gửi OTP mới.
        Đây cũng là cách frontend làm chức năng "gửi lại mã".

        Kiểm tra username:
        - Giữ nguyên username cũ (không phân biệt hoa thường) → không cần kiểm tra trùng.
        - Đổi sang username khác → username đó phải chưa ai dùng.

        Có an toàn không khi người khác đăng ký lại bằng email của mình?
        Có. Kẻ gian đổi được mật khẩu của tài khoản chưa xác thực,
        nhưng mã OTP gửi về email của chủ thật, kẻ gian không kích hoạt được.

        -----------------------------------------------------
        Trường hợp 3: email chưa tồn tại
        -----------------------------------------------------
        1. Username phải chưa ai dùng.
        2. Tạo User mới: UNVERIFIED, role USER, quyền vào web REFUSE, mật khẩu đã băm.
        3. Lưu vào database.
        4. Tạo và gửi OTP mục đích REGISTER.

        -----------------------------------------------------
        Khi có lỗi ở bước gửi OTP
        -----------------------------------------------------
        - Chưa đủ 60 giây kể từ mã trước → "Please wait 60 seconds..." → rollback.
        - Gửi email thất bại → EmailSendException (503) → rollback.
        Cả 2 trường hợp, username và mật khẩu vừa cập nhật (trường hợp 2)
        hoặc User vừa tạo (trường hợp 3) đều bị hủy.

        2 người đăng ký cùng username đúng cùng lúc: cả 2 đều qua bước existsByUsername,
        nhưng ràng buộc UNIQUE trong database chặn người thứ hai
        → DataIntegrityViolationException → 409 "Data already exists".
    */
    @Transactional
    public void register(String username, String email, String password) {
        Optional<User> existing = userRepository.findByEmail(email);

        if (existing.isPresent()) {
            User oldUser = existing.get();

            if (oldUser.getAccountStatus() != AccountStatus.UNVERIFIED) {
                throw new IllegalArgumentException("Email already exists");
            }

            if (!oldUser.getUsername().equalsIgnoreCase(username)
                    && userRepository.existsByUsername(username)) {
                throw new IllegalArgumentException("Username already exists");
            }

            oldUser.setUsername(username);
            oldUser.setPassword(passwordEncoder.encode(password));
            userRepository.save(oldUser);

            otpService.generateAndSend(oldUser, OtpPurpose.REGISTER);
            return;
        }

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setAccountStatus(AccountStatus.UNVERIFIED);
        user.setRole(Role.USER);
        user.setAccessStatus(AccessStatus.REFUSE);

        userRepository.save(user);

        otpService.generateAndSend(user, OtpPurpose.REGISTER);
    }

    /*
        =====================================================
        UC02 - XÁC THỰC OTP ĐĂNG KÝ
        =====================================================

        1. Tìm User theo email. Không có → "Email does not exist".
        2. Chỉ tài khoản UNVERIFIED mới cần xác thực → khác thì "Account is already verified".
        3. OtpService.verifyAndConsume kiểm tra mã:
           hết hạn, sai quá 5 lần, không khớp → ném lỗi, dừng tại đây.
        4. Mã đúng → chuyển tài khoản sang ACTIVE.

        Không tự đăng nhập sau khi xác thực: người dùng đăng nhập ở bước tiếp theo.
        Sau bước này tài khoản vẫn là REFUSE → chưa đăng nhập được
        cho tới khi chủ dự án đổi thành ACCEPT bằng SQL.
    */
    @Transactional
    public void verifyOtp(String email, String otpCode) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email does not exist"));

        if (user.getAccountStatus() != AccountStatus.UNVERIFIED) {
            throw new IllegalArgumentException("Account is already verified");
        }

        otpService.verifyAndConsume(user, otpCode, OtpPurpose.REGISTER);

        user.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(user);
    }

    /*
        =====================================================
        UC03 - ĐĂNG NHẬP
        =====================================================

        -----------------------------------------------------
        Thứ tự kiểm tra (thứ tự này có chủ đích)
        -----------------------------------------------------
        1. Tìm User theo email.
        2. So mật khẩu bằng BCrypt.
           passwordEncoder.matches(mậtKhẩuGốc, chuỗiĐãBăm): BCrypt lấy "muối" có sẵn
           trong chuỗi đã băm, băm lại mật khẩu vừa nhập rồi so 2 kết quả.
        3. Kiểm tra trạng thái tài khoản.
        4. Kiểm tra quyền vào web: khác ACCEPT → "Account has not been approved yet".

        Sai email hoặc sai mật khẩu → cùng 1 message "Email or password is incorrect"
        → người ngoài không đoán được email nào đã đăng ký.

        Trạng thái và quyền vào web được kiểm tra sau mật khẩu → chỉ người biết mật khẩu
        mới biết tài khoản đang bị khóa, chưa xác thực hay chưa được duyệt.

        -----------------------------------------------------
        Tạo phiên và token
        -----------------------------------------------------
        5. sessionService.createSession(email):
           sinh 1 UUID mới, ghi vào Redis (key session:{email}), sống 24 giờ.
           Ghi đè phiên cũ → thiết bị đăng nhập trước đó bị đăng xuất.
        6. jwtUtil.generateToken(user, sessionId):
           tạo JWT chứa email (sub) và UUID vừa sinh (jti).
        7. Trả token, userId, username, role cho frontend.

        Không cần @Transactional vì hàm này không ghi gì vào MySQL.
    */
    public LoginResponse login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email or password is incorrect"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Email or password is incorrect");
        }

        if (user.getAccountStatus() == AccountStatus.UNVERIFIED) {
            throw new IllegalArgumentException("Account is not verified yet");
        }

        if (user.getAccountStatus() == AccountStatus.DISABLED) {
            throw new IllegalArgumentException("Account has been disabled");
        }

        if (user.getAccountStatus() == AccountStatus.DELETED) {
            throw new IllegalArgumentException("Account has been deleted");
        }

        /*
            Chỉ tài khoản được chủ dự án cho phép (ACCEPT) mới đăng nhập được.
            Dùng "!= ACCEPT" (không dùng "== REFUSE") để an toàn:
            nếu cột này vì lý do nào đó bị null thì cũng bị chặn.
        */
        if (user.getAccessStatus() != AccessStatus.ACCEPT) {
            throw new IllegalArgumentException("Account has not been approved yet");
        }

        /*
            Tạo phiên mới trong Redis (ghi đè phiên cũ nếu đang đăng nhập ở thiết bị khác),
            rồi ghi mã phiên vào token.
        */
        String sessionId = sessionService.createSession(user.getEmail());
        String token = jwtUtil.generateToken(user, sessionId);

        return new LoginResponse(token, user.getId(), user.getUsername(), user.getRole());
    }

    /*
        =====================================================
        UC04 - QUÊN MẬT KHẨU (BƯỚC 1: GỬI OTP)
        =====================================================

        1. Tìm User theo email. Không có → "Email does not exist".
        2. Chỉ tài khoản ACTIVE được đặt lại mật khẩu → khác thì "Account is not active".
           Tài khoản bị khóa không dùng được chức năng này để lấy lại quyền truy cập.
        3. Tạo và gửi OTP mục đích RESET_PASSWORD tới email của tài khoản.

        Gửi email thất bại → 503 và mã OTP vừa tạo bị hủy (rollback),
        các mã cũ được khôi phục như trước.
    */
    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email does not exist"));

        if(user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Account is not active");
        }

        otpService.generateAndSend(user, OtpPurpose.RESET_PASSWORD);
    }

    /*
        =====================================================
        UC05 - ĐẶT LẠI MẬT KHẨU (BƯỚC 2: OTP VÀ MẬT KHẨU MỚI)
        =====================================================

        Frontend tự gửi lại email đã nhập ở bước 1 (người dùng chưa đăng nhập, không có token).

        1. Tìm User theo email.
        2. Kiểm tra OTP mục đích RESET_PASSWORD.
        3. Băm mật khẩu mới rồi lưu.
        4. Xóa phiên trong Redis → đăng xuất khỏi mọi thiết bị.
           Lý do: người dùng thường đặt lại mật khẩu vì nghi bị lộ.
           Nếu kẻ gian đang dùng tài khoản thì bị đá ra ngay.

        Không kiểm tra lại trạng thái ACTIVE ở bước này, vì OTP RESET_PASSWORD
        chỉ được tạo cho tài khoản ACTIVE (bước 1).
    */
    @Transactional
    public void resetPassword(String email, String otpCode, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email does not exist"));

        otpService.verifyAndConsume(user, otpCode, OtpPurpose.RESET_PASSWORD);

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        /*
            Đăng xuất khỏi mọi thiết bị: nếu ai đó đang dùng tài khoản
            (lý do thường gặp khiến người dùng phải đặt lại mật khẩu) → bị đá ra ngay.
        */
        sessionService.deleteSession(user.getEmail());
    }
}