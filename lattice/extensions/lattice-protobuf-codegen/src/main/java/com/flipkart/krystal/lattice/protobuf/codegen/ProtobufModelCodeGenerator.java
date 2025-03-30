package com.flipkart.krystal.lattice.protobuf.codegen;

import com.flipkart.krystal.ext.protobuf.Protobuf;
import com.flipkart.krystal.lattice.annotations.RemoteInvocation;
import com.flipkart.krystal.vajram.codegen.common.models.Utils;
import com.flipkart.krystal.vajram.codegen.common.spi.CodeGeneratorCreationContext;
import com.flipkart.krystal.vajram.codegen.common.spi.VajramCodeGenerator;
import java.util.Arrays;
import javax.lang.model.element.TypeElement;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class ProtobufModelCodeGenerator implements VajramCodeGenerator {

  private final CodeGeneratorCreationContext creationContext;
  private final Utils util;

  public ProtobufModelCodeGenerator(CodeGeneratorCreationContext creationContext) {
    this.creationContext = creationContext;
    this.util = creationContext.util();
  }

  @Override
  public void generate() {
    TypeElement vajramClass = creationContext.vajramInfo().vajramClass();
    RemoteInvocation remoteInvocation = vajramClass.getAnnotation(RemoteInvocation.class);
    if (remoteInvocation == null || !remoteInvocation.allow()) {
      log.debug(
          "Skipping class '{}' since remote invocation is not enabled",
          vajramClass.getQualifiedName());
      return;
    }
    if (Arrays.stream(remoteInvocation.serializationProtocols())
        .noneMatch(Protobuf.class::equals)) {
      log.debug(
          "Skipping class '{}' since Protobuf is not one of the intended serialization protocols : {} ",
          vajramClass.getQualifiedName(),
          remoteInvocation.serializationProtocols());
      return;
    }
  }
}
