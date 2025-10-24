package com.flipkart.krystal.vajram.sql.r2dbc;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.model.IfAbsent.IfAbsentThen;
import com.flipkart.krystal.vajram.IOVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.sql.SQLResult;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import jakarta.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@InvocableOutsideGraph
@SuppressWarnings("initialization.field.uninitialized")
@Vajram
public abstract class SQLWrite extends IOVajramDef<SQLResult> {

  static class _Inputs {
    @IfAbsent(IfAbsentThen.FAIL)
    String query;

    @IfAbsent(IfAbsentThen.FAIL)
    List<Object> parameters;
  }

  static class _InternalFacets {
    @Inject
    @IfAbsent(IfAbsentThen.FAIL)
    ConnectionPool connectionPool;
  }

  @Output
  static CompletableFuture<SQLResult> write(
      String query, ConnectionPool connectionPool, List<Object> parameters) {
    return Mono.usingWhen(
            connectionPool.create(),
            connection -> {
              Statement statement = connection.createStatement(query);

              // Bind parameters
              if (parameters != null && !parameters.isEmpty()) {
                for (int i = 0; i < parameters.size(); i++) {
                  statement.bind(i, parameters.get(i));
                }
              }
              return Flux.from(statement.execute())
                  .flatMap(Result::getRowsUpdated)
                  .reduce(Long::sum)
                  .defaultIfEmpty(0L)
                  .map(rowsUpdated -> new SQLResult(Collections.emptyList(), rowsUpdated));
            },
            Connection::close)
        .toFuture();
  }
}
