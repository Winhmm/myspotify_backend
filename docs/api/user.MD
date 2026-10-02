# MYSPOTIFY – USER MODULE API

# 1. THÔNG TIN CHUNG

## 1.1. Địa chỉ gốc

```text
http://157.66.100.87:8080
```

URL đầy đủ được tạo từ:

```text
Địa chỉ gốc + Đường dẫn API
```

Ví dụ:

```text
http://157.66.100.87:8080/api/auth/login
```

Lưu ý:

* Địa chỉ gốc chỉ gồm IP và port.
* Không thêm `/api/...` vào địa chỉ gốc.
* Không thêm dấu `/` ở cuối địa chỉ gốc.

---

## 1.2. Định dạng dữ liệu

### Request

Các API thông thường sử dụng:

```text
Content-Type: application/json
```

Riêng API cập nhật ảnh đại diện sử dụng:

```text
multipart/form-data
```

### Response

Các API trả dữ liệu dưới dạng:

```text
JSON
```

---

## 1.3. Xác thực bằng JWT

Quy trình sử dụng JWT:

```text
1. Gọi API đăng nhập
        ↓
2. Nhận token
        ↓
3. Frontend lưu token
        ↓
4. Gửi token khi gọi API cần xác thực
```

Ví dụ lưu token:

```text
localStorage
```

Các API yêu cầu token phải gửi header:

```text
Authorization: Bearer <token>
```

Token có hiệu lực:

```text
1 ngày
```

Đăng xuất:

```text
Frontend xóa token đã lưu.
```

---

## 1.4. Định dạng lỗi

Lỗi nghiệp vụ và lỗi dữ liệu thường trả về:

```json
{
    "message": "Email already exists"
}
```

### HTTP Status

| HTTP Status | Ý nghĩa                                                  | Frontend nên làm                       |
| ----------- | -------------------------------------------------------- | -------------------------------------- |
| 200         | Thành công                                               | Xử lý dữ liệu trả về                   |
| 400         | Dữ liệu sai hoặc lỗi nghiệp vụ                           | Hiển thị `message`                     |
| 401         | Chưa đăng nhập, token sai/hết hạn hoặc tài khoản bị khóa | Xóa token và chuyển về trang đăng nhập |
| 403         | Không đủ quyền                                           | Thông báo không có quyền truy cập      |
| 409         | Dữ liệu bị trùng                                         | Hiển thị `message`                     |
| 413         | File quá lớn                                             | Hiển thị `message`                     |

Lưu ý:

```text
401 và 403 không có body.
Frontend chỉ cần kiểm tra HTTP status.
```

---

## 1.5. Ví dụ gọi API bằng JavaScript

### API không cần token

```text
const API_URL = "http://157.66.100.87:8080";

const res = await fetch(API_URL + "/api/auth/login", {
    method: "POST",
    headers: {
        "Content-Type": "application/json"
    },
    body: JSON.stringify({
        email: "abc@gmail.com",
        password: "123456"
    })
});

const data = await res.json();

localStorage.setItem("token", data.token);
```

### API cần token

```text
const res2 = await fetch(API_URL + "/api/users/me/profile", {
    headers: {
        "Authorization": "Bearer " + localStorage.getItem("token")
    }
});
```

### Xử lý token hết hạn

```text
if (res2.status === 401) {
    localStorage.removeItem("token");

    // Chuyển về trang đăng nhập
}
```

---

# 2. DỮ LIỆU TRẢ VỀ DÙNG CHUNG

## 2.1. UserProfile

Nhiều API sử dụng object `UserProfile`.

```json
{
    "id": 1,
    "username": "sontungmtp",
    "email": "mtp@gmail.com",
    "fullName": "Nguyễn Thanh Tùng",
    "avatarUrl": "/uploads/avatars/3f2a9c1e.jpg",
    "accountStatus": "ACTIVE",
    "role": "USER",
    "artistRequestStatus": "NONE",
    "artistName": null,
    "bio": null
}
```

### Các giá trị của `accountStatus`

| Giá trị      | Ý nghĩa           |
| ------------ | ----------------- |
| `UNVERIFIED` | Chưa xác thực     |
| `ACTIVE`     | Đang hoạt động    |
| `DISABLED`   | Đã bị vô hiệu hóa |
| `DELETED`    | Đã xóa            |

### Các giá trị của `role`

| Giá trị  | Ý nghĩa                 |
| -------- | ----------------------- |
| `USER`   | Người dùng thông thường |
| `ARTIST` | Nghệ sĩ                 |
| `ADMIN`  | Quản trị viên           |

