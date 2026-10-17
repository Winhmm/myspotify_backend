package com.winhmm.myspotify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/*
    =====================================================
    ĐIỂM KHỞI ĐỘNG ỨNG DỤNG
    =====================================================
    @EnableAsync: bật tính năng chạy bất đồng bộ.
    Hàm nào gắn @Async sẽ chạy ở 1 thread riêng,
    nơi gọi không phải chờ hàm đó chạy xong.
*/
@SpringBootApplication
@EnableAsync
public class MyspotifyApplication {

	public static void main(String[] args) {
		SpringApplication.run(MyspotifyApplication.class, args);
	}

}
