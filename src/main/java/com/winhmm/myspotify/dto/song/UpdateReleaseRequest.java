package com.winhmm.myspotify.dto.song;

import java.time.LocalDateTime;

/*
    UC31 - Artist đổi lịch phát hành.

    JSON: { "releaseAt": "2026-12-24T20:00:00" }   ← giờ Việt Nam, không kèm múi giờ

    Gửi null → phát hành ngay (sau khi duyệt, hoặc ngay lập tức nếu đã duyệt).
    Kiểm tra "phải ở tương lai" sẽ làm trong service.
*/
public class UpdateReleaseRequest {
    private LocalDateTime releaseAt;

    public LocalDateTime getReleaseAt() { return releaseAt; }
    public void setReleaseAt(LocalDateTime releaseAt) { this.releaseAt = releaseAt; }
}
