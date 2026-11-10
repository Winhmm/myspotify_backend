# MODULE USER — TÀI LIỆU NGHIỆP VỤ, LUỒNG XỬ LÝ VÀ GIẢI THÍCH CODE

> Dự án: **MySpotify Backend** (Spring Boot 4.1, Java 17 trong `pom.xml` — VPS chạy JDK 25, MySQL, Redis)
> Package gốc: `com.winhmm.myspotify`
> Cập nhật lần cuối: **10/11/2026** — khớp với code trên nhánh `main` (code module User không đổi kể từ Pull Request #11).
> Đối tượng đọc: thành viên mới trong nhóm (backend, frontend, tester), **kể cả người chưa từng học Spring Boot**.
> Tài liệu liên quan:
> - `docs/SONG_MODULE.md` — module bài hát (nghe nhạc, đăng nhạc, thể loại, lời bài hát, gợi ý).
> - `docs/PLAYLIST_MODULE.md` — module Playlist (mỗi tài khoản tự tạo playlist).
> - `docs/TEST_SONG_POSTMAN.md` — test module Song bằng Postman.

---

## MỤC LỤC

**Mở đầu**
- 0. [Có gì mới trong bản cập nhật này](#0-có-gì-mới-trong-bản-cập-nhật-này)
- 1. [Cách đọc tài liệu này](#1-cách-đọc-tài-liệu-này)

**PHẦN A — Kiến thức nền cho người chưa học Spring Boot**
- A1. Backend là gì? Ví dụ "quán ăn"
- A2. HTTP — "ngôn ngữ" giữa frontend và backend
- A3. JSON và form-data
- A4. API, endpoint và quy ước đặt đường dẫn
- A5. Java cần nắm trước khi đọc code
- A6. Spring Boot là gì?
- A7. Bean, IoC container và Dependency Injection
- A8. Kiến trúc nhiều tầng: Controller → Service → Repository
- A9. Database, MySQL và JPA / Hibernate
- A10. Redis — bộ nhớ tạm siêu nhanh
- A11. Kiểm tra dữ liệu đầu vào (validation)
- A12. Xử lý lỗi tập trung
- A13. Spring Security cơ bản
- A14. Mật khẩu: băm bằng BCrypt
- A15. JWT — "thẻ ra vào" có chữ ký
- A16. Mã OTP và gửi email

**PHẦN B — Nghiệp vụ: hệ thống làm gì**
- 2. [Module User giải quyết bài toán gì?](#2-module-user-giải-quyết-bài-toán-gì)
- 3. [Từ điển thuật ngữ](#3-từ-điển-thuật-ngữ)
- 4. [Danh sách chức năng](#4-danh-sách-chức-năng)
- 5. [Kiến trúc và vị trí các file](#5-kiến-trúc-và-vị-trí-các-file)
- 6. [Dữ liệu: MySQL và Redis](#6-dữ-liệu-mysql-và-redis)
- 7. [Bốn "trục trạng thái" của 1 tài khoản](#7-bốn-trục-trạng-thái-của-1-tài-khoản)
- 8. [Mã OTP — trái tim của module](#8-mã-otp--trái-tim-của-module)
- 9. [Đăng nhập, JWT và phiên đăng nhập](#9-đăng-nhập-jwt-và-phiên-đăng-nhập)
- 10. [Hành trình của 1 request đã đăng nhập](#10-hành-trình-của-1-request-đã-đăng-nhập)
- 11. [Luồng: đăng ký, xác thực, đăng nhập, quên mật khẩu](#11-luồng-đăng-ký-xác-thực-đăng-nhập-quên-mật-khẩu)
- 12. [Luồng: người dùng tự quản lý tài khoản](#12-luồng-người-dùng-tự-quản-lý-tài-khoản)
- 13. [Luồng: trở thành nghệ sĩ (ARTIST)](#13-luồng-trở-thành-nghệ-sĩ-artist)
- 14. [Luồng: Admin quản lý người dùng](#14-luồng-admin-quản-lý-người-dùng)
- 15. [Giới hạn tần suất (rate limit)](#15-giới-hạn-tần-suất-rate-limit)
- 16. [Bảo mật — tổng hợp](#16-bảo-mật--tổng-hợp)
- 17. [Xử lý lỗi](#17-xử-lý-lỗi)
- 18. [Ảnh hưởng của module User lên module Song và Playlist](#18-ảnh-hưởng-của-module-user-lên-module-song-và-playlist)

**PHẦN C — Đọc code từng file**
- C0. Cách đọc phần này
- C1. `MyspotifyApplication` · C2. Các enum · C3. `User` · C4. `OtpVerification` · C5. Repository
- C6. DTO request · C7. DTO response và `UserMapper`
- C8. `SessionService` · C9. `JwtUtil` · C10. `CustomUserDetailsService` · C11. `JwtAuthenticationFilter`
- C12. `RateLimitService` và `RateLimitFilter` · C13. `SecurityConfig` · C14. `CorsConfig` và `WebMvcConfig`
- C15. `EmailService` và `EmailSendException` · C16. `OtpService` · C17. `AuthService`
- C18. `UserService` · C19. `AdminService` · C20. `FileStorageService` (phần ảnh đại diện)
- C21. `AuthController` · C22. `UserController` · C23. `AdminController` · C24. `GlobalExceptionHandler`
- C25. Theo dấu 1 request đăng ký từ đầu tới cuối
- C26. Ví dụ luyện tập: tự thêm 1 API mới
- C27. Bài tập tự kiểm tra (có đáp án)

**PHẦN D — Frontend, test, cấu hình và phụ lục**
- 19. [Hướng dẫn riêng cho Frontend](#19-hướng-dẫn-riêng-cho-frontend)
- 20. [Hướng dẫn test nhanh bằng Postman](#20-hướng-dẫn-test-nhanh-bằng-postman)
- 21. [Cấu hình, môi trường và deploy](#21-cấu-hình-môi-trường-và-deploy) (mới: 21.7 chạy dự án lần đầu trên máy của bạn)
- 22. [Giới hạn đã biết và hướng phát triển](#22-giới-hạn-đã-biết-và-hướng-phát-triển)
- 23. [Câu hỏi thường gặp](#23-câu-hỏi-thường-gặp)
- 24. [Phụ lục](#24-phụ-lục)

---

## 0. CÓ GÌ MỚI TRONG BẢN CẬP NHẬT NÀY

### 0.1. Bản 10/11/2026 (bản này): viết thêm cho người chưa học Spring Boot

Code module User **không thay đổi**. Bản này chỉ bổ sung phần giải thích:

| # | Bổ sung | Dành cho | Ở đâu |
|---|---|---|---|
| 1 | **Phần A — Kiến thức nền**: HTTP, JSON, Java cần biết, Spring Boot, bean và Dependency Injection, các tầng, database và JPA, Redis, validation, xử lý lỗi, Spring Security, BCrypt, JWT, OTP và email. Mọi ví dụ lấy từ chính dự án | Người chưa học Spring Boot | A1 → A16 |
| 2 | **Phần C — Đọc code từng file**: trích code thật của mọi file module User, giải thích từng dòng, kèm lý do viết như vậy | Người sẽ đọc / sửa code | C0 → C24 |
| 3 | **Theo dấu 1 request** đăng ký đi qua từng filter, từng class, từng câu SQL, từng lệnh Redis | Mọi người | C25 |
| 4 | **Bài luyện tập** tự thêm 1 API mới theo đúng thứ tự các tầng | Người mới tập code | C26 |
| 5 | **14 câu hỏi tự kiểm tra** có đáp án | Người mới | C27 |
| 6 | **Chạy dự án lần đầu trên máy của bạn**: cài gì, tạo database, tạo file cấu hình, các lỗi khởi động hay gặp | Thành viên mới | 21.7 |
| 7 | Thêm vài câu hỏi thường gặp và 1 giới hạn đã biết (mật khẩu có dấu) | | 22, 23 |

Phần B (nghiệp vụ) và Phần D (frontend, test, cấu hình) giữ nguyên nội dung bản 08/11, chỉ thêm đường dẫn chéo sang Phần A và C.

### 0.2. Bản 08/11/2026

So với bản 18/10/2026, code module User **chỉ có 1 thay đổi nghiệp vụ**, nhưng thay đổi này ảnh hưởng tới cả luồng đăng ký → đăng nhập:

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

Tài liệu chia 4 phần:

| Phần | Trả lời câu hỏi | Độ khó |
|---|---|---|
| **A** (A1 → A16) | "Spring Boot, HTTP, JWT... là gì?" | Dễ, cho người mới hoàn toàn |
| **B** (mục 2 → 18) | "Hệ thống làm gì, theo luật nào?" | Trung bình, không cần đọc code |
| **C** (C0 → C27) | "Code viết ra sao, từng dòng nghĩa là gì?" | Khó nhất, cần đọc A trước |
| **D** (mục 19 → 24) | "Frontend gọi thế nào, test thế nào, cấu hình ra sao?" | Tra cứu khi cần |

Lộ trình gợi ý:

| Bạn là | Đọc theo thứ tự | Có thể bỏ qua |
|---|---|---|
| **Chưa học Spring Boot, muốn hiểu và code được** | A1 → A16 → 2 → 4 → 7 → 8 → 9 → 11 → C0 → C27 → 21.7 (tự chạy thử) → 20 (tự test) | |
| **Người mới, chỉ cần hiểu hệ thống** | 0 → 2 → 3 → 4 → 7 → 8 → 9 → 11 → 12 → 13 → 14 | Phần C, 10, 21 |
| **Frontend** | A2, A3, A15 (nếu chưa rõ) → 2 → 4 → 7 → 9 → 19 → 11 → 12 → 13 → 17 | Phần C, 5, 6, 10, 21 |
| **Tester** | A2 → 2 → 4 → 7 → 8 → 9 → 11 → 12 → 13 → 14 → 15 → 20 → C27 | Phần C (trừ C27) |
| **Backend sắp sửa code** | Đọc hết, đặc biệt 5, 6, 7.3a, 8, 9, 10, 16 và cả Phần C | |

Mẹo cho người mới:
- Đọc Phần A một lượt cho quen từ, **không cần nhớ hết**. Đọc phần sau gặp từ lạ thì quay lại tra.
- Đọc Phần C thì **mở file thật trong IntelliJ bên cạnh** (giữ phím `Ctrl` rồi bấm vào tên class / method để nhảy tới chỗ nó được viết; `Ctrl + Alt + ←` để quay lại).
- Học nhanh nhất là **tự chạy** (21.7) và **tự gọi API bằng Postman** (mục 20), vừa gọi vừa xem dữ liệu đổi trong MySQL.

Quy ước:
- `chữ như thế này` là tên trong code: class, method, cột database, trường JSON.
- **400 `"Email already exists"`** = server trả mã HTTP 400 kèm `{ "message": "Email already exists" }`.
- "Tài khoản đang hoạt động" = `accountStatus = ACTIVE`.
- "Được phép vào web" = `accessStatus = ACCEPT`.
- "Chủ dự án" = người quản lý server và database (khác với tài khoản ADMIN trong app).

---

## PHẦN A — KIẾN THỨC NỀN CHO NGƯỜI CHƯA HỌC SPRING BOOT

Phần này **không** nói riêng về module User. Nó giải thích các khái niệm mà mọi mục phía sau đều dùng tới: HTTP, JSON, Spring Boot, database, Redis, bảo mật, mật khẩu, token... Mỗi khái niệm đều lấy ví dụ từ chính code của dự án.

- Ai đã biết Spring Boot: bỏ qua, đọc từ mục 2.
- Ai chưa biết gì: đọc lần lượt A1 → A16. Mỗi mục khoảng 5–10 phút. Không cần nhớ hết ngay, đọc phần sau thấy lạ thì quay lại tra.

---

### A1. Backend là gì? Ví dụ "quán ăn"

Một ứng dụng web (hoặc app điện thoại) thường có 2 nửa:

| Nửa | Là gì | Trong dự án |
|---|---|---|
| **Frontend** | Phần người dùng nhìn thấy và bấm vào: nút, ô nhập, danh sách bài hát | Web React trên Vercel, sau này có thể thêm app React Native |
| **Backend** | Phần chạy trên máy chủ (server), người dùng không nhìn thấy. Nhận yêu cầu, kiểm tra, xử lý, đọc ghi dữ liệu, trả kết quả | Dự án Spring Boot này, chạy trên VPS ở cổng `8080` |

So sánh với một quán ăn:

```
 KHÁCH HÀNG (người dùng)
      │ bấm nút "Đăng nhập"
      ▼
 THỰC ĐƠN + NGƯỜI GHI ORDER (frontend)
      │ gửi "phiếu order" qua Internet  ← đây là 1 HTTP request
      ▼
 NHÀ BẾP (backend)
      ├─ Bồi bàn nhận phiếu, xem phiếu ghi đủ chưa          ← Controller
      ├─ Đầu bếp nấu theo công thức, theo luật của quán      ← Service
      └─ Thủ kho lấy / cất nguyên liệu                       ← Repository
      ▼
 KHO (database: MySQL, Redis)
```

Backend **không có giao diện**. Nó chỉ nói chuyện bằng dữ liệu. Vì vậy:
- Muốn thử backend mà chưa có frontend → dùng **Postman** (một phần mềm gửi request bằng tay, xem mục 20).
- Cùng 1 backend có thể phục vụ nhiều frontend: web, app Android, app iOS.

Các "nhà cung cấp" mà backend của dự án nói chuyện cùng:

```
                    ┌──────────────────────────────┐
  Web / App / ────► │  BACKEND SPRING BOOT (:8080) │ ──► MySQL   (lưu tài khoản, bài hát, playlist)
  Postman           │                              │ ──► Redis   (phiên đăng nhập, bộ đếm)
                    │                              │ ──► MongoDB (lịch sử nghe, module Song)
                    │                              │ ──► Gmail   (gửi mã OTP)
                    │                              │ ──► Ổ đĩa   (ảnh đại diện, file nhạc)
                    └──────────────────────────────┘
```

---

### A2. HTTP — "ngôn ngữ" giữa frontend và backend

HTTP là quy ước để 2 máy nói chuyện qua mạng. Mỗi lần nói chuyện gồm **1 câu hỏi** (request) và **1 câu trả lời** (response).

#### A2.1. Request (yêu cầu) gồm 4 phần

Ví dụ thật: đăng nhập.

```http
POST /api/auth/login HTTP/1.1                 ← (1) method + (2) đường dẫn
Host: 157.66.100.87:8080
Content-Type: application/json                ← (3) header: thông tin phụ
                                              ← dòng trống ngăn cách header và body
{                                             ← (4) body: dữ liệu gửi kèm
  "email": "lan@gmail.com",
  "password": "matkhau123"
}
```

| Phần | Ý nghĩa | Ví dụ |
|---|---|---|
| **Method** | Muốn làm **loại** việc gì | `GET`, `POST`, `PUT`, `DELETE` |
| **Đường dẫn (path)** | Làm việc đó với **cái gì** | `/api/auth/login` |
| **Header** | Thông tin phụ: dữ liệu dạng gì, ai đang gọi | `Content-Type: application/json`, `Authorization: Bearer eyJ...` |
| **Body** | Dữ liệu chính gửi lên (không phải request nào cũng có) | `{"email": "...", "password": "..."}` |

#### A2.2. Response (trả lời) gồm 3 phần

```http
HTTP/1.1 200 OK                               ← (1) mã trạng thái
Content-Type: application/json                ← (2) header

{                                             ← (3) body
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "userId": 7,
  "username": "lan.nguyen",
  "role": "USER"
}
```

#### A2.3. Bốn method dùng trong dự án

| Method | Nghĩa thông thường | Ví dụ trong module User |
|---|---|---|
| `GET` | **Đọc** dữ liệu, không thay đổi gì. Không có body | `GET /api/users/me/profile` — xem hồ sơ |
| `POST` | **Tạo mới** hoặc **gửi 1 hành động** | `POST /api/auth/register` — đăng ký; `POST /api/users/logout` — đăng xuất |
| `PUT` | **Sửa / cập nhật** | `PUT /api/users/me/profile` — sửa họ tên |
| `DELETE` | **Xóa** | Module User không dùng (xem dưới) |

Lưu ý: xóa tài khoản dùng `PUT /api/users/me/delete` chứ **không** phải `DELETE`. Lý do: tài khoản không bị xóa khỏi database, chỉ **đổi trạng thái** thành `DELETED` (gọi là "xóa mềm", mục 7.1). Về bản chất đó là một lần **cập nhật**, nên dùng `PUT`.

Cùng 1 đường dẫn nhưng khác method là **2 API khác nhau**:
- `GET /api/users/me/profile` → xem hồ sơ.
- `PUT /api/users/me/profile` → sửa hồ sơ.

#### A2.4. Các phần của 1 địa chỉ (URL)

```
http://157.66.100.87:8080/api/admin/users/42/lock
└─┬─┘ └─────┬──────┘ └┬─┘└────────────┬─────────┘
giao thức  máy chủ   cổng          đường dẫn
```

- `42` trong đường dẫn gọi là **path variable** (biến trên đường dẫn). Trong code viết là `/users/{userId}/lock`, `{userId}` là "chỗ trống" được điền bằng số thật.
- Một số API module khác có **query string** sau dấu `?`, ví dụ `?page=0&size=20`. Module User không dùng.

#### A2.5. Mã trạng thái (status code)

Số 3 chữ số đầu tiên của response cho biết kết quả. Nhóm đầu 2 = thành công, đầu 4 = **phía gửi** làm sai, đầu 5 = **phía server** gặp sự cố.

| Mã | Tên | Nghĩa dễ hiểu | Ví dụ trong module User |
|---|---|---|---|
| **200** | OK | Thành công | Đăng nhập đúng |
| **400** | Bad Request | Dữ liệu gửi lên sai, hoặc vi phạm luật nghiệp vụ | Email sai định dạng; `Email already exists` |
| **401** | Unauthorized | **Chưa xác định được bạn là ai**: không gửi token, token sai, hết hạn, phiên đã bị hủy | Gọi `/api/users/me/profile` mà không gửi token |
| **403** | Forbidden | **Biết bạn là ai, nhưng bạn không đủ quyền** | USER thường gọi `/api/admin/users` |
| **404** | Not Found | Không có API này | Gõ sai đường dẫn (khi đã gửi token) |
| **409** | Conflict | Dữ liệu bị trùng do 2 request xảy ra cùng lúc | 2 người đăng ký cùng username trong cùng 1 tích tắc |
| **413** | Content Too Large | File gửi lên quá lớn so với giới hạn của server | Ảnh đại diện vài chục MB |
| **429** | Too Many Requests | Gọi quá nhiều lần trong thời gian ngắn | Đăng nhập sai liên tục > 10 lần / 60 giây |
| **500** | Internal Server Error | Server bị lỗi không lường trước (lỗi code) | Hiếm khi xảy ra; xem log |
| **503** | Service Unavailable | Một dịch vụ bên ngoài đang trục trặc | Gmail không gửi được email OTP |

Mẹo nhớ 401 và 403: **401 = "bạn là ai?"**, **403 = "biết bạn rồi, nhưng không được vào"**.

#### A2.6. Ba header hay gặp

| Header | Ai gửi | Ý nghĩa |
|---|---|---|
| `Content-Type: application/json` | Frontend → server | "Body tôi gửi là JSON" |
| `Authorization: Bearer eyJ...` | Frontend → server | "Đây là token đăng nhập của tôi" (mục A15) |
| `Retry-After: 37` | Server → frontend | Đi kèm lỗi 429: "37 giây nữa hãy thử lại" |

---

### A3. JSON và form-data — hai cách gói dữ liệu

#### A3.1. JSON

JSON là cách viết dữ liệu dạng chữ mà máy nào, ngôn ngữ nào cũng đọc được. Ví dụ hồ sơ người dùng trả về từ `GET /api/users/me/profile`:

```json
{
  "id": 7,
  "username": "lan.nguyen",
  "email": "lan@gmail.com",
  "fullName": "Nguyễn Thị Lan",
  "avatarUrl": null,
  "accountStatus": "ACTIVE",
  "role": "USER",
  "artistRequestStatus": "NONE",
  "artistName": null,
  "bio": null
}
```

Quy tắc viết JSON:

| Quy tắc | Đúng | Sai |
|---|---|---|
| Bao ngoài bằng `{ }` (object) hoặc `[ ]` (danh sách) | `{"email": "a@b.com"}` | `email: a@b.com` |
| Tên trường đặt trong **nháy kép** | `"email"` | `'email'` hoặc `email` |
| Chuỗi chữ đặt trong **nháy kép** | `"lan@gmail.com"` | `'lan@gmail.com'` |
| Số, `true`, `false`, `null` **không** có nháy | `"id": 7` | (viết `"7"` vẫn là JSON hợp lệ, nhưng thành chuỗi chữ chứ không phải số) |
| Các cặp ngăn cách bằng dấu phẩy, **không** có dấu phẩy sau cặp cuối | `{"a": 1, "b": 2}` | `{"a": 1, "b": 2,}` |

Viết JSON sai cú pháp → server không đọc được → **400 `"Invalid request data"`**.

Danh sách (array) trong JSON, ví dụ `GET /api/admin/users` trả về:
```json
[
  { "id": 1, "username": "admin", "...": "..." },
  { "id": 7, "username": "lan.nguyen", "...": "..." }
]
```

#### A3.2. multipart/form-data

JSON chỉ chứa chữ, **không** gửi được file. Muốn gửi file (ảnh đại diện) phải dùng kiểu **form-data**: body chia thành nhiều "ngăn", mỗi ngăn có 1 tên (key). API đổi ảnh đại diện cần 1 ngăn tên `file` chứa ảnh.

Trong Postman: tab **Body → form-data**, key `file`, đổi kiểu từ Text sang **File**, chọn ảnh.

#### A3.3. Ai chuyển JSON thành object Java?

Thư viện **Jackson** (Spring Boot cài sẵn) tự làm 2 chiều:
- **JSON → object Java** (khi nhận request): Jackson tạo object rỗng, rồi gọi các **setter** theo tên trường. JSON có `"email"` → gọi `setEmail(...)`.
- **Object Java → JSON** (khi trả response): Jackson gọi các **getter**. Có `getFullName()` → JSON có trường `"fullName"`.

Vì vậy tên trường JSON phải **khớp chính xác** với tên trong Java, phân biệt hoa thường: gửi `"Email"` hay `"e-mail"` thì Jackson không điền vào `email`, trường đó còn `null` → lỗi `Email is required`.

---

### A4. API, endpoint và quy ước đặt đường dẫn

- **API** (Application Programming Interface): danh sách các "món" backend phục vụ, kèm cách gọi. Giống thực đơn của quán.
- **Endpoint**: 1 món cụ thể = 1 cặp **method + đường dẫn**. Ví dụ `POST /api/auth/login`.

Module User có **24 endpoint**, chia 3 nhóm theo tiền tố đường dẫn:

| Tiền tố | Ai gọi được | Ví dụ |
|---|---|---|
| `/api/auth/...` | **Ai cũng gọi được**, không cần đăng nhập | Đăng ký, đăng nhập, quên mật khẩu |
| `/api/users/...` | Phải đăng nhập (mọi role) | Xem / sửa hồ sơ của chính mình |
| `/api/admin/...` | Phải đăng nhập **và** là ADMIN | Khóa tài khoản, duyệt nghệ sĩ |

Chữ `me` trong `/api/users/me/...` nghĩa là **"chính tôi"**. Server **không** lấy id người dùng từ đường dẫn hay body, mà lấy từ **token**. Vì vậy không ai sửa được hồ sơ của người khác bằng cách đổi số trong đường dẫn: đường dẫn không có số nào để đổi cả.

---

### A5. Java cần nắm trước khi đọc code

Code dự án viết bằng Java. Mục này chỉ nhắc những gì **thật sự xuất hiện** trong module User.

#### A5.1. Class, object, field, `new`

```java
public class User {               // class = "bản thiết kế"
    private String email;         // field = 1 thuộc tính mà mọi object User đều có
}

User u = new User();              // new = tạo 1 object (1 "sản phẩm" theo bản thiết kế)
u.setEmail("lan@gmail.com");      // gọi method của object
```

#### A5.2. `private`, getter và setter

Field để `private` (bên ngoài không đụng trực tiếp được). Muốn đọc / ghi phải qua method:

```java
public String getEmail() { return email; }                 // getter: đọc
public void setEmail(String email) { this.email = email; } // setter: ghi
```
`this.email` = field của object; `email` (không có `this`) = tham số truyền vào. Hai cái trùng tên nên phải có `this` để phân biệt.

Dự án **tự viết tay** getter / setter (không dùng thư viện Lombok sinh tự động), để người mới đọc thấy rõ mọi thứ.

#### A5.3. Constructor (hàm khởi tạo)

Method đặc biệt, **cùng tên với class**, không có kiểu trả về, chạy 1 lần lúc `new`:

```java
public LoginResponse(String token, Long userId, String username, Role role) {
    this.token = token;
    this.userId = userId;
    this.username = username;
    this.role = role;
}
```

#### A5.4. `final` và `static final`

```java
private final UserRepository userRepository;   // final: gán 1 lần trong constructor, sau đó không đổi được
private static final int OTP_EXPIRE_MINUTES = 5; // static final: hằng số dùng chung, viết HOA_GẠCH_DƯỚI
```

#### A5.5. Interface

Interface chỉ khai báo **tên method**, không có phần thân (không có code bên trong):

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);    // không có { ... }
}
```
Ai viết phần thân? **Spring** tự sinh lúc chạy (mục A9.5). Đây là điều "thần kỳ" đầu tiên người mới hay thắc mắc.

#### A5.6. Enum

Kiểu dữ liệu chỉ có **vài giá trị cố định**:
```java
public enum Role { USER, ARTIST, ADMIN }
```
Biến kiểu `Role` chỉ có thể là 1 trong 3 giá trị đó (hoặc `null`). So sánh bằng `==`: `if (user.getRole() == Role.ADMIN)`.

#### A5.7. Annotation — "nhãn dán" có chữ `@`

```java
@Service
public class AuthService { ... }

@NotBlank(message = "Email is required")
private String email;
```
Annotation **không tự chạy**. Nó là nhãn dán để **người khác đọc** (ở đây là Spring, Hibernate, Jackson): "class này là Service", "trường này không được để trống". Spring đọc nhãn rồi làm việc tương ứng. Hiểu Spring Boot phần lớn là hiểu các nhãn dán này; mỗi nhãn được giải thích ngay chỗ nó xuất hiện trong Phần C.

#### A5.8. Exception — báo lỗi bằng cách "ném"

```java
if (userRepository.existsByUsername(username)) {
    throw new IllegalArgumentException("Username already exists");
}
```
- `throw` = dừng method ngay lập tức và "ném" lỗi lên chỗ đã gọi method này.
- Lỗi cứ được ném ngược lên qua từng tầng (Service → Controller) cho tới khi có ai "bắt" (`catch`).
- Trong dự án, người bắt là `GlobalExceptionHandler` (mục A12): nó biến lỗi thành response 400 + `{"message": "Username already exists"}`.

`try / catch` là chỗ tự bắt lỗi:
```java
try {
    mailSender.send(message);            // thử làm
} catch (Exception e) {                  // nếu lỗi thì làm phần này
    throw new EmailSendException("Could not send OTP email, please try again later", e);
}
```

`RuntimeException` (và các con của nó như `IllegalArgumentException`) là loại lỗi không bắt buộc khai báo. Điều này quan trọng với `@Transactional` (mục A9.7).

#### A5.9. `Optional` — "hộp có thể rỗng"

`findByEmail` có thể không tìm thấy ai. Thay vì trả `null` (dễ gây lỗi `NullPointerException`), nó trả `Optional<User>` = 1 cái hộp, có thể có user hoặc rỗng.

```java
User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new IllegalArgumentException("Email does not exist"));
// Hộp có user → lấy user ra. Hộp rỗng → ném lỗi.

Optional<User> existing = userRepository.findByEmail(email);
if (existing.isPresent()) {        // hộp có đồ không?
    User oldUser = existing.get(); // lấy ra
}
```

#### A5.10. `List`, `Map`

```java
List<User> users = userRepository.findAll();          // danh sách nhiều user
Map.of("message", "Logout successful")                 // 1 cặp khóa → giá trị
```
`Map.of("message", "...")` được Jackson biến thành JSON `{"message": "..."}`. Đây là cách dự án trả mọi thông báo ngắn.

#### A5.11. Lambda, method reference và stream

```java
() -> new IllegalArgumentException("User does not exist")
```
**Lambda** = 1 hàm nhỏ không tên, viết gọn. `()` là danh sách tham số (ở đây không có), sau `->` là việc cần làm. `orElseThrow` chỉ gọi hàm này **khi hộp rỗng**.

```java
adminService.getAllUsers()                         // List<User>
        .stream()                                  // biến danh sách thành "băng chuyền"
        .map(UserMapper::toUserProfileResponse)    // mỗi User trên băng chuyền → đổi thành UserProfileResponse
        .collect(Collectors.toList());             // gom lại thành List<UserProfileResponse>
```
`UserMapper::toUserProfileResponse` là **method reference**: cách viết gọn của `user -> UserMapper.toUserProfileResponse(user)`.

#### A5.12. Thời gian và định dạng số

```java
LocalDateTime now = LocalDateTime.now();       // thời điểm hiện tại (ngày + giờ)
now.plusMinutes(5)                              // 5 phút sau
expiredAt.isBefore(LocalDateTime.now())         // đã qua thời điểm expiredAt chưa?
Duration.ofMinutes(5)                           // 1 khoảng thời gian dài 5 phút

String.format("%06d", 4213)                     // → "004213"
```
`%06d` nghĩa là: in số nguyên (`d`), đủ 6 chữ số (`6`), thiếu thì thêm số `0` phía trước (`0`). Nhờ vậy mã OTP luôn đủ 6 ký tự.

---

### A6. Spring Boot là gì?

#### A6.1. Framework

**Framework** là "bộ khung" làm sẵn những việc lặp đi lặp lại. **Spring** là framework Java phổ biến nhất để làm backend. **Spring Boot** = Spring + cấu hình mặc định sẵn + server chạy kèm (Tomcat), bấm Run là có server.

| Việc phải làm | Không có Spring Boot | Có Spring Boot |
|---|---|---|
| Mở cổng 8080, nhận request HTTP | Tự cài và cấu hình Tomcat | Có sẵn |
| Đọc JSON trong request thành object | Tự viết code đọc chuỗi | Jackson tự làm |
| Viết câu SQL `SELECT ... WHERE email = ?` | Tự viết, tự mở / đóng kết nối | Đặt tên method `findByEmail` là xong |
| Kiểm tra email đúng định dạng | Tự viết `if` | Dán `@Email` |
| Kiểm tra token mỗi request | Tự viết ở đầu mọi method | Viết 1 filter, Spring Security gọi giúp |
| Tạo object và nối các object với nhau | Tự `new` hàng chục object theo đúng thứ tự | Spring tự làm (mục A7) |

#### A6.2. Maven và `pom.xml`

**Maven** là công cụ quản lý thư viện và build. File `pom.xml` liệt kê các thư viện dự án cần; Maven tự tải về. Những thư viện module User dùng:

| Thư viện (`artifactId`) | Dùng để | Thấy ở đâu trong module User |
|---|---|---|
| `spring-boot-starter-webmvc` | Nhận request HTTP, Controller, Jackson, Tomcat | `@RestController`, `@PostMapping` |
| `spring-boot-starter-data-jpa` | Làm việc với database qua Entity và Repository (Hibernate) | `@Entity`, `UserRepository` |
| `mysql-connector-j` | "Dây nối" từ Java tới MySQL | Không thấy trong code, chỉ cần có |
| `spring-boot-starter-security` | Đăng nhập, phân quyền, filter, BCrypt | `SecurityConfig`, `@PreAuthorize` |
| `spring-boot-starter-validation` | Kiểm tra dữ liệu đầu vào | `@Valid`, `@NotBlank`, `@Email` |
| `spring-boot-starter-data-redis` | Nói chuyện với Redis | `StringRedisTemplate` |
| `spring-boot-starter-mail` | Gửi email | `JavaMailSender` |
| `jjwt-api`, `jjwt-impl`, `jjwt-gson` | Tạo và đọc JWT | `JwtUtil` |
| `spring-boot-starter-actuator` | API kiểm tra sức khỏe server | `GET /actuator/health` |
| `springdoc-openapi-starter-webmvc-ui` | Trang Swagger liệt kê API | `/swagger-ui.html` |

`lombok` có trong `pom.xml` nhưng code dự án **không dùng** (getter / setter viết tay).

#### A6.3. Ứng dụng khởi động như thế nào?

File `MyspotifyApplication.java` chỉ có vài dòng:

```java
@SpringBootApplication
@EnableAsync
public class MyspotifyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyspotifyApplication.class, args);
    }
}
```

Bấm Run → `main` chạy → `SpringApplication.run(...)` làm lần lượt:

```
1. Đọc file cấu hình application.properties (mật khẩu database, khóa JWT...)
2. Quét (scan) mọi class nằm trong package com.winhmm.myspotify và các package con
   → tìm class có nhãn @Service, @RestController, @Configuration, @Component...
3. Tạo object cho từng class đó (gọi là bean, mục A7) và nối chúng với nhau
4. Kết nối MySQL; Hibernate so sánh các @Entity với bảng thật
   (local: tự tạo / thêm cột còn thiếu; VPS: chỉ kiểm tra, sai là dừng)
5. Bật Tomcat, mở cổng 8080, bắt đầu nhận request
```
Dòng log cuối kiểu `Started MyspotifyApplication in 6.2 seconds` nghĩa là server đã sẵn sàng.

- `@SpringBootApplication`: nhãn "đây là điểm bắt đầu, hãy quét package này". Vì quét từ `com.winhmm.myspotify` trở xuống, **mọi class mới phải nằm trong package đó**, đặt ra ngoài là Spring không thấy.
- `@EnableAsync`: bật khả năng chạy method ở luồng riêng (module Song dùng để ghi lịch sử nghe). Module User không dùng.

#### A6.4. File cấu hình `application.properties`

Những giá trị **khác nhau giữa các máy** (mật khẩu database, khóa bí mật, thư mục upload) không viết cứng trong code mà để trong file cấu hình, dạng `khóa=giá trị`:

```properties
app.jwt.expiration-ms=86400000
```
Code đọc giá trị bằng `@Value`:
```java
public SessionService(StringRedisTemplate redisTemplate,
                      @Value("${app.jwt.expiration-ms}") long expirationMs) { ... }
```
`${app.jwt.expiration-ms}` nghĩa là "lấy giá trị của khóa `app.jwt.expiration-ms` trong file cấu hình". Thiếu khóa này → ứng dụng **không khởi động được** và báo `Could not resolve placeholder 'app.jwt.expiration-ms'`.

Danh sách khóa của module User: mục 21.1. Cách tạo file trên máy mới: mục 21.7.

---

### A7. Bean, IoC container và Dependency Injection

Đây là khái niệm **quan trọng nhất** của Spring, và cũng khó hiểu nhất với người mới. Đọc chậm.

#### A7.1. Vấn đề

`AuthService` cần 5 "đồ nghề" để làm việc: `UserRepository`, `OtpService`, `PasswordEncoder`, `JwtUtil`, `SessionService`. `OtpService` lại cần `OtpVerificationRepository`, `EmailService`, `StringRedisTemplate`. `EmailService` cần `JavaMailSender`... Nếu tự làm bằng tay:

```java
// KHÔNG CÓ SPRING: tự tạo mọi thứ, đúng thứ tự, ở đâu đó
JavaMailSender mailSender = new JavaMailSenderImpl(/* host, port, user, pass... */);
EmailService emailService = new EmailService(mailSender);
StringRedisTemplate redis = new StringRedisTemplate(/* kết nối Redis... */);
OtpService otpService = new OtpService(otpRepo, emailService, redis);
// ... thêm vài chục dòng nữa ...
AuthService authService = new AuthService(userRepo, otpService, encoder, jwtUtil, sessionService);
```
Hàng chục class, mỗi class cần vài class khác → rối và dễ sai.

#### A7.2. Cách Spring giải quyết

Spring có 1 cái "kho" gọi là **IoC container** (hay ApplicationContext). Lúc khởi động (bước 3 ở A6.3), Spring:
1. Tìm mọi class có nhãn `@Service`, `@Component`, `@RestController`, `@Configuration`...
2. Tạo **đúng 1 object** cho mỗi class đó, cất vào kho. Mỗi object trong kho gọi là **bean**.
3. Khi tạo 1 bean mà constructor của nó cần tham số, Spring **tự lấy bean phù hợp trong kho truyền vào**.

Việc "đồ nghề được đưa tới tận tay thay vì tự đi lấy" gọi là **Dependency Injection** (DI, tiêm phụ thuộc). Việc "Spring nắm quyền tạo object thay vì mình" gọi là **Inversion of Control** (IoC, đảo ngược quyền điều khiển).

Trong code, DI trông như sau:

```java
@Service                                         // ① nhãn: "Spring ơi, tạo giúp 1 bean AuthService"
public class AuthService {
    private final UserRepository userRepository; // ② đồ nghề cần dùng
    private final OtpService otpService;
    // ...

    public AuthService(UserRepository userRepository, OtpService otpService, /* ... */) {
        this.userRepository = userRepository;    // ③ Spring gọi constructor này,
        this.otpService = otpService;            //    tự truyền các bean đúng kiểu vào
        // ...
    }
}
```
Không có chỗ nào gọi `new AuthService(...)`. Spring gọi giúp. Cách này gọi là **constructor injection** (tiêm qua constructor), là cách dự án dùng ở mọi nơi.

#### A7.3. Bean được tạo từ đâu?

| Cách | Ví dụ trong dự án |
|---|---|
| Class có `@Service` | `AuthService`, `UserService`, `OtpService`, `SessionService`... |
| Class có `@Component` | `JwtUtil` |
| Class có `@RestController` | `AuthController`, `UserController`, `AdminController` |
| Method có `@Bean` nằm trong class `@Configuration` (dùng khi muốn tự tay tạo object, thường là object của thư viện) | `passwordEncoder()` trong `SecurityConfig` tạo bean `PasswordEncoder`; `filterChain(...)` tạo bean `SecurityFilterChain`; `corsConfigurationSource()` trong `CorsConfig` |
| Interface kế thừa `JpaRepository` | `UserRepository`, `OtpVerificationRepository` (Spring tự sinh class bên dưới) |
| Spring Boot tự tạo sẵn khi thấy thư viện và cấu hình | `JavaMailSender` (từ `spring.mail.*`), `StringRedisTemplate` (từ `spring.data.redis.*`) |

`JwtAuthenticationFilter` và `RateLimitFilter` **không** phải bean: chúng được tạo bằng `new` bên trong `SecurityConfig`. Lý do ở mục C13.

#### A7.4. Lỗi hay gặp khi mới tập

```
Parameter 0 of constructor in com.winhmm.myspotify.service.AuthService
required a bean of type 'com.winhmm.myspotify.service.SessionService' that could not be found.
```
Nghĩa là: AuthService cần `SessionService` nhưng kho không có. Nguyên nhân thường gặp:
- Quên dán `@Service` lên class `SessionService`.
- Class nằm ngoài package `com.winhmm.myspotify` nên Spring không quét tới.

---

### A8. Kiến trúc nhiều tầng: Controller → Service → Repository

#### A8.1. Mỗi tầng một việc

| Tầng | Ví dụ "quán ăn" | Việc được làm | Việc **không** được làm |
|---|---|---|---|
| **Controller** | Bồi bàn | Nhận request, kiểm tra dữ liệu có đúng mẫu không (`@Valid`), gọi Service, đóng gói kết quả thành response | Chứa luật nghiệp vụ (ví dụ "email đã tồn tại thì báo lỗi") |
| **Service** | Đầu bếp | Toàn bộ **luật nghiệp vụ**: kiểm tra trạng thái, mã hóa mật khẩu, gửi OTP, tạo token | Biết gì về HTTP, JSON, header |
| **Repository** | Thủ kho | Đọc / ghi database | Chứa luật nghiệp vụ |
| **Entity** | Nguyên liệu xếp trong kho | Ánh xạ 1 bảng database (`User` ↔ `users`) | Được gửi thẳng ra API |
| **DTO** | Tờ order và đĩa thức ăn đưa ra bàn | Hình dạng dữ liệu **nhận vào** (`RegisterRequest`) và **trả ra** (`UserProfileResponse`) | Lưu xuống database |
| **Mapper** | Người bày món ra đĩa | Chuyển Entity → DTO | |

Lợi ích: muốn đổi luật đăng ký → chỉ sửa `AuthService`. Muốn đổi tên trường JSON → chỉ sửa DTO. Mỗi chỗ sửa nằm gọn ở 1 nơi.

#### A8.2. Vì sao không trả thẳng Entity ra API?

Ví dụ Entity `User` có trường `password` (bản băm mật khẩu) và `accessStatus`. Nếu Controller trả thẳng `User` ra JSON:
1. **Lộ dữ liệu nhạy cảm**: bản băm mật khẩu đi ra ngoài. (Dự án vẫn dán thêm `@JsonIgnore` lên `password` để phòng hờ, nhưng không dựa vào đó.)
2. **Khó kiểm soát**: thêm 1 cột mới vào bảng là API tự lộ thêm 1 trường, không ai để ý.
3. **Không phù hợp đầu vào**: đăng ký chỉ cần 3 trường (username, email, password), nhưng `User` có 12 trường. Nhận `User` làm đầu vào thì kẻ xấu có thể gửi kèm `"role": "ADMIN"`.

Vì vậy dự án luôn: **nhận vào bằng DTO request, trả ra bằng DTO response**.

#### A8.3. Tầng và thư mục

```
controller/   ← tầng Controller
service/      ← tầng Service
repository/   ← tầng Repository
entity/       ← Entity
dto/          ← DTO (dto/auth/ cho đăng ký đăng nhập, dto/user/ cho hồ sơ và admin)
mapper/       ← Mapper
enums/        ← các enum dùng chung
security/     ← các class bảo mật (filter, JWT)
config/       ← cấu hình Spring (bảo mật, CORS, file tĩnh)
exception/    ← xử lý lỗi tập trung
```

---

### A9. Database, MySQL và JPA / Hibernate

#### A9.1. Bảng, dòng, cột

**MySQL** lưu dữ liệu thành các **bảng**, giống sheet Excel. Bảng `users` (rút gọn vài cột):

| id | username | email | password | account_status | role | access_status |
|---|---|---|---|---|---|---|
| 1 | admin | admin@gmail.com | `$2a$10$Xk...` | ACTIVE | ADMIN | ACCEPT |
| 7 | lan.nguyen | lan@gmail.com | `$2a$10$9q...` | ACTIVE | USER | ACCEPT |
| 8 | minh_99 | minh@gmail.com | `$2a$10$Lm...` | UNVERIFIED | USER | REFUSE |

- **Cột (column)**: 1 loại thông tin (`email`).
- **Dòng (row)**: 1 tài khoản.
- **Khóa chính (primary key)**: cột định danh, mỗi dòng 1 giá trị riêng, không bao giờ trùng. Ở đây là `id`, MySQL **tự tăng** (1, 2, 3...) khi thêm dòng mới.
- **Duy nhất (unique)**: không 2 dòng nào được trùng giá trị. `username` và `email` là unique → database **tự từ chối** dòng trùng, kể cả khi code quên kiểm tra.
- **NOT NULL**: cột bắt buộc có giá trị.
- **Khóa ngoại (foreign key)**: cột trỏ tới khóa chính của bảng khác. Bảng `otp_verifications` có cột `user_id` trỏ tới `users.id` → biết mã OTP này của ai.
- **Index (chỉ mục)**: giống mục lục cuối sách, giúp tìm nhanh theo 1 cột mà không phải đọc hết bảng.

Xem dữ liệu bằng SQL (chạy trong MySQL Workbench, hoặc dòng lệnh `mysql`):
```sql
SELECT id, username, email, account_status, role, access_status FROM users;
```

#### A9.2. ORM: không cần tự viết SQL

**ORM** (Object-Relational Mapping) = cầu nối giữa **object Java** và **bảng database**:

| Java | Database |
|---|---|
| 1 class có `@Entity` (ví dụ `User`) | 1 bảng (`users`) |
| 1 object `User` | 1 dòng |
| 1 field (`fullName`) | 1 cột (`full_name`) |

Thư viện làm việc này là **Hibernate**; **JPA** là bộ quy tắc chung (các nhãn `@Entity`, `@Id`, `@Column`... thuộc JPA, package `jakarta.persistence`). Spring Data JPA ghép Hibernate vào Spring.

Các nhãn ánh xạ gặp trong module User:

| Nhãn | Ý nghĩa | Ví dụ |
|---|---|---|
| `@Entity` | Class này là 1 bảng | `public class User` |
| `@Table(name = "users")` | Tên bảng thật | Không có thì bảng tên `user` |
| `@Id` | Field này là khóa chính | `private Long id;` |
| `@GeneratedValue(strategy = GenerationType.IDENTITY)` | Để MySQL tự tăng id | |
| `@Column(nullable = false, unique = true, length = 50)` | Cột NOT NULL, unique, tối đa 50 ký tự | `username` |
| `@Column(name = "full_name")` | Tên cột khác tên field | `fullName` ↔ `full_name` |
| `@Enumerated(EnumType.STRING)` | Lưu enum bằng **chữ** (`"ACTIVE"`) thay vì số thứ tự (`1`) | `accountStatus` |
| `@ManyToOne` + `@JoinColumn(name = "user_id")` | Nhiều OTP thuộc về 1 user, nối qua cột `user_id` | `OtpVerification.user` |
| `@Index` (trong `@Table`) | Tạo chỉ mục | `idx_otp_user_purpose_verified` |

#### A9.3. Ai tạo bảng?

Khóa cấu hình `spring.jpa.hibernate.ddl-auto` quyết định:

| Giá trị | Dùng ở | Hibernate làm gì lúc khởi động |
|---|---|---|
| `update` | Máy cá nhân | So sánh Entity với bảng thật: thiếu bảng → tạo; thiếu cột → thêm. **Không** xóa cột, **không** sửa cột đã có |
| `validate` | VPS | Chỉ kiểm tra khớp chưa. Không khớp (ví dụ Entity có cột mà bảng không có) → **dừng, không khởi động** |

Vì vậy: thêm field mới vào Entity → máy cá nhân tự có cột, nhưng **VPS phải tự chạy `ALTER TABLE` trước khi deploy** (mục 21.5).

#### A9.4. Enum lưu thế nào?

`@Enumerated(EnumType.STRING)` lưu tên giá trị (`'ACTIVE'`). Nếu dùng kiểu mặc định (`ORDINAL`) thì lưu số thứ tự (`UNVERIFIED`=0, `ACTIVE`=1...); chỉ cần ai đó chèn 1 giá trị mới vào giữa enum là toàn bộ dữ liệu cũ bị hiểu sai. Lưu bằng chữ thì an toàn và đọc database cũng dễ hiểu.

Với MySQL, Hibernate tạo các cột này kiểu `ENUM('UNVERIFIED','ACTIVE','DISABLED','DELETED')`. Hệ quả: thêm giá trị mới vào enum Java → phải `ALTER TABLE ... MODIFY COLUMN ... ENUM(...)` liệt kê đủ mọi giá trị (mục 21.4).

#### A9.5. Repository: SQL sinh từ tên method

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
```
- `JpaRepository<User, Long>`: "repository cho Entity `User`, khóa chính kiểu `Long`". Kế thừa nó là **có sẵn miễn phí** các method: `save`, `findById`, `findAll`, `deleteById`, `count`, `existsById`...
- `findByEmail`: Spring **đọc tên method** và tự sinh câu SQL. Không cần viết phần thân.

Cách Spring "đọc" tên method:

| Tên method | Spring hiểu là | SQL tương đương (viết gọn) |
|---|---|---|
| `findByEmail(email)` | Tìm theo `email` | `SELECT * FROM users WHERE email = ?` |
| `existsByEmail(email)` | Có tồn tại không (trả `boolean`) | `SELECT 1 FROM users WHERE email = ? LIMIT 1` |
| `existsByUsername(username)` | | `SELECT 1 FROM users WHERE username = ? LIMIT 1` |
| `findByArtistRequestStatus(status)` | Tìm mọi dòng có trạng thái nghệ sĩ = `status` | `SELECT * FROM users WHERE artist_request_status = ?` |

Một tên dài hơn trong `OtpVerificationRepository`:

```
findFirstByUserAndPurposeAndVerifiedFalseOrderByIdDesc(user, purpose)
└──┬───┘  └┬─┘ └───┬───┘ └──────┬─────┘ └──────┬──────┘
 lấy 1    user =  purpose =  verified =     sắp theo id
 dòng đầu  ?      ?          false          giảm dần (mới nhất trước)
```
```sql
SELECT * FROM otp_verifications
WHERE user_id = ? AND purpose = ? AND verified = false
ORDER BY id DESC
LIMIT 1
```
Nghĩa: "lấy mã OTP **mới nhất**, **chưa dùng**, của user này, cho mục đích này".

Dấu `?` là chỗ giá trị được điền vào **an toàn** (gọi là prepared statement), nên không bị tấn công SQL injection dù người dùng nhập ký tự lạ.

Gõ sai tên field trong tên method (ví dụ `findByEmal`) → ứng dụng **không khởi động được**, log báo không tìm thấy thuộc tính `emal`.

#### A9.6. `save()`: thêm mới hay cập nhật?

```java
userRepository.save(user);
```
- `user` chưa có `id` (vừa `new User()`) → Hibernate chạy `INSERT`, MySQL cấp id mới, Hibernate gán id đó vào object.
- `user` đã có `id` (đọc từ database lên) → Hibernate chạy `UPDATE ... WHERE id = ?`.

Cùng 1 method, Hibernate tự chọn.

#### A9.7. Transaction và `@Transactional`

**Transaction** (giao dịch) = gói nhiều thao tác database thành **1 khối "tất cả hoặc không gì cả"**. Ví dụ chuyển khoản: trừ tiền tài khoản A và cộng tiền tài khoản B phải cùng thành công; trừ xong mà cộng lỗi thì phải trả lại tiền cho A.

Trong module User, đăng ký gồm 2 bước:
1. `INSERT` tài khoản mới vào `users`.
2. `INSERT` mã OTP vào `otp_verifications`, rồi gửi email.

Nếu bước 2 gửi email thất bại mà bước 1 vẫn được lưu → database có 1 tài khoản "mồ côi" không ai nhận được mã. Vì vậy method `register` có nhãn `@Transactional`:

```java
@Transactional
public void register(String username, String email, String password) { ... }
```

| Kết thúc method | Kết quả |
|---|---|
| Chạy hết, không lỗi | **Commit**: mọi thay đổi được ghi thật |
| Ném ra 1 `RuntimeException` (ví dụ `EmailSendException`, `IllegalArgumentException`) | **Rollback**: hoàn tác **mọi** thay đổi database trong method |

Ba điều cần nhớ:
- Rollback **chỉ** hoàn tác **MySQL**. Redis, file đã lưu trên ổ đĩa, email đã gửi thì **không** quay lại được.
- `register` gọi `otpService.generateAndSend(...)` cũng có `@Transactional`. Method bên trong **nhập chung** transaction của method bên ngoài (không mở cái mới), nên cả 2 cùng commit hoặc cùng rollback.
- Method chỉ đọc (như `login`) không cần `@Transactional`.

---

### A10. Redis — bộ nhớ tạm siêu nhanh

#### A10.1. Redis là gì?

**Redis** là database lưu trong **RAM** (bộ nhớ), dạng cặp **khóa → giá trị** (key → value), giống 1 cuốn từ điển khổng lồ. Rất nhanh, và mỗi khóa có thể đặt **thời gian sống (TTL)**: hết giờ tự xóa.

Rất hợp với dữ liệu **tạm thời**, đọc ghi **liên tục**. MySQL hợp với dữ liệu **lâu dài**.

#### A10.2. Ba loại khóa module User cất trong Redis

| Khóa | Giá trị | Sống bao lâu | Dùng để | Code |
|---|---|---|---|---|
| `session:{email}` ví dụ `session:lan@gmail.com` | Mã phiên đăng nhập hiện tại (chuỗi UUID) | 24 giờ (bằng thời hạn token) | Mỗi tài khoản chỉ đăng nhập 1 nơi; đá người dùng ra ngay khi cần | `SessionService` |
| `otp-attempt:{id mã OTP}` ví dụ `otp-attempt:52` | Số lần đã thử nhập mã | 5 phút | Nhập sai quá 5 lần → khóa mã đó | `OtpService` |
| `rate:auth:{IP}`, `rate:user:{email}`, `rate:ip:{IP}` | Số request đã gửi | 60 giây | Chặn gọi API quá nhiều (lỗi 429) | `RateLimitService` |

#### A10.3. Câu lệnh Redis tương ứng

Code Java dùng `StringRedisTemplate` (bean Spring Boot tạo sẵn). Mỗi method tương ứng 1 lệnh Redis:

| Code Java | Lệnh Redis | Ý nghĩa |
|---|---|---|
| `opsForValue().set(key, value, Duration.ofMillis(...))` | `SET key value PX 86400000` | Ghi kèm thời gian sống |
| `opsForValue().get(key)` | `GET key` | Đọc; khóa không tồn tại / hết hạn → `null` |
| `opsForValue().increment(key)` | `INCR key` | Cộng 1; khóa chưa có thì coi như 0 rồi cộng thành 1 |
| `expire(key, Duration...)` | `EXPIRE key 60` | Đặt thời gian sống |
| `getExpire(key)` | `TTL key` | Còn sống bao nhiêu giây |
| `delete(key)` | `DEL key` | Xóa |

Tự xem Redis trên máy cá nhân (mở `redis-cli`):
```
KEYS session:*                 ← liệt kê phiên (chỉ dùng khi học, KEYS chậm nếu dữ liệu lớn)
GET session:lan@gmail.com      ← mã phiên hiện tại của lan
TTL session:lan@gmail.com      ← còn bao nhiêu giây
DEL session:lan@gmail.com      ← xóa phiên → lan bị đăng xuất ngay
```

---

### A11. Kiểm tra dữ liệu đầu vào (validation)

Người dùng có thể gửi bất cứ thứ gì: để trống, email sai, mật khẩu 1 ký tự. Phải chặn **trước** khi vào Service.

#### A11.1. Cách làm

1. Dán nhãn kiểm tra lên các field của DTO request:
```java
public class RegisterRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 5, max = 20, message = "Username must be between 5 and 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_.]+$",
            message = "Username can only contain letters, numbers, dot and underscore")
    private String username;
    // ...
}
```
2. Dán `@Valid` trước tham số trong Controller:
```java
public ResponseEntity<...> register(@Valid @RequestBody RegisterRequest request)
```
Spring kiểm tra **trước khi** chạy phần thân method. Sai → method **không chạy**, trả **400** kèm `message` lấy từ nhãn.

#### A11.2. Các nhãn kiểm tra dùng trong module User

| Nhãn | Kiểm tra | Ví dụ lỗi |
|---|---|---|
| `@NotBlank` | Chuỗi không `null`, không rỗng, không toàn dấu cách | `"username": "   "` |
| `@NotNull` | Không `null` (dùng cho kiểu không phải chuỗi, ví dụ `Role`) | Thiếu `"role"` |
| `@Email` | Đúng dạng email | `"email": "lan.gmail.com"` |
| `@Size(min, max)` | Độ dài chuỗi trong khoảng | Mật khẩu 3 ký tự |
| `@Pattern(regexp)` | Khớp biểu thức chính quy (regex) | Username có dấu cách |

#### A11.3. Đọc hiểu 2 regex trong dự án

```
^[a-zA-Z0-9_.]+$          (username)
^        bắt đầu chuỗi
[...]    1 ký tự thuộc nhóm: chữ thường a-z, chữ hoa A-Z, số 0-9, gạch dưới _, dấu chấm .
+        lặp lại 1 lần trở lên
$        kết thúc chuỗi
→ "lan.nguyen_99" hợp lệ; "lan nguyen", "lân", "lan@" không hợp lệ.

^\d{6}$                   (mã OTP; trong code Java viết "^\\d{6}$" vì \ phải viết thành \\)
\d       1 chữ số
{6}      đúng 6 lần
→ "004213" hợp lệ; "4213", "12345a", "1234567" không hợp lệ.
```

#### A11.4. Kiểm tra định dạng khác kiểm tra nghiệp vụ

| Loại | Ví dụ | Ở đâu |
|---|---|---|
| **Định dạng** (không cần database) | Email đúng dạng, mật khẩu đủ 6 ký tự | Nhãn trong DTO |
| **Nghiệp vụ** (cần đọc database / trạng thái) | Email đã có người dùng chưa? Tài khoản đã xác thực chưa? | Service, ném `IllegalArgumentException` |

Cả 2 loại đều trả 400, frontend cứ đọc `message`.

---

### A12. Xử lý lỗi tập trung

Thay vì mỗi Controller tự `try/catch`, dự án gom về 1 class duy nhất: `GlobalExceptionHandler`, có nhãn `@RestControllerAdvice` ("người bắt lỗi chung cho mọi Controller").

```
Service: throw new IllegalArgumentException("Email already exists")
   │ (lỗi bay lên)
   ▼
Controller: không bắt → lỗi bay tiếp
   │
   ▼
GlobalExceptionHandler:
   @ExceptionHandler(IllegalArgumentException.class)  ← "tôi phụ trách loại lỗi này"
   → trả 400 + {"message": "Email already exists"}
```

Mỗi method có `@ExceptionHandler(LoạiLỗi.class)` phụ trách 1 loại lỗi. Spring chọn method phụ trách **đúng loại nhất** (loại cụ thể được ưu tiên hơn loại chung chung `Exception`).

Kết quả: **mọi lỗi** của dự án đều có cùng 1 dạng `{"message": "..."}` → frontend chỉ cần 1 cách đọc. Bảng đầy đủ: mục 17.2 và C24.

---

### A13. Spring Security cơ bản

#### A13.1. Hai câu hỏi bảo mật

| Câu hỏi | Tên gọi | Trả lời sai thì |
|---|---|---|
| "Bạn là ai?" | **Authentication** (xác thực) | **401** |
| "Bạn được làm gì?" | **Authorization** (phân quyền) | **403** |

Ví dụ tòa nhà: bảo vệ cổng kiểm tra thẻ nhân viên (xác thực). Có thẻ rồi nhưng thẻ không mở được phòng giám đốc (phân quyền).

#### A13.2. Filter và filter chain

Trước khi tới Controller, mọi request phải đi qua **1 dãy trạm kiểm soát** gọi là **filter chain**. Mỗi trạm (filter) có thể:
- Ghi thêm thông tin rồi cho đi tiếp (`filterChain.doFilter(request, response)`).
- Hoặc chặn lại, tự trả response luôn (không gọi `doFilter`). Ví dụ `RateLimitFilter` trả 429.

```
request ─► [CORS] ─► [JwtAuthenticationFilter] ─► [RateLimitFilter] ─► ... ─► [AuthorizationFilter] ─► Controller
            cho phép   đọc token, ghi nhận         đếm request,                 kiểm tra luật URL:
            domain     "người gọi là ai"           quá giới hạn → 429           chưa đăng nhập → 401
            khác
```
Thứ tự và vai trò từng trạm của dự án: mục 10 và C13.

#### A13.3. SecurityContext — "tờ giấy ghi người đang gọi"

Khi `JwtAuthenticationFilter` kiểm tra token hợp lệ, nó ghi vào **SecurityContext** 1 object `Authentication` = "request này của `lan@gmail.com`, quyền `ROLE_USER`". Tờ giấy này **chỉ tồn tại trong 1 request**; request sau phải gửi lại token để ghi lại từ đầu.

Không ai ghi gì → request là **khách** (anonymous). Khách gọi API cần đăng nhập → 401.

Controller lấy lại tờ giấy bằng cách khai báo tham số `Authentication authentication`; `authentication.getName()` trả về **email** người gọi.

#### A13.4. Role và authority

Spring Security gọi quyền là **authority**, quy ước role có tiền tố `ROLE_`: role `ADMIN` → authority `ROLE_ADMIN`. Viết `hasRole('ADMIN')` thì Spring tự thêm `ROLE_` khi so sánh.

```java
@PreAuthorize("hasRole('ADMIN')")    // dán trên AdminController
```
= "mọi method của class này chỉ chạy được nếu người gọi có `ROLE_ADMIN`". Không có → 403. Nhãn này chỉ có tác dụng khi `SecurityConfig` có `@EnableMethodSecurity`.

#### A13.5. Stateless, CSRF, CORS

- **Stateless** (không trạng thái): server không dùng "phiên" kiểu truyền thống (cookie `JSESSIONID`). Mỗi request tự mang token. Dự án vẫn lưu 1 mã phiên nhỏ trong Redis để thu hồi token khi cần (mục 9.2).
- **CSRF**: kiểu tấn công lợi dụng **cookie** trình duyệt tự gửi kèm. Dự án gửi token qua **header**, trình duyệt không tự gửi header → tắt chống CSRF (`csrf.disable()`) là an toàn.
- **CORS**: luật của **trình duyệt**: trang web ở domain A (ví dụ `music-spotify-gamma.vercel.app`) gọi API ở domain B (VPS) thì trình duyệt hỏi B trước (request `OPTIONS`, gọi là preflight) "có cho A gọi không?". Dự án trả lời "cho tất cả" (`CorsConfig`). App điện thoại và Postman **không** bị luật này chi phối.

---

### A14. Mật khẩu: băm bằng BCrypt

#### A14.1. Không bao giờ lưu mật khẩu thật

Nếu database lưu `matkhau123` mà bị lộ (hacker, nhân viên tò mò, file backup thất lạc) → lộ hết mật khẩu, và người dùng thường dùng chung mật khẩu cho Facebook, ngân hàng...

Dự án lưu **bản băm** (hash):

| | Mã hóa (encrypt) | Băm (hash) |
|---|---|---|
| Đảo ngược được? | Có, nếu có khóa | **Không** |
| Ví dụ | Tin nhắn mã hóa | Vân tay: từ người lấy được vân tay, từ vân tay không dựng lại được người |

#### A14.2. BCrypt

Bản băm BCrypt dài 60 ký tự:
```
$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
└┬┘└┬┘└──────────┬──────────┘└──────────────┬──────────────┘
phiên  độ khó   "muối" (salt) 22 ký tự      kết quả băm 31 ký tự
bản    10 = 2^10 vòng
```
- **Salt (muối)**: chuỗi ngẫu nhiên trộn vào trước khi băm, **mỗi lần băm 1 khác**. Vì vậy 2 người cùng mật khẩu `123456` vẫn có 2 bản băm khác nhau, và băm lại cùng 1 mật khẩu cũng ra chuỗi khác.
- **Độ khó 10**: băm 1 lần mất cỡ vài chục mili giây. Người dùng không thấy chậm, nhưng kẻ thử đoán hàng tỷ mật khẩu thì tốn rất nhiều thời gian.

#### A14.3. Vậy so sánh mật khẩu thế nào?

```java
passwordEncoder.encode("matkhau123")            // → "$2a$10$...": lúc đăng ký, đổi, đặt lại mật khẩu
passwordEncoder.matches("matkhau123", hash)     // → true / false: lúc đăng nhập, kiểm tra mật khẩu hiện tại
```
`matches` lấy muối nằm sẵn trong `hash`, băm mật khẩu vừa nhập với đúng muối đó, rồi so 2 kết quả. **Không** có bước "giải mã".

Hệ quả: **không ai**, kể cả Admin hay chủ dự án, xem được mật khẩu của người dùng. Quên thì chỉ có cách đặt lại (mục 11.4).

#### A14.4. Vì sao giới hạn 72 ký tự?

BCrypt chỉ xử lý tối đa **72 byte**. Phần dư bị bỏ qua (thư viện bản mới còn từ chối hẳn). DTO giới hạn `max = 72` để tránh chuyện này. Lưu ý: 1 chữ tiếng Việt có dấu chiếm 2–3 byte, nên mật khẩu **có dấu** dài gần 72 ký tự vẫn có thể vượt 72 byte. Khuyên người dùng đặt mật khẩu không dấu.

---

### A15. JWT — "thẻ ra vào" có chữ ký

#### A15.1. Vì sao cần token?

HTTP **không nhớ gì** giữa 2 request. Đăng nhập xong, request tiếp theo server không biết bạn là ai. Giải pháp: lúc đăng nhập, server phát cho bạn 1 **token** (thẻ ra vào). Mọi request sau, bạn đưa thẻ trong header:

```
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJsYW5AZ21haWwuY29tIiwi...
               └─┬──┘ └──────────────────────────┬──────────────────────────┘
              chữ cố định                        token
              + 1 dấu cách
```

#### A15.2. JWT gồm 3 phần, ngăn cách bởi dấu chấm

```
eyJhbGciOiJIUzUxMiJ9 . eyJzdWIiOiJsYW5AZ21haWwuY29tIiwianRpIjoi... . 7Hk2xQ9vR...
└──────┬───────────┘   └──────────────────┬──────────────────────┘   └────┬─────┘
    HEADER                             PAYLOAD                          SIGNATURE
```

Phần 1 và 2 chỉ là chữ được **mã hóa Base64URL** (một cách viết lại cho gọn, **không phải** mật mã). Ai cũng giải ra đọc được:

```json
HEADER:  { "alg": "HS512" }

PAYLOAD: {
  "sub": "lan@gmail.com",                          ← subject: token này của ai (email)
  "jti": "3f6c2a1e-9b7d-4c1a-8e2f-5d6b7a8c9e0f",   ← JWT id: mã phiên đăng nhập
  "iat": 1762740000,                               ← issued at: phát lúc nào (giây, tính từ 1/1/1970)
  "exp": 1762826400                                ← expiration: hết hạn lúc nào (24 giờ sau)
}
```
Vì ai cũng đọc được payload → **không bao giờ để thông tin bí mật** (mật khẩu...) trong token. Token của dự án chỉ có email và mã phiên.

Thuật toán (`alg`) do độ dài khóa bí mật quyết định: khóa ≥ 64 byte → `HS512` (khuyên dùng); ≥ 48 byte → `HS384`; ≥ 32 byte → `HS256`; ngắn hơn 32 byte → thư viện báo lỗi **ngay lúc khởi động** (`WeakKeyException`).

#### A15.3. Chữ ký: vì sao không làm giả được?

```
SIGNATURE = HMAC-SHA512( HEADER + "." + PAYLOAD , khóa bí mật app.jwt.secret )
```
Chỉ server biết khóa bí mật. Kẻ xấu sửa payload (`"sub": "admin@gmail.com"`) → không tính lại được chữ ký đúng (vì không có khóa) → server kiểm tra thấy chữ ký không khớp → token vô hiệu → 401.

Hệ quả quan trọng: **ai có khóa bí mật thì tự tạo được token của bất kỳ ai**. Khóa chỉ nằm trong file cấu hình, không lên GitHub, không chia sẻ (mục 21.1).

#### A15.4. Điểm yếu của JWT và cách dự án vá

JWT thuần **không thu hồi được**: token hợp lệ cho tới lúc `exp`. Nếu Admin khóa tài khoản, token cũ vẫn dùng được thêm tới 24 giờ.

Dự án vá bằng mã phiên `jti`: lúc đăng nhập lưu `session:{email} = jti` vào Redis. Mỗi request so `jti` trong token với Redis; xóa khóa Redis là token **mất hiệu lực ngay**. Chi tiết: mục 9.2, 9.3.

⚠️ Đừng dán token thật lên các trang giải mã JWT công khai: token còn hạn = thẻ ra vào còn dùng được.

---

### A16. Mã OTP và gửi email

#### A16.1. OTP

**OTP** (One-Time Password) = mã dùng **1 lần**, sống **ngắn** (5 phút). Gửi vào email để chứng minh "bạn đúng là chủ email này". Module User dùng OTP cho 4 việc: xác thực đăng ký, quên mật khẩu, đổi mật khẩu, đổi email (mục 8).

Mã được sinh bằng `SecureRandom` chứ không phải `Random`:
- `Random`: dãy số "giả ngẫu nhiên", biết vài số trước có thể đoán số sau.
- `SecureRandom`: ngẫu nhiên đủ mạnh cho mục đích bảo mật, không đoán được.

```java
String.format("%06d", RANDOM.nextInt(1_000_000))   // số từ 0 tới 999999, đệm đủ 6 chữ số
```
`1_000_000` chỉ là cách viết số 1000000 cho dễ đọc (Java cho phép dấu `_` trong số).

#### A16.2. Gửi email qua SMTP

**SMTP** là giao thức gửi email. Dự án gửi bằng máy chủ SMTP của Gmail:
- `smtp.gmail.com`, cổng `587`, bật **STARTTLS** (mã hóa đường truyền).
- Đăng nhập bằng **App Password** 16 ký tự (không phải mật khẩu Gmail thường, mục 21.2).

Spring Boot thấy các khóa `spring.mail.*` trong file cấu hình → tự tạo bean `JavaMailSender`. `EmailService` chỉ việc nhận bean đó qua constructor và gọi `mailSender.send(message)`.

Gửi email có thể **chậm** (vài giây) hoặc **lỗi** (sai App Password, Gmail chặn, mất mạng). Dự án đặt timeout 5 giây và gói mọi lỗi thành `EmailSendException` → 503 + rollback (mục 8.4, C15).

---

**Hết Phần A.** Giờ bạn đã có đủ "từ vựng" để đọc phần nghiệp vụ (mục 2 → 18) và phần đọc code (Phần C).

---

## PHẦN B — NGHIỆP VỤ: HỆ THỐNG LÀM GÌ

Phần này mô tả **luật nghiệp vụ** và **luồng xử lý** của module User mà không cần đọc code. Gặp từ lạ (Controller, filter, transaction, token...) → tra Phần A. Muốn xem đoạn code thật tương ứng → Phần C.

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

Giải thích kỹ, có ví dụ: Phần A (A6 → A13).

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

Giải thích từng file, từng dòng code: Phần C (C1 → C24). Bảng tra nhanh file → mục: 24.5.

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

## PHẦN C — ĐỌC CODE TỪNG FILE CỦA MODULE USER

Phần B (mục 2 → 18) nói **hệ thống làm gì**. Phần này nói **code viết ra sao**: mở từng file, đọc từng đoạn, giải thích từng dòng.

### C0. Cách đọc phần này

- Code trích ở đây là code thật trên nhánh `main`, đã **bỏ bớt comment** và dòng `import` cho gọn. File thật trong IntelliJ có comment tiếng Việt rất dài, đọc kèm càng dễ hiểu.
- Chỗ ghi `// ...` là đoạn lặp lại hoặc không quan trọng, đã lược bớt.
- Thứ tự đọc **từ dưới lên**: thứ nằm sát database trước (enum, entity, repository), rồi tới dữ liệu vào ra (DTO), tới logic (service), bảo mật, cuối cùng là cửa ngõ (controller) và xử lý lỗi. Đọc theo thứ tự này thì lúc tới Service, bạn đã biết hết mọi thứ nó dùng.

```
C1  MyspotifyApplication       (điểm khởi động)
C2  enum                       (các giá trị cố định)
C3  User                       (bảng users)
C4  OtpVerification            (bảng otp_verifications)
C5  Repository                 (đọc ghi database)
C6  DTO request                (dữ liệu nhận vào)
C7  DTO response + UserMapper  (dữ liệu trả ra)
C8  SessionService             (phiên đăng nhập trong Redis)
C9  JwtUtil                    (tạo, đọc token)
C10 CustomUserDetailsService   (nạp user cho Spring Security)
C11 JwtAuthenticationFilter    (kiểm tra token mỗi request)
C12 RateLimitService + Filter  (chống spam)
C13 SecurityConfig             (luật bảo mật tổng)
C14 CorsConfig + WebMvcConfig
C15 EmailService + EmailSendException
C16 OtpService
C17 AuthService
C18 UserService
C19 AdminService
C20 FileStorageService (phần ảnh đại diện)
C21 AuthController
C22 UserController
C23 AdminController
C24 GlobalExceptionHandler
C25 Theo dấu 1 request đăng ký từ đầu tới cuối
C26 Ví dụ luyện tập: tự thêm 1 API mới
C27 Bài tập tự kiểm tra (có đáp án)
```

---

### C1. `MyspotifyApplication.java` — điểm khởi động

```java
@SpringBootApplication
@EnableAsync
public class MyspotifyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyspotifyApplication.class, args);
    }
}
```

| Dòng | Giải thích |
|---|---|
| `@SpringBootApplication` | Gộp 3 nhãn: đây là class cấu hình; bật cấu hình tự động của Spring Boot (thấy thư viện mail thì tạo `JavaMailSender`, thấy Redis thì tạo `StringRedisTemplate`...); quét mọi class trong `com.winhmm.myspotify` và package con |
| `@EnableAsync` | Cho phép method có `@Async` chạy ở luồng riêng (module Song dùng). Module User không dùng |
| `public static void main(String[] args)` | Hàm Java chạy đầu tiên khi bấm Run |
| `SpringApplication.run(...)` | Khởi động toàn bộ Spring: đọc cấu hình, tạo bean, nối database, mở cổng 8080 (xem A6.3) |

---

### C2. Các enum (`enums/`)

```java
public enum AccountStatus { UNVERIFIED, ACTIVE, DISABLED, DELETED }
public enum Role { USER, ARTIST, ADMIN }
public enum ArtistRequestStatus { NONE, PENDING, APPROVED, REJECTED }
public enum AccessStatus { ACCEPT, REFUSE }
public enum OtpPurpose { REGISTER, RESET_PASSWORD, CHANGE_PASSWORD, CHANGE_EMAIL }
```
(Trong file thật mỗi giá trị nằm 1 dòng, kèm comment giải thích.)

| Enum | Dùng ở đâu | Ý nghĩa từng giá trị |
|---|---|---|
| `AccountStatus` | Cột `users.account_status` | Mục 7.1 |
| `Role` | Cột `users.role` | Mục 2.3, 7.2 |
| `ArtistRequestStatus` | Cột `users.artist_request_status` | Mục 7.3 |
| `AccessStatus` | Cột `users.access_status` | Mục 7.3a |
| `OtpPurpose` | Cột `otp_verifications.purpose` | Mã OTP này dùng cho việc gì (mục 8) |

Vì sao `OtpPurpose` quan trọng? Nhờ nó, mã OTP gửi cho việc **đổi email** không thể đem đi dùng cho việc **đặt lại mật khẩu**: mỗi lần kiểm tra mã, code luôn tìm theo cặp (user, purpose).

Thêm giá trị mới vào enum → phải sửa cả cột `ENUM` trong MySQL trên VPS (A9.4, mục 21.4).

---

### C3. `entity/User.java` — bảng `users`

```java
@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_artist_request_status", columnList = "artist_request_status")
        }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @JsonIgnore
    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", length = 20, nullable = false)
    private AccountStatus accountStatus = AccountStatus.UNVERIFIED;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "artist_request_status", length = 20, nullable = false)
    private ArtistRequestStatus artistRequestStatus = ArtistRequestStatus.NONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_status", length = 20, nullable = false)
    private AccessStatus accessStatus = AccessStatus.REFUSE;

    @Column(name = "artist_name", length = 100)
    private String artistName;

    @Column(length = 255)
    private String bio;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    // ... getter và setter cho cả 12 field, cùng 1 kiểu như trên (24 method)
}
```

Giải thích từng phần:

| Code | Giải thích |
|---|---|
| `@Entity` | "Class này ánh xạ 1 bảng." Hibernate sẽ quản lý nó |
| `@Table(name = "users", ...)` | Tên bảng là `users` (số nhiều). Không ghi thì Hibernate đặt là `user`, mà `user` là từ khóa trong nhiều hệ database, dễ gây lỗi |
| `@Index(name = "idx_users_artist_request_status", columnList = "artist_request_status")` | Tạo chỉ mục cho cột trạng thái nghệ sĩ, vì Admin thường lọc "ai đang chờ duyệt" (`findByArtistRequestStatus(PENDING)`). Có chỉ mục thì MySQL tìm nhanh, không phải đọc hết bảng |
| `@Id` + `@GeneratedValue(strategy = GenerationType.IDENTITY)` | `id` là khóa chính, MySQL tự tăng (cột `AUTO_INCREMENT`) |
| `private Long id;` | Dùng `Long` (chữ L hoa, là object) thay vì `long` (kiểu nguyên thủy), vì `Long` được phép là `null`. `id == null` nghĩa là "object này chưa được lưu lần nào" → `save()` sẽ `INSERT` (A9.6) |
| `@Column(nullable = false, unique = true, length = 50)` trên `username` | Bắt buộc, không trùng, tối đa 50 ký tự. DTO chỉ cho 5–20 ký tự; database để rộng hơn cho thoải mái nếu sau này nới giới hạn |
| `email`: `unique = true, length = 100` | Email là thứ dùng để **đăng nhập**, nên không được trùng |
| `@JsonIgnore` trên `password` | Nhãn của Jackson: "đừng bao giờ đưa field này vào JSON". Lớp bảo vệ thứ 2, phòng khi ai đó lỡ trả thẳng Entity ra API |
| `password`: `length = 255` | Bản băm BCrypt chỉ dài 60 ký tự; để 255 cho dư nếu sau này đổi thuật toán |
| `fullName`, `avatarUrl` không có `nullable = false` | Được phép trống: lúc đăng ký chưa có họ tên, chưa có ảnh |
| `@Enumerated(EnumType.STRING)` | Lưu enum bằng chữ (`'ACTIVE'`), xem A9.4 |
| `= AccountStatus.UNVERIFIED` (giá trị gán sẵn) | Giá trị **mặc định trong Java**: vừa `new User()` là có sẵn. Tài khoản mới luôn bắt đầu ở "chưa xác thực" |
| `role = Role.USER` | Mặc định là người nghe thường |
| `artistRequestStatus = ArtistRequestStatus.NONE` | Mặc định chưa gửi yêu cầu làm nghệ sĩ |
| `accessStatus = AccessStatus.REFUSE` | Mặc định **chưa được vào web**, chờ chủ dự án cho phép |
| `artistName`, `bio` | Chỉ có giá trị khi đã gửi yêu cầu nghệ sĩ hoặc đã là ARTIST |
| Getter / setter | Hibernate và code khác đọc ghi field qua đây (A5.2) |

Bảng `users` **không có** cột thời gian tạo (`created_at`). Điều này giải thích 1 mẹo nhỏ trong `OtpService` (C16).

---

### C4. `entity/OtpVerification.java` — bảng `otp_verifications`

```java
@Entity
@Table(
        name = "otp_verifications",
        indexes = {
                @Index(name = "idx_otp_user_purpose_verified", columnList = "user_id, purpose, verified")
        }
)
public class OtpVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "otp_code", nullable = false, length = 10)
    private String otpCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OtpPurpose purpose;

    @Column(name = "new_email", length = 100)
    private String newEmail;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    @Column(nullable = false)
    private boolean verified = false;

    // ... getter và setter (với boolean, getter tên là isVerified() thay vì getVerified())
}
```

| Code | Giải thích |
|---|---|
| `@Index(... columnList = "user_id, purpose, verified")` | Chỉ mục **3 cột cùng lúc**, khớp đúng câu tìm kiếm duy nhất của bảng này: "OTP mới nhất, chưa dùng, của user X, cho mục đích Y" (C5) |
| `@ManyToOne` | Quan hệ **nhiều – một**: 1 user có thể có nhiều mã OTP (mỗi lần xin mã tạo 1 dòng mới), mỗi mã thuộc đúng 1 user |
| `fetch = FetchType.LAZY` | **Tải lười**: đọc 1 dòng OTP lên thì **chưa** đọc kèm user. Chỉ khi code gọi `otp.getUser().getEmail()` thì Hibernate mới chạy thêm câu SQL lấy user. Tiết kiệm câu SQL không cần thiết |
| `@JoinColumn(name = "user_id", nullable = false)` | Cột khóa ngoại tên `user_id`, trỏ tới `users.id`. Trong Java là cả object `User`, trong database chỉ là 1 con số |
| `otpCode`, `length = 10` | Mã 6 chữ số, lưu dạng **chuỗi** (không phải số) để giữ được số 0 ở đầu: `"004213"`. Lưu số thì thành `4213` |
| `purpose` | Mục đích: REGISTER / RESET_PASSWORD / CHANGE_PASSWORD / CHANGE_EMAIL |
| `newEmail` | **Chỉ** dùng cho CHANGE_EMAIL: nhớ email mới người dùng muốn đổi sang, để bước 2 lấy ra. Các mục đích khác để `null` |
| `expiredAt` | Thời điểm hết hạn = lúc tạo + 5 phút |
| `boolean verified = false` | `true` nghĩa là "mã này **không còn dùng được**", gồm 2 trường hợp: đã dùng thành công, **hoặc** đã bị mã mới hơn thay thế. Dùng `boolean` nguyên thủy (không phải `Boolean`) vì cột này không bao giờ `null` |

Dòng OTP cũ **không bị xóa**, chỉ đánh dấu `verified = true`. Bảng sẽ lớn dần theo thời gian (mục 22).

---

### C5. Repository — `UserRepository` và `OtpVerificationRepository`

```java
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    List<User> findByArtistRequestStatus(ArtistRequestStatus status);

    List<User> findTop10ByRoleAndAccountStatusAndArtistNameContainingIgnoreCaseAndIdNot(
            Role role, AccountStatus accountStatus, String keyword, Long excludeId);
}
```

| Method | SQL tương đương (viết gọn) | Ai dùng |
|---|---|---|
| `findByEmail` | `SELECT * FROM users WHERE email = ?` | Hầu hết mọi nơi: đăng ký, đăng nhập, filter kiểm tra token, `me()` trong UserController... |
| `existsByEmail` | `SELECT 1 ... WHERE email = ? LIMIT 1` | Đổi email: email mới đã có người dùng chưa |
| `existsByUsername` | `SELECT 1 ... WHERE username = ? LIMIT 1` | Đăng ký: username trùng chưa |
| `findByArtistRequestStatus` | `SELECT * FROM users WHERE artist_request_status = ?` | Admin xem yêu cầu nghệ sĩ đang chờ |
| `findTop10ByRole...AndIdNot` | `SELECT * FROM users WHERE role = ? AND account_status = ? AND LOWER(artist_name) LIKE LOWER('%từ khóa%') AND id <> ? LIMIT 10` | **Module Song**: nghệ sĩ tìm nghệ sĩ khác để ghi tên "feat" (`ArtistSongService.searchArtists`) |

Các method có sẵn từ `JpaRepository` mà module User dùng: `findById(id)` (tìm theo id, trả `Optional`), `findAll()` (lấy tất cả), `save(entity)` (thêm hoặc sửa).

Phân tích tên dài nhất:
```
findTop10By  Role  And  AccountStatus  And  ArtistNameContainingIgnoreCase  And  IdNot
└───┬────┘   └┬─┘       └─────┬─────┘       └─────────────┬─────────────┘       └─┬─┘
lấy tối đa  role=?     account_status=?      artist_name chứa từ khóa,         id khác ?
10 dòng                                      không phân biệt hoa thường        (bỏ chính mình)
```
Các tham số truyền vào **theo đúng thứ tự** xuất hiện trong tên.

```java
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {
    Optional<OtpVerification> findFirstByUserAndPurposeAndVerifiedFalseOrderByIdDesc(User user, OtpPurpose purpose);
}
```
Chỉ 1 method, đã phân tích ở A9.5. Chú ý:
- Tham số là cả object `User`, Spring tự hiểu là so sánh `user_id = user.getId()`.
- `VerifiedFalse` không cần tham số: giá trị `false` đã nằm sẵn trong tên.
- `OrderByIdDesc` + `First` = lấy dòng có id **lớn nhất** = dòng **mới nhất** (vì id tự tăng).

---

### C6. DTO request — dữ liệu nhận vào (`dto/auth/`, `dto/user/`)

#### C6.1. Đọc kỹ 1 file mẫu: `RegisterRequest`

```java
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
    // ... getter, setter cho email và password
}
```

Chuyện gì xảy ra khi frontend gửi `{"username": "lan.nguyen", "email": "lan@gmail.com", "password": "matkhau123"}`:

```
1. Jackson tạo object rỗng:     new RegisterRequest()     (constructor mặc định, không tham số)
2. Jackson điền từng trường:    setUsername("lan.nguyen")
                                setEmail("lan@gmail.com")
                                setPassword("matkhau123")
3. @Valid chạy các nhãn kiểm tra trên từng field
4. Hợp lệ → object được đưa vào method register(...) của Controller
```

Vì sao `max = 100` cho email? Để khớp `length = 100` của cột `users.email`. Không kiểm tra ở đây thì email 150 ký tự sẽ đi tới tận database mới bị MySQL từ chối, ra lỗi khó hiểu.

#### C6.2. Tất cả DTO request của module User

| DTO | Dùng cho API | Field và luật kiểm tra |
|---|---|---|
| `RegisterRequest` | Đăng ký | `username` 5–20 ký tự, chỉ `a-z A-Z 0-9 _ .`; `email` đúng dạng, ≤ 100; `password` 6–72 |
| `VerifyOtpRequest` | Xác thực OTP đăng ký | `email` đúng dạng; `otpCode` đúng 6 chữ số |
| `LoginRequest` | Đăng nhập | `email` đúng dạng; `password` không trống (**không** kiểm tra độ dài, để không lộ luật mật khẩu cho kẻ đoán) |
| `ForgotPasswordRequest` | Quên mật khẩu | `email` đúng dạng |
| `ResetPasswordRequest` | Đặt lại mật khẩu | `email`; `otpCode` 6 số; `newPassword` 6–72 |
| `UpdateProfileRequest` | Sửa họ tên | `fullName` không trống, ≤ 100 |
| `RequestChangePasswordOtpRequest` | Xin OTP đổi mật khẩu | `currentPassword` không trống |
| `ChangePasswordRequest` | Đổi mật khẩu | `otpCode` 6 số; `newPassword` 6–72 |
| `RequestUpdateEmailOtpRequest` | Xin OTP đổi email | `newEmail` đúng dạng, ≤ 100 |
| `UpdateEmailRequest` | Đổi email | `otpCode` 6 số |
| `AccountActionRequest` | Vô hiệu hóa / xóa tài khoản | `password` không trống |
| `ArtistRequest` | Gửi yêu cầu nghệ sĩ **và** sửa hồ sơ nghệ sĩ (dùng chung) | `artistName` không trống, ≤ 100; `bio` ≤ 255, được bỏ trống |
| `AssignRoleRequest` | Admin đổi role | `role` không `null`, kiểu `Role` |

Chú ý `AssignRoleRequest` có field kiểu **enum** `Role`. Gửi `{"role": "ADMIN"}` thì Jackson tự đổi chữ `"ADMIN"` thành `Role.ADMIN`. Gửi `{"role": "BOSS"}` (không có trong enum) thì Jackson **không đổi được** → lỗi đọc JSON → 400 `"Invalid request data"` (không phải message của `@NotNull`).

---

### C7. DTO response và `UserMapper` — dữ liệu trả ra

#### C7.1. `LoginResponse`

```java
public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
    private Role role;

    public LoginResponse(String token, Long userId, String username, Role role) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.role = role;
    }
    // ... chỉ có getter (không cần setter: object tạo 1 lần bằng constructor rồi trả ra luôn)
}
```
Jackson gọi `getToken()`, `getUserId()`... để viết JSON `{"token": "...", "userId": 7, "username": "...", "role": "USER"}`. Enum `Role` được viết thành chữ `"USER"`.

#### C7.2. `UserProfileResponse`

10 field: `id`, `username`, `email`, `fullName`, `avatarUrl`, `accountStatus`, `role`, `artistRequestStatus`, `artistName`, `bio`. Constructor nhận đủ 10 giá trị, chỉ có getter.

So với Entity `User` (12 field), response **bỏ** 2 field:
- `password`: bí mật.
- `accessStatus`: chỉ chủ dự án quản lý; người đang đăng nhập được thì chắc chắn đang là `ACCEPT`, trả ra cũng không có thông tin gì thêm.

#### C7.3. `UserMapper`

```java
public class UserMapper {

    private UserMapper() {}

    public static UserProfileResponse toUserProfileResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatarUrl(),
                user.getAccountStatus(),
                user.getRole(),
                user.getArtistRequestStatus(),
                user.getArtistName(),
                user.getBio()
        );
    }
}
```

| Code | Giải thích |
|---|---|
| `private UserMapper() {}` | Constructor `private` → **không ai `new UserMapper()` được**. Class này chỉ là "hộp đựng hàm", không cần object |
| `public static ...` | `static` = gọi thẳng bằng tên class: `UserMapper.toUserProfileResponse(user)`, không cần object |
| Không có `@Component` | Mapper **không phải bean**. Nó không cần đồ nghề gì (không có field), nên không cần Spring quản lý |
| Thân hàm | Lấy từng giá trị từ Entity, truyền vào constructor của DTO **đúng thứ tự** 10 tham số. Sai thứ tự (ví dụ đổi chỗ `username` và `email`) thì Java không báo lỗi vì cùng kiểu `String`, nhưng JSON sẽ sai → cẩn thận khi sửa |

---

### C8. `service/SessionService.java` — phiên đăng nhập trong Redis

```java
@Service
public class SessionService {

    private static final String KEY_PREFIX = "session:";

    private final StringRedisTemplate redisTemplate;
    private final long expirationMs;

    public SessionService(StringRedisTemplate redisTemplate,
                          @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.redisTemplate = redisTemplate;
        this.expirationMs = expirationMs;
    }

    private String key(String email) {
        return KEY_PREFIX + email;
    }

    public String createSession(String email) {
        String sessionId = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(key(email), sessionId, Duration.ofMillis(expirationMs));
        return sessionId;
    }

    public boolean isValidSession(String email, String sessionId) {
        if(sessionId == null) {
            return false;
        }
        String currentSessionId = redisTemplate.opsForValue().get(key(email));
        return sessionId.equals(currentSessionId);
    }

    public void deleteSession(String email) {
        redisTemplate.delete(key(email));
    }
}
```

| Code | Giải thích |
|---|---|
| `KEY_PREFIX = "session:"` | Mọi khóa phiên đều bắt đầu bằng `session:`, để không đụng các khóa khác (`rate:`, `otp-attempt:`) trong cùng Redis |
| Constructor có `@Value("${app.jwt.expiration-ms}")` | Vừa nhận bean `StringRedisTemplate`, vừa nhận 1 giá trị cấu hình. Phiên sống **đúng bằng** thời hạn token (24 giờ), token hết hạn thì phiên cũng tự biến mất |
| `key(email)` | Hàm nhỏ ghép khóa: `"session:" + "lan@gmail.com"` → `"session:lan@gmail.com"`. Để `private` vì chỉ dùng trong class |
| `createSession` | Sinh **UUID** (chuỗi ngẫu nhiên 36 ký tự, gần như không bao giờ trùng), ghi `SET session:{email} = UUID` sống 24 giờ, rồi trả UUID về để đưa vào token (trường `jti`) |
| Ghi đè | `set` lên khóa đã có thì **giá trị cũ mất**. Đăng nhập lần 2 → UUID mới đè UUID cũ → token của lần đăng nhập 1 hết khớp → máy cũ bị đá ra. Đây chính là cơ chế "mỗi tài khoản 1 nơi" (mục 9.2) |
| `isValidSession` | Token không có `jti` → không hợp lệ. Có thì đọc Redis, so 2 chuỗi. Khóa đã bị xóa hoặc hết hạn → `get` trả `null` → `equals` trả `false` |
| `sessionId.equals(currentSessionId)` | Viết `sessionId.equals(...)` chứ không viết `currentSessionId.equals(...)`, vì `currentSessionId` có thể `null` (gọi method trên `null` → lỗi `NullPointerException`), còn `sessionId` đã chắc chắn khác `null` ở dòng trên |
| `deleteSession` | Xóa khóa → mọi token đang có của tài khoản này **mất hiệu lực ngay** ở request tiếp theo. Được gọi khi: đăng xuất, đổi mật khẩu, đặt lại mật khẩu, đổi email, tự vô hiệu hóa, tự xóa, bị Admin khóa |

---

### C9. `security/JwtUtil.java` — tạo và đọc token

```java
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(User user, String sessionId) {
        return Jwts.builder()
                .subject(user.getEmail())
                .id(sessionId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getEmailFromToken(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public String getSessionIdFromToken(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token)
                .getPayload()
                .getId();
    }
}
```

| Code | Giải thích |
|---|---|
| `@Component` | Bean bình thường. Dùng `@Component` thay vì `@Service` vì đây là "công cụ" chứ không phải logic nghiệp vụ. Với Spring, 2 nhãn này **hoạt động giống hệt nhau**, chỉ khác ý nghĩa cho người đọc |
| `Keys.hmacShaKeyFor(secret.getBytes(UTF_8))` | Biến chuỗi bí mật trong file cấu hình thành **khóa ký** (`SecretKey`). Khóa ngắn hơn 32 byte → báo lỗi ngay lúc khởi động (A15.2). Làm 1 lần trong constructor, dùng lại mãi |
| `Jwts.builder()` | Bắt đầu "lắp ráp" 1 token, mỗi dòng `.xxx(...)` thêm 1 thông tin (kiểu viết nối chuỗi method này gọi là builder) |
| `.subject(user.getEmail())` | Trường `sub` = email: token này của ai |
| `.id(sessionId)` | Trường `jti` = mã phiên (UUID từ `SessionService.createSession`) |
| `.issuedAt(new Date())` | Trường `iat` = thời điểm phát |
| `.expiration(new Date(System.currentTimeMillis() + expirationMs))` | Trường `exp` = bây giờ + 24 giờ. `System.currentTimeMillis()` = số mili giây từ 1/1/1970 tới giờ |
| `.signWith(key)` | Ký bằng khóa bí mật, thuật toán tự chọn theo độ dài khóa (HS512 với khóa ≥ 64 byte) |
| `.compact()` | Ghép thành chuỗi `header.payload.signature` |
| `validateToken` | `parseSignedClaims` kiểm tra **cùng lúc**: đúng cấu trúc, chữ ký đúng, chưa hết hạn. Bất kỳ sai sót nào → thư viện ném lỗi → `catch` → `false`. Không phân biệt "sai chữ ký" hay "hết hạn": với dự án, sai kiểu gì cũng là "không hợp lệ" |
| `getEmailFromToken`, `getSessionIdFromToken` | Đọc lại `sub` và `jti`. Mỗi hàm tự kiểm tra chữ ký lại 1 lần nữa (`verifyWith(key)`), nên không bao giờ đọc nhầm dữ liệu từ token giả |

---

### C10. `security/CustomUserDetailsService.java` — nạp user cho Spring Security

Spring Security không biết gì về Entity `User` của dự án. Nó chỉ hiểu kiểu `UserDetails` của nó (gồm: tên đăng nhập, mật khẩu, quyền, có bị vô hiệu không). Class này là "phiên dịch viên" giữa 2 bên.

```java
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .disabled(user.getAccountStatus() != AccountStatus.ACTIVE
                        || user.getAccessStatus() != AccessStatus.ACCEPT)
                .build();
    }
}
```

| Code | Giải thích |
|---|---|
| `implements UserDetailsService` | Interface của Spring Security, bắt buộc có method `loadUserByUsername` |
| `@Override` | "Method này là bản cài đặt của method trong interface". Viết sai tên thì Java báo lỗi ngay |
| `loadUserByUsername(String email)` | Tên method do Spring đặt (`Username`), nhưng dự án truyền **email** vào, vì dự án đăng nhập bằng email |
| `org.springframework.security.core.userdetails.User` | Viết **đầy đủ tên package**, vì trùng tên class `User` của dự án (`com.winhmm.myspotify.entity.User`). Không viết đầy đủ thì Java không biết `User` nào |
| `.withUsername(user.getEmail())` | "Tên đăng nhập" trong mắt Spring = email. Đây là lý do `authentication.getName()` ở Controller trả về **email** |
| `.password(...)` | Bản băm. Dự án không dùng tới ở đây (tự so mật khẩu trong `AuthService.login`), nhưng Spring yêu cầu phải có |
| `.roles(user.getRole().name())` | `Role.ADMIN.name()` = chuỗi `"ADMIN"` → Spring tự thêm tiền tố → quyền `ROLE_ADMIN` (A13.4) |
| `.disabled(...)` | Đánh dấu "bị vô hiệu" nếu **1 trong 2**: tài khoản không ở trạng thái ACTIVE, **hoặc** không được phép vào web. `||` nghĩa là "hoặc" |
| `.build()` | Tạo ra object `UserDetails` |

Class này được gọi ở **mọi request có token** (C11). Vì nó đọc MySQL mỗi lần, nên khóa tài khoản hay thu hồi quyền vào web đều có tác dụng **ngay request tiếp theo**.

---

### C11. `security/JwtAuthenticationFilter.java` — kiểm tra token mỗi request

```java
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String SESSION_ERROR = "sessionError";

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final SessionService sessionService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil,
                                   CustomUserDetailsService userDetailsService,
                                   SessionService sessionService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.sessionService = sessionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");                        // ①

        if (header != null && header.startsWith("Bearer ")) {                      // ②
            String token = header.substring(7);                                     // ③

            if (jwtUtil.validateToken(token)) {                                     // ④
                try {
                    String email = jwtUtil.getEmailFromToken(token);                // ⑤
                    String sessionId = jwtUtil.getSessionIdFromToken(token);

                    if (!sessionService.isValidSession(email, sessionId)) {          // ⑥
                        request.setAttribute(SESSION_ERROR,
                                "Your session has ended. Your account may have been logged in on another device");
                    } else {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(email); // ⑦

                        if (userDetails.isEnabled()) {                               // ⑧
                            SecurityContextHolder.getContext().setAuthentication(
                                    new UsernamePasswordAuthenticationToken(
                                            userDetails, null, userDetails.getAuthorities()));
                        }
                    }
                } catch (Exception e) {}                                             // ⑨
            }
        }

        filterChain.doFilter(request, response);                                    // ⑩
    }
}
```

| Bước | Giải thích |
|---|---|
| `extends OncePerRequestFilter` | Kế thừa 1 loại filter có sẵn của Spring, đảm bảo **mỗi request chỉ chạy filter này 1 lần**. Chỉ cần viết method `doFilterInternal` |
| `SESSION_ERROR = "sessionError"` | Tên 1 "ghi chú" dán vào request (bước ⑥), để `SecurityConfig` đọc lại khi viết lỗi 401. `public static` để class khác dùng chung đúng tên |
| ① | Đọc header `Authorization` |
| ② | Không có header, hoặc không bắt đầu bằng `"Bearer "` (có dấu cách) → bỏ qua cả khối `if`, đi thẳng tới ⑩ với tư cách **khách** |
| ③ | `substring(7)`: cắt bỏ 7 ký tự đầu (`B-e-a-r-e-r-dấu cách`), còn lại đúng chuỗi token |
| ④ | Token sai chữ ký / hết hạn / sai cấu trúc → bỏ qua, coi như khách |
| ⑤ | Đọc email và mã phiên trong token |
| ⑥ | Mã phiên không khớp Redis (đăng nhập nơi khác, đã đăng xuất, bị khóa...) → **dán ghi chú lỗi** vào request, **không** ghi nhận đăng nhập. Request vẫn đi tiếp như khách; nếu API cần đăng nhập thì 401 kèm đúng ghi chú này |
| ⑦ | Đọc user từ MySQL qua C10 |
| ⑧ | Chỉ ghi nhận đăng nhập nếu tài khoản **không bị vô hiệu** (ACTIVE và ACCEPT). `UsernamePasswordAuthenticationToken(userDetails, null, quyền)` = tờ giấy "người gọi là ai, quyền gì" (A13.3). Tham số giữa (`null`) là mật khẩu, không cần vì đã xác thực bằng token |
| ⑨ | Bất kỳ lỗi gì (ví dụ user đã bị xóa khỏi database → `UsernameNotFoundException`) → **nuốt lỗi**, coi như khách. Filter **không bao giờ tự trả lỗi**; quyết định 401 hay không để cho trạm cuối (luật URL trong `SecurityConfig`). Nhờ vậy API công khai như `/api/auth/login` vẫn chạy được dù người dùng gửi kèm 1 token hỏng |
| ⑩ | **Luôn** chuyển request cho trạm tiếp theo. Quên dòng này thì mọi request "chết" giữa đường, không bao giờ tới Controller |

Filter **không có** `@Component`. Vì sao: xem C13.

---

### C12. `RateLimitService` và `RateLimitFilter` — chống gọi quá nhiều

#### C12.1. `service/RateLimitService.java`

```java
@Service
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;
    private final int windowSeconds;
    private final int userRequests;
    private final int authRequests;

    public RateLimitService (
            StringRedisTemplate redisTemplate,
            @Value("${app.rate-limit.window-seconds}") int windowSeconds,
            @Value("${app.rate-limit.user-requests}") int userRequests,
            @Value("${app.rate-limit.auth-requests}") int authRequests
    ) { /* gán 4 field */ }

    public boolean tryAcquire(String key, int limit) {
        Long count = redisTemplate.opsForValue().increment(key);
        if(count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
        }
        return count != null && count <= limit;
    }

    public long getRetryAfterSeconds(String key) {
        Long ttl = redisTemplate.getExpire(key);
        if(ttl == null || ttl < 1) {
            return 1;
        } else {
            return ttl;
        }
    }

    public int getUserRequests() { return userRequests; }
    public int getAuthRequests() { return authRequests; }
}
```

Cách đếm kiểu "cửa sổ cố định" (fixed window), ví dụ giới hạn 10 lần / 60 giây:

```
Request 1  → INCR rate:auth:1.2.3.4 → 1  → là lần đầu → EXPIRE 60 giây → cho qua
Request 2  → INCR                   → 2  → cho qua
...
Request 10 → INCR                   → 10 → cho qua (10 ≤ 10)
Request 11 → INCR                   → 11 → CHẶN (11 > 10) → 429
... 60 giây kể từ request 1 trôi qua → Redis tự xóa khóa → đếm lại từ đầu
```

| Code | Giải thích |
|---|---|
| `increment(key)` | Lệnh `INCR`: cộng 1 và trả về số mới, **nguyên tử** (atomic): 100 request cùng lúc thì Redis vẫn xếp hàng cộng lần lượt, không bao giờ 2 request cùng đọc ra 1 số |
| `count == 1` → `expire` | Chỉ đặt hạn ở **lần đầu tiên** của cửa sổ. Nếu lần nào cũng đặt lại hạn, người dùng gọi đều đều sẽ bị gia hạn mãi, không bao giờ được đếm lại |
| `count <= limit` | Còn trong giới hạn → `true` (cho qua) |
| `getRetryAfterSeconds` | Đọc thời gian còn lại của khóa (`TTL`) để báo người dùng "chờ bao lâu". Tối thiểu trả 1 giây |

#### C12.2. `security/RateLimitFilter.java`

```java
public class RateLimitFilter extends OncePerRequestFilter {

    // ... field + constructor nhận RateLimitService

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || path.startsWith("/uploads/")
                || path.startsWith("/actuator/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String ip = request.getRemoteAddr();
        String key;
        int limit;

        if(path.startsWith("/api/auth/")) {
            key = "rate:auth:" + ip;
            limit = rateLimitService.getAuthRequests();
        } else {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if(auth != null && auth.isAuthenticated()
                    && !(auth instanceof AnonymousAuthenticationToken)) {
                key = "rate:user:" + auth.getName();
            } else {
                key = "rate:ip:" + ip;
            }
            limit = rateLimitService.getUserRequests();
        }

        boolean allowed;
        try {
            allowed = rateLimitService.tryAcquire(key, limit);
        } catch (Exception e) {
            allowed = true;
        }

        if(!allowed) {
            long retryAfter = rateLimitService.getRetryAfterSeconds(key);
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"message\":\"Too many requests. Please try again in "
                            + retryAfter + " seconds\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}
```

| Code | Giải thích |
|---|---|
| `shouldNotFilter` | Trả `true` = **bỏ qua** filter cho request này. Bỏ qua: request `OPTIONS` của CORS (trình duyệt tự gửi, không tính), file tĩnh `/uploads/...` (1 trang có thể tải hàng chục ảnh), trang kiểm tra sức khỏe, trang Swagger |
| `request.getRemoteAddr()` | Địa chỉ IP người gọi |
| Nhánh `/api/auth/` | Đăng ký, đăng nhập, quên mật khẩu: đếm theo **IP**, giới hạn chặt (**10**). Chống đoán mật khẩu và chống spam email OTP |
| Nhánh còn lại, đã đăng nhập | Đếm theo **email** (`auth.getName()`), giới hạn **100**. Đếm theo tài khoản công bằng hơn: nhiều người chung 1 mạng Wi-Fi (chung IP) không ảnh hưởng nhau |
| `!(auth instanceof AnonymousAuthenticationToken)` | Phòng hờ trường hợp SecurityContext chứa 1 "khách vô danh" (Spring Security có loại `Authentication` riêng cho khách). Khách thì không đếm theo email được |
| Nhánh còn lại, là khách | Đếm theo IP, giới hạn 100 |
| `try { ... } catch → allowed = true` | Redis bị tắt / lỗi → **cho qua** thay vì chặn hết. Thà mất tạm tính năng chống spam còn hơn cả web ngừng chạy |
| Bị chặn | Tự viết response 429: header `Retry-After` + JSON message, rồi `return` **không** gọi `doFilter` → request dừng ở đây, không tới Controller |
| Vị trí | Đứng **sau** `JwtAuthenticationFilter` (C13). Phải vậy thì lúc chạy, SecurityContext đã có thông tin người dùng để đếm theo email |

---

### C13. `config/SecurityConfig.java` — luật bảo mật tổng

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final SessionService sessionService;
    private final RateLimitService rateLimitService;

    // ... constructor nhận 4 bean trên

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())                                          // ①
                .cors(Customizer.withDefaults())                                       // ②
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // ③
                .authorizeHttpRequests(auth -> auth                                    // ④
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**", "/uploads/**", "/actuator/health",
                                "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
                                "/error").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex                                            // ⑤
                        .authenticationEntryPoint((request, response, authException) -> {
                            Object sessionError = request.getAttribute(JwtAuthenticationFilter.SESSION_ERROR);
                            String message = (sessionError != null)
                                    ? sessionError.toString()
                                    : "Unauthorized, please login";
                            writeJsonError(response, 401, message);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeJsonError(response, 403, "You do not have permission to access this resource"))
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil, userDetailsService, sessionService), // ⑥
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new RateLimitFilter(rateLimitService),                 // ⑦
                        JwtAuthenticationFilter.class);

        return http.build();
    }

    private void writeJsonError(HttpServletResponse response, int status,
                                String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {                                         // ⑧
        return new BCryptPasswordEncoder();
    }
}
```

| Phần | Giải thích |
|---|---|
| `@Configuration` | Class chứa các method `@Bean` (A7.3) |
| `@EnableWebSecurity` | Bật Spring Security với cấu hình viết ở đây |
| `@EnableMethodSecurity` | Bật `@PreAuthorize`. **Thiếu nhãn này thì `@PreAuthorize("hasRole('ADMIN')")` trên AdminController bị bỏ qua**, USER thường gọi được API admin |
| `@Bean SecurityFilterChain filterChain(HttpSecurity http)` | Spring đưa vào 1 "bảng điều khiển" `http`, ta chỉnh từng mục, cuối cùng `http.build()` tạo ra dãy filter |
| ① `csrf.disable()` | Tắt chống CSRF (lý do ở A13.5) |
| ② `cors(Customizer.withDefaults())` | Bật CORS, dùng bean `CorsConfigurationSource` trong `CorsConfig` (C14) |
| ③ `STATELESS` | Không tạo phiên kiểu cookie `JSESSIONID` |
| ④ Luật URL | Đọc **từ trên xuống, luật nào khớp trước thì dùng luật đó**. (1) Mọi request `OPTIONS` → cho qua. (2) Các đường dẫn công khai → cho qua (`permitAll`). (3) **Mọi thứ còn lại** → phải đăng nhập (`authenticated`). Vì vậy luật chung `anyRequest()` phải đặt **cuối cùng**; đặt lên đầu thì khóa luôn cả trang đăng nhập |
| `/**` | Ký hiệu "mọi đường dẫn con, bao nhiêu cấp cũng được". `/api/auth/**` khớp `/api/auth/login`, `/api/auth/a/b/c` |
| `/error` | Đường dẫn nội bộ Spring dùng khi có lỗi; không mở thì lỗi thật bị che thành 401 |
| ⑤ `authenticationEntryPoint` | Được gọi khi **khách** gọi API cần đăng nhập → viết **401**. Có ghi chú `sessionError` (C11 bước ⑥) thì dùng ghi chú đó, không thì dùng câu chung `"Unauthorized, please login"` |
| ⑤ `accessDeniedHandler` | Được gọi khi **đã đăng nhập nhưng thiếu quyền** → viết **403** |
| `writeJsonError` | Tự viết JSON `{"message":"..."}` cùng dạng với `GlobalExceptionHandler`, để frontend đọc lỗi 1 kiểu duy nhất |
| ⑥ `addFilterBefore(new JwtAuthenticationFilter(...), UsernamePasswordAuthenticationFilter.class)` | Chèn filter kiểm tra token vào **trước** filter đăng nhập bằng form mặc định của Spring |
| ⑦ `addFilterAfter(new RateLimitFilter(...), JwtAuthenticationFilter.class)` | Chèn filter rate limit ngay **sau** filter token (lý do ở C12.2) |
| Vì sao `new` chứ không dán `@Component` lên 2 filter? | Spring Boot có thói quen: **bean nào là Filter thì tự đăng ký thêm 1 lần ở tầng Tomcat**, nằm **ngoài** dãy của Spring Security. Khi đó filter nằm ở 2 nơi: dễ chạy **sai vị trí** (ví dụ `RateLimitFilter` chạy trước khi biết người gọi là ai → mọi request bị đếm theo IP) hoặc chạy **2 lần**. Tạo bằng `new` ở đây thì chúng chỉ nằm trong dãy Spring Security, đúng 1 lần, đúng vị trí. Đồ nghề cần thiết thì truyền qua constructor bằng tay |
| ⑧ `@Bean PasswordEncoder` | Tạo bean BCrypt (độ khó mặc định 10). `AuthService`, `UserService` nhận bean này qua constructor. Muốn đổi thuật toán chỉ sửa 1 dòng này |

Thứ tự đầy đủ của 1 request qua dãy filter: mục 10.

---

### C14. `CorsConfig` và `WebMvcConfig`

#### C14.1. `config/CorsConfig.java`

```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of(
                "Authorization", "Content-Disposition", "Content-Range",
                "Accept-Ranges", "Content-Length", "Retry-After"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

| Dòng | Giải thích |
|---|---|
| `setAllowedOriginPatterns(List.of("*"))` | Trang web ở **domain nào** cũng được gọi API (localhost lúc dev, Vercel lúc chạy thật). Dùng "pattern" vì kiểu `setAllowedOrigins("*")` không đi chung được với `setAllowCredentials(true)` |
| `setAllowedMethods(...)` | Các method được phép |
| `setAllowedHeaders("*")` | Frontend gửi header nào cũng được (`Authorization`, `Content-Type`...) |
| `setExposedHeaders(...)` | Mặc định JavaScript trên trình duyệt **không đọc được** phần lớn header của response. Liệt kê ở đây thì đọc được. Liên quan module User: `Retry-After` (lỗi 429). Các header còn lại phục vụ phát nhạc của module Song |
| `setAllowCredentials(true)` | Cho phép gửi kèm thông tin đăng nhập |
| `setMaxAge(3600L)` | Trình duyệt nhớ kết quả "hỏi trước" (preflight) trong 1 giờ, đỡ phải hỏi lại mỗi request |
| `registerCorsConfiguration("/**", config)` | Áp dụng cho mọi đường dẫn |

#### C14.2. `config/WebMvcConfig.java`

```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Value("${app.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadDir).toAbsolutePath().toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
    }
}
```
Dịch: "request tới `/uploads/...` thì lấy **file thật** trong thư mục `app.upload-dir` trên ổ đĩa trả về". Ví dụ `avatarUrl = "/uploads/avatars/9f1c...e2.jpg"` → mở `http://<server>:8080/uploads/avatars/9f1c...e2.jpg` là thấy ảnh. `setCachePeriod(3600)`: trình duyệt được giữ ảnh trong bộ nhớ đệm 1 giờ.

`@Value` ở đây gắn thẳng trên **field** (không qua constructor). Cách này cũng chạy được; dự án dùng constructor ở hầu hết nơi khác.

---

### C15. `EmailService` và `EmailSendException`

#### C15.1. `service/EmailService.java`

```java
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtp(String toEmail, String otpCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("MySpotify <" + fromEmail + ">");
            message.setTo(toEmail);
            message.setSubject("MySpotify - Mã xác thực OTP");
            message.setText(
                    "Xin chào,\n\n"
                            + "Mã OTP của bạn là: " + otpCode + "\n\n"
                            + "Mã có hiệu lực trong 5 phút và chỉ sử dụng được 1 lần.\n\n"
                            + "Nếu bạn không yêu cầu mã này, hãy bỏ qua email.\n\n"
                            + "MySpotify"
            );
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Could not send OTP email to {}: {}", toEmail, e.getMessage());
            throw new EmailSendException("Could not send OTP email, please try again later", e);
        }
    }
}
```

| Code | Giải thích |
|---|---|
| `JavaMailSender mailSender` | Bean do Spring Boot tự tạo từ các khóa `spring.mail.*` (A16.2) |
| `Logger log` | Công cụ ghi log (nhật ký) ra file / màn hình. `log.error(...)` ghi 1 dòng mức lỗi. `{}` là chỗ điền giá trị theo thứ tự |
| `@Value("${spring.mail.username}") fromEmail` | Địa chỉ Gmail dùng để gửi, đọc từ cấu hình |
| `SimpleMailMessage` | Email **chữ thường** (không có HTML, không có ảnh) |
| `setFrom("MySpotify <" + fromEmail + ">")` | Người nhận thấy tên người gửi là "MySpotify" thay vì chỉ thấy địa chỉ Gmail |
| `\n` | Ký tự xuống dòng |
| `try ... catch (Exception e)` | Bắt **mọi** lỗi khi gửi: sai App Password, quá thời gian 5 giây, Gmail từ chối, mất mạng... |
| `log.error(...)` | Ghi lý do thật vào log cho người làm backend xem. Người dùng **không** thấy chi tiết này |
| `throw new EmailSendException(..., e)` | Ném lỗi riêng của dự án, kèm lỗi gốc `e` (để log còn truy được nguyên nhân) |

#### C15.2. `exception/EmailSendException.java`

```java
public class EmailSendException extends RuntimeException {
    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
```
- `extends RuntimeException`: là `RuntimeException` → `@Transactional` **rollback** khi gặp nó (A9.7). Nhờ vậy gửi email lỗi thì tài khoản vừa tạo / mã OTP vừa lưu đều bị hoàn tác.
- `super(message, cause)`: gọi constructor của lớp cha để lưu message và lỗi gốc.
- `GlobalExceptionHandler` có 1 method riêng cho loại lỗi này → trả **503** kèm message (C24).

Vì sao cần 1 loại lỗi riêng thay vì dùng lại `IllegalArgumentException`? Vì 2 loại lỗi **khác bản chất**: `IllegalArgumentException` = "người dùng làm sai" (400), còn lỗi email = "server gặp sự cố, người dùng không có lỗi, thử lại sau" (503). Frontend dựa vào mã để hiển thị cho đúng.

---

### C16. `service/OtpService.java` — tạo, gửi, kiểm tra mã OTP

Đây là file "khó" nhất của module, đọc chậm.

#### C16.1. Khai báo

```java
@Service
public class OtpService {

    private final OtpVerificationRepository otpVerificationRepository;
    private final EmailService emailService;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int OTP_EXPIRE_MINUTES = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 60;
    private final StringRedisTemplate redisTemplate;
    private static final int MAX_FAILED_ATTEMPTS = 5;

    // ... constructor nhận 3 bean: OtpVerificationRepository, EmailService, StringRedisTemplate
```

| Hằng số | Giá trị | Ý nghĩa |
|---|---|---|
| `RANDOM` | `new SecureRandom()` | Bộ sinh số ngẫu nhiên an toàn (A16.1). `static` → cả ứng dụng dùng chung 1 cái |
| `OTP_EXPIRE_MINUTES` | 5 | Mã sống 5 phút |
| `RESEND_COOLDOWN_SECONDS` | 60 | Phải chờ 60 giây mới được xin mã mới |
| `MAX_FAILED_ATTEMPTS` | 5 | Mỗi mã được thử tối đa 5 lần |

Muốn đổi các con số này → sửa ở đây, mọi nơi dùng tự đổi theo. (Nhớ sửa cả câu "5 phút" trong nội dung email ở `EmailService`.)

#### C16.2. Hai cửa vào `generateAndSend`

```java
    @Transactional
    public void generateAndSend(User user, OtpPurpose purpose) {
        createAndSend(user, purpose, null, user.getEmail());
    }

    @Transactional
    public void generateAndSend(User user, OtpPurpose purpose, String newEmail) {
        createAndSend(user, purpose, newEmail, newEmail);
    }
```
Hai method **cùng tên, khác số tham số** (Java gọi là **overload**). Java tự chọn bản nào tùy lúc gọi truyền 2 hay 3 tham số.

| Bản | Dùng cho | Gửi mã tới | Lưu `newEmail` |
|---|---|---|---|
| 2 tham số | REGISTER, RESET_PASSWORD, CHANGE_PASSWORD | Email **hiện tại** của user | `null` |
| 3 tham số | CHANGE_EMAIL | Email **mới** (để chứng minh người dùng sở hữu email mới) | Email mới |

Cả 2 đều gọi 1 hàm chung `createAndSend` (tránh viết lặp code).

#### C16.3. `createAndSend` — 5 bước

```java
    private void createAndSend(User user, OtpPurpose purpose, String newEmail, String toEmail) {
        LocalDateTime now = LocalDateTime.now();

        // BƯỚC 1: kiểm tra thời gian chờ 60 giây
        Optional<OtpVerification> latest = otpVerificationRepository
                .findFirstByUserAndPurposeAndVerifiedFalseOrderByIdDesc(user, purpose);
        if(latest.isPresent()) {
            LocalDateTime createdAt = latest.get().getExpiredAt().minusMinutes(OTP_EXPIRE_MINUTES);
            if(createdAt.plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(now)) {
                throw new IllegalArgumentException(
                        "Please wait " + RESEND_COOLDOWN_SECONDS + " seconds before requesting a new OTP");
            }
        }

        // BƯỚC 2: vô hiệu mọi mã cũ chưa dùng
        Optional<OtpVerification> old = latest;
        while(old.isPresent()) {
            OtpVerification otp = old.get();
            otp.setVerified(true);
            otpVerificationRepository.save(otp);
            old = otpVerificationRepository
                    .findFirstByUserAndPurposeAndVerifiedFalseOrderByIdDesc(user, purpose);
        }

        // BƯỚC 3: sinh mã 6 chữ số
        String otpCode = String.format("%06d", RANDOM.nextInt(1_000_000));

        // BƯỚC 4: lưu mã mới
        OtpVerification otp = new OtpVerification();
        otp.setUser(user);
        otp.setOtpCode(otpCode);
        otp.setPurpose(purpose);
        otp.setNewEmail(newEmail);
        otp.setExpiredAt(now.plusMinutes(OTP_EXPIRE_MINUTES));
        otp.setVerified(false);
        otpVerificationRepository.save(otp);

        // BƯỚC 5: gửi email
        emailService.sendOtp(toEmail, otpCode);
    }
```
(Chú thích `// BƯỚC ...` do tài liệu thêm vào cho dễ theo dõi.)

**Bước 1 — mẹo tính "thời điểm tạo mã".** Bảng không có cột `created_at` (C4). Nhưng `expiredAt` luôn = lúc tạo + 5 phút, nên **lúc tạo = `expiredAt` − 5 phút**. Ví dụ:

```
Mã cũ: expiredAt = 10:05:00  →  createdAt = 10:00:00  →  hết thời gian chờ lúc 10:01:00
Bây giờ 10:00:40  → 10:01:00 vẫn sau bây giờ (isAfter = true)  → báo "Please wait 60 seconds..."
Bây giờ 10:01:05  → 10:01:00 đã qua                            → cho tạo mã mới
```
Message luôn ghi "60 seconds" dù có thể chỉ còn vài giây; frontend nên tự đếm ngược 60 giây kể từ lúc gửi thành công (mục 19.3).

**Bước 2 — vì sao là vòng `while` chứ không chỉ sửa 1 mã?** Bình thường chỉ có tối đa 1 mã chưa dùng. Nhưng nếu 2 request xin mã gửi tới cùng 1 lúc, có thể lọt 2 mã. Vòng lặp "cứ còn mã chưa dùng là đánh dấu tiếp" đảm bảo sau bước này **không còn mã cũ nào dùng được**. Vòng lặp dừng được vì trong cùng transaction, trước mỗi lần tìm Hibernate ghi các thay đổi đang chờ xuống database, nên lần tìm sau không thấy lại mã vừa đánh dấu.

**Bước 3** — xem A16.1.

**Bước 4** — `verified = false`: mã mới, chưa dùng. `save` → `INSERT`.

**Bước 5** — gửi email **sau cùng**. Gửi lỗi → `EmailSendException` → transaction rollback: mã mới bị xóa, các mã cũ ở bước 2 được **trả lại trạng thái chưa dùng**. Người dùng có thể bấm gửi lại.

#### C16.4. `verifyAndConsume` — kiểm tra và "tiêu" mã

```java
    public OtpVerification verifyAndConsume(User user, String otpCode, OtpPurpose purpose) {
        OtpVerification otp = otpVerificationRepository
                .findFirstByUserAndPurposeAndVerifiedFalseOrderByIdDesc(user, purpose)
                .orElseThrow(() -> new IllegalArgumentException("Otp code does not exist"));    // ①

        if(otp.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Otp code has expired");                         // ②
        }

        String attemptKey = "otp-attempt:" + otp.getId();
        Long attempts = redisTemplate.opsForValue().increment(attemptKey);                      // ③
        if(attempts != null && attempts == 1) {
            redisTemplate.expire(attemptKey, Duration.ofMinutes(OTP_EXPIRE_MINUTES));
        }
        if(attempts == null || attempts > MAX_FAILED_ATTEMPTS) {
            throw new IllegalArgumentException("Too many wrong attempts, please request a new OTP"); // ④
        }

        if(!otp.getOtpCode().equals(otpCode)) {
            throw new IllegalArgumentException("Otp code does not match");                      // ⑤
        }

        redisTemplate.delete(attemptKey);                                                       // ⑥
        otp.setVerified(true);
        return otpVerificationRepository.save(otp);
    }
```

| Bước | Giải thích |
|---|---|
| ① | Không có mã nào chưa dùng cho (user, mục đích) này → chưa từng xin mã, hoặc mã đã dùng rồi |
| ② | `expiredAt` trước bây giờ → mã đã quá 5 phút |
| ③ | **Tăng bộ đếm trước, so mã sau.** Lệnh `INCR` của Redis là nguyên tử (C12.1). Nếu làm ngược lại (so mã trước, sai mới tăng đếm), kẻ xấu gửi 20 lần đoán **cùng 1 lúc** thì cả 20 đều đọc được "đếm = 0" → cả 20 được so mã. Tăng trước thì 20 request nhận lần lượt các số 1, 2, ..., 20 → chỉ 5 request đầu được so. Test tự động `OtpRaceConditionTest` kiểm tra đúng điều này: 20 lần nhập sai song song, chỉ đúng 5 lần được so mã |
| `attempts == 1` → `expire` | Bộ đếm sống 5 phút (bằng tuổi thọ mã), sau đó Redis tự dọn |
| ④ | Từ lần thứ **6** trở đi bị chặn, **kể cả khi nhập đúng**. Cách duy nhất: xin mã mới (mã mới có id mới → khóa đếm mới, bắt đầu lại từ 0) |
| ⑤ | So 2 chuỗi. Sai → báo lỗi; bộ đếm Redis **vẫn giữ** số lần vừa tăng (Redis không bị rollback, A9.7) |
| ⑥ | Đúng mã → xóa bộ đếm, đánh dấu mã đã dùng (`verified = true`) → không dùng lại được lần 2. Trả object mã về cho nơi gọi (đổi email cần đọc `getNewEmail()` từ đây) |

Method này **không có** `@Transactional` riêng: nó luôn được gọi từ 1 method đã có `@Transactional` (`verifyOtp`, `resetPassword`, `changePassword`, `updateEmail`) nên chạy chung transaction của method đó.

Redis bị tắt → bước ③ ném lỗi kết nối → **500**. Nghĩa là mọi thao tác nhập OTP cần Redis chạy.

---

### C17. `service/AuthService.java` — đăng ký, xác thực, đăng nhập, quên mật khẩu

Constructor nhận 5 bean: `UserRepository`, `OtpService`, `PasswordEncoder`, `JwtUtil`, `SessionService` (đã giải thích ở A7.2).

#### C17.1. `register` — đăng ký

```java
    @Transactional
    public void register(String username, String email, String password) {
        Optional<User> existing = userRepository.findByEmail(email);

        if (existing.isPresent()) {                                                 // NHÁNH A: email đã có trong database
            User oldUser = existing.get();
            if (oldUser.getAccountStatus() != AccountStatus.UNVERIFIED) {
                throw new IllegalArgumentException("Email already exists");          // A1
            }
            if (!oldUser.getUsername().equalsIgnoreCase(username)
                    && userRepository.existsByUsername(username)) {
                throw new IllegalArgumentException("Username already exists");       // A2
            }
            oldUser.setUsername(username);                                           // A3
            oldUser.setPassword(passwordEncoder.encode(password));
            userRepository.save(oldUser);
            otpService.generateAndSend(oldUser, OtpPurpose.REGISTER);
            return;
        }

        if (userRepository.existsByUsername(username)) {                            // NHÁNH B: email mới hoàn toàn
            throw new IllegalArgumentException("Username already exists");          // B1
        }

        User user = new User();                                                      // B2
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setAccountStatus(AccountStatus.UNVERIFIED);
        user.setRole(Role.USER);
        user.setAccessStatus(AccessStatus.REFUSE);
        userRepository.save(user);
        otpService.generateAndSend(user, OtpPurpose.REGISTER);                       // B3
    }
```

Vì sao có **nhánh A**? Tình huống: Lan đăng ký nhưng không nhận được email, hoặc quên nhập mã, bỏ đó vài ngày. Database đã có dòng của Lan ở trạng thái `UNVERIFIED`. Nếu báo "Email already exists" thì Lan **mắc kẹt vĩnh viễn**: không đăng ký lại được, cũng không đăng nhập được. Nên dự án cho **đăng ký đè** lên dòng chưa xác thực.

| Bước | Giải thích |
|---|---|
| A1 | Email đã có và tài khoản **đã xác thực** (hoặc đã khóa, đã xóa) → không cho đăng ký đè |
| A2 | Đổi sang username khác mà username mới thuộc **người khác** → báo trùng. `equalsIgnoreCase` = so không phân biệt hoa thường: giữ username cũ (hoặc chỉ đổi hoa thường) thì không cần kiểm tra |
| A3 | Ghi đè username, mật khẩu mới (đã băm), `save` → `UPDATE`, gửi mã mới. Vẫn chịu luật chờ 60 giây: đăng ký đè quá nhanh → `Please wait 60 seconds...` |
| `return;` | Kết thúc method sớm, không chạy xuống nhánh B |
| B1 | Username đã có người dùng → báo trùng |
| B2 | Tạo user mới. Các dòng `setAccountStatus`, `setRole`, `setAccessStatus` thật ra trùng giá trị mặc định trong Entity (C3), viết lại cho rõ ý |
| `passwordEncoder.encode(password)` | **Không bao giờ** lưu mật khẩu gốc (A14) |
| B3 | Gửi mã. Gửi lỗi → `EmailSendException` → **rollback**: dòng user vừa `INSERT` bị hoàn tác, như chưa từng đăng ký |

Hai người đăng ký cùng username **cùng 1 tích tắc**: cả 2 qua được `existsByUsername` (lúc đó chưa ai lưu), người lưu sau bị MySQL từ chối vì cột unique → **409** `Data already exists` (mục 17.3).

#### C17.2. `verifyOtp` — xác thực mã đăng ký

```java
    @Transactional
    public void verifyOtp(String email, String otpCode) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email does not exist"));
        if (user.getAccountStatus() != AccountStatus.UNVERIFIED) {
            throw new IllegalArgumentException("Account is already verified");
        }
        otpService.verifyAndConsume(user, otpCode, OtpPurpose.REGISTER);
        user.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(user);
    }
```
Tìm user → phải đang `UNVERIFIED` → kiểm tra mã (C16.4) → chuyển `ACTIVE`. Chú ý: `accessStatus` **vẫn là `REFUSE`**, chưa đăng nhập được cho tới khi chủ dự án cho phép (mục 7.3a).

#### C17.3. `login` — đăng nhập

```java
    public LoginResponse login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email or password is incorrect")); // ①
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Email or password is incorrect");                    // ②
        }
        if (user.getAccountStatus() == AccountStatus.UNVERIFIED) {
            throw new IllegalArgumentException("Account is not verified yet");                       // ③
        }
        if (user.getAccountStatus() == AccountStatus.DISABLED) {
            throw new IllegalArgumentException("Account has been disabled");                         // ④
        }
        if (user.getAccountStatus() == AccountStatus.DELETED) {
            throw new IllegalArgumentException("Account has been deleted");                          // ⑤
        }
        if (user.getAccessStatus() != AccessStatus.ACCEPT) {
            throw new IllegalArgumentException("Account has not been approved yet");                 // ⑥
        }
        String sessionId = sessionService.createSession(user.getEmail());                           // ⑦
        String token = jwtUtil.generateToken(user, sessionId);                                       // ⑧
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getRole());          // ⑨
    }
```

| Bước | Giải thích |
|---|---|
| ①② | Sai email **hoặc** sai mật khẩu → **cùng 1 câu**. Báo riêng thì kẻ xấu thử hàng loạt email sẽ biết email nào đã đăng ký |
| Thứ tự ①② trước ③–⑥ | Trạng thái tài khoản chỉ được tiết lộ cho người **nhập đúng mật khẩu**. Người lạ đoán email sẽ không biết tài khoản đó bị khóa hay chưa xác thực |
| ③④⑤ | Lần lượt theo `accountStatus` |
| ⑥ | Chưa được chủ dự án cho vào web |
| ⑦ | Tạo phiên mới trong Redis (đè phiên cũ nếu có → máy cũ bị đá ra, C8) |
| ⑧ | Tạo token chứa email + mã phiên (C9) |
| ⑨ | Trả về token và vài thông tin để frontend hiển thị ngay (khỏi gọi thêm API hồ sơ) |

Không có `@Transactional`: method này **không ghi** gì vào MySQL.

#### C17.4. `forgotPassword` và `resetPassword`

```java
    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email does not exist"));
        if(user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Account is not active");
        }
        otpService.generateAndSend(user, OtpPurpose.RESET_PASSWORD);
    }

    @Transactional
    public void resetPassword(String email, String otpCode, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email does not exist"));
        otpService.verifyAndConsume(user, otpCode, OtpPurpose.RESET_PASSWORD);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        sessionService.deleteSession(user.getEmail());
    }
```
- `forgotPassword`: chỉ tài khoản đang hoạt động mới được gửi mã đặt lại. (Khác `login`, ở đây báo thẳng `Email does not exist`: là điểm đánh đổi giữa tiện cho người dùng và che giấu email, xem mục 22.)
- `resetPassword`: kiểm tra mã → băm mật khẩu mới → lưu → **xóa phiên**: nếu ai đó đang dùng tài khoản (có thể chính là kẻ đã đoán ra mật khẩu cũ), họ bị đá ra ngay.

---

### C18. `service/UserService.java` — người dùng tự quản lý tài khoản

Constructor nhận 5 bean: `UserRepository`, `OtpService`, `PasswordEncoder`, `SessionService`, `FileStorageService`. Hằng số `AVATAR_FOLDER = "avatars"`.

#### C18.1. Các hàm nhỏ dùng chung

```java
    public User getProfile(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));
    }

    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));
    }

    private void checkPassword(User user, String rawPassword) {
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new IllegalArgumentException("Password does not match");
        }
    }

    private void checkActive(User user) {
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Account is not active");
        }
    }
```
- `getProfile`: tìm theo id. Hầu hết method khác mở đầu bằng hàm này.
- `getByEmail`: Controller dùng để đổi email (lấy từ token) ra id (C22).
- `checkPassword`: dùng trước các thao tác nguy hiểm (xin mã đổi mật khẩu, vô hiệu hóa, xóa). `private` vì chỉ dùng trong class.
- `checkActive`: thật ra filter đã chỉ cho tài khoản ACTIVE đi qua (C10, C11), nên qua HTTP hàm này hầu như không bao giờ báo lỗi. Nó là **lớp bảo vệ thứ 2**, phòng khi sau này có nơi khác gọi thẳng vào service.

#### C18.2. Sửa họ tên, đổi mật khẩu

```java
    public User updateProfile(Long userId, String fullName) {
        User user = getProfile(userId);
        user.setFullName(fullName);
        return userRepository.save(user);
    }

    @Transactional
    public void requestChangePasswordOtp(Long userId, String currentPassword) {
        User user = getProfile(userId);
        checkPassword(user, currentPassword);
        otpService.generateAndSend(user, OtpPurpose.CHANGE_PASSWORD);
    }

    @Transactional
    public void changePassword(Long userId, String otpCode, String newPassword) {
        User user = getProfile(userId);
        otpService.verifyAndConsume(user, otpCode, OtpPurpose.CHANGE_PASSWORD);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        sessionService.deleteSession(user.getEmail());
    }
```
- `updateProfile` trả về `User` đã lưu, để Controller trả lại hồ sơ mới cho frontend hiển thị.
- Đổi mật khẩu cần **2 lớp**: biết mật khẩu hiện tại (bước 1) **và** đọc được email (bước 2). Kẻ mượn máy lúc bạn quên đăng xuất không đổi được mật khẩu nếu không vào được email của bạn.
- Đổi xong → xóa phiên → phải đăng nhập lại bằng mật khẩu mới (mục 19.4).

#### C18.3. Đổi ảnh đại diện

```java
    public User updateAvatar(Long userId, MultipartFile file) {
        User user = getProfile(userId);
        String oldAvatarUrl = user.getAvatarUrl();
        String newAvatarUrl = fileStorageService.saveImage(file, AVATAR_FOLDER);   // ①

        try {
            user.setAvatarUrl(newAvatarUrl);
            User saved = userRepository.save(user);                                // ②
            fileStorageService.delete(oldAvatarUrl);                                // ③
            return saved;
        } catch (RuntimeException e) {
            fileStorageService.delete(newAvatarUrl);                                // ④
            throw e;
        }
    }
```
| Bước | Giải thích |
|---|---|
| `MultipartFile file` | Kiểu của Spring đại diện cho 1 file gửi lên bằng form-data |
| ① | Kiểm tra và lưu file mới xuống ổ đĩa trước (C20). File không hợp lệ → lỗi ngay, chưa đụng database |
| ② | Ghi đường dẫn mới vào database |
| ③ | Thành công rồi mới xóa ảnh cũ. Làm ngược (xóa cũ trước) mà bước ② lỗi thì người dùng **mất cả 2 ảnh** |
| ④ | Bước ② lỗi → xóa file mới vừa lưu (tránh rác trên ổ đĩa), rồi ném lại lỗi (`throw e`) cho `GlobalExceptionHandler` xử lý |

Đây là ví dụ của việc "rollback bằng tay": ổ đĩa không có transaction như MySQL (A9.7), nên phải tự dọn.

#### C18.4. Đổi email (2 bước)

```java
    @Transactional
    public void requestUpdateEmailOtp(Long userId, String newEmail) {
        User user = getProfile(userId);
        if (newEmail.equalsIgnoreCase(user.getEmail())) {
            throw new IllegalArgumentException("New email must be different from current email");
        }
        if (userRepository.existsByEmail(newEmail)) {
            throw new IllegalArgumentException("Email already exists");
        }
        otpService.generateAndSend(user, OtpPurpose.CHANGE_EMAIL, newEmail);
    }

    @Transactional
    public void updateEmail(Long userId, String otpCode) {
        User user = getProfile(userId);
        OtpVerification otp = otpService.verifyAndConsume(user, otpCode, OtpPurpose.CHANGE_EMAIL);
        if (userRepository.existsByEmail(otp.getNewEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        sessionService.deleteSession(user.getEmail());
        user.setEmail(otp.getNewEmail());
        userRepository.save(user);
    }
```
| Điểm | Giải thích |
|---|---|
| Bước 1 gửi mã tới **email mới** | Bản `generateAndSend` 3 tham số (C16.2). Chứng minh bạn thật sự sở hữu email mới |
| Bước 2 chỉ cần mã, không cần gửi lại email mới | Email mới đã nằm trong dòng OTP (`otp.getNewEmail()`). Frontend không thể "tráo" sang 1 email khác ở bước 2 |
| Kiểm tra trùng **lần 2** ở bước 2 | Trong 5 phút chờ nhập mã, có thể người khác vừa đăng ký đúng email đó. Báo lỗi → rollback → mã OTP được trả lại trạng thái chưa dùng |
| `deleteSession(user.getEmail())` **trước** `setEmail(...)` | Khóa phiên là `session:{email cũ}`. Đổi email rồi mới xóa thì sẽ xóa nhầm `session:{email mới}` (không tồn tại), phiên cũ vẫn sống |
| Sau khi đổi | Token cũ chứa email cũ → hết dùng được → đăng nhập lại bằng **email mới** |

#### C18.5. Vô hiệu hóa, xóa tài khoản

```java
    public void disableAccount(Long userId, String currentPassword) {
        User user = getProfile(userId);
        checkActive(user);
        checkPassword(user, currentPassword);
        user.setAccountStatus(AccountStatus.DISABLED);
        userRepository.save(user);
        sessionService.deleteSession(user.getEmail());
    }

    public void deleteAccount(Long userId, String currentPassword) {
        // giống hệt disableAccount, chỉ khác: setAccountStatus(AccountStatus.DELETED)
    }
```
Hai method gần như **giống hệt nhau**, khác đúng 1 giá trị trạng thái. Khác biệt **nghiệp vụ** nằm ở chỗ khác: `DISABLED` thì Admin mở lại được (AD3), `DELETED` thì **không API nào** mở lại được (mục 12.7, 12.8).

Không có `@Transactional` vì chỉ có 1 lần ghi MySQL (`save`), không cần gói nhiều thao tác.

#### C18.6. Nghệ sĩ

```java
    public void requestBecomeArtist(Long userId, String artistName, String bio) {
        User user = getProfile(userId);
        checkActive(user);
        if (user.getRole() != Role.USER) {
            throw new IllegalArgumentException("Only users with role USER can request to become an artist");
        }
        if (user.getArtistRequestStatus() != ArtistRequestStatus.NONE
                && user.getArtistRequestStatus() != ArtistRequestStatus.REJECTED) {
            throw new IllegalArgumentException("An artist request already exists or has been approved");
        }
        user.setArtistName(artistName);
        user.setBio(bio);
        user.setArtistRequestStatus(ArtistRequestStatus.PENDING);
        userRepository.save(user);
    }

    public User updateArtistProfile(Long userId, String artistName, String bio) {
        User user = getProfile(userId);
        if (user.getRole() != Role.ARTIST) {
            throw new IllegalArgumentException("Only artists can update artist profile");
        }
        user.setArtistName(artistName);
        user.setBio(bio);
        return userRepository.save(user);
    }
```
- Gửi yêu cầu: chỉ USER (không phải ARTIST, ADMIN), và trạng thái phải là `NONE` (chưa gửi bao giờ) **hoặc** `REJECTED` (bị từ chối, được gửi lại). `&&` nghĩa là "và": báo lỗi khi **khác cả 2**.
- Sửa hồ sơ nghệ sĩ: chỉ ARTIST.

---

### C19. `service/AdminService.java` — Admin quản lý người dùng

Constructor nhận `UserRepository`, `SessionService`.

```java
    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
```
`getAllUsers` trả **tất cả** tài khoản, mọi trạng thái (kể cả DELETED, UNVERIFIED), không phân trang (mục 22).

#### C19.1. Khóa và mở khóa

```java
    public void lockAccount(Long userId) {
        User user = findUser(userId);
        if(user.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Cannot lock an admin account");
        }
        if(user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Only active accounts can be locked");
        }
        user.setAccountStatus(AccountStatus.DISABLED);
        userRepository.save(user);
        sessionService.deleteSession(user.getEmail());
    }

    public void unlockAccount(Long userId) {
        User user = findUser(userId);
        if(user.getAccountStatus() != AccountStatus.DISABLED) {
            throw new IllegalArgumentException("Only disabled accounts can be unlocked");
        }
        user.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(user);
    }
```
- Không khóa được Admin (kể cả Admin tự khóa mình) → tránh tình huống không còn ai quản trị.
- Khóa → xóa phiên → người bị khóa bị đá ra **ngay** request tiếp theo.
- Mở khóa: chỉ từ `DISABLED`. Vì người dùng **tự vô hiệu hóa** cũng ra `DISABLED`, Admin mở khóa được cả trường hợp đó (mục 14.3).

#### C19.2. Duyệt / từ chối nghệ sĩ

```java
    public List<User> getPendingArtistRequests() {
        return userRepository.findByArtistRequestStatus(ArtistRequestStatus.PENDING);
    }

    public void approveArtistRequest(Long userId) {
        User user = findUser(userId);
        if(user.getArtistRequestStatus() != ArtistRequestStatus.PENDING) {
            throw new IllegalArgumentException("No pending artist request for this user");
        }
        if(user.getRole() != Role.USER) {
            throw new IllegalArgumentException("Only users with role USER can become an artist");
        }
        if(user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Account is not active");
        }
        user.setArtistRequestStatus(ArtistRequestStatus.APPROVED);
        user.setRole(Role.ARTIST);
        userRepository.save(user);
    }

    public void rejectArtistRequest(Long userId) {
        User user = findUser(userId);
        if(user.getArtistRequestStatus() != ArtistRequestStatus.PENDING) {
            throw new IllegalArgumentException("No pending artist request for this user");
        }
        user.setArtistRequestStatus(ArtistRequestStatus.REJECTED);
        user.setArtistName(null);
        user.setBio(null);
        userRepository.save(user);
    }
```
- Duyệt: kiểm tra 3 điều kiện (đang chờ, đang là USER, đang hoạt động), rồi đổi **cùng lúc** 2 thứ: trạng thái yêu cầu → `APPROVED`, role → `ARTIST`.
- Từ chối: xóa nghệ danh và tiểu sử đã gửi, để lần gửi lại bắt đầu sạch.
- Duyệt xong **không** xóa phiên. Người dùng không cần đăng nhập lại: ở request tiếp theo, filter đọc lại user từ MySQL (C10) và thấy ngay quyền `ROLE_ARTIST`.

#### C19.3. Đổi role

```java
    public void assignRole(Long userId, Role role) {
        User user = findUser(userId);
        if(role == null) {
            throw new IllegalArgumentException("Role is required");                          // ①
        }
        if(role == Role.ARTIST) {
            throw new IllegalArgumentException(
                    "ARTIST role can only be granted by approving an artist request");       // ②
        }
        if(user.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Cannot change role of an admin account");    // ③
        }
        if(user.getAccountStatus() == AccountStatus.DELETED
                || user.getAccountStatus() == AccountStatus.UNVERIFIED) {
            throw new IllegalArgumentException("Cannot assign role to a deleted or unverified account"); // ④
        }
        if(user.getRole() == role) {
            throw new IllegalArgumentException("User already has this role");               // ⑤
        }
        if(user.getRole() == Role.ARTIST || user.getArtistRequestStatus() == ArtistRequestStatus.PENDING) {
            user.setArtistName(null);                                                        // ⑥
            user.setBio(null);
            user.setArtistRequestStatus(ArtistRequestStatus.NONE);
        }
        user.setRole(role);
        userRepository.save(user);
    }
```
| Bước | Giải thích |
|---|---|
| ① | Phòng hờ. Qua HTTP thì `@NotNull` trong DTO đã chặn trước |
| ② | ARTIST phải đi qua quy trình gửi yêu cầu + duyệt, không gán tắt được |
| ③ | Admin không đổi role của Admin (kể cả chính mình). Muốn hạ Admin → chỉ chủ dự án làm bằng SQL |
| ④ | Không đổi role tài khoản đã xóa hoặc chưa xác thực |
| ⑤ | Đã đúng role đó rồi |
| ⑥ | Đang là ARTIST bị hạ xuống, **hoặc** đang có yêu cầu nghệ sĩ chờ duyệt → xóa sạch thông tin nghệ sĩ, đưa trạng thái về `NONE`. Thực tế chỉ còn 2 hướng đổi: ARTIST/USER → USER/ADMIN (mục 14.4) |

---

### C20. `service/FileStorageService.java` — phần lưu ảnh đại diện

File này dùng chung với module Song (lưu nhạc, ảnh bìa, video). Module User chỉ dùng 2 method: `saveImage` và `delete`.

```java
    private static final long MAX_IMAGE_SIZE = 2L * 1024 * 1024;   // 2 MB tính bằng byte
    private static final String URL_PREFIX = "/uploads/";

    @Value("${app.upload-dir}")
    private String uploadDir;

    public String saveImage(MultipartFile file, String folder) {
        if(file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");                    // ①
        }
        if(file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("Image must not exceed 2MB");                 // ②
        }
        String contentType = file.getContentType();
        String extension;
        if("image/jpeg".equals(contentType)) {
            extension = ".jpg";
        } else if("image/png".equals(contentType)) {
            extension = ".png";
        } else if("image/webp".equals(contentType)) {
            extension = ".webp";
        } else {
            throw new IllegalArgumentException("Only JPG, PNG or WEBP images are allowed");  // ③
        }
        if(!isRealImage(file, extension)) {
            throw new IllegalArgumentException("Invalid or corrupted image file");           // ④
        }
        return saveFile(file, folder, extension);                                            // ⑤
    }
```

| Bước | Giải thích |
|---|---|
| `2L * 1024 * 1024` | 1 KB = 1024 byte, 1 MB = 1024 KB. Chữ `L` = kiểu `long` (số nguyên lớn) |
| ① | Không gửi file, hoặc file rỗng 0 byte |
| ② | Quá 2 MB. (File cực lớn vượt giới hạn chung của server thì bị chặn sớm hơn với **413**, chưa tới được đây) |
| ③ | Đọc **loại file** (content type) do trình duyệt / Postman khai báo. Đuôi file lưu xuống do server tự quyết, **không** lấy theo tên file người dùng gửi |
| ④ | Nhưng content type có thể bị khai man. Nên kiểm tra thêm **"chữ ký" ở đầu file** (magic bytes, xem dưới) |
| ⑤ | Lưu và trả về đường dẫn dạng `/uploads/avatars/<tên ngẫu nhiên>.jpg` |

**Magic bytes** — vài byte đầu tiên của mỗi loại file luôn cố định:

| Loại | Mấy byte đầu (hệ 16) | Code kiểm tra |
|---|---|---|
| JPG | `FF D8 FF` | `head[0] == 0xFF && head[1] == 0xD8 && head[2] == 0xFF` |
| PNG | `89 50 4E 47 0D 0A 1A 0A` (`.PNG....`) | So 8 byte đầu |
| WEBP | `RIFF` ở byte 0–3, `WEBP` ở byte 8–11 | So từng ký tự |

Đổi tên file `virus.exe` thành `anh.jpg` → content type có thể là `image/jpeg`, nhưng byte đầu không phải `FF D8 FF` → bị chặn ở ④.

```java
    private String saveFile(MultipartFile file, String folder, String extension) {
        String fileName = UUID.randomUUID() + extension;
        try {
            Path dir = Paths.get(uploadDir, folder);
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), dir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        } catch(IOException e) {
            throw new RuntimeException("Could not save file", e);
        }
        return URL_PREFIX + folder + "/" + fileName;
    }

    public void delete(String url) {
        if(url == null || !url.startsWith(URL_PREFIX)) {
            return;
        }
        try {
            Files.deleteIfExists(toPath(url));
        } catch(IOException ignored) {}
    }
```
- Tên file = **UUID ngẫu nhiên** + đuôi. Không dùng tên gốc của người dùng: tên gốc có thể trùng nhau, có ký tự lạ, hoặc chứa `../` để ghi file ra ngoài thư mục cho phép.
- `Files.createDirectories(dir)`: thư mục `uploads/avatars` chưa có thì tự tạo.
- `delete`: đường dẫn `null` (người dùng chưa từng có ảnh) hoặc không bắt đầu bằng `/uploads/` → bỏ qua. Xóa lỗi → bỏ qua (không vì 1 file rác mà làm hỏng cả thao tác đổi ảnh).

---

### C21. `controller/AuthController.java` — cửa ngõ không cần đăng nhập

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request.getUsername(), request.getEmail(), request.getPassword());
        return ResponseEntity.ok(Map.of("message", "Registration successful, the account is awaiting OTP verification"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, String>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyOtp(request.getEmail(), request.getOtpCode());
        return ResponseEntity.ok(Map.of("message", "Verification successful, the account has been activated"));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "OTP code has been sent to your email"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getEmail(), request.getOtpCode(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password has been reset successfully"));
    }
}
```

Giải thích các nhãn (gặp lại ở mọi Controller):

| Code | Giải thích |
|---|---|
| `@RestController` | "Class này nhận request HTTP, và **giá trị return của mọi method được viết thành JSON** vào body response". (Khác với `@Controller` thường, vốn trả về tên 1 trang HTML) |
| `@RequestMapping("/api/auth")` | Tiền tố chung: mọi đường dẫn trong class bắt đầu bằng `/api/auth` |
| `@PostMapping("/register")` | Method này xử lý `POST` tới `/api/auth` + `/register` = `/api/auth/register`. Tương tự có `@GetMapping`, `@PutMapping`, `@DeleteMapping` |
| `@RequestBody` | "Lấy **body** của request, dùng Jackson đổi JSON thành object `RegisterRequest`" |
| `@Valid` | Chạy các nhãn kiểm tra trong DTO trước (A11) |
| `ResponseEntity<Map<String, String>>` | Kiểu trả về: 1 response có mã trạng thái + body. Body là `Map` chuỗi → chuỗi (thành `{"message": "..."}`) |
| `ResponseEntity.ok(...)` | Mã **200** + body |
| Chỉ 2 dòng mỗi method | Đúng vai "bồi bàn" (A8.1): nhận phiếu, chuyển cho đầu bếp, đưa món ra. Không có `if` nghiệp vụ nào ở đây |
| Lỗi đi đâu? | Service ném `IllegalArgumentException` → Controller **không bắt** → bay lên `GlobalExceptionHandler` (C24) |

Không có `@PreAuthorize`, và cả `/api/auth/**` đã được mở trong `SecurityConfig` (C13) → **không cần token**.

| API | Gọi | Trả về khi thành công |
|---|---|---|
| `POST /api/auth/register` | `AuthService.register` | 200 `Registration successful, the account is awaiting OTP verification` |
| `POST /api/auth/verify-otp` | `AuthService.verifyOtp` | 200 `Verification successful, the account has been activated` |
| `POST /api/auth/login` | `AuthService.login` | 200 + `LoginResponse` (token...) |
| `POST /api/auth/forgot-password` | `AuthService.forgotPassword` | 200 `OTP code has been sent to your email` |
| `POST /api/auth/reset-password` | `AuthService.resetPassword` | 200 `Password has been reset successfully` |

---

### C22. `controller/UserController.java` — người dùng đã đăng nhập

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final SessionService sessionService;

    // ... constructor nhận 2 bean trên

    private Long me(Authentication authentication) {
        return userService.getByEmail(authentication.getName()).getId();
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(Authentication authentication) {
        sessionService.deleteSession(authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }

    @GetMapping("/me/profile")
    public ResponseEntity<UserProfileResponse> getProfile(Authentication authentication) {
        User user = userService.getProfile(me(authentication));
        return ResponseEntity.ok(UserMapper.toUserProfileResponse(user));
    }

    @PutMapping("/me/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(Authentication authentication,
                                                             @Valid @RequestBody UpdateProfileRequest request) {
        User user = userService.updateProfile(me(authentication), request.getFullName());
        return ResponseEntity.ok(UserMapper.toUserProfileResponse(user));
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserProfileResponse> updateAvatar(Authentication authentication,
                                                            @RequestParam("file") MultipartFile file) {
        User user = userService.updateAvatar(me(authentication), file);
        return ResponseEntity.ok(UserMapper.toUserProfileResponse(user));
    }

    // ... 8 method còn lại cùng 1 khuôn: me(authentication) → gọi UserService → trả message hoặc hồ sơ
}
```

| Code | Giải thích |
|---|---|
| Tham số `Authentication authentication` | Spring **tự điền** tờ giấy "người đang gọi là ai" mà `JwtAuthenticationFilter` đã ghi vào SecurityContext (A13.3). Frontend không gửi gì thêm, chỉ cần token |
| `authentication.getName()` | Trả về **email** (vì C10 đặt "tên đăng nhập" = email) |
| `me(...)` | Hàm nhỏ: email → tìm user → lấy id. Mọi API `/me/...` mở đầu bằng hàm này. `private` + không có nhãn `@GetMapping`... → không phải API, chỉ là hàm nội bộ |
| `logout` | Không cần tìm user, xóa thẳng khóa `session:{email}` trong Redis là xong. Token cũ hết dùng được |
| `UserMapper.toUserProfileResponse(user)` | Đổi Entity → DTO trước khi trả ra (C7.3, A8.2) |
| `consumes = MediaType.MULTIPART_FORM_DATA_VALUE` | API này **chỉ nhận** form-data (A3.2). Gửi JSON vào đây → lỗi 415 |
| `@RequestParam("file") MultipartFile file` | Lấy ngăn tên `file` trong form-data. Không có ngăn đó → 400 `File is required` (C24) |

Bảng đủ 12 API:

| API | Gọi | Trả về khi thành công |
|---|---|---|
| `POST /logout` | `SessionService.deleteSession` | `Logout successful` |
| `GET /me/profile` | `UserService.getProfile` | Hồ sơ (`UserProfileResponse`) |
| `PUT /me/profile` | `UserService.updateProfile` | Hồ sơ mới |
| `POST /me/change-password/request-otp` | `UserService.requestChangePasswordOtp` | `OTP code has been sent to your email` |
| `PUT /me/change-password` | `UserService.changePassword` | `Password has been changed successfully` |
| `POST /me/avatar` | `UserService.updateAvatar` | Hồ sơ mới (có `avatarUrl` mới) |
| `POST /me/email/request-otp` | `UserService.requestUpdateEmailOtp` | `OTP code has been sent to your new email` |
| `PUT /me/email` | `UserService.updateEmail` | `Email has been updated, please login again` |
| `PUT /me/disable` | `UserService.disableAccount` | `Account has been disabled` |
| `PUT /me/delete` | `UserService.deleteAccount` | `Account has been deleted` |
| `POST /me/artist-request` | `UserService.requestBecomeArtist` | `Artist request has been submitted` |
| `PUT /me/artist-profile` | `UserService.updateArtistProfile` | Hồ sơ mới |

(Mọi đường dẫn trên đều có tiền tố `/api/users`.)

Không có `@PreAuthorize` → mọi role đều gọi được. Nhưng `SecurityConfig` bắt `/api/users/**` phải đăng nhập (luật `anyRequest().authenticated()`).

---

### C23. `controller/AdminController.java` — chỉ ADMIN

```java
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    // ... constructor

    @GetMapping("/users")
    public ResponseEntity<List<UserProfileResponse>> getAllUsers() {
        List<UserProfileResponse> result = adminService.getAllUsers()
                .stream()
                .map(UserMapper::toUserProfileResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PutMapping("/users/{userId}/lock")
    public ResponseEntity<Map<String, String>> lockAccount(@PathVariable Long userId) {
        adminService.lockAccount(userId);
        return ResponseEntity.ok(Map.of("message", "Account has been locked"));
    }

    @PutMapping("/users/{userId}/role")
    public ResponseEntity<Map<String, String>> assignRole(@PathVariable Long userId,
                                                          @Valid @RequestBody AssignRoleRequest request) {
        adminService.assignRole(userId, request.getRole());
        return ResponseEntity.ok(Map.of("message", "Role has been assigned successfully"));
    }

    // ... unlock, artist-requests, approve, reject cùng 1 khuôn
}
```

| Code | Giải thích |
|---|---|
| `@PreAuthorize("hasRole('ADMIN')")` trên **class** | Áp dụng cho **mọi method** trong class. Người gọi không có `ROLE_ADMIN` → **403**, method không chạy (A13.4) |
| `@PathVariable Long userId` | Lấy giá trị từ chỗ trống `{userId}` trên đường dẫn: `/api/admin/users/42/lock` → `userId = 42`. Tên tham số phải trùng tên trong ngoặc nhọn. Gửi chữ thay vì số (`/users/abc/lock`) → 400 `Invalid parameter: userId` |
| `.stream().map(UserMapper::toUserProfileResponse).collect(...)` | Đổi **cả danh sách** Entity thành danh sách DTO (A5.11) |
| Controller không kiểm tra "người bị khóa có phải Admin không" | Đó là luật nghiệp vụ → nằm trong `AdminService` (C19) |

| API (tiền tố `/api/admin`) | Gọi | Trả về khi thành công |
|---|---|---|
| `GET /users` | `getAllUsers` | Danh sách hồ sơ |
| `PUT /users/{userId}/lock` | `lockAccount` | `Account has been locked` |
| `PUT /users/{userId}/unlock` | `unlockAccount` | `Account has been unlocked` |
| `GET /artist-requests` | `getPendingArtistRequests` | Danh sách hồ sơ đang chờ duyệt |
| `PUT /artist-requests/{userId}/approve` | `approveArtistRequest` | `Artist request has been approved` |
| `PUT /artist-requests/{userId}/reject` | `rejectArtistRequest` | `Artist request has been rejected` |
| `PUT /users/{userId}/role` | `assignRole` | `Role has been assigned successfully` |

---

### C24. `exception/GlobalExceptionHandler.java` — biến mọi lỗi thành JSON

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private Map<String, String> body(String message, String fallback) {
        return Map.of("message", message != null ? message : fallback);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(body(e.getMessage(), "Invalid request data"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        ObjectError error = e.getBindingResult().getAllErrors().get(0);
        if (error instanceof FieldError fieldError && fieldError.isBindingFailure()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid value for " + fieldError.getField()));
        }
        return ResponseEntity.badRequest().body(body(error.getDefaultMessage(), "Invalid request data"));
    }

    // ... các handler khác, xem bảng dưới

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleOther(Exception e) {
        if (e instanceof ErrorResponse errorResponse) {
            HttpStatusCode status = errorResponse.getStatusCode();
            String detail = errorResponse.getBody().getDetail();
            return ResponseEntity.status(status).body(body(detail, "Request error"));
        }
        log.error("Lỗi không mong muốn", e);
        return ResponseEntity.internalServerError()
                .body(Map.of("message", "Internal server error"));
    }

    @ExceptionHandler(EmailSendException.class)
    public ResponseEntity<Map<String, String>> handleEmailSend(EmailSendException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", e.getMessage()));
    }
}
```

| Code | Giải thích |
|---|---|
| `@RestControllerAdvice` | "Class này đứng sau **mọi** Controller, bắt lỗi chúng ném ra, và kết quả viết thành JSON" |
| `body(message, fallback)` | Hàm nhỏ: có message thì dùng, `null` thì dùng câu dự phòng. (`Map.of` không cho giá trị `null`, gặp `null` là lỗi, nên phải có hàm này) |
| `a != null ? a : b` | Toán tử 3 ngôi: "nếu `a` khác `null` thì lấy `a`, không thì lấy `b`" |
| `getAllErrors().get(0)` | DTO sai nhiều chỗ cùng lúc thì **chỉ báo 1 lỗi** (lỗi đầu tiên trong danh sách, không cố định là trường nào). Frontend nên tự kiểm tra trước để báo đủ |
| `e instanceof ErrorResponse errorResponse` | "Nếu lỗi này thuộc loại `ErrorResponse` thì đặt tên nó là `errorResponse` để dùng". Các lỗi có sẵn của Spring như sai đường dẫn (404), sai method (405) đều thuộc loại này → giữ đúng mã của chúng |
| `log.error(...)` + 500 | Lỗi thật sự không lường trước → ghi **toàn bộ chi tiết** vào log, nhưng chỉ trả câu chung chung cho người dùng (không lộ chi tiết bên trong server) |

Bảng đầy đủ các handler và khi nào module User gặp:

| Loại lỗi | Mã | message | Module User gặp khi |
|---|---|---|---|
| `IllegalArgumentException` | 400 | Message do Service đặt | Mọi lỗi nghiệp vụ (`Email already exists`...) |
| `MethodArgumentNotValidException` | 400 | Message trong nhãn kiểm tra của DTO | `@Valid` không qua |
| `HttpMessageNotReadableException` | 400 | `Invalid request data` | JSON sai cú pháp; thiếu body; `"role": "BOSS"` |
| `MissingServletRequestPartException` | 400 | `File is required` | Đổi ảnh mà không gửi ngăn `file` |
| `MethodArgumentTypeMismatchException` | 400 | `Invalid parameter: userId` | `/api/admin/users/abc/lock` |
| `AuthenticationException` | 401 | `Unauthorized, please login` | Hiếm, lỗi xác thực ném ra bên trong Controller |
| `AccessDeniedException` | 403 | `You do not have permission to access this resource` | USER gọi API admin: `@PreAuthorize` ném lỗi này bên trong lúc gọi method, nên `GlobalExceptionHandler` bắt (không phải `accessDeniedHandler` của `SecurityConfig`). Hai nơi cố ý trả cùng 1 câu |
| `DataIntegrityViolationException` | 409 | `Data already exists` | 2 người đăng ký trùng username / email cùng lúc (mục 17.3) |
| `MaxUploadSizeExceededException` | 413 | `File is too large` | File vượt giới hạn chung của server |
| `MailException` | 503 | `Could not send email, please try again later` | **Không gặp** trong module User (vì `EmailService` đã gói lỗi thành `EmailSendException`). Để dành cho nơi khác gửi email trực tiếp |
| `OptimisticLockingFailureException` | 409 | `This data was changed by someone else, please reload and try again` | Không gặp, dùng cho module Song |
| `EmailSendException` | 503 | `Could not send OTP email, please try again later` | Gửi OTP thất bại |
| `ErrorResponse` (lỗi sẵn của Spring) | Theo lỗi | Mô tả của Spring | `GET /api/auth/login` → 405; đường dẫn không tồn tại (khi có token) → 404 |
| `Exception` (còn lại) | 500 | `Internal server error` | Lỗi code, Redis tắt khi đang nhập OTP... |

Spring chọn handler theo **loại lỗi cụ thể nhất**, không theo thứ tự viết trong file. Ví dụ `EmailSendException` vừa là `RuntimeException` vừa là `Exception`, nhưng vẫn vào `handleEmailSend` vì đó là handler đúng loại nhất.

Lỗi 401 / 403 **ở tầng filter** (chưa tới Controller) không đi qua class này mà do `SecurityConfig` tự viết (C13 ⑤), cùng dạng JSON.

---

### C25. Theo dấu 1 request đăng ký từ đầu tới cuối

Ghép tất cả lại: chuyện gì xảy ra khi bấm **Send** trong Postman.

```http
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{ "username": "minh_99", "email": "minh@gmail.com", "password": "matkhau123" }
```

```
① TOMCAT (cổng 8080) nhận request, đưa vào dãy filter của Spring Security

② CORS filter
   Postman không gửi header Origin → không phải request từ trình duyệt khác domain → cho qua

③ JwtAuthenticationFilter (C11)
   Không có header Authorization → bỏ qua, request là "khách"

④ RateLimitFilter (C12)
   Đường dẫn bắt đầu /api/auth/ → khóa rate:auth:127.0.0.1, giới hạn 10
   Redis:  INCR rate:auth:127.0.0.1   → 1
           EXPIRE rate:auth:127.0.0.1 60
   1 ≤ 10 → cho qua

⑤ AuthorizationFilter: luật URL (C13 ④)
   /api/auth/** là permitAll → khách vẫn được đi tiếp

⑥ DispatcherServlet (bộ điều phối của Spring)
   Tìm method khớp POST + /api/auth/register → AuthController.register

⑦ Jackson: JSON → new RegisterRequest(), gọi 3 setter (C6.1)
   @Valid: username 7 ký tự, đúng ký tự cho phép ✔ · email đúng dạng ✔ · mật khẩu 10 ký tự ✔
   (sai bất kỳ điều gì → dừng ở đây, 400)

⑧ AuthController.register → authService.register("minh_99", "minh@gmail.com", "matkhau123")
   Vì có @Transactional: Spring mở transaction        → MySQL: BEGIN

⑨ AuthService.register (C17.1)
   MySQL: SELECT * FROM users WHERE email = 'minh@gmail.com'           → không có → nhánh B
   MySQL: SELECT 1 FROM users WHERE username = 'minh_99' LIMIT 1       → không có
   BCrypt: băm "matkhau123" → "$2a$10$..." (mất vài chục mili giây)
   MySQL: INSERT INTO users (username, email, password, account_status, role,
                             artist_request_status, access_status, ...)
          VALUES ('minh_99', 'minh@gmail.com', '$2a$10$...', 'UNVERIFIED', 'USER',
                  'NONE', 'REFUSE', ...)                              → MySQL cấp id = 8

⑩ OtpService.generateAndSend(user 8, REGISTER) → nhập chung transaction (C16)
   MySQL: SELECT * FROM otp_verifications
          WHERE user_id = 8 AND purpose = 'REGISTER' AND verified = false
          ORDER BY id DESC LIMIT 1                                    → không có → không phải chờ
   SecureRandom → "004213"
   MySQL: INSERT INTO otp_verifications (user_id, otp_code, purpose, new_email, expired_at, verified)
          VALUES (8, '004213', 'REGISTER', NULL, '<bây giờ + 5 phút>', false)

⑪ EmailService.sendOtp("minh@gmail.com", "004213") (C15)
   Kết nối smtp.gmail.com:587 → gửi email (thường 1–3 giây)

⑫ register chạy xong, không lỗi                      → MySQL: COMMIT (2 dòng mới được ghi thật)

⑬ Controller: ResponseEntity.ok(Map.of("message", "Registration successful, ..."))
   Jackson: Map → JSON

⑭ Response:  200  {"message": "Registration successful, the account is awaiting OTP verification"}
```

Sau request này:

| Nơi | Có gì |
|---|---|
| MySQL `users` | Dòng id 8: `minh_99`, `UNVERIFIED`, `USER`, `NONE`, `REFUSE` |
| MySQL `otp_verifications` | 1 dòng: mã `004213`, `REGISTER`, `verified = false`, hết hạn sau 5 phút |
| Redis | `rate:auth:127.0.0.1 = 1` (sống 60 giây) |
| Hộp thư minh@gmail.com | Email chứa mã `004213` |

Nếu ở bước ⑪ Gmail lỗi:
```
EmailService ném EmailSendException
   → bay qua OtpService, AuthService (không ai bắt)
   → Spring thấy RuntimeException rời khỏi method @Transactional → MySQL: ROLLBACK
        (dòng user 8 và dòng OTP đều biến mất, như chưa từng đăng ký)
   → GlobalExceptionHandler.handleEmailSend → 503 {"message": "Could not send OTP email, please try again later"}
   (Redis vẫn giữ rate:auth = 1: Redis không bị rollback)
```

So sánh với 1 request **đã đăng nhập**, ví dụ `GET /api/users/me/profile` kèm token:
```
JwtAuthenticationFilter:  kiểm tra chữ ký token (không cần database)
                          Redis: GET session:minh@gmail.com          → so với jti trong token
                          MySQL: SELECT * FROM users WHERE email = ? (CustomUserDetailsService)
RateLimitFilter:          Redis: INCR rate:user:minh@gmail.com
UserController.me():      MySQL: SELECT * FROM users WHERE email = ?
UserService.getProfile(): findById(8) → thường Hibernate lấy luôn bản đã đọc ở dòng trên, không chạy SELECT lần nữa
UserMapper → JSON → 200
```
Mỗi request đã đăng nhập tốn khoảng 2 lần đọc Redis và 2 câu SELECT. Đây là cái giá của việc "khóa là có tác dụng ngay" (mục 22, dòng 13).

---

### C26. Ví dụ luyện tập: tự thêm 1 API mới

> ⚠️ **Đây là bài tập**, code dưới đây **không có** trong dự án. Mục đích: thấy rõ 1 tính năng mới chạy qua những tầng nào, theo thứ tự nào.

**Yêu cầu giả định:** Admin muốn xem danh sách tài khoản **theo trạng thái**, ví dụ chỉ xem các tài khoản `DISABLED`.
API: `GET /api/admin/users/status/{status}`, ví dụ `GET /api/admin/users/status/DISABLED`.

**Bước 1 — Repository:** cần câu SQL `SELECT * FROM users WHERE account_status = ?`. Đặt tên method theo luật A9.5:
```java
// UserRepository.java — thêm 1 dòng
List<User> findByAccountStatus(AccountStatus status);
```

**Bước 2 — Service:** luật nghiệp vụ (ở đây đơn giản, chỉ gọi repository):
```java
// AdminService.java — thêm method
public List<User> getUsersByStatus(AccountStatus status) {
    return userRepository.findByAccountStatus(status);
}
```

**Bước 3 — Controller:** cửa ngõ HTTP. `AdminController` đã có `@PreAuthorize("hasRole('ADMIN')")` trên class → tự động chỉ Admin gọi được, không cần viết thêm gì về quyền:
```java
// AdminController.java — thêm method
@GetMapping("/users/status/{status}")
public ResponseEntity<List<UserProfileResponse>> getUsersByStatus(@PathVariable AccountStatus status) {
    List<UserProfileResponse> result = adminService.getUsersByStatus(status)
            .stream()
            .map(UserMapper::toUserProfileResponse)
            .collect(Collectors.toList());
    return ResponseEntity.ok(result);
}
```
Spring tự đổi chữ `"DISABLED"` trên đường dẫn thành `AccountStatus.DISABLED`. Gõ sai (`/status/abc`) → 400 `Invalid parameter: status` (C24).

**Bước 4 — Có cần sửa database không?** Không: chỉ đọc cột đã có. (Nếu thêm field vào Entity thì phải `ALTER TABLE` trên VPS, A9.3.)

**Bước 5 — Test bằng Postman:**

| Thử | Kết quả mong đợi |
|---|---|
| Token Admin, `/status/DISABLED` | 200 + danh sách |
| Token Admin, `/status/abc` | 400 `Invalid parameter: status` |
| Token USER | 403 |
| Không token | 401 |

**Bước 6 — Đưa lên GitHub** (mỗi tính năng 1 nhánh, mục 21.5):
```bash
git checkout -b admin-users-by-status
git add .
git commit -m "Add admin API to list users by status"
git push -u origin admin-users-by-status
```
Rồi lên GitHub tạo Pull Request vào `main`, chờ CI xanh, nhờ người khác xem rồi mới merge.

Thứ tự **Repository → Service → Controller → Test** là thứ tự nên làm cho mọi tính năng: viết từ dưới lên, mỗi tầng chỉ dùng tầng ngay dưới nó.

---

### C27. Bài tập tự kiểm tra (có đáp án)

Tự trả lời trước, rồi mới xem đáp án ở dưới.

1. Trong `AuthService` không có chỗ nào viết `new UserRepository()` hay `new OtpService()`. Vậy 2 object đó từ đâu ra?
2. Gửi `POST /api/auth/login` với body `{"Email": "lan@gmail.com", "password": "123456"}` thì nhận được gì?
3. Lan đăng nhập trên laptop, rồi đăng nhập tiếp trên điện thoại. Laptop gọi 1 API bất kỳ thì sao?
4. Admin khóa tài khoản Lan khi Lan đang mở web. Lan bị chặn ở lúc nào, nhận message gì?
5. Chủ dự án chạy `UPDATE users SET access_status = 'REFUSE' WHERE email = 'lan@gmail.com'` khi Lan đang đăng nhập. Request tiếp theo của Lan nhận gì? Vì sao message khác câu 4?
6. Vì sao cột `otp_code` kiểu chuỗi chứ không phải số?
7. Nhập sai mã OTP 5 lần, lần thứ 6 nhập **đúng**. Kết quả? Phải làm gì?
8. Đăng ký đúng hết, nhưng Gmail đang lỗi. Sau request đó, bảng `users` có thêm dòng nào không?
9. Trong `updateEmail`, vì sao `deleteSession(user.getEmail())` phải đứng **trước** `user.setEmail(...)`?
10. USER thường gọi `GET /api/admin/users` nhận mã gì? Không gửi token thì mã gì?
11. Vì sao Controller không trả thẳng Entity `User` ra JSON?
12. Muốn thêm giá trị `LOCKED` vào enum `AccountStatus` rồi deploy lên VPS, ngoài sửa code Java còn phải làm gì?
13. Lỡ gõ `findByEmial` trong `UserRepository`. Chuyện gì xảy ra?
14. Từ 1 máy, gửi 11 request đăng nhập trong 30 giây. Request thứ 11 nhận gì?

**Đáp án**

1. Spring tạo sẵn (bean) và truyền vào qua constructor: Dependency Injection (A7).
2. **400 `Email is required`**: tên trường JSON phân biệt hoa thường, `"Email"` không khớp `email` → field `email` còn `null` → `@NotBlank` báo lỗi (A3.3).
3. **401 `Your session has ended. Your account may have been logged in on another device`**: đăng nhập trên điện thoại ghi đè `session:lan@gmail.com` bằng mã phiên mới, mã trong token của laptop hết khớp (C8, C11 bước ⑥).
4. Ngay **request tiếp theo**, vì `lockAccount` xóa phiên trong Redis. Message: `Your session has ended...` (C19.1).
5. **401 `Unauthorized, please login`**. Phiên trong Redis **vẫn còn** (SQL không chạy code Java nên không ai xóa phiên), nên qua được bước kiểm tra phiên. Nhưng `CustomUserDetailsService` thấy `accessStatus ≠ ACCEPT` → đánh dấu bị vô hiệu → filter không ghi nhận đăng nhập → khách → câu 401 chung (C10, C11 bước ⑧).
6. Để giữ số 0 ở đầu: `"004213"`. Lưu số thì thành `4213` (C4).
7. **400 `Too many wrong attempts, please request a new OTP`**, vì bộ đếm tăng **trước** khi so mã. Phải xin mã mới (C16.4).
8. **Không.** `EmailSendException` là `RuntimeException` → `@Transactional` rollback cả dòng user lẫn dòng OTP. Người dùng nhận 503 (C25).
9. Khóa phiên là `session:{email}`. Đổi email trước thì sẽ xóa nhầm khóa của email mới, phiên cũ vẫn sống (C18.4).
10. **403** (biết bạn là ai nhưng không đủ quyền). Không token: **401** (A2.5, A13.1).
11. Lộ bản băm mật khẩu, lộ cột mới thêm, và không kiểm soát được hình dạng API (A8.2).
12. Chạy `ALTER TABLE users MODIFY COLUMN account_status ENUM(...)` liệt kê **đủ** các giá trị trên VPS **trước khi** merge, vì VPS dùng `ddl-auto=validate`. Và rà lại mọi chỗ code dùng `AccountStatus` (`login`, `CustomUserDetailsService`, `AdminService`...) để xử lý giá trị mới (A9.3, A9.4, mục 21.4).
13. Ứng dụng **không khởi động được**: Spring không tìm thấy field `emial` trong `User` để sinh câu SQL (A9.5).
14. **429** `Too many requests. Please try again in <n> seconds`, kèm header `Retry-After` (C12).

---

## PHẦN D — FRONTEND, TEST, CẤU HÌNH VÀ PHỤ LỤC

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

### 21.7. Chạy dự án lần đầu trên máy của bạn (MỚI)

Dành cho thành viên mới muốn chạy backend trên máy cá nhân (Windows) để học và thử.

#### 21.7.1. Cần cài gì?

| Phần mềm | Dùng để | Ghi chú |
|---|---|---|
| **Git** | Tải code về | Có sẵn Git Bash (dùng cho vài lệnh bên dưới) |
| **IntelliJ IDEA** (bản Community miễn phí là đủ) | Mở, chạy code | IntelliJ tự đề nghị tải JDK nếu máy chưa có |
| **JDK 17 trở lên** | Chạy Java | `pom.xml` đặt Java 17; VPS chạy JDK 25 |
| **MySQL 8** + MySQL Workbench | Database chính | Nhớ mật khẩu user `root` đặt lúc cài |
| **Redis** | Phiên đăng nhập, bộ đếm | Redis không có bản chính thức cho Windows. Cách dễ: dùng Docker Desktop (`docker run -d --name redis -p 6379:6379 redis`), hoặc cài trong WSL, hoặc dùng Memurai (bản tương thích Redis cho Windows) |
| **MongoDB** | Lịch sử nghe (module Song) | Xin chuỗi kết nối từ trưởng nhóm, hoặc tự tạo MongoDB Atlas miễn phí. Xem `SONG_MODULE.md` mục 24.2 |
| **Postman** | Gọi thử API | |

#### 21.7.2. Các bước

**Bước 1 — Tải code và mở bằng IntelliJ**
```bash
git clone https://github.com/Winhmm/myspotify_backend.git
```
IntelliJ → **Open** → chọn thư mục `myspotify_backend` → chờ Maven tải thư viện (lần đầu vài phút, thanh tiến trình ở góc dưới).

**Bước 2 — Tạo database rỗng** (chạy trong MySQL Workbench):
```sql
CREATE DATABASE myspotify_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```
`utf8mb4` để lưu được tiếng Việt có dấu và emoji. **Không cần tạo bảng**: lần chạy đầu Hibernate tự tạo (`ddl-auto=update`, A9.3).

**Bước 3 — Tạo file cấu hình** `src/main/resources/application.properties`.
File này **không có** trên GitHub (nằm trong `.gitignore`) nên phải tự tạo. Cách tốt nhất: **xin file mẫu từ trưởng nhóm** rồi sửa các giá trị của máy mình. Khung tối thiểu (thay mọi chỗ `<...>`):

```properties
# Database MySQL trên máy bạn
spring.datasource.url=jdbc:mysql://localhost:3306/myspotify_db
spring.datasource.username=root
spring.datasource.password=<mật khẩu MySQL của bạn>
spring.jpa.hibernate.ddl-auto=update

# JWT: chuỗi ngẫu nhiên dài ít nhất 64 ký tự, tự tạo riêng cho máy bạn
app.jwt.secret=<chuỗi bí mật>
app.jwt.expiration-ms=86400000

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Gmail gửi OTP (xem 21.2 để lấy App Password)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=<gmail dùng để gửi>
spring.mail.password=<App Password 16 ký tự, viết liền>
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000

# Rate limit
app.rate-limit.window-seconds=60
app.rate-limit.user-requests=100
app.rate-limit.auth-requests=10

# File upload
app.upload-dir=uploads
spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=110MB

# MongoDB (module Song), xem SONG_MODULE.md mục 24.2
spring.mongodb.uri=<chuỗi kết nối MongoDB, có tên database>
spring.data.mongodb.auto-index-creation=true
```
Nhắc lại: ghi chú `#` phải nằm trên **dòng riêng**, không viết ở cuối dòng giá trị (21.1).

Tạo nhanh chuỗi bí mật JWT (mở **Git Bash**):
```bash
openssl rand -base64 64
```
Dán kết quả (viết liền trên 1 dòng) vào `app.jwt.secret`. Mỗi máy 1 chuỗi riêng, **không** dùng chung chuỗi của VPS.

**Bước 4 — Chạy.** Mở `MyspotifyApplication.java` → bấm nút ▶ xanh cạnh `main`. Chờ tới dòng log:
```
Started MyspotifyApplication in ... seconds
```

**Bước 5 — Kiểm tra server sống.** Mở trình duyệt:
- `http://localhost:8080/actuator/health` → thấy `{"status":"UP"}`.
- `http://localhost:8080/swagger-ui.html` → trang liệt kê mọi API.

Thấy `{"status":"DOWN"}` → server chạy nhưng 1 trong các kết nối (MySQL, Redis, MongoDB) đang hỏng, xem log để biết cái nào.

**Bước 6 — Tạo tài khoản đầu tiên** (Postman, base URL `http://localhost:8080`):
1. `POST /api/auth/register` với email thật của bạn.
2. Mở email lấy mã → `POST /api/auth/verify-otp`.
3. Cho phép vào web và (tùy chọn) nâng thành Admin, trong MySQL Workbench:
   ```sql
   UPDATE users SET access_status = 'ACCEPT', role = 'ADMIN' WHERE email = '<email của bạn>';
   ```
4. `POST /api/auth/login` → nhận token → dùng cho các API khác (mục 20).

Muốn thử module Song trên database mới: thêm 25 thể loại nhạc theo `SONG_MODULE.md` mục 24.2a.

#### 21.7.3. Lỗi khởi động hay gặp

| Log báo | Nguyên nhân | Cách sửa |
|---|---|---|
| `Could not resolve placeholder 'app.jwt.secret'` (hoặc khóa khác) | Thiếu khóa trong file cấu hình, hoặc file đặt sai chỗ / sai tên | Kiểm tra đúng đường dẫn `src/main/resources/application.properties` và đủ khóa |
| `Access denied for user 'root'@'localhost'` | Sai mật khẩu MySQL | Sửa `spring.datasource.password` |
| `Unknown database 'myspotify_db'` | Chưa tạo database | Làm lại bước 2 |
| `Communications link failure` | MySQL chưa chạy | Bật dịch vụ MySQL (Windows: Services → MySQL80 → Start) |
| `WeakKeyException` | Chuỗi bí mật JWT quá ngắn (< 32 byte) | Tạo lại bằng `openssl rand -base64 64` |
| `Port 8080 was already in use` | Đang có 1 bản khác chạy (thường là bấm Run 2 lần) | Tắt bản cũ (nút ■ đỏ), hoặc thêm `server.port=8081` |
| Gọi API báo 500, log có `RedisConnectionFailureException` | Redis chưa chạy | Bật Redis |
| Đăng ký báo 503, log có `Could not send OTP email to ...` kèm `535 ... Username and Password not accepted` | Sai Gmail hoặc App Password | Làm lại 21.2, kiểm tra không có dấu cách |
| Đăng nhập báo `Account has not been approved yet` | Chưa chạy câu `UPDATE ... access_status = 'ACCEPT'` | Làm bước 6.3 |

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
| 19 | (MỚI) DTO giới hạn mật khẩu 72 **ký tự**, còn BCrypt chỉ xử lý 72 **byte** (A14.4) | Mật khẩu tiếng Việt có dấu, dài gần 72 ký tự có thể vượt 72 byte và bị thư viện từ chối | Kiểm tra theo số byte, hoặc khuyên người dùng đặt mật khẩu không dấu |

---

## 23. CÂU HỎI THƯỜNG GẶP

**H: Mình chưa học Spring Boot, nên bắt đầu đọc từ đâu?**
Đ: Đọc Phần A (A1 → A16) một lượt, rồi theo lộ trình đầu tiên ở mục 1. Song song, tự chạy dự án trên máy (21.7) và gọi thử vài API bằng Postman (mục 20): vừa gọi vừa mở MySQL và Redis xem dữ liệu đổi thế nào là cách hiểu nhanh nhất.

**H: Sửa code xong có phải tắt ứng dụng rồi chạy lại không?**
Đ: Dự án có thư viện `spring-boot-devtools`: sửa code rồi bấm **Build → Build Project** (`Ctrl + F9`) là ứng dụng tự khởi động lại nhanh. Sửa file cấu hình `.properties`, thêm thư viện vào `pom.xml`, hoặc không chắc → cứ bấm ■ tắt rồi ▶ chạy lại.

**H: Đọc code thấy `@Service`, `@RestController`... mà không thấy ai gọi `new`, vậy object ở đâu ra?**
Đ: Spring tạo sẵn lúc khởi động và tự truyền vào constructor (Dependency Injection), xem A7.

**H: `UserRepository` chỉ là interface, không có code bên trong, sao chạy được?**
Đ: Spring đọc tên method (`findByEmail`...) và tự sinh câu SQL lúc chạy, xem A9.5.


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

### 24.5. Bản đồ file → mục giải thích

| File | Nghiệp vụ (Phần B) | Giải thích code (Phần C) |
|---|---|---|
| `MyspotifyApplication.java` | | C1, A6.3 |
| `enums/AccountStatus`, `Role`, `ArtistRequestStatus`, `AccessStatus`, `OtpPurpose` | 7 | C2 |
| `entity/User.java` | 6.1 | C3 |
| `entity/OtpVerification.java` | 6.2, 8 | C4 |
| `repository/UserRepository.java`, `OtpVerificationRepository.java` | | C5, A9.5 |
| `dto/auth/*`, `dto/user/*` (request) | 24.2 | C6 |
| `LoginResponse`, `UserProfileResponse`, `mapper/UserMapper.java` | 24.2 | C7 |
| `service/SessionService.java` | 9.2, 9.3 | C8 |
| `security/JwtUtil.java` | 9.1 | C9, A15 |
| `security/CustomUserDetailsService.java` | 10 | C10 |
| `security/JwtAuthenticationFilter.java` | 10 | C11 |
| `service/RateLimitService.java`, `security/RateLimitFilter.java` | 15 | C12 |
| `config/SecurityConfig.java` | 10, 16 | C13, A13 |
| `config/CorsConfig.java`, `config/WebMvcConfig.java` | 16, 19.6 | C14 |
| `service/EmailService.java`, `exception/EmailSendException.java` | 8.4 | C15 |
| `service/OtpService.java` | 8 | C16 |
| `service/AuthService.java` | 9.4, 11 | C17 |
| `service/UserService.java` | 12, 13 | C18 |
| `service/AdminService.java` | 13, 14 | C19 |
| `service/FileStorageService.java` (ảnh đại diện) | 12.4 | C20 |
| `controller/AuthController.java` | 4.1, 11 | C21 |
| `controller/UserController.java` | 4.2, 12, 13 | C22 |
| `controller/AdminController.java` | 4.3, 13, 14 | C23 |
| `exception/GlobalExceptionHandler.java` | 17 | C24, A12 |
