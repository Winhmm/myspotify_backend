package com.winhmm.myspotify.repository;

import com.winhmm.myspotify.entity.User;
import com.winhmm.myspotify.enums.AccountStatus;
import com.winhmm.myspotify.enums.ArtistRequestStatus;
import com.winhmm.myspotify.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/*
    =====================================================
    USERREPOSITORY - TRUY CẬP BẢNG USERS (MYSQL)
    =====================================================

    Lớp duy nhất đọc và ghi trực tiếp bảng users.
    Service gọi Repository, không tự viết SQL.

    extends JpaRepository<User, Long>:
    - User: entity mà repository này quản lý.
    - Long: kiểu của khóa chính (User.id).
    Được tặng sẵn: save(), findById(), findAll(), delete(), count()...

    Interface không có code bên trong: Spring Data JPA đọc tên method
    rồi tự sinh câu truy vấn khi ứng dụng khởi động (derived query).
    Giải thích chi tiết cách đặt tên và JPQL: xem comment đầu file SongRepository.
*/
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    List<User> findByArtistRequestStatus(ArtistRequestStatus status);

    /*
        UC37 - Tìm nghệ sĩ để feat khi Artist đăng bài.

        Tên method được Spring tách thành từng phần:
            findTop10                           → LIMIT 10
            ByRole                              → WHERE role = ?           (truyền ARTIST)
            AndAccountStatus                    → AND account_status = ?   (truyền ACTIVE)
            AndArtistNameContainingIgnoreCase   → AND LOWER(artist_name) LIKE LOWER('%keyword%')
            AndIdNot                            → AND id <> ?              (loại chính mình)

        Containing: Spring tự thêm % 2 đầu và tự escape các ký tự đặc biệt % và _ trong keyword,
        nên tìm "100%" vẫn chính xác.
        Chỉ lấy 10 người để danh sách gợi ý gọn, frontend gõ thêm chữ để lọc tiếp.
    */
    List<User> findTop10ByRoleAndAccountStatusAndArtistNameContainingIgnoreCaseAndIdNot(
            Role role, AccountStatus accountStatus, String keyword, Long excludeId);
}
