package com.winhmm.myspotify.service;

import com.winhmm.myspotify.entity.OtpVerification;
import com.winhmm.myspotify.entity.User;
import com.winhmm.myspotify.enums.AccountStatus;
import com.winhmm.myspotify.enums.ArtistRequestStatus;
import com.winhmm.myspotify.enums.OtpPurpose;
import com.winhmm.myspotify.enums.Role;
import com.winhmm.myspotify.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final OtpService otpService;

    @Value("${app.upload-dir}")
    private String uploadDir;

    public UserService(UserRepository userRepository, OtpService otpService) {
        this.userRepository = userRepository;
        this.otpService = otpService;
    }

    /*
        getProfile():
        - Nhận userId → Repository tìm User trong Database.
        - Tìm thấy → trả về User.
        - Không tìm thấy → báo lỗi.

        updateProfile():
        - Nhận userId + thông tin mới.
        - Tìm User bằng getProfile().
        - Cập nhật thông tin User.
        - save() để lưu thay đổi vào Database.
    */
    public User getProfile(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));
    }

    /*
        1. Gọi getProfile(userId) để tìm User theo userId.
        Nếu không tìm thấy User thì sẽ xử lý lỗi tại getProfile().

        2. Cập nhật fullName mới cho User.

        3. Gọi userRepository.save(user) để lưu User
        đã cập nhật vào Database.

        4. Trả về User sau khi cập nhật thành công.
    */
    public User updateProfile(Long userId, String fullName) {
        User user = getProfile(userId);
        user.setFullName(fullName);
        return userRepository.save(user);
    }

    /*
        Đổi mật khẩu gồm 2 request riêng:

        1. requestChangePasswordOtp:
        - User nhập mật khẩu hiện tại.
        - Backend kiểm tra mật khẩu hiện tại đúng → gửi OTP về email.

        2. changePassword:
        - User nhập OTP + mật khẩu mới.
        - Backend kiểm tra OTP hợp lệ → cập nhật mật khẩu mới.
    */
    public void requestChangePasswordOtp(Long userId, String currentPassword) {
        // Tận dụng hàm có sẵn để tìm User theo userId trên hàm getProfile
        User user = getProfile(userId);

        if(!user.getPassword().equals(currentPassword)) {
            throw new IllegalArgumentException("Current password does not match");
        }

        otpService.generateAndSend(user, OtpPurpose.CHANGE_PASSWORD);
    }

    public void changePassword(Long userId, String otpCode, String newPassword) {
        User user = getProfile(userId);

        otpService.verifyAndConsume(user, otpCode, OtpPurpose.CHANGE_PASSWORD);

        user.setPassword(newPassword);

        userRepository.save(user);
    }

    /*
        1. Tìm User bằng getProfile().

        2. Kiểm tra file: không được rỗng, chỉ nhận ảnh JPG / PNG / WEBP.

        3. Đặt tên file mới bằng UUID (tránh trùng tên và tránh
        tên file lạ từ người dùng), đuôi file lấy theo loại ảnh.

        4. Lưu file vào thư mục uploads/avatars.

        5. Lưu đường dẫn ảnh vào avatarUrl của User.
    */
    public User updateAvatar(Long userId, MultipartFile file) {
        User user = getProfile(userId);

        if(file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String contentType = file.getContentType();
        String extension;

        if("image/jpeg".equals(contentType)) {
            extension = ".jpg";
        } else if("image/png".equals(contentType)) {
            extension = ".png";
        } else if("image/webp".equals(contentType)) {
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

        user.setAvatarUrl("/uploads/avatars/" + fileName);
        return userRepository.save(user);
    }

    public void requestUpdateEmailOtp(Long userId, String newEmail) {
        User user = getProfile(userId);

        if(userRepository.existsByEmail(newEmail)) {
            throw new IllegalArgumentException("Email already exists");
        }

        otpService.generateAndSend(user, OtpPurpose.CHANGE_EMAIL, newEmail);
    }

    public User updateEmail(Long userId, String otpCode) {
        User user = getProfile(userId);

        OtpVerification otp = otpService.verifyAndConsume(user, otpCode, OtpPurpose.CHANGE_EMAIL);

        user.setEmail(otp.getNewEmail());
        return userRepository.save(user);
    }

    public void disableAccount(Long userId, String currentPassword) {
        User user = getProfile(userId);

        if(!user.getPassword().equals(currentPassword)) {
            throw new IllegalArgumentException("Password does not match");
        }

        user.setAccountStatus(AccountStatus.DISABLED);
        userRepository.save(user);
    }

    public void deleteAccount(Long userId, String currentPassword) {
        User user = getProfile(userId);

        if(!user.getPassword().equals(currentPassword)) {
            throw new IllegalArgumentException("Password does not match");
        }

        user.setAccountStatus(AccountStatus.DELETED);
        userRepository.save(user);
    }

    /*
        Chỉ cho gửi yêu cầu khi artistRequestStatus đang là NONE.

        Lưu tạm artistName/bio vào User, chuyển trạng thái sang PENDING để Admin xem và duyệt.
    */
    public void requestBecomeArtist(Long userId, String artistName, String bio) {
        User user = getProfile(userId);

        if(user.getArtistRequestStatus() != ArtistRequestStatus.NONE
                    && user.getArtistRequestStatus() != ArtistRequestStatus.REJECTED) {
            throw new IllegalArgumentException("An artist request already exists or has been approved");
        }

        user.setArtistName(artistName);
        user.setBio(bio);
        user.setArtistRequestStatus(ArtistRequestStatus.PENDING);

        userRepository.save(user);
    }

    public User updateArtistProfile(Long userId, String artistName, String bio) {
        User user = getProfile(userId);

        if(user.getRole() != Role.ARTIST) {
            throw new IllegalArgumentException("Only artists can update artist profile");
        }

        user.setArtistName(artistName);
        user.setBio(bio);

        return userRepository.save(user);
    }
}
