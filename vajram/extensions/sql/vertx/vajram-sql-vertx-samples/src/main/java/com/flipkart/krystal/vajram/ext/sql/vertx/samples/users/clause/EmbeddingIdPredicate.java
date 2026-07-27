package com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause;

import static com.flipkart.krystal.model.ModelRoot.ModelType.REQUEST;

import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.vajram.ext.sql.lang.ColumnPredicate;
import com.flipkart.krystal.vajram.ext.sql.lang.WHERE;
import com.flipkart.krystal.vajram.ext.sql.lang.operators.comparison.IsEqualTo;
import com.flipkart.krystal.vajram.ext.sql.model.Column;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.model.Embedding;

@ModelRoot(type = REQUEST)
@WHERE(inTable = Embedding.class)
public interface EmbeddingIdPredicate extends ColumnPredicate {

  @IsEqualTo
  @Column("id")
  long idIs();

  static EmbeddingIdPredicate_Immut.Builder _builder() {
    return EmbeddingIdPredicate_ImmutPojo._builder();
  }
}
