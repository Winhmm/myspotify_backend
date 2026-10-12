package com.winhmm.myspotify.service;

import com.mpatric.mp3agic.Mp3File;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.UUID;

/*
    ===================================================================
    FileStorageService - Lưu / đọc / xóa FILE trên ổ đĩa server.
    ===================================================================

    Dùng chung cho:
    - Module Song: file nhạc, ảnh bìa (ArtistSongService).
    - Module User: ảnh đại diện (UserService.updateAvatar).

    ---------- CẤU TRÚC THƯ MỤC ----------

        {uploadDir}/songs/audio/<uuid>.mp3 | .wav
        {uploadDir}/songs/covers/<uuid>.jpg | .png | .webp
        {uploadDir}/avatars/<uuid>.jpg | .png | .webp

    {uploadDir} lấy từ app.upload-dir trong application.properties
    (local và VPS cấu hình khác nhau, code không đổi).

    ---------- DATABASE LƯU GÌ, FRONTEND ĐỌC THẾ NÀO? ----------

    Database chỉ lưu ĐƯỜNG DẪN, VD: /uploads/songs/audio/3f2a9c1e.mp3
    Frontend truy cập file qua:     http://<server>/uploads/songs/audio/3f2a9c1e.mp3
    WebMvcConfig ánh xạ /uploads/** → thư mục {uploadDir} trên ổ đĩa.

    ---------- VÌ SAO ĐỔI TÊN FILE THÀNH UUID? ----------

    UUID = chuỗi ngẫu nhiên 36 ký tự, VD 3f2a9c1e-8b7d-4e21-9c3a-1f5b6d7e8a90.
    - Không trùng: 2 Artist cùng tải "track01.mp3" → 2 tên khác nhau, không ghi đè nhau.
    - Không đoán được: không ai dò được file chưa phát hành bằng cách thử tên.
    - An toàn: tên gốc có thể chứa ký tự nguy hiểm, VD "../../etc/passwd.mp3"
      (thủ đoạn ghi file ra ngoài thư mục cho phép, gọi là path traversal).
      Chỉ giữ lại đuôi file đã kiểm tra, còn tên luôn do server tự sinh.

    ---------- CÁC LỚP KIỂM TRA FILE ----------

    Ảnh  : Content-Type → dung lượng → CHỮ KÝ FILE (vài byte đầu) mới tin là ảnh thật.
    Nhạc : đuôi file → dung lượng → ĐỌC THỜI LƯỢNG bằng thư viện nhạc mới tin là nhạc thật.
    → Đổi đuôi file .exe thành .mp3, .pdf thành .png đều bị chặn.
*/
@Service
public class FileStorageService {
    /*
        Giới hạn dung lượng, tính bằng byte:
            1KB = 1024 byte, 1MB = 1024 x 1024 byte.

        Viết "20L" (chữ L = kiểu long): 100 x 1024 x 1024 vẫn vừa int,
        nhưng ghi L để chắc chắn phép nhân tính bằng long, không bao giờ tràn số.

        WAV lớn hơn MP3 vì WAV không nén: 1 phút WAV khoảng 10MB, MP3 khoảng 1MB.
    */
    private static final long MAX_MP3_SIZE = 20L * 1024 * 1024;
    private static final long MAX_WAV_SIZE = 100L * 1024 * 1024;
    private static final long MAX_IMAGE_SIZE = 2L * 1024 * 1024;

    /*
        Tiền tố của mọi đường dẫn lưu trong database.
    */
    private static final String URL_PREFIX = "/uploads/";
    private static final String AUDIO_FOLDER = "songs/audio";

    /*
        @Value("${app.upload-dir}"): Spring đọc giá trị app.upload-dir trong
        application.properties rồi gán vào biến này khi khởi động.
        VD local: uploads   (thư mục uploads trong project)
            VPS : /home/myspotify/uploads
    */
    @Value("${app.upload-dir}")
    private String uploadDir;

