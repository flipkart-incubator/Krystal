package com.flipkart.krystal.vajram.ext.sql.codegen;

import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.WhereColumn;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.WhereLeaf;
import com.squareup.javapoet.CodeBlock;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public interface WhereOperator {
  CodeBlock toSql(String columnRef, WhereLeaf leaf, WhereColumn col, AtomicInteger argIdx);

  List<CodeBlock> getJavaParamAccessors(WhereLeaf leaf, WhereColumn col);
}
