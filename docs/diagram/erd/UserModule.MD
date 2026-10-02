# ERD – Module Người Dùng

## 1. Tổng quan

ERD (Entity Relationship Diagram) của **Module Người Dùng** mô tả cấu trúc dữ liệu được lưu trữ trong Database, bao gồm:

* Các bảng trong Database
* Các thuộc tính của từng bảng
* Kiểu dữ liệu của các thuộc tính
* Primary Key
* Foreign Key
* Unique Constraint
* Các mối quan hệ giữa các bảng
* Một số giá trị mặc định và trạng thái được sử dụng trong hệ thống

Module hiện tại gồm 2 bảng chính:

```text
users
otp_verifications
```

Quan hệ giữa hai bảng:

```text
users
  1
  |
  |
  *
otp_verifications
```

Điều này có nghĩa:

```text
Một User có thể có nhiều OTP

Mỗi OTP thuộc về một User
```

---

# 2. Cấu trúc Database

Database của Module Người Dùng gồm:

```text
users
    |
    | 1 - N
    |
otp_verifications
```

Trong đó:

* `users` lưu thông tin tài khoản người dùng.
* `otp_verifications` lưu các OTP được tạo cho người dùng.
* `users.id` là Primary Key của bảng `users`.
* `otp_verifications.id` là Primary Key của bảng `otp_verifications`.
* `otp_verifications.user_id` là Foreign Key tham chiếu đến `users.id`.

---

# 3. Bảng users

## 3.1. Mục đích

Bảng `users` lưu trữ thông tin của tài khoản người dùng trong hệ thống.

Bảng này chứa:

* Thông tin đăng nhập
* Thông tin cá nhân
* Trạng thái tài khoản
* Vai trò của tài khoản
* Thông tin liên quan đến Artist

---

## 3.2. Cấu trúc bảng

| Column                  | Data Type    | Constraint | Ý nghĩa                             |
| ----------------------- | ------------ | ---------- | ----------------------------------- |
| `id`                    | BIGINT       | PK         | ID của User                         |
| `username`              | VARCHAR(50)  | UNIQUE     | Tên đăng nhập                       |
| `email`                 | VARCHAR(100) | UNIQUE     | Email của User                      |
| `password`              | VARCHAR(255) | NOT NULL   | Mật khẩu                            |
| `full_name`             | VARCHAR(100) |            | Họ và tên                           |
| `avatar_url`            | VARCHAR(255) |            | Đường dẫn ảnh đại diện              |
| `account_status`        | VARCHAR(20)  | NOT NULL   | Trạng thái tài khoản                |
| `role`                  | VARCHAR(20)  | NOT NULL   | Vai trò của User                    |
| `artist_request_status` | VARCHAR(20)  | NOT NULL   | Trạng thái yêu cầu trở thành Artist |
| `artist_name`           | VARCHAR(100) |            | Nghệ danh                           |
| `bio`                   | VARCHAR(255) |            | Thông tin giới thiệu                |

---

# 4. Chi tiết bảng users

## 4.1. id

```text
id : BIGINT
```

`id` là định danh của User.

Đây là:

```text
PRIMARY KEY
```

Mỗi User có một `id` duy nhất.

Ví dụ về mặt ý nghĩa:

```text
User A → id = 1
User B → id = 2
User C → id = 3
```

`id` được sử dụng để xác định chính xác một User trong Database.

---

## 4.2. username

```text
username : VARCHAR(50)
```

Lưu tên đăng nhập của User.

Constraint:

```text
UNIQUE
```

Điều này có nghĩa là hai User không được có cùng `username`.

Ví dụ:

```text
User 1 → phong123
User 2 → abc123
```

Không thể tồn tại:

```text
User 1 → phong123
User 2 → phong123
```

---

## 4.3. email

```text
email : VARCHAR(100)
```

Lưu email của User.

Constraint:

```text
UNIQUE
```

Mỗi email chỉ được sử dụng cho một tài khoản.

