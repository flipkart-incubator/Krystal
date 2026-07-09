package com.flipkart.krystal.vajram.ext.sql.codegen.syntax;

import java.util.List;

/** SqlLite 3.35 syntax — supports {@code RETURNING col1, col2, ...}. */
public record Sqlite3_35Syntax() implements SqlSyntax {
  static final Sqlite3_35Syntax INSTANCE = new Sqlite3_35Syntax();

  @Override
  public String returningClause(List<String> columnNames) {
    return " RETURNING " + String.join(", ", columnNames);
  }
}
