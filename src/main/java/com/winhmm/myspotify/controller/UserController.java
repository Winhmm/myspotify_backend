package com.winhmm.myspotify.controller;

import com.winhmm.myspotify.dto.user.*;
import com.winhmm.myspotify.dto.user.UserProfileResponse;
import com.winhmm.myspotify.entity.User;
import com.winhmm.myspotify.service.SessionService;
import com.winhmm.myspotify.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final SessionService sessionService;

    public UserController(UserService userService, SessionService sessionService) {
        this.userService = userService;
        this.sessionService = sessionService;
    }

    /*
        Lấy id của User đang đăng nhập.
        authentication.getName() = email trong token.
    */
    private Long me(Authentication authentication) {
        return userService.getByEmail(authentication.getName()).getId();
    }

    /*
        UC06 - Đăng xuất.
        Xóa phiên trong Redis → token hiện tại mất hiệu lực ngay lập tức.
        Frontend vẫn cần tự xóa token đã lưu.

        authentication.getName() trả về email của người đang đăng nhập.
    */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(Authentication authentication) {
        sessionService.deleteSession(authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }

    /*
        UC07 - Xem thông tin cá nhân.
    */
    @GetMapping("/me/profile")
    public ResponseEntity<UserProfileResponse> getProfile(Authentication authentication) {
        return ResponseEntity.ok(toResponse(userService.getProfile(me(authentication))));
    }

    /*
        UC08 - Cập nhật thông tin cá nhân.
    */
    @PutMapping("/me/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(Authentication authentication,
                                                             @Valid @RequestBody UpdateProfileRequest request) {
        User user = userService.updateProfile(me(authentication), request.getFullName());
        return ResponseEntity.ok(toResponse(user));
    }

    /*
        UC09 - Đổi mật khẩu (bước 1):
        Nhận mật khẩu hiện tại → Backend kiểm tra rồi gửi OTP về email.
    */
    @PostMapping("/me/change-password/request-otp")
    public ResponseEntity<Map<String, String>> requestChangePasswordOtp(Authentication authentication,
                                                                        @Valid @RequestBody RequestChangePasswordOtpRequest request) {
        userService.requestChangePasswordOtp(me(authentication), request.getCurrentPassword());
        return ResponseEntity.ok(Map.of("message", "OTP code has been sent to your email"));
    }

    /*
        UC09 - Đổi mật khẩu (bước 2):
        Nhận OTP + mật khẩu mới → Backend kiểm tra OTP rồi đổi mật khẩu.
    */
    @PutMapping("/me/change-password")
    public ResponseEntity<Map<String, String>> changePassword(Authentication authentication,
                                                              @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(me(authentication), request.getOtpCode(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password has been changed successfully"));
    }

    /*
        UC10 - Cập nhật ảnh đại diện.
        Frontend gửi multipart/form-data, key = "file".
    */
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserProfileResponse> updateAvatar(Authentication authentication,
                                                            @RequestParam("file") MultipartFile file) {
        User user = userService.updateAvatar(me(authentication), file);
        return ResponseEntity.ok(toResponse(user));
    }

    /*
        UC13 - Cập nhật email (bước 1):
        Nhận email mới → Backend kiểm tra rồi gửi OTP tới email mới.
    */
    @PostMapping("/me/email/request-otp")
    public ResponseEntity<Map<String, String>> requestUpdateEmailOtp(Authentication authentication,
                                                                     @Valid @RequestBody RequestUpdateEmailOtpRequest request) {
        userService.requestUpdateEmailOtp(me(authentication), request.getNewEmail());
        return ResponseEntity.ok(Map.of("message", "OTP code has been sent to your new email"));
    }

    /*
        UC13 - Cập nhật email (bước 2):
        Nhận OTP → Backend xác thực rồi đổi sang email mới.
        Token cũ hết hiệu lực → Frontend phải đăng nhập lại bằng email mới.
    */
    @PutMapping("/me/email")
    public ResponseEntity<Map<String, String>> updateEmail(Authentication authentication,
                                                           @Valid @RequestBody UpdateEmailRequest request) {
        userService.updateEmail(me(authentication), request.getOtpCode());
        return ResponseEntity.ok(Map.of("message", "Email has been updated, please login again"));
    }

    /*
        UC11 - Vô hiệu hóa tài khoản.
    */
    @PutMapping("/me/disable")
    public ResponseEntity<Map<String, String>> disableAccount(Authentication authentication,
                                                              @Valid @RequestBody AccountActionRequest request) {
        userService.disableAccount(me(authentication), request.getPassword());
        return ResponseEntity.ok(Map.of("message", "Account has been disabled"));
    }

    /*
        UC12 - Xóa tài khoản (soft delete).
    */
    @PutMapping("/me/delete")
    public ResponseEntity<Map<String, String>> deleteAccount(Authentication authentication,
                                                             @Valid @RequestBody AccountActionRequest request) {
        userService.deleteAccount(me(authentication), request.getPassword());
        return ResponseEntity.ok(Map.of("message", "Account has been deleted"));
    }

    /*
        UC14 - Gửi yêu cầu trở thành Artist.
    */
    @PostMapping("/me/artist-request")
    public ResponseEntity<Map<String, String>> requestBecomeArtist(Authentication authentication,
                                                                   @Valid @RequestBody ArtistRequest request) {
        userService.requestBecomeArtist(me(authentication), request.getArtistName(), request.getBio());
        return ResponseEntity.ok(Map.of("message", "Artist request has been submitted"));
    }

    /*
        UC19 - Cập nhật hồ sơ Artist.
    */
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
