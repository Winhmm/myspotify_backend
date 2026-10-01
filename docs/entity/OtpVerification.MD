# OtpVerification Entity

# 1. Mục đích của `OtpVerification`

`OtpVerification` là Entity dùng để lưu thông tin về **mã OTP** được gửi cho người dùng.

OTP được sử dụng để xác thực các chức năng cần kiểm tra quyền sở hữu email, ví dụ:

* Xác thực email khi đăng ký tài khoản.
* Quên mật khẩu.
* Đổi email.
* Xác thực các thao tác bảo mật thông qua email.

Mỗi OTP được lưu vào bảng:

```text
otp_verifications
```

Entity `OtpVerification` tương ứng với bảng này trong database.

---

# 2. Cấu trúc tổng quan

Entity có các thuộc tính:

```text
OtpVerification
│
├── id
├── user
├── otpCode
├── purpose
├── newEmail
├── expiredAt
└── verified
```

Có thể hiểu đơn giản:

```text
OTP Verification
│
├── OTP này thuộc về User nào?
├── Mã OTP là gì?
├── OTP được dùng cho mục đích gì?
├── Nếu đổi email thì email mới là gì?
├── OTP hết hạn lúc nào?
└── OTP đã được xác thực chưa?
```

---

# 3. Entity và Table

```text
@Entity
@Table(name = "otp_verifications")
```

## `@Entity`

`@Entity` cho Spring Boot biết rằng class `OtpVerification` là một **JPA Entity**.

Hibernate sẽ quản lý Entity này và ánh xạ nó với một bảng trong database.

Nói đơn giản:

```text
Java Class
     ↓
OtpVerification
     ↓
Hibernate / JPA
     ↓
Database Table
```

---

## `@Table(name = "otp_verifications")`

Annotation này chỉ định tên bảng trong database:

```text
otp_verifications
```

Vì vậy:

```text
OtpVerification
```

sẽ tương ứng với:

```text
otp_verifications
```

---

# 4. Field `id`

```text
private Long id;
```

Đây là ID của bản ghi OTP.

```text
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
```

## `@Id`

Đánh dấu `id` là **Primary Key** của bảng.

Database sẽ sử dụng `id` để xác định duy nhất từng bản ghi OTP.

Ví dụ:

```text
id
---
1
2
3
4
```

Mỗi bản ghi OTP sẽ có một ID riêng.

---

## `@GeneratedValue`

```text
@GeneratedValue(strategy = GenerationType.IDENTITY)
```

Cho phép database tự động tạo ID.

Ví dụ:

```text
OTP 1 → id = 1
OTP 2 → id = 2
OTP 3 → id = 3
```

Service không cần tự tạo ID.

---

# 5. Field `user`

```text
private User user;
```

Field này xác định:

> OTP này thuộc về User nào?

---

## `@ManyToOne`

```text
@ManyToOne(fetch = FetchType.LAZY)
```

Quan hệ:

```text
User 1 ---- N OtpVerification
```

Có nghĩa là:

```text
Một User
    ↓
có thể có nhiều OTP
```

Ví dụ:

```text
User
id = 1
email = phong@gmail.com

        ↓

OtpVerification
id = 1
user_id = 1
otp_code = 123456

OtpVerification
id = 2
user_id = 1
otp_code = 654321

OtpVerification
id = 3
user_id = 1
otp_code = 999999
```

Tất cả các OTP trên đều thuộc User có `id = 1`.

---

## Vì sao là `ManyToOne`?

Một người dùng có thể yêu cầu OTP nhiều lần.

Ví dụ:

```text
Đăng ký
   ↓
OTP 123456

Quên mật khẩu
   ↓
OTP 654321

Đổi email
   ↓
OTP 999999
```

Do đó:

```text
1 User
   ↓
Nhiều OtpVerification
```

nên dùng:

```text
@ManyToOne
```

---

# 6. `fetch = FetchType.LAZY`

```text
@ManyToOne(fetch = FetchType.LAZY)
```

`LAZY` có nghĩa là khi lấy `OtpVerification`, Hibernate **không cần tải toàn bộ User ngay lập tức**.

Ví dụ:

```text
OtpVerification
├── id
├── otpCode
├── purpose
├── expiredAt
└── user
```

Nếu code chỉ cần:

```text
otpVerification.getOtpCode()
```

thì không nhất thiết phải lấy toàn bộ thông tin User ngay.

Điều này giúp tránh việc lấy dữ liệu không cần thiết.

Có thể hiểu đơn giản:

