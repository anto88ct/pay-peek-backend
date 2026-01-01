package com.paypeek.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableMongoAuditing
@EnableScheduling
public class PayPeekBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(PayPeekBackendApplication.class, args);
	}

}
