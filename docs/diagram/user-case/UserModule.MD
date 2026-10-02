# Use-Case Documentation – Module Người Dùng

## 1. Tổng quan

Use-Case Diagram của **Module Người Dùng** mô tả các chức năng mà các Actor có thể thực hiện trong hệ thống.

Module gồm 4 Actor:

```text
Guest
User
Admin
Artist
```

Trong đó:

* **Guest**: người dùng chưa đăng nhập.
* **User**: người dùng đã đăng nhập.
* **Admin**: người quản trị hệ thống.
* **Artist**: người dùng đã được duyệt trở thành Artist.

Module gồm 19 Use Case:

```text
01. Đăng ký
02. Xác thực Email / OTP
03. Đăng nhập
04. Quên mật khẩu
05. Đặt lại mật khẩu

06. Đăng xuất
07. Xem thông tin cá nhân
08. Cập nhật thông tin cá nhân
09. Đổi mật khẩu
10. Cập nhật ảnh đại diện
11. Vô hiệu hóa tài khoản
12. Xóa tài khoản
13. Cập nhật email
14. Gửi yêu cầu trở thành Artist

15. Xem danh sách người dùng
16. Khóa / Mở khóa tài khoản
17. Duyệt yêu cầu trở thành Artist
18. Gán vai trò (phân quyền)

19. Cập nhật hồ sơ Artist
```

---

# 2. Actor

## 2.1. Guest

**Guest** là người dùng chưa đăng nhập vào hệ thống.

Guest có thể thực hiện:

```text
01. Đăng ký
02. Xác thực Email / OTP
03. Đăng nhập
04. Quên mật khẩu
05. Đặt lại mật khẩu
```

Guest chưa có quyền truy cập các chức năng yêu cầu tài khoản đã đăng nhập.

---

## 2.2. User

**User** là người dùng đã đăng nhập vào hệ thống.

User có thể thực hiện:

```text
06. Đăng xuất
07. Xem thông tin cá nhân
08. Cập nhật thông tin cá nhân
09. Đổi mật khẩu
10. Cập nhật ảnh đại diện
11. Vô hiệu hóa tài khoản
12. Xóa tài khoản
13. Cập nhật email
14. Gửi yêu cầu trở thành Artist
```

---

## 2.3. Admin

**Admin** là người có quyền quản trị hệ thống.

Admin có thể thực hiện:

```text
15. Xem danh sách người dùng
16. Khóa / Mở khóa tài khoản
17. Duyệt yêu cầu trở thành Artist
18. Gán vai trò (phân quyền)
```

Các chức năng của Admin yêu cầu quyền quản trị.

---

## 2.4. Artist

**Artist** là User đã được Admin chấp nhận yêu cầu trở thành Artist.

Artist có thể thực hiện:

```text
19. Cập nhật hồ sơ Artist
```

---

# 3. Phân nhóm Use Case

## 3.1. Authentication

```text
01. Đăng ký
02. Xác thực Email / OTP
03. Đăng nhập
04. Quên mật khẩu
05. Đặt lại mật khẩu
```

Nhóm này xử lý quá trình tạo tài khoản, xác thực tài khoản và khôi phục mật khẩu.

---

## 3.2. User

```text
06. Đăng xuất
07. Xem thông tin cá nhân
08. Cập nhật thông tin cá nhân
09. Đổi mật khẩu
10. Cập nhật ảnh đại diện
11. Vô hiệu hóa tài khoản
12. Xóa tài khoản
13. Cập nhật email
14. Gửi yêu cầu trở thành Artist
```

---

## 3.3. Admin

```text
15. Xem danh sách người dùng
16. Khóa / Mở khóa tài khoản
17. Duyệt yêu cầu trở thành Artist
18. Gán vai trò (phân quyền)
```

---

## 3.4. Artist

```text
19. Cập nhật hồ sơ Artist
```

---

# 4. Quan hệ giữa các Use Case

Use-Case Diagram thể hiện các quan hệ `include` và `extend`.

## 4.1. Đăng ký include Xác thực Email / OTP

