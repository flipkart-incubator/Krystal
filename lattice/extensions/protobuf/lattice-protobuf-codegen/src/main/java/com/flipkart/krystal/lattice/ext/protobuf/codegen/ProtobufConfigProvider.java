package com.flipkart.krystal.lattice.ext.protobuf.codegen;

import com.flipkart.krystal.codegen.common.spi.ModelProtocolConfigProvider;
import com.flipkart.krystal.lattice.ext.protobuf.LatticeProtoConstants;
import com.flipkart.krystal.vajram.protobuf3.Protobuf3;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.CodeBlock;

@AutoService(ModelProtocolConfigProvider.class)
public class ProtobufConfigProvider implements ModelProtocolConfigProvider {
  @Override
  public ModelProtocolConfig getConfig() {
    return new ModelProtocolConfig(
        Protobuf3.class,
        Protobuf3.PROTO_SUFFIX,
        CodeBlock.of("$S", LatticeProtoConstants.PROTOBUF_CONTENT_TYPE));
  }
}
