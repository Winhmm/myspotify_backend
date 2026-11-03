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
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.UUID;

/*
    =====================================================================
    FILESTORAGESERVICE - LƯU, ĐỌC, XÓA FILE TRÊN Ổ ĐĨA SERVER
    =====================================================================

    Các chức năng:
        saveAudio(...)          → lưu file nhạc (MP3, WAV)
        saveImage(...)          → lưu ảnh (JPG, PNG, WEBP)
        saveVideo(...)          → lưu video MP4 (video lặp Canvas)
        getAudioDuration(...)   → đọc thời lượng nhạc, đồng thời kiểm tra file có phải nhạc thật
        getVideoDurationMs(...) → đọc thời lượng video MP4, đồng thời kiểm tra có phải video thật
        delete(...)             → xóa file

    Dùng chung cho:
    - Module Song: file nhạc, ảnh bìa, video lặp Canvas (ArtistSongService).
    - Module User: ảnh đại diện (UserService.updateAvatar).

    =====================================================================
    CẤU TRÚC THƯ MỤC
    =====================================================================

        {uploadDir}/songs/audio/<uuid>.mp3 | .wav
        {uploadDir}/songs/covers/<uuid>.jpg | .png | .webp
        {uploadDir}/songs/canvas/<uuid>.mp4
        {uploadDir}/avatars/<uuid>.jpg | .png | .webp

    {uploadDir} lấy từ app.upload-dir trong application.properties
    (local và VPS cấu hình khác nhau, code không đổi).

    =====================================================================
    DATABASE LƯU GÌ, FRONTEND ĐỌC THẾ NÀO?
    =====================================================================

    Database chỉ lưu đường dẫn, VD: /uploads/songs/audio/3f2a9c1e.mp3
    Frontend truy cập file qua:     http://<server>/uploads/songs/audio/3f2a9c1e.mp3
    WebMvcConfig ánh xạ /uploads/** → thư mục {uploadDir} trên ổ đĩa.

    =====================================================================
    VÌ SAO ĐỔI TÊN FILE THÀNH UUID?
    =====================================================================

    UUID là chuỗi ngẫu nhiên 36 ký tự, VD 3f2a9c1e-8b7d-4e21-9c3a-1f5b6d7e8a90.
    - Không trùng: 2 Artist cùng tải "track01.mp3" → 2 tên khác nhau, không ghi đè nhau.
    - Không đoán được: không ai dò được file chưa phát hành bằng cách thử tên.
    - An toàn: tên gốc có thể chứa ký tự nguy hiểm, VD "../../etc/passwd.mp3"
      (thủ đoạn ghi file ra ngoài thư mục cho phép, gọi là path traversal).
      Chỉ giữ lại đuôi file đã kiểm tra, còn tên luôn do server tự sinh.

    =====================================================================
    CÁC LỚP KIỂM TRA FILE
    =====================================================================

    Ảnh : Content-Type → dung lượng → chữ ký file (vài byte đầu) mới tin là ảnh thật.
    Nhạc: đuôi file → dung lượng → đọc thời lượng bằng thư viện nhạc mới tin là nhạc thật.
    Video: dung lượng → Content-Type → chữ ký file "ftyp" → đọc thời lượng trong file MP4.
    → Đổi đuôi file .exe thành .mp3, .pdf thành .png, .txt thành .mp4 đều bị chặn.
*/
@Service
public class FileStorageService {
    /*
        Giới hạn dung lượng, tính bằng byte:
            1 KB = 1024 byte, 1 MB = 1024 KB → 20 MB = 20 * 1024 * 1024 byte.
        Chữ L sau số 20 (20L): tính bằng kiểu long ngay từ đầu.
        Nếu tính bằng int, số lớn sẽ bị tràn (int chỉ tới khoảng 2,1 tỷ),
        VD 3000 * 1024 * 1024 = 3,1 tỷ → ra số âm sai bét. Với long thì không lo.
    */
    private static final long MAX_MP3_SIZE = 20L * 1024 * 1024;
    private static final long MAX_WAV_SIZE = 100L * 1024 * 1024;
    private static final long MAX_IMAGE_SIZE = 2L * 1024 * 1024;
    private static final long MAX_VIDEO_SIZE = 50L * 1024 * 1024;

    /*
        Tiền tố của mọi đường dẫn lưu trong database.
    */
    private static final String URL_PREFIX = "/uploads/";
    private static final String AUDIO_FOLDER = "songs/audio";

