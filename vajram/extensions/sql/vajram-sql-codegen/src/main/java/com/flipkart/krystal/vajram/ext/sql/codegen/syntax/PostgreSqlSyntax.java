package com.flipkart.krystal.vajram.ext.sql.codegen.syntax;

import java.util.List;
import java.util.Locale;

/** PostgreSQL 18 syntax — supports {@code RETURNING col1, col2, ...}. */
public record PostgreSqlSyntax() implements SqlSyntax {
  static final com.flipkart.krystal.vajram.ext.sql.codegen.syntax.PostgreSqlSyntax INSTANCE =
      new com.flipkart.krystal.vajram.ext.sql.codegen.syntax.PostgreSqlSyntax();

  @Override
  public boolean supportsReturning() {
    return true;
  }

  @Override
  public String returningClause(List<String> columnNames) {
    return " RETURNING " + String.join(", ", columnNames);
  }

  @Override
  public String columnNameInResult(String columnName) {
    return columnName.toLowerCase(Locale.ROOT);
  }
}
