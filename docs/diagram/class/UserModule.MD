# Class Diagram – Module Người Dùng

## 1. Tổng quan

Class Diagram của **Module Người Dùng** mô tả cấu trúc các class, thuộc tính, phương thức và mối quan hệ giữa các thành phần trong module.

Module được tổ chức thành các package chính:

```text
entity
dto
repository
security
service
controller
```

Trong đó:

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

Ngoài kiến trúc chính, package `security` chịu trách nhiệm xử lý Authentication và Authorization:

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
    ↓
Controller
```

Module bao gồm các nhóm chức năng chính:

```text
Authentication
    ↓
Đăng ký
Xác thực OTP
Đăng nhập
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
Khóa / Mở khóa tài khoản
Duyệt Artist
Phân quyền
```

---

# 2. Package Entity

Package `entity` chứa các class đại diện cho dữ liệu được lưu trữ trong Database.

Diagram gồm hai Entity chính:

```text
User
OtpVerification
```

Ngoài ra package này chứa các enum:

```text
AccountStatus
OtpPurpose
Role
ArtistRequestStatus
```

---

# 3. Entity User

## 3.1. Mục đích

`User` là Entity chính đại diện cho tài khoản người dùng trong hệ thống.

Entity này lưu:

* Thông tin tài khoản
* Thông tin cá nhân
* Trạng thái tài khoản
* Role
* Thông tin liên quan đến Artist

## 3.2. Thuộc tính

| Thuộc tính            | Kiểu dữ liệu        | Ý nghĩa                             |
| --------------------- | ------------------- | ----------------------------------- |
| `id`                  | Long                | ID của User                         |
| `username`            | String              | Tên đăng nhập                       |
| `email`               | String              | Email của User                      |
| `password`            | String              | Mật khẩu                            |
| `fullName`            | String              | Họ và tên                           |
| `avatarUrl`           | String              | Đường dẫn ảnh đại diện              |
| `accountStatus`       | AccountStatus       | Trạng thái tài khoản                |
| `role`                | Role                | Vai trò của User                    |
| `artistRequestStatus` | ArtistRequestStatus | Trạng thái yêu cầu trở thành Artist |
| `artistName`          | String              | Nghệ danh                           |
| `bio`                 | String              | Thông tin giới thiệu Artist         |

## 3.3. Khai báo trong diagram

```text
User
- id: Long
- username: String
- email: String
- password: String
- fullName: String
- avatarUrl: String
- accountStatus: AccountStatus
- role: Role
- artistRequestStatus: ArtistRequestStatus
- artistName: String
- bio: String
```

## 3.4. Nhóm thuộc tính

### Thông tin tài khoản

```text
id
username
email
password
```

Dùng để xác định và xác thực tài khoản.

### Thông tin cá nhân

```text
fullName
avatarUrl
```

Dùng cho các chức năng liên quan đến Profile.

### Trạng thái tài khoản

```text
accountStatus
```

Dùng để xác định trạng thái hiện tại của tài khoản.

### Phân quyền

```text
role
```

Dùng để xác định quyền của User.

### Thông tin Artist

```text
artistRequestStatus
artistName
bio
```

Dùng trong quá trình User gửi yêu cầu trở thành Artist và cập nhật hồ sơ Artist.

---

# 4. Entity OtpVerification

## 4.1. Mục đích

`OtpVerification` dùng để lưu thông tin OTP được sử dụng trong các chức năng xác thực.

Các mục đích OTP trong diagram:

```text
REGISTER
RESET_PASSWORD
CHANGE_PASSWORD
CHANGE_EMAIL
```

## 4.2. Thuộc tính

| Thuộc tính  | Kiểu dữ liệu  | Ý nghĩa                      |
| ----------- | ------------- | ---------------------------- |
| `id`        | Long          | ID của bản ghi OTP           |
| `user`      | User          | User sở hữu OTP              |
| `otpCode`   | String        | Mã OTP                       |
| `purpose`   | OtpPurpose    | Mục đích của OTP             |
| `newEmail`  | String        | Email mới khi cập nhật email |
| `expiredAt` | LocalDateTime | Thời gian hết hạn OTP        |
| `verified`  | boolean       | Trạng thái xác thực OTP      |

## 4.3. Khai báo trong diagram

```text
OtpVerification
- id: Long
- user: User
- otpCode: String
- purpose: OtpPurpose
- newEmail: String
- expiredAt: LocalDateTime
- verified: boolean
```

## 4.4. Quan hệ với User

`OtpVerification` có thuộc tính:

```text
user: User
```

Quan hệ trong diagram:

```text
User
  1
  |
  |
  *
