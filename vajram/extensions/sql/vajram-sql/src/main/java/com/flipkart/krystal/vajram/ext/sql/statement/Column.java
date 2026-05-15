package com.flipkart.krystal.vajram.ext.sql.statement;

/**
 * Annotation to mark a field in a @{@link SELECT} interface as a column in the SELECT statement. If
 * this annotation is not used, then the model method name is used as the column name. If this
 * annotation is used on a method whose name is different thatn the column name, then the method
 * name is considered as the alias for the column.
 */
public @interface Column {
  String value();
}
