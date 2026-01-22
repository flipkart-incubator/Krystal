package com.flipkart.krystal.lattice.samples.mcp.quarkus.models;

import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.vajram.json.Json;

@ModelRoot(type = RESPONSE)
@SupportedModelProtocols({PlainJavaObject.class, Json.class})
public interface MultiplicationTable extends Model {
  int times1();

  int times2();

  int times3();

  int times4();

  int times5();

  int times6();

  int times7();

  int times8();

  int times9();

  int times10();
}
