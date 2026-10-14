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
    =====================================================================
    FileStorageService - LƯU, ĐỌC, XÓA FILE TRÊN Ổ ĐĨA SERVER
    =====================================================================

    Các chức năng:
        saveAudio(...)        → lưu file nhạc (MP3, WAV)
        saveImage(...)        → lưu ảnh (JPG, PNG, WEBP)
        getAudioDuration(...) → đọc thời lượng, đồng thời kiểm tra file có phải nhạc thật
        delete(...)           → xóa file

    Dùng chung cho:
    - Module Song: file nhạc, ảnh bìa (ArtistSongService).
    - Module User: ảnh đại diện (UserService.updateAvatar).

    =====================================================================
    CẤU TRÚC THƯ MỤC
    =====================================================================

        {uploadDir}/songs/audio/<uuid>.mp3 | .wav
        {uploadDir}/songs/covers/<uuid>.jpg | .png | .webp
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
    → Đổi đuôi file .exe thành .mp3, .pdf thành .png đều bị chặn.
*/
@Service
public class FileStorageService {
    private static final long MAX_MP3_SIZE = 20L * 1024 * 1024;
    private static final long MAX_WAV_SIZE = 100L * 1024 * 1024;
    private static final long MAX_IMAGE_SIZE = 2L * 1024 * 1024;

    /*
        Tiền tố của mọi đường dẫn lưu trong database.
    */
    private static final String URL_PREFIX = "/uploads/";
    private static final String AUDIO_FOLDER = "songs/audio";

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
        Xóa file theo đường dẫn đã lưu
        ---------------------------------------------------------------
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
    */
    private Path toPath(String url) {
        return Paths.get(uploadDir, url.substring(URL_PREFIX.length()));
    }

    /*
        ---------------------------------------------------------------
        Hàm hỗ trợ: lấy đuôi file (chữ thường)
        ---------------------------------------------------------------
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
}