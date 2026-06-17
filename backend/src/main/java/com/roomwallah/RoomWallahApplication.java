package com.roomwallah;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class RoomWallahApplication {
    public static void main(String[] args) {
        SpringApplication.run(RoomWallahApplication.class, args);
    }
}
