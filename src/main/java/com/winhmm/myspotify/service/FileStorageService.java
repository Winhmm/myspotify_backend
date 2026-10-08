package com.winhmm.myspotify.service;

import com.mpatric.mp3agic.Mp3File;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/*
    Lưu / đọc / xóa file của module Song (file nhạc, ảnh bìa).

    Cấu trúc thư mục:
        {uploadDir}/songs/audio/<uuid>.mp3 | .wav
        {uploadDir}/songs/covers/<uuid>.jpg | .png | .webp

    Database chỉ lưu đường dẫn, VD: /uploads/songs/audio/abc-123.mp3
    Frontend truy cập file qua: http://<server>/uploads/songs/audio/abc-123.mp3
*/
@Service
public class FileStorageService {
    /*
        20MB
    */
    private static final long MAX_MP3_SIZE = 20L * 1024 * 1024;

    /*
        100MB
    */
    private static final long MAX_WAV_SIZE = 100L * 1024 * 1024;

    /*
        2MB
    */
    private static final long MAX_IMAGE_SIZE = 2L * 1024 * 1024;

    private static final String URL_PREFIX = "/uploads/";
    private static final String AUDIO_FOLDER = "songs/audio";

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
        → Sau khi lưu, gọi getAudioDuration() để kiểm tra.
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

        return saveFile(file, folder, extension);
    }

    /* ================= ĐỌC THỜI LƯỢNG ================= */

    /*
        Đọc thời lượng bài hát (giây) từ đường dẫn đã lưu.
        - MP3 → thư viện mp3agic.
        - WAV → thư viện có sẵn của Java (javax.sound.sampled).
        - Không đọc được (file hỏng / đổi đuôi giả mạo) → báo lỗi.
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
        MP3: Mp3File đọc phần header của file để tính thời lượng.
        File không phải MP3 thật → thư viện ném lỗi.
    */
    private int getMp3Duration(Path path) throws Exception {
        Mp3File mp3File = new Mp3File(path.toString());
        return (int) mp3File.getLengthInSeconds();
    }

    /*
        WAV: thời lượng = tổng số khung âm thanh / số khung mỗi giây.
        try (...) → tự đóng file sau khi đọc xong.
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
        - Chỉ xóa file do hệ thống lưu (bắt đầu bằng /uploads/).
        - Xóa lỗi → bỏ qua, không ảnh hưởng chức năng chính.
    */
    public void delete(String url) {
        if (url == null || !url.startsWith(URL_PREFIX)) {
            return;
        }

        try {
            Files.deleteIfExists(toPath(url));
        } catch (IOException ignored) {
        }
    }

    /* ================= HÀM HỖ TRỢ ================= */

    /*
        Lưu file vào {uploadDir}/{folder}/<uuid><extension>
        → trả về đường dẫn: /uploads/{folder}/<uuid><extension>
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
    */
    private Path toPath(String url) {
        return Paths.get(uploadDir, url.substring(URL_PREFIX.length()));
    }

    /*
        Lấy đuôi file (chữ thường).
        VD: "Em Cua Ngay Hom Qua.MP3" → ".mp3"
    */
    private String getExtension(String fileName) {
        if(fileName == null || !fileName.contains(".")) {
            return "";
        }

        return fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
    }
}
