package com.winhmm.myspotify.controller;

import com.winhmm.myspotify.dto.user.AssignRoleRequest;
import com.winhmm.myspotify.dto.user.UserProfileResponse;
import com.winhmm.myspotify.entity.User;
import com.winhmm.myspotify.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
    =====================================================
    ADMINCONTROLLER - API QUẢN TRỊ NGƯỜI DÙNG (UC15 - UC18)
    =====================================================

    Các API trong class:
        GET /api/admin/users                              → danh sách tất cả tài khoản
        PUT /api/admin/users/{userId}/lock                → khóa tài khoản
        PUT /api/admin/users/{userId}/unlock              → mở khóa tài khoản
        GET /api/admin/artist-requests                    → danh sách yêu cầu làm Artist đang chờ
        PUT /api/admin/artist-requests/{userId}/approve   → duyệt yêu cầu làm Artist
        PUT /api/admin/artist-requests/{userId}/reject    → từ chối yêu cầu làm Artist
        PUT /api/admin/users/{userId}/role                → gán role (USER hoặc ADMIN)

    -----------------------------------------------------
    Ai được gọi?
    -----------------------------------------------------
    Chỉ tài khoản có role ADMIN.

    @PreAuthorize("hasRole('ADMIN')") đặt trên class → áp dụng cho mọi hàm trong class.
    Cách kiểm tra:
    1. JwtAuthenticationFilter đọc token, lấy role hiện tại từ MySQL, gắn quyền "ROLE_ADMIN".
    2. Trước khi chạy hàm, Spring so quyền đó với hasRole('ADMIN').
    3. Không khớp → 403 "You do not have permission to access this resource".
       Chưa đăng nhập → 401.

    Cần @EnableMethodSecurity trong SecurityConfig thì @PreAuthorize mới có tác dụng.

    -----------------------------------------------------
    Vì sao ở đây URL có userId, còn UserController thì không?
    -----------------------------------------------------
    Admin thao tác trên tài khoản của người khác, nên phải chỉ rõ tài khoản nào.
    Việc này an toàn vì đã có @PreAuthorize chặn, người không phải Admin không gọi được.

    -----------------------------------------------------
    Các quy tắc bảo vệ chính (trong AdminService)
    -----------------------------------------------------
    - Không khóa được tài khoản ADMIN (kể cả chính mình).
    - Không đổi role của tài khoản ADMIN qua API (muốn hạ quyền phải dùng SQL).
    - Không gán role ARTIST trực tiếp: phải qua quy trình gửi yêu cầu và duyệt,
      để Artist nào cũng có nghệ danh.
    - Khóa tài khoản → đá người đó ra khỏi mọi thiết bị ngay lập tức.
    - Mọi thay đổi role có hiệu lực ngay ở request tiếp theo của người bị đổi,
      không cần họ đăng nhập lại (vì role được đọc từ MySQL ở mỗi request).

    -----------------------------------------------------
    Admin đầu tiên
    -----------------------------------------------------
    Không có API tạo Admin (nếu có thì ai cũng tự biến mình thành Admin được).
    Admin đầu tiên được tạo bằng SQL sau khi đăng ký và xác thực tài khoản bình thường:
        UPDATE users SET role = 'ADMIN' WHERE email = 'admin@gmail.com';
    Các Admin sau: Admin hiện tại gán qua PUT /api/admin/users/{userId}/role.
