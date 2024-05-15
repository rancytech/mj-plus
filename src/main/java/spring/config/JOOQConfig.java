package spring.config;


import com.mysql.cj.jdbc.MysqlConnectionPoolDataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.jooq.SpringTransactionProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.sql.DataSource;

@Configuration
@ConditionalOnExpression("'${mj.account-store-type}'.equals('mysql') || '${mj.task-store.type}'.equals('mysql')")
public class JOOQConfig {

	@Bean
	@ConfigurationProperties(prefix = "spring.datasource")
	public DataSource dataSource() {
		return new MysqlConnectionPoolDataSource();
	}

	@Bean
	public DSLContext dslContext(SpringTransactionProvider springTransactionProvider) {
		DefaultConfiguration configuration = new DefaultConfiguration();
		configuration.set(SQLDialect.MYSQL);
		configuration.set(new TransactionAwareDataSourceProxy(dataSource()));
		configuration.setTransactionProvider(springTransactionProvider);
		return new DefaultDSLContext(configuration);
	}
}