    /*
        Thư mục gốc chứa file upload, đọc từ application.properties (theo tài liệu song.md):
            Local: app.upload-dir=uploads                  (thư mục uploads ngay trong project)
            VPS  : app.upload-dir=/home/myspotify/uploads
    */
    @Value("${app.upload-dir}")
    private String uploadDir;

    /*
        ---------------------------------------------------------------
        Lưu file nhạc (MP3 hoặc WAV)
        ---------------------------------------------------------------
        1. Kiểm tra file không rỗng.
        2. Kiểm tra đuôi file: chỉ nhận .mp3 hoặc .wav.
        3. Kiểm tra dung lượng: MP3 ≤ 20MB, WAV ≤ 100MB.
        4. Lưu với tên ngẫu nhiên (UUID) → trả về đường dẫn.

        Method này chưa kiểm tra file có phải nhạc thật không.
        → Sau khi lưu, ArtistSongService gọi getAudioDuration() để kiểm tra
          (thư viện đọc nhạc cần file nằm trên ổ đĩa).
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

    /*
        ---------------------------------------------------------------
        Lưu ảnh (JPG, PNG, WEBP, tối đa 2MB)
        ---------------------------------------------------------------
        VD: saveImage(file, "songs/covers") → /uploads/songs/covers/<uuid>.jpg
            saveImage(file, "avatars")      → /uploads/avatars/<uuid>.png

        Kiểm tra 2 lớp:
        1. Content-Type client khai báo (lọc nhanh), VD "image/jpeg".
           Đuôi file lưu lại lấy theo Content-Type, không theo tên gốc.
        2. Chữ ký file (vài byte đầu) → chặn file giả mạo,
           VD PDF đổi đuôi thành .png, hoặc client tự khai Content-Type sai.
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

    /*
        ---------------------------------------------------------------
        Đọc thời lượng bài hát (giây) từ đường dẫn đã lưu
        ---------------------------------------------------------------
        - MP3 → thư viện mp3agic (khai báo trong pom.xml).
        - WAV → thư viện có sẵn của Java (javax.sound.sampled).
        - Không đọc được (file hỏng hoặc đổi đuôi giả mạo) → báo lỗi.
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
        } catch(Exception e) {
            throw new IllegalArgumentException("Invalid or corrupted audio file");
        }

        if(seconds <= 0) {
            throw new IllegalArgumentException("Invalid or corrupted audio file");
        }

        return seconds;
    }

    /*
        2 hàm đọc thời lượng:
        - MP3: Mp3File đọc file, đếm các khung âm thanh (frame) rồi tính ra số giây.
          File không phải MP3 thật → thư viện ném lỗi.
          (int): ép kiểu long (getLengthInSeconds trả long) sang int.
        - WAV: số khung (frame) chia cho số khung mỗi giây (frame rate).
          VD 9.555.000 khung / 44.100 khung mỗi giây = 216,6 → (int) bỏ phần lẻ → 216 giây.
          try (...) { } (try-with-resources): tự đóng file sau khi đọc xong, kể cả khi có lỗi.
        throws Exception: khai báo hàm có thể ném lỗi,
        để getAudioDuration bắt chung trong 1 khối catch rồi đổi thành message dễ hiểu.
    */
    private int getMp3Duration(Path path) throws Exception {
        Mp3File mp3File = new Mp3File(path.toString());
        return (int) mp3File.getLengthInSeconds();
    }

    private int getWavDuration(Path path) throws Exception {
        try(AudioInputStream stream = AudioSystem.getAudioInputStream(path.toFile())) {
            AudioFormat format = stream.getFormat();
            long frames = stream.getFrameLength();
            return (int) (frames / format.getFrameRate());
        }
    }

