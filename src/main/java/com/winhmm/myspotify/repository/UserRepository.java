package com.winhmm.myspotify.repository;

import com.winhmm.myspotify.entity.User;
import com.winhmm.myspotify.enums.AccountStatus;
import com.winhmm.myspotify.enums.ArtistRequestStatus;
import com.winhmm.myspotify.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    /* ================= MODULE NGƯỜI DÙNG ================= */

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    List<User> findByArtistRequestStatus(ArtistRequestStatus status);

    /* ================= MODULE BÀI HÁT ================= */

    /*
       UC37 - Tìm nghệ sĩ để feat.
       Lấy tối đa 10 người: đúng role, đúng trạng thái,
       nghệ danh chứa từ khóa (không phân biệt hoa thường),
       và khác id truyền vào (để loại chính mình).
   */
    List<User> findTop10ByRoleAndAccountStatusAndArtistNameContainingIgnoreCaseAndIdNot(
            Role role, AccountStatus accountStatus, String keyword, Long excludeId);
}
