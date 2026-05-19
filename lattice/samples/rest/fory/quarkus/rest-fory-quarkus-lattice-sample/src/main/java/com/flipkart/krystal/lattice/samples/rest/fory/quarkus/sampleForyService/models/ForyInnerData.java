package com.flipkart.krystal.lattice.samples.rest.fory.quarkus.sampleForyService.models;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.model.ModelRoot.ModelType.REQUEST;
import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.vajram.fory.Fory;
import com.flipkart.krystal.vajram.json.Json;

@ModelRoot(type = {REQUEST, RESPONSE})
@SupportedModelProtocols({PlainJavaObject.class, Json.class, Fory.class})
public interface ForyInnerData extends Model {
  @IfAbsent(FAIL)
  String value();

  @IfAbsent(FAIL)
  int count();
}
