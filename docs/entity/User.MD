# User Entity

# 1. Tổng quan

`User` là Entity đại diện cho **người dùng trong hệ thống Spotify**.

Entity này có nhiệm vụ chính:

* Đại diện cho một người dùng trong hệ thống.
* Ánh xạ với bảng `users` trong MySQL.
* Lưu thông tin đăng nhập.
* Lưu thông tin cá nhân.
* Lưu trạng thái tài khoản.
* Lưu quyền của người dùng.
* Lưu thông tin liên quan đến Artist.

Có thể hiểu đơn giản:

**User Entity ↔ bảng `users` trong Database**.

Mỗi object `User` trong Java tương ứng với một user trong hệ thống.

---

# 2. Entity là gì?

Trong Spring Boot sử dụng JPA/Hibernate, **Entity** là một class Java đại diện cho một bảng trong Database.

Ví dụ:

| Java                  | Database                |
| --------------------- | ----------------------- |
| `User`                | `users`                 |
| `id`                  | `id`                    |
| `username`            | `username`              |
| `email`               | `email`                 |
| `password`            | `password`              |
| `fullName`            | `full_name`             |
| `avatarUrl`           | `avatar_url`            |
| `accountStatus`       | `account_status`        |
| `role`                | `role`                  |
| `artistRequestStatus` | `artist_request_status` |
| `artistName`          | `artist_name`           |
| `bio`                 | `bio`                   |

Ví dụ một User trong Java có:

`id = 1`

`username = "phong123"`

`email = "phong@gmail.com"`

thì trong Database sẽ có một dòng tương ứng trong bảng `users`.

---

# 3. `@Entity`

`@Entity` đánh dấu class `User` là một JPA Entity.

Điều này cho Hibernate biết:

> Class `User` cần được quản lý và ánh xạ với Database.

Nếu một class không có `@Entity`, JPA sẽ không xem class đó là Entity để thực hiện các thao tác lưu, tìm kiếm, cập nhật hoặc xóa dữ liệu.

---

# 4. `@Table(name = "users")`

`@Table(name = "users")` xác định tên bảng Database mà Entity này ánh xạ tới.

Trong trường hợp này:

`User` → `users`

Vì vậy, tất cả dữ liệu của User sẽ được lưu trong bảng `users`.

---

# 5. ID của User

Field `id` là **Primary Key** của User.

`@Id` đánh dấu `id` là khóa chính.

`@GeneratedValue(strategy = GenerationType.IDENTITY)` cho phép Database tự động tạo ID khi thêm User mới.

Ví dụ:

| id | username |
| -: | -------- |
|  1 | phong123 |
|  2 | user01   |
|  3 | artist01 |

Khi tạo User mới, Service không cần tự tạo ID.

Database sẽ tự sinh ID.

Ví dụ:

User mới → Database tự tạo `id = 4`.

---

# 6. Username

Field `username` lưu tên đăng nhập của User.

Các cấu hình:

* `nullable = false`: bắt buộc phải có username.
* `unique = true`: username không được trùng.
* `length = 50`: tối đa 50 ký tự.

Ví dụ:

`username = "phong123"`

Hai User không thể cùng sử dụng username `phong123`.

---

# 7. Email

Field `email` lưu email của User.

Các cấu hình:

* `nullable = false`: bắt buộc phải có email.
* `unique = true`: email không được trùng.
* `length = 100`: tối đa 100 ký tự.

Ví dụ:

`email = "phong@gmail.com"`

Nếu email này đã tồn tại trong Database thì User khác không thể đăng ký lại bằng email đó.

Email cũng có thể được sử dụng để đăng nhập.

---

# 8. Password

Field `password` lưu mật khẩu của User.

Password bắt buộc phải có.

Trong hệ thống thực tế, **không lưu password dạng text gốc**.

Ví dụ không nên lưu:

`123456`

Thay vào đó, password nên được mã hóa bằng BCrypt trước khi lưu Database.

Ví dụ Database có thể lưu dạng:

`$2a$10$......`

Luồng xử lý:

**User nhập password** → `123456` → `PasswordEncoder` → BCrypt hash → lưu password đã hash vào Database.

Khi đăng nhập:

**User nhập password** → `PasswordEncoder.matches()` → so sánh với password đã hash trong Database.

---

# 9. Full Name

Field `fullName` lưu họ tên đầy đủ của User.

Ví dụ:

`fullName = "Nguyễn Thanh Phong"`

Trong Java field có tên:

`fullName`

Nhưng trong Database column có tên:

`full_name`

