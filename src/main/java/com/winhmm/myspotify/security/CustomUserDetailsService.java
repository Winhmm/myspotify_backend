package com.winhmm.myspotify.security;

import com.winhmm.myspotify.entity.User;
import com.winhmm.myspotify.enums.AccessStatus;
import com.winhmm.myspotify.enums.AccountStatus;
import com.winhmm.myspotify.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/*
    =====================================================
    CUSTOMUSERDETAILSSERVICE - CUNG CẤP THÔNG TIN NGƯỜI DÙNG CHO SPRING SECURITY
    =====================================================

    -----------------------------------------------------
    Vì sao cần class này?
    -----------------------------------------------------
    Spring Security không biết entity User của project có những field gì
    (email, password, role, accountStatus...).
    Nó chỉ làm việc với 1 interface chuẩn tên là UserDetails, gồm các thông tin:
    - username: định danh người dùng (project dùng email).
    - password: mật khẩu đã băm bằng BCrypt.
    - authorities: danh sách quyền, ví dụ ROLE_ADMIN.
    - enabled: tài khoản có được phép sử dụng hay không.

    Class này làm nhiệm vụ "phiên dịch":
        email → tìm User trong MySQL → chuyển sang UserDetails mà Spring hiểu.

    implements UserDetailsService: cam kết với Spring rằng class này có hàm
    loadUserByUsername(), để Spring và filter của mình gọi khi cần.

    -----------------------------------------------------
    Được gọi ở đâu và khi nào?
    -----------------------------------------------------
    - JwtAuthenticationFilter gọi loadUserByUsername(email) ở mỗi request có token hợp lệ.
    - Nghĩa là mỗi request đã đăng nhập tốn thêm 1 câu SELECT vào bảng users.
    - Đổi lại, role và trạng thái tài khoản luôn là dữ liệu mới nhất trong database:
        + Admin khóa tài khoản → request tiếp theo của người đó bị chặn ngay.
        + Admin đổi role USER thành ADMIN → request tiếp theo đã có quyền Admin,
          không cần đăng nhập lại.
        + Chủ dự án đổi access_status thành REFUSE → request tiếp theo của người đó bị chặn ngay.

    Lưu ý: khi đăng nhập, việc so mật khẩu do AuthService tự làm
    bằng passwordEncoder.matches(), không đi qua class này.
*/
@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /*
        =====================================================
        TẢI THÔNG TIN NGƯỜI DÙNG THEO EMAIL
        =====================================================

        Tên hàm là loadUserByUsername vì đó là tên Spring đặt sẵn trong interface.
        Project dùng email làm "username", nên tham số được đặt tên là email cho dễ hiểu.

        @Override: đánh dấu đây là hàm ghi đè từ interface UserDetailsService.
        Nếu gõ sai tên hàm, trình biên dịch sẽ báo lỗi ngay.
    */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        /*
            -----------------------------------------------------
            Bước 1: Tìm User trong MySQL
            -----------------------------------------------------
        */
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        /*
            -----------------------------------------------------
            Bước 2: Chuyển User (entity) sang UserDetails (của Spring)
            -----------------------------------------------------
            Dùng tên đầy đủ org.springframework.security.core.userdetails.User
            vì trùng tên với entity User của project (đã import ở trên).

            Từng dòng trong builder:

            - withUsername(user.getEmail()):
              Định danh người dùng là email.
              Sau này trong Controller, authentication.getName() sẽ trả về chính email này.

            - password(user.getPassword()):
              Mật khẩu đã băm. JwtAuthenticationFilter không dùng tới
              (token đã chứng minh danh tính rồi), nhưng UserDetails bắt buộc phải có.

            - roles(user.getRole().name()):
              user.getRole() trả về enum Role, ví dụ Role.ADMIN.
              .name() chuyển enum thành chuỗi "ADMIN".
              .roles("ADMIN") tự thêm tiền tố "ROLE_" → quyền thật được lưu là "ROLE_ADMIN".

              Khi Controller ghi @PreAuthorize("hasRole('ADMIN')"),
              Spring cũng tự thêm "ROLE_" → so "ROLE_ADMIN" với "ROLE_ADMIN" → khớp.

              Bảng đối chiếu:
                Role trong DB    Quyền trong Spring    Khớp với
                USER             ROLE_USER             hasRole('USER')
                ARTIST           ROLE_ARTIST           hasRole('ARTIST')
                ADMIN            ROLE_ADMIN            hasRole('ADMIN')

              Lưu ý:
              + Không được truyền "ROLE_ADMIN" vào .roles(), Spring sẽ ném IllegalArgumentException
                vì nó tự thêm tiền tố rồi.
              + Nếu dùng hasAuthority thay vì hasRole thì phải ghi đầy đủ: hasAuthority('ROLE_ADMIN').

            - disabled(...):
              Tài khoản không ACTIVE, hoặc chưa được chủ dự án cho phép (khác ACCEPT)
              → disabled = true → isEnabled() = false.

              Bảng đối chiếu:
                accountStatus    accessStatus    isEnabled()    Kết quả khi gọi API cần đăng nhập
                ACTIVE           ACCEPT          true           Được xác thực bình thường
                ACTIVE           REFUSE          false          401 (bị thu hồi quyền vào web)
                UNVERIFIED       bất kỳ          false          401 (chưa xác thực OTP)
                DISABLED         bất kỳ          false          401 (bị Admin khóa)
                DELETED          bất kỳ          false          401 (đã xóa mềm)

              Nhờ điều kiện accessStatus ở đây, người đang đăng nhập bị đổi sang REFUSE
              sẽ bị chặn ngay từ request tiếp theo, không cần chờ token hết hạn.

            - build(): tạo ra đối tượng UserDetails hoàn chỉnh.
        */
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .disabled(user.getAccountStatus() != AccountStatus.ACTIVE
                        || user.getAccessStatus() != AccessStatus.ACCEPT)
                .build();
    }
}