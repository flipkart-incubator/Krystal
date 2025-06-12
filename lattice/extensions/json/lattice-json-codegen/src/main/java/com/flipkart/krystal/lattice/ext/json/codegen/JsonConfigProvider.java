package com.flipkart.krystal.lattice.ext.json.codegen;

import static com.flipkart.krystal.vajram.json.Json.JSON;

import com.flipkart.krystal.codegen.common.spi.ModelProtocolConfigProvider;
import com.google.auto.service.AutoService;

@AutoService(ModelProtocolConfigProvider.class)
public class JsonConfigProvider implements ModelProtocolConfigProvider {
  @Override
  public ModelProtocolConfig getConfig() {
    return new ModelProtocolConfig(JSON);
  }
}