OtpVerification
```

Điều này thể hiện:

```text
Một User
    ↓
Có thể có nhiều OtpVerification
```

Mỗi bản ghi `OtpVerification` thuộc về một `User`.

---

# 5. Enum AccountStatus

`AccountStatus` biểu diễn trạng thái của tài khoản.

## 5.1. Các giá trị

```text
UNVERIFIED
ACTIVE
DISABLED
DELETED
```

## 5.2. Ý nghĩa

### UNVERIFIED

```text
UNVERIFIED → chưa xác thực OTP
```

Được sử dụng khi tài khoản chưa hoàn thành quá trình xác thực.

Liên quan đến:

```text
UC01 – Đăng ký
UC02 – Xác thực Email / OTP
```

### ACTIVE

```text
ACTIVE → tài khoản đang hoạt động
```

### DISABLED

```text
DISABLED → UC11 Vô hiệu hóa
```

Tài khoản đã bị vô hiệu hóa.

### DELETED

```text
DELETED → UC12 Xóa (soft delete)
```

Tài khoản được đánh dấu là đã xóa thay vì nhất thiết phải xóa vật lý khỏi Database.

---

# 6. Enum OtpPurpose

`OtpPurpose` xác định mục đích của OTP.

## 6.1. Các giá trị

```text
REGISTER
RESET_PASSWORD
CHANGE_PASSWORD
CHANGE_EMAIL
```

## 6.2. Ý nghĩa

| Giá trị           | Mục đích                    |
| ----------------- | --------------------------- |
| `REGISTER`        | Xác thực khi đăng ký        |
| `RESET_PASSWORD`  | Xác thực khi quên mật khẩu  |
| `CHANGE_PASSWORD` | Xác thực khi đổi mật khẩu   |
| `CHANGE_EMAIL`    | Xác thực khi cập nhật email |

---

# 7. Enum Role

`Role` biểu diễn vai trò của User.

## 7.1. Các giá trị

```text
USER
ARTIST
ADMIN
```

## 7.2. USER

```text
USER → mặc định khi đăng ký
```

## 7.3. ARTIST

```text
ARTIST → được gán khi UC17 duyệt
```

## 7.4. ADMIN

```text
ADMIN → gán thủ công qua UC18
```

---

# 8. Enum ArtistRequestStatus

`ArtistRequestStatus` biểu diễn trạng thái yêu cầu trở thành Artist.

## 8.1. Các giá trị

```text
NONE
PENDING
APPROVED
REJECTED
```

## 8.2. Ý nghĩa

| Trạng thái | Ý nghĩa                          |
| ---------- | -------------------------------- |
| `NONE`     | Chưa gửi yêu cầu                 |
| `PENDING`  | Đã gửi yêu cầu và đang chờ duyệt |
| `APPROVED` | Admin đã duyệt                   |
| `REJECTED` | Admin đã từ chối                 |

## 8.3. Luồng trạng thái

```text
NONE
    ↓
User gửi yêu cầu
    ↓
PENDING
    ↓
Admin xử lý
    ↓
┌───────────┬───────────┐
↓           ↓
APPROVED    REJECTED
```

---

# 9. Package DTO

Package `dto` chứa các class dùng để trao đổi dữ liệu giữa Client và Backend.

Diagram hiện có:

```text
LoginResponse
```

---

# 10. LoginResponse

`LoginResponse` là DTO được trả về sau khi đăng nhập thành công.

## 10.1. Thuộc tính

| Thuộc tính | Kiểu dữ liệu | Ý nghĩa       |
| ---------- | ------------ | ------------- |
| `token`    | String       | JWT Token     |
| `userId`   | Long         | ID của User   |
| `username` | String       | Username      |
| `role`     | Role         | Role của User |

## 10.2. Khai báo trong diagram

```text
LoginResponse
- token: String
- userId: Long
- username: String
- role: Role
```

## 10.3. Luồng

```text
Client
    ↓
AuthController
    ↓
AuthService
    ↓
Kiểm tra thông tin đăng nhập
    ↓
JwtUtil
    ↓
JWT Token
    ↓
LoginResponse
    ↓
Client
```

Client sử dụng JWT Token cho các request yêu cầu Authentication.

---

# 11. Package Repository

Repository là tầng giao tiếp với Database thông qua Entity.

Diagram gồm:

```text
UserRepository
OtpVerificationRepository
```

---

# 12. UserRepository

`UserRepository` chịu trách nhiệm truy vấn dữ liệu `User`.

## 12.1. Các phương thức

```text
findByEmail(email): Optional<User>

existsByEmail(email): boolean

existsByUsername(username): boolean

