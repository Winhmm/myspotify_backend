package com.winhmm.myspotify.controller;

import com.winhmm.myspotify.dto.song.PageResponse;
import com.winhmm.myspotify.dto.song.RejectSongRequest;
import com.winhmm.myspotify.dto.song.SongManageResponse;
import com.winhmm.myspotify.enums.SongStatus;
import com.winhmm.myspotify.mapper.SongMapper;
import com.winhmm.myspotify.service.AdminSongService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/*
    ===================================================================
    API dành cho ADMIN quản lý bài hát (UC34 - UC36).
    ===================================================================

    ---------- 1. KIẾN TRÚC 3 LỚP: MỖI LỚP 1 NHIỆM VỤ ----------

        Frontend ──HTTP──► Controller ──► Service ──► Repository ──► Database

        - Controller : nhận request, trả response (file này).
        - Service    : xử lý nghiệp vụ (kiểm tra trạng thái, quyền, quy tắc).
        - Repository : đọc / ghi database.

    Controller chỉ làm 3 việc:
        1. Nhận request: đọc tham số trên URL, đọc JSON trong body.
        2. Gọi Service để xử lý nghiệp vụ.
        3. Chuyển kết quả (entity Song) sang DTO rồi trả về JSON.

    → Controller không chứa if/else nghiệp vụ (VD: "bài phải đang PENDING mới duyệt được").
      Logic chỉ nằm 1 chỗ là Service, nên:
        + Sửa luật chỉ cần sửa 1 nơi.
        + Viết test cho Service không cần giả lập HTTP.

    ---------- 2. HÀNH TRÌNH CỦA 1 REQUEST ----------

    VD: Admin bấm "Duyệt" bài 15 → frontend gửi PUT /api/admin/songs/15/approve

        (1) JwtAuthenticationFilter
            Đọc header "Authorization: Bearer <token>"
            → kiểm tra chữ ký, hạn dùng, mã phiên trong Redis
            → biết request này của tài khoản nào.

        (2) RateLimitFilter
            Đếm số request của tài khoản trong 1 phút
            → vượt giới hạn thì trả 429, không đi tiếp.

        (3) SecurityConfig
            /api/admin/** không nằm trong permitAll() → bắt buộc đã đăng nhập.
            Chưa đăng nhập → 401.

        (4) @PreAuthorize("hasRole('ADMIN')") ở đầu class này
            Đã đăng nhập nhưng không phải ADMIN → 403.

        (5) Method approveSong(...) trong file này
            → gọi adminSongService.approveSong(15)
            → Service kiểm tra nghiệp vụ, lưu database, trả về Song.

        (6) SongMapper.toManageResponse(song)
            → chuyển entity Song thành DTO SongManageResponse.

        (7) ResponseEntity.ok(...)
            → Jackson chuyển DTO thành JSON, trả HTTP 200 về frontend.

        Bất kỳ bước nào ném lỗi → GlobalExceptionHandler bắt và trả JSON lỗi thống nhất:
            { "message": "Only pending songs can be approved" }

    ---------- 3. CÁC ANNOTATION Ở ĐẦU CLASS ----------

    @PreAuthorize("hasRole('ADMIN')")
        - Đặt ở đầu class → áp dụng cho tất cả method bên dưới, không cần ghi lại từng method.
        - "hasRole('ADMIN')" kiểm tra quyền "ROLE_ADMIN".
          Quyền này do CustomUserDetailsService gán: .roles(user.getRole().name())
          (Spring tự thêm tiền tố "ROLE_" vào trước "ADMIN").
        - Hoạt động được nhờ @EnableMethodSecurity trong SecurityConfig.
          Thiếu annotation đó thì @PreAuthorize bị bỏ qua hoàn toàn (rất nguy hiểm).

    @RestController
        - = @Controller + @ResponseBody.
        - @Controller: báo cho Spring đây là class nhận request HTTP.
        - @ResponseBody: giá trị return được chuyển thành JSON (nhờ thư viện Jackson),
          thay vì hiểu là tên 1 trang HTML.

    @RequestMapping("/api/admin/songs")
        - Tiền tố chung cho mọi API trong class.
        - VD: @PutMapping("/{songId}/approve")
              → URL đầy đủ: PUT /api/admin/songs/{songId}/approve

    ---------- 4. QUY ƯỚC HTTP METHOD ----------

        GET    : chỉ đọc dữ liệu, không thay đổi gì (danh sách bài).
        POST   : tạo mới (đăng bài, ghi nhận lượt nghe).
        PUT    : cập nhật thứ đã có (duyệt, từ chối, gỡ bài → đổi trạng thái bài).
        DELETE : xóa.

    ---------- 5. ĐỊNH DẠNG LỖI CHUNG ----------

        Mọi lỗi đều trả JSON 1 trường: { "message": "..." }
        Frontend luôn đọc lỗi bằng: error.response.data.message

        400 : dữ liệu gửi lên sai, hoặc vi phạm quy tắc nghiệp vụ.
        401 : chưa đăng nhập / token hết hạn / đã đăng nhập ở thiết bị khác.
        403 : đã đăng nhập nhưng không phải ADMIN.
        409 : xung đột, dữ liệu vừa bị người khác thay đổi (Optimistic Locking).
        429 : gửi quá nhiều request trong 1 phút.
*/
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/admin/songs")
public class AdminSongController {
    /*
        Dependency Injection (tiêm phụ thuộc) qua constructor:

        - Controller cần AdminSongService để làm việc, nhưng không tự "new AdminSongService()".
        - Khi ứng dụng khởi động, Spring tự tạo 1 đối tượng AdminSongService duy nhất
          (gọi là "bean", phạm vi singleton) rồi truyền vào constructor bên dưới.
        - Lợi ích:
            + Mọi controller dùng chung 1 đối tượng service, tiết kiệm bộ nhớ.
            + Khi viết test, có thể truyền vào 1 service giả (mock) thay cho service thật.

        final: chỉ gán 1 lần trong constructor, không ai đổi được về sau → an toàn.
    */
    private final AdminSongService adminSongService;

