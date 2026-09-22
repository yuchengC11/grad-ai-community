package com.graduate.graidaicommunity;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@MapperScan("com.graduate.graidaicommunity.mapper")
public class GradAiCommunityApplication {

    public static void main(String[] args) {
        SpringApplication.run(GradAiCommunityApplication.class, args);
    }

}
