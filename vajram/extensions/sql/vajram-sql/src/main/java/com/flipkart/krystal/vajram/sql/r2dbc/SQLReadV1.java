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
import io.r2dbc.spi.Statement;
import jakarta.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SuppressWarnings("initialization.field.uninitialized")
@InvocableOutsideGraph
@Vajram
public abstract class SQLReadV1 extends IOVajramDef<SQLResult> {

  static class _Inputs {
    @IfAbsent(IfAbsentThen.FAIL)
    String selectQuery;

    @Nullable
    List<Object> parameters;

    @IfAbsent(IfAbsentThen.FAIL)
    Class<?> resultType;
  }

  static class _InternalFacets {

    @Inject
    @IfAbsent(IfAbsentThen.FAIL)
    ConnectionPool connectionPool;

    @Inject
    @IfAbsent(IfAbsentThen.FAIL)
    RowMapper rowMapper;
  }


  @Output
  static CompletableFuture<SQLResult> read(
      String selectQuery,
      ConnectionPool connectionPool,
      @Nullable List<Object> parameters,
      @Nullable Class<?> resultType,
      RowMapper rowMapper) {
    //Validate that the statement starts with select
    if (!selectQuery.trim().toLowerCase().startsWith("select")) {
      throw new IllegalArgumentException("Read query must start with 'select'");
    }
    return Mono.usingWhen(
            connectionPool.create(),
            connection -> {
              Statement statement = connection.createStatement(selectQuery);
              // Bind parameters if provided
              if (parameters != null && !parameters.isEmpty()) {
                for (int i = 0; i < parameters.size(); i++) {
                  statement.bind(i, parameters.get(i));
                }
              }

              // Execute and map based on resultType
              return Mono.from(statement.execute())
                  .flatMap(result -> {
                    if (resultType != null) {
                      // Use RowMapper to map to the specified type
                      Mono<? extends List<?>> res =  rowMapper.map(result, resultType);
                      return res.map(list -> new SQLResult(list, 0));
                    } else {
                      // Default behavior: return Map<String, Object>
                      return Flux.from(result.map((row, rowMetadata) -> {
                        java.util.Map<String, Object> rowData = new java.util.HashMap<>();
                        rowMetadata.getColumnMetadatas().forEach(columnMetadata -> {
                          String columnName = columnMetadata.getName();
                          Object value = row.get(columnName);
                          rowData.put(columnName, value);
                        });
                        return rowData;
                      })).collectList().map(list -> new SQLResult(list, 0));
                    }
                  });
            },
            Connection::close)
        .toFuture();
  }
}
