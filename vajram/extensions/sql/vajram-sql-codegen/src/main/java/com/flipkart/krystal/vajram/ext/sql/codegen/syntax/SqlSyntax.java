package com.flipkart.krystal.vajram.ext.sql.codegen.syntax;

import com.flipkart.krystal.vajram.ext.sql.lang.SqlDialect;
import java.util.List;

/**
 * Provides SQL-dialect-specific syntax for code generation. Different database servers have
 * different syntax for operations like {@code RETURNING} after an INSERT. This class encapsulates
 * those differences so that code generators can produce the correct SQL for the target database.
 *
 * <p>Obtain an instance via {@link #forDialect(SqlDialect)}.
 */
public sealed interface SqlSyntax
    permits MySql8Syntax, PostgreSqlSyntax, Sql2023Syntax, SqlLite3_35Syntax {

  /**
   * Returns the appropriate {@link SqlSyntax} implementation for the given SQL dialect.
   *
   * @param dialect the target database dialect from the {@code @SQL} annotation
   * @return a syntax provider for the given dialect
   */
  static SqlSyntax forDialect(SqlDialect dialect) {
    return switch (dialect) {
      case POSTGRESQL_18 -> PostgreSqlSyntax.INSTANCE;
      case MYSQL_8 -> MySql8Syntax.INSTANCE;
      case SQL_2023 -> Sql2023Syntax.INSTANCE;
      case SQL_LITE_3_35 -> SqlLite3_35Syntax.INSTANCE;
    };
  }

  /**
   * Builds the {@code RETURNING col1, col2, ...} clause to append after an INSERT statement.
   *
   * @param columnNames the column names to include in the RETURNING clause
   * @return the SQL RETURNING clause string (including the leading space), or empty string if not
   *     supported
   * @throws UnsupportedOperationException if this dialect does not support RETURNING clause and the
   *     columnNames list is not empty
   */
  String returningClause(List<String> columnNames) throws Exception;

  /**
   * Transforms a column name (or alias) to match what the database returns in result set metadata.
   * This is used at code-generation time so that the generated {@code row.getLong("...")} calls use
   * the correct case.
   *
   * <p>For example, PostgreSQL lowercases all unquoted identifiers, so {@code orderId} becomes
   * {@code orderid} in the result set.
   *
   * @param columnName the column name or alias as written in the generated SQL
   * @return the column name as it will appear in the result set
   */
  default String columnNameInResult(String columnName) {
    return columnName;
  }
}
