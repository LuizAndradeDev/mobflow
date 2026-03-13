package com.mobflow.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuthServiceApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(AuthServiceApplication.class);
		app.run(args);
	}

}
