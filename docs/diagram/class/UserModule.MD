# Class Diagram – Module Người Dùng

## 1. Tổng quan

Class Diagram của **Module Người Dùng** mô tả cấu trúc các class và cách các thành phần trong module phối hợp với nhau.

Module được chia thành các package chính:

```text
entity
dto
repository
security
service
controller
```

Ngoài ra, package `entity` chứa các `enum` được sử dụng để biểu diễn trạng thái tài khoản, vai trò, mục đích OTP và trạng thái yêu cầu trở thành Artist.

Kiến trúc tổng thể:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Entity
    ↓
Database
```

Đối với xác thực và bảo mật:

```text
Request
   ↓
JwtAuthenticationFilter
   ↓
JwtUtil
   ↓
CustomUserDetailsService
   ↓
SecurityConfig
```

---

# 2. Package Entity

Package `entity` chứa các class đại diện cho dữ liệu chính của hệ thống.

Trong diagram có 2 Entity:

* `User`
* `OtpVerification`

Ngoài ra còn có các enum:

* `AccountStatus`
* `OtpPurpose`
* `Role`
* `ArtistRequestStatus`

---

# 3. Entity User

## 3.1. Mục đích

`User` là Entity chính đại diện cho người dùng trong hệ thống.

User lưu các thông tin cơ bản của tài khoản, thông tin cá nhân và thông tin liên quan đến quyền Artist.

## 3.2. Các thuộc tính

| Thuộc tính            | Kiểu dữ liệu        | Ý nghĩa                             |
| --------------------- | ------------------- | ----------------------------------- |
| `id`                  | Long                | ID của người dùng                   |
| `username`            | String              | Tên đăng nhập                       |
| `email`               | String              | Email của người dùng                |
| `password`            | String              | Mật khẩu                            |
| `fullName`            | String              | Họ và tên                           |
| `avatarUrl`           | String              | Đường dẫn ảnh đại diện              |
| `accountStatus`       | AccountStatus       | Trạng thái tài khoản                |
| `role`                | Role                | Vai trò của người dùng              |
| `artistRequestStatus` | ArtistRequestStatus | Trạng thái yêu cầu trở thành Artist |
| `artistName`          | String              | Nghệ danh                           |
| `bio`                 | String              | Thông tin giới thiệu Artist         |

## 3.3. Ý nghĩa các nhóm thuộc tính

### Thông tin tài khoản

```text
id
username
email
password
```

Dùng để xác định và xác thực tài khoản người dùng.

### Thông tin cá nhân

```text
fullName
avatarUrl
```

Dùng cho chức năng xem và cập nhật thông tin cá nhân.

### Trạng thái tài khoản

```text
accountStatus
```

Dùng để xác định tài khoản đang ở trạng thái nào.

### Phân quyền

```text
role
```

Dùng để xác định người dùng là:

```text
USER
ARTIST
ADMIN
```

### Thông tin Artist

```text
artistRequestStatus
artistName
bio
```

Được sử dụng trong quá trình User gửi yêu cầu trở thành Artist và sau đó cập nhật hồ sơ Artist.

---

# 4. Entity OtpVerification

## 4.1. Mục đích

`OtpVerification` dùng để lưu thông tin OTP được sử dụng trong các chức năng xác thực của hệ thống.

Các chức năng sử dụng OTP gồm:

* Đăng ký
* Quên mật khẩu
* Đổi mật khẩu
* Cập nhật email

## 4.2. Các thuộc tính

| Thuộc tính  | Kiểu dữ liệu  | Ý nghĩa                                   |
| ----------- | ------------- | ----------------------------------------- |
| `id`        | Long          | ID của bản ghi OTP                        |
| `user`      | User          | Người dùng sở hữu OTP                     |
| `otpCode`   | String        | Mã OTP                                    |
| `purpose`   | OtpPurpose    | Mục đích sử dụng OTP                      |
| `newEmail`  | String        | Email mới trong trường hợp cập nhật email |
| `expiredAt` | LocalDateTime | Thời gian hết hạn OTP                     |
| `verified`  | boolean       | Xác định OTP đã được xác thực hay chưa    |

## 4.3. Quan hệ với User

`OtpVerification` chứa thuộc tính:

```text
user: User
```

Điều này thể hiện một OTP thuộc về một User.

Có thể hiểu đơn giản:

```text
User
  │
  │ 1
  │
  │
  │ *
