package my.lazyskulptor.commerce.config;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * LiquibaseConfig
 */
@Configuration
public class LiquibaseConfig {

	@Bean
	public SpringLiquibase liquibase(LiquibaseProperties props, DataSource dataSource) {
		SpringLiquibase liquibase = new SpringLiquibase();
		liquibase.setChangeLog(props.getChangeLog());
		liquibase.setDataSource(dataSource);
        liquibase.setContexts(props.getContexts());

		return liquibase;
	}
}
