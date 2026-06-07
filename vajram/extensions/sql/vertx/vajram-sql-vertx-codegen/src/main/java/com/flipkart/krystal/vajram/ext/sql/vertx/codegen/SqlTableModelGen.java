package com.flipkart.krystal.vajram.ext.sql.vertx.codegen;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;

/**
 * Code generator invoked for each {@code @Table} element. Previously generated {@code
 * Table_InsertResult} selection interfaces automatically; now a no-op since users define their own
 * {@code @ReturnOnInsert} interfaces. Retained as a hook for future table-level code generation.
 */
public final class SqlTableModelGen implements CodeGenerator {

  private final SqlTableGenContext context;
  private final CodeGenUtility util;

  public SqlTableModelGen(SqlTableGenContext context) {
    this.context = context;
    this.util = context.util();
  }

  @Override
  public void generate() {
    // No-op. Users now define their own @ReturnOnInsert interfaces manually.
  }
}
