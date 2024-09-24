package com.example.tictactoe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TictactoeApplication {

	public static void main(String[] args) {
		SpringApplication.run(TictactoeApplication.class, args);
	}

}
