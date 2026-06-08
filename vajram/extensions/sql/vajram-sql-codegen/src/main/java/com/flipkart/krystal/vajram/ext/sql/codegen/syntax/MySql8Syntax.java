package com.flipkart.krystal.vajram.ext.sql.codegen.syntax;

import java.util.List;

/**
 * MySQL 8 syntax — does <b>not</b> support {@code RETURNING} on INSERT. A compile-time error should
 * be raised if a RETURNING clause is requested with this dialect.
 */
public record MySql8Syntax() implements SqlSyntax {
  static final com.flipkart.krystal.vajram.ext.sql.codegen.syntax.MySql8Syntax INSTANCE =
      new com.flipkart.krystal.vajram.ext.sql.codegen.syntax.MySql8Syntax();

  @Override
  public boolean supportsReturning() {
    return false;
  }

  @Override
  public String returningClause(List<String> columnNames) {
    if (columnNames.size() > 1) {
      throw new UnsupportedOperationException(
          "MySQL 8 does not support RETURNING on INSERT statements. "
              + "Use SqlDialect.POSTGRESQL_18 or SqlDialect.SQL_2023 instead.");
    }
    // WE can add `SELECT LAST_INSERT_ID()` but, drivers like vertx mysql client make this value
    // available by default. so just return empty
    return "";
  }
}
