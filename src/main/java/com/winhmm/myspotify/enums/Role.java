package com.winhmm.myspotify.enums;

/*
    -----------------------------------------------------
    Package enums: thứ tự nên đọc
    -----------------------------------------------------
    Package enums có 6 file, mỗi file là 1 danh sách giá trị cố định.

        1. Role                → quyền trong hệ thống (file này, đọc trước: giải thích enum là gì)
        2. AccountStatus       → tài khoản có dùng được không
        3. AccessStatus        → có được chủ dự án cho vào web không
        4. ArtistRequestStatus → tiến trình xin làm Artist
        5. SongStatus          → trạng thái bài hát (vòng đời phức tạp nhất)
        6. OtpPurpose          → mục đích của mã OTP

    4 enum đầu là 4 trạng thái độc lập của 1 tài khoản (4 cột trong bảng users, xem entity User).
    Không có dòng import nào: enum không dùng class nào ngoài Java cơ bản.
*/

/*
    =====================================================================
    ENUM LÀ GÌ?
    =====================================================================

    Enum (enumeration, "kiểu liệt kê") là 1 kiểu dữ liệu chỉ có 1 danh sách giá trị cố định,
    viết sẵn trong code. Giống 1 ô chọn (dropdown) trên form: chỉ được chọn 1 trong các lựa chọn có sẵn,
    không được gõ chữ tự do.

    Ví dụ đời thường: ngày trong tuần chỉ có 7 giá trị (Thứ 2 → Chủ nhật), không có "Thứ 9".

    -----------------------------------------------------
    Cú pháp
    -----------------------------------------------------
        public enum Role {
            USER,
            ARTIST,
            ADMIN
        }

    - public enum Role: khai báo 1 enum tên Role (giống khai báo class, nhưng dùng chữ enum).
      Tên file phải trùng tên enum: Role.java.
    - USER, ARTIST, ADMIN: các giá trị (gọi là hằng số enum), cách nhau bằng dấu phẩy.
      Quy ước đặt tên: viết hoa toàn bộ, nhiều từ thì nối bằng dấu gạch dưới (VD RESET_PASSWORD).
    - Enum chỉ có danh sách giá trị, không có thuộc tính hay hàm nào thêm,
      nên sau giá trị cuối không cần dấu chấm phẩy.

    -----------------------------------------------------
    Dùng enum trong code Java
    -----------------------------------------------------
        Role role = Role.ADMIN;               → gán giá trị: TênEnum.TÊN_GIÁ_TRỊ
        if (user.getRole() != Role.USER) {...} → so sánh bằng == hoặc !=

    Vì sao so sánh enum được bằng == (khác String phải dùng .equals())?
    Mỗi giá trị enum chỉ có đúng 1 đối tượng duy nhất trong cả chương trình.
    Mọi chỗ viết Role.ADMIN đều trỏ tới cùng 1 đối tượng đó → == luôn đúng.

    Các hàm có sẵn của mọi enum:
        Role.ADMIN.name()       → chuỗi "ADMIN" (tên của giá trị)
        Role.valueOf("ADMIN")   → Role.ADMIN (đổi chuỗi thành enum; chuỗi sai → lỗi IllegalArgumentException)
        Role.values()           → mảng mọi giá trị [USER, ARTIST, ADMIN]
        Role.ADMIN.ordinal()    → 2 (số thứ tự, đếm từ 0; project không dùng để lưu, xem bên dưới)

    -----------------------------------------------------
    Vì sao dùng enum mà không dùng String?
    -----------------------------------------------------
    Nếu role là String:
    - Gõ nhầm "ADMNI" hay "admin" → trình biên dịch không báo lỗi, chạy mới thấy sai.
    - Có thể lưu nhầm giá trị lạ như "SUPERADMIN" vào database.
    Dùng enum:
    - Gõ nhầm Role.ADMNI → IntelliJ báo đỏ ngay, không build được.
    - Chỉ có đúng 3 giá trị, không thể có giá trị thứ 4.
    - IntelliJ gợi ý sẵn danh sách khi gõ Role. (có dấu chấm).

    =====================================================================
    ENUM ĐI QUA DATABASE VÀ JSON THẾ NÀO?
    =====================================================================

    -----------------------------------------------------
    Lưu vào MySQL
    -----------------------------------------------------
    Trong entity User: @Enumerated(EnumType.STRING) → lưu tên giá trị dạng chữ: "ADMIN".
    Hibernate tạo cột kiểu enum của MySQL: role enum('USER','ARTIST','ADMIN').
    (Không dùng EnumType.ORDINAL lưu số 0, 1, 2: lý do xem accountStatus trong User.)

    Vì lưu bằng tên, nên:
    - Đổi thứ tự các giá trị trong file này: không sao.
    - Đổi tên 1 giá trị (VD USER → LISTENER): dữ liệu cũ trong database vẫn là "USER"
      → Hibernate đọc không được → lỗi. Muốn đổi tên phải kèm câu UPDATE trong MySQL.
    - Thêm giá trị mới: phải tự sửa cột trong MySQL, cả máy local lẫn VPS
      (ddl-auto=update không sửa kiểu cột đã có, VPS dùng validate), VD:
          ALTER TABLE users MODIFY role enum('USER','ARTIST','ADMIN','MODERATOR') NOT NULL;

    -----------------------------------------------------
    Đổi sang JSON
    -----------------------------------------------------
    Jackson đổi enum ↔ chuỗi theo tên:
        Role.ARTIST  → "role": "ARTIST"   (trả về: LoginResponse, UserProfileResponse)
        "role": "ADMIN" → Role.ADMIN       (nhận vào: AssignRoleRequest)
    Chuỗi không có trong enum (VD "SUPERADMIN") → 400 "Invalid request data".
*/

