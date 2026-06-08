package com.flipkart.krystal.vajram.ext.sql.vertx;

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
 * {@link RowSet#property property}. When inserting N rows, MySQL auto-increment assigns sequential
 * IDs starting from {@code LAST_INSERT_ID}, so we compute the full list as {@code [firstId, firstId
 * + 1, …, firstId + N − 1]}.
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
    return rowSet.property(MySQLClient.LAST_INSERTED_ID);
  }

  /**
   * Extracts auto-assigned IDs from a MySQL batch INSERT result. MySQL assigns sequential IDs
   * starting from {@link MySQLClient#LAST_INSERTED_ID}.
   *
   * @param rowSet the result of a batch INSERT statement
   * @param count the number of rows inserted
   * @return list of auto-assigned IDs in insertion order
   */
  public static List<Long> extractMySqlAutoIds(RowSet<Row> rowSet, int count) {
    long firstId = rowSet.property(MySQLClient.LAST_INSERTED_ID);
    List<Long> ids = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      ids.add(firstId + i);
    }
    return ids;
  }
}
