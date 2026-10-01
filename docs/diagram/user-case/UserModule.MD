# USER MODULE – USE CASE DOCUMENTATION

# 1. Tổng quan

User Module quản lý các chức năng liên quan đến:

* Người dùng chưa đăng nhập (Guest)
* Người dùng đã đăng nhập (User)
* Quản trị viên (Admin)
* Nghệ sĩ (Artist)

Module bao gồm các chức năng từ đăng ký tài khoản, xác thực email, đăng nhập, quản lý tài khoản cá nhân cho đến quản lý User và xử lý yêu cầu trở thành Artist.

---

# 2. Các vai trò trong User Module

## 2.1. Guest

Guest là người dùng **chưa đăng nhập** vào hệ thống.

Guest có thể thực hiện:

1. Đăng ký
2. Xác thực Email / OTP
3. Đăng nhập
4. Quên mật khẩu
5. Đặt lại mật khẩu

---

## 2.2. User

User là người dùng **đã đăng nhập** vào hệ thống.

User có thể thực hiện:

6. Đăng xuất
7. Xem thông tin cá nhân
8. Cập nhật thông tin cá nhân
9. Đổi mật khẩu
10. Cập nhật ảnh đại diện
11. Vô hiệu hóa tài khoản
12. Xóa tài khoản
13. Cập nhật email
14. Gửi yêu cầu trở thành Artist

---

## 2.3. Admin

Admin là người quản trị hệ thống.

Admin có thể thực hiện:

15. Xem danh sách người dùng
16. Khóa / Mở khóa tài khoản
17. Duyệt yêu cầu trở thành Artist
18. Gán vai trò (phân quyền)

---

## 2.4. Artist

Artist là User đã được hệ thống xác nhận với vai trò Artist.

Artist có thể thực hiện:

19. Cập nhật hồ sơ Artist

---

# 3. Danh sách Use Case

| STT | Use Case                       | Actor  |
| --- | ------------------------------ | ------ |
| 01  | Đăng ký                        | Guest  |
| 02  | Xác thực Email / OTP           | Guest  |
| 03  | Đăng nhập                      | Guest  |
| 04  | Quên mật khẩu                  | Guest  |
| 05  | Đặt lại mật khẩu               | Guest  |
| 06  | Đăng xuất                      | User   |
| 07  | Xem thông tin cá nhân          | User   |
| 08  | Cập nhật thông tin cá nhân     | User   |
| 09  | Đổi mật khẩu                   | User   |
| 10  | Cập nhật ảnh đại diện          | User   |
| 11  | Vô hiệu hóa tài khoản          | User   |
| 12  | Xóa tài khoản                  | User   |
| 13  | Cập nhật email                 | User   |
| 14  | Gửi yêu cầu trở thành Artist   | User   |
| 15  | Xem danh sách người dùng       | Admin  |
| 16  | Khóa / Mở khóa tài khoản       | Admin  |
| 17  | Duyệt yêu cầu trở thành Artist | Admin  |
| 18  | Gán vai trò (phân quyền)       | Admin  |
| 19  | Cập nhật hồ sơ Artist          | Artist |

---

# 4. Nhóm chức năng Guest

## 4.1. Đăng ký

Guest sử dụng chức năng này để tạo tài khoản mới.

### Luồng

```text
Guest
↓
Đăng ký
↓
Nhập thông tin đăng ký
↓
Tạo tài khoản
↓
Xác thực Email / OTP
↓
Hoàn tất đăng ký
```

Sau khi đăng ký, hệ thống yêu cầu người dùng xác thực Email / OTP.

---

# 5. Xác thực Email / OTP

Chức năng này dùng để xác minh người dùng thông qua mã OTP được gửi qua email.

Chức năng này được sử dụng trong quá trình đăng ký.

### Luồng

```text
Đăng ký
↓
Gửi OTP
↓
User nhập OTP
↓
Kiểm tra OTP
↓
OTP hợp lệ
↓
Hoàn tất xác thực
```

Nếu OTP không hợp lệ hoặc hết hạn, quá trình xác thực không được hoàn tất.

---

# 6. Đăng nhập

Guest sử dụng chức năng đăng nhập để truy cập vào hệ thống.

### Luồng tổng quát

```text
Guest
↓
Đăng nhập
↓
Nhập email
↓
Nhập password
↓
Kiểm tra thông tin
↓
Đăng nhập thành công
↓
User
```

Sau khi đăng nhập thành công, người dùng trở thành User đã đăng nhập.

---

# 7. Quên mật khẩu

Guest sử dụng chức năng này khi không nhớ mật khẩu.

### Luồng

```text
Quên mật khẩu
↓
Xác thực OTP
↓
Đặt lại mật khẩu
↓
Đăng nhập
```

