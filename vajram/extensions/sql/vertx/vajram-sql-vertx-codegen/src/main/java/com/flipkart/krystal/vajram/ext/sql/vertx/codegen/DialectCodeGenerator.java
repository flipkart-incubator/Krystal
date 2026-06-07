package com.flipkart.krystal.vajram.ext.sql.vertx.codegen;

import com.flipkart.krystal.vajram.ext.sql.lang.SqlDialect;
import com.squareup.javapoet.MethodSpec;

sealed interface DialectCodeGenerator
    permits PostGreCodeGenerator, MySqlCodeGenerator, Sql2023CodeGenerator {

  /**
   * Builds the mapResult method for INSERT where some data is returned by the Database (For example
   * RETURNING in PostGre or LAST_INSERT_ID in MySQL)— maps the returned rows to the selection type
   * or {@code List<Selection>}.
   */
  MethodSpec mapResultsForInsertReturn(InsertResultType resultType);

  static DialectCodeGenerator forDialect(SqlDialect dialect, VertxSqlUtil vertxSqlUtil) {
    return switch (dialect) {
      case SQL_2023 -> null;
      case MYSQL_8 -> new MySqlCodeGenerator(vertxSqlUtil);
      case POSTGRESQL_18 -> new PostGreCodeGenerator(vertxSqlUtil);
    };
  }
}
