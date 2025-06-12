package com.flipkart.krystal.lattice.ext.protobuf.codegen;

import static com.flipkart.krystal.vajram.protobuf3.Protobuf3.PROTOBUF_3;

import com.flipkart.krystal.codegen.common.spi.ModelProtocolConfigProvider;
import com.google.auto.service.AutoService;

@AutoService(ModelProtocolConfigProvider.class)
public class ProtobufConfigProvider implements ModelProtocolConfigProvider {
  @Override
  public ModelProtocolConfig getConfig() {
    return new ModelProtocolConfig(PROTOBUF_3);
  }
}
