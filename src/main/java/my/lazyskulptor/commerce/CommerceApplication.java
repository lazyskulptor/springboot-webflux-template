package my.lazyskulptor.commerce;

import my.lazyskulptor.adapter.annotation.EnableHrsaRepositories;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableHrsaRepositories(basePackages = "my.lazyskulptor.commerce.repo")
public class CommerceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CommerceApplication.class, args);
	}

}