### Các giá trị của `artistRequestStatus`

| Giá trị    | Ý nghĩa          |
| ---------- | ---------------- |
| `NONE`     | Chưa gửi yêu cầu |
| `PENDING`  | Đang chờ duyệt   |
| `APPROVED` | Đã được duyệt    |
| `REJECTED` | Bị từ chối       |

### `avatarUrl`

`avatarUrl` là đường dẫn tương đối.

Ví dụ:

```text
/uploads/avatars/3f2a9c1e.jpg
```

Muốn hiển thị ảnh, Frontend ghép địa chỉ gốc vào trước:

```text
http://157.66.100.87:8080/uploads/avatars/3f2a9c1e.jpg
```

Nếu `avatarUrl = null`, Frontend hiển thị ảnh mặc định.

---

## 2.2. Message

Các API không trả dữ liệu cụ thể sẽ trả về:

```json
{
    "message": "..."
}
```

---

# 3. API XÁC THỰC

Base URL:

```text
/api/auth
```

Các API trong phần này không yêu cầu JWT.

---

## 3.1. Đăng ký

### Endpoint

```text
POST /api/auth/register
```

### Request Body

```json
{
    "username": "sontungmtp",
    "email": "mtp@gmail.com",
    "password": "mtp123"
}
```

### Validation

| Trường     | Quy tắc                                    |
| ---------- | ------------------------------------------ |
| `username` | Bắt buộc, 5–20 ký tự                       |
| `email`    | Bắt buộc, đúng định dạng, tối đa 100 ký tự |
| `password` | Bắt buộc, 6–72 ký tự                       |

### Thành công

HTTP Status:

```text
200
```

Response:

```json
{
    "message": "Registration successful, the account is awaiting OTP verification"
}
```

### Lỗi

HTTP Status:

```text
400
```

Các trường hợp:

```text
Email already exists
Username already exists
```

Hoặc dữ liệu không đúng validation.

### Luồng xử lý

```text
Đăng ký
    ↓
Gửi OTP
    ↓
Màn hình nhập OTP
    ↓
POST /api/auth/verify-otp
```

Nếu đăng ký lại bằng email chưa xác thực, hệ thống gửi OTP mới.

---

## 3.2. Xác thực OTP đăng ký

### Endpoint

```text
POST /api/auth/verify-otp
```

### Request Body

```json
{
    "email": "mtp@gmail.com",
    "otpCode": "123456"
}
```

### Thành công

HTTP Status:

```text
200
```

Response:

```json
{
    "message": "Verification successful, the account has been activated"
}
```

### Lỗi

HTTP Status:

```text
400
```

Các trường hợp:

```text
Email does not exist
Otp code does not exist
Otp code has expired
Otp code does not match
```

### OTP

```text
Độ dài: 6 chữ số
Thời gian hiệu lực: 5 phút
```

---

## 3.3. Đăng nhập

### Endpoint

```text
POST /api/auth/login
```

### Request Body

```json
{
    "email": "mtp@gmail.com",
    "password": "mtp123"
}
```

### Thành công

HTTP Status:

```text
200
```

Response:

```json
{
    "token": "eyJhbGciOiJIUzM4NCJ9...",
    "userId": 1,
    "username": "sontungmtp",
    "role": "USER"
}
```

Frontend cần lưu `token`.

Frontend có thể sử dụng `role` để quyết định hiển thị các chức năng tương ứng.

Ví dụ:

```text
ADMIN → hiển thị trang quản trị
USER → không hiển thị trang quản trị
```

### Lỗi

HTTP Status:

```text
400
```

| Message                          | Ý nghĩa                 |
| -------------------------------- | ----------------------- |
| `Email or password is incorrect` | Sai email hoặc mật khẩu |
| `Account is not verified yet`    | Chưa xác thực OTP       |
| `Account has been disabled`      | Tài khoản bị khóa       |
| `Account has been deleted`       | Tài khoản đã bị xóa     |

---

## 3.4. Quên mật khẩu – Gửi OTP

### Endpoint

```text
POST /api/auth/forgot-password
```

### Request Body

```json
{
    "email": "mtp@gmail.com"
}
```

### Thành công

HTTP Status:

```text
200
```

Response:

```json
{
    "message": "OTP code has been sent to your email"
}
```

### Lỗi

HTTP Status:

```text
400
```

```text
Email does not exist
```