findByArtistRequestStatus(status): List<User>
```

## 12.2. findByEmail()

```text
findByEmail(email): Optional<User>
```

Dùng để tìm User dựa trên email.

Có thể được sử dụng trong:

```text
Đăng nhập
JWT Authentication
Spring Security
Các chức năng cần tìm User bằng email
```

## 12.3. existsByEmail()

```text
existsByEmail(email): boolean
```

Kiểm tra email đã tồn tại trong Database hay chưa.

## 12.4. existsByUsername()

```text
existsByUsername(username): boolean
```

Kiểm tra username đã tồn tại hay chưa.

## 12.5. findByArtistRequestStatus()

```text
findByArtistRequestStatus(status): List<User>
```

Tìm danh sách User dựa trên trạng thái yêu cầu trở thành Artist.

Ví dụ:

```text
PENDING
```

được sử dụng để lấy các yêu cầu đang chờ Admin xử lý.

---

# 13. OtpVerificationRepository

`OtpVerificationRepository` chịu trách nhiệm truy vấn dữ liệu OTP.

## 13.1. Phương thức

```text
findFirstByUserAndPurposeAndVerifiedFalseOrderByIdDesc(
    user,
    purpose
): Optional<OtpVerification>
```

## 13.2. Ý nghĩa

Phương thức này:

```text
1. Tìm OTP của User
2. Kiểm tra đúng purpose
3. Chỉ lấy OTP chưa được xác thực
4. Lấy bản ghi mới nhất theo id
```

Có thể hình dung:

```text
User
    ↓
Purpose
    ↓
OTP chưa verified
    ↓
Lấy OTP mới nhất
```

---

# 14. Package Security

Package `security` chịu trách nhiệm cho Authentication và Authorization.

Diagram gồm:

```text
JwtUtil
JwtAuthenticationFilter
CustomUserDetailsService
SecurityConfig
```

---

# 15. JwtUtil

`JwtUtil` chịu trách nhiệm tạo và xử lý JWT.

## 15.1. Các phương thức

```text
generateToken(user): String

validateToken(token): boolean

getEmailFromToken(token): String
```

## 15.2. generateToken()

```text
generateToken(user): String
```

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

## 15.3. validateToken()

```text
validateToken(token): boolean
```

Kiểm tra JWT Token có hợp lệ hay không.

Kết quả:

```text
true
```

hoặc:

```text
false
```

## 15.4. getEmailFromToken()

```text
getEmailFromToken(token): String
```

Lấy email từ JWT Token.

Email được sử dụng để xác định User tương ứng.

---

# 16. JwtAuthenticationFilter

`JwtAuthenticationFilter` xử lý JWT trong HTTP Request.

## 16.1. Phương thức

```text
doFilterInternal(req, res, chain): void
```

## 16.2. Luồng

```text
Client
    ↓
Request + JWT
    ↓
JwtAuthenticationFilter
    ↓
Đọc JWT
    ↓
JwtUtil
    ↓
Kiểm tra JWT
    ↓
Xác định User
    ↓
Spring Security
    ↓
Controller
```

---

# 17. CustomUserDetailsService

`CustomUserDetailsService` kết nối User trong Database với Spring Security.

## 17.1. Phương thức

```text
loadUserByUsername(email): UserDetails
```

Trong thiết kế này, `email` được sử dụng để tìm User.

## 17.2. Luồng

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
    ↓
Spring Security
```

---

# 18. SecurityConfig

`SecurityConfig` là class cấu hình Spring Security.

## 18.1. Các phương thức

```text
filterChain(http): SecurityFilterChain

passwordEncoder(): PasswordEncoder
```

## 18.2. filterChain()

```text
filterChain(http): SecurityFilterChain
```

Dùng để cấu hình Security Filter Chain của hệ thống.

Đây là nơi cấu hình cách Spring Security xử lý các HTTP Request và kiểm tra quyền truy cập.

## 18.3. passwordEncoder()

```text
passwordEncoder(): PasswordEncoder
```

Cung cấp cơ chế mã hóa mật khẩu.

Mục đích là không lưu mật khẩu dạng plain text trong Database.

## 18.4. Quyền Admin

Theo diagram, toàn bộ API của `AdminController` yêu cầu:

```text
@PreAuthorize("hasRole('ADMIN')")
```

Cấu hình này đảm bảo các API Admin chỉ được truy cập bởi User có role `ADMIN`.

---

# 19. Package Service

Package `service` chứa Business Logic của hệ thống.

Diagram gồm:

```text
AuthService
OtpService
EmailService
UserService
AdminService
```

---

# 20. AuthService

`AuthService` xử lý các chức năng Authentication.

## 20.1. Các phương thức

