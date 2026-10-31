package com.winhmm.myspotify.client;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    - RecommendApiResponse          : DTO nhận JSON Python trả về (danh sách id bài hát).
    - Value                         : đọc giá trị từ application.properties.
    - MediaType                     : hằng số tên loại dữ liệu, VD APPLICATION_JSON = "application/json".
    - SimpleClientHttpRequestFactory: bộ phận thật sự mở kết nối HTTP, cho phép đặt thời gian chờ.
    - Component                     : đánh dấu class để Spring tự tạo đối tượng (giống @Service).
    - RestClient                    : công cụ gửi request HTTP có sẵn trong Spring (từ Spring 6.1).
    - Duration                      : kiểu khoảng thời gian của Java, VD Duration.ofMillis(3000) = 3 giây.
    - List, Map                     : danh sách, và kiểu cặp "khóa → giá trị".
*/
import com.winhmm.myspotify.dto.recommend.RecommendApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/*
    =====================================================================
    RECOMMENDCLIENT - GỌI SANG DỊCH VỤ AI GỢI Ý BÀI HÁT (PYTHON FASTAPI)
    =====================================================================

    Từ trước tới giờ backend chỉ nhận request (từ frontend).
    Class này làm điều ngược lại: backend gửi request đi, tới 1 server khác là dịch vụ Python.

        Frontend ──► Spring Boot (8080) ──► Python FastAPI (127.0.0.1:8000)
                       │ RecommendClient gửi POST, nhận về danh sách id bài hát

    Python chạy ngay trên VPS, chỉ nghe ở 127.0.0.1 (internet không gọi thẳng vào được).
    Spring Boot cũng chạy trên VPS, nên gọi qua http://127.0.0.1:8000.

    -----------------------------------------------------
    4 API của Python (lấy từ http://127.0.0.1:8000/openapi.json)
    -----------------------------------------------------
        POST /recommend           { "song_id": 15, "top_n": 5 }            → bài tương tự 1 bài
        POST /recommend/artist    { "song_id": 15, "top_n": 5 }            → bài liên quan nghệ sĩ
        POST /recommend/genres    { "genre_ids": [1, 24], "top_n": 5 }     → bài theo thể loại
        POST /recommend/playlist  { "song_ids": [7, 15], "top_n": 5 }      → gợi ý thêm cho playlist
    Cả 4 API chỉ trả về danh sách id bài hát (xem RecommendApiResponse).

    -----------------------------------------------------
    Class này chỉ lo việc gửi và nhận
    -----------------------------------------------------
    - Không lọc bài chưa phát hành, không đổi id thành thông tin bài hát.
      Những việc đó do RecommendationService làm.
    - Python lỗi, tắt, hoặc chậm quá thời gian chờ → RestClient ném RestClientException.
      Class này không bắt lỗi, để RecommendationService bắt và chuyển sang phương án dự phòng.
    - Đây là file duy nhất biết đường dẫn và tên field của Python.
      Sau này bạn làm AI đổi API thì chỉ cần sửa file này (và RecommendApiResponse).

    -----------------------------------------------------
    @Component
    -----------------------------------------------------
    Giống @Service: Spring tự tạo 1 đối tượng RecommendClient khi khởi động,
    rồi truyền vào class nào cần (RecommendationService).
    Dùng @Component vì class này không chứa nghiệp vụ, chỉ là công cụ gọi HTTP.
*/
@Component
public class RecommendClient {
    /*
        Đối tượng RestClient đã cấu hình sẵn địa chỉ Python và thời gian chờ.
        Tạo 1 lần trong constructor, dùng lại cho mọi request (không tạo mới mỗi lần gọi).
    */
    private final RestClient restClient;

