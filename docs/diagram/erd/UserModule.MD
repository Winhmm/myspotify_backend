# ERD – Module Người Dùng

# 1. Tổng quan

ERD (Entity Relationship Diagram) của **Module Người Dùng** mô tả cấu trúc Database và mối quan hệ giữa các bảng.

Hiện tại module gồm 2 bảng chính:

```text
users
otp_verifications
```

Quan hệ giữa hai bảng:

```text
users
  │
  │ 1
  │
  │
  │ N
otp_verifications
```

Có nghĩa:

> Một User có thể có nhiều bản ghi OTP.

---

# 2. Bảng `users`

Bảng `users` lưu toàn bộ thông tin liên quan đến tài khoản người dùng.

## 2.1. Cấu trúc bảng

| Column                  | Data Type    | Constraint | Ý nghĩa                             |
| ----------------------- | ------------ | ---------- | ----------------------------------- |
| `id`                    | BIGINT       | PK         | ID của User                         |
| `username`              | VARCHAR(50)  | UNIQUE     | Tên người dùng                      |
| `email`                 | VARCHAR(100) | UNIQUE     | Email                               |
| `password`              | VARCHAR(255) | NOT NULL   | Mật khẩu                            |
| `full_name`             | VARCHAR(100) | —          | Họ và tên                           |
| `avatar_url`            | VARCHAR(255) | —          | URL ảnh đại diện                    |
| `account_status`        | VARCHAR(20)  | NOT NULL   | Trạng thái tài khoản                |
| `role`                  | VARCHAR(20)  | NOT NULL   | Vai trò của User                    |
| `artist_request_status` | VARCHAR(20)  | NOT NULL   | Trạng thái yêu cầu trở thành Artist |
| `artist_name`           | VARCHAR(100) | —          | Nghệ danh                           |
| `bio`                   | VARCHAR(255) | —          | Thông tin giới thiệu                |

---

# 3. Chi tiết bảng `users`

## 3.1. `id`

```text
BIGINT
PRIMARY KEY
```

Dùng để định danh duy nhất mỗi User.

Ví dụ:

```text
1
2
3
4
```

Mỗi User chỉ có một `id`.

`id` cũng được sử dụng để liên kết User với các bảng khác trong Database.

---

## 3.2. `username`

```text
VARCHAR(50)
UNIQUE
```

Lưu username của User.

`UNIQUE` đảm bảo hai User không thể có cùng username.

Ví dụ:

```text
nguyenvana
phong123
user01
```

---

## 3.3. `email`

```text
VARCHAR(100)
UNIQUE
```

Lưu email của User.

Email được đánh dấu `UNIQUE`, vì vậy mỗi email chỉ được sử dụng cho một tài khoản.

Ví dụ:

```text
user@gmail.com
example@gmail.com
```

---

## 3.4. `password`

```text
VARCHAR(255)
NOT NULL
```

Lưu mật khẩu của User.

Trong hệ thống thực tế, giá trị lưu trong Database phải là **mật khẩu đã được mã hóa**, không phải mật khẩu plain text.

---

## 3.5. `full_name`

```text
VARCHAR(100)
```

Lưu họ và tên của User.

Ví dụ:

```text
Nguyễn Văn A
Trần Thị B
```

Thuộc tính này phục vụ cho thông tin cá nhân của User.

---

## 3.6. `avatar_url`

```text
VARCHAR(255)
```

Lưu đường dẫn đến ảnh đại diện của User.

Ví dụ:

```text
https://example.com/avatar/user1.jpg
```

Database chỉ lưu URL thay vì lưu trực tiếp file ảnh.

---

# 4. `account_status`

```text
VARCHAR(20)
NOT NULL
```

Lưu trạng thái hiện tại của tài khoản.

Diagram định nghĩa 4 giá trị:

```text
UNVERIFIED
ACTIVE
DISABLED
DELETED
```

## UNVERIFIED

