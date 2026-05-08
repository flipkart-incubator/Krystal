package com.flipkart.krystal.vajram.ext.sql.model;

import static java.lang.annotation.ElementType.TYPE;

import com.flipkart.krystal.model.Model;
import java.lang.annotation.Target;

/**
 * Annotation to mark a @{@link Model} interface as an SQL table. Each field of the model
 * corresponds to a column in the table.
 */
@Target(TYPE)
public @interface Table {
  /**
   * Name of the SQL table modelled by the class on which this annotation is present. If empty, it
   * means the class name is same as the table name
   */
  String name() default "";
}