    /*
        -----------------------------------------------------
        Constructor: đọc cấu hình và tạo RestClient
        -----------------------------------------------------
        @Value("${app.recommend.base-url:http://127.0.0.1:8000}"):
        - Đọc dòng app.recommend.base-url trong application.properties.
        - Phần sau dấu hai chấm là giá trị mặc định, dùng khi file cấu hình không có dòng đó.
          Nhờ vậy quên khai báo trong application-prod.properties trên VPS
          thì ứng dụng vẫn khởi động được (không bị lỗi "Could not resolve placeholder").

        @Value("${app.recommend.timeout-ms:3000}"):
        - Thời gian chờ tối đa, đơn vị mili giây. Mặc định 3000 = 3 giây.

        Thời gian chờ quan trọng thế nào?
        Không đặt thời gian chờ thì Python bị treo → request của người dùng cũng treo theo, rất lâu.
        Đặt 3 giây: quá 3 giây chưa trả lời → báo lỗi ngay → dùng phương án dự phòng,
        người dùng vẫn thấy danh sách gợi ý.
    */
    public RecommendClient(@Value("${app.recommend.base-url:http://127.0.0.1:8000}") String baseUrl,
                           @Value("${app.recommend.timeout-ms:3000}") long timeoutMs) {
        /*
            SimpleClientHttpRequestFactory: bộ phận mở kết nối HTTP (dùng thư viện có sẵn của Java).
            - setConnectTimeout: chờ tối đa bao lâu để kết nối được tới Python.
              Python đang tắt → báo lỗi ngay (Connection refused), không phải chờ.
            - setReadTimeout: đã kết nối, chờ tối đa bao lâu để Python trả kết quả.
        */
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));

        /*
            RestClient.builder(): tạo RestClient theo kiểu nối hàm (Builder pattern).
            - baseUrl(...)       : địa chỉ gốc. Mỗi request chỉ cần ghi phần đường dẫn phía sau,
                                   VD "/recommend" → http://127.0.0.1:8000/recommend.
            - requestFactory(...): dùng bộ phận kết nối có thời gian chờ ở trên.
            - build()            : tạo ra đối tượng RestClient.
        */
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    /*
        -----------------------------------------------------
        Bài hát tương tự 1 bài → POST /recommend
        -----------------------------------------------------
        Map.of("song_id", songId, "top_n", topN) tạo Map 2 cặp khóa giá trị.
        Jackson đổi Map thành JSON: { "song_id": 15, "top_n": 5 }
        Dùng Map thay vì tạo DTO riêng, vì body rất ngắn và tên khóa trùng đúng tên field của Python.
    */
    public List<Long> similarSongs(Long songId, int topN) {
        return post("/recommend", Map.of("song_id", songId, "top_n", topN));
    }

    /*
        -----------------------------------------------------
        Bài liên quan tới nghệ sĩ của 1 bài → POST /recommend/artist
        -----------------------------------------------------
        Body giống hệt /recommend: { "song_id": 15, "top_n": 5 }
    */
    public List<Long> artistSongs(Long songId, int topN) {
        return post("/recommend/artist", Map.of("song_id", songId, "top_n", topN));
    }

    /*
        -----------------------------------------------------
        Bài theo danh sách thể loại → POST /recommend/genres
        -----------------------------------------------------
        Body: { "genre_ids": [1, 24], "top_n": 10 }
        List<Long> được Jackson đổi thành mảng JSON [1, 24].
    */
    public List<Long> genreSongs(List<Long> genreIds, int topN) {
        return post("/recommend/genres", Map.of("genre_ids", genreIds, "top_n", topN));
    }

    /*
        -----------------------------------------------------
        Gợi ý thêm bài cho 1 playlist → POST /recommend/playlist
        -----------------------------------------------------
        Body: { "song_ids": [7, 15, 22], "top_n": 5 }
        Hiện chưa dùng. Để sẵn cho tính năng Playlist (đang làm trên nhánh playlist).
    */
    public List<Long> playlistSongs(List<Long> songIds, int topN) {
        return post("/recommend/playlist", Map.of("song_ids", songIds, "top_n", topN));
    }

    /*
        -----------------------------------------------------
        Hàm chung: gửi POST tới Python, nhận về danh sách id
        -----------------------------------------------------
        4 hàm public ở trên chỉ khác đường dẫn và body, nên gom phần gửi nhận vào đây.

        Đọc chuỗi lệnh restClient từ trên xuống:
        - post()                               : tạo request phương thức POST.
        - uri(path)                            : đường dẫn, ghép sau baseUrl.
        - contentType(MediaType.APPLICATION_JSON): gửi header Content-Type: application/json,
                                                 báo cho Python biết body là JSON.
        - body(body)                           : nội dung gửi đi. Jackson đổi Map thành chuỗi JSON.
        - retrieve()                           : gửi request đi và nhận response.
                                                 Python trả mã lỗi (4xx, 5xx) → ném RestClientException.
        - body(RecommendApiResponse.class)     : Jackson đổi JSON nhận về thành đối tượng
                                                 RecommendApiResponse.

        Có thể ném RestClientException khi:
        - Python đang tắt (không kết nối được).
        - Quá thời gian chờ.
        - Python trả mã lỗi, VD 422 (body sai định dạng) hoặc 500 (lỗi bên trong Python).
        - JSON trả về không đọc được.

        Map<String, Object>: khóa là chuỗi, giá trị là Object (kiểu chung nhất của Java),
        vì giá trị có thể là số (song_id, top_n) hoặc danh sách (genre_ids, song_ids).
    */
    private List<Long> post(String path, Map<String, Object> body) {
        RecommendApiResponse response = restClient.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(RecommendApiResponse.class);

        /*
            Python trả về body rỗng → response là null → coi như không có gợi ý nào.
        */
        if (response == null) {
            return List.of();
        }
        return response.getSongIds();
    }
}