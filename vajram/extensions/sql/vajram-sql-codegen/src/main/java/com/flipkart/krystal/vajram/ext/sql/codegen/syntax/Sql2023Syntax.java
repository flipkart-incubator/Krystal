package com.flipkart.krystal.vajram.ext.sql.codegen.syntax;

import java.util.List;

/** SQL:2023 standard syntax — supports {@code RETURNING col1, col2, ...}. */
public record Sql2023Syntax() implements SqlSyntax {
  static final com.flipkart.krystal.vajram.ext.sql.codegen.syntax.Sql2023Syntax INSTANCE =
      new com.flipkart.krystal.vajram.ext.sql.codegen.syntax.Sql2023Syntax();

  @Override
  public boolean supportsReturning() {
    return false;
  }

  @Override
  public String returningClause(List<String> columnNames) {
    return "";
  }
}
