# RegisterRequest DTO

# 1. RegisterRequest dùng để làm gì?

RegisterRequest là một Request DTO.

DTO là viết tắt của Data Transfer Object, nghĩa là đối tượng dùng để truyền dữ liệu giữa các tầng trong hệ thống.

RegisterRequest được sử dụng để nhận dữ liệu mà Frontend gửi lên Backend khi người dùng đăng ký tài khoản.

Ví dụ Frontend gửi lên 3 thông tin:

* username
* email
* password

Backend sẽ nhận 3 thông tin này vào RegisterRequest.

Luồng tổng quát:

Frontend
→ RegisterRequest
→ AuthController
→ AuthService
→ User
→ Database.

## 2. Vì sao cần RegisterRequest?

Entity User của hệ thống có nhiều thông tin:

* id
* username
* email
* password
* fullName
* avatarUrl
* accountStatus
* role
* artistRequestStatus
* artistName
* bio

Tuy nhiên khi đăng ký tài khoản, người dùng chỉ cần cung cấp:

* username
* email
* password

Vì vậy không nên sử dụng trực tiếp Entity User để nhận dữ liệu đăng ký.

Thay vào đó sử dụng RegisterRequest.

RegisterRequest chỉ chứa những dữ liệu cần thiết cho chức năng đăng ký.

## 3. Dữ liệu Frontend gửi lên

Khi người dùng đăng ký, Frontend sẽ gửi:

* username
* email
* password

Ví dụ:

username = phong123
email = [phong@gmail.com](mailto:phong@gmail.com)
password = 123456

Spring Boot nhận những dữ liệu này và đưa vào RegisterRequest.

## 4. Field username

username là tên tài khoản mà người dùng muốn đăng ký.

Ví dụ:

username = phong123

Backend có thể lấy username từ RegisterRequest thông qua Getter.

Getter getUsername() có nhiệm vụ:

Lấy giá trị username hiện tại trong RegisterRequest.

Ví dụ:

RegisterRequest
→ getUsername()
→ phong123.

## 5. Field email

email là email mà người dùng sử dụng để đăng ký tài khoản.

Ví dụ:

email = [phong@gmail.com](mailto:phong@gmail.com)

Email này có thể được sử dụng cho nhiều chức năng:

* Xác thực tài khoản.
* Nhận OTP.
* Đăng nhập.
* Quên mật khẩu.
* Đổi email.
* Các chức năng bảo mật khác.

Backend lấy email thông qua Getter getEmail().

## 6. Field password

password là mật khẩu người dùng nhập khi đăng ký.

Ví dụ:

password = 123456

Backend lấy password thông qua Getter getPassword().

Sau đó password sẽ được xử lý trong AuthService.

Password không nên được lưu trực tiếp dưới dạng plain text trong database.

Luồng xử lý:

Frontend
→ password
→ RegisterRequest
→ AuthService
→ PasswordEncoder
→ mã hóa password
→ User
→ Database.

## 7. Getter và Setter

RegisterRequest có Getter và Setter cho từng field.

Getter dùng để lấy dữ liệu.

Các Getter gồm:

* getUsername()
* getEmail()
* getPassword()

Ví dụ:

RegisterRequest
→ getUsername()
→ lấy username.

RegisterRequest
→ getEmail()
→ lấy email.

RegisterRequest
→ getPassword()
→ lấy password.

Setter dùng để gán hoặc thay đổi dữ liệu.

Các Setter gồm:

* setUsername()
* setEmail()
* setPassword()

Ví dụ:

setUsername()
→ gán username vào RegisterRequest

setEmail()
→ gán email vào RegisterRequest

setPassword()
→ gán password vào RegisterRequest

## 8. Frontend gửi dữ liệu như thế nào?

Ví dụ người dùng nhập:

- Username: phong123
- Email: [phong@gmail.com](mailto:phong@gmail.com)
- Password: 123456

Frontend gửi Request đến:

POST /api/auth/register

Backend nhận Request.

Spring Boot sẽ chuyển dữ liệu Request thành một object RegisterRequest.

Kết quả:

RegisterRequest
- username = phong123
- email = [phong@gmail.com](mailto:phong@gmail.com)
- password = 123456

## 9. AuthController sử dụng RegisterRequest

