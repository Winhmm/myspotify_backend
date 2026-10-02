package com.winhmm.myspotify.service;

import com.winhmm.myspotify.dto.request.*;
import com.winhmm.myspotify.dto.response.LoginResponse;
import com.winhmm.myspotify.entity.User;
import com.winhmm.myspotify.enums.AccountStatus;
import com.winhmm.myspotify.enums.OtpPurpose;
import com.winhmm.myspotify.enums.Role;
import com.winhmm.myspotify.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, OtpService otpService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.otpService = otpService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(RegisterRequest request) {
        Optional<User> existing = userRepository.findByEmail(request.getEmail());
        if(existing.isPresent()) {
            User oldUser = existing.get();

            /*
                Nếu email đã tồn tại nhưng tài khoản chưa được xác thực, thì gửi lại OTP để xác thực.

                Nếu email đã tồn tại và tài khoản đã được xác thực, thì báo lỗi "Email already exists".
            */
            if(oldUser.getAccountStatus() == AccountStatus.UNVERIFIED) {
                otpService.generateAndSend(oldUser, OtpPurpose.REGISTER);
                return;
            }

            throw new IllegalArgumentException("Email already exists");
        }

        if(userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setAccountStatus(AccountStatus.UNVERIFIED);
        user.setRole(Role.USER);

        userRepository.save(user);

        otpService.generateAndSend(user, OtpPurpose.REGISTER);
    }

    @Transactional
    public void verifyOtp(VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Email does not exist"));

        otpService.verifyAndConsume(user, request.getOtpCode(), OtpPurpose.REGISTER);

        user.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(user);
    }

    /*
        Dùng chung 1 message cho cả sai email và sai mật khẩu.
    */
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Email or password is incorrect"));

        if(!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Email or password is incorrect");
        }

        if(user.getAccountStatus() == AccountStatus.UNVERIFIED) {
            throw new IllegalArgumentException("Account is not verified yet");
        }

        if(user.getAccountStatus() == AccountStatus.DISABLED) {
            throw new IllegalArgumentException("Account has been disabled");
        }

        if(user.getAccountStatus() == AccountStatus.DELETED) {
            throw new IllegalArgumentException("Account has been deleted");
        }

        /*
            Token tạm thời là null vì chưa làm JWT
        */
        return new LoginResponse(null, user.getId(), user.getUsername(), user.getRole());
    }

    /*
        Quên mật khẩu được chia thành 2 request riêng:

        1. Request forgotPassword:
        - User nhập email → Backend gửi OTP về email.

        2. Request resetPassword:
        - User nhập OTP + mật khẩu mới.
        - Frontend tự gửi lại email đã nhập ở bước 1, User không cần nhập email lần 2.
        - Backend dùng email để tìm User, kiểm tra OTP hợp lệ → đổi mật khẩu mới.
    */
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Email does not exist"));

        otpService.generateAndSend(user, OtpPurpose.RESET_PASSWORD);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Email does not exist"));

        otpService.verifyAndConsume(user, request.getOtpCode(), OtpPurpose.RESET_PASSWORD);

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
