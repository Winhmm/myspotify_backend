package com.winhmm.myspotify.controller;

import com.winhmm.myspotify.dto.request.*;
import com.winhmm.myspotify.dto.response.UserProfileResponse;
import com.winhmm.myspotify.entity.User;
import com.winhmm.myspotify.service.UserService;
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

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }

    /*
        1. Frontend gửi request:
        GET /api/users/{userId}/profile

        2. @PathVariable lấy userId từ URL.

        3. Gọi UserService để tìm User trong Database.

        4. Chuyển User Entity thành UserProfileResponse
        thông qua hàm toResponse().

        5. Trả về HTTP 200 OK cùng thông tin profile cho Frontend.
    */
    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserProfileResponse> getProfile(@PathVariable Long userId) {
        User user = userService.getProfile(userId);
        return ResponseEntity.ok(toResponse(user));
    }

    /*
        1. Frontend gửi request:
        PUT /api/users/{userId}/profile

        2. @PathVariable lấy userId từ URL.

        3. @RequestBody nhận dữ liệu cập nhật từ Frontend
        thông qua UpdateProfileRequest.

        4. Lấy fullName từ request và gửi userId + fullName
        sang UserService để xử lý cập nhật User.

        5. Chuyển User sau khi cập nhật thành UserProfileResponse
        thông qua hàm toResponse().

        6. Trả về HTTP 200 OK cùng thông tin profile đã cập nhật.
    */
    @PutMapping("/{userId}/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(@PathVariable Long userId,
                                                             @RequestBody UpdateProfileRequest request) {
        User user = userService.updateProfile(userId, request.getFullName());
        return ResponseEntity.ok(toResponse(user));
    }

    /*
        Bước 1 của đổi mật khẩu:
        Nhận mật khẩu hiện tại → Backend kiểm tra rồi gửi OTP về email.
    */
    @PostMapping("/{userId}/change-password/request-otp")
    public ResponseEntity<Map<String, String>> requestChangePasswordOtp(@PathVariable Long userId,
                                                                        @RequestBody RequestChangePasswordOtpRequest request) {
        userService.requestChangePasswordOtp(userId, request.getCurrentPassword());
        return ResponseEntity.ok(Map.of("message", "OTP code has been sent to your email"));
    }

    /*
        Bước 2 của đổi mật khẩu:
        Nhận OTP + mật khẩu mới → Backend kiểm tra OTP rồi đổi mật khẩu.
    */
    @PutMapping("/{userId}/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@PathVariable Long userId,
                                                              @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userId, request.getOtpCode(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password has been changed successfully"));
    }

    @PutMapping("/{userId}/avatar")
    public ResponseEntity<UserProfileResponse> updateAvatar(@PathVariable Long userId,
                                                            @RequestParam("file") MultipartFile file) {
        User user = userService.updateAvatar(userId, file);
        return ResponseEntity.ok(toResponse(user));
    }

    /*
        Nhận email mới → Backend kiểm tra rồi gửi OTP tới email mới.
    */
    @PostMapping("/{userId}/email/request-otp")
    public ResponseEntity<Map<String, String>> requestUpdateEmailOtp(@PathVariable Long userId,
                                                                     @RequestBody RequestUpdateEmailOtpRequest request) {
        userService.requestUpdateEmailOtp(userId, request.getNewEmail());
        return ResponseEntity.ok(Map.of("message", "OTP code has been sent to your new email"));
    }

    /*
        Nhận OTP → Backend xác thực rồi đổi sang email mới.
    */
    @PutMapping("/{userId}/email")
    public ResponseEntity<UserProfileResponse> updateEmail(@PathVariable Long userId,
                                                           @RequestBody UpdateEmailRequest request) {
        User user = userService.updateEmail(userId, request.getOtpCode());
        return ResponseEntity.ok(toResponse(user));
    }

    @PutMapping("/{userId}/disable")
    public ResponseEntity<Map<String, String>> disableAccount(@PathVariable Long userId,
                                                              @RequestBody AccountActionRequest request) {
        userService.disableAccount(userId, request.getPassword());
        return ResponseEntity.ok(Map.of("message", "Account has been disabled"));
    }

    @PutMapping("/{userId}/delete")
    public ResponseEntity<Map<String, String>> deleteAccount(@PathVariable Long userId,
                                                             @RequestBody AccountActionRequest request) {
        userService.deleteAccount(userId, request.getPassword());
        return ResponseEntity.ok(Map.of("message", "Account has been deleted"));
    }

    @PostMapping("/{userId}/artist-request")
    public ResponseEntity<Map<String, String>> requestBecomeArtist(@PathVariable Long userId,
                                                                   @RequestBody ArtistRequest request) {
        userService.requestBecomeArtist(userId, request.getArtistName(), request.getBio());
        return ResponseEntity.ok(Map.of("message", "Artist request has been submitted"));
    }

    @PutMapping("/{userId}/artist-profile")
    public ResponseEntity<UserProfileResponse> updateArtistProfile(@PathVariable Long userId,
                                                                   @RequestBody ArtistRequest request) {
        User user = userService.updateArtistProfile(userId, request.getArtistName(), request.getBio());
        return ResponseEntity.ok(toResponse(user));
    }

    private UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatarUrl(),
                user.getAccountStatus(),
                user.getRole(),
                user.getArtistRequestStatus(),
                user.getArtistName(),
                user.getBio()
        );
    }
}