OtpVerification
```

Một User có thể có nhiều bản ghi OTP trong quá trình sử dụng hệ thống.

---

# 5. Enum AccountStatus

`AccountStatus` biểu diễn trạng thái của tài khoản User.

Các giá trị:

```text
UNVERIFIED
ACTIVE
DISABLED
DELETED
```

## 5.1. UNVERIFIED

Tài khoản chưa hoàn thành xác thực OTP.

Liên quan đến:

```text
UC01 – Đăng ký
UC02 – Xác thực Email / OTP
```

## 5.2. ACTIVE

Tài khoản đang hoạt động bình thường.

## 5.3. DISABLED

Tài khoản đã bị vô hiệu hóa.

Liên quan đến:

```text
UC11 – Vô hiệu hóa tài khoản
```

## 5.4. DELETED

Tài khoản đã được đánh dấu xóa.

Trong diagram ghi rõ:

```text
DELETED → UC12 Xóa (soft delete)
```

Tức là tài khoản được đánh dấu trạng thái `DELETED` thay vì nhất thiết phải xóa vật lý khỏi Database.

---

# 6. Enum OtpPurpose

`OtpPurpose` xác định OTP đang được sử dụng cho chức năng nào.

Các giá trị:

```text
REGISTER
RESET_PASSWORD
CHANGE_PASSWORD
CHANGE_EMAIL
```

| Giá trị           | Mục đích                    |
| ----------------- | --------------------------- |
| `REGISTER`        | Xác thực khi đăng ký        |
| `RESET_PASSWORD`  | Xác thực khi quên mật khẩu  |
| `CHANGE_PASSWORD` | Xác thực khi đổi mật khẩu   |
| `CHANGE_EMAIL`    | Xác thực khi cập nhật email |

---

# 7. Enum Role

`Role` biểu diễn quyền của User trong hệ thống.

Các giá trị:

```text
USER
ARTIST
ADMIN
```

## USER

Đây là role mặc định khi người dùng đăng ký.

```text
USER → mặc định khi đăng ký
```

## ARTIST

Được gán khi Admin duyệt yêu cầu trở thành Artist.

```text
ARTIST → được gán khi UC17 duyệt
```

## ADMIN

Role Admin được gán thủ công.

```text
ADMIN → gán thủ công qua UC18
```

---

# 8. Enum ArtistRequestStatus

`ArtistRequestStatus` biểu diễn trạng thái yêu cầu trở thành Artist.

Các giá trị:

```text
NONE
PENDING
APPROVED
REJECTED
```

| Trạng thái | Ý nghĩa                          |
| ---------- | -------------------------------- |
| `NONE`     | Chưa gửi yêu cầu                 |
| `PENDING`  | Đã gửi yêu cầu và đang chờ duyệt |
| `APPROVED` | Admin đã duyệt                   |
| `REJECTED` | Admin đã từ chối                 |

Chi tiết theo diagram:

```text
NONE
→ chưa gửi yêu cầu

PENDING
→ UC14 vừa gửi, chờ duyệt

APPROVED
→ UC17 Admin duyệt

