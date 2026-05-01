package com.flipkart.krystal.lattice.ext.protobuf.codegen;

import static com.flipkart.krystal.vajram.protobuf2024e.Protobuf2024e.PROTOBUF_2024E;

import com.flipkart.krystal.codegen.common.spi.ModelProtocolConfigProvider;
import com.google.auto.service.AutoService;

@AutoService(ModelProtocolConfigProvider.class)
public class Protobuf2024eConfigProvider implements ModelProtocolConfigProvider {
  @Override
  public ModelProtocolConfig getConfig() {
    return new ModelProtocolConfig(PROTOBUF_2024E);
  }
}
