# MODULE USER — TÀI LIỆU NGHIỆP VỤ VÀ LUỒNG XỬ LÝ

> Dự án: **MySpotify Backend** (Spring Boot 4.1, Java 17 trong `pom.xml` — VPS chạy JDK 25, MySQL, Redis)
> Package gốc: `com.winhmm.myspotify`
> Cập nhật lần cuối: **08/11/2026** — khớp với code trên nhánh `main` sau Pull Request #11.
> Đối tượng đọc: thành viên mới trong nhóm (backend, frontend, tester), **không cần biết trước Spring Boot**.
> Tài liệu liên quan:
> - `docs/SONG_MODULE.md` — module bài hát (nghe nhạc, đăng nhạc, thể loại, lời bài hát, gợi ý).
> - `docs/PLAYLIST_MODULE.md` — module Playlist (mỗi tài khoản tự tạo playlist).
> - `docs/TEST_SONG_POSTMAN.md` — test module Song bằng Postman.

---

## MỤC LỤC

0. [Có gì mới trong bản cập nhật này](#0-có-gì-mới-trong-bản-cập-nhật-này)
1. [Cách đọc tài liệu này](#1-cách-đọc-tài-liệu-này)
2. [Module User giải quyết bài toán gì?](#2-module-user-giải-quyết-bài-toán-gì)
3. [Từ điển thuật ngữ](#3-từ-điển-thuật-ngữ)
4. [Danh sách chức năng](#4-danh-sách-chức-năng)
5. [Kiến trúc và vị trí các file](#5-kiến-trúc-và-vị-trí-các-file)
6. [Dữ liệu: MySQL và Redis](#6-dữ-liệu-mysql-và-redis)
7. [Bốn "trục trạng thái" của 1 tài khoản](#7-bốn-trục-trạng-thái-của-1-tài-khoản)
8. [Mã OTP — trái tim của module](#8-mã-otp--trái-tim-của-module)
9. [Đăng nhập, JWT và phiên đăng nhập](#9-đăng-nhập-jwt-và-phiên-đăng-nhập)
10. [Hành trình của 1 request đã đăng nhập](#10-hành-trình-của-1-request-đã-đăng-nhập)
11. [Luồng: đăng ký, xác thực, đăng nhập, quên mật khẩu](#11-luồng-đăng-ký-xác-thực-đăng-nhập-quên-mật-khẩu)
12. [Luồng: người dùng tự quản lý tài khoản](#12-luồng-người-dùng-tự-quản-lý-tài-khoản)
13. [Luồng: trở thành nghệ sĩ (ARTIST)](#13-luồng-trở-thành-nghệ-sĩ-artist)
14. [Luồng: Admin quản lý người dùng](#14-luồng-admin-quản-lý-người-dùng)
15. [Giới hạn tần suất (rate limit)](#15-giới-hạn-tần-suất-rate-limit)
16. [Bảo mật — tổng hợp](#16-bảo-mật--tổng-hợp)
17. [Xử lý lỗi](#17-xử-lý-lỗi)
18. [Ảnh hưởng của module User lên module Song và Playlist](#18-ảnh-hưởng-của-module-user-lên-module-song-và-playlist)
19. [Hướng dẫn riêng cho Frontend](#19-hướng-dẫn-riêng-cho-frontend)
20. [Hướng dẫn test nhanh bằng Postman](#20-hướng-dẫn-test-nhanh-bằng-postman)
21. [Cấu hình, môi trường và deploy](#21-cấu-hình-môi-trường-và-deploy)
22. [Giới hạn đã biết và hướng phát triển](#22-giới-hạn-đã-biết-và-hướng-phát-triển)
23. [Câu hỏi thường gặp](#23-câu-hỏi-thường-gặp)
24. [Phụ lục](#24-phụ-lục)

---

## 0. CÓ GÌ MỚI TRONG BẢN CẬP NHẬT NÀY

Bản trước viết ngày 18/10/2026. Từ đó tới nay code module User **chỉ có 1 thay đổi nghiệp vụ**, nhưng thay đổi này ảnh hưởng tới cả luồng đăng ký → đăng nhập:

| # | Thay đổi | Ảnh hưởng | Đọc ở mục |
|---|---|---|---|
| 1 | **Quyền vào web** (`accessStatus`): trục trạng thái thứ 4 của tài khoản. Tài khoản mới mặc định `REFUSE`; chủ dự án đổi sang `ACCEPT` bằng SQL thì mới đăng nhập được | Sau khi nhập OTP, đăng nhập báo `Account has not been approved yet` cho tới khi được cho phép. Thu hồi quyền → request tiếp theo bị 401 | 7.3a, 9.4, 11, 19.3, 20 |
| 2 | Sửa message lỗi gửi email cho đúng code: `Could not send OTP email, please try again later` (503) | Frontend so sánh đúng chuỗi | 8.4, 17.2 |
| 3 | Module **Playlist** mới: khóa / vô hiệu hóa / xóa tài khoản làm playlist công khai của người đó bị ẩn | | 18 |
| 4 | Module Song có thêm thể loại, canvas, lời bài hát, gợi ý; tài liệu Song đánh số lại (mục xử lý lỗi giờ là 22) | Sửa tham chiếu sang `SONG_MODULE.md` | 18 |
| 5 | **Deploy tự động** bằng GitHub Actions khi merge Pull Request vào `main` | | 21.5 |
| 6 | Sửa ví dụ file cấu hình: file `.properties` không cho ghi chú ở cuối dòng | Chép ví dụ cũ sẽ làm ứng dụng không khởi động được | 21.1 |

Số API của module User **không đổi** (vẫn 24 API).

---

## 1. CÁCH ĐỌC TÀI LIỆU NÀY

| Bạn là | Đọc theo thứ tự | Có thể bỏ qua |
|---|---|---|
| **Người mới hoàn toàn** | 0 → 2 → 3 → 4 → 7 → 8 → 9 → 11 → 12 → 13 → 14 | 10, 21 |
| **Frontend** | 0 → 2 → 4 → 7 → 9 → 19 → 11 → 12 → 13 → 17 | 5, 6, 10, 21 |
| **Tester** | 0 → 2 → 4 → 7 → 8 → 9 → 11 → 12 → 13 → 14 → 15 → 20 | 5, 21 |
| **Backend sắp sửa code** | Đọc hết, đặc biệt 5, 6, 7.3a, 8, 9, 10, 16 | |

Quy ước:
- `chữ như thế này` là tên trong code: class, method, cột database, trường JSON.
- **400 `"Email already exists"`** = server trả mã HTTP 400 kèm `{ "message": "Email already exists" }`.
- "Tài khoản đang hoạt động" = `accountStatus = ACTIVE`.
- "Được phép vào web" = `accessStatus = ACCEPT`.
- "Chủ dự án" = người quản lý server và database (khác với tài khoản ADMIN trong app).

---

## 2. MODULE USER GIẢI QUYẾT BÀI TOÁN GÌ?

### 2.1. Một câu

> Quản lý **ai được vào hệ thống**, **họ là ai** và **họ được làm gì**.

### 2.2. Câu chuyện minh họa

1. **Lan** tải app MySpotify, điền username `lan.nguyen`, email, mật khẩu → bấm Đăng ký.
2. Server tạo tài khoản ở trạng thái **chưa xác thực** và gửi **mã OTP 6 số** vào email của Lan.
3. Lan nhập mã → tài khoản được **kích hoạt**.
4. Lan thử đăng nhập → server báo **"tài khoản chưa được cho phép"**. Web chỉ dành cho người được chủ dự án mời (liên quan bản quyền nhạc). Lan nhắn chủ dự án; chủ dự án chạy 1 câu SQL đổi quyền vào web của Lan thành `ACCEPT`.
5. Lan đăng nhập lại, nhận về 1 **token**. Từ giờ mọi thao tác app gửi kèm token này.
6. Lan đăng nhập thêm trên laptop → điện thoại bị **đăng xuất tự động** (mỗi tài khoản chỉ được dùng ở 1 nơi).
7. Lan cập nhật họ tên, ảnh đại diện. Muốn đổi mật khẩu → nhập mật khẩu cũ → nhận OTP qua email → nhập OTP + mật khẩu mới.
8. Lan quên mật khẩu → bấm "Quên mật khẩu" → nhận OTP → đặt mật khẩu mới.
9. Lan là ca sĩ nghiệp dư → gửi **yêu cầu trở thành nghệ sĩ** với nghệ danh "Mây Chiều".
10. **Admin** xem danh sách yêu cầu, **duyệt** → Lan trở thành **ARTIST**, có thể đăng nhạc (module Song).
11. Một tài khoản khác đặt tên playlist công khai phản cảm → Admin **khóa** tài khoản đó. Người đó lập tức bị đăng xuất, không đăng nhập lại được, playlist công khai của họ tự ẩn với mọi người.
12. Lan muốn nghỉ dùng app → **xóa tài khoản** (cần nhập mật khẩu để xác nhận).

### 2.3. Ba vai trò (`Role`)

| Role | Có được bằng cách nào | Làm được gì thêm |
|---|---|---|
| **USER** | Mặc định khi đăng ký | Nghe nhạc, quản lý tài khoản, gửi yêu cầu làm nghệ sĩ |
| **ARTIST** | **Chỉ** qua: gửi yêu cầu → Admin duyệt | Mọi thứ của USER + đăng và quản lý bài hát |
| **ADMIN** | Admin khác gán, hoặc tạo trực tiếp trong database | Quản lý người dùng, duyệt nghệ sĩ, duyệt bài hát |

### 2.4. Tám quy tắc nghiệp vụ cốt lõi

1. **Email phải được xác thực bằng OTP** trước khi đăng nhập được.
2. **Mọi thao tác nhạy cảm** (đổi mật khẩu, đổi email, quên mật khẩu) đều cần **OTP gửi qua email**.
3. **Mỗi tài khoản chỉ đăng nhập ở 1 nơi.** Đăng nhập nơi mới → nơi cũ bị đăng xuất.
4. **Đổi mật khẩu / email, bị khóa, vô hiệu hóa, xóa** → bị đăng xuất **ngay lập tức** ở mọi nơi.
5. **ARTIST không được Admin gán thẳng**, chỉ có qua quy trình "gửi yêu cầu → duyệt".
6. **Không ai đụng được vào tài khoản ADMIN** (không khóa, không đổi role được).
7. **Không xóa cứng**: tài khoản bị xóa chỉ được đánh dấu `DELETED`.
8. **Chỉ người được chủ dự án cho phép (`accessStatus = ACCEPT`) mới đăng nhập được** (MỚI). Không có API nào cấp quyền này, kể cả API của Admin.

---

## 3. TỪ ĐIỂN THUẬT NGỮ

### 3.1. Đăng nhập và bảo mật

| Thuật ngữ | Giải thích dễ hiểu |
|---|---|
| **Authentication (xác thực)** | Kiểm tra "bạn là ai". Sai → **401** |
| **Authorization (phân quyền)** | Kiểm tra "bạn được làm gì". Không đủ quyền → **403** |
| **OTP** (One-Time Password) | Mã 6 số dùng 1 lần, gửi qua email, sống 5 phút |
| **JWT / Token** | Chuỗi dài bắt đầu bằng `eyJ...` nhận được khi đăng nhập. Như "thẻ ra vào" có chữ ký của server |
| **Chữ ký (signature)** | Phần cuối của JWT, tạo bằng khóa bí mật của server. Sửa 1 ký tự trong token → chữ ký sai → token vô hiệu |
| **Session (phiên)** | 1 lần đăng nhập. Mỗi phiên có 1 mã `sessionId` ngẫu nhiên, lưu trong Redis |
| **Single session (phiên đơn)** | Mỗi tài khoản chỉ có 1 phiên còn hiệu lực tại 1 thời điểm |
| **BCrypt** | Thuật toán **băm** mật khẩu 1 chiều. Database chỉ lưu bản băm, không ai (kể cả Admin) xem được mật khẩu gốc |
| **Băm (hash)** | Biến dữ liệu thành chuỗi khác, **không đảo ngược được**. Khác với mã hóa (có thể giải mã) |
| **Rate limit** | Giới hạn số request trong 1 khoảng thời gian |
| **Brute force** | Thử đoán liên tục (mật khẩu, mã OTP...). Rate limit và giới hạn số lần nhập OTP để chống việc này |
| **Quyền vào web (`accessStatus`)** | (MỚI) Cờ `ACCEPT` / `REFUSE` do **chủ dự án** đặt bằng SQL, quyết định ai được dùng web. Độc lập với `accountStatus` (mục 7.3a) |

### 3.2. Spring Boot

| Thuật ngữ | Giải thích |
|---|---|
| **Controller** | Nhận request HTTP, gọi Service, trả JSON |
| **Service** | Chứa logic nghiệp vụ |
| **Repository** | Đọc/ghi database |
| **Entity** | Class ánh xạ 1 bảng (`User` ↔ `users`) |
| **DTO** | Class chỉ để gửi/nhận dữ liệu qua API. VD `RegisterRequest`, `LoginResponse` |
| **Enum** | Kiểu chỉ có vài giá trị cố định (`Role`, `AccountStatus`...) |
| **Filter** | Đoạn code chạy **trước** Controller cho mọi request (kiểm tra token, rate limit...) |
| **`@Transactional`** | Gói nhiều thao tác database thành 1 khối: lỗi giữa chừng → **hoàn tác tất cả** |
| **`@PreAuthorize`** | Kiểm tra role trước khi chạy method |
| **`@Valid`** | Kiểm tra dữ liệu đầu vào theo annotation trong DTO |

### 3.3. Dữ liệu

| Thuật ngữ | Giải thích |
|---|---|
| **MySQL** | Database dạng bảng, lưu user và OTP |
| **Redis** | Database trong bộ nhớ, cực nhanh, lưu dữ liệu tạm có hạn sử dụng: phiên đăng nhập, số lần nhập sai OTP, bộ đếm rate limit |
| **TTL** | Thời gian sống của 1 key trong Redis, hết hạn tự xóa |
| **Soft delete** | Không xóa dòng, chỉ đánh dấu trạng thái `DELETED` |
| **SMTP** | Giao thức gửi email. Dự án gửi OTP qua SMTP của Gmail |

---

## 4. DANH SÁCH CHỨC NĂNG

### 4.1. Không cần đăng nhập — `/api/auth/**`

| # | Chức năng | API |
|---|---|---|
| A1 | Đăng ký | `POST /api/auth/register` |
| A2 | Xác thực OTP đăng ký | `POST /api/auth/verify-otp` |
| A3 | Đăng nhập | `POST /api/auth/login` |
| A4 | Quên mật khẩu (xin OTP) | `POST /api/auth/forgot-password` |
| A5 | Đặt lại mật khẩu bằng OTP | `POST /api/auth/reset-password` |

### 4.2. Cần đăng nhập, mọi role — `/api/users/**`

| # | Chức năng | API |
|---|---|---|
| U1 | Đăng xuất | `POST /api/users/logout` |
| U2 | Xem hồ sơ | `GET /api/users/me/profile` |
| U3 | Sửa họ tên | `PUT /api/users/me/profile` |
| U4 | Đổi ảnh đại diện | `POST /api/users/me/avatar` |
| U5 | Đổi mật khẩu — bước 1: xin OTP | `POST /api/users/me/change-password/request-otp` |
| U6 | Đổi mật khẩu — bước 2: xác nhận | `PUT /api/users/me/change-password` |
| U7 | Đổi email — bước 1: xin OTP | `POST /api/users/me/email/request-otp` |
| U8 | Đổi email — bước 2: xác nhận | `PUT /api/users/me/email` |
| U9 | Tự vô hiệu hóa tài khoản | `PUT /api/users/me/disable` |
| U10 | Xóa tài khoản | `PUT /api/users/me/delete` |
| U11 | Gửi yêu cầu làm nghệ sĩ | `POST /api/users/me/artist-request` |
| U12 | Sửa hồ sơ nghệ sĩ (ARTIST) | `PUT /api/users/me/artist-profile` |

### 4.3. Chỉ ADMIN — `/api/admin/**`

| # | Chức năng | API |
|---|---|---|
| AD1 | Xem tất cả người dùng | `GET /api/admin/users` |
| AD2 | Khóa tài khoản | `PUT /api/admin/users/{userId}/lock` |
| AD3 | Mở khóa tài khoản | `PUT /api/admin/users/{userId}/unlock` |
| AD4 | Xem yêu cầu làm nghệ sĩ đang chờ | `GET /api/admin/artist-requests` |
| AD5 | Duyệt yêu cầu làm nghệ sĩ | `PUT /api/admin/artist-requests/{userId}/approve` |
| AD6 | Từ chối yêu cầu làm nghệ sĩ | `PUT /api/admin/artist-requests/{userId}/reject` |
| AD7 | Đổi role (USER ↔ ADMIN) | `PUT /api/admin/users/{userId}/role` |

**Không có API** nào đổi quyền vào web (`accessStatus`) — chỉ chủ dự án đổi bằng SQL (mục 7.3a).

### 4.4. API của module khác có dùng tới tài khoản

Không thuộc module User nhưng nằm dưới đường dẫn có chữ `users` hoặc gắn với từng tài khoản, để khỏi tìm nhầm chỗ:

| API | Thuộc module | Tài liệu |
|---|---|---|
| `GET /api/users/me/history` (lịch sử nghe gần đây) | Song | `SONG_MODULE.md` mục 12 |
| `GET /api/playlists/me` (playlist của tôi) | Playlist | `PLAYLIST_MODULE.md` mục 8.2 |
| `GET /api/playlists/users/{userId}` (playlist công khai của 1 người) | Playlist | `PLAYLIST_MODULE.md` mục 8.3 |
| `GET /api/songs/artists/{artistId}` (trang nghệ sĩ: nghệ danh, bio, ảnh đại diện, bài hát) | Song | `SONG_MODULE.md` mục 11.6 |

---

## 5. KIẾN TRÚC VÀ VỊ TRÍ CÁC FILE

### 5.1. Kiến trúc 3 tầng

```
 Frontend / Postman
        │ HTTP + token
        ▼
┌───────────────────────────────────────────────────────────┐
│ FILTER CHAIN (chạy trước mọi Controller)                    │
│  CORS → JwtAuthenticationFilter → RateLimitFilter → phân quyền│
└───────────────────────────────────────────────────────────┘
        ▼
  CONTROLLER  (AuthController, UserController, AdminController)
        ▼
  SERVICE     (AuthService, UserService, AdminService, OtpService, SessionService, EmailService)
        ▼
  REPOSITORY  (UserRepository, OtpVerificationRepository)
        ▼
  MySQL (users, otp_verifications)     Redis (session, otp-attempt, rate)     Gmail SMTP
```

### 5.2. Danh sách file

```
src/main/java/com/winhmm/myspotify/
├── controller/
│   ├── AuthController.java         ← Đăng ký, OTP, đăng nhập, quên mật khẩu (không cần token)
│   ├── UserController.java         ← Người dùng tự quản lý tài khoản (cần token)
│   └── AdminController.java        ← Admin quản lý người dùng
├── service/
│   ├── AuthService.java            ← Logic đăng ký, xác thực, đăng nhập, đặt lại mật khẩu
│   ├── UserService.java            ← Logic hồ sơ, đổi mật khẩu, đổi email, vô hiệu hóa, xóa, yêu cầu nghệ sĩ
│   ├── AdminService.java           ← Logic khóa/mở khóa, duyệt nghệ sĩ, đổi role
│   ├── OtpService.java             ← Tạo, gửi, kiểm tra OTP
│   ├── EmailService.java           ← Gửi email qua SMTP
│   ├── SessionService.java         ← Tạo / kiểm tra / xóa phiên đăng nhập (Redis)
│   ├── RateLimitService.java       ← Đếm request (Redis)
│   └── FileStorageService.java     ← Lưu ảnh đại diện (dùng chung với module Song)
├── security/
│   ├── JwtUtil.java                ← Tạo và đọc token
│   ├── JwtAuthenticationFilter.java← Đọc token mỗi request, kiểm tra phiên
│   ├── RateLimitFilter.java        ← Chặn request quá tần suất
│   └── CustomUserDetailsService.java ← Nạp user từ database cho Spring Security
├── config/
│   ├── SecurityConfig.java         ← API nào công khai, thứ tự filter, lỗi 401/403
│   ├── CorsConfig.java             ← Cho phép frontend ở domain khác gọi API
│   └── WebMvcConfig.java           ← Phục vụ file /uploads/** (ảnh đại diện)
├── entity/
│   ├── User.java                   ← Bảng users
│   └── OtpVerification.java        ← Bảng otp_verifications
├── enums/
│   ├── Role.java                   ← USER, ARTIST, ADMIN
│   ├── AccountStatus.java          ← UNVERIFIED, ACTIVE, DISABLED, DELETED
│   ├── ArtistRequestStatus.java    ← NONE, PENDING, APPROVED, REJECTED
│   ├── OtpPurpose.java             ← REGISTER, RESET_PASSWORD, CHANGE_PASSWORD, CHANGE_EMAIL
│   └── AccessStatus.java           ← ACCEPT, REFUSE (MỚI — quyền vào web)
├── repository/
│   ├── UserRepository.java
│   └── OtpVerificationRepository.java
├── dto/  (request + response, xem Phụ lục 24.2)
└── exception/
    ├── GlobalExceptionHandler.java ← Chuyển mọi lỗi thành JSON
    └── EmailSendException.java     ← Lỗi gửi email OTP → 503 + hoàn tác thay đổi database
```

### 5.3. Ai gọi ai

```
AuthController ──► AuthService ──┬──► UserRepository
                                 ├──► OtpService ──┬──► OtpVerificationRepository
                                 │                 ├──► Redis (đếm lần nhập sai)
                                 │                 └──► EmailService ──► Gmail
                                 ├──► SessionService ──► Redis
                                 ├──► JwtUtil
                                 └──► PasswordEncoder (BCrypt)

UserController ──► UserService ──┬──► UserRepository, OtpService, SessionService
       │                         ├──► PasswordEncoder
       │                         └──► FileStorageService (ảnh đại diện)
       └──► SessionService (đăng xuất)

AdminController ─► AdminService ─┬──► UserRepository
                                 └──► SessionService (khóa → đá ra)
```

---

## 6. DỮ LIỆU: MYSQL VÀ REDIS

### 6.1. Bảng `users`

| Cột | Kiểu | Bắt buộc | Ý nghĩa |
|---|---|---|---|
| `id` | BIGINT tự tăng | Có | Mã người dùng |
| `username` | VARCHAR(50), **duy nhất** | Có | Tên đăng nhập hiển thị (đăng ký giới hạn 5–20 ký tự) |
| `email` | VARCHAR(100), **duy nhất** | Có | Dùng để **đăng nhập** và nhận OTP |
| `password` | VARCHAR(255) | Có | **Bản băm BCrypt**, không phải mật khẩu thật. Không bao giờ trả ra API (`@JsonIgnore`) |
| `full_name` | VARCHAR(100) | Không | Họ tên |
| `avatar_url` | VARCHAR(255) | Không | VD `/uploads/avatars/7c2e....jpg` |
| `account_status` | ENUM | Có | `UNVERIFIED` / `ACTIVE` / `DISABLED` / `DELETED`. Mặc định `UNVERIFIED` |
| `role` | ENUM | Có | `USER` / `ARTIST` / `ADMIN`. Mặc định `USER` |
| `artist_request_status` | ENUM | Có | `NONE` / `PENDING` / `APPROVED` / `REJECTED`. Mặc định `NONE` |
| `artist_name` | VARCHAR(100) | Không | Nghệ danh (điền khi gửi yêu cầu làm nghệ sĩ) |
| `bio` | VARCHAR(255) | Không | Giới thiệu nghệ sĩ |
| `access_status` | ENUM | Có | (MỚI) `ACCEPT` / `REFUSE`. Mặc định `REFUSE`. **Chỉ chủ dự án đổi bằng SQL**, không API nào đổi được, **không trả ra API** (mục 7.3a) |

Index: `idx_users_artist_request_status` trên `artist_request_status` → Admin lấy nhanh danh sách yêu cầu đang chờ.

Bản băm BCrypt trông như: `$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy`. Cùng 1 mật khẩu băm 2 lần ra 2 chuỗi khác nhau (có "muối" ngẫu nhiên), nên không tra ngược được bằng bảng có sẵn.

### 6.2. Bảng `otp_verifications`

| Cột | Ý nghĩa |
|---|---|
| `id` | Mã bản ghi |
| `user_id` → `users.id` | OTP của ai |
| `otp_code` | Mã 6 số |
| `purpose` | Dùng cho việc gì: `REGISTER`, `RESET_PASSWORD`, `CHANGE_PASSWORD`, `CHANGE_EMAIL` |
| `new_email` | Chỉ dùng cho `CHANGE_EMAIL`: email mới đang chờ xác nhận |
| `expired_at` | Thời điểm hết hạn (= lúc tạo + 5 phút) |
| `verified` | `true` = mã đã được dùng **hoặc** đã bị thay bằng mã mới → không dùng được nữa |

Index: `idx_otp_user_purpose_verified` trên `(user_id, purpose, verified)` → tìm nhanh "mã chưa dùng mới nhất của user X cho mục đích Y".

Mọi mã OTP **được giữ lại** trong bảng (kể cả đã dùng) để có lịch sử tra cứu.

### 6.3. Sơ đồ quan hệ

```
┌──────────────────────┐ 1          N ┌────────────────────────┐
│        users         │──────────────│   otp_verifications    │
│──────────────────────│   user_id    │────────────────────────│
│ id                   │              │ id                     │
│ username (unique)    │              │ user_id                │
│ email (unique)       │              │ otp_code               │
│ password (BCrypt)    │              │ purpose                │
│ account_status       │              │ new_email              │
│ role                 │              │ expired_at             │
│ artist_request_status│              │ verified               │
│ artist_name, bio     │              └────────────────────────┘
│ access_status (MỚI)  │
└──────────────────────┘
         │ 1
         │        (module Song)
         ├──────N── songs.artist_id               (bài mình là Artist chính)
         ├──────N── song_featured_artists.artist_id (bài mình được feat)
         │        (module Playlist)
         └──────N── playlists.user_id             (playlist mình tạo)
```

### 6.4. Redis

| Key | Giá trị | Sống bao lâu | Dùng để |
|---|---|---|---|
| `session:{email}` | `sessionId` (UUID) | 24 giờ (bằng hạn token) | Phiên đăng nhập hiện tại của tài khoản |
| `otp-attempt:{otpId}` | Số lần đã nhập | 5 phút | Giới hạn 5 lần nhập cho mỗi mã OTP |
| `rate:auth:{ip}` | Số request | 60 giây | Rate limit API `/api/auth/**` theo IP |
| `rate:user:{email}` | Số request | 60 giây | Rate limit theo tài khoản đã đăng nhập |
| `rate:ip:{ip}` | Số request | 60 giây | Rate limit request chưa đăng nhập (ngoài `/api/auth`) |

Redis bị xóa sạch (khởi động lại không lưu) → mọi người bị đăng xuất, phải đăng nhập lại. Không mất dữ liệu quan trọng nào.

---

## 7. BỐN "TRỤC TRẠNG THÁI" CỦA 1 TÀI KHOẢN

Mỗi tài khoản có **4 thuộc tính trạng thái độc lập**. Hiểu 4 trục này là hiểu 80% module.

```
 Trục 1: account_status       Trục 2: role       Trục 3: artist_request_status       Trục 4: access_status (MỚI)
 (tài khoản có dùng được?)    (được làm gì?)     (yêu cầu làm nghệ sĩ tới đâu?)      (chủ dự án có cho vào web?)
 do HỆ THỐNG quản lý          do ADMIN quản lý   do người dùng + ADMIN               do CHỦ DỰ ÁN quản lý (SQL)
```

### 7.1. Trục 1 — `AccountStatus`

| Giá trị | Nghĩa | Đăng nhập được? |
|---|---|---|
| `UNVERIFIED` | Đã đăng ký, chưa nhập OTP | ✘ `Account is not verified yet` |
| `ACTIVE` | Đang hoạt động | ✔ |
| `DISABLED` | Bị Admin **khóa**, hoặc người dùng **tự vô hiệu hóa** | ✘ `Account has been disabled` |
| `DELETED` | Người dùng đã xóa tài khoản | ✘ `Account has been deleted` |

```
     Đăng ký
        │
        ▼
 ┌─────────────┐  Nhập đúng OTP  ┌─────────────┐
 │ UNVERIFIED  │───────────────►│   ACTIVE    │◄──────────────┐
 └─────────────┘                 └─────────────┘               │
   │   ▲                          │          │                 │ Admin mở khóa
   └───┘                          │          │                 │
 Đăng ký lại cùng email           │          │ Admin khóa      │
 (đổi username/mật khẩu,          │          │ hoặc tự vô hiệu │
  gửi OTP mới)                    │          ▼                 │
                                  │    ┌─────────────┐         │
                                  │    │  DISABLED   │─────────┘
                                  │    └─────────────┘
                                  │ Tự xóa (cần mật khẩu)
                                  ▼
                            ┌─────────────┐
                            │   DELETED   │  điểm cuối, không quay lại
                            └─────────────┘
```

**Lưu ý thiết kế:** "Admin khóa" và "người dùng tự vô hiệu hóa" dùng **chung** trạng thái `DISABLED`. Hệ quả: người tự vô hiệu hóa muốn dùng lại phải **liên hệ Admin mở khóa** (không có API tự kích hoạt lại).

### 7.2. Trục 2 — `Role`

```
             Admin duyệt yêu cầu nghệ sĩ
   ┌──────┐ ─────────────────────────────► ┌────────┐
   │ USER │                                 │ ARTIST │
   └──────┘ ◄───────────────────────────── └────────┘
      │  ▲        Admin đổi role về USER       │
      │  │        (xóa nghệ danh, bio)          │
      │  │                                      │ Admin đổi role thành ADMIN
      │  │ ✘ không ai đổi được role             │ (xóa nghệ danh, bio)
      ▼  │   của ADMIN                          ▼
   ┌────────────────────────────────────────────────┐
   │                     ADMIN                       │
   └────────────────────────────────────────────────┘
      ▲ Admin đổi role USER → ADMIN
```

- USER → ARTIST: **chỉ** qua duyệt yêu cầu (mục 13). API đổi role từ chối gán ARTIST.
- USER → ADMIN, ARTIST → USER, ARTIST → ADMIN: qua API đổi role của Admin.
- ADMIN → bất cứ gì: **bị chặn**. Muốn hạ quyền 1 Admin phải sửa trực tiếp trong database.

### 7.3. Trục 3 — `ArtistRequestStatus`

| Giá trị | Nghĩa |
|---|---|
| `NONE` | Chưa từng gửi yêu cầu (hoặc đã bị reset khi đổi role) |
| `PENDING` | Đang chờ Admin xét |
| `APPROVED` | Đã được duyệt → role đã là ARTIST |
| `REJECTED` | Bị từ chối → **được gửi lại** |

```
 NONE ──gửi yêu cầu──► PENDING ──Admin duyệt──► APPROVED  (role = ARTIST)
                        │   ▲
              Admin     │   │ gửi lại
              từ chối   ▼   │
                       REJECTED
```

### 7.3a. Trục 4 — `AccessStatus` (quyền vào web) — MỚI

| Giá trị | Nghĩa | Đăng nhập được? |
|---|---|---|
| `REFUSE` | Chưa được chủ dự án cho phép. **Mặc định** của mọi tài khoản mới đăng ký | ✘ `Account has not been approved yet` |
| `ACCEPT` | Được phép dùng web | ✔ (nếu `accountStatus` cũng là `ACTIVE`) |

**Vì sao có trục này?** Web phát nhạc liên quan bản quyền, chỉ dành cho những người chủ dự án mời. Đây là "lớp cửa" thứ hai, **độc lập** với `accountStatus`:
- `accountStatus`: tài khoản đã xác thực email chưa, có bị khóa, bị xóa không → **hệ thống** tự quản lý (OTP, Admin khóa...).
- `accessStatus`: chủ dự án có cho người này vào web không → **chủ dự án** tự quyết.

Đăng nhập được khi **đủ cả 2**: `ACTIVE` **và** `ACCEPT`.

**Chỉ đổi được bằng SQL** (không có API nào, kể cả API của Admin — Admin cũng phải `ACCEPT` mới vào được):
```sql
-- Cho phép
UPDATE users SET access_status = 'ACCEPT' WHERE email = 'lan@gmail.com';
-- Thu hồi
UPDATE users SET access_status = 'REFUSE' WHERE email = 'lan@gmail.com';
-- Xem ai đang chờ được cho phép (đã xác thực OTP nhưng chưa ACCEPT)
SELECT id, username, email FROM users WHERE account_status = 'ACTIVE' AND access_status = 'REFUSE';
```

```
 Đăng ký ──► REFUSE ──(chủ dự án chạy SQL)──► ACCEPT
               ▲                                │
               └──────(chủ dự án thu hồi)───────┘
```

Hệ quả:
| Tình huống | Kết quả |
|---|---|
| Đăng ký, nhận OTP, nhập OTP | **Bình thường** (không bị chặn) |
| Đăng nhập khi đang `REFUSE` | 400 `Account has not been approved yet` (chỉ báo **sau khi** mật khẩu đúng và tài khoản ACTIVE) |
| Quên mật khẩu khi đang `REFUSE` | Vẫn đặt lại được mật khẩu, nhưng đăng nhập vẫn bị chặn |
| Đang đăng nhập thì bị đổi sang `REFUSE` | Request **tiếp theo** bị 401 `Unauthorized, please login` (phiên trong Redis không bị xóa, nhưng `CustomUserDetailsService` coi tài khoản là "disabled") |
| Được đổi lại `ACCEPT` | Token cũ (nếu còn hạn và phiên còn trong Redis) dùng lại được ngay; không thì đăng nhập lại |
| Đăng ký lại cùng email khi chưa xác thực | `accessStatus` giữ nguyên giá trị cũ |

`accessStatus` **không** có trong `UserProfileResponse` → frontend và Admin không xem được qua API. Đây là thông tin quản trị nội bộ.

### 7.4. Kết hợp 4 trục — ví dụ

| Tài khoản | account_status | role | artist_request_status | access_status | Giải thích |
|---|---|---|---|---|---|
| Lan vừa đăng ký | UNVERIFIED | USER | NONE | REFUSE | Chưa nhập OTP |
| Lan đã nhập OTP | ACTIVE | USER | NONE | REFUSE | Chờ chủ dự án cho phép, chưa đăng nhập được |
| Lan dùng bình thường | ACTIVE | USER | NONE | ACCEPT | |
| Lan chờ duyệt nghệ sĩ | ACTIVE | USER | PENDING | ACCEPT | Vẫn là USER cho tới khi được duyệt |
| Lan là nghệ sĩ | ACTIVE | ARTIST | APPROVED | ACCEPT | |
| Nghệ sĩ bị khóa | DISABLED | ARTIST | APPROVED | ACCEPT | Role giữ nguyên, nhưng không đăng nhập được, bài hát và playlist công khai bị ẩn |
| Nghệ sĩ bị thu hồi quyền vào web | ACTIVE | ARTIST | APPROVED | REFUSE | Không đăng nhập được, nhưng **bài hát vẫn hiển thị** với người nghe (điều kiện "đã phát hành" chỉ xét `accountStatus` và `role`) |
| Minh bị từ chối | ACTIVE | USER | REJECTED | ACCEPT | Có thể gửi lại |

---

## 8. MÃ OTP — TRÁI TIM CỦA MODULE

Toàn bộ nằm trong `OtpService`. **4 luồng** dùng OTP:

| Mục đích (`OtpPurpose`) | Dùng ở | Gửi tới |
|---|---|---|
| `REGISTER` | Kích hoạt tài khoản mới | Email đăng ký |
| `RESET_PASSWORD` | Quên mật khẩu | Email tài khoản |
| `CHANGE_PASSWORD` | Đổi mật khẩu khi đã đăng nhập | Email tài khoản |
| `CHANGE_EMAIL` | Đổi email | **Email MỚI** (để chứng minh email mới là của mình) |

### 8.1. Quy tắc

| Quy tắc | Giá trị | Vì sao |
|---|---|---|
| Độ dài | 6 chữ số (`000000`–`999999`) | 1 triệu khả năng, đủ khó đoán khi kết hợp giới hạn số lần nhập |
| Sinh mã | `SecureRandom` | Ngẫu nhiên an toàn, không đoán trước được (khác `Random` thường) |
| Thời hạn | **5 phút** | Ngắn để giảm rủi ro lộ mã |
| Dùng được mấy lần | **1 lần** | Dùng xong đánh dấu `verified = true` |
| Chờ giữa 2 lần xin mã | **60 giây** | Chống spam email |
| Số lần nhập tối đa / mã | **5 lần** | Chống đoán mã |
| Tách theo mục đích | Có | Mã đăng ký không dùng được để đặt lại mật khẩu |
| Xin mã mới | Mã cũ chưa dùng **bị vô hiệu** | Luôn chỉ có 1 mã còn hiệu lực cho mỗi mục đích |

### 8.2. Tạo và gửi mã (`createAndSend`)

```
1. Tìm mã CHƯA DÙNG mới nhất của user cho mục đích này
2. Nếu có và mới tạo chưa đủ 60 giây
        → lỗi "Please wait 60 seconds before requesting a new OTP"
3. Đánh dấu MỌI mã chưa dùng (cùng mục đích) là đã dùng → vô hiệu hóa
4. Sinh mã 6 số mới, hết hạn sau 5 phút, lưu vào otp_verifications
5. Gửi email
```

Thời điểm tạo mã được suy ra từ `expired_at − 5 phút` (bảng không có cột `created_at` riêng).

### 8.3. Kiểm tra mã (`verifyAndConsume`)

```
1. Lấy mã CHƯA DÙNG mới nhất của user cho mục đích này
        Không có → "Otp code does not exist"
2. Quá hạn → "Otp code has expired"
3. Tăng bộ đếm Redis "otp-attempt:{id}" thêm 1 (lần đầu thì đặt hạn 5 phút)
        Vượt 5 lần → "Too many wrong attempts, please request a new OTP"
4. Sai mã → "Otp code does not match"
5. Đúng → xóa bộ đếm, đánh dấu verified = true, trả về bản ghi OTP
```

**Vì sao đếm bằng Redis `INCREMENT`?** Lệnh này **nguyên tử**: 10 request gửi cùng lúc cũng được đếm đủ 10, không ai "lách" được bằng cách gửi song song. Nếu đọc số lần ra, cộng 1, rồi ghi lại, 10 request cùng lúc có thể đều đọc được số 0 và đều được thử.

**Vì sao 5 lần?** Đoán đúng 1 mã 6 số trong 5 lần: xác suất 5/1.000.000. Cộng thêm chờ 60 giây mỗi lần xin mã mới và rate limit, brute force gần như không khả thi.

### 8.4. Email gửi đi

```
Tiêu đề: MySpotify - Mã xác thực OTP
Người gửi: MySpotify <email cấu hình>

Xin chào,

Mã OTP của bạn là: 482913

Mã có hiệu lực trong 5 phút và chỉ sử dụng được 1 lần.

Nếu bạn không yêu cầu mã này, hãy bỏ qua email.

MySpotify
```

Gửi email thất bại (Gmail lỗi, sai mật khẩu ứng dụng, mất mạng, quá 5 giây) → **503** `"Could not send OTP email, please try again later"`.

Cách hoạt động:
```
EmailService.sendOtp
    try { gửi mail qua Gmail SMTP }
    catch (bất kỳ lỗi nào) {
        ghi log ERROR "Could not send OTP email to <email>: <lý do>"
        ném EmailSendException("Could not send OTP email, please try again later")
    }
        │
        ▼ (EmailSendException là RuntimeException)
@Transactional của luồng đang chạy (đăng ký, quên mật khẩu...) → HOÀN TÁC mọi thay đổi database lần đó
        │                                     (VD tài khoản vừa tạo, mã OTP vừa lưu)
        ▼
GlobalExceptionHandler → 503 { "message": "Could not send OTP email, please try again later" }
```
→ Người dùng chỉ cần thử lại, không bị "kẹt" tài khoản hay mã OTP dở dang. Lý do lỗi thật (sai App Password, Gmail chặn...) chỉ nằm trong log server, không trả về frontend.

---

## 9. ĐĂNG NHẬP, JWT VÀ PHIÊN ĐĂNG NHẬP

### 9.1. Token chứa gì?

JWT gồm 3 phần ngăn bởi dấu chấm: `header.payload.signature`. Phần payload (đã giải mã Base64) trông như:
```json
{
    "sub": "lan@gmail.com",
    "jti": "9b2f4c1e-7a3d-4e8b-a1c2-5d6e7f8a9b0c",
    "iat": 1760520000,
    "exp": 1760606400
}
```

| Trường | Nghĩa |
|---|---|
| `sub` (subject) | Email người dùng |
| `jti` (JWT ID) | `sessionId` của phiên đăng nhập này |
| `iat` | Thời điểm cấp |
| `exp` | Thời điểm hết hạn (cấp + **24 giờ**) |

⚠️ JWT **không được mã hóa**, chỉ được **ký**. Ai có token đều đọc được payload (dán vào jwt.io là thấy). Vì vậy token **không chứa** mật khẩu, role hay thông tin nhạy cảm. Chữ ký chỉ đảm bảo **không ai sửa được** nội dung.

**Role không nằm trong token** → mỗi request server đọc role mới nhất từ database. Admin đổi role → có hiệu lực **ngay request tiếp theo**, không cần đăng nhập lại.

### 9.2. Phiên đơn (single session)

```
Lan đăng nhập trên điện thoại
    → Redis: session:lan@gmail.com = "AAA"
    → Token điện thoại có jti = "AAA"

Lan đăng nhập trên laptop
    → Redis: session:lan@gmail.com = "BBB"   (GHI ĐÈ "AAA")
    → Token laptop có jti = "BBB"

Điện thoại gửi request với token jti = "AAA"
    → So với Redis: "AAA" ≠ "BBB" → 401
      "Your session has ended. Your account may have been logged in on another device"
```

**Vì sao cần phiên đơn?** JWT thuần không thể "thu hồi" trước hạn: đã cấp thì dùng được tới hết 24 giờ. Lưu `sessionId` trong Redis cho phép server **vô hiệu hóa token bất cứ lúc nào** bằng cách xóa / ghi đè key. Đồng thời ngăn chia sẻ tài khoản.

### 9.3. Khi nào phiên bị xóa (đăng xuất cưỡng bức)

| Sự kiện | Lý do |
|---|---|
| Đăng nhập ở nơi khác | Phiên đơn |
| Đăng xuất | Người dùng yêu cầu |
| Đổi mật khẩu | Nếu mật khẩu cũ bị lộ, kẻ gian đang dùng token cũ sẽ bị đá ra |
| Đặt lại mật khẩu (quên mật khẩu) | Như trên |
| Đổi email | Token cũ chứa email cũ, không còn đúng |
| Tự vô hiệu hóa / xóa tài khoản | Tài khoản không còn dùng được |
| Admin khóa tài khoản | Người bị khóa phải mất quyền truy cập **ngay**, không đợi token hết hạn |

Riêng **chủ dự án thu hồi quyền vào web** (`REFUSE` bằng SQL) **không** xóa phiên (SQL không đi qua code Java). Người đó vẫn bị chặn ngay từ request tiếp theo, vì mỗi request filter đều đọc lại `accessStatus` từ MySQL (mục 10), nhưng message là `Unauthorized, please login`.

### 9.4. Đăng nhập (`AuthService.login`)

```
1. Tìm user theo email           → không có: "Email or password is incorrect"
2. So mật khẩu với bản băm BCrypt → sai:     "Email or password is incorrect"
3. Kiểm tra trạng thái (CHỈ sau khi mật khẩu đúng):
        UNVERIFIED → "Account is not verified yet"
        DISABLED   → "Account has been disabled"
        DELETED    → "Account has been deleted"
4. Kiểm tra quyền vào web (MỚI):
        accessStatus ≠ ACCEPT → "Account has not been approved yet"
5. Tạo sessionId mới, ghi vào Redis (ghi đè phiên cũ)
6. Tạo JWT chứa email + sessionId
7. Trả { token, userId, username, role }
```

Bước 4 dùng `!= ACCEPT` (không dùng `== REFUSE`) để an toàn: nếu cột này vì lý do nào đó bị `NULL` thì cũng bị chặn. Bước 4 nằm **sau** bước 3 → người chưa xác thực OTP luôn thấy `Account is not verified yet` trước (đúng thứ tự họ cần làm).

**Vì sao sai email và sai mật khẩu trả cùng 1 message?** Nếu trả "Email không tồn tại", kẻ xấu biết được email nào đã đăng ký → đi dò mật khẩu đúng email đó.
**Vì sao kiểm tra trạng thái sau mật khẩu?** Để người không biết mật khẩu không dò được "tài khoản này đang bị khóa".

Kết quả:
```json
{
    "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOi...",
    "userId": 6,
    "username": "lan.nguyen",
    "role": "USER"
}
```

---

## 10. HÀNH TRÌNH CỦA 1 REQUEST ĐÃ ĐĂNG NHẬP

Ví dụ: `PUT /api/admin/users/42/lock` do Admin gửi.

```
① CORS — cho phép mọi origin (frontend ở domain nào cũng gọi được)

② JwtAuthenticationFilter
   ├─ Không có header "Authorization: Bearer ..." → bỏ qua (coi như khách)
   ├─ Token sai chữ ký / hết hạn → bỏ qua (coi như khách)
   ├─ Đọc email (sub) và sessionId (jti)
   ├─ Redis: session:{email} có đúng bằng sessionId không?
   │     Không → đánh dấu lỗi "Your session has ended..." → coi như khách
   ├─ Đọc user từ MySQL (CustomUserDetailsService)
   │     accountStatus ≠ ACTIVE hoặc accessStatus ≠ ACCEPT → coi như khách
   └─ Hợp lệ → ghi nhận: "người gọi là admin@gmail.com, quyền ROLE_ADMIN"

③ RateLimitFilter
   └─ Đếm request của admin@gmail.com trong 60 giây. Quá 100 → 429, dừng.

④ SecurityConfig
   ├─ /api/auth/**, /uploads/**, swagger, /actuator/health → ai cũng vào được
   └─ Còn lại → phải đăng nhập. Đang là "khách" → 401

⑤ @PreAuthorize("hasRole('ADMIN')") trên AdminController
   └─ Không phải ADMIN → 403

⑥ AdminController.lockAccount(42) → AdminService.lockAccount(42)
   ├─ User 42 không tồn tại → 400 "User does not exist"
   ├─ User 42 là ADMIN → 400 "Cannot lock an admin account"
   ├─ User 42 không ACTIVE → 400 "Only active accounts can be locked"
   ├─ accountStatus = DISABLED, lưu MySQL
   └─ Xóa session:{email của user 42} trong Redis → user 42 bị đá ra ngay

⑦ 200 { "message": "Account has been locked" }
```

**Hai thông báo 401:**
- Phiên trong Redis không khớp (đăng nhập nơi khác, đã đăng xuất, bị khóa, đổi mật khẩu / email, vô hiệu hóa, xóa tài khoản — mọi trường hợp xóa phiên ở mục 9.3): `"Your session has ended. Your account may have been logged in on another device"`
- Mọi trường hợp khác (không token, token sai chữ ký, token hết hạn, bị chủ dự án thu hồi quyền vào web...): `"Unauthorized, please login"`

> Vì message phiên hết hạn dùng chung cho nhiều tình huống, frontend nên hiển thị câu trung tính như "Phiên đăng nhập đã kết thúc, vui lòng đăng nhập lại".

**Thông báo 403:** `"You do not have permission to access this resource"`

---

## 11. LUỒNG: ĐĂNG KÝ, XÁC THỰC, ĐĂNG NHẬP, QUÊN MẬT KHẨU

Controller: `AuthController`. Service: `AuthService`. **Không cần token.** Rate limit: **10 request / 60 giây / IP**.

### 11.1. A1 — Đăng ký

**API:** `POST /api/auth/register`
```json
{ "username": "lan.nguyen", "email": "lan@gmail.com", "password": "matkhau123" }
```

| Trường | Quy tắc | Message khi sai |
|---|---|---|
| `username` | Bắt buộc, 5–20 ký tự, chỉ chữ cái, số, dấu chấm, gạch dưới | `Username is required` / `Username must be between 5 and 20 characters` / `Username can only contain letters, numbers, dot and underscore` |
| `email` | Bắt buộc, đúng định dạng, ≤ 100 ký tự | `Email is required` / `Invalid email` / `Email must not exceed 100 characters` |
| `password` | Bắt buộc, 6–72 ký tự | `Password is required` / `Password must be between 6 and 72 characters` |

> Vì sao tối đa **72**? BCrypt chỉ dùng 72 byte đầu của mật khẩu. Cho phép dài hơn sẽ khiến 2 mật khẩu khác nhau ở phần sau ký tự 72 bị coi là giống nhau.

**Các bước:**
```
Email đã tồn tại?
├─ CÓ, và tài khoản đó KHÔNG phải UNVERIFIED
│     → 400 "Email already exists"
├─ CÓ, và tài khoản đó đang UNVERIFIED (đăng ký rồi nhưng chưa nhập OTP)
│     ├─ Username mới khác username cũ VÀ đã có người dùng → "Username already exists"
│     ├─ Cập nhật username + mật khẩu mới
│     └─ Gửi OTP mới (phải cách lần trước ≥ 60 giây)
└─ KHÔNG
      ├─ Username đã có người dùng → "Username already exists"
      ├─ Tạo user: UNVERIFIED, role USER, accessStatus REFUSE (MỚI), mật khẩu băm BCrypt
      └─ Gửi OTP (REGISTER)
→ 200 "Registration successful, the account is awaiting OTP verification"
```

**Vì sao cho "đăng ký lại" với email UNVERIFIED?** Người dùng gõ sai mật khẩu, không nhận được mail, mã hết hạn... đều có thể đơn giản là đăng ký lại. Nếu chặn "Email already exists", email đó sẽ bị "kẹt" vĩnh viễn ở tài khoản chưa xác thực.

### 11.2. A2 — Xác thực OTP đăng ký

**API:** `POST /api/auth/verify-otp`
```json
{ "email": "lan@gmail.com", "otpCode": "482913" }
```

```
1. Tìm user theo email     → không có: "Email does not exist"
2. Không phải UNVERIFIED   → "Account is already verified"
3. Kiểm tra OTP (mục 8.3)
4. accountStatus = ACTIVE
→ 200 "Verification successful, the account has been activated"
```
`otpCode` không đủ 6 chữ số → 400 `OTP code must be 6 digits`.

**Nhận lại mã:** gọi lại A1 (đăng ký) với cùng email. Không có API "gửi lại OTP" riêng.

⚠️ **Xác thực OTP xong chưa đăng nhập được ngay** (MỚI): tài khoản đã `ACTIVE` nhưng `accessStatus` vẫn là `REFUSE`. Người dùng phải chờ chủ dự án cho phép (mục 7.3a). Message thành công vẫn là `Verification successful, the account has been activated` → frontend nên hiện thêm câu "Tài khoản sẽ dùng được sau khi được quản trị cho phép".

### 11.3. A3 — Đăng nhập

**API:** `POST /api/auth/login`
```json
{ "email": "lan@gmail.com", "password": "matkhau123" }
```
Chi tiết ở mục 9.4. Thêm lỗi mới: 400 `Account has not been approved yet` (chưa được chủ dự án cho phép vào web).

### 11.4. A4 + A5 — Quên mật khẩu

**Bước 1 — xin OTP:** `POST /api/auth/forgot-password`
```json
{ "email": "lan@gmail.com" }
```
```
1. Tìm user → không có: "Email does not exist"
2. Không ACTIVE → "Account is not active"
3. Gửi OTP (RESET_PASSWORD)
→ 200 "OTP code has been sent to your email"
```

**Bước 2 — đặt mật khẩu mới:** `POST /api/auth/reset-password`
```json
{ "email": "lan@gmail.com", "otpCode": "482913", "newPassword": "matkhaumoi456" }
```
```
1. Tìm user → không có: "Email does not exist"
2. Kiểm tra OTP (RESET_PASSWORD)
3. Lưu mật khẩu mới (băm BCrypt)
4. Xóa phiên → mọi thiết bị đang đăng nhập bị đăng xuất
→ 200 "Password has been reset successfully"
```

### 11.5. Sơ đồ tổng hợp

```
           ┌──────────┐  OTP qua email  ┌────────────┐
 Đăng ký ─►│UNVERIFIED│────────────────►│ verify-otp │──► ACTIVE (accessStatus vẫn REFUSE)
           └──────────┘                 └────────────┘                 │
                                                                       │ Chủ dự án chạy SQL
                                                                       ▼ access_status = 'ACCEPT'   (MỚI)
                                                              ACTIVE + ACCEPT
                                                                       │
                     ┌───────────────────── login ◄────────────────────┘
                     │                      ▲
                     ▼                      │
                 có token ──► dùng app      │ mật khẩu mới
                                            │
 Quên mật khẩu ──► forgot-password ──OTP──► reset-password

 (login khi còn REFUSE → 400 "Account has not been approved yet")
```

---

## 12. LUỒNG: NGƯỜI DÙNG TỰ QUẢN LÝ TÀI KHOẢN

Controller: `UserController`. Service: `UserService`.
- Đường dẫn `/api/users/...`, **mọi role**, **bắt buộc token**.
- `me` = người đang đăng nhập, id lấy từ token. **Không API nào nhận userId của người khác** → không ai sửa được tài khoản người khác.

### 12.1. U1 — Đăng xuất

**API:** `POST /api/users/logout` (không body)
Xóa phiên trong Redis → token hiện tại vô hiệu.
→ 200 `"Logout successful"`

Frontend cũng nên xóa token đã lưu ở máy.

### 12.2. U2 — Xem hồ sơ

**API:** `GET /api/users/me/profile`
```json
{
    "id": 6,
    "username": "lan.nguyen",
    "email": "lan@gmail.com",
    "fullName": "Nguyễn Thị Lan",
    "avatarUrl": "/uploads/avatars/7c2e....jpg",
    "accountStatus": "ACTIVE",
    "role": "USER",
    "artistRequestStatus": "NONE",
    "artistName": null,
    "bio": null
}
```
Không bao giờ có `password`.

### 12.3. U3 — Sửa họ tên

**API:** `PUT /api/users/me/profile`
```json
{ "fullName": "Nguyễn Thị Lan" }
```
- Bắt buộc, ≤ 100 ký tự (`Full name is required` / `Full name must not exceed 100 characters`).
- Trả hồ sơ mới (như U2).
- Username và email **không** sửa ở đây (username cố định; email đổi qua U7–U8).

### 12.4. U4 — Đổi ảnh đại diện

**API:** `POST /api/users/me/avatar` · **Body:** form-data, key **`file`** (loại File)

Dùng chung `FileStorageService.saveImage` với ảnh bìa bài hát:

| Kiểm tra | Message |
|---|---|
| Thiếu key `file` | `File is required` |
| File rỗng | `Image file is required` |
| > 2MB | `Image must not exceed 2MB` |
| Không phải JPG/PNG/WEBP | `Only JPG, PNG or WEBP images are allowed` |
| Ảnh giả (sai chữ ký file) | `Invalid or corrupted image file` |

```
1. Ghi nhớ ảnh cũ
2. Lưu ảnh mới → /uploads/avatars/<uuid>.<đuôi>
3. Cập nhật avatarUrl, lưu database
4. Lưu thành công → xóa file ảnh cũ
   Lưu thất bại  → xóa file ảnh MỚI, giữ nguyên ảnh cũ
```
Trả hồ sơ mới. Ảnh xem được tại `http://<server>/uploads/avatars/<file>` (không cần token).

### 12.5. U5 + U6 — Đổi mật khẩu (2 bước)

**Bước 1:** `POST /api/users/me/change-password/request-otp`
```json
{ "currentPassword": "matkhau123" }
```
```
1. Mật khẩu hiện tại sai → "Password does not match"
2. Gửi OTP (CHANGE_PASSWORD) tới email tài khoản
→ 200 "OTP code has been sent to your email"
```

**Bước 2:** `PUT /api/users/me/change-password`
```json
{ "otpCode": "482913", "newPassword": "matkhaumoi456" }
```
```
1. Kiểm tra OTP (CHANGE_PASSWORD)
2. Lưu mật khẩu mới (BCrypt)
3. Xóa phiên → phải đăng nhập lại bằng mật khẩu mới
→ 200 "Password has been changed successfully"
```

**Vì sao cần cả mật khẩu cũ VÀ OTP?** 2 lớp bảo vệ:
- Ai đó mượn điện thoại đang đăng nhập (có token) nhưng không biết mật khẩu → bị chặn ở bước 1.
- Ai đó biết mật khẩu (bị lộ) nhưng không vào được email → bị chặn ở bước 2.

### 12.6. U7 + U8 — Đổi email (2 bước)

**Bước 1:** `POST /api/users/me/email/request-otp`
```json
{ "newEmail": "lan.moi@gmail.com" }
```
```
1. Email mới trùng email hiện tại → "New email must be different from current email"
2. Email mới đã có người dùng → "Email already exists"
3. Gửi OTP (CHANGE_EMAIL) tới EMAIL MỚI, lưu email mới vào bản ghi OTP
→ 200 "OTP code has been sent to your new email"
```

**Bước 2:** `PUT /api/users/me/email`
```json
{ "otpCode": "482913" }
```
```
1. Kiểm tra OTP (CHANGE_EMAIL) → lấy được email mới từ bản ghi OTP
2. Kiểm tra lại email mới chưa bị ai chiếm (có thể có người đăng ký trong lúc chờ)
        → "Email already exists"
3. Xóa phiên (theo email cũ)
4. Đổi email
→ 200 "Email has been updated, please login again"
```

**Vì sao OTP gửi tới email MỚI?** Để chứng minh người dùng thật sự sở hữu email mới. Nếu gửi về email cũ, người dùng có thể gõ nhầm email mới (hoặc gõ email người khác) mà vẫn đổi được.

**Vì sao bước 2 không gửi lại `newEmail`?** Email mới đã được lưu cùng OTP ở bước 1. Nếu nhận lại từ request, kẻ gian có thể xin OTP cho email A rồi dùng OTP đó để đổi sang email B chưa được xác thực.

### 12.7. U9 — Tự vô hiệu hóa tài khoản

**API:** `PUT /api/users/me/disable`
```json
{ "password": "matkhau123" }
```
```
1. Tài khoản không ACTIVE → "Account is not active"
2. Mật khẩu sai → "Password does not match"
3. accountStatus = DISABLED, xóa phiên
→ 200 "Account has been disabled"
```
Muốn dùng lại → **liên hệ Admin mở khóa** (mục 14.3).

Trong thời gian vô hiệu hóa: bài hát (nếu là ARTIST) và playlist công khai của người đó bị ẩn với người khác, mở khóa thì hiện lại.

### 12.8. U10 — Xóa tài khoản

**API:** `PUT /api/users/me/delete` (dùng PUT vì chỉ đổi trạng thái, không xóa dữ liệu thật)
```json
{ "password": "matkhau123" }
```
```
1. Tài khoản không ACTIVE → "Account is not active"
2. Mật khẩu sai → "Password does not match"
3. accountStatus = DELETED, xóa phiên
→ 200 "Account has been deleted"
```
- **Không khôi phục được** qua API.
- Email và username **vẫn bị giữ** (không đăng ký lại được bằng email đó — `Email already exists`).
- Nếu là ARTIST: mọi bài hát biến mất khỏi người nghe (xem mục 18).
- Mọi playlist công khai của người đó **tự ẩn** với người khác (dữ liệu playlist vẫn còn trong database).

**Vì sao xóa mềm?** Giữ toàn vẹn dữ liệu liên quan (bài hát, OTP, lịch sử), tránh người khác đăng ký lại đúng email/username để mạo danh.

**Vì sao bắt nhập mật khẩu?** Hành động nghiêm trọng. Người mượn máy đang đăng nhập không được phép xóa tài khoản của chủ máy.

---

## 13. LUỒNG: TRỞ THÀNH NGHỆ SĨ (ARTIST)

### 13.1. Tổng quan

```
 USER gửi yêu cầu                 ADMIN xét
 (nghệ danh + giới thiệu)
        │
        ▼
 artistRequestStatus = PENDING ──► Duyệt   ──► role = ARTIST, status = APPROVED
        ▲                      └─► Từ chối ──► status = REJECTED, xóa nghệ danh/giới thiệu
        │                                           │
        └──────────── USER gửi lại ◄────────────────┘
```

Quyết định thiết kế: **Admin không được gán thẳng role ARTIST.** Mọi nghệ sĩ phải tự khai nghệ danh và được xét duyệt → có trách nhiệm với danh tính mình đăng ký.

### 13.2. U11 — Gửi yêu cầu

**API:** `POST /api/users/me/artist-request`
```json
{ "artistName": "Mây Chiều", "bio": "Ballad nhẹ nhàng cho buổi chiều." }
```
| Trường | Quy tắc |
|---|---|
| `artistName` | Bắt buộc, ≤ 100 ký tự (`Artist name is required` / `Artist name must not exceed 100 characters`) |
| `bio` | Không bắt buộc, ≤ 255 ký tự (`Bio must not exceed 255 characters`) |

```
1. Tài khoản không ACTIVE → "Account is not active"
2. Role không phải USER → "Only users with role USER can request to become an artist"
3. Đang PENDING hoặc đã APPROVED → "An artist request already exists or has been approved"
   (Chỉ gửi được khi NONE hoặc REJECTED)
4. Lưu artistName, bio, artistRequestStatus = PENDING
→ 200 "Artist request has been submitted"
```

Trong lúc chờ, tài khoản **vẫn là USER**, chưa đăng bài được.

### 13.3. AD4 — Admin xem yêu cầu đang chờ

**API:** `GET /api/admin/artist-requests`
Trả mảng hồ sơ (`UserProfileResponse`) của mọi tài khoản có `artistRequestStatus = PENDING`.

### 13.4. AD5 — Admin duyệt

**API:** `PUT /api/admin/artist-requests/{userId}/approve`
```
1. User không tồn tại → "User does not exist"
2. Không có yêu cầu PENDING → "No pending artist request for this user"
3. Role không phải USER → "Only users with role USER can become an artist"
4. Tài khoản không ACTIVE → "Account is not active"
5. artistRequestStatus = APPROVED, role = ARTIST
→ 200 "Artist request has been approved"
```
Có hiệu lực **ngay**: request tiếp theo của người đó đã có quyền ARTIST, không cần đăng nhập lại (vì role được đọc từ database mỗi request — mục 9.1).

### 13.5. AD6 — Admin từ chối

**API:** `PUT /api/admin/artist-requests/{userId}/reject`
```
1. Không có yêu cầu PENDING → "No pending artist request for this user"
2. artistRequestStatus = REJECTED, xóa artistName và bio
→ 200 "Artist request has been rejected"
```
Người dùng thấy `artistRequestStatus: "REJECTED"` trong hồ sơ và có thể gửi lại với nghệ danh khác.

> Hiện tại từ chối **không kèm lý do** (khác với từ chối bài hát). Xem mục 22.

### 13.6. U12 — Nghệ sĩ sửa hồ sơ nghệ sĩ

**API:** `PUT /api/users/me/artist-profile`
```json
{ "artistName": "Mây Chiều Official", "bio": "Giới thiệu mới" }
```
- Chỉ role **ARTIST** → khác: 400 `"Only artists can update artist profile"`.
- **Không cần duyệt lại** khi đổi nghệ danh.
- Trả hồ sơ mới.
- Nghệ danh mới hiện ngay trên mọi bài hát (module Song đọc nghệ danh trực tiếp từ bảng users).

---

## 14. LUỒNG: ADMIN QUẢN LÝ NGƯỜI DÙNG

Controller: `AdminController` (`@PreAuthorize("hasRole('ADMIN')")`). Service: `AdminService`.
Đường dẫn `/api/admin/...`. USER / ARTIST gọi → **403**.

### 14.1. AD1 — Xem tất cả người dùng

**API:** `GET /api/admin/users`
Trả **mảng** `UserProfileResponse` của **mọi** tài khoản (mọi trạng thái, mọi role). Không phân trang (xem mục 22).

### 14.2. AD2 — Khóa tài khoản

**API:** `PUT /api/admin/users/{userId}/lock`
```
1. User không tồn tại → "User does not exist"
2. Là ADMIN → "Cannot lock an admin account"
3. Không ACTIVE → "Only active accounts can be locked"
4. accountStatus = DISABLED
5. Xóa phiên → người bị khóa bị đá ra NGAY LẬP TỨC
→ 200 "Account has been locked"
```
Hệ quả ở module khác: nếu là ARTIST thì mọi bài hát bị ẩn; mọi playlist công khai của người đó bị ẩn với người khác (mục 18). Mở khóa → tất cả hiện lại.

Khóa (Admin) khác thu hồi quyền vào web (chủ dự án) thế nào?

| | Admin khóa (`DISABLED`) | Chủ dự án thu hồi (`REFUSE`) |
|---|---|---|
| Ai làm | Tài khoản ADMIN, qua API | Chủ dự án, bằng SQL |
| Bị đá ra | Ngay (xóa phiên), message `Your session has ended...` | Từ request tiếp theo, message `Unauthorized, please login` |
| Đăng nhập lại | `Account has been disabled` | `Account has not been approved yet` |
| Bài hát (nếu là ARTIST), playlist công khai | **Bị ẩn** | **Vẫn hiển thị** |
| Khóa được ADMIN không | Không | Có (chủ dự án quản lý cả Admin) |

### 14.3. AD3 — Mở khóa

**API:** `PUT /api/admin/users/{userId}/unlock`
```
1. Không phải DISABLED → "Only disabled accounts can be unlocked"
2. accountStatus = ACTIVE
→ 200 "Account has been unlocked"
```
Áp dụng cho cả tài khoản bị Admin khóa lẫn tài khoản **tự vô hiệu hóa**. Không mở được tài khoản `DELETED` hay `UNVERIFIED`.

### 14.4. AD7 — Đổi role

**API:** `PUT /api/admin/users/{userId}/role`
```json
{ "role": "ADMIN" }
```
```
1. role = null → "Role must not be null"   (role không hợp lệ, VD "BOSS" → "Invalid request data")
2. role = ARTIST → "ARTIST role can only be granted by approving an artist request"
3. Người được đổi đang là ADMIN → "Cannot change role of an admin account"
4. Tài khoản DELETED hoặc UNVERIFIED → "Cannot assign role to a deleted or unverified account"
5. Đã có đúng role đó → "User already has this role"
6. Nếu đang là ARTIST hoặc có yêu cầu nghệ sĩ PENDING:
       xóa artistName, bio, artistRequestStatus = NONE
7. Đặt role mới
→ 200 "Role has been assigned successfully"
```

Các chuyển đổi hợp lệ:

| Từ | Sang | Được? | Ghi chú |
|---|---|---|---|
| USER | ADMIN | ✔ | Yêu cầu nghệ sĩ đang chờ (nếu có) bị hủy |
| ARTIST | USER | ✔ | **Mất nghệ danh, mọi bài hát biến mất khỏi người nghe** |
| ARTIST | ADMIN | ✔ | Như trên |
| bất kỳ | ARTIST | ✘ | Chỉ qua duyệt yêu cầu |
| ADMIN | bất kỳ | ✘ | Bảo vệ tài khoản Admin |

**Vì sao không cho đổi role của Admin?** Tránh 1 Admin (hoặc kẻ chiếm được tài khoản Admin) hạ quyền tất cả Admin khác để độc chiếm hệ thống. Cũng tránh Admin vô tình tự hạ quyền mình.

### 14.5. Admin đầu tiên tạo thế nào?

Không có API đăng ký Admin. Admin đầu tiên được tạo bằng cách:
1. Đăng ký + xác thực 1 tài khoản bình thường.
2. Sửa trực tiếp trong MySQL (nhớ cho phép vào web luôn, nếu không Admin cũng không đăng nhập được):
   ```sql
   UPDATE users SET role = 'ADMIN', access_status = 'ACCEPT' WHERE email = 'admin@gmail.com';
   ```
Các Admin sau có thể được Admin này nâng quyền qua AD7.

---

## 15. GIỚI HẠN TẦN SUẤT (RATE LIMIT)

`RateLimitFilter` + `RateLimitService`, dùng bộ đếm Redis.

| Nhóm | Đếm theo | Giới hạn | Key Redis |
|---|---|---|---|
| `/api/auth/**` (đăng ký, đăng nhập, OTP...) | **IP** | **10 request / 60 giây** | `rate:auth:{ip}` |
| Mọi API khác, đã đăng nhập | **Tài khoản (email)** | **100 request / 60 giây** | `rate:user:{email}` |
| Mọi API khác, chưa đăng nhập | **IP** | 100 request / 60 giây | `rate:ip:{ip}` |
| `/uploads/**`, swagger, actuator, `OPTIONS` | — | Không giới hạn | — |

Vượt giới hạn:
```
HTTP 429
Retry-After: 37
{ "message": "Too many requests. Please try again in 37 seconds" }
```

Cách đếm: "cửa sổ cố định" — request đầu tiên tạo bộ đếm sống 60 giây, mỗi request cộng 1 (`INCREMENT` nguyên tử), hết 60 giây bộ đếm tự xóa.

**Vì sao API auth giới hạn chặt hơn (10)?** Đây là nơi kẻ xấu thử đoán mật khẩu, spam đăng ký, spam gửi email OTP. 10 lần/phút là quá đủ cho người dùng thật.

**Vì sao auth đếm theo IP còn API khác đếm theo tài khoản?** Lúc gọi API auth chưa có token nên chỉ biết IP. Khi đã đăng nhập, đếm theo tài khoản công bằng hơn (nhiều người chung wifi trường học không ảnh hưởng nhau).

**Redis lỗi** → cho qua (fail-open): thà mất chống spam vài phút còn hơn cả hệ thống không dùng được.

---

## 16. BẢO MẬT — TỔNG HỢP

| Mối đe dọa | Cách hệ thống chặn |
|---|---|
| Lộ mật khẩu khi database bị lộ | Chỉ lưu bản băm BCrypt, có muối ngẫu nhiên |
| Lộ mật khẩu qua API | `password` có `@JsonIgnore`, mọi API trả DTO không có trường password |
| Dò email đã đăng ký qua đăng nhập | Sai email / sai mật khẩu trả cùng 1 message |
| Dò trạng thái tài khoản | Kiểm tra trạng thái **sau** khi mật khẩu đúng |
| Đoán mật khẩu liên tục | Rate limit 10 / phút / IP trên `/api/auth/**` |
| Đoán mã OTP | 6 số, 5 phút, tối đa 5 lần / mã, chờ 60 giây giữa 2 lần xin mã |
| Spam email OTP | Chờ 60 giây + rate limit |
| Dùng OTP sai mục đích | OTP tách theo `purpose` |
| Dùng lại OTP | Dùng 1 lần, xin mã mới thì mã cũ bị vô hiệu |
| Sửa token để nâng quyền | Token được ký; role không nằm trong token mà đọc từ database |
| Token bị đánh cắp | Hết hạn 24 giờ; bị vô hiệu ngay khi đăng nhập lại, đổi mật khẩu, đăng xuất |
| Chia sẻ tài khoản | Phiên đơn |
| Tài khoản bị khóa vẫn dùng token cũ | Khóa → xóa phiên; filter còn kiểm tra `accountStatus` mỗi request |
| Người lạ tự đăng ký để dùng web | (MỚI) Tài khoản mới mặc định `accessStatus = REFUSE`, chỉ chủ dự án cho phép bằng SQL; filter kiểm tra `accessStatus` mỗi request |
| Sửa tài khoản người khác | Mọi API `/me/...` lấy id từ token, không nhận userId |
| Đổi email sang email không phải của mình | OTP gửi tới email mới, email mới lưu cùng OTP |
| Người mượn máy xóa / đổi mật khẩu | Cần mật khẩu hiện tại |
| Chiếm quyền Admin | Không gán được ARTIST thẳng, không đổi role / khóa được Admin |
| Upload ảnh độc hại | Kiểm tra định dạng, dung lượng, chữ ký file; tên file UUID |
| SQL injection | Spring Data JPA dùng truy vấn tham số hóa |

---

## 17. XỬ LÝ LỖI

### 17.1. Định dạng

Mọi lỗi trả JSON:
```json
{ "message": "Email already exists" }
```

### 17.2. Bảng loại lỗi → mã HTTP

| Loại lỗi | Mã | Message |
|---|---|---|
| Vi phạm nghiệp vụ (`IllegalArgumentException`) | 400 | Theo nội dung (xem Phụ lục 24.3) |
| Sai validation DTO | 400 | Message trong annotation, VD `Invalid email` |
| JSON sai cú pháp / sai kiểu / role không tồn tại | 400 | `Invalid request data` |
| Thiếu key file (avatar) | 400 | `File is required` |
| Tham số URL sai kiểu (`/users/abc/lock`) | 400 | `Invalid parameter: userId` |
| Chưa đăng nhập / token sai / hết hạn / bị chủ dự án thu hồi quyền vào web | 401 | `Unauthorized, please login` |
| Phiên đã bị xóa: đăng nhập nơi khác, đã đăng xuất, bị khóa, đổi mật khẩu / email, vô hiệu hóa, xóa tài khoản | 401 | `Your session has ended. Your account may have been logged in on another device` |
| Sai role | 403 | `You do not have permission to access this resource` |
| Trùng dữ liệu ở database (2 người đăng ký cùng username cùng lúc) | 409 | `Data already exists` |
| Ảnh vượt giới hạn upload tổng | 413 | `File is too large` |
| Rate limit | 429 | `Too many requests. Please try again in X seconds` |
| Gửi email OTP thất bại | 503 | `Could not send OTP email, please try again later` |
| Lỗi không lường trước | 500 | `Internal server error` — **là bug** |

### 17.3. Vì sao có lỗi 409 dù đã kiểm tra "Username already exists"?

```
Request A: kiểm tra "lan.nguyen" chưa có ──┐
Request B: kiểm tra "lan.nguyen" chưa có ──┤ cùng lúc
Request A: INSERT lan.nguyen ✔            │
Request B: INSERT lan.nguyen ✘ ───────────┘ vi phạm UNIQUE của database
```
Kiểm tra bằng code chỉ là lớp 1. Ràng buộc `UNIQUE` trong database là lớp cuối, chắc chắn không bao giờ có 2 tài khoản trùng. Lỗi đó được trả 409.

---

## 18. ẢNH HƯỞNG CỦA MODULE USER LÊN MODULE SONG VÀ PLAYLIST

### 18.1. Lên module Song

| Sự kiện ở module User | Hệ quả ở module Song |
|---|---|
| Admin duyệt yêu cầu nghệ sĩ | Người đó dùng được `/api/artist/**`: đăng bài, quản lý bài, canvas, lời bài hát |
| Admin **khóa** Artist | Mọi bài của Artist **biến mất** khỏi người nghe ở **mọi nơi**: danh sách, tìm kiếm, thể loại, chi tiết, trang nghệ sĩ, lời bài hát, gợi ý, lịch sử nghe, playlist của người khác. Bài mới của họ không duyệt được (`Artist account is not active`). Không feat được họ |
| Admin **mở khóa** | Mọi bài **xuất hiện lại**, lượt nghe giữ nguyên |
| Artist tự vô hiệu hóa / xóa tài khoản | Như bị khóa (vĩnh viễn nếu xóa) |
| Admin đổi ARTIST → USER / ADMIN | Như bị khóa: bài không còn hiển thị vì điều kiện "Artist chính phải là ARTIST". Mất nghệ danh. Đổi lại ARTIST phải gửi yêu cầu mới |
| Artist đổi nghệ danh | Mọi bài (kể cả bài được feat) hiện nghệ danh mới ngay |
| Artist được feat bị khóa | Bài **vẫn hiển thị**, nhưng tên người đó bị ẩn khỏi `featuredArtists` |
| Chủ dự án thu hồi quyền vào web của Artist (`REFUSE`) | Artist không đăng nhập được, nhưng bài **vẫn hiển thị** với người nghe (điều kiện "đã phát hành" không xét `accessStatus`) |

Chi tiết xem `docs/SONG_MODULE.md` mục 8.4 (điều kiện "đã phát hành") và mục 22 (xử lý lỗi).

### 18.2. Lên module Playlist (MỚI)

Mọi tài khoản (USER, ARTIST, ADMIN) đều tạo được playlist (`docs/PLAYLIST_MODULE.md`).

| Sự kiện ở module User | Hệ quả ở module Playlist |
|---|---|
| Admin **khóa** / người dùng **tự vô hiệu hóa** / **xóa** tài khoản | Mọi playlist công khai của người đó **tự ẩn** với người khác: không có trong `GET /api/playlists/users/{userId}`, mở thẳng bằng id → `Playlist does not exist`. Dữ liệu playlist **không bị xóa** |
| Admin **mở khóa** | Playlist công khai **hiện lại** |
| Artist bị khóa | Bài của Artist đó tự biến mất khỏi **mọi playlist** của mọi người; mở khóa thì hiện lại đúng vị trí cũ |
| Chủ dự án thu hồi quyền vào web | Người đó không đăng nhập được, nhưng playlist công khai **vẫn hiển thị** với người khác |
| Sửa họ tên (`fullName`) | Không ảnh hưởng: playlist hiển thị `username` của chủ (`ownerName`), không hiển thị họ tên |

Khi xóa **hẳn** 1 tài khoản bằng SQL (không qua API): phải xóa `playlist_songs` của các playlist của họ → `playlists` → rồi mới xóa `users`, nếu không MySQL báo lỗi khóa ngoại (xem `PLAYLIST_MODULE.md` mục 15.3).

---

## 19. HƯỚNG DẪN RIÊNG CHO FRONTEND

### 19.1. Lưu token

```js
const res = await axios.post(`${BASE_URL}/api/auth/login`, { email, password });
localStorage.setItem("token", res.data.token);
localStorage.setItem("user", JSON.stringify({
    id: res.data.userId, username: res.data.username, role: res.data.role
}));
```

Gắn token cho mọi request:
```js
axios.interceptors.request.use(config => {
    const token = localStorage.getItem("token");
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
});
```

### 19.2. Xử lý 401 toàn cục

```js
axios.interceptors.response.use(
    res => res,
    err => {
        if (err.response?.status === 401) {
            const msg = err.response.data?.message || "";
            localStorage.removeItem("token");
            if (msg.startsWith("Your session has ended")) {
                // Có thể do: đăng nhập nơi khác, bị khóa, vừa đổi mật khẩu...
                alert("Phiên đăng nhập đã kết thúc, vui lòng đăng nhập lại.");
            }
            // "Unauthorized, please login": token hết hạn, token sai,
            // hoặc bị chủ dự án thu hồi quyền vào web → cứ về trang đăng nhập
            window.location.href = "/login";
        }
        return Promise.reject(err);
    }
);
```

### 19.3. Màn hình đăng ký → OTP

```
[Form đăng ký] ──200──► [Màn nhập OTP, đếm ngược 5:00]
                              │
                              ├─ Nút "Gửi lại mã" (ẩn 60 giây đầu)
                              │     → gọi lại /api/auth/register với cùng dữ liệu
                              │
                              └─ Nhập 6 số → /api/auth/verify-otp ──200──► [Màn "Chờ cho phép"] ──► [Đăng nhập]
```

Màn **"Chờ cho phép"** (MỚI): sau khi xác thực OTP thành công, tài khoản vẫn chưa đăng nhập được cho tới khi chủ dự án cho phép (mục 7.3a). Nên hiện:
> "Xác thực email thành công! Tài khoản của bạn sẽ dùng được sau khi được quản trị viên cho phép. Vui lòng liên hệ quản trị viên."

Kèm nút "Đến trang đăng nhập" để người dùng thử lại sau.

Hiển thị lỗi đăng nhập:

| message | Hiển thị gợi ý |
|---|---|
| `Email or password is incorrect` | Email hoặc mật khẩu không đúng |
| `Account is not verified yet` | Tài khoản chưa xác thực email → chuyển sang màn nhập OTP (gọi lại đăng ký để nhận mã mới) |
| `Account has not been approved yet` | (MỚI) Tài khoản chưa được quản trị viên cho phép sử dụng. Vui lòng liên hệ quản trị viên |
| `Account has been disabled` | Tài khoản đã bị khóa hoặc vô hiệu hóa. Liên hệ quản trị viên để mở lại |
| `Account has been deleted` | Tài khoản đã bị xóa |
| `Could not send OTP email, please try again later` | Không gửi được email, vui lòng thử lại sau (dùng ở màn đăng ký, quên mật khẩu, đổi mật khẩu, đổi email) |

Hiển thị lỗi OTP:

| message | Hiển thị gợi ý |
|---|---|
| `Otp code does not match` | Mã không đúng, vui lòng kiểm tra lại |
| `Otp code has expired` | Mã đã hết hạn, vui lòng gửi lại mã |
| `Too many wrong attempts, please request a new OTP` | Nhập sai quá nhiều lần, vui lòng gửi lại mã |
| `Otp code does not exist` | Vui lòng bấm gửi mã |
| `Please wait 60 seconds before requesting a new OTP` | Vui lòng đợi trước khi gửi lại |

### 19.4. Sau các thao tác bắt đăng nhập lại

Sau **đổi mật khẩu**, **đặt lại mật khẩu**, **đổi email**, **vô hiệu hóa**, **xóa tài khoản**: phiên đã bị xóa ở server → frontend chủ động xóa token và chuyển về trang đăng nhập (đừng đợi request sau trả 401).

### 19.5. Hiển thị theo role và trạng thái nghệ sĩ

Gọi `GET /api/users/me/profile` khi mở app (role có thể đã thay đổi từ lúc đăng nhập):

| role | artistRequestStatus | Hiển thị |
|---|---|---|
| USER | NONE | Nút "Trở thành nghệ sĩ" |
| USER | PENDING | "Yêu cầu đang chờ duyệt" (khóa nút) |
| USER | REJECTED | "Yêu cầu bị từ chối" + nút "Gửi lại" |
| ARTIST | APPROVED | Menu "Quản lý bài hát", "Hồ sơ nghệ sĩ" |
| ADMIN | — | Menu "Quản trị" |

⚠️ Ẩn/hiện menu ở frontend **chỉ để giao diện gọn**. Bảo mật thật nằm ở backend — kể cả khi ai đó tự mở menu Admin, API vẫn trả 403.

### 19.6. Ảnh đại diện

```js
const form = new FormData();
form.append("file", fileInput.files[0]);     // key là "file"
await axios.post(`${BASE_URL}/api/users/me/avatar`, form);
// hiển thị: BASE_URL + profile.avatarUrl  (avatarUrl có thể null → ảnh mặc định)
```

---

## 20. HƯỚNG DẪN TEST NHANH BẰNG POSTMAN

Base URL: `http://157.66.100.87:8080`. Dùng email thật để nhận OTP (mẹo Gmail: `ten+test1@gmail.com` vẫn về hộp thư `ten@gmail.com`).

### 20.1. Luồng chính

| # | Request | Mong đợi |
|---|---|---|
| 1 | `POST /api/auth/register` | 200, nhận email OTP |
| 2 | `POST /api/auth/login` (chưa xác thực) | 400 `Account is not verified yet` |
| 3 | `POST /api/auth/verify-otp` mã sai | 400 `Otp code does not match` |
| 4 | `POST /api/auth/verify-otp` mã đúng | 200 |
| 5 | `POST /api/auth/verify-otp` lần nữa | 400 `Account is already verified` |
| 6 | `POST /api/auth/login` (chưa được cho phép) | 400 `Account has not been approved yet` (MỚI) |
| 7 | Chủ dự án chạy SQL: `UPDATE users SET access_status = 'ACCEPT' WHERE email = '...';` | 1 dòng được cập nhật |
| 8 | `POST /api/auth/login` | 200, lưu token |
| 9 | `GET /api/users/me/profile` | 200, `accountStatus: ACTIVE`, không có `password`, không có `accessStatus` |
| 10 | `PUT /api/users/me/profile` | 200, `fullName` mới |
| 11 | `POST /api/users/me/avatar` (key `file`) | 200, có `avatarUrl` |
| 12 | Login lại → dùng token cũ của bước 8 | 401 `Your session has ended...` |
| 13 | `POST /api/users/me/artist-request` | 200 |
| 14 | Gửi lại yêu cầu | 400 `An artist request already exists or has been approved` |
| 15 | Admin: `GET /api/admin/artist-requests` | Có user ở bước 13 |
| 16 | Admin: `PUT /api/admin/artist-requests/{id}/approve` | 200 |
| 17 | User: `GET /api/users/me/profile` (không cần login lại) | `role: ARTIST`, `artistRequestStatus: APPROVED` |
| 18 | Admin: `PUT /api/admin/users/{id}/lock` | 200 |
| 19 | User: gọi bất kỳ API nào | 401 `Your session has ended...` |
| 20 | User: login | 400 `Account has been disabled` |
| 21 | Admin: `PUT /api/admin/users/{id}/unlock` | 200, user login lại được |
| 22 | Chủ dự án chạy SQL đổi user về `REFUSE`, user gọi `GET /api/users/me/profile` bằng token đang dùng | 401 `Unauthorized, please login` (MỚI) |
| 23 | User: login | 400 `Account has not been approved yet` |
| 24 | Chủ dự án đổi lại `ACCEPT`, user login | 200 |
| 25 | `POST /api/users/logout` → dùng lại token | 401 |

### 20.2. Các trường hợp lỗi nên thử

| Tình huống | Mong đợi |
|---|---|
| Đăng ký username `ab` | `Username must be between 5 and 20 characters` |
| Đăng ký username `lan nguyen` (có dấu cách) | `Username can only contain letters, numbers, dot and underscore` |
| Đăng ký email `abc` | `Invalid email` |
| Đăng ký email đã ACTIVE | `Email already exists` |
| Đăng ký 2 lần liên tiếp < 60 giây với email chưa xác thực | `Please wait 60 seconds before requesting a new OTP` |
| Nhập sai OTP 6 lần | Lần 6: `Too many wrong attempts, please request a new OTP` |
| Đợi > 5 phút rồi nhập OTP | `Otp code has expired` |
| Login sai mật khẩu | `Email or password is incorrect` |
| Login email không tồn tại | `Email or password is incorrect` (giống hệt) |
| Gửi > 10 request vào `/api/auth/login` trong 1 phút | 429 |
| Đổi mật khẩu với mật khẩu hiện tại sai | `Password does not match` |
| Đổi email sang email hiện tại | `New email must be different from current email` |
| USER gọi `GET /api/admin/users` | 403 |
| Admin khóa Admin khác | `Cannot lock an admin account` |
| Admin gán role ARTIST | `ARTIST role can only be granted by approving an artist request` |
| Admin gán role `"BOSS"` | `Invalid request data` |
| Admin đổi role của Admin khác | `Cannot change role of an admin account` |
| USER gọi `PUT /api/users/me/artist-profile` | `Only artists can update artist profile` |
| Token bị sửa 1 ký tự | 401 `Unauthorized, please login` |
| Tài khoản mới (chưa ACCEPT) gọi `POST /api/auth/forgot-password` | 200, đặt lại mật khẩu được, nhưng đăng nhập vẫn `Account has not been approved yet` |
| Gmail sai App Password rồi đăng ký | 503 `Could not send OTP email, please try again later`, tài khoản **không** được tạo (kiểm tra bằng SQL) |

---

## 21. CẤU HÌNH, MÔI TRƯỜNG VÀ DEPLOY

### 21.1. Khóa cấu hình liên quan

```properties
# JWT
# secret: chuỗi bí mật dài ≥ 64 ký tự, KHÁC nhau giữa local và VPS
app.jwt.secret=<chuỗi bí mật>
# expiration-ms: 86400000 ms = 24 giờ
app.jwt.expiration-ms=86400000

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Gửi email (Gmail SMTP)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=<gmail gửi OTP>
# password: App Password 16 ký tự của Gmail (KHÔNG phải mật khẩu Gmail), viết liền không dấu cách
spring.mail.password=<App Password>
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000

# Rate limit
app.rate-limit.window-seconds=60
app.rate-limit.user-requests=100
app.rate-limit.auth-requests=10

# Thư mục ảnh đại diện (chung với module Song)
app.upload-dir=uploads
```

⚠️ File `.properties` **không cho ghi chú ở cuối dòng**: viết `app.jwt.expiration-ms=86400000   # 24 giờ` thì giá trị đọc được là cả chuỗi `86400000   # 24 giờ` → ứng dụng không khởi động được. Ghi chú phải nằm trên **1 dòng riêng** bắt đầu bằng `#` (bản tài liệu cũ có ví dụ ghi sai kiểu này, đã sửa).

⚠️ File cấu hình **không đưa lên GitHub** (có trong `.gitignore`). Ai có `app.jwt.secret` có thể tự tạo token của bất kỳ ai → tuyệt đối không chia sẻ.

### 21.2. Lấy App Password của Gmail

1. Bật xác minh 2 bước cho tài khoản Gmail gửi OTP.
2. Vào **Google Account → Security → App passwords**.
3. Tạo mật khẩu ứng dụng cho "Mail" → nhận chuỗi 16 ký tự → dán vào `spring.mail.password` (bỏ dấu cách).

### 21.3. Timeout gửi email 5 giây

Nếu không đặt timeout, Gmail chậm có thể làm request đăng ký "treo" rất lâu. Đặt 5 giây: quá thời gian → báo lỗi 503 ngay để người dùng thử lại.

### 21.4. Hai môi trường

| | Local | VPS |
|---|---|---|
| File | `application.properties` | `application-prod.properties` |
| Redis | `localhost:6379` trên máy | `localhost:6379` trên VPS |
| `app.jwt.secret` | Chuỗi riêng | Chuỗi riêng khác (token local không dùng được trên VPS) |
| `ddl-auto` | `update` | `validate` |

Thêm giá trị mới vào `AccountStatus`, `Role`, `ArtistRequestStatus`, `OtpPurpose`, `AccessStatus` → cột tương ứng là ENUM của MySQL → phải chạy `ALTER TABLE ... MODIFY COLUMN ... ENUM(...)` liệt kê **đủ mọi giá trị** trên cả local và VPS.

### 21.5. Deploy (MỚI: tự động bằng GitHub Actions)

```
1. Làm tính năng trên nhánh riêng → push → tạo Pull Request vào main
2. GitHub Actions build bằng Maven (CI)
3. Merge Pull Request → GitHub Actions SSH vào VPS chạy deploy.sh
       (kéo code → build → tắt bản cũ → chạy bản mới với profile prod → kiểm tra sức khỏe)
4. Xem log trên VPS: tail -f /home/myspotify/app.log
```
- **Không** push thẳng lên `main`: mọi thứ lên `main` đều được deploy ngay.
- VPS dùng `ddl-auto=validate` → thêm cột / bảng mới phải tự chạy SQL trên VPS **trước** khi merge.

### 21.6. Cột `access_status` trên môi trường mới (MỚI)

Máy local (`ddl-auto=update`) Hibernate tự thêm cột. Trên database mới hoặc VPS (`validate`), câu SQL tương đương (tham khảo):
```sql
ALTER TABLE users ADD COLUMN access_status ENUM('ACCEPT','REFUSE') NOT NULL DEFAULT 'REFUSE';
```
⚠️ Sau khi thêm cột, **mọi tài khoản cũ đều là `REFUSE`** → không ai đăng nhập được, kể cả Admin. Phải cho phép lại những tài khoản cần dùng:
```sql
UPDATE users SET access_status = 'ACCEPT' WHERE email IN ('admin@gmail.com', 'lan@gmail.com');
```

---

## 22. GIỚI HẠN ĐÃ BIẾT VÀ HƯỚNG PHÁT TRIỂN

| # | Giới hạn | Ảnh hưởng | Hướng cải thiện |
|---|---|---|---|
| 1 | `register` báo `Email already exists`, `forgot-password` báo `Email does not exist` | Có thể dò email đã đăng ký qua 2 API này | Trả message chung; gửi email thông báo thay vì báo lỗi |
| 2 | Admin khóa và người dùng tự vô hiệu hóa chung trạng thái `DISABLED` | Không phân biệt được ai khóa; người tự vô hiệu phải nhờ Admin mở | Tách `LOCKED` / `DISABLED`, cho tự kích hoạt lại bằng OTP |
| 3 | Từ chối yêu cầu nghệ sĩ không có lý do | Người dùng không biết sai ở đâu | Thêm `artistRejectReason` |
| 4 | `GET /api/admin/users` không phân trang | Chậm khi có hàng chục nghìn user | Phân trang + tìm kiếm + lọc |
| 5 | Không có thời gian tạo tài khoản (`created_at`) | Không thống kê được người dùng mới | Thêm `created_at`, `updated_at` |
| 6 | Không có refresh token | Hết 24 giờ phải đăng nhập lại | Access token ngắn + refresh token |
| 7 | Không khóa tạm thời sau nhiều lần đăng nhập sai | Chỉ dựa vào rate limit theo IP | Đếm số lần sai theo email |
| 8 | Rate limit dạng cửa sổ cố định | Có thể dồn ~2× request quanh mốc chuyển cửa sổ | Sliding window / token bucket |
| 9 | Xóa tài khoản giữ email/username vĩnh viễn | Người dùng không đăng ký lại được bằng email cũ | Ẩn danh hóa dữ liệu sau X ngày |
| 10 | Không ghi nhật ký hành động Admin | Không biết Admin nào khóa ai, lúc nào | Bảng `audit_logs` |
| 11 | OTP gửi đồng bộ trong request | Gmail chậm → request chậm (tối đa ~5 giây) | Gửi email bất đồng bộ (`@Async` / hàng đợi) |
| 12 | Đổi email không thông báo về email cũ | Bị chiếm tài khoản khó phát hiện | Gửi email cảnh báo tới địa chỉ cũ |
| 13 | Mỗi request đọc user từ MySQL | Thêm 1 truy vấn mỗi request | Cache thông tin user trong Redis |
| 14 | (MỚI) Quyền vào web chỉ đổi được bằng SQL | Chủ dự án phải vào MySQL mỗi lần cho phép 1 người; Admin không xem được ai đang chờ | API riêng cho chủ dự án (hoặc Admin) + trang "Chờ cho phép" |
| 15 | (MỚI) Lúc đăng ký / xác thực OTP không báo trước là cần được cho phép | Người dùng chỉ biết khi đăng nhập bị chặn | Thêm câu thông báo vào message sau khi xác thực OTP |
| 16 | (MỚI) Thu hồi quyền vào web không xóa phiên trong Redis | Token cũ dùng lại được ngay nếu được `ACCEPT` lại trong lúc token còn hạn | Xóa phiên khi thu hồi (cần API để có chỗ gọi `SessionService`) |
| 17 | (MỚI) Bài hát, playlist công khai của người bị thu hồi quyền vào web vẫn hiển thị | Khác với khi bị Admin khóa | Tùy nghiệp vụ: thêm `accessStatus` vào điều kiện hiển thị nếu muốn ẩn |
| 18 | (MỚI) Admin không có công cụ xử lý playlist công khai có tên phản cảm | Chỉ có cách khóa cả tài khoản | API Admin ẩn / xóa playlist |

---

## 23. CÂU HỎI THƯỜNG GẶP

**H: Không nhận được email OTP?**
Đ: Kiểm tra thư mục Spam / Quảng cáo. Đợi đủ 60 giây rồi gọi lại API đăng ký (hoặc forgot-password) để nhận mã mới. Nếu API trả 503 → server gửi mail lỗi, báo backend kiểm tra App Password Gmail.

**H: Tôi nhập đúng mã mà vẫn báo sai?**
Đ: Có thể bạn đã xin mã mới → mã cũ trong email trước đã bị vô hiệu. Luôn dùng mã trong **email mới nhất**.

**H: Đang dùng bình thường tự nhiên bị 401?**
Đ: Tài khoản vừa đăng nhập ở nơi khác (message `Your session has ended...`), hoặc token hết hạn 24 giờ, hoặc bị Admin khóa, hoặc vừa đổi mật khẩu / email, hoặc bị chủ dự án thu hồi quyền vào web (message `Unauthorized, please login`).

**H: Admin vừa duyệt nghệ sĩ, tôi có phải đăng nhập lại không?**
Đ: Không. Role được đọc từ database mỗi request. Chỉ cần gọi lại `GET /api/users/me/profile` để cập nhật giao diện. (Trường `role` trong kết quả đăng nhập cũ sẽ không tự đổi.)

**H: Sao Admin không gán thẳng ARTIST cho nhanh?**
Đ: Quyết định nghiệp vụ: nghệ sĩ phải tự khai nghệ danh và qua kiểm duyệt.

**H: Làm sao hạ quyền 1 Admin?**
Đ: Không có API. Sửa trực tiếp database. Đây là chủ ý để bảo vệ tài khoản Admin.

**H: Tài khoản đã xóa có khôi phục được không?**
Đ: Không qua API. Dữ liệu vẫn còn trong database (xóa mềm), có thể khôi phục thủ công bằng SQL nếu thật sự cần.

**H: Tôi tự vô hiệu hóa tài khoản, giờ muốn dùng lại?**
Đ: Liên hệ Admin để mở khóa (`PUT /api/admin/users/{id}/unlock`).

**H: Sao đăng ký lại cùng email không báo lỗi "đã tồn tại"?**
Đ: Nếu tài khoản đó **chưa xác thực**, đăng ký lại = cập nhật username/mật khẩu và gửi mã mới. Chỉ tài khoản đã kích hoạt mới báo `Email already exists`.

**H: Username có phân biệt hoa thường không?**
Đ: Kiểm tra trùng phụ thuộc collation của MySQL (mặc định không phân biệt hoa thường) → `Lan.Nguyen` và `lan.nguyen` bị coi là trùng.

**H: Đăng nhập bằng username được không?**
Đ: Không. Chỉ đăng nhập bằng **email**.

**H: Token đặt ở đâu trong Postman?**
Đ: Tab Authorization → Bearer Token → dán token (không cần gõ chữ "Bearer").

**H: Đã nhập OTP thành công mà đăng nhập báo `Account has not been approved yet`?**
Đ: Đúng thiết kế (MỚI). Tài khoản mới phải được chủ dự án cho phép vào web (`access_status = 'ACCEPT'`, mục 7.3a). Liên hệ chủ dự án.

**H: Tôi là Admin mà cũng bị `Account has not been approved yet`?**
Đ: Quyền vào web độc lập với role. Admin cũng phải `ACCEPT`. Chủ dự án chạy: `UPDATE users SET access_status = 'ACCEPT' WHERE email = '...';`

**H: Admin có cho người khác vào web được không?**
Đ: Không. Không có API nào đổi `accessStatus`, chỉ chủ dự án đổi bằng SQL.

**H: Đang dùng bình thường bị 401 `Unauthorized, please login`, đăng nhập lại thì báo `Account has not been approved yet`?**
Đ: Chủ dự án vừa thu hồi quyền vào web của tài khoản.

**H: Thêm cột `access_status` xong không ai đăng nhập được?**
Đ: Mọi tài khoản cũ nhận giá trị mặc định `REFUSE`. Chạy `UPDATE` cho các tài khoản cần dùng (mục 21.6).

**H: Đăng ký báo 503 `Could not send OTP email...`, tài khoản có bị tạo dở không?**
Đ: Không. Đăng ký có `@Transactional`, gửi mail lỗi thì mọi thay đổi bị hoàn tác. Sửa cấu hình Gmail (mục 21.2) rồi đăng ký lại. Lý do lỗi thật nằm trong log server (`Could not send OTP email to ...`).

---

## 24. PHỤ LỤC

### 24.1. Bảng tổng hợp API

| # | Method | URL | Quyền | Body | Kết quả 200 |
|---|---|---|---|---|---|
| 1 | POST | `/api/auth/register` | Công khai | `username, email, password` | `Registration successful, the account is awaiting OTP verification` |
| 2 | POST | `/api/auth/verify-otp` | Công khai | `email, otpCode` | `Verification successful, the account has been activated` |
| 3 | POST | `/api/auth/login` | Công khai | `email, password` | `{ token, userId, username, role }` |
| 4 | POST | `/api/auth/forgot-password` | Công khai | `email` | `OTP code has been sent to your email` |
| 5 | POST | `/api/auth/reset-password` | Công khai | `email, otpCode, newPassword` | `Password has been reset successfully` |
| 6 | POST | `/api/users/logout` | Đăng nhập | — | `Logout successful` |
| 7 | GET | `/api/users/me/profile` | Đăng nhập | — | `UserProfileResponse` |
| 8 | PUT | `/api/users/me/profile` | Đăng nhập | `fullName` | `UserProfileResponse` |
| 9 | POST | `/api/users/me/avatar` | Đăng nhập | form-data `file` | `UserProfileResponse` |
| 10 | POST | `/api/users/me/change-password/request-otp` | Đăng nhập | `currentPassword` | `OTP code has been sent to your email` |
| 11 | PUT | `/api/users/me/change-password` | Đăng nhập | `otpCode, newPassword` | `Password has been changed successfully` |
| 12 | POST | `/api/users/me/email/request-otp` | Đăng nhập | `newEmail` | `OTP code has been sent to your new email` |
| 13 | PUT | `/api/users/me/email` | Đăng nhập | `otpCode` | `Email has been updated, please login again` |
| 14 | PUT | `/api/users/me/disable` | Đăng nhập | `password` | `Account has been disabled` |
| 15 | PUT | `/api/users/me/delete` | Đăng nhập | `password` | `Account has been deleted` |
| 16 | POST | `/api/users/me/artist-request` | Đăng nhập (USER) | `artistName, bio` | `Artist request has been submitted` |
| 17 | PUT | `/api/users/me/artist-profile` | Đăng nhập (ARTIST) | `artistName, bio` | `UserProfileResponse` |
| 18 | GET | `/api/admin/users` | ADMIN | — | `UserProfileResponse[]` |
| 19 | PUT | `/api/admin/users/{userId}/lock` | ADMIN | — | `Account has been locked` |
| 20 | PUT | `/api/admin/users/{userId}/unlock` | ADMIN | — | `Account has been unlocked` |
| 21 | GET | `/api/admin/artist-requests` | ADMIN | — | `UserProfileResponse[]` |
| 22 | PUT | `/api/admin/artist-requests/{userId}/approve` | ADMIN | — | `Artist request has been approved` |
| 23 | PUT | `/api/admin/artist-requests/{userId}/reject` | ADMIN | — | `Artist request has been rejected` |
| 24 | PUT | `/api/admin/users/{userId}/role` | ADMIN | `role` | `Role has been assigned successfully` |

### 24.2. DTO

**Request**

| DTO | Trường và ràng buộc |
|---|---|
| `RegisterRequest` | `username` (5–20, `[a-zA-Z0-9_.]`), `email` (≤ 100), `password` (6–72) |
| `VerifyOtpRequest` | `email`, `otpCode` (6 chữ số) |
| `LoginRequest` | `email`, `password` |
| `ForgotPasswordRequest` | `email` |
| `ResetPasswordRequest` | `email`, `otpCode` (6 chữ số), `newPassword` (6–72) |
| `UpdateProfileRequest` | `fullName` (bắt buộc, ≤ 100) |
| `RequestChangePasswordOtpRequest` | `currentPassword` |
| `ChangePasswordRequest` | `otpCode` (6 chữ số), `newPassword` (6–72) |
| `RequestUpdateEmailOtpRequest` | `newEmail` (≤ 100) |
| `UpdateEmailRequest` | `otpCode` (6 chữ số) |
| `AccountActionRequest` | `password` (dùng cho vô hiệu hóa và xóa) |
| `ArtistRequest` | `artistName` (bắt buộc, ≤ 100), `bio` (≤ 255) |
| `AssignRoleRequest` | `role` (bắt buộc: `USER` / `ARTIST` / `ADMIN`) |

**Response**

| DTO | Trường |
|---|---|
| `LoginResponse` | `token`, `userId`, `username`, `role` |
| `UserProfileResponse` | `id`, `username`, `email`, `fullName`, `avatarUrl`, `accountStatus`, `role`, `artistRequestStatus`, `artistName`, `bio` |

Cố ý **không** trả: `password` (bản băm), `accessStatus` (thông tin quản trị nội bộ của chủ dự án).

### 24.3. Bảng message lỗi nghiệp vụ

| Message | Ở đâu | Nghĩa |
|---|---|---|
| `Email already exists` | Đăng ký, đổi email | Email đã được dùng |
| `Username already exists` | Đăng ký | Username đã được dùng |
| `Email does not exist` | Xác thực OTP, quên / đặt lại mật khẩu | Không có tài khoản với email này |
| `Account is already verified` | Xác thực OTP | Đã kích hoạt rồi |
| `Email or password is incorrect` | Đăng nhập | Sai email hoặc mật khẩu |
| `Account is not verified yet` | Đăng nhập | Chưa nhập OTP |
| `Account has been disabled` | Đăng nhập | Bị khóa / tự vô hiệu hóa |
| `Account has been deleted` | Đăng nhập | Đã xóa |
| `Account has not been approved yet` | Đăng nhập | (MỚI) Chưa được chủ dự án cho phép vào web (`accessStatus ≠ ACCEPT`) |
| `Could not send OTP email, please try again later` | Mọi luồng gửi OTP (503) | Gửi email thất bại, thay đổi database bị hoàn tác |
| `Account is not active` | Quên mật khẩu, vô hiệu hóa, xóa, gửi / duyệt yêu cầu nghệ sĩ | Tài khoản không ở trạng thái ACTIVE |
| `Password does not match` | Đổi mật khẩu (bước 1), vô hiệu hóa, xóa | Sai mật khẩu hiện tại |
| `New email must be different from current email` | Đổi email | Trùng email cũ |
| `Otp code does not exist` | Mọi luồng OTP | Không có mã nào còn hiệu lực |
| `Otp code has expired` | Mọi luồng OTP | Quá 5 phút |
| `Otp code does not match` | Mọi luồng OTP | Sai mã |
| `Too many wrong attempts, please request a new OTP` | Mọi luồng OTP | Quá 5 lần nhập |
| `Please wait 60 seconds before requesting a new OTP` | Mọi luồng xin OTP | Xin mã quá nhanh |
| `OTP code must be 6 digits` | DTO | Mã không phải 6 chữ số |
| `User does not exist` | Admin, nội bộ | Không tìm thấy user |
| `Cannot lock an admin account` | Khóa | |
| `Only active accounts can be locked` | Khóa | |
| `Only disabled accounts can be unlocked` | Mở khóa | |
| `Only users with role USER can request to become an artist` | Gửi yêu cầu nghệ sĩ | |
| `An artist request already exists or has been approved` | Gửi yêu cầu nghệ sĩ | Đang chờ hoặc đã là nghệ sĩ |
| `No pending artist request for this user` | Duyệt / từ chối yêu cầu | |
| `Only users with role USER can become an artist` | Duyệt yêu cầu | |
| `Only artists can update artist profile` | Sửa hồ sơ nghệ sĩ | |
| `Role must not be null` | Đổi role | |
| `ARTIST role can only be granted by approving an artist request` | Đổi role | |
| `Cannot change role of an admin account` | Đổi role | |
| `Cannot assign role to a deleted or unverified account` | Đổi role | |
| `User already has this role` | Đổi role | |

### 24.4. Bản đồ "muốn sửa X thì mở file nào"

| Muốn thay đổi | Mở file |
|---|---|
| Thời hạn OTP, thời gian chờ gửi lại, số lần nhập | `service/OtpService.java` (`OTP_EXPIRE_MINUTES`, `RESEND_COOLDOWN_SECONDS`, `MAX_FAILED_ATTEMPTS`) — nhớ sửa cả nội dung email trong `EmailService` |
| Nội dung email | `service/EmailService.java` |
| Thời hạn token / phiên | `app.jwt.expiration-ms` |
| Giới hạn request | `app.rate-limit.*` |
| API nào công khai | `config/SecurityConfig.java` |
| Thông báo 401 / 403 | `config/SecurityConfig.java` + `exception/GlobalExceptionHandler.java` |
| Quy tắc username / mật khẩu | `dto/.../RegisterRequest.java` (+ các DTO đổi / đặt lại mật khẩu) |
| Quy tắc khóa, đổi role, duyệt nghệ sĩ | `service/AdminService.java` |
| Quy tắc tự quản lý tài khoản | `service/UserService.java` |
| Quyền vào web (giá trị mặc định, chỗ kiểm tra) | `enums/AccessStatus.java`, `entity/User.java` (mặc định `REFUSE`), `AuthService.register` / `login`, `security/CustomUserDetailsService.java` |
| Cho phép / thu hồi quyền vào web 1 người | Không sửa code — chạy SQL `UPDATE users SET access_status = ...` |
| Message lỗi gửi email | `service/EmailService.java` (`EmailSendException`) + `exception/GlobalExceptionHandler.java` |
| Thêm trạng thái / role | `enums/*.java` + `ALTER TABLE` trên local và VPS |