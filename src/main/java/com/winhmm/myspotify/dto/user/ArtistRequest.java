package com.winhmm.myspotify.dto.user;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    (Các khái niệm cơ bản: xem RegisterRequest. Thứ tự đọc package dto.user: xem UpdateProfileRequest.)

    - NotBlank: không được null, rỗng hoặc chỉ toàn dấu cách.
    - Size    : độ dài chuỗi nằm trong khoảng cho phép.
*/
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/*
    =====================================================
    ARTISTREQUEST - NGHỆ DANH VÀ GIỚI THIỆU CỦA ARTIST (UC14, UC19)
    =====================================================

    API (xem UserController):
        POST /api/users/me/artist-request   → USER gửi yêu cầu trở thành Artist (UC14)
        PUT  /api/users/me/artist-profile   → ARTIST sửa nghệ danh và giới thiệu (UC19)

    JSON frontend gửi lên (giống nhau cho cả 2 API):
        {
            "artistName": "Mây Chiều",
            "bio": "Ballad nhẹ nhàng cho buổi chiều."
        }

    Dùng chung 1 DTO cho 2 API vì có cùng 2 trường và cùng quy tắc kiểm tra.
    Chữ "Request" trong tên class nghĩa là "dữ liệu gửi lên" (như mọi request DTO khác),
    không chỉ riêng "yêu cầu làm Artist", vì class còn dùng cho cả sửa hồ sơ.

    -----------------------------------------------------
    Quy trình trở thành Artist
    -----------------------------------------------------
        1. USER gửi yêu cầu (API thứ nhất)  → lưu artistName, bio, artistRequestStatus = PENDING
        2. Admin xem danh sách chờ duyệt    → GET /api/admin/artist-requests
                                              (Admin đọc artistName, bio trong UserProfileResponse)
        3a. Admin duyệt                      → role = ARTIST, artistRequestStatus = APPROVED
        3b. Admin từ chối                    → artistRequestStatus = REJECTED, xóa artistName và bio
                                              → USER được gửi lại yêu cầu mới

    Điều kiện gửi yêu cầu (UserService.requestBecomeArtist):
    - Tài khoản ACTIVE → nếu không: 400 "Account is not active".
    - Role là USER     → nếu không: 400 "Only users with role USER can request to become an artist"
                          (ARTIST rồi thì không cần, ADMIN thì không được).
    - artistRequestStatus là NONE (chưa gửi bao giờ) hoặc REJECTED (từng bị từ chối)
                       → nếu không: 400 "An artist request already exists or has been approved"
                          (đang PENDING hoặc đã APPROVED, tránh gửi trùng).
    Kết quả: 200 { "message": "Artist request has been submitted" }

    Điều kiện sửa hồ sơ (UserService.updateArtistProfile):
    - Role là ARTIST → nếu không: 400 "Only artists can update artist profile".
    Kết quả: 200, trả về UserProfileResponse đã có nghệ danh mới.
    Nghệ danh mới hiện ngay trên mọi bài hát của Artist,
    vì bài hát chỉ lưu artist_id, còn tên được lấy từ bảng users mỗi lần truy vấn.
*/
public class ArtistRequest {
    /*
        -----------------------------------------------------
        artistName: nghệ danh
        -----------------------------------------------------
        - @NotBlank: bắt buộc → "Artist name is required".
        - @Size(max = 100): tối đa 100 ký tự → "Artist name must not exceed 100 characters".
          Khớp với độ dài cột artist_name (length = 100) trong entity User.

        - Không có @Size(min): nghệ danh 1 ký tự vẫn hợp lệ (VD "K").
        - Không bắt nghệ danh phải duy nhất (cột artist_name không có unique):
          2 Artist có thể trùng nghệ danh, backend phân biệt bằng id.
    */
    @NotBlank(message = "Artist name is required")
    @Size(max = 100, message = "Artist name must not exceed 100 characters")
    private String artistName;

    /*
        -----------------------------------------------------
        bio: đoạn giới thiệu (không bắt buộc)
        -----------------------------------------------------
        - Không có @NotBlank → không gửi cũng được, bio = null.
        - @Size(max = 255): tối đa 255 ký tự → "Bio must not exceed 255 characters".
          Khớp với độ dài cột bio (length = 255) trong entity User.
          bio = null thì @Size bỏ qua, coi như hợp lệ (xem bảng trong RegisterRequest).

        Lưu ý khi sửa hồ sơ (UC19): Service gán cả 2 trường theo đúng giá trị gửi lên.
        Không gửi bio → bio thành null → mất đoạn giới thiệu cũ.
        → Frontend điền sẵn giá trị cũ vào form sửa, và luôn gửi đủ cả 2 trường
          (cách làm giống UpdateSongRequest trong dto.song).

        Gửi "bio": "" (chuỗi rỗng) thì lưu chuỗi rỗng, không phải null.
        Frontend nên coi cả null và "" là "chưa có giới thiệu".
    */
    @Size(max = 255, message = "Bio must not exceed 255 characters")
    private String bio;

    /*
        -----------------------------------------------------
        Getter và setter
        -----------------------------------------------------
        - Setter: Jackson gọi khi đổi JSON thành đối tượng ("artistName" → setArtistName, "bio" → setBio).
        - Getter: UserController gọi request.getArtistName(), request.getBio()
          để truyền xuống UserService.requestBecomeArtist hoặc UserService.updateArtistProfile.
        Cách đọc từng dòng: xem RegisterRequest.
    */
    public String getArtistName() { return artistName; }
    public void setArtistName(String artistName) { this.artistName = artistName; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}