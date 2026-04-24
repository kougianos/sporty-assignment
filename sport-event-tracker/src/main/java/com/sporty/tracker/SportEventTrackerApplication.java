package com.sporty.tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableRetry
public class SportEventTrackerApplication {

    static void main(String[] args) {
        SpringApplication.run(SportEventTrackerApplication.class, args);
    }
}