REJECTED
→ UC17 Admin từ chối
```

---

# 9. Package DTO

Package `dto` chứa các class dùng để trao đổi dữ liệu giữa Client và Backend.

Diagram hiện thể hiện:

```text
LoginResponse
```

## 9.1. LoginResponse

`LoginResponse` là DTO được trả về sau khi User đăng nhập thành công.

Các thuộc tính:

| Thuộc tính | Kiểu dữ liệu | Ý nghĩa       |
| ---------- | ------------ | ------------- |
| `token`    | String       | JWT Token     |
| `userId`   | Long         | ID User       |
| `username` | String       | Username      |
| `role`     | Role         | Role của User |

Có thể hình dung response:

```text
Đăng nhập
    ↓
AuthService
    ↓
Tạo JWT
    ↓
LoginResponse
    ↓
Client
```

Client sử dụng `token` để thực hiện các request cần xác thực.

---

# 10. Package Repository

Repository là tầng giao tiếp trực tiếp với Database thông qua Entity.

Diagram có 2 Repository:

```text
UserRepository
OtpVerificationRepository
```

---

# 11. UserRepository

`UserRepository` dùng để thao tác với dữ liệu `User`.

## Các phương thức

### findByEmail(email)

```text
findByEmail(email): Optional<User>
```

Dùng để tìm User dựa trên email.

Có thể sử dụng trong:

```text
Đăng nhập
JWT / Spring Security
Các chức năng cần tìm User bằng email
```

### existsByEmail(email)

```text
existsByEmail(email): boolean
```

Kiểm tra email đã tồn tại trong Database hay chưa.

### existsByUsername(username)

```text
existsByUsername(username): boolean
```

Kiểm tra username đã tồn tại hay chưa.

### findByArtistRequestStatus(status)

```text
findByArtistRequestStatus(status): List<User>
```

Tìm danh sách User dựa trên trạng thái yêu cầu trở thành Artist.

Ví dụ:

```text
PENDING
```

được sử dụng để lấy danh sách các User đang chờ Admin duyệt.

---

# 12. OtpVerificationRepository

`OtpVerificationRepository` dùng để thao tác với dữ liệu OTP.

Diagram thể hiện phương thức:

```text
findFirstByUserAndPurposeAndVerifiedFalseOrderByIdDesc(
    user,
    purpose
): Optional<OtpVerification>
```

Ý nghĩa:

* Tìm OTP của một User.
* OTP phải đúng `purpose`.
* OTP chưa được xác thực.
* Lấy bản ghi mới nhất dựa trên `id`.

Có thể hiểu đơn giản:

```text
User
  ↓
Purpose
  ↓
OTP chưa xác thực
  ↓
Lấy OTP mới nhất
```

---

# 13. Package Security

Package `security` chịu trách nhiệm cho Authentication và Authorization.

Diagram có:

```text
JwtUtil
JwtAuthenticationFilter
CustomUserDetailsService
SecurityConfig
```

---

# 14. JwtUtil

`JwtUtil` chịu trách nhiệm xử lý JWT.

Các phương thức:

```text
generateToken(user): String
validateToken(token): boolean
getEmailFromToken(token): String
```

## generateToken(user)

Tạo JWT Token dựa trên thông tin User.

Luồng:

```text
User đăng nhập thành công
        ↓
AuthService
        ↓
JwtUtil
        ↓
generateToken(user)
        ↓
JWT Token
```

## validateToken(token)

Kiểm tra JWT Token có hợp lệ hay không.

Kết quả:

```text
true
```

hoặc

```text
false
```

## getEmailFromToken(token)

Lấy email được lưu trong JWT Token.

Email sau đó có thể được sử dụng để tìm User.

---

# 15. JwtAuthenticationFilter

`JwtAuthenticationFilter` chịu trách nhiệm xử lý JWT trong các HTTP Request.

Phương thức:

```text
doFilterInternal(req, res, chain): void
```

Có thể hiểu luồng tổng quát:

```text
Client gửi Request
        ↓
JwtAuthenticationFilter
        ↓
Đọc JWT
        ↓
Kiểm tra JWT
        ↓
Xác định User
        ↓
