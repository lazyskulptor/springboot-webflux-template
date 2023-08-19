package my.lazyskulptor.commerce;

import my.lazyskulptor.adapter.annotation.EnableAdapterRepositories;
import my.lazyskulptor.adapter.autoconfigure.EnableHibernateReactiveSpringAdapter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableAdapterRepositories(basePackages = "my.lazyskulptor.commerce.repo")
public class CommerceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CommerceApplication.class, args);
	}

}