Tài khoản chưa hoàn tất quá trình xác thực.

Liên quan đến quá trình:

```text
Đăng ký
    ↓
Gửi OTP
    ↓
Xác thực Email / OTP
```

## ACTIVE

Tài khoản đang hoạt động bình thường.

## DISABLED

Tài khoản đang bị vô hiệu hóa.

## DELETED

Tài khoản đã được đánh dấu xóa.

Theo diagram:

```text
DELETED → Xóa tài khoản (soft delete)
```

Tức là tài khoản có thể được đánh dấu `DELETED` thay vì xóa hoàn toàn bản ghi khỏi Database.

---

# 5. `role`

```text
VARCHAR(20)
NOT NULL
```

Lưu vai trò của User.

Diagram định nghĩa:

```text
USER
ARTIST
ADMIN
```

## USER

Role mặc định khi User đăng ký.

```text
Đăng ký
   ↓
role = USER
```

## ARTIST

Role được sử dụng cho User đã trở thành Artist.

Theo diagram, Role này được gán khi Admin duyệt yêu cầu trở thành Artist.

## ADMIN

Role dành cho Admin.

Role này được sử dụng để thực hiện các chức năng quản trị hệ thống.

---

# 6. `artist_request_status`

```text
VARCHAR(20)
NOT NULL
```

Lưu trạng thái yêu cầu trở thành Artist.

Diagram định nghĩa:

```text
NONE
PENDING
APPROVED
REJECTED
```

## NONE

User chưa gửi yêu cầu trở thành Artist.

Đây là trạng thái mặc định.

```text
artist_request_status = NONE
```

## PENDING

User đã gửi yêu cầu và đang chờ Admin xử lý.

Theo diagram:

```text
UC14 – Gửi yêu cầu trở thành Artist
        ↓
PENDING
```

## APPROVED

Admin đã chấp nhận yêu cầu.

```text
Admin duyệt
     ↓
APPROVED
```

## REJECTED

Admin đã từ chối yêu cầu.

```text
Admin từ chối
     ↓
REJECTED
```

---

# 7. `artist_name`

```text
VARCHAR(100)
```

Lưu nghệ danh của User.

Thuộc tính này liên quan đến chức năng trở thành Artist.

Ví dụ:

```text
artist_name = "Sơn Tùng M-TP"
```

Theo diagram, `artist_name` chỉ có giá trị khi:

```text
role = ARTIST
```

và liên quan đến:

```text
UC19 – Cập nhật hồ sơ Artist
```

---

# 8. `bio`

```text
VARCHAR(255)
```

Lưu phần giới thiệu của Artist.

Ví dụ:

```text
bio = "Singer and songwriter..."
```

Theo diagram:

```text
bio
↓
chỉ có giá trị khi role = ARTIST
```

và được sử dụng trong:

```text
UC19 – Cập nhật hồ sơ Artist
```

---

# 9. Bảng `otp_verifications`

Bảng `otp_verifications` dùng để lưu các mã OTP được tạo trong hệ thống.

Bảng này phục vụ các chức năng xác thực như:

```text
Đăng ký
Quên mật khẩu
Đổi mật khẩu
Cập nhật email
```

---

# 10. Cấu trúc bảng `otp_verifications`

| Column       | Data Type    | Constraint | Ý nghĩa                  |
| ------------ | ------------ | ---------- | ------------------------ |
| `id`         | BIGINT       | PK         | ID của OTP               |
| `user_id`    | BIGINT       | FK         | User sở hữu OTP          |
| `otp_code`   | VARCHAR(10)  | NOT NULL   | Mã OTP                   |
| `purpose`    | VARCHAR(20)  | NOT NULL   | Mục đích của OTP         |
| `new_email`  | VARCHAR(100) | —          | Email mới                |
| `expired_at` | TIMESTAMP    | NOT NULL   | Thời gian OTP hết hạn    |
| `verified`   | BOOLEAN      | NOT NULL   | OTP đã xác thực hay chưa |

