package com.example.kartu;

import org.springframework.boot.SpringApplication; // Ganti import ke CategoryRepository
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KartuApplication {

	public static void main(String[] args) {
		SpringApplication.run(KartuApplication.class, args);
	}

	
}