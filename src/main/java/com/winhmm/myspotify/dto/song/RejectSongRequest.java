package com.winhmm.myspotify.dto.song;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/*
    UC35 / UC36 - Admin từ chối hoặc gỡ bài, bắt buộc nhập lý do.

    JSON: { "reason": "Ảnh bìa không phù hợp" }
*/
public class RejectSongRequest {
    @NotBlank(message = "Reason is required")
    @Size(max = 255, message = "Reason must not exceed 255 characters")
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
