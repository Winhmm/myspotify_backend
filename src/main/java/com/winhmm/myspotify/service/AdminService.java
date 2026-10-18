package com.winhmm.myspotify.service;

import com.winhmm.myspotify.entity.User;
import com.winhmm.myspotify.enums.AccountStatus;
import com.winhmm.myspotify.enums.ArtistRequestStatus;
import com.winhmm.myspotify.enums.Role;
import com.winhmm.myspotify.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/*
    =====================================================
    ADMINSERVICE - NGHIỆP VỤ QUẢN TRỊ NGƯỜI DÙNG (UC15 - UC18)
    =====================================================

    Các chức năng:
        getAllUsers()                  → danh sách tất cả tài khoản (UC15)
        lockAccount(userId)            → khóa tài khoản (UC16)
        unlockAccount(userId)          → mở khóa tài khoản (UC16)
        getPendingArtistRequests()     → danh sách yêu cầu làm Artist đang chờ (UC17)
        approveArtistRequest(userId)   → duyệt yêu cầu làm Artist (UC17)
        rejectArtistRequest(userId)    → từ chối yêu cầu làm Artist (UC17)
        assignRole(userId, role)       → gán role USER hoặc ADMIN (UC18)

    Được gọi từ AdminController. Chỉ Admin gọi được
    (AdminController có @PreAuthorize("hasRole('ADMIN')")).

    -----------------------------------------------------
    Vì sao không có @Transactional?
    -----------------------------------------------------
    Mỗi hàm chỉ có 1 lần save() vào MySQL, bản thân save() đã tự chạy trong 1 transaction.
    Việc xóa phiên trong Redis nằm ngoài MySQL, không bị ảnh hưởng bởi transaction.

    -----------------------------------------------------
    Thay đổi có hiệu lực ngay
    -----------------------------------------------------
    Role và trạng thái tài khoản được đọc lại từ MySQL ở mỗi request
    (CustomUserDetailsService), nên mọi thay đổi ở đây có tác dụng
    ngay ở request tiếp theo của người bị thay đổi, không cần họ đăng nhập lại.
*/
@Service
public class AdminService {
    private final UserRepository userRepository;
    private final SessionService sessionService;

    public AdminService(UserRepository userRepository, SessionService sessionService) {
        this.userRepository = userRepository;
        this.sessionService = sessionService;
    }

