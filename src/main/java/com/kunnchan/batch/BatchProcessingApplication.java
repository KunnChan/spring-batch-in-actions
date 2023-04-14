package com.kunnchan.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

@SpringBootApplication(exclude={BatchAutoConfiguration.class})
public class BatchProcessingApplication {

	public static void main(String[] args) {
		SpringApplication.run(BatchProcessingApplication.class, args);
	}

//	@Bean
//	@ConfigurationProperties(prefix = "spring.datasource")
//	public DataSource dataSource() {
//
//		return DataSourceBuilder.create()
//				.build();
//	}

}
