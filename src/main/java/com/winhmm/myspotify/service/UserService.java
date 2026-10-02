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
    private final UserRepository userRepository;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.upload-dir}")
    private String uploadDir;

    public UserService(UserRepository userRepository, OtpService otpService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.otpService = otpService;
        this.passwordEncoder = passwordEncoder;
    }

    public User getProfile(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));
    }

    private void checkPassword(User user, String rawPassword) {
        if(!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new IllegalArgumentException("Password does not match");
        }
    }

    private void checkActive(User user) {
        if(user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Account is not active");
        }
    }

    public User updateProfile(Long userId, String fullName) {
        User user = getProfile(userId);
        user.setFullName(fullName);
        return userRepository.save(user);
    }

    public void requestChangePasswordOtp(Long userId, String currentPassword) {
        User user = getProfile(userId);
        checkPassword(user, currentPassword);
        otpService.generateAndSend(user, OtpPurpose.CHANGE_PASSWORD);
    }

    @Transactional
    public void changePassword(Long userId, String otpCode, String newPassword) {
        User user = getProfile(userId);

        otpService.verifyAndConsume(user, otpCode, OtpPurpose.CHANGE_PASSWORD);

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

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

    @Transactional
    public User updateEmail(Long userId, String otpCode) {
        User user = getProfile(userId);

        OtpVerification otp = otpService.verifyAndConsume(user, otpCode, OtpPurpose.CHANGE_EMAIL);

        if(userRepository.existsByEmail(otp.getNewEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        user.setEmail(otp.getNewEmail());
        return userRepository.save(user);
    }

    public void disableAccount(Long userId, String currentPassword) {
        User user = getProfile(userId);
        checkActive(user);
        checkPassword(user, currentPassword);

        user.setAccountStatus(AccountStatus.DISABLED);
        userRepository.save(user);
    }

    public void deleteAccount(Long userId, String currentPassword) {
        User user = getProfile(userId);
        checkActive(user);
        checkPassword(user, currentPassword);

        user.setAccountStatus(AccountStatus.DELETED);
        userRepository.save(user);
    }

    public void requestBecomeArtist(Long userId, String artistName, String bio) {
        User user = getProfile(userId);
        checkActive(user);

        if(user.getRole() != Role.USER) {
            throw new IllegalArgumentException("Only users with role USER can request to become an artist");
        }

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
