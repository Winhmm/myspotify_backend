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

    public AdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
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

        if(user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Only active accounts can be locked");
        }

        user.setAccountStatus(AccountStatus.DISABLED);
        userRepository.save(user);
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
}
