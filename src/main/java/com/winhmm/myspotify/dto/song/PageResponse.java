package com.winhmm.myspotify.dto.song;

import java.util.List;

/*
    =====================================================================
    PageResponse<T> - KẾT QUẢ CÓ PHÂN TRANG TRẢ VỀ FRONTEND
    =====================================================================

    Dùng ở: GET /api/songs, /api/songs/search, /api/songs/artists/{id} (phần songs),
            GET /api/admin/songs.

    =====================================================================
    DTO LÀ GÌ?
    =====================================================================

    DTO (Data Transfer Object) là đối tượng chỉ dùng để chở dữ liệu giữa backend và frontend.
    - Không chứa logic nghiệp vụ, không kết nối database.
    - Chỉ có các trường, constructor, getter (và setter nếu là DTO nhận dữ liệu).

    Project có 2 loại DTO:
    - DTO request  (dữ liệu frontend gửi lên): UploadSongRequest, UpdateSongRequest...
    - DTO response (dữ liệu backend trả về)  : SongResponse, PageResponse...

    =====================================================================
    VÌ SAO CẦN PHÂN TRANG?
    =====================================================================

    Giả sử có 10.000 bài hát. Trả hết trong 1 lần:
    - Database phải đọc 10.000 dòng, server tốn bộ nhớ.
    - JSON nặng hàng chục MB, điện thoại tải rất chậm.
    → Chia thành từng trang nhỏ (VD 20 bài), frontend cần trang nào thì xin trang đó.

    =====================================================================
    <T> LÀ GÌ? (GENERIC)
    =====================================================================

    <T> là "kiểu dữ liệu sẽ được điền vào sau", giống cách List<String>, List<User> hoạt động.
    Viết 1 class PageResponse duy nhất, dùng được cho mọi loại dữ liệu:
    - PageResponse<SongResponse>       → 1 trang bài hát cho người nghe.
    - PageResponse<SongManageResponse> → 1 trang bài hát cho Admin.
    - Sau này: PageResponse<PlaylistResponse>, PageResponse<UserProfileResponse>...
    Không có generic → phải viết SongPageResponse, ManagePageResponse... lặp code y hệt nhau.

    =====================================================================
    VÌ SAO KHÔNG TRẢ THẲNG Page<Song> CỦA SPRING?
    =====================================================================

    Page<Song> (kết quả từ Repository) khi chuyển sang JSON có rất nhiều trường lạ:
    "pageable", "sort", "first", "last", "numberOfElements", "empty"...
    Cấu trúc này có thể thay đổi giữa các phiên bản Spring → frontend dễ bị vỡ.
    (Bản thân Spring Data cũng in cảnh báo khuyên không trả thẳng Page ra JSON.)
    → Tự định nghĩa PageResponse gọn gàng, chỉ 5 trường, cố định lâu dài.

    =====================================================================
    JSON TRẢ VỀ
    =====================================================================

    {
        "content": [ ...các bài hát của trang này... ],
        "page": 0,             ← trang hiện tại, đếm từ 0 (trang đầu tiên là 0)
        "size": 20,            ← số bài tối đa mỗi trang
        "totalElements": 135,  ← tổng số bài thỏa điều kiện (của tất cả các trang)
        "totalPages": 7        ← tổng số trang = làm tròn lên (135 / 20) = 7
    }

    Trang cuối có thể ít bài hơn size: trang cuối (page = 6) chỉ có 135 - 6 x 20 = 15 bài.
    Xin trang vượt quá (VD page = 99) → "content": [], các con số tổng vẫn đúng.

    =====================================================================
    VÌ SAO KHÔNG CÓ SETTER?
    =====================================================================

    Đây là DTO response: backend tạo 1 lần bằng constructor rồi gửi đi, không ai sửa nữa.
    - Không có setter → đối tượng không đổi được sau khi tạo (immutable) → an toàn.
    - Jackson chỉ cần getter để chuyển object thành JSON:
        getContent()       → "content"
        getTotalElements() → "totalElements"
      (Jackson bỏ chữ "get", viết thường chữ cái đầu → tên trường JSON.)
*/
public class PageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public PageResponse(List<T> content, int page, int size, long totalElements, int totalPages) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public List<T> getContent() { return content; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
}