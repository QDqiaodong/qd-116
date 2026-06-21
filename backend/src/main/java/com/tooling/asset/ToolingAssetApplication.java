package com.tooling.asset;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@RequiredArgsConstructor
public class ToolingAssetApplication {

    private final ScrapReasonService scrapReasonService;

    public static void main(String[] args) {
        SpringApplication.run(ToolingAssetApplication.class, args);
    }

    @Bean
    public CommandLineRunner initScrapReasons() {
        return args -> scrapReasonService.initDefaultReasons();
    }
}