    /*
        Tìm User theo id, không có → "User does not exist".
        Hàm private dùng chung cho các chức năng bên dưới.
    */
    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));
    }

    /*
        =====================================================
        UC15 - DANH SÁCH TẤT CẢ TÀI KHOẢN
        =====================================================

        Lấy mọi tài khoản ở mọi trạng thái, chưa phân trang.
        Controller chuyển sang UserProfileResponse để bỏ trường password.
    */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /*
        =====================================================
        UC16 - KHÓA TÀI KHOẢN
        =====================================================

        1. Không khóa được ADMIN (kể cả chính mình)
           → tránh 1 Admin khóa các Admin khác để chiếm quyền.
        2. Chỉ khóa được tài khoản ACTIVE
           (UNVERIFIED chưa dùng được, DISABLED đã bị khóa, DELETED đã xóa).
        3. Chuyển sang DISABLED.
        4. Xóa phiên trong Redis → người bị khóa bị đá ra khỏi mọi thiết bị ngay lập tức.
           Không xóa phiên thì họ vẫn bị chặn ở request tiếp theo
           (CustomUserDetailsService thấy DISABLED), nhưng xóa phiên giúp
           frontend nhận message rõ ràng "Your session has ended...".

        Nếu là Artist: mọi bài hát tự ẩn khỏi người nghe
        (PUBLISHED_CONDITION yêu cầu Artist phải ACTIVE).
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
        =====================================================
        UC16 - MỞ KHÓA TÀI KHOẢN
        =====================================================

        1. Chỉ mở khóa được tài khoản DISABLED.
           DELETED không mở lại được (người dùng đã chủ động xóa).
        2. Chuyển về ACTIVE → người dùng đăng nhập lại được.

        Áp dụng cho cả tài khoản bị Admin khóa và tài khoản người dùng tự vô hiệu hóa (UC11),
        vì cả 2 đều có trạng thái DISABLED.
        Nếu là Artist: bài hát đã phát hành tự hiện lại cho người nghe.
    */
    public void unlockAccount(Long userId) {
        User user = findUser(userId);

        if(user.getAccountStatus() != AccountStatus.DISABLED) {
            throw new IllegalArgumentException("Only disabled accounts can be unlocked");
        }

        user.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(user);
    }

    /*
        =====================================================
        UC17 - DANH SÁCH YÊU CẦU LÀM ARTIST ĐANG CHỜ
        =====================================================

        Lọc theo artistRequestStatus = PENDING.
        Cột này có index (idx_users_artist_request_status) nên lọc nhanh.
    */
    public List<User> getPendingArtistRequests() {
        return userRepository.findByArtistRequestStatus(ArtistRequestStatus.PENDING);
    }

    /*
        =====================================================
        UC17 - DUYỆT YÊU CẦU LÀM ARTIST
        =====================================================

        1. Phải có yêu cầu đang PENDING.
        2. Người dùng phải đang là USER.
           Phòng trường hợp role đã bị thay đổi trong lúc chờ duyệt.
        3. Tài khoản phải ACTIVE (không duyệt cho tài khoản bị khóa hoặc đã xóa).
        4. artistRequestStatus → APPROVED, role → ARTIST.
           Nghệ danh và bio đã gửi ở UC14 được giữ nguyên.

        Người được duyệt dùng được API /api/artist/** ngay ở request tiếp theo.
    */
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

    /*
        =====================================================
        UC17 - TỪ CHỐI YÊU CẦU LÀM ARTIST
        =====================================================

        1. Phải có yêu cầu đang PENDING.
        2. artistRequestStatus → REJECTED.
        3. Xóa nghệ danh và bio về null (không giữ thông tin của yêu cầu bị từ chối).

        Role giữ nguyên USER. Người dùng được gửi yêu cầu mới,
        vì UC14 cho phép gửi khi trạng thái là NONE hoặc REJECTED.
    */
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
        =====================================================
        UC18 - GÁN ROLE
        =====================================================

        Dùng để nâng 1 tài khoản lên ADMIN, hoặc hạ 1 ARTIST về USER.

        -----------------------------------------------------
        Các bước kiểm tra (theo thứ tự trong code)
        -----------------------------------------------------
        1. role không được null.
           AssignRoleRequest đã có @NotNull, đây là lớp kiểm tra dự phòng
           nếu sau này có chỗ khác gọi hàm này.
        2. Không gán ARTIST ở đây.
           Artist bắt buộc phải có nghệ danh, mà hàm này không nhận nghệ danh.
           User phải tự gửi yêu cầu (UC14) và được duyệt (UC17).
        3. Không đổi role của ADMIN.
           Tránh 1 Admin hạ quyền các Admin khác. Muốn hạ quyền Admin thì dùng SQL.
        4. Không gán cho tài khoản DELETED hoặc UNVERIFIED.
        5. Không gán trùng role hiện tại.

        -----------------------------------------------------
        Thu hồi trạng thái Artist
        -----------------------------------------------------
        Nếu người bị đổi role đang là ARTIST, hoặc đang có yêu cầu PENDING:
        - Xóa nghệ danh và bio.
        - artistRequestStatus về NONE.
        → Sau này muốn làm Artist lại thì gửi yêu cầu mới.
        → Bài hát của Artist bị hạ quyền tự ẩn khỏi người nghe
          (PUBLISHED_CONDITION yêu cầu Artist vẫn có role ARTIST).

        Ví dụ: USER có yêu cầu PENDING được nâng lên ADMIN
        → yêu cầu bị hủy, vì ADMIN không thể đồng thời là ARTIST.
    */
    public void assignRole(Long userId, Role role) {
        User user = findUser(userId);

        if(role == null) {
            throw new IllegalArgumentException("Role is required");
        }

        if(role == Role.ARTIST) {
            throw new IllegalArgumentException(
                    "ARTIST role can only be granted by approving an artist request");
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

        if(user.getRole() == Role.ARTIST || user.getArtistRequestStatus() == ArtistRequestStatus.PENDING) {
            /*
                Thu hồi quyền / trạng thái Artist.
            */
            user.setArtistName(null);
            user.setBio(null);
            user.setArtistRequestStatus(ArtistRequestStatus.NONE);
        }

        user.setRole(role);
        userRepository.save(user);
    }
}
