package com.flipkart.krystal.lattice.ext.grpc.codegen;

import static com.flipkart.krystal.datatypes.Trilean.TRUE;
import static java.util.Objects.requireNonNull;

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
      context
          .codeGenUtility()
          .codegenUtil()
          .getTypesFromAnnotationMember(service::rpcVajrams)
          .stream()
          .map(
              tm ->
                  (TypeElement)
                      requireNonNull(
                          context.codeGenUtility().processingEnv().getTypeUtils().asElement(tm)))
          .forEach(results::add);
    }
    return ImmutableList.copyOf(results);
  }
}
