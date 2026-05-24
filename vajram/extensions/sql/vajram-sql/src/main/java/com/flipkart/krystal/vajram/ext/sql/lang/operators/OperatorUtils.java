package com.flipkart.krystal.vajram.ext.sql.lang.operators;

import com.flipkart.krystal.data.Errable;
import com.google.common.collect.Range;
import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class OperatorUtils {

  public static String rangeComparisonSql(
      String column, Range<?> range, String lowerParamHolder, String upperParamHolder) {
    if (!range.hasLowerBound() && !range.hasUpperBound()) {
      // Unbounded range means always true - so just return TRUE so that it fits nicely in the
      // surrounding WHERE clause
      return "TRUE";
    }
    List<String> bounds = new ArrayList<>();
    if (range.hasLowerBound()) {
      bounds.add(column + " " + lowerBoundOperator(range) + " " + lowerParamHolder + " ");
    }
    if (range.hasUpperBound()) {
      bounds.add(column + " " + upperBoundOperator(range) + " " + upperParamHolder + " ");
    }
    return String.join(" AND ", bounds);
  }

  public static Object lowerBoundSqlParam(Range<?> range) {
    if (range.hasLowerBound()) {
      return range.lowerEndpoint();
    }
    return Errable.nil();
  }

  public static Object upperBoundSqlParam(Range<?> range) {
    if (range.hasUpperBound()) {
      return range.upperEndpoint();
    }
    return Errable.nil();
  }

  public static String lowerBoundOperator(Range<?> range) {
    return switch (range.lowerBoundType()) {
      case OPEN -> ">";
      case CLOSED -> ">=";
    };
  }

  public static String upperBoundOperator(Range<?> range) {
    return switch (range.upperBoundType()) {
      case OPEN -> "<";
      case CLOSED -> "<=";
    };
  }
}
