package com.winhmm.myspotify.controller;

import com.winhmm.myspotify.dto.request.AssignRoleRequest;
import com.winhmm.myspotify.dto.response.UserProfileResponse;
import com.winhmm.myspotify.entity.User;
import com.winhmm.myspotify.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserProfileResponse>> getAllUsers() {
        List<UserProfileResponse> result = adminService.getAllUsers()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @PutMapping("/users/{userId}/lock")
    public ResponseEntity<Map<String, String>> lockAccount(@PathVariable Long userId) {
        adminService.lockAccount(userId);
        return ResponseEntity.ok(Map.of("message", "Account has been locked"));
    }

    @PutMapping("/users/{userId}/unlock")
    public ResponseEntity<Map<String, String>> unlockAccount(@PathVariable Long userId) {
        adminService.unlockAccount(userId);
        return ResponseEntity.ok(Map.of("message", "Account has been unlocked"));
    }

    @GetMapping("/artist-requests")
    public ResponseEntity<List<UserProfileResponse>> getPendingArtistRequests() {
        List<UserProfileResponse> result = adminService.getPendingArtistRequests()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @PutMapping("/artist-requests/{userId}/approve")
    public ResponseEntity<Map<String, String>> approveArtistRequest(@PathVariable Long userId) {
        adminService.approveArtistRequest(userId);
        return ResponseEntity.ok(Map.of("message", "Artist request has been approved"));
    }

    @PutMapping("/artist-requests/{userId}/reject")
    public ResponseEntity<Map<String, String>> rejectArtistRequest(@PathVariable Long userId) {
        adminService.rejectArtistRequest(userId);
        return ResponseEntity.ok(Map.of("message", "Artist request has been rejected"));
    }

    @PutMapping("/users/{userId}/role")
    public ResponseEntity<Map<String, String>> assignRole(@PathVariable Long userId,
                                                          @Valid @RequestBody AssignRoleRequest request) {
        adminService.assignRole(userId, request.getRole());
        return ResponseEntity.ok(Map.of("message", "Role has been assigned successfully"));
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
