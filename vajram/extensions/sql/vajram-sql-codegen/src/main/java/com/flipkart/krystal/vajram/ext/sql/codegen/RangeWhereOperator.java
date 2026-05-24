package com.flipkart.krystal.vajram.ext.sql.codegen;

import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.WhereColumn;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.WhereLeaf;
import com.flipkart.krystal.vajram.ext.sql.lang.operators.OperatorUtils;
import com.squareup.javapoet.CodeBlock;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link WhereOperator} implementation for {@code @IsInRange} comparisons.
 *
 * <p>A single {@code Range<T>} field expands into two positional SQL parameters — one for the lower
 * bound and one for the upper bound — and delegates SQL generation entirely to {@link
 * OperatorUtils#rangeComparisonSql} at runtime:
 *
 * <pre>{@code
 * OperatorUtils.rangeComparisonSql(column, range, "$N", "$N+1")
 * }</pre>
 *
 * <p>{@code rangeComparisonSql} inspects the {@link com.google.common.collect.BoundType} of each
 * endpoint to choose the correct operator ({@code >} vs {@code >=} for the lower bound, {@code <}
 * vs {@code <=} for the upper bound), producing output such as:
 *
 * <pre>{@code
 * col >= $1 AND col < $2          (closedOpen)
 * col > $1 AND col <= $2          (openClosed)
 * col >= $1 AND col <= $2         (closedClosed)
 * col > $1 AND col < $2          (openOpen)
 * TRUE                            (unbounded)
 * }</pre>
 *
 * <p>Bounds that are absent (e.g. an unbounded upper end) are omitted from the generated SQL.
 * {@link #getJavaParamAccessors} correspondingly returns two accessors — {@link
 * OperatorUtils#lowerBoundSqlParam} and {@link OperatorUtils#upperBoundSqlParam} — which return
 * {@link com.flipkart.krystal.data.Errable#nil()} for absent endpoints so the parameter slot is
 * safely skipped.
 */
final class RangeWhereOperator implements WhereOperator {

  private final SqlDriverConfig sqlParamPrinter;

  RangeWhereOperator(SqlDriverConfig sqlParamPrinter) {
    this.sqlParamPrinter = sqlParamPrinter;
  }

  @Override
  public CodeBlock toSql(String columnRef, WhereLeaf leaf, WhereColumn col, AtomicInteger argIdx) {
    return CodeBlock.of(
        "$T.rangeComparisonSql($S, $L, $S, $S)",
        OperatorUtils.class,
        columnRef,
        getRangeAccessor(leaf, col),
        sqlParamPrinter.getParamPlaceholder(argIdx.incrementAndGet()),
        sqlParamPrinter.getParamPlaceholder(argIdx.incrementAndGet()));
  }

  @Override
  public List<CodeBlock> getJavaParamAccessors(WhereLeaf leaf, WhereColumn col) {
    CodeBlock rangeAccessor = getRangeAccessor(leaf, col);
    return List.of(
        CodeBlock.of("$T.lowerBoundSqlParam($L)", OperatorUtils.class, rangeAccessor),
        CodeBlock.of("$T.upperBoundSqlParam($L)", OperatorUtils.class, rangeAccessor));
  }

  private static CodeBlock getRangeAccessor(WhereLeaf leaf, WhereColumn col) {
    return CodeBlock.of(leaf.javaAccessorPrefix() + "." + col.accessorMethod() + "()");
  }
}
