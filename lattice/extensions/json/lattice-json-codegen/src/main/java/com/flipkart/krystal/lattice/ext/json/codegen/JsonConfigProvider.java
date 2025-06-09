package com.flipkart.krystal.lattice.ext.json.codegen;

import com.flipkart.krystal.codegen.common.spi.ModelProtocolConfigProvider;
import com.flipkart.krystal.vajram.json.Json;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.CodeBlock;

@AutoService(ModelProtocolConfigProvider.class)
public class JsonConfigProvider implements ModelProtocolConfigProvider {
  @Override
  public ModelProtocolConfig getConfig() {
    return new ModelProtocolConfig(
        Json.class, Json.JSON_SUFFIX, CodeBlock.of("$S", "application/json"));
  }
}
