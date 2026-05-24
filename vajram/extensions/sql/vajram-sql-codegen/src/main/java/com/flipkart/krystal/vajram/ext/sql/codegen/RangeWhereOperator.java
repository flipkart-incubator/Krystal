package com.flipkart.krystal.vajram.ext.sql.codegen;

import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.WhereColumn;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.WhereLeaf;
import com.flipkart.krystal.vajram.ext.sql.lang.operators.OperatorUtils;
import com.squareup.javapoet.CodeBlock;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WHERE operator for {@code @IsInRange} comparisons. A single {@code Range<T>} field produces two
 * SQL conditions (lower bound and upper bound) with two positional parameters:
 *
 * <ol>
 *   <li>{@code $N} — lower endpoint value
 *   <li>{@code $N+1} — upper endpoint value
 * </ol>
 *
 * <p>The comparison operators ({@code >} vs {@code >=}, {@code <} vs {@code <=}) are determined at
 * runtime based on the Range's bound types. The SQL template embeds sentinel tokens that the code
 * generator replaces with runtime ternary expressions:
 *
 * <pre>{@code
 * col __RANGE_LOP_1__ $1 AND col __RANGE_UOP_2__ $2
 * }</pre>
 *
 * <p>At runtime this resolves to, for example:
 *
 * <pre>{@code
 * col >= $1 AND col < $2          (closedOpen)
 * col > $1 AND col <= $2          (openClosed)
 * }</pre>
 */
final class RangeWhereOperator implements WhereOperator {

  private final SqlDriverConfig sqlParamPrinter;

  RangeWhereOperator(SqlDriverConfig sqlParamPrinter) {
    this.sqlParamPrinter = sqlParamPrinter;
  }

  @Override
  public CodeBlock toSql(String columnRef, WhereLeaf leaf, WhereColumn col, AtomicInteger argIdx) {
    CodeBlock rangeAccessor = getRangeAccessor(leaf, col);

    String lowerParam = sqlParamPrinter.getParamPlaceholder(argIdx.incrementAndGet());
    String upperParam = sqlParamPrinter.getParamPlaceholder(argIdx.incrementAndGet());

    CodeBlock lowerBoundOperator =
        CodeBlock.of("$T.lowerBoundOperator($L)", OperatorUtils.class, rangeAccessor);
    CodeBlock upperBoundOperator =
        CodeBlock.of("$T.upperBoundOperator($L)", OperatorUtils.class, rangeAccessor);

    return CodeBlock.of(
        "$S + $L + $S + $L + $S",
        CodeBlock.of("$L ", columnRef),
        lowerBoundOperator,
        CodeBlock.of(" $L AND $L ", lowerParam, columnRef),
        upperBoundOperator,
        CodeBlock.of(" $L", upperParam));
  }

  @Override
  public List<CodeBlock> getJavaParamAccessors(WhereLeaf leaf, WhereColumn col) {
    CodeBlock rangeAccessor = getRangeAccessor(leaf, col);
    return List.of(
        CodeBlock.of("$L.lowerEndpoint()", rangeAccessor),
        CodeBlock.of("$L.upperEndpoint()", rangeAccessor));
  }

  private static CodeBlock getRangeAccessor(WhereLeaf leaf, WhereColumn col) {
    return CodeBlock.of(leaf.javaAccessorPrefix() + "." + col.accessorMethod() + "()");
  }
}