```text
LAZY
=
Chỉ lấy User khi thực sự cần sử dụng.
```

---

# 7. `@JoinColumn`

```text
@JoinColumn(name = "user_id", nullable = false)
```

Field:

```text
private User user;
```

sẽ được ánh xạ vào column:

```text
user_id
```

trong database.

Database sẽ có:

```text
otp_verifications
-----------------
id
user_id
otp_code
purpose
new_email
expired_at
verified
```

`user_id` là Foreign Key trỏ đến:

```text
users.id
```

Quan hệ:

```text
users
  │
  │ id
  │
  ▼
otp_verifications
  user_id
```

---

## `nullable = false`

```text
nullable = false
```

Có nghĩa là mỗi OTP **bắt buộc phải thuộc về một User**.

Không được phép:

```text
user_id = NULL
```

---

# 8. Field `otpCode`

```text
private String otpCode;
```

Đây là mã OTP được gửi cho người dùng.

Ví dụ:

```text
123456
```

---

## `@Column`

```text
@Column(name = "otp_code", nullable = false, length = 10)
```

Database sẽ lưu field này trong column:

```text
otp_code
```

Các cấu hình:

```text
nullable = false
```

OTP bắt buộc phải có.

```text
length = 10
```

OTP tối đa 10 ký tự.

Ví dụ:

```text
123456
```

là hợp lệ.

---

# 9. Field `purpose`

```text
private OtpPurpose purpose;
```

Field này xác định:

> OTP này được sử dụng cho mục đích gì?

Project sử dụng Enum:

```text
OtpPurpose
```

Ví dụ Enum có thể gồm:

```text
REGISTER
RESET_PASSWORD
CHANGE_EMAIL
```

---

## `@Enumerated(EnumType.STRING)`

```text
@Enumerated(EnumType.STRING)
```

Cho Hibernate biết cách lưu Enum vào database.

Sử dụng:

```text
EnumType.STRING
```

thì database sẽ lưu:

```text
REGISTER
RESET_PASSWORD
CHANGE_EMAIL
```

thay vì:

```text
0
1
2
```

Cách lưu bằng String dễ đọc và an toàn hơn khi kiểm tra database.

---

# 10. Ý nghĩa của `purpose`

Ví dụ:

```text
otpCode = 123456
purpose = REGISTER
```

Có nghĩa:

```text
OTP 123456
↓
dùng để xác thực đăng ký tài khoản
```

Hoặc:

```text
otpCode = 654321
purpose = RESET_PASSWORD
```

Có nghĩa:

```text
OTP 654321
↓
dùng để xác thực quá trình quên mật khẩu
```

Hoặc:

```text
otpCode = 999999
purpose = CHANGE_EMAIL
```

Có nghĩa:

```text
OTP 999999
↓
dùng để xác thực việc đổi email
```

---

# 11. Field `newEmail`

```text
private String newEmail;
```

Field này dùng trong trường hợp người dùng muốn **đổi email**.

Ví dụ:

```text
Email hiện tại:
old@gmail.com

Email mới:
new@gmail.com
```

Khi gửi OTP để xác nhận đổi email:

```text
otpCode = 123456
purpose = CHANGE_EMAIL
newEmail = new@gmail.com
```

Sau khi OTP được xác thực thành công:

```text
User.email
    ↓
new@gmail.com
```

---

## Vì sao không lưu `newEmail` trực tiếp vào User?

Vì khi người dùng mới yêu cầu đổi email:

```text
Email cũ:
old@gmail.com

Email mới:
new@gmail.com
```

chúng ta **chưa nên thay đổi email của User ngay lập tức**.

Phải đợi:

```text
Gửi OTP
   ↓
User nhập OTP
   ↓
Kiểm tra OTP
   ↓
OTP đúng?
   ↓
Có
   ↓
Mới cập nhật email
```

Vì vậy `newEmail` tạm thời được lưu trong `OtpVerification`.

---

# 12. Field `expiredAt`

```text
private LocalDateTime expiredAt;
```

Field này lưu thời điểm OTP hết hạn.

Ví dụ:

```text
OTP được tạo:
10:00

expiredAt:
10:05
```

Khi User nhập OTP lúc:

```text
10:03
```

OTP vẫn còn hiệu lực.

Nhưng nếu nhập lúc:

```text
10:06
```

OTP đã hết hạn.

---

## `LocalDateTime`

```text
LocalDateTime
```

dùng để lưu:

```text
Ngày + Giờ
```

Ví dụ:

```text
2026-09-26 10:05:00
```

---

## `nullable = false`

