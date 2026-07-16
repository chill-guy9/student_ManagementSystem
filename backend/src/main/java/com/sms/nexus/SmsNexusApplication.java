package com.sms.nexus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SmsNexusApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmsNexusApplication.class, args);
    }
}