```text
01. Đăng ký
    |
    | <<include>>
    ↓
02. Xác thực Email / OTP
```

Điều này thể hiện quá trình đăng ký bao gồm bước xác thực Email / OTP.

Luồng tổng quát:

```text
Đăng ký
    ↓
Xác thực Email / OTP
    ↓
Hoàn tất đăng ký
```

---

## 4.2. Quên mật khẩu include Xác thực Email / OTP

```text
04. Quên mật khẩu
    |
    | <<include>>
    ↓
02. Xác thực Email / OTP
```

Người dùng phải xác thực OTP trong quá trình khôi phục mật khẩu.

---

## 4.3. Quên mật khẩu include Đặt lại mật khẩu

```text
04. Quên mật khẩu
    |
    | <<include>>
    ↓
05. Đặt lại mật khẩu
```

Luồng tổng quát:

```text
Quên mật khẩu
    ↓
Xác thực OTP
    ↓
Đặt lại mật khẩu
    ↓
Đăng nhập
```

---

## 4.4. Cập nhật email include Xác thực Email / OTP

```text
13. Cập nhật email
    |
    | <<include>>
    ↓
02. Xác thực Email / OTP
```

Email mới phải được xác thực bằng OTP.

---

## 4.5. Đổi mật khẩu include Xác thực Email / OTP

```text
09. Đổi mật khẩu
    |
    | <<include>>
    ↓
02. Xác thực Email / OTP
```

Việc đổi mật khẩu bao gồm bước gửi và xác thực OTP.

---

## 4.6. Duyệt yêu cầu Artist extend Gửi yêu cầu trở thành Artist

```text
17. Duyệt yêu cầu trở thành Artist
              |
              | <<extend>>
              ↓
14. Gửi yêu cầu trở thành Artist
```

User gửi yêu cầu trước.

Sau đó Admin có thể thực hiện việc duyệt hoặc từ chối yêu cầu.

---

# 5. Chi tiết Use Case

# UC01 – Đăng ký

## 5.1. Mục đích

Cho phép Guest tạo một tài khoản mới trong hệ thống.

## 5.2. Actor

```text
Guest
```

## 5.3. Tiền điều kiện

```text
Guest chưa đăng nhập.
Guest có thông tin đăng ký hợp lệ.
```

## 5.4. Thông tin đầu vào

```text
Username
Email
Password
```

## 5.5. Luồng chính

```text
1. Guest chọn chức năng Đăng ký.
2. Guest nhập thông tin đăng ký.
3. Hệ thống kiểm tra thông tin đăng ký.
4. Hệ thống tạo tài khoản.
5. Tài khoản được đưa vào trạng thái chưa xác thực.
6. Hệ thống gửi OTP qua Email.
7. Guest thực hiện xác thực Email / OTP.
8. Hoàn tất đăng ký.
```

## 5.6. Quan hệ

```text
UC01 – Đăng ký
    |
    | <<include>>
    ↓
UC02 – Xác thực Email / OTP
```

## 5.7. Kết quả

```text
Tài khoản được tạo.
Guest có thể tiếp tục xác thực tài khoản.
```

---

# UC02 – Xác thực Email / OTP

## 6.1. Mục đích

Xác thực Email của người dùng bằng mã OTP.

## 6.2. Actor

```text
Guest
```

Ngoài ra Use Case này được sử dụng trong các chức năng yêu cầu xác thực OTP.

## 6.3. Tiền điều kiện

```text
Đã có thông tin xác thực cần được kiểm tra.
OTP đã được gửi đến Email.
```

## 6.4. Thông tin đầu vào

```text
Email
OTP
```

## 6.5. Luồng chính

```text
1. Guest nhập OTP.
2. Hệ thống kiểm tra OTP.
3. Hệ thống xác thực OTP.
4. Hoàn tất quá trình xác thực.
```

## 6.6. Các Use Case sử dụng

```text
UC01 – Đăng ký
UC04 – Quên mật khẩu
UC09 – Đổi mật khẩu
UC13 – Cập nhật email
```

## 6.7. Kết quả

```text
OTP được xác thực thành công.
```

---

