package com.flipkart.krystal.vajram.ext.sql.statement;

import static java.lang.annotation.ElementType.TYPE_USE;

import com.flipkart.krystal.vajram.ext.sql.statement.ORDER.ORDER_Clauses;
import com.google.auto.value.AutoAnnotation;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Target;
import lombok.experimental.UtilityClass;

@Repeatable(ORDER_Clauses.class)
@Target(TYPE_USE)
public @interface ORDER {
  String by();

  Direction direction();

  enum Direction {
    ASC,
    DESC
  }

  @UtilityClass
  final class Creator {
    public static @AutoAnnotation ORDER create(String by, Direction direction) {
      return new AutoAnnotation_ORDER_Creator_create(by, direction);
    }
  }

  @Target(TYPE_USE)
  @interface ORDER_Clauses {
    ORDER[] value();

    @UtilityClass
    final class Creator {
      public static @AutoAnnotation ORDER_Clauses create(ORDER[] value) {
        return new AutoAnnotation_ORDER_ORDER_Clauses_Creator_create(value);
      }
    }
  }
}
