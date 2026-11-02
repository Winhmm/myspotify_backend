package com.winhmm.myspotify.dto.song;

/*
    -----------------------------------------------------
    Dòng import
    -----------------------------------------------------
    (Các khái niệm cơ bản: xem RegisterRequest. Thứ tự đọc package dto.song: xem UploadSongRequest.)

    - LocalDateTime: kiểu ngày giờ của Java (năm, tháng, ngày, giờ, phút, giây), không kèm múi giờ.
*/
import java.time.LocalDateTime;

/*
    =====================================================================
    UPDATERELEASEREQUEST - ARTIST ĐỔI LỊCH PHÁT HÀNH (UC31)
    =====================================================================

    API: PUT /api/artist/songs/{songId}/release   (Content-Type: application/json)
    Xem ArtistSongController.updateRelease.

    JSON:
        { "releaseAt": "2026-12-24T20:00:00" }   → lên lịch 20h ngày 24/12
        { "releaseAt": null }                    → phát hành ngay
        { }                                      → giống gửi null (phát hành ngay)

    Vì sao { } giống gửi null?
    JSON không có trường releaseAt → Jackson không gọi setter → thuộc tính giữ giá trị mặc định.
    Thuộc tính kiểu đối tượng (LocalDateTime) chưa được gán thì mặc định là null.

    "20h" là theo giờ của máy chủ (server chạy múi giờ Asia/Ho_Chi_Minh thì là giờ Việt Nam).

    Ý nghĩa của null (ArtistSongService.updateReleaseSchedule xử lý):
        - Bài đang chờ duyệt (PENDING) hoặc bị từ chối (REJECTED)
                                            → để null, phát hành ngay khi Admin duyệt.
        - Bài đã duyệt nhưng chưa tới lịch (APPROVED, HIDDEN)
                                            → phát hành ngay lập tức (Service gán bằng bây giờ).
        - Bài đã phát hành rồi              → không đổi lịch được nữa, Service báo
                                              "Cannot change the schedule of a released song".
    Gửi thời gian ở quá khứ → Service báo "Release time must be in the future".

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
    Vì không có annotation kiểm tra nào, Controller cũng không ghi @Valid trước @RequestBody.
*/
public class UpdateReleaseRequest {
    /*
        releaseAt: lịch phát hành mới, null = phát hành ngay (xem ý nghĩa ở trên).
        private: bên ngoài đọc ghi qua getter và setter.
    */
    private LocalDateTime releaseAt;

    /*
        -----------------------------------------------------
        Getter và setter
        -----------------------------------------------------
        - Setter: Jackson gọi khi đổi JSON thành đối tượng ("releaseAt" → setReleaseAt).
        - Getter: ArtistSongController gọi request.getReleaseAt() để truyền xuống Service.
        Cách đọc từng dòng: xem RegisterRequest.
    */
    public LocalDateTime getReleaseAt() { return releaseAt; }
    public void setReleaseAt(LocalDateTime releaseAt) { this.releaseAt = releaseAt; }
}