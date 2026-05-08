package com.flipkart.krystal.vajram.ext.sql.statement;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.vajram.ext.sql.model.TableModel;

/**
 * Annotation to mark a @{@link Model} interface as one containing one or more 'AND'ed WHERE clause
 * conditions.
 */
public @interface WHERE {
  Class<? extends TableModel> inTable();
}
