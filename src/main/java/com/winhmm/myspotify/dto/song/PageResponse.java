package com.winhmm.myspotify.dto.song;

/*
    -----------------------------------------------------
    Dòng import
    -----------------------------------------------------
    (Các khái niệm cơ bản: xem RegisterRequest. Thứ tự đọc package dto.song: xem UploadSongRequest.)

    - List: kiểu danh sách của Java, dùng cho content (danh sách phần tử của 1 trang).
*/
import java.util.List;

/*
    =====================================================================
    PAGERESPONSE<T> - KẾT QUẢ CÓ PHÂN TRANG TRẢ VỀ FRONTEND
    =====================================================================

    Dùng ở: GET /api/songs, /api/songs/search, /api/songs/artists/{id} (phần songs),
            GET /api/admin/songs.

    =====================================================================
    VÌ SAO CẦN PHÂN TRANG?
    =====================================================================

    Giả sử có 10.000 bài hát. Trả hết trong 1 lần:
    - Database phải đọc 10.000 dòng, server tốn bộ nhớ.
    - JSON nặng hàng chục MB, điện thoại tải rất chậm.
    → Chia thành từng trang nhỏ (VD 20 bài), frontend cần trang nào thì xin trang đó.
    Giống cuốn sách: không ai đọc cả cuốn trong 1 trang, mà lật từng trang.

    Frontend xin trang bằng tham số trên URL: GET /api/songs?page=1&size=20
    → "trang thứ 2 (đếm từ 0), mỗi trang 20 bài".

    =====================================================================
    <T> LÀ GÌ? (GENERIC)
    =====================================================================

    <T> là "kiểu dữ liệu sẽ được điền vào sau", giống cách List<String>, List<User> hoạt động.
    T là tên tự đặt (viết tắt của Type), có thể đặt tên khác nhưng T là quy ước phổ biến.

    Viết 1 class PageResponse duy nhất, dùng được cho mọi loại dữ liệu:
    - PageResponse<SongResponse>       → 1 trang bài hát cho người nghe.
    - PageResponse<SongManageResponse> → 1 trang bài hát cho Admin.
    - Sau này: PageResponse<PlaylistResponse>, PageResponse<UserProfileResponse>...
    Không có generic → phải viết SongPageResponse, ManagePageResponse... lặp code y hệt nhau.

    Khi viết PageResponse<SongResponse>, Java coi như mọi chữ T trong class được thay bằng SongResponse:
        private List<T> content   →   private List<SongResponse> content
    Bỏ nhầm 1 SongManageResponse vào PageResponse<SongResponse> → IntelliJ báo lỗi ngay.

    =====================================================================
    VÌ SAO KHÔNG TRẢ THẲNG Page<Song> CỦA SPRING?
    =====================================================================

    Page<Song> (kết quả từ Repository) khi chuyển sang JSON có rất nhiều trường lạ:
    "pageable", "sort", "first", "last", "numberOfElements", "empty"...
    Cấu trúc này có thể thay đổi giữa các phiên bản Spring → frontend dễ bị vỡ.
    (Bản thân Spring Data cũng in cảnh báo khuyên không trả thẳng Page ra JSON.)
    Ngoài ra Page<Song> chứa entity Song, mà project không bao giờ trả entity ra ngoài.
    → Tự định nghĩa PageResponse gọn gàng, chỉ 5 trường, cố định lâu dài.

    Đối tượng được tạo trong SongMapper.toPageResponse(page, converter):
    lấy từng Song trong Page, đổi thành DTO bằng converter (VD SongMapper::toSongResponse),
    rồi gói cùng 4 con số phân trang vào PageResponse.

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
    Frontend dùng totalPages để vẽ các nút chuyển trang (1, 2, 3, ..., 7).

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
/*
    public class PageResponse<T>: khai báo class có 1 kiểu generic tên T.
    Mọi chỗ dùng T bên trong class sẽ mang kiểu được điền vào lúc sử dụng.
*/
public class PageResponse<T> {
    /*
        -----------------------------------------------------
        5 thuộc tính
        -----------------------------------------------------
        - List<T> content    : các phần tử của trang hiện tại (VD 20 SongResponse).
        - int page           : số thứ tự trang, đếm từ 0.
        - int size           : số phần tử tối đa mỗi trang.
        - long totalElements : tổng số phần tử của mọi trang.
                               Dùng long (số nguyên lớn) vì Spring Data trả về kiểu long cho con số này,
                               tổng số bản ghi có thể rất lớn.
        - int totalPages     : tổng số trang.
        int, long là kiểu nguyên thủy (luôn có giá trị, không null), đủ dùng vì 4 con số này luôn tồn tại.
    */
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    /*
        -----------------------------------------------------
        Constructor 5 tham số
        -----------------------------------------------------
        Được gọi trong SongMapper.toPageResponse:
            new PageResponse<>(content, page.getNumber(), page.getSize(),
                               page.getTotalElements(), page.getTotalPages())
        - page.getNumber()       : số trang hiện tại (Spring gọi là number, DTO gọi là page cho dễ hiểu).
        - new PageResponse<>(...): dấu <> rỗng (diamond), Java tự suy ra T từ kiểu của content.
    */
    public PageResponse(List<T> content, int page, int size, long totalElements, int totalPages) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    /*
        Getter: Jackson gọi để tạo JSON.
        getContent() trả về List<T>: với PageResponse<SongResponse> thì là List<SongResponse>,
        Jackson đổi thành mảng JSON, mỗi phần tử là 1 object bài hát.
    */
    public List<T> getContent() { return content; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
}