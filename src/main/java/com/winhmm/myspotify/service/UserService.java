package com.winhmm.myspotify.service;

import com.winhmm.myspotify.entity.OtpVerification;
import com.winhmm.myspotify.entity.User;
import com.winhmm.myspotify.enums.AccountStatus;
import com.winhmm.myspotify.enums.ArtistRequestStatus;
import com.winhmm.myspotify.enums.OtpPurpose;
import com.winhmm.myspotify.enums.Role;
import com.winhmm.myspotify.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class UserService {
    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024; // 2MB
    private static final String AVATAR_URL_PREFIX = "/uploads/avatars/";

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;

    @Value("${app.upload-dir}")
    private String uploadDir;

    public UserService(UserRepository userRepository, OtpService otpService,
                       PasswordEncoder passwordEncoder, SessionService sessionService) {
        this.userRepository = userRepository;
        this.otpService = otpService;
        this.passwordEncoder = passwordEncoder;
        this.sessionService = sessionService;
    }

    public User getProfile(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));
    }

    /*
        Controller dùng để lấy User đang đăng nhập từ email trong token.
    */
    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));
    }

    private void checkPassword(User user, String rawPassword) {
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new IllegalArgumentException("Password does not match");
        }
    }

    private void checkActive(User user) {
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Account is not active");
        }
    }

    /*
        UC08 - Cập nhật thông tin cá nhân.
    */
    public User updateProfile(Long userId, String fullName) {
        User user = getProfile(userId);
        user.setFullName(fullName);
        return userRepository.save(user);
    }

    /*
        UC09 - Đổi mật khẩu (bước 1): kiểm tra mật khẩu hiện tại → gửi OTP.
    */
    @Transactional
    public void requestChangePasswordOtp(Long userId, String currentPassword) {
        User user = getProfile(userId);
        checkPassword(user, currentPassword);
        otpService.generateAndSend(user, OtpPurpose.CHANGE_PASSWORD);
    }

    /*
        UC09 - Đổi mật khẩu (bước 2): xác thực OTP → cập nhật mật khẩu mới.
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
        UC10 - Cập nhật ảnh đại diện.
        - Chỉ nhận JPG / PNG / WEBP, tối đa 2MB.
        - Lưu file mới thành công → xóa file avatar cũ (tránh đầy ổ đĩa).
    */
    public User updateAvatar(Long userId, MultipartFile file) {
        User user = getProfile(userId);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new IllegalArgumentException("Avatar must not exceed 2MB");
        }

        String contentType = file.getContentType();
        String extension;

        if ("image/jpeg".equals(contentType)) {
            extension = ".jpg";
        } else if ("image/png".equals(contentType)) {
            extension = ".png";
        } else if ("image/webp".equals(contentType)) {
            extension = ".webp";
        } else {
            throw new IllegalArgumentException("Only JPG, PNG or WEBP images are allowed");
        }

        String fileName = UUID.randomUUID() + extension;

        try {
            Path avatarDir = Paths.get(uploadDir, "avatars");
            Files.createDirectories(avatarDir);

            Path target = avatarDir.resolve(fileName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Could not save avatar file", e);
        }

        String oldAvatarUrl = user.getAvatarUrl();

        user.setAvatarUrl(AVATAR_URL_PREFIX + fileName);
        User saved = userRepository.save(user);

        deleteOldAvatar(oldAvatarUrl);
        return saved;
    }

    /*
        Chỉ xóa file avatar do hệ thống lưu (bắt đầu bằng /uploads/avatars/).
        Xóa lỗi thì bỏ qua, không ảnh hưởng việc cập nhật avatar.
    */
    private void deleteOldAvatar(String avatarUrl) {
        if (avatarUrl == null || !avatarUrl.startsWith(AVATAR_URL_PREFIX)) {
            return;
        }

        String oldFileName = avatarUrl.substring(AVATAR_URL_PREFIX.length());
        try {
            Files.deleteIfExists(Paths.get(uploadDir, "avatars", oldFileName));
        } catch (IOException ignored) {
        }
    }

    /*
        UC13 - Cập nhật email (bước 1): kiểm tra email mới → gửi OTP tới email mới.
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
        UC13 - Cập nhật email (bước 2): xác thực OTP → cập nhật email.
        Lưu ý: token cũ chứa email cũ → hết hiệu lực, Frontend phải đăng nhập lại.
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
        UC11 - Vô hiệu hóa tài khoản.
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
        UC12 - Xóa tài khoản (soft delete).
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
        UC14 - Gửi yêu cầu trở thành Artist.
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
        UC19 - Cập nhật hồ sơ Artist.
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