Việc chuyển từ `fullName` sang `full_name` được cấu hình thông qua `@Column(name = "full_name")`.

---

# 10. Avatar URL

Field `avatarUrl` lưu đường dẫn đến ảnh đại diện của User.

Ví dụ:

`avatarUrl = "https://example.com/avatar/phong.jpg"`

Database thường **không lưu trực tiếp file ảnh**.

Thay vào đó:

**File ảnh** → được lưu ở nơi lưu trữ ảnh.

Sau đó:

**URL của ảnh** → được lưu vào `avatar_url`.

Ví dụ:

`avatar_url = "https://example.com/avatar/phong.jpg"`

---

# 11. Account Status

Field `accountStatus` lưu **trạng thái hiện tại của tài khoản**.

Entity sử dụng Enum `AccountStatus`.

Các trạng thái có thể gồm:

* `UNVERIFIED`
* `ACTIVE`
* `DISABLED`
* `DELETED`

## UNVERIFIED

Tài khoản vừa đăng ký nhưng chưa xác thực email/OTP.

Ví dụ:

**Đăng ký** → `UNVERIFIED` → gửi OTP → User nhập OTP → xác thực thành công.

---

## ACTIVE

Tài khoản đã được xác thực và đang hoạt động bình thường.

Ví dụ:

`accountStatus = ACTIVE`

User có thể đăng nhập và sử dụng hệ thống.

---

## DISABLED

Tài khoản bị vô hiệu hóa.

Ví dụ:

User thực hiện chức năng vô hiệu hóa tài khoản.

Trạng thái:

`ACTIVE → DISABLED`

---

## DELETED

Tài khoản đã bị xóa hoặc được đánh dấu là đã xóa.

Trạng thái:

`ACTIVE → DELETED`

---

# 12. Tại sao dùng `@Enumerated(EnumType.STRING)`?

`accountStatus` là một Enum.

`@Enumerated(EnumType.STRING)` yêu cầu Hibernate lưu **tên của Enum dưới dạng String** trong Database.

Ví dụ:

`AccountStatus.ACTIVE`

Database sẽ lưu:

`ACTIVE`

Thay vì lưu số:

`0`, `1`, `2`, `3`.

Cách lưu String giúp Database dễ đọc và tránh một số vấn đề khi thay đổi thứ tự các giá trị trong Enum.

---

# 13. Giá trị mặc định của Account Status

`accountStatus` có giá trị mặc định:

`UNVERIFIED`

Điều này có nghĩa khi tạo User mới, nếu không thay đổi trạng thái thì User sẽ bắt đầu ở trạng thái:

`UNVERIFIED`

Luồng đăng ký:

**Đăng ký** → tạo User → `accountStatus = UNVERIFIED` → gửi OTP → User nhập OTP → xác thực thành công → `accountStatus = ACTIVE`.

---

# 14. Role

Field `role` xác định **quyền của User trong hệ thống**.

Các Role có thể gồm:

* `USER`
* `ARTIST`
* `ADMIN`

User mới mặc định:

`role = USER`

---

# 15. Ý nghĩa của Role

### USER

Người dùng thông thường.

Có thể sử dụng các chức năng dành cho User.

### ARTIST

Người dùng đã trở thành Artist.

Có thể sử dụng thêm các chức năng dành cho Artist, ví dụ quản lý bài hát hoặc album của mình.

### ADMIN

Quản trị viên hệ thống.

Có quyền thực hiện các chức năng quản trị.

---

# 16. Role và Spring Security

`role` có vai trò quan trọng trong Spring Security.

Khi User đăng nhập:

**Email + Password** → tìm User → kiểm tra password → kiểm tra trạng thái tài khoản → lấy Role → tạo Authentication → tạo JWT.

Sau đó JWT được sử dụng trong những request tiếp theo.

Spring Security có thể dựa vào Role để kiểm tra quyền truy cập API.

Ví dụ:

`USER` → API dành cho User.

`ARTIST` → API dành cho Artist.

`ADMIN` → API dành cho Admin.

---

# 17. Artist Request Status

Field `artistRequestStatus` lưu trạng thái **yêu cầu trở thành Artist** của User.

Các trạng thái có thể gồm:

* `NONE`
* `PENDING`
* `APPROVED`
* `REJECTED`

---

# 18. Ý nghĩa Artist Request Status

## NONE

User chưa gửi yêu cầu trở thành Artist.

Ví dụ:

`role = USER`

`artistRequestStatus = NONE`

---

## PENDING

User đã gửi yêu cầu và đang chờ Admin xét duyệt.

Ví dụ:

`role = USER`

