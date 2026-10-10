package com.winhmm.myspotify.service;

import com.winhmm.myspotify.entity.User;
import com.winhmm.myspotify.enums.AccountStatus;
import com.winhmm.myspotify.enums.ArtistRequestStatus;
import com.winhmm.myspotify.enums.Role;
import com.winhmm.myspotify.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {
    private final UserRepository userRepository;
    private final SessionService sessionService;

    public AdminService(UserRepository userRepository, SessionService sessionService) {
        this.userRepository = userRepository;
        this.sessionService = sessionService;
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /*
        Khóa tài khoản:
        - Chỉ khóa được tài khoản đang ACTIVE.
        - Chuyển sang DISABLED → User không đăng nhập được nữa.
    */
    public void lockAccount(Long userId) {
        User user = findUser(userId);

        if(user.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Cannot lock an admin account");
        }

        if(user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Only active accounts can be locked");
        }

        user.setAccountStatus(AccountStatus.DISABLED);
        userRepository.save(user);

        /*
            Người bị khóa → đá ra khỏi mọi thiết bị ngay lập tức.
        */
        sessionService.deleteSession(user.getEmail());
    }

    /*
        Mở khóa tài khoản:
        - Chỉ mở khóa được tài khoản đang DISABLED.
        - Chuyển về ACTIVE → User đăng nhập lại được.
    */
    public void unlockAccount(Long userId) {
        User user = findUser(userId);

        if(user.getAccountStatus() != AccountStatus.DISABLED) {
            throw new IllegalArgumentException("Only disabled accounts can be unlocked");
        }

        user.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(user);
    }

    public List<User> getPendingArtistRequests() {
        return userRepository.findByArtistRequestStatus(ArtistRequestStatus.PENDING);
    }

    public void approveArtistRequest(Long userId) {
        User user = findUser(userId);

        if(user.getArtistRequestStatus() != ArtistRequestStatus.PENDING) {
            throw new IllegalArgumentException("No pending artist request for this user");
        }

        if(user.getRole() != Role.USER) {
            throw new IllegalArgumentException("Only users with role USER can become an artist");
        }

        if(user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Account is not active");
        }

        user.setArtistRequestStatus(ArtistRequestStatus.APPROVED);
        user.setRole(Role.ARTIST);
        userRepository.save(user);
    }

    public void rejectArtistRequest(Long userId) {
        User user = findUser(userId);

        if(user.getArtistRequestStatus() != ArtistRequestStatus.PENDING) {
            throw new IllegalArgumentException("No pending artist request for this user");
        }

        user.setArtistRequestStatus(ArtistRequestStatus.REJECTED);
        user.setArtistName(null);
        user.setBio(null);
        userRepository.save(user);
    }

    /*
        Gán role cho User:

        1. Không gán cho tài khoản đã xóa hoặc chưa xác thực.
        2. Không gán trùng role hiện tại.
        3. Gán ARTIST → artistRequestStatus = APPROVED.
        4. Gán role khác ARTIST, mà User đang là ARTIST hoặc đang có yêu cầu PENDING
           → xoá artistName/bio, artistRequestStatus về NONE.
    */
    public void assignRole(Long userId, Role role) {
        User user = findUser(userId);

        if(role == null) {
            throw new IllegalArgumentException("Role is required");
        }

        if(user.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Cannot change role of an admin account");
        }

        if(user.getAccountStatus() == AccountStatus.DELETED
                || user.getAccountStatus() == AccountStatus.UNVERIFIED) {
            throw new IllegalArgumentException("Cannot assign role to a deleted or unverified account");
        }

        if(user.getRole() == role) {
            throw new IllegalArgumentException("User already has this role");
        }

        if(role == Role.ARTIST) {
            user.setArtistRequestStatus(ArtistRequestStatus.APPROVED);
        } else if(user.getRole() == Role.ARTIST || user.getArtistRequestStatus() == ArtistRequestStatus.PENDING) {
            /*
                Thu hồi quyền/trạng thái Artist.
            */
            user.setArtistName(null);
            user.setBio(null);
            user.setArtistRequestStatus(ArtistRequestStatus.NONE);
        }

        user.setRole(role);
        userRepository.save(user);
    }
}
