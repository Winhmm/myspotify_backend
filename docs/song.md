# MODULE SONG — TÀI LIỆU NGHIỆP VỤ VÀ LUỒNG XỬ LÝ

> Dự án: **MySpotify Backend** (Spring Boot 4.1, Java 17 trong `pom.xml` — VPS chạy JDK 25, MySQL, Redis, MongoDB)
> Package gốc: `com.winhmm.myspotify`
> Cập nhật lần cuối: **08/11/2026** — khớp với code trên nhánh `main` sau Pull Request #11.
> Đối tượng đọc: thành viên mới trong nhóm (backend, frontend, tester), **không cần biết trước Spring Boot**.
> Tài liệu đi kèm:
> - `docs/TEST_SONG_POSTMAN.md` — hướng dẫn test từng API bằng Postman.
> - `docs/GENRE_FRONTEND.md` — hướng dẫn riêng cho frontend phần thể loại nhạc.
> - `docs/PLAYLIST_MODULE.md` — module Playlist (dùng lại bài hát của module này).
> - `docs/USER_MODULE.md` — tài khoản, đăng nhập, xin làm Artist.

---

## MỤC LỤC

0. [Có gì mới trong bản cập nhật này](#0-có-gì-mới-trong-bản-cập-nhật-này)
1. [Cách đọc tài liệu này](#1-cách-đọc-tài-liệu-này)
2. [Module Song giải quyết bài toán gì?](#2-module-song-giải-quyết-bài-toán-gì)
3. [Từ điển thuật ngữ](#3-từ-điển-thuật-ngữ)
4. [Danh sách use case và API](#4-danh-sách-use-case-và-api)
5. [Kiến trúc và vị trí các file](#5-kiến-trúc-và-vị-trí-các-file)
6. [Hành trình của 1 request](#6-hành-trình-của-1-request)
7. [Dữ liệu: MySQL, MongoDB và ổ đĩa](#7-dữ-liệu-mysql-mongodb-và-ổ-đĩa)
8. [Trạng thái và vòng đời bài hát](#8-trạng-thái-và-vòng-đời-bài-hát)
9. [Luồng nghiệp vụ của ARTIST](#9-luồng-nghiệp-vụ-của-artist)
10. [Luồng nghiệp vụ của ADMIN](#10-luồng-nghiệp-vụ-của-admin)
11. [Luồng nghiệp vụ của NGƯỜI NGHE](#11-luồng-nghiệp-vụ-của-người-nghe)
12. [Lịch sử nghe gần đây (MongoDB)](#12-lịch-sử-nghe-gần-đây-mongodb)
13. [Thể loại nhạc (Genre)](#13-thể-loại-nhạc-genre)
14. [Canvas — video nền ngắn của bài hát](#14-canvas--video-nền-ngắn-của-bài-hát)
15. [Lời bài hát (Lyrics)](#15-lời-bài-hát-lyrics)
16. [Gợi ý bài hát (Recommendation)](#16-gợi-ý-bài-hát-recommendation)
17. [Lưu trữ file nhạc, ảnh và video](#17-lưu-trữ-file-nhạc-ảnh-và-video)
18. [Bảo mật và phân quyền](#18-bảo-mật-và-phân-quyền)
19. [Xử lý đồng thời (nhiều người thao tác cùng lúc)](#19-xử-lý-đồng-thời-nhiều-người-thao-tác-cùng-lúc)
20. [Thời gian và múi giờ](#20-thời-gian-và-múi-giờ)
21. [Phân trang](#21-phân-trang)
22. [Xử lý lỗi](#22-xử-lý-lỗi)
23. [Hướng dẫn riêng cho Frontend](#23-hướng-dẫn-riêng-cho-frontend)
24. [Cấu hình, môi trường và deploy](#24-cấu-hình-môi-trường-và-deploy)
25. [Giới hạn đã biết và hướng phát triển](#25-giới-hạn-đã-biết-và-hướng-phát-triển)
26. [Câu hỏi thường gặp](#26-câu-hỏi-thường-gặp)
27. [Phụ lục](#27-phụ-lục)

---

## 0. CÓ GÌ MỚI TRONG BẢN CẬP NHẬT NÀY

Bản trước (15/10/2026) chỉ mô tả phần đăng bài, kiểm duyệt, nghe nhạc và lịch sử nghe. Từ đó tới nay module Song đã có thêm:

| # | Thay đổi | API / file liên quan | Đọc ở mục |
|---|---|---|---|
| 1 | **Thể loại nhạc**: 25 thể loại có sẵn, đăng bài **bắt buộc** chọn 1 thể loại, không đổi được sau khi đăng, lọc bài theo thể loại | `GET /api/genres`, `POST /api/artist/songs` (thêm `genreId`), `GET /api/songs?genreId=` | 13 |
| 2 | **Canvas**: video MP4 ngắn 7–10 giây chạy lặp phía sau trình phát, giống Spotify | `PUT` / `DELETE /api/artist/songs/{songId}/canvas` | 14 |
| 3 | **Lời bài hát**: lời thường hoặc lời có mốc thời gian (chạy theo nhạc, kiểu karaoke) | `GET /api/songs/{songId}/lyrics`, `GET` / `PUT` / `DELETE /api/artist/songs/{songId}/lyrics` | 15 |
| 4 | **Gợi ý bài hát**: hàng chờ phát tiếp, bài khác của cùng nghệ sĩ, bài theo thể loại (có service AI viết bằng Python) | `GET /api/songs/{songId}/recommendations`, `.../recommendations/artist`, `GET /api/recommendations/genres` | 16 |
| 5 | `SongResponse` và `SongManageResponse` có thêm 3 trường: `canvasUrl`, `genreId`, `genreName` | Mọi API trả bài hát | 11, 13, 14 |
| 6 | **Lịch sử nghe ghi bất đồng bộ** (`@Async`): API `/play` trả kết quả ngay, không chờ MongoDB | `ListeningHistoryService.record` | 12.3 |
| 7 | **Xung đột khi 2 người cùng sửa 1 bài** giờ trả **409** kèm message rõ ràng (trước đây là việc "cần kiểm tra") | `GlobalExceptionHandler` | 19.1, 22 |
| 8 | **Quyền vào web** (`accessStatus`): tài khoản phải được chủ dự án cho phép (`ACCEPT`) mới đăng nhập và gọi API được | `User.accessStatus`, `CustomUserDetailsService` | 6, 18 |
| 9 | **Liên kết với Playlist**: bài hát được thêm vào playlist; bài bị ẩn/gỡ tự biến mất khỏi playlist | `PLAYLIST_MODULE.md` | 7.7, 9.8 |
| 10 | **Deploy tự động**: merge Pull Request vào `main` là GitHub Actions tự build và deploy lên VPS | `.github/workflows` | 24.4 |

Số mục cũ **13 → 23** được đánh lại thành **17 → 27** (chèn 4 mục mới 13–16 ở giữa).

---

## 1. CÁCH ĐỌC TÀI LIỆU NÀY

Tài liệu dài, không cần đọc hết một lần. Chọn lộ trình theo vai trò:

| Bạn là | Đọc theo thứ tự | Có thể bỏ qua |
|---|---|---|
| **Người mới hoàn toàn** | 0 → 2 → 3 → 4 → 8 → 9 → 10 → 11 → 13 → 16 | 19, 24 |
| **Frontend** | 0 → 2 → 4 → 8 → **23** → 11 → 13 → 14 → 15 → 16 → 9 → 22 | 5, 6, 17, 19, 24 |
| **Tester** | 0 → 2 → 4 → 8 → 9 → 10 → 11 → 12 → 13 → 14 → 15 → 16 → 22 → rồi mở `TEST_SONG_POSTMAN.md` | 5, 6, 24 |
| **Backend sắp sửa code** | Đọc hết, đặc biệt 5, 6, 7, 15, 16, 17, 18, 19 | |

Quy ước trong tài liệu:
- `chữ như thế này` là tên trong code: class, method, cột database, tên trường JSON.
- **400 `"Song does not exist"`** nghĩa là server trả mã HTTP 400 kèm JSON `{ "message": "Song does not exist" }`.
- "Người nghe" = bất kỳ ai đã đăng nhập (USER, ARTIST hay ADMIN đều có thể nghe nhạc).
- Base URL server: `http://157.66.100.87:8080`. Mọi API (trừ file `/uploads/**`) đều cần header `Authorization: Bearer <token>`.

---

## 2. MODULE SONG GIẢI QUYẾT BÀI TOÁN GÌ?

### 2.1. Một câu

> Cho phép **nghệ sĩ đăng nhạc**, **Admin kiểm duyệt**, và **mọi người nghe nhạc đã được duyệt**.

### 2.2. Câu chuyện minh họa

1. **Mây** là một ca sĩ. Mây đã có tài khoản MySpotify và được Admin cấp vai trò ARTIST (xem module User).
2. Mây đăng bài **"Chiều Mưa"**: chọn file `chieu-mua.mp3`, ảnh bìa, chọn thể loại **Ballad** (bắt buộc), mời bạn **Sóng** hát chung (feat), hẹn phát hành lúc **20:00 ngày 24/12**.
3. Bài vào trạng thái **chờ duyệt**. Chưa ai nghe được. Trong lúc chờ, Mây tải thêm 1 **canvas** (video 8 giây cảnh mưa rơi) và dán **lời bài hát có mốc thời gian** để lời chạy theo nhạc.
4. **Admin** mở danh sách bài chờ duyệt, nghe thử, thấy ảnh bìa không phù hợp → **từ chối** với lý do *"Ảnh bìa không phù hợp"*.
5. Mây thấy lý do, **đổi ảnh bìa**. Bài tự động quay lại **chờ duyệt**.
6. Admin **duyệt**. Bài đã được duyệt nhưng vẫn **chưa phát hành** vì chưa tới 20:00 ngày 24/12.
7. Đúng 20:00 ngày 24/12, bài **tự xuất hiện** với người nghe. Không ai phải bấm gì.
8. **Lan** (người nghe) tìm "chiều mưa", bấm nghe. Lượt nghe tăng lên 1. Bài được ghi vào **lịch sử nghe gần đây** của Lan. Phía sau trình phát, canvas cảnh mưa chạy lặp lại; bấm "Lời bài hát" thì lời chạy theo nhạc.
9. Lan tìm "Sóng" cũng ra bài này (vì Sóng được feat). Trang nghệ sĩ của Sóng cũng hiển thị bài này. Lan mở trang **Genres → Ballad** cũng thấy bài này.
10. Hết bài, trình phát **tự phát tiếp**: tối đa 3 bài khác của Mây (ưu tiên cùng thể loại Ballad), sau đó là bài Ballad của nghệ sĩ khác. Lan thích bài này nên bấm **"Thêm vào playlist"** (xem `PLAYLIST_MODULE.md`).
11. Một thời gian sau, Mây muốn tạm cất bài → **ẩn bài**. Bài biến mất với người nghe (và khỏi playlist của Lan), lượt nghe vẫn giữ nguyên. Mây **hiện lại** thì bài trở lại như cũ, kể cả trong playlist.
12. Có người khiếu nại bản quyền → Admin **gỡ bài** kèm lý do. Mây sửa lại (hoặc đổi ảnh bìa, đổi canvas) thì bài lại vào hàng chờ duyệt.
13. Mây không muốn giữ bài nữa → **xóa bài**. File nhạc, ảnh bìa, canvas bị xóa khỏi server.

Toàn bộ module là để làm được 13 bước trên một cách **an toàn, đúng quyền và không lỗi khi nhiều người dùng cùng lúc**.

### 2.3. Ba vai trò

| Vai | Làm được gì trong module Song |
|---|---|
| **ARTIST** (nghệ sĩ) | Đăng bài (chọn thể loại), xem bài của mình, sửa tên, đổi danh sách feat, đổi ảnh bìa, đổi/gỡ canvas, thêm/sửa/xóa lời bài hát, đổi lịch phát hành, ẩn/hiện, xóa, tìm nghệ sĩ khác để feat |
| **ADMIN** (quản trị) | Xem mọi bài, lọc theo trạng thái, duyệt, từ chối, gỡ bài đã duyệt |
| **Người nghe** (mọi tài khoản đã đăng nhập) | Xem danh sách bài (lọc theo thể loại), xem danh sách thể loại, tìm kiếm, xem chi tiết, nghe nhạc, xem canvas, xem lời bài hát, xem trang nghệ sĩ, xem lịch sử nghe gần đây, nhận gợi ý bài hát |

ARTIST và ADMIN **cũng là người nghe**: họ dùng được mọi chức năng nghe nhạc.

### 2.4. Bảy quy tắc nghiệp vụ cốt lõi

Nếu chỉ nhớ được 7 điều, hãy nhớ 7 điều này:

1. **Không có gì được phát công khai nếu Admin chưa duyệt.**
2. **Người nghe chỉ thấy bài "đã phát hành"**: đã duyệt + đã tới giờ phát hành + nghệ sĩ chính còn hoạt động. Quy tắc này áp dụng ở **mọi nơi**: danh sách, tìm kiếm, thể loại, lời bài hát, gợi ý, lịch sử nghe, playlist.
3. **Artist chỉ đụng được vào bài của chính mình.** Bài của người khác thì coi như không tồn tại.
4. **Bị từ chối hoặc bị gỡ thì luôn có lý do**, và Artist sửa lại (tên/feat, ảnh bìa, canvas) là bài tự động được gửi duyệt lại.
5. **Lượt nghe không thể bị "cày"**: cùng 1 tài khoản nghe cùng 1 bài chỉ được tính 1 lượt mỗi 30 giây.
6. **Mỗi bài có đúng 1 thể loại**, chọn lúc đăng và **không đổi được** sau đó.
7. **Chức năng phụ hỏng không được làm hỏng chức năng chính**: MongoDB lỗi thì vẫn nghe nhạc được (chỉ mất lịch sử); service AI Python lỗi thì vẫn có gợi ý (lấy bài phổ biến thay thế).

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
| **`@Async`** | Chạy 1 hàm ở **luồng (thread) riêng**, người gọi không phải chờ hàm đó chạy xong. Dùng để ghi lịch sử nghe |
| **`RestClient`** | Công cụ của Spring để **backend gọi sang 1 server khác** qua HTTP. Dùng để gọi service AI Python |
| **Fallback** | "Phương án dự phòng": cách làm thay thế khi cách chính bị lỗi |

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
| **Thể loại (`genre`)** | Dòng nhạc của bài (Pop, Ballad, Rap...). Mỗi bài đúng 1 thể loại (mục 13) |
| **Canvas** | Video MP4 ngắn 7–10 giây, không tiếng, chạy lặp phía sau trình phát (mục 14) |
| **Lời thường** | Lời bài hát chỉ có chữ, không có mốc thời gian |
| **Lời có mốc thời gian** (synced / LRC) | Mỗi dòng có `[phút:giây.xx]` ở đầu → frontend tô sáng đúng dòng đang hát (mục 15) |
| **Gợi ý phát tiếp** | Danh sách bài tự phát khi hết bài đang nghe (mục 16.2) |
| **Service AI Python** | 1 server riêng viết bằng Python (do bạn trong nhóm làm), backend gọi sang để lấy gợi ý theo thể loại (mục 16.4) |
| **Quyền vào web (`accessStatus`)** | `ACCEPT` / `REFUSE`. Tài khoản mới đăng ký mặc định `REFUSE`, chủ dự án đổi sang `ACCEPT` thì mới đăng nhập được (xem `USER_MODULE.md`) |

---

## 4. DANH SÁCH USE CASE VÀ API

Các chức năng có mã UC là những use case đã có từ bản thiết kế ban đầu. Các chức năng thêm sau (thể loại, canvas, lời bài hát, gợi ý) chưa được đánh mã, ghi `—`.

| Mã | Tên | Ai | API |
|---|---|---|---|
| UC20 | Xem danh sách bài hát (lọc theo thể loại nếu muốn) | Người nghe | `GET /api/songs` (`?genreId=`) |
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
| — | Xem danh sách thể loại | Người nghe | `GET /api/genres` |
| — | Đổi canvas | ARTIST | `PUT /api/artist/songs/{songId}/canvas` |
| — | Gỡ canvas | ARTIST | `DELETE /api/artist/songs/{songId}/canvas` |
| — | Xem lời bài hát (người nghe) | Người nghe | `GET /api/songs/{songId}/lyrics` |
| — | Xem lời bài hát của mình | ARTIST | `GET /api/artist/songs/{songId}/lyrics` |
| — | Thêm / sửa lời bài hát | ARTIST | `PUT /api/artist/songs/{songId}/lyrics` |
| — | Xóa lời bài hát | ARTIST | `DELETE /api/artist/songs/{songId}/lyrics` |
| — | Gợi ý phát tiếp | Người nghe | `GET /api/songs/{songId}/recommendations` |
| — | Bài khác của cùng nghệ sĩ | Người nghe | `GET /api/songs/{songId}/recommendations/artist` |
| — | Gợi ý theo thể loại (AI) | Người nghe | `GET /api/recommendations/genres` |

Tổng cộng **29 API** (bảng đầy đủ ở mục 27.1).

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
├── MyspotifyApplication.java            ← Có @EnableAsync (bật ghi lịch sử bất đồng bộ)
├── client/
│   └── RecommendClient.java             ← Gọi sang service AI Python bằng RestClient (MỚI)
├── controller/
│   ├── SongController.java              ← API người nghe (UC20–UC25)
│   ├── ArtistSongController.java        ← API nghệ sĩ (UC26–UC33, UC37, canvas)
│   ├── AdminSongController.java         ← API quản trị (UC34–UC36)
│   ├── ListeningHistoryController.java  ← API lịch sử nghe
│   ├── GenreController.java             ← API danh sách thể loại (MỚI)
│   ├── LyricsController.java            ← API lời bài hát, cả người nghe lẫn Artist (MỚI)
│   └── RecommendationController.java    ← API gợi ý bài hát (MỚI)
├── service/
│   ├── SongService.java                 ← Nghiệp vụ người nghe + ghi lượt nghe + danh sách thể loại
│   ├── ArtistSongService.java           ← Nghiệp vụ nghệ sĩ (kể cả canvas)
│   ├── AdminSongService.java            ← Nghiệp vụ quản trị
│   ├── FileStorageService.java          ← Lưu / kiểm tra / xóa file nhạc, ảnh, video
│   ├── ListeningHistoryService.java     ← Ghi (@Async) và đọc lịch sử nghe (MongoDB)
│   ├── LyricsService.java               ← Kiểm tra, chuẩn hóa, lưu, đọc lời bài hát (MỚI)
│   └── RecommendationService.java       ← Thuật toán gợi ý + gọi AI + phương án dự phòng (MỚI)
├── repository/
│   ├── SongRepository.java              ← Truy vấn bảng songs (MySQL)
│   ├── GenreRepository.java             ← Truy vấn bảng genres (MỚI)
│   ├── SongLyricsRepository.java        ← Truy vấn bảng song_lyrics (MỚI)
│   └── ListeningHistoryRepository.java  ← Truy vấn collection listening_history (MongoDB)
├── entity/
│   ├── Song.java                        ← Ánh xạ bảng songs (thêm genre, canvasUrl)
│   ├── Genre.java                       ← Ánh xạ bảng genres (MỚI)
│   └── SongLyrics.java                  ← Ánh xạ bảng song_lyrics (MỚI)
├── document/
│   └── ListeningHistory.java            ← Ánh xạ collection listening_history
├── enums/
│   └── SongStatus.java                  ← PENDING, APPROVED, REJECTED, HIDDEN, DELETED
├── mapper/
│   └── SongMapper.java                  ← Song → SongResponse / SongManageResponse / PageResponse, Genre → GenreResponse
└── dto/
    ├── song/
    │   ├── UploadSongRequest.java       ← Dữ liệu đăng bài (form-data, có genreId)
    │   ├── UpdateSongRequest.java       ← Sửa tên + feat (JSON)
    │   ├── UpdateReleaseRequest.java    ← Đổi lịch (JSON)
    │   ├── RejectSongRequest.java       ← Lý do từ chối / gỡ (JSON)
    │   ├── SongResponse.java            ← Bài hát cho người nghe (rút gọn)
    │   ├── SongManageResponse.java      ← Bài hát cho Artist / Admin (đầy đủ)
    │   ├── ArtistSummary.java           ← { id, artistName } của 1 nghệ sĩ
    │   ├── ArtistSongsResponse.java     ← Trang nghệ sĩ = thông tin + danh sách bài
    │   ├── ListeningHistoryResponse.java← { song, playedAt }
    │   ├── GenreResponse.java           ← { id, name } của 1 thể loại (MỚI)
    │   └── PageResponse.java            ← Khung phân trang chung
    ├── lyrics/                          (MỚI)
    │   ├── LyricsRequest.java           ← { content } gửi lên khi lưu lời
    │   ├── LyricsResponse.java          ← { songId, synced, content, lines, updatedAt }
    │   └── LyricLine.java               ← { timeMs, text } của 1 dòng lời
    └── recommend/                       (MỚI)
        └── RecommendApiResponse.java    ← Đọc kết quả service AI Python trả về
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
| `security/CustomUserDetailsService.java` | Chặn tài khoản không ACTIVE hoặc chưa được cho phép vào web (`accessStatus`) |

Module **phụ thuộc vào** module Song (dùng lại bài hát, không làm module Song thay đổi):

| File | Dùng gì của module Song |
|---|---|
| `service/PlaylistService.java`, `repository/PlaylistSongRepository.java` | `SongRepository.PUBLISHED_CONDITION`, `findPublishedById`, `findPublishedByIds`, `SongMapper.toSongResponse` (xem `PLAYLIST_MODULE.md`) |

### 5.3. Ai gọi ai

```
SongController ───────────► SongService ──────────┬──► SongRepository ──► MySQL
                                                  ├──► UserRepository ──► MySQL
                                                  ├──► GenreRepository ─► MySQL
                                                  ├──► Redis (chống spam lượt nghe)
                                                  └──► ListeningHistoryService (@Async)

GenreController ──────────► SongService.getGenres ──► GenreRepository ──► MySQL

ArtistSongController ─────► ArtistSongService ────┬──► SongRepository
       │                                          ├──► UserRepository
       │                                          ├──► GenreRepository
       └──► UserService (lấy id từ email)         └──► FileStorageService ──► Ổ đĩa

AdminSongController ──────► AdminSongService ─────────► SongRepository

LyricsController ─────────► LyricsService ────────┬──► SongLyricsRepository ──► MySQL
       │                                          ├──► SongRepository / UserRepository
       └──► UserService                           └──► SongService.getPublishedSong

RecommendationController ─► RecommendationService ┬──► SongRepository ──► MySQL
                                                  └──► RecommendClient ──► Service AI Python (HTTP)

ListeningHistoryController ► ListeningHistoryService ─┬► ListeningHistoryRepository ► MongoDB
       │                                              └► SongRepository ► MySQL
       └──► UserService

Mọi Controller / Service trả bài hát ──► SongMapper (đổi entity sang DTO trước khi trả về)
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
     ├─ Đọc user từ MySQL, kiểm tra tài khoản còn hoạt động (ACTIVE)
     │     và được phép vào web (accessStatus = ACCEPT) → không đạt thì về sau trả 401
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
| **MySQL** | Thông tin bài hát, quan hệ feat, thể loại, lời bài hát | Dữ liệu chính, cần quan hệ chặt chẽ, truy vấn phức tạp |
| **MongoDB** | Từng lượt nghe (lịch sử) | Dữ liệu nhiều, chỉ ghi thêm, cấu trúc đơn giản, tự xóa sau 90 ngày |
| **Redis** | Khóa chống spam lượt nghe 30 giây | Dữ liệu tạm, tự hết hạn, cần cực nhanh |
| **Ổ đĩa server** | File `.mp3`, `.wav`, ảnh bìa, video canvas `.mp4` | File lớn, không nên nhét vào database |

### 7.1. Bảng `songs` (MySQL)

| Cột | Kiểu | Bắt buộc | Ý nghĩa |
|---|---|---|---|
| `id` | BIGINT, tự tăng | Có | Mã bài hát |
| `artist_id` | BIGINT → `users.id` | Có | Artist chính (chủ bài) |
| `genre_id` | BIGINT → `genres.id` | Không (trong database) | Thể loại (**MỚI**). Bài đăng mới **bắt buộc** có; bài cũ đăng trước khi có tính năng có thể `NULL` |
| `title` | VARCHAR(150) | Có | Tên bài |
| `audio_url` | VARCHAR(255) | Có | Đường dẫn file nhạc, VD `/uploads/songs/audio/3f2a....mp3` |
| `cover_url` | VARCHAR(255) | Không | Đường dẫn ảnh bìa, có thể `NULL` |
| `canvas_url` | VARCHAR(255) | Không | Đường dẫn video canvas (**MỚI**), VD `/uploads/songs/canvas/5d1e....mp4`, có thể `NULL` |
| `duration_seconds` | INT | Có | Thời lượng (giây), **server tự đo** từ file |
| `play_count` | BIGINT | Có | Lượt nghe, mặc định 0 |
| `status` | ENUM | Có | `PENDING` / `APPROVED` / `REJECTED` / `HIDDEN` / `DELETED` |
| `release_at` | DATETIME | Không | Lịch phát hành. `NULL` = phát hành ngay khi được duyệt |
| `reject_reason` | VARCHAR(255) | Không | Lý do từ chối / gỡ |
| `created_at` | DATETIME | Có | Thời điểm đăng, **không bao giờ đổi** |
| `updated_at` | DATETIME | Có | Lần sửa gần nhất, tự cập nhật |
| `version` | BIGINT | Có | Số phiên bản, tự tăng mỗi lần sửa (xem mục 19) |

**Index (mục lục) để tìm nhanh:**

| Tên index | Cột | Phục vụ truy vấn |
|---|---|---|
| `idx_songs_status_release` | `status, release_at` | "Lấy bài APPROVED đã tới giờ phát hành" (người nghe) |
| `idx_songs_artist_status` | `artist_id, status` | "Lấy bài của Artist X" |

**Hai điểm đặc biệt trong entity `Song`:**

1. `play_count` được khai báo `updatable = false`. Nghĩa là khi gọi `songRepository.save(song)` (VD lúc Artist sửa tên), Hibernate **không ghi đè** cột này. Lượt nghe chỉ được tăng bằng 1 câu lệnh riêng `UPDATE songs SET play_count = play_count + 1`. Lý do: tránh trường hợp Artist sửa tên đúng lúc có người nghe, lượt nghe bị ghi đè bằng số cũ (xem mục 19).

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

### 7.2a. Bảng `genres` (MySQL) — MỚI

Mỗi dòng = 1 thể loại nhạc.

| Cột | Kiểu | Bắt buộc | Ý nghĩa |
|---|---|---|---|
| `id` | BIGINT, tự tăng | Có | Mã thể loại |
| `name` | VARCHAR(50), **UNIQUE** | Có | Tên thể loại, không trùng |

- Có sẵn **25 thể loại**, được thêm 1 lần bằng câu `INSERT` chạy tay trong MySQL (không có API thêm/sửa/xóa thể loại).
- Thêm thể loại mới sau này: chỉ cần chạy thêm 1 câu, **không phải sửa code**:
  ```sql
  INSERT INTO genres (name) VALUES ('Lofi');
  ```
- Vì sao dùng **bảng** mà không dùng **enum** như `SongStatus`? Danh sách thể loại có thể thêm bớt theo thời gian, code không cần biết từng giá trị. Dùng enum thì thêm 1 thể loại = sửa code, build lại, chạy `ALTER TABLE`.
- `id` của thể loại **không theo thứ tự** và có thể **khác nhau giữa máy local và VPS** → frontend không được viết cứng id.

### 7.2b. Bảng `song_lyrics` (MySQL) — MỚI

Mỗi dòng = lời của 1 bài hát.

| Cột | Kiểu | Bắt buộc | Ý nghĩa |
|---|---|---|---|
| `id` | BIGINT, tự tăng | Có | Mã dòng |
| `song_id` | BIGINT → `songs.id`, **UNIQUE** | Có | Bài hát. UNIQUE → **mỗi bài tối đa 1 bản lời** |
| `content` | TEXT | Có | Nội dung lời **đã chuẩn hóa** (mục 15.4) |
| `updated_at` | DATETIME | Có | Lần lưu gần nhất (tự gán mỗi lần lưu) |

Vì sao tách bảng riêng mà không thêm cột `lyrics` vào `songs`?
- Lời bài hát dài (tới 10.000 ký tự). Mỗi lần lấy danh sách bài (rất thường xuyên) không cần tải lời theo.
- Quan hệ 1 - 1 (`@OneToOne`): 1 bài có 0 hoặc 1 bản lời.

### 7.3. Sơ đồ quan hệ

```
                                 ┌──────────────┐
                                 │   genres     │   1 thể loại có nhiều bài
                                 │──────────────│
                                 │ id, name     │
                                 └──────┬───────┘
                                        │ 1
                                        │ genre_id
 ┌──────────────┐ 1          N ┌────────┴─────┐ 1        0..1 ┌──────────────┐
 │    users     │──────────────│    songs     │───────────────│ song_lyrics  │  1 bài có tối đa 1 bản lời
 │──────────────│  artist_id   │──────────────│    song_id    │──────────────│
 │ id           │              │ id           │               │ song_id (UQ) │
 │ role         │              │ artist_id    │               │ content      │
 │ account_status│             │ genre_id     │               └──────────────┘
 │ artist_name  │              │ canvas_url   │
 └──────┬───────┘              │ status ...   │──────────────┐
        │ 1                    └──────┬───────┘              │ 1
        │                             │ 1                    │
        │            N ┌──────────────┴─────────┐ N    N ┌───┴──────────────┐
        └──────────────│ song_featured_artists  │        │ playlist_songs   │  bài nằm trong playlist
             artist_id │────────────────────────│        │ (module Playlist)│
                       │ song_id, artist_id     │        └──────────────────┘
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
    ├── covers/
    │   ├── 9b1c4d7f-....jpg
    │   └── a2b3c4d5-....webp
    └── canvas/                         (MỚI)
        └── 5d1e7a2b-....mp4
```
(Ảnh bìa playlist nằm ở `<app.upload-dir>/playlists/`, ảnh đại diện người dùng ở `<app.upload-dir>/avatars/` — của module khác.)
- Local: `app.upload-dir=uploads` (thư mục `uploads` trong project).
- VPS: `app.upload-dir=/home/myspotify/uploads`.
- Tên file luôn là **UUID ngẫu nhiên** + đuôi, không dùng tên gốc người dùng đặt.
- Database chỉ lưu **đường dẫn** (`/uploads/songs/audio/...`), không lưu nội dung file.

### 7.7. Liên kết với module Playlist

Bảng `playlist_songs` (module Playlist) có cột `song_id` là **khóa ngoại** trỏ tới `songs.id`. Hệ quả cho module Song:

| Tình huống | Ảnh hưởng tới playlist |
|---|---|
| Bài bị ẩn, bị gỡ, nghệ sĩ bị khóa | Bài **tự biến mất** khỏi mọi playlist (playlist chỉ hiện bài đang phát hành). Dòng trong `playlist_songs` vẫn giữ |
| Bài được hiện lại / mở khóa nghệ sĩ | Bài **tự hiện lại đúng vị trí cũ** trong playlist |
| Artist **xóa** bài (xóa mềm, `status = DELETED`) | Bài biến mất khỏi playlist; dòng `playlist_songs` vẫn còn (vẫn chiếm 1 chỗ trong giới hạn 200 bài/playlist) |
| Xóa **hẳn** 1 bài bằng SQL (`DELETE FROM songs`) | Phải xóa các dòng `playlist_songs` (và `song_lyrics`, `song_featured_artists`) của bài đó **trước**, nếu không MySQL báo lỗi khóa ngoại |

Câu SQL dọn trước khi xóa hẳn bài 15 (chỉ dùng khi thật sự cần):
```sql
DELETE FROM playlist_songs WHERE song_id = 15;
DELETE FROM song_lyrics WHERE song_id = 15;
DELETE FROM song_featured_artists WHERE song_id = 15;
DELETE FROM songs WHERE id = 15;
```

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
| **Artist** đổi canvas | ✔ | ✔ | ✔ | ✔ → **PENDING** | ✔ | ✘ |
| **Artist** gỡ canvas | ✔ | ✔ | ✔ | ✔ (vẫn REJECTED) | ✔ | ✘ |
| **Artist** thêm/sửa/xóa lời | ✔ | ✔ | ✔ | ✔ (vẫn REJECTED) | ✔ | ✘ |
| **Artist** đổi thể loại | ✘ (không có API) | ✘ | ✘ | ✘ | ✘ | ✘ |
| **Artist** đổi lịch | ✔ | ✔ | ✘ Cannot change the schedule of a released song | ✔ (vẫn REJECTED) | ✘ nếu đã từng phát hành | ✘ |
| **Artist** ẩn | ✘ Only approved songs can be hidden | ✔ → HIDDEN | ✔ → HIDDEN | ✘ | ✘ | ✘ |
| **Artist** hiện | ✘ Only hidden songs can be shown again | ✘ | ✘ | ✘ | ✔ → APPROVED | ✘ |
| **Artist** xóa | ✔ → DELETED | ✔ | ✔ | ✔ | ✔ | ✘ |
| **Admin** duyệt | ✔ → APPROVED | ✘ Only pending songs can be approved | ✘ | ✘ | ✘ | ✘ Song does not exist |
| **Admin** từ chối | ✔ → REJECTED | ✘ Only pending songs can be rejected | ✘ | ✘ | ✘ | ✘ |
| **Admin** gỡ | ✘ Only approved songs can be removed | ✔ → REJECTED | ✔ → REJECTED | ✘ | ✔ → REJECTED | ✘ |
| **Người nghe** thấy (kể cả lời, canvas, gợi ý, playlist) | ✘ | ✘ | ✔ | ✘ | ✘ | ✘ |

Ghi chú: đổi canvas, sửa lời bài hát ở bài **đang APPROVED** không cần duyệt lại (giống sửa tên, đổi ảnh — xem mục 25.1 dòng 3).

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

`DELETE /api/artist/songs/{id}` không xóa dòng trong bảng `songs`, chỉ đặt `status = DELETED` (nhưng **có** xóa file nhạc, ảnh bìa, canvas trên ổ đĩa). Lý do:
- Admin vẫn tra cứu được (`?status=DELETED`) khi có khiếu nại.
- Lịch sử nghe trong MongoDB vẫn trỏ tới `songId` cũ mà không lỗi (bài DELETED tự bị lọc khỏi kết quả).
- Không phải xử lý dây chuyền xóa các bảng `song_featured_artists`, `song_lyrics`, `playlist_songs` (đều có khóa ngoại trỏ tới bài).

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
| `genreId` (**MỚI**) | **Có** | `@NotNull`, phải là số | Thể loại phải tồn tại trong bảng `genres` (mục 13) |
| `audio` | Có | `@NotNull` | Đuôi `.mp3`/`.wav`, dung lượng, file nhạc thật, đo thời lượng |
| `cover` | Không | — | JPG/PNG/WEBP, ≤ 2MB, ảnh thật |
| `featuredArtistIds` | Không | Tối đa 5 phần tử | Bỏ trùng, bỏ ô trống, không tự feat, phải là Artist đang hoạt động |
| `releaseAt` | Không | Đúng định dạng `yyyy-MM-ddTHH:mm:ss` | Phải ở tương lai |

**Các bước xử lý** (`ArtistSongService.uploadSong`) — **thứ tự rất quan trọng**:

```
 1. Kiểm tra Artist còn hoạt động
 2. Kiểm tra releaseAt ở tương lai (nếu có)
 3. Kiểm tra danh sách feat hợp lệ
 4. Kiểm tra thể loại: genreId tồn tại trong bảng genres       (MỚI)
        ▲ 4 bước trên chỉ đọc database, CHƯA lưu file nào.
        │ Sai ở đây → báo lỗi ngay, không tốn công lưu file rồi phải xóa.
 5. Lưu file nhạc xuống ổ đĩa (đã kiểm tra đuôi + dung lượng)
 ┌── try ─────────────────────────────────────────────────────┐
 │ 6. Đo thời lượng file nhạc (file hỏng / giả → lỗi)          │
 │ 7. Nếu có ảnh bìa: kiểm tra + lưu ảnh                        │
 │ 8. Tạo Song: status = PENDING, playCount = 0, genre = ...    │
 │ 9. Lưu vào MySQL                                             │
 └── catch (bất kỳ lỗi nào ở 6–9) ────────────────────────────┘
       → Xóa file nhạc VÀ ảnh vừa lưu, rồi báo lỗi
10. Trả về SongManageResponse

 Canvas và lời bài hát KHÔNG gửi lúc đăng bài.
 Đăng xong, Artist thêm bằng API riêng (mục 14, 15).
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
  "canvasUrl": null,
  "durationSeconds": 215, "playCount": 0,
  "status": "PENDING", "released": false,
  "releaseAt": "2026-12-24T20:00:00", "rejectReason": null,
  "artistId": 5, "artistName": "Mây Chiều",
  "featuredArtists": [ { "id": 12, "artistName": "Sóng Nhỏ" } ],
  "genreId": 5, "genreName": "Ballad",
  "createdAt": "2026-10-15T16:30:00", "updatedAt": "2026-10-15T16:30:00"
}
```

Đầy đủ các trường của `SongManageResponse`: `id`, `title`, `audioUrl`, `coverUrl`, `canvasUrl`, `durationSeconds`, `playCount`, `status`, `released`, `releaseAt`, `rejectReason`, `artistId`, `artistName`, `featuredArtists`, `genreId`, `genreName`, `createdAt`, `updatedAt`.

**Lỗi có thể gặp:**

| Tình huống | Kết quả |
|---|---|
| Thiếu / rỗng title | 400 `Title is required` |
| Title > 150 ký tự | 400 `Title must not exceed 150 characters` |
| Thiếu `genreId` (hoặc sai tên key, VD `genre_id`) | 400 `Genre is required` |
| `genreId` không phải số | 400 `Invalid value for genreId` |
| `genreId` không tồn tại | 400 `Genre does not exist` |
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
- Mỗi phần tử là `SongManageResponse` đầy đủ: có `status`, `released`, `rejectReason`, `canvasUrl`, `genreId`, `genreName`.
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

**Sửa bài đang APPROVED (đã phát hành)** → vẫn APPROVED, **không cần duyệt lại**, người nghe thấy tên mới ngay. (Đây là quyết định thiết kế hiện tại, xem mục 25.)

**Không đổi được thể loại.** `UpdateSongRequest` không có trường `genreId`; gửi kèm thì server bỏ qua. Lý do: thể loại ảnh hưởng tới trang Genres và thuật toán gợi ý; cho đổi tùy ý dễ bị lợi dụng để "chen" bài vào thể loại đang hot. Muốn đổi thể loại → xóa bài và đăng lại.

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
3. Xóa file nhạc, ảnh bìa và video canvas khỏi ổ đĩa
```
Được xóa ở **mọi trạng thái** (trừ đã DELETED). Không có bước xác nhận ở backend — frontend nên hỏi lại "Bạn chắc chắn muốn xóa?" trước khi gọi.

Thứ tự **lưu database trước, xóa file sau** để: nếu lưu database lỗi thì file vẫn còn, bài vẫn dùng được bình thường.

Những thứ **không** bị xóa theo (vì bài chỉ bị xóa mềm):
- Dòng lời bài hát trong `song_lyrics` (không ai đọc được nữa vì bài đã DELETED).
- Các dòng trong `playlist_songs` (bài tự biến mất khỏi playlist).
- Lịch sử nghe trong MongoDB (tự bị lọc lúc đọc, tự xóa sau 90 ngày).

### 9.9. Đổi / gỡ canvas

**API:** `PUT /api/artist/songs/{songId}/canvas` (form-data, key `canvas`) và `DELETE /api/artist/songs/{songId}/canvas`.
Chi tiết đầy đủ ở **mục 14**.

### 9.10. Lời bài hát

**API:** `GET` / `PUT` / `DELETE /api/artist/songs/{songId}/lyrics`.
Chi tiết đầy đủ ở **mục 15**.

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
  "canvasUrl": "/uploads/songs/canvas/5d1e7a2b-....mp4",
  "durationSeconds": 215,
  "playCount": 1024,
  "releaseAt": "2026-12-24T20:00:00",
  "artistId": 5,
  "artistName": "Mây Chiều",
  "featuredArtists": [ { "id": 12, "artistName": "Sóng Nhỏ" } ],
  "genreId": 5,
  "genreName": "Ballad"
}
```

| Trường | Kiểu | Có thể null | Ghi chú |
|---|---|---|---|
| `id` | số | Không | |
| `title` | chuỗi | Không | |
| `audioUrl` | chuỗi | Không | Đường dẫn tương đối, ghép Base URL để phát |
| `coverUrl` | chuỗi | **Có** | Không có ảnh → frontend hiện ảnh mặc định |
| `canvasUrl` | chuỗi | **Có** | (MỚI) Video canvas, `null` = không có canvas (mục 14) |
| `durationSeconds` | số | Không | Server tự đo từ file |
| `playCount` | số | Không | |
| `releaseAt` | chuỗi | Không | Giờ Việt Nam |
| `artistId`, `artistName` | số, chuỗi | Không | Artist chính |
| `featuredArtists` | mảng `{ id, artistName }` | Không (có thể `[]`) | Người feat còn hoạt động |
| `genreId`, `genreName` | số, chuỗi | **Có** | (MỚI) Thể loại. Bài cũ đăng trước khi có tính năng có thể `null` |

`featuredArtists` **tự động bỏ** những người feat đã bị khóa hoặc mất quyền Artist, và được sắp theo `id` tăng dần (`SongMapper.activeFeaturedArtists`).

`SongResponse` này dùng chung ở **mọi nơi** trả bài hát cho người nghe: danh sách, tìm kiếm, chi tiết, trang nghệ sĩ, lịch sử nghe, gợi ý, playlist → frontend chỉ cần 1 component hiển thị bài.

### 11.1. UC20 — Danh sách bài đã phát hành

**API:** `GET /api/songs?genreId=5&page=0&size=20`

| Tham số | Bắt buộc | Mặc định | Ghi chú |
|---|---|---|---|
| `genreId` (MỚI) | Không | (không lọc) | Chỉ lấy bài thuộc thể loại này. Không gửi = mọi thể loại |
| `page` | Không | 0 | Bắt đầu từ 0 |
| `size` | Không | 20 | Server ép về khoảng 1–50 |

- Sắp xếp: **mới phát hành lên đầu** (`releaseAt` giảm dần).
- Kết quả dạng `PageResponse` (mục 21).
- `genreId` không tồn tại → 200 với `content: []` (không báo lỗi).
- `genreId=abc` → 400 `Invalid parameter: genreId`.
- Dùng cho trang chủ (không gửi `genreId`) và trang **Genres → 1 thể loại** (có `genreId`, mục 13).

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
     b. Giao việc ghi 1 document vào MongoDB listening_history cho thread khác (@Async, mục 12.3)

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

### 11.7. Các chức năng nghe nhạc khác (MỚI)

| Chức năng | API | Chi tiết |
|---|---|---|
| Danh sách 25 thể loại (trang Genres, form đăng bài) | `GET /api/genres` | Mục 13 |
| Xem canvas khi phát bài | Không có API riêng — dùng `canvasUrl` trong `SongResponse` | Mục 14 |
| Xem lời bài hát | `GET /api/songs/{songId}/lyrics` | Mục 15 |
| Phát tiếp khi hết bài | `GET /api/songs/{songId}/recommendations` | Mục 16.2 |
| "Thêm từ nghệ sĩ này" | `GET /api/songs/{songId}/recommendations/artist` | Mục 16.3 |
| Gợi ý theo thể loại yêu thích | `GET /api/recommendations/genres` | Mục 16.4 |
| Thêm bài vào playlist | `POST /api/playlists/{playlistId}/songs/{songId}` | `PLAYLIST_MODULE.md` |

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
@Async                                     ← chạy ở thread riêng (MỚI)
record(userId, songId):
    try {
        lưu { userId, songId, playedAt = bây giờ } vào MongoDB
    } catch (bất kỳ lỗi nào) {
        chỉ ghi log WARN "Could not save listening history (user X, song Y): ..."
        KHÔNG ném lỗi ra ngoài
    }
```

**Ghi bất đồng bộ (`@Async`, MỚI):**
- Trước đây API `/play` phải **chờ** MongoDB Atlas (máy chủ ở xa, có khi mất vài trăm mili giây) lưu xong mới trả kết quả.
- Giờ `SongService.play` chỉ "giao việc" ghi lịch sử cho 1 thread khác rồi trả `200 Play recorded` **ngay**. Thread kia tự ghi MongoDB sau đó.
- Bật bằng `@EnableAsync` trong `MyspotifyApplication.java`. `@Async` chỉ có tác dụng khi hàm được gọi từ **class khác** (ở đây `SongService` gọi sang `ListeningHistoryService`).
- Hệ quả: gọi `/play` rồi gọi **ngay lập tức** `GET /api/users/me/history` có thể chưa thấy bài vừa nghe (chậm khoảng dưới 1 giây). Frontend nên đợi một chút hoặc tải lại khi mở mục "Nghe gần đây".

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

## 13. THỂ LOẠI NHẠC (GENRE)

### 13.1. Tổng quan

- Hệ thống có sẵn **25 thể loại**: Pop, V-Pop, K-Pop, US-UK, Ballad, Rap, Hip-hop, R&B, Soul, Rock, Metal, Indie, Folk, Country, Blues, Jazz, Acoustic, Bolero, Classical, EDM, Dance, Remix, Lo-fi, Chill, OST.
- Mỗi bài có **đúng 1** thể loại, Artist chọn **bắt buộc** lúc đăng bài, **không đổi được** sau đó (mục 9.4).
- **Không có** API thêm, sửa, xóa thể loại. Muốn thêm thể loại → chạy `INSERT` trong MySQL (mục 7.2a).
- Thể loại dùng ở 4 nơi: form đăng bài, trang Genres (lọc bài), hiển thị nhãn trên bài hát, thuật toán gợi ý (mục 16).

### 13.2. Lấy danh sách thể loại

**API:** `GET /api/genres` (mọi tài khoản đã đăng nhập, không tham số, không phân trang)

**Response 200:**
```json
[
  { "id": 17, "name": "Acoustic" },
  { "id": 5,  "name": "Ballad" },
  { "id": 15, "name": "Blues" },
  { "id": 24, "name": "Chill" },
  { "id": 25, "name": "OST" }
]
```
- Đã sắp xếp theo **tên A → Z** (`GenreRepository.findAllByOrderByNameAsc`).
- `id` **không theo thứ tự** và có thể khác nhau giữa local và VPS → frontend luôn lấy id từ API này, **không viết cứng**.
- Danh sách gần như không đổi → frontend gọi 1 lần, lưu vào store dùng chung.

Luồng: `GenreController` → `SongService.getGenres()` → `GenreRepository` → `SongMapper.toGenreResponse` → `[{ id, name }]`.

### 13.3. Chọn thể loại khi đăng bài

Form-data `POST /api/artist/songs` có thêm key **`genreId`** (bắt buộc), chi tiết ở mục 9.1.

| Tình huống | Kết quả |
|---|---|
| Không gửi `genreId` (hoặc viết sai tên key: `genre_id`, `genreID`...) | 400 `Genre is required` |
| `genreId=abc` | 400 `Invalid value for genreId` |
| `genreId=9999` (không tồn tại) | 400 `Genre does not exist` |

Kiểm tra thể loại nằm **trước** bước lưu file → sai thể loại thì không tốn công lưu file nhạc rồi phải xóa.

### 13.4. Lọc bài theo thể loại

`GET /api/songs?genreId=24&page=0&size=20` (mục 11.1). Câu truy vấn `SongRepository.findPublishedByGenre`:
```sql
SELECT s FROM Song s WHERE s.genre.id = :genreId AND <đã phát hành>
```
- Vẫn áp dụng đủ điều kiện "đã phát hành" (mục 8.4).
- Sắp xếp mới phát hành lên đầu, có phân trang.
- Tìm kiếm (`/api/songs/search`) **không** lọc theo thể loại.

### 13.5. Thể loại trong dữ liệu trả về

`SongResponse` và `SongManageResponse` có thêm `genreId`, `genreName` (từ `song.getGenre()`).
- Bài **cũ** (đăng trước khi có tính năng thể loại) có thể `genreId = null`, `genreName = null` → frontend **ẩn nhãn**, không hiện chữ "null".
- Bài cũ có `genre = null` **không** xuất hiện ở trang Genres và không được ưu tiên "cùng thể loại" trong gợi ý, nhưng vẫn nghe, tìm kiếm bình thường.

Muốn gán thể loại cho bài cũ: chạy SQL trực tiếp, VD `UPDATE songs SET genre_id = 5 WHERE id = 15;`

### 13.6. Hướng dẫn frontend

Toàn bộ hướng dẫn frontend cho thể loại (form đăng bài, trang `/genres`, `/genres/:genreId`, checklist) nằm trong **`docs/GENRE_FRONTEND.md`**.

---

## 14. CANVAS — VIDEO NỀN NGẮN CỦA BÀI HÁT

### 14.1. Tổng quan

Canvas là 1 video MP4 ngắn chạy **lặp lại, không tiếng** phía sau trình phát khi bài đang phát (giống tính năng Canvas của Spotify).

| Quy định | Giá trị | Kiểm tra ở |
|---|---|---|
| Định dạng | Chỉ **MP4** (`Content-Type: video/mp4`) | `FileStorageService.saveVideo` |
| Dung lượng | ≤ **50MB** | `saveVideo` |
| File thật | Byte thứ 5–8 phải là chữ `ftyp` (chữ ký của file MP4) | `saveVideo` (`isRealMp4`) |
| Độ dài | Từ **7 đến 10 giây** (làm tròn tới 0,1 giây) | `ArtistSongService.checkCanvasDuration` |
| Số lượng | Mỗi bài **tối đa 1** canvas (đổi = thay cái cũ) | |
| Nơi lưu | `<upload-dir>/songs/canvas/<UUID>.mp4` | |
| Cột database | `songs.canvas_url` | |

Canvas **không gửi lúc đăng bài**. Đăng bài xong, Artist thêm canvas bằng API riêng.

### 14.2. Đổi canvas

**API:** `PUT /api/artist/songs/{songId}/canvas` · **Role:** ARTIST · **Body:** form-data, key **`canvas`** (File)

Các bước (`ArtistSongService.updateCanvas`):
```
1. Kiểm tra Artist còn hoạt động, tìm bài CỦA MÌNH (bài đã DELETED → "Song does not exist")
2. Ghi nhớ đường dẫn canvas CŨ
3. Kiểm tra + lưu video MỚI xuống ổ đĩa (định dạng, dung lượng, chữ ký ftyp)
┌─ try ───────────────────────────────────────────────────────┐
│ 4. Đo độ dài video: phải từ 7 đến 10 giây                    │
│ 5. Đặt canvasUrl = video mới                                 │
│ 6. Nếu bài đang REJECTED → PENDING (gửi duyệt lại)           │
│ 7. Lưu database                                              │
│ 8. Lưu xong mới XÓA video cũ khỏi ổ đĩa                      │
└─ catch: xóa video MỚI vừa lưu, giữ nguyên video cũ ─────────┘
9. Trả về SongManageResponse ("canvasUrl" là video mới)
```

Vì sao đo độ dài **sau** khi đã lưu file? Phải có file trên ổ đĩa mới đọc được thông tin bên trong. Đo sai thì khối `catch` xóa ngay file vừa lưu, không để lại rác.

**Cách server đo độ dài video** (không dùng thư viện ngoài): file MP4 được chia thành các "hộp" (box). Server tìm hộp `moov` → bên trong tìm hộp `mvhd` (movie header) → đọc 2 con số `timescale` (số đơn vị mỗi giây) và `duration` (tổng số đơn vị) → `độ dài (ms) = duration × 1000 / timescale`. Không tìm thấy hộp hoặc số không hợp lệ → `Invalid or corrupted video file`.

**Lỗi có thể gặp:**

| Tình huống | Kết quả |
|---|---|
| Thiếu hẳn key `canvas` | 400 `File is required` |
| Có key nhưng không chọn file | 400 `Video file is required` |
| Video > 50MB | 400 `Video must not exceed 50MB` |
| Video > 100MB (vượt giới hạn upload tổng) | 413 `File is too large` |
| Không phải MP4 (MOV, WEBM, AVI...) | 400 `Only MP4 videos are allowed` |
| File giả đổi đuôi `.mp4` / hỏng | 400 `Invalid or corrupted video file` |
| Video ngắn hơn 7 giây hoặc dài hơn 10 giây | 400 `Canvas video must be between 7 and 10 seconds (current: 12.3s)` |
| Bài không phải của mình / không tồn tại / đã xóa | 400 `Song does not exist` |
| Không phải tài khoản ARTIST | 403 |

### 14.3. Gỡ canvas

**API:** `DELETE /api/artist/songs/{songId}/canvas` (không body)

- Đặt `canvasUrl = null`, lưu database, **rồi mới** xóa file video.
- Trạng thái bài **giữ nguyên** (gỡ canvas không gửi duyệt lại, kể cả bài REJECTED).
- Bài chưa có canvas → 400 `Song has no canvas video`.
- Trả về `SongManageResponse` với `"canvasUrl": null`.

### 14.4. Canvas và trạng thái bài

| Bài đang | Đổi canvas | Gỡ canvas |
|---|---|---|
| PENDING / APPROVED / HIDDEN | ✔, giữ trạng thái | ✔, giữ trạng thái |
| REJECTED | ✔, chuyển **PENDING** (gửi duyệt lại, xóa `rejectReason`) | ✔, vẫn REJECTED |
| DELETED | ✘ `Song does not exist` | ✘ |

Xóa bài → video canvas bị xóa khỏi ổ đĩa cùng file nhạc và ảnh bìa.

### 14.5. Hiển thị canvas (frontend)

```html
<!-- Có canvas: video lặp, tắt tiếng, tự chạy -->
<video src="http://157.66.100.87:8080/uploads/songs/canvas/5d1e....mp4"
       autoplay muted loop playsinline></video>
```
- `canvasUrl = null` → hiển thị ảnh bìa như bình thường.
- **Bắt buộc `muted`**: trình duyệt chặn video tự chạy có tiếng; hơn nữa nhạc đã phát bằng thẻ `<audio>` riêng.
- `playsinline`: trên iPhone video chạy ngay trong trang, không tự bật toàn màn hình.
- Dừng nhạc → nên dừng luôn video (`video.pause()`) cho đỡ tốn pin.
- File canvas cũng nằm ở `/uploads/**` → **không cần token**, có cache 1 giờ.

---

## 15. LỜI BÀI HÁT (LYRICS)

### 15.1. Tổng quan

Mỗi bài có **0 hoặc 1** bản lời, do Artist (chủ bài) thêm. Có 2 loại:

| Loại | Ví dụ 1 dòng | `synced` | Frontend hiển thị |
|---|---|---|---|
| **Lời thường** | `Chiều nay mưa rơi bên hiên` | `false` | Hiện cả bài như văn bản |
| **Lời có mốc thời gian** (định dạng LRC) | `[00:12.50] Chiều nay mưa rơi bên hiên` | `true` | Tô sáng dòng đang hát, tự cuộn theo nhạc (kiểu karaoke) |

Trong 1 bài, lời phải **toàn bộ** là lời thường **hoặc toàn bộ** có mốc thời gian, không được trộn.

### 15.2. Các API

| Method | URL | Ai | Body | Trả về |
|---|---|---|---|---|
| GET | `/api/songs/{songId}/lyrics` | Mọi tài khoản đăng nhập | — | `LyricsResponse` |
| GET | `/api/artist/songs/{songId}/lyrics` | ARTIST (chủ bài) | — | `LyricsResponse` |
| PUT | `/api/artist/songs/{songId}/lyrics` | ARTIST (chủ bài) | JSON `{ "content": "..." }` | `LyricsResponse` |
| DELETE | `/api/artist/songs/{songId}/lyrics` | ARTIST (chủ bài) | — | `{ "message": "Lyrics have been deleted" }` |

Controller: `LyricsController` (không có đường dẫn chung, mỗi hàm tự ghi đủ đường dẫn; 3 API của Artist có `@PreAuthorize("hasRole('ARTIST')")`). Service: `LyricsService`.

### 15.3. Cấu trúc `LyricsResponse`

**Bài có lời có mốc thời gian:**
```json
{
  "songId": 15,
  "synced": true,
  "content": "[00:12.50] Chiều nay mưa rơi bên hiên\n[00:17.20] Nhớ em như nhớ một miền\n[00:22.00]",
  "lines": [
    { "timeMs": 12500, "text": "Chiều nay mưa rơi bên hiên" },
    { "timeMs": 17200, "text": "Nhớ em như nhớ một miền" },
    { "timeMs": 22000, "text": "" }
  ],
  "updatedAt": "2026-11-02T21:15:40"
}
```

**Bài có lời thường:**
```json
{
  "songId": 22,
  "synced": false,
  "content": "Phố đêm lên đèn\nMình anh lang thang",
  "lines": [
    { "timeMs": null, "text": "Phố đêm lên đèn" },
    { "timeMs": null, "text": "Mình anh lang thang" }
  ],
  "updatedAt": "2026-11-02T21:20:11"
}
```

**Bài chưa có lời** (vẫn trả **200**, không báo lỗi):
```json
{ "songId": 9, "synced": false, "content": "", "lines": [], "updatedAt": null }
```

| Trường | Ý nghĩa |
|---|---|
| `songId` | Id bài hát |
| `synced` | `true` = lời có mốc thời gian |
| `content` | Toàn bộ lời dạng chữ **đã chuẩn hóa** (mục 15.5), dùng để điền vào ô soạn lời khi Artist sửa |
| `lines` | Từng dòng đã tách sẵn: `timeMs` (mili giây kể từ đầu bài, `null` nếu lời thường) và `text` (có thể rỗng `""` — dòng nghỉ nhạc) |
| `updatedAt` | Lần lưu gần nhất, `null` nếu chưa có lời |

Frontend kiểm tra "bài có lời không" bằng `lines.length > 0`.

### 15.4. Quy tắc nội dung lời (khi Artist lưu)

`PUT /api/artist/songs/{songId}/lyrics` với body:
```json
{ "content": "[ti:Chiều Mưa]\n[ar:Mây Chiều]\n\n[00:12.5] Chiều nay mưa rơi bên hiên\n[00:17.20] Nhớ em như nhớ một miền\n[00:22.00]" }
```

Server đọc `content` **từng dòng** (tách theo xuống dòng `\n` hoặc `\r\n`), cắt dấu cách 2 đầu mỗi dòng, rồi:

| Loại dòng | Ví dụ | Xử lý |
|---|---|---|
| Dòng trống | `` | **Bỏ qua** |
| Dòng thẻ thông tin LRC | `[ti:Chiều Mưa]`, `[ar:Mây Chiều]`, `[length:03:35]`, `[by:...]` | **Bỏ qua** (không lưu) |
| Dòng có mốc thời gian | `[00:12.50] Chiều nay...` | Đọc thời gian + chữ |
| Dòng chữ thường | `Chiều nay mưa rơi bên hiên` | Chỉ có chữ |

**Định dạng mốc thời gian được chấp nhận:** `[phút:giây]`, `[phút:giây.xx]`, `[phút:giây:xx]`, `[phút:giây.xxx]`
- `phút`: 1 đến 3 chữ số. `giây`: đúng 2 chữ số, từ `00` đến `59`.
- Phần lẻ: 1 đến 3 chữ số, server chỉ lấy **2 chữ số đầu** (phần trăm giây). VD `.5` = 0,50 giây; `.123` = 0,12 giây.
- Ví dụ: `[03:05.40]` = 3 phút 5,4 giây = `185400` ms.

**Các quy tắc và lỗi** (thứ tự kiểm tra từ trên xuống):

| # | Quy tắc | Lỗi |
|---|---|---|
| 1 | `content` không rỗng, không toàn dấu cách | 400 `Lyrics must not be empty` |
| 2 | `content` tối đa **10.000 ký tự** | 400 `Lyrics must not exceed 10000 characters` |
| 3 | Giây trong mốc thời gian phải từ 00 đến 59 | 400 `Line 4: invalid time, seconds must be 00 - 59` |
| 4 | Mốc thời gian không được vượt quá độ dài bài + 1 giây | 400 `Line 4: time is longer than the song` |
| 5 | Mốc thời gian dòng sau phải **lớn hơn** dòng trước (không bằng, không lùi) | 400 `Line 5: time must be greater than the previous line` |
| 6 | Mỗi dòng (phần chữ) tối đa **200 ký tự** | 400 `Line 7: must not exceed 200 characters` |
| 7 | Sau khi bỏ dòng trống và dòng thẻ, phải còn ít nhất 1 dòng | 400 `Lyrics must not be empty` |
| 8 | Tối đa **300 dòng** (không tính dòng trống, dòng thẻ) | 400 `Lyrics must not exceed 300 lines` |
| 9 | Hoặc **mọi** dòng có mốc thời gian, hoặc **không** dòng nào có | 400 `Only 3/10 lines have a time. Either all lines must have a time or none` |

- Số dòng trong message (`Line 4`) là số thứ tự dòng trong **nội dung gửi lên** (đếm cả dòng trống, dòng thẻ) → Artist mở ô soạn lời, tìm đúng dòng đó để sửa.
- Mốc thời gian **được phép** không có chữ (`[00:22.00]`) để đánh dấu đoạn nhạc không lời.

### 15.5. Chuẩn hóa khi lưu

Server **không lưu nguyên văn** nội dung gửi lên mà lưu bản đã dọn:
- Bỏ dòng trống, dòng thẻ thông tin.
- Cắt dấu cách thừa.
- Viết lại mốc thời gian theo 1 dạng chuẩn `[mm:ss.xx]` (phút 2 chữ số, phần trăm giây 2 chữ số), cách chữ 1 dấu cách.

Ví dụ:
```
Gửi lên:                               Lưu (và trả về trong "content"):
[ti:Chiều Mưa]                          [00:12.50] Chiều nay mưa rơi bên hiên
[ar:Mây Chiều]                          [00:17.20] Nhớ em như nhớ một miền
                                        [00:22.00]
[0:12.5]Chiều nay mưa rơi bên hiên
  [00:17:20]   Nhớ em như nhớ một miền
[00:22]
```

### 15.6. Các bước xử lý

**Lưu lời** (`LyricsService.saveLyrics`):
```
1. (Controller) @Valid kiểm tra content: không rỗng, ≤ 10.000 ký tự
2. Kiểm tra Artist còn hoạt động
3. Tìm bài CỦA MÌNH (không có / của người khác / đã DELETED → "Song does not exist")
4. Đọc và kiểm tra từng dòng theo bảng 15.4 (cần độ dài bài để kiểm tra quy tắc 4)
5. Bài đã có lời → thay nội dung cũ; chưa có → tạo mới (mỗi bài chỉ 1 bản lời)
6. Lưu bản đã chuẩn hóa, updatedAt = bây giờ
7. Trả về LyricsResponse
```
- Lưu lời được ở **mọi trạng thái** bài (trừ DELETED), kể cả lúc đang chờ duyệt.
- Lưu lời **không** làm đổi trạng thái bài (không gửi duyệt lại, kể cả bài REJECTED).

**Xem lời (người nghe)** (`getPublishedLyrics`): bài phải **đang phát hành** (không thì 400 `Song does not exist`) → đọc lời → trả về. Chưa có lời → response rỗng (mục 15.3).

**Xem lời (Artist)** (`getMyLyrics`): bài của mình, mọi trạng thái trừ DELETED. Dùng để điền sẵn ô soạn lời khi Artist mở trang sửa.

**Xóa lời** (`deleteLyrics`): bài của mình; chưa có lời → 400 `Song has no lyrics`; thành công → `{ "message": "Lyrics have been deleted" }`.

### 15.7. Hiển thị lời có mốc thời gian (frontend)

```js
// lines: mảng { timeMs, text } từ API; audio: thẻ <audio> đang phát
audio.addEventListener("timeupdate", () => {
  const nowMs = audio.currentTime * 1000;
  // Dòng đang hát = dòng CUỐI CÙNG có timeMs <= thời điểm hiện tại
  let current = -1;
  for (let i = 0; i < lines.length; i++) {
    if (lines[i].timeMs <= nowMs) current = i;
    else break;                       // lines đã xếp tăng dần theo thời gian
  }
  highlightLine(current);             // tô sáng + cuộn dòng đó vào giữa khung
});
```
- `synced === false` → chỉ hiện danh sách `lines[].text`, không tô sáng.
- `lines.length === 0` → ẩn nút "Lời bài hát" hoặc hiện "Bài hát này chưa có lời".
- Bấm vào 1 dòng (khi `synced`) → tua nhạc tới dòng đó: `audio.currentTime = line.timeMs / 1000`.

Ô soạn lời cho Artist: dùng `<textarea>`, gửi `JSON.stringify({ content: textarea.value })` (xuống dòng tự thành `\n`). Lỗi `Line N: ...` → bôi đỏ dòng N trong ô soạn.

---

## 16. GỢI Ý BÀI HÁT (RECOMMENDATION)

### 16.1. Tổng quan

| API | Dùng ở đâu trên giao diện | Cách tính | Gọi AI Python? |
|---|---|---|---|
| `GET /api/songs/{songId}/recommendations` | **Tự phát tiếp** khi hết bài | Quy tắc cố định trong Java (mục 16.2) | Không |
| `GET /api/songs/{songId}/recommendations/artist` | Mục "Thêm từ nghệ sĩ này" ở trang chi tiết bài | Quy tắc cố định (mục 16.3) | Không |
| `GET /api/recommendations/genres` | Mục "Dành cho bạn" theo thể loại yêu thích | Service AI Python, lỗi thì dùng dự phòng (mục 16.4) | **Có** |

Chung cho cả 3:
- Mọi tài khoản đã đăng nhập đều gọi được. Controller: `RecommendationController`. Service: `RecommendationService`.
- Trả về **mảng `SongResponse`** (không phân trang), có thể ít hơn `limit`, có thể `[]`.
- Chỉ gợi ý bài **đang phát hành** (mục 8.4).
- `limit` được server ép về khoảng **1–20** (`limit=0` → 1, `limit=100` → 20).
- Bài gốc (`songId`) phải đang phát hành, không thì 400 `Song does not exist`.

Các bước lấy ứng viên đều dùng "**bể ứng viên**" (candidate pool) gồm tối đa **50 bài nhiều lượt nghe nhất** (sắp `playCount` giảm dần, bằng nhau thì `id` giảm dần), rồi mới lọc.

### 16.2. Gợi ý phát tiếp (hàng chờ tự phát)

**API:** `GET /api/songs/{songId}/recommendations?limit=5&excludeIds=15&excludeIds=22`

| Tham số | Bắt buộc | Mặc định | Ghi chú |
|---|---|---|---|
| `songId` (đường dẫn) | Có | | Bài vừa nghe xong |
| `limit` | Không | `5` | 1–20 |
| `excludeIds` | Không | (không có) | Các bài **không muốn gợi ý lại** (VD các bài vừa phát). Lặp key (`excludeIds=1&excludeIds=2`) hoặc 1 key dạng `1,2` |

**Thuật toán** (`getSimilarSongs`) — đúng yêu cầu "phát tối đa 3 bài cùng nghệ sĩ, rồi sang nghệ sĩ khác cùng thể loại":
```
Bỏ qua (skip): bài gốc + mọi bài trong excludeIds

Bước 1. CÙNG NGHỆ SĨ (tối đa 3 bài)
        Lấy ≤ 50 bài phổ biến nhất mà nghệ sĩ của bài gốc là Artist chính HOẶC được feat.
        Xếp bài CÙNG THỂ LOẠI với bài gốc lên trước, còn lại xếp sau (giữ thứ tự lượt nghe).
        Lấy tối đa min(3, limit) bài chưa bị skip.
        → Sau bước này, MỌI bài của nghệ sĩ đó đều bị đưa vào skip
          (để bước 2, 3 không lấy thêm bài của nghệ sĩ này nữa).

Bước 2. CÙNG THỂ LOẠI, NGHỆ SĨ KHÁC (nếu chưa đủ limit và bài gốc có thể loại)
        Lấy ≤ 50 bài phổ biến nhất cùng thể loại với bài gốc.
        Bỏ bài bị skip, bỏ bài có Artist chính là nghệ sĩ của bài gốc.
        Thêm vào cho tới khi đủ limit.

Bước 3. BÀI PHỔ BIẾN BẤT KỲ (nếu vẫn chưa đủ limit)
        Lấy ≤ 50 bài phổ biến nhất toàn hệ thống, cùng quy tắc bỏ như bước 2.
        Thêm vào cho tới khi đủ limit.
```

**Ví dụ.** Lan nghe xong "Chiều Mưa" (Mây Chiều, Ballad), `limit=5`. Mây Chiều có 4 bài khác: 2 Ballad, 2 Pop.
```
Bước 1 → 2 bài Ballad của Mây + 1 bài Pop phổ biến nhất của Mây       (đủ 3, dừng)
Bước 2 → 2 bài Ballad phổ biến nhất của nghệ sĩ khác                   (đủ 5, dừng)
Kết quả: [Ballad-Mây-1, Ballad-Mây-2, Pop-Mây-1, Ballad-Khác-1, Ballad-Khác-2]
```
Nếu Mây chỉ có đúng 1 bài thì bước 1 trả 0 bài, đi thẳng sang bước 2. Nếu cả thể loại chỉ có vài bài thì bước 3 bù bằng bài phổ biến.

**Vì sao không dùng AI cho phát tiếp?** Quy tắc cố định dễ đoán, đúng yêu cầu nghiệp vụ (3 bài cùng nghệ sĩ rồi sang thể loại), chạy nhanh, không phụ thuộc server Python.

**Frontend dùng thế nào** (tự phát tiếp vô hạn mà không lặp bài):
```js
const played = [];                         // id các bài đã phát trong phiên nghe này
async function onSongEnded(currentSong) {
  played.push(currentSong.id);
  const { data } = await api.get(`/api/songs/${currentSong.id}/recommendations`, {
    params: { limit: 5, excludeIds: played.slice(-50).join(",") },
  });
  if (data.length > 0) playSong(data[0]);  // hoặc đưa cả 5 bài vào hàng chờ
  else stopPlayer();                       // hết bài để gợi ý
}
```
Gửi tối đa khoảng 50 id gần nhất trong `excludeIds` để URL không quá dài.

### 16.3. Bài khác của cùng nghệ sĩ

**API:** `GET /api/songs/{songId}/recommendations/artist?limit=5`

Thuật toán (`getArtistSongs`):
```
1. Lấy ≤ 50 bài phổ biến nhất của nghệ sĩ bài gốc (Artist chính HOẶC được feat),
   bỏ bài gốc, bài cùng thể loại lên trước → lấy tối đa limit bài.
2. Nghệ sĩ không còn bài nào khác (kết quả rỗng)
   → DỰ PHÒNG: lấy bài phổ biến nhất CÙNG THỂ LOẠI với bài gốc (của bất kỳ nghệ sĩ nào), bỏ bài gốc.
     Bài gốc không có thể loại → lấy bài phổ biến nhất toàn hệ thống.
```
Lưu ý frontend: khi rơi vào dự phòng, kết quả là bài của **nghệ sĩ khác** → kiểm tra `artistId` để đổi tiêu đề mục từ "Thêm từ {nghệ sĩ}" thành "Có thể bạn cũng thích".

### 16.4. Gợi ý theo thể loại (service AI Python)

**API:** `GET /api/recommendations/genres?genreIds=5&genreIds=24&limit=10`

| Tham số | Bắt buộc | Mặc định | Ghi chú |
|---|---|---|---|
| `genreIds` | **Có** | | Id thể loại (lấy từ `GET /api/genres`). Lặp key hoặc `5,24`. Tối đa **10** thể loại khác nhau |
| `limit` | Không | `10` | 1–20 |

Thuật toán (`getGenreSongs`):
```
1. Kiểm tra genreIds: bỏ ô trống, bỏ trùng.
     Rỗng → 400 "Genre ids are required"
     > 10 → 400 "At most 10 genres are allowed"
2. Gọi service AI Python, xin GẤP ĐÔI số bài cần (vì 1 số bài AI trả có thể không còn phát hành):
     POST {app.recommend.base-url}/recommend/genres
     Body: { "genre_ids": [5, 24], "top_n": 20 }
     Python trả: { "recommended_ids": [31, 15, 8, ...] }   (hoặc khóa "recommendations")
3. Lọc: chỉ giữ bài ĐANG PHÁT HÀNH, bỏ trùng, GIỮ ĐÚNG THỨ TỰ AI trả, cắt còn limit bài.
4. Kết quả rỗng (AI lỗi, quá 3 giây, server Python tắt, hoặc mọi bài AI trả đều không dùng được)
   → DỰ PHÒNG: với mỗi thể loại lấy các bài nhiều lượt nghe nhất, gộp lại,
     sắp theo lượt nghe giảm dần, cắt còn limit bài.
```
- Python lỗi **không** làm API lỗi: server chỉ ghi log `Recommend service error (genres): ...` rồi dùng dự phòng → người dùng vẫn luôn thấy gợi ý.
- `genreIds=abc` → 400 `Invalid parameter: genreIds`. Id thể loại không tồn tại → chỉ ít kết quả hơn, không báo lỗi.
- Nếu AI trả về **một ít** bài dùng được (ít hơn `limit`), server trả đúng số đó, **không** bù thêm bằng dự phòng.

Gợi ý giao diện: lần đầu đăng nhập cho người dùng chọn vài thể loại yêu thích → lưu ở frontend → trang chủ có mục "Dành cho bạn" gọi API này.

### 16.5. Kết nối với service AI Python

| Mục | Giá trị |
|---|---|
| Class gọi sang | `client/RecommendClient.java` (dùng `RestClient` của Spring) |
| Địa chỉ | `app.recommend.base-url` trong file cấu hình, không khai báo thì mặc định `http://127.0.0.1:8000` (service Python chạy cùng máy) |
| Thời gian chờ tối đa | `app.recommend.timeout-ms`, mặc định `3000` (3 giây) cho cả lúc kết nối lẫn lúc đọc kết quả |
| Định dạng gửi | JSON, khóa viết kiểu Python: `genre_ids`, `top_n` |
| Định dạng nhận | JSON có `recommended_ids` hoặc `recommendations` (mảng id bài), khóa lạ bị bỏ qua |

`RecommendClient` còn sẵn 3 hàm gọi `/recommend`, `/recommend/artist`, `/recommend/playlist` của service Python nhưng **hiện chưa API nào dùng** (để dành cho gợi ý bài cho playlist sau này).

Kiểm tra service Python trên VPS: xem log `app.log` có dòng `Recommend service error` không. Có → service Python đang tắt hoặc sai địa chỉ; gợi ý theo thể loại đang chạy bằng dự phòng.

---

## 17. LƯU TRỮ FILE NHẠC, ẢNH VÀ VIDEO

Toàn bộ nằm trong `FileStorageService`.

### 17.1. File nhạc (`saveAudio`)

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

### 17.2. File ảnh (`saveImage`)

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

`saveImage(file, folder)` dùng chung cho nhiều nơi, mỗi nơi 1 thư mục: ảnh bìa bài hát (`songs/covers`), ảnh đại diện (`avatars`), ảnh bìa playlist (`playlists`).

### 17.2a. File video canvas (`saveVideo`) — MỚI

| Kiểm tra | Cách làm | Lỗi |
|---|---|---|
| Có file | `file == null` hoặc `isEmpty()` | `Video file is required` |
| Dung lượng | ≤ **50MB** | `Video must not exceed 50MB` |
| Định dạng khai báo | `Content-Type` phải đúng `video/mp4` | `Only MP4 videos are allowed` |
| Video thật | Đọc **8 byte đầu**: byte thứ 5–8 phải là chữ `ftyp` (chữ ký file MP4) | `Invalid or corrupted video file` |
| Độ dài | Đọc hộp `moov` → `mvhd` trong file (`getVideoDurationMs`), xem mục 14.2 | `Invalid or corrupted video file` |

- File luôn lưu đuôi `.mp4`, tên UUID, thư mục `songs/canvas`.
- Kiểm tra "7–10 giây" là quy tắc **nghiệp vụ** nên nằm ở `ArtistSongService`, không nằm ở `FileStorageService` (class lưu file chỉ lo file có hợp lệ hay không).

### 17.3. Lưu file (`saveFile`)

```
Tên file   = UUID ngẫu nhiên + đuôi           VD 3f2a9c1e-7b4d-4e8a-9c21-5d6f7e8a9b0c.mp3
Thư mục    = <app.upload-dir>/songs/audio     (tự tạo nếu chưa có)
Trả về URL = /uploads/songs/audio/3f2a9c1e-....mp3   ← lưu vào database
```

Vì sao dùng UUID thay vì tên gốc?
- 2 người cùng upload `bai-hat.mp3` → không ghi đè nhau.
- Tên gốc có thể chứa ký tự lạ (`../../`, dấu, khoảng trắng) gây lỗi hoặc lỗ hổng.
- Không đoán được đường dẫn file của người khác.

### 17.4. Xóa file (`delete`)

- Chỉ xóa đường dẫn bắt đầu bằng `/uploads/` (không xóa nhầm file hệ thống).
- `null` → bỏ qua (bài không có ảnh bìa, không có canvas).
- File không tồn tại hoặc lỗi khi xóa → **bỏ qua im lặng**. Xóa file là việc dọn dẹp, không được làm thất bại thao tác chính.

### 17.5. Giới hạn upload tổng của server

Trong `application.properties`:
```properties
spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=110MB
```
Vượt mức này, Spring chặn **trước khi** vào code → 413 `File is too large`. Mức 100MB khớp giới hạn WAV; giới hạn chi tiết từng loại (MP3 20MB, video canvas 50MB, ảnh 2MB) do `FileStorageService` kiểm tra sau.

Bảng tổng hợp giới hạn file:

| Loại | Định dạng | Tối đa | Thư mục |
|---|---|---|---|
| Nhạc | MP3 | 20MB | `songs/audio` |
| Nhạc | WAV | 100MB | `songs/audio` |
| Ảnh bìa bài hát | JPG, PNG, WEBP | 2MB | `songs/covers` |
| Canvas | MP4, 7–10 giây | 50MB | `songs/canvas` |

---

## 18. BẢO MẬT VÀ PHÂN QUYỀN

### 18.1. Ma trận quyền

| API | Chưa đăng nhập | USER | ARTIST | ADMIN |
|---|---|---|---|---|
| `/api/songs/**` (nghe nhạc, lời bài hát, gợi ý theo bài) | 401 | ✔ | ✔ | ✔ |
| `/api/genres` | 401 | ✔ | ✔ | ✔ |
| `/api/recommendations/genres` | 401 | ✔ | ✔ | ✔ |
| `/api/users/me/history` | 401 | ✔ | ✔ | ✔ |
| `/api/artist/**` (kể cả canvas, lời bài hát của Artist) | 401 | 403 | ✔ | 403 |
| `/api/admin/songs/**` | 401 | 403 | 403 | ✔ |
| `/uploads/**` (file nhạc, ảnh, video) | ✔ | ✔ | ✔ | ✔ |

ADMIN **không** đăng bài được. Muốn đăng bài phải dùng tài khoản ARTIST riêng.

"Đăng nhập" ở đây nghĩa là có token hợp lệ **và** tài khoản đang `ACTIVE` **và** `accessStatus = ACCEPT` (được chủ dự án cho phép vào web). Thiếu 1 trong 3 → 401.

### 18.2. Các lớp bảo vệ

| Lớp | Chặn cái gì | Ở đâu |
|---|---|---|
| Token JWT | Người lạ không đăng nhập | `JwtAuthenticationFilter` |
| Phiên đơn (Redis) | Token cũ sau khi đăng nhập nơi khác, sau khi đăng xuất | `SessionService` |
| Tài khoản bị khóa | Tài khoản bị Admin khóa | `JwtAuthenticationFilter` (`isEnabled`) |
| Quyền vào web (MỚI) | Tài khoản chưa được chủ dự án cho phép (`accessStatus = REFUSE`): không đăng nhập được (400 `Account has not been approved yet`), đang đăng nhập mà bị đổi sang REFUSE thì request sau bị 401 | `AuthService.login`, `CustomUserDetailsService` |
| Rate limit | Spam request: **100 request / 60 giây / tài khoản** (API đăng nhập: 10 / 60 giây / IP) | `RateLimitFilter` |
| Role | Sai vai trò | `@PreAuthorize` trên Controller |
| Quyền sở hữu | Artist đụng vào bài người khác | `findByIdAndArtistId` trong `ArtistSongService` |
| Ẩn thông tin | Dò bài chưa công khai | Mọi lỗi "không thấy bài" đều là `Song does not exist` |
| DTO tách biệt | Lộ dữ liệu nội bộ | Người nghe nhận `SongResponse` (không có status, lý do...) |
| Id lấy từ token | Giả mạo người khác | Không API nào nhận `artistId` / `userId` của người gọi qua body hay URL |
| Kiểm tra file | Upload file độc | Đuôi, dung lượng, chữ ký, đọc thử file |
| Tên file UUID | Ghi đè, đoán đường dẫn, path traversal | `FileStorageService.saveFile` |
| Truy vấn tham số hóa | SQL injection | Spring Data JPA dùng PreparedStatement, giá trị người dùng không bao giờ ghép thẳng vào câu SQL |

### 18.3. Ví dụ tấn công và cách hệ thống chặn

| Kẻ xấu thử | Kết quả |
|---|---|
| Artist B gọi `DELETE /api/artist/songs/15` (bài của A) | 400 `Song does not exist`. Bài A không sao |
| USER gọi `PUT /api/admin/songs/15/approve` | 403 |
| Sửa role trong token thành ADMIN | Chữ ký token không khớp → 401 |
| Bấm play 1000 lần để tăng lượt nghe | Chỉ được tính 1 lượt mỗi 30 giây; sau 100 request/phút bị 429 |
| Đổi `virus.exe` thành `song.mp3` để upload | Không đọc được như file nhạc → 400 |
| Tìm kiếm `' OR 1=1 --` | Được coi là chuỗi bình thường, chỉ tìm bài có chứa đúng chuỗi đó |
| Thử id liên tục `/api/songs/1`, `/2`, `/3`... để tìm bài chờ duyệt | Mọi bài không công khai đều trả cùng 1 message |
| Xem lời của bài chưa phát hành qua `/api/songs/{id}/lyrics` | 400 `Song does not exist` |
| Lấy gợi ý từ bài chưa phát hành để dò bài khác | 400 `Song does not exist`; kết quả gợi ý cũng chỉ chứa bài đã phát hành |
| Đổi `video.avi` thành `canvas.mp4` để upload | Không có chữ ký `ftyp` hoặc đọc không ra độ dài → 400 |
| Gửi lời bài hát chứa `<script>...</script>` | Lưu như chữ bình thường; frontend phải hiển thị bằng text (không dùng `innerHTML`) |
| Artist B sửa lời bài của Artist A | 400 `Song does not exist` |

---

## 19. XỬ LÝ ĐỒNG THỜI (NHIỀU NGƯỜI THAO TÁC CÙNG LÚC)

Server phục vụ nhiều người song song. 2 request có thể chạm vào **cùng 1 bài hát** cùng lúc.

### 19.1. Optimistic locking — cột `version`

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

**Đã xử lý (MỚI):** `GlobalExceptionHandler` có handler riêng cho `OptimisticLockingFailureException` (lớp cha của `ObjectOptimisticLockingFailureException`), trả:
```
409 { "message": "This data was changed by someone else, please reload and try again" }
```
Frontend nhận 409 → báo "Bài vừa được người khác thay đổi, vui lòng tải lại" và tải lại dữ liệu.

### 19.2. Lượt nghe không dùng version

Nếu lượt nghe cũng đi qua `version`, thì cứ có người nghe là `version` tăng → Artist đang sửa tên sẽ liên tục bị lỗi xung đột vô lý. Vì vậy:
- `play_count` khai báo `updatable = false` → các lần `save()` thông thường **không đụng** tới nó.
- Tăng lượt nghe bằng câu `UPDATE ... SET play_count = play_count + 1` riêng → nguyên tử, không lỗi khi nhiều người nghe cùng lúc, không làm tăng `version`.

### 19.3. Tóm tắt

| Tình huống | Cơ chế | Kết quả |
|---|---|---|
| 1000 người cùng nghe 1 bài | `UPDATE play_count + 1` trong database | Đủ 1000 lượt, không mất |
| 1 người bấm play 2 lần cùng lúc | Redis `SET NX` nguyên tử | Chỉ 1 lượt |
| Admin duyệt đúng lúc Artist xóa | `@Version` | 1 người thành công, người kia nhận 409 |
| Artist đổi canvas đúng lúc Admin gỡ bài | `@Version` | 1 người thành công, người kia nhận 409 |
| Artist sửa tên đúng lúc có người nghe | `play_count` `updatable = false` | Cả 2 thành công, lượt nghe không bị ghi đè |
| Ghi lịch sử nghe chậm (MongoDB ở xa) | `@Async` | API `/play` không phải chờ |
| Artist lưu lời bài hát ở 2 tab cùng lúc | Bảng `song_lyrics` có UNIQUE `song_id` | Mỗi bài vẫn chỉ 1 bản lời; lần lưu sau ghi đè lần trước (hoặc 409 `Data already exists` nếu cả 2 cùng tạo mới đúng 1 lúc) |

---

## 20. THỜI GIAN VÀ MÚI GIỜ

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
| `updatedAt` của lời bài hát | Mỗi lần lưu lời (`@PrePersist` + `@PreUpdate` trong `SongLyrics`) |

---

## 21. PHÂN TRANG

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

Các API sau **không phân trang** (trả mảng), vì số lượng nhỏ hoặc đã có giới hạn:

| API | Giới hạn |
|---|---|
| `GET /api/artist/songs` | Mọi bài của Artist (trừ DELETED) |
| `GET /api/users/me/history` | Tối đa 10 bài |
| `GET /api/genres` | 25 thể loại |
| `GET /api/artist/search-artists` | Tối đa 10 nghệ sĩ |
| 3 API gợi ý (mục 16) | Theo `limit`, tối đa 20 bài |

---

## 22. XỬ LÝ LỖI

### 22.1. Nguyên tắc

- Service phát hiện vi phạm nghiệp vụ → `throw new IllegalArgumentException("message")`.
- `GlobalExceptionHandler` (`@RestControllerAdvice`) bắt **mọi** lỗi ở 1 chỗ và trả JSON thống nhất `{ "message": "..." }`.
- Controller và Service không phải tự `try/catch` để trả lỗi.
- Message viết **tiếng Anh, ngắn gọn, cố định** để frontend có thể so sánh và dịch sang tiếng Việt.

### 22.2. Bảng loại lỗi → mã HTTP

| Loại lỗi (exception) | Mã | Message | Khi nào |
|---|---|---|---|
| `IllegalArgumentException` | 400 | Theo nội dung lỗi | Vi phạm nghiệp vụ (phần lớn lỗi trong module) |
| `MethodArgumentNotValidException` | 400 | Message trong annotation DTO, hoặc `Invalid value for <trường>` | Sai validation (`@NotBlank`, `@Size`...) hoặc sai kiểu dữ liệu ở form-data |
| `HttpMessageNotReadableException` | 400 | `Invalid request data` | JSON sai cú pháp, sai kiểu, thiếu body |
| `MissingServletRequestPartException` | 400 | `File is required` | Thiếu hẳn key file trong form-data |
| `MethodArgumentTypeMismatchException` | 400 | `Invalid parameter: <tên>` | `/api/songs/abc`, `?status=pending`, `?page=abc` |
| Chưa đăng nhập / token sai / hết hạn | 401 | `Unauthorized, please login` | |
| Bị đăng nhập nơi khác / đã đăng xuất / bị khóa / vừa đổi mật khẩu | 401 | `Your session has ended. Your account may have been logged in on another device` | |
| `AccessDeniedException` | 403 | `You do not have permission to access this resource` | Sai role |
| `DataIntegrityViolationException` | 409 | `Data already exists` | Vi phạm ràng buộc database (UNIQUE...) |
| `OptimisticLockingFailureException` (MỚI) | 409 | `This data was changed by someone else, please reload and try again` | 2 người cùng sửa 1 bài (mục 19.1) |
| `MaxUploadSizeExceededException` | 413 | `File is too large` | Vượt giới hạn upload tổng |
| Rate limit | 429 | `Too many requests. Please try again in X seconds` | Kèm header `Retry-After` |
| Lỗi khác của Spring (405, 415...) | Theo Spring | Theo Spring | Sai method, sai Content-Type... |
| Mọi lỗi không lường trước | 500 | `Internal server error` | **Bug** — ghi log đầy đủ để sửa |

### 22.3. Hai lớp kiểm tra dữ liệu

```
Lớp 1 — DTO (@Valid ở Controller): kiểm tra HÌNH THỨC, không cần database
        có gửi không? dài quá không? quá 5 người không? đúng định dạng ngày không?

Lớp 2 — Service: kiểm tra NGHIỆP VỤ, cần đọc database / file / giờ hiện tại
        file có phải nhạc thật? nghệ sĩ feat có tồn tại? lịch có ở tương lai?
        bài có đang PENDING? bài có phải của mình? thể loại có tồn tại?
        canvas có đủ 7–10 giây? mốc thời gian lời bài hát có tăng dần, có vượt độ dài bài?
```

---

## 23. HƯỚNG DẪN RIÊNG CHO FRONTEND

### 23.1. Đường dẫn file

`audioUrl`, `coverUrl`, `canvasUrl`, `avatarUrl` là **đường dẫn tương đối**. Luôn ghép với địa chỉ server:
```js
const BASE_URL = "http://157.66.100.87:8080";
const src = BASE_URL + song.audioUrl;
```
- `coverUrl` có thể `null` → hiển thị ảnh mặc định.
- `canvasUrl` có thể `null` → không có video, hiển thị ảnh bìa.
- `genreName` có thể `null` (bài cũ) → ẩn nhãn thể loại.

Kiểu dữ liệu (TypeScript, để tham khảo):
```ts
export interface ArtistSummary { id: number; artistName: string; }
export interface Genre { id: number; name: string; }

export interface SongResponse {
  id: number;
  title: string;
  audioUrl: string;
  coverUrl: string | null;
  canvasUrl: string | null;      // MỚI
  durationSeconds: number;
  playCount: number;
  releaseAt: string;
  artistId: number;
  artistName: string;
  featuredArtists: ArtistSummary[];
  genreId: number | null;        // MỚI
  genreName: string | null;      // MỚI
}

export interface SongManageResponse extends Omit<SongResponse, "releaseAt"> {
  status: "PENDING" | "APPROVED" | "REJECTED" | "HIDDEN" | "DELETED";  // DELETED chỉ gặp ở Admin lọc ?status=DELETED
  released: boolean;
  releaseAt: string | null;
  rejectReason: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface LyricLine { timeMs: number | null; text: string; }
export interface LyricsResponse {
  songId: number;
  synced: boolean;
  content: string;
  lines: LyricLine[];
  updatedAt: string | null;
}

export interface PageResponse<T> {
  content: T[]; page: number; size: number; totalElements: number; totalPages: number;
}
```

### 23.2. Đăng bài (form-data)

```js
const form = new FormData();
form.append("title", title);
form.append("genreId", String(selectedGenreId));               // MỚI, bắt buộc, lấy từ GET /api/genres
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

Kiểm tra sớm ở frontend để trải nghiệm tốt hơn (server vẫn kiểm tra lại): đuôi file, dung lượng (MP3 ≤ 20MB, WAV ≤ 100MB, ảnh ≤ 2MB), title ≤ 150 ký tự, tối đa 5 feat, **đã chọn thể loại**.

Ô chọn thể loại: dropdown 25 thể loại từ `GET /api/genres`, bắt buộc chọn 1, ghi chú nhỏ "Thể loại không thể thay đổi sau khi đăng" (chi tiết: `GENRE_FRONTEND.md`).

Canvas và lời bài hát **không** nằm trong form đăng bài. Sau khi đăng thành công, chuyển sang trang sửa bài để Artist thêm canvas, lời (mục 23.4).

### 23.3. Ô chọn nghệ sĩ feat

1. Người dùng gõ → debounce khoảng 300ms → gọi `GET /api/artist/search-artists?keyword=...`.
2. Hiển thị danh sách `artistName`, lưu lại `id` khi chọn.
3. Gửi các `id` trong `featuredArtistIds`.

### 23.4. Sửa bài

- Luôn gửi **đủ** `featuredArtistIds` hiện tại (kể cả không đổi), vì danh sách gửi lên thay thế hoàn toàn danh sách cũ.
- Sau khi sửa bài REJECTED (tên/feat, ảnh bìa, canvas), hiển thị thông báo "Bài đã được gửi duyệt lại".
- Thể loại hiển thị **chỉ đọc**, không gửi `genreId`.

Trang sửa bài nên có thêm 2 phần (MỚI):

**Canvas:**
```js
const form = new FormData();
form.append("canvas", videoFile);                              // key là "canvas"
const { data } = await api.put(`/api/artist/songs/${songId}/canvas`, form);
// Gỡ: await api.delete(`/api/artist/songs/${songId}/canvas`);
```
- `<input type="file" accept="video/mp4">`, kiểm tra ≤ 50MB trước khi gửi.
- Có thể kiểm tra độ dài trước bằng thẻ `<video>` ẩn (`video.duration` sau sự kiện `loadedmetadata`): 7–10 giây.
- Nút "Gỡ canvas" chỉ hiện khi `song.canvasUrl !== null`.

**Lời bài hát:**
```js
// Mở trang sửa: điền sẵn lời hiện có
const { data } = await api.get(`/api/artist/songs/${songId}/lyrics`);
textarea.value = data.content;                                 // "" nếu chưa có lời

// Lưu
await api.put(`/api/artist/songs/${songId}/lyrics`, { content: textarea.value });

// Xóa (chỉ hiện nút khi data.lines.length > 0)
await api.delete(`/api/artist/songs/${songId}/lyrics`);
```
- Hiện hướng dẫn ngắn dưới ô soạn: "Mỗi dòng 1 câu. Muốn lời chạy theo nhạc, thêm mốc thời gian ở đầu mỗi dòng, VD `[00:12.50] Câu hát`. Hoặc tất cả các dòng đều có mốc, hoặc không dòng nào có."
- Lỗi dạng `Line N: ...` → bôi đỏ dòng N.

### 23.5. Nhãn trạng thái (trang "Bài hát của tôi" và trang Admin)

```js
function songLabel(s) {
    if (s.status === "PENDING")  return "Chờ duyệt";
    if (s.status === "REJECTED") return "Bị từ chối: " + s.rejectReason;
    if (s.status === "HIDDEN")   return "Đã ẩn";
    if (s.status === "APPROVED") return s.released ? "Đã phát hành" : "Đã lên lịch";
}
```

Nút nào hiện ở trạng thái nào (dựa trên bảng mục 8.3):

| status / released | Sửa | Đổi ảnh | Canvas | Lời | Đổi lịch | Ẩn | Hiện | Xóa |
|---|---|---|---|---|---|---|---|---|
| PENDING | ✔ | ✔ | ✔ | ✔ | ✔ | | | ✔ |
| APPROVED, chưa phát hành | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | | ✔ |
| APPROVED, đã phát hành | ✔ | ✔ | ✔ | ✔ | | ✔ | | ✔ |
| REJECTED | ✔ | ✔ | ✔ | ✔ | ✔ | | | ✔ |
| HIDDEN | ✔ | ✔ | ✔ | ✔ | | | ✔ | ✔ |

(Cột "Canvas" = đổi và gỡ canvas; cột "Lời" = thêm/sửa/xóa lời bài hát.)

(HIDDEN mà chưa từng tới lịch thì vẫn đổi lịch được, nhưng hiếm gặp — có thể ẩn nút cho đơn giản.)

### 23.6. Trình phát nhạc

```js
audio.src = BASE_URL + song.audioUrl;
audio.play();
// Gọi 1 lần khi BẮT ĐẦU phát, không gọi khi tạm dừng / tua
axios.post(`${BASE_URL}/api/songs/${song.id}/play`, null, { headers: { Authorization: `Bearer ${token}` } });
```
- Không cần quan tâm chống spam, server tự lo.
- Lỗi khi gọi `/play` không nên làm dừng nhạc — chỉ cần bỏ qua.
- Sau khi phát, muốn cập nhật mục "Nghe gần đây" thì gọi lại `/api/users/me/history` (lịch sử ghi bất đồng bộ, có thể trễ dưới 1 giây — mục 12.3).

Màn hình đang phát (MỚI) nên có:

| Thành phần | Nguồn dữ liệu | Chi tiết |
|---|---|---|
| Video canvas chạy nền | `song.canvasUrl` (null → ảnh bìa) | Mục 14.5 |
| Nút / khung "Lời bài hát" | `GET /api/songs/{id}/lyrics` khi mở bài | Mục 15.7 |
| Tự phát tiếp khi hết bài | `GET /api/songs/{id}/recommendations?excludeIds=...` | Mục 16.2 |
| Mục "Thêm từ nghệ sĩ này" | `GET /api/songs/{id}/recommendations/artist` | Mục 16.3 |
| Nút "Thêm vào playlist" | `PLAYLIST_MODULE.md` mục 11.9 | |
| Nhãn thể loại (bấm → trang thể loại) | `song.genreName`, `song.genreId` | `GENRE_FRONTEND.md` |

Khi chuyển sang bài mới: đổi `audio.src`, đổi `video.src` canvas, tải lại lời, gọi `/play`.

### 23.7. Xử lý lỗi chung

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

Gợi ý dịch các message mới:

| Message | Tiếng Việt |
|---|---|
| `Genre is required` | Vui lòng chọn thể loại |
| `Genre does not exist` | Thể loại không tồn tại |
| `Video file is required` | Vui lòng chọn video |
| `Video must not exceed 50MB` | Video tối đa 50MB |
| `Only MP4 videos are allowed` | Chỉ hỗ trợ video MP4 |
| `Invalid or corrupted video file` | File video không hợp lệ hoặc bị hỏng |
| `Canvas video must be between 7 and 10 seconds (current: Xs)` | Video canvas phải dài 7–10 giây (video của bạn: X giây) |
| `Song has no canvas video` | Bài hát chưa có canvas |
| `Lyrics must not be empty` | Vui lòng nhập lời bài hát |
| `Lyrics must not exceed 10000 characters` | Lời bài hát tối đa 10.000 ký tự |
| `Lyrics must not exceed 300 lines` | Lời bài hát tối đa 300 dòng |
| `Line N: invalid time, seconds must be 00 - 59` | Dòng N: thời gian sai, giây phải từ 00 đến 59 |
| `Line N: time is longer than the song` | Dòng N: thời gian vượt quá độ dài bài hát |
| `Line N: time must be greater than the previous line` | Dòng N: thời gian phải lớn hơn dòng trước |
| `Line N: must not exceed 200 characters` | Dòng N: tối đa 200 ký tự |
| `Only X/Y lines have a time. Either all lines must have a time or none` | Chỉ X/Y dòng có thời gian. Hoặc tất cả các dòng đều có, hoặc không dòng nào có |
| `Song has no lyrics` | Bài hát chưa có lời |
| `Genre ids are required` | Vui lòng chọn ít nhất 1 thể loại |
| `At most 10 genres are allowed` | Chọn tối đa 10 thể loại |
| `This data was changed by someone else, please reload and try again` | Dữ liệu vừa được thay đổi, vui lòng tải lại |

Các message có số thay đổi (`Line N`, `current: Xs`, `Only X/Y`) → so sánh bằng `startsWith` hoặc biểu thức chính quy, rồi chèn lại số vào câu tiếng Việt.

### 23.8. Hiển thị thời gian

- `releaseAt`, `playedAt`... là giờ Việt Nam. Dùng thẳng, **không** cộng/trừ 7 tiếng.
- "15 phút trước": lấy giờ hiện tại của máy trừ `playedAt`.
- `durationSeconds = 215` → hiển thị `3:35`.
- Lời bài hát: `timeMs` là **mili giây** kể từ đầu bài; so với `audio.currentTime * 1000` (`currentTime` tính bằng giây).

### 23.9. Lỗi hay gặp ở frontend (MỚI)

| Lỗi | Hậu quả | Cách đúng |
|---|---|---|
| Quên gửi `genreId` khi đăng bài, hoặc viết `genre_id` | 400 `Genre is required` | Đúng key `genreId` |
| Viết cứng id thể loại (VD Chill = 24) | Sai trên môi trường khác | Luôn lấy từ `GET /api/genres` |
| Gửi canvas với key `video` hoặc `file` | 400 `File is required` | Key đúng là `canvas` |
| Video canvas không `muted` | Trình duyệt chặn tự chạy | Thêm `muted autoplay loop playsinline` |
| Gửi lời bài hát dạng form-data | 415 hoặc 400 | Gửi JSON `{ "content": "..." }` |
| Dùng `innerHTML` để hiện lời bài hát | Lỗ hổng XSS | Hiện bằng text (`textContent`, hoặc cách hiển thị mặc định của React/Vue) |
| Coi lời rỗng là lỗi | Hiện thông báo lỗi sai | Bài chưa có lời vẫn trả 200 với `lines: []` |
| Gọi gợi ý phát tiếp không gửi `excludeIds` | Phát lặp lại các bài vừa nghe | Gửi danh sách id đã phát |
| Gửi `Content-Type` tự đặt cho FormData | Server không đọc được form | Để axios / trình duyệt tự đặt |

---

## 24. CẤU HÌNH, MÔI TRƯỜNG VÀ DEPLOY

### 24.1. Hai môi trường

| | Local (máy cá nhân) | Production (VPS) |
|---|---|---|
| File cấu hình | `application.properties` | `application-prod.properties` (chạy với `-Dspring.profiles.active=prod`) |
| MySQL | `localhost`, user root | `localhost` trên VPS, user `myspotify` |
| `ddl-auto` | `update` (tự sửa bảng) | `validate` (chỉ kiểm tra, **không** tự sửa) |
| MongoDB database | `myspotify_db` | `myspotify_prod` |
| Thư mục upload | `uploads` | `/home/myspotify/uploads` |
| Múi giờ | Theo máy (Việt Nam) | Phải đặt `Asia/Ho_Chi_Minh` |

⚠️ Cả 2 file cấu hình **không đưa lên GitHub** (có trong `.gitignore`) vì chứa mật khẩu database, chuỗi kết nối MongoDB, khóa bí mật JWT. Thành viên mới xin file mẫu từ trưởng nhóm.

### 24.2. Các khóa cấu hình liên quan module Song

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

# Service AI Python (MỚI) — không khai báo thì dùng đúng 2 giá trị mặc định dưới đây
# (Lưu ý: file .properties không cho ghi chú ở cuối dòng, ghi chú phải nằm trên 1 dòng riêng)
app.recommend.base-url=http://127.0.0.1:8000
app.recommend.timeout-ms=3000
```

Chuỗi kết nối MongoDB phải có **tên database** giữa `.mongodb.net/` và `?`. Thiếu → lỗi khởi động `Database name must not be empty`.

### 24.2a. Dữ liệu cần có sẵn trong MySQL (MỚI)

| Dữ liệu | Cách tạo | Ghi chú |
|---|---|---|
| Bảng `genres` + 25 thể loại | Bảng do Hibernate tạo (local) hoặc tạo tay (VPS); 25 dòng thêm bằng `INSERT` chạy tay | Thiếu thể loại → không đăng bài được (`Genre does not exist`) |
| Bảng `song_lyrics` | Hibernate tạo (local) hoặc tạo tay (VPS) | |
| Cột `songs.genre_id`, `songs.canvas_url` | Hibernate thêm (local) hoặc `ALTER TABLE` (VPS) | |
| Tài khoản được vào web | `UPDATE users SET access_status = 'ACCEPT' WHERE email = '...';` | Tài khoản mới đăng ký mặc định `REFUSE` |

Câu `INSERT` 25 thể loại (chạy 1 lần trên database mới):
```sql
INSERT INTO genres (name) VALUES
('Pop'), ('V-Pop'), ('K-Pop'), ('US-UK'), ('Ballad'), ('Rap'), ('Hip-hop'), ('R&B'), ('Soul'),
('Rock'), ('Metal'), ('Indie'), ('Folk'), ('Country'), ('Blues'), ('Jazz'), ('Acoustic'),
('Bolero'), ('Classical'), ('EDM'), ('Dance'), ('Remix'), ('Lo-fi'), ('Chill'), ('OST');
```

### 24.3. Chạy test

```bash
./mvnw clean test        # Windows: .\mvnw clean test
```
Test cần kết nối được MySQL, Redis và MongoDB Atlas (IP máy phải nằm trong Network Access của Atlas).

Các test tự động hiện có: `SongOptimisticLockTest` (2 người cùng sửa 1 bài), `FileStorageServiceTest` (kiểm tra file), `OtpRaceConditionTest`, `RateLimitServiceTest`, `SessionServiceTest`, `MyspotifyApplicationTests`.

### 24.4. Deploy lên VPS (ĐÃ ĐỔI: tự động bằng GitHub Actions)

```
1. Làm tính năng trên 1 nhánh riêng (VD: git checkout -b ten-tinh-nang)
2. git push → lên GitHub tạo Pull Request vào main
3. GitHub Actions chạy bước CI: build bằng Maven (./mvnw -B package -DskipTests)
4. Merge Pull Request vào main → GitHub Actions SSH vào VPS và chạy deploy.sh
       deploy.sh: kéo code → build Maven → tắt bản cũ → chạy bản mới (profile prod) → kiểm tra sức khỏe
5. Xem log trên VPS: tail -f /home/myspotify/app.log
```
- **Không** push thẳng lên `main`: mọi thứ lên `main` đều được deploy ngay.
- CI hiện **chưa chạy test** (bỏ qua bằng `-DskipTests`), test chạy ở máy local.
- Khi thay đổi enum `SongStatus` hoặc cấu trúc bảng (thêm cột, thêm bảng): vì VPS dùng `ddl-auto=validate`, phải tự chạy `ALTER TABLE` / `CREATE TABLE` trên VPS **trước** khi merge, nếu không bản mới sẽ không khởi động được.

---

## 25. GIỚI HẠN ĐÃ BIẾT VÀ HƯỚNG PHÁT TRIỂN

### 25.1. Giới hạn hiện tại (chấp nhận được trong phạm vi dự án)

| # | Giới hạn | Ảnh hưởng | Hướng cải thiện |
|---|---|---|---|
| 1 | Tìm kiếm không xử lý ký tự `%` và `_` (ký tự đặc biệt của LIKE) | Tìm `%` ra mọi bài | Escape 2 ký tự này trước khi truy vấn |
| 2 | Tìm kiếm bằng `LIKE '%...%'` không dùng được index | Chậm khi có hàng trăm nghìn bài | Full-text index MySQL hoặc Elasticsearch |
| 3 | Sửa tên / đổi ảnh / đổi canvas / sửa lời bài APPROVED không cần duyệt lại | Artist có thể đổi thành nội dung xấu sau khi được duyệt | Chuyển về PENDING, hoặc tách "bản nháp chờ duyệt" |
| 4 | Lấy danh sách có thể phát sinh nhiều truy vấn phụ (N+1) khi đọc Artist và feat của từng bài | Chậm hơn khi trang lớn | `JOIN FETCH` / `@EntityGraph` |
| 5 | `/uploads/**` công khai | Ai có link đều tải được file | URL có chữ ký + thời hạn |
| 6 | File lưu trên ổ đĩa VPS | Ổ đĩa đầy, không nhân bản được khi có nhiều server | Object storage (S3, Cloudflare R2) |
| 7 | Phân trang theo offset | Có bài mới chen vào giữa lúc chuyển trang → trùng/thiếu 1 bài | Phân trang theo con trỏ (cursor) |
| 8 | Lịch sử lấy 100 lượt thô | Người nghe 1 bài hơn 100 lần liên tiếp có thể thấy < 10 bài | Dùng aggregation `$group` của MongoDB |
| 9 | `released` trong API quản trị không xét Artist bị khóa | Admin thấy `released = true` cho bài người nghe không thấy | Thêm điều kiện vào `SongMapper` |
| 10 | Thời gian không kèm múi giờ | Phụ thuộc cấu hình múi giờ server | Dùng `OffsetDateTime` / `Instant` |
| 11 | Không đổi được thể loại sau khi đăng | Chọn nhầm thể loại phải xóa bài đăng lại (mất lượt nghe) | API đổi thể loại kèm gửi duyệt lại |
| 12 | Bài cũ có `genre = null` | Không nằm ở trang Genres, không được ưu tiên "cùng thể loại" khi gợi ý | Gán thể loại cho bài cũ bằng SQL |
| 13 | Xóa bài (xóa mềm) không xóa dòng `song_lyrics` | Dữ liệu thừa trong database (không ai đọc được) | Xóa lời cùng lúc xóa bài |
| 14 | Lời bài hát không có cột `version` | 2 tab cùng sửa lời → lần lưu sau ghi đè lần trước | `@Version` |
| 15 | Gợi ý chỉ xét "bể" 50 bài nhiều lượt nghe nhất mỗi bước | Bài mới, ít lượt nghe khó được gợi ý | Trộn thêm bài mới phát hành |
| 16 | Gợi ý phát tiếp chưa dùng lịch sử nghe của từng người | Mọi người nghe cùng 1 bài nhận cùng 1 gợi ý | Kết hợp `listening_history` hoặc service AI |
| 17 | Service AI Python lỗi thì im lặng dùng dự phòng | Khó phát hiện AI đang tắt nếu không xem log | Health check riêng cho service Python |
| 18 | Canvas không kiểm tra độ phân giải, tỷ lệ khung hình | Video ngang/dọc lộn xộn trên giao diện | Kiểm tra tỷ lệ 9:16 như Spotify |
| 19 | `/uploads/songs/canvas/**` cũng công khai như file nhạc | Ai có link đều tải được video | Như dòng 5 |

### 25.2. Việc còn lại cần kiểm tra

- [x] ~~Handler cho `ObjectOptimisticLockingFailureException` trả **409**~~ — **đã có** (mục 19.1).
- [ ] Cập nhật sơ đồ use case (thêm thể loại, canvas, lời bài hát, gợi ý), ERD (thêm `genres`, `song_lyrics`, `playlists`, `playlist_songs`, `listening_history`), sơ đồ class theo code hiện tại.
- [ ] Đánh mã UC cho các chức năng mới (mục 4).
- [ ] Bổ sung test case Postman cho thể loại, canvas, lời bài hát, gợi ý vào `TEST_SONG_POSTMAN.md`.
- [ ] Viết bộ test case Excel từ `TEST_SONG_POSTMAN.md`.

### 25.3. Ý tưởng mở rộng

- **Đã làm:** playlist (`PLAYLIST_MODULE.md`), thể loại, canvas, lời bài hát, gợi ý phát tiếp, gợi ý theo thể loại bằng AI.
- **Chưa làm:** thích bài hát, album, bảng xếp hạng theo tuần (dựa trên `listening_history`), gợi ý theo lịch sử nghe của từng người, gợi ý bài để thêm vào playlist (đã có sẵn hàm `RecommendClient.playlistSongs`), thông báo cho Artist khi bài được duyệt / từ chối, đếm lượt nghe chỉ khi nghe đủ 30 giây, đổi thể loại có kiểm duyệt.

---

## 26. CÂU HỎI THƯỜNG GẶP

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
Đ: Trình duyệt cache 1 giờ. `coverUrl` mới là đường dẫn khác hẳn nên chỉ cần frontend dùng `coverUrl` mới trả về. Canvas cũng vậy.

**H: Đăng bài bị báo `Genre is required` dù đã chọn thể loại?**
Đ: Kiểm tra tên key trong form-data phải đúng `genreId` (phân biệt hoa thường), giá trị là **id** (số) chứ không phải tên thể loại.

**H: Bài cũ không có thể loại, không thấy ở trang Genres?**
Đ: Bài đăng trước khi có tính năng thể loại có `genre_id = NULL`. Gán bằng SQL: `UPDATE songs SET genre_id = <id> WHERE id = <songId>;`.

**H: Upload canvas bị báo sai độ dài dù video đúng 10 giây?**
Đ: Server làm tròn tới 0,1 giây theo thông tin trong file. Video xuất ra có thể dài 10,04 giây → làm tròn 10,0 là được, nhưng 10,06 → 10,1 bị từ chối. Cắt video ngắn hơn một chút (VD 9,5 giây). Message có ghi độ dài server đo được (`current: ...s`).

**H: Upload canvas bị `Only MP4 videos are allowed` dù file đuôi .mp4?**
Đ: Server kiểm tra `Content-Type` trình duyệt gửi lên phải là `video/mp4`. File `.mov` đổi đuôi, hoặc trình duyệt nhận diện sai, sẽ bị chặn. Xuất lại video đúng định dạng MP4 (H.264).

**H: Lời bài hát lưu xong bị khác chữ đã gõ?**
Đ: Server chuẩn hóa: bỏ dòng trống, dòng thẻ `[ar:...]`, viết lại mốc thời gian dạng `[mm:ss.xx]` (mục 15.5). Nội dung chữ không đổi.

**H: Người nghe không xem được lời dù Artist đã thêm?**
Đ: Lời chỉ hiện với bài **đã phát hành**. Bài đang chờ duyệt / chưa tới lịch / bị ẩn → `Song does not exist`.

**H: Tự phát tiếp cứ lặp lại mấy bài cũ?**
Đ: Frontend chưa gửi `excludeIds` (các bài đã phát). Xem mục 16.2.

**H: Gợi ý theo thể loại lúc nào cũng ra mấy bài nhiều lượt nghe nhất?**
Đ: Có thể service AI Python đang tắt hoặc lỗi → server dùng phương án dự phòng. Xem log VPS có dòng `Recommend service error (genres)` không.

**H: Vừa nghe xong, mở "Nghe gần đây" chưa thấy bài?**
Đ: Lịch sử được ghi bất đồng bộ (`@Async`), có thể trễ dưới 1 giây. Tải lại là thấy. Nếu vẫn không thấy → xem log `Could not save listening history`.

**H: Đăng nhập báo `Account has not been approved yet`?**
Đ: Tài khoản chưa được chủ dự án cho phép vào web (`access_status = REFUSE`). Liên hệ chủ dự án để đổi sang `ACCEPT`.

---

## 27. PHỤ LỤC

### 27.1. Bảng tổng hợp API

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
| 21 | GET | `/api/genres` | Đăng nhập | — | `GenreResponse[]` | Thể loại (mục 13) |
| 22 | PUT | `/api/artist/songs/{songId}/canvas` | ARTIST | form-data `canvas` | `SongManageResponse` | Canvas (mục 14) |
| 23 | DELETE | `/api/artist/songs/{songId}/canvas` | ARTIST | — | `SongManageResponse` | Canvas (mục 14) |
| 24 | GET | `/api/songs/{songId}/lyrics` | Đăng nhập | — | `LyricsResponse` | Lời (mục 15) |
| 25 | GET | `/api/artist/songs/{songId}/lyrics` | ARTIST | — | `LyricsResponse` | Lời (mục 15) |
| 26 | PUT | `/api/artist/songs/{songId}/lyrics` | ARTIST | JSON `content` | `LyricsResponse` | Lời (mục 15) |
| 27 | DELETE | `/api/artist/songs/{songId}/lyrics` | ARTIST | — | `{message}` | Lời (mục 15) |
| 28 | GET | `/api/songs/{songId}/recommendations?limit&excludeIds` | Đăng nhập | — | `SongResponse[]` | Gợi ý (mục 16.2) |
| 29 | GET | `/api/songs/{songId}/recommendations/artist?limit` | Đăng nhập | — | `SongResponse[]` | Gợi ý (mục 16.3) |
| 30 | GET | `/api/recommendations/genres?genreIds&limit` | Đăng nhập | — | `SongResponse[]` | Gợi ý (mục 16.4) |

(Dòng 4 là file tĩnh, không phải API → tổng cộng 29 API. Đăng bài số 7 có thêm key `genreId` bắt buộc; `GET /api/songs` số 1 có thêm `genreId` tùy chọn.)

### 27.2. Bảng message lỗi nghiệp vụ

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
| `Genre is required` | `UploadSongRequest` + `ArtistSongService.getGenre` | Thiếu thể loại khi đăng bài |
| `Genre does not exist` | `ArtistSongService.getGenre` | Id thể loại không tồn tại |
| `Video file is required` | `saveVideo` | |
| `Video must not exceed 50MB` | `saveVideo` | |
| `Only MP4 videos are allowed` | `saveVideo` | |
| `Invalid or corrupted video file` | `saveVideo`, `getVideoDurationMs` | |
| `Canvas video must be between 7 and 10 seconds (current: Xs)` | `ArtistSongService.checkCanvasDuration` | |
| `Song has no canvas video` | `ArtistSongService.removeCanvas` | |
| `Lyrics must not be empty` | `LyricsRequest` + `LyricsService.parseLyrics` | |
| `Lyrics must not exceed 10000 characters` | `LyricsRequest` | |
| `Lyrics must not exceed 300 lines` | `parseLyrics` | |
| `Line N: invalid time, seconds must be 00 - 59` | `parseLyrics` | |
| `Line N: time is longer than the song` | `parseLyrics` | |
| `Line N: time must be greater than the previous line` | `parseLyrics` | |
| `Line N: must not exceed 200 characters` | `parseLyrics` | |
| `Only X/Y lines have a time. Either all lines must have a time or none` | `parseLyrics` | Trộn lời thường và lời có mốc |
| `Song has no lyrics` | `LyricsService.deleteLyrics` | |
| `Genre ids are required` | `RecommendationService.getGenreSongs` | |
| `At most 10 genres are allowed` | `RecommendationService.getGenreSongs` | |
| `Account has not been approved yet` | `AuthService.login` | Chưa được cho phép vào web (module User) |

### 27.3. Bản đồ "muốn sửa X thì mở file nào"

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
| Thêm thể loại | Không sửa code — chạy `INSERT INTO genres (name) VALUES ('...')` trên local và VPS |
| Giới hạn canvas (dung lượng, định dạng) | `FileStorageService.saveVideo` (`MAX_VIDEO_SIZE`) |
| Độ dài canvas (7–10 giây) | `ArtistSongService.MIN_CANVAS_SECONDS`, `MAX_CANVAS_SECONDS` |
| Quy tắc lời bài hát (số dòng, độ dài dòng, định dạng thời gian) | `LyricsService` (`MAX_LINES`, `MAX_LINE_LENGTH`, `TIMED_LINE`, `TAG_LINE`) + `LyricsRequest` (10.000 ký tự) |
| Số bài cùng nghệ sĩ khi phát tiếp | `RecommendationService.SAME_ARTIST_LIMIT` |
| Kích thước bể ứng viên gợi ý | `RecommendationService.CANDIDATE_POOL` |
| Giới hạn `limit`, số thể loại khi gợi ý | `RecommendationService.MAX_LIMIT`, `MAX_GENRES` |
| Địa chỉ, thời gian chờ service AI Python | `app.recommend.base-url`, `app.recommend.timeout-ms` (file cấu hình) |
| Đường dẫn API gọi sang Python | `client/RecommendClient.java` |

### 27.4. Tài liệu liên quan

- `docs/TEST_SONG_POSTMAN.md` — hướng dẫn test bằng Postman (~300 test case; chưa có phần thể loại, canvas, lời, gợi ý)
- `docs/GENRE_FRONTEND.md` — hướng dẫn frontend phần thể loại nhạc
- `docs/PLAYLIST_MODULE.md` — module Playlist
- `docs/USER_MODULE.md` — tài khoản, đăng nhập, quyền vào web, xin làm Artist
- `docs/CACHING_REDIS.md` — tài liệu về Redis
- Swagger: `http://157.66.100.87:8080/swagger-ui.html` — thử API trực tiếp trên trình duyệt
- Comment trong từng file code — giải thích chi tiết từng dòng
- Comment trong từng file code — giải thích chi tiết từng dòng