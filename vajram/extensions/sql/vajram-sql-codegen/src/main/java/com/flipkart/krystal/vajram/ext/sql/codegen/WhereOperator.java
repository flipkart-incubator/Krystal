package com.flipkart.krystal.vajram.ext.sql.codegen;

import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.WhereColumn;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.WhereLeaf;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public interface WhereOperator {
  String toSql(String columnRef, AtomicInteger argIdx);

  List<String> toJavaParams(WhereLeaf leaf, WhereColumn col);
}