AuthController nhận RegisterRequest từ Frontend.

Sau đó Controller sử dụng Getter để lấy dữ liệu:

* getUsername()
* getEmail()
* getPassword()

Sau đó truyền dữ liệu sang AuthService.

Luồng:

RegisterRequest
→ getUsername()
→ getEmail()
→ getPassword()
→ AuthService

## 10. AuthService sử dụng dữ liệu như thế nào?

AuthService nhận:

* username
* email
* password

Sau đó xử lý quá trình đăng ký.

Luồng:

Kiểm tra username
→ Kiểm tra email
→ Kiểm tra password
→ Mã hóa password
→ Tạo User
→ Lưu User vào Database
→ Tạo OTP
→ Gửi OTP qua Email.

## 11. Vì sao RegisterRequest không có id?

RegisterRequest không cần id.

Lý do là id của User được Database tự động tạo.

Khi đăng ký, Frontend chỉ cần gửi:

* username
* email
* password

Không cần gửi id.

Luồng:

Frontend
→ username
→ email
→ password.

Backend tạo User

Database
→ tự động tạo id.

## 12. Vì sao RegisterRequest không có accountStatus?

RegisterRequest không có accountStatus vì Frontend không nên tự quyết định trạng thái tài khoản.

Khi User mới đăng ký, tài khoản chưa xác thực email.

Backend tự đặt:

accountStatus = UNVERIFIED

Sau khi User nhập OTP chính xác:

accountStatus = ACTIVE

Luồng:

Đăng ký
→ accountStatus = UNVERIFIED
→ Gửi OTP
→ User nhập OTP
→ OTP đúng
→ accountStatus = ACTIVE

## 13. Vì sao RegisterRequest không có role?

RegisterRequest không có role.

Frontend không được tự quyết định quyền của tài khoản.

Ví dụ người dùng không được tự gửi:

role = ADMIN

Khi đăng ký, Backend tự đặt Role mặc định:

role = USER

Luồng:

User đăng ký
→ Backend tạo User
→ role = USER

## 14. Vì sao RegisterRequest không có avatarUrl, fullName và bio?

Những thông tin này không bắt buộc trong bước đăng ký.

Sau khi đăng ký thành công, User có thể cập nhật thông tin cá nhân thông qua chức năng cập nhật Profile.

Ví dụ:

Đăng ký
→ username
→ email
→ password

Sau đó:

Cập nhật Profile
→ fullName
→ avatarUrl
→ các thông tin Profile khác

## 15. So sánh RegisterRequest và User

RegisterRequest dùng để nhận dữ liệu từ Client cho chức năng đăng ký.

RegisterRequest gồm:

* username
* email
* password

User là Entity đại diện cho User trong hệ thống và Database.

User gồm:

* id
* username
* email
* password
* fullName
* avatarUrl
* accountStatus
* role
* artistRequestStatus
* artistName
* bio

Có thể hiểu đơn giản:

RegisterRequest
→ Dữ liệu Client gửi lên.

User
→ Dữ liệu User được Backend quản lý và lưu trong Database.

## 16. Luồng hoàn chỉnh

Frontend
→ gửi username, email, password
→ AuthController
→ nhận RegisterRequest
→ sử dụng Getter để lấy dữ liệu
→ AuthService
→ kiểm tra dữ liệu
→ mã hóa password
→ tạo User
→ UserRepository
→ Database
→ tạo OtpVerification
→ gửi OTP qua Email.

## 17. Tóm tắt

RegisterRequest có nhiệm vụ nhận dữ liệu đăng ký từ Frontend.

Ba thông tin chính:

username
→ tên tài khoản.

email
→ email đăng ký và nhận OTP.

password
→ mật khẩu đăng ký.

Getter:

getUsername()
→ lấy username.

getEmail()
→ lấy email.

getPassword()
→ lấy password.

Setter:

setUsername()
→ gán username.

setEmail()
→ gán email.

setPassword()
→ gán password.

Điểm quan trọng nhất:

RegisterRequest không phải User.

RegisterRequest là DTO dùng để nhận dữ liệu từ Client cho một API cụ thể.

User là Entity đại diện cho User trong hệ thống và Database.

Luồng:

Frontend
→ RegisterRequest
→ AuthController
→ AuthService
→ User
→ UserRepository
→ Database.
