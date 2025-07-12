package pl.logistic.logisticops.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(basePackages = "pl.logistic.logisticops.repository")
@EnableJpaAuditing
@EnableTransactionManagement
public class JpaConfig {
    // JPA configuration if needed
}