Quá trình quên mật khẩu bao gồm việc xác thực OTP trước khi cho phép đặt lại mật khẩu.

---

# 8. Đặt lại mật khẩu

Sau khi xác thực OTP thành công, người dùng có thể đặt mật khẩu mới.

### Luồng

```text
Quên mật khẩu
↓
Xác thực OTP
↓
Đặt lại mật khẩu
↓
Nhập mật khẩu mới
↓
Xác nhận
↓
Cập nhật mật khẩu
```

---

# 9. Nhóm chức năng User

User là người dùng đã đăng nhập.

Các chức năng của User gồm:

* Đăng xuất
* Xem thông tin cá nhân
* Cập nhật thông tin cá nhân
* Đổi mật khẩu
* Cập nhật ảnh đại diện
* Vô hiệu hóa tài khoản
* Xóa tài khoản
* Cập nhật email
* Gửi yêu cầu trở thành Artist

---

# 10. Đăng xuất

User sử dụng chức năng này để đăng xuất khỏi hệ thống.

### Luồng

```text
User
↓
Đăng xuất
↓
Kết thúc phiên đăng nhập
```

Sau khi đăng xuất, người dùng trở thành Guest.

---

# 11. Xem thông tin cá nhân

User sử dụng chức năng này để xem thông tin cá nhân của tài khoản.

Các thông tin có thể bao gồm:

* Username
* Email
* Họ tên
* Ảnh đại diện
* Vai trò
* Thông tin Artist nếu có

---

# 12. Cập nhật thông tin cá nhân

User sử dụng chức năng này để cập nhật thông tin cá nhân.

### Luồng

```text
User
↓
Cập nhật thông tin cá nhân
↓
Nhập thông tin mới
↓
Kiểm tra dữ liệu
↓
Cập nhật thông tin
```

---

# 13. Đổi mật khẩu

Đổi mật khẩu là chức năng yêu cầu nhiều bước xác thực để đảm bảo người đang thực hiện thao tác là chủ tài khoản.

### Luồng chi tiết

```text
1. Nhập mật khẩu hiện tại
        ↓
2. Kiểm tra mật khẩu hiện tại
        ↓
3. Gửi OTP qua Email
        ↓
4. Nhập và xác thực OTP
        ↓
5. Nhập mật khẩu mới
        ↓
6. Xác nhận mật khẩu mới
        ↓
7. Cập nhật mật khẩu
```

### Mục đích

Hệ thống kiểm tra:

* Mật khẩu hiện tại có chính xác không.
* OTP có hợp lệ không.
* Mật khẩu mới có được xác nhận chính xác không.

Sau khi tất cả bước hợp lệ, hệ thống cập nhật mật khẩu mới.

---

# 14. Cập nhật ảnh đại diện

User có thể thay đổi ảnh đại diện của tài khoản.

### Luồng

```text
User
↓
Cập nhật ảnh đại diện
↓
Chọn ảnh mới
↓
Upload ảnh
↓
Cập nhật avatar
```

---

# 15. Vô hiệu hóa tài khoản

User có thể vô hiệu hóa tài khoản của mình.

### Luồng chi tiết

```text
1. Nhập mật khẩu hiện tại
        ↓
2. Kiểm tra mật khẩu
        ↓
3. Xác nhận vô hiệu hóa
        ↓
4. Vô hiệu hóa tài khoản
```

Hệ thống yêu cầu mật khẩu hiện tại để xác nhận quyền thực hiện thao tác.

---

# 16. Xóa tài khoản

User có thể yêu cầu xóa tài khoản.

### Luồng chi tiết

```text
1. Nhập mật khẩu hiện tại
        ↓
2. Kiểm tra mật khẩu
        ↓
3. Xác nhận xóa tài khoản
        ↓
4. Xóa tài khoản
```

Hệ thống yêu cầu người dùng xác nhận trước khi thực hiện thao tác xóa.

---

# 17. Cập nhật email

User có thể thay đổi email của tài khoản.

### Luồng chi tiết

```text
1. Nhập email mới
        ↓
2. Kiểm tra email mới chưa được dùng
        ↓
3. Gửi OTP qua Email mới
        ↓
4. Nhập và xác thực OTP
        ↓
5. Cập nhật email
```

### Giải thích

Đầu tiên hệ thống kiểm tra email mới có đang được sử dụng bởi tài khoản khác hay không.

Nếu email chưa được sử dụng:

```text
Email mới
↓
Gửi OTP
↓
User nhập OTP
↓
Xác thực thành công
↓
Cập nhật email
```

---

# 18. Gửi yêu cầu trở thành Artist

User có thể gửi yêu cầu để trở thành Artist.

### Luồng chi tiết

```text
1. Điền thông tin nghệ danh
        ↓
2. Gửi yêu cầu
        ↓
3. Chờ Admin duyệt
```

