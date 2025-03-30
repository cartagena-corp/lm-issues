package com.cartagenacorp.lm_issues;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(basePackages = {
		"com.cartagenacorp.lm_issues",
		"com.cartagenacorp.lm_issues.mapper"
})

@SpringBootApplication
public class LmIssuesApplication {

	public static void main(String[] args) {
		SpringApplication.run(LmIssuesApplication.class, args);
	}

}
