package com.flipkart.krystal.vajram.ext.sql.vertx.codegen;

import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.InsertResultType;
import com.squareup.javapoet.MethodSpec;

public final class Sql2023CodeGenerator implements DialectCodeGenerator {

  @Override
  public MethodSpec mapResultsForInsertReturn(InsertResultType resultType) {
    throw new UnsupportedOperationException("SQL_2023 doesn't support returning data from INSERT.");
  }
}