*/
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /*
        =====================================================
        UC15 - XEM DANH SÁCH TẤT CẢ TÀI KHOẢN
        =====================================================

        Ví dụ gọi:
            GET /api/admin/users

        Trả về mọi tài khoản ở mọi trạng thái (UNVERIFIED, ACTIVE, DISABLED, DELETED)
        để Admin có cái nhìn đầy đủ.

        -----------------------------------------------------
        Cách chuyển danh sách
        -----------------------------------------------------
        adminService.getAllUsers()         → List<User> (entity, có password)
            .stream()                      → biến danh sách thành "dòng chảy" để xử lý từng phần tử
            .map(this::toResponse)         → đổi từng User thành UserProfileResponse (bỏ password)
            .collect(Collectors.toList())  → gom kết quả lại thành List

        this::toResponse là cách viết gọn của: user -> toResponse(user)

        Kết quả: 200, mảng các UserProfileResponse
            [
                { "id": 1, "username": "admin01", "role": "ADMIN", "accountStatus": "ACTIVE", ... },
                { "id": 6, "username": "maychieu", "role": "ARTIST", "accountStatus": "ACTIVE", ... }
            ]

        Lưu ý: chưa phân trang. Phù hợp với lượng người dùng của đồ án.
        Khi có hàng chục nghìn tài khoản thì nên phân trang như GET /api/songs.
    */
    @GetMapping("/users")
    public ResponseEntity<List<UserProfileResponse>> getAllUsers() {
        List<UserProfileResponse> result = adminService.getAllUsers()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /*
        =====================================================
        UC16 - KHÓA TÀI KHOẢN
        =====================================================

        Ví dụ gọi:
            PUT /api/admin/users/6/lock      (không cần body)

        @PathVariable Long userId: lấy số 6 trong URL gán vào biến userId.
        Gửi chữ thay vì số (VD /users/abc/lock) → 400 "Invalid parameter: userId".

        Luồng xử lý (AdminService.lockAccount):
        1. Tìm User. Không có → 400 "User does not exist".
        2. Không khóa được ADMIN → 400 "Cannot lock an admin account".
        3. Chỉ khóa được tài khoản ACTIVE → 400 "Only active accounts can be locked".
        4. Chuyển accountStatus sang DISABLED.
        5. Xóa phiên trong Redis → người bị khóa bị đá ra ngay,
           request tiếp theo của họ nhận 401 "Your session has ended...".

        Sau khi khóa:
        - Không đăng nhập được: 400 "Account has been disabled".
        - Nếu là Artist: toàn bộ bài hát tự ẩn khỏi người nghe.

        Kết quả: 200 { "message": "Account has been locked" }
    */
    @PutMapping("/users/{userId}/lock")
    public ResponseEntity<Map<String, String>> lockAccount(@PathVariable Long userId) {
        adminService.lockAccount(userId);
        return ResponseEntity.ok(Map.of("message", "Account has been locked"));
    }

    /*
        =====================================================
        UC16 - MỞ KHÓA TÀI KHOẢN
        =====================================================

        Ví dụ gọi:
            PUT /api/admin/users/6/unlock    (không cần body)

        Luồng xử lý (AdminService.unlockAccount):
        1. Tìm User. Không có → 400 "User does not exist".
        2. Chỉ mở khóa được tài khoản DISABLED → 400 "Only disabled accounts can be unlocked".
           (Tài khoản DELETED không mở lại được.)
        3. Chuyển accountStatus về ACTIVE → người dùng đăng nhập lại được.

        Áp dụng cho cả 2 trường hợp DISABLED:
        - Bị Admin khóa (UC16).
        - Người dùng tự vô hiệu hóa (UC11), sau đó liên hệ Admin để mở lại.
        Nếu là Artist, bài hát đã phát hành tự hiện lại cho người nghe.

        Kết quả: 200 { "message": "Account has been unlocked" }
    */
    @PutMapping("/users/{userId}/unlock")
    public ResponseEntity<Map<String, String>> unlockAccount(@PathVariable Long userId) {
        adminService.unlockAccount(userId);
        return ResponseEntity.ok(Map.of("message", "Account has been unlocked"));
    }

    /*
        =====================================================
        UC17 - XEM DANH SÁCH YÊU CẦU LÀM ARTIST ĐANG CHỜ DUYỆT
        =====================================================

        Ví dụ gọi:
            GET /api/admin/artist-requests

        Chỉ trả về tài khoản có artistRequestStatus = PENDING.
        Admin xem artistName và bio trong từng phần tử để quyết định duyệt hay từ chối.

        Kết quả: 200, mảng các UserProfileResponse (cùng cấu trúc với UC15).
        Không có yêu cầu nào → mảng rỗng [].
    */
    @GetMapping("/artist-requests")
    public ResponseEntity<List<UserProfileResponse>> getPendingArtistRequests() {
        List<UserProfileResponse> result = adminService.getPendingArtistRequests()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /*
        =====================================================
        UC17 - DUYỆT YÊU CẦU LÀM ARTIST
        =====================================================

        Ví dụ gọi:
            PUT /api/admin/artist-requests/6/approve    (không cần body)

        Luồng xử lý (AdminService.approveArtistRequest):
        1. Yêu cầu phải đang PENDING → 400 "No pending artist request for this user".
        2. Người dùng phải đang là USER → 400 "Only users with role USER can become an artist".
        3. Tài khoản phải ACTIVE → 400 "Account is not active".
        4. Đổi role sang ARTIST, artistRequestStatus sang APPROVED.
           Nghệ danh và bio đã gửi ở UC14 được giữ nguyên.

        Người được duyệt dùng được API Artist (/api/artist/**) ngay ở request tiếp theo,
        không cần đăng nhập lại.
        Frontend phía người dùng nên gọi lại GET /api/users/me/profile
        để cập nhật role và hiện menu "Quản lý bài hát".

        Kết quả: 200 { "message": "Artist request has been approved" }
    */
    @PutMapping("/artist-requests/{userId}/approve")
    public ResponseEntity<Map<String, String>> approveArtistRequest(@PathVariable Long userId) {
        adminService.approveArtistRequest(userId);
        return ResponseEntity.ok(Map.of("message", "Artist request has been approved"));
    }

    /*
        =====================================================
        UC17 - TỪ CHỐI YÊU CẦU LÀM ARTIST
        =====================================================

        Ví dụ gọi:
            PUT /api/admin/artist-requests/6/reject     (không cần body)

        Luồng xử lý (AdminService.rejectArtistRequest):
        1. Yêu cầu phải đang PENDING → 400 "No pending artist request for this user".
        2. artistRequestStatus sang REJECTED.
        3. Xóa nghệ danh và bio (đặt về null).
        Role giữ nguyên USER. Người dùng được gửi yêu cầu mới (UC14) với nghệ danh khác.

        Kết quả: 200 { "message": "Artist request has been rejected" }
    */
    @PutMapping("/artist-requests/{userId}/reject")
    public ResponseEntity<Map<String, String>> rejectArtistRequest(@PathVariable Long userId) {
        adminService.rejectArtistRequest(userId);
        return ResponseEntity.ok(Map.of("message", "Artist request has been rejected"));
    }

    /*
        =====================================================
        UC18 - GÁN ROLE CHO TÀI KHOẢN
        =====================================================

        Ví dụ gọi:
            PUT /api/admin/users/7/role
            {
                "role": "ADMIN"
            }

        Dùng để:
        - Nâng 1 tài khoản lên ADMIN.
        - Hạ 1 ARTIST về USER (thu hồi quyền Artist).

        -----------------------------------------------------
        Quy tắc (AdminService.assignRole)
        -----------------------------------------------------
        1. Không gán ARTIST ở đây
           → 400 "ARTIST role can only be granted by approving an artist request".
           Lý do: Artist bắt buộc phải có nghệ danh, mà API này không nhận nghệ danh.
        2. Không đổi role của ADMIN → 400 "Cannot change role of an admin account".
           Lý do: tránh 1 Admin hạ quyền các Admin khác để chiếm quyền.
        3. Không gán cho tài khoản DELETED hoặc UNVERIFIED
           → 400 "Cannot assign role to a deleted or unverified account".
        4. Không gán trùng role hiện tại → 400 "User already has this role".
        5. Hạ ARTIST xuống role khác, hoặc người đang có yêu cầu PENDING
           → xóa nghệ danh, bio, đưa artistRequestStatus về NONE.
           Bài hát của Artist bị hạ quyền tự ẩn khỏi người nghe
           (điều kiện phát hành yêu cầu Artist vẫn có role ARTIST).

        Lỗi có thể gặp thêm:
            400 "Role must not be null"   → thiếu trường role
            400 "Invalid request data"    → role không tồn tại, VD "SUPERADMIN"

        Kết quả: 200 { "message": "Role has been assigned successfully" }
    */
    @PutMapping("/users/{userId}/role")
    public ResponseEntity<Map<String, String>> assignRole(@PathVariable Long userId,
                                                          @Valid @RequestBody AssignRoleRequest request) {
        adminService.assignRole(userId, request.getRole());
        return ResponseEntity.ok(Map.of("message", "Role has been assigned successfully"));
    }

    /*
        -----------------------------------------------------
        Chuyển User (entity) sang UserProfileResponse (DTO)
        -----------------------------------------------------
        Bỏ trường password, chỉ giữ những trường Admin cần xem.
        Hàm này giống hệt hàm cùng tên trong UserController.
    */
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
