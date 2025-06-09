package com.flipkart.krystal.lattice.ext.rest.quarkus.codegen;

import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.flipkart.krystal.lattice.codegen.spi.DefaultSerdeProtocolProvider;
import com.flipkart.krystal.vajram.json.Json;
import com.google.auto.service.AutoService;
import flipkart.krystal.lattice.ext.rest.quarkus.restServer.RestService;
import javax.lang.model.element.TypeElement;
import org.checkerframework.checker.nullness.qual.Nullable;

@AutoService(DefaultSerdeProtocolProvider.class)
public final class QuarkusRestDefaultSerdeProtocolProvider implements DefaultSerdeProtocolProvider {

  @Override
  public @Nullable TypeElement getDefaultSerializationProtocol(LatticeCodegenContext context) {
    RestService grpcServer = context.latticeAppTypeElement().getAnnotation(RestService.class);
    if (grpcServer == null) {
      return null;
    }
    return context
        .codeGenUtility()
        .processingEnv()
        .getElementUtils()
        .getTypeElement(Json.class.getCanonicalName());
  }
}