---

# 11. Chi tiết bảng `otp_verifications`

## 11.1. `id`

```text
BIGINT
PRIMARY KEY
```

ID duy nhất của mỗi bản ghi OTP.

Ví dụ:

```text
1
2
3
4
```

Mỗi lần tạo một OTP có thể tạo một bản ghi mới trong bảng.

---

# 12. `user_id`

```text
BIGINT
FOREIGN KEY
```

`user_id` dùng để xác định OTP thuộc về User nào.

Đây là Foreign Key liên kết với:

```text
users.id
```

Quan hệ:

```text
users.id
      ↑
      │
otp_verifications.user_id
```

Ví dụ:

```text
users

id
---
1
2
3
```

và:

```text
otp_verifications

id   user_id
------------
1      1
2      1
3      2
4      1
```

Có thể thấy User có `id = 1` sở hữu nhiều OTP.

---

# 13. `otp_code`

```text
VARCHAR(10)
NOT NULL
```

Lưu mã OTP.

Ví dụ:

```text
482913
```

OTP được gửi đến email của User để thực hiện xác thực.

---

# 14. `purpose`

```text
VARCHAR(20)
NOT NULL
```

Xác định OTP được sử dụng cho mục đích gì.

Các giá trị được sử dụng trong thiết kế:

```text
REGISTER
RESET_PASSWORD
CHANGE_PASSWORD
CHANGE_EMAIL
```

## REGISTER

OTP dùng để xác thực đăng ký tài khoản.

## RESET_PASSWORD

OTP dùng trong quá trình quên và đặt lại mật khẩu.

## CHANGE_PASSWORD

OTP dùng để xác nhận đổi mật khẩu.

## CHANGE_EMAIL

OTP dùng để xác nhận cập nhật email.

---

# 15. `new_email`

```text
VARCHAR(100)
```

Lưu email mới trong trường hợp User muốn thay đổi email.

Ví dụ:

```text
Email hiện tại:
old@gmail.com

Email mới:
new@gmail.com
```

Trong quá trình xác thực:

```text
new_email = new@gmail.com
```

Sau khi OTP hợp lệ, hệ thống có thể sử dụng giá trị này để cập nhật email của User.

---

# 16. `expired_at`

```text
TIMESTAMP
NOT NULL
```

Lưu thời điểm OTP hết hạn.

Ví dụ:

```text
OTP được tạo:
20:00:00

OTP hết hạn:
20:05:00
```

Khi User nhập OTP, hệ thống cần kiểm tra OTP còn thời hạn hay không.

---

# 17. `verified`

```text
BOOLEAN
NOT NULL
```

Cho biết OTP đã được xác thực hay chưa.

Có thể hiểu:

```text
false
↓
OTP chưa được xác thực
```

và:

```text
true
↓
OTP đã được xác thực
```

---

# 18. Quan hệ giữa `users` và `otp_verifications`

ERD thể hiện quan hệ:

```text
users
  │
  │ has
  │
  │ 1
  │
  │
  │ N
otp_verifications
```

Đây là quan hệ:

```text
1 User
   ↓
N OTP Verification
```

Hay:

> Một User có thể có nhiều bản ghi OTP.

Ví dụ:

```text
User
id = 1
email = user@gmail.com
```

có thể có:

```text
OTP 1 → REGISTER
OTP 2 → CHANGE_PASSWORD
OTP 3 → CHANGE_EMAIL
OTP 4 → RESET_PASSWORD
```

Tất cả đều có:

```text
user_id = 1
```

---

# 19. Vì sao cần bảng `otp_verifications` riêng?

OTP không được lưu trực tiếp trong bảng `users`.

Thay vào đó:

```text
users
```

chứa thông tin tài khoản.

```text
otp_verifications
```

chứa thông tin xác thực OTP.

Tách thành hai bảng giúp phân biệt rõ:

