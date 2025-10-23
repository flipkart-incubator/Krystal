package com.flipkart.krystal.vajram.sql.r2dbc;

import io.r2dbc.spi.ColumnMetadata;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import java.lang.reflect.Field;
import java.util.List;
import java.util.function.BiFunction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class RowMapper {

  public <T> Mono<List<T>> map(Result result, Class<T> type) {
    BiFunction<Row, RowMetadata, T> mappingFunction = createMappingFunction(type);

    return Flux.from(result.map(mappingFunction)).collectList();
  }

  private <T> BiFunction<Row, RowMetadata, T> createMappingFunction(Class<T> type) {
    return (row, metadata) -> {
      try {
        T instance = type.getDeclaredConstructor().newInstance();

        for (ColumnMetadata columnMetadata : metadata.getColumnMetadatas()) {
          String columnName = columnMetadata.getName();
          String fieldName = toCamelCase(columnName);
          try {
            Field field = type.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = row.get(columnName, field.getType());
            field.set(instance, value);
          } catch (NoSuchFieldException e) {

          }
        }
        return instance;
      } catch (Exception e) {
        throw new RuntimeException("Could not map row to " + type.getName(), e);
      }
    };
  }

  private static String toCamelCase(String snakeCase) {
    StringBuilder sb = new StringBuilder();
    boolean nextIsUpper = false;
    for (char c : snakeCase.toLowerCase().toCharArray()) {
      if (c == '_') {
        nextIsUpper = true;
      } else {
        sb.append(nextIsUpper ? Character.toUpperCase(c) : c);
        nextIsUpper = false;
      }
    }
    return sb.toString();
  }
}
