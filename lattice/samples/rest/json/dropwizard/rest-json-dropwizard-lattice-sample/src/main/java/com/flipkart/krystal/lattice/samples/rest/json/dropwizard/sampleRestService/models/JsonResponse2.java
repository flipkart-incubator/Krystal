package com.flipkart.krystal.lattice.samples.rest.json.dropwizard.sampleRestService.models;

import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.vajram.json.Json;

@ModelRoot(type = RESPONSE)
@SupportedModelProtocol(PlainJavaObject.class)
@SupportedModelProtocol(Json.class)
// No @DefaultSerdeProtocol - needs explicit content type in accept header
public interface JsonResponse2 extends Model {
  String responseValue();
}