# UC03 – Đăng nhập

## 7.1. Mục đích

Cho phép Guest đăng nhập vào hệ thống bằng thông tin tài khoản.

## 7.2. Actor

```text
Guest
```

## 7.3. Tiền điều kiện

```text
Tài khoản đã tồn tại.
Thông tin đăng nhập hợp lệ.
```

## 7.4. Thông tin đầu vào

```text
Email
Password
```

## 7.5. Luồng chính

```text
1. Guest chọn chức năng Đăng nhập.
2. Guest nhập Email và Password.
3. Hệ thống kiểm tra thông tin tài khoản.
4. Hệ thống kiểm tra mật khẩu.
5. Đăng nhập thành công.
6. Hệ thống tạo phiên xác thực cho User.
7. Guest trở thành User đã đăng nhập.
```

## 7.6. Kết quả

```text
User đăng nhập thành công.
User có thể sử dụng các chức năng yêu cầu đăng nhập.
```

---

# UC04 – Quên mật khẩu

## 8.1. Mục đích

Cho phép người dùng khôi phục quyền truy cập khi quên mật khẩu.

## 8.2. Actor

```text
Guest
```

## 8.3. Tiền điều kiện

```text
Guest có tài khoản trong hệ thống.
Guest nhớ Email của tài khoản.
```

## 8.4. Thông tin đầu vào

```text
Email
```

## 8.5. Luồng chính

```text
1. Guest chọn Quên mật khẩu.
2. Guest nhập Email.
3. Hệ thống tìm tài khoản tương ứng.
4. Hệ thống gửi OTP.
5. Guest xác thực OTP.
6. Guest đặt lại mật khẩu.
7. Guest đăng nhập lại.
```

## 8.6. Quan hệ

```text
UC04 – Quên mật khẩu
    |
    | <<include>>
    ↓
UC02 – Xác thực Email / OTP

UC04 – Quên mật khẩu
    |
    | <<include>>
    ↓
UC05 – Đặt lại mật khẩu
```

---

# UC05 – Đặt lại mật khẩu

## 9.1. Mục đích

Cho phép người dùng tạo mật khẩu mới sau khi xác thực OTP.

## 9.2. Actor

```text
Guest
```

## 9.3. Tiền điều kiện

```text
Người dùng đã thực hiện bước xác thực OTP.
```

## 9.4. Thông tin đầu vào

```text
Email
OTP
New Password
```

## 9.5. Luồng chính

```text
1. Guest nhập OTP.
2. Hệ thống xác thực OTP.
3. Guest nhập mật khẩu mới.
4. Hệ thống cập nhật mật khẩu.
5. Hoàn tất đặt lại mật khẩu.
6. Guest đăng nhập lại.
```

## 9.6. Kết quả

```text
Mật khẩu của tài khoản được cập nhật.
```

---

# UC06 – Đăng xuất

## 10.1. Mục đích

Cho phép User kết thúc trạng thái đăng nhập.

## 10.2. Actor

```text
User
```

## 10.3. Tiền điều kiện

```text
User đã đăng nhập.
```

## 10.4. Luồng chính

```text
1. User chọn Đăng xuất.
2. Hệ thống xử lý yêu cầu đăng xuất.
3. User kết thúc phiên sử dụng.
```

## 10.5. Kết quả

```text
User không còn ở trạng thái đăng nhập.
```

---

# UC07 – Xem thông tin cá nhân

## 11.1. Mục đích

Cho phép User xem thông tin cá nhân của tài khoản.

## 11.2. Actor

```text
User
```

## 11.3. Tiền điều kiện

```text
User đã đăng nhập.
```

## 11.4. Luồng chính

```text
1. User yêu cầu xem thông tin cá nhân.
2. Hệ thống xác định User.
3. Hệ thống lấy thông tin cá nhân.
4. Hệ thống trả thông tin cho User.
```

## 11.5. Kết quả

```text
User xem được thông tin cá nhân của mình.
```

---

# UC08 – Cập nhật thông tin cá nhân

## 12.1. Mục đích

Cho phép User cập nhật thông tin cá nhân.

