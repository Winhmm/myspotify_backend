package com.winhmm.myspotify.dto.user;

/*
    -----------------------------------------------------
    Dòng import
    -----------------------------------------------------
    (Các khái niệm cơ bản: xem RegisterRequest. Thứ tự đọc package dto.user: xem UpdateProfileRequest.)

    - NotBlank: không được null, rỗng hoặc chỉ toàn dấu cách.
*/
import jakarta.validation.constraints.NotBlank;

/*
    =====================================================
    ACCOUNTACTIONREQUEST - NHẬP MẬT KHẨU ĐỂ TỰ KHÓA HOẶC TỰ XÓA TÀI KHOẢN (UC11, UC12)
    =====================================================

    API (xem UserController):
        PUT /api/users/me/disable   → tự vô hiệu hóa tài khoản (UC11)
        PUT /api/users/me/delete    → tự xóa tài khoản, xóa mềm (UC12)

    JSON frontend gửi lên (giống nhau cho cả 2 API):
        {
            "password": "matkhau123"
        }

    Dùng chung 1 DTO cho 2 API vì cả 2 chỉ cần đúng 1 thứ: mật khẩu để xác nhận.
    Vì vậy tên class đặt chung là AccountAction (hành động trên tài khoản),
    thay vì tạo 2 class giống hệt nhau DisableAccountRequest và DeleteAccountRequest.
    (Giống RejectSongRequest dùng chung cho từ chối bài và gỡ bài.)

    Vì sao phải nhập lại mật khẩu dù đã đăng nhập?
    Đây là hành động nghiêm trọng. Người mượn máy (token đang đăng nhập sẵn)
    không biết mật khẩu → không khóa hay xóa tài khoản giúp được.

    -----------------------------------------------------
    Backend làm gì? (UserService.disableAccount, UserService.deleteAccount)
    -----------------------------------------------------
    1. Tài khoản phải đang ACTIVE → 400 "Account is not active".
       (Lớp bảo vệ thêm. Thực tế hiếm gặp, vì tài khoản không ACTIVE đã bị đăng xuất từ trước.)
    2. Mật khẩu phải đúng → 400 "Password does not match".
    3. Đổi accountStatus sang DISABLED (API /disable) hoặc DELETED (API /delete).
    4. Xóa phiên trong Redis → đăng xuất ngay.

    Kết quả:
        200 { "message": "Account has been disabled" }
        200 { "message": "Account has been deleted" }
    Frontend: thành công → xóa token, chuyển về trang chủ hoặc trang đăng nhập.

    -----------------------------------------------------
    DISABLED và DELETED khác nhau thế nào?
    -----------------------------------------------------
                            DISABLED (/disable)               DELETED (/delete)
        Đăng nhập           400 "Account has been disabled"   400 "Account has been deleted"
        Dùng lại được không Admin mở khóa là dùng lại được   Không (Admin chỉ mở khóa tài khoản DISABLED)
        Dữ liệu trong MySQL giữ nguyên                        giữ nguyên (xóa mềm, chỉ đổi trạng thái)
        Email, username     vẫn bị giữ                        vẫn bị giữ, không đăng ký lại được

    Nếu là Artist: cả 2 trường hợp, bài hát tự ẩn khỏi người nghe
    (điều kiện phát hành yêu cầu Artist phải ACTIVE).
*/
public class AccountActionRequest {
    /*
        -----------------------------------------------------
        password: mật khẩu hiện tại (dạng gốc, chưa băm)
        -----------------------------------------------------
        - @NotBlank: bắt buộc phải có → "Password is required".

        Không có @Size: mật khẩu này không được lưu, chỉ dùng để so với mật khẩu đã lưu
        (giải thích chi tiết: xem currentPassword trong RequestChangePasswordOtpRequest).

        Chú ý tên key: ở đây là "password", còn ở bước 1 đổi mật khẩu là "currentPassword".
        Cả 2 đều là mật khẩu hiện tại, nhưng tên key phải khớp đúng setter của từng DTO.
        Gửi { "currentPassword": "..." } vào API /disable → Jackson bỏ qua key lạ
        → password = null → 400 "Password is required".
    */
    @NotBlank(message = "Password is required")
    private String password;

    /*
        -----------------------------------------------------
        Getter và setter
        -----------------------------------------------------
        - Setter: Jackson gọi khi đổi JSON thành đối tượng ("password" → setPassword).
        - Getter: UserController gọi request.getPassword() để truyền xuống
          UserService.disableAccount hoặc UserService.deleteAccount.
        Cách đọc từng dòng: xem RegisterRequest.
    */
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}