Email có thể được sử dụng để:

```text
Đăng nhập
Xác thực tài khoản
Gửi OTP
Quên mật khẩu
Cập nhật email
```

---

## 4.4. password

```text
password : VARCHAR(255)
```

Lưu mật khẩu của User.

Độ dài:

```text
VARCHAR(255)
```

Cột này được đánh dấu bắt buộc trong diagram.

Mật khẩu trong Database cần được lưu dưới dạng đã mã hóa thay vì lưu trực tiếp mật khẩu gốc.

---

## 4.5. full_name

```text
full_name : VARCHAR(100)
```

Lưu họ và tên của User.

Ví dụ về mặt ý nghĩa:

```text
Nguyễn Thanh Phong
```

Thuộc tính này phục vụ cho thông tin Profile của User.

---

## 4.6. avatar_url

```text
avatar_url : VARCHAR(255)
```

Lưu đường dẫn đến ảnh đại diện của User.

Ví dụ về mặt ý nghĩa:

```text
/uploads/avatar/user_1.jpg
```

Database chỉ lưu đường dẫn hoặc URL đến ảnh, không lưu trực tiếp file ảnh trong cột này.

---

# 5. account_status

```text
account_status : VARCHAR(20)
```

`account_status` biểu diễn trạng thái hiện tại của tài khoản.

Diagram xác định các giá trị:

```text
UNVERIFIED
ACTIVE
DISABLED
DELETED
```

## 5.1. UNVERIFIED

```text
UNVERIFIED
```

Tài khoản chưa hoàn thành quá trình xác thực.

Trạng thái này được sử dụng trong quá trình đăng ký và xác thực OTP.

Luồng:

```text
Đăng ký
    ↓
UNVERIFIED
    ↓
Xác thực OTP
    ↓
ACTIVE
```

---

## 5.2. ACTIVE

```text
ACTIVE
```

Tài khoản đã được xác thực và đang hoạt động bình thường.

---

## 5.3. DISABLED

```text
DISABLED
```

Tài khoản đang bị vô hiệu hóa.

Tài khoản vẫn có thể tồn tại trong Database nhưng không còn hoạt động bình thường như tài khoản `ACTIVE`.

---

## 5.4. DELETED

```text
DELETED
```

Tài khoản được đánh dấu là đã xóa.

Theo thiết kế của diagram, việc xóa tài khoản được biểu diễn thông qua trạng thái:

```text
DELETED
```

thay vì nhất thiết phải xóa bản ghi khỏi Database.

Đây là cách tiếp cận thường được gọi là **soft delete**.

---

# 6. role

```text
role : VARCHAR(20)
```

`role` xác định vai trò của User trong hệ thống.

Diagram xác định ba giá trị:

```text
USER
ARTIST
ADMIN
```

## 6.1. USER

```text
USER
```

Là role mặc định khi User đăng ký tài khoản.

Diagram ghi rõ:

```text
Mặc định USER khi đăng ký
```

---

## 6.2. ARTIST

```text
ARTIST
```

Được sử dụng khi User trở thành Artist.

Role này liên quan đến quá trình:

```text
User gửi yêu cầu
    ↓
PENDING
    ↓
Admin xử lý
    ↓
APPROVED
    ↓
Role = ARTIST
```

---

## 6.3. ADMIN

```text
ADMIN
```

Là role dành cho tài khoản có quyền quản trị hệ thống.

---

# 7. artist_request_status

```text
artist_request_status : VARCHAR(20)
```

Thuộc tính này lưu trạng thái yêu cầu trở thành Artist của User.

Diagram xác định các giá trị:

```text
NONE
PENDING
APPROVED
REJECTED
```

---

## 7.1. NONE

```text
NONE
```

User chưa gửi yêu cầu trở thành Artist.

Đây là trạng thái mặc định theo diagram.

---

## 7.2. PENDING

```text
PENDING
```

User đã gửi yêu cầu trở thành Artist và đang chờ Admin xử lý.

