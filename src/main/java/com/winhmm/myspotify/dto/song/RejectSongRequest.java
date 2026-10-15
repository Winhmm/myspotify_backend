package com.winhmm.myspotify.dto.song;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/*
    =====================================================================
    RejectSongRequest - ADMIN TỪ CHỐI (UC35) HOẶC GỠ (UC36) BÀI, BẮT BUỘC CÓ LÝ DO
    =====================================================================

    API:
        PUT /api/admin/songs/{songId}/reject   → từ chối bài đang chờ duyệt (PENDING)
        PUT /api/admin/songs/{songId}/remove   → gỡ bài đã duyệt (APPROVED hoặc HIDDEN),
                                                 kể cả bài đã duyệt nhưng chưa tới lịch phát hành

    JSON: { "reason": "Ảnh bìa không phù hợp" }

    Dùng chung 1 DTO cho 2 API vì cấu trúc giống hệt nhau (chỉ cần lý do).

    Vì sao bắt buộc có lý do?
    - Artist thấy lý do trong "Bài hát của tôi" → biết phải sửa gì để gửi lại.
    - Không có lý do, Artist không biết sai ở đâu → gửi lại y nguyên → Admin lại từ chối.

    =====================================================================
    KIỂM TRA
    =====================================================================

    - @NotBlank        : không được thiếu, rỗng, hay toàn dấu cách.
                         (Admin gõ "   " cho có → vẫn bị chặn.)
    - @Size(max = 255) : khớp độ dài cột reject_reason (length = 255) trong entity Song.
    Vi phạm → 400 kèm đúng message ghi trong annotation, VD "Reason is required".

    Service còn gọi reason.trim() để bỏ dấu cách thừa ở 2 đầu trước khi lưu.
*/
public class RejectSongRequest {
    @NotBlank(message = "Reason is required")
    @Size(max = 255, message = "Reason must not exceed 255 characters")
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}