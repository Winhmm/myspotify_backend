# MODULE PLAYLIST — TÀI LIỆU NGHIỆP VỤ, API VÀ HƯỚNG DẪN FRONTEND

> Dự án: **MySpotify Backend** (Spring Boot 4, Java 25, MySQL, Redis, MongoDB)
> Package gốc: `com.winhmm.myspotify`
> Trạng thái: **đã xong và đã deploy** (merge vào `main` qua Pull Request #11).
> Đối tượng đọc: bạn bè trong nhóm (backend, frontend, tester), **không cần biết trước Spring Boot**.
> Tài liệu liên quan: `docs/SONG_MODULE.md` (bài hát, trạng thái bài, nghe nhạc), `docs/USER_MODULE.md` (tài khoản, đăng nhập).

---

## MỤC LỤC

1. [Cách đọc tài liệu này](#1-cách-đọc-tài-liệu-này)
2. [Module Playlist giải quyết bài toán gì?](#2-module-playlist-giải-quyết-bài-toán-gì)
3. [Từ điển thuật ngữ](#3-từ-điển-thuật-ngữ)
4. [Danh sách chức năng và API](#4-danh-sách-chức-năng-và-api)
5. [Kiến trúc và vị trí các file](#5-kiến-trúc-và-vị-trí-các-file)
6. [Dữ liệu: MySQL và ổ đĩa](#6-dữ-liệu-mysql-và-ổ-đĩa)
7. [Quy tắc nghiệp vụ chi tiết](#7-quy-tắc-nghiệp-vụ-chi-tiết)
8. [Chi tiết từng API](#8-chi-tiết-từng-api)
9. [Cấu trúc JSON trả về](#9-cấu-trúc-json-trả-về)
10. [Xử lý lỗi](#10-xử-lý-lỗi)
11. [Hướng dẫn cho Frontend](#11-hướng-dẫn-cho-frontend)
12. [Hướng dẫn test bằng Postman](#12-hướng-dẫn-test-bằng-postman)
13. [Bảo mật và phân quyền](#13-bảo-mật-và-phân-quyền)
14. [Xử lý đồng thời (nhiều request cùng lúc)](#14-xử-lý-đồng-thời-nhiều-request-cùng-lúc)
15. [Cấu hình, deploy và database](#15-cấu-hình-deploy-và-database)
16. [Giới hạn đã biết và hướng phát triển](#16-giới-hạn-đã-biết-và-hướng-phát-triển)
17. [Câu hỏi thường gặp](#17-câu-hỏi-thường-gặp)
18. [Phụ lục](#18-phụ-lục)

---

## 1. CÁCH ĐỌC TÀI LIỆU NÀY

Tài liệu dài, không cần đọc hết một lần. Chọn lộ trình theo vai trò:

| Bạn là | Đọc theo thứ tự | Có thể bỏ qua |
|---|---|---|
| **Người mới hoàn toàn** | 2 → 3 → 4 → 7 → 8 | 5, 14, 15 |
| **Frontend** | 2 → 4 → 7 → **11** → 9 → 8 → 10 | 5, 6, 14, 15 |
| **Tester** | 2 → 4 → 7 → 8 → 10 → **12** | 5, 15 |
| **Backend sắp sửa code** | Đọc hết, đặc biệt 5, 6, 7, 13, 14, 16 | |
| **Đưa cho AI viết code frontend** | Đưa nguyên mục 11 (có sẵn hướng dẫn cho AI ở 11.0) kèm mục 8, 9, 10 | |

Quy ước trong tài liệu:
- `chữ như thế này` là tên trong code: class, method, cột database, tên trường JSON.
- **400 `"Playlist does not exist"`** nghĩa là server trả mã HTTP 400 kèm JSON `{ "message": "Playlist does not exist" }`.
- **"Người dùng"** = bất kỳ ai đã đăng nhập: USER, ARTIST hay ADMIN đều tạo và nghe playlist được.
- **"Chủ playlist"** = người đã tạo playlist đó.
- **"Bài đang phát hành"** = bài người nghe đang nghe được (giải thích ở mục 3.2 và 7.3).

---

## 2. MODULE PLAYLIST GIẢI QUYẾT BÀI TOÁN GÌ?

### 2.1. Một câu

> Cho phép **mỗi người dùng tự gom các bài hát mình thích thành danh sách riêng**, đặt tên, chọn ảnh bìa, để **riêng tư** hoặc **công khai** cho người khác xem.

### 2.2. Câu chuyện minh họa

1. **Lan** đã có tài khoản MySpotify (role USER). Lan nghe nhiều bài và muốn gom các bài nhẹ nhàng để nghe trước khi ngủ.
2. Lan bấm **"Tạo playlist"**, đặt tên **"Chill buổi tối"**, mô tả *"Nghe trước khi ngủ"*, tìm và chọn 3 bài: **Chiều Mưa**, **Phố Đêm**, **Mưa Tháng Sáu**. Lan không tải ảnh bìa.
3. Playlist được tạo ở chế độ **riêng tư** (mặc định). Ảnh bìa tự lấy từ ảnh của bài đầu tiên (**Chiều Mưa**).
4. Hôm sau Lan nghe được bài **Gió Đầu Mùa**, bấm **"Thêm vào playlist"** → chọn "Chill buổi tối". Bài mới nằm **cuối** danh sách.
5. Lan chán bài **Phố Đêm** → bấm **"Xóa khỏi playlist"**. Bài hát gốc vẫn còn nguyên, chỉ không còn trong playlist của Lan.
6. Lan tải lên 1 ảnh bìa riêng (ảnh bầu trời đêm). Từ giờ playlist hiện ảnh này thay cho ảnh của bài đầu tiên.
7. Lan đổi playlist sang **công khai**. Bạn của Lan là **Minh** mở trang cá nhân của Lan → thấy "Chill buổi tối", bấm vào xem được danh sách bài và nghe được.
8. Minh **không** thấy playlist riêng tư "Nhạc chạy bộ" của Lan. Minh cũng **không** sửa, xóa được playlist của Lan (backend chặn 403).
9. Nghệ sĩ của bài **Chiều Mưa** tạm **ẩn bài**. Bài tự biến mất khỏi playlist của Lan (và ảnh bìa dự phòng tự đổi sang bài khác nếu Lan chưa có ảnh riêng). Khi nghệ sĩ hiện lại, bài tự xuất hiện lại **đúng vị trí cũ**, Lan không phải làm gì.
10. Lan gỡ ảnh bìa riêng → playlist quay về dùng ảnh bài đầu tiên.
11. Cuối cùng Lan **xóa** playlist. Danh sách bài của playlist bị xóa, ảnh bìa riêng bị xóa khỏi server. Các bài hát gốc không bị ảnh hưởng.

Toàn bộ module là để làm được 11 bước trên một cách **đúng quyền, đúng thứ tự, và không lộ playlist riêng tư**.

### 2.3. Ai làm được gì

| Vai | Làm được gì trong module Playlist |
|---|---|
| **Chủ playlist** (bất kỳ tài khoản đã đăng nhập) | Tạo, xem, sửa tên/mô tả/công khai, đổi/gỡ ảnh bìa, thêm/bỏ bài, xóa playlist của mình |
| **Người khác** (bất kỳ tài khoản đã đăng nhập) | Xem danh sách playlist **công khai** của 1 người, xem chi tiết playlist **công khai** |
| **Chưa đăng nhập** | Không làm được gì (401) |

ADMIN **không** có quyền đặc biệt với playlist của người khác: Admin cũng chỉ là "người khác" (xem mục 16 — hướng phát triển).

### 2.4. Sáu quy tắc cốt lõi

Nếu chỉ nhớ được 6 điều, hãy nhớ 6 điều này:

1. **Mỗi tài khoản tối đa 50 playlist, mỗi playlist tối đa 200 bài.**
2. **Tạo playlist bắt buộc có ít nhất 1 bài.** Sau đó bỏ hết bài vẫn được (playlist rỗng).
3. **Chỉ thêm được bài đang phát hành, và chỉ hiện bài đang phát hành.** Bài bị ẩn/gỡ tự biến mất, phát hành lại thì tự hiện lại.
4. **Bài xếp theo thời điểm thêm vào: thêm trước đứng trước.** Không có kéo thả đổi thứ tự.
5. **Ảnh bìa: ảnh riêng → không có thì ảnh của bài đầu tiên → vẫn không có thì `null`** (frontend hiện ảnh mặc định).
6. **Chỉ chủ playlist được sửa.** Người khác chỉ xem được playlist **công khai** của tài khoản **còn hoạt động**.

---

## 3. TỪ ĐIỂN THUẬT NGỮ

Các thuật ngữ chung (API, endpoint, JSON, token, 400/401/403, Controller, Service, Repository, Entity, DTO...) đã giải thích trong `docs/SONG_MODULE.md` mục 3. Dưới đây chỉ là thuật ngữ thêm của module Playlist.

### 3.1. Thuật ngữ kỹ thuật dùng trong tài liệu

| Thuật ngữ | Giải thích dễ hiểu |
|---|---|
| **form-data** (`multipart/form-data`) | Kiểu body dùng khi **có gửi file** (ở đây là ảnh bìa). JSON không chứa được file |
| **JSON body** | Kiểu body dạng chữ `{ "name": "..." }`, dùng khi không có file |
| **Path variable** | Giá trị nằm trong đường dẫn. `/api/playlists/3/songs/15` → `playlistId = 3`, `songId = 15` |
| **Bảng trung gian** | Bảng ở giữa để nối 2 bảng có quan hệ nhiều-nhiều. Ở đây: `playlist_songs` nối `playlists` và `songs` |
| **Khóa ngoại** (foreign key) | Cột trỏ tới `id` của bảng khác. VD `playlists.user_id` trỏ tới `users.id` |
| **UNIQUE** | Ràng buộc "không được trùng". Cặp (`playlist_id`, `song_id`) là UNIQUE → 1 bài chỉ nằm 1 lần trong 1 playlist |
| **Transaction** | Gom nhiều thao tác ghi database thành 1 khối: hoặc thành công hết, hoặc hủy hết |
| **JOIN FETCH** | Cách lấy dữ liệu 2 bảng trong **1 câu SQL** (VD playlist + người tạo), tránh phải chạy thêm nhiều câu |

### 3.2. Thuật ngữ nghiệp vụ của module

| Thuật ngữ | Giải thích |
|---|---|
| **Playlist** | Danh sách bài hát do 1 người dùng tự tạo |
| **Chủ playlist** (`owner`) | Người tạo playlist. Chỉ người này được sửa, xóa |
| **Công khai** (`isPublic = true`) | Người khác xem được |
| **Riêng tư** (`isPublic = false`) | Chỉ chủ playlist xem được. **Mặc định khi tạo** |
| **Bài đang phát hành** | Bài người nghe đang nghe được: Admin đã duyệt + đã tới giờ phát hành + nghệ sĩ chính còn hoạt động (giống hệt điều kiện ở trang nghe nhạc, xem `SONG_MODULE.md` mục 8.4) |
| **Ảnh bìa riêng** (`coverUrl` trong database) | Ảnh chủ playlist tự tải lên |
| **Ảnh bìa dự phòng** | Ảnh bìa của bài đang phát hành **đầu tiên** trong playlist, dùng khi không có ảnh riêng. **Không lưu** vào database, mỗi lần trả kết quả lại tính |
| **`hasCustomCover`** | `true` = đang dùng ảnh riêng; `false` = đang dùng ảnh dự phòng (hoặc không có ảnh) |
| **`isOwner`** | Người **đang xem** có phải chủ playlist không. Frontend dùng để hiện/ẩn các nút sửa |
| **Playlist rỗng** | Playlist không còn bài đang phát hành nào (bị bỏ hết, hoặc mọi bài đều bị ẩn) |
| **`addedAt`** | Thời điểm 1 bài được thêm vào playlist. Quyết định thứ tự bài |

---

## 4. DANH SÁCH CHỨC NĂNG VÀ API

Base URL server: `http://157.66.100.87:8080`. Mọi API đều cần header `Authorization: Bearer <token>`.

| # | Chức năng | Method | URL | Body | Ai gọi | Trả về |
|---|---|---|---|---|---|---|
| 1 | Tạo playlist | POST | `/api/playlists` | form-data | Đăng nhập | `PlaylistDetailResponse` |
| 2 | Playlist của tôi | GET | `/api/playlists/me` | — | Đăng nhập | `PlaylistSummaryResponse[]` |
| 3 | Playlist công khai của 1 người | GET | `/api/playlists/users/{userId}` | — | Đăng nhập | `PlaylistSummaryResponse[]` |
| 4 | Chi tiết 1 playlist | GET | `/api/playlists/{playlistId}` | — | Chủ, hoặc ai cũng được nếu công khai | `PlaylistDetailResponse` |
| 5 | Sửa tên, mô tả, công khai | PUT | `/api/playlists/{playlistId}` | JSON | Chủ | `PlaylistDetailResponse` |
| 6 | Đổi ảnh bìa | PUT | `/api/playlists/{playlistId}/cover` | form-data `cover` | Chủ | `PlaylistDetailResponse` |
| 7 | Gỡ ảnh bìa | DELETE | `/api/playlists/{playlistId}/cover` | — | Chủ | `PlaylistDetailResponse` |
| 8 | Thêm 1 bài | POST | `/api/playlists/{playlistId}/songs/{songId}` | — | Chủ | `PlaylistDetailResponse` |
| 9 | Bỏ 1 bài | DELETE | `/api/playlists/{playlistId}/songs/{songId}` | — | Chủ | `PlaylistDetailResponse` |
| 10 | Xóa playlist | DELETE | `/api/playlists/{playlistId}` | — | Chủ | `{ "message": "Playlist deleted" }` |

**Điểm tiện cho frontend:** mọi API sửa (5 → 9) đều **trả về luôn chi tiết playlist mới nhất**. Frontend chỉ cần thay dữ liệu đang hiển thị bằng response, **không phải gọi lại API chi tiết**.

Các API của module khác mà frontend sẽ dùng kèm:

| Mục đích | API | Tài liệu |
|---|---|---|
| Tìm bài để chọn khi tạo playlist / thêm bài | `GET /api/songs/search?keyword=...&page=0&size=20` | `SONG_MODULE.md` 11.2 |
| Lấy id của chính mình (để mở trang cá nhân của mình) | `GET /api/users/me/profile` (trường `id`), hoặc `userId` trong response đăng nhập | `USER_MODULE.md` |
| Ghi nhận lượt nghe khi phát từng bài trong playlist | `POST /api/songs/{songId}/play` | `SONG_MODULE.md` 11.5 |
| Phát nhạc | Ghép `BASE_URL + song.audioUrl` | `SONG_MODULE.md` 11.4 |

---

## 5. KIẾN TRÚC VÀ VỊ TRÍ CÁC FILE

### 5.1. Kiến trúc 3 tầng

```
 Frontend (web / Postman)
        │  HTTP request (JSON hoặc form-data, kèm token)
        ▼
┌──────────────────────────────────────────────────────────────┐
│ TẦNG BẢO MẬT: JWT → kiểm tra phiên (Redis) → rate limit         │
│ /api/playlists/** không nằm trong danh sách mở → phải đăng nhập │
└──────────────────────────────────────────────────────────────┘
        ▼
┌─────────────────────┐  Nhận request, kiểm tra hình thức dữ liệu (@Valid),
│ PlaylistController  │  lấy id người gọi từ token, gọi Service, trả JSON
└─────────────────────┘
        ▼
┌─────────────────────┐  LOGIC NGHIỆP VỤ: kiểm tra chủ playlist, quyền xem,
│  PlaylistService    │  giới hạn 50/200, bài có đang phát hành, lưu/xóa ảnh...
└─────────────────────┘
        ▼
┌─────────────────────┐  Đọc / ghi database
│ PlaylistRepository  │  bảng playlists
│ PlaylistSongRepo... │  bảng playlist_songs
│ SongRepository      │  bảng songs (kiểm tra bài đang phát hành)
│ UserRepository      │  bảng users
└─────────────────────┘
        ▼
   MySQL (playlists, playlist_songs, songs, users)      Ổ đĩa (ảnh bìa playlist)
```

Khác module Song một chút: ở đây **Service trả thẳng DTO** (không trả entity), vì để tạo DTO còn phải hỏi database thêm (đếm bài, tìm ảnh bìa dự phòng, lấy danh sách bài) — việc hỏi database thuộc về Service, Controller không nên làm.

### 5.2. Danh sách file của module

```
src/main/java/com/winhmm/myspotify/
├── controller/
│   └── PlaylistController.java          ← 10 API của playlist
├── service/
│   └── PlaylistService.java             ← Toàn bộ nghiệp vụ playlist
├── repository/
│   ├── PlaylistRepository.java          ← Truy vấn bảng playlists
│   └── PlaylistSongRepository.java      ← Truy vấn bảng playlist_songs
├── entity/
│   ├── Playlist.java                    ← Ánh xạ bảng playlists
│   └── PlaylistSong.java                ← Ánh xạ bảng playlist_songs
├── mapper/
│   └── PlaylistMapper.java              ← Playlist → DTO trả về
└── dto/playlist/
    ├── CreatePlaylistRequest.java       ← Dữ liệu tạo playlist (form-data)
    ├── UpdatePlaylistRequest.java       ← Dữ liệu sửa thông tin (JSON)
    ├── PlaylistSummaryResponse.java     ← 1 ô playlist trong danh sách (không kèm bài)
    ├── PlaylistDetailResponse.java      ← Chi tiết playlist (kèm danh sách bài)
    └── PlaylistSongResponse.java        ← 1 bài trong playlist = { song, addedAt }
```

Các file **dùng chung** với module khác mà module Playlist phụ thuộc:

| File | Vai trò với module Playlist |
|---|---|
| `entity/User.java` | Chủ playlist là 1 `User` |
| `entity/Song.java` | Bài trong playlist là 1 `Song` |
| `repository/SongRepository.java` | `PUBLISHED_CONDITION` (điều kiện "đang phát hành"), `findPublishedById`, `findPublishedByIds` |
| `mapper/SongMapper.java` | Đổi `Song` → `SongResponse` (dùng lại, không viết mới) |
| `dto/song/SongResponse.java` | Thông tin 1 bài hát trong playlist |
| `service/FileStorageService.java` | Lưu, kiểm tra, xóa ảnh bìa |
| `service/UserService.java` | Lấy user từ email trong token |
| `exception/GlobalExceptionHandler.java` | Đổi mọi lỗi thành JSON `{ "message": "..." }` |
| `config/SecurityConfig.java` | Bắt buộc đăng nhập cho `/api/playlists/**` |

### 5.3. Ai gọi ai

```
PlaylistController ──► UserService (email trong token → id người gọi)
        │
        └──► PlaylistService ──┬──► PlaylistRepository ─────► MySQL playlists
                               ├──► PlaylistSongRepository ─► MySQL playlist_songs
                               ├──► SongRepository ─────────► MySQL songs
                               ├──► UserRepository ─────────► MySQL users
                               ├──► FileStorageService ─────► Ổ đĩa (ảnh bìa)
                               └──► PlaylistMapper ──► SongMapper (đổi sang DTO)
```

### 5.4. Hành trình của 1 request

Ví dụ: Lan gửi `POST /api/playlists/3/songs/15` để thêm bài 15 vào playlist 3.

```
① Request tới server (cổng 8080)
     │
② JwtAuthenticationFilter: đọc token → email lan@gmail.com → kiểm tra phiên trong Redis
     Token sai / hết hạn / bị đăng nhập nơi khác → 401
     │
③ RateLimitFilter: quá 100 request / 60 giây → 429
     │
④ SecurityConfig: /api/playlists/** bắt buộc đăng nhập (không cần role cụ thể)
     │
⑤ PlaylistController.addSong(...)
     ├─ playlistId = 3, songId = 15 (lấy từ đường dẫn; không phải số → 400 "Invalid parameter: ...")
     ├─ id của Lan = lấy từ email trong token (KHÔNG lấy từ frontend)
     └─ Gọi playlistService.addSong(idCủaLan, 3, 15)
     │
⑥ PlaylistService.addSong  (@Transactional: cả khối thành công hoặc hủy hết)
     ├─ Tìm playlist 3 kèm chủ playlist      → không có → 400 "Playlist does not exist"
     ├─ Chủ playlist có phải Lan không?       → không    → 403
     ├─ Bài 15 có đang phát hành không?       → không    → 400 "Song does not exist"
     ├─ Bài 15 đã có trong playlist chưa?     → có rồi   → 400 "Song is already in this playlist"
     ├─ Playlist đã đủ 200 dòng chưa?         → đủ rồi   → 400 "Maximum 200 songs per playlist"
     ├─ INSERT 1 dòng playlist_songs (added_at = bây giờ)
     ├─ Cập nhật updated_at của playlist
     └─ Lấy lại danh sách bài đang phát hành → PlaylistMapper → PlaylistDetailResponse
     │
⑦ Spring đổi DTO thành JSON → trả 200
```

---

## 6. DỮ LIỆU: MYSQL VÀ Ổ ĐĨA

### 6.1. Bảng `playlists`

Mỗi dòng = 1 playlist do 1 tài khoản tạo.

| Cột | Kiểu | Bắt buộc | Ý nghĩa |
|---|---|---|---|
| `id` | BIGINT, tự tăng | Có | Mã playlist |
| `user_id` | BIGINT → `users.id` | Có | Chủ playlist (trong Java tên là `owner`) |
| `name` | VARCHAR(100) | Có | Tên playlist |
| `description` | VARCHAR(300) | Không | Mô tả, `NULL` nếu không có |
| `is_public` | BIT (0/1) | Có | 1 = công khai, 0 = riêng tư. Mặc định 0 |
| `cover_url` | VARCHAR(255) | Không | Ảnh bìa **riêng** do chủ tải lên. `NULL` = không có ảnh riêng |
| `created_at` | DATETIME(6) | Có | Thời điểm tạo, **không bao giờ đổi** |
| `updated_at` | DATETIME(6) | Có | Lần thay đổi gần nhất |

Ví dụ dữ liệu:

| id | user_id | name | description | is_public | cover_url | created_at |
|---|---|---|---|---|---|---|
| 3 | 21 | Chill buổi tối | Nghe trước khi ngủ | 1 | `/uploads/playlists/7c1e2b9a-....jpg` | 2026-10-27 22:30:00 |
| 4 | 21 | Nhạc chạy bộ | NULL | 0 | NULL | 2026-10-28 07:10:00 |

→ Tài khoản 21 (Lan) có 2 playlist: 1 công khai có ảnh riêng, 1 riêng tư dùng ảnh dự phòng.

**Không có cột "danh sách bài"** trong bảng này. Bài nằm ở bảng `playlist_songs`.

**Không có cột "ảnh bìa dự phòng"**: ảnh của bài đầu tiên được tính lại mỗi lần trả kết quả. Nhờ vậy khi bài đầu bị bỏ hoặc bị ẩn, ảnh tự đổi theo mà không phải cập nhật database.

`updated_at` đổi khi: sửa tên/mô tả/công khai (nếu có thay đổi thật), đổi ảnh, gỡ ảnh, thêm bài, bỏ bài.

### 6.2. Bảng `playlist_songs`

Mỗi dòng = 1 bài hát nằm trong 1 playlist.

| Cột | Kiểu | Bắt buộc | Ý nghĩa |
|---|---|---|---|
| `id` | BIGINT, tự tăng | Có | Mã dòng |
| `playlist_id` | BIGINT → `playlists.id` | Có | Playlist chứa bài |
| `song_id` | BIGINT → `songs.id` | Có | Bài hát |
| `added_at` | DATETIME(6) | Có | Thời điểm thêm bài vào playlist |

Ràng buộc: `UNIQUE (playlist_id, song_id)` tên `uk_playlist_songs_playlist_song`.

Ví dụ playlist 3 có 3 bài:

| id | playlist_id | song_id | added_at |
|---|---|---|---|
| 10 | 3 | 15 | 2026-10-27 22:30:00 |
| 11 | 3 | 22 | 2026-10-27 22:30:00 |
| 12 | 3 | 9 | 2026-10-28 08:15:00 |

**Thứ tự bài** = `ORDER BY added_at ASC, id ASC`:
- Thêm trước đứng trước.
- Các bài thêm **cùng lúc** (lúc tạo playlist) có cùng `added_at`, nên xếp tiếp theo `id`. Dòng nào lưu trước có `id` nhỏ hơn → giữ đúng thứ tự người dùng đã chọn.

**Vì sao không dùng `@ManyToMany` như bảng `song_featured_artists`?**
`@ManyToMany` chỉ tạo được bảng trung gian có đúng 2 cột (`playlist_id`, `song_id`). Ở đây cần thêm cột `added_at` để sắp xếp, nên phải viết bảng trung gian thành 1 entity riêng (`PlaylistSong`) gồm 2 quan hệ `@ManyToOne`.

**UNIQUE dùng để làm gì?**
1. 1 bài chỉ nằm 1 lần trong 1 playlist. Service kiểm tra trước (báo `Song is already in this playlist`); UNIQUE là **lớp bảo vệ thứ hai** khi 2 request thêm cùng 1 bài đúng cùng lúc (xem mục 14).
2. UNIQUE tự tạo index bắt đầu bằng `playlist_id` → lấy bài của 1 playlist (`WHERE playlist_id = ?`) rất nhanh.
3. UNIQUE trên **cặp** 2 cột: bài 15 vẫn nằm được trong playlist 3 và playlist 4; chỉ cặp (3, 15) không được lặp.

**Bài bị ẩn / bị gỡ / bị xóa thì sao?**
Dòng trong `playlist_songs` **vẫn giữ nguyên**. Khi hiển thị chỉ lấy bài đang phát hành. → Bài bị ẩn tự biến mất khỏi playlist; phát hành lại thì tự hiện lại **đúng vị trí cũ**.

### 6.3. Sơ đồ quan hệ

```
 ┌──────────────┐ 1            N ┌──────────────┐ 1          N ┌──────────────────┐ N          1 ┌──────────────┐
 │    users     │────────────────│  playlists   │──────────────│  playlist_songs  │──────────────│    songs     │
 │──────────────│    user_id     │──────────────│ playlist_id  │──────────────────│   song_id    │──────────────│
 │ id           │                │ id           │              │ id               │              │ id           │
 │ username     │                │ user_id      │              │ playlist_id      │              │ title        │
 │ account_status│               │ name         │              │ song_id          │              │ status       │
 └──────────────┘                │ description  │              │ added_at         │              │ cover_url ...│
                                 │ is_public    │              │ UNIQUE(playlist_ │              └──────────────┘
                                 │ cover_url    │              │   id, song_id)   │
                                 │ created_at   │              └──────────────────┘
                                 │ updated_at   │
                                 └──────────────┘
 1 tài khoản có nhiều playlist.  1 playlist có nhiều dòng.  1 bài nằm trong nhiều playlist.
 → playlists ⟷ songs là quan hệ NHIỀU - NHIỀU, nối qua bảng trung gian playlist_songs.
```

### 6.4. Câu SQL tạo bảng (tham khảo)

Máy local dùng `ddl-auto=update` nên Hibernate tự tạo. VPS dùng `ddl-auto=validate` (**không tự tạo**), nên ở môi trường mới phải tạo bằng tay **trước khi deploy** (xem mục 15). SQL tương đương (gần đúng):

```sql
CREATE TABLE playlists (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(300),
    is_public   BIT          NOT NULL,
    cover_url   VARCHAR(255),
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE playlist_songs (
    id          BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    playlist_id BIGINT      NOT NULL,
    song_id     BIGINT      NOT NULL,
    added_at    DATETIME(6) NOT NULL,
    CONSTRAINT uk_playlist_songs_playlist_song UNIQUE (playlist_id, song_id),
    FOREIGN KEY (playlist_id) REFERENCES playlists (id),
    FOREIGN KEY (song_id) REFERENCES songs (id)
);
```

### 6.5. Ổ đĩa (ảnh bìa riêng)

```
<app.upload-dir>/
└── playlists/
    ├── 7c1e2b9a-3d4f-4a5b-8c6d-9e0f1a2b3c4d.jpg
    └── b2c3d4e5-....webp
```
- Local: `app.upload-dir=uploads` (thư mục `uploads` trong project). VPS: `/home/myspotify/uploads`.
- Tên file là **UUID ngẫu nhiên** + đuôi, không dùng tên gốc.
- Database chỉ lưu **đường dẫn**: `/uploads/playlists/7c1e2b9a-....jpg`.
- Truy cập file: `http://157.66.100.87:8080/uploads/playlists/...` (**không cần token**).
- Ảnh bìa dự phòng là ảnh của bài hát, nằm ở `/uploads/songs/covers/...` (của module Song).

---

## 7. QUY TẮC NGHIỆP VỤ CHI TIẾT

### 7.1. Giới hạn số lượng

| Giới hạn | Giá trị | Đếm thế nào | Hằng số trong code |
|---|---|---|---|
| Số playlist mỗi tài khoản | **50** | Đếm **cả** riêng tư lẫn công khai | `PlaylistService.MAX_PLAYLISTS_PER_USER` |
| Số bài mỗi playlist | **200** | Khi thêm bài: đếm **mọi dòng** trong `playlist_songs`, **kể cả bài đang bị ẩn** (vẫn chiếm 1 chỗ) | `PlaylistService.MAX_SONGS_PER_PLAYLIST` + `@Size(max = 200)` trong `CreatePlaylistRequest` |
| Số bài khi tạo | 1 → 200 | Sau khi bỏ id trùng | `@NotEmpty`, `@Size(max = 200)` |
| Tên | 1 → 100 ký tự | Không được rỗng hoặc toàn dấu cách | `@NotBlank`, `@Size(max = 100)` |
| Mô tả | 0 → 300 ký tự | Không bắt buộc | `@Size(max = 300)` |
| Ảnh bìa | ≤ **2MB**, JPG/PNG/WEBP, phải là ảnh thật | | `FileStorageService.saveImage` |

Lưu ý: tên playlist **được phép trùng** (Lan có thể có 2 playlist cùng tên "Chill").

### 7.2. Dọn dữ liệu đầu vào

| Dữ liệu | Server xử lý |
|---|---|
| `name` | Cắt dấu cách 2 đầu: `"  Chill  "` → `"Chill"` |
| `description` | Rỗng hoặc toàn dấu cách → lưu `NULL`. Có chữ → cắt dấu cách 2 đầu |
| `isPublic` (lúc tạo) | Không gửi → `false` (riêng tư) |
| `songIds` (lúc tạo) | Bỏ id trùng, bỏ ô trống, **giữ nguyên thứ tự**. `[15, 22, 15, 9]` → `[15, 22, 9]` |

### 7.3. "Bài đang phát hành"

Một bài được coi là đang phát hành khi đủ **cả 4 điều kiện** (hằng số `SongRepository.PUBLISHED_CONDITION`, dùng chung với trang nghe nhạc):

```
1. status = APPROVED                    → Admin đã duyệt
2. releaseAt <= giờ hiện tại             → đã tới giờ phát hành
3. Nghệ sĩ chính có accountStatus = ACTIVE → nghệ sĩ không bị khóa
4. Nghệ sĩ chính có role = ARTIST         → nghệ sĩ còn quyền Artist
```

Quy tắc này áp dụng ở **3 chỗ** trong module Playlist:

| Chỗ | Bài không đang phát hành thì... |
|---|---|
| **Tạo playlist** | Báo lỗi `Song does not exist: <id>` (kèm id để biết bài nào) |
| **Thêm bài** | Báo lỗi `Song does not exist` |
| **Hiển thị và đếm** | Bị **ẩn** khỏi danh sách `songs`, **không tính** vào `songCount`, **không dùng** làm ảnh bìa dự phòng. Dòng trong database vẫn giữ nguyên |

Bảng tình huống: Lan có playlist gồm A, B, C (theo thứ tự thêm).

| Chuyện gì xảy ra | Playlist hiển thị |
|---|---|
| Bình thường | A, B, C |
| Nghệ sĩ ẩn bài B | A, C |
| Nghệ sĩ hiện lại bài B | A, **B**, C (đúng vị trí cũ) |
| Admin khóa nghệ sĩ của A | B, C (ảnh dự phòng đổi sang ảnh của B) |
| Admin mở khóa | A, B, C |
| Nghệ sĩ xóa hẳn bài C | A, B (dòng của C vẫn còn trong database, vẫn chiếm 1 chỗ trong giới hạn 200) |

### 7.4. Thứ tự bài

- Thêm trước đứng trước (`addedAt` tăng dần).
- Lúc tạo playlist: đúng thứ tự `songIds` gửi lên.
- Thêm bài sau này: bài mới luôn nằm **cuối**.
- Bỏ 1 bài rồi thêm lại: bài đó về **cuối** (vì là dòng mới, `addedAt` mới).
- **Không có** API đổi thứ tự / kéo thả (xem mục 16).

### 7.5. Ảnh bìa

```
                 ┌───────────────────────────────┐
                 │ Playlist có ảnh riêng không?  │  (cột cover_url)
                 └───────────────┬───────────────┘
                     Có ▼                 ▼ Không
          coverUrl = ảnh riêng     ┌──────────────────────────────────────┐
          hasCustomCover = true    │ Còn bài đang phát hành nào không?    │
                                   └───────────────┬──────────────────────┘
                                        Có ▼                ▼ Không
                     coverUrl = ảnh bìa của BÀI ĐẦU TIÊN    coverUrl = null
                     (bài đó không có ảnh thì vẫn là null)  hasCustomCover = false
                     hasCustomCover = false
```

- Server **chỉ xét bài đầu tiên**. Bài đầu không có ảnh bìa → `coverUrl = null`, server **không** tìm tiếp sang bài thứ 2.
- `coverUrl = null` → frontend hiện **ảnh mặc định** (VD icon nốt nhạc trên nền xám, giống Spotify).
- Đổi ảnh riêng: lưu ảnh mới → lưu database → **lưu xong mới xóa ảnh cũ** (lỗi giữa chừng thì playlist vẫn luôn có 1 ảnh hợp lệ).
- Gỡ ảnh riêng: lưu database (`cover_url = NULL`) → lưu xong mới xóa file.
- Xóa playlist: ảnh riêng bị xóa khỏi ổ đĩa.

### 7.6. Ai xem được playlist nào

| Người xem | Playlist riêng tư | Playlist công khai | Playlist của tài khoản bị khóa / vô hiệu hóa / đã xóa |
|---|---|---|---|
| **Chủ playlist** | ✔ | ✔ | (Chủ bị khóa thì không đăng nhập được) |
| **Người khác** — xem danh sách (`/users/{userId}`) | ✘ không có trong danh sách | ✔ | ✘ danh sách rỗng `[]` |
| **Người khác** — mở thẳng bằng id (`/{playlistId}`) | ✘ 400 `This playlist is private` | ✔ | ✘ 400 `Playlist does not exist` |

Ghi chú:
- Tài khoản bị khóa được mở khóa → mọi playlist công khai của họ **tự hiện lại**.
- `GET /api/playlists/users/{idCủaChínhMình}` cũng **chỉ** trả playlist công khai (đúng như người khác nhìn thấy). Muốn xem đủ → `GET /api/playlists/me`.

### 7.7. Ai sửa được playlist nào

**Chỉ chủ playlist.** Áp dụng cho mọi API sửa: sửa thông tin, đổi/gỡ ảnh, thêm/bỏ bài, xóa.

| Tình huống | Kết quả |
|---|---|
| Playlist không tồn tại (hoặc đã bị xóa) | 400 `Playlist does not exist` |
| Playlist của người khác (kể cả công khai) | **403** `You do not have permission to access this resource` |
| Playlist của mình | Cho phép |

Vì sao 403 chứ không phải 400? 403 Forbidden nghĩa là "server biết bạn là ai, nhưng bạn không được làm việc này" — đúng nghĩa nhất. Frontend bình thường không hiện nút sửa cho người khác (`isOwner = false`), nên lỗi này chỉ xảy ra khi ai đó tự gọi API.

### 7.8. Ai xóa được bài trong playlist

- Bỏ bài **không** kiểm tra bài còn phát hành hay không: bài đã bị ẩn vẫn bỏ được nếu biết `songId`.
- Bỏ bài **chỉ xóa dòng trong playlist**. Bài hát gốc, lượt nghe, các playlist khác chứa bài đó không bị ảnh hưởng.

### 7.9. Xóa playlist

- **Xóa hẳn** (không phải xóa mềm như bài hát hay tài khoản). Không khôi phục được.
- Thứ tự: xóa mọi dòng `playlist_songs` của playlist → xóa dòng `playlists` → xóa file ảnh bìa riêng.
- Backend **không** hỏi xác nhận → frontend phải hỏi "Bạn chắc chắn muốn xóa?" trước khi gọi.

---

## 8. CHI TIẾT TỪNG API

Mọi API ở phần này:
- Đường dẫn bắt đầu bằng `/api/playlists`.
- Bắt buộc header `Authorization: Bearer <token>`. Không có / sai / hết hạn → **401**.
- Mọi role (USER, ARTIST, ADMIN) đều gọi được.
- Id người gọi **luôn lấy từ token**. Không API nào nhận `userId` của người gọi qua body.
- `playlistId`, `songId`, `userId` trong đường dẫn phải là số. Không phải số → 400 `Invalid parameter: playlistId` (hoặc `songId`, `userId`).
- Thành công trả **200** (kể cả khi tạo mới).

---

### 8.1. Tạo playlist

```
POST /api/playlists
Content-Type: multipart/form-data
```

**Các key trong form-data** (`CreatePlaylistRequest`):

| Key | Kiểu | Bắt buộc | Kiểm tra | Ghi chú |
|---|---|---|---|---|
| `name` | Text | **Có** | Không rỗng, không toàn dấu cách, ≤ 100 ký tự | Server cắt dấu cách 2 đầu |
| `description` | Text | Không | ≤ 300 ký tự | Rỗng → lưu `null` |
| `isPublic` | Text `true`/`false` | Không | Phải là `true` hoặc `false` | Không gửi → `false` (riêng tư) |
| `songIds` | Text (số) | **Có** | Ít nhất 1, tối đa 200, mỗi phần tử là số | **Lặp lại key** cho mỗi bài, hoặc 1 key dạng `15,22,9`. Thứ tự gửi = thứ tự trong playlist |
| `cover` | File | Không | JPG/PNG/WEBP, ≤ 2MB, ảnh thật | Không gửi → dùng ảnh bài đầu tiên |

**Các bước xử lý** (`PlaylistService.createPlaylist`, có `@Transactional`):

```
 0. (Controller) Kiểm tra hình thức dữ liệu theo bảng trên → sai thì 400, dừng
 1. Lấy tài khoản người gọi
 2. Đã có 50 playlist?                       → 400 "Maximum 50 playlists per account"
 3. Kiểm tra danh sách bài:
      a. Bỏ id trùng, bỏ ô trống, giữ thứ tự
      b. Rỗng?                                → 400 "At least 1 song is required"
      c. Quá 200?                             → 400 "Maximum 200 songs per playlist"
      d. Hỏi MySQL 1 câu cho cả danh sách: bài nào đang phát hành?
      e. Có id nào không đang phát hành?      → 400 "Song does not exist: <id>"
        ▲ Bước 1–3 chỉ ĐỌC database, chưa lưu file nào.
        │ Sai ở đây → báo lỗi ngay, không tốn công lưu ảnh rồi phải xóa.
 4. Có gửi ảnh bìa → kiểm tra + lưu ảnh xuống ổ đĩa
 ┌── try ──────────────────────────────────────────────────────┐
 │ 5. Lưu playlist (isPublic không gửi → false)                 │
 │ 6. Lưu từng dòng playlist_songs theo đúng thứ tự             │
 │ 7. Đọc lại danh sách bài → PlaylistDetailResponse            │
 └── catch (lỗi bất kỳ ở 5–7) ─────────────────────────────────┘
       → Xóa ảnh bìa vừa lưu, rồi báo lỗi như bình thường
```

Vì sao cần `try/catch` dọn ảnh? `@Transactional` chỉ hủy được thay đổi trong MySQL, **không xóa được file đã ghi xuống ổ đĩa**. Không dọn thì file ảnh nằm "mồ côi" trên VPS mãi mãi.

**Ví dụ request bằng JavaScript:**
```js
const form = new FormData();
form.append("name", "Chill buổi tối");
form.append("description", "Nghe trước khi ngủ");
form.append("isPublic", "false");
[15, 22, 9].forEach((id) => form.append("songIds", String(id)));
if (coverFile) form.append("cover", coverFile);

const { data } = await api.post("/api/playlists", form);
// KHÔNG tự set header Content-Type, để axios / trình duyệt tự thêm boundary
```

**Response 200** (`PlaylistDetailResponse`):
```json
{
  "id": 3,
  "name": "Chill buổi tối",
  "description": "Nghe trước khi ngủ",
  "coverUrl": "/uploads/songs/covers/9b1c4d7f-....jpg",
  "hasCustomCover": false,
  "isPublic": false,
  "ownerId": 21,
  "ownerName": "lan.nguyen",
  "isOwner": true,
  "songCount": 3,
  "createdAt": "2026-11-08T22:30:00.123456",
  "updatedAt": "2026-11-08T22:30:00.123456",
  "songs": [
    {
      "song": {
        "id": 15, "title": "Chiều Mưa",
        "audioUrl": "/uploads/songs/audio/3f2a9c1e-....mp3",
        "coverUrl": "/uploads/songs/covers/9b1c4d7f-....jpg",
        "canvasUrl": null, "durationSeconds": 215, "playCount": 1024,
        "releaseAt": "2026-10-12T16:30:00",
        "artistId": 5, "artistName": "Mây Chiều",
        "featuredArtists": [{ "id": 12, "artistName": "Sóng Nhỏ" }],
        "genreId": 24, "genreName": "Chill"
      },
      "addedAt": "2026-11-08T22:30:00.120001"
    },
    { "song": { "id": 22, "title": "Phố Đêm", "...": "..." }, "addedAt": "2026-11-08T22:30:00.121002" },
    { "song": { "id": 9,  "title": "Mưa Tháng Sáu", "...": "..." }, "addedAt": "2026-11-08T22:30:00.122003" }
  ]
}
```

**Lỗi có thể gặp:**

| Tình huống | Kết quả |
|---|---|
| Thiếu `name` / rỗng / toàn dấu cách | 400 `Name is required` |
| `name` > 100 ký tự | 400 `Name must not exceed 100 characters` |
| `description` > 300 ký tự | 400 `Description must not exceed 300 characters` |
| `isPublic` không phải `true`/`false` | 400 `Invalid value for isPublic` |
| Không gửi `songIds` | 400 `At least 1 song is required` |
| `songIds` có phần tử không phải số | 400 `Invalid value for songIds` |
| Gửi quá 200 `songIds` | 400 `Maximum 200 songs per playlist` |
| Đã có 50 playlist | 400 `Maximum 50 playlists per account` |
| 1 bài không tồn tại / chưa duyệt / chưa tới giờ / bị ẩn / bị gỡ / nghệ sĩ bị khóa | 400 `Song does not exist: 15` |
| Ảnh > 2MB | 400 `Image must not exceed 2MB` |
| Ảnh không phải JPG/PNG/WEBP | 400 `Only JPG, PNG or WEBP images are allowed` |
| File giả (đổi đuôi .jpg) / hỏng | 400 `Invalid or corrupted image file` |
| Gửi body JSON thay vì form-data | 415 (Content-Type không được hỗ trợ) |
| Chưa đăng nhập | 401 |

Sai **nhiều chỗ cùng lúc** ở bước kiểm tra hình thức → server chỉ trả **1** message (lỗi đầu tiên tìm thấy).

---

### 8.2. Playlist của tôi

```
GET /api/playlists/me
```

- Không cần body, không có tham số.
- Trả về **mảng** (không phân trang, vì tối đa 50) **mọi** playlist của người đang đăng nhập, **cả riêng tư lẫn công khai**.
- Sắp xếp: **mới tạo đứng trước** (`createdAt` giảm dần, cùng thời điểm thì `id` giảm dần).
- Chưa có playlist nào → `[]`.
- Mỗi phần tử là `PlaylistSummaryResponse` (**không kèm danh sách bài**).

**Response 200:**
```json
[
  {
    "id": 4,
    "name": "Nhạc chạy bộ",
    "coverUrl": null,
    "isPublic": false,
    "ownerId": 21,
    "ownerName": "lan.nguyen",
    "songCount": 0,
    "createdAt": "2026-10-28T07:10:00",
    "updatedAt": "2026-11-01T18:00:00"
  },
  {
    "id": 3,
    "name": "Chill buổi tối",
    "coverUrl": "/uploads/playlists/7c1e2b9a-....jpg",
    "isPublic": true,
    "ownerId": 21,
    "ownerName": "lan.nguyen",
    "songCount": 12,
    "createdAt": "2026-10-27T22:30:00",
    "updatedAt": "2026-11-05T21:10:00"
  }
]
```

`songCount` chỉ đếm **bài đang phát hành**. `coverUrl` đã được tính sẵn (ảnh riêng → ảnh bài đầu → `null`).

---

### 8.3. Playlist công khai của 1 người

```
GET /api/playlists/users/{userId}          VD: GET /api/playlists/users/21
```

- Dùng ở **trang cá nhân** của 1 người: hiện mục "Playlist công khai".
- Chỉ trả playlist **công khai** của tài khoản `userId`, và chỉ khi tài khoản đó **đang hoạt động** (ACTIVE).
- Sắp xếp giống 8.2.
- Các trường hợp trả `[]` (không báo lỗi): tài khoản không tồn tại, bị khóa, vô hiệu hóa, đã xóa, hoặc chưa có playlist công khai nào.
- `userId` không phải số → 400 `Invalid parameter: userId`.

`userId` lấy ở đâu? Từ `ownerId` của 1 playlist (bấm vào tên người tạo → mở trang của họ), hoặc từ `userId` khi đăng nhập / `id` trong `GET /api/users/me/profile` (trang của chính mình).

**Response 200:** giống 8.2 (mảng `PlaylistSummaryResponse`), nhưng toàn bộ `isPublic = true`.

---

### 8.4. Chi tiết 1 playlist

```
GET /api/playlists/{playlistId}            VD: GET /api/playlists/3
```

**Các bước xử lý** (`PlaylistService.getPlaylistDetail`):
```
1. Tìm playlist kèm chủ playlist             → không có → 400 "Playlist does not exist"
2. Người xem là chủ playlist?                → Có → cho xem (bỏ qua bước 3)
3. Người xem là người khác:
     Chủ playlist không còn ACTIVE?         → 400 "Playlist does not exist"
     Playlist riêng tư?                      → 400 "This playlist is private"
4. Lấy các bài đang phát hành theo thứ tự thêm → PlaylistDetailResponse
```

**Response 200:** `PlaylistDetailResponse` (ví dụ ở 8.1).
- Chủ playlist xem: `"isOwner": true`.
- Người khác xem playlist công khai: `"isOwner": false` → frontend **ẩn** mọi nút sửa.

**Lỗi có thể gặp:**

| Tình huống | Kết quả |
|---|---|
| Playlist không tồn tại / đã xóa | 400 `Playlist does not exist` |
| Playlist của tài khoản bị khóa (người khác xem) | 400 `Playlist does not exist` |
| Playlist riêng tư của người khác | 400 `This playlist is private` |
| `playlistId` không phải số | 400 `Invalid parameter: playlistId` |

---

### 8.5. Sửa tên, mô tả, công khai

```
PUT /api/playlists/{playlistId}
Content-Type: application/json
```

**Body** (`UpdatePlaylistRequest`):
```json
{
  "name": "Chill tối thứ 7",
  "description": "Nghe lúc đọc sách",
  "isPublic": true
}
```

| Trường | Bắt buộc | Kiểm tra | Ghi chú |
|---|---|---|---|
| `name` | **Có** | Không rỗng, không toàn dấu cách, ≤ 100 ký tự | |
| `description` | Không | ≤ 300 ký tự | Không gửi / `null` / `""` → **xóa mô tả** |
| `isPublic` | **Có** | `true` hoặc `false` | Bắt buộc gửi để tránh vô tình đổi công khai thành riêng tư |

⚠️ **API này thay toàn bộ 3 thông tin cũ.** Luôn gửi đủ cả 3 trường. Muốn giữ mô tả cũ → gửi lại đúng mô tả cũ. (Giống màn hình "Sửa thông tin" của Spotify: hiện sẵn dữ liệu cũ, người dùng sửa chỗ cần sửa, bấm Lưu gửi cả 3.)

**Các bước:** tìm playlist → kiểm tra chủ → gán 3 giá trị mới (tên được cắt dấu cách, mô tả rỗng → `null`) → lưu → trả chi tiết mới.
Gửi lên **giống hệt** dữ liệu cũ → không có gì thay đổi → `updatedAt` giữ nguyên.

**Lỗi có thể gặp:**

| Tình huống | Kết quả |
|---|---|
| Thiếu `name` / rỗng / toàn dấu cách | 400 `Name is required` |
| `name` > 100 ký tự | 400 `Name must not exceed 100 characters` |
| `description` > 300 ký tự | 400 `Description must not exceed 300 characters` |
| Thiếu `isPublic` | 400 `isPublic is required` |
| JSON sai cú pháp / `isPublic` không phải boolean | 400 `Invalid request data` |
| Playlist không tồn tại | 400 `Playlist does not exist` |
| Playlist của người khác | 403 |

Thứ tự kiểm tra: dữ liệu body (400) được kiểm tra **trước** quyền chủ playlist (403). Người khác gửi body sai sẽ nhận 400 chứ không phải 403.

---

### 8.6. Đổi ảnh bìa

```
PUT /api/playlists/{playlistId}/cover
Content-Type: multipart/form-data
```

| Key | Kiểu | Bắt buộc |
|---|---|---|
| `cover` | File | **Có** |

**Các bước:**
```
1. Tìm playlist, kiểm tra chủ                   (sai → 400 / 403)
2. Ghi nhớ đường dẫn ảnh CŨ
3. Kiểm tra + lưu ảnh MỚI xuống ổ đĩa
┌─ try ─────────────────────────────┐
│ 4. coverUrl = ảnh mới, lưu database │
└─ catch: xóa ảnh MỚI, giữ ảnh cũ ───┘
5. Lưu thành công → mới xóa ảnh CŨ khỏi ổ đĩa
6. Trả chi tiết mới: "coverUrl" là ảnh mới, "hasCustomCover": true
```

**JavaScript:**
```js
const form = new FormData();
form.append("cover", file);
const { data } = await api.put(`/api/playlists/${playlistId}/cover`, form);
```

**Lỗi có thể gặp:**

| Tình huống | Kết quả |
|---|---|
| Thiếu hẳn key `cover` | 400 `File is required` |
| Có key `cover` nhưng không chọn file | 400 `Image file is required` |
| Ảnh > 2MB | 400 `Image must not exceed 2MB` |
| Không phải JPG/PNG/WEBP | 400 `Only JPG, PNG or WEBP images are allowed` |
| File giả / hỏng | 400 `Invalid or corrupted image file` |
| Playlist không tồn tại | 400 `Playlist does not exist` |
| Playlist của người khác | 403 |

---

### 8.7. Gỡ ảnh bìa

```
DELETE /api/playlists/{playlistId}/cover     (không body)
```

- Xóa ảnh riêng → playlist quay về dùng ảnh bìa của bài đầu tiên (hoặc `null`).
- Thứ tự: lưu database (`cover_url = NULL`) trước, thành công mới xóa file.
- Response: chi tiết mới, `"hasCustomCover": false`.

| Tình huống | Kết quả |
|---|---|
| Playlist chưa có ảnh riêng | 400 `Playlist has no custom cover` |
| Playlist không tồn tại | 400 `Playlist does not exist` |
| Playlist của người khác | 403 |

Frontend chỉ hiện nút "Gỡ ảnh bìa" khi `hasCustomCover === true` thì sẽ không bao giờ gặp lỗi đầu tiên.

---

### 8.8. Thêm 1 bài vào playlist

```
POST /api/playlists/{playlistId}/songs/{songId}      VD: POST /api/playlists/3/songs/15
(không body)
```

**Các bước** (`PlaylistService.addSong`, có `@Transactional`) — xem sơ đồ đầy đủ ở mục 5.4:
```
1. Tìm playlist, kiểm tra chủ                  → 400 "Playlist does not exist" / 403
2. Bài phải đang phát hành                     → 400 "Song does not exist"
3. Bài chưa có trong playlist                  → 400 "Song is already in this playlist"
4. Playlist chưa đủ 200 dòng                   → 400 "Maximum 200 songs per playlist"
5. Lưu dòng mới (addedAt = bây giờ → bài nằm CUỐI)
6. Cập nhật updatedAt của playlist
7. Trả chi tiết mới
```

**Lỗi có thể gặp:**

| Tình huống | Kết quả |
|---|---|
| Playlist không tồn tại | 400 `Playlist does not exist` |
| Playlist của người khác | 403 |
| Bài không tồn tại / chưa phát hành / bị ẩn / nghệ sĩ bị khóa | 400 `Song does not exist` |
| Bài đã có trong playlist | 400 `Song is already in this playlist` |
| Playlist đã có 200 dòng | 400 `Maximum 200 songs per playlist` |
| Bấm thêm 2 lần **đúng cùng lúc** (request thứ 2 bị database chặn) | 409 `Data already exists` |

---

### 8.9. Bỏ 1 bài khỏi playlist

```
DELETE /api/playlists/{playlistId}/songs/{songId}    VD: DELETE /api/playlists/3/songs/15
(không body)
```

- Chỉ xóa bài **khỏi playlist**. Bài hát gốc vẫn còn nguyên.
- Bài đã bị ẩn vẫn bỏ được (không kiểm tra trạng thái bài).
- Bỏ hết bài vẫn được → playlist rỗng (`songs: []`, `songCount: 0`).
- Response: chi tiết mới (đã không còn bài vừa bỏ).

| Tình huống | Kết quả |
|---|---|
| Bài không nằm trong playlist | 400 `Song is not in this playlist` |
| Playlist không tồn tại | 400 `Playlist does not exist` |
| Playlist của người khác | 403 |

---

### 8.10. Xóa playlist

```
DELETE /api/playlists/{playlistId}           (không body)
```

**Các bước** (`PlaylistService.deletePlaylist`, có `@Transactional`):
```
1. Tìm playlist, kiểm tra chủ
2. Xóa mọi dòng của playlist trong playlist_songs   (phải xóa trước vì khóa ngoại)
3. Xóa playlist
4. Gửi ngay các câu DELETE xuống MySQL (flush) → nếu lỗi thì lỗi ở đây, ảnh chưa bị xóa oan
5. Xóa file ảnh bìa riêng (nếu có)
```

**Response 200:**
```json
{ "message": "Playlist deleted" }
```

| Tình huống | Kết quả |
|---|---|
| Playlist không tồn tại / đã xóa rồi | 400 `Playlist does not exist` |
| Playlist của người khác | 403 |

---

## 9. CẤU TRÚC JSON TRẢ VỀ

### 9.1. `PlaylistSummaryResponse` — 1 ô playlist trong danh sách

Dùng ở `GET /api/playlists/me` và `GET /api/playlists/users/{userId}`.

| Trường | Kiểu | Có thể null | Ý nghĩa |
|---|---|---|---|
| `id` | number | Không | Id playlist |
| `name` | string | Không | Tên playlist |
| `coverUrl` | string | **Có** | Ảnh bìa đã tính sẵn (ảnh riêng → ảnh bài đầu → `null`). Đường dẫn tương đối, ghép `BASE_URL` |
| `isPublic` | boolean | Không | `true` công khai, `false` riêng tư |
| `ownerId` | number | Không | Id chủ playlist |
| `ownerName` | string | Không | `username` của chủ playlist (không phải email, không phải họ tên) |
| `songCount` | number | Không | Số bài **đang phát hành** |
| `createdAt` | string | Không | Thời điểm tạo, giờ Việt Nam, dạng `2026-11-08T22:30:00` (có thể kèm phần lẻ giây) |
| `updatedAt` | string | Không | Lần thay đổi gần nhất |

**Không có** `description`, `hasCustomCover`, `isOwner`, `songs` (muốn có → gọi API chi tiết).
Vì sao tách 2 loại response? Danh sách 50 playlist mà kèm cả bài hát của từng playlist thì JSON rất nặng, trong khi màn hình danh sách không cần tới bài hát.

### 9.2. `PlaylistDetailResponse` — chi tiết 1 playlist

Dùng ở API chi tiết và **mọi API tạo/sửa** (8.1, 8.4 → 8.9).

| Trường | Kiểu | Có thể null | Ý nghĩa |
|---|---|---|---|
| `id` | number | Không | |
| `name` | string | Không | |
| `description` | string | **Có** | Mô tả, `null` nếu không có |
| `coverUrl` | string | **Có** | Giống 9.1 |
| `hasCustomCover` | boolean | Không | `true` = đang dùng ảnh riêng → hiện nút "Gỡ ảnh bìa" |
| `isPublic` | boolean | Không | |
| `ownerId` | number | Không | |
| `ownerName` | string | Không | |
| `isOwner` | boolean | Không | Người **đang xem** là chủ → hiện các nút sửa |
| `songCount` | number | Không | = số phần tử của `songs` |
| `createdAt` | string | Không | |
| `updatedAt` | string | Không | |
| `songs` | array | Không (có thể `[]`) | Các bài đang phát hành, **đúng thứ tự** (thêm trước đứng trước) |

### 9.3. `PlaylistSongResponse` — 1 phần tử của `songs`

| Trường | Kiểu | Ý nghĩa |
|---|---|---|
| `song` | `SongResponse` | Thông tin bài hát, **giống hệt** các API bài hát khác (`GET /api/songs`...) → dùng lại được component hiển thị bài |
| `addedAt` | string | Lúc bài được thêm vào playlist ("Ngày thêm") |

### 9.4. `SongResponse` (của module Song, nhắc lại cho tiện)

| Trường | Kiểu | Có thể null | Ghi chú |
|---|---|---|---|
| `id` | number | Không | |
| `title` | string | Không | |
| `audioUrl` | string | Không | Ghép `BASE_URL` để phát |
| `coverUrl` | string | **Có** | |
| `canvasUrl` | string | **Có** | Video nền ngắn của bài |
| `durationSeconds` | number | Không | VD `215` → hiển thị `3:35` |
| `playCount` | number | Không | Lượt nghe |
| `releaseAt` | string | Không | |
| `artistId` | number | Không | |
| `artistName` | string | Không | |
| `featuredArtists` | `{ id, artistName }[]` | Không (có thể `[]`) | Đã tự bỏ người feat bị khóa |
| `genreId` | number | **Có** | Bài cũ có thể `null` |
| `genreName` | string | **Có** | |

### 9.5. Về tên trường bắt đầu bằng `is` / `has`

Tên trường JSON là **`isPublic`, `isOwner`, `hasCustomCover`** (giữ nguyên chữ `is`/`has`). Không phải `public`, `owner`, `customCover`. Frontend phải viết **đúng chính xác** (phân biệt hoa thường).

---

## 10. XỬ LÝ LỖI

### 10.1. Định dạng chung

Mọi lỗi đều trả JSON:
```json
{ "message": "Playlist does not exist" }
```
Message viết **tiếng Anh, cố định** → frontend so sánh chuỗi để dịch sang tiếng Việt (bảng 10.3).

### 10.2. Mã HTTP

| Mã | Khi nào |
|---|---|
| 200 | Thành công (kể cả tạo mới) |
| 400 | Dữ liệu sai, vi phạm quy tắc nghiệp vụ, playlist không tồn tại / riêng tư |
| 401 | Chưa đăng nhập, token sai / hết hạn, bị đăng nhập ở nơi khác |
| 403 | Sửa / xóa playlist của người khác |
| 409 | 2 request thêm cùng 1 bài vào cùng 1 playlist đúng cùng lúc |
| 413 | File vượt giới hạn upload tổng của server (100MB) |
| 415 | Gửi JSON vào API cần form-data |
| 429 | Gửi quá 100 request / 60 giây |
| 500 | Lỗi không lường trước (bug) |

### 10.3. Bảng message và gợi ý dịch cho frontend

| Message | Mã | API | Gợi ý tiếng Việt |
|---|---|---|---|
| `Name is required` | 400 | Tạo, Sửa | Vui lòng nhập tên playlist |
| `Name must not exceed 100 characters` | 400 | Tạo, Sửa | Tên playlist tối đa 100 ký tự |
| `Description must not exceed 300 characters` | 400 | Tạo, Sửa | Mô tả tối đa 300 ký tự |
| `isPublic is required` | 400 | Sửa | Vui lòng chọn công khai hoặc riêng tư |
| `Invalid value for isPublic` | 400 | Tạo | Giá trị công khai không hợp lệ |
| `At least 1 song is required` | 400 | Tạo | Vui lòng chọn ít nhất 1 bài hát |
| `Invalid value for songIds` | 400 | Tạo | Danh sách bài hát không hợp lệ |
| `Maximum 200 songs per playlist` | 400 | Tạo, Thêm bài | Playlist chỉ chứa tối đa 200 bài |
| `Maximum 50 playlists per account` | 400 | Tạo | Bạn chỉ tạo được tối đa 50 playlist |
| `Song does not exist: <id>` | 400 | Tạo | Có bài hát không còn khả dụng, vui lòng chọn lại |
| `Song does not exist` | 400 | Thêm bài | Bài hát không còn khả dụng |
| `Song is already in this playlist` | 400 | Thêm bài | Bài hát đã có trong playlist |
| `Song is not in this playlist` | 400 | Bỏ bài | Bài hát không có trong playlist |
| `Playlist does not exist` | 400 | Mọi API theo id | Playlist không tồn tại |
| `This playlist is private` | 400 | Chi tiết | Playlist này đang ở chế độ riêng tư |
| `Playlist has no custom cover` | 400 | Gỡ ảnh | Playlist chưa có ảnh bìa riêng |
| `File is required` | 400 | Đổi ảnh | Vui lòng chọn ảnh |
| `Image file is required` | 400 | Đổi ảnh | Vui lòng chọn ảnh |
| `Image must not exceed 2MB` | 400 | Tạo, Đổi ảnh | Ảnh tối đa 2MB |
| `Only JPG, PNG or WEBP images are allowed` | 400 | Tạo, Đổi ảnh | Chỉ hỗ trợ ảnh JPG, PNG, WEBP |
| `Invalid or corrupted image file` | 400 | Tạo, Đổi ảnh | File ảnh không hợp lệ hoặc bị hỏng |
| `Invalid request data` | 400 | Sửa | Dữ liệu gửi lên không hợp lệ |
| `Invalid parameter: playlistId` (`songId`, `userId`) | 400 | Mọi API có id | Đường dẫn không hợp lệ |
| `User does not exist` | 400 | Tạo | (Gần như không xảy ra) Tài khoản không tồn tại |
| `You do not have permission to access this resource` | 403 | Mọi API sửa | Bạn không có quyền sửa playlist này |
| `Data already exists` | 409 | Thêm bài | Bài hát đã có trong playlist |
| `File is too large` | 413 | Tạo, Đổi ảnh | File quá lớn |
| `Unauthorized, please login` | 401 | Mọi API | Vui lòng đăng nhập lại |
| `Your session has ended. Your account may have been logged in on another device` | 401 | Mọi API | Phiên đăng nhập đã kết thúc (tài khoản đăng nhập ở nơi khác) |
| `Too many requests. Please try again in N seconds` | 429 | Mọi API | Thao tác quá nhanh, thử lại sau N giây |

Message `Song does not exist: <id>` có số ở cuối → so sánh bằng `startsWith("Song does not exist")`.

---

## 11. HƯỚNG DẪN CHO FRONTEND

### 11.0. Hướng dẫn cho AI đọc phần này (nếu dùng AI viết code frontend)

- Viết code theo đúng stack, cấu trúc thư mục, cách gọi API (axios instance, interceptor gắn token, store, router) **đang có sẵn trong project frontend**. Không tạo axios instance mới nếu project đã có.
- Tên field trong request/response phải **giữ nguyên chính xác** (phân biệt hoa thường): `songIds`, `isPublic`, `isOwner`, `hasCustomCover`, `coverUrl`, `ownerId`, `ownerName`, `songCount`, `addedAt`.
- Mọi đường dẫn file (`coverUrl`, `audioUrl`, `canvasUrl`) là **tương đối**, phải ghép `BASE_URL`.
- Không sửa backend. Không tự thêm API không có trong tài liệu.
- Dùng lại component hiển thị bài hát và trình phát nhạc **đang có**.
- Làm đủ các phần 11.3 → 11.11 và tự kiểm tra theo checklist 11.14.

### 11.1. Thông tin chung

| Mục | Giá trị |
|---|---|
| Base URL | `http://157.66.100.87:8080` |
| Xác thực | Mọi API cần header `Authorization: Bearer <token>` |
| Định dạng lỗi | Luôn là JSON `{ "message": "..." }` |
| File tĩnh | `coverUrl`, `audioUrl`, `canvasUrl` là đường dẫn tương đối (`/uploads/...`), ghép Base URL phía trước. `/uploads/**` **không** cần token |
| Thời gian | Giờ Việt Nam, dạng `2026-11-08T22:30:00` (có thể có phần lẻ giây). Dùng thẳng, **không** cộng/trừ 7 tiếng |

### 11.2. Kiểu dữ liệu (TypeScript, để tham khảo)

```ts
export interface ArtistSummary {
  id: number;
  artistName: string;
}

// Bài hát (giống các API bài hát khác)
export interface SongResponse {
  id: number;
  title: string;
  audioUrl: string;
  coverUrl: string | null;
  canvasUrl: string | null;
  durationSeconds: number;
  playCount: number;
  releaseAt: string;
  artistId: number;
  artistName: string;
  featuredArtists: ArtistSummary[];
  genreId: number | null;
  genreName: string | null;
}

// 1 ô playlist trong danh sách
export interface PlaylistSummary {
  id: number;
  name: string;
  coverUrl: string | null;
  isPublic: boolean;
  ownerId: number;
  ownerName: string;
  songCount: number;
  createdAt: string;
  updatedAt: string;
}

// 1 bài trong playlist
export interface PlaylistSong {
  song: SongResponse;
  addedAt: string;
}

// Chi tiết playlist
export interface PlaylistDetail {
  id: number;
  name: string;
  description: string | null;
  coverUrl: string | null;
  hasCustomCover: boolean;
  isPublic: boolean;
  ownerId: number;
  ownerName: string;
  isOwner: boolean;
  songCount: number;
  createdAt: string;
  updatedAt: string;
  songs: PlaylistSong[];
}

export interface CreatePlaylistInput {
  name: string;
  description?: string;
  isPublic?: boolean;
  songIds: number[];       // ít nhất 1, tối đa 200, đúng thứ tự muốn xếp
  cover?: File | null;
}

export interface UpdatePlaylistInput {
  name: string;
  description: string | null;
  isPublic: boolean;
}
```

### 11.3. Service / API layer

```js
// playlistApi.js — dùng axios instance "api" có sẵn (đã gắn baseURL và token)

export async function createPlaylist({ name, description, isPublic, songIds, cover }) {
  const form = new FormData();
  form.append("name", name);
  if (description) form.append("description", description);
  form.append("isPublic", String(Boolean(isPublic)));
  songIds.forEach((id) => form.append("songIds", String(id)));   // lặp key, giữ thứ tự
  if (cover) form.append("cover", cover);
  const { data } = await api.post("/api/playlists", form);         // KHÔNG tự set Content-Type
  return data;                                                     // PlaylistDetail
}

export async function getMyPlaylists() {
  const { data } = await api.get("/api/playlists/me");
  return data;                                                     // PlaylistSummary[]
}

export async function getPublicPlaylists(userId) {
  const { data } = await api.get(`/api/playlists/users/${userId}`);
  return data;                                                     // PlaylistSummary[]
}

export async function getPlaylist(playlistId) {
  const { data } = await api.get(`/api/playlists/${playlistId}`);
  return data;                                                     // PlaylistDetail
}

export async function updatePlaylist(playlistId, { name, description, isPublic }) {
  const { data } = await api.put(`/api/playlists/${playlistId}`, {
    name,
    description: description || null,
    isPublic,
  });
  return data;                                                     // PlaylistDetail
}

export async function updatePlaylistCover(playlistId, file) {
  const form = new FormData();
  form.append("cover", file);
  const { data } = await api.put(`/api/playlists/${playlistId}/cover`, form);
  return data;                                                     // PlaylistDetail
}

export async function removePlaylistCover(playlistId) {
  const { data } = await api.delete(`/api/playlists/${playlistId}/cover`);
  return data;                                                     // PlaylistDetail
}

export async function addSongToPlaylist(playlistId, songId) {
  const { data } = await api.post(`/api/playlists/${playlistId}/songs/${songId}`);
  return data;                                                     // PlaylistDetail
}

export async function removeSongFromPlaylist(playlistId, songId) {
  const { data } = await api.delete(`/api/playlists/${playlistId}/songs/${songId}`);
  return data;                                                     // PlaylistDetail
}

export async function deletePlaylist(playlistId) {
  const { data } = await api.delete(`/api/playlists/${playlistId}`);
  return data;                                                     // { message: "Playlist deleted" }
}
```

Hàm tiện ích nên có:
```js
export const BASE_URL = "http://157.66.100.87:8080";

// Ảnh bìa: null → ảnh mặc định
export function playlistCover(coverUrl) {
  return coverUrl ? BASE_URL + coverUrl : "/images/default-playlist.png";
}

// 215 → "3:35"
export function formatDuration(seconds) {
  const m = Math.floor(seconds / 60);
  const s = String(seconds % 60).padStart(2, "0");
  return `${m}:${s}`;
}

// Tổng thời lượng playlist: 3725 → "1 giờ 2 phút"
export function totalDuration(songs) {
  const total = songs.reduce((sum, item) => sum + item.song.durationSeconds, 0);
  const h = Math.floor(total / 3600);
  const m = Math.floor((total % 3600) / 60);
  return h > 0 ? `${h} giờ ${m} phút` : `${m} phút`;
}
```

### 11.4. Các màn hình và route gợi ý

| Route | Màn hình | API |
|---|---|---|
| (thanh bên trái) | Mục "Thư viện" / "Playlist của tôi" | `GET /api/playlists/me` |
| `/playlists/:playlistId` | Trang chi tiết playlist | `GET /api/playlists/{id}` |
| (modal) | Tạo playlist | `GET /api/songs/search` + `POST /api/playlists` |
| (modal) | Sửa thông tin playlist | `PUT /api/playlists/{id}` |
| (menu trên mỗi bài hát) | "Thêm vào playlist" | `GET /api/playlists/me` + `POST .../songs/{songId}` |
| `/users/:userId` | Trang cá nhân → mục "Playlist công khai" | `GET /api/playlists/users/{userId}` |

### 11.5. Mục "Playlist của tôi" (thanh bên / trang Thư viện)

- Gọi `getMyPlaylists()` khi vào app (sau đăng nhập) và lưu vào store dùng chung (menu "Thêm vào playlist" cũng cần danh sách này).
- Mỗi ô hiển thị: ảnh bìa (`playlistCover(coverUrl)`), `name`, dòng phụ `"Playlist • {ownerName}"` hoặc `"{songCount} bài hát"`, biểu tượng 🔒 nếu `isPublic === false`.
- Danh sách đã được server sắp mới tạo trước → hiển thị đúng thứ tự trả về.
- Rỗng → hiện "Bạn chưa có playlist nào" + nút "Tạo playlist".
- Có nút **"+ Tạo playlist"**. Khi đã có 50 playlist → có thể disable nút và ghi chú "Tối đa 50 playlist".
- Sau khi tạo / sửa / xóa playlist → **cập nhật lại store** (gọi lại `getMyPlaylists()` hoặc tự sửa phần tử trong store).

### 11.6. Modal tạo playlist

Các ô nhập:

| Ô | Kiểm tra ở frontend (server vẫn kiểm tra lại) |
|---|---|
| Tên (bắt buộc) | Không rỗng sau khi `trim()`, ≤ 100 ký tự, hiện bộ đếm `x/100` |
| Mô tả | ≤ 300 ký tự, hiện bộ đếm `x/300` |
| Công khai (toggle) | Mặc định **tắt** (riêng tư) |
| Ảnh bìa (tùy chọn) | Chỉ nhận `image/jpeg, image/png, image/webp` (`<input accept="image/jpeg,image/png,image/webp">`), ≤ 2MB, hiện ảnh xem trước |
| Chọn bài hát (bắt buộc ≥ 1) | Ô tìm kiếm → danh sách kết quả → bấm để chọn |

Ô chọn bài hát:
1. Người dùng gõ → debounce khoảng 300ms → gọi `GET /api/songs/search?keyword=...&page=0&size=20`.
2. Hiển thị kết quả (ảnh, tên bài, nghệ sĩ). Bấm "+" để thêm vào danh sách đã chọn; bài đã chọn hiện dấu ✓ và không thêm trùng.
3. Danh sách đã chọn hiện bên dưới, **đúng thứ tự bấm chọn**, có nút "×" để bỏ. Đây cũng là thứ tự trong playlist.
4. Tối đa 200 bài; chưa chọn bài nào → disable nút "Tạo".

Gợi ý tiện lợi: khi mở modal từ menu **"Thêm vào playlist → Tạo playlist mới"** của 1 bài hát, **chọn sẵn** bài đó trong danh sách.

Bấm "Tạo":
- Disable nút + hiện loading để tránh bấm 2 lần (bấm 2 lần sẽ tạo **2 playlist**).
- Thành công → đóng modal, thêm playlist mới vào **đầu** danh sách trong store, chuyển tới `/playlists/{data.id}` (response đã là chi tiết, có thể hiển thị luôn không cần gọi lại).
- Lỗi → hiện `message` đã dịch dưới form. Riêng `Song does not exist: <id>` → báo "Có bài hát không còn khả dụng" và **bỏ bài có id đó** khỏi danh sách đã chọn để người dùng bấm tạo lại.

### 11.7. Trang chi tiết playlist `/playlists/:playlistId`

Gọi `getPlaylist(playlistId)` khi vào trang.

**Phần đầu trang (header):**
- Ảnh bìa lớn: `playlistCover(coverUrl)`.
- Chữ nhỏ "Playlist công khai" / "Playlist riêng tư 🔒" theo `isPublic`.
- Tên playlist (chữ lớn), mô tả (nếu `description` khác `null`).
- Dòng thông tin: `ownerName` (bấm → `/users/{ownerId}`) • `{songCount} bài hát` • `totalDuration(songs)`.
- Nút **▶ Phát**: phát từ bài đầu tiên (11.10). `songs` rỗng → disable.

**Chỉ khi `isOwner === true`, hiện thêm:**
- Bấm vào ảnh bìa / tên → mở modal sửa (11.8).
- Menu "⋯": "Sửa thông tin", "Đổi ảnh bìa", "Gỡ ảnh bìa" (chỉ khi `hasCustomCover === true`), "Chuyển sang công khai/riêng tư", "Xóa playlist".

**Danh sách bài** (bảng giống Spotify):

| # | Tiêu đề | Nghệ sĩ | Ngày thêm | Thời lượng | (chủ) |
|---|---|---|---|---|---|
| `index + 1` | ảnh nhỏ + `song.title` | `song.artistName` (+ feat) | từ `addedAt` (VD "3 ngày trước" hoặc `08/11/2026`) | `formatDuration(song.durationSeconds)` | nút "Xóa khỏi playlist" |

- Hiển thị **đúng thứ tự** server trả về. Không tự sắp xếp lại.
- Bấm 1 bài → phát từ bài đó (11.10).
- `songs` rỗng → "Playlist này chưa có bài hát nào" (+ nút "Tìm bài hát" nếu là chủ).

**Xử lý lỗi khi tải trang:**
- `This playlist is private` → trang thông báo "Playlist này đang ở chế độ riêng tư".
- `Playlist does not exist` → trang "Không tìm thấy playlist" + nút về trang chủ.
- 401 → về trang đăng nhập.

**Sau mỗi thao tác sửa** (sửa thông tin, đổi/gỡ ảnh, bỏ bài): response là chi tiết mới → **thay toàn bộ dữ liệu trang bằng response**, đồng thời cập nhật phần tử tương ứng trong store "Playlist của tôi" (tên, ảnh, `isPublic`, `songCount`).

### 11.8. Sửa thông tin, ảnh bìa, xóa playlist

**Modal sửa thông tin:**
- Mở ra **điền sẵn** `name`, `description` (null → ô trống), toggle `isPublic` từ dữ liệu hiện tại.
- Bấm Lưu → `updatePlaylist(id, { name, description, isPublic })` — **luôn gửi đủ 3 trường**.
- Toggle nhanh công khai/riêng tư ở menu cũng phải gửi đủ 3: `{ name: hiệnTại.name, description: hiệnTại.description, isPublic: !hiệnTại.isPublic }`.

**Đổi ảnh bìa:**
- `<input type="file" accept="image/jpeg,image/png,image/webp">` → kiểm tra ≤ 2MB → `updatePlaylistCover(id, file)`.
- Ảnh mới có đường dẫn **khác hẳn** ảnh cũ → không lo trình duyệt cache ảnh cũ.

**Gỡ ảnh bìa:** chỉ hiện khi `hasCustomCover === true` → `removePlaylistCover(id)`.

**Xóa playlist:**
- **Bắt buộc hỏi xác nhận**: "Xóa playlist '{name}'? Không thể hoàn tác."
- Thành công → bỏ playlist khỏi store, chuyển về trang chủ / thư viện, toast "Đã xóa playlist".

### 11.9. "Thêm vào playlist" trên mỗi bài hát

Ở mọi nơi hiển thị bài hát (danh sách bài, kết quả tìm kiếm, trang nghệ sĩ, trình phát, trang chi tiết bài), thêm menu "⋯" → **"Thêm vào playlist"**:

1. Hiện danh sách playlist **của tôi** (lấy từ store, nếu chưa có thì gọi `getMyPlaylists()`), có ô lọc theo tên nếu nhiều.
2. Dòng đầu tiên: **"+ Tạo playlist mới"** → mở modal 11.6 với bài này **chọn sẵn**.
3. Bấm 1 playlist → `addSongToPlaylist(playlist.id, song.id)`:
    - Thành công → toast "Đã thêm vào {playlist.name}", cập nhật `songCount` trong store bằng `data.songCount`.
    - `Song is already in this playlist` hoặc 409 → toast "Bài hát đã có trong {playlist.name}" (không phải lỗi nghiêm trọng).
    - `Maximum 200 songs per playlist` → toast "Playlist đã đủ 200 bài".
    - `Song does not exist` → toast "Bài hát không còn khả dụng".

Gợi ý: disable mục playlist trong lúc đang gửi request để tránh bấm 2 lần.

### 11.10. Phát nhạc cả playlist

- Hàng chờ phát = `playlist.songs.map(item => item.song)` **đúng thứ tự**.
- Bấm "▶ Phát" → bắt đầu từ bài 0. Bấm 1 bài → bắt đầu từ bài đó, các bài sau nối tiếp.
- Mỗi khi **bắt đầu phát 1 bài**: gọi `POST /api/songs/{song.id}/play` (giống phát nhạc ở chỗ khác; lỗi API này thì bỏ qua, không dừng nhạc).
- Hết bài cuối của playlist → dừng, hoặc chuyển sang chế độ gợi ý bài tiếp theo (`GET /api/songs/{songId}/recommendations`) nếu project đang có tính năng này.
- Tùy chọn: nút "Phát ngẫu nhiên" → xáo trộn hàng chờ **ở frontend** (không gọi API).

### 11.11. Trang cá nhân `/users/:userId` — mục "Playlist công khai"

- Gọi `getPublicPlaylists(userId)`.
- Hiện lưới ô playlist (giống 11.5 nhưng không có ổ khóa vì toàn công khai). Bấm → `/playlists/{id}`.
- `[]` → "Người dùng này chưa có playlist công khai nào".
- Trang cá nhân của **chính mình**: API này chỉ trả playlist công khai. Có thể ghi chú "Chỉ hiển thị playlist công khai".
- Backend **chưa có** API xem hồ sơ công khai của người khác (tên, ảnh đại diện). Tạm thời lấy `ownerName` từ playlist đầu tiên làm tiêu đề trang, hoặc truyền tên qua state của router khi bấm vào tên người tạo.

### 11.12. Xử lý lỗi chung

```js
function handlePlaylistError(error) {
  const status = error.response?.status;
  const message = error.response?.data?.message ?? "";

  switch (status) {
    case 400: showToast(translatePlaylistMessage(message)); break;
    case 401: logout(); redirectToLogin(); break;   // gồm cả bị đăng nhập nơi khác
    case 403: showToast("Bạn không có quyền sửa playlist này"); break;
    case 409: showToast("Bài hát đã có trong playlist"); break;
    case 413: showToast("File quá lớn"); break;
    case 429: showToast(`Thao tác quá nhanh, thử lại sau ${error.response.headers["retry-after"]} giây`); break;
    default:  showToast("Có lỗi xảy ra, vui lòng thử lại");
  }
}

function translatePlaylistMessage(message) {
  if (message.startsWith("Song does not exist")) return "Bài hát không còn khả dụng";
  const map = {
    "Name is required": "Vui lòng nhập tên playlist",
    "Name must not exceed 100 characters": "Tên playlist tối đa 100 ký tự",
    "Description must not exceed 300 characters": "Mô tả tối đa 300 ký tự",
    "At least 1 song is required": "Vui lòng chọn ít nhất 1 bài hát",
    "Maximum 200 songs per playlist": "Playlist chỉ chứa tối đa 200 bài",
    "Maximum 50 playlists per account": "Bạn chỉ tạo được tối đa 50 playlist",
    "Song is already in this playlist": "Bài hát đã có trong playlist",
    "Song is not in this playlist": "Bài hát không có trong playlist",
    "Playlist does not exist": "Playlist không tồn tại",
    "This playlist is private": "Playlist này đang ở chế độ riêng tư",
    "Playlist has no custom cover": "Playlist chưa có ảnh bìa riêng",
    "Image must not exceed 2MB": "Ảnh tối đa 2MB",
    "Only JPG, PNG or WEBP images are allowed": "Chỉ hỗ trợ ảnh JPG, PNG, WEBP",
    "Invalid or corrupted image file": "File ảnh không hợp lệ hoặc bị hỏng",
    "Image file is required": "Vui lòng chọn ảnh",
    "File is required": "Vui lòng chọn ảnh",
  };
  return map[message] ?? message;
}
```

### 11.13. Lỗi hay gặp ở frontend

| Lỗi | Hậu quả | Cách đúng |
|---|---|---|
| Tự set `Content-Type: multipart/form-data` khi gửi FormData | Mất boundary → server không đọc được form → 400 / 500 | Không set, để axios tự đặt |
| Gửi JSON vào `POST /api/playlists` | 415 | Phải dùng `FormData` |
| Gửi `songIds` bằng `JSON.stringify([15,22])` | Server nhận chuỗi `"[15,22]"` → `Invalid value for songIds` | `forEach` + `append("songIds", id)` cho từng bài |
| Viết sai tên key: `songIDs`, `song_ids`, `public`, `is_public` | Server coi như không gửi | Đúng chính xác: `songIds`, `isPublic` |
| `PUT /api/playlists/{id}` chỉ gửi `{ isPublic }` | 400 `Name is required` | Luôn gửi đủ `name`, `description`, `isPublic` |
| Không gửi `description` khi sửa | **Mô tả bị xóa** | Gửi lại mô tả cũ nếu không đổi |
| Đọc `playlist.public` hoặc `playlist.owner` | `undefined` | Đúng tên: `isPublic`, `isOwner` |
| Hiển thị `coverUrl` khi nó `null` | Ảnh vỡ | Dùng ảnh mặc định |
| Quên ghép `BASE_URL` trước `coverUrl` / `audioUrl` | Ảnh / nhạc không tải được | `BASE_URL + url` |
| Tự sắp xếp lại `songs` | Sai thứ tự người dùng muốn | Hiển thị đúng thứ tự server trả |
| Hiện nút sửa khi `isOwner === false` | Người dùng bấm → 403 | Ẩn nút theo `isOwner` |
| Sửa xong lại gọi thêm `GET /api/playlists/{id}` | Thừa 1 request | Dùng luôn response của API sửa |
| Không hỏi xác nhận trước khi xóa | Xóa nhầm, không khôi phục được | Luôn hỏi xác nhận |
| Không disable nút "Tạo" khi đang gửi | Bấm 2 lần tạo ra 2 playlist giống nhau | Disable + loading |

### 11.14. Checklist tự kiểm tra (frontend)

- [ ] Thanh bên / thư viện hiện đúng playlist của tôi, mới tạo đứng trước, có ổ khóa cho playlist riêng tư.
- [ ] Tạo playlist: bắt buộc tên và ≥ 1 bài; thứ tự bài trong playlist đúng thứ tự đã chọn.
- [ ] Tạo playlist không có ảnh → ảnh bìa là ảnh của bài đầu tiên (hoặc ảnh mặc định).
- [ ] Tạo playlist có ảnh → hiện đúng ảnh đã chọn.
- [ ] Trang chi tiết hiện đủ: ảnh, tên, mô tả, người tạo, số bài, tổng thời lượng, danh sách bài có ngày thêm.
- [ ] Người khác mở playlist công khai: xem và nghe được, **không** thấy nút sửa.
- [ ] Người khác mở playlist riêng tư: hiện thông báo riêng tư.
- [ ] Sửa tên/mô tả/công khai: lưu đúng, mô tả cũ **không** bị mất khi chỉ đổi tên.
- [ ] Đổi ảnh bìa, gỡ ảnh bìa (nút gỡ chỉ hiện khi `hasCustomCover`).
- [ ] "Thêm vào playlist" từ mọi nơi có bài hát; thêm trùng hiện thông báo thân thiện.
- [ ] "Xóa khỏi playlist" chỉ hiện với chủ; xóa xong danh sách cập nhật ngay.
- [ ] Xóa playlist có hỏi xác nhận; xóa xong biến mất khỏi thanh bên.
- [ ] Phát playlist: phát đúng thứ tự, mỗi bài gọi `/play` 1 lần khi bắt đầu.
- [ ] Trang cá nhân hiện playlist công khai; người không có thì hiện thông báo trống.
- [ ] Mọi lỗi hiển thị tiếng Việt, không hiện chữ "null" hay `undefined`.

---

## 12. HƯỚNG DẪN TEST BẰNG POSTMAN

### 12.1. Chuẩn bị

1. **2 tài khoản** đang hoạt động: **A** (chủ playlist) và **B** (người khác). Đăng nhập cả 2 qua `POST /api/auth/login`, lưu `token` và `userId` của từng người.
2. Ít nhất **4 bài hát đang phát hành**. Lấy id bằng `GET /api/songs?page=0&size=20` (ghi lại 4 id, gọi là S1, S2, S3, S4; nên có ít nhất 1 bài có ảnh bìa).
3. Nếu test ẩn bài: 1 tài khoản **ARTIST** là chủ của 1 trong các bài trên.
4. 1 ảnh JPG < 2MB, 1 ảnh > 2MB, 1 file `.txt` đổi đuôi thành `.jpg`.
5. Postman: tạo Environment có biến `baseUrl = http://localhost:8080` (hoặc VPS), `tokenA`, `tokenB`. Mỗi request: tab **Authorization → Bearer Token → `{{tokenA}}`**.

### 12.2. Kịch bản chính (chạy theo thứ tự)

| # | Tài khoản | Request | Kết quả mong đợi |
|---|---|---|---|
| 1 | A | `POST /api/playlists` form-data: `name=Test 1`, `songIds=S1`, `songIds=S2`, `songIds=S3` | 200, `isPublic: false`, `songCount: 3`, `songs` đúng thứ tự S1, S2, S3, `hasCustomCover: false`, `coverUrl` = ảnh của S1 (hoặc `null`), `isOwner: true`. Ghi lại `id` = **P** |
| 2 | A | `GET /api/playlists/me` | Mảng có P ở **đầu**, `songCount: 3` |
| 3 | A | `GET /api/playlists/P` | Giống kết quả bước 1 |
| 4 | B | `GET /api/playlists/P` | 400 `This playlist is private` |
| 5 | B | `GET /api/playlists/users/{userIdA}` | `[]` (P đang riêng tư) |
| 6 | A | `PUT /api/playlists/P` JSON `{"name":"Test 1 sửa","description":"Mô tả","isPublic":true}` | 200, tên/mô tả mới, `isPublic: true`, `updatedAt` mới |
| 7 | B | `GET /api/playlists/P` | 200, `isOwner: false` |
| 8 | B | `GET /api/playlists/users/{userIdA}` | Mảng có P |
| 9 | B | `PUT /api/playlists/P` (body hợp lệ) | **403** |
| 10 | B | `POST /api/playlists/P/songs/S4` | **403** |
| 11 | B | `DELETE /api/playlists/P` | **403** |
| 12 | A | `POST /api/playlists/P/songs/S4` | 200, S4 nằm **cuối**, `songCount: 4` |
| 13 | A | `POST /api/playlists/P/songs/S4` (lần 2) | 400 `Song is already in this playlist` |
| 14 | A | `DELETE /api/playlists/P/songs/S2` | 200, còn S1, S3, S4 |
| 15 | A | `DELETE /api/playlists/P/songs/S2` (lần 2) | 400 `Song is not in this playlist` |
| 16 | A | `POST /api/playlists/P/songs/S2` | 200, S2 giờ nằm **cuối**: S1, S3, S4, S2 |
| 17 | A | `PUT /api/playlists/P/cover` form-data `cover` = ảnh JPG | 200, `hasCustomCover: true`, `coverUrl` bắt đầu `/uploads/playlists/` |
| 18 | — | Mở `{{baseUrl}}` + `coverUrl` trên trình duyệt | Thấy ảnh |
| 19 | A | `PUT /api/playlists/P/cover` lại với ảnh khác | 200, `coverUrl` mới. Dùng Postman `GET` đường dẫn ảnh **cũ** → 404 (file đã bị xóa; mở bằng trình duyệt có thể vẫn thấy do cache 1 giờ) |
| 20 | A | `DELETE /api/playlists/P/cover` | 200, `hasCustomCover: false`, `coverUrl` = ảnh của bài đầu (S1) |
| 21 | A | `DELETE /api/playlists/P/cover` (lần 2) | 400 `Playlist has no custom cover` |
| 22 | A | `DELETE /api/playlists/P` | 200 `{"message":"Playlist deleted"}` |
| 23 | A | `GET /api/playlists/P` | 400 `Playlist does not exist` |
| 24 | A | `GET /api/playlists/me` | Không còn P |

### 12.3. Kịch bản kiểm tra dữ liệu sai (tạo playlist)

| # | Body form-data | Kết quả mong đợi |
|---|---|---|
| 1 | Không có `name` | 400 `Name is required` |
| 2 | `name` = `"   "` | 400 `Name is required` |
| 3 | `name` dài 101 ký tự | 400 `Name must not exceed 100 characters` |
| 4 | `description` dài 301 ký tự | 400 `Description must not exceed 300 characters` |
| 5 | Không có `songIds` | 400 `At least 1 song is required` |
| 6 | `songIds = abc` | 400 `Invalid value for songIds` |
| 7 | `songIds = 999999` (không tồn tại) | 400 `Song does not exist: 999999` |
| 8 | `isPublic = abc` | 400 `Invalid value for isPublic` |
| 9 | `songIds = S1`, `songIds = S1`, `songIds = S2` | 200, chỉ 2 bài S1, S2 (bỏ trùng) |
| 10 | 1 key `songIds = S3,S1,S2` | 200, đúng thứ tự S3, S1, S2 |
| 11 | Không gửi `isPublic` | 200, `isPublic: false` |
| 12 | `cover` = ảnh > 2MB | 400 `Image must not exceed 2MB` |
| 13 | `cover` = file .txt đổi đuôi .jpg | 400 `Only JPG, PNG or WEBP images are allowed` hoặc `Invalid or corrupted image file` |
| 14 | Body raw JSON thay vì form-data | 415 |
| 15 | Không có token | 401 |

### 12.4. Kịch bản sửa thông tin sai

| # | Body JSON | Kết quả mong đợi |
|---|---|---|
| 1 | `{"description":"x","isPublic":true}` | 400 `Name is required` |
| 2 | `{"name":"x","description":"x"}` | 400 `isPublic is required` |
| 3 | `{"name":"x","isPublic":"abc"}` | 400 `Invalid request data` |
| 4 | `{"name":"x","isPublic":true}` (không có description) | 200, `description: null` (mô tả bị xóa) |
| 5 | `{"name":"  x  ","description":"   ","isPublic":true}` | 200, `name: "x"`, `description: null` |
| 6 | JSON thiếu dấu `}` | 400 `Invalid request data` |

### 12.5. Kịch bản "bài đang phát hành"

| # | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | A tạo playlist Q gồm S1, S2, S3 (S1 có ảnh bìa, Q không có ảnh riêng) | `coverUrl` = ảnh S1 |
| 2 | ARTIST chủ bài S1 ẩn bài: `PUT /api/artist/songs/S1/hide` | |
| 3 | A `GET /api/playlists/Q` | `songs` chỉ còn S2, S3; `songCount: 2`; `coverUrl` = ảnh S2 (hoặc `null`) |
| 4 | A `GET /api/playlists/me` | Q có `songCount: 2` |
| 5 | A `POST /api/playlists/Q/songs/S1` | 400 `Song does not exist` |
| 6 | ARTIST hiện lại: `PUT /api/artist/songs/S1/show` | |
| 7 | A `GET /api/playlists/Q` | Lại đủ S1, S2, S3, S1 đứng **đầu** như cũ |

### 12.6. Kịch bản giới hạn và tài khoản bị khóa (tùy chọn)

| # | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | A tạo đủ 50 playlist, tạo cái thứ 51 | 400 `Maximum 50 playlists per account` |
| 2 | Playlist có 200 bài, thêm bài thứ 201 | 400 `Maximum 200 songs per playlist` (dễ test hơn bằng cách tạm đổi hằng số trên local) |
| 3 | A có playlist công khai P. Admin khóa A | B: `GET /api/playlists/P` → 400 `Playlist does not exist`; `GET /api/playlists/users/{userIdA}` → `[]` |
| 4 | Admin mở khóa A | B xem lại được P |
| 5 | `GET /api/playlists/abc` | 400 `Invalid parameter: playlistId` |
| 6 | `GET /api/playlists/users/abc` | 400 `Invalid parameter: userId` |

### 12.7. Kiểm tra trực tiếp trong MySQL (tùy chọn)

```
mysql -u myspotify -p myspotify_db
```
(Nhập mật khẩu khi được hỏi. **Không** ghi mật khẩu vào câu lệnh.)

```sql
-- Playlist của 1 tài khoản
SELECT id, name, is_public, cover_url, created_at, updated_at FROM playlists WHERE user_id = 21;

-- Các dòng của 1 playlist, đúng thứ tự hiển thị (kể cả bài đang bị ẩn)
SELECT ps.id, ps.song_id, s.title, s.status, ps.added_at
FROM playlist_songs ps JOIN songs s ON s.id = ps.song_id
WHERE ps.playlist_id = 3
ORDER BY ps.added_at, ps.id;

-- Sau khi xóa playlist 3: phải không còn dòng nào
SELECT COUNT(*) FROM playlist_songs WHERE playlist_id = 3;
```

---

## 13. BẢO MẬT VÀ PHÂN QUYỀN

### 13.1. Ma trận quyền

| API | Chưa đăng nhập | Chủ playlist | Người khác |
|---|---|---|---|
| Tạo playlist | 401 | — | ✔ (tạo cho chính mình) |
| Playlist của tôi | 401 | ✔ | ✔ (của chính họ) |
| Playlist công khai của 1 người | 401 | ✔ | ✔ |
| Chi tiết | 401 | ✔ | Chỉ khi công khai + chủ còn hoạt động |
| Sửa, đổi/gỡ ảnh, thêm/bỏ bài, xóa | 401 | ✔ | **403** |
| File ảnh `/uploads/playlists/...` | ✔ | ✔ | ✔ |

### 13.2. Các lớp bảo vệ

| Lớp | Chặn cái gì | Ở đâu |
|---|---|---|
| Token JWT + phiên Redis | Người lạ, token cũ | `JwtAuthenticationFilter` |
| Rate limit | Spam: 100 request / 60 giây / tài khoản | `RateLimitFilter` |
| Id lấy từ token | Giả làm người khác: không API nào nhận id người gọi từ frontend | `PlaylistController.me(...)` |
| Kiểm tra chủ playlist | Sửa playlist người khác | `PlaylistService.getOwnedPlaylist` |
| Kiểm tra quyền xem | Xem playlist riêng tư / của tài khoản bị khóa | `getPlaylistDetail`, `findPublicPlaylists` |
| DTO tách biệt | Lộ dữ liệu: response chỉ có `ownerId`, `ownerName`, **không** có email hay thông tin khác của chủ | `PlaylistMapper` |
| DTO request | Gửi kèm trường lạ (VD `owner`, `id`) để phá: DTO chỉ có đúng các trường được phép | `CreatePlaylistRequest`, `UpdatePlaylistRequest` |
| Kiểm tra ảnh | Upload file độc: định dạng, dung lượng, chữ ký file | `FileStorageService.saveImage` |
| Tên file UUID | Ghi đè, đoán đường dẫn | `FileStorageService` |
| Câu truy vấn tham số hóa | SQL injection | Spring Data JPA |

### 13.3. Ví dụ tấn công và cách hệ thống chặn

| Kẻ xấu thử | Kết quả |
|---|---|
| B gọi `DELETE /api/playlists/3` (của A) | 403, playlist A không sao |
| B dò `GET /api/playlists/1`, `/2`, `/3`... | Playlist riêng tư → `This playlist is private`, không thấy nội dung |
| Gửi `ownerId` / `owner` trong body tạo playlist | Bị bỏ qua, chủ luôn là người có token |
| Thêm bài chưa duyệt vào playlist để "nghe trước" | 400 `Song does not exist` |
| Đặt tên playlist chứa `' OR 1=1 --` | Lưu như chữ bình thường |
| Upload `virus.html` đổi tên `anh.jpg` | 400 (không đúng chữ ký ảnh) |

---

## 14. XỬ LÝ ĐỒNG THỜI (NHIỀU REQUEST CÙNG LÚC)

| Tình huống | Cơ chế | Kết quả |
|---|---|---|
| Bấm "Thêm" 2 lần đúng cùng lúc cùng 1 bài | Service kiểm tra trước; database có `UNIQUE (playlist_id, song_id)` chặn request thứ 2 | Request 1: 200. Request 2: 400 `Song is already in this playlist` hoặc **409** `Data already exists` |
| Tạo playlist bị lỗi giữa chừng (VD lưu bài thứ 3 lỗi) | `@Transactional` | Hủy toàn bộ, không để lại playlist dở dang; ảnh bìa vừa lưu bị xóa |
| Xóa playlist | `@Transactional`: xóa `playlist_songs` + `playlists` cùng 1 khối | Hoặc xóa hết, hoặc không xóa gì |
| Đổi ảnh bìa lỗi khi lưu database | Lưu ảnh mới → lưu DB → mới xóa ảnh cũ | Playlist luôn có 1 ảnh hợp lệ |

Giới hạn đã biết (chấp nhận được trong phạm vi dự án, xem mục 16):
- Giới hạn 50 playlist / 200 bài kiểm tra bằng "đếm rồi mới lưu". 2 request đúng cùng lúc ở sát giới hạn có thể vượt 1 đơn vị.
- Bảng `playlists` **không** có cột `version` (optimistic locking). 2 tab cùng sửa thông tin 1 playlist → lần lưu sau ghi đè lần lưu trước.

---

## 15. CẤU HÌNH, DEPLOY VÀ DATABASE

### 15.1. Không có cấu hình riêng

Module Playlist **không** thêm khóa cấu hình mới. Dùng lại:
- `app.upload-dir` (thư mục lưu ảnh, ảnh playlist nằm ở `<upload-dir>/playlists/`).
- `spring.servlet.multipart.max-file-size=100MB` / `max-request-size=110MB` (giới hạn upload tổng).
- Rate limit chung.

### 15.2. Deploy

```
Nhánh tính năng (VD playlist) → Pull Request → merge vào main
→ GitHub Actions tự build và chạy deploy.sh trên VPS
```

⚠️ VPS dùng `spring.jpa.hibernate.ddl-auto=validate`: Hibernate **chỉ kiểm tra**, **không tự tạo bảng**. Với môi trường mới (VPS mới, database mới), phải **tạo 2 bảng `playlists`, `playlist_songs` trước** (SQL ở mục 6.4), nếu không ứng dụng không khởi động được (lỗi thiếu bảng).

### 15.3. Lưu ý khi xóa dữ liệu bằng SQL

- **Xóa hẳn 1 bài hát** (`DELETE FROM songs ...`): phải xóa các dòng trong `playlist_songs` trước, nếu không MySQL báo lỗi khóa ngoại:
  ```sql
  DELETE FROM playlist_songs WHERE song_id = 15;
  ```
  (Bình thường nghệ sĩ "xóa bài" chỉ là xóa mềm `status = DELETED`, dòng vẫn còn → không gặp lỗi này.)
- **Xóa hẳn 1 tài khoản** bằng SQL: phải xóa `playlist_songs` của các playlist của họ → xóa `playlists` → rồi mới xóa `users`. (Bình thường tài khoản chỉ bị xóa mềm.)
- **Xóa playlist bằng SQL**: xóa `playlist_songs` trước, rồi `playlists`. File ảnh bìa riêng phải tự xóa tay trên VPS.

---

## 16. GIỚI HẠN ĐÃ BIẾT VÀ HƯỚNG PHÁT TRIỂN

### 16.1. Giới hạn hiện tại

| # | Giới hạn | Ảnh hưởng | Hướng cải thiện |
|---|---|---|---|
| 1 | Không đổi được thứ tự bài (kéo thả) | Không đưa được 1 bài lên đầu; bỏ rồi thêm lại thì bài về **cuối** | Thêm cột `position` + API sắp xếp lại |
| 2 | Sau khi tạo chỉ thêm được **từng bài 1** | Thêm nhiều bài tốn nhiều request | API thêm nhiều bài 1 lần |
| 3 | Bài bị ẩn / bị xóa vẫn chiếm chỗ trong giới hạn 200 nhưng không hiện ra | Playlist hiện 198 bài mà không thêm được bài mới; người dùng không thấy bài để bỏ | Không đếm bài không phát hành, hoặc API dọn bài không còn phát hành |
| 4 | Danh sách playlist: khoảng 2 câu SQL phụ cho **mỗi** playlist (đếm bài + ảnh dự phòng) | Chậm hơn khi có nhiều playlist (tối đa 50 nên vẫn ổn) | Gom bằng 1 câu `GROUP BY` |
| 5 | Chi tiết playlist: thông tin nghệ sĩ, feat, thể loại của từng bài được tải thêm (truy vấn phụ) | Playlist 200 bài có thể nhiều câu SQL | `JOIN FETCH` / `@EntityGraph` |
| 6 | Không có cột `version` | 2 tab cùng sửa → lần sau ghi đè lần trước | `@Version` như bảng `songs` |
| 7 | Giới hạn 50/200 không nguyên tử | 2 request sát giới hạn đúng cùng lúc có thể vượt 1 | Khóa dòng (`SELECT ... FOR UPDATE`) hoặc đếm bằng Redis |
| 8 | Admin không có công cụ quản lý playlist | Tên / ảnh playlist công khai phản cảm không gỡ được | API Admin ẩn/xóa playlist; lọc từ ngữ khi tạo/sửa tên |
| 9 | Chưa có API hồ sơ công khai của người dùng | Trang cá nhân người khác thiếu tên, ảnh đại diện | `GET /api/users/{userId}/public-profile` |
| 10 | Ảnh bìa dự phòng chỉ xét bài đầu tiên | Bài đầu không có ảnh → playlist không có ảnh dù bài sau có | Lấy bài đầu tiên **có** ảnh; hoặc ghép lưới 4 ảnh như Spotify |
| 11 | Tên playlist được phép trùng | Người dùng dễ nhầm | Có thể giữ nguyên (Spotify cũng cho trùng) |

### 16.2. Ý tưởng mở rộng

- Kéo thả sắp xếp bài, thêm nhiều bài 1 lần.
- Thích / theo dõi playlist của người khác; mục "Playlist đã thích".
- Tìm kiếm playlist công khai.
- Gợi ý bài hát để thêm vào playlist (dựa trên thể loại các bài đang có, kết hợp service AI Python).
- Playlist cộng tác (nhiều người cùng thêm bài).
- Chia sẻ playlist riêng tư bằng link bí mật.
- Lọc từ ngữ không phù hợp ở tên/mô tả playlist (service FastAPI nhỏ).

---

## 17. CÂU HỎI THƯỜNG GẶP

**H: Tạo playlist mà không chọn bài được không?**
Đ: Không. Tạo bắt buộc có ít nhất 1 bài (giống Spotify gợi ý bài lúc tạo). Tạo xong có thể bỏ hết bài.

**H: Playlist mới tạo sao người khác không thấy?**
Đ: Mặc định là **riêng tư**. Gửi `isPublic=true` lúc tạo, hoặc sửa sang công khai sau.

**H: Bài hát trong playlist tự nhiên biến mất?**
Đ: Bài đó không còn phát hành (nghệ sĩ ẩn bài, Admin gỡ bài, nghệ sĩ bị khóa, hoặc bài bị xóa). Khi bài phát hành lại, nó tự xuất hiện lại đúng vị trí cũ.

**H: Ảnh bìa playlist tự đổi dù tôi không làm gì?**
Đ: Playlist không có ảnh riêng thì dùng ảnh của bài **đang phát hành đầu tiên**. Bài đầu bị bỏ hoặc bị ẩn → ảnh tự đổi theo bài kế tiếp. Muốn cố định → tải ảnh riêng.

**H: Playlist hiện 198 bài mà thêm bài mới bị báo đủ 200?**
Đ: Có 2 bài đang bị ẩn/bị xóa vẫn chiếm chỗ (mục 7.1, 16.1 dòng 3).

**H: Sửa tên playlist xong mô tả bị mất?**
Đ: API sửa thay toàn bộ 3 thông tin. Không gửi `description` = xóa mô tả. Phải gửi lại mô tả cũ.

**H: Bỏ 1 bài rồi thêm lại, sao bài đó xuống cuối?**
Đ: Thêm lại là 1 dòng mới với `addedAt` mới. Thứ tự luôn theo thời điểm thêm.

**H: Tôi là ADMIN, sao không sửa/xóa được playlist của người khác?**
Đ: Đúng thiết kế hiện tại: Admin cũng chỉ là "người khác" với playlist. Chưa có API quản trị playlist (mục 16).

**H: Gọi `GET /api/playlists/users/{id của tôi}` sao không thấy playlist riêng tư?**
Đ: API này cho thấy đúng những gì người khác nhìn thấy. Xem đủ → `GET /api/playlists/me`.

**H: Xóa playlist có xóa bài hát không?**
Đ: Không. Chỉ xóa playlist, danh sách bài của nó và ảnh bìa riêng. Bài hát gốc, lượt nghe, các playlist khác không bị ảnh hưởng.

**H: Nghe nhạc trong playlist có được tính lượt nghe không?**
Đ: Có, nếu frontend gọi `POST /api/songs/{songId}/play` khi bắt đầu phát mỗi bài (giống phát ở chỗ khác).

**H: Vì sao tạo playlist dùng form-data còn sửa thông tin dùng JSON?**
Đ: Tạo playlist có thể kèm ảnh bìa (file) → bắt buộc form-data. Sửa thông tin không có file → dùng JSON cho gọn. Đổi ảnh có API riêng (form-data).

**H: `songIds` gửi thế nào trong Postman?**
Đ: Body → form-data → thêm nhiều dòng cùng key `songIds`, mỗi dòng 1 id. Hoặc 1 dòng giá trị `15,22,9`.

**H: Thời gian `createdAt` có số lẻ dài sau giây?**
Đ: Là phần lẻ của giây (micro giây), bình thường. Frontend định dạng lại khi hiển thị.

---

## 18. PHỤ LỤC

### 18.1. Bảng tổng hợp API

| # | Method | URL | Body | Trả về | Lỗi đặc trưng |
|---|---|---|---|---|---|
| 1 | POST | `/api/playlists` | form-data: `name`, `description`, `isPublic`, `songIds`, `cover` | `PlaylistDetailResponse` | `Maximum 50 playlists per account`, `Song does not exist: <id>` |
| 2 | GET | `/api/playlists/me` | — | `PlaylistSummaryResponse[]` | |
| 3 | GET | `/api/playlists/users/{userId}` | — | `PlaylistSummaryResponse[]` | |
| 4 | GET | `/api/playlists/{playlistId}` | — | `PlaylistDetailResponse` | `This playlist is private` |
| 5 | PUT | `/api/playlists/{playlistId}` | JSON: `name`, `description`, `isPublic` | `PlaylistDetailResponse` | `isPublic is required`, 403 |
| 6 | PUT | `/api/playlists/{playlistId}/cover` | form-data: `cover` | `PlaylistDetailResponse` | `File is required`, 403 |
| 7 | DELETE | `/api/playlists/{playlistId}/cover` | — | `PlaylistDetailResponse` | `Playlist has no custom cover`, 403 |
| 8 | POST | `/api/playlists/{playlistId}/songs/{songId}` | — | `PlaylistDetailResponse` | `Song is already in this playlist`, `Maximum 200 songs per playlist`, 403, 409 |
| 9 | DELETE | `/api/playlists/{playlistId}/songs/{songId}` | — | `PlaylistDetailResponse` | `Song is not in this playlist`, 403 |
| 10 | DELETE | `/api/playlists/{playlistId}` | — | `{ "message": "Playlist deleted" }` | 403 |

### 18.2. Bảng message lỗi nghiệp vụ và nơi phát sinh

| Message | Phát sinh ở |
|---|---|
| `Name is required` | `CreatePlaylistRequest`, `UpdatePlaylistRequest` (`@NotBlank`) |
| `Name must not exceed 100 characters` | 2 DTO trên (`@Size`) |
| `Description must not exceed 300 characters` | 2 DTO trên (`@Size`) |
| `isPublic is required` | `UpdatePlaylistRequest` (`@NotNull`) |
| `At least 1 song is required` | `CreatePlaylistRequest` (`@NotEmpty`) + `PlaylistService.loadPlayableSongs` |
| `Maximum 200 songs per playlist` | `CreatePlaylistRequest` (`@Size`) + `loadPlayableSongs` + `addSong` |
| `Maximum 50 playlists per account` | `PlaylistService.createPlaylist` |
| `Song does not exist: <id>` | `PlaylistService.loadPlayableSongs` |
| `Song does not exist` | `PlaylistService.addSong` |
| `Song is already in this playlist` | `PlaylistService.addSong` |
| `Song is not in this playlist` | `PlaylistService.removeSong` |
| `Playlist does not exist` | `getPlaylistDetail`, `getOwnedPlaylist` |
| `This playlist is private` | `getPlaylistDetail` |
| `Playlist has no custom cover` | `PlaylistService.removeCover` |
| `User does not exist` | `PlaylistService.getUser` |
| 403 (chủ playlist) | `PlaylistService.getOwnedPlaylist` (`AccessDeniedException`) |
| Lỗi ảnh (`Image ...`, `Only JPG...`, `Invalid or corrupted image file`) | `FileStorageService.saveImage` |
| `Invalid value for <trường>` | `GlobalExceptionHandler` (sai kiểu ở form-data) |
| `Invalid request data` | `GlobalExceptionHandler` (JSON sai) |
| `Invalid parameter: <tên>` | `GlobalExceptionHandler` (id trong đường dẫn không phải số) |
| `File is required` | `GlobalExceptionHandler` (thiếu key file) |
| `Data already exists` | `GlobalExceptionHandler` (vi phạm UNIQUE) |

### 18.3. Bản đồ "muốn sửa X thì mở file nào"

| Muốn thay đổi | Mở file |
|---|---|
| Thêm / sửa API, đường dẫn | `controller/PlaylistController.java` |
| Quy tắc nghiệp vụ (ai xem, ai sửa, giới hạn...) | `service/PlaylistService.java` |
| Số playlist tối đa | `PlaylistService.MAX_PLAYLISTS_PER_USER` |
| Số bài tối đa | `PlaylistService.MAX_SONGS_PER_PLAYLIST` **và** `@Size(max = 200)` trong `CreatePlaylistRequest` |
| Độ dài tên / mô tả | `@Size` trong 2 DTO request **và** `length` trong `entity/Playlist.java` (+ `ALTER TABLE` trên VPS) |
| Thư mục lưu ảnh bìa | `PlaylistService.COVER_FOLDER` |
| Thứ tự playlist trong danh sách | `PlaylistRepository.findMyPlaylists`, `findPublicPlaylists` (`ORDER BY`) |
| Thứ tự bài trong playlist | `PlaylistSongRepository.findPlayableSongs`, `findPlayableCoverUrls` (`ORDER BY`) |
| Điều kiện "bài đang phát hành" | `SongRepository.PUBLISHED_CONDITION` (ảnh hưởng cả module Song) |
| Cách tính ảnh bìa | `PlaylistMapper.toDetailResponse` + `PlaylistService.resolveCover` |
| Trường JSON trả về | `dto/playlist/*Response.java` + `mapper/PlaylistMapper.java` |
| Thông báo lỗi chung | `exception/GlobalExceptionHandler.java` |

### 18.4. Tài liệu liên quan

- `docs/SONG_MODULE.md` — bài hát, trạng thái bài, điều kiện "đã phát hành", nghe nhạc, ghi lượt nghe.
- `docs/USER_MODULE.md` — đăng ký, đăng nhập, token, hồ sơ.
- `docs/GENRE_FRONTEND.md` — thể loại nhạc (trường `genreId`, `genreName` trong `SongResponse`).
- Swagger: `http://157.66.100.87:8080/swagger-ui.html` — thử API trực tiếp trên trình duyệt.
- Comment trong từng file code của module — giải thích chi tiết từng dòng, viết cho người chưa học Spring.