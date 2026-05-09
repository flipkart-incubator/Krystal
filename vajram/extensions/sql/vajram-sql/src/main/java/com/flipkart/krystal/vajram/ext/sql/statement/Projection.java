package com.flipkart.krystal.vajram.ext.sql.statement;

import static java.lang.annotation.ElementType.TYPE;

import com.flipkart.krystal.vajram.ext.sql.model.TableModel;
import java.lang.annotation.Target;

@Target(TYPE)
public @interface Projection {
  Class<? extends TableModel> over();
}