    /*
        ---------------------------------------------------------------
        Lưu video MP4 (tối đa 50MB)
        ---------------------------------------------------------------
        VD: saveVideo(file, "songs/canvas") → /uploads/songs/canvas/<uuid>.mp4

        Kiểm tra 3 lớp, theo thứ tự từ nhanh đến chậm:
        1. Dung lượng tối đa 50MB.
        2. Content-Type client khai báo phải là "video/mp4" (lọc nhanh).
           Postman và trình duyệt tự đặt Content-Type theo đuôi file:
           .mp4 → "video/mp4", .mov → "video/quicktime", .jpg → "image/jpeg"...
        3. Chữ ký file: byte thứ 4 đến thứ 7 phải là chữ "ftyp"
           → chặn file giả mạo, VD file .txt hoặc .exe đổi đuôi thành .mp4
             (đổi đuôi thì Content-Type vẫn là "video/mp4", nhưng chữ ký file thì không đổi được).

        Tên file lưu luôn là <uuid>.mp4, không dùng tên gốc (lý do: xem phần UUID ở đầu file).

        Method này chưa kiểm tra thời lượng.
        → Sau khi lưu, ArtistSongService gọi getVideoDurationMs() để kiểm tra,
          vì cần file nằm trên ổ đĩa mới đọc được (giống cách làm với file nhạc).
    */
    public String saveVideo(MultipartFile file, String folder) {
        if(file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Video file is required");
        }

        if(file.getSize() > MAX_VIDEO_SIZE) {
            throw new IllegalArgumentException("Video must not exceed 50MB");
        }

        if(!"video/mp4".equals(file.getContentType())) {
            throw new IllegalArgumentException("Only MP4 videos are allowed");
        }

        if(!isRealMp4(file)) {
            throw new IllegalArgumentException("Invalid or corrupted video file");
        }

        return saveFile(file, folder, ".mp4");
    }

    /*
        ---------------------------------------------------------------
        Đọc thời lượng video MP4 từ đường dẫn đã lưu (đơn vị mili giây)
        ---------------------------------------------------------------

        -----------------------------------------------------
        File MP4 được tổ chức thế nào?
        -----------------------------------------------------
        File MP4 gồm nhiều "hộp" (box) nối tiếp nhau, hộp lớn chứa hộp nhỏ bên trong:
            [ftyp]  loại file (chữ ký "ftyp" mà isRealMp4 kiểm tra)
            [moov]  thông tin về video
                [mvhd]  thông tin chung, có timescale và duration   ← cần đọc hộp này
                [trak]  ...
            [mdat]  dữ liệu hình và tiếng thật
        Mỗi hộp bắt đầu bằng 8 byte: 4 byte kích thước (của cả hộp) + 4 byte tên hộp (VD "moov").

        Các bước:
        1. Tìm hộp moov trong cả file.
        2. Tìm hộp mvhd bên trong moov.
        3. Đọc mvhd:
               1 byte version, 3 byte flags (bỏ qua)
               version 0: 4 byte thời điểm tạo + 4 byte thời điểm sửa (bỏ qua 8 byte),
                          rồi 4 byte timescale, 4 byte duration
               version 1: 2 trường thời gian dài 8 byte (bỏ qua 16 byte),
                          rồi 4 byte timescale, 8 byte duration
        4. Thời lượng (ms) = duration * 1000 / timescale.
           timescale = số "nhịp" trong 1 giây. VD timescale = 1000, duration = 8500 → 8500 ms = 8,5 giây.

        Vì sao tự đọc, không dùng thư viện? Chỉ cần đúng 2 con số, vài chục dòng là đủ,
        không phải thêm thư viện xử lý video nặng vào dự án.

        -----------------------------------------------------
        Cú pháp đọc byte
        -----------------------------------------------------
        - new RandomAccessFile(file, "r"): mở file để đọc ("r" = read), nhảy được tới vị trí bất kỳ.
          Đặt trong try (...) → tự đóng file khi xong.
        - seek(vịTrí): nhảy tới byte thứ vịTrí (đếm từ 0).
        - readUnsignedByte(): đọc 1 byte, trả số từ 0 đến 255.
        - skipBytes(n): bỏ qua n byte.
        - readInt(): đọc 4 byte thành int. readLong(): đọc 8 byte thành long.
        - readInt() & 0xFFFFFFFFL: int của Java có dấu (lớn hơn khoảng 2,1 tỷ thì thành số âm).
          "& 0xFFFFFFFFL" đổi sang long và giữ nguyên 32 bit → luôn ra số dương (0 đến khoảng 4,2 tỷ).
        - Lỗi đọc file (IOException) → đổi thành IllegalArgumentException → 400 "Invalid or corrupted video file".
    */
    public long getVideoDurationMs(String videoUrl) {
        try(RandomAccessFile file = new RandomAccessFile(toPath(videoUrl).toFile(), "r")) {
            long[] moov = findBox(file, 0, file.length(), "moov");
            if(moov == null) {
                throw new IllegalArgumentException("Invalid or corrupted video file");
            }

            long[] mvhd = findBox(file, moov[0], moov[1], "mvhd");
            if(mvhd == null) {
                throw new IllegalArgumentException("Invalid or corrupted video file");
            }

            file.seek(mvhd[0]);
            int version = file.readUnsignedByte();
            file.skipBytes(3);

            long timescale;
            long duration;

            if(version == 1) {
                file.skipBytes(16);
                timescale = file.readInt() & 0xFFFFFFFFL;
                duration = file.readLong();
            } else {
                file.skipBytes(8);
                timescale = file.readInt() & 0xFFFFFFFFL;
                duration = file.readInt() & 0xFFFFFFFFL;
            }

            if(timescale == 0 || duration <= 0) {
                throw new IllegalArgumentException("Invalid or corrupted video file");
            }

            return duration * 1000 / timescale;
        } catch(IOException e) {
            throw new IllegalArgumentException("Invalid or corrupted video file");
        }
    }