    /*
        Class chỉ có 1 constructor → Spring tự dùng nó để tiêm phụ thuộc,
        không cần ghi @Autowired.
    */
    public AdminSongController(AdminSongService adminSongService) {
        this.adminSongService = adminSongService;
    }

    /*
        ---------------------------------------------------------------
        UC34 - Danh sách bài hát (có phân trang, lọc theo trạng thái).
        ---------------------------------------------------------------

        VÍ DỤ GỌI:
            GET /api/admin/songs                          → mọi bài (trừ đã xóa), trang đầu
            GET /api/admin/songs?status=PENDING           → chỉ bài đang chờ duyệt
            GET /api/admin/songs?status=APPROVED&page=1   → bài đã duyệt, trang thứ 2

        THAM SỐ (@RequestParam: đọc tham số sau dấu "?" trên URL):

        - status (required = false): không bắt buộc.
            + Không gửi → status = null → Service trả mọi bài trừ DELETED.
            + Gửi "PENDING" → Spring tự đổi chuỗi thành enum SongStatus.PENDING.
            + Phải viết hoa đúng tên enum: PENDING, APPROVED, REJECTED, HIDDEN, DELETED.
            + Gửi sai (VD status=ABC hoặc status=pending)
              → Spring không đổi được sang enum
              → MethodArgumentTypeMismatchException
              → GlobalExceptionHandler trả 400 "Invalid parameter: status".

        - page (defaultValue = "0"): trang thứ mấy, đếm từ 0.
            Trang đầu tiên là page = 0, trang thứ hai là page = 1.
            defaultValue viết dạng chuỗi "0" vì mọi giá trị trên URL đều là chữ,
            Spring tự đổi sang int.

        - size (defaultValue = "20"): số bài mỗi trang.
            Service tự chặn trong khoảng 1 - 50:
            gửi size = 100000 → chỉ lấy 50 (chống kéo cả database trong 1 lần làm nặng server).

        LUỒNG XỬ LÝ:
        1. adminSongService.getSongs(...)
           → trả về Page<Song>: danh sách bài của trang này + tổng số bài, tổng số trang.
           Sắp xếp mới tải lên trước (createdAt giảm dần) để bài chờ duyệt mới nhất nằm trên.

        2. SongMapper.toPageResponse(page, SongMapper::toManageResponse)
           → chuyển từng Song trong trang thành SongManageResponse.

           "SongMapper::toManageResponse" là METHOD REFERENCE:
           truyền "cách chuyển 1 bài" vào hàm như truyền 1 tham số.
           Tương đương lambda:  song -> SongMapper.toManageResponse(song)

        3. ResponseEntity.ok(...) → HTTP 200 kèm JSON.

        JSON TRẢ VỀ (rút gọn):
            {
                "content": [
                    {
                        "id": 15,
                        "title": "Chiều Mưa",
                        "status": "PENDING",
                        "released": false,
                        "releaseAt": null,
                        "rejectReason": null,
                        "artistId": 5,
                        "artistName": "Mây Chiều",
                        "featuredArtists": [ { "id": 12, "artistName": "Sóng Nhỏ" } ],
                        ...
                    }
                ],
                "page": 0,
                "size": 20,
                "totalElements": 35,
                "totalPages": 2
            }

        VÌ SAO KHÔNG TRẢ THẲNG ENTITY Song?
        - Song có quan hệ LAZY (artist, featuredArtists): Jackson đọc vào có thể
          kéo theo cả object User, rồi User lại có... → JSON khổng lồ hoặc lỗi.
        - Entity chứa dữ liệu nội bộ không nên lộ ra
          (VD: version, hoặc password đã mã hóa của Artist nằm trong object User).
        - Đổi cấu trúc database sau này sẽ làm vỡ frontend nếu trả thẳng entity.

        → Luôn chuyển sang DTO: chỉ chọn đúng những trường frontend cần.
    */
    @GetMapping
    public ResponseEntity<PageResponse<SongManageResponse>> getSongs(
            @RequestParam(required = false) SongStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(SongMapper.toPageResponse(
                adminSongService.getSongs(status, page, size),
                SongMapper::toManageResponse));
    }

