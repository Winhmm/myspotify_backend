package com.winhmm.myspotify.dto.song;

import java.util.List;

/*
    Kết quả có phân trang trả về cho frontend.

    <T> = kiểu dữ liệu tùy ý (giống List<User>, Optional<User>), VD:
    - PageResponse<SongResponse>       → 1 trang bài hát cho người nghe.
    - PageResponse<SongManageResponse> → 1 trang bài hát cho Admin.

    JSON:
    {
        "content": [ ...các bài hát của trang này... ],
        "page": 0,             ← trang hiện tại (bắt đầu từ 0)
        "size": 20,            ← số bài mỗi trang
        "totalElements": 135,  ← tổng số bài
        "totalPages": 7        ← tổng số trang
    }
*/
public class PageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public PageResponse(List<T> content, int page, int size, long totalElements, int totalPages) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public List<T> getContent() { return content; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
}
