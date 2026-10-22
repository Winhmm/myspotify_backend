package com.winhmm.myspotify.controller;

import com.winhmm.myspotify.dto.song.GenreResponse;
import com.winhmm.myspotify.mapper.SongMapper;
import com.winhmm.myspotify.service.SongService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/*
    =====================================================================
    GENRECONTROLLER - API THỂ LOẠI NHẠC
    =====================================================================

    API trong class:
        GET /api/genres   → danh sách 25 thể loại, xếp theo tên A → Z

    Ai được gọi:
    - Không có @PreAuthorize → mọi role (USER, ARTIST, ADMIN).
    - Phải đăng nhập (/api/genres không nằm trong permitAll() của SecurityConfig).

    Dùng ở 2 chỗ trên frontend:
    - Artist đăng bài: ô chọn thể loại (bắt buộc chọn 1).
    - Người nghe: trang Genres, bấm vào 1 thể loại → GET /api/songs?genreId=<id>.

    Chỉ có API đọc. Thêm thể loại mới: chạy câu INSERT trong MySQL (không có API Admin).

    Vì sao gọi SongService mà không tạo GenreService riêng?
    - Chỉ có 1 chức năng (lấy danh sách), tạo cả 1 Service riêng thì thừa.
    - Thể loại là 1 phần của module Song, SongService là nghiệp vụ của người nghe.
    - Vẫn giữ đúng kiến trúc 3 lớp: Controller → Service → Repository
      (Controller không gọi thẳng Repository).

    JSON trả về:
        [
            { "id": 1,  "name": "Acoustic" },
            { "id": 2,  "name": "Ballad" },
            ...
        ]
*/
@RestController
@RequestMapping("/api/genres")
public class GenreController {
    private final SongService songService;

    public GenreController(SongService songService) {
        this.songService = songService;
    }

    @GetMapping
    public ResponseEntity<List<GenreResponse>> getGenres() {
        List<GenreResponse> result = songService.getGenres()
                .stream()
                .map(SongMapper::toGenreResponse)
                .toList();

        return ResponseEntity.ok(result);
    }
}