/*
    =====================================================================
    ROLE - QUYỀN CỦA TÀI KHOẢN TRONG HỆ THỐNG
    =====================================================================

    Lưu ở cột users.role. Mỗi tài khoản có đúng 1 role tại 1 thời điểm.

    -----------------------------------------------------
    Role được kiểm tra thế nào? (Spring Security)
    -----------------------------------------------------
    1. Mỗi request, CustomUserDetailsService đọc role mới nhất từ MySQL:
           .roles(user.getRole().name())   → Spring tự thêm tiền tố, thành quyền "ROLE_ARTIST"
    2. Controller ghi @PreAuthorize("hasRole('ARTIST')") → Spring so "ROLE_ARTIST" với quyền ở bước 1.
       (hasRole tự thêm tiền tố ROLE_, nên trong ngoặc chỉ ghi ARTIST.)
    3. Không khớp → 403 "You do not have permission to access this resource".

    Vì đọc lại từ MySQL mỗi request, Admin đổi role thì có tác dụng ngay ở request tiếp theo,
    không cần đăng nhập lại.

    -----------------------------------------------------
    Đổi role thế nào?
    -----------------------------------------------------
        USER   → ARTIST : chỉ qua quy trình gửi yêu cầu làm Artist và Admin duyệt
                          (ArtistRequest, AdminService.approveArtistRequest).
        USER   → ADMIN  : Admin gán (PUT /api/admin/users/{userId}/role, AssignRoleRequest).
        ARTIST → USER   : Admin hạ quyền (cùng API trên), nghệ danh và bio bị xóa.
        ARTIST → ADMIN  : Admin gán (cùng API trên).
        ADMIN  → khác   : không đổi được qua API (tránh Admin hạ quyền lẫn nhau).

    Admin đầu tiên tạo thế nào?
    Chưa có Admin thì không ai gọi được API gán role → chủ dự án tự chạy SQL 1 lần:
        UPDATE users SET role = 'ADMIN' WHERE email = 'email_cua_ban@gmail.com';
*/
public enum Role {
    /*
        USER: người nghe. Giá trị mặc định khi đăng ký (xem entity User).
        Được nghe nhạc, xem lời bài hát, quản lý tài khoản cá nhân, gửi yêu cầu làm Artist.
    */
    USER,

    /*
        ARTIST: nghệ sĩ. Có mọi quyền của người nghe, cộng thêm:
        đăng và quản lý bài hát (ArtistSongController), viết lời bài hát (LyricsController).
        Bài hát chỉ hiện với người nghe khi Artist chính còn role ARTIST
        (bị hạ về USER thì toàn bộ bài tự ẩn, xem PUBLISHED_CONDITION trong SongRepository).
    */
    ARTIST,

    /*
        ADMIN: quản trị viên.
        Quản lý tài khoản (AdminController): khóa, mở khóa, duyệt yêu cầu làm Artist, gán role.
        Quản lý bài hát (AdminSongController): duyệt, từ chối, gỡ bài.
        Không đăng bài được (API của Artist yêu cầu đúng role ARTIST).
    */
    ADMIN
}