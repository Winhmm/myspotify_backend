package com.winhmm.myspotify.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/*
    =====================================================
    REGISTERREQUEST - DỮ LIỆU ĐĂNG KÝ TÀI KHOẢN
    =====================================================
    Dùng cho: POST /api/auth/register (UC01)

    JSON frontend gửi lên:
        {
            "username": "maychieu",
            "email": "maychieu@gmail.com",
            "password": "matkhau123"
        }

    -----------------------------------------------------
    DTO là gì?
    -----------------------------------------------------
    DTO (Data Transfer Object) là class chỉ dùng để chứa dữ liệu
    đi vào (request) hoặc đi ra (response) qua API.

    Vì sao không nhận thẳng entity User?
    - Entity User có nhiều trường frontend không được phép gửi lên:
      role, accountStatus, artistRequestStatus...
      Nếu nhận thẳng entity, kẻ gian có thể gửi kèm "role": "ADMIN" để tự thăng quyền.
    - DTO chỉ có đúng 3 trường cần cho đăng ký → trường lạ gửi lên bị bỏ qua.

    -----------------------------------------------------
    JSON được chuyển thành đối tượng thế nào?
    -----------------------------------------------------
    Thư viện Jackson (có sẵn trong Spring Boot) làm việc này:
    1. Tạo đối tượng bằng constructor không tham số (Java tự tạo khi class không khai báo constructor nào).
    2. Gọi setter tương ứng với từng key trong JSON:
       "username" → setUsername(...), "email" → setEmail(...)...
    Vì vậy request DTO cần có setter.

    -----------------------------------------------------
    Validation hoạt động thế nào?
    -----------------------------------------------------
    Các annotation trên field (@NotBlank, @Email, @Size, @Pattern) là các quy tắc kiểm tra.
    Chúng chỉ có tác dụng khi Controller ghi @Valid trước @RequestBody.

    Khi dữ liệu vi phạm quy tắc:
    - Spring dừng ngay, không gọi Service.
    - Ném MethodArgumentNotValidException.
    - GlobalExceptionHandler bắt lỗi, trả 400 kèm message của quy tắc đầu tiên bị vi phạm:
          { "message": "Username must be between 5 and 20 characters" }

    Ý nghĩa từng annotation:
    - @NotBlank: không được null, rỗng "" hoặc chỉ có dấu cách "   ".
    - @Email: phải đúng định dạng email (có @ và tên miền).
    - @Size(min, max): độ dài chuỗi nằm trong khoảng cho phép.
    - @Pattern(regexp): chuỗi phải khớp biểu thức chính quy (regex).
*/
public class RegisterRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 5, max = 20, message = "Username must be between 5 and 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_.]+$",
            message = "Username can only contain letters, numbers, dot and underscore")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 72, message = "Password must be between 6 and 72 characters")
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
