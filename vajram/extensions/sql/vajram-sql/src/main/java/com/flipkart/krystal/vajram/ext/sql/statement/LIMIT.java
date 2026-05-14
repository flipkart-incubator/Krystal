package com.flipkart.krystal.vajram.ext.sql.statement;

import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.auto.value.AutoAnnotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import lombok.experimental.UtilityClass;

@Target(TYPE_USE)
@Retention(RUNTIME)
public @interface LIMIT {
  /** Sentinel meaning "no limit": fetches all matching rows. */
  int NO_LIMIT = -1;

  int value();

  @UtilityClass
  final class Creator {
    public static @AutoAnnotation LIMIT create(int value) {
      return new AutoAnnotation_LIMIT_Creator_create(value);
    }
  }
}