`artistRequestStatus = PENDING`

Điều này có nghĩa:

> User vẫn đang là USER nhưng đã gửi yêu cầu trở thành Artist.

---

## APPROVED

Yêu cầu trở thành Artist đã được chấp thuận.

Ví dụ:

`role = ARTIST`

`artistRequestStatus = APPROVED`

---

## REJECTED

Yêu cầu trở thành Artist đã bị từ chối.

Ví dụ:

`role = USER`

`artistRequestStatus = REJECTED`

User có thể được phép gửi lại yêu cầu tùy theo logic của hệ thống.

---

# 19. Role và Artist Request Status khác nhau

Hai field này có mục đích khác nhau.

### `role`

Cho biết:

> User hiện tại có quyền gì?

### `artistRequestStatus`

Cho biết:

> Yêu cầu trở thành Artist đang ở trạng thái nào?

Ví dụ:

`role = USER`

`artistRequestStatus = PENDING`

Có nghĩa:

> User hiện tại vẫn là USER và đang chờ Admin xét duyệt.

Sau khi được duyệt:

`role = ARTIST`

`artistRequestStatus = APPROVED`

---

# 20. Artist Name

Field `artistName` lưu **nghệ danh của Artist**.

Ví dụ:

`artistName = "My Artist"`

User bình thường có thể không có artist name:

`artistName = null`

Sau khi trở thành Artist, User có thể có:

`artistName = "My Artist"`

---

# 21. Bio

Field `bio` lưu phần giới thiệu của User hoặc Artist.

Ví dụ:

`bio = "Singer and songwriter from Vietnam."`

Bio có thể được sử dụng để hiển thị trên trang Profile hoặc Artist Profile.

---

# 22. Getter và Setter

Class `User` có Getter và Setter cho các field.

## Getter

Getter dùng để **lấy dữ liệu** từ object.

Ví dụ:

`user.getEmail()`

có nghĩa:

> Lấy email của User.

Một số getter:

* `getId()`
* `getUsername()`
* `getEmail()`
* `getPassword()`
* `getFullName()`
* `getAvatarUrl()`
* `getAccountStatus()`
* `getRole()`
* `getArtistRequestStatus()`
* `getArtistName()`
* `getBio()`

---

## Setter

Setter dùng để **thay đổi dữ liệu** của object.

Ví dụ:

`user.setFullName("Nguyễn Thanh Phong")`

có nghĩa:

> Thay đổi full name của User.

Một số setter:

* `setId()`
* `setUsername()`
* `setEmail()`
* `setPassword()`
* `setFullName()`
* `setAvatarUrl()`
* `setAccountStatus()`
* `setRole()`
* `setArtistRequestStatus()`
* `setArtistName()`
* `setBio()`

---

# 23. Cấu trúc dữ liệu của User

Có thể chia User thành 4 nhóm chính:

### 1. Authentication

Thông tin phục vụ đăng nhập:

* `id`
* `username`
* `email`
* `password`

### 2. Profile

Thông tin cá nhân:

* `fullName`
* `avatarUrl`
* `bio`

### 3. Account & Security

Thông tin tài khoản và quyền:

* `accountStatus`
* `role`

### 4. Artist

Thông tin liên quan đến Artist:

* `artistRequestStatus`
* `artistName`

---

# 24. Bảng tổng hợp các Field

| Field                 | Database Column         | Kiểu dữ liệu        | Mục đích                  |
| --------------------- | ----------------------- | ------------------- | ------------------------- |
| `id`                  | `id`                    | Long                | ID của User               |
| `username`            | `username`              | String              | Tên đăng nhập             |
| `email`               | `email`                 | String              | Email                     |
| `password`            | `password`              | String              | Password đã hash          |
| `fullName`            | `full_name`             | String              | Họ tên                    |
| `avatarUrl`           | `avatar_url`            | String              | URL avatar                |
| `accountStatus`       | `account_status`        | AccountStatus       | Trạng thái tài khoản      |
| `role`                | `role`                  | Role                | Quyền của User            |
| `artistRequestStatus` | `artist_request_status` | ArtistRequestStatus | Trạng thái yêu cầu Artist |
| `artistName`          | `artist_name`           | String              | Nghệ danh                 |
| `bio`                 | `bio`                   | String              | Giới thiệu                |

---

# 25. User Entity và Database

Quan hệ giữa Entity và Database:

**User Entity** → JPA  → Hibernate → MySQL → bảng `users`.

Entity không trực tiếp gửi SQL bằng tay trong những thao tác JPA cơ bản.

