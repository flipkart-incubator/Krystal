package com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.models;

import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.vajram.json.Json;
import java.util.List;

@ModelRoot(
    type = {RESPONSE},
    builderExtendsModelRoot = true)
@SupportedModelProtocols({PlainJavaObject.class, Json.class})
public interface InnerDataV2 extends Model {
  String value();

  int count();

  List<InnerData> innerData();
}
