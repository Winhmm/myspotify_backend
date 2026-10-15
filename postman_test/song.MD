# HƯỚNG DẪN TEST MODULE SONG BẰNG POSTMAN — MYSPOTIFY

> Tài liệu dành cho 2 bạn tester. Đọc hết **Phần 0 → Phần 3** trước khi bấm Send request đầu tiên.
> Mỗi test case có **mã riêng** (VD `AR-UP-05`). Khi báo lỗi, chỉ cần ghi mã, không cần mô tả lại.

---

## MỤC LỤC

- [Phần 0 — Thông tin chung](#phần-0--thông-tin-chung)
- [Phần 1 — Chuẩn bị Postman](#phần-1--chuẩn-bị-postman)
- [Phần 2 — Chuẩn bị tài khoản](#phần-2--chuẩn-bị-tài-khoản)
- [Phần 3 — Chuẩn bị file test](#phần-3--chuẩn-bị-file-test)
- [Phần 4 — Hiểu vòng đời của 1 bài hát](#phần-4--hiểu-vòng-đời-của-1-bài-hát)
- [Phần 5 — Artist: đăng bài (UC26)](#phần-5--artist-đăng-bài-uc26)
- [Phần 6 — Artist: bài hát của tôi (UC28) và tìm nghệ sĩ feat (UC37)](#phần-6--artist-bài-hát-của-tôi-uc28-và-tìm-nghệ-sĩ-feat-uc37)
- [Phần 7 — Admin: danh sách, duyệt, từ chối, gỡ bài (UC34–UC36)](#phần-7--admin-danh-sách-duyệt-từ-chối-gỡ-bài-uc34uc36)
- [Phần 8 — Artist: sửa bài, đổi ảnh, đổi lịch (UC29–UC31)](#phần-8--artist-sửa-bài-đổi-ảnh-đổi-lịch-uc29uc31)
- [Phần 9 — Artist: ẩn, hiện, xóa bài (UC32–UC33)](#phần-9--artist-ẩn-hiện-xóa-bài-uc32uc33)
- [Phần 10 — Người nghe: danh sách, tìm kiếm, chi tiết, nghe nhạc (UC20–UC25)](#phần-10--người-nghe-danh-sách-tìm-kiếm-chi-tiết-nghe-nhạc-uc20uc25)
- [Phần 11 — Lịch sử nghe gần đây (MongoDB)](#phần-11--lịch-sử-nghe-gần-đây-mongodb)
- [Phần 12 — Phân quyền và bảo mật](#phần-12--phân-quyền-và-bảo-mật)
- [Phần 13 — Kịch bản tổng hợp đầu-cuối](#phần-13--kịch-bản-tổng-hợp-đầu-cuối)
- [Phần 14 — Giới hạn đã biết (KHÔNG phải bug)](#phần-14--giới-hạn-đã-biết-không-phải-bug)
- [Phần 15 — Cách báo bug](#phần-15--cách-báo-bug)
- [Phụ lục — Bảng tổng hợp API](#phụ-lục--bảng-tổng-hợp-api)

---

## PHẦN 0 — THÔNG TIN CHUNG

### 0.1. Địa chỉ server

| Mục | Giá trị |
|---|---|
| Base URL | `http://157.66.100.87:8080` |
| Swagger (xem danh sách API trên trình duyệt) | `http://157.66.100.87:8080/swagger-ui.html` |
| File nhạc / ảnh | `http://157.66.100.87:8080/uploads/...` (không cần token) |

Server chạy giờ Việt Nam (`Asia/Ho_Chi_Minh`). Mọi thời gian trong JSON (`releaseAt`, `createdAt`, `playedAt`...) đều là **giờ Việt Nam**, dạng `2026-10-15T17:30:00`, không có chữ `Z` hay `+07:00` ở cuối.

### 0.2. Phân công gợi ý cho 2 bạn

| | Tester A | Tester B |
|---|---|---|
| Vai chính | Artist + Admin | Người nghe + Bảo mật |
| Làm các phần | 5, 6, 7, 8, 9 | 10, 11, 12 |
| Làm chung | Phần 13 (kịch bản đầu-cuối) — 2 bạn phối hợp | |

Tester B cần bài hát đã phát hành để test. Nếu trên server chưa có đủ, **Tester A làm Phần 5 + Phần 7 trước** để có bài, rồi báo cho Tester B bắt đầu.

### 0.3. Quy tắc QUAN TRỌNG: mỗi người dùng tài khoản riêng

Hệ thống chỉ cho **1 tài khoản đăng nhập ở 1 nơi tại 1 thời điểm**. Nếu 2 bạn cùng đăng nhập 1 tài khoản:
- Người đăng nhập sau sẽ "đá" người đăng nhập trước.
- Token cũ bị vô hiệu, mọi request trả **401** với message:
  `Your session has ended. Your account may have been logged in on another device`

→ Đây là tính năng, **không phải bug**. Mỗi bạn dùng bộ tài khoản riêng (xem Phần 2).

Cũng vì lý do này: nếu bạn bấm Login lại trong Postman, phải **cập nhật token mới** vào biến môi trường, token cũ không dùng được nữa.

### 0.4. Mã trạng thái HTTP sẽ gặp

| Mã | Ý nghĩa | Khi nào gặp |
|---|---|---|
| **200** | Thành công | Request hợp lệ |
| **400** | Dữ liệu sai / vi phạm nghiệp vụ | Thiếu trường, sai định dạng, bài không đúng trạng thái... |
| **401** | Chưa đăng nhập / token sai / phiên đã hết | Không gửi token, token hỏng, bị đá ra |
| **403** | Đã đăng nhập nhưng không đủ quyền | USER gọi API của ARTIST, ARTIST gọi API của ADMIN |
| **409** | Xung đột dữ liệu | 2 người sửa cùng 1 bài cùng lúc (khó tái hiện bằng tay) |
| **413** | File quá lớn so với giới hạn upload của server | Gửi file rất lớn |
| **415** | Sai Content-Type | Gửi JSON vào API yêu cầu form-data |
| **429** | Gửi quá nhiều request | Bấm Send liên tục quá nhanh |
| **500** | Lỗi server | **Luôn là bug** (trừ khi đã ghi rõ trong tài liệu) → báo ngay |

### 0.5. Định dạng lỗi

Hầu hết lỗi trả về JSON 1 trường:
```json
{ "message": "Song does not exist" }
```

Lỗi 401/403 do lớp bảo mật trả về:
```json
{ "message": "Unauthorized, please login" }
{ "message": "Your session has ended. Your account may have been logged in on another device" }
{ "message": "You do not have permission to access this resource" }
```

Khi test, **so sánh đúng từng chữ** của `message` với tài liệu. Sai 1 chữ cũng ghi lại (frontend có thể đang dựa vào message để hiển thị).

---

## PHẦN 1 — CHUẨN BỊ POSTMAN

### 1.1. Tạo Environment

1. Postman → góc phải trên chọn **Environments** (hoặc biểu tượng con mắt) → **Create Environment** → đặt tên `MySpotify VPS`.
2. Thêm các biến sau (cột **Initial value** và **Current value** điền giống nhau):

| Variable | Giá trị ban đầu | Dùng để |
|---|---|---|
| `baseUrl` | `http://157.66.100.87:8080` | Địa chỉ server |
| `tokenUser` | *(để trống)* | Token tài khoản USER |
| `tokenArtist` | *(để trống)* | Token Artist chính |
| `tokenArtist2` | *(để trống)* | Token Artist thứ 2 (để test feat, test bài của người khác) |
| `tokenAdmin` | *(để trống)* | Token Admin |
| `songId` | *(để trống)* | Id bài vừa đăng |
| `artistId` | *(để trống)* | Id Artist chính |
| `artist2Id` | *(để trống)* | Id Artist thứ 2 |
| `userId` | *(để trống)* | Id tài khoản USER |

3. Bấm **Save**, rồi chọn `MySpotify VPS` ở ô chọn environment góc phải trên (chỗ đang ghi *No Environment*).

### 1.2. Tạo Collection theo thư mục

Tạo collection `MySpotify - Song` với các thư mục:
```
MySpotify - Song
├── 00 Auth
├── 01 Artist - Upload
├── 02 Artist - Quản lý
├── 03 Admin
├── 04 Người nghe
├── 05 Lịch sử nghe
└── 06 Bảo mật
```
Mỗi test case lưu thành 1 request riêng, đặt tên theo mã test (VD `AR-UP-01 Upload thành công`). Sau này test lại chỉ cần bấm lại.

### 1.3. Gắn token vào request

Trong mỗi request: tab **Authorization** → Type = **Bearer Token** → ô Token gõ `{{tokenArtist}}` (hoặc `{{tokenAdmin}}`, `{{tokenUser}}`... tùy vai).

Mẹo: đặt Authorization ở cấp **thư mục** (bấm vào thư mục → tab Authorization), các request bên trong chọn *Inherit auth from parent* → không phải gõ lại từng request.

### 1.4. Tự động lưu token sau khi đăng nhập

Trong request Login, tab **Scripts** → **Post-response**, dán:
```js
const data = pm.response.json();
if (pm.response.code === 200) {
    // Đổi "tokenArtist" thành tên biến tương ứng với tài khoản đang đăng nhập
    pm.environment.set("tokenArtist", data.token);
    pm.environment.set("artistId", data.userId);
    console.log("Đã lưu token cho", data.username, "- role:", data.role);
}
```
Nên tạo **4 request Login riêng** (Login USER, Login ARTIST, Login ARTIST2, Login ADMIN), mỗi cái lưu vào biến riêng.

### 1.5. Tự động lưu songId sau khi upload

Trong request upload bài (Phần 5), tab **Scripts** → **Post-response**:
```js
if (pm.response.code === 200) {
    pm.environment.set("songId", pm.response.json().id);
    console.log("songId =", pm.response.json().id);
}
```

### 1.6. Gửi form-data có file (rất hay sai)

1. Tab **Body** → chọn **form-data**.
2. Gõ key, ví dụ `audio`.
3. Rê chuột vào **cuối ô Key**, sẽ hiện dropdown **Text / File** → chọn **File**.
4. Ô Value đổi thành nút **Select Files** → chọn file trên máy.
5. **KHÔNG** tự thêm header `Content-Type` — Postman tự đặt `multipart/form-data; boundary=...`. Tự đặt tay sẽ làm request hỏng.

### 1.7. Xem console

Menu dưới cùng → **Console** (hoặc `Ctrl + Alt + C`). Console hiện request thật đã gửi, rất hữu ích khi nghi ngờ Postman gửi sai.

---

## PHẦN 2 — CHUẨN BỊ TÀI KHOẢN

Mỗi tester cần **4 tài khoản**:

| Vai | Ví dụ username | Ghi chú |
|---|---|---|
| USER | `testerA_user` | Người nghe bình thường |
| ARTIST | `testerA_artist` | Artist chính, đăng bài |
| ARTIST 2 | `testerA_artist2` | Để feat, và để thử sửa bài của người khác |
| ADMIN | *(xin Winhmm)* | Admin không tự đăng ký được |

Dùng email thật (hoặc email phụ) vì cần nhận mã OTP.

### 2.1. Đăng ký + kích hoạt

**Đăng ký**
```
POST {{baseUrl}}/api/auth/register
Body → raw → JSON:
{
    "username": "testerA_user",
    "email": "emailcuaban+user@gmail.com",
    "password": "matkhau123"
}
```
Ràng buộc: username 5–20 ký tự, password 6–72 ký tự.
Mong đợi: `200` — `"Registration successful, the account is awaiting OTP verification"`.

> Mẹo Gmail: `ten+user@gmail.com`, `ten+artist@gmail.com` đều về cùng hộp thư `ten@gmail.com` → 1 hộp thư tạo được nhiều tài khoản.

**Xác thực OTP** (mở email lấy mã)
```
POST {{baseUrl}}/api/auth/verify-otp
{
    "email": "emailcuaban+user@gmail.com",
    "otpCode": "123456"
}
```
Mong đợi: `200` — `"Verification successful, the account has been activated"`.

**Đăng nhập**
```
POST {{baseUrl}}/api/auth/login
{
    "email": "emailcuaban+user@gmail.com",
    "password": "matkhau123"
}
```
Mong đợi: `200`, JSON có `token`, `userId`, `username`, `role`.

### 2.2. Nâng tài khoản lên ARTIST

ARTIST **không được Admin gán thẳng**. Phải: người dùng gửi yêu cầu → Admin duyệt.

**B1 — Tài khoản (đang là USER) gửi yêu cầu** (token của chính tài khoản đó)
```
POST {{baseUrl}}/api/users/me/artist-request
{
    "artistName": "Tester A Artist",
    "bio": "Tài khoản test của Tester A"
}
```
Mong đợi: `200` — `"Artist request has been submitted"`.

> Đặt `artistName` dễ nhận ra và **khác nhau giữa 2 tester** (VD `TA Artist`, `TA Artist Two`, `TB Artist`...) để lúc test tìm kiếm không bị lẫn.

**B2 — Admin xem danh sách yêu cầu** (token Admin)
```
GET {{baseUrl}}/api/admin/artist-requests
```
Tìm đúng tài khoản, ghi lại `id`.

**B3 — Admin duyệt**
```
PUT {{baseUrl}}/api/admin/artist-requests/{userId}/approve
```
Mong đợi: `200` — `"Artist request has been approved"`.

**B4 — Đăng nhập lại** tài khoản Artist, kiểm tra `"role": "ARTIST"` trong kết quả, lưu token vào `tokenArtist`.

Làm tương tự cho Artist 2 → lưu `tokenArtist2`, `artist2Id`.

### 2.3. Checklist sau Phần 2

- [ ] `tokenUser` có giá trị, role = USER
- [ ] `tokenArtist` có giá trị, role = ARTIST, `artistId` đã lưu
- [ ] `tokenArtist2` có giá trị, role = ARTIST, `artist2Id` đã lưu
- [ ] `tokenAdmin` có giá trị, role = ADMIN
- [ ] Ghi lại `userId` của tài khoản USER (dùng ở test feat sai)

---

## PHẦN 3 — CHUẨN BỊ FILE TEST

Tạo thư mục `test-files` trên máy, chuẩn bị đủ các file sau:

| Tên file gợi ý | Mô tả | Cách tạo | Dùng ở |
|---|---|---|---|
| `song1.mp3` | MP3 thật, 2–8MB | Bất kỳ bài nhạc nào | Upload thành công |
| `song2.mp3` | MP3 thật khác | | Upload thêm bài |
| `song3.wav` | WAV thật, < 100MB | Đổi 1 đoạn nhạc ngắn sang WAV bằng tool online | Test WAV |
| `fake.mp3` | File chữ đổi đuôi | Tạo `a.txt` gõ vài chữ → đổi tên thành `fake.mp3` | Chặn file giả |
| `note.txt` | File chữ | | Sai định dạng |
| `video.mp4` | Video bất kỳ | | Sai định dạng |
| `big.mp3` | MP3 > 20MB | Bài nhạc dài / chất lượng cao | Vượt giới hạn MP3 |
| `cover.jpg` | Ảnh JPG < 2MB | | Ảnh bìa hợp lệ |
| `cover.png` | Ảnh PNG < 2MB | | |
| `cover.webp` | Ảnh WEBP < 2MB | Đổi ảnh sang WEBP bằng tool online | |
| `cover2.jpg` | JPG khác < 2MB | | Đổi ảnh bìa |
| `anim.gif` | Ảnh GIF | | Sai định dạng ảnh |
| `big.jpg` | Ảnh JPG > 2MB | Ảnh chụp điện thoại thường > 2MB | Vượt 2MB |
| `fake.jpg` | File chữ đổi đuôi | `a.txt` → `fake.jpg` | Chặn ảnh giả |

Ghi lại **thời lượng thật** của `song1.mp3` (xem trong trình phát nhạc trên máy) để đối chiếu với `durationSeconds` server trả về.

> Để có thể test giới hạn 10 bài ở Phần 11, cần **ít nhất 11 bài đã phát hành**. Có thể dùng chung 1–2 file mp3 để upload nhiều lần với tên khác nhau.

---

## PHẦN 4 — HIỂU VÒNG ĐỜI CỦA 1 BÀI HÁT

Đọc kỹ phần này — gần như mọi test case đều xoay quanh việc bài hát đang ở trạng thái nào.

### 4.1. Các trạng thái (`status`)

| status | Nghĩa | Người nghe thấy? |
|---|---|---|
| `PENDING` | Mới đăng / vừa sửa sau khi bị từ chối, đang chờ Admin duyệt | Không |
| `APPROVED` | Admin đã duyệt | Có — **nếu đã tới giờ phát hành** |
| `REJECTED` | Admin từ chối (bài PENDING) **hoặc** Admin gỡ (bài APPROVED/HIDDEN). Có `rejectReason` | Không |
| `HIDDEN` | Artist tự ẩn bài đã duyệt | Không |
| `DELETED` | Artist xóa bài. File nhạc + ảnh bị xóa khỏi server | Không, Artist cũng không thấy nữa |

### 4.2. Sơ đồ chuyển trạng thái

```
                  Artist đăng
                      │
                      ▼
               ┌────────────┐   Admin reject (có lý do)   ┌────────────┐
               │  PENDING   │ ──────────────────────────► │  REJECTED  │
               └────────────┘                              └────────────┘
                 │      ▲                                    │      ▲
   Admin approve │      │  Artist sửa tên/feat hoặc đổi ảnh  │      │
                 │      └────────────────────────────────────┘      │
                 ▼                                                  │
               ┌────────────┐      Admin remove (có lý do)          │
               │  APPROVED  │ ──────────────────────────────────────┤
               └────────────┘                                       │
                 │      ▲                                           │
   Artist hide   │      │  Artist show                              │
                 ▼      │                                           │
               ┌────────────┐      Admin remove (có lý do)          │
               │   HIDDEN   │ ──────────────────────────────────────┘
               └────────────┘

   Từ BẤT KỲ trạng thái nào (trừ DELETED) ── Artist delete ──► DELETED (không quay lại được)
```

### 4.3. "Đã phát hành" nghĩa là gì?

Một bài được người nghe nhìn thấy khi **đủ cả 3 điều kiện**:
1. `status = APPROVED`
2. `releaseAt` ≤ giờ hiện tại của server
3. Artist chính đang hoạt động (không bị khóa) và vẫn là ARTIST

Trong API của Artist/Admin, trường `released: true/false` cho biết bài đã phát hành chưa.

| status | released | Frontend hiển thị |
|---|---|---|
| PENDING | false | Chờ duyệt |
| APPROVED | false | Đã lên lịch (duyệt rồi, chưa tới giờ) |
| APPROVED | true | Đã phát hành |
| REJECTED | false | Bị từ chối (kèm lý do) |
| HIDDEN | false | Đã ẩn |

### 4.4. Lịch phát hành (`releaseAt`)

- Artist **không gửi** `releaseAt` khi đăng → `releaseAt = null` → khi Admin duyệt, server tự gán `releaseAt = giờ duyệt` → phát hành ngay.
- Artist **gửi** `releaseAt` ở tương lai → Admin duyệt xong bài vẫn chưa hiện, tới đúng giờ mới hiện.
- `releaseAt` phải ở **tương lai** tại thời điểm gửi, gửi giờ quá khứ bị chặn.

---

## PHẦN 5 — ARTIST: ĐĂNG BÀI (UC26)

**API:** `POST {{baseUrl}}/api/artist/songs`
**Token:** `{{tokenArtist}}`
**Body:** form-data

| Key | Loại | Bắt buộc | Ghi chú |
|---|---|---|---|
| `title` | Text | Có | 1–150 ký tự, không toàn dấu cách |
| `audio` | File | Có | `.mp3` (≤ 20MB) hoặc `.wav` (≤ 100MB) |
| `cover` | File | Không | JPG / PNG / WEBP, ≤ 2MB |
| `featuredArtistIds` | Text | Không | Id của Artist khác. Feat nhiều người → **thêm nhiều dòng cùng key** |
| `releaseAt` | Text | Không | `2026-12-24T20:00:00`, phải ở tương lai |

**JSON trả về khi thành công (SongManageResponse):**
```json
{
    "id": 15,
    "title": "Bài Test 1",
    "audioUrl": "/uploads/songs/audio/3f2a9c1e-....mp3",
    "coverUrl": "/uploads/songs/covers/9b1c4d7f-....jpg",
    "durationSeconds": 215,
    "playCount": 0,
    "status": "PENDING",
    "released": false,
    "releaseAt": null,
    "rejectReason": null,
    "artistId": 23,
    "artistName": "TA Artist",
    "featuredArtists": [],
    "createdAt": "2026-10-15T17:30:00",
    "updatedAt": "2026-10-15T17:30:00"
}
```

### 5.1. Các trường hợp thành công

| Mã | Mô tả | Body | Kết quả mong đợi |
|---|---|---|---|
| **AR-UP-01** | Chỉ title + audio MP3 | `title`=`TA Bài 01`, `audio`=song1.mp3 | 200. `status`=`PENDING`, `released`=false, `coverUrl`=null, `releaseAt`=null, `playCount`=0, `featuredArtists`=[] |
| **AR-UP-02** | Kiểm tra thời lượng | (dùng kết quả AR-UP-01) | `durationSeconds` khớp thời lượng thật của file (lệch ≤ 1–2 giây là chấp nhận được) |
| **AR-UP-03** | Có ảnh bìa JPG | thêm `cover`=cover.jpg | 200. `coverUrl` bắt đầu bằng `/uploads/songs/covers/` và kết thúc `.jpg` |
| **AR-UP-04** | Ảnh bìa PNG | `cover`=cover.png | 200, `coverUrl` kết thúc `.png` |
| **AR-UP-05** | Ảnh bìa WEBP | `cover`=cover.webp | 200, `coverUrl` kết thúc `.webp` |
| **AR-UP-06** | File WAV | `audio`=song3.wav | 200, `audioUrl` kết thúc `.wav`, `durationSeconds` đúng |
| **AR-UP-07** | Feat 1 người | thêm `featuredArtistIds`=`{{artist2Id}}` | 200, `featuredArtists` có đúng 1 phần tử `{ "id": ..., "artistName": "..." }` của Artist 2 |
| **AR-UP-08** | Feat nhiều người | 2 dòng `featuredArtistIds`: `{{artist2Id}}` và id 1 Artist khác (của tester kia) | 200, `featuredArtists` có 2 phần tử |
| **AR-UP-09** | Feat trùng id | 2 dòng `featuredArtistIds` cùng = `{{artist2Id}}` | 200, `featuredArtists` chỉ có **1** phần tử (server tự bỏ trùng) |
| **AR-UP-10** | Có lịch phát hành | `releaseAt`=`2026-12-24T20:00:00` (hoặc ngày tương lai bất kỳ) | 200, `releaseAt`=`2026-12-24T20:00:00`, `status`=PENDING |
| **AR-UP-11** | Title có dấu cách 2 đầu | `title`=`   TA Bài Cách   ` | 200, `title`=`TA Bài Cách` (đã cắt dấu cách 2 đầu) |
| **AR-UP-12** | Title tiếng Việt + emoji | `title`=`Chiều Mưa Hà Nội 🌧️` | 200, hiển thị đúng dấu, không bị lỗi font `?` |
| **AR-UP-13** | Title đúng 150 ký tự | Chuỗi đúng 150 ký tự | 200 |
| **AR-UP-14** | Ô featuredArtistIds để trống | 1 dòng `featuredArtistIds` với value rỗng | 200, `featuredArtists`=[] |
| **AR-UP-15** | Mở file trên trình duyệt | Lấy `audioUrl` ở AR-UP-01, mở `{{baseUrl}}` + audioUrl, VD `http://157.66.100.87:8080/uploads/songs/audio/xxx.mp3` | Trình duyệt phát được nhạc, tua được. Không cần đăng nhập |
| **AR-UP-16** | Mở ảnh bìa | Mở `{{baseUrl}}` + coverUrl của AR-UP-03 | Hiện đúng ảnh |
| **AR-UP-17** | Tên file gốc bị thay | So sánh `audioUrl` với tên file gốc | Tên trên server là chuỗi ngẫu nhiên (UUID), **không** chứa tên file gốc |
| **AR-UP-18** | Đuôi viết hoa | Đổi tên song1.mp3 thành `SONG1.MP3` rồi upload | 200, `audioUrl` kết thúc `.mp3` (chữ thường) |

> Sau AR-UP-01, nhớ kiểm tra biến `songId` đã được lưu tự động (Phần 1.5).

### 5.2. Các trường hợp lỗi — dữ liệu chữ

| Mã | Mô tả | Body | Kết quả mong đợi |
|---|---|---|---|
| **AR-UP-20** | Thiếu title | Bỏ key `title` | 400 `"Title is required"` |
| **AR-UP-21** | Title rỗng | `title`=`` (để trống) | 400 `"Title is required"` |
| **AR-UP-22** | Title toàn dấu cách | `title`=`     ` | 400 `"Title is required"` |
| **AR-UP-23** | Title 151 ký tự | Chuỗi 151 ký tự | 400 `"Title must not exceed 150 characters"` |
| **AR-UP-24** | Key viết sai hoa thường | Key `Title` thay vì `title` | 400 `"Title is required"` (server không nhận key `Title`) |
| **AR-UP-25** | releaseAt ở quá khứ | `releaseAt`=`2020-01-01T10:00:00` | 400 `"Release time must be in the future"` |
| **AR-UP-26** | releaseAt = đúng giờ hiện tại | Giờ hiện tại (chính xác đến phút) | 400 `"Release time must be in the future"` (đến lúc server xử lý thì đã thành quá khứ) |
| **AR-UP-27** | releaseAt sai định dạng | `releaseAt`=`24/12/2026` | 400 `"Invalid value for releaseAt"` |
| **AR-UP-28** | releaseAt thiếu giờ | `releaseAt`=`2026-12-24` | 400 `"Invalid value for releaseAt"` |
| **AR-UP-29** | releaseAt ngày không tồn tại | `releaseAt`=`2026-02-30T10:00:00` | 400 `"Invalid value for releaseAt"` |

### 5.3. Các trường hợp lỗi — feat

| Mã | Mô tả | Body | Kết quả mong đợi |
|---|---|---|---|
| **AR-UP-30** | Feat chính mình | `featuredArtistIds`=`{{artistId}}` | 400 `"You cannot feature yourself"` |
| **AR-UP-31** | Feat id không tồn tại | `featuredArtistIds`=`999999` | 400 `"Some featured artists do not exist"` |
| **AR-UP-32** | Feat tài khoản USER | `featuredArtistIds`=`{{userId}}` (tài khoản USER) | 400 `"Featured artist is not available: <id>"` |
| **AR-UP-33** | Feat 6 người | 6 dòng `featuredArtistIds`, 6 id khác nhau | 400 `"Maximum 5 featured artists"` |
| **AR-UP-34** | Feat id là chữ | `featuredArtistIds`=`abc` | 400, message bắt đầu bằng `"Invalid value for featuredArtistIds"` |
| **AR-UP-35** | Feat Artist đã bị khóa | Nhờ Admin khóa Artist 2 (Phần 12), rồi feat Artist 2 | 400 `"Featured artist is not available: <id>"`. **Nhớ mở khóa lại sau test** |
| **AR-UP-36** | Feat 5 người (đúng giới hạn) | 5 id Artist hợp lệ khác nhau (nếu có đủ) | 200 |

> Khi feat bị lỗi, **không được** có file rác nào được lưu lên server. Không kiểm tra được bằng Postman — chỉ cần ghi nhận request trả 400.

### 5.4. Các trường hợp lỗi — file

| Mã | Mô tả | Body | Kết quả mong đợi |
|---|---|---|---|
| **AR-UP-40** | Thiếu audio | Bỏ key `audio` | 400 `"Audio file is required"` |
| **AR-UP-41** | Có key audio nhưng không chọn file | Key `audio` loại File, không chọn file | 400 `"Audio file is required"` |
| **AR-UP-42** | Audio là file .txt | `audio`=note.txt | 400 `"Only MP3 or WAV files are allowed"` |
| **AR-UP-43** | Audio là .mp4 | `audio`=video.mp4 | 400 `"Only MP3 or WAV files are allowed"` |
| **AR-UP-44** | File giả mạo đuôi .mp3 | `audio`=fake.mp3 | 400 `"Invalid or corrupted audio file"` |
| **AR-UP-45** | MP3 > 20MB | `audio`=big.mp3 | 400 `"MP3 file must not exceed 20MB"` **hoặc** 413 `"File is too large"` (tùy file to cỡ nào) |
| **AR-UP-46** | Ảnh bìa GIF | `cover`=anim.gif | 400 `"Only JPG, PNG or WEBP images are allowed"` |
| **AR-UP-47** | Ảnh bìa > 2MB | `cover`=big.jpg | 400 `"Image must not exceed 2MB"` |
| **AR-UP-48** | Ảnh bìa giả mạo | `cover`=fake.jpg | 400 `"Invalid or corrupted image file"` hoặc `"Only JPG, PNG or WEBP images are allowed"` |
| **AR-UP-49** | Ảnh bìa là file mp3 | `cover`=song1.mp3 | 400 `"Only JPG, PNG or WEBP images are allowed"` hoặc `"Image must not exceed 2MB"` |
| **AR-UP-50** | Audio hợp lệ + ảnh lỗi | `audio`=song1.mp3, `cover`=anim.gif | 400. Sau đó gọi `GET /api/artist/songs` → **không** có bài mới nào được tạo |
| **AR-UP-51** | Gửi JSON thay vì form-data | Body → raw → JSON `{"title":"abc"}` | 415 (Content-Type không được hỗ trợ) |

### 5.5. Các trường hợp lỗi — quyền

| Mã | Mô tả | Token | Kết quả mong đợi |
|---|---|---|---|
| **AR-UP-60** | Không có token | Authorization = No Auth | 401 |
| **AR-UP-61** | Tài khoản USER đăng bài | `{{tokenUser}}` | 403 |
| **AR-UP-62** | Token sai | Bearer `abc.def.ghi` | 401 |
| **AR-UP-63** | Admin đăng bài | `{{tokenAdmin}}` | 403 (Admin không phải Artist) |

---

## PHẦN 6 — ARTIST: BÀI HÁT CỦA TÔI (UC28) VÀ TÌM NGHỆ SĨ FEAT (UC37)

### 6.1. Bài hát của tôi

**API:** `GET {{baseUrl}}/api/artist/songs`
**Token:** `{{tokenArtist}}`
Trả về **mảng** (không phân trang) các bài của mình, mới đăng ở đầu, **mọi trạng thái trừ DELETED**.

| Mã | Mô tả | Kết quả mong đợi |
|---|---|---|
| **AR-MY-01** | Xem danh sách | 200, mảng chứa các bài vừa đăng ở Phần 5. Bài đăng sau cùng nằm đầu (`createdAt` giảm dần) |
| **AR-MY-02** | Có đủ trường quản lý | Mỗi phần tử có `status`, `released`, `rejectReason`, `createdAt`, `updatedAt` |
| **AR-MY-03** | Không thấy bài của người khác | Gọi bằng `{{tokenArtist2}}` | Chỉ có bài của Artist 2, **không** có bài của Artist 1 |
| **AR-MY-04** | Bài mình được feat | Artist 1 feat Artist 2 (AR-UP-07). Gọi bằng `{{tokenArtist2}}` | Bài đó **không** xuất hiện (đây là danh sách bài mình là Artist chính) |
| **AR-MY-05** | Artist mới chưa đăng bài nào | Artist mới tạo, chưa upload | 200, `[]` |
| **AR-MY-06** | USER gọi | `{{tokenUser}}` | 403 |
| **AR-MY-07** | Bài bị từ chối hiện lý do | Sau khi làm AD-RJ-01 (Phần 7) | Bài đó có `status`=`REJECTED`, `rejectReason`=lý do Admin ghi |
| **AR-MY-08** | Bài đã xóa không hiện | Sau khi làm AR-DEL-01 (Phần 9) | Bài đã xóa **biến mất** khỏi danh sách |

### 6.2. Tìm nghệ sĩ để feat

**API:** `GET {{baseUrl}}/api/artist/search-artists?keyword=...`
**Token:** `{{tokenArtist}}`
Trả về mảng tối đa 10 nghệ sĩ: `[ { "id": 30, "artistName": "TA Artist Two" } ]`

| Mã | Mô tả | Request | Kết quả mong đợi |
|---|---|---|---|
| **AR-SA-01** | Tìm đúng tên | `?keyword=TA Artist Two` | Có Artist 2 |
| **AR-SA-02** | Tìm 1 phần tên | `?keyword=Two` | Có Artist 2 |
| **AR-SA-03** | Không phân biệt hoa thường | `?keyword=ta artist two` | Có Artist 2 |
| **AR-SA-04** | Không trả về chính mình | `?keyword=TA Artist` | Có Artist 2 nhưng **không có** Artist 1 (người đang gọi) |
| **AR-SA-05** | Keyword rỗng | `?keyword=` | 200, `[]` |
| **AR-SA-06** | Keyword toàn dấu cách | `?keyword=%20%20%20` | 200, `[]` |
| **AR-SA-07** | Thiếu tham số keyword | Bỏ hẳn `?keyword=` | 400 |
| **AR-SA-08** | Không có kết quả | `?keyword=zzzzkhongtontai` | 200, `[]` |
| **AR-SA-09** | Tối đa 10 kết quả | `?keyword=a` (chữ phổ biến) | Tối đa **10** phần tử |
| **AR-SA-10** | Không trả về USER | Tìm theo tên tài khoản USER | USER không xuất hiện (USER không có artistName) |
| **AR-SA-11** | Không trả về Artist bị khóa | Admin khóa Artist 2 → tìm Artist 2 | Không xuất hiện. **Mở khóa lại sau test** |
| **AR-SA-12** | Tìm có dấu tiếng Việt | Đặt 1 artistName có dấu, VD `Mây Chiều Test`, tìm `?keyword=Mây` | Có kết quả |
| **AR-SA-13** | USER gọi | `{{tokenUser}}` | 403 |
| **AR-SA-14** | Có dấu cách 2 đầu | `?keyword=%20Two%20` | Vẫn tìm thấy Artist 2 (server cắt dấu cách) |

---

## PHẦN 7 — ADMIN: DANH SÁCH, DUYỆT, TỪ CHỐI, GỠ BÀI (UC34–UC36)

**Token:** `{{tokenAdmin}}` cho toàn bộ phần này.

### 7.1. Danh sách bài (UC34)

**API:** `GET {{baseUrl}}/api/admin/songs?status=...&page=0&size=20`
Trả về dạng phân trang:
```json
{ "content": [ ...SongManageResponse... ], "page": 0, "size": 20, "totalElements": 35, "totalPages": 2 }
```
Sắp xếp: bài đăng mới nhất ở đầu.

| Mã | Mô tả | Request | Kết quả mong đợi |
|---|---|---|---|
| **AD-LS-01** | Không lọc | `GET /api/admin/songs` | 200, mọi bài **trừ DELETED**. `page`=0, `size`=20 |
| **AD-LS-02** | Lọc PENDING | `?status=PENDING` | Chỉ có bài PENDING. Thấy các bài vừa đăng ở Phần 5 |
| **AD-LS-03** | Lọc APPROVED | `?status=APPROVED` | Chỉ có bài APPROVED |
| **AD-LS-04** | Lọc REJECTED | `?status=REJECTED` | Chỉ có bài REJECTED |
| **AD-LS-05** | Lọc HIDDEN | `?status=HIDDEN` | Chỉ có bài HIDDEN |
| **AD-LS-06** | Lọc DELETED | `?status=DELETED` | Chỉ có bài đã bị Artist xóa (Admin vẫn xem được để đối chiếu) |
| **AD-LS-07** | status viết thường | `?status=pending` | 400 `"Invalid parameter: status"` |
| **AD-LS-08** | status sai | `?status=ABC` | 400 `"Invalid parameter: status"` |
| **AD-LS-09** | Phân trang | `?size=2&page=0` rồi `?size=2&page=1` | Mỗi trang 2 bài, 2 trang **không trùng** bài. `totalPages` = làm tròn lên (`totalElements` / 2) |
| **AD-LS-10** | size quá lớn | `?size=1000` | 200, `size` trong JSON = **50** (server tự giới hạn) |
| **AD-LS-11** | size = 0 | `?size=0` | 200, `size` = **1** |
| **AD-LS-12** | size âm | `?size=-5` | 200, `size` = **1** |
| **AD-LS-13** | page âm | `?page=-1` | 200, `page` = **0** |
| **AD-LS-14** | page vượt quá | `?page=9999` | 200, `content`=[] , `totalElements` vẫn đúng |
| **AD-LS-15** | page là chữ | `?page=abc` | 400 `"Invalid parameter: page"` |
| **AD-LS-16** | Artist gọi | `{{tokenArtist}}` | 403 |
| **AD-LS-17** | USER gọi | `{{tokenUser}}` | 403 |

### 7.2. Duyệt bài (UC35)

**API:** `PUT {{baseUrl}}/api/admin/songs/{songId}/approve` — không cần body.

| Mã | Mô tả | Chuẩn bị | Kết quả mong đợi |
|---|---|---|---|
| **AD-AP-01** | Duyệt bài PENDING không lịch | Bài AR-UP-01 | 200. `status`=`APPROVED`, `released`=**true**, `releaseAt` = giờ vừa duyệt (giờ Việt Nam, khớp đồng hồ của bạn) |
| **AD-AP-02** | Bài đã duyệt xuất hiện cho người nghe | Sau AD-AP-01, gọi `GET /api/songs` bằng `{{tokenUser}}` | Bài xuất hiện ở **đầu** danh sách |
| **AD-AP-03** | Duyệt bài có lịch tương lai | Bài AR-UP-10 | 200. `status`=`APPROVED`, `released`=**false**, `releaseAt` giữ nguyên ngày đã hẹn |
| **AD-AP-04** | Bài có lịch chưa hiện cho người nghe | Sau AD-AP-03, `GET /api/songs/{id}` bằng `{{tokenUser}}` | 400 `"Song does not exist"` |
| **AD-AP-05** | Duyệt lại bài đã APPROVED | Duyệt bài AD-AP-01 lần nữa | 400 `"Only pending songs can be approved"` |
| **AD-AP-06** | Duyệt bài REJECTED | Bài sau AD-RJ-01 | 400 `"Only pending songs can be approved"` |
| **AD-AP-07** | Duyệt bài HIDDEN | Bài sau AR-HD-01 | 400 `"Only pending songs can be approved"` |
| **AD-AP-08** | Duyệt bài đã xóa | Bài sau AR-DEL-01 | 400 `"Song does not exist"` |
| **AD-AP-09** | Duyệt id không tồn tại | `/999999/approve` | 400 `"Song does not exist"` |
| **AD-AP-10** | Id là chữ | `/abc/approve` | 400 `"Invalid parameter: songId"` |
| **AD-AP-11** | Duyệt bài của Artist bị khóa | Artist đăng bài → Admin khóa Artist đó → duyệt bài | 400 `"Artist account is not active"`. **Mở khóa lại sau test** |
| **AD-AP-12** | `rejectReason` bị xóa khi duyệt | Bài bị từ chối → Artist sửa (thành PENDING) → Admin duyệt | 200, `rejectReason`=**null** |
| **AD-AP-13** | Artist gọi API duyệt | `{{tokenArtist}}` | 403 |
| **AD-AP-14** | Dùng sai method | `POST` thay vì `PUT` | 405 |

### 7.3. Từ chối bài (UC35)

**API:** `PUT {{baseUrl}}/api/admin/songs/{songId}/reject`
**Body:** raw → JSON
```json
{ "reason": "Ảnh bìa không phù hợp" }
```

| Mã | Mô tả | Body | Kết quả mong đợi |
|---|---|---|---|
| **AD-RJ-01** | Từ chối bài PENDING | `{"reason": "Ảnh bìa không phù hợp"}` | 200. `status`=`REJECTED`, `rejectReason`=`"Ảnh bìa không phù hợp"`, `released`=false |
| **AD-RJ-02** | Lý do có dấu cách 2 đầu | `{"reason": "   Sai chính tả tên bài   "}` | 200, `rejectReason`=`"Sai chính tả tên bài"` |
| **AD-RJ-03** | Thiếu reason | `{}` | 400 `"Reason is required"` |
| **AD-RJ-04** | reason rỗng | `{"reason": ""}` | 400 `"Reason is required"` |
| **AD-RJ-05** | reason toàn dấu cách | `{"reason": "    "}` | 400 `"Reason is required"` |
| **AD-RJ-06** | reason = null | `{"reason": null}` | 400 `"Reason is required"` |
| **AD-RJ-07** | reason 256 ký tự | Chuỗi 256 ký tự | 400 `"Reason must not exceed 255 characters"` |
| **AD-RJ-08** | reason đúng 255 ký tự | Chuỗi 255 ký tự | 200 |
| **AD-RJ-09** | Không gửi body | Body = none | 400 `"Invalid request data"` |
| **AD-RJ-10** | JSON sai cú pháp | `{"reason": "abc"` (thiếu `}`) | 400 `"Invalid request data"` |
| **AD-RJ-11** | Từ chối bài đã APPROVED | Bài đã duyệt | 400 `"Only pending songs can be rejected"` (bài đã duyệt phải dùng **remove**) |
| **AD-RJ-12** | Từ chối bài đã REJECTED | Từ chối lần 2 | 400 `"Only pending songs can be rejected"` |
| **AD-RJ-13** | Từ chối bài đã xóa | | 400 `"Song does not exist"` |
| **AD-RJ-14** | Artist thấy lý do | Sau AD-RJ-01, `GET /api/artist/songs` bằng `{{tokenArtist}}` | Bài có `rejectReason` đúng nội dung |
| **AD-RJ-15** | Lý do tiếng Việt + ký tự đặc biệt | `{"reason": "Lời bài có từ ngữ \"nhạy cảm\" & không phù hợp"}` | 200, lưu đúng nguyên văn |

### 7.4. Gỡ bài đã duyệt (UC36)

**API:** `PUT {{baseUrl}}/api/admin/songs/{songId}/remove`
**Body:** giống từ chối `{ "reason": "..." }`

Gỡ bài = chuyển bài đang `APPROVED` hoặc `HIDDEN` sang **`REJECTED`** kèm lý do. Bài biến mất khỏi người nghe. Artist sửa lại thì bài tự quay về PENDING để duyệt lại.

> ⚠️ Gỡ bài **không** chuyển sang DELETED. Trạng thái sau khi gỡ là **REJECTED**.

| Mã | Mô tả | Chuẩn bị | Kết quả mong đợi |
|---|---|---|---|
| **AD-RM-01** | Gỡ bài APPROVED đã phát hành | Bài AD-AP-01, body `{"reason": "Vi phạm bản quyền"}` | 200. `status`=`REJECTED`, `rejectReason`=`"Vi phạm bản quyền"`, `released`=false |
| **AD-RM-02** | Bài bị gỡ biến mất khỏi người nghe | Sau AD-RM-01: `GET /api/songs` và `GET /api/songs/{id}` bằng `{{tokenUser}}` | Không có trong danh sách; chi tiết trả 400 `"Song does not exist"` |
| **AD-RM-03** | Gỡ bài HIDDEN | Bài sau AR-HD-01 | 200, `status`=`REJECTED` |
| **AD-RM-04** | Gỡ bài APPROVED chưa tới lịch | Bài AD-AP-03 | 200, `status`=`REJECTED` |
| **AD-RM-05** | Gỡ lại bài vừa gỡ | Gọi AD-RM-01 lần 2 | 400 `"Only approved songs can be removed"` |
| **AD-RM-06** | Gỡ bài PENDING | Bài chưa duyệt | 400 `"Only approved songs can be removed"` (bài chưa duyệt phải dùng **reject**) |
| **AD-RM-07** | Gỡ bài đã xóa | | 400 `"Song does not exist"` |
| **AD-RM-08** | Thiếu reason | `{}` | 400 `"Reason is required"` |
| **AD-RM-09** | reason 256 ký tự | | 400 `"Reason must not exceed 255 characters"` |
| **AD-RM-10** | Artist sửa bài bị gỡ → gửi duyệt lại | Sau AD-RM-01, Artist gọi AR-ED-01 (Phần 8) | Bài về `PENDING`, `rejectReason`=null |
| **AD-RM-11** | Admin duyệt lại bài bị gỡ | Sau AD-RM-10, Admin approve | 200, `APPROVED`, bài hiện lại cho người nghe |
| **AD-RM-12** | playCount giữ nguyên qua gỡ/duyệt lại | So sánh `playCount` trước AD-RM-01 và sau AD-RM-11 | Không bị reset về 0 |
| **AD-RM-13** | Artist gọi API gỡ | `{{tokenArtist}}` | 403 |

---

## PHẦN 8 — ARTIST: SỬA BÀI, ĐỔI ẢNH, ĐỔI LỊCH (UC29–UC31)

**Token:** `{{tokenArtist}}`

### 8.1. Sửa tên và danh sách feat (UC29)

**API:** `PUT {{baseUrl}}/api/artist/songs/{songId}`
**Body:** raw → JSON
```json
{
    "title": "Tên mới",
    "featuredArtistIds": [30, 31]
}
```

> ⚠️ `featuredArtistIds` là **danh sách mới thay thế hoàn toàn** danh sách cũ. Không gửi trường này (hoặc gửi `[]`) = **xóa hết feat**. Muốn giữ feat cũ thì phải gửi lại đủ id cũ.

| Mã | Mô tả | Body | Kết quả mong đợi |
|---|---|---|---|
| **AR-ED-01** | Đổi tên | `{"title": "TA Bài 01 (sửa)", "featuredArtistIds": []}` | 200, `title` mới, `updatedAt` đổi thành giờ hiện tại |
| **AR-ED-02** | Thêm feat | `{"title": "TA Bài 01", "featuredArtistIds": [{{artist2Id}}]}` | 200, `featuredArtists` có Artist 2 |
| **AR-ED-03** | Bỏ hết feat | Bài đang có feat, gửi `{"title": "TA Bài 01", "featuredArtistIds": []}` | 200, `featuredArtists`=[] |
| **AR-ED-04** | Không gửi featuredArtistIds | `{"title": "TA Bài 01"}` (bài đang có feat) | 200, `featuredArtists`=[] (**đúng thiết kế**, xem cảnh báo trên) |
| **AR-ED-05** | Sửa bài REJECTED → tự gửi duyệt lại | Bài sau AD-RJ-01 | 200, `status`=`PENDING`, `rejectReason`=null |
| **AR-ED-06** | Sửa bài PENDING | | 200, vẫn `PENDING` |
| **AR-ED-07** | Sửa bài APPROVED | Bài đã phát hành | 200, vẫn `APPROVED`, người nghe thấy tên mới ngay |
| **AR-ED-08** | Sửa bài HIDDEN | | 200, vẫn `HIDDEN` |
| **AR-ED-09** | Thiếu title | `{"featuredArtistIds": []}` | 400 `"Title is required"` |
| **AR-ED-10** | Title toàn dấu cách | `{"title": "   "}` | 400 `"Title is required"` |
| **AR-ED-11** | Title 151 ký tự | | 400 `"Title must not exceed 150 characters"` |
| **AR-ED-12** | Feat chính mình | `"featuredArtistIds": [{{artistId}}]` | 400 `"You cannot feature yourself"` |
| **AR-ED-13** | Feat id không tồn tại | `[999999]` | 400 `"Some featured artists do not exist"` |
| **AR-ED-14** | Feat USER | `[{{userId}}]` | 400 `"Featured artist is not available: <id>"` |
| **AR-ED-15** | Feat 6 người | 6 id | 400 `"Maximum 5 featured artists"` |
| **AR-ED-16** | Feat trùng | `[{{artist2Id}}, {{artist2Id}}]` | 200, chỉ 1 Artist 2 |
| **AR-ED-17** | featuredArtistIds có null | `[null, {{artist2Id}}]` | 200, chỉ có Artist 2 |
| **AR-ED-18** | Sửa bài của Artist khác | Dùng `{{tokenArtist2}}` sửa bài của Artist 1 | 400 `"Song does not exist"` (cố ý không nói "không có quyền" để không lộ bài tồn tại) |
| **AR-ED-19** | Sửa bài đã xóa | | 400 `"Song does not exist"` |
| **AR-ED-20** | Sửa id không tồn tại | `/api/artist/songs/999999` | 400 `"Song does not exist"` |
| **AR-ED-21** | JSON sai cú pháp | Thiếu dấu `}` | 400 `"Invalid request data"` |
| **AR-ED-22** | featuredArtistIds không phải mảng | `"featuredArtistIds": "abc"` | 400 `"Invalid request data"` |
| **AR-ED-23** | Sửa lỗi bị từ chối → không thay đổi lịch | Bài REJECTED có `releaseAt` → sửa tên | `releaseAt` giữ nguyên |

### 8.2. Đổi ảnh bìa (UC30)

**API:** `PUT {{baseUrl}}/api/artist/songs/{songId}/cover`
**Body:** form-data, key `cover` (File)

| Mã | Mô tả | Body | Kết quả mong đợi |
|---|---|---|---|
| **AR-CV-01** | Đổi ảnh bài chưa có ảnh | Bài AR-UP-01, `cover`=cover.jpg | 200, `coverUrl` có giá trị mới |
| **AR-CV-02** | Đổi ảnh bài đã có ảnh | Ghi lại `coverUrl` cũ → upload cover2.jpg | 200, `coverUrl` **khác** cũ |
| **AR-CV-03** | Ảnh cũ bị xóa khỏi server | Mở link ảnh cũ ở AR-CV-02 trên trình duyệt | **404** (không còn) |
| **AR-CV-04** | Ảnh mới mở được | Mở link `coverUrl` mới | Hiện ảnh mới |
| **AR-CV-05** | Đổi ảnh bài REJECTED → gửi duyệt lại | | 200, `status`=`PENDING`, `rejectReason`=null |
| **AR-CV-06** | Đổi ảnh bài APPROVED | | 200, vẫn `APPROVED` |
| **AR-CV-07** | PNG / WEBP | cover.png, cover.webp | 200 |
| **AR-CV-08** | Thiếu key cover | Body không có `cover` | 400 `"File is required"` |
| **AR-CV-09** | Key cover nhưng không chọn file | | 400 `"Image file is required"` hoặc `"File is required"` |
| **AR-CV-10** | GIF | anim.gif | 400 `"Only JPG, PNG or WEBP images are allowed"` |
| **AR-CV-11** | > 2MB | big.jpg | 400 `"Image must not exceed 2MB"` |
| **AR-CV-12** | Ảnh giả | fake.jpg | 400 |
| **AR-CV-13** | Ảnh lỗi → ảnh cũ còn nguyên | Sau AR-CV-10, mở lại `coverUrl` hiện tại | Ảnh cũ **vẫn mở được** |
| **AR-CV-14** | Bài của Artist khác | `{{tokenArtist2}}` | 400 `"Song does not exist"` |
| **AR-CV-15** | Bài đã xóa | | 400 `"Song does not exist"` |

### 8.3. Đổi lịch phát hành (UC31)

**API:** `PUT {{baseUrl}}/api/artist/songs/{songId}/release`
**Body:** raw → JSON
```json
{ "releaseAt": "2026-12-24T20:00:00" }
```
Gửi `{"releaseAt": null}` hoặc `{}` = **bỏ lịch**:
- Bài chưa duyệt → khi duyệt sẽ phát hành ngay.
- Bài đã duyệt nhưng chưa tới lịch → **phát hành ngay lập tức**.

| Mã | Mô tả | Chuẩn bị + Body | Kết quả mong đợi |
|---|---|---|---|
| **AR-RL-01** | Đặt lịch cho bài PENDING | `{"releaseAt": "2026-12-31T23:00:00"}` | 200, `releaseAt` đúng giá trị gửi |
| **AR-RL-02** | Bỏ lịch bài PENDING | `{"releaseAt": null}` | 200, `releaseAt`=null, vẫn `PENDING` |
| **AR-RL-03** | Đổi lịch bài APPROVED chưa tới giờ | Bài AD-AP-03, `{"releaseAt": "2027-01-01T08:00:00"}` | 200, `releaseAt` mới, `released`=false |
| **AR-RL-04** | Bỏ lịch bài APPROVED chưa tới giờ → phát hành ngay | Bài AD-AP-03, `{"releaseAt": null}` | 200, `releaseAt` = giờ hiện tại, `released`=**true**. Người nghe thấy bài ngay |
| **AR-RL-05** | Đổi lịch bài đã phát hành | Bài AD-AP-01 | 400 `"Cannot change the schedule of a released song"` |
| **AR-RL-06** | Giờ quá khứ | `{"releaseAt": "2020-01-01T00:00:00"}` | 400 `"Release time must be in the future"` |
| **AR-RL-07** | Sai định dạng | `{"releaseAt": "24/12/2026"}` | 400 `"Invalid request data"` |
| **AR-RL-08** | Body rỗng | `{}` | 200 — tương đương bỏ lịch (giống AR-RL-02 / AR-RL-04) |
| **AR-RL-09** | Bài của Artist khác | `{{tokenArtist2}}` | 400 `"Song does not exist"` |
| **AR-RL-10** | Bài HIDDEN đã từng phát hành | Bài đã phát hành rồi bị ẩn | 400 `"Cannot change the schedule of a released song"` |
| **AR-RL-11** | Bài REJECTED | `{"releaseAt": "<tương lai>"}` | 200, đổi được lịch, **vẫn** `REJECTED` (đổi lịch không tự gửi duyệt lại) |

### 8.4. Test lịch phát hành thật (chờ đồng hồ)

Đây là test quan trọng để chắc chắn server chạy đúng giờ Việt Nam.

| Bước | Hành động | Kết quả mong đợi |
|---|---|---|
| **AR-SCH-01** | Artist upload bài với `releaseAt` = **giờ hiện tại + 3 phút** (VD bây giờ 17:30 → `2026-10-15T17:33:00`) | 200, PENDING |
| **AR-SCH-02** | Admin duyệt ngay | `APPROVED`, `released`=false |
| **AR-SCH-03** | USER gọi `GET /api/songs/{id}` ngay | 400 `"Song does not exist"` |
| **AR-SCH-04** | Đợi qua mốc giờ đã hẹn, USER gọi lại | 200, thấy bài. Gọi `GET /api/artist/songs` bằng Artist → `released`=true |

Nếu ở bước 4 phải đợi thêm **đúng 7 tiếng** mới thấy bài → server sai múi giờ → **báo bug nghiêm trọng**.

Biến thể: **AR-SCH-05** — upload với lịch +2 phút, **không duyệt**, đợi qua giờ hẹn rồi mới duyệt → bài phát hành **ngay khi duyệt** (`released`=true).

---

## PHẦN 9 — ARTIST: ẨN, HIỆN, XÓA BÀI (UC32–UC33)

**Token:** `{{tokenArtist}}`

### 9.1. Ẩn bài

**API:** `PUT {{baseUrl}}/api/artist/songs/{songId}/hide` — không cần body.

| Mã | Mô tả | Chuẩn bị | Kết quả mong đợi |
|---|---|---|---|
| **AR-HD-01** | Ẩn bài APPROVED | Bài đã phát hành | 200, `status`=`HIDDEN`, `released`=false |
| **AR-HD-02** | Bài ẩn biến mất với người nghe | USER: `GET /api/songs`, `GET /api/songs/{id}`, tìm kiếm theo tên | Không thấy; chi tiết trả 400 `"Song does not exist"` |
| **AR-HD-03** | Bài ẩn không nghe được | USER: `POST /api/songs/{id}/play` | 400 `"Song does not exist"` |
| **AR-HD-04** | Bài ẩn vẫn thấy trong "Bài hát của tôi" | Artist: `GET /api/artist/songs` | Có, `status`=`HIDDEN` |
| **AR-HD-05** | Ẩn bài PENDING | | 400 `"Only approved songs can be hidden"` |
| **AR-HD-06** | Ẩn bài REJECTED | | 400 `"Only approved songs can be hidden"` |
| **AR-HD-07** | Ẩn bài đã HIDDEN | Ẩn 2 lần | 400 `"Only approved songs can be hidden"` |
| **AR-HD-08** | Ẩn bài APPROVED chưa tới lịch | | 200, `HIDDEN` |
| **AR-HD-09** | Ẩn bài Artist khác | `{{tokenArtist2}}` | 400 `"Song does not exist"` |
| **AR-HD-10** | USER gọi | `{{tokenUser}}` | 403 |

### 9.2. Hiện lại bài

**API:** `PUT {{baseUrl}}/api/artist/songs/{songId}/show`

| Mã | Mô tả | Chuẩn bị | Kết quả mong đợi |
|---|---|---|---|
| **AR-SH-01** | Hiện bài HIDDEN | Bài AR-HD-01 | 200, `status`=`APPROVED`, `released`=true |
| **AR-SH-02** | Người nghe thấy lại | USER: `GET /api/songs/{id}` | 200 |
| **AR-SH-03** | playCount không mất | So sánh `playCount` trước khi ẩn và sau khi hiện | Giữ nguyên |
| **AR-SH-04** | Hiện bài APPROVED | | 400 `"Only hidden songs can be shown again"` |
| **AR-SH-05** | Hiện bài PENDING | | 400 `"Only hidden songs can be shown again"` |
| **AR-SH-06** | Hiện bài bị Admin gỡ | Bài sau AD-RM-03 (REJECTED) | 400 `"Only hidden songs can be shown again"` (Artist không tự "hiện" được bài Admin đã gỡ) |
| **AR-SH-07** | Bài Artist khác | `{{tokenArtist2}}` | 400 `"Song does not exist"` |

### 9.3. Xóa bài

**API:** `DELETE {{baseUrl}}/api/artist/songs/{songId}`

> Xóa là **vĩnh viễn**: file nhạc và ảnh bị xóa khỏi ổ đĩa server. Tạo bài riêng để xóa, đừng xóa bài đang dùng cho test khác.

| Mã | Mô tả | Chuẩn bị | Kết quả mong đợi |
|---|---|---|---|
| **AR-DEL-01** | Xóa bài PENDING | Upload bài mới có ảnh bìa. **Ghi lại** `audioUrl`, `coverUrl` | 200 `{"message": "Song has been deleted"}` |
| **AR-DEL-02** | File nhạc bị xóa | Mở `audioUrl` cũ trên trình duyệt | 404 |
| **AR-DEL-03** | Ảnh bị xóa | Mở `coverUrl` cũ | 404 |
| **AR-DEL-04** | Biến mất khỏi "Bài hát của tôi" | `GET /api/artist/songs` | Không còn bài đó |
| **AR-DEL-05** | Admin vẫn thấy trong mục DELETED | Admin: `GET /api/admin/songs?status=DELETED` | Có bài đó |
| **AR-DEL-06** | Admin không thấy khi không lọc | Admin: `GET /api/admin/songs` | Không có bài đó |
| **AR-DEL-07** | Xóa lần 2 | Xóa lại cùng id | 400 `"Song does not exist"` |
| **AR-DEL-08** | Xóa bài APPROVED đã phát hành | | 200; người nghe không thấy nữa |
| **AR-DEL-09** | Xóa bài HIDDEN | | 200 |
| **AR-DEL-10** | Xóa bài REJECTED | | 200 |
| **AR-DEL-11** | Xóa bài Artist khác | `{{tokenArtist2}}` | 400 `"Song does not exist"`. Bài của Artist 1 **còn nguyên** |
| **AR-DEL-12** | Xóa id không tồn tại | `/999999` | 400 `"Song does not exist"` |
| **AR-DEL-13** | Sửa / ẩn / đổi ảnh bài đã xóa | Gọi lần lượt AR-ED, AR-HD, AR-CV trên bài đã xóa | Đều 400 `"Song does not exist"` |
| **AR-DEL-14** | USER gọi | `{{tokenUser}}` | 403 |

---

## PHẦN 10 — NGƯỜI NGHE: DANH SÁCH, TÌM KIẾM, CHI TIẾT, NGHE NHẠC (UC20–UC25)

**Token:** `{{tokenUser}}` (USER, ARTIST, ADMIN đều gọi được — nên thử cả 3, kết quả phải giống nhau).

**Cấu trúc 1 bài hát (SongResponse) — bản rút gọn cho người nghe:**
```json
{
    "id": 5,
    "title": "Mind game",
    "audioUrl": "/uploads/songs/audio/b3106cb4-....mp3",
    "coverUrl": "/uploads/songs/covers/4552685a-....jpg",
    "durationSeconds": 212,
    "playCount": 0,
    "releaseAt": "2026-10-12T00:26:13.14861",
    "artistId": 23,
    "artistName": "AMIEU",
    "featuredArtists": []
}
```
**Không được** có `status`, `rejectReason`, `version`, `createdAt`, `updatedAt` — nếu thấy là **bug lộ dữ liệu**.

### 10.1. Danh sách bài đã phát hành (UC20)

**API:** `GET {{baseUrl}}/api/songs?page=0&size=20`

| Mã | Mô tả | Request | Kết quả mong đợi |
|---|---|---|---|
| **LS-01** | Lấy danh sách | `GET /api/songs` | 200, dạng phân trang, `page`=0, `size`=20 |
| **LS-02** | Sắp xếp mới phát hành trước | | `releaseAt` **giảm dần** từ trên xuống |
| **LS-03** | Không lộ trường quản lý | | Không có `status`, `rejectReason`, `createdAt`, `updatedAt` |
| **LS-04** | Không có bài PENDING | So với Admin `?status=PENDING` | Không bài PENDING nào xuất hiện |
| **LS-05** | Không có bài REJECTED / HIDDEN / DELETED | | Không xuất hiện |
| **LS-06** | Không có bài APPROVED chưa tới lịch | Bài AD-AP-03 | Không xuất hiện |
| **LS-07** | size > 50 | `?size=500` | `size` = 50 |
| **LS-08** | size = 0 | `?size=0` | `size` = 1 |
| **LS-09** | page âm | `?page=-3` | `page` = 0 |
| **LS-10** | page vượt | `?page=9999` | `content`=[] |
| **LS-11** | page là chữ | `?page=abc` | 400 `"Invalid parameter: page"` |
| **LS-12** | size là chữ | `?size=abc` | 400 `"Invalid parameter: size"` |
| **LS-13** | Phân trang không trùng | `?size=3&page=0`, `?size=3&page=1` | 2 trang không có bài trùng nhau |
| **LS-14** | totalPages đúng | | `totalPages` = làm tròn lên (`totalElements` / `size`) |
| **LS-15** | Không token | No Auth | 401 |

### 10.2. Tìm kiếm (UC21)

**API:** `GET {{baseUrl}}/api/songs/search?keyword=...&page=0&size=20`

Tìm trong 3 chỗ (không phân biệt hoa thường, khớp 1 phần):
1. Tên bài
2. Nghệ danh Artist chính
3. Nghệ danh Artist feat (chỉ feat đang hoạt động)

| Mã | Mô tả | Request | Kết quả mong đợi |
|---|---|---|---|
| **SR-01** | Tìm theo tên bài | `?keyword=TA Bài 01` | Có bài đó |
| **SR-02** | Tìm 1 phần tên | `?keyword=Bài 0` | Có các bài chứa "Bài 0" |
| **SR-03** | Không phân biệt hoa thường | `?keyword=ta bài 01` / `?keyword=TA BÀI 01` | Đều tìm thấy |
| **SR-04** | Tìm theo Artist chính | `?keyword=TA Artist` | Có các bài của Artist 1 |
| **SR-05** | Tìm theo Artist feat | `?keyword=TA Artist Two` | Có bài Artist 1 **feat** Artist 2 (dù Artist 2 không phải Artist chính) |
| **SR-06** | 1 bài khớp nhiều điều kiện | Bài có tên chứa "Two" và feat "TA Artist Two", tìm `?keyword=Two` | Bài đó chỉ xuất hiện **1 lần** (không bị lặp) |
| **SR-07** | Keyword rỗng | `?keyword=` | Kết quả **giống hệt** `GET /api/songs` |
| **SR-08** | Không gửi keyword | `/api/songs/search` | Giống `GET /api/songs` |
| **SR-09** | Không có kết quả | `?keyword=zzzzzz` | 200, `content`=[], `totalElements`=0 |
| **SR-10** | Không tìm ra bài chưa phát hành | Tìm tên bài PENDING / HIDDEN / chưa tới lịch | Không có |
| **SR-11** | Tiếng Việt có dấu | `?keyword=Chiều Mưa` | Có bài AR-UP-12 |
| **SR-12** | Dấu cách 2 đầu | `?keyword=%20%20TA Bài 01%20%20` | Vẫn tìm thấy |
| **SR-13** | Feat bị khóa không còn được tìm theo feat | Admin khóa Artist 2 → tìm `?keyword=TA Artist Two` | Bài Artist 1 feat Artist 2 **không** còn khớp theo tên feat (vẫn hiện nếu tìm theo tên bài). Mở khóa sau test |
| **SR-14** | Phân trang | `?keyword=TA&size=2&page=1` | Hoạt động như LS-13 |
| **SR-15** | `totalElements` đúng | Đếm tay số bài khớp | Khớp `totalElements` |

### 10.3. Chi tiết bài (UC22)

**API:** `GET {{baseUrl}}/api/songs/{songId}`

| Mã | Mô tả | Request | Kết quả mong đợi |
|---|---|---|---|
| **DT-01** | Bài đã phát hành | id bài APPROVED đã tới giờ | 200, SongResponse đầy đủ |
| **DT-02** | Bài PENDING | | 400 `"Song does not exist"` |
| **DT-03** | Bài REJECTED | | 400 `"Song does not exist"` |
| **DT-04** | Bài HIDDEN | | 400 `"Song does not exist"` |
| **DT-05** | Bài DELETED | | 400 `"Song does not exist"` |
| **DT-06** | Bài APPROVED chưa tới lịch | | 400 `"Song does not exist"` |
| **DT-07** | Id không tồn tại | `/999999` | 400 `"Song does not exist"` |
| **DT-08** | Id là chữ | `/abc` | 400 `"Invalid parameter: songId"` |
| **DT-09** | Id âm | `/-1` | 400 `"Song does not exist"` |
| **DT-10** | Bài của Artist bị khóa | Admin khóa Artist 1 → xem bài của Artist 1 | 400 `"Song does not exist"`. **Mở khóa lại** → bài xem được lại |
| **DT-11** | Có featuredArtists đúng | Bài có feat | `featuredArtists` liệt kê đúng `id` + `artistName` |

> DT-02 đến DT-07 cố ý trả **cùng 1 message**. Đây là thiết kế bảo mật, không phải bug.

### 10.4. Nghe nhạc (UC23)

Không có API riêng. Frontend đặt `audioUrl` vào thẻ `<audio>`.

| Mã | Mô tả | Cách test | Kết quả mong đợi |
|---|---|---|---|
| **PL-AU-01** | Phát trên trình duyệt | Mở `http://157.66.100.87:8080` + `audioUrl` | Phát được nhạc |
| **PL-AU-02** | Tua nhạc | Kéo thanh tua tới giữa bài | Nhảy tới đúng chỗ, không phát lại từ đầu |
| **PL-AU-03** | Không cần token | Mở trên tab ẩn danh | Vẫn phát được |
| **PL-AU-04** | Range request (Postman) | `GET` link audio, tab Headers thêm `Range: bytes=0-1023` | **206 Partial Content**, header `Content-Range` có mặt |
| **PL-AU-05** | File không tồn tại | `/uploads/songs/audio/khongco.mp3` | 404 |
| **PL-AU-06** | Path traversal | `/uploads/../../etc/passwd` | **Không** được trả nội dung file hệ thống (400 / 404 đều được). Nếu thấy nội dung lạ → **báo bug bảo mật ngay** |

### 10.5. Tăng lượt nghe (UC24)

**API:** `POST {{baseUrl}}/api/songs/{songId}/play` — không cần body.
Luôn trả `200 {"message": "Play recorded"}` nếu bài đã phát hành.

Quy tắc: **1 tài khoản + 1 bài** chỉ được tính **1 lượt trong 30 giây**. Lượt bị bỏ qua vẫn trả 200 (cố ý, để kẻ gian không dò được quy luật).

| Mã | Mô tả | Cách test | Kết quả mong đợi |
|---|---|---|---|
| **PC-01** | Nghe lần đầu | Ghi `playCount` hiện tại (DT-01) → play → xem lại | 200 `"Play recorded"`, `playCount` **+1** |
| **PC-02** | Play liên tục trong 30 giây | Bấm Send 5 lần liên tiếp | Cả 5 lần 200, nhưng `playCount` chỉ **+1** |
| **PC-03** | Sau 30 giây | Đợi > 30 giây → play lại | `playCount` **+1** nữa |
| **PC-04** | 2 tài khoản khác nhau | USER play, rồi ngay sau đó Artist play cùng bài | `playCount` **+2** (mỗi tài khoản tính riêng) |
| **PC-05** | Cùng tài khoản, 2 bài khác nhau | Play bài A rồi ngay bài B | Cả 2 bài đều **+1** |
| **PC-06** | Bài chưa phát hành | Play bài PENDING / HIDDEN / chưa tới lịch | 400 `"Song does not exist"`, `playCount` không đổi |
| **PC-07** | Id không tồn tại | `/999999/play` | 400 `"Song does not exist"` |
| **PC-08** | Không token | | 401 |
| **PC-09** | Dùng GET thay POST | `GET /api/songs/{id}/play` | 405 hoặc 400 (không được tăng lượt nghe) |
| **PC-10** | Artist tự nghe bài mình | `{{tokenArtist}}` play bài của mình | 200, `playCount` +1 (được phép) |
| **PC-11** | 2 tester play cùng lúc | 2 bạn đếm 3-2-1 rồi cùng bấm Send (mỗi người tài khoản riêng) | `playCount` **+2**, không bị mất lượt |

### 10.6. Trang Artist (UC25)

**API:** `GET {{baseUrl}}/api/songs/artists/{artistId}?page=0&size=20`

Trả về:
```json
{
    "artistId": 23,
    "artistName": "TA Artist",
    "bio": "Tài khoản test của Tester A",
    "avatarUrl": null,
    "songs": { "content": [...], "page": 0, "size": 20, "totalElements": 4, "totalPages": 1 }
}
```

| Mã | Mô tả | Request | Kết quả mong đợi |
|---|---|---|---|
| **AP-01** | Trang Artist hợp lệ | `/api/songs/artists/{{artistId}}` | 200, đúng `artistName`, `bio` |
| **AP-02** | Chỉ bài đã phát hành | | Không có bài PENDING / HIDDEN / chưa tới lịch của Artist này |
| **AP-03** | Có bài được feat | `/api/songs/artists/{{artist2Id}}` | Có cả bài Artist 1 **feat** Artist 2 |
| **AP-04** | Không lặp bài | | Mỗi bài xuất hiện 1 lần |
| **AP-05** | Id là USER | `/api/songs/artists/{{userId}}` | 400 `"Artist does not exist"` |
| **AP-06** | Id không tồn tại | `/999999` | 400 `"Artist does not exist"` |
| **AP-07** | Artist bị khóa | Admin khóa Artist 1 | 400 `"Artist does not exist"`. Mở khóa lại |
| **AP-08** | Phân trang | `?size=1&page=1` | Trang thứ 2, 1 bài |
| **AP-09** | Artist chưa có bài | Artist mới | 200, `songs.content`=[] |
| **AP-10** | Id là chữ | `/abc` | 400 `"Invalid parameter: artistId"` |

---

## PHẦN 11 — LỊCH SỬ NGHE GẦN ĐÂY (MONGODB)

**API:** `GET {{baseUrl}}/api/users/me/history` — không có tham số.
**Token:** bất kỳ role nào.

Trả về **mảng** tối đa **10 bài khác nhau** nghe gần nhất, mới nhất ở đầu:
```json
[
    {
        "song": { ...SongResponse giống Phần 10... },
        "playedAt": "2026-10-15T17:40:12"
    }
]
```

Quy tắc:
1. Lịch sử được ghi **tự động** khi gọi `POST /api/songs/{id}/play` và lượt đó **được tính** (không bị chặn 30 giây).
2. Mỗi bài chỉ xuất hiện **1 lần**, `playedAt` = lần nghe gần nhất.
3. Bài không còn phát hành (ẩn, gỡ, xóa, Artist bị khóa) **tự biến mất** khỏi lịch sử; hiện lại thì xuất hiện lại.
4. Lịch sử của ai người nấy xem, không xem được của người khác.

> Dùng tài khoản **chưa nghe bài nào** cho phần này để dễ kiểm tra (VD tạo thêm tài khoản `testerB_history`).

| Mã | Mô tả | Cách test | Kết quả mong đợi |
|---|---|---|---|
| **HS-01** | Chưa nghe bài nào | Tài khoản mới, gọi history | 200, `[]` |
| **HS-02** | Nghe 1 bài | Play bài A → history | 1 phần tử, `song.id` = A |
| **HS-03** | playedAt đúng giờ VN | So `playedAt` với đồng hồ máy | Lệch không quá vài giây. Lệch **7 tiếng** → bug múi giờ |
| **HS-04** | Thứ tự mới nhất trước | Play A → B → C (cách nhau vài giây) | Thứ tự: **C, B, A** |
| **HS-05** | Nghe lại bài cũ → lên đầu | Đợi > 30 giây, play A lại | Thứ tự: **A, C, B**. Vẫn đúng **3** phần tử |
| **HS-06** | Không trùng lặp | Play A thêm vài lần (mỗi lần cách > 30 giây) | A chỉ xuất hiện **1 lần** |
| **HS-07** | Play trong 30 giây không cập nhật | Play B, rồi play B lại ngay (< 30 giây) | `playedAt` của B là lần **đầu**, không phải lần thứ 2 |
| **HS-08** | Tối đa 10 bài | Play 11–12 bài **khác nhau** | Đúng **10** phần tử, bài nghe sớm nhất bị đẩy ra |
| **HS-09** | Bài bị ẩn biến mất | Artist ẩn 1 bài đang có trong lịch sử | Bài đó **không** còn trong history |
| **HS-10** | Hiện lại → xuất hiện lại | Artist hiện lại bài ở HS-09 | Bài xuất hiện lại **đúng vị trí cũ** (theo `playedAt` cũ) |
| **HS-11** | Bài bị Admin gỡ biến mất | Admin remove 1 bài trong lịch sử | Biến mất |
| **HS-12** | Bài bị xóa biến mất | Artist xóa 1 bài trong lịch sử | Biến mất vĩnh viễn |
| **HS-13** | Artist bị khóa | Admin khóa Artist có bài trong lịch sử | Các bài của Artist đó biến mất. Mở khóa → xuất hiện lại |
| **HS-14** | Tách biệt người dùng | Tester A và Tester B mỗi người nghe bài khác nhau, rồi mỗi người gọi history | Mỗi người chỉ thấy bài **mình** nghe |
| **HS-15** | Không xem được của người khác | Thử `/api/users/{{artistId}}/history` | 400 / 404 / 401 / 403 — **không** được trả lịch sử của người khác |
| **HS-16** | Không token | No Auth | 401 |
| **HS-17** | `song` có đúng cấu trúc | | Giống hệt 1 phần tử của `GET /api/songs`, không có `status`, `rejectReason` |
| **HS-18** | playCount trong history cập nhật | Bài vừa được play | `song.playCount` là con số hiện tại |
| **HS-19** | Play bài chưa phát hành không vào lịch sử | Play bài PENDING (400) → history | Không có bài đó |
| **HS-20** | Artist / Admin cũng có lịch sử | Play bằng `{{tokenArtist}}`, gọi history bằng `{{tokenArtist}}` | Có lịch sử riêng của Artist |

### Mẹo tiết kiệm thời gian cho HS-08

Tạo 1 request play với URL `{{baseUrl}}/api/songs/{{playId}}/play`, rồi dùng **Collection Runner** + file CSV:
```csv
playId
5
6
7
8
9
10
11
12
13
14
15
```
Runner → chọn request → Data = file CSV → Delay = 1000ms → Run. Sau đó gọi history.

---

## PHẦN 12 — PHÂN QUYỀN VÀ BẢO MẬT

### 12.1. Ma trận phân quyền

Gọi **từng API** với **từng loại token**. Điền kết quả, đối chiếu với cột mong đợi.

| API | Không token | USER | ARTIST | ADMIN |
|---|---|---|---|---|
| `GET /api/songs` | 401 | 200 | 200 | 200 |
| `GET /api/songs/search` | 401 | 200 | 200 | 200 |
| `GET /api/songs/{id}` | 401 | 200 | 200 | 200 |
| `POST /api/songs/{id}/play` | 401 | 200 | 200 | 200 |
| `GET /api/songs/artists/{id}` | 401 | 200 | 200 | 200 |
| `GET /api/users/me/history` | 401 | 200 | 200 | 200 |
| `POST /api/artist/songs` | 401 | **403** | 200 | **403** |
| `GET /api/artist/songs` | 401 | **403** | 200 | **403** |
| `GET /api/artist/search-artists` | 401 | **403** | 200 | **403** |
| `PUT /api/artist/songs/{id}` | 401 | **403** | 200 | **403** |
| `PUT /api/artist/songs/{id}/cover` | 401 | **403** | 200 | **403** |
| `PUT /api/artist/songs/{id}/release` | 401 | **403** | 200 | **403** |
| `PUT /api/artist/songs/{id}/hide` | 401 | **403** | 200 | **403** |
| `PUT /api/artist/songs/{id}/show` | 401 | **403** | 200 | **403** |
| `DELETE /api/artist/songs/{id}` | 401 | **403** | 200 | **403** |
| `GET /api/admin/songs` | 401 | **403** | **403** | 200 |
| `PUT /api/admin/songs/{id}/approve` | 401 | **403** | **403** | 200 |
| `PUT /api/admin/songs/{id}/reject` | 401 | **403** | **403** | 200 |
| `PUT /api/admin/songs/{id}/remove` | 401 | **403** | **403** | 200 |
| `GET /uploads/...` (file) | 200 | 200 | 200 | 200 |

> "200" trong bảng nghĩa là **qua được lớp phân quyền**. Có thể ra 400 vì dữ liệu (VD id không tồn tại) — vẫn tính là đúng, miễn **không phải 401/403**.

Mã test: **SEC-MX-01** đến **SEC-MX-20** theo thứ tự dòng.

### 12.2. Token

| Mã | Mô tả | Cách test | Kết quả mong đợi |
|---|---|---|---|
| **SEC-TK-01** | Token sai chữ ký | Sửa 1 ký tự cuối của token | 401 |
| **SEC-TK-02** | Token rác | `Bearer abc` | 401 |
| **SEC-TK-03** | Thiếu chữ "Bearer " | Header `Authorization: <token>` (không có Bearer) | 401 |
| **SEC-TK-04** | Đăng nhập lại → token cũ chết | Login lấy token 1 → login lại lấy token 2 → gọi API bằng token 1 | 401 `"Your session has ended. Your account may have been logged in on another device"` |
| **SEC-TK-05** | Token 2 vẫn dùng được | Gọi bằng token 2 | 200 |
| **SEC-TK-06** | Đăng xuất | `POST /api/users/logout` → gọi API bằng token đó | 401 |
| **SEC-TK-07** | Sửa role trong token | Dán token vào jwt.io, đổi phần payload → dùng token đã sửa | 401 (chữ ký không khớp) |

### 12.3. Khóa tài khoản

**API Admin:** `PUT /api/admin/users/{userId}/lock` và `PUT /api/admin/users/{userId}/unlock`

> ⚠️ **Luôn mở khóa lại** sau mỗi test. Khóa nhầm Artist của tester kia → báo cho bạn ấy biết.

| Mã | Mô tả | Kết quả mong đợi |
|---|---|---|
| **SEC-LK-01** | Khóa Artist 1 → Artist 1 gọi `GET /api/artist/songs` | 401 `"Your session has ended. Your account may have been logged in on another device"` (khóa tài khoản xóa luôn phiên đăng nhập) |
| **SEC-LK-02** | Bài của Artist 1 với người nghe | Biến mất khỏi `/api/songs`, tìm kiếm, chi tiết, trang Artist, lịch sử |
| **SEC-LK-03** | Mở khóa Artist 1 | Bài xuất hiện lại đầy đủ, `playCount` không đổi |
| **SEC-LK-04** | Bài **feat** Artist bị khóa | Khóa Artist 2 → bài của Artist 1 feat Artist 2 **vẫn hiện** (Artist chính vẫn hoạt động) |

### 12.4. Giới hạn tần suất (rate limit)

| Mã | Mô tả | Cách test | Kết quả mong đợi |
|---|---|---|---|
| **SEC-RL-01** | Gửi dồn dập | Collection Runner, 1 request `GET /api/songs`, Iterations = 200, Delay = 0 | Tới 1 lúc trả **429** `"Too many requests. Please try again in X seconds"`, header `Retry-After` có giá trị |
| **SEC-RL-02** | Hết thời gian chờ | Đợi đúng số giây trong `Retry-After` → gọi lại | 200 |
| **SEC-RL-03** | Giới hạn tính theo tài khoản | Tester A bị 429 → Tester B (tài khoản khác) gọi ngay | Tester B vẫn 200 |
| **SEC-RL-04** | File nhạc không bị giới hạn | Mở file audio liên tục | Không bao giờ 429 |

> Làm SEC-RL **cuối cùng**, sau khi test xong các phần khác, để khỏi bị chặn giữa chừng.

### 12.5. Dữ liệu độc hại

| Mã | Mô tả | Cách test | Kết quả mong đợi |
|---|---|---|---|
| **SEC-IN-01** | SQL injection ở tìm kiếm | `?keyword=' OR 1=1 --` | 200, `content`=[] hoặc chỉ bài có chứa đúng chuỗi đó. **Không** lỗi 500, **không** trả bài chưa phát hành |
| **SEC-IN-02** | SQL injection ở title | Upload `title`=`'); DROP TABLE songs; --` | 200, title lưu **nguyên văn**, bảng không mất |
| **SEC-IN-03** | Script trong title | `title`=`<script>alert(1)</script>` | 200, lưu nguyên văn dạng chữ (frontend tự hiển thị an toàn) |
| **SEC-IN-04** | Script trong lý do từ chối | `reason`=`<img src=x onerror=alert(1)>` | 200, lưu nguyên văn |
| **SEC-IN-05** | Upload file .html giả mp3 | Đổi `x.html` thành `x.mp3` | 400 `"Invalid or corrupted audio file"` |
| **SEC-IN-06** | Upload file .jsp / .exe | | 400 `"Only MP3 or WAV files are allowed"` |
| **SEC-IN-07** | Id số cực lớn | `/api/songs/99999999999999999999` | 400 `"Invalid parameter: songId"` (vượt kiểu Long), **không** 500 |

---

## PHẦN 13 — KỊCH BẢN TỔNG HỢP ĐẦU-CUỐI

2 bạn làm cùng nhau, mỗi bước ghi lại kết quả. Đây là kịch bản giống thật nhất.

### Kịch bản E2E-01: Vòng đời đầy đủ của 1 bài hát

| Bước | Ai | Hành động | Kiểm tra |
|---|---|---|---|
| 1 | Artist (A) | Upload `E2E Bài 1` có ảnh, feat Artist 2 | 200, PENDING |
| 2 | User (B) | Tìm `E2E Bài 1` | Không thấy |
| 3 | Admin (A) | `GET /api/admin/songs?status=PENDING` | Thấy bài |
| 4 | Admin | Reject: `"Tên bài chưa đúng"` | REJECTED |
| 5 | Artist | `GET /api/artist/songs` | Thấy lý do |
| 6 | Artist | Sửa tên thành `E2E Bài 1 (bản sửa)`, giữ feat Artist 2 | PENDING, lý do = null |
| 7 | Admin | Approve | APPROVED, released = true |
| 8 | User | `GET /api/songs` | Bài ở đầu danh sách, có feat Artist 2 |
| 9 | User | Play | `playCount` = 1 |
| 10 | User | `GET /api/users/me/history` | Bài ở đầu |
| 11 | User | `GET /api/songs/artists/{{artist2Id}}` | Có bài (vì được feat) |
| 12 | Artist | Hide | HIDDEN |
| 13 | User | Chi tiết, tìm kiếm, history | Không thấy ở đâu cả |
| 14 | Artist | Show | APPROVED |
| 15 | User | history | Bài xuất hiện lại, `playCount` vẫn = 1 |
| 16 | Admin | Remove: `"Khiếu nại bản quyền"` | REJECTED |
| 17 | User | Chi tiết | 400 |
| 18 | Artist | Đổi ảnh bìa | PENDING (tự gửi duyệt lại) |
| 19 | Admin | Approve | APPROVED, `playCount` vẫn = 1 |
| 20 | Artist | Delete | `"Song has been deleted"` |
| 21 | User | Chi tiết, history, mở audioUrl | 400 / không có / 404 |
| 22 | Admin | `?status=DELETED` | Thấy bài |

### Kịch bản E2E-02: Phát hành theo lịch

| Bước | Ai | Hành động | Kiểm tra |
|---|---|---|---|
| 1 | Artist | Upload `E2E Hẹn Giờ`, `releaseAt` = bây giờ + 5 phút | PENDING |
| 2 | Admin | Approve | APPROVED, released = false |
| 3 | User | `GET /api/songs` | Không thấy |
| 4 | Artist | Đổi lịch thành bây giờ + 10 phút | released = false |
| 5 | Artist | Đổi lịch = null | released = **true** ngay |
| 6 | User | `GET /api/songs` | Thấy bài ở đầu |
| 7 | Artist | Đổi lịch lần nữa | 400 `"Cannot change the schedule of a released song"` |

### Kịch bản E2E-03: Artist bị khóa giữa chừng

| Bước | Ai | Hành động | Kiểm tra |
|---|---|---|---|
| 1 | Artist 2 | Upload `E2E Khóa`, Admin approve | Người nghe thấy |
| 2 | User | Play `E2E Khóa` | Có trong history |
| 3 | Artist 2 | Upload thêm `E2E Khóa 2` (PENDING) | |
| 4 | Admin | Khóa Artist 2 | |
| 5 | User | `/api/songs`, tìm, chi tiết, trang Artist 2, history | `E2E Khóa` biến mất khắp nơi |
| 6 | Admin | Approve `E2E Khóa 2` | 400 `"Artist account is not active"` |
| 7 | Artist 1 | Upload feat Artist 2 | 400 `"Featured artist is not available: <id>"` |
| 8 | Artist 1 | Tìm feat Artist 2 | Không thấy |
| 9 | Admin | Mở khóa Artist 2 | |
| 10 | User | Lặp lại bước 5 | Mọi thứ trở lại bình thường |

### Kịch bản E2E-04: 2 người nghe cùng lúc

| Bước | Ai | Hành động | Kiểm tra |
|---|---|---|---|
| 1 | A + B | Ghi `playCount` của 1 bài | Cùng số N |
| 2 | A + B | Đếm 3-2-1, cùng bấm play (mỗi người tài khoản riêng) | Cả 2 đều 200 |
| 3 | A | Xem chi tiết | `playCount` = N + 2 |
| 4 | A + B | Mỗi người xem history | Mỗi người có bài đó ở đầu, playedAt khác nhau vài mili giây |

---

## PHẦN 14 — GIỚI HẠN ĐÃ BIẾT (KHÔNG PHẢI BUG)

Các hành vi dưới đây là **đã biết và chấp nhận** trong phiên bản hiện tại. Gặp thì **không cần báo bug**, nhưng có thể ghi chú.

| # | Hành vi | Giải thích |
|---|---|---|
| 1 | Tìm kiếm `?keyword=%25` (ký tự `%`) hoặc `_` trả về **nhiều bài** | 2 ký tự này là ký tự đặc biệt của câu lệnh LIKE trong SQL, chưa được xử lý riêng |
| 2 | Sửa tên / đổi ảnh bài **đã APPROVED** không cần duyệt lại | Thiết kế hiện tại: chỉ bài REJECTED mới tự quay về PENDING khi sửa |
| 3 | Đổi lịch bài REJECTED không tự chuyển về PENDING | Chỉ sửa tên/feat hoặc đổi ảnh mới gửi duyệt lại |
| 4 | Không gửi `featuredArtistIds` khi sửa bài → mất hết feat | Danh sách gửi lên thay thế hoàn toàn danh sách cũ |
| 5 | Play trong 30 giây vẫn trả 200 | Cố ý, để không lộ quy luật chống spam |
| 6 | Mọi lỗi "không xem được bài" đều là `"Song does not exist"` | Cố ý, để không lộ bài đang chờ duyệt / bị ẩn |
| 7 | Lịch sử nghe tự xóa sau 90 ngày | Thiết kế |
| 8 | `releaseAt` có phần lẻ giây dài như `2026-10-12T00:26:13.14861` | Bài được duyệt không hẹn giờ → lấy đúng thời điểm duyệt, có phần lẻ giây |
| 9 | Trang 2 của danh sách có thể trùng / thiếu bài nếu có bài mới phát hành đúng lúc đang chuyển trang | Phân trang theo offset |
| 10 | Mở `/swagger-ui.html` không cần đăng nhập | Bật cho nhóm frontend tiện tra cứu trong giai đoạn phát triển |

---

## PHẦN 15 — CÁCH BÁO BUG

### 15.1. Bảng ghi kết quả

Tạo Google Sheets (hoặc Excel) với các cột:

| Mã test | Người test | Ngày giờ | Kết quả (PASS / FAIL / BLOCKED) | Thực tế nhận được | Ghi chú |
|---|---|---|---|---|---|
| AR-UP-01 | Tester A | 15/10 18:00 | PASS | | |
| AR-UP-44 | Tester A | 15/10 18:05 | FAIL | 500 Internal server error | Ảnh chụp đính kèm |

- **PASS**: đúng hoàn toàn như cột mong đợi (cả mã HTTP lẫn message).
- **FAIL**: sai mã HTTP, sai message, sai dữ liệu, hoặc 500.
- **BLOCKED**: không test được (VD thiếu tài khoản Admin, chưa có bài phát hành).

### 15.2. Mẫu báo 1 bug

```
[MÃ TEST] AR-UP-44
[MỨC ĐỘ] Nghiêm trọng / Trung bình / Nhẹ
[API] POST /api/artist/songs
[TÀI KHOẢN] testerA_artist (ARTIST)
[DỮ LIỆU GỬI]
    title = Bài giả
    audio = fake.mp3 (file txt đổi đuôi, 1KB)
[MONG ĐỢI] 400 "Invalid or corrupted audio file"
[THỰC TẾ]  500 {"message": "Internal server error"}
[THỜI ĐIỂM] 15/10/2026 18:05:12   ← rất quan trọng để tìm log trên server
[ẢNH CHỤP] (đính kèm)
```

### 15.3. Mức độ

| Mức | Ví dụ |
|---|---|
| **Nghiêm trọng** | Lỗi 500; xem/sửa/xóa được bài của người khác; USER gọi được API Admin; lộ bài chưa duyệt; sai múi giờ 7 tiếng; mất dữ liệu |
| **Trung bình** | Sai mã HTTP; sai logic trạng thái; playCount đếm sai; lịch sử sai thứ tự |
| **Nhẹ** | Sai chính tả message; thiếu trường không quan trọng |

### 15.4. Lưu ý khi chụp ảnh

- **Che token** (chuỗi dài bắt đầu bằng `eyJ...`) và **mật khẩu** trước khi gửi ảnh.
- Chụp cả **URL + method + body gửi đi** và **kết quả trả về** trong cùng 1 ảnh.
- Ghi **giờ chính xác** lúc gửi request, để Winhmm tra log trên server.

---

## PHỤ LỤC — BẢNG TỔNG HỢP API

| # | Method | URL | Role | Body | UC |
|---|---|---|---|---|---|
| 1 | GET | `/api/songs?page&size` | Mọi role | — | UC20 |
| 2 | GET | `/api/songs/search?keyword&page&size` | Mọi role | — | UC21 |
| 3 | GET | `/api/songs/{songId}` | Mọi role | — | UC22 |
| 4 | GET | `/uploads/songs/audio/{file}` | Không cần đăng nhập | — | UC23 |
| 5 | POST | `/api/songs/{songId}/play` | Mọi role | — | UC24 |
| 6 | GET | `/api/songs/artists/{artistId}?page&size` | Mọi role | — | UC25 |
| 7 | POST | `/api/artist/songs` | ARTIST | form-data: title, audio, cover, featuredArtistIds, releaseAt | UC26 |
| 8 | GET | `/api/artist/songs` | ARTIST | — | UC28 |
| 9 | PUT | `/api/artist/songs/{songId}` | ARTIST | JSON: title, featuredArtistIds | UC29 |
| 10 | PUT | `/api/artist/songs/{songId}/cover` | ARTIST | form-data: cover | UC30 |
| 11 | PUT | `/api/artist/songs/{songId}/release` | ARTIST | JSON: releaseAt | UC31 |
| 12 | PUT | `/api/artist/songs/{songId}/hide` | ARTIST | — | UC32 |
| 13 | PUT | `/api/artist/songs/{songId}/show` | ARTIST | — | UC32 |
| 14 | DELETE | `/api/artist/songs/{songId}` | ARTIST | — | UC33 |
| 15 | GET | `/api/admin/songs?status&page&size` | ADMIN | — | UC34 |
| 16 | PUT | `/api/admin/songs/{songId}/approve` | ADMIN | — | UC35 |
| 17 | PUT | `/api/admin/songs/{songId}/reject` | ADMIN | JSON: reason | UC35 |
| 18 | PUT | `/api/admin/songs/{songId}/remove` | ADMIN | JSON: reason | UC36 |
| 19 | GET | `/api/artist/search-artists?keyword` | ARTIST | — | UC37 |
| 20 | GET | `/api/users/me/history` | Mọi role | — | Lịch sử nghe |

### Bảng tra nhanh message lỗi

| Message | Gặp ở |
|---|---|
| `Song does not exist` | Bài không tồn tại / chưa phát hành / đã xóa / không phải của mình |
| `Artist does not exist` | Trang Artist: id không phải Artist hoặc bị khóa |
| `Title is required` | Upload / sửa thiếu tên |
| `Title must not exceed 150 characters` | Tên > 150 ký tự |
| `Audio file is required` | Upload thiếu file nhạc |
| `Only MP3 or WAV files are allowed` | Sai đuôi file nhạc |
| `MP3 file must not exceed 20MB` | MP3 quá lớn |
| `WAV file must not exceed 100MB` | WAV quá lớn |
| `Invalid or corrupted audio file` | File nhạc giả / hỏng |
| `Image file is required` | Đổi ảnh nhưng file rỗng |
| `Only JPG, PNG or WEBP images are allowed` | Sai định dạng ảnh |
| `Image must not exceed 2MB` | Ảnh quá lớn |
| `Invalid or corrupted image file` | Ảnh giả / hỏng |
| `File is required` | Thiếu hẳn key file |
| `File is too large` (413) | Vượt giới hạn upload tổng của server |
| `Release time must be in the future` | Lịch ở quá khứ |
| `Cannot change the schedule of a released song` | Đổi lịch bài đã phát hành |
| `Invalid value for releaseAt` | Sai định dạng ngày (form-data) |
| `You cannot feature yourself` | Feat chính mình |
| `Some featured artists do not exist` | Feat id không tồn tại |
| `Featured artist is not available: <id>` | Feat USER hoặc Artist bị khóa |
| `Maximum 5 featured artists` | Feat > 5 người |
| `Only pending songs can be approved` | Duyệt bài không phải PENDING |
| `Only pending songs can be rejected` | Từ chối bài không phải PENDING |
| `Only approved songs can be removed` | Gỡ bài không phải APPROVED / HIDDEN |
| `Artist account is not active` | Duyệt bài của Artist bị khóa |
| `Only approved songs can be hidden` | Ẩn bài không phải APPROVED |
| `Only hidden songs can be shown again` | Hiện bài không phải HIDDEN |
| `Only active artists can manage songs` | Artist bị khóa / mất quyền vẫn gọi API quản lý |
| `Reason is required` | Từ chối / gỡ thiếu lý do |
| `Reason must not exceed 255 characters` | Lý do > 255 ký tự |
| `Invalid parameter: <tên>` | Tham số URL sai kiểu (chữ thay vì số, enum sai) |
| `Invalid request data` | JSON sai cú pháp / sai kiểu dữ liệu |
| `Too many requests. Please try again in X seconds` | Rate limit (429) |
| `Your session has ended. Your account may have been logged in on another device` | Tài khoản đăng nhập nơi khác (401) |

---

**Tổng số test case: khoảng 300.** Không cần làm hết trong 1 ngày — ưu tiên theo thứ tự:
1. Phần 13 (kịch bản đầu-cuối) — bắt được nhiều lỗi nhất
2. Phần 12.1 (ma trận phân quyền) — lỗi bảo mật
3. Các test **thành công** (AR-UP-01..18, AD-AP-01..03, LS-01..06, HS-01..08)
4. Các test lỗi còn lại

Chúc 2 bạn test vui! 🎧