package com.flipkart.krystal.vajram.ext.fory.codegen;

import static com.flipkart.krystal.vajram.fory.Fory.FORY;

import com.flipkart.krystal.codegen.common.spi.ModelProtocolConfigProvider;
import com.google.auto.service.AutoService;

@AutoService(ModelProtocolConfigProvider.class)
public class ForyConfigProvider implements ModelProtocolConfigProvider {
  @Override
  public ModelProtocolConfig getConfig() {
    return () -> FORY;
  }
}