```text
Thông tin User
        ↓
users

Thông tin OTP
        ↓
otp_verifications
```

Ngoài ra, một User có thể tạo nhiều OTP trong quá trình sử dụng hệ thống.

---

# 20. Ví dụ dữ liệu

## Bảng `users`

```text
id | username | email              | role | account_status | artist_request_status
-------------------------------------------------------------------------------------
1  | phong    | phong@gmail.com     | USER | ACTIVE         | NONE
2  | artist1  | artist@gmail.com    | ARTIST | ACTIVE      | APPROVED
```

## Bảng `otp_verifications`

```text
id | user_id | otp_code | purpose          | new_email       | verified
----------------------------------------------------------------------
1  | 1       | 123456   | REGISTER         | NULL            | true
2  | 1       | 654321   | CHANGE_PASSWORD  | NULL            | false
3  | 1       | 987654   | CHANGE_EMAIL     | new@gmail.com   | false
```

Ví dụ trên minh họa rằng User có `id = 1` có thể có nhiều OTP khác nhau.

---

# 21. Luồng đăng ký

```text
User
 ↓
Đăng ký
 ↓
users
 ↓
account_status = UNVERIFIED
 ↓
Tạo OTP
 ↓
otp_verifications
 ↓
purpose = REGISTER
 ↓
Gửi OTP qua Email
 ↓
User nhập OTP
 ↓
Xác thực thành công
 ↓
account_status = ACTIVE
```

---

# 22. Luồng quên mật khẩu

```text
User nhập Email
        ↓
Tạo OTP
        ↓
otp_verifications
        ↓
purpose = RESET_PASSWORD
        ↓
Gửi OTP
        ↓
User nhập OTP
        ↓
Xác thực OTP
        ↓
Đặt mật khẩu mới
```

---

# 23. Luồng đổi mật khẩu

```text
User
 ↓
Nhập mật khẩu hiện tại
 ↓
Kiểm tra mật khẩu
 ↓
Tạo OTP
 ↓
otp_verifications
 ↓
purpose = CHANGE_PASSWORD
 ↓
Gửi OTP
 ↓
Xác thực OTP
 ↓
Đổi mật khẩu
```

---

# 24. Luồng cập nhật email

```text
User
 ↓
Nhập email mới
 ↓
Kiểm tra email mới
 ↓
Tạo OTP
 ↓
otp_verifications
 ↓
purpose = CHANGE_EMAIL
 ↓
new_email = Email mới
 ↓
Gửi OTP
 ↓
User nhập OTP
 ↓
Xác thực
 ↓
Cập nhật email
```

---

# 25. Luồng trở thành Artist

Thông tin liên quan đến Artist được lưu trực tiếp trong bảng `users`.

Ban đầu:

```text
role = USER
artist_request_status = NONE
```

User gửi yêu cầu:

```text
UC14
 ↓
artist_name
bio
 ↓
artist_request_status = PENDING
```

Admin xử lý:

### Trường hợp duyệt

```text
artist_request_status = APPROVED
role = ARTIST
```

### Trường hợp từ chối

```text
artist_request_status = REJECTED
```

Sau khi trở thành Artist, User có thể cập nhật:

```text
artist_name
bio
```

theo:

```text
UC19 – Cập nhật hồ sơ Artist
```

---

# 26. Luồng khóa / vô hiệu hóa tài khoản

Trạng thái tài khoản được lưu trong:

```text
users.account_status
```

Khi tài khoản bị vô hiệu hóa:

```text
account_status = DISABLED
```

Khi tài khoản được mở lại:

```text
account_status = ACTIVE
```

---

# 27. Luồng xóa tài khoản

Theo ERD, `account_status` có trạng thái:

```text
DELETED
```

Do đó chức năng xóa tài khoản được thiết kế theo hướng:

```text
User
 ↓
Xác nhận xóa
 ↓
account_status = DELETED
```

