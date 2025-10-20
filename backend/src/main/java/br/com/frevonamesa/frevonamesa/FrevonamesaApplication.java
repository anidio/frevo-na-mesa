package br.com.frevonamesa.frevonamesa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FrevonamesaApplication {

	public static void main(String[] args) {
		SpringApplication.run(FrevonamesaApplication.class, args);
	}

}
