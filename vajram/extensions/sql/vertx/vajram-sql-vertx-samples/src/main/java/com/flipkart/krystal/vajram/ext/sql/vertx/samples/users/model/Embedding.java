package com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.model;

import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.array.FloatArray;
import com.flipkart.krystal.vajram.ext.sql.model.PrimaryKey;
import com.flipkart.krystal.vajram.ext.sql.model.Table;
import com.flipkart.krystal.vajram.ext.sql.model.TableModel;

@ModelRoot
@Table(name = "EmbeddingEntity")
public interface Embedding extends TableModel {

  @PrimaryKey
  long id();

  FloatArray embeddingValues();
}