Cho Request tiếp tục
```

---

# 16. CustomUserDetailsService

`CustomUserDetailsService` kết nối User trong Database với Spring Security.

Phương thức:

```text
loadUserByUsername(email): UserDetails
```

Trong thiết kế này, `email` được sử dụng để tìm User.

Luồng:

```text
Email
 ↓
CustomUserDetailsService
 ↓
UserRepository
 ↓
User
 ↓
UserDetails
```

---

# 17. SecurityConfig

`SecurityConfig` là class cấu hình Spring Security.

Các phương thức:

```text
filterChain(http): SecurityFilterChain
passwordEncoder(): PasswordEncoder
```

## filterChain(http)

Cấu hình Security Filter Chain của hệ thống.

Đây là nơi cấu hình cách Spring Security xử lý request.

## passwordEncoder()

Cung cấp cơ chế mã hóa mật khẩu.

Mục đích là không lưu mật khẩu dạng plain text trong Database.

---

# 18. Package Service

Service chứa Business Logic của hệ thống.

Diagram có:

```text
AuthService
OtpService
EmailService
UserService
AdminService
```

---

# 19. AuthService

`AuthService` xử lý các chức năng Authentication.

Các phương thức:

```text
register(username, email, password): void

login(email, password): LoginResponse

forgotPassword(email): void

resetPassword(email, otp, newPassword): void
```

## register()

Xử lý đăng ký tài khoản.

Luồng tổng quát:

```text
Register Request
       ↓
AuthController
       ↓
AuthService.register()
       ↓
Tạo User
       ↓
OTP
       ↓
Xác thực
```

## login()

Xử lý đăng nhập.

```text
Email + Password
       ↓
AuthService
       ↓
Kiểm tra User
       ↓
JWT
       ↓
LoginResponse
```

## forgotPassword()

Xử lý yêu cầu quên mật khẩu.

```text
Email
 ↓
AuthService
 ↓
Gửi OTP
```

## resetPassword()

Xử lý đặt lại mật khẩu sau khi xác thực OTP.

```text
Email
 + OTP
 + New Password
       ↓
AuthService
       ↓
Reset Password
```

---

# 20. OtpService

`OtpService` chịu trách nhiệm tạo và xác thực OTP.

Các phương thức:

```text
generateAndSend(user, purpose): void

generateAndSend(user, purpose, newEmail): void

verifyAndConsume(user, otp, purpose): OtpVerification
```

## generateAndSend()

Tạo OTP và gửi OTP cho User.

Có 2 trường hợp:

```text
generateAndSend(user, purpose)
```

và:

```text
generateAndSend(user, purpose, newEmail)
```

Trường hợp có `newEmail` phục vụ cho chức năng cập nhật email.

## verifyAndConsume()

Xác thực OTP.

```text
User
 + OTP
 + Purpose
       ↓
OtpService
       ↓
Kiểm tra OTP
       ↓
OtpVerification
```

---

# 21. EmailService

`EmailService` chịu trách nhiệm gửi OTP qua email.

Phương thức:

```text
sendOtp(toEmail, otpCode): void
```

Luồng:

```text
OtpService
    ↓
EmailService
    ↓
Email người dùng
    ↓
OTP
```

---

# 22. UserService

`UserService` xử lý các chức năng của User đã đăng nhập.

Các phương thức:

```text
getProfile(userId): User

updateProfile(userId, fullName): User

requestChangePasswordOtp(userId, currentPassword): void

changePassword(userId, otp, newPassword): void

updateAvatar(userId, file): User

requestUpdateEmailOtp(userId, newEmail): void

updateEmail(userId, otp): void

disableAccount(userId, currentPassword): void

deleteAccount(userId, currentPassword): void

requestBecomeArtist(userId, artistName, bio): void

updateArtistProfile(userId, artistName, bio): User
```

## getProfile()

Lấy thông tin cá nhân của User.

```text
User ID
 ↓
UserService
 ↓
