# MODULE USER — TÀI LIỆU NGHIỆP VỤ VÀ LUỒNG XỬ LÝ

> Dự án: **MySpotify Backend** (Spring Boot 4, Java 25, MySQL, Redis)
> Package gốc: `com.winhmm.myspotify`
> Đối tượng đọc: thành viên mới trong nhóm (backend, frontend, tester), **không cần biết trước Spring Boot**.
> Tài liệu liên quan: `docs/SONG_MODULE.md` (module bài hát), `docs/TEST_SONG_POSTMAN.md`.

---

## MỤC LỤC

1. [Cách đọc tài liệu này](#1-cách-đọc-tài-liệu-này)
2. [Module User giải quyết bài toán gì?](#2-module-user-giải-quyết-bài-toán-gì)
3. [Từ điển thuật ngữ](#3-từ-điển-thuật-ngữ)
4. [Danh sách chức năng](#4-danh-sách-chức-năng)
5. [Kiến trúc và vị trí các file](#5-kiến-trúc-và-vị-trí-các-file)
6. [Dữ liệu: MySQL và Redis](#6-dữ-liệu-mysql-và-redis)
7. [Ba "trục trạng thái" của 1 tài khoản](#7-ba-trục-trạng-thái-của-1-tài-khoản)
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
18. [Ảnh hưởng của module User lên module Song](#18-ảnh-hưởng-của-module-user-lên-module-song)
19. [Hướng dẫn riêng cho Frontend](#19-hướng-dẫn-riêng-cho-frontend)
20. [Hướng dẫn test nhanh bằng Postman](#20-hướng-dẫn-test-nhanh-bằng-postman)
21. [Cấu hình và môi trường](#21-cấu-hình-và-môi-trường)
22. [Giới hạn đã biết và hướng phát triển](#22-giới-hạn-đã-biết-và-hướng-phát-triển)
23. [Câu hỏi thường gặp](#23-câu-hỏi-thường-gặp)
24. [Phụ lục](#24-phụ-lục)

---

## 1. CÁCH ĐỌC TÀI LIỆU NÀY

| Bạn là | Đọc theo thứ tự | Có thể bỏ qua |
|---|---|---|
| **Người mới hoàn toàn** | 2 → 3 → 4 → 7 → 8 → 9 → 11 → 12 → 13 → 14 | 10, 21 |
| **Frontend** | 2 → 4 → 7 → 9 → 19 → 11 → 12 → 13 → 17 | 5, 6, 10, 21 |
| **Tester** | 2 → 4 → 7 → 8 → 9 → 11 → 12 → 13 → 14 → 15 → 20 | 5, 21 |
| **Backend sắp sửa code** | Đọc hết, đặc biệt 5, 6, 8, 9, 10, 16 | |

Quy ước:
- `chữ như thế này` là tên trong code: class, method, cột database, trường JSON.
- **400 `"Email already exists"`** = server trả mã HTTP 400 kèm `{ "message": "Email already exists" }`.
- "Tài khoản đang hoạt động" = `accountStatus = ACTIVE`.

---

## 2. MODULE USER GIẢI QUYẾT BÀI TOÁN GÌ?

### 2.1. Một câu

> Quản lý **ai được vào hệ thống**, **họ là ai** và **họ được làm gì**.

### 2.2. Câu chuyện minh họa

1. **Lan** tải app MySpotify, điền username `lan.nguyen`, email, mật khẩu → bấm Đăng ký.
2. Server tạo tài khoản ở trạng thái **chưa xác thực** và gửi **mã OTP 6 số** vào email của Lan.
3. Lan nhập mã → tài khoản được **kích hoạt**.
4. Lan đăng nhập, nhận về 1 **token**. Từ giờ mọi thao tác app gửi kèm token này.
5. Lan đăng nhập thêm trên laptop → điện thoại bị **đăng xuất tự động** (mỗi tài khoản chỉ được dùng ở 1 nơi).
6. Lan cập nhật họ tên, ảnh đại diện. Muốn đổi mật khẩu → nhập mật khẩu cũ → nhận OTP qua email → nhập OTP + mật khẩu mới.
7. Lan quên mật khẩu → bấm "Quên mật khẩu" → nhận OTP → đặt mật khẩu mới.
8. Lan là ca sĩ nghiệp dư → gửi **yêu cầu trở thành nghệ sĩ** với nghệ danh "Mây Chiều".
9. **Admin** xem danh sách yêu cầu, **duyệt** → Lan trở thành **ARTIST**, có thể đăng nhạc (module Song).
10. Một tài khoản khác spam bình luận xấu → Admin **khóa** tài khoản đó. Người đó lập tức bị đăng xuất và không đăng nhập lại được.
11. Lan muốn nghỉ dùng app → **xóa tài khoản** (cần nhập mật khẩu để xác nhận).

### 2.3. Ba vai trò (`Role`)

| Role | Có được bằng cách nào | Làm được gì thêm |
|---|---|---|
| **USER** | Mặc định khi đăng ký | Nghe nhạc, quản lý tài khoản, gửi yêu cầu làm nghệ sĩ |
| **ARTIST** | **Chỉ** qua: gửi yêu cầu → Admin duyệt | Mọi thứ của USER + đăng và quản lý bài hát |
| **ADMIN** | Admin khác gán, hoặc tạo trực tiếp trong database | Quản lý người dùng, duyệt nghệ sĩ, duyệt bài hát |

### 2.4. Bảy quy tắc nghiệp vụ cốt lõi

1. **Email phải được xác thực bằng OTP** trước khi đăng nhập được.
2. **Mọi thao tác nhạy cảm** (đổi mật khẩu, đổi email, quên mật khẩu) đều cần **OTP gửi qua email**.
3. **Mỗi tài khoản chỉ đăng nhập ở 1 nơi.** Đăng nhập nơi mới → nơi cũ bị đăng xuất.
4. **Đổi mật khẩu / email, bị khóa, vô hiệu hóa, xóa** → bị đăng xuất **ngay lập tức** ở mọi nơi.
5. **ARTIST không được Admin gán thẳng**, chỉ có qua quy trình "gửi yêu cầu → duyệt".
6. **Không ai đụng được vào tài khoản ADMIN** (không khóa, không đổi role được).
7. **Không xóa cứng**: tài khoản bị xóa chỉ được đánh dấu `DELETED`.

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
│   └── OtpPurpose.java             ← REGISTER, RESET_PASSWORD, CHANGE_PASSWORD, CHANGE_EMAIL
├── repository/
│   ├── UserRepository.java
│   └── OtpVerificationRepository.java
├── dto/  (request + response, xem Phụ lục 24.2)
└── exception/
    └── GlobalExceptionHandler.java ← Chuyển mọi lỗi thành JSON
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
└──────────────────────┘
         │ 1
         │        (module Song)
         ├──────N── songs.artist_id               (bài mình là Artist chính)
         └──────N── song_featured_artists.artist_id (bài mình được feat)
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

## 7. BA "TRỤC TRẠNG THÁI" CỦA 1 TÀI KHOẢN

Mỗi tài khoản có **3 thuộc tính trạng thái độc lập**. Hiểu 3 trục này là hiểu 80% module.

```
 Trục 1: account_status          Trục 2: role          Trục 3: artist_request_status
 (tài khoản có dùng được?)       (được làm gì?)        (yêu cầu làm nghệ sĩ tới đâu?)
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

### 7.4. Kết hợp 3 trục — ví dụ

| Tài khoản | account_status | role | artist_request_status | Giải thích |
|---|---|---|---|---|
| Lan vừa đăng ký | UNVERIFIED | USER | NONE | Chưa nhập OTP |
| Lan dùng bình thường | ACTIVE | USER | NONE | |
| Lan chờ duyệt nghệ sĩ | ACTIVE | USER | PENDING | Vẫn là USER cho tới khi được duyệt |
| Lan là nghệ sĩ | ACTIVE | ARTIST | APPROVED | |
| Nghệ sĩ bị khóa | DISABLED | ARTIST | APPROVED | Role giữ nguyên, nhưng không đăng nhập được, bài hát bị ẩn khỏi người nghe |
| Minh bị từ chối | ACTIVE | USER | REJECTED | Có thể gửi lại |

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

Gửi email thất bại (Gmail lỗi, sai mật khẩu ứng dụng, mất mạng) → **503** `"Could not send email, please try again later"`. Vì các luồng gọi OTP đều có `@Transactional`, mọi thay đổi trong database ở lần đó (VD tài khoản vừa tạo) được **hoàn tác** → người dùng chỉ cần thử lại.

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

### 9.4. Đăng nhập (`AuthService.login`)

```
1. Tìm user theo email           → không có: "Email or password is incorrect"
2. So mật khẩu với bản băm BCrypt → sai:     "Email or password is incorrect"
3. Kiểm tra trạng thái (CHỈ sau khi mật khẩu đúng):
        UNVERIFIED → "Account is not verified yet"
        DISABLED   → "Account has been disabled"
        DELETED    → "Account has been deleted"
4. Tạo sessionId mới, ghi vào Redis (ghi đè phiên cũ)
5. Tạo JWT chứa email + sessionId
6. Trả { token, userId, username, role }
```

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
   │     accountStatus ≠ ACTIVE → coi như khách
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
- Mọi trường hợp khác (không token, token sai chữ ký, token hết hạn...): `"Unauthorized, please login"`

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
      ├─ Tạo user: UNVERIFIED, role USER, mật khẩu băm BCrypt
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

### 11.3. A3 — Đăng nhập

**API:** `POST /api/auth/login`
```json
{ "email": "lan@gmail.com", "password": "matkhau123" }
```
Chi tiết ở mục 9.4.

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
 Đăng ký ─►│UNVERIFIED│────────────────►│ verify-otp │──► ACTIVE
           └──────────┘                 └────────────┘
                                                          │
                                                          ▼
                     ┌───────────────────── login ◄───────┘
                     │                      ▲
                     ▼                      │
                 có token ──► dùng app      │ mật khẩu mới
                                            │
 Quên mật khẩu ──► forgot-password ──OTP──► reset-password
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
2. Sửa trực tiếp trong MySQL:
   ```sql
   UPDATE users SET role = 'ADMIN' WHERE email = 'admin@gmail.com';
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
| Chưa đăng nhập / token sai / hết hạn | 401 | `Unauthorized, please login` |
| Phiên đã bị xóa: đăng nhập nơi khác, đã đăng xuất, bị khóa, đổi mật khẩu / email, vô hiệu hóa, xóa tài khoản | 401 | `Your session has ended. Your account may have been logged in on another device` |
| Sai role | 403 | `You do not have permission to access this resource` |
| Trùng dữ liệu ở database (2 người đăng ký cùng username cùng lúc) | 409 | `Data already exists` |
| Ảnh vượt giới hạn upload tổng | 413 | `File is too large` |
| Rate limit | 429 | `Too many requests. Please try again in X seconds` |
| Gửi email thất bại | 503 | `Could not send email, please try again later` |
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

## 18. ẢNH HƯỞNG CỦA MODULE USER LÊN MODULE SONG

| Sự kiện ở module User | Hệ quả ở module Song |
|---|---|
| Admin duyệt yêu cầu nghệ sĩ | Người đó dùng được `/api/artist/**`: đăng bài, quản lý bài |
| Admin **khóa** Artist | Mọi bài của Artist **biến mất** khỏi người nghe (danh sách, tìm kiếm, chi tiết, trang nghệ sĩ, lịch sử nghe). Bài mới của họ không duyệt được (`Artist account is not active`). Không feat được họ |
| Admin **mở khóa** | Mọi bài **xuất hiện lại**, lượt nghe giữ nguyên |
| Artist tự vô hiệu hóa / xóa tài khoản | Như bị khóa (vĩnh viễn nếu xóa) |
| Admin đổi ARTIST → USER / ADMIN | Như bị khóa: bài không còn hiển thị vì điều kiện "Artist chính phải là ARTIST". Mất nghệ danh. Đổi lại ARTIST phải gửi yêu cầu mới |
| Artist đổi nghệ danh | Mọi bài (kể cả bài được feat) hiện nghệ danh mới ngay |
| Artist được feat bị khóa | Bài **vẫn hiển thị**, nhưng tên người đó bị ẩn khỏi `featuredArtists` |

Chi tiết xem `docs/SONG_MODULE.md` mục 8.4 và 18.

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
                              └─ Nhập 6 số → /api/auth/verify-otp ──200──► [Đăng nhập]
```

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
| 6 | `POST /api/auth/login` | 200, lưu token |
| 7 | `GET /api/users/me/profile` | 200, `accountStatus: ACTIVE`, không có `password` |
| 8 | `PUT /api/users/me/profile` | 200, `fullName` mới |
| 9 | `POST /api/users/me/avatar` (key `file`) | 200, có `avatarUrl` |
| 10 | Login lại → dùng token cũ của bước 6 | 401 `Your session has ended...` |
| 11 | `POST /api/users/me/artist-request` | 200 |
| 12 | Gửi lại yêu cầu | 400 `An artist request already exists or has been approved` |
| 13 | Admin: `GET /api/admin/artist-requests` | Có user ở bước 11 |
| 14 | Admin: `PUT /api/admin/artist-requests/{id}/approve` | 200 |
| 15 | User: `GET /api/users/me/profile` (không cần login lại) | `role: ARTIST`, `artistRequestStatus: APPROVED` |
| 16 | Admin: `PUT /api/admin/users/{id}/lock` | 200 |
| 17 | User: gọi bất kỳ API nào | 401 `Your session has ended...` |
| 18 | User: login | 400 `Account has been disabled` |
| 19 | Admin: `PUT /api/admin/users/{id}/unlock` | 200, user login lại được |
| 20 | `POST /api/users/logout` → dùng lại token | 401 |

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

---

## 21. CẤU HÌNH VÀ MÔI TRƯỜNG

### 21.1. Khóa cấu hình liên quan

```properties
# JWT
app.jwt.secret=<chuỗi bí mật dài ≥ 64 ký tự, KHÁC nhau giữa local và VPS>
app.jwt.expiration-ms=86400000        # 24 giờ

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Gửi email (Gmail SMTP)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=<gmail gửi OTP>
spring.mail.password=<App Password 16 ký tự của Gmail, KHÔNG phải mật khẩu Gmail>
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

Thêm giá trị mới vào `AccountStatus`, `Role`, `ArtistRequestStatus`, `OtpPurpose` → cột tương ứng là ENUM của MySQL → phải chạy `ALTER TABLE ... MODIFY COLUMN ... ENUM(...)` liệt kê **đủ mọi giá trị** trên cả local và VPS.

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

---

## 23. CÂU HỎI THƯỜNG GẶP

**H: Không nhận được email OTP?**
Đ: Kiểm tra thư mục Spam / Quảng cáo. Đợi đủ 60 giây rồi gọi lại API đăng ký (hoặc forgot-password) để nhận mã mới. Nếu API trả 503 → server gửi mail lỗi, báo backend kiểm tra App Password Gmail.

**H: Tôi nhập đúng mã mà vẫn báo sai?**
Đ: Có thể bạn đã xin mã mới → mã cũ trong email trước đã bị vô hiệu. Luôn dùng mã trong **email mới nhất**.

**H: Đang dùng bình thường tự nhiên bị 401?**
Đ: Tài khoản vừa đăng nhập ở nơi khác (message `Your session has ended...`), hoặc token hết hạn 24 giờ, hoặc bị Admin khóa, hoặc vừa đổi mật khẩu / email.

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
| Thêm trạng thái / role | `enums/*.java` + `ALTER TABLE` trên local và VPS |