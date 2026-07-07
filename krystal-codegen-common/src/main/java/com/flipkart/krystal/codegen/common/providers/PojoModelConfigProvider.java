package com.flipkart.krystal.codegen.common.providers;

import static com.flipkart.krystal.model.PlainJavaObject.POJO;

import com.flipkart.krystal.codegen.common.spi.ModelProtocolConfigProvider;
import com.google.auto.service.AutoService;

@AutoService(ModelProtocolConfigProvider.class)
public class PojoModelConfigProvider implements ModelProtocolConfigProvider {

  @Override
  public ModelProtocolConfig getConfig() {
    return () -> POJO;
  }
}
