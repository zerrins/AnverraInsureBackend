package com.anverraglobal.insurance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AnverraGlobalApplication {

	public static void main(String[] args) {
		SpringApplication.run(AnverraGlobalApplication.class, args);
	}

}
