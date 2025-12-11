package com.flipkart.krystal.lattice.ext.rest.codegen;

import static com.flipkart.krystal.lattice.codegen.spi.di.BindingScope.StandardBindingScope.LAZY_SINGLETON;
import static com.flipkart.krystal.lattice.codegen.spi.di.BindingScope.StandardBindingScope.REQUEST;

import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.flipkart.krystal.lattice.codegen.LatticeCodegenUtils;
import com.flipkart.krystal.lattice.codegen.spi.di.Binding;
import com.flipkart.krystal.lattice.codegen.spi.di.BindingsContainer;
import com.flipkart.krystal.lattice.codegen.spi.di.BindingsProvider;
import com.flipkart.krystal.lattice.codegen.spi.di.ImplTypeBinding;
import com.flipkart.krystal.lattice.codegen.spi.di.NullBinding;
import com.flipkart.krystal.lattice.core.headers.Header;
import com.flipkart.krystal.lattice.core.headers.StandardHeaderNames;
import com.flipkart.krystal.lattice.ext.rest.RestService;
import com.flipkart.krystal.lattice.ext.rest.RestServiceDopant;
import com.flipkart.krystal.tags.Names;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import jakarta.inject.Named;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;
import javax.lang.model.element.TypeElement;

@AutoService(BindingsProvider.class)
public final class RestServiceBindingsProvider implements BindingsProvider {
  @Override
  public ImmutableList<BindingsContainer> bindings(LatticeCodegenContext context) {
    TypeElement latticeAppElem = context.latticeAppTypeElement();
    RestService restService = latticeAppElem.getAnnotation(RestService.class);
    if (restService == null) {
      return ImmutableList.of();
    }
    return ImmutableList.of(
        new BindingsContainer(
            ImmutableList.of(
                restServerDopantBinding(context),
                httpHeadersBinding(),
                uriInfoBinding(),
                bindAcceptHeaderInRequestScope())));
  }

  private ImplTypeBinding restServerDopantBinding(LatticeCodegenContext context) {
    LatticeCodegenUtils latticeCodegenUtils =
        new LatticeCodegenUtils(context.codeGenUtility().codegenUtil());
    TypeElement latticeAppElem = context.latticeAppTypeElement();
    return new ImplTypeBinding(
        ClassName.get(RestServiceDopant.class),
        latticeCodegenUtils.getDopantImplName(latticeAppElem, RestServiceDopant.class),
        LAZY_SINGLETON,
        true);
  }

  private Binding httpHeadersBinding() {
    return new NullBinding(ClassName.get(HttpHeaders.class), null, null, REQUEST);
  }

  private Binding uriInfoBinding() {
    return new NullBinding(ClassName.get(UriInfo.class), null, null, REQUEST);
  }

  private static Binding bindAcceptHeaderInRequestScope() {
    return new NullBinding(
        ClassName.get(Header.class),
        CodeBlock.of("$T.$L($T.$L)", Names.class, "named", StandardHeaderNames.class, "ACCEPT"),
        AnnotationSpec.builder(Named.class)
            .addMember("value", "$L", CodeBlock.of("$T.$L", StandardHeaderNames.class, "ACCEPT"))
            .build(),
        REQUEST);
  }
}
