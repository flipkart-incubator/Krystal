package com.flipkart.krystal.vajram.ext.sql.codegen;

import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.WhereColumn;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.WhereLeaf;
import com.squareup.javapoet.CodeBlock;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

final class SimpleWhereOperator implements WhereOperator {

  private final SqlDriverConfig sqlParamPrinter;
  private final String operator;

  public SimpleWhereOperator(String operator, SqlDriverConfig sqlParamPrinter) {
    this.sqlParamPrinter = sqlParamPrinter;
    this.operator = operator;
  }

  @Override
  public CodeBlock toSql(String columnRef, WhereLeaf leaf, WhereColumn col, AtomicInteger argIdx) {
    return CodeBlock.of(
        "$S",
        CodeBlock.of(
            "$L $L $L",
            columnRef,
            operator,
            sqlParamPrinter.getParamPlaceholder(argIdx.incrementAndGet())));
  }

  @Override
  public List<CodeBlock> getJavaParamAccessors(WhereLeaf leaf, WhereColumn col) {
    return List.of(CodeBlock.of("$L.$L()", leaf.javaAccessorPrefix(), col.accessorMethod()));
  }
}
