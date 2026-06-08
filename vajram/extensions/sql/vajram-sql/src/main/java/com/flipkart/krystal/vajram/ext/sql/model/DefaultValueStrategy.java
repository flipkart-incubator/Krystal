package com.flipkart.krystal.vajram.ext.sql.model;

import static com.flipkart.krystal.vajram.ext.sql.model.DefaultValueStrategy.Trigger.ON_INSERT;
import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Target;
import java.time.Instant;

/** Specifies the default value to be stored in a column */
@Target(METHOD)
public @interface DefaultValueStrategy {
  ValueComputation value();

  Trigger trigger() default ON_INSERT;

  enum ValueComputation {
    /**
     * This means the default value is a static value that is provided by the user via the {@link
     * DefaultValue} annotation.
     */
    CUSTOM_STATIC_VALUE,
    /**
     * Marks a column whose value is auto-assigned by the database (e.g. auto-increment IDs). Such
     * columns are excluded from INSERT value lists. To return auto-assigned values from an INSERT,
     * declare a {@code @ReturnOnInsert} interface listing the columns to return.
     */
    AUTO_ASSIGN_ID,
    /**
     * Applicable to columns of type {@link Instant}. This will cause the column to be set to the
     * current timestamp when the row is inserted.
     */
    CURRENT_TIMESTAMP,
  }

  enum Trigger {
    ON_INSERT,
    ON_UPDATE
  }
}
