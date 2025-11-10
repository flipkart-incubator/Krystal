package com.flipkart.krystal.vajram.sql.r2dbc;

import io.r2dbc.spi.ColumnMetadata;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public final class DefaultRowMapper implements RowMapper {

  public <T> Mono<List<T>> map(Result result, Class<T> type) {
    BiFunction<Row, RowMetadata, T> mappingFunction = createMappingFunction(type);

    return Flux.from(result.map(mappingFunction)).collectList();
  }

  private <T> BiFunction<Row, RowMetadata, T> createMappingFunction(Class<T> type) {
    return (row, metadata) -> {
      // Check if type is a primitive wrapper or simple type (String, Long, Integer, etc.)
      if (isPrimitiveOrWrapper(type)) {
        // For primitive/wrapper types, get the first (and likely only) column value
        List<? extends ColumnMetadata> columns = metadata.getColumnMetadatas();
        if (columns.isEmpty()) {
          throw new RuntimeException("No columns found in result set");
        }
        String columnName = columns.get(0).getName();
        return row.get(columnName, type);
      }

      // Check if type is a record
      if (type.isRecord()) {
        return mapRecord(row, metadata, type);
      }

      // For regular classes, map fields using reflection
      return mapClass(row, metadata, type);
    };
  }

  private <T> T mapRecord(Row row, RowMetadata metadata, Class<T> type) {
    try {
      RecordComponent[] components = type.getRecordComponents();

      // Build a map of column values by field name
      Map<String, String> columnValues = new LinkedHashMap<>();
      for (ColumnMetadata columnMetadata : metadata.getColumnMetadatas()) {
        String columnName = columnMetadata.getName();
        String fieldName = toCamelCase(columnName);
        columnValues.put(fieldName, columnName);
      }
      // Prepare constructor arguments in the order of record components
      Class<?>[] paramTypes = new Class<?>[components.length];
      Object[] args = new Object[components.length];

      for (int i = 0; i < components.length; i++) {
        RecordComponent component = components[i];
        String componentName = component.getName();
        paramTypes[i] = component.getType();

        // Get value from column map, or null if not present
        args[i] = row.get(columnValues.get(componentName), paramTypes[i]);
      }
      // Invoke the canonical constructor
      Constructor<T> constructor = type.getDeclaredConstructor(paramTypes);
      constructor.setAccessible(true);
      return constructor.newInstance(args);
    } catch (Exception e) {
      throw new RuntimeException("Could not map row to record " + type.getName(), e);
    }
  }

  /**
   * Maps a database row to a regular Java class using field reflection.
   *
   * @param row The database row
   * @param metadata The row metadata
   * @param type The class
   * @return Instance of the class
   */
  private <T> T mapClass(Row row, RowMetadata metadata, Class<T> type) {
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
          // Field not found, skip this column
        }
      }
      return instance;
    } catch (Exception e) {
      throw new RuntimeException("Could not map row to " + type.getName(), e);
    }
  }

  /**
   * Checks if the type is a primitive wrapper or commonly used simple type. These types are
   * typically used for single-column queries.
   */
  private boolean isPrimitiveOrWrapper(Class<?> type) {
    return type == String.class
        || type == Long.class
        || type == Integer.class
        || type == Double.class
        || type == Float.class
        || type == Boolean.class
        || type == Short.class
        || type == Byte.class
        || type == Character.class
        || type == BigDecimal.class
        || type == LocalDate.class
        || type == LocalDateTime.class
        || type.isPrimitive();
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
