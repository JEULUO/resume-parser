package com.example.resumematcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ResumeMatcherApplication {
    public static void main(String[] args) {
        SpringApplication.run(ResumeMatcherApplication.class, args);
    }
}