    /* ================= LƯU FILE NHẠC ================= */

    /*
        Lưu file nhạc (MP3 hoặc WAV).
        1. Kiểm tra file không rỗng.
        2. Kiểm tra đuôi file: chỉ nhận .mp3 / .wav.
        3. Kiểm tra dung lượng: MP3 ≤ 20MB, WAV ≤ 100MB.
        4. Lưu với tên ngẫu nhiên (UUID) → trả về đường dẫn.

        Lưu ý: method này CHƯA kiểm tra file có phải nhạc thật không.
        → Sau khi lưu, ArtistSongService gọi getAudioDuration() để kiểm tra
          (thư viện đọc nhạc cần file nằm trên ổ đĩa).

        Đây cũng là tuyến phòng thủ thứ hai về dung lượng. Tuyến thứ nhất là
        spring.servlet.multipart.max-file-size trong application.properties:
        file quá lớn bị chặn ngay khi đang tải lên (413), chưa vào tới đây.
    */
    public String saveAudio(MultipartFile file) {
        if(file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Audio file is required");
        }

        String extension = getExtension(file.getOriginalFilename());

        if(".mp3".equals(extension)) {
            if(file.getSize() > MAX_MP3_SIZE) {
                throw new IllegalArgumentException("MP3 file must not exceed 20MB");
            }
        } else if(".wav".equals(extension)) {
            if(file.getSize() > MAX_WAV_SIZE) {
                throw new IllegalArgumentException("WAV file must not exceed 100MB");
            }
        } else {
            throw new IllegalArgumentException("Only MP3 or WAV files are allowed");
        }

        return saveFile(file, AUDIO_FOLDER, extension);
    }

    /* ================= LƯU ẢNH ================= */

    /*
        Lưu ảnh (JPG / PNG / WEBP, tối đa 2MB) vào thư mục folder.
        VD: saveImage(file, "songs/covers") → /uploads/songs/covers/<uuid>.jpg
            saveImage(file, "avatars")      → /uploads/avatars/<uuid>.png

        Kiểm tra 2 lớp:
        1. Content-Type client khai báo (lọc nhanh), VD "image/jpeg".
           Đuôi file lưu lại lấy theo Content-Type, không theo tên gốc.
        2. Chữ ký file (vài byte đầu) → chặn file giả mạo,
           VD: PDF đổi đuôi thành .png, hoặc client tự khai Content-Type sai.

        "image/jpeg".equals(contentType) thay vì contentType.equals("image/jpeg"):
        → contentType có thể null (client không gửi), viết ngược thì null.equals(...)
          sẽ ném NullPointerException. Viết hằng số trước luôn an toàn.
    */
    public String saveImage(MultipartFile file, String folder) {
        if(file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }

        if(file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("Image must not exceed 2MB");
        }

        String contentType = file.getContentType();
        String extension;

        if("image/jpeg".equals(contentType)) {
            extension = ".jpg";
        } else if("image/png".equals(contentType)) {
            extension = ".png";
        } else if("image/webp".equals(contentType)) {
            extension = ".webp";
        } else {
            throw new IllegalArgumentException("Only JPG, PNG or WEBP images are allowed");
        }

        if(!isRealImage(file, extension)) {
            throw new IllegalArgumentException("Invalid or corrupted image file");
        }

        return saveFile(file, folder, extension);
    }

    /* ================= ĐỌC THỜI LƯỢNG ================= */

