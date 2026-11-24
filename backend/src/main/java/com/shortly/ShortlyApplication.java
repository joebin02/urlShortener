package com.shortly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ShortlyApplication {
    public static void main(String[] args) { SpringApplication.run(ShortlyApplication.class, args); }
}
