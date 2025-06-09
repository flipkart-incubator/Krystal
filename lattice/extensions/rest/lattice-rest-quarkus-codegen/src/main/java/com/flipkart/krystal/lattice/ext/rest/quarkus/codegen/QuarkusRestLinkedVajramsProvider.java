package com.flipkart.krystal.lattice.ext.rest.quarkus.codegen;

import static com.flipkart.krystal.datatypes.Trilean.TRUE;
import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.flipkart.krystal.lattice.codegen.spi.LatticeAppCodeGenAttrsProvider;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import flipkart.krystal.lattice.ext.rest.quarkus.restServer.RestService;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.TypeElement;

@AutoService(LatticeAppCodeGenAttrsProvider.class)
public final class QuarkusRestLinkedVajramsProvider implements LatticeAppCodeGenAttrsProvider {

  @Override
  public LatticeAppCodeGenAttributes get(LatticeCodegenContext context) {
    return LatticeAppCodeGenAttributes.builder()
        .needsRequestScopedHeaders(TRUE)
        .remotelyInvocableVajrams(getRemotelyInvocableVajrams(context))
        .build();
  }

  private static ImmutableList<TypeElement> getRemotelyInvocableVajrams(
      LatticeCodegenContext context) {
    RestService restService = context.latticeAppTypeElement().getAnnotation(RestService.class);
    if (restService == null) {
      return ImmutableList.of();
    }

    List<TypeElement> results = new ArrayList<>();

    context
        .codeGenUtility()
        .codegenUtil()
        .getTypesFromAnnotationMember(restService::resourceVajrams)
        .stream()
        .map(
            tm ->
                (TypeElement)
                    requireNonNull(
                        context.codeGenUtility().processingEnv().getTypeUtils().asElement(tm)))
        .forEach(typeElement -> results.add(typeElement));
    return ImmutableList.copyOf(results);
  }
}
