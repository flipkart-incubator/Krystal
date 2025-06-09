package com.flipkart.krystal.lattice.ext.grpc.codegen;

import com.flipkart.krystal.lattice.codegen.spi.DefaultSerdeProtocolProvider;
import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.flipkart.krystal.lattice.ext.grpc.GrpcServer;
import com.flipkart.krystal.vajram.protobuf3.Protobuf3;
import com.google.auto.service.AutoService;
import javax.lang.model.element.TypeElement;
import org.checkerframework.checker.nullness.qual.Nullable;

@AutoService(DefaultSerdeProtocolProvider.class)
public final class GrpcDefaultSerdeProtocolProvider implements DefaultSerdeProtocolProvider {

  @Override
  public @Nullable TypeElement getDefaultSerializationProtocol(LatticeCodegenContext context) {
    GrpcServer grpcServer = context.latticeAppTypeElement().getAnnotation(GrpcServer.class);
    if (grpcServer == null) {
      return null;
    }
    return context
        .codeGenUtility()
        .processingEnv()
        .getElementUtils()
        .getTypeElement(Protobuf3.class.getCanonicalName());
  }
}
