package com.flipkart.krystal.vajram.samples.sql;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.asyncer.r2dbc.mysql.MySqlConnectionConfiguration;
import io.asyncer.r2dbc.mysql.MySqlConnectionFactory;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactory;
import java.time.Duration;

public class SqlGuiceModule extends AbstractModule {
  @Provides
  @Singleton
  // @Named("connectionFactory")
  public ConnectionPool provideConnectionPool() {
    MySqlConnectionConfiguration configuration =
        MySqlConnectionConfiguration.builder()
            .host("localhost")
            .port(3306)
            .username("root")
            .database("users")
            .build();

    ConnectionFactory connectionFactory = MySqlConnectionFactory.from(configuration);
    return new ConnectionPool(
        ConnectionPoolConfiguration.builder(connectionFactory)
            .maxIdleTime(Duration.ofMinutes(30))
            .initialSize(5)
            .maxSize(20)
            .build());
  }
}