Sau khi gửi yêu cầu, User chờ Admin xử lý yêu cầu.

---

# 19. Nhóm chức năng Admin

Admin có các chức năng quản lý User:

* Xem danh sách người dùng
* Khóa / Mở khóa tài khoản
* Duyệt yêu cầu trở thành Artist
* Gán vai trò (phân quyền)

---

# 20. Xem danh sách người dùng

Admin có thể xem danh sách các User trong hệ thống.

Thông tin có thể được sử dụng để quản lý:

* Tài khoản
* Trạng thái tài khoản
* Vai trò
* Thông tin người dùng

---

# 21. Khóa / Mở khóa tài khoản

Admin có thể khóa hoặc mở khóa tài khoản người dùng.

### Luồng

```text
1. Chọn tài khoản cần xử lý
        ↓
2. Xác nhận thao tác
        ↓
3. Cập nhật trạng thái tài khoản
```

### Các trường hợp

Admin có thể:

```text
Tài khoản đang hoạt động
↓
Khóa tài khoản
```

Hoặc:

```text
Tài khoản đang bị khóa
↓
Mở khóa tài khoản
```

---

# 22. Duyệt yêu cầu trở thành Artist

Admin xử lý các yêu cầu trở thành Artist từ User.

### Luồng

```text
1. Xem danh sách yêu cầu
        ↓
2. Chấp nhận hoặc từ chối
        ↓
3. Nếu chấp nhận
        ↓
Cập nhật vai trò của User thành Artist
```

### Nếu Admin chấp nhận

```text
User
↓
Gửi yêu cầu Artist
↓
Admin duyệt
↓
Chấp nhận
↓
Cập nhật Role
↓
User trở thành Artist
```

### Nếu Admin từ chối

```text
User
↓
Gửi yêu cầu Artist
↓
Admin duyệt
↓
Từ chối
↓
User không trở thành Artist
```

---

# 23. Gán vai trò (Phân quyền)

Admin có chức năng gán vai trò cho User.

Các vai trò được thể hiện trong hệ thống gồm:

* Guest
* User
* Admin
* Artist

Role được sử dụng để xác định quyền truy cập vào các chức năng của hệ thống.

---

# 24. Nhóm chức năng Artist

Artist là người dùng đã được hệ thống xác nhận với vai trò Artist.

Artist có chức năng:

* Cập nhật hồ sơ Artist

---

# 25. Cập nhật hồ sơ Artist

Artist có thể cập nhật thông tin hồ sơ Artist của mình.

Thông tin hồ sơ Artist có thể liên quan đến:

* Nghệ danh
* Thông tin giới thiệu
* Thông tin hồ sơ Artist

Chức năng này dành cho Actor Artist.

---

# 26. Quan hệ giữa các Actor

Có thể hiểu các Actor trong hệ thống theo luồng:

```text
Guest
  ↓
Đăng ký
  ↓
Xác thực
  ↓
User
  ↓
Gửi yêu cầu trở thành Artist
  ↓
Admin duyệt
  ↓
Artist
```

Trong đó:

```text
Guest
→ Người chưa đăng nhập

User
→ Người đã đăng nhập

Admin
→ Người quản trị hệ thống

Artist
→ User được chấp nhận trở thành Artist
```

---

# 27. Luồng tổng quát của User Module

```text
                         USER MODULE

                              │
          ┌───────────────────┼───────────────────┐
          │                   │                   │
        Guest                User               Admin
          │                   │                   │
          │                   │                   │
     Đăng ký             Đăng xuất          Xem User
     Xác thực             Xem Profile        Khóa/Mở khóa
     Đăng nhập            Cập nhật Profile   Duyệt Artist
     Quên mật khẩu        Đổi mật khẩu       Gán Role
     Đặt lại mật khẩu     Đổi email
                          Đổi Avatar
                          Disable
                          Delete
                          Yêu cầu Artist
                              │
                              ▼
                           Artist
                              │
                              ▼
                       Cập nhật hồ sơ
```

---

# 28. Tổng kết Use Case

User Module gồm tổng cộng 19 Use Case:

### Guest

1. Đăng ký
2. Xác thực Email / OTP
3. Đăng nhập
4. Quên mật khẩu
5. Đặt lại mật khẩu

### User

6. Đăng xuất
7. Xem thông tin cá nhân
8. Cập nhật thông tin cá nhân
9. Đổi mật khẩu
10. Cập nhật ảnh đại diện
11. Vô hiệu hóa tài khoản
12. Xóa tài khoản
13. Cập nhật email
14. Gửi yêu cầu trở thành Artist

### Admin

15. Xem danh sách người dùng
16. Khóa / Mở khóa tài khoản
17. Duyệt yêu cầu trở thành Artist
18. Gán vai trò (phân quyền)

### Artist

19. Cập nhật hồ sơ Artist


