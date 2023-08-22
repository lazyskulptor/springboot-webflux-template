package my.lazyskulptor.commerce.config;

import my.lazyskulptor.adapter.annotation.EnableHibernateReactiveSpringAdapter;
import org.hibernate.reactive.mutiny.Mutiny;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.sql.DataSource;
import java.util.Map;
import java.util.Objects;

@EnableConfigurationProperties({ LiquibaseProperties.class })
@EnableHibernateReactiveSpringAdapter
@Configuration
public class HibernateConfig {

    final private Environment environment;

    public HibernateConfig(Environment environment) {
        this.environment = environment;
    }


    @Bean
    public EntityManagerFactory entityManagerFactory() {
        return Persistence.createEntityManagerFactory("mysql-example",
                Map.of("jakarta.persistence.jdbc.driver", Objects.requireNonNull(environment.getProperty("spring.datasource.driver-class-name")),
                        "jakarta.persistence.jdbc.url", Objects.requireNonNull(environment.getProperty("spring.datasource.url")),
                        "jakarta.persistence.jdbc.user", Objects.requireNonNull(environment.getProperty("spring.datasource.username")),
                        "jakarta.persistence.jdbc.password", Objects.requireNonNull(environment.getProperty("spring.datasource.password"))));
    }

    @Bean
    public Mutiny.SessionFactory sessionFactory(EntityManagerFactory entityManagerFactory) {
        return entityManagerFactory.unwrap(Mutiny.SessionFactory.class);
    }
}