User
```

## updateProfile()

Cập nhật thông tin cá nhân.

Trong diagram tham số cập nhật là:

```text
fullName
```

## requestChangePasswordOtp()

Yêu cầu OTP để đổi mật khẩu.

Input:

```text
userId
currentPassword
```

Luồng:

```text
Current Password
       ↓
Kiểm tra
       ↓
Gửi OTP
```

## changePassword()

Đổi mật khẩu sau khi OTP được xác thực.

Input:

```text
userId
otp
newPassword
```

## updateAvatar()

Cập nhật ảnh đại diện.

Input:

```text
userId
file
```

Kết quả trả về:

```text
User
```

## requestUpdateEmailOtp()

Yêu cầu OTP để cập nhật email.

Input:

```text
userId
newEmail
```

## updateEmail()

Cập nhật email sau khi OTP được xác thực.

Input:

```text
userId
otp
```

## disableAccount()

Vô hiệu hóa tài khoản.

Yêu cầu:

```text
userId
currentPassword
```

## deleteAccount()

Xóa tài khoản.

Yêu cầu:

```text
userId
currentPassword
```

Theo enum `AccountStatus`, tài khoản sau khi xóa có trạng thái:

```text
DELETED
```

## requestBecomeArtist()

Gửi yêu cầu trở thành Artist.

Input:

```text
userId
artistName
bio
```

Sau khi gửi:

```text
ArtistRequestStatus = PENDING
```

## updateArtistProfile()

Cập nhật hồ sơ Artist.

Input:

```text
userId
artistName
bio
```

Kết quả trả về:

```text
User
```

---

# 23. AdminService

`AdminService` xử lý các chức năng dành cho Admin.

Các phương thức:

```text
getAllUsers(): List<User>

lockAccount(userId): void

unlockAccount(userId): void

getPendingArtistRequests(): List<User>

approveArtistRequest(userId): void

rejectArtistRequest(userId): void

assignRole(userId, role): void
```

## getAllUsers()

Lấy danh sách tất cả User.

## lockAccount()

Khóa tài khoản User.

Input:

```text
userId
```

## unlockAccount()

Mở khóa tài khoản User.

Input:

```text
userId
```

## getPendingArtistRequests()

Lấy danh sách các yêu cầu trở thành Artist đang chờ xử lý.

Có thể hiểu:

```text
ArtistRequestStatus = PENDING
```

## approveArtistRequest()

Duyệt yêu cầu trở thành Artist.

Sau khi Admin duyệt:

```text
ArtistRequestStatus = APPROVED
```

và theo diagram:

```text
Role = ARTIST
```

## rejectArtistRequest()

Từ chối yêu cầu trở thành Artist.

Theo enum:

```text
ArtistRequestStatus = REJECTED
```

## assignRole()

Gán role cho User.

Input:

```text
userId
role
```

Các role trong diagram:

```text
USER
ARTIST
ADMIN
```

---

# 24. Package Controller

Controller là tầng tiếp nhận HTTP Request từ Client.

Diagram có:

```text
AuthController
UserController
AdminController
```

---

# 25. AuthController

`AuthController` xử lý các API liên quan đến Authentication.

Các phương thức:

```text
register()

verifyOtp()

login()

forgotPassword()

resetPassword()
```

Chức năng:

| Method             | Chức năng        |
| ------------------ | ---------------- |
| `register()`       | Đăng ký          |
| `verifyOtp()`      | Xác thực OTP     |
| `login()`          | Đăng nhập        |
| `forgotPassword()` | Quên mật khẩu    |
| `resetPassword()`  | Đặt lại mật khẩu |

Luồng:

```text
Client
  ↓
AuthController
  ↓
AuthService
  ↓
Repository / Security / OTP
  ↓
Database
```

---

# 26. UserController

`UserController` xử lý API dành cho User.

Các phương thức:

```text
logout()

getProfile()

updateProfile()

updateAvatar()

requestChangePasswordOtp()

