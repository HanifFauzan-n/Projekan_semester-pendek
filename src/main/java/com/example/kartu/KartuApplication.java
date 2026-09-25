package com.example.kartu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class KartuApplication {

	public static void main(String[] args) {
		SpringApplication.run(KartuApplication.class, args);
	}

}
