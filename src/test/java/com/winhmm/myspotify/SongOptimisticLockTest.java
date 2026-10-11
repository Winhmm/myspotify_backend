package com.winhmm.myspotify;

import com.winhmm.myspotify.entity.Song;
import com.winhmm.myspotify.repository.SongRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;

import static org.junit.jupiter.api.Assertions.assertThrows;

/*
    Tái hiện race condition "lost update":
    Admin và Artist cùng đọc 1 bài, Artist ghi trước, Admin ghi sau.

    Không có @Version → Admin ghi đè mất thay đổi của Artist (lost update).
    Có @Version      → Admin bị chặn bằng OptimisticLockingFailureException.

    Test chạy trên database local, đổi tên bài tạm thời rồi trả lại tên cũ.
*/
@SpringBootTest
class SongOptimisticLockTest {

    @Autowired
    private SongRepository songRepository;

    @Test
    void secondWriterIsRejected() {
        Long songId = songRepository.findAll().get(0).getId();
        String originalTitle = songRepository.findById(songId).orElseThrow().getTitle();

        try {
            /*
                Bước 1 + 2: hai người cùng đọc 1 bài.
                Mỗi lần findById là 1 lần đọc riêng → 2 bản sao độc lập, cùng version.
            */
            Song adminCopy = songRepository.findById(songId).orElseThrow();
            Song artistCopy = songRepository.findById(songId).orElseThrow();

            System.out.println("Admin đọc:  version = " + adminCopy.getVersion());
            System.out.println("Artist đọc: version = " + artistCopy.getVersion());

            /*
                Bước 3: Artist ghi trước → thành công, version trong database tăng lên.
            */
            artistCopy.setTitle(originalTitle + " (Artist sửa)");
            songRepository.save(artistCopy);

            long versionInDb = songRepository.findById(songId).orElseThrow().getVersion();
            System.out.println("Artist ghi xong → version trong database = " + versionInDb);

            /*
                Bước 4: Admin ghi bằng bản sao CŨ (vẫn mang version cũ) → phải bị chặn.
            */
            adminCopy.setTitle(originalTitle + " (Admin sửa)");

            assertThrows(OptimisticLockingFailureException.class, () -> songRepository.save(adminCopy));

            System.out.println("Admin ghi bằng bản sao cũ (version = " + adminCopy.getVersion()
                    + ") → BỊ CHẶN, thay đổi của Artist không bị mất");

        } finally {
            /*
                Trả lại tên bài ban đầu, không làm bẩn dữ liệu.
            */
            Song fresh = songRepository.findById(songId).orElseThrow();
            fresh.setTitle(originalTitle);
            songRepository.save(fresh);
        }
    }
}