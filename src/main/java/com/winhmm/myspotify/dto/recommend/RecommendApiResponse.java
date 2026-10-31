package com.winhmm.myspotify.dto.recommend;

/*
    -----------------------------------------------------
    Các dòng import
    -----------------------------------------------------
    - JsonIgnoreProperties: bảo Jackson bỏ qua các field lạ trong JSON, không báo lỗi.
    - JsonProperty        : chỉ rõ tên field trong JSON tương ứng với thuộc tính Java nào.
    - List                : kiểu danh sách của Java.

    2 annotation trên thuộc gói com.fasterxml.jackson.annotation,
    giống @JsonIgnore đang dùng trong entity User.
*/
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/*
    =====================================================================
    RECOMMENDAPIRESPONSE - JSON DỊCH VỤ AI (PYTHON) TRẢ VỀ
    =====================================================================

    DTO này chỉ dùng bên trong backend: nhận JSON từ Python, không trả cho frontend.
    Frontend luôn nhận SongResponse (thông tin bài hát đầy đủ).

    -----------------------------------------------------
    4 API của Python trả về 2 dạng JSON khác nhau
    -----------------------------------------------------
    POST /recommend, /recommend/artist, /recommend/genres trả về:
        { "song_id": 15, "recommended_ids": [7, 22, 3] }
        (riêng /recommend/genres không có song_id)

    POST /recommend/playlist trả về:
        { "recommendations": [7, 22, 3] }

    Cả 2 dạng đều chỉ chứa 1 danh sách id bài hát, chỉ khác tên field.
    → Dùng chung 1 class có cả 2 thuộc tính. JSON có field nào thì thuộc tính đó có giá trị,
      thuộc tính còn lại là null. Hàm getSongIds() bên dưới tự chọn cái có giá trị.

    -----------------------------------------------------
    Jackson đọc JSON thành đối tượng này thế nào?
    -----------------------------------------------------
    1. Gọi new RecommendApiResponse() (constructor không tham số).
    2. Gặp field "recommended_ids" trong JSON → tìm thuộc tính có @JsonProperty("recommended_ids")
       → ghi thẳng giá trị vào thuộc tính recommendedIds (kể cả khi thuộc tính là private).
    3. JSON [7, 22, 3] được đổi thành List<Long> gồm 7, 22, 3.

    Vì sao cần @JsonProperty?
    Python đặt tên kiểu snake_case (recommended_ids, chữ thường nối bằng dấu gạch dưới).
    Java đặt tên kiểu camelCase (recommendedIds). Tên khác nhau nên Jackson không tự ghép được,
    phải chỉ rõ bằng @JsonProperty.
*/
/*
    @JsonIgnoreProperties(ignoreUnknown = true):
    JSON có field mà class không khai báo (VD "song_id") → bỏ qua, không báo lỗi.
    Sau này bạn làm AI có thêm field mới vào JSON (VD "score") thì code Java vẫn chạy bình thường.
*/
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecommendApiResponse {
    /*
        Danh sách id bài hát được gợi ý, từ field "recommended_ids"
        (các API /recommend, /recommend/artist, /recommend/genres).
    */
    @JsonProperty("recommended_ids")
    private List<Long> recommendedIds;

    /*
        Danh sách id bài hát được gợi ý, từ field "recommendations"
        (API /recommend/playlist).
    */
    @JsonProperty("recommendations")
    private List<Long> recommendations;

    /*
        Constructor không tham số: Jackson cần để tạo đối tượng rỗng trước khi ghi dữ liệu vào.
        Code của mình không tự tạo đối tượng này.
    */
    public RecommendApiResponse() {}

    /*
        -----------------------------------------------------
        Lấy danh sách id, không cần biết JSON dùng tên field nào
        -----------------------------------------------------
        - Có recommended_ids → trả về danh sách đó.
        - Không có, nhưng có recommendations → trả về danh sách đó.
        - Không có cả 2 → trả về danh sách rỗng (không trả null, để code gọi hàm này
          không phải kiểm tra null, tránh lỗi NullPointerException).

        List.of(): tạo 1 danh sách rỗng, không sửa được.
    */
    public List<Long> getSongIds() {
        if (recommendedIds != null) {
            return recommendedIds;
        }
        if (recommendations != null) {
            return recommendations;
        }
        return List.of();
    }
}