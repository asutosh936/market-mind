package com.marketmind;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MarketMindApplication {

    private static final Logger logger = LoggerFactory.getLogger(MarketMindApplication.class);

    public static void main(String[] args) {
        logger.info("Starting Market Mind application");
        SpringApplication.run(MarketMindApplication.class, args);
        logger.info("Market Mind application started successfully");
    }
}