    /*
        ---------------------------------------------------------------
        Hàm hỗ trợ: tìm 1 hộp theo tên trong đoạn [start, end) của file MP4
        ---------------------------------------------------------------
        [start, end): từ vị trí start (tính cả) tới vị trí end (không tính).
        Trả về mảng 2 số { vị trí bắt đầu nội dung hộp, vị trí kết thúc hộp }. Không tìm thấy → null.

        Duyệt lần lượt từng hộp:
        - Đọc 4 byte kích thước + 4 byte tên.
          new String(typeBytes, US_ASCII): đổi 4 byte thành chuỗi, VD 4 byte 6D 6F 6F 76 → "moov".
        - size == 1: hộp quá lớn, kích thước thật nằm ở 8 byte tiếp theo (phần đầu hộp dài 16 byte).
        - size == 0: hộp kéo dài tới hết đoạn đang tìm.
        - Kích thước vô lý (nhỏ hơn phần đầu hộp, hoặc vượt ra ngoài đoạn) → file hỏng → null.
        - Đúng tên cần tìm → trả vị trí. Không đúng → nhảy qua cả hộp (position += size), xét hộp kế tiếp.
    */
    private long[] findBox(RandomAccessFile file, long start, long end, String wantedType) throws IOException {
        long position = start;

        while(position + 8 <= end) {
            file.seek(position);

            long size = file.readInt() & 0xFFFFFFFFL;
            byte[] typeBytes = new byte[4];
            file.readFully(typeBytes);
            String type = new String(typeBytes, StandardCharsets.US_ASCII);

            long headerSize = 8;
            if(size == 1) {
                size = file.readLong();
                headerSize = 16;
            } else if(size == 0) {
                size = end - position;
            }

            if(size < headerSize || position + size > end) {
                return null;
            }

            if(type.equals(wantedType)) {
                return new long[] { position + headerSize, position + size };
            }

            position += size;
        }

        return null;
    }

    /*
        ---------------------------------------------------------------
        Xóa file theo đường dẫn đã lưu
        ---------------------------------------------------------------
        - url null (VD bài không có ảnh bìa) → không làm gì.
        - Chỉ xóa đường dẫn bắt đầu bằng /uploads/ → không bao giờ lỡ tay xóa file ngoài thư mục upload.
        - Files.deleteIfExists: file không tồn tại cũng không báo lỗi.
        - Xóa lỗi (IOException) → bỏ qua (catch rỗng): sót 1 file rác trên ổ đĩa
          không ảnh hưởng người dùng, không đáng làm hỏng cả request.
    */
    public void delete(String url) {
        if(url == null || !url.startsWith(URL_PREFIX)) {
            return;
        }

        try {
            Files.deleteIfExists(toPath(url));
        } catch(IOException ignored) {}
    }

