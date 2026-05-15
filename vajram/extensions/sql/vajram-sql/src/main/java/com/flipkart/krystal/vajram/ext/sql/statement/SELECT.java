package com.flipkart.krystal.vajram.ext.sql.statement;

import static java.lang.annotation.ElementType.TYPE;

import com.flipkart.krystal.model.Model;
import java.lang.annotation.Target;

/**
 * Annotation to mark a @{@link Model} interface as an SQL SELECT statement. Each field in the model
 * corresponds to a column or alias in the SELECT statement.
 */
@Target(TYPE)
public @interface SELECT {}