## 12.2. Actor

```text
User
```

## 12.3. Tiền điều kiện

```text
User đã đăng nhập.
```

## 12.4. Thông tin cập nhật

Theo thiết kế hiện tại:

```text
Full Name
```

## 12.5. Luồng chính

```text
1. User chọn Cập nhật thông tin cá nhân.
2. User nhập thông tin mới.
3. Hệ thống kiểm tra thông tin.
4. Hệ thống cập nhật thông tin.
5. Hoàn tất cập nhật.
```

---

# UC09 – Đổi mật khẩu

## 13.1. Mục đích

Cho phép User thay đổi mật khẩu hiện tại.

## 13.2. Actor

```text
User
```

## 13.3. Tiền điều kiện

```text
User đã đăng nhập.
User biết mật khẩu hiện tại.
```

## 13.4. Thông tin đầu vào

```text
Current Password
OTP
New Password
Confirm New Password
```

## 13.5. Luồng chính

```text
1. User nhập mật khẩu hiện tại.
2. Hệ thống kiểm tra mật khẩu hiện tại.
3. Hệ thống gửi OTP qua Email.
4. User nhập OTP.
5. Hệ thống xác thực OTP.
6. User nhập mật khẩu mới.
7. User xác nhận mật khẩu mới.
8. Hệ thống cập nhật mật khẩu.
```

## 13.6. Quan hệ

```text
UC09 – Đổi mật khẩu
    |
    | <<include>>
    ↓
UC02 – Xác thực Email / OTP
```

---

# UC10 – Cập nhật ảnh đại diện

## 14.1. Mục đích

Cho phép User thay đổi ảnh đại diện của tài khoản.

## 14.2. Actor

```text
User
```

## 14.3. Tiền điều kiện

```text
User đã đăng nhập.
```

## 14.4. Thông tin đầu vào

```text
Avatar File
```

## 14.5. Luồng chính

```text
1. User chọn Cập nhật ảnh đại diện.
2. User chọn ảnh.
3. Hệ thống tiếp nhận ảnh.
4. Hệ thống cập nhật ảnh đại diện.
5. Hoàn tất cập nhật.
```

---

# UC11 – Vô hiệu hóa tài khoản

## 15.1. Mục đích

Cho phép User vô hiệu hóa tài khoản.

## 15.2. Actor

```text
User
```

## 15.3. Tiền điều kiện

```text
User đã đăng nhập.
User biết mật khẩu hiện tại.
```

## 15.4. Thông tin đầu vào

```text
Current Password
Confirmation
```

## 15.5. Luồng chính

```text
1. User chọn Vô hiệu hóa tài khoản.
2. User nhập mật khẩu hiện tại.
3. Hệ thống kiểm tra mật khẩu.
4. User xác nhận vô hiệu hóa.
5. Hệ thống vô hiệu hóa tài khoản.
```

## 15.6. Trạng thái

```text
Account Status = DISABLED
```

## 15.7. Kết quả

```text
Tài khoản chuyển sang trạng thái vô hiệu hóa.
```

---

# UC12 – Xóa tài khoản

## 16.1. Mục đích

Cho phép User yêu cầu xóa tài khoản.

## 16.2. Actor

```text
User
```

## 16.3. Tiền điều kiện

```text
User đã đăng nhập.
User biết mật khẩu hiện tại.
```

## 16.4. Thông tin đầu vào

```text
Current Password
Confirmation
```

## 16.5. Luồng chính

```text
1. User chọn Xóa tài khoản.
2. User nhập mật khẩu hiện tại.
3. Hệ thống kiểm tra mật khẩu.
4. User xác nhận xóa tài khoản.
5. Hệ thống xử lý xóa tài khoản.
```

## 16.6. Trạng thái

Theo thiết kế hiện tại, tài khoản được chuyển sang trạng thái:

```text
Account Status = DELETED
```

---

# UC13 – Cập nhật email

## 17.1. Mục đích

Cho phép User thay đổi Email của tài khoản.

## 17.2. Actor

```text
User
```

## 17.3. Tiền điều kiện

