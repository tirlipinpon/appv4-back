package com.appv4;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class Appv4BackApplication {

	public static void main(String[] args) {
		SpringApplication.run(Appv4BackApplication.class, args);
	}

}
