package com.winhmm.myspotify;

import com.winhmm.myspotify.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Paths;

/*
    Test thử FileStorageService với file thật.
    MockMultipartFile: giả lập 1 file được upload từ frontend.
*/
@SpringBootTest
class FileStorageServiceTest {

    @Autowired
    private FileStorageService fileStorageService;

    @Test
    void testMp3() throws Exception {
        byte[] data = Files.readAllBytes(Paths.get("D:/spotify_testing/W_n - SSD ft. (267, Nguyenn, PAR SG) - (320 Kbps).mp3"));
        MockMultipartFile file = new MockMultipartFile("audio", "sample.mp3", "audio/mpeg", data);

        String url = fileStorageService.saveAudio(file);
        System.out.println("MP3 đã lưu tại: " + url);
        System.out.println("Thời lượng MP3: " + fileStorageService.getAudioDuration(url) + " giây");
    }

    @Test
    void testWav() throws Exception {
        byte[] data = Files.readAllBytes(Paths.get("D:/spotify_testing/Wn_-_SSD_ft._(267,_Nguyenn,_PAR_SG).wav"));
        MockMultipartFile file = new MockMultipartFile("audio", "sample.wav", "audio/wav", data);

        String url = fileStorageService.saveAudio(file);
        System.out.println("WAV đã lưu tại: " + url);
        System.out.println("Thời lượng WAV: " + fileStorageService.getAudioDuration(url) + " giây");
    }

    @Test
    void testFakeMp3() {
        // File chữ đổi đuôi thành .mp3 → phải báo lỗi
        MockMultipartFile file = new MockMultipartFile("audio", "fake.mp3", "audio/mpeg",
                "day khong phai file nhac".getBytes());

        String url = fileStorageService.saveAudio(file);
        try {
            fileStorageService.getAudioDuration(url);
            System.out.println("SAI: không phát hiện được file giả");
        } catch (IllegalArgumentException e) {
            System.out.println("ĐÚNG: phát hiện file giả → " + e.getMessage());
        } finally {
            fileStorageService.delete(url);
        }
    }
}
