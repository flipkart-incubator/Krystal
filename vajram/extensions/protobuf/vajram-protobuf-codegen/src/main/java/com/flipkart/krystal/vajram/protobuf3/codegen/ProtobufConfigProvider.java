package com.flipkart.krystal.vajram.protobuf3.codegen;

import static com.flipkart.krystal.vajram.protobuf3.Protobuf3.PROTO_SUFFIX;

import com.flipkart.krystal.codegen.common.spi.ModelProtocolConfigProvider;
import com.flipkart.krystal.codegen.common.spi.ModelProtocolConfigProvider.ModelProtocolConfig;
import com.flipkart.krystal.lattice.core.headers.StandardHeaders.AcceptHeaders;
import com.flipkart.krystal.vajram.protobuf3.Protobuf3;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.CodeBlock;

@AutoService(ModelProtocolConfigProvider.class)
public class ProtobufConfigProvider implements ModelProtocolConfigProvider {
  @Override
  public ModelProtocolConfig getConfig() {
    return new ModelProtocolConfig(
        Protobuf3.class, PROTO_SUFFIX, CodeBlock.of("$S", AcceptHeaders.PROTOBUF.value()));
  }
}
