package com.paypeek.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableMongoAuditing
public class PayPeekBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(PayPeekBackendApplication.class, args);
	}

}
