package com.flipkart.krystal.lattice.ext.grpc.codegen;

import static com.flipkart.krystal.datatypes.Trilean.TRUE;

import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.flipkart.krystal.lattice.codegen.spi.LatticeAppCodeGenAttrsProvider;
import com.flipkart.krystal.lattice.ext.grpc.GrpcServer;
import com.flipkart.krystal.lattice.ext.grpc.GrpcService;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.TypeElement;

@AutoService(LatticeAppCodeGenAttrsProvider.class)
public final class GrpcLinkedVajramsProvider implements LatticeAppCodeGenAttrsProvider {

  @Override
  public LatticeAppCodeGenAttributes get(LatticeCodegenContext context) {
    return LatticeAppCodeGenAttributes.builder()
        .needsRequestScopedHeaders(TRUE)
        .remotelyInvocableVajrams(getRemotelyInvokedVajrams(context))
        .build();
  }

  private static ImmutableList<TypeElement> getRemotelyInvokedVajrams(
      LatticeCodegenContext context) {
    GrpcServer grpcServer = context.latticeAppTypeElement().getAnnotation(GrpcServer.class);
    if (grpcServer == null) {
      return ImmutableList.of();
    }
    GrpcService[] services = grpcServer.services();
    List<TypeElement> results = new ArrayList<>();
    for (GrpcService service : services) {
      results.addAll(
          context
              .codeGenUtility()
              .codegenUtil()
              .getTypeElemsFromAnnotationMember(service::rpcVajrams));
    }
    return ImmutableList.copyOf(results);
  }
}
