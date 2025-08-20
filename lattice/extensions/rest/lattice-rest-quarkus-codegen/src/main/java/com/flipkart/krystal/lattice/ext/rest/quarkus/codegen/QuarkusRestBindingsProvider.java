package com.flipkart.krystal.lattice.ext.rest.quarkus.codegen;

import static com.flipkart.krystal.lattice.codegen.spi.BindingsProvider.BindingScope.REQUEST;
import static com.flipkart.krystal.lattice.core.headers.StandardHeaderNames.ACCEPT;

import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.flipkart.krystal.lattice.codegen.spi.BindingsProvider;
import com.flipkart.krystal.lattice.core.headers.Header;
import com.flipkart.krystal.lattice.core.headers.SingleValueHeader;
import com.flipkart.krystal.lattice.core.headers.StandardHeaderNames;
import com.flipkart.krystal.lattice.rest.RestService;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import io.vertx.ext.web.RoutingContext;
import jakarta.inject.Named;
import java.util.List;
import javax.lang.model.element.TypeElement;

@AutoService(BindingsProvider.class)
public final class QuarkusRestBindingsProvider implements BindingsProvider {
  @Override
  public ImmutableList<Binding> bindings(LatticeCodegenContext context) {
    TypeElement latticeAppElem = context.latticeAppTypeElement();
    RestService restService = latticeAppElem.getAnnotation(RestService.class);
    if (restService == null) {
      return ImmutableList.of();
    }
    return ImmutableList.of(acceptHeaderBinding(), routingContextBinding());
  }

  private Binding routingContextBinding() {
    return new SimpleBinding(
        ClassName.get(RoutingContext.class),
        null,
        new BindTo.Provider(CodeBlock.of("null"), REQUEST));
  }

  private Binding acceptHeaderBinding() {
    return new ProviderMethod(
        "acceptHttpHeader",
        TypeName.get(Header.class)
            .annotated(
                AnnotationSpec.builder(Named.class).addMember("value", "$S", ACCEPT).build()),
        List.of(ParameterSpec.builder(RoutingContext.class, "routingContext")),
        CodeBlock.of(
            "return new $T($T.ACCEPT, routingContext.request().getHeader($T.ACCEPT));",
            SingleValueHeader.class,
            StandardHeaderNames.class,
            StandardHeaderNames.class),
        REQUEST);
  }
}
