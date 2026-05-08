package com.flipkart.krystal.vajram.ext.sql.statement;

import com.flipkart.krystal.vajram.ext.sql.model.TableModel;

public @interface Projection {
  Class<? extends TableModel> over();
}
