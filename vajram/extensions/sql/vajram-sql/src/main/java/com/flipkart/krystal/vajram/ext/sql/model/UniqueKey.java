package com.flipkart.krystal.vajram.ext.sql.model;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Target;

/**
 * Annotation to mark a field or a set of fields in a @{@link Table} model as a unique key. If the
 * unique key is on a single column, the annotation must be applied to the model method. If the
 * unique key is on multiple columns, the annotation must be applied to the model interface.
 */
@Target({METHOD, TYPE})
public @interface UniqueKey {

  /** The name of the unique key */
  String name();

  /**
   * The columns that make up the unique key.
   *
   * <ul>
   *   <li>If the unique key is on a single column, this MUST be left empty and the annotation must
   *       be applied to the model method.
   *   <li>If the unique key is on multiple columns, this MUST be non-empty and the annotation must
   *       be applied to the model interface.
   * </ul>
   */
  String[] columns() default {};
}
