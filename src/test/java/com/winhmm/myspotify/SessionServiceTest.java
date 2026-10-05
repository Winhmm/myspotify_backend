package com.winhmm.myspotify;

import com.winhmm.myspotify.service.SessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SessionServiceTest {

    @Autowired
    private SessionService sessionService;

    @Test
    void testSession() {
        String email = "test@example.com";

        // 1. Đăng nhập lần 1 (giả lập Chrome)
        String chrome = sessionService.createSession(email);
        System.out.println("Phiên Chrome: " + chrome);
        System.out.println("Chrome hợp lệ? " + sessionService.isValidSession(email, chrome));    // true

        // 2. Đăng nhập lần 2 (giả lập Firefox) → ghi đè phiên Chrome
        String firefox = sessionService.createSession(email);
        System.out.println("Phiên Firefox: " + firefox);
        System.out.println("Chrome hợp lệ? " + sessionService.isValidSession(email, chrome));    // false
        System.out.println("Firefox hợp lệ? " + sessionService.isValidSession(email, firefox));  // true

        // 3. Đăng xuất → xóa phiên
        sessionService.deleteSession(email);
        System.out.println("Firefox sau đăng xuất? " + sessionService.isValidSession(email, firefox)); // false
    }
}