Luồng:

```text
NONE
    ↓
User gửi yêu cầu
    ↓
PENDING
```

---

## 7.3. APPROVED

```text
APPROVED
```

Yêu cầu trở thành Artist đã được Admin chấp thuận.

Theo diagram:

```text
PENDING
    ↓
Admin duyệt
    ↓
APPROVED
```

Khi yêu cầu được duyệt, User có thể chuyển sang:

```text
role = ARTIST
```

---

## 7.4. REJECTED

```text
REJECTED
```

Yêu cầu trở thành Artist đã bị Admin từ chối.

Luồng:

```text
PENDING
    ↓
Admin từ chối
    ↓
REJECTED
```

---

# 8. artist_name

```text
artist_name : VARCHAR(100)
```

Lưu nghệ danh của User khi User hoạt động với vai trò Artist.

Diagram ghi chú rằng:

```text
artist_name, bio
chỉ có giá trị khi role = ARTIST
```

Do đó hai thuộc tính:

```text
artist_name
bio
```

phục vụ cho thông tin hồ sơ Artist.

---

# 9. bio

```text
bio : VARCHAR(255)
```

Lưu phần giới thiệu của Artist.

Thuộc tính này liên quan đến hồ sơ Artist và theo diagram chỉ có giá trị khi:

```text
role = ARTIST
```

---

# 10. Bảng otp_verifications

## 10.1. Mục đích

Bảng `otp_verifications` lưu các mã OTP được tạo trong quá trình xác thực.

OTP có thể phục vụ các mục đích:

```text
REGISTER
RESET_PASSWORD
CHANGE_PASSWORD
CHANGE_EMAIL
```

Mỗi bản ghi OTP thuộc về một User cụ thể.

---

# 11. Cấu trúc bảng otp_verifications

| Column       | Data Type    | Constraint | Ý nghĩa                       |
| ------------ | ------------ | ---------- | ----------------------------- |
| `id`         | BIGINT       | PK         | ID của OTP                    |
| `user_id`    | BIGINT       | FK         | User sở hữu OTP               |
| `otp_code`   | VARCHAR(10)  | NOT NULL   | Mã OTP                        |
| `purpose`    | VARCHAR(20)  | NOT NULL   | Mục đích của OTP              |
| `new_email`  | VARCHAR(100) |            | Email mới                     |
| `expired_at` | TIMESTAMP    | NOT NULL   | Thời điểm OTP hết hạn         |
| `verified`   | BOOLEAN      | NOT NULL   | OTP đã được xác thực hay chưa |

---

# 12. Chi tiết bảng otp_verifications

## 12.1. id

```text
id : BIGINT
```

Là định danh của bản ghi OTP.

Đây là:

```text
PRIMARY KEY
```

Mỗi bản ghi OTP có một `id` riêng.

---

## 12.2. user_id

```text
user_id : BIGINT
```

Lưu ID của User sở hữu OTP.

Đây là:

```text
FOREIGN KEY
```

`user_id` tham chiếu đến:

```text
users.id
```

Quan hệ:

```text
users.id
      ↑
      |
otp_verifications.user_id
```

---

## 12.3. otp_code

```text
otp_code : VARCHAR(10)
```

Lưu mã OTP được tạo cho User.

Ví dụ về mặt ý nghĩa:

```text
123456
```

OTP được sử dụng để xác thực các thao tác yêu cầu xác minh.

---

## 12.4. purpose

```text
purpose : VARCHAR(20)
```

Xác định OTP được tạo ra để phục vụ chức năng nào.

Các giá trị được thiết kế:

```text
REGISTER
RESET_PASSWORD
CHANGE_PASSWORD
CHANGE_EMAIL
```

### REGISTER

```text
REGISTER
```

OTP dùng để xác thực tài khoản khi đăng ký.

### RESET_PASSWORD

```text
RESET_PASSWORD
```

OTP dùng trong quá trình đặt lại mật khẩu.

### CHANGE_PASSWORD

