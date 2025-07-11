package com.example.pfe_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling

public class PfeBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PfeBackendApplication.class, args);
    }

}
