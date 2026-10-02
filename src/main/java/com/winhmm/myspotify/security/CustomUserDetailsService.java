package com.winhmm.myspotify.security;

import com.winhmm.myspotify.entity.User;
import com.winhmm.myspotify.enums.AccountStatus;
import com.winhmm.myspotify.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
/*
    CustomUserDetailsService implements UserDetailsService để cung cấp thông tin người dùng cho Spring Security.

    Nó tìm kiếm người dùng trong cơ sở dữ liệu dựa trên email và tạo UserDetails từ thực thể User.
*/
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        /*
            Tạo UserDetails từ User entity vì Spring Security cần UserDetails để xác thực và ủy quyền.
            - username là email của user.
            - password là password của user.
            - roles là role của user.
            - Nếu accountStatus không phải ACTIVE thì disable tài khoản.
        */
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .disabled(user.getAccountStatus() != AccountStatus.ACTIVE)
                .build();
    }
}
