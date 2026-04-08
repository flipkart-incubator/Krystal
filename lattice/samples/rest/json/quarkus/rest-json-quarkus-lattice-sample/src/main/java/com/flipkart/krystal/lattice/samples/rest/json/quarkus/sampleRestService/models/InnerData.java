package com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.models;

import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.vajram.json.Json;

@ModelRoot(type = RESPONSE)
@SupportedModelProtocols({PlainJavaObject.class, Json.class})
public interface InnerData extends Model {
  String value();

  int count();
}
