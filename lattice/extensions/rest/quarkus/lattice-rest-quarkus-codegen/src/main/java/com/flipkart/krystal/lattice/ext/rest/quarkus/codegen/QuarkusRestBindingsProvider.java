package com.flipkart.krystal.lattice.ext.rest.quarkus.codegen;

import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.flipkart.krystal.lattice.codegen.spi.di.Binding;
import com.flipkart.krystal.lattice.codegen.spi.di.BindingsContainer;
import com.flipkart.krystal.lattice.codegen.spi.di.BindingsProvider;
import com.flipkart.krystal.lattice.codegen.spi.di.NullBinding;
import com.flipkart.krystal.lattice.ext.rest.RestService;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.RequestScoped;
import javax.lang.model.element.TypeElement;

@AutoService(BindingsProvider.class)
public final class QuarkusRestBindingsProvider implements BindingsProvider {
  @Override
  public ImmutableList<BindingsContainer> bindings(LatticeCodegenContext context) {
    TypeElement latticeAppElem = context.latticeAppTypeElement();
    RestService restService = latticeAppElem.getAnnotation(RestService.class);
    if (restService == null) {
      return ImmutableList.of();
    }
    return ImmutableList.of(new BindingsContainer(ImmutableList.of(routingContextBinding())));
  }

  private Binding routingContextBinding() {
    return new NullBinding(
        ClassName.get(RoutingContext.class),
        null,
        null,
        AnnotationSpec.builder(RequestScoped.class).build());
  }
}
