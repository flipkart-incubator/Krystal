package com.flipkart.krystal.vajram.ext.sql.lang.operators;

import com.google.common.collect.Range;
import lombok.experimental.UtilityClass;

@UtilityClass
public class OperatorUtils {
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