    /*
        Đọc thời lượng bài hát (giây) từ đường dẫn đã lưu.
        - MP3 → thư viện mp3agic (khai báo trong pom.xml).
        - WAV → thư viện có sẵn của Java (javax.sound.sampled).
        - Không đọc được (file hỏng / đổi đuôi giả mạo) → báo lỗi.

        Hàm này làm 2 việc 1 lúc:
        1. Lấy thời lượng để lưu vào Song.durationSeconds.
        2. KIỂM TRA file có phải nhạc thật không: file giả thì thư viện không đọc được.

        catch (Exception e): thư viện có thể ném nhiều loại lỗi khác nhau
        (file sai định dạng, file hỏng, không đọc được...) → gom lại thành 1 thông báo dễ hiểu.

        seconds <= 0: file đọc được nhưng thời lượng bằng 0 (file rỗng, chỉ có header)
        → cũng coi là không hợp lệ.
    */
    public int getAudioDuration(String audioUrl) {
        Path path = toPath(audioUrl);
        int seconds;

        try {
            if(audioUrl.endsWith(".mp3")) {
                seconds = getMp3Duration(path);
            } else {
                seconds = getWavDuration(path);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid or corrupted audio file");
        }

        if(seconds <= 0) {
            throw new IllegalArgumentException("Invalid or corrupted audio file");
        }

        return seconds;
    }

    /*
        MP3: Mp3File đọc phần header của từng khung âm thanh để tính tổng thời lượng.
        File không phải MP3 thật → thư viện ném lỗi (InvalidDataException...).
        (int) ép kiểu long → int: bài hát vài phút, vừa int thoải mái.
    */
    private int getMp3Duration(Path path) throws Exception {
        Mp3File mp3File = new Mp3File(path.toString());
        return (int) mp3File.getLengthInSeconds();
    }

    /*
        WAV: thời lượng = tổng số khung âm thanh / số khung mỗi giây.
            VD 9.483.000 khung / 44.100 khung mỗi giây = 215 giây.

        try (...) gọi là try-with-resources: tự đóng file sau khi đọc xong,
        kể cả khi có lỗi. Không đóng → file bị "khóa", không xóa được về sau
        (nhất là trên Windows), và rò rỉ tài nguyên.

        File WAV không xác định được số khung → getFrameLength() trả -1
        → kết quả âm → bị chặn bởi "seconds <= 0" ở hàm gọi.
    */
    private int getWavDuration(Path path) throws Exception {
        try(AudioInputStream stream = AudioSystem.getAudioInputStream(path.toFile())) {
            AudioFormat format = stream.getFormat();
            long frames = stream.getFrameLength();
            return (int) (frames / format.getFrameRate());
        }
    }

    /* ================= XÓA FILE ================= */

    /*
        Xóa file theo đường dẫn đã lưu (VD: /uploads/songs/covers/abc.jpg).

        - url = null → không làm gì (bài không có ảnh bìa). Nhờ vậy nơi gọi
          không phải kiểm tra null trước khi gọi delete.
        - Chỉ xóa file do hệ thống lưu (bắt đầu bằng /uploads/)
          → không bao giờ xóa nhầm file khác, kể cả khi database có dữ liệu lạ
            (VD ảnh đại diện là link ngoài https://...).
        - deleteIfExists: file đã không còn → không báo lỗi.
        - Xóa lỗi (file đang bị khóa...) → bỏ qua (catch rỗng), vì chức năng chính
          (đổi ảnh, xóa bài) đã thành công, không nên báo lỗi cho người dùng chỉ vì
          1 file rác chưa dọn được.
    */
    public void delete(String url) {
        if (url == null || !url.startsWith(URL_PREFIX)) {
            return;
        }

        try {
            Files.deleteIfExists(toPath(url));
        } catch (IOException ignored) {}
    }

    /* ================= HÀM HỖ TRỢ ================= */

    /*
        Lưu file vào {uploadDir}/{folder}/<uuid><extension>
        → trả về đường dẫn: /uploads/{folder}/<uuid><extension>

        - UUID.randomUUID(): tên ngẫu nhiên, không trùng.
        - Files.createDirectories(dir): tạo thư mục nếu chưa có (lần đầu chạy, VPS mới).
          Thư mục đã có → không làm gì, không lỗi.
        - Files.copy(...): ghi nội dung file tải lên xuống ổ đĩa.
          REPLACE_EXISTING: trùng tên thì ghi đè (với UUID thì gần như không bao giờ xảy ra).
        - IOException (ổ đĩa đầy, không có quyền ghi...) → đổi thành RuntimeException
          → GlobalExceptionHandler trả 500 và ghi log để debug.
    */
    private String saveFile(MultipartFile file, String folder, String extension) {
        String fileName = UUID.randomUUID() + extension;

        try {
            Path dir = Paths.get(uploadDir, folder);
            Files.createDirectories(dir);

            Files.copy(file.getInputStream(), dir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Could not save file", e);
        }

        return URL_PREFIX + folder + "/" + fileName;
    }

    /*
        Đường dẫn URL → đường dẫn thật trên ổ đĩa.
        VD: /uploads/songs/audio/abc.mp3 → {uploadDir}/songs/audio/abc.mp3

        url.substring(URL_PREFIX.length()): cắt bỏ "/uploads/" ở đầu
            "/uploads/songs/audio/abc.mp3" → "songs/audio/abc.mp3"
        Paths.get(uploadDir, ...): ghép đúng dấu phân cách của hệ điều hành
            (\ trên Windows, / trên Linux) → cùng code chạy được cả local lẫn VPS.
    */
    private Path toPath(String url) {
        return Paths.get(uploadDir, url.substring(URL_PREFIX.length()));
    }

    /*
        Lấy đuôi file (chữ thường).
        VD: "Em Cua Ngay Hom Qua.MP3" → ".mp3"
            "bai.hat.moi.wav"         → ".wav" (lấy dấu chấm CUỐI CÙNG)
            "khongcoduoi"             → ""     (→ bị báo "Only MP3 or WAV files are allowed")

        toLowerCase(): người dùng đặt tên ".MP3" hay ".Mp3" vẫn được chấp nhận.
    */
    private String getExtension(String fileName) {
        if(fileName == null || !fileName.contains(".")) {
            return "";
        }

        return fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
    }

    /*
        Kiểm tra file có THẬT SỰ là ảnh bằng CHỮ KÝ FILE (magic bytes).

        Mỗi định dạng file luôn bắt đầu bằng vài byte cố định:
            - JPG : FF D8 FF
            - PNG : 89 50 4E 47 0D 0A 1A 0A   (89 rồi chữ "PNG")
            - WEBP: "RIFF" + 4 byte kích thước + "WEBP"
        Đổi đuôi hay đổi Content-Type không làm thay đổi các byte này
        → PDF giả làm PNG vẫn bắt đầu bằng "%PDF" → bị phát hiện.

        Không tin Content-Type vì client tự gửi được giá trị bất kỳ
        (Postman, hoặc kẻ gian sửa request).

        in.readNBytes(12): chỉ đọc 12 byte đầu, đủ cho cả 3 định dạng,
        không cần đọc cả file 2MB.

        (head[0] & 0xFF) == 0xFF:
            byte trong Java có dấu (-128 → 127), nên byte 0xFF được đọc thành -1.
            "& 0xFF" đổi về số không dấu (0 → 255) để so sánh đúng với 0xFF (255).

        Đọc file lỗi → coi như không phải ảnh (trả false), không để lỗi lọt ra ngoài.
    */
    private boolean isRealImage(MultipartFile file, String extension) {
        try (InputStream in = file.getInputStream()) {
            byte[] head = in.readNBytes(12);

            if(".jpg".equals(extension)) {
                return head.length >= 3
                        && (head[0] & 0xFF) == 0xFF
                        && (head[1] & 0xFF) == 0xD8
                        && (head[2] & 0xFF) == 0xFF;
            }

            if(".png".equals(extension)) {
                byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
                return head.length >= 8 && Arrays.equals(Arrays.copyOf(head, 8), png);
            }

            if(".webp".equals(extension)) {
                return head.length >= 12
                        && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                        && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P';
            }

            return false;
        } catch (IOException e) {
            return false;
        }
    }
}