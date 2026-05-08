package com.flipkart.krystal.vajram.ext.sql.statement;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE_USE;

import java.lang.annotation.Target;

@Target({METHOD, TYPE_USE})
public @interface LIMIT {
  /** Sentinel meaning "no limit": fetches all matching rows. */
  int NO_LIMIT = -1;

  int value();
}