```text
register(username, email, password): void

login(email, password): LoginResponse

forgotPassword(email): void

resetPassword(email, otp, newPassword): void
```

## 20.2. register()

```text
register(username, email, password): void
```

Xử lý quá trình đăng ký tài khoản.

Luồng tổng quát:

```text
Client
    ↓
AuthController
    ↓
AuthService.register()
    ↓
Kiểm tra thông tin đăng ký
    ↓
Tạo User
    ↓
OtpService
    ↓
Gửi OTP
```

## 20.3. login()

```text
login(email, password): LoginResponse
```

Xử lý đăng nhập.

```text
Email + Password
    ↓
AuthService
    ↓
Kiểm tra User
    ↓
Kiểm tra Password
    ↓
JwtUtil
    ↓
JWT
    ↓
LoginResponse
```

## 20.4. forgotPassword()

```text
forgotPassword(email): void
```

Xử lý yêu cầu quên mật khẩu.

```text
Email
    ↓
AuthService
    ↓
Tìm User
    ↓
OtpService
    ↓
Gửi OTP
```

## 20.5. resetPassword()

```text
resetPassword(email, otp, newPassword): void
```

Đặt lại mật khẩu sau khi OTP được xác thực.

```text
Email
+ OTP
+ New Password
    ↓
AuthService
    ↓
OtpService
    ↓
Xác thực OTP
    ↓
Cập nhật Password
```

---

# 21. OtpService

`OtpService` chịu trách nhiệm tạo, gửi và xác thực OTP.

## 21.1. Các phương thức

```text
generateAndSend(user, purpose): void

generateAndSend(user, purpose, newEmail): void

verifyAndConsume(user, otp, purpose): OtpVerification
```

## 21.2. generateAndSend()

```text
generateAndSend(user, purpose): void
```

Tạo OTP cho User theo một mục đích cụ thể và gửi OTP.

## 21.3. generateAndSend() với newEmail

```text
generateAndSend(user, purpose, newEmail): void
```

Phương thức này có thêm `newEmail`.

Dùng cho trường hợp cập nhật email:

```text
User
    ↓
newEmail
    ↓
Tạo OTP
    ↓
Lưu OtpVerification
    ↓
Gửi OTP
```

## 21.4. verifyAndConsume()

```text
verifyAndConsume(user, otp, purpose): OtpVerification
```

Xác thực OTP dựa trên:

```text
User
OTP
Purpose
```

Luồng:

```text
User
+ OTP
+ Purpose
    ↓
OtpService
    ↓
OtpVerificationRepository
    ↓
Tìm OTP
    ↓
Kiểm tra OTP
    ↓
Kiểm tra purpose
    ↓
Kiểm tra thời gian hết hạn
    ↓
OtpVerification
```

---

# 22. EmailService

`EmailService` chịu trách nhiệm gửi OTP qua Email.

## 22.1. Phương thức

```text
sendOtp(toEmail, otpCode): void
```

## 22.2. Luồng

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

# 23. UserService

`UserService` xử lý Business Logic dành cho User đã đăng nhập.

## 23.1. Các phương thức

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

---

## 23.2. getProfile()

```text
getProfile(userId): User
```

Lấy thông tin Profile của User.

```text
User ID
    ↓
UserService
    ↓
UserRepository
    ↓
User
```

---

## 23.3. updateProfile()

```text
updateProfile(userId, fullName): User
```

Cập nhật thông tin cá nhân của User.

Theo diagram, thông tin được cập nhật là:

```text
fullName
```

---

## 23.4. requestChangePasswordOtp()

```text
requestChangePasswordOtp(userId, currentPassword): void
```

Yêu cầu OTP để đổi mật khẩu.

Input:

```text
userId
currentPassword
```

Luồng:

```text
User
    ↓
Current Password
    ↓
Kiểm tra Password
    ↓
OtpService
    ↓
Gửi OTP
```

---

## 23.5. changePassword()

```text
changePassword(userId, otp, newPassword): void
```

Đổi mật khẩu sau khi OTP được xác thực.

Input:

```text
userId
otp
newPassword
```

Luồng:

```text
User ID
+ OTP
+ New Password
    ↓
UserService
    ↓
OtpService
    ↓
Xác thực OTP
    ↓
Cập nhật Password
```

---

## 23.6. updateAvatar()

```text
updateAvatar(userId, file): User
```

Cập nhật ảnh đại diện.

Input:

```text
userId
file
```

Kết quả:

```text
User
```

---

## 23.7. requestUpdateEmailOtp()

```text
requestUpdateEmailOtp(userId, newEmail): void
```

Yêu cầu OTP để cập nhật email.

Input:

```text
userId
newEmail
```