```text
User đã đăng nhập.
Email mới chưa được sử dụng.
```

## 17.4. Thông tin đầu vào

```text
New Email
OTP
```

## 17.5. Luồng chính

```text
1. User nhập Email mới.
2. Hệ thống kiểm tra Email mới chưa được sử dụng.
3. Hệ thống gửi OTP đến Email mới.
4. User nhập OTP.
5. Hệ thống xác thực OTP.
6. Hệ thống cập nhật Email.
```

## 17.6. Quan hệ

```text
UC13 – Cập nhật email
    |
    | <<include>>
    ↓
UC02 – Xác thực Email / OTP
```

---

# UC14 – Gửi yêu cầu trở thành Artist

## 18.1. Mục đích

Cho phép User gửi yêu cầu trở thành Artist.

## 18.2. Actor

```text
User
```

## 18.3. Tiền điều kiện

```text
User đã đăng nhập.
```

## 18.4. Thông tin đầu vào

```text
Artist Name
Bio
```

## 18.5. Luồng chính

```text
1. User chọn chức năng trở thành Artist.
2. User điền thông tin nghệ danh.
3. User gửi yêu cầu.
4. Hệ thống ghi nhận yêu cầu.
5. Yêu cầu chuyển sang trạng thái chờ Admin duyệt.
```

## 18.6. Trạng thái

```text
Artist Request Status = PENDING
```

## 18.7. Kết quả

```text
Yêu cầu được gửi thành công.
User chờ Admin xử lý.
```

---

# UC15 – Xem danh sách người dùng

## 19.1. Mục đích

Cho phép Admin xem danh sách người dùng trong hệ thống.

## 19.2. Actor

```text
Admin
```

## 19.3. Tiền điều kiện

```text
Admin đã đăng nhập.
Admin có quyền quản trị.
```

## 19.4. Luồng chính

```text
1. Admin chọn Xem danh sách người dùng.
2. Hệ thống kiểm tra quyền Admin.
3. Hệ thống lấy danh sách User.
4. Hệ thống trả danh sách cho Admin.
```

## 19.5. Kết quả

```text
Admin xem được danh sách người dùng.
```

---

# UC16 – Khóa / Mở khóa tài khoản

## 20.1. Mục đích

Cho phép Admin khóa hoặc mở khóa tài khoản người dùng.

## 20.2. Actor

```text
Admin
```

## 20.3. Tiền điều kiện

```text
Admin đã đăng nhập.
Admin có quyền quản trị.
Tài khoản cần xử lý tồn tại.
```

## 20.4. Luồng chính

```text
1. Admin chọn tài khoản cần xử lý.
2. Admin chọn thao tác Khóa hoặc Mở khóa.
3. Admin xác nhận thao tác.
4. Hệ thống cập nhật trạng thái tài khoản.
```

## 20.5. Kết quả

```text
Trạng thái tài khoản được cập nhật.
```

---

# UC17 – Duyệt yêu cầu trở thành Artist

## 21.1. Mục đích

Cho phép Admin xem và xử lý yêu cầu trở thành Artist.

## 21.2. Actor

```text
Admin
```

## 21.3. Tiền điều kiện

```text
Admin đã đăng nhập.
Admin có quyền quản trị.
User đã gửi yêu cầu trở thành Artist.
```

## 21.4. Luồng chính

```text
1. Admin xem danh sách yêu cầu Artist.
2. Admin chọn một yêu cầu.
3. Admin lựa chọn Chấp nhận hoặc Từ chối.
4. Hệ thống xử lý kết quả.
```

## 21.5. Khi chấp nhận

```text
Artist Request Status = APPROVED
Role của User = ARTIST
```

## 21.6. Khi từ chối

```text
Artist Request Status = REJECTED
```

## 21.7. Quan hệ

```text
UC17 – Duyệt yêu cầu trở thành Artist
    |
    | <<extend>>
    ↓
UC14 – Gửi yêu cầu trở thành Artist
```

---

# UC18 – Gán vai trò (phân quyền)

## 22.1. Mục đích

Cho phép Admin gán vai trò cho User.

## 22.2. Actor

