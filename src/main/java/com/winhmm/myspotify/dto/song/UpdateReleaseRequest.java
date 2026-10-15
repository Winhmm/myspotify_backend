package com.winhmm.myspotify.dto.song;

import java.time.LocalDateTime;

/*
    =====================================================================
    UpdateReleaseRequest - ARTIST ĐỔI LỊCH PHÁT HÀNH (UC31)
    =====================================================================

    API: PUT /api/artist/songs/{songId}/release   (Content-Type: application/json)

    JSON:
        { "releaseAt": "2026-12-24T20:00:00" }   → lên lịch 20h ngày 24/12
        { "releaseAt": null }                    → phát hành ngay
        { }                                      → giống gửi null (phát hành ngay)

    "20h" là theo giờ của máy chủ (server chạy múi giờ Asia/Ho_Chi_Minh thì là giờ Việt Nam).

    Ý nghĩa của null:
        - Bài đang chờ duyệt (PENDING) hoặc bị từ chối (REJECTED)
                                            → để null, phát hành ngay khi Admin duyệt.
        - Bài đã duyệt nhưng chưa tới lịch (APPROVED, HIDDEN)
                                            → phát hành ngay lập tức (Service gán bằng bây giờ).
        - Bài đã phát hành rồi              → không đổi lịch được nữa, Service báo lỗi.

    =====================================================================
    VÌ SAO KHÔNG CẦN @DateTimeFormat NHƯ UploadSongRequest?
    =====================================================================

    Hai DTO được điền dữ liệu bởi 2 cơ chế khác nhau:
    - UploadSongRequest (form-data, @ModelAttribute): Spring tự đổi chuỗi → LocalDateTime,
      cần @DateTimeFormat để biết định dạng.
    - UpdateReleaseRequest (JSON, @RequestBody): Jackson đọc JSON, và Jackson trong Spring Boot
      mặc định đã hiểu định dạng chuẩn ISO "2026-12-24T20:00:00" → không cần annotation.

    Gửi sai định dạng (VD "24/12/2026") → Jackson không đọc được
        → HttpMessageNotReadableException → 400 "Invalid request data".

    =====================================================================
    VÌ SAO KHÔNG CÓ ANNOTATION KIỂM TRA (@Future...)?
    =====================================================================

    Có thể dùng @Future để bắt buộc thời gian ở tương lai, nhưng null lại là giá trị hợp lệ
    (= phát hành ngay), và còn các quy tắc phụ thuộc trạng thái bài
    (bài đã phát hành thì không đổi lịch được). → Gom hết vào Service cho thống nhất.
*/
public class UpdateReleaseRequest {
    private LocalDateTime releaseAt;

    public LocalDateTime getReleaseAt() { return releaseAt; }
    public void setReleaseAt(LocalDateTime releaseAt) { this.releaseAt = releaseAt; }
}