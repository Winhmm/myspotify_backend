package com.winhmm.myspotify.mapper;

import com.winhmm.myspotify.dto.user.UserProfileResponse;
import com.winhmm.myspotify.entity.User;

/*
    =====================================================
    USERMAPPER - CHUYỂN ENTITY USER SANG DTO
    =====================================================

    Gom việc chuyển User (entity) sang UserProfileResponse (DTO) về 1 chỗ duy nhất.

    Được dùng ở:
    - UserController: xem và sửa thông tin cá nhân, đổi avatar, sửa hồ sơ Artist.
    - AdminController: danh sách tài khoản, danh sách yêu cầu làm Artist.

    -----------------------------------------------------
    Vì sao tách ra class riêng?
    -----------------------------------------------------
    Trước đây hàm chuyển đổi này bị copy y hệt ở UserController và AdminController.
    Tách ra đây theo nguyên tắc Single Responsibility (chữ S trong SOLID):
    - Controller chỉ lo nhận request và trả response.
    - Mapper chỉ lo chuyển đổi dữ liệu.
    → Thêm 1 trường mới vào UserProfileResponse thì chỉ sửa đúng file này.

    Không trả thẳng entity User ra ngoài vì entity có trường password (đã băm).

    -----------------------------------------------------
    Vì sao toàn hàm static?
    -----------------------------------------------------
    Mapper không lưu trạng thái gì: nhận User vào, trả DTO ra.
    → Gọi thẳng UserMapper.toUserProfileResponse(user), không cần new UserMapper()
      hay để Spring tiêm vào. Giống cách dùng SongMapper.
*/
public class UserMapper {
    private UserMapper() {}

    /*
        -----------------------------------------------------
        User → UserProfileResponse
        -----------------------------------------------------
        Lấy đủ 10 trường frontend cần hiển thị, bỏ trường password.
    */
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