changePassword()

requestUpdateEmailOtp()

updateEmail()

disableAccount()

deleteAccount()

requestBecomeArtist()

updateArtistProfile()
```

Có thể chia thành các nhóm:

### Profile

```text
getProfile()
updateProfile()
updateAvatar()
```

### Password

```text
requestChangePasswordOtp()
changePassword()
```

### Email

```text
requestUpdateEmailOtp()
updateEmail()
```

### Account

```text
logout()
disableAccount()
deleteAccount()
```

### Artist

```text
requestBecomeArtist()
updateArtistProfile()
```

---

# 27. AdminController

`AdminController` xử lý API dành cho Admin.

Các phương thức:

```text
getAllUsers()

lockAccount()

unlockAccount()

getPendingArtistRequests()

approveArtistRequest()

rejectArtistRequest()

assignRole()
```

Tất cả API của `AdminController` trong diagram được ghi chú:

```text
Toàn bộ API của AdminController
gắn @PreAuthorize("hasRole('ADMIN')")
trong SecurityConfig
```

Điều này có nghĩa là các API của Admin được giới hạn cho User có role `ADMIN`.

Luồng:

```text
Client
   ↓
AdminController
   ↓
Kiểm tra quyền ADMIN
   ↓
AdminService
   ↓
UserRepository
   ↓
Database
```

---

# 28. Luồng tổng thể của Module

## 28.1. Authentication

```text
Client
  ↓
AuthController
  ↓
AuthService
  ↓
UserRepository
  ↓
User
```

Khi đăng nhập thành công:

```text
AuthService
    ↓
JwtUtil
    ↓
JWT
    ↓
LoginResponse
    ↓
Client
```

---

# 29. Luồng JWT Authentication

Đối với các API cần đăng nhập:

```text
Client
  ↓
Request + JWT
  ↓
JwtAuthenticationFilter
  ↓
JwtUtil
  ↓
Validate JWT
  ↓
CustomUserDetailsService
  ↓
UserRepository
  ↓
User
  ↓
Spring Security
  ↓
Controller
```

---

# 30. Luồng OTP

OTP được xử lý chủ yếu thông qua:

```text
OtpService
EmailService
OtpVerificationRepository
OtpVerification
```

Luồng tổng quát:

```text
Service
   ↓
OtpService
   ↓
Tạo OTP
   ↓
OtpVerification
   ↓
OtpVerificationRepository
   ↓
Database

OtpService
   ↓
EmailService
   ↓
Email
   ↓
User
```

Khi User nhập OTP:

```text
User
 ↓
Controller
 ↓
Service
 ↓
OtpService
 ↓
OtpVerificationRepository
 ↓
Kiểm tra OTP
```

---

# 31. Luồng User trở thành Artist

```text
User
 ↓
UserController
 ↓
UserService
 ↓
requestBecomeArtist()
 ↓
ArtistRequestStatus = PENDING
```

Sau đó Admin xử lý:

```text
Admin
 ↓
AdminController
 ↓
AdminService
 ↓
getPendingArtistRequests()
 ↓
Danh sách yêu cầu
```

Nếu Admin chấp nhận:

```text
approveArtistRequest()
        ↓
ArtistRequestStatus = APPROVED
        ↓
Role = ARTIST
```

Nếu Admin từ chối:

```text
rejectArtistRequest()
        ↓
ArtistRequestStatus = REJECTED
```

---

# 32. Quan hệ giữa các tầng

Có thể hiểu Class Diagram theo cấu trúc:

```text
                    ┌─────────────────┐
                    │    Controller   │
                    │                 │
                    │ AuthController  │
                    │ UserController  │
                    │ AdminController │
                    └────────┬────────┘
                             │
                             ↓
                    ┌─────────────────┐
                    │     Service     │
                    │                 │
                    │ AuthService     │
                    │ OtpService      │
                    │ EmailService    │
                    │ UserService     │
                    │ AdminService    │
                    └────────┬────────┘
                             │
                             ↓
                    ┌─────────────────┐
                    │   Repository    │
                    │                 │
                    │ UserRepository  │
                    │ OtpVerification │
                    │ Repository      │
                    └────────┬────────┘
                             │
                             ↓
                    ┌─────────────────┐
                    │     Entity      │
                    │                 │
                    │ User            │
                    │ OtpVerification │
                    └─────────────────┘