```text
@Column(name = "expired_at", nullable = false)
```

Mỗi OTP bắt buộc phải có thời gian hết hạn.

Không được:

```text
expired_at = NULL
```

---

# 13. Field `verified`

```text
private boolean verified = false;
```

Field này xác định OTP đã được xác thực hay chưa.

Ban đầu:

```text
verified = false
```

Có nghĩa:

```text
OTP chưa được sử dụng/xác thực thành công.
```

Sau khi User nhập đúng OTP:

```text
verified = true
```

Có nghĩa:

```text
OTP đã được xác thực thành công.
```

---

## Vì sao mặc định là `false`?

Khi OTP vừa được tạo:

```text
OTP được tạo
    ↓
chưa xác thực
    ↓
verified = false
```

Chỉ sau khi kiểm tra OTP thành công mới chuyển thành:

```text
verified = true
```

---

# 14. Getter và Setter

Entity có các Getter và Setter:

```text
getId()
setId()

getUser()
setUser()

getOtpCode()
setOtpCode()

getPurpose()
setPurpose()

getNewEmail()
setNewEmail()

getExpiredAt()
setExpiredAt()

isVerified()
setVerified()
```

## Getter

Getter dùng để **lấy dữ liệu**.

Ví dụ:

```text
otpVerification.getOtpCode()
```

Có nghĩa:

> Lấy mã OTP hiện tại.

```text
otpVerification.getPurpose()
```

Có nghĩa:

> Lấy mục đích của OTP.

```text
otpVerification.getExpiredAt()
```

Có nghĩa:

> Lấy thời gian hết hạn.

```text
otpVerification.isVerified()
```

Có nghĩa:

> Kiểm tra OTP đã được xác thực hay chưa.

---

## Setter

Setter dùng để **thay đổi dữ liệu**.

Ví dụ:

```text
otpVerification.setOtpCode("123456");
```

Thay đổi mã OTP.

```text
otpVerification.setPurpose(OtpPurpose.REGISTER);
```

Đặt mục đích OTP là đăng ký.

```text
otpVerification.setExpiredAt(...);
```

Đặt thời gian hết hạn.

```text
otpVerification.setVerified(true);
```

Đánh dấu OTP đã xác thực.

---

# 15. Vì sao `verified` dùng `isVerified()`?

`verified` là kiểu:

```text
boolean
```

Đối với boolean, Java thường sử dụng:

```text
isVerified()
```

thay vì:

```text
getVerified()
```

Ví dụ:

```text
if (otpVerification.isVerified()) {
    // OTP đã xác thực
}
```

---

# 16. Các chức năng trong project sử dụng Entity này

`OtpVerification` sẽ được sử dụng chủ yếu trong `OtpService`.

---

## 16.1. Đăng ký tài khoản

Luồng:

```text
User nhập:
username
email
password
        ↓
AuthController
        ↓
AuthService
        ↓
Tạo User
        ↓
Tạo OTP
        ↓
OtpVerification
        ↓
Gửi OTP qua Email
```

Ví dụ dữ liệu:

```text
user_id = 1
otp_code = 123456
purpose = REGISTER
expired_at = 10:05
verified = false
```

User nhập OTP:

```text
123456
```

Backend kiểm tra:

```text
OTP có tồn tại?
       ↓
Đúng User?
       ↓
Đúng purpose?
       ↓
Chưa hết hạn?
       ↓
OTP đúng?
       ↓
Có
       ↓
verified = true
       ↓
User.accountStatus = ACTIVE
```

---

# 16.2. Quên mật khẩu

Luồng:

```text
User nhập email
       ↓
Forgot Password
       ↓
Tạo OTP
       ↓
purpose = RESET_PASSWORD
       ↓
Gửi OTP qua email
       ↓
User nhập OTP
       ↓
Kiểm tra OTP
       ↓
OTP đúng
       ↓
Cho phép đặt password mới
```

Ở đây `purpose` giúp Backend biết:

```text
OTP này dùng cho RESET_PASSWORD
```

chứ không phải OTP đăng ký.

---

# 16.3. Đổi email

Luồng:

```text
User nhập email mới
       ↓
Kiểm tra email mới
       ↓
Tạo OTP
       ↓
purpose = CHANGE_EMAIL
       ↓
newEmail = email mới
       ↓
Gửi OTP
       ↓
User nhập OTP
       ↓
OTP đúng
       ↓
User.email = newEmail
```

Ví dụ:

```text
User.email
=
old@gmail.com

OtpVerification.newEmail
=
new@gmail.com
```

Sau khi xác thực:

