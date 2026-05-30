package com.flipkart.krystal.vajram.ext.sql.codegen;

import java.util.List;

/**
 * Builds SQL INSERT statements from parsed {@link InsertQueryModel} records.
 *
 * <p>The generated INSERT uses positional placeholders ({@code $1}, {@code $2}, …) for parameter
 * binding, matching the convention used by the Vert.x SQL client.
 */
public final class InsertQueryBuilder {

  private InsertQueryBuilder() {}

  /**
   * Builds a parameterized {@code INSERT INTO table (col1, col2, …) VALUES ($1, $2, …)} statement.
   *
   * @param model the parsed INSERT model containing table name and column definitions
   * @param config driver-specific config for placeholder format
   * @return the SQL INSERT string with positional placeholders
   */
  public static String buildInsertSql(InsertQueryModel model, SqlDriverConfig config) {
    List<InsertQueryModel.InsertColumn> columns = model.columns();

    StringBuilder sb = new StringBuilder();
    sb.append("INSERT INTO ").append(model.tableName()).append(" (");

    for (int i = 0; i < columns.size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(columns.get(i).columnName());
    }

    sb.append(") VALUES (");

    for (int i = 0; i < columns.size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(config.getParamPlaceholder(i + 1));
    }

    sb.append(")");

    return sb.toString();
  }
}