    /*
        ---------------------------------------------------------------
        Hàm hỗ trợ: lưu file xuống ổ đĩa
        ---------------------------------------------------------------
        1. Tên file = UUID ngẫu nhiên + đuôi, VD "3f2a9c1e-8b7d-4e21-9c3a-1f5b6d7e8a90.mp3".
        2. Paths.get(uploadDir, folder): ghép đường dẫn thư mục, VD uploads/songs/audio.
           Files.createDirectories: tạo thư mục (cả các thư mục cha) nếu chưa có, có rồi thì thôi.
        3. Files.copy(...): chép dữ liệu file upload xuống ổ đĩa tại thư mục đó.
           dir.resolve(fileName): ghép thư mục với tên file → đường dẫn đầy đủ.
           REPLACE_EXISTING: trùng tên thì ghi đè (với UUID thì gần như không bao giờ trùng).
        4. Lỗi ổ đĩa (đầy, không có quyền ghi...) → RuntimeException → 500 (lỗi phía server).
        5. Trả đường dẫn dạng URL để lưu database: /uploads/songs/audio/<uuid>.mp3

        Ghi chú: file.getInputStream() mở 1 luồng đọc, nhưng Files.copy không tự đóng luồng đó.
        Vẫn chạy đúng (Java tự dọn khi luồng không còn được dùng), nhưng chuẩn hơn là bọc trong try (...) { }
        giống isRealImage bên dưới.
    */
    private String saveFile(MultipartFile file, String folder, String extension) {
        String fileName = UUID.randomUUID() + extension;

        try {
            Path dir = Paths.get(uploadDir, folder);
            Files.createDirectories(dir);

            Files.copy(file.getInputStream(), dir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        } catch(IOException e) {
            throw new RuntimeException("Could not save file", e);
        }

        return URL_PREFIX + folder + "/" + fileName;
    }

    /*
        ---------------------------------------------------------------
        Hàm hỗ trợ: đổi đường dẫn URL thành đường dẫn thật trên ổ đĩa
        ---------------------------------------------------------------
        VD url = "/uploads/songs/audio/x.mp3":
            url.substring(URL_PREFIX.length()) → bỏ 9 ký tự "/uploads/" ở đầu → "songs/audio/x.mp3"
            Paths.get(uploadDir, ...)          → "/home/myspotify/uploads/songs/audio/x.mp3" (trên VPS)
    */
    private Path toPath(String url) {
        return Paths.get(uploadDir, url.substring(URL_PREFIX.length()));
    }

    /*
        ---------------------------------------------------------------
        Hàm hỗ trợ: lấy đuôi file (chữ thường)
        ---------------------------------------------------------------
        lastIndexOf("."): vị trí dấu chấm CUỐI CÙNG trong tên.
            "my.song.MP3" → substring từ dấu chấm cuối → ".MP3" → toLowerCase() → ".mp3"
        Không có tên hoặc không có dấu chấm → "" → saveAudio báo "Only MP3 or WAV files are allowed".
    */
    private String getExtension(String fileName) {
        if(fileName == null || !fileName.contains(".")) {
            return "";
        }

        return fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
    }

    /*
        ---------------------------------------------------------------
        Hàm hỗ trợ: kiểm tra file có thật sự là ảnh (chữ ký file)
        ---------------------------------------------------------------
        Mỗi loại file có vài byte đầu cố định (chữ ký, magic number), đổi đuôi file không đổi được:
            JPG : FF D8 FF
            PNG : 89 50 4E 47 0D 0A 1A 0A   (50 4E 47 là chữ "PNG")
            WEBP: "RIFF" ở byte 0 đến 3, "WEBP" ở byte 8 đến 11

        - in.readNBytes(12): đọc tối đa 12 byte đầu (file ngắn hơn thì đọc được ít hơn,
          nên mới kiểm tra head.length trước khi lấy từng byte).
        - (head[0] & 0xFF) == 0xFF: byte của Java có dấu (từ -128 đến 127), byte FF đọc ra là -1.
          "& 0xFF" đổi về số 0 đến 255 để so được với 0xFF (255).
        - Arrays.copyOf(head, 8): lấy 8 byte đầu. Arrays.equals: so 2 mảng từng phần tử.
        - head[0] == 'R': so byte với mã ASCII của chữ 'R' (82).
        Lỗi đọc → coi như không phải ảnh (false).
    */
    private boolean isRealImage(MultipartFile file, String extension) {
        try(InputStream in = file.getInputStream()) {
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
        } catch(IOException e) {
            return false;
        }
    }

    /*
        ---------------------------------------------------------------
        Hàm hỗ trợ: kiểm tra chữ ký file MP4
        ---------------------------------------------------------------
        File MP4 luôn mở đầu bằng hộp ftyp: 4 byte kích thước, rồi 4 byte chữ "ftyp" (byte thứ 4 đến 7).
        Chỉ cần đọc 8 byte đầu, so 4 byte cuối với 'f', 't', 'y', 'p'.
    */
    private boolean isRealMp4(MultipartFile file) {
        try(InputStream in = file.getInputStream()) {
            byte[] head = in.readNBytes(8);

            return head.length >= 8
                    && head[4] == 'f' && head[5] == 't' && head[6] == 'y' && head[7] == 'p';
        } catch(IOException e) {
            return false;
        }
    }
}