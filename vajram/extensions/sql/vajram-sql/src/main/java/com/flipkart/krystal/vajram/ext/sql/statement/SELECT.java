package com.flipkart.krystal.vajram.ext.sql.statement;

import com.flipkart.krystal.model.Model;

/**
 * Annotation to mark a @{@link Model} interface as an SQL SELECT statement. Each field in the model
 * corresponds to a column or alias in the SELECT statement.
 */
public @interface SELECT {}
