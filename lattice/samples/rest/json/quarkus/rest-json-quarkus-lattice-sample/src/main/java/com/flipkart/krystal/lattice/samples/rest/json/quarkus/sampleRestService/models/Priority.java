package com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.models;

import com.flipkart.krystal.model.DefaultValue;
import com.flipkart.krystal.model.EnumModel;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.vajram.json.Json;

@ModelRoot
@SupportedModelProtocol(PlainJavaObject.class)
@SupportedModelProtocol(Json.class)
public enum Priority implements EnumModel {
  @DefaultValue
  UNKNOWN,
  LOW,
  MEDIUM,
  HIGH,
}
