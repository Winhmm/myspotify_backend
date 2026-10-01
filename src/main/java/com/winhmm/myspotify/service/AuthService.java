package com.winhmm.myspotify.service;

import com.winhmm.myspotify.dto.request.*;
import com.winhmm.myspotify.dto.response.LoginResponse;
import com.winhmm.myspotify.entity.User;
import com.winhmm.myspotify.enums.AccountStatus;
import com.winhmm.myspotify.enums.OtpPurpose;
import com.winhmm.myspotify.enums.Role;
import com.winhmm.myspotify.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final OtpService otpService;

    public AuthService(UserRepository userRepository, OtpService otpService) {
        this.userRepository = userRepository;
        this.otpService = otpService;
    }

    public void register(RegisterRequest request) {
        if(userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setAccountStatus(AccountStatus.UNVERIFIED);
        user.setRole(Role.USER);

        userRepository.save(user);

        otpService.generateAndSend(user, OtpPurpose.REGISTER);
    }

//    private void generateAndSaveOtp(User user, OtpPurpose purpose) {
//        String otpCode = String.format("%06d", new Random().nextInt(1_000_000));
//
//        OtpVerification otp = new OtpVerification();
//        otp.setUser(user);
//        otp.setOtpCode(otpCode);
//        otp.setPurpose(purpose);
//        otp.setExpiredAt(LocalDateTime.now().plusMinutes(5));
//        otp.setVerified(false);
//
//        otpVerificationRepository.save(otp);
//
//        System.out.println("=== OTP cho " + user.getEmail() + " (" + purpose + "): " + otpCode + " ===");
//    }

//    public void verifyOtp(VerifyOtpRequest request) {
//        User user = userRepository.findByEmail(request.getEmail())
//                .orElseThrow(() -> new IllegalArgumentException("Email does not exist"));
//
//        OtpVerification otp = otpVerificationRepository
//                .findFirstByUserAndPurposeAndVerifiedFalseOrderByIdDesc(user, OtpPurpose.REGISTER)
//                .orElseThrow(() -> new IllegalArgumentException("Otp code does not exist"));
//
//        if(otp.getExpiredAt().isBefore(LocalDateTime.now())) {
//            throw new IllegalArgumentException("Otp code has expired");
//        }
//
//        if(!otp.getOtpCode().equals(request.getOtpCode())) {
//            throw new IllegalArgumentException("Otp code does not match");
//        }
//
//        otp.setVerified(true);
//        otpVerificationRepository.save(otp);
//
//        user.setAccountStatus(AccountStatus.ACTIVE);
//        userRepository.save(user);
//    }

    public void verifyOtp(VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Email does not exist"));

        otpService.verifyAndConsume(user, request.getOtpCode(), OtpPurpose.REGISTER);

        user.setAccountStatus(AccountStatus.ACTIVE);

        userRepository.save(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Email does not exist"));

        if(!user.getPassword().equals(request.getPassword())) {
            throw new IllegalArgumentException("Password does not match");
        }

        /*
            Không cho phép đăng nhập khi:

            - Tài khoản chưa được xác thực.
            - Tài khoản đã bị vô hiệu hóa.
            - Tài khoản đã bị xóa.
        */
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
            Hiện tại token đang truyền là null vì chưa tạo JWT.
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

//    public void resetPassword(ResetPasswordRequest request) {
//        User user = userRepository.findByEmail(request.getEmail())
//                .orElseThrow(() -> new IllegalArgumentException("Email does not exist"));
//
//        OtpVerification otp = otpVerificationRepository
//                .findFirstByUserAndPurposeAndVerifiedFalseOrderByIdDesc(user, OtpPurpose.RESET_PASSWORD)
//                .orElseThrow(() -> new IllegalArgumentException("Otp code does not exist"));
//
//        if(otp.getExpiredAt().isBefore(LocalDateTime.now())) {
//            throw new IllegalArgumentException("Otp code has expired");
//        }
//
//        if(!otp.getOtpCode().equals(request.getOtpCode())) {
//            throw new IllegalArgumentException("Otp code does not match");
//        }
//
//        otp.setVerified(true);
//        otpVerificationRepository.save(otp);
//
//        user.setPassword(request.getNewPassword());
//        userRepository.save(user);
//    }

    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Email does not exist"));

        otpService.verifyAndConsume(user, request.getOtpCode(), OtpPurpose.RESET_PASSWORD);

        user.setPassword(request.getNewPassword());

        userRepository.save(user);
    }
}