Repository sẽ sử dụng Entity để thao tác với Database.

---

# 26. User Entity và Repository

Ví dụ project có `UserRepository`.

Repository sẽ sử dụng:

`User`

làm Entity và:

`Long`

làm kiểu dữ liệu của Primary Key.

Repository có thể thực hiện các thao tác như:

* Tìm User.
* Lưu User.
* Cập nhật User.
* Xóa User.

Ví dụ:

`userRepository.save(user)`

có nghĩa:

> Lưu User vào Database.

---

# 27. User Entity trong luồng đăng ký

Luồng đăng ký có thể hiểu như sau:

**Frontend** → gửi request đăng ký → `AuthController` → `AuthService` → kiểm tra username/email → mã hóa password → tạo `User` → `UserRepository` → Hibernate/JPA → MySQL → bảng `users`.

Khi User mới được tạo, các giá trị mặc định có thể là:

`accountStatus = UNVERIFIED`

`role = USER`

`artistRequestStatus = NONE`

---

# 28. User Entity trong luồng đăng nhập

Luồng đăng nhập:

**Frontend** → gửi email + password  → `AuthController` → `AuthService` → `UserRepository` → tìm User bằng email  → kiểm tra password → kiểm tra `accountStatus`→ lấy `role`→ tạo JWT → trả JWT cho Frontend.

User Entity cung cấp các dữ liệu quan trọng:

* Email.
* Password đã hash.
* Account status.
* Role.
* User ID.

---

# 29. User Entity và JWT

Khi đăng nhập thành công, hệ thống có thể lấy:

`user.id`

`user.email`

`user.role`

để tạo JWT.

Ví dụ JWT có thể chứa:

* User ID.
* Email.
* Role.
* Thời gian phát hành.
* Thời gian hết hạn.

Frontend gửi JWT trong các request sau:

`Authorization: Bearer <token>`

Backend xác thực JWT để biết request đang thuộc User nào.

---

# 30. User Entity và Spring Security

User Entity có thể được sử dụng bởi `CustomUserDetails`.

Luồng:

**User** → `CustomUserDetails` → Spring Security.

`CustomUserDetails` có thể lấy:

* Email.
* Password.
* Account status.
* Role.

Sau đó Spring Security sử dụng các thông tin này để authentication và authorization.

---

# 31. User Entity và DTO

Entity và DTO có mục đích khác nhau.

## Entity

`User`

Dùng để:

> Đại diện dữ liệu trong Database.

## DTO

Ví dụ:

`RegisterRequest`

Dùng để:

> Nhận dữ liệu đăng ký từ Frontend.

Ví dụ Frontend gửi:

`username`

`email`

`password`

Backend nhận dữ liệu thông qua `RegisterRequest`.

Sau đó Service chuyển dữ liệu từ DTO sang Entity `User`.

Luồng:

**RegisterRequest** → `AuthService` → `User` → `UserRepository` → Database.

---

# 32. Tại sao không nên dùng User Entity trực tiếp làm Request?

Nếu Frontend được phép gửi trực tiếp toàn bộ User Entity, Frontend có thể cố gắng thay đổi những field mà nó không được phép thay đổi.

Ví dụ:

`role = ADMIN`

hoặc:

`accountStatus = ACTIVE`

Đây là lý do nên sử dụng DTO.

Frontend chỉ gửi những dữ liệu cần thiết.

Ví dụ đăng ký:

`username`

`email`

`password`

Backend tự quyết định:

`role = USER`

`accountStatus = UNVERIFIED`

`artistRequestStatus = NONE`

---

# 33. User Entity và Service

Service là nơi xử lý Business Logic.

Ví dụ:

**Đăng ký User**

Service sẽ:

1. Kiểm tra username đã tồn tại chưa.
2. Kiểm tra email đã tồn tại chưa.
3. Hash password.
4. Tạo User.
5. Thiết lập trạng thái tài khoản.
6. Lưu User.
7. Gửi OTP.

Entity `User` chỉ đại diện cho dữ liệu.

Nó không nên chịu trách nhiệm thực hiện toàn bộ Business Logic của hệ thống.

---

# 34. User Entity và Repository

Repository chịu trách nhiệm giao tiếp với Database.

Ví dụ các thao tác:

**Tìm User theo email** → Repository tìm trong bảng `users`.

**Lưu User** → Repository yêu cầu JPA/Hibernate lưu vào bảng `users`.

**Cập nhật User** → Repository/JPA cập nhật dữ liệu tương ứng.

---

# 35. Tổng quan kiến trúc

Có thể hình dung:

**Controller**

Nhận HTTP Request.