    /*
        ---------------------------------------------------------------
        UC35 - Duyệt bài.
        ---------------------------------------------------------------

        VÍ DỤ GỌI:
            PUT /api/admin/songs/15/approve      (không cần body)

        @PathVariable Long songId: lấy giá trị nằm ngay trong đường dẫn.
        - Khuôn "/{songId}/approve" khớp với URL thật "/15/approve" → songId = 15.
        - Gửi "/abc/approve" → "abc" không đổi được sang Long
          → 400 "Invalid parameter: songId".
        - Dùng Long (không phải long) cho khớp kiểu id trong entity Song.

        SERVICE LÀM GÌ KHI DUYỆT:
        - Bài phải đang PENDING.
        - Artist chính phải còn ACTIVE và còn role ARTIST.
        - releaseAt đang null (Artist muốn phát hành ngay) → gán = thời điểm duyệt.
          releaseAt có giá trị (Artist đã hẹn lịch) → giữ nguyên lịch đó.
        - Xóa lý do từ chối cũ (nếu bài từng bị từ chối rồi gửi lại).
        - Đổi trạng thái sang APPROVED.

        JSON TRẢ VỀ: 1 SongManageResponse với "status": "APPROVED".
            "released": true  → đã tới giờ, người nghe thấy ngay.
            "released": false → đang chờ tới lịch phát hành.

        LỖI CÓ THỂ GẶP:
            400 "Song does not exist"                → sai id, hoặc Artist đã xóa bài
            400 "Only pending songs can be approved" → bài đã duyệt / bị từ chối từ trước
            400 "Artist account is not active"       → Artist bị khóa hoặc mất quyền Artist
            409 "This data was changed by someone else, please reload and try again"
                → Artist sửa / xóa bài đúng lúc Admin bấm duyệt (xem Song.version).
                  Frontend nên tải lại danh sách rồi cho Admin thao tác lại.
    */
    @PutMapping("/{songId}/approve")
    public ResponseEntity<SongManageResponse> approveSong(@PathVariable Long songId) {
        return ResponseEntity.ok(SongMapper.toManageResponse(adminSongService.approveSong(songId)));
    }