Luồng:

```text
User
    ↓
New Email
    ↓
UserService
    ↓
OtpService
    ↓
Tạo OTP
    ↓
EmailService
    ↓
Gửi OTP
```

---

## 23.8. updateEmail()

```text
updateEmail(userId, otp): void
```

Cập nhật email sau khi OTP được xác thực.

Input:

```text
userId
otp
```

Luồng:

```text
User ID
+ OTP
    ↓
UserService
    ↓
OtpService
    ↓
Xác thực OTP
    ↓
Cập nhật Email
```

---

## 23.9. disableAccount()

```text
disableAccount(userId, currentPassword): void
```

Vô hiệu hóa tài khoản.

Input:

```text
userId
currentPassword
```

Sau khi xử lý, trạng thái tài khoản:

```text
DISABLED
```

---

## 23.10. deleteAccount()

```text
deleteAccount(userId, currentPassword): void
```

Xóa tài khoản.

Input:

```text
userId
currentPassword
```

Theo diagram, tài khoản được xử lý theo hướng soft delete:

```text
AccountStatus = DELETED
```

---

## 23.11. requestBecomeArtist()

```text
requestBecomeArtist(userId, artistName, bio): void
```

User gửi yêu cầu trở thành Artist.

Input:

```text
userId
artistName
bio
```

Sau khi gửi yêu cầu:

```text
ArtistRequestStatus = PENDING
```

Luồng:

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

---

## 23.12. updateArtistProfile()

```text
updateArtistProfile(userId, artistName, bio): User
```

Cập nhật thông tin hồ sơ Artist.

Input:

```text
userId
artistName
bio
```

Kết quả:

```text
User
```

---

# 24. AdminService

`AdminService` xử lý Business Logic dành cho Admin.

## 24.1. Các phương thức

```text
getAllUsers(): List<User>

lockAccount(userId): void

unlockAccount(userId): void

getPendingArtistRequests(): List<User>

approveArtistRequest(userId): void

rejectArtistRequest(userId): void

assignRole(userId, role): void
```

---

## 24.2. getAllUsers()

```text
getAllUsers(): List<User>
```

Lấy danh sách User trong hệ thống.

---

## 24.3. lockAccount()

```text
lockAccount(userId): void
```

Khóa tài khoản User.

Input:

```text
userId
```

---

## 24.4. unlockAccount()

```text
unlockAccount(userId): void
```

Mở khóa tài khoản User.

Input:

```text
userId
```

---

## 24.5. getPendingArtistRequests()

```text
getPendingArtistRequests(): List<User>
```

Lấy danh sách các User đang yêu cầu trở thành Artist.

Điều kiện:

```text
ArtistRequestStatus = PENDING
```

---

## 24.6. approveArtistRequest()

```text
approveArtistRequest(userId): void
```

Duyệt yêu cầu trở thành Artist.

Sau khi duyệt:

```text
ArtistRequestStatus = APPROVED
Role = ARTIST
```

Luồng:

```text
Admin
    ↓
AdminController
    ↓
AdminService
    ↓
approveArtistRequest()
    ↓
ArtistRequestStatus = APPROVED
    ↓
Role = ARTIST
```

---

## 24.7. rejectArtistRequest()

```text
rejectArtistRequest(userId): void
```

Từ chối yêu cầu trở thành Artist.

Sau khi từ chối:

```text
ArtistRequestStatus = REJECTED
```

---

## 24.8. assignRole()

```text
assignRole(userId, role): void
```

Gán Role cho User.

Input:

```text
userId
role
```

Các Role trong diagram:

```text
USER
ARTIST
ADMIN
```

---

# 25. Package Controller

Package `controller` là tầng tiếp nhận HTTP Request từ Client.

Diagram gồm:

```text
AuthController
UserController
AdminController
```

Controller không trực tiếp xử lý Business Logic mà chuyển request xuống Service tương ứng.

---

# 26. AuthController

`AuthController` xử lý các API Authentication.

## 26.1. Các phương thức

```text
register()

verifyOtp()

login()

forgotPassword()

resetPassword()
```

## 26.2. Chức năng

| Method             | Chức năng        |
| ------------------ | ---------------- |
| `register()`       | Đăng ký          |
| `verifyOtp()`      | Xác thực OTP     |
| `login()`          | Đăng nhập        |
| `forgotPassword()` | Quên mật khẩu    |
| `resetPassword()`  | Đặt lại mật khẩu |

## 26.3. Luồng

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

# 27. UserController

`UserController` xử lý các API dành cho User.

## 27.1. Các phương thức

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

## 27.2. Nhóm Profile

```text
getProfile()
updateProfile()
updateAvatar()
```

