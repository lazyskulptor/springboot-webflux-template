package my.lazyskulptor.commerce.config;

import org.hibernate.reactive.mutiny.Mutiny;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.Map;
import java.util.Objects;

@Configuration
public class DatabaseConfig {

    final private Environment environment;

    public DatabaseConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public Mutiny.SessionFactory sessionFactory() {
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("mysql-example",
                Map.of("jakarta.persistence.jdbc.driver", Objects.requireNonNull(environment.getProperty("jakarta.persistence.jdbc.driver")),
                        "jakarta.persistence.jdbc.url", Objects.requireNonNull(environment.getProperty("jakarta.persistence.jdbc.url")),
                        "jakarta.persistence.jdbc.user", Objects.requireNonNull(environment.getProperty("jakarta.persistence.jdbc.user")),
                        "jakarta.persistence.jdbc.password", Objects.requireNonNull(environment.getProperty("jakarta.persistence.jdbc.password"))));
        return entityManagerFactory.unwrap(Mutiny.SessionFactory.class);
    }
}