```

Security hoạt động xuyên suốt quá trình xác thực:

```text
             ┌──────────────────────┐
             │    SecurityConfig    │
             └──────────┬───────────┘
                        │
                        ↓
             ┌──────────────────────┐
             │ JwtAuthentication    │
             │ Filter               │
             └──────────┬───────────┘
                        │
                        ↓
             ┌──────────────────────┐
             │       JwtUtil        │
             └──────────┬───────────┘
                        │
                        ↓
             ┌──────────────────────┐
             │ CustomUserDetails    │
             │ Service              │
             └──────────┬───────────┘
                        │
                        ↓
                 UserRepository
```

---

# 33. Tổng hợp toàn bộ Class

| Package    | Class                       | Vai trò                       |
| ---------- | --------------------------- | ----------------------------- |
| Entity     | `User`                      | Thông tin người dùng          |
| Entity     | `OtpVerification`           | Lưu thông tin OTP             |
| Enum       | `AccountStatus`             | Trạng thái tài khoản          |
| Enum       | `OtpPurpose`                | Mục đích OTP                  |
| Enum       | `Role`                      | Vai trò User                  |
| Enum       | `ArtistRequestStatus`       | Trạng thái yêu cầu Artist     |
| DTO        | `LoginResponse`             | Response sau đăng nhập        |
| Repository | `UserRepository`            | Truy vấn User                 |
| Repository | `OtpVerificationRepository` | Truy vấn OTP                  |
| Security   | `JwtUtil`                   | Tạo và kiểm tra JWT           |
| Security   | `JwtAuthenticationFilter`   | Xử lý JWT trong Request       |
| Security   | `CustomUserDetailsService`  | Load User cho Spring Security |
| Security   | `SecurityConfig`            | Cấu hình Spring Security      |
| Service    | `AuthService`               | Authentication                |
| Service    | `OtpService`                | Xử lý OTP                     |
| Service    | `EmailService`              | Gửi OTP qua Email             |
| Service    | `UserService`               | Business logic của User       |
| Service    | `AdminService`              | Business logic của Admin      |
| Controller | `AuthController`            | API Authentication            |
| Controller | `UserController`            | API User                      |
| Controller | `AdminController`           | API Admin                     |

---

# 34. Tổng kết

Class Diagram của Module Người Dùng được tổ chức thành các tầng chính:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Entity
```

Trong đó:

* **Controller** tiếp nhận request từ Client.
* **Service** xử lý Business Logic.
* **Repository** làm việc với Database.
* **Entity** đại diện cho dữ liệu trong Database.
* **DTO** dùng để trao đổi dữ liệu giữa Client và Backend.
* **Security** chịu trách nhiệm Authentication và Authorization.
* **Enum** giúp biểu diễn các trạng thái và quyền cố định.

Module hiện bao phủ các nhóm chức năng chính:

```text
Authentication
    ↓
Đăng ký
Đăng nhập
OTP
Quên mật khẩu
Đặt lại mật khẩu

User
    ↓
Profile
Password
Email
Avatar
Account

Artist
    ↓
Gửi yêu cầu trở thành Artist
Duyệt / Từ chối yêu cầu
Cập nhật hồ sơ Artist

Admin
    ↓
Quản lý User
Khóa / Mở khóa
Duyệt Artist
Phân quyền
```

Đây là cấu trúc Class Diagram được sử dụng để làm cơ sở triển khai các class Java tương ứng trong Module Người Dùng.
