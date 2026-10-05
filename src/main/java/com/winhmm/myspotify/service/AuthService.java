package com.winhmm.myspotify.service;

import com.winhmm.myspotify.dto.auth.*;
import com.winhmm.myspotify.entity.User;
import com.winhmm.myspotify.enums.AccountStatus;
import com.winhmm.myspotify.enums.OtpPurpose;
import com.winhmm.myspotify.enums.Role;
import com.winhmm.myspotify.repository.UserRepository;
import com.winhmm.myspotify.security.JwtUtil;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

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
        UC01 - Đăng ký.

        - Email chưa tồn tại → tạo User UNVERIFIED → gửi OTP.

        - Email đã tồn tại nhưng UNVERIFIED (đăng ký lại vì OTP hết hạn)
          → cập nhật username/password mới → gửi lại OTP.

        - Email đã tồn tại và đã xác thực → báo lỗi.
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

        userRepository.save(user);

        otpService.generateAndSend(user, OtpPurpose.REGISTER);
    }

    /*
        UC02 - Xác thực Email / OTP: chỉ dành cho tài khoản UNVERIFIED.
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
        UC03 - Đăng nhập.

        Dùng chung 1 message cho cả sai email và sai mật khẩu.
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
            Tạo phiên mới trong Redis (ghi đè phiên cũ nếu đang đăng nhập ở thiết bị khác),
            rồi ghi mã phiên vào token.
        */
        String sessionId = sessionService.createSession(user.getEmail());
        String token = jwtUtil.generateToken(user, sessionId);

        return new LoginResponse(token, user.getId(), user.getUsername(), user.getRole());
    }

    /*
        UC04 - Quên mật khẩu (bước 1): nhập email → gửi OTP.
    */
    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email does not exist"));

        otpService.generateAndSend(user, OtpPurpose.RESET_PASSWORD);
    }

    /*
        UC05 - Đặt lại mật khẩu (bước 2): email (frontend tự gửi lại) + OTP + mật khẩu mới.
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