↓

**Service**

Xử lý Business Logic.

↓

**Repository**

Thao tác với Database.

↓

**Entity - User**

Đại diện dữ liệu User.

↓

**JPA / Hibernate**

Chuyển đổi giữa Java Object và Database.

↓

**MySQL**

Lưu dữ liệu thật.

---

# 36. Ví dụ luồng đăng ký hoàn chỉnh

Người dùng nhập:

`username = phong123`

`email = phong@gmail.com`

`password = 123456`

Frontend gửi request đến Backend.

Backend xử lý:

**AuthController** → nhận Request.

**AuthService** → kiểm tra username → kiểm tra email → hash password → tạo User.

User mới có:

`username = phong123`

`email = phong@gmail.com`

`password = password đã hash`

`accountStatus = UNVERIFIED`

`role = USER`

`artistRequestStatus = NONE`

Sau đó:

**UserRepository** → lưu User → MySQL → bảng `users`.

Sau khi đăng ký thành công, hệ thống gửi OTP để User xác thực email.

---

# 37. Ví dụ luồng xác thực OTP

Sau khi đăng ký:

`accountStatus = UNVERIFIED`

User nhập OTP.

Backend kiểm tra OTP.

Nếu OTP hợp lệ:

`accountStatus = ACTIVE`

Sau đó User có thể đăng nhập.

Luồng:

**Register** → `UNVERIFIED` → gửi OTP → Verify OTP → `ACTIVE` → Login → JWT.

---

# 38. Ví dụ luồng trở thành Artist

User hiện tại:

`role = USER`

`artistRequestStatus = NONE`

User gửi yêu cầu:

`artistRequestStatus = PENDING`

Admin xử lý.

Nếu được chấp thuận:

`artistRequestStatus = APPROVED`

và:

`role = ARTIST`

Sau đó User có thể sử dụng các chức năng dành cho Artist.

---

# 39. Ví dụ dữ liệu User

Một User thông thường có thể có:

| Field                 | Value                                     |
| --------------------- | ----------------------------------------- |
| `id`                  | 1                                         |
| `username`            | phong123                                  |
| `email`               | [phong@gmail.com](mailto:phong@gmail.com) |
| `password`            | BCrypt hash                               |
| `fullName`            | Nguyễn Thanh Phong                        |
| `avatarUrl`           | Avatar URL                                |
| `accountStatus`       | ACTIVE                                    |
| `role`                | USER                                      |
| `artistRequestStatus` | NONE                                      |
| `artistName`          | null                                      |
| `bio`                 | null                                      |

Một Artist có thể có:

| Field                 | Value                                       |
| --------------------- | ------------------------------------------- |
| `id`                  | 2                                           |
| `username`            | artist01                                    |
| `email`               | [artist@gmail.com](mailto:artist@gmail.com) |
| `password`            | BCrypt hash                                 |
| `fullName`            | Artist Name                                 |
| `avatarUrl`           | Avatar URL                                  |
| `accountStatus`       | ACTIVE                                      |
| `role`                | ARTIST                                      |
| `artistRequestStatus` | APPROVED                                    |
| `artistName`          | My Artist                                   |
| `bio`                 | Singer and songwriter                       |

---

# 40. Sơ đồ tổng quan

```text
                        User
                         │
        ┌────────────────┼────────────────┐
        │                │                │
        ↓                ↓                ↓
 Authentication       Profile          Artist
        │                │                │
 ┌──────┼──────┐    ┌────┼────┐      ┌────┼──────────┐
 │      │      │    │    │    │      │    │           │
id username email  fullName avatarUrl bio role artistName
        │
        ↓
     password

        │
        ↓
 accountStatus

        │
        ↓
artistRequestStatus
```

---

# 41. Tóm tắt

`User` là Entity trung tâm dùng để đại diện cho người dùng trong hệ thống.

Nó chứa các nhóm thông tin chính:

### Authentication

* `id`
* `username`
* `email`
* `password`

### Profile

* `fullName`
* `avatarUrl`
* `bio`

### Account

* `accountStatus`
* `role`

### Artist

* `artistRequestStatus`
* `artistName`

Entity `User` có quan hệ trực tiếp với bảng:

`users`

và được sử dụng xuyên suốt Backend:

**Controller → Service → Repository → User Entity → JPA/Hibernate → MySQL**.

Điểm quan trọng cần nhớ:

> **Entity `User` chủ yếu dùng để biểu diễn và ánh xạ dữ liệu User với Database. Business Logic nên nằm ở Service, còn thao tác Database nên thông qua Repository.**