Thay vì nhất thiết phải xóa toàn bộ record khỏi bảng `users`.

Đây là cách biểu diễn **soft delete** theo ghi chú trong diagram.

---

# 28. Sơ đồ Database tổng quát

```text
┌──────────────────────────────────────┐
│                users                 │
├──────────────────────────────────────┤
│ PK  id                               │
│     username       UNIQUE            │
│     email          UNIQUE            │
│     password                         │
│     full_name                        │
│     avatar_url                       │
│     account_status                   │
│     role                             │
│     artist_request_status            │
│     artist_name                      │
│     bio                              │
└──────────────────┬───────────────────┘
                   │
                   │ 1
                   │
                   │
                   │ N
┌──────────────────▼───────────────────┐
│          otp_verifications           │
├──────────────────────────────────────┤
│ PK  id                               │
│ FK  user_id                          │
│     otp_code                         │
│     purpose                          │
│     new_email                        │
│     expired_at                       │
│     verified                         │
└──────────────────────────────────────┘
```

---

# 29. Tổng hợp Database

## Bảng `users`

Chịu trách nhiệm lưu:

```text
Thông tin tài khoản
Thông tin cá nhân
Trạng thái tài khoản
Role
Thông tin yêu cầu Artist
Thông tin hồ sơ Artist
```

## Bảng `otp_verifications`

Chịu trách nhiệm lưu:

```text
OTP
User sở hữu OTP
Mục đích OTP
Email mới
Thời gian hết hạn
Trạng thái xác thực
```

## Quan hệ

```text
users 1 ─────────── N otp_verifications
```

---

# 30. Mapping giữa Entity và Database

Trong Backend:

```text
User Entity
     ↓
users table
```

và:

```text
OtpVerification Entity
     ↓
otp_verifications table
```

Mapping quan trọng:

| Java Entity                 | Database                       |
| --------------------------- | ------------------------------ |
| `User.id`                   | `users.id`                     |
| `User.username`             | `users.username`               |
| `User.email`                | `users.email`                  |
| `User.password`             | `users.password`               |
| `User.fullName`             | `users.full_name`              |
| `User.avatarUrl`            | `users.avatar_url`             |
| `User.accountStatus`        | `users.account_status`         |
| `User.role`                 | `users.role`                   |
| `User.artistRequestStatus`  | `users.artist_request_status`  |
| `User.artistName`           | `users.artist_name`            |
| `User.bio`                  | `users.bio`                    |
| `OtpVerification.id`        | `otp_verifications.id`         |
| `OtpVerification.user`      | `otp_verifications.user_id`    |
| `OtpVerification.otpCode`   | `otp_verifications.otp_code`   |
| `OtpVerification.purpose`   | `otp_verifications.purpose`    |
| `OtpVerification.newEmail`  | `otp_verifications.new_email`  |
| `OtpVerification.expiredAt` | `otp_verifications.expired_at` |
| `OtpVerification.verified`  | `otp_verifications.verified`   |

---

# 31. Kết luận

ERD của Module Người Dùng hiện được thiết kế với hai bảng chính:

```text
users
otp_verifications
```

Trong đó:

```text
users
```

là bảng trung tâm, lưu thông tin tài khoản, trạng thái, role và thông tin Artist.

```text
otp_verifications
```

lưu các OTP liên quan đến User.

Quan hệ giữa hai bảng:

```text
users 1 ─────────── N otp_verifications
```

Các trạng thái và vai trò quan trọng được lưu dưới dạng `VARCHAR`:

```text
account_status
    ↓
UNVERIFIED
ACTIVE
DISABLED
DELETED

role
    ↓
USER
ARTIST
ADMIN

artist_request_status
    ↓
NONE
PENDING
APPROVED
REJECTED
```

Thiết kế này là cơ sở để triển khai tiếp:

```text
ERD
 ↓
Database
 ↓
Entity
 ↓
Repository
 ↓
Service
 ↓
Controller
 ↓
API
```