---

## 3.5. Đặt lại mật khẩu

### Endpoint

```text
POST /api/auth/reset-password
```

### Request Body

```json
{
    "email": "mtp@gmail.com",
    "otpCode": "123456",
    "newPassword": "matkhaumoi123"
}
```

Frontend sử dụng lại email đã nhập ở bước quên mật khẩu.

Người dùng không cần nhập lại email.

Trường "Nhập lại mật khẩu mới" được Frontend kiểm tra trước khi gửi request.

### Thành công

HTTP Status:

```text
200
```

Response:

```json
{
    "message": "Password has been reset successfully"
}
```

### Lỗi

HTTP Status:

```text
400
```

Các lỗi OTP tương tự bước xác thực OTP.

Mật khẩu mới phải có:

```text
6–72 ký tự
```

---

# 4. API NGƯỜI DÙNG

Base URL:

```text
/api/users
```

Các API trong phần này yêu cầu JWT.

Header:

```text
Authorization: Bearer <token>
```

---

## 4.1. Đăng xuất

### Endpoint

```text
POST /api/users/logout
```

### Thành công

HTTP Status:

```text
200
```

Response:

```json
{
    "message": "Logout successful"
}
```

Sau khi gọi API, Frontend xóa token đã lưu.

---

## 4.2. Xem thông tin cá nhân

### Endpoint

```text
GET /api/users/me/profile
```

### Thành công

HTTP Status:

```text
200
```

Response:

```text
UserProfile
```

Chi tiết `UserProfile` được trình bày tại mục 2.1.

---

## 4.3. Cập nhật thông tin cá nhân

### Endpoint

```text
PUT /api/users/me/profile
```

### Request Body

```json
{
    "fullName": "Nguyễn Thanh Tùng"
}
```

### Validation

| Trường     | Quy tắc                    |
| ---------- | -------------------------- |
| `fullName` | Bắt buộc, tối đa 100 ký tự |

### Thành công

HTTP Status:

```text
200
```

Response:

```text
UserProfile đã được cập nhật
```

---

## 4.4. Đổi mật khẩu – Bước 1: Gửi OTP

### Endpoint

```text
POST /api/users/me/change-password/request-otp
```

### Request Body

```json
{
    "currentPassword": "mtp123"
}
```

### Thành công

HTTP Status:

```text
200
```

Response:

```json
{
    "message": "OTP code has been sent to your email"
}
```

### Lỗi

HTTP Status:

```text
400
```

```text
Password does not match
```

---

## 4.5. Đổi mật khẩu – Bước 2: Xác nhận OTP

### Endpoint

```text
PUT /api/users/me/change-password
```

### Request Body

```json
{
    "otpCode": "123456",
    "newPassword": "matkhaumoi123"
}
```

Frontend tự kiểm tra ô "Nhập lại mật khẩu mới" trước khi gửi request.

### Thành công

HTTP Status:

```text
200
```

Response:

```json
{
    "message": "Password has been changed successfully"
}
```

### Lỗi

HTTP Status:

```text
400
```

Các lỗi OTP tương tự bước xác thực OTP.

Mật khẩu mới phải có:

```text
6–72 ký tự
```

---

## 4.6. Cập nhật ảnh đại diện

### Endpoint

```text
PUT /api/users/me/avatar
```

### Content-Type

```text
multipart/form-data
```

### Form field

```text
file
```

Ví dụ:

```text
const formData = new FormData();

formData.append("file", inputFile.files[0]);

await fetch(API_URL + "/api/users/me/avatar", {
    method: "PUT",
    headers: {
        "Authorization": "Bearer " + localStorage.getItem("token")
    },
    body: formData
});
```

Không tự đặt `Content-Type`.

Trình duyệt sẽ tự thiết lập `Content-Type` cho `multipart/form-data`.

### Thành công

HTTP Status:

```text
200
```

Response:

```text
UserProfile với avatarUrl mới
```

### Lỗi

| Status | Message                                    |
| ------ | ------------------------------------------ |
| 400    | `File is required`                         |
| 400    | `File is empty`                            |
| 400    | `Only JPG, PNG or WEBP images are allowed` |
| 413    | `File size must not exceed 2MB`            |

---

## 4.7. Đổi email – Bước 1: Gửi OTP

### Endpoint

```text
POST /api/users/me/email/request-otp
```

### Request Body

```json
{
    "newEmail": "emailmoi@gmail.com"
}
```

### Thành công

HTTP Status:

```text
200
```

Response:

```json
{
    "message": "OTP code has been sent to your new email"
}
```

### Lỗi

HTTP Status:

```text
400
```

```text
Email already exists
```

---

## 4.8. Đổi email – Bước 2: Xác nhận OTP

### Endpoint

```text
PUT /api/users/me/email
```

### Request Body

```json
{
    "otpCode": "123456"
}
```

### Thành công

HTTP Status:

```text
200
```

Response:

```text
UserProfile với email mới
```

### Lỗi

HTTP Status:

```text
400
```

Các lỗi OTP hoặc:

```text
Email already exists
```

### Sau khi đổi email

Token cũ không còn sử dụng được.

Frontend cần:

```text
1. Xóa token cũ.
2. Chuyển người dùng về trang đăng nhập.
3. Người dùng đăng nhập lại bằng email mới.
```

---

## 4.9. Vô hiệu hóa tài khoản

### Endpoint

```text
PUT /api/users/me/disable
```

### Request Body

```json
{
    "password": "mtp123"
}
```

Frontend nên hiển thị hộp thoại xác nhận trước khi gửi request.

### Thành công

HTTP Status:

```text
200
```

Response:

```json
{
    "message": "Account has been disabled"
}
```

### Lỗi

HTTP Status:

```text
400
```

```text
Password does not match
Account is not active
```

Sau khi vô hiệu hóa:

```text
1. Xóa token.
2. Chuyển về trang đăng nhập.
3. Muốn sử dụng lại tài khoản phải liên hệ Admin.
```

---

## 4.10. Xóa tài khoản

### Endpoint

```text
PUT /api/users/me/delete
```

### Request Body

```json
{
    "password": "mtp123"
}
```

Frontend nên hiển thị hộp thoại xác nhận trước khi gửi request.

### Thành công

HTTP Status:

```text
200
```

Response:

```json
{
    "message": "Account has been deleted"
}
```

### Lỗi

HTTP Status:

```text
400
```

```text
Password does not match
Account is not active
```

Sau khi xóa:

```text
1. Xóa token.
2. Chuyển về trang đăng nhập.
```

---

## 4.11. Gửi yêu cầu trở thành Artist

### Endpoint

```text
POST /api/users/me/artist-request
```

### Request Body

```json
{
    "artistName": "Sơn Tùng M-TP",
    "bio": "Ca sĩ, nhạc sĩ"
}
```

### Validation

| Trường       | Quy tắc                          |
| ------------ | -------------------------------- |
| `artistName` | Bắt buộc, tối đa 100 ký tự       |
| `bio`        | Không bắt buộc, tối đa 255 ký tự |

### Thành công

HTTP Status:

```text
200
```

Response:

```json
{
    "message": "Artist request has been submitted"
}
```

### Lỗi

HTTP Status:

```text
400
```

```text
Only users with role USER can request to become an artist
An artist request already exists or has been approved
```

### Điều kiện hiển thị nút đăng ký Artist

Frontend chỉ hiển thị nút "Đăng ký làm Artist" khi:

```text
role = USER
```

và:

```text
artistRequestStatus = NONE
```

hoặc:

```text
artistRequestStatus = REJECTED
```

---

## 4.12. Cập nhật hồ sơ Artist

### Endpoint

```text
PUT /api/users/me/artist-profile
```

### Quyền

```text
Chỉ role ARTIST
```

### Request Body

```json
{
    "artistName": "Sơn Tùng M-TP",
    "bio": "Giới thiệu mới"
}
```

### Thành công

HTTP Status:

```text
200
```

Response:

```text
UserProfile đã được cập nhật
```

### Lỗi

HTTP Status:

```text
400
```

```text
Only artists can update artist profile
```

---

# 5. API QUẢN TRỊ

Base URL:

```text
/api/admin
```

Các API trong phần này yêu cầu:

```text
JWT của tài khoản ADMIN
```

Nếu tài khoản không phải ADMIN gọi các API này:

```text
HTTP 403
```

`{userId}` trên URL là ID của người dùng cần xử lý.

---

## 5.1. Xem danh sách người dùng

### Endpoint

```text
GET /api/admin/users
```

### Thành công

HTTP Status:

```text
200
```

Response:

```text
Mảng UserProfile
```

Ví dụ:

```json
[
    {
        "id": 1,
        "username": "...",
        "accountStatus": "ACTIVE",
        "role": "USER"
    },
    {
        "id": 2,
        "username": "...",
        "accountStatus": "DISABLED",
        "role": "ARTIST"
    }
]
```

