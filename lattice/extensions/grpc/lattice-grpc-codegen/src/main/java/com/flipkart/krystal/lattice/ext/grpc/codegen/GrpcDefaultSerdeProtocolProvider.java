package com.flipkart.krystal.lattice.ext.grpc.codegen;

import static com.flipkart.krystal.lattice.ext.grpc.codegen.GrpcProtocolResolver.resolveResponseProtocol;

import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.flipkart.krystal.lattice.codegen.spi.DefaultSerdeProtocolProvider;
import com.flipkart.krystal.lattice.ext.grpc.GrpcServer;
import com.flipkart.krystal.vajram.protobuf.util.ProtobufProtocol;
import com.google.auto.service.AutoService;
import javax.lang.model.element.TypeElement;
import org.checkerframework.checker.nullness.qual.Nullable;

@AutoService(DefaultSerdeProtocolProvider.class)
public final class GrpcDefaultSerdeProtocolProvider implements DefaultSerdeProtocolProvider {

  @Override
  public @Nullable TypeElement getDefaultSerializationProtocol(LatticeCodegenContext context) {
    TypeElement latticeAppElement = context.latticeAppTypeElement();
    GrpcServer grpcServer = latticeAppElement.getAnnotation(GrpcServer.class);
    if (grpcServer == null) {
      return null;
    }
    ProtobufProtocol protocol =
        resolveResponseProtocol(grpcServer, context.codeGenUtility(), latticeAppElement);
    if (protocol == null) {
      return null;
    }
    return context
        .codeGenUtility()
        .processingEnv()
        .getElementUtils()
        .getTypeElement(protocol.getClass().getCanonicalName());
  }
}
