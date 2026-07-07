package com.flipkart.krystal.vajram.ext.sql.codegen.syntax;

import java.util.List;

/** PostgreSQL 18 syntax — supports {@code RETURNING col1, col2, ...}. */
public record SqlLite3_35Syntax() implements SqlSyntax {
  static final SqlLite3_35Syntax INSTANCE = new SqlLite3_35Syntax();

  @Override
  public String returningClause(List<String> columnNames) {
    return " RETURNING " + String.join(", ", columnNames);
  }
}