## 27.3. Nhóm Password

```text
requestChangePasswordOtp()
changePassword()
```

## 27.4. Nhóm Email

```text
requestUpdateEmailOtp()
updateEmail()
```

## 27.5. Nhóm Account

```text
logout()
disableAccount()
deleteAccount()
```

## 27.6. Nhóm Artist

```text
requestBecomeArtist()
updateArtistProfile()
```

---

# 28. AdminController

`AdminController` xử lý các API dành cho Admin.

## 28.1. Các phương thức

```text
getAllUsers()

lockAccount()

unlockAccount()

getPendingArtistRequests()

approveArtistRequest()

rejectArtistRequest()

assignRole()
```

## 28.2. Authorization

Theo diagram, toàn bộ API của `AdminController` được bảo vệ bằng:

```text
@PreAuthorize("hasRole('ADMIN')")
```

Do đó:

```text
Client
    ↓
AdminController
    ↓
Kiểm tra Role
    ↓
ADMIN ?
    ↓
AdminService
```

Chỉ User có role `ADMIN` mới được phép thực hiện các API này.

---

# 29. Luồng Authentication

## 29.1. Đăng ký

```text
Client
    ↓
AuthController
    ↓
AuthService
    ↓
Kiểm tra Username / Email
    ↓
Tạo User
    ↓
AccountStatus = UNVERIFIED
    ↓
OtpService
    ↓
EmailService
    ↓
Gửi OTP
```

---

## 29.2. Xác thực OTP

```text
Client
    ↓
AuthController.verifyOtp()
    ↓
OtpService
    ↓
OtpVerificationRepository
    ↓
Tìm OTP
    ↓
Kiểm tra OTP
    ↓
Xác thực thành công
    ↓
AccountStatus = ACTIVE
```

---

## 29.3. Đăng nhập

```text
Client
    ↓
AuthController.login()
    ↓
AuthService
    ↓
UserRepository
    ↓
Kiểm tra Email
    ↓
Kiểm tra Password
    ↓
JwtUtil
    ↓
Tạo JWT
    ↓
LoginResponse
    ↓
Client
```

---

# 30. Luồng JWT Authentication

Đối với API yêu cầu đăng nhập:

```text
Client
    ↓
Request + JWT
    ↓
JwtAuthenticationFilter
    ↓
JwtUtil
    ↓
validateToken()
    ↓
getEmailFromToken()
    ↓
CustomUserDetailsService
    ↓
UserRepository
    ↓
User
    ↓
UserDetails
    ↓
Spring Security
    ↓
Controller
```

---

# 31. Luồng OTP

OTP được xử lý thông qua các thành phần:

```text
OtpService
EmailService
OtpVerificationRepository
OtpVerification
```

## 31.1. Tạo và gửi OTP

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

## 31.2. Xác thực OTP

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
Tìm OTP
    ↓
Kiểm tra OTP
    ↓
Xác thực
```

---

# 32. Luồng Quên mật khẩu

```text
User
    ↓
Nhập Email
    ↓
AuthController
    ↓
AuthService.forgotPassword()
    ↓
UserRepository
    ↓
Tìm User
    ↓
OtpService
    ↓
OtpPurpose = RESET_PASSWORD
    ↓
EmailService
    ↓
Gửi OTP
```

Sau đó User nhập OTP và mật khẩu mới:

```text
User
    ↓
Email
+ OTP
+ New Password
    ↓
AuthController
    ↓
AuthService.resetPassword()
    ↓
OtpService
    ↓
Xác thực OTP
    ↓
Cập nhật Password
```

---

# 33. Luồng Đổi mật khẩu

Đổi mật khẩu gồm hai bước.

## Bước 1 – Yêu cầu OTP

```text
User
    ↓
Current Password
    ↓
UserController
    ↓
UserService.requestChangePasswordOtp()
    ↓
Kiểm tra Current Password
    ↓
OtpService
    ↓
OtpPurpose = CHANGE_PASSWORD
    ↓
EmailService
    ↓
Gửi OTP
```

## Bước 2 – Đổi mật khẩu

```text
User
    ↓
OTP + New Password
    ↓
UserController
    ↓
UserService.changePassword()
    ↓
OtpService
    ↓
Xác thực OTP
    ↓
Cập nhật Password
```

---

# 34. Luồng Cập nhật Email

Đổi email cũng gồm hai bước.

## Bước 1 – Yêu cầu OTP

```text
User
    ↓
New Email
    ↓
UserController
    ↓
UserService.requestUpdateEmailOtp()
    ↓
OtpService
    ↓
OtpPurpose = CHANGE_EMAIL
    ↓
EmailService
    ↓
