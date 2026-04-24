package com.sporty.external;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ExternalRestDummyApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExternalRestDummyApiApplication.class, args);
    }
}