```text
CHANGE_PASSWORD
```

OTP dùng trong quá trình thay đổi mật khẩu.

### CHANGE_EMAIL

```text
CHANGE_EMAIL
```

OTP dùng trong quá trình thay đổi email.

---

# 13. new_email

```text
new_email : VARCHAR(100)
```

Lưu email mới trong trường hợp User thực hiện thay đổi email.

Thuộc tính này đặc biệt liên quan đến:

```text
purpose = CHANGE_EMAIL
```

Luồng dữ liệu:

```text
Email hiện tại
    ↓
User nhập email mới
    ↓
new_email
    ↓
Tạo OTP
    ↓
Xác thực OTP
    ↓
Cập nhật email
```

Đối với các mục đích OTP khác, `new_email` không cần sử dụng.

---

# 14. expired_at

```text
expired_at : TIMESTAMP
```

Lưu thời điểm OTP hết hạn.

Mục đích là xác định OTP còn hợp lệ hay không.

Khi kiểm tra OTP:

```text
Thời gian hiện tại < expired_at
```

thì OTP vẫn còn trong thời gian hiệu lực.

Ngược lại:

```text
Thời gian hiện tại >= expired_at
```

thì OTP đã hết hạn.

---

# 15. verified

```text
verified : BOOLEAN
```

Cho biết OTP đã được xác thực hay chưa.

Có hai trạng thái:

```text
true
false
```

Ý nghĩa:

```text
false → OTP chưa được xác thực

true → OTP đã được xác thực
```

Thuộc tính này giúp hệ thống phân biệt OTP đã sử dụng và OTP chưa được sử dụng.

---

# 16. Quan hệ giữa users và otp_verifications

## 16.1. Cardinality

Diagram thể hiện quan hệ:

```text
users
  1
  |
  |
  *
otp_verifications
```

Đây là quan hệ:

```text
1 - N
```

hay:

```text
One-to-Many
```

---

## 16.2. Ý nghĩa

Một User có thể có nhiều bản ghi OTP.

Ví dụ về mặt dữ liệu:

```text
User
id = 1
```

có thể có:

```text
OTP 1
OTP 2
OTP 3
OTP 4
```

Tất cả các OTP này đều có:

```text
user_id = 1
```

---

## 16.3. Chiều ngược lại

Mỗi OTP chỉ thuộc về một User.

Ví dụ:

```text
otp_verifications.id = 10
otp_verifications.user_id = 1
```

có nghĩa:

```text
OTP 10 → thuộc User 1
```

Không thể để một OTP thuộc đồng thời nhiều User.

---

# 17. Foreign Key

Foreign Key nằm trong bảng:

```text
otp_verifications
```

Cụ thể:

```text
user_id : BIGINT
```

tham chiếu:

```text
users.id
```

Quan hệ:

```text
users
┌──────────────┐
│ id (PK)      │
└──────┬───────┘
       │
       │ 1
       │
       │ N
┌──────▼─────────────────┐
│ otp_verifications      │
│                        │
│ id (PK)                │
│ user_id (FK)           │
│ otp_code               │
│ purpose                │
│ new_email              │
│ expired_at             │
│ verified               │
└────────────────────────┘
```

Foreign Key giúp Database xác định OTP thuộc về User nào.

---

# 18. Primary Key

Database có hai Primary Key:

```text
users.id
otp_verifications.id
```

Trong đó:

```text
users.id
    ↓
Primary Key của users

otp_verifications.id
    ↓
Primary Key của otp_verifications
```

Primary Key dùng để định danh duy nhất mỗi record trong bảng.

---

# 19. Unique Constraint

Diagram xác định hai thuộc tính có `UNIQUE`:

```text
users.username
users.email
```

Điều này đảm bảo:

```text
username không trùng
email không trùng
```

Có thể hình dung:

```text
users
│
├── id        → PK
├── username  → UNIQUE
└── email     → UNIQUE
```

---

# 20. Các trạng thái của User

