package com.flipkart.krystal.vajram.ext.sql.lang;

import static java.lang.annotation.ElementType.TYPE;

import com.flipkart.krystal.vajram.ext.sql.model.TableModel;
import java.lang.annotation.Target;

/**
 * When placed on a @ModelRoot, this annotation indicates to the platform that the columns declared
 * in the model must be returned on insertion into the provided table. Rules:
 *
 * <ul>
 *   <li>The model must have at least one method
 *   <li>The column name derived from the method must be same as the column name in the table and
 *       must have the same data type
 *   <li>The model must be a @ModelRoot
 *   <li>The table must be a @Table
 * </ul>
 *
 * Different {@link SqlDialect}s may have varying support for this and might have additional rules
 * which need to be followed.
 */
@Target(TYPE)
public @interface ReturnOnInsert {
  boolean value();

  Class<? extends TableModel> inTable();
}