---

## 5.2. Khóa tài khoản

### Endpoint

```text
PUT /api/admin/users/{userId}/lock
```

### Request Body

```text
Không có
```

### Thành công

HTTP Status:

```text
200
```

Response:

```json
{
    "message": "Account has been locked"
}
```

### Lỗi

HTTP Status:

```text
400
```

```text
Cannot lock an admin account
Only active accounts can be locked
User does not exist
```

---

## 5.3. Mở khóa tài khoản

### Endpoint

```text
PUT /api/admin/users/{userId}/unlock
```

### Request Body

```text
Không có
```

### Thành công

HTTP Status:

```text
200
```

Response:

```json
{
    "message": "Account has been unlocked"
}
```

### Lỗi

HTTP Status:

```text
400
```

```text
Only disabled accounts can be unlocked
User does not exist
```

### Gợi ý giao diện

```text
accountStatus = ACTIVE
→ Hiển thị nút "Khóa"

accountStatus = DISABLED
→ Hiển thị nút "Mở khóa"
```

---

## 5.4. Xem danh sách yêu cầu làm Artist

### Endpoint

```text
GET /api/admin/artist-requests
```

### Thành công

HTTP Status:

```text
200
```

Response:

```text
Mảng UserProfile
```

Danh sách chỉ bao gồm các user có:

```text
artistRequestStatus = PENDING
```

Admin sử dụng:

```text
artistName
bio
```

để xem thông tin yêu cầu.

---

## 5.5. Duyệt yêu cầu làm Artist

### Endpoint

```text
PUT /api/admin/artist-requests/{userId}/approve
```

### Request Body

```text
Không có
```

### Thành công

HTTP Status:

```text
200
```

Response:

```json
{
    "message": "Artist request has been approved"
}
```

### Lỗi

HTTP Status:

```text
400
```

```text
No pending artist request for this user
Only users with role USER can become an artist
```

---

## 5.6. Từ chối yêu cầu làm Artist

### Endpoint

```text
PUT /api/admin/artist-requests/{userId}/reject
```

### Request Body

```text
Không có
```

### Thành công

HTTP Status:

```text
200
```

Response:

```json
{
    "message": "Artist request has been rejected"
}
```

### Lỗi

HTTP Status:

```text
400
```

```text
No pending artist request for this user
```

---

## 5.7. Gán vai trò

### Endpoint

```text
PUT /api/admin/users/{userId}/role
```

### Request Body

```json
{
    "role": "ARTIST"
}
```

Các giá trị hợp lệ:

```text
USER
ARTIST
ADMIN
```

### Thành công

HTTP Status:

```text
200
```

Response:

```json
{
    "message": "Role has been assigned successfully"
}
```

### Lỗi

HTTP Status:

```text
400
```

```text
Role must not be null
Invalid request data
Cannot assign role to a deleted or unverified account
User already has this role
```

---

# 6. CÁC LUỒNG MÀN HÌNH GỢI Ý

## 6.1. Đăng ký

```text
Form đăng ký
    ↓
POST /api/auth/register
    ↓
Màn hình nhập OTP
    ↓
POST /api/auth/verify-otp
    ↓
Trang đăng nhập
```

---

## 6.2. Quên mật khẩu

```text
Nhập email
    ↓
POST /api/auth/forgot-password
    ↓
Nhập OTP + mật khẩu mới
    ↓
POST /api/auth/reset-password
    ↓
Trang đăng nhập
```

---

## 6.3. Đổi mật khẩu

```text
Nhập mật khẩu hiện tại
    ↓
POST /api/users/me/change-password/request-otp
    ↓
Nhập OTP + mật khẩu mới
    ↓
PUT /api/users/me/change-password
```

---

## 6.4. Đổi email

```text
Nhập email mới
    ↓
POST /api/users/me/email/request-otp
    ↓
Nhập OTP
    ↓
PUT /api/users/me/email
    ↓
Xóa token
    ↓
Trang đăng nhập
```

---

# 7. LƯU Ý GIAI ĐOẠN PHÁT TRIỂN

## 7.1. OTP

Hiện tại OTP chưa được gửi qua email thật.

OTP được ghi vào log của server.

Khi test, cần lấy mã OTP từ log server.

---

## 7.2. Swagger

Swagger được sử dụng để xem và thử API trên trình duyệt:

```text
http://157.66.100.87:8080/swagger-ui/index.html
```