Bảng `users` chứa hai nhóm trạng thái quan trọng:

```text
account_status
artist_request_status
```

Hai thuộc tính này có mục đích khác nhau.

## account_status

Theo dõi trạng thái tài khoản:

```text
UNVERIFIED
ACTIVE
DISABLED
DELETED
```

## artist_request_status

Theo dõi trạng thái yêu cầu trở thành Artist:

```text
NONE
PENDING
APPROVED
REJECTED
```

Không nên nhầm hai thuộc tính này với nhau.

Ví dụ:

```text
account_status = ACTIVE
artist_request_status = PENDING
```

có nghĩa là:

```text
Tài khoản đang hoạt động
nhưng yêu cầu trở thành Artist đang chờ Admin xử lý
```

---

# 21. Quan hệ giữa Role và Artist

Bảng `users` có:

```text
role
artist_request_status
artist_name
bio
```

Các thuộc tính này phối hợp để quản lý quá trình trở thành Artist.

Luồng tổng quát:

```text
User đăng ký
    ↓
role = USER
    ↓
artist_request_status = NONE
```

Khi User gửi yêu cầu:

```text
artist_request_status = PENDING
```

Admin xử lý:

```text
             PENDING
              /   \
             /     \
            ↓       ↓
       APPROVED   REJECTED
            |
            ↓
      role = ARTIST
```

Khi User trở thành Artist:

```text
role = ARTIST
artist_name
bio
```

được sử dụng cho thông tin Artist.

---

# 22. Quan hệ giữa OTP và User

OTP không tồn tại độc lập với User.

Cấu trúc:

```text
User
  |
  | has
  |
  └──────< OTP
```

Một User có thể phát sinh nhiều OTP vì User có thể thực hiện nhiều thao tác cần xác thực.

Ví dụ:

```text
User
 │
 ├── OTP REGISTER
 │
 ├── OTP RESET_PASSWORD
 │
 ├── OTP CHANGE_PASSWORD
 │
 └── OTP CHANGE_EMAIL
```

Các bản ghi OTP được phân biệt bằng:

```text
purpose
```

và:

```text
id
```

---

# 23. Luồng dữ liệu đăng ký

Khi User đăng ký:

```text
User
    ↓
Tạo record trong users
    ↓
account_status = UNVERIFIED
    ↓
role = USER
    ↓
artist_request_status = NONE
```

Sau đó hệ thống tạo OTP:

```text
users
    ↓
otp_verifications
    ↓
purpose = REGISTER
```

OTP được liên kết với User thông qua:

```text
otp_verifications.user_id
```

Sau khi xác thực thành công:

```text
account_status
    ↓
ACTIVE
```

---

# 24. Luồng quên mật khẩu

Khi User quên mật khẩu:

```text
User
    ↓
Email
    ↓
Tạo OTP
```

OTP được lưu vào:

```text
otp_verifications
```

với:

```text
purpose = RESET_PASSWORD
```

Cấu trúc dữ liệu:

```text
user_id
otp_code
purpose = RESET_PASSWORD
expired_at
verified
```

Sau khi OTP được xác thực, User có thể thực hiện quá trình đặt lại mật khẩu.

---

# 25. Luồng đổi mật khẩu

Khi User muốn đổi mật khẩu:

```text
User
    ↓
Yêu cầu đổi mật khẩu
    ↓
Tạo OTP
```

OTP được lưu với:

```text
purpose = CHANGE_PASSWORD
```

Sau khi OTP hợp lệ:

```text
OTP
    ↓
Xác thực
    ↓
Đổi mật khẩu
```

---

# 26. Luồng thay đổi Email

Khi User muốn thay đổi email:

```text
User
    ↓
Nhập email mới
    ↓
new_email
    ↓
Tạo OTP
```

Bản ghi OTP có:

```text
purpose = CHANGE_EMAIL
new_email = email mới
```

Sau khi OTP được xác thực:

```text
OTP hợp lệ
    ↓
Lấy new_email
    ↓
Cập nhật users.email
```

---

# 27. Tổng quan dữ liệu

Có thể hình dung toàn bộ Module như sau:

```text
┌──────────────────────────────────────┐
│                users                 │
├──────────────────────────────────────┤
│ id               BIGINT       PK     │
│ username         VARCHAR(50)  UNIQUE │
│ email            VARCHAR(100) UNIQUE │
│ password         VARCHAR(255)        │
│ full_name        VARCHAR(100)        │
│ avatar_url       VARCHAR(255)        │
│ account_status   VARCHAR(20)         │
│ role             VARCHAR(20)         │
│ artist_request_status VARCHAR(20)    │
│ artist_name      VARCHAR(100)        │
│ bio              VARCHAR(255)        │
└──────────────────┬───────────────────┘
                   │
                   │ 1
                   │
                   │ N
┌──────────────────▼───────────────────┐
│          otp_verifications            │
├───────────────────────────────────────┤
│ id               BIGINT       PK      │
│ user_id          BIGINT       FK      │
│ otp_code         VARCHAR(10)          │
│ purpose          VARCHAR(20)          │
│ new_email        VARCHAR(100)         │
│ expired_at       TIMESTAMP            │
│ verified         BOOLEAN              │
└───────────────────────────────────────┘
```

---

# 28. Tổng hợp các bảng

| Bảng                | Mục đích                     |
| ------------------- | ---------------------------- |
| `users`             | Lưu thông tin tài khoản User |
| `otp_verifications` | Lưu các OTP của User         |

---

# 29. Tổng hợp Primary Key và Foreign Key

| Bảng                | Column    | Loại | Tham chiếu |
| ------------------- | --------- | ---- | ---------- |
| `users`             | `id`      | PK   | —          |
| `otp_verifications` | `id`      | PK   | —          |
| `otp_verifications` | `user_id` | FK   | `users.id` |

---

# 30. Tổng hợp Unique

```text
users.username
users.email
```

Cả hai đều có:

```text
UNIQUE
```

---

# 31. Tổng hợp các giá trị trạng thái

## account_status

```text
UNVERIFIED
ACTIVE
DISABLED
DELETED
```

## role

```text
USER
ARTIST
ADMIN
```

## artist_request_status

```text
NONE
PENDING
APPROVED
REJECTED
```

## purpose

```text
REGISTER
RESET_PASSWORD
CHANGE_PASSWORD
CHANGE_EMAIL
```

---

# 32. Tổng kết ERD

Module Người Dùng hiện tại được xây dựng dựa trên hai bảng:

```text
users
otp_verifications
```

Bảng `users` là bảng trung tâm, lưu thông tin tài khoản và các trạng thái liên quan đến User.

Bảng `otp_verifications` lưu các OTP được tạo cho User.

Quan hệ giữa hai bảng là:

```text
users 1 ───── N otp_verifications
```

Trong đó:

```text
users.id
    ↑
    |
    | FK
    |
otp_verifications.user_id
```

Bảng `users` quản lý:

```text
Thông tin tài khoản
Thông tin cá nhân
Trạng thái tài khoản
Role
Thông tin Artist
```

Bảng `otp_verifications` quản lý:

```text
OTP
Mục đích OTP
Email mới
Thời gian hết hạn
Trạng thái xác thực
```

Các trạng thái chính của tài khoản:

```text
UNVERIFIED
ACTIVE
DISABLED
DELETED
```

Các Role:

```text
USER
ARTIST
ADMIN
```

Các trạng thái yêu cầu Artist:

```text
NONE
PENDING
APPROVED
REJECTED
```

Các mục đích OTP:

```text
REGISTER
RESET_PASSWORD
CHANGE_PASSWORD
CHANGE_EMAIL
```

Như vậy, ERD này cung cấp cấu trúc dữ liệu nền tảng cho Module Người Dùng và là cơ sở để triển khai Entity, Repository và các tầng xử lý trong Spring Boot.
