package com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause;

import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.model.array.FloatArray;
import com.flipkart.krystal.vajram.ext.sql.model.Selection;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.model.Embedding;

@ModelRoot(type = RESPONSE)
@SupportedModelProtocol(PlainJavaObject.class)
@Selection(from = Embedding.class)
public interface EmbeddingInfo extends Model {

  long id();

  FloatArray embeddingValues();
}
