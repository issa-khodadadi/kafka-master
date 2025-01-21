package com.issa.kafka.master;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.issa.kafka.master")
public class KafkaMasterApplication {
	public static void main(String[] args) {
		SpringApplication.run(KafkaMasterApplication.class, args);
	}
}