Gửi OTP
```

## Bước 2 – Cập nhật Email

```text
User
    ↓
OTP
    ↓
UserController
    ↓
UserService.updateEmail()
    ↓
OtpService
    ↓
Xác thực OTP
    ↓
Cập nhật Email
```

---

# 35. Luồng User trở thành Artist

## 35.1. User gửi yêu cầu

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

## 35.2. Admin xem yêu cầu

```text
Admin
    ↓
AdminController
    ↓
AdminService
    ↓
getPendingArtistRequests()
    ↓
UserRepository
    ↓
ArtistRequestStatus = PENDING
    ↓
Danh sách User
```

## 35.3. Admin duyệt

```text
Admin
    ↓
AdminController
    ↓
AdminService
    ↓
approveArtistRequest()
    ↓
ArtistRequestStatus = APPROVED
    ↓
Role = ARTIST
```

## 35.4. Admin từ chối

```text
Admin
    ↓
AdminController
    ↓
AdminService
    ↓
rejectArtistRequest()
    ↓
ArtistRequestStatus = REJECTED
```

---

# 36. Luồng quản lý tài khoản

## 36.1. Vô hiệu hóa tài khoản

```text
User
    ↓
Current Password
    ↓
UserController
    ↓
UserService.disableAccount()
    ↓
Kiểm tra Password
    ↓
AccountStatus = DISABLED
```

## 36.2. Xóa tài khoản

```text
User
    ↓
Current Password
    ↓
UserController
    ↓
UserService.deleteAccount()
    ↓
Kiểm tra Password
    ↓
AccountStatus = DELETED
```

Đây là hướng xử lý soft delete được thể hiện trong diagram.

---

# 37. Luồng Admin

Các API Admin được bảo vệ bởi Role:

```text
@PreAuthorize("hasRole('ADMIN')")
```

Luồng tổng quát:

```text
Client
    ↓
JWT
    ↓
JwtAuthenticationFilter
    ↓
Spring Security
    ↓
Kiểm tra Role
    ↓
ADMIN
    ↓
AdminController
    ↓
AdminService
    ↓
UserRepository
    ↓
Database
```

Các chức năng Admin:

```text
getAllUsers()
lockAccount()
unlockAccount()
getPendingArtistRequests()
approveArtistRequest()
rejectArtistRequest()
assignRole()
```

---

# 38. Quan hệ giữa các tầng

Kiến trúc chính của Module Người Dùng:

```text
                    ┌─────────────────────┐
                    │     Controller      │
                    │                     │
                    │ AuthController      │
                    │ UserController      │
                    │ AdminController     │
                    └──────────┬──────────┘
                               │
                               ↓
                    ┌─────────────────────┐
                    │       Service       │
                    │                     │
                    │ AuthService         │
                    │ OtpService          │
                    │ EmailService        │
                    │ UserService         │
                    │ AdminService        │
                    └──────────┬──────────┘
                               │
                               ↓
                    ┌─────────────────────┐
                    │     Repository      │
                    │                     │
                    │ UserRepository      │
                    │ OtpVerification     │
                    │ Repository          │
                    └──────────┬──────────┘
                               │
                               ↓
                    ┌─────────────────────┐
                    │       Entity        │
                    │                     │
                    │ User                │
                    │ OtpVerification     │
                    └─────────────────────┘
```

---

# 39. Security hoạt động xuyên suốt

Security không nằm hoàn toàn trong luồng Controller → Service → Repository mà tham gia vào quá trình xác thực Request.

```text
                 ┌──────────────────────┐
                 │    SecurityConfig     │
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

# 40. Mối quan hệ giữa các thành phần

## User và OtpVerification

```text
User
  1
  |
  *
OtpVerification
```

Một User có thể có nhiều bản ghi OTP.

## User và các Enum

`User` sử dụng:

```text
AccountStatus
Role
ArtistRequestStatus
```

Có thể hình dung:

```text
User
 ├── accountStatus: AccountStatus
 ├── role: Role
 └── artistRequestStatus: ArtistRequestStatus
```

## OtpVerification và OtpPurpose

```text
OtpVerification
    |
    └── purpose: OtpPurpose
```

OTP được xác định mục đích thông qua `OtpPurpose`.

---

# 41. Tổng hợp toàn bộ Class

| Package       | Class                       | Vai trò                       |
| ------------- | --------------------------- | ----------------------------- |
| Entity        | `User`                      | Đại diện tài khoản User       |
| Entity        | `OtpVerification`           | Lưu thông tin OTP             |
| Entity / Enum | `AccountStatus`             | Trạng thái tài khoản          |
| Entity / Enum | `OtpPurpose`                | Mục đích OTP                  |
| Entity / Enum | `Role`                      | Vai trò User                  |
| Entity / Enum | `ArtistRequestStatus`       | Trạng thái yêu cầu Artist     |
| DTO           | `LoginResponse`             | Response sau đăng nhập        |
| Repository    | `UserRepository`            | Truy vấn User                 |
| Repository    | `OtpVerificationRepository` | Truy vấn OTP                  |
| Security      | `JwtUtil`                   | Tạo và kiểm tra JWT           |
| Security      | `JwtAuthenticationFilter`   | Xử lý JWT Request             |
| Security      | `CustomUserDetailsService`  | Load User cho Spring Security |
| Security      | `SecurityConfig`            | Cấu hình Spring Security      |
| Service       | `AuthService`               | Authentication                |
| Service       | `OtpService`                | Tạo và xác thực OTP           |
| Service       | `EmailService`              | Gửi OTP qua Email             |
| Service       | `UserService`               | Business Logic của User       |
| Service       | `AdminService`              | Business Logic của Admin      |
| Controller    | `AuthController`            | API Authentication            |
| Controller    | `UserController`            | API User                      |
| Controller    | `AdminController`           | API Admin                     |

---

# 42. Tổng hợp Method

## AuthController

```text
register()
verifyOtp()
login()
forgotPassword()
resetPassword()
```

## UserController

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

## AdminController

```text
getAllUsers()
lockAccount()
unlockAccount()
getPendingArtistRequests()
approveArtistRequest()
rejectArtistRequest()
assignRole()
```

## AuthService

```text
register(username, email, password): void
login(email, password): LoginResponse
forgotPassword(email): void
resetPassword(email, otp, newPassword): void
```

## OtpService

```text
generateAndSend(user, purpose): void
generateAndSend(user, purpose, newEmail): void
verifyAndConsume(user, otp, purpose): OtpVerification
```

## EmailService

```text
sendOtp(toEmail, otpCode): void
```

## UserService

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

## AdminService

```text
getAllUsers(): List<User>
lockAccount(userId): void
unlockAccount(userId): void
getPendingArtistRequests(): List<User>
approveArtistRequest(userId): void
rejectArtistRequest(userId): void
assignRole(userId, role): void
```

## UserRepository

```text
findByEmail(email): Optional<User>
existsByEmail(email): boolean
existsByUsername(username): boolean
findByArtistRequestStatus(status): List<User>
```

## OtpVerificationRepository

```text
findFirstByUserAndPurposeAndVerifiedFalseOrderByIdDesc(
    user,
    purpose
): Optional<OtpVerification>
```

## JwtUtil

```text
generateToken(user): String
validateToken(token): boolean
getEmailFromToken(token): String
```

## JwtAuthenticationFilter

```text
doFilterInternal(req, res, chain): void
```

## CustomUserDetailsService

```text
loadUserByUsername(email): UserDetails
```

## SecurityConfig

```text
filterChain(http): SecurityFilterChain
passwordEncoder(): PasswordEncoder
```

---

# 43. Tổng kết kiến trúc

Module Người Dùng được xây dựng theo kiến trúc nhiều tầng:

```text
Client
   ↓
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

Mỗi tầng có trách nhiệm riêng:

```text
Controller
→ Tiếp nhận HTTP Request và trả Response

Service
→ Xử lý Business Logic

Repository
→ Truy vấn và thao tác dữ liệu

Entity
→ Đại diện cho dữ liệu trong Database

DTO
→ Trao đổi dữ liệu giữa Client và Backend

Security
→ Authentication và Authorization

Enum
→ Biểu diễn các trạng thái và giá trị cố định
```

Các thành phần Security:

```text
JwtAuthenticationFilter
JwtUtil
CustomUserDetailsService
SecurityConfig
```

phối hợp để xử lý Authentication và Authorization.

Các thành phần OTP:

```text
OtpService
EmailService
OtpVerificationRepository
OtpVerification
```

phối hợp để thực hiện các chức năng:

```text
Đăng ký
Quên mật khẩu
Đổi mật khẩu
Cập nhật email
```

Các thành phần Artist:

```text
UserService
AdminService
ArtistRequestStatus
Role
```

phối hợp để thực hiện:

```text
User gửi yêu cầu
    ↓
PENDING
    ↓
Admin xử lý
    ↓
APPROVED / REJECTED
    ↓
Nếu APPROVED
    ↓
Role = ARTIST
```

Như vậy, Class Diagram của Module Người Dùng là cơ sở để triển khai các class thực tế trong Backend và thể hiện rõ trách nhiệm của từng package, từng class cũng như luồng phối hợp giữa các tầng.
