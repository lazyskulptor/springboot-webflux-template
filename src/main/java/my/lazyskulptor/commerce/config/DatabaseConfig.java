package my.lazyskulptor.commerce.config;

import my.lazyskulptor.adapter.EnableHibernateReactiveSpringAdapter;
import org.hibernate.reactive.mutiny.Mutiny;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.sql.DataSource;
import java.util.Map;
import java.util.Objects;

@Configuration
@EnableHibernateReactiveSpringAdapter
public class DatabaseConfig {

    final private Environment environment;

    public DatabaseConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public DataSource getDataSource(DataSourceProperties props) {
        return DataSourceBuilder.create()
                .driverClassName(props.getDriverClassName())
                .url(props.getUrl())
                .username(props.getUsername())
                .password(props.getPassword())
                .build();
    }

    @Bean
    public Mutiny.SessionFactory sessionFactory() {
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("mysql-example",
                Map.of("jakarta.persistence.jdbc.driver", Objects.requireNonNull(environment.getProperty("spring.datasource.driver-class-name")),
                        "jakarta.persistence.jdbc.url", Objects.requireNonNull(environment.getProperty("spring.datasource.url")),
                        "jakarta.persistence.jdbc.user", Objects.requireNonNull(environment.getProperty("spring.datasource.username")),
                        "jakarta.persistence.jdbc.password", Objects.requireNonNull(environment.getProperty("spring.datasource.password"))));
        return entityManagerFactory.unwrap(Mutiny.SessionFactory.class);
    }
}