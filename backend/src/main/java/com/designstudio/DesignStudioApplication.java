package com.designstudio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {
		"com.designstudio",
		"com.designstudio.api",
		"com.designstudio.application",
		"com.designstudio.application.dto",
		"com.designstudio.application.service",
		"com.designstudio.domain",
		"com.designstudio.domain.entity",
		"com.designstudio.domain.repository",
		"com.designstudio.infrastructure",
		"com.designstudio.infrastructure.config",
		"com.designstudio.infrastructure.filter",
		"com.designstudio.infrastructure.security"
})
public class DesignStudioApplication {
	public static void main(String[] args) {
		SpringApplication.run(DesignStudioApplication.class, args);
	}
}