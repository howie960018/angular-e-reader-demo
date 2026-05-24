package com.ctbc.ebookstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EbookstoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(EbookstoreApplication.class, args);
    }
}