```text
Admin
```

## 22.3. Tiền điều kiện

```text
Admin đã đăng nhập.
Admin có quyền quản trị.
User cần phân quyền tồn tại.
```

## 22.4. Thông tin đầu vào

```text
User
Role
```

## 22.5. Các Role

```text
USER
ARTIST
ADMIN
```

## 22.6. Luồng chính

```text
1. Admin chọn User cần phân quyền.
2. Admin chọn Role.
3. Admin xác nhận thao tác.
4. Hệ thống cập nhật Role cho User.
5. Hoàn tất phân quyền.
```

## 22.7. Kết quả

```text
Role của User được cập nhật.
```

---

# UC19 – Cập nhật hồ sơ Artist

## 23.1. Mục đích

Cho phép Artist cập nhật thông tin hồ sơ Artist.

## 23.2. Actor

```text
Artist
```

## 23.3. Tiền điều kiện

```text
User đã được xác nhận là Artist.
User có quyền Artist.
```

## 23.4. Thông tin cập nhật

Theo thiết kế của Module:

```text
Artist Name
Bio
```

## 23.5. Luồng chính

```text
1. Artist chọn Cập nhật hồ sơ Artist.
2. Artist nhập thông tin mới.
3. Hệ thống kiểm tra thông tin.
4. Hệ thống cập nhật hồ sơ Artist.
5. Hoàn tất cập nhật.
```

## 23.6. Kết quả

```text
Thông tin hồ sơ Artist được cập nhật.
```

---

# 24. Luồng đăng ký hoàn chỉnh

```text
Guest
    ↓
01. Đăng ký
    ↓
02. Xác thực Email / OTP
    ↓
Xác thực thành công
    ↓
Hoàn tất đăng ký
```

---

# 25. Luồng đăng nhập

```text
Guest
    ↓
03. Đăng nhập
    ↓
Kiểm tra thông tin tài khoản
    ↓
Đăng nhập thành công
    ↓
User
```

---

# 26. Luồng quên mật khẩu

```text
Guest
    ↓
04. Quên mật khẩu
    ↓
Nhập Email
    ↓
02. Xác thực Email / OTP
    ↓
05. Đặt lại mật khẩu
    ↓
Nhập mật khẩu mới
    ↓
Đăng nhập
```

---

# 27. Luồng đổi mật khẩu

```text
User
    ↓
09. Đổi mật khẩu
    ↓
Nhập mật khẩu hiện tại
    ↓
Kiểm tra mật khẩu hiện tại
    ↓
Gửi OTP qua Email
    ↓
02. Xác thực Email / OTP
    ↓
Nhập mật khẩu mới
    ↓
Xác nhận mật khẩu mới
    ↓
Cập nhật mật khẩu
```

---

# 28. Luồng cập nhật Email

```text
User
    ↓
13. Cập nhật email
    ↓
Nhập Email mới
    ↓
Kiểm tra Email mới chưa được sử dụng
    ↓
Gửi OTP đến Email mới
    ↓
02. Xác thực Email / OTP
    ↓
Cập nhật Email
```

---

# 29. Luồng vô hiệu hóa tài khoản

```text
User
    ↓
11. Vô hiệu hóa tài khoản
    ↓
Nhập mật khẩu hiện tại
    ↓
Kiểm tra mật khẩu
    ↓
Xác nhận vô hiệu hóa
    ↓
Vô hiệu hóa tài khoản
```

---

# 30. Luồng xóa tài khoản

```text
User
    ↓
12. Xóa tài khoản
    ↓
Nhập mật khẩu hiện tại
    ↓
Kiểm tra mật khẩu
    ↓
Xác nhận xóa tài khoản
    ↓
Xóa tài khoản
```

---

# 31. Luồng trở thành Artist

## 31.1. User gửi yêu cầu

```text
User
    ↓
14. Gửi yêu cầu trở thành Artist
    ↓
Điền thông tin nghệ danh
    ↓
Gửi yêu cầu
    ↓
Chờ Admin duyệt
```

## 31.2. Admin xử lý

