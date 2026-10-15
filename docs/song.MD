# MODULE SONG — TÀI LIỆU NGHIỆP VỤ VÀ LUỒNG XỬ LÝ

> Dự án: **MySpotify Backend** (Spring Boot 4, Java 25, MySQL, Redis, MongoDB)
> Package gốc: `com.winhmm.myspotify`
> Đối tượng đọc: thành viên mới trong nhóm (backend, frontend, tester), **không cần biết trước Spring Boot**.
> Tài liệu đi kèm: `docs/TEST_SONG_POSTMAN.md` (hướng dẫn test từng API bằng Postman).

---

## MỤC LỤC

1. [Cách đọc tài liệu này](#1-cách-đọc-tài-liệu-này)
2. [Module Song giải quyết bài toán gì?](#2-module-song-giải-quyết-bài-toán-gì)
3. [Từ điển thuật ngữ](#3-từ-điển-thuật-ngữ)
4. [Danh sách use case](#4-danh-sách-use-case)
5. [Kiến trúc và vị trí các file](#5-kiến-trúc-và-vị-trí-các-file)
6. [Hành trình của 1 request](#6-hành-trình-của-1-request)
7. [Dữ liệu: MySQL, MongoDB và ổ đĩa](#7-dữ-liệu-mysql-mongodb-và-ổ-đĩa)
8. [Trạng thái và vòng đời bài hát](#8-trạng-thái-và-vòng-đời-bài-hát)
9. [Luồng nghiệp vụ của ARTIST](#9-luồng-nghiệp-vụ-của-artist)
10. [Luồng nghiệp vụ của ADMIN](#10-luồng-nghiệp-vụ-của-admin)
11. [Luồng nghiệp vụ của NGƯỜI NGHE](#11-luồng-nghiệp-vụ-của-người-nghe)
12. [Lịch sử nghe gần đây (MongoDB)](#12-lịch-sử-nghe-gần-đây-mongodb)
13. [Lưu trữ file nhạc và ảnh](#13-lưu-trữ-file-nhạc-và-ảnh)
14. [Bảo mật và phân quyền](#14-bảo-mật-và-phân-quyền)
15. [Xử lý đồng thời (nhiều người thao tác cùng lúc)](#15-xử-lý-đồng-thời-nhiều-người-thao-tác-cùng-lúc)
16. [Thời gian và múi giờ](#16-thời-gian-và-múi-giờ)
17. [Phân trang](#17-phân-trang)
18. [Xử lý lỗi](#18-xử-lý-lỗi)
19. [Hướng dẫn riêng cho Frontend](#19-hướng-dẫn-riêng-cho-frontend)
20. [Cấu hình và môi trường](#20-cấu-hình-và-môi-trường)
21. [Giới hạn đã biết và hướng phát triển](#21-giới-hạn-đã-biết-và-hướng-phát-triển)
22. [Câu hỏi thường gặp](#22-câu-hỏi-thường-gặp)
23. [Phụ lục](#23-phụ-lục)

---

## 1. CÁCH ĐỌC TÀI LIỆU NÀY

Tài liệu dài, không cần đọc hết một lần. Chọn lộ trình theo vai trò:

| Bạn là | Đọc theo thứ tự | Có thể bỏ qua |
|---|---|---|
| **Người mới hoàn toàn** | 2 → 3 → 4 → 8 → 9 → 10 → 11 | 15, 20 |
| **Frontend** | 2 → 4 → 8 → 19 → 11 → 9 → 18 | 5, 6, 13, 15, 20 |
| **Tester** | 2 → 4 → 8 → 9 → 10 → 11 → 12 → 18 → rồi mở `TEST_SONG_POSTMAN.md` | 5, 6, 20 |
| **Backend sắp sửa code** | Đọc hết, đặc biệt 5, 6, 7, 13, 14, 15 | |

Quy ước trong tài liệu:
- `chữ như thế này` là tên trong code: class, method, cột database, tên trường JSON.
- **400 `"Song does not exist"`** nghĩa là server trả mã HTTP 400 kèm JSON `{ "message": "Song does not exist" }`.
- "Người nghe" = bất kỳ ai đã đăng nhập (USER, ARTIST hay ADMIN đều có thể nghe nhạc).

---

## 2. MODULE SONG GIẢI QUYẾT BÀI TOÁN GÌ?

### 2.1. Một câu

> Cho phép **nghệ sĩ đăng nhạc**, **Admin kiểm duyệt**, và **mọi người nghe nhạc đã được duyệt**.

### 2.2. Câu chuyện minh họa

1. **Mây** là một ca sĩ. Mây đã có tài khoản MySpotify và được Admin cấp vai trò ARTIST (xem module User).
2. Mây đăng bài **"Chiều Mưa"**: chọn file `chieu-mua.mp3`, ảnh bìa, mời bạn **Sóng** hát chung (feat), hẹn phát hành lúc **20:00 ngày 24/12**.
3. Bài vào trạng thái **chờ duyệt**. Chưa ai nghe được.
4. **Admin** mở danh sách bài chờ duyệt, nghe thử, thấy ảnh bìa không phù hợp → **từ chối** với lý do *"Ảnh bìa không phù hợp"*.
5. Mây thấy lý do, **đổi ảnh bìa**. Bài tự động quay lại **chờ duyệt**.
6. Admin **duyệt**. Bài đã được duyệt nhưng vẫn **chưa phát hành** vì chưa tới 20:00 ngày 24/12.
7. Đúng 20:00 ngày 24/12, bài **tự xuất hiện** với người nghe. Không ai phải bấm gì.
8. **Lan** (người nghe) tìm "chiều mưa", bấm nghe. Lượt nghe tăng lên 1. Bài được ghi vào **lịch sử nghe gần đây** của Lan.
9. Lan tìm "Sóng" cũng ra bài này (vì Sóng được feat). Trang nghệ sĩ của Sóng cũng hiển thị bài này.
10. Một thời gian sau, Mây muốn tạm cất bài → **ẩn bài**. Bài biến mất với người nghe, lượt nghe vẫn giữ nguyên. Mây **hiện lại** thì bài trở lại như cũ.
11. Có người khiếu nại bản quyền → Admin **gỡ bài** kèm lý do. Mây sửa lại thì bài lại vào hàng chờ duyệt.
12. Mây không muốn giữ bài nữa → **xóa bài**. File nhạc, ảnh bị xóa khỏi server.

Toàn bộ module là để làm được 12 bước trên một cách **an toàn, đúng quyền và không lỗi khi nhiều người dùng cùng lúc**.

### 2.3. Ba vai trò

| Vai | Làm được gì trong module Song |
|---|---|
| **ARTIST** (nghệ sĩ) | Đăng bài, xem bài của mình, sửa tên, đổi danh sách feat, đổi ảnh bìa, đổi lịch phát hành, ẩn/hiện, xóa, tìm nghệ sĩ khác để feat |
| **ADMIN** (quản trị) | Xem mọi bài, lọc theo trạng thái, duyệt, từ chối, gỡ bài đã duyệt |
| **Người nghe** (mọi tài khoản đã đăng nhập) | Xem danh sách bài, tìm kiếm, xem chi tiết, nghe nhạc, xem trang nghệ sĩ, xem lịch sử nghe gần đây |

ARTIST và ADMIN **cũng là người nghe**: họ dùng được mọi chức năng nghe nhạc.

### 2.4. Năm quy tắc nghiệp vụ cốt lõi

Nếu chỉ nhớ được 5 điều, hãy nhớ 5 điều này:

1. **Không có gì được phát công khai nếu Admin chưa duyệt.**
2. **Người nghe chỉ thấy bài "đã phát hành"**: đã duyệt + đã tới giờ phát hành + nghệ sĩ chính còn hoạt động.
3. **Artist chỉ đụng được vào bài của chính mình.** Bài của người khác thì coi như không tồn tại.
4. **Bị từ chối hoặc bị gỡ thì luôn có lý do**, và Artist sửa lại là bài tự động được gửi duyệt lại.
5. **Lượt nghe không thể bị "cày"**: cùng 1 tài khoản nghe cùng 1 bài chỉ được tính 1 lượt mỗi 30 giây.

---

## 3. TỪ ĐIỂN THUẬT NGỮ

Đọc lướt bảng này trước. Gặp từ lạ ở phần sau thì quay lại tra.

### 3.1. Thuật ngữ chung về web và API

| Thuật ngữ | Giải thích dễ hiểu |
|---|---|
| **Backend** | Chương trình chạy trên server, nhận yêu cầu, xử lý, đọc/ghi dữ liệu. Dự án này là backend |
| **Frontend** | Giao diện người dùng thấy (web/app), gọi backend để lấy dữ liệu |
| **API** | "Cửa" mà frontend gọi vào backend. VD: `GET /api/songs` |
| **Endpoint** | 1 API cụ thể = method + đường dẫn. VD: `POST /api/songs/15/play` |
| **HTTP method** | Loại hành động: `GET` (đọc), `POST` (tạo / ghi nhận), `PUT` (sửa), `DELETE` (xóa) |
| **Request / Response** | Yêu cầu frontend gửi đi / kết quả backend trả về |
| **JSON** | Định dạng dữ liệu dạng chữ, VD `{ "title": "Chiều Mưa" }` |
| **Body** | Phần dữ liệu gửi kèm request (JSON hoặc form-data) |
| **Path variable** | Giá trị nằm trong đường dẫn. `/api/songs/15` → `songId = 15` |
| **Query parameter** | Giá trị sau dấu `?`. `/api/songs?page=0&size=20` |
| **multipart/form-data** | Kiểu body dùng khi **gửi file**. JSON không chứa được file nhị phân |
| **HTTP status code** | Con số cho biết kết quả: 200 thành công, 400 dữ liệu sai, 401 chưa đăng nhập, 403 không đủ quyền, 404 không tìm thấy, 409 xung đột, 429 gửi quá nhiều, 500 lỗi server |

### 3.2. Thuật ngữ về đăng nhập và quyền

| Thuật ngữ | Giải thích |
|---|---|
| **JWT / Token** | Chuỗi dài (bắt đầu bằng `eyJ...`) nhận được khi đăng nhập. Gửi kèm mọi request để server biết bạn là ai |
| **Header Authorization** | Nơi đặt token: `Authorization: Bearer eyJ...` |
| **Role** | Vai trò: `USER`, `ARTIST`, `ADMIN` |
| **Authentication (xác thực)** | Kiểm tra "bạn là ai" → sai thì **401** |
| **Authorization (phân quyền)** | Kiểm tra "bạn được làm gì" → không đủ quyền thì **403** |
| **Session đơn (single session)** | Mỗi tài khoản chỉ đăng nhập 1 nơi. Đăng nhập nơi mới → token cũ hết hiệu lực |
| **Rate limit** | Giới hạn số request trong 1 khoảng thời gian, chống spam |

### 3.3. Thuật ngữ Spring Boot / Java

| Thuật ngữ | Giải thích |
|---|---|
| **Controller** | Lớp nhận request HTTP, gọi Service, trả JSON. Không chứa logic nghiệp vụ |
| **Service** | Lớp chứa **logic nghiệp vụ**: kiểm tra điều kiện, đổi trạng thái... |
| **Repository** | Lớp đọc/ghi database. Spring tự sinh code SQL từ tên method |
| **Entity** | Class Java ánh xạ tới 1 bảng MySQL. `Song` ↔ bảng `songs` |
| **Document** | Giống entity nhưng cho MongoDB. `ListeningHistory` ↔ collection `listening_history` |
| **DTO** (Data Transfer Object) | Class chỉ dùng để **đóng gói dữ liệu gửi/nhận** qua API. Tách riêng khỏi entity để không lộ dữ liệu nội bộ |
| **Request DTO** | Dữ liệu frontend gửi lên (VD `UploadSongRequest`) |
| **Response DTO** | Dữ liệu backend trả về (VD `SongResponse`) |
| **Mapper** | Code chuyển entity → DTO (VD `SongMapper`) |
| **Enum** | Kiểu dữ liệu chỉ có vài giá trị cố định. `SongStatus` chỉ có `PENDING, APPROVED, REJECTED, HIDDEN, DELETED` |
| **Annotation** | Ký hiệu bắt đầu bằng `@` gắn lên code để Spring tự xử lý. VD `@NotBlank` = không được rỗng |
| **Validation** | Kiểm tra dữ liệu đầu vào hợp lệ (độ dài, bắt buộc...) |
| **Dependency Injection** | Spring tự tạo đối tượng và "tiêm" vào nơi cần, ta không tự `new` |
| **Exception** | Lỗi được "ném" ra khi có vấn đề. Service ném `IllegalArgumentException` → trả 400 |

### 3.4. Thuật ngữ dữ liệu

| Thuật ngữ | Giải thích |
|---|---|
| **MySQL** | Database dạng bảng, lưu dữ liệu chính (user, bài hát) |
| **MongoDB** | Database dạng document (giống JSON), dùng lưu **lịch sử nghe** |
| **MongoDB Atlas** | Dịch vụ MongoDB trên cloud, dự án dùng gói miễn phí |
| **Redis** | Database trong bộ nhớ, rất nhanh, dùng cho dữ liệu tạm: phiên đăng nhập, chống spam lượt nghe, rate limit |
| **Index** | "Mục lục" của bảng giúp tìm nhanh |
| **TTL** (Time To Live) | Thời gian sống. Hết TTL, dữ liệu tự bị xóa |
| **Soft delete (xóa mềm)** | Không xóa dòng khỏi database, chỉ đánh dấu `status = DELETED` |
| **Pagination (phân trang)** | Chia danh sách dài thành nhiều trang |
| **Optimistic locking** | Cơ chế phát hiện 2 người cùng sửa 1 dòng dữ liệu cùng lúc (cột `version`) |

### 3.5. Thuật ngữ nghiệp vụ của module

| Thuật ngữ | Giải thích |
|---|---|
| **Artist chính** | Người đăng bài, chủ sở hữu bài (`artist`) |
| **Feat / Artist feat** | Nghệ sĩ khác được ghi tên hát cùng (`featuredArtists`). Tối đa 5 người |
| **Duyệt (approve)** | Admin cho phép bài được phát hành |
| **Từ chối (reject)** | Admin không cho bài **đang chờ duyệt** được phát hành |
| **Gỡ (remove)** | Admin hạ bài **đã duyệt** xuống |
| **Lịch phát hành (`releaseAt`)** | Thời điểm bài bắt đầu được công khai |
| **Đã phát hành (`released`)** | Bài đã duyệt **và** đã tới giờ phát hành |
| **Lượt nghe (`playCount`)** | Số lần bài được nghe (đã lọc spam) |

---

## 4. DANH SÁCH USE CASE

| Mã | Tên | Ai | API |
|---|---|---|---|
| UC20 | Xem danh sách bài hát | Người nghe | `GET /api/songs` |
| UC21 | Tìm kiếm bài hát | Người nghe | `GET /api/songs/search` |
| UC22 | Xem chi tiết bài hát | Người nghe | `GET /api/songs/{songId}` |
| UC23 | Nghe nhạc | Người nghe | Không có API riêng — dùng `audioUrl` |
| UC24 | Ghi nhận lượt nghe | Người nghe | `POST /api/songs/{songId}/play` |
| UC25 | Xem trang nghệ sĩ | Người nghe | `GET /api/songs/artists/{artistId}` |
| UC26 | Đăng bài hát | ARTIST | `POST /api/artist/songs` |
| UC28 | Xem bài hát của tôi | ARTIST | `GET /api/artist/songs` |
| UC29 | Sửa tên và danh sách feat | ARTIST | `PUT /api/artist/songs/{songId}` |
| UC30 | Đổi ảnh bìa | ARTIST | `PUT /api/artist/songs/{songId}/cover` |
| UC31 | Đổi lịch phát hành | ARTIST | `PUT /api/artist/songs/{songId}/release` |
| UC32 | Ẩn / hiện bài hát | ARTIST | `PUT /api/artist/songs/{songId}/hide` và `/show` |
| UC33 | Xóa bài hát | ARTIST | `DELETE /api/artist/songs/{songId}` |
| UC34 | Xem danh sách bài (quản trị) | ADMIN | `GET /api/admin/songs` |
| UC35 | Duyệt / từ chối bài | ADMIN | `PUT /api/admin/songs/{songId}/approve` và `/reject` |
| UC36 | Gỡ bài đã duyệt | ADMIN | `PUT /api/admin/songs/{songId}/remove` |
| UC37 | Tìm nghệ sĩ để feat | ARTIST | `GET /api/artist/search-artists` |
| — | Lịch sử nghe gần đây | Người nghe | `GET /api/users/me/history` |

---

## 5. KIẾN TRÚC VÀ VỊ TRÍ CÁC FILE

### 5.1. Kiến trúc 3 tầng

```
 Frontend (web / app / Postman)
        │  HTTP request (JSON hoặc form-data, kèm token)
        ▼
┌──────────────────────────────────────────────────────────────┐
│ TẦNG BẢO MẬT (Security Filter Chain)                          │
│   JWT → kiểm tra phiên (Redis) → Rate limit (Redis) → phân quyền│
└──────────────────────────────────────────────────────────────┘
        ▼
┌───────────────┐   Nhận request, kiểm tra hình thức dữ liệu (@Valid),
│  CONTROLLER   │   gọi Service, đổi kết quả sang DTO, trả JSON
└───────────────┘
        ▼
┌───────────────┐   LOGIC NGHIỆP VỤ: kiểm tra trạng thái, quyền sở hữu,
│    SERVICE    │   đổi trạng thái, lưu file, ghi lịch sử...
└───────────────┘
        ▼
┌───────────────┐   Đọc / ghi dữ liệu
│  REPOSITORY   │
└───────────────┘
        ▼
   MySQL (songs, users...)   MongoDB (listening_history)   Redis (tạm)   Ổ đĩa (file mp3, ảnh)
```

**Vì sao tách tầng?**
- Mỗi tầng 1 việc → dễ đọc, dễ sửa, dễ test.
- Logic nghiệp vụ **chỉ nằm ở Service**. Muốn biết "khi nào được ẩn bài" chỉ cần mở `ArtistSongService.hideSong`, không phải tìm khắp nơi.
- Controller mỏng: không có `if/else` nghiệp vụ.

### 5.2. Danh sách file của module

```
src/main/java/com/winhmm/myspotify/
├── controller/
│   ├── SongController.java              ← API người nghe (UC20–UC25)
│   ├── ArtistSongController.java        ← API nghệ sĩ (UC26–UC33, UC37)
│   ├── AdminSongController.java         ← API quản trị (UC34–UC36)
│   └── ListeningHistoryController.java  ← API lịch sử nghe
├── service/
│   ├── SongService.java                 ← Nghiệp vụ người nghe + ghi lượt nghe
│   ├── ArtistSongService.java           ← Nghiệp vụ nghệ sĩ
│   ├── AdminSongService.java            ← Nghiệp vụ quản trị
│   ├── FileStorageService.java          ← Lưu / kiểm tra / xóa file nhạc, ảnh
│   └── ListeningHistoryService.java     ← Ghi và đọc lịch sử nghe (MongoDB)
├── repository/
│   ├── SongRepository.java              ← Truy vấn bảng songs (MySQL)
│   └── ListeningHistoryRepository.java  ← Truy vấn collection listening_history (MongoDB)
├── entity/
│   └── Song.java                        ← Ánh xạ bảng songs
├── document/
│   └── ListeningHistory.java            ← Ánh xạ collection listening_history
├── enums/
│   └── SongStatus.java                  ← PENDING, APPROVED, REJECTED, HIDDEN, DELETED
├── mapper/
│   └── SongMapper.java                  ← Song → SongResponse / SongManageResponse / PageResponse
└── dto/song/
    ├── UploadSongRequest.java           ← Dữ liệu đăng bài (form-data)
    ├── UpdateSongRequest.java           ← Sửa tên + feat (JSON)
    ├── UpdateReleaseRequest.java        ← Đổi lịch (JSON)
    ├── RejectSongRequest.java           ← Lý do từ chối / gỡ (JSON)
    ├── SongResponse.java                ← Bài hát cho người nghe (rút gọn)
    ├── SongManageResponse.java          ← Bài hát cho Artist / Admin (đầy đủ)
    ├── ArtistSummary.java               ← { id, artistName } của 1 nghệ sĩ
    ├── ArtistSongsResponse.java         ← Trang nghệ sĩ = thông tin + danh sách bài
    ├── ListeningHistoryResponse.java    ← { song, playedAt }
    └── PageResponse.java                ← Khung phân trang chung
```

Các file **dùng chung** với module khác nhưng module Song phụ thuộc:

| File | Vai trò với module Song |
|---|---|
| `config/SecurityConfig.java` | Quy định API nào cần đăng nhập, bật `@PreAuthorize` |
| `config/WebMvcConfig.java` | Cho phép truy cập file qua đường dẫn `/uploads/**` |
| `security/JwtAuthenticationFilter.java` | Đọc token, kiểm tra phiên |
| `security/RateLimitFilter.java` | Giới hạn số request |
| `exception/GlobalExceptionHandler.java` | Chuyển mọi lỗi thành JSON thống nhất |
| `entity/User.java` | Artist chính và Artist feat đều là `User` |
| `repository/UserRepository.java` | Tìm nghệ sĩ để feat |
| `service/UserService.java` | Lấy user từ email trong token |

### 5.3. Ai gọi ai

```
SongController ───────────► SongService ──────────┬──► SongRepository ──► MySQL
                                                  ├──► UserRepository ──► MySQL
                                                  ├──► Redis (chống spam lượt nghe)
                                                  └──► ListeningHistoryService

ArtistSongController ─────► ArtistSongService ────┬──► SongRepository
       │                                          ├──► UserRepository
       └──► UserService (lấy id từ email)         └──► FileStorageService ──► Ổ đĩa

AdminSongController ──────► AdminSongService ─────────► SongRepository

ListeningHistoryController ► ListeningHistoryService ─┬► ListeningHistoryRepository ► MongoDB
       │                                              └► SongRepository ► MySQL
       └──► UserService

Mọi Controller ──► SongMapper (đổi entity sang DTO trước khi trả về)
```

---

## 6. HÀNH TRÌNH CỦA 1 REQUEST

Lấy ví dụ: Artist gửi `PUT /api/artist/songs/15/hide` để ẩn bài 15.

```
① Request tới server (Tomcat, cổng 8080)
     │
② CORS: cho phép frontend ở domain khác gọi vào (dự án mở cho mọi origin)
     │
③ JwtAuthenticationFilter
     ├─ Lấy token trong header "Authorization: Bearer ..."
     ├─ Kiểm tra chữ ký và hạn của token (24 giờ)
     ├─ Đọc email + sessionId trong token
     ├─ Hỏi Redis: sessionId này có phải phiên MỚI NHẤT của email không?
     │     Không → ghi nhận lỗi "Your session has ended..." → về sau trả 401
     ├─ Đọc user từ MySQL, kiểm tra tài khoản còn hoạt động
     └─ Hợp lệ → ghi nhận "người đang gọi là mây@gmail.com, role ARTIST"
     │
④ RateLimitFilter
     ├─ Đếm số request của tài khoản này trong 60 giây (Redis)
     └─ Quá 100 → trả 429 ngay, dừng tại đây
     │
⑤ SecurityConfig: API này có cần đăng nhập không?
     └─ /api/artist/** không nằm trong danh sách công khai → bắt buộc đăng nhập
        Chưa đăng nhập → 401
     │
⑥ @PreAuthorize("hasRole('ARTIST')") trên ArtistSongController
     └─ Không phải ARTIST → 403
     │
⑦ ArtistSongController.hideSong(...)
     ├─ Lấy id của Artist từ email trong token
     └─ Gọi artistSongService.hideSong(artistId, 15)
     │
⑧ ArtistSongService.hideSong
     ├─ Kiểm tra Artist còn hoạt động
     ├─ Tìm bài 15 CỦA CHÍNH Artist này (không phải của ai khác)
     │     Không thấy / đã xóa → ném lỗi "Song does not exist"
     ├─ Kiểm tra bài đang APPROVED
     │     Không phải → ném lỗi "Only approved songs can be hidden"
     ├─ Đổi status → HIDDEN
     └─ songRepository.save(song) → UPDATE songs ... (version tăng 1)
     │
⑨ Controller nhận Song đã lưu → SongMapper.toManageResponse(song) → SongManageResponse
     │
⑩ Spring tự đổi DTO thành JSON → trả 200
```

**Nếu có lỗi ở bước ⑧:** Service ném `IllegalArgumentException("...")` → `GlobalExceptionHandler` bắt → trả **400** `{ "message": "..." }`. Controller không cần `try/catch`.

---

## 7. DỮ LIỆU: MYSQL, MONGODB VÀ Ổ ĐĨA

Module Song lưu dữ liệu ở **4 nơi**, mỗi nơi 1 mục đích:

| Nơi | Lưu gì | Vì sao chọn nơi này |
|---|---|---|
| **MySQL** | Thông tin bài hát, quan hệ feat | Dữ liệu chính, cần quan hệ chặt chẽ, truy vấn phức tạp |
| **MongoDB** | Từng lượt nghe (lịch sử) | Dữ liệu nhiều, chỉ ghi thêm, cấu trúc đơn giản, tự xóa sau 90 ngày |
| **Redis** | Khóa chống spam lượt nghe 30 giây | Dữ liệu tạm, tự hết hạn, cần cực nhanh |
| **Ổ đĩa server** | File `.mp3`, `.wav`, ảnh bìa | File lớn, không nên nhét vào database |

### 7.1. Bảng `songs` (MySQL)

| Cột | Kiểu | Bắt buộc | Ý nghĩa |
|---|---|---|---|
| `id` | BIGINT, tự tăng | Có | Mã bài hát |
| `artist_id` | BIGINT → `users.id` | Có | Artist chính (chủ bài) |
| `title` | VARCHAR(150) | Có | Tên bài |
| `audio_url` | VARCHAR(255) | Có | Đường dẫn file nhạc, VD `/uploads/songs/audio/3f2a....mp3` |
| `cover_url` | VARCHAR(255) | Không | Đường dẫn ảnh bìa, có thể `NULL` |
| `duration_seconds` | INT | Có | Thời lượng (giây), **server tự đo** từ file |
| `play_count` | BIGINT | Có | Lượt nghe, mặc định 0 |
| `status` | ENUM | Có | `PENDING` / `APPROVED` / `REJECTED` / `HIDDEN` / `DELETED` |
| `release_at` | DATETIME | Không | Lịch phát hành. `NULL` = phát hành ngay khi được duyệt |
| `reject_reason` | VARCHAR(255) | Không | Lý do từ chối / gỡ |
| `created_at` | DATETIME | Có | Thời điểm đăng, **không bao giờ đổi** |
| `updated_at` | DATETIME | Có | Lần sửa gần nhất, tự cập nhật |
| `version` | BIGINT | Có | Số phiên bản, tự tăng mỗi lần sửa (xem mục 15) |

**Index (mục lục) để tìm nhanh:**

| Tên index | Cột | Phục vụ truy vấn |
|---|---|---|
| `idx_songs_status_release` | `status, release_at` | "Lấy bài APPROVED đã tới giờ phát hành" (người nghe) |
| `idx_songs_artist_status` | `artist_id, status` | "Lấy bài của Artist X" |

**Hai điểm đặc biệt trong entity `Song`:**

1. `play_count` được khai báo `updatable = false`. Nghĩa là khi gọi `songRepository.save(song)` (VD lúc Artist sửa tên), Hibernate **không ghi đè** cột này. Lượt nghe chỉ được tăng bằng 1 câu lệnh riêng `UPDATE songs SET play_count = play_count + 1`. Lý do: tránh trường hợp Artist sửa tên đúng lúc có người nghe, lượt nghe bị ghi đè bằng số cũ (xem mục 15).

2. `status` là kiểu **ENUM của MySQL** (Hibernate tự tạo). Nếu sau này **thêm giá trị mới** vào `SongStatus` trong Java, phải chạy tay câu lệnh `ALTER TABLE` trên **cả máy local lẫn VPS**:
   ```sql
   ALTER TABLE songs MODIFY COLUMN status
       ENUM('APPROVED','DELETED','HIDDEN','PENDING','REJECTED', 'GIÁ_TRỊ_MỚI') NOT NULL;
   ```
   Thiếu bước này, lưu bài với trạng thái mới sẽ lỗi `Data truncated for column 'status'`.
   ⚠️ Câu lệnh phải liệt kê **đủ mọi giá trị cũ**, thiếu giá trị nào là mất giá trị đó.

### 7.2. Bảng `song_featured_artists` (MySQL)

Bảng trung gian cho quan hệ **nhiều-nhiều**: 1 bài có nhiều người feat, 1 nghệ sĩ được feat trong nhiều bài.

| Cột | Ý nghĩa |
|---|---|
| `song_id` → `songs.id` | Bài hát |
| `artist_id` → `users.id` | Nghệ sĩ được feat |

Ví dụ: bài 15 feat nghệ sĩ 12 và 13 → 2 dòng `(15, 12)` và `(15, 13)`.

### 7.3. Sơ đồ quan hệ

```
 ┌──────────────┐ 1          N ┌──────────────┐
 │    users     │──────────────│    songs     │   1 Artist có nhiều bài
 │──────────────│  artist_id   │──────────────│
 │ id           │              │ id           │
 │ role         │              │ artist_id    │
 │ account_status│             │ status ...   │
 │ artist_name  │              └──────┬───────┘
 └──────┬───────┘                     │ 1
        │ 1                           │
        │            N ┌──────────────┴─────────┐ N
        └──────────────│ song_featured_artists  │    nhiều-nhiều: feat
             artist_id │────────────────────────│
                       │ song_id, artist_id     │
                       └────────────────────────┘

 MongoDB (tách riêng, không có khóa ngoại):
 ┌───────────────────────────┐
 │ listening_history          │   userId  ~~> users.id   (chỉ là con số)
 │ { userId, songId, playedAt}│   songId  ~~> songs.id   (chỉ là con số)
 └───────────────────────────┘
```

### 7.4. Collection `listening_history` (MongoDB)

Mỗi **lượt nghe được tính** = 1 document:
```json
{
    "_id": "6710b2f9c1e4a52d8f3a9b01",
    "userId": 6,
    "songId": 15,
    "playedAt": "2026-10-15T10:40:12.000Z"
}
```

| Index | Định nghĩa | Mục đích |
|---|---|---|
| `idx_user_played_at` | `{ userId: 1, playedAt: -1 }` | Lấy nhanh các lượt nghe mới nhất của 1 người |
| `idx_played_at_ttl` | `playedAt`, hết hạn sau **90 ngày** | MongoDB **tự xóa** lượt nghe cũ hơn 90 ngày |

Database: `myspotify_db` ở máy local, `myspotify_prod` trên VPS (tách riêng để dữ liệu test không lẫn dữ liệu thật). Index được Spring tự tạo khi khởi động (`spring.data.mongodb.auto-index-creation=true`).

> MongoDB lưu thời gian theo UTC nên nhìn trong Atlas sẽ thấy lệch 7 tiếng so với giờ Việt Nam. Khi đọc ra qua API, Spring tự đổi lại đúng giờ server. **Không phải lỗi.**

### 7.5. Redis

| Key | Giá trị | Sống bao lâu | Dùng để |
|---|---|---|---|
| `play:{songId}:{email}` | `"1"` | 30 giây | Đánh dấu "người này vừa nghe bài này", chặn tính lượt trùng |
| `rate:user:{email}` | Số đếm | 60 giây | Rate limit (dùng chung toàn hệ thống) |

### 7.6. Ổ đĩa

```
<app.upload-dir>/
└── songs/
    ├── audio/
    │   ├── 3f2a9c1e-7b4d-4e8a-9c21-5d6f7e8a9b0c.mp3
    │   └── 8c1d2e3f-....wav
    └── covers/
        ├── 9b1c4d7f-....jpg
        └── a2b3c4d5-....webp
```
- Local: `app.upload-dir=uploads` (thư mục `uploads` trong project).
- VPS: `app.upload-dir=/home/myspotify/uploads`.
- Tên file luôn là **UUID ngẫu nhiên** + đuôi, không dùng tên gốc người dùng đặt.
- Database chỉ lưu **đường dẫn** (`/uploads/songs/audio/...`), không lưu nội dung file.

---

## 8. TRẠNG THÁI VÀ VÒNG ĐỜI BÀI HÁT

Đây là phần **quan trọng nhất** để hiểu module.

### 8.1. Năm trạng thái

| status | Nghĩa | Ai đưa bài vào trạng thái này |
|---|---|---|
| `PENDING` | Chờ Admin duyệt | Artist đăng bài; hoặc Artist sửa bài đang REJECTED |
| `APPROVED` | Đã duyệt | Admin duyệt; hoặc Artist hiện lại bài HIDDEN |
| `REJECTED` | Bị từ chối / bị gỡ, kèm lý do | Admin từ chối bài PENDING; hoặc Admin gỡ bài APPROVED / HIDDEN |
| `HIDDEN` | Artist tự ẩn | Artist ẩn bài APPROVED |
| `DELETED` | Đã xóa (xóa mềm) | Artist xóa bài |

### 8.2. Sơ đồ chuyển trạng thái

```
                         ARTIST đăng bài
                               │
                               ▼
                       ┌──────────────┐
             ┌────────►│   PENDING    │◄────────────────────────┐
             │         └──────────────┘                         │
             │           │          │                           │
             │  ADMIN    │          │ ADMIN reject              │ ARTIST sửa tên/feat
             │  approve  │          │ (bắt buộc lý do)          │ hoặc đổi ảnh bìa
             │           ▼          ▼                           │
             │   ┌──────────────┐  ┌──────────────┐             │
             │   │   APPROVED   │  │   REJECTED   │─────────────┘
             │   └──────────────┘  └──────────────┘
             │     │    ▲    │            ▲    ▲
             │     │    │    │ ADMIN      │    │
             │     │    │    │ remove ────┘    │
             │     │    │    │ (lý do)         │
             │     │    │                      │
             │ ARTIST   │ ARTIST               │ ADMIN remove
             │  hide    │  show                │ (lý do)
             │     ▼    │                      │
             │   ┌──────────────┐              │
             │   │    HIDDEN    │──────────────┘
             │   └──────────────┘
             │
   (PENDING không bao giờ quay về từ APPROVED / HIDDEN trực tiếp —
    phải qua REJECTED rồi Artist sửa)

   ┌───────────────────────────────────────────────────────────────┐
   │ Từ BẤT KỲ trạng thái nào (trừ DELETED): ARTIST delete → DELETED │
   │ DELETED là điểm cuối, không quay lại được.                      │
   └───────────────────────────────────────────────────────────────┘
```

### 8.3. Bảng "ở trạng thái X thì được làm gì"

✔ = được phép · ✘ = bị chặn (kèm message) · — = không áp dụng

| Hành động | PENDING | APPROVED (chưa tới lịch) | APPROVED (đã phát hành) | REJECTED | HIDDEN | DELETED |
|---|---|---|---|---|---|---|
| **Artist** sửa tên/feat | ✔ giữ PENDING | ✔ giữ APPROVED | ✔ giữ APPROVED | ✔ → **PENDING** | ✔ giữ HIDDEN | ✘ Song does not exist |
| **Artist** đổi ảnh | ✔ | ✔ | ✔ | ✔ → **PENDING** | ✔ | ✘ |
| **Artist** đổi lịch | ✔ | ✔ | ✘ Cannot change the schedule of a released song | ✔ (vẫn REJECTED) | ✘ nếu đã từng phát hành | ✘ |
| **Artist** ẩn | ✘ Only approved songs can be hidden | ✔ → HIDDEN | ✔ → HIDDEN | ✘ | ✘ | ✘ |
| **Artist** hiện | ✘ Only hidden songs can be shown again | ✘ | ✘ | ✘ | ✔ → APPROVED | ✘ |
| **Artist** xóa | ✔ → DELETED | ✔ | ✔ | ✔ | ✔ | ✘ |
| **Admin** duyệt | ✔ → APPROVED | ✘ Only pending songs can be approved | ✘ | ✘ | ✘ | ✘ Song does not exist |
| **Admin** từ chối | ✔ → REJECTED | ✘ Only pending songs can be rejected | ✘ | ✘ | ✘ | ✘ |
| **Admin** gỡ | ✘ Only approved songs can be removed | ✔ → REJECTED | ✔ → REJECTED | ✘ | ✔ → REJECTED | ✘ |
| **Người nghe** thấy | ✘ | ✘ | ✔ | ✘ | ✘ | ✘ |

### 8.4. "Đã phát hành" — 3 điều kiện

Người nghe chỉ thấy bài khi **đủ cả 3** (hằng số `PUBLISHED_CONDITION` trong `SongRepository`):

```
1. status = APPROVED
2. releaseAt <= giờ hiện tại của server
3. Artist chính có accountStatus = ACTIVE và role = ARTIST
```

Điều kiện 3 có nghĩa: Admin **khóa** 1 nghệ sĩ → **toàn bộ** bài của nghệ sĩ đó biến mất ngay lập tức với người nghe. **Mở khóa** → bài xuất hiện lại, lượt nghe giữ nguyên. Không phải sửa từng bài.

Vì sao không có "công việc chạy ngầm" bật bài lên đúng giờ? Vì không cần: mỗi lần người nghe gọi API, server so `releaseAt` với **giờ lúc đó**. Tới giờ thì tự nhiên bài thỏa điều kiện 2.

### 8.5. Trường `released` trong API của Artist/Admin

Được tính trong `SongMapper.toManageResponse`:
```
released = (status == APPROVED) && (releaseAt != null) && (releaseAt <= bây giờ)
```
Không lưu trong database, mỗi lần trả về lại tính mới.

Lưu ý: `released` **không** xét điều kiện 3 (Artist bị khóa). Bài của Artist bị khóa vẫn có thể `released = true` trong API quản trị nhưng người nghe không thấy.

| status | released | Ý nghĩa hiển thị |
|---|---|---|
| PENDING | false | Chờ duyệt |
| APPROVED | false | Đã lên lịch |
| APPROVED | true | Đã phát hành |
| REJECTED | false | Bị từ chối (xem `rejectReason`) |
| HIDDEN | false | Đã ẩn |

### 8.6. Vì sao "gỡ bài" lại dùng REJECTED mà không dùng DELETED?

- **DELETED** là Artist tự xóa, **file bị xóa khỏi ổ đĩa**, không khôi phục được.
- Admin gỡ thường vì vi phạm có thể sửa được (sai ảnh, khiếu nại...). Dùng **REJECTED + lý do** để Artist **biết lý do và sửa lại**, bài giữ nguyên file và lượt nghe.
- Nhờ vậy chỉ cần **1 cơ chế** "bị từ chối → sửa → tự gửi duyệt lại" cho cả 2 trường hợp từ chối và gỡ.

### 8.7. Vì sao xóa là "xóa mềm"?

`DELETE /api/artist/songs/{id}` không xóa dòng trong bảng `songs`, chỉ đặt `status = DELETED` (nhưng **có** xóa file trên ổ đĩa). Lý do:
- Admin vẫn tra cứu được (`?status=DELETED`) khi có khiếu nại.
- Lịch sử nghe trong MongoDB vẫn trỏ tới `songId` cũ mà không lỗi (bài DELETED tự bị lọc khỏi kết quả).
- Không phải xử lý dây chuyền xóa bảng `song_featured_artists`.

---

## 9. LUỒNG NGHIỆP VỤ CỦA ARTIST

Mọi API ở phần này:
- Đường dẫn bắt đầu bằng `/api/artist`.
- Bắt buộc token của tài khoản **ARTIST** (`@PreAuthorize("hasRole('ARTIST')")`). USER, ADMIN gọi → **403**.
- Controller: `ArtistSongController`. Service: `ArtistSongService`.
- Id của Artist **luôn lấy từ token**, không bao giờ lấy từ URL hay body → không giả mạo được thành Artist khác.

### 9.0. Hai kiểm tra chung ở đầu mọi thao tác

**a) Artist còn hoạt động** (`getArtist`): role phải là ARTIST và tài khoản ACTIVE.
Không đạt → 400 `"Only active artists can manage songs"`.
(Thực tế tài khoản bị khóa đã bị chặn từ bước JWT, đây là lớp phòng thủ thứ 2.)

**b) Bài phải là của mình** (`getMySong`): tìm bài bằng **cả 2 điều kiện** `id = songId AND artist_id = id của tôi` (`findByIdAndArtistId`).
- Bài không tồn tại → 400 `"Song does not exist"`
- Bài **của người khác** → **cũng** 400 `"Song does not exist"`
- Bài đã DELETED → **cũng** 400 `"Song does not exist"`

Vì sao không trả "Bạn không có quyền" khi bài của người khác? Vì như vậy kẻ xấu sẽ biết "bài id 15 có tồn tại", rồi dò từng id để đoán dữ liệu. Trả chung 1 message → không lộ gì.

### 9.1. UC26 — Đăng bài hát

**API:** `POST /api/artist/songs` · **Body:** `multipart/form-data`

| Key | Bắt buộc | Kiểm tra ở DTO (`UploadSongRequest`) | Kiểm tra ở Service |
|---|---|---|---|
| `title` | Có | `@NotBlank`, tối đa 150 ký tự | Cắt dấu cách 2 đầu (`trim`) |
| `audio` | Có | `@NotNull` | Đuôi `.mp3`/`.wav`, dung lượng, file nhạc thật, đo thời lượng |
| `cover` | Không | — | JPG/PNG/WEBP, ≤ 2MB, ảnh thật |
| `featuredArtistIds` | Không | Tối đa 5 phần tử | Bỏ trùng, bỏ ô trống, không tự feat, phải là Artist đang hoạt động |
| `releaseAt` | Không | Đúng định dạng `yyyy-MM-ddTHH:mm:ss` | Phải ở tương lai |

**Các bước xử lý** (`ArtistSongService.uploadSong`) — **thứ tự rất quan trọng**:

```
 1. Kiểm tra Artist còn hoạt động
 2. Kiểm tra releaseAt ở tương lai (nếu có)
 3. Kiểm tra danh sách feat hợp lệ
        ▲ 3 bước trên chỉ đọc database, CHƯA lưu file nào.
        │ Sai ở đây → báo lỗi ngay, không tốn công lưu file rồi phải xóa.
 4. Lưu file nhạc xuống ổ đĩa (đã kiểm tra đuôi + dung lượng)
 ┌── try ─────────────────────────────────────────────────────┐
 │ 5. Đo thời lượng file nhạc (file hỏng / giả → lỗi)          │
 │ 6. Nếu có ảnh bìa: kiểm tra + lưu ảnh                        │
 │ 7. Tạo Song: status = PENDING, playCount = 0                 │
 │ 8. Lưu vào MySQL                                             │
 └── catch (bất kỳ lỗi nào ở 5–8) ────────────────────────────┘
       → Xóa file nhạc VÀ ảnh vừa lưu, rồi báo lỗi
 9. Trả về SongManageResponse
```

**Vì sao cần `try/catch` dọn file?** Nếu file nhạc đã lưu mà ảnh bìa bị lỗi (VD ảnh GIF), không dọn thì file nhạc nằm "mồ côi" trên ổ đĩa mãi mãi, không bài nào trỏ tới, chiếm dung lượng VPS.

**Quy tắc feat chi tiết** (`loadFeaturedArtists`):
```
Đầu vào: [12, 12, null, 13]
 1. Bỏ trùng            → {12, 13, null}
 2. Bỏ null             → {12, 13}
 3. Quá 5 người?        → lỗi "Maximum 5 featured artists"
 4. Có chính mình?      → lỗi "You cannot feature yourself"
 5. Tìm trong database:
      người nào không phải ARTIST hoặc bị khóa → lỗi "Featured artist is not available: <id>"
 6. Tìm thấy ít hơn số id gửi lên → lỗi "Some featured artists do not exist"
Kết quả: tập 2 nghệ sĩ 12, 13
```

**Kết quả thành công:**
```json
{
    "id": 15, "title": "Chiều Mưa",
    "audioUrl": "/uploads/songs/audio/3f2a9c1e-....mp3",
    "coverUrl": "/uploads/songs/covers/9b1c4d7f-....jpg",
    "durationSeconds": 215, "playCount": 0,
    "status": "PENDING", "released": false,
    "releaseAt": "2026-12-24T20:00:00", "rejectReason": null,
    "artistId": 5, "artistName": "Mây Chiều",
    "featuredArtists": [ { "id": 12, "artistName": "Sóng Nhỏ" } ],
    "createdAt": "2026-10-15T16:30:00", "updatedAt": "2026-10-15T16:30:00"
}
```

**Lỗi có thể gặp:**

| Tình huống | Kết quả |
|---|---|
| Thiếu / rỗng title | 400 `Title is required` |
| Title > 150 ký tự | 400 `Title must not exceed 150 characters` |
| Thiếu file nhạc | 400 `Audio file is required` |
| Đuôi không phải mp3/wav | 400 `Only MP3 or WAV files are allowed` |
| MP3 > 20MB / WAV > 100MB | 400 `MP3 file must not exceed 20MB` / `WAV file must not exceed 100MB` |
| File vượt giới hạn upload tổng (100MB/file, 110MB/request) | 413 `File is too large` |
| File giả (txt đổi đuôi) / hỏng | 400 `Invalid or corrupted audio file` |
| Ảnh sai định dạng | 400 `Only JPG, PNG or WEBP images are allowed` |
| Ảnh > 2MB | 400 `Image must not exceed 2MB` |
| Ảnh giả | 400 `Invalid or corrupted image file` |
| releaseAt sai định dạng | 400 `Invalid value for releaseAt` |
| releaseAt ở quá khứ | 400 `Release time must be in the future` |
| Feat sai | 400 (xem quy tắc feat ở trên) |

### 9.2. UC28 — Bài hát của tôi

**API:** `GET /api/artist/songs`

- Trả về **mảng** (không phân trang) mọi bài **mình là Artist chính**, **trừ DELETED**.
- Sắp xếp: đăng mới nhất ở đầu (`createdAt` giảm dần).
- Mỗi phần tử là `SongManageResponse` đầy đủ: có `status`, `released`, `rejectReason`.
- Bài mình **được feat** (của người khác) **không** nằm ở đây — xem ở trang nghệ sĩ (UC25).

Truy vấn: `findByArtistIdAndStatusNotOrderByCreatedAtDesc(artistId, DELETED)`.

### 9.3. UC37 — Tìm nghệ sĩ để feat

**API:** `GET /api/artist/search-artists?keyword=Sóng`

Dùng khi Artist gõ tên trong ô "Mời hát cùng" để lấy `id` đưa vào `featuredArtistIds`.

Quy tắc:
- `keyword` rỗng / toàn dấu cách → trả `[]` luôn (không truy vấn database).
- Tìm `artistName` **chứa** keyword, **không phân biệt hoa thường**.
- Chỉ nghệ sĩ **role ARTIST + đang ACTIVE**.
- **Loại chính mình** ra khỏi kết quả.
- Tối đa **10** kết quả.

Kết quả: `[ { "id": 12, "artistName": "Sóng Nhỏ" } ]`

### 9.4. UC29 — Sửa tên và danh sách feat

**API:** `PUT /api/artist/songs/{songId}` · **Body JSON:**
```json
{ "title": "Chiều Mưa (Acoustic)", "featuredArtistIds": [12] }
```

Các bước:
```
1. Kiểm tra Artist hoạt động
2. Tìm bài của chính mình (getMySong)
3. Đặt title mới (đã trim)
4. Đặt danh sách feat MỚI (cùng quy tắc như lúc đăng bài)
5. Nếu bài đang REJECTED → chuyển PENDING + xóa rejectReason   (resubmitIfRejected)
6. Lưu
```

⚠️ **`featuredArtistIds` thay thế hoàn toàn danh sách cũ.** Không gửi trường này hoặc gửi `[]` = bỏ hết feat. Frontend muốn giữ feat cũ phải gửi lại đủ id cũ.

**Sửa bài đang APPROVED (đã phát hành)** → vẫn APPROVED, **không cần duyệt lại**, người nghe thấy tên mới ngay. (Đây là quyết định thiết kế hiện tại, xem mục 21.)

### 9.5. UC30 — Đổi ảnh bìa

**API:** `PUT /api/artist/songs/{songId}/cover` · **Body:** form-data, key `cover`

Các bước:
```
1. Kiểm tra Artist, tìm bài của mình
2. Ghi nhớ đường dẫn ảnh CŨ
3. Lưu ảnh MỚI xuống ổ đĩa (kiểm tra định dạng, dung lượng, ảnh thật)
┌─ try ──────────────────────────────────────────────┐
│ 4. Đặt coverUrl = ảnh mới                           │
│ 5. Nếu đang REJECTED → PENDING (gửi duyệt lại)      │
│ 6. Lưu database                                     │
│ 7. Lưu xong mới XÓA ảnh cũ khỏi ổ đĩa              │
└─ catch: xóa ảnh MỚI vừa lưu, giữ nguyên ảnh cũ ────┘
```

Vì sao **lưu database xong mới xóa ảnh cũ**? Nếu xóa ảnh cũ trước mà lưu database thất bại → bài trỏ tới ảnh cũ đã mất → ảnh bìa bị vỡ. Làm theo thứ tự này thì lỗi ở bất kỳ bước nào, bài vẫn luôn có 1 ảnh hợp lệ.

### 9.6. UC31 — Đổi lịch phát hành

**API:** `PUT /api/artist/songs/{songId}/release` · **Body JSON:**
```json
{ "releaseAt": "2026-12-24T20:00:00" }      ← đặt / đổi lịch
{ "releaseAt": null }                       ← bỏ lịch
```

Các bước (`updateReleaseSchedule`):
```
1. Kiểm tra Artist, tìm bài của mình
2. Bài đã phát hành? → lỗi "Cannot change the schedule of a released song"
      ("đã phát hành" ở đây = APPROVED hoặc HIDDEN, và releaseAt đã qua)
3. releaseAt (nếu có) phải ở tương lai
4. Nếu releaseAt = null VÀ bài đã từng được duyệt (APPROVED/HIDDEN):
        → releaseAt = BÂY GIỜ  (phát hành ngay lập tức)
   Ngược lại:
        → releaseAt = giá trị gửi lên (có thể null)
5. Lưu
```

Bảng tình huống:

| Bài đang | Gửi | Kết quả |
|---|---|---|
| PENDING | ngày tương lai | Lưu lịch. Duyệt xong chờ tới giờ |
| PENDING | `null` | `releaseAt = null`. Duyệt xong phát hành ngay |
| APPROVED chưa tới lịch | ngày tương lai khác | Đổi lịch |
| APPROVED chưa tới lịch | `null` | **Phát hành ngay bây giờ** |
| APPROVED đã phát hành | bất kỳ | 400 `Cannot change the schedule of a released song` |
| HIDDEN đã từng phát hành | bất kỳ | 400 (như trên) |
| REJECTED | ngày tương lai | Đổi lịch, **vẫn REJECTED** (đổi lịch không gửi duyệt lại) |

**Vì sao không cho đổi lịch bài đã phát hành?** Bài đã công khai, đã có người nghe. Cho đổi sang tương lai = bài đột nhiên biến mất với mọi người, gây khó hiểu. Muốn tạm cất bài thì dùng **ẩn bài**.

Lưu ý định dạng: endpoint này nhận **JSON** nên ngày sai định dạng trả `"Invalid request data"` (khác với form-data ở UC26 trả `"Invalid value for releaseAt"`).

### 9.7. UC32 — Ẩn và hiện bài

**Ẩn:** `PUT /api/artist/songs/{songId}/hide` (không body)
- Chỉ bài **APPROVED** (kể cả chưa tới lịch) → HIDDEN.
- Khác → 400 `Only approved songs can be hidden`.
- Bài ẩn biến mất khỏi: danh sách, tìm kiếm, chi tiết, trang nghệ sĩ, lịch sử nghe. Không ghi được lượt nghe mới.
- **Lượt nghe giữ nguyên.**

**Hiện:** `PUT /api/artist/songs/{songId}/show` (không body)
- Chỉ bài **HIDDEN** → APPROVED.
- Khác → 400 `Only hidden songs can be shown again`.
- Bài bị Admin gỡ (REJECTED) **không** dùng "hiện" để lấy lại được — phải sửa bài để gửi duyệt lại.

**Ẩn khác xóa thế nào?**

| | Ẩn | Xóa |
|---|---|---|
| Khôi phục được | Có (hiện lại) | Không |
| File nhạc, ảnh | Giữ nguyên | **Bị xóa khỏi ổ đĩa** |
| Lượt nghe | Giữ | Mất cùng bài |
| Artist còn thấy trong "Bài hát của tôi" | Có | Không |

### 9.8. UC33 — Xóa bài

**API:** `DELETE /api/artist/songs/{songId}`
**Kết quả:** `{ "message": "Song has been deleted" }`

Các bước:
```
1. Kiểm tra Artist, tìm bài của mình (bài đã DELETED → "Song does not exist")
2. status = DELETED, lưu database
3. Xóa file nhạc và ảnh bìa khỏi ổ đĩa
```
Được xóa ở **mọi trạng thái** (trừ đã DELETED). Không có bước xác nhận ở backend — frontend nên hỏi lại "Bạn chắc chắn muốn xóa?" trước khi gọi.

Thứ tự **lưu database trước, xóa file sau** để: nếu lưu database lỗi thì file vẫn còn, bài vẫn dùng được bình thường.

---

## 10. LUỒNG NGHIỆP VỤ CỦA ADMIN

Mọi API ở phần này:
- Đường dẫn bắt đầu bằng `/api/admin/songs`.
- Bắt buộc token **ADMIN**. USER / ARTIST gọi → **403**.
- Controller: `AdminSongController`. Service: `AdminSongService`.
- Admin **không** cần là chủ bài. Bài DELETED được coi như không tồn tại với mọi thao tác (trừ xem danh sách lọc DELETED).

### 10.1. UC34 — Danh sách bài

**API:** `GET /api/admin/songs?status=PENDING&page=0&size=20`

| Tham số | Bắt buộc | Mặc định | Ghi chú |
|---|---|---|---|
| `status` | Không | (không lọc) | `PENDING`, `APPROVED`, `REJECTED`, `HIDDEN`, `DELETED` — **viết hoa** |
| `page` | Không | 0 | Trang đầu là 0 |
| `size` | Không | 20 | Server ép về khoảng 1–50 |

- Không truyền `status` → mọi bài **trừ DELETED**.
- `status=DELETED` → xem riêng các bài đã bị xóa.
- Sắp xếp: đăng mới nhất ở đầu.
- `status` sai (VD `pending` viết thường, `ABC`) → 400 `Invalid parameter: status`.

Cách dùng thực tế: Admin mở trang "Chờ duyệt" = `?status=PENDING`.

### 10.2. UC35 — Duyệt bài

**API:** `PUT /api/admin/songs/{songId}/approve` (không body)

Các bước (`approveSong`):
```
1. Tìm bài (không có / DELETED → "Song does not exist")
2. Bài phải đang PENDING → không thì "Only pending songs can be approved"
3. Artist chính phải còn ACTIVE và là ARTIST → không thì "Artist account is not active"
4. Nếu releaseAt = null → releaseAt = BÂY GIỜ (phát hành ngay)
   Nếu releaseAt có giá trị → giữ nguyên (kể cả khi đã qua → phát hành ngay)
5. Xóa rejectReason (bài từng bị từ chối giờ được duyệt thì lý do cũ không còn ý nghĩa)
6. status = APPROVED, lưu
```

Vì sao **gán `releaseAt = bây giờ`** khi Artist không hẹn lịch? Để danh sách người nghe sắp xếp theo `releaseAt` (mới phát hành lên đầu) luôn có giá trị để so sánh, và để biết chính xác bài phát hành lúc nào.

### 10.3. UC35 — Từ chối bài

**API:** `PUT /api/admin/songs/{songId}/reject` · **Body:** `{ "reason": "Ảnh bìa không phù hợp" }`

- Chỉ áp dụng bài **PENDING** → REJECTED, lưu lý do (đã trim).
- Bài không phải PENDING → 400 `Only pending songs can be rejected`.
- `reason` bắt buộc, không toàn dấu cách, tối đa 255 ký tự (`RejectSongRequest`).

**Vì sao bắt buộc lý do?** Không có lý do, Artist không biết sai ở đâu → gửi lại y nguyên → Admin lại từ chối → vòng lặp vô ích.

### 10.4. UC36 — Gỡ bài đã duyệt

**API:** `PUT /api/admin/songs/{songId}/remove` · **Body:** `{ "reason": "Vi phạm bản quyền" }`

- Chỉ áp dụng bài **APPROVED** (đã hoặc chưa tới lịch) hoặc **HIDDEN** → **REJECTED** + lý do.
- Khác → 400 `Only approved songs can be removed`.
- Dùng chung DTO `RejectSongRequest` với từ chối.

**Vì sao gỡ được cả bài HIDDEN?** Artist có thể ẩn bài vi phạm để "né", định sau này hiện lại. Admin phải gỡ được để bài không thể tự hiện lại.

**Reject và Remove khác nhau thế nào?**

| | Reject | Remove |
|---|---|---|
| Áp dụng cho | PENDING (chưa từng công khai) | APPROVED / HIDDEN (đã duyệt) |
| Trạng thái sau | REJECTED | REJECTED |
| Cần lý do | Có | Có |
| Artist sửa lại | Tự gửi duyệt lại | Tự gửi duyệt lại |

Tách thành 2 API để Admin không vô tình gỡ nhầm bài đang công khai khi chỉ định từ chối bài mới.

---

## 11. LUỒNG NGHIỆP VỤ CỦA NGƯỜI NGHE

Mọi API ở phần này:
- Đường dẫn `/api/songs/...`. Controller: `SongController`. Service: `SongService`.
- **Không giới hạn role** nhưng **phải đăng nhập** (không token → 401).
- Chỉ làm việc với bài **đã phát hành** (mục 8.4).
- Trả về `SongResponse` — bản **rút gọn**, không có `status`, `rejectReason`, `createdAt`, `updatedAt`, `version`.

`SongResponse`:
```json
{
    "id": 15,
    "title": "Chiều Mưa",
    "audioUrl": "/uploads/songs/audio/3f2a9c1e-....mp3",
    "coverUrl": "/uploads/songs/covers/9b1c4d7f-....jpg",
    "durationSeconds": 215,
    "playCount": 1024,
    "releaseAt": "2026-12-24T20:00:00",
    "artistId": 5,
    "artistName": "Mây Chiều",
    "featuredArtists": [ { "id": 12, "artistName": "Sóng Nhỏ" } ]
}
```

`featuredArtists` **tự động bỏ** những người feat đã bị khóa hoặc mất quyền Artist, và được sắp theo `id` tăng dần (`SongMapper.activeFeaturedArtists`).

### 11.1. UC20 — Danh sách bài đã phát hành

**API:** `GET /api/songs?page=0&size=20`

- Sắp xếp: **mới phát hành lên đầu** (`releaseAt` giảm dần).
- Kết quả dạng `PageResponse` (mục 17).

### 11.2. UC21 — Tìm kiếm

**API:** `GET /api/songs/search?keyword=may chieu&page=0&size=20`

Tìm `keyword` (khớp 1 phần, không phân biệt hoa thường) ở **3 nơi**:
1. Tên bài
2. Nghệ danh Artist chính
3. Nghệ danh Artist feat (chỉ người feat đang hoạt động)

- Keyword rỗng / không gửi → trả kết quả giống hệt UC20.
- Keyword được trim trước khi tìm.
- 1 bài khớp nhiều điều kiện chỉ xuất hiện **1 lần** (truy vấn dùng `DISTINCT`).

Câu truy vấn (rút gọn, JPQL):
```sql
SELECT DISTINCT s FROM Song s LEFT JOIN s.featuredArtists f
WHERE <đã phát hành>
  AND (   LOWER(s.title)             LIKE %keyword%
       OR LOWER(s.artist.artistName) LIKE %keyword%
       OR (LOWER(f.artistName)       LIKE %keyword% AND f đang hoạt động))
```
Dùng `LEFT JOIN` (không phải `JOIN`) để bài **không có feat** vẫn được tìm theo tên.

### 11.3. UC22 — Chi tiết bài

**API:** `GET /api/songs/{songId}`

Bài không tồn tại / chưa duyệt / chưa tới lịch / bị ẩn / bị gỡ / đã xóa / Artist bị khóa → **đều** 400 `Song does not exist`. Cố ý gộp chung để không lộ thông tin (mục 9.0b).

### 11.4. UC23 — Nghe nhạc

**Không có API riêng.** Frontend ghép địa chỉ server + `audioUrl` rồi đưa vào thẻ audio:
```html
<audio controls src="http://157.66.100.87:8080/uploads/songs/audio/3f2a9c1e-....mp3"></audio>
```

- File được phục vụ trực tiếp bởi `WebMvcConfig` (`/uploads/**` → thư mục `app.upload-dir`).
- `/uploads/**` **không cần token** (trình duyệt không gắn được header Authorization vào thẻ `<audio>`).
- Hỗ trợ **tua nhạc**: trình duyệt gửi header `Range`, server trả **206 Partial Content** với đúng đoạn được yêu cầu, không phải tải lại cả file.
- Có cache 1 giờ (`setCachePeriod(3600)`).
- `/uploads/**` không bị tính vào rate limit.

> Đánh đổi: ai có đường dẫn file đều tải được file. Vì tên file là UUID ngẫu nhiên nên không đoán được, chấp nhận được với phạm vi dự án.

### 11.5. UC24 — Ghi nhận lượt nghe

**API:** `POST /api/songs/{songId}/play` (không body)
**Kết quả:** luôn `200 { "message": "Play recorded" }` nếu bài đã phát hành.

Frontend gọi **1 lần khi người dùng bắt đầu phát** bài.

Các bước (`SongService.play`):
```
1. Bài phải đang phát hành → không thì 400 "Song does not exist"

2. Hỏi Redis: SET "play:{songId}:{email}" = "1"  CHỈ KHI CHƯA CÓ, sống 30 giây
       Đặt được (chưa có)  → lượt này ĐƯỢC TÍNH
       Đã có sẵn           → người này vừa nghe bài này < 30 giây trước → BỎ QUA
       Redis lỗi           → vẫn TÍNH (fail-open, xem bên dưới)

3. Nếu được tính:
     a. UPDATE songs SET play_count = play_count + 1 WHERE id = songId
     b. Ghi 1 document vào MongoDB listening_history (mục 12)

4. Trả 200 "Play recorded" (CẢ KHI bị bỏ qua)
```

**Giải thích từng quyết định:**

| Quyết định | Lý do |
|---|---|
| Key Redis gồm cả `songId` và `email` | Chặn riêng từng người, từng bài. Lan nghe bài A không ảnh hưởng Lan nghe bài B, không ảnh hưởng Minh nghe bài A |
| `SET ... NX` (chỉ đặt khi chưa có) | Kiểm tra + đặt trong **1 lệnh nguyên tử**. Nếu tách làm 2 bước "kiểm tra rồi đặt", 2 request đến cùng lúc đều thấy "chưa có" → tính 2 lượt |
| Hết hạn 30 giây | Đủ để chặn spam bấm liên tục, vẫn cho phép nghe lại bài yêu thích |
| `UPDATE ... play_count + 1` thay vì đọc lên, cộng 1, lưu lại | Đọc-cộng-lưu: 2 người nghe cùng lúc, cả 2 đọc được 100, cùng lưu 101 → **mất 1 lượt**. Câu UPDATE cộng thẳng trong database thì không bao giờ mất |
| Luôn trả 200 kể cả bị bỏ qua | Không cho kẻ gian biết "đang bị chặn", không dò được quy luật 30 giây |
| Redis lỗi vẫn tính | Ưu tiên chức năng chính chạy được. Mất chống spam vài phút ít hại hơn là không ai nghe nhạc được |
| Ghi lịch sử chỉ khi lượt được tính | Bấm play 10 lần trong 30 giây không tạo 10 dòng lịch sử |

### 11.6. UC25 — Trang nghệ sĩ

**API:** `GET /api/songs/artists/{artistId}?page=0&size=20`

Kết quả (`ArtistSongsResponse`):
```json
{
    "artistId": 5,
    "artistName": "Mây Chiều",
    "bio": "Ballad nhẹ nhàng cho buổi chiều.",
    "avatarUrl": "/uploads/avatars/7c2e.jpg",
    "songs": { "content": [ ...SongResponse... ], "page": 0, "size": 20, "totalElements": 12, "totalPages": 1 }
}
```

- `artistId` không phải ARTIST hoặc bị khóa → 400 `Artist does not exist`.
- `songs` gồm bài nghệ sĩ là **Artist chính HOẶC được feat**, chỉ bài đã phát hành, mới phát hành lên đầu.
- Gộp 2 phần trong 1 API để frontend chỉ gọi 1 lần là vẽ được cả trang. Chuyển trang bài hát thì gọi lại với `page` khác (phần thông tin nghệ sĩ trả lại giống hệt).
- Đường dẫn `/api/songs/artists/...` (có chữ **s**) là của người nghe, khác `/api/artist/...` (không **s**) là API quản lý của Artist.

---

## 12. LỊCH SỬ NGHE GẦN ĐÂY (MONGODB)

### 12.1. Chức năng

**API:** `GET /api/users/me/history` (không tham số, mọi role, phải đăng nhập)

Trả **mảng tối đa 10 bài khác nhau** người dùng nghe gần nhất, mới nhất ở đầu:
```json
[
    { "song": { ...SongResponse... }, "playedAt": "2026-10-15T17:40:12" },
    { "song": { ...SongResponse... }, "playedAt": "2026-10-15T17:35:03" }
]
```

`me` trong đường dẫn = người đang đăng nhập (lấy từ token). URL không có userId → **không ai xem được lịch sử người khác**.

### 12.2. Vì sao dùng MongoDB thay vì MySQL?

| Đặc điểm của lịch sử nghe | Phù hợp MongoDB vì |
|---|---|
| Rất nhiều bản ghi (mỗi lượt nghe 1 dòng) | Ghi thêm nhanh, dễ mở rộng |
| Chỉ ghi thêm, không bao giờ sửa | Không cần ràng buộc chặt chẽ của MySQL |
| Cấu trúc đơn giản (3 trường) | Document gọn |
| Cần tự xóa dữ liệu cũ | **TTL index**: MongoDB tự xóa, không phải viết code dọn dẹp |
| Không phải dữ liệu sống còn | Mất vài dòng lịch sử không ảnh hưởng nghiệp vụ chính |

Đồng thời là cơ hội để nhóm học thêm 1 loại database phổ biến.

### 12.3. Ghi lịch sử (`ListeningHistoryService.record`)

Được gọi tự động từ `SongService.play` khi lượt nghe **được tính**. Frontend **không** cần gọi API nào khác.

```
try {
    lưu { userId, songId, playedAt = bây giờ } vào MongoDB
} catch (bất kỳ lỗi nào) {
    chỉ ghi log WARN "Could not save listening history (user X, song Y): ..."
    KHÔNG ném lỗi ra ngoài
}
```

**Fail-open:** MongoDB / Atlas có sự cố → nghe nhạc **vẫn bình thường**, lượt nghe vẫn được tính, chỉ mất dòng lịch sử đó. Lịch sử là chức năng phụ, không được phép làm hỏng chức năng chính.

Hệ quả cho người vận hành: lỗi MongoDB **không hiện ra ở Postman** (vẫn 200), chỉ thấy trong log server (`app.log`).

### 12.4. Đọc lịch sử (`ListeningHistoryService.getRecentSongs`)

Đây là thuật toán hay nhất module, đọc kỹ ví dụ.

```
Bước 1. MongoDB: lấy 100 lượt nghe mới nhất của user (findTop100ByUserIdOrderByPlayedAtDesc)
Bước 2. Lọc trùng, giữ thứ tự: duyệt từ mới → cũ, mỗi songId chỉ giữ LẦN ĐẦU gặp
        (LinkedHashMap + putIfAbsent → giữ được thứ tự chèn)
Bước 3. MySQL: lấy thông tin các bài đó, CHỈ những bài còn đang phát hành
        (findPublishedByIds — 1 câu truy vấn cho tất cả, không gọi từng bài)
Bước 4. Ghép: duyệt danh sách ở bước 2 theo thứ tự,
        bài nào có ở bước 3 thì đưa vào kết quả, đủ 10 thì dừng
```

**Ví dụ.** Lan nghe theo thứ tự thời gian: A (9:00), B (9:05), A (9:10), C (9:15), D (9:20). Bài C vừa bị Artist ẩn.

```
Bước 1 — MongoDB trả (mới → cũ):
    D 9:20 | C 9:15 | A 9:10 | B 9:05 | A 9:00

Bước 2 — lọc trùng, giữ lần đầu gặp:
    D → 9:20
    C → 9:15
    A → 9:10      (A 9:00 bị bỏ vì A đã có)
    B → 9:05

Bước 3 — MySQL: bài nào trong {D, C, A, B} còn phát hành?
    → D, A, B   (C đang ẩn nên không có)

Bước 4 — ghép theo thứ tự bước 2, bỏ bài không có ở bước 3:
    [ D 9:20, A 9:10, B 9:05 ]
```

Khi Artist hiện lại C, lần gọi sau kết quả tự thành `[D, C, A, B]` — **không mất dữ liệu**, vì MongoDB vẫn giữ nguyên lượt nghe, chỉ là lúc đọc bị lọc.

**Vì sao lấy 100 mà không lấy 10?** Vì sau khi lọc trùng và lọc bài không còn phát hành, 10 lượt thô có thể chỉ còn 2–3 bài. Lấy dư 100 để gần như chắc chắn còn đủ 10 bài khác nhau.

**Vì sao lọc ở lúc đọc thay vì xóa khỏi MongoDB khi bài bị ẩn?** Vì ẩn / gỡ / khóa đều có thể **đảo ngược**. Lọc lúc đọc → hiện lại là có lại ngay, không phải đồng bộ 2 database.

**Đọc lỗi thì sao?** Khác với lúc ghi: MongoDB lỗi khi **đọc** → trả **500**, vì nhiệm vụ chính của API này là đọc lịch sử, đọc không được thì phải báo.

### 12.5. Ghép dữ liệu 2 database (application-side join)

MySQL và MongoDB là 2 hệ thống khác nhau, **không JOIN được bằng SQL**. Nên server tự ghép trong code Java (bước 3–4 ở trên). MongoDB chỉ lưu con số `songId`; thông tin đầy đủ (tên, ảnh, lượt nghe mới nhất...) luôn lấy từ MySQL → luôn là dữ liệu mới nhất.

### 12.6. Ghi nhớ nhanh cho frontend

1. Mỗi bài chỉ xuất hiện 1 lần, `playedAt` là lần nghe gần nhất.
2. Nghe lại bài cũ → bài đó nhảy lên đầu (sau ít nhất 30 giây kể từ lần nghe trước của chính bài đó).
3. Tối đa 10 bài, có thể ít hơn.
4. Chưa nghe gì → `[]`.
5. Muốn cập nhật sau khi phát bài → gọi lại API.
6. Lịch sử tự xóa sau 90 ngày.

---

## 13. LƯU TRỮ FILE NHẠC VÀ ẢNH

Toàn bộ nằm trong `FileStorageService`.

### 13.1. File nhạc (`saveAudio`)

| Kiểm tra | Cách làm | Lỗi |
|---|---|---|
| Có file | `file == null` hoặc `isEmpty()` | `Audio file is required` |
| Định dạng | Lấy **đuôi file** (viết thường): chỉ `.mp3` hoặc `.wav` | `Only MP3 or WAV files are allowed` |
| Dung lượng | MP3 ≤ **20MB**, WAV ≤ **100MB** (WAV không nén nên to hơn nhiều) | `MP3 file must not exceed 20MB` / `WAV file must not exceed 100MB` |
| File thật | Đọc file để đo thời lượng (`getAudioDuration`); đọc không được hoặc ≤ 0 giây | `Invalid or corrupted audio file` |

Đo thời lượng:
- MP3: thư viện `mp3agic` (`Mp3File.getLengthInSeconds()`).
- WAV: Java có sẵn `AudioSystem` (số khung hình ÷ số khung mỗi giây).

`durationSeconds` do **server đo**, frontend **không gửi** → không ai khai gian thời lượng được.

### 13.2. File ảnh (`saveImage`)

| Kiểm tra | Cách làm | Lỗi |
|---|---|---|
| Có file | | `Image file is required` |
| Dung lượng | ≤ **2MB** | `Image must not exceed 2MB` |
| Định dạng khai báo | `Content-Type` là `image/jpeg`, `image/png` hoặc `image/webp` | `Only JPG, PNG or WEBP images are allowed` |
| Ảnh thật | Đọc **12 byte đầu** ("chữ ký file" / magic bytes) | `Invalid or corrupted image file` |

Chữ ký file:
| Định dạng | Byte đầu |
|---|---|
| JPG | `FF D8 FF` |
| PNG | `89 50 4E 47 0D 0A 1A 0A` |
| WEBP | `RIFF....WEBP` |

**Vì sao phải kiểm tra chữ ký?** `Content-Type` và đuôi file là do **trình duyệt / người dùng khai báo**, đổi được dễ dàng. Kẻ xấu đổi `virus.html` thành `anh.jpg` → nếu chỉ tin đuôi file, server sẽ lưu và phát tán file độc. Chữ ký nằm trong nội dung file, khó giả hơn nhiều.

Đuôi file lưu trên server được **suy ra từ Content-Type**, không lấy từ tên gốc.

### 13.3. Lưu file (`saveFile`)

```
Tên file   = UUID ngẫu nhiên + đuôi           VD 3f2a9c1e-7b4d-4e8a-9c21-5d6f7e8a9b0c.mp3
Thư mục    = <app.upload-dir>/songs/audio     (tự tạo nếu chưa có)
Trả về URL = /uploads/songs/audio/3f2a9c1e-....mp3   ← lưu vào database
```

Vì sao dùng UUID thay vì tên gốc?
- 2 người cùng upload `bai-hat.mp3` → không ghi đè nhau.
- Tên gốc có thể chứa ký tự lạ (`../../`, dấu, khoảng trắng) gây lỗi hoặc lỗ hổng.
- Không đoán được đường dẫn file của người khác.

### 13.4. Xóa file (`delete`)

- Chỉ xóa đường dẫn bắt đầu bằng `/uploads/` (không xóa nhầm file hệ thống).
- `null` → bỏ qua (bài không có ảnh bìa).
- File không tồn tại hoặc lỗi khi xóa → **bỏ qua im lặng**. Xóa file là việc dọn dẹp, không được làm thất bại thao tác chính.

### 13.5. Giới hạn upload tổng của server

Trong `application.properties`:
```properties
spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=110MB
```
Vượt mức này, Spring chặn **trước khi** vào code → 413 `File is too large`. Mức 100MB khớp giới hạn WAV; giới hạn chi tiết từng loại (MP3 20MB, ảnh 2MB) do `FileStorageService` kiểm tra sau.

---

## 14. BẢO MẬT VÀ PHÂN QUYỀN

### 14.1. Ma trận quyền

| API | Chưa đăng nhập | USER | ARTIST | ADMIN |
|---|---|---|---|---|
| `/api/songs/**` (nghe nhạc) | 401 | ✔ | ✔ | ✔ |
| `/api/users/me/history` | 401 | ✔ | ✔ | ✔ |
| `/api/artist/**` | 401 | 403 | ✔ | 403 |
| `/api/admin/songs/**` | 401 | 403 | 403 | ✔ |
| `/uploads/**` (file) | ✔ | ✔ | ✔ | ✔ |

ADMIN **không** đăng bài được. Muốn đăng bài phải dùng tài khoản ARTIST riêng.

### 14.2. Các lớp bảo vệ

| Lớp | Chặn cái gì | Ở đâu |
|---|---|---|
| Token JWT | Người lạ không đăng nhập | `JwtAuthenticationFilter` |
| Phiên đơn (Redis) | Token cũ sau khi đăng nhập nơi khác, sau khi đăng xuất | `SessionService` |
| Tài khoản bị khóa | Tài khoản bị Admin khóa | `JwtAuthenticationFilter` (`isEnabled`) |
| Rate limit | Spam request: **100 request / 60 giây / tài khoản** (API đăng nhập: 10 / 60 giây / IP) | `RateLimitFilter` |
| Role | Sai vai trò | `@PreAuthorize` trên Controller |
| Quyền sở hữu | Artist đụng vào bài người khác | `findByIdAndArtistId` trong `ArtistSongService` |
| Ẩn thông tin | Dò bài chưa công khai | Mọi lỗi "không thấy bài" đều là `Song does not exist` |
| DTO tách biệt | Lộ dữ liệu nội bộ | Người nghe nhận `SongResponse` (không có status, lý do...) |
| Id lấy từ token | Giả mạo người khác | Không API nào nhận `artistId` / `userId` của người gọi qua body hay URL |
| Kiểm tra file | Upload file độc | Đuôi, dung lượng, chữ ký, đọc thử file |
| Tên file UUID | Ghi đè, đoán đường dẫn, path traversal | `FileStorageService.saveFile` |
| Truy vấn tham số hóa | SQL injection | Spring Data JPA dùng PreparedStatement, giá trị người dùng không bao giờ ghép thẳng vào câu SQL |

### 14.3. Ví dụ tấn công và cách hệ thống chặn

| Kẻ xấu thử | Kết quả |
|---|---|
| Artist B gọi `DELETE /api/artist/songs/15` (bài của A) | 400 `Song does not exist`. Bài A không sao |
| USER gọi `PUT /api/admin/songs/15/approve` | 403 |
| Sửa role trong token thành ADMIN | Chữ ký token không khớp → 401 |
| Bấm play 1000 lần để tăng lượt nghe | Chỉ được tính 1 lượt mỗi 30 giây; sau 100 request/phút bị 429 |
| Đổi `virus.exe` thành `song.mp3` để upload | Không đọc được như file nhạc → 400 |
| Tìm kiếm `' OR 1=1 --` | Được coi là chuỗi bình thường, chỉ tìm bài có chứa đúng chuỗi đó |
| Thử id liên tục `/api/songs/1`, `/2`, `/3`... để tìm bài chờ duyệt | Mọi bài không công khai đều trả cùng 1 message |

---

## 15. XỬ LÝ ĐỒNG THỜI (NHIỀU NGƯỜI THAO TÁC CÙNG LÚC)

Server phục vụ nhiều người song song. 2 request có thể chạm vào **cùng 1 bài hát** cùng lúc.

### 15.1. Optimistic locking — cột `version`

Entity `Song` có:
```java
@Version
private long version;
```

Cách hoạt động:
```
Bài 15 đang có version = 3.

Admin đọc bài 15 (version 3) ──┐        ┌── Artist đọc bài 15 (version 3)
                               │        │
Admin bấm Duyệt:               │        │   Artist bấm Xóa:
  UPDATE songs SET status='APPROVED', version=4
  WHERE id=15 AND version=3   → 1 dòng được sửa ✔  (version giờ là 4)
                                        │
                                        UPDATE songs SET status='DELETED', version=4
                                        WHERE id=15 AND version=3   → 0 dòng (version đã là 4) ✘
                                        → Hibernate ném ObjectOptimisticLockingFailureException
```
Người đến sau **không ghi đè mù quáng** lên thay đổi của người đến trước. Họ nhận lỗi và phải tải lại dữ liệu mới. Có test tự động `SongOptimisticLockTest` kiểm tra cơ chế này.

Mong muốn: lỗi này trả về **409 Conflict** để frontend báo "Bài vừa được người khác thay đổi, vui lòng tải lại". (Xem mục 21 — cần có handler tương ứng trong `GlobalExceptionHandler`.)

### 15.2. Lượt nghe không dùng version

Nếu lượt nghe cũng đi qua `version`, thì cứ có người nghe là `version` tăng → Artist đang sửa tên sẽ liên tục bị lỗi xung đột vô lý. Vì vậy:
- `play_count` khai báo `updatable = false` → các lần `save()` thông thường **không đụng** tới nó.
- Tăng lượt nghe bằng câu `UPDATE ... SET play_count = play_count + 1` riêng → nguyên tử, không lỗi khi nhiều người nghe cùng lúc, không làm tăng `version`.

### 15.3. Tóm tắt

| Tình huống | Cơ chế | Kết quả |
|---|---|---|
| 1000 người cùng nghe 1 bài | `UPDATE play_count + 1` trong database | Đủ 1000 lượt, không mất |
| 1 người bấm play 2 lần cùng lúc | Redis `SET NX` nguyên tử | Chỉ 1 lượt |
| Admin duyệt đúng lúc Artist xóa | `@Version` | 1 người thành công, người kia báo lỗi |
| Artist sửa tên đúng lúc có người nghe | `play_count` `updatable = false` | Cả 2 thành công, lượt nghe không bị ghi đè |

---

## 16. THỜI GIAN VÀ MÚI GIỜ

- Code dùng `LocalDateTime.now()` = **giờ của máy chủ**, không kèm múi giờ.
- Vì vậy server **bắt buộc** chạy múi giờ `Asia/Ho_Chi_Minh` (VPS đã cài: `timedatectl set-timezone Asia/Ho_Chi_Minh`).
- Nếu server chạy UTC: bài hẹn 20:00 sẽ phát hành lúc **03:00 sáng hôm sau** giờ Việt Nam (lệch 7 tiếng), `playedAt` cũng lệch.
- Định dạng trong JSON: `2026-12-24T20:00:00` (năm-tháng-ngày, chữ `T`, giờ:phút:giây). **Không** có `Z`, **không** có `+07:00`. Có thể có phần lẻ giây, VD `2026-10-12T00:26:13.14861` (bài duyệt không hẹn giờ, lấy đúng thời điểm duyệt).
- Frontend gửi `releaseAt` cũng theo **giờ Việt Nam**, đúng định dạng trên.

Các mốc thời gian tự động:

| Trường | Đặt khi nào |
|---|---|
| `createdAt` | Lần lưu đầu tiên (`@PrePersist`), không bao giờ đổi |
| `updatedAt` | Mỗi lần sửa (`@PreUpdate`) |
| `releaseAt` | Artist đặt; hoặc tự gán "bây giờ" khi duyệt bài không có lịch, khi bỏ lịch bài đã duyệt |
| `playedAt` | Lúc lượt nghe được tính |

---

## 17. PHÂN TRANG

Các API danh sách lớn (`/api/songs`, `/api/songs/search`, `/api/songs/artists/{id}`, `/api/admin/songs`) trả về `PageResponse`:

```json
{
    "content": [ ... các phần tử của trang hiện tại ... ],
    "page": 0,
    "size": 20,
    "totalElements": 135,
    "totalPages": 7
}
```

| Trường | Ý nghĩa |
|---|---|
| `content` | Dữ liệu của trang này |
| `page` | Trang hiện tại, **bắt đầu từ 0** |
| `size` | Số phần tử mỗi trang (sau khi server đã điều chỉnh) |
| `totalElements` | Tổng số phần tử của tất cả các trang |
| `totalPages` | Tổng số trang |

Server tự điều chỉnh tham số để tránh lạm dụng:
- `page < 0` → 0
- `size < 1` → 1
- `size > 50` → 50 (chặn kiểu xin `size=1000000` làm sập server)

Frontend:
- Trang cuối: `page == totalPages - 1`.
- Hiển thị cho người dùng thì cộng 1 (`page = 0` → "Trang 1").
- `PageResponse` là class riêng của dự án (không trả thẳng `Page` của Spring) để JSON gọn, ổn định, không lộ chi tiết nội bộ của Spring.

`/api/artist/songs` và `/api/users/me/history` **không phân trang** (trả mảng), vì số lượng nhỏ.

---

## 18. XỬ LÝ LỖI

### 18.1. Nguyên tắc

- Service phát hiện vi phạm nghiệp vụ → `throw new IllegalArgumentException("message")`.
- `GlobalExceptionHandler` (`@RestControllerAdvice`) bắt **mọi** lỗi ở 1 chỗ và trả JSON thống nhất `{ "message": "..." }`.
- Controller và Service không phải tự `try/catch` để trả lỗi.
- Message viết **tiếng Anh, ngắn gọn, cố định** để frontend có thể so sánh và dịch sang tiếng Việt.

### 18.2. Bảng loại lỗi → mã HTTP

| Loại lỗi (exception) | Mã | Message | Khi nào |
|---|---|---|---|
| `IllegalArgumentException` | 400 | Theo nội dung lỗi | Vi phạm nghiệp vụ (phần lớn lỗi trong module) |
| `MethodArgumentNotValidException` | 400 | Message trong annotation DTO, hoặc `Invalid value for <trường>` | Sai validation (`@NotBlank`, `@Size`...) hoặc sai kiểu dữ liệu ở form-data |
| `HttpMessageNotReadableException` | 400 | `Invalid request data` | JSON sai cú pháp, sai kiểu, thiếu body |
| `MissingServletRequestPartException` | 400 | `File is required` | Thiếu hẳn key file trong form-data |
| `MethodArgumentTypeMismatchException` | 400 | `Invalid parameter: <tên>` | `/api/songs/abc`, `?status=pending`, `?page=abc` |
| Chưa đăng nhập / token sai | 401 | `Bạn chưa đăng nhập hoặc token không hợp lệ` hoặc thông báo hết phiên | |
| `AccessDeniedException` | 403 | Không đủ quyền | Sai role |
| `DataIntegrityViolationException` | 409 | `Data already exists` | Vi phạm ràng buộc database |
| `MaxUploadSizeExceededException` | 413 | `File is too large` | Vượt giới hạn upload tổng |
| Rate limit | 429 | `Too many requests. Please try again in X seconds` | Kèm header `Retry-After` |
| Lỗi khác của Spring (405, 415...) | Theo Spring | Theo Spring | Sai method, sai Content-Type... |
| Mọi lỗi không lường trước | 500 | `Internal server error` | **Bug** — ghi log đầy đủ để sửa |

### 18.3. Hai lớp kiểm tra dữ liệu

```
Lớp 1 — DTO (@Valid ở Controller): kiểm tra HÌNH THỨC, không cần database
        có gửi không? dài quá không? quá 5 người không? đúng định dạng ngày không?

Lớp 2 — Service: kiểm tra NGHIỆP VỤ, cần đọc database / file / giờ hiện tại
        file có phải nhạc thật? nghệ sĩ feat có tồn tại? lịch có ở tương lai?
        bài có đang PENDING? bài có phải của mình?
```

---

## 19. HƯỚNG DẪN RIÊNG CHO FRONTEND

### 19.1. Đường dẫn file

`audioUrl`, `coverUrl`, `avatarUrl` là **đường dẫn tương đối**. Luôn ghép với địa chỉ server:
```js
const BASE_URL = "http://157.66.100.87:8080";
const src = BASE_URL + song.audioUrl;
```
`coverUrl` có thể `null` → hiển thị ảnh mặc định.

### 19.2. Đăng bài (form-data)

```js
const form = new FormData();
form.append("title", title);
form.append("audio", audioInput.files[0]);
if (coverInput.files[0]) form.append("cover", coverInput.files[0]);
featIds.forEach(id => form.append("featuredArtistIds", id));   // lặp key cho nhiều người
if (releaseAt) form.append("releaseAt", "2026-12-24T20:00:00"); // giờ Việt Nam

await axios.post(`${BASE_URL}/api/artist/songs`, form, {
    headers: { Authorization: `Bearer ${token}` }
    // KHÔNG tự đặt Content-Type, axios/trình duyệt tự thêm boundary
});
```

Từ thẻ `<input type="datetime-local">` (giá trị `2026-12-24T20:00`), thêm `:00` vào cuối cho đủ giây.

Kiểm tra sớm ở frontend để trải nghiệm tốt hơn (server vẫn kiểm tra lại): đuôi file, dung lượng (MP3 ≤ 20MB, WAV ≤ 100MB, ảnh ≤ 2MB), title ≤ 150 ký tự, tối đa 5 feat.

### 19.3. Ô chọn nghệ sĩ feat

1. Người dùng gõ → debounce khoảng 300ms → gọi `GET /api/artist/search-artists?keyword=...`.
2. Hiển thị danh sách `artistName`, lưu lại `id` khi chọn.
3. Gửi các `id` trong `featuredArtistIds`.

### 19.4. Sửa bài

- Luôn gửi **đủ** `featuredArtistIds` hiện tại (kể cả không đổi), vì danh sách gửi lên thay thế hoàn toàn danh sách cũ.
- Sau khi sửa bài REJECTED, hiển thị thông báo "Bài đã được gửi duyệt lại".

### 19.5. Nhãn trạng thái (trang "Bài hát của tôi" và trang Admin)

```js
function songLabel(s) {
    if (s.status === "PENDING")  return "Chờ duyệt";
    if (s.status === "REJECTED") return "Bị từ chối: " + s.rejectReason;
    if (s.status === "HIDDEN")   return "Đã ẩn";
    if (s.status === "APPROVED") return s.released ? "Đã phát hành" : "Đã lên lịch";
}
```

Nút nào hiện ở trạng thái nào (dựa trên bảng mục 8.3):

| status / released | Sửa | Đổi ảnh | Đổi lịch | Ẩn | Hiện | Xóa |
|---|---|---|---|---|---|---|
| PENDING | ✔ | ✔ | ✔ | | | ✔ |
| APPROVED, chưa phát hành | ✔ | ✔ | ✔ | ✔ | | ✔ |
| APPROVED, đã phát hành | ✔ | ✔ | | ✔ | | ✔ |
| REJECTED | ✔ | ✔ | ✔ | | | ✔ |
| HIDDEN | ✔ | ✔ | | | ✔ | ✔ |

(HIDDEN mà chưa từng tới lịch thì vẫn đổi lịch được, nhưng hiếm gặp — có thể ẩn nút cho đơn giản.)

### 19.6. Trình phát nhạc

```js
audio.src = BASE_URL + song.audioUrl;
audio.play();
// Gọi 1 lần khi BẮT ĐẦU phát, không gọi khi tạm dừng / tua
axios.post(`${BASE_URL}/api/songs/${song.id}/play`, null, { headers: { Authorization: `Bearer ${token}` } });
```
- Không cần quan tâm chống spam, server tự lo.
- Lỗi khi gọi `/play` không nên làm dừng nhạc — chỉ cần bỏ qua.
- Sau khi phát, muốn cập nhật mục "Nghe gần đây" thì gọi lại `/api/users/me/history`.

### 19.7. Xử lý lỗi chung

```js
switch (error.response?.status) {
    case 400: showToast(translate(error.response.data.message)); break;
    case 401: logout(); redirectToLogin(); break;             // gồm cả trường hợp bị đăng nhập nơi khác
    case 403: showToast("Bạn không có quyền thực hiện thao tác này"); break;
    case 409: showToast("Dữ liệu vừa thay đổi, vui lòng tải lại"); reload(); break;
    case 413: showToast("File quá lớn"); break;
    case 429: showToast(`Thao tác quá nhanh, thử lại sau ${error.response.headers["retry-after"]} giây`); break;
    default:  showToast("Có lỗi xảy ra, vui lòng thử lại");
}
```

### 19.8. Hiển thị thời gian

- `releaseAt`, `playedAt`... là giờ Việt Nam. Dùng thẳng, **không** cộng/trừ 7 tiếng.
- "15 phút trước": lấy giờ hiện tại của máy trừ `playedAt`.
- `durationSeconds = 215` → hiển thị `3:35`.

---

## 20. CẤU HÌNH VÀ MÔI TRƯỜNG

### 20.1. Hai môi trường

| | Local (máy cá nhân) | Production (VPS) |
|---|---|---|
| File cấu hình | `application.properties` | `application-prod.properties` (chạy với `-Dspring.profiles.active=prod`) |
| MySQL | `localhost`, user root | `localhost` trên VPS, user `myspotify` |
| `ddl-auto` | `update` (tự sửa bảng) | `validate` (chỉ kiểm tra, **không** tự sửa) |
| MongoDB database | `myspotify_db` | `myspotify_prod` |
| Thư mục upload | `uploads` | `/home/myspotify/uploads` |
| Múi giờ | Theo máy (Việt Nam) | Phải đặt `Asia/Ho_Chi_Minh` |

⚠️ Cả 2 file cấu hình **không đưa lên GitHub** (có trong `.gitignore`) vì chứa mật khẩu database, chuỗi kết nối MongoDB, khóa bí mật JWT. Thành viên mới xin file mẫu từ trưởng nhóm.

### 20.2. Các khóa cấu hình liên quan module Song

```properties
# Thư mục lưu file
app.upload-dir=uploads

# Giới hạn upload tổng
spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=110MB

# MongoDB (Spring Boot 4 dùng spring.mongodb.uri, KHÔNG phải spring.data.mongodb.uri)
spring.mongodb.uri=mongodb+srv://<user>:<password>@<cluster>/<database>?retryWrites=true&w=majority&appName=<app>
spring.data.mongodb.auto-index-creation=true

# Rate limit
app.rate-limit.window-seconds=60
app.rate-limit.user-requests=100
app.rate-limit.auth-requests=10
```

Chuỗi kết nối MongoDB phải có **tên database** giữa `.mongodb.net/` và `?`. Thiếu → lỗi khởi động `Database name must not be empty`.

### 20.3. Chạy test

```bash
./mvnw clean test        # Windows: .\mvnw clean test
```
Test cần kết nối được MySQL, Redis và MongoDB Atlas (IP máy phải nằm trong Network Access của Atlas).

### 20.4. Deploy lên VPS

```
Máy local: git push origin main
VPS:       bash /home/myspotify/deploy.sh   (kéo code → build Maven → tắt bản cũ → chạy bản mới)
Xem log:   tail -f /home/myspotify/app.log
```
Khi thay đổi enum `SongStatus` hoặc cấu trúc bảng: vì VPS dùng `ddl-auto=validate`, phải tự chạy `ALTER TABLE` trên VPS **trước** khi deploy.

---

## 21. GIỚI HẠN ĐÃ BIẾT VÀ HƯỚNG PHÁT TRIỂN

### 21.1. Giới hạn hiện tại (chấp nhận được trong phạm vi dự án)

| # | Giới hạn | Ảnh hưởng | Hướng cải thiện |
|---|---|---|---|
| 1 | Tìm kiếm không xử lý ký tự `%` và `_` (ký tự đặc biệt của LIKE) | Tìm `%` ra mọi bài | Escape 2 ký tự này trước khi truy vấn |
| 2 | Tìm kiếm bằng `LIKE '%...%'` không dùng được index | Chậm khi có hàng trăm nghìn bài | Full-text index MySQL hoặc Elasticsearch |
| 3 | Sửa tên / đổi ảnh bài APPROVED không cần duyệt lại | Artist có thể đổi ảnh thành nội dung xấu sau khi được duyệt | Chuyển về PENDING, hoặc tách "bản nháp chờ duyệt" |
| 4 | Lấy danh sách có thể phát sinh nhiều truy vấn phụ (N+1) khi đọc Artist và feat của từng bài | Chậm hơn khi trang lớn | `JOIN FETCH` / `@EntityGraph` |
| 5 | `/uploads/**` công khai | Ai có link đều tải được file | URL có chữ ký + thời hạn |
| 6 | File lưu trên ổ đĩa VPS | Ổ đĩa đầy, không nhân bản được khi có nhiều server | Object storage (S3, Cloudflare R2) |
| 7 | Phân trang theo offset | Có bài mới chen vào giữa lúc chuyển trang → trùng/thiếu 1 bài | Phân trang theo con trỏ (cursor) |
| 8 | Lịch sử lấy 100 lượt thô | Người nghe 1 bài hơn 100 lần liên tiếp có thể thấy < 10 bài | Dùng aggregation `$group` của MongoDB |
| 9 | `released` trong API quản trị không xét Artist bị khóa | Admin thấy `released = true` cho bài người nghe không thấy | Thêm điều kiện vào `SongMapper` |
| 10 | Thời gian không kèm múi giờ | Phụ thuộc cấu hình múi giờ server | Dùng `OffsetDateTime` / `Instant` |

### 21.2. Việc còn lại cần kiểm tra

- [ ] Đảm bảo `GlobalExceptionHandler` có handler cho `ObjectOptimisticLockingFailureException` trả **409** (mục 15.1). Nếu chưa có, lỗi xung đột sẽ rơi vào handler chung và trả **500**.
- [ ] Cập nhật sơ đồ use case, ERD (thêm `listening_history`), sơ đồ class theo code hiện tại.
- [ ] Viết bộ test case Excel từ `TEST_SONG_POSTMAN.md`.

### 21.3. Ý tưởng mở rộng

Playlist, thích bài hát, album, bảng xếp hạng theo tuần (dựa trên `listening_history`), gợi ý bài theo lịch sử nghe, thông báo cho Artist khi bài được duyệt / từ chối, đếm lượt nghe chỉ khi nghe đủ 30 giây.

---

## 22. CÂU HỎI THƯỜNG GẶP

**H: Tôi đăng bài rồi mà tìm không thấy?**
Đ: Bài cần Admin duyệt. Kiểm tra `GET /api/artist/songs`: `PENDING` = chờ duyệt; `APPROVED` + `released: false` = chưa tới lịch.

**H: Admin duyệt rồi mà người nghe vẫn không thấy?**
Đ: Kiểm tra 3 điều kiện mục 8.4: đã tới `releaseAt` chưa? Artist có bị khóa không? Server có đúng múi giờ không?

**H: Bấm play nhiều lần mà `playCount` không tăng?**
Đ: Đúng thiết kế. 1 tài khoản + 1 bài chỉ tính 1 lượt mỗi 30 giây.

**H: Sao API trả "Song does not exist" trong khi bài rõ ràng có trong database?**
Đ: Message này dùng chung cho: không tồn tại, chưa phát hành, bị ẩn, bị gỡ, đã xóa, không phải bài của bạn. Mở `GET /api/admin/songs` bằng Admin để xem trạng thái thật.

**H: Sửa bài xong mất hết người feat?**
Đ: `featuredArtistIds` gửi lên thay thế toàn bộ danh sách cũ. Phải gửi lại đủ id muốn giữ.

**H: Gỡ bài có xóa file không?**
Đ: Không. Gỡ = REJECTED + lý do, file giữ nguyên để Artist sửa và gửi lại. Chỉ Artist **xóa** bài mới xóa file.

**H: Admin có xóa được bài không?**
Đ: Không có API xóa cho Admin. Admin dùng **gỡ** (remove). Chỉ chủ bài mới xóa được.

**H: Lịch sử nghe trống dù tôi đã nghe nhạc?**
Đ: Kiểm tra log server (`app.log`) có dòng `Could not save listening history` không. Lỗi ghi MongoDB không hiện ra ở API `/play` (luôn 200). Nguyên nhân hay gặp: IP server chưa được thêm vào Network Access của Atlas, sai mật khẩu trong `spring.mongodb.uri`.

**H: Thời gian trong MongoDB Atlas lệch 7 tiếng?**
Đ: MongoDB lưu theo UTC. Đọc qua API sẽ đúng giờ Việt Nam. Không phải lỗi.

**H: Thêm trạng thái mới vào `SongStatus` thì lưu bài bị lỗi `Data truncated`?**
Đ: Cột `status` là ENUM của MySQL. Phải chạy `ALTER TABLE` liệt kê đủ mọi giá trị (mục 7.1), trên cả local và VPS.

**H: Tại sao không gửi `artistId` khi đăng bài?**
Đ: Server lấy id từ token. Nếu nhận từ body, ai cũng có thể đăng bài dưới tên người khác.

**H: Sao ảnh bìa cũ vẫn hiện sau khi đổi?**
Đ: Trình duyệt cache 1 giờ. `coverUrl` mới là đường dẫn khác hẳn nên chỉ cần frontend dùng `coverUrl` mới trả về.

---

## 23. PHỤ LỤC

### 23.1. Bảng tổng hợp API

| # | Method | URL | Role | Body | Trả về | UC |
|---|---|---|---|---|---|---|
| 1 | GET | `/api/songs?page&size` | Đăng nhập | — | `PageResponse<SongResponse>` | UC20 |
| 2 | GET | `/api/songs/search?keyword&page&size` | Đăng nhập | — | `PageResponse<SongResponse>` | UC21 |
| 3 | GET | `/api/songs/{songId}` | Đăng nhập | — | `SongResponse` | UC22 |
| 4 | GET | `/uploads/songs/audio/{file}` | Công khai | — | File nhạc | UC23 |
| 5 | POST | `/api/songs/{songId}/play` | Đăng nhập | — | `{message}` | UC24 |
| 6 | GET | `/api/songs/artists/{artistId}?page&size` | Đăng nhập | — | `ArtistSongsResponse` | UC25 |
| 7 | POST | `/api/artist/songs` | ARTIST | form-data | `SongManageResponse` | UC26 |
| 8 | GET | `/api/artist/songs` | ARTIST | — | `SongManageResponse[]` | UC28 |
| 9 | PUT | `/api/artist/songs/{songId}` | ARTIST | JSON `title`, `featuredArtistIds` | `SongManageResponse` | UC29 |
| 10 | PUT | `/api/artist/songs/{songId}/cover` | ARTIST | form-data `cover` | `SongManageResponse` | UC30 |
| 11 | PUT | `/api/artist/songs/{songId}/release` | ARTIST | JSON `releaseAt` | `SongManageResponse` | UC31 |
| 12 | PUT | `/api/artist/songs/{songId}/hide` | ARTIST | — | `SongManageResponse` | UC32 |
| 13 | PUT | `/api/artist/songs/{songId}/show` | ARTIST | — | `SongManageResponse` | UC32 |
| 14 | DELETE | `/api/artist/songs/{songId}` | ARTIST | — | `{message}` | UC33 |
| 15 | GET | `/api/admin/songs?status&page&size` | ADMIN | — | `PageResponse<SongManageResponse>` | UC34 |
| 16 | PUT | `/api/admin/songs/{songId}/approve` | ADMIN | — | `SongManageResponse` | UC35 |
| 17 | PUT | `/api/admin/songs/{songId}/reject` | ADMIN | JSON `reason` | `SongManageResponse` | UC35 |
| 18 | PUT | `/api/admin/songs/{songId}/remove` | ADMIN | JSON `reason` | `SongManageResponse` | UC36 |
| 19 | GET | `/api/artist/search-artists?keyword` | ARTIST | — | `ArtistSummary[]` | UC37 |
| 20 | GET | `/api/users/me/history` | Đăng nhập | — | `ListeningHistoryResponse[]` | Lịch sử |

### 23.2. Bảng message lỗi nghiệp vụ

| Message | Ném ở | Nghĩa |
|---|---|---|
| `Song does not exist` | Mọi service | Không tìm thấy bài (hoặc không được phép thấy) |
| `Artist does not exist` | `SongService.getArtist` | Trang nghệ sĩ không hợp lệ |
| `User does not exist` | `ArtistSongService.getArtist` | Không tìm thấy tài khoản của chính người gọi |
| `Only active artists can manage songs` | `ArtistSongService.getArtist` | Người gọi không còn là Artist hoạt động |
| `Release time must be in the future` | `checkFutureTime` | Lịch ở quá khứ |
| `Cannot change the schedule of a released song` | `updateReleaseSchedule` | Bài đã phát hành |
| `Maximum 5 featured artists` | DTO + `loadFeaturedArtists` | Quá 5 người feat |
| `You cannot feature yourself` | `loadFeaturedArtists` | Tự feat |
| `Featured artist is not available: <id>` | `loadFeaturedArtists` | Người feat không phải Artist hoạt động |
| `Some featured artists do not exist` | `loadFeaturedArtists` | Id feat không tồn tại |
| `Only approved songs can be hidden` | `hideSong` | |
| `Only hidden songs can be shown again` | `showSong` | |
| `Only pending songs can be approved` | `approveSong` | |
| `Artist account is not active` | `approveSong` | Artist bị khóa |
| `Only pending songs can be rejected` | `rejectSong` | |
| `Only approved songs can be removed` | `removeSong` | |
| `Audio file is required` | DTO + `saveAudio` | |
| `Only MP3 or WAV files are allowed` | `saveAudio` | |
| `MP3 file must not exceed 20MB` | `saveAudio` | |
| `WAV file must not exceed 100MB` | `saveAudio` | |
| `Invalid or corrupted audio file` | `getAudioDuration` | |
| `Image file is required` | `saveImage` | |
| `Image must not exceed 2MB` | `saveImage` | |
| `Only JPG, PNG or WEBP images are allowed` | `saveImage` | |
| `Invalid or corrupted image file` | `saveImage` | |
| `Title is required` | DTO | |
| `Title must not exceed 150 characters` | DTO | |
| `Reason is required` | DTO | |
| `Reason must not exceed 255 characters` | DTO | |

### 23.3. Bản đồ "muốn sửa X thì mở file nào"

| Muốn thay đổi | Mở file |
|---|---|
| Thêm / sửa API, đường dẫn | `controller/*SongController.java` |
| Điều kiện ẩn, duyệt, xóa... | `service/ArtistSongService.java`, `service/AdminSongService.java` |
| Điều kiện "đã phát hành" | `SongRepository.PUBLISHED_CONDITION` (+ `SongMapper` nếu đổi `released`) |
| Giới hạn dung lượng, định dạng file | `service/FileStorageService.java` (+ `application.properties` nếu vượt 100MB) |
| Thời gian chống spam lượt nghe | `SongService.PLAY_COOLDOWN` |
| Số bài trong lịch sử | `ListeningHistoryService.RECENT_SONG_LIMIT` |
| Thời gian giữ lịch sử | `ListeningHistory.playedAt` (`expireAfter = "90d"`) |
| Số feat tối đa | `ArtistSongService.MAX_FEATURED_ARTISTS` **và** `@Size(max = 5)` ở 2 DTO |
| Số phần tử tối đa mỗi trang | `MAX_PAGE_SIZE` trong `SongService`, `AdminSongService` |
| Trường JSON trả về | `dto/song/*Response.java` + `mapper/SongMapper.java` |
| Thông báo lỗi chung | `exception/GlobalExceptionHandler.java` |
| Thêm trạng thái bài | `enums/SongStatus.java` + `ALTER TABLE` local và VPS |

### 23.4. Tài liệu liên quan

- `docs/TEST_SONG_POSTMAN.md` — hướng dẫn test bằng Postman (~300 test case)
- Swagger: `http://157.66.100.87:8080/swagger-ui.html`
- Comment trong từng file code — giải thích chi tiết từng dòng
