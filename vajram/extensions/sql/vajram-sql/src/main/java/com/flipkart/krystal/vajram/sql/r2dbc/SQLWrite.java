package com.flipkart.krystal.vajram.sql.r2dbc;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.model.IfAbsent.IfAbsentThen;
import com.flipkart.krystal.vajram.IOVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import jakarta.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@InvocableOutsideGraph
@SuppressWarnings("initialization.field.uninitialized")
@Vajram
public abstract class SQLWrite extends IOVajramDef<Long> {

  static class _Inputs {
    @IfAbsent(IfAbsentThen.FAIL)
    String query;

    @IfAbsent(IfAbsentThen.FAIL)
    List<Object> parameters;

    @Nullable
    List<String> generatedColumns;
  }

  static class _InternalFacets {
    @Inject
    @IfAbsent(IfAbsentThen.FAIL)
    ConnectionPool connectionPool;
  }

  @Output
  static CompletableFuture<? extends Result> write(
      String query, ConnectionPool connectionPool, List<Object> parameters, @Nullable List<String> generatedColumns) {
    return Mono.usingWhen(
            connectionPool.create(),
            connection -> {
              Statement statement = connection.createStatement(query);

              if (generatedColumns != null && !generatedColumns.isEmpty()) {
                statement.returnGeneratedValues(
                    generatedColumns.toArray(new String[0]));
              } else {
                // Return all generated values (database-dependent)
                statement.returnGeneratedValues();
              }

              // Bind parameters
              if (parameters != null && !parameters.isEmpty()) {
                for (int i = 0; i < parameters.size(); i++) {
                  statement.bind(i, parameters.get(i));
                }
              }

              return Mono.from(statement.execute());
            },
            Connection::close)
        .toFuture();
  }
}
