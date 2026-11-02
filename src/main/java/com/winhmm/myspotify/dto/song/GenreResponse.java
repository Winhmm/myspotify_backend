package com.winhmm.myspotify.dto.song;

/*
    =====================================================================
    GENRERESPONSE - 1 THỂ LOẠI NHẠC TRẢ VỀ FRONTEND
    =====================================================================

    API: GET /api/genres (xem GenreController) → trả về 1 mảng các GenreResponse, xếp theo tên A → Z.

    JSON:
        [
            { "id": 1, "name": "Acoustic" },
            { "id": 2, "name": "Ballad" },
            ...
        ]

    Frontend dùng ở 2 chỗ:
    - Artist đăng bài: ô chọn thể loại (gửi id lên trong trường genreId của UploadSongRequest).
    - Người nghe: trang Genres, bấm vào 1 thể loại → GET /api/songs?genreId=<id>.

    -----------------------------------------------------
    Đây là response DTO đơn giản nhất của project, nên đọc kỹ để hiểu cấu trúc chung
    -----------------------------------------------------
    Mọi response DTO trong package này đều theo cùng 1 khuôn:
    1. Các thuộc tính private.
    2. 1 constructor nhận đủ giá trị.
    3. Chỉ có getter, không có setter.

    Vì sao không có setter?
    - Backend tạo đối tượng 1 lần bằng constructor (trong SongMapper.toGenreResponse), rồi trả về luôn.
      Không ai cần sửa giá trị sau khi tạo.
    - Không có setter thì không có cách nào sửa nhầm.
    - Jackson chỉ cần getter để đổi đối tượng thành JSON:
        getId()   → "id"
        getName() → "name"

    Vì sao class không có dòng import nào?
    Chỉ dùng Long và String, 2 class có sẵn trong gói java.lang mà Java tự import cho mọi file.

    -----------------------------------------------------
    Vì sao không trả thẳng entity Genre?
    -----------------------------------------------------
    Giữ đúng quy ước của project: Controller luôn trả DTO, không trả entity.
    Sau này entity Genre có thêm cột hay quan hệ mới (VD danh sách bài hát của thể loại),
    JSON trả về vẫn giữ nguyên 2 trường, frontend không bị ảnh hưởng.
*/
public class GenreResponse {
    /*
        - id  : id thể loại trong bảng genres. Long để đồng bộ với kiểu id của entity Genre.
        - name: tên thể loại, VD "Ballad", "Chill".
    */
    private Long id;
    private String name;

    /*
        -----------------------------------------------------
        Constructor
        -----------------------------------------------------
        Được gọi trong SongMapper.toGenreResponse(genre):
            new GenreResponse(genre.getId(), genre.getName())

        Class đã tự khai báo constructor có tham số, nên Java không tự tạo constructor rỗng nữa.
        Không sao, vì response DTO chỉ được Jackson đổi thành JSON,
        không bao giờ phải tạo từ JSON (xem LoginResponse).
    */
    public GenreResponse(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    /*
        Getter: Jackson gọi để tạo JSON. Cách đọc từng dòng: xem RegisterRequest.
    */
    public Long getId() { return id; }
    public String getName() { return name; }
}