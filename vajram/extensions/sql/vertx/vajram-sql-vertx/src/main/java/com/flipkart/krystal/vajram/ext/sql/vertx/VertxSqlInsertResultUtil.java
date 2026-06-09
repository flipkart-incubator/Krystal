package com.flipkart.krystal.vajram.ext.sql.vertx;

import static java.util.Objects.requireNonNull;

import io.vertx.mysqlclient.MySQLClient;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Runtime utility for extracting auto-assigned IDs from SQL INSERT results. Encapsulates
 * database-specific logic that the generated vajrams call in their {@code mapResult} methods.
 *
 * <p>For MySQL, the Vert.x MySQL client exposes the last auto-assigned ID via a driver-specific
 * {@link RowSet#property property}. When inserting N rows, MySQL auto-increment assigns IDs
 * starting from {@code LAST_INSERT_ID} with a step equal to {@code @@auto_increment_increment}, so
 * we compute the full list as {@code [firstId, firstId + inc, …, firstId + (N − 1) * inc]}.
 */
public final class VertxSqlInsertResultUtil {

  private VertxSqlInsertResultUtil() {}

  /**
   * Extracts the auto-assigned ID from a MySQL single-row INSERT result.
   *
   * @param rowSet the result of a single-row INSERT statement
   * @return the auto-assigned ID assigned by MySQL
   */
  public static long extractMySqlAutoId(RowSet<Row> rowSet) {
    return requireNonNull(
        rowSet.property(MySQLClient.LAST_INSERTED_ID),
        "MySQL auto-assigned ID not found in result set");
  }

  /**
   * Extracts auto-assigned IDs from a MySQL batch INSERT result. MySQL assigns IDs starting from
   * {@link MySQLClient#LAST_INSERTED_ID} with a step of {@code autoIncrementIncrement} (the server
   * variable {@code @@auto_increment_increment}).
   *
   * @param rowSet the result of a batch INSERT statement
   * @param count the number of rows inserted (must be &gt; 0)
   * @param autoIncrementIncrement the MySQL {@code @@auto_increment_increment} value (must be &ge;
   *     1)
   * @return list of auto-assigned IDs in insertion order
   */
  public static List<Long> extractMySqlAutoIds(
      RowSet<Row> rowSet, int count, int autoIncrementIncrement) {
    if (count <= 0) {
      throw new IllegalArgumentException("count must be > 0, got " + count);
    }
    if (autoIncrementIncrement < 1) {
      throw new IllegalArgumentException(
          "autoIncrementIncrement must be >= 1, got " + autoIncrementIncrement);
    }
    long firstId = rowSet.property(MySQLClient.LAST_INSERTED_ID);
    List<Long> ids = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      ids.add(firstId + (long) i * autoIncrementIncrement);
    }
    return ids;
  }
}
