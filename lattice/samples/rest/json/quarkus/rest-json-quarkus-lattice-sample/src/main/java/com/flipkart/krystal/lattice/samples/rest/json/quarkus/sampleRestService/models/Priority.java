package com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.models;

import com.flipkart.krystal.model.EnumModel;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.vajram.json.Json;

@ModelRoot
@SupportedModelProtocols({PlainJavaObject.class, Json.class})
public enum Priority implements EnumModel {
  UNKNOWN,
  LOW,
  MEDIUM,
  HIGH,
}
