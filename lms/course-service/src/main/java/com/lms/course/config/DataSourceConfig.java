package com.lms.course.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Two databases:
 *  - WRITE (primary): used by JPA + Flyway automatically.
 *  - READ: used only by the catalog projector and catalog queries (plain JDBC).
 */
@Configuration
public class DataSourceConfig {

    // ---- write side ----
    @Bean @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties writeDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean @Primary
    public DataSource writeDataSource(@Qualifier("writeDataSourceProperties") DataSourceProperties props) {
        return props.initializeDataSourceBuilder().build();
    }

    // ---- read side ----
    @Bean
    @ConfigurationProperties("app.read-datasource")
    public DataSourceProperties readDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource readDataSource(@Qualifier("readDataSourceProperties") DataSourceProperties props) {
        return props.initializeDataSourceBuilder().build();
    }

    @Bean
    public JdbcTemplate readJdbc(@Qualifier("readDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }

    /** Creates the read-side table on startup. */
    @Bean
    public InitializingBean readDbMigration(@Qualifier("readDataSource") DataSource ds) {
        return () -> Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/read-migration")
                .load()
                .migrate();
    }
}
