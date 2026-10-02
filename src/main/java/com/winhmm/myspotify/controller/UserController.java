package com.winhmm.myspotify.controller;

import com.winhmm.myspotify.dto.user.*;
import com.winhmm.myspotify.dto.user.UserProfileResponse;
import com.winhmm.myspotify.entity.User;
import com.winhmm.myspotify.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /*
        Lấy id của User đang đăng nhập.
        authentication.getName() = email trong token.
    */
    private Long me(Authentication authentication) {
        return userService.getByEmail(authentication.getName()).getId();
    }

    /*
        JWT không lưu trên server → đăng xuất = Frontend tự xóa token.
    */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }

    @GetMapping("/me/profile")
    public ResponseEntity<UserProfileResponse> getProfile(Authentication authentication) {
        return ResponseEntity.ok(toResponse(userService.getProfile(me(authentication))));
    }

    @PutMapping("/me/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(Authentication authentication,
                                                             @Valid @RequestBody UpdateProfileRequest request) {
        User user = userService.updateProfile(me(authentication), request.getFullName());
        return ResponseEntity.ok(toResponse(user));
    }

    /*
        Bước 1 của đổi mật khẩu:
        Nhận mật khẩu hiện tại → Backend kiểm tra rồi gửi OTP về email.
    */
    @PostMapping("/me/change-password/request-otp")
    public ResponseEntity<Map<String, String>> requestChangePasswordOtp(Authentication authentication,
                                                                        @Valid @RequestBody RequestChangePasswordOtpRequest request) {
        userService.requestChangePasswordOtp(me(authentication), request.getCurrentPassword());
        return ResponseEntity.ok(Map.of("message", "OTP code has been sent to your email"));
    }

    /*
        Bước 2 của đổi mật khẩu:
        Nhận OTP + mật khẩu mới → Backend kiểm tra OTP rồi đổi mật khẩu.
    */
    @PutMapping("/me/change-password")
    public ResponseEntity<Map<String, String>> changePassword(Authentication authentication,
                                                              @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(me(authentication), request.getOtpCode(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password has been changed successfully"));
    }

    @PutMapping("/me/avatar")
    public ResponseEntity<UserProfileResponse> updateAvatar(Authentication authentication,
                                                            @RequestParam("file") MultipartFile file) {
        User user = userService.updateAvatar(me(authentication), file);
        return ResponseEntity.ok(toResponse(user));
    }

    /*
        Nhận email mới → Backend kiểm tra rồi gửi OTP tới email mới.
    */
    @PostMapping("/me/email/request-otp")
    public ResponseEntity<Map<String, String>> requestUpdateEmailOtp(Authentication authentication,
                                                                     @Valid @RequestBody RequestUpdateEmailOtpRequest request) {
        userService.requestUpdateEmailOtp(me(authentication), request.getNewEmail());
        return ResponseEntity.ok(Map.of("message", "OTP code has been sent to your new email"));
    }

    /*
        Nhận OTP → Backend xác thực rồi đổi sang email mới.
    */
    @PutMapping("/me/email")
    public ResponseEntity<UserProfileResponse> updateEmail(Authentication authentication,
                                                           @Valid @RequestBody UpdateEmailRequest request) {
        User user = userService.updateEmail(me(authentication), request.getOtpCode());
        return ResponseEntity.ok(toResponse(user));
    }

    @PutMapping("/me/disable")
    public ResponseEntity<Map<String, String>> disableAccount(Authentication authentication,
                                                              @Valid @RequestBody AccountActionRequest request) {
        userService.disableAccount(me(authentication), request.getPassword());
        return ResponseEntity.ok(Map.of("message", "Account has been disabled"));
    }

    @PutMapping("/me/delete")
    public ResponseEntity<Map<String, String>> deleteAccount(Authentication authentication,
                                                             @Valid @RequestBody AccountActionRequest request) {
        userService.deleteAccount(me(authentication), request.getPassword());
        return ResponseEntity.ok(Map.of("message", "Account has been deleted"));
    }

    @PostMapping("/me/artist-request")
    public ResponseEntity<Map<String, String>> requestBecomeArtist(Authentication authentication,
                                                                   @Valid @RequestBody ArtistRequest request) {
        userService.requestBecomeArtist(me(authentication), request.getArtistName(), request.getBio());
        return ResponseEntity.ok(Map.of("message", "Artist request has been submitted"));
    }

    @PutMapping("/me/artist-profile")
    public ResponseEntity<UserProfileResponse> updateArtistProfile(Authentication authentication,
                                                                   @Valid @RequestBody ArtistRequest request) {
        User user = userService.updateArtistProfile(me(authentication), request.getArtistName(), request.getBio());
        return ResponseEntity.ok(toResponse(user));
    }

    private UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(
                user.getId(), user.getUsername(), user.getEmail(), user.getFullName(),
                user.getAvatarUrl(), user.getAccountStatus(), user.getRole(),
                user.getArtistRequestStatus(), user.getArtistName(), user.getBio()
        );
    }
}