```text
User.email
=
new@gmail.com
```

---

# 17. Kiểm tra OTP

Khi User nhập OTP, Backend cần kiểm tra nhiều điều kiện.

Có thể hiểu luồng như sau:

```text
User nhập OTP
      ↓
Tìm OTP trong database
      ↓
OTP có tồn tại?
      ↓
Có
      ↓
Kiểm tra User
      ↓
Kiểm tra purpose
      ↓
Kiểm tra verified
      ↓
Kiểm tra expiredAt
      ↓
Kiểm tra otpCode
      ↓
OTP hợp lệ
      ↓
setVerified(true)
```

Các Getter được sử dụng trong quá trình này:

```text
getUser()
getOtpCode()
getPurpose()
getExpiredAt()
isVerified()
```

Setter được sử dụng khi OTP hợp lệ:

```text
setVerified(true)
```

---

# 18. Quan hệ với Database

Database có thể được hình dung như sau:

```text
users
------------------------------------------------
id
username
email
password
...


otp_verifications
------------------------------------------------
id
user_id
otp_code
purpose
new_email
expired_at
verified
```

Quan hệ:

```text
users
  │
  │ 1
  │
  │
  │ N
  ▼
otp_verifications
```

Tức là:

```text
1 User
   │
   ├── OTP đăng ký
   ├── OTP quên mật khẩu
   ├── OTP đổi email
   └── OTP khác
```

---

# 19. Tóm tắt từng field

| Field       | Kiểu dữ liệu    | Ý nghĩa                  |
| ----------- | --------------- | ------------------------ |
| `id`        | `Long`          | ID của OTP               |
| `user`      | `User`          | User sở hữu OTP          |
| `otpCode`   | `String`        | Mã OTP                   |
| `purpose`   | `OtpPurpose`    | Mục đích sử dụng OTP     |
| `newEmail`  | `String`        | Email mới khi đổi email  |
| `expiredAt` | `LocalDateTime` | Thời gian OTP hết hạn    |
| `verified`  | `boolean`       | OTP đã xác thực hay chưa |

---

# 20. Tóm tắt toàn bộ Entity

Có thể hiểu `OtpVerification` bằng một câu:

> `OtpVerification` là Entity lưu một mã OTP gắn với một User, xác định OTP dùng cho mục đích gì, có email mới hay không, hết hạn lúc nào và đã được xác thực hay chưa.

Luồng tổng quát:

```text
User
  │
  │ yêu cầu chức năng cần OTP
  ▼
OtpService
  │
  ├── Tạo OTP
  ├── Xác định purpose
  ├── Xác định expiredAt
  ├── Lưu OtpVerification
  └── Gửi OTP qua Email
           │
           ▼
      User nhập OTP
           │
           ▼
     Kiểm tra OTP
           │
      ┌────┴────┐
      │         │
    Sai        Đúng
      │         │
      ▼         ▼
   Từ chối   verified=true
                │
                ▼
          Thực hiện chức năng
          tương ứng
```

---

# 21. Code Entity tương ứng

```text
package com.winhmm.myspotify.entity;

import com.winhmm.myspotify.enums.OtpPurpose;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_verifications")
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

    // Getter và Setter
    // Dùng để các class khác có thể lấy hoặc thay đổi
    // dữ liệu của OtpVerification thông qua method.

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }

    public OtpPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(OtpPurpose purpose) {
        this.purpose = purpose;
    }

    public String getNewEmail() {
        return newEmail;
    }

    public void setNewEmail(String newEmail) {
        this.newEmail = newEmail;
    }

    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(LocalDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }
}
```

# 22. Kết luận

`OtpVerification` chịu trách nhiệm **lưu trạng thái và thông tin của từng OTP**.

Các thành phần quan trọng nhất:

```text
user
    ↓
OTP thuộc về ai?

otpCode
    ↓
Mã OTP là gì?

purpose
    ↓
OTP dùng để làm gì?

newEmail
    ↓
Email mới nếu đang đổi email

expiredAt
    ↓
OTP hết hạn khi nào?

verified
    ↓
OTP đã được xác thực chưa?
```

Entity này kết hợp với:

```text
User
   ↓
OtpVerificationRepository
   ↓
OtpService
   ↓
AuthService
   ↓
AuthController
```

để xử lý các chức năng:

```text
Đăng ký
   ↓
Xác thực OTP

Quên mật khẩu
   ↓
Xác thực OTP
   ↓
Đặt lại mật khẩu

Đổi email
   ↓
Xác thực OTP
   ↓
Cập nhật email
```
