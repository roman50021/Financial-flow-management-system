package com.example.ffms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FinancialFlowApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinancialFlowApiApplication.class, args);
    }
}
