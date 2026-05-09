package com.flipkart.krystal.vajram.ext.sql.statement;

import static java.lang.annotation.ElementType.TYPE;

import com.flipkart.krystal.vajram.ext.sql.model.TableModel;
import java.lang.annotation.Target;

/**
 * Represents a selection of columns from a table. This is the primary way a developer using
 * vajram-sql declares the shape of the data that they want to SELECT/fetch from the database. Each
 * Selection selects a set of columns (as-is or via aliases) from a table.
 */
@Target(TYPE)
public @interface Selection {

  /** The table from which the columns need to be selection. */
  Class<? extends TableModel> from();
}
