package my.lazyskulptor.commerce.config;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * LiquibaseConfig
 */
@Configuration
public class LiquibaseConfig {

	@Bean
	public SpringLiquibase liquibase(LiquibaseProperties props) {
		SpringLiquibase liquibase = new SpringLiquibase();
		liquibase.setChangeLog(props.getChangeLog());
		liquibase.setDataSource(createNewDataSource(props));
        liquibase.setContexts(props.getContexts());

		return liquibase;
	}

	private DataSource createNewDataSource(LiquibaseProperties liquibaseProperties) {
		return DataSourceBuilder.create()
				.driverClassName(liquibaseProperties.getDriverClassName())
				.url(liquibaseProperties.getUrl())
				.username(liquibaseProperties.getUser())
				.password(liquibaseProperties.getPassword())
				.build();
	}
}