```text
Admin
    ↓
17. Duyệt yêu cầu trở thành Artist
    ↓
Xem danh sách yêu cầu
    ↓
Chấp nhận hoặc từ chối
```

Nếu chấp nhận:

```text
Artist Request Status = APPROVED
    ↓
Role = ARTIST
    ↓
User trở thành Artist
```

Nếu từ chối:

```text
Artist Request Status = REJECTED
```

---

# 32. Luồng quản lý tài khoản của Admin

```text
Admin
    ↓
15. Xem danh sách người dùng
    ↓
Chọn tài khoản
    ↓
16. Khóa / Mở khóa tài khoản
    ↓
Xác nhận thao tác
    ↓
Cập nhật trạng thái tài khoản
```

---

# 33. Luồng phân quyền

```text
Admin
    ↓
18. Gán vai trò
    ↓
Chọn User
    ↓
Chọn Role
    ↓
Xác nhận
    ↓
Cập nhật Role
```

Các Role:

```text
USER
ARTIST
ADMIN
```

---

# 34. Luồng cập nhật hồ sơ Artist

```text
Artist
    ↓
19. Cập nhật hồ sơ Artist
    ↓
Nhập Artist Name / Bio
    ↓
Kiểm tra thông tin
    ↓
Cập nhật hồ sơ Artist
```

---

# 35. Bảng tổng hợp Use Case

| ID   | Use Case                       | Actor  | Nhóm           |
| ---- | ------------------------------ | ------ | -------------- |
| UC01 | Đăng ký                        | Guest  | Authentication |
| UC02 | Xác thực Email / OTP           | Guest  | Authentication |
| UC03 | Đăng nhập                      | Guest  | Authentication |
| UC04 | Quên mật khẩu                  | Guest  | Authentication |
| UC05 | Đặt lại mật khẩu               | Guest  | Authentication |
| UC06 | Đăng xuất                      | User   | User           |
| UC07 | Xem thông tin cá nhân          | User   | User           |
| UC08 | Cập nhật thông tin cá nhân     | User   | User           |
| UC09 | Đổi mật khẩu                   | User   | User           |
| UC10 | Cập nhật ảnh đại diện          | User   | User           |
| UC11 | Vô hiệu hóa tài khoản          | User   | User           |
| UC12 | Xóa tài khoản                  | User   | User           |
| UC13 | Cập nhật email                 | User   | User           |
| UC14 | Gửi yêu cầu trở thành Artist   | User   | Artist Request |
| UC15 | Xem danh sách người dùng       | Admin  | Admin          |
| UC16 | Khóa / Mở khóa tài khoản       | Admin  | Admin          |
| UC17 | Duyệt yêu cầu trở thành Artist | Admin  | Admin          |
| UC18 | Gán vai trò (phân quyền)       | Admin  | Admin          |
| UC19 | Cập nhật hồ sơ Artist          | Artist | Artist         |

---

# 36. Bảng Actor – Use Case

| Actor  | Use Case                                             |
| ------ | ---------------------------------------------------- |
| Guest  | UC01, UC02, UC03, UC04, UC05                         |
| User   | UC06, UC07, UC08, UC09, UC10, UC11, UC12, UC13, UC14 |
| Admin  | UC15, UC16, UC17, UC18                               |
| Artist | UC19                                                 |

---

# 37. Bảng quan hệ Use Case

| Use Case chính              | Quan hệ | Use Case liên quan                  |
| --------------------------- | ------- | ----------------------------------- |
| UC01 – Đăng ký              | include | UC02 – Xác thực Email / OTP         |
| UC04 – Quên mật khẩu        | include | UC02 – Xác thực Email / OTP         |
| UC04 – Quên mật khẩu        | include | UC05 – Đặt lại mật khẩu             |
| UC09 – Đổi mật khẩu         | include | UC02 – Xác thực Email / OTP         |
| UC13 – Cập nhật email       | include | UC02 – Xác thực Email / OTP         |
| UC17 – Duyệt yêu cầu Artist | extend  | UC14 – Gửi yêu cầu trở thành Artist |

---

# 38. Trạng thái tài khoản liên quan đến Use Case

Các chức năng trong Module có liên quan đến trạng thái tài khoản:

```text
UNVERIFIED
ACTIVE
DISABLED
DELETED
```

Luồng cơ bản của tài khoản:

```text
Đăng ký
    ↓
UNVERIFIED
    ↓
Xác thực Email / OTP
    ↓
ACTIVE
```

Khi User vô hiệu hóa tài khoản:

```text
ACTIVE
    ↓
DISABLED
```

Khi User xóa tài khoản:

```text
ACTIVE
    ↓
DELETED
```

---

# 39. Trạng thái yêu cầu Artist

Yêu cầu trở thành Artist sử dụng các trạng thái:

```text
NONE
PENDING
APPROVED
REJECTED
```

Luồng chính:

```text
NONE
    ↓
User gửi yêu cầu
    ↓
PENDING
    ↓
Admin xử lý
    ↓
┌───────────────┐
↓               ↓
APPROVED       REJECTED
↓
Role = ARTIST
```

---

# 40. Tổng quan chức năng theo Actor

## Guest

```text
Đăng ký
    ↓
Xác thực Email / OTP
    ↓
Đăng nhập

Quên mật khẩu
    ↓
Xác thực OTP
    ↓
Đặt lại mật khẩu
    ↓
Đăng nhập
```

## User

```text
Đăng xuất

Xem thông tin cá nhân
Cập nhật thông tin cá nhân
Cập nhật ảnh đại diện

Đổi mật khẩu
    ↓
Xác thực OTP

Cập nhật email
    ↓
Xác thực OTP

Vô hiệu hóa tài khoản
Xóa tài khoản

Gửi yêu cầu trở thành Artist
```

## Admin

```text
Xem danh sách người dùng
Khóa / Mở khóa tài khoản
Duyệt yêu cầu trở thành Artist
Gán vai trò
```

## Artist

```text
Cập nhật hồ sơ Artist
```

---

# 41. Tổng kết

Use-Case Diagram của Module Người Dùng mô tả toàn bộ chức năng liên quan đến tài khoản, xác thực, quản lý User, quản lý Admin và hồ sơ Artist.

Cấu trúc chức năng tổng quát:

```text
                         USER MODULE
                              |
        ┌─────────────────────┼─────────────────────┐
        ↓                     ↓                     ↓
      GUEST                  USER                  ADMIN
        |                     |                     |
        ↓                     ↓                     ↓
 Authentication          User Management       Admin Management
        |                     |                     |
        ↓                     ↓                     ↓
 Đăng ký                  Đăng xuất            Xem User
 Xác thực OTP             Xem Profile          Khóa/Mở khóa
 Đăng nhập                Cập nhật Profile     Duyệt Artist
 Quên mật khẩu            Đổi Password         Phân quyền
 Đặt lại mật khẩu         Cập nhật Avatar
                          Cập nhật Email
                          Vô hiệu hóa
                          Xóa tài khoản
                          Gửi yêu cầu Artist
                              |
                              ↓
                           ARTIST
                              |
                              ↓
                     Cập nhật hồ sơ Artist
```

Module được tổ chức xoay quanh 4 nhóm Actor:

```text
Guest
    ↓
Authentication

User
    ↓
Account & Profile Management

Admin
    ↓
User & Permission Management

Artist
    ↓
Artist Profile Management
```

Các luồng quan trọng của hệ thống gồm:

```text
Đăng ký
    ↓
Xác thực Email / OTP
    ↓
Hoàn tất đăng ký
```

```text
Quên mật khẩu
    ↓
Xác thực OTP
    ↓
Đặt lại mật khẩu
    ↓
Đăng nhập
```

```text
User
    ↓
Gửi yêu cầu trở thành Artist
    ↓
PENDING
    ↓
Admin duyệt
    ↓
APPROVED / REJECTED
    ↓
Nếu APPROVED → Role = ARTIST
```

Use-Case Documentation này là cơ sở để tiếp tục xây dựng **Activity Diagram, Sequence Diagram, Class Diagram, ERD và triển khai các Controller/Service tương ứng** cho Module Người Dùng.