    /*
        ---------------------------------------------------------------
        UC35 - Từ chối bài đang chờ duyệt.
        ---------------------------------------------------------------

        VÍ DỤ GỌI:
            PUT /api/admin/songs/15/reject
            Content-Type: application/json
            Body: { "reason": "Ảnh bìa không phù hợp" }

        @RequestBody RejectSongRequest request
            - Jackson đọc JSON trong body → tạo đối tượng RejectSongRequest
              → gọi setReason("Ảnh bìa không phù hợp").
            - Tên trường trong JSON phải trùng tên thuộc tính trong DTO ("reason").
              Gửi "reasons" → reason = null → bị @Valid chặn bên dưới.

        @Valid
            - Sau khi tạo xong DTO, kiểm tra các annotation khai báo trong RejectSongRequest:
                @NotBlank       : không được null, rỗng "", hay toàn dấu cách "   ".
                @Size(max = 255): tối đa 255 ký tự (khớp độ dài cột reject_reason trong database).
            - Vi phạm → MethodArgumentNotValidException → 400 kèm message khai báo trong DTO,
              VD: { "message": "Reason is required" }.
            → Nhờ @Valid, Service không phải tự kiểm tra reason rỗng.

        Body sai cú pháp JSON (thiếu dấu ngoặc, thiếu dấu phẩy...)
            → HttpMessageNotReadableException → 400 "Invalid request data".

        KẾT QUẢ: bài chuyển sang REJECTED, lưu lý do.
            Artist thấy lý do trong "Bài hát của tôi", sửa bài → tự gửi duyệt lại.

        LỖI CÓ THỂ GẶP:
            400 "Reason is required"
            400 "Reason must not exceed 255 characters"
            400 "Song does not exist"
            400 "Only pending songs can be rejected"
            409 (xung đột, như phần duyệt bài)
    */
    @PutMapping("/{songId}/reject")
    public ResponseEntity<SongManageResponse> rejectSong(@PathVariable Long songId,
                                                         @Valid @RequestBody RejectSongRequest request) {
        return ResponseEntity.ok(SongMapper.toManageResponse(
                adminSongService.rejectSong(songId, request.getReason())));
    }

    /*
        ---------------------------------------------------------------
        UC36 - Gỡ bài đã duyệt (kể cả bài Artist đang ẩn).
        ---------------------------------------------------------------

        VÍ DỤ GỌI:
            PUT /api/admin/songs/15/remove
            Body: { "reason": "Vi phạm bản quyền" }

        Dùng lại RejectSongRequest vì cấu trúc giống hệt (chỉ cần 1 trường reason),
        không cần tạo thêm DTO mới.

        KHÁC NHAU GIỮA "TỪ CHỐI" VÀ "GỠ BÀI":
            reject : chỉ áp dụng bài PENDING (chưa từng phát hành, chưa ai nghe).
            remove : chỉ áp dụng bài APPROVED / HIDDEN (đã duyệt, có thể đã có người nghe).
                     VD: sau khi phát hành mới phát hiện vi phạm bản quyền.

        Cả hai đều chuyển bài sang REJECTED + lưu lý do.
        → Người nghe không thấy bài nữa ngay lập tức.
        → Artist thấy lý do, sửa bài thì tự gửi duyệt lại
          (xem resubmitIfRejected trong ArtistSongService).
        → Lượt nghe cũ vẫn được giữ, không bị xóa.

        LỖI CÓ THỂ GẶP:
            400 "Only approved songs can be removed" → bài đang PENDING (hãy dùng reject)
            400 "Song does not exist"
            409 (xung đột)
    */
    @PutMapping("/{songId}/remove")
    public ResponseEntity<SongManageResponse> removeSong(@PathVariable Long songId,
                                                         @Valid @RequestBody RejectSongRequest request) {
        return ResponseEntity.ok(SongMapper.toManageResponse(
                adminSongService.removeSong(songId, request.getReason())));
    }
}
