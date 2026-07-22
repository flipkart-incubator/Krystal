package com.flipkart.krystal.vajram.ext.sql.lang;

import static java.lang.annotation.ElementType.TYPE;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.vajram.ext.sql.model.SelectionModel;
import com.flipkart.krystal.vajram.ext.sql.model.TableModel;
import java.lang.annotation.Target;

/**
 * Annotation to mark a @{@link Model} interface as one containing one or more 'AND'ed WHERE clause
 * conditions.
 */
@Target(TYPE)
public @interface WHERE {
  Class<? extends TableModel> inTable() default TableModel.class;

  Class<? extends SelectionModel> forSelection() default SelectionModel.class;
}
