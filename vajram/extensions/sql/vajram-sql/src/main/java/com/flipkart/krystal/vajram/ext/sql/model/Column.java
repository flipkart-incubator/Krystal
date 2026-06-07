package com.flipkart.krystal.vajram.ext.sql.model;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

import com.flipkart.krystal.vajram.ext.sql.lang.ColumnPredicate;
import com.flipkart.krystal.vajram.ext.sql.lang.operators.comparison.IsEqualTo;
import com.flipkart.krystal.vajram.ext.sql.lang.operators.comparison.IsGreaterThan;
import java.lang.annotation.Target;

/**
 * This annotation can be used in different contexts:
 *
 * <p><b>In a {@link Selection}</b>:
 *
 * <p>Used to mark a field in a @{@link Selection} interface as a column in a SELECT statement. If
 * this annotation is not used, then the model method name is used as the column name. If this
 * annotation is used on a method whose name is different from the column name, then the method name
 * is considered as the alias for the column.
 *
 * <p><b>In a {@link ColumnPredicate}</b>:
 *
 * <p>Used to declare the table column on which a comparison operator (like {@link IsEqualTo} or
 * {@link IsGreaterThan}) applies.
 */
@Target({METHOD, TYPE})
public @interface Column {
  String value();
}
