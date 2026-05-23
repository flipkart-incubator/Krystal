package com.flipkart.krystal.vajram.ext.sql.codegen;

import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.WhereColumn;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.WhereLeaf;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

final class SimpleWhereOperator implements WhereOperator {

  private final SqlParameterPrinter sqlParamPrinter;
  private final String operator;

  public SimpleWhereOperator(String operator, SqlParameterPrinter sqlParamPrinter) {
    this.sqlParamPrinter = sqlParamPrinter;
    this.operator = operator;
  }

  @Override
  public String toSql(String columnRef, AtomicInteger argIdx) {
    return columnRef
        + " "
        + operator
        + " "
        + sqlParamPrinter.printParameter(argIdx.incrementAndGet());
  }

  @Override
  public List<String> toJavaParams(WhereLeaf leaf, WhereColumn col) {
    return List.of(leaf.javaAccessorPrefix() + "." + col.accessorMethod() + "()");
  }
}
