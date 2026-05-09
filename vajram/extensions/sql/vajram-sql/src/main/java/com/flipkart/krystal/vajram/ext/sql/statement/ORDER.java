package com.flipkart.krystal.vajram.ext.sql.statement;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE_USE;

import com.flipkart.krystal.vajram.ext.sql.statement.ORDER.ORDER_BYs;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Target;

@Repeatable(ORDER_BYs.class)
@Target({METHOD, TYPE_USE})
public @interface ORDER {
  String by();

  Direction direction();

  enum Direction {
    ASC,
    DESC
  }

  @Target({METHOD, TYPE_USE})
  @interface ORDER_BYs {
    ORDER[] value();
  }
}
