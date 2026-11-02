package com.winhmm.myspotify.dto.song;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    (Các khái niệm cơ bản: xem RegisterRequest. Thứ tự đọc package dto.song: xem UploadSongRequest.)

    - NotBlank: không được null, rỗng hoặc chỉ toàn dấu cách.
    - Size    : độ dài chuỗi nằm trong khoảng cho phép.
*/
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/*
    =====================================================================
    REJECTSONGREQUEST - ADMIN TỪ CHỐI (UC35) HOẶC GỠ (UC36) BÀI, BẮT BUỘC CÓ LÝ DO
    =====================================================================

    API (xem AdminSongController):
        PUT /api/admin/songs/{songId}/reject   → từ chối bài đang chờ duyệt (PENDING)
        PUT /api/admin/songs/{songId}/remove   → gỡ bài đã duyệt (APPROVED hoặc HIDDEN),
                                                 kể cả bài đã duyệt nhưng chưa tới lịch phát hành

    JSON: { "reason": "Ảnh bìa không phù hợp" }

    Dùng chung 1 DTO cho 2 API vì cấu trúc giống hệt nhau (chỉ cần lý do).
    Không cần tạo 2 class giống hệt nhau chỉ khác tên.

    Vì sao bắt buộc có lý do?
    - Artist thấy lý do trong "Bài hát của tôi" (trường rejectReason của SongManageResponse)
      → biết phải sửa gì để gửi lại.
    - Không có lý do, Artist không biết sai ở đâu → gửi lại y nguyên → Admin lại từ chối.

    =====================================================================
    KIỂM TRA
    =====================================================================

    - @NotBlank        : không được thiếu, rỗng, hay toàn dấu cách.
                         (Admin gõ "   " cho có → vẫn bị chặn.)
    - @Size(max = 255) : khớp độ dài cột reject_reason (length = 255) trong entity Song.
                         Không có quy tắc này thì lý do dài hơn 255 ký tự gây lỗi lúc lưu MySQL (lỗi 500).
    Vi phạm → 400 kèm đúng message ghi trong annotation, VD "Reason is required".

    Service còn gọi reason.trim() để bỏ dấu cách thừa ở 2 đầu trước khi lưu
    ("  Ảnh bìa mờ  " → "Ảnh bìa mờ").
*/
public class RejectSongRequest {
    /*
        reason: lý do từ chối hoặc gỡ bài, hiển thị cho Artist xem.
        2 annotation: xem phần "Kiểm tra" ở trên.
    */
    @NotBlank(message = "Reason is required")
    @Size(max = 255, message = "Reason must not exceed 255 characters")
    private String reason;

    /*
        -----------------------------------------------------
        Getter và setter
        -----------------------------------------------------
        - Setter: Jackson gọi khi đổi JSON thành đối tượng ("reason" → setReason).
        - Getter: AdminSongController gọi request.getReason() để truyền xuống AdminSongService.
        Cách đọc từng dòng: xem RegisterRequest.
    */
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}