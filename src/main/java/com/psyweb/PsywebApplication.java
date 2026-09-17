package com.psyweb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PsywebApplication {

	public static void main(String[] args) {
		SpringApplication.run(PsywebApplication.class, args);
	}

}
