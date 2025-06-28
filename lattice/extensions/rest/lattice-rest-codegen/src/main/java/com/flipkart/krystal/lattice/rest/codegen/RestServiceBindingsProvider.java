package com.flipkart.krystal.lattice.rest.codegen;

import static com.flipkart.krystal.lattice.codegen.spi.BindingsProvider.BindingScope.REQUEST;

import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.flipkart.krystal.lattice.codegen.LatticeCodegenUtils;
import com.flipkart.krystal.lattice.codegen.spi.BindingsProvider;
import com.flipkart.krystal.lattice.rest.RestService;
import com.flipkart.krystal.lattice.rest.RestServiceDopant;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import jakarta.ws.rs.core.UriInfo;
import java.net.http.HttpHeaders;
import javax.lang.model.element.TypeElement;

@AutoService(BindingsProvider.class)
public final class RestServiceBindingsProvider implements BindingsProvider {
  @Override
  public ImmutableList<Binding> bindings(LatticeCodegenContext context) {
    TypeElement latticeAppElem = context.latticeAppTypeElement();
    RestService restService = latticeAppElem.getAnnotation(RestService.class);
    if (restService == null) {
      return ImmutableList.of();
    }
    return ImmutableList.of(
        restServerDopantBinding(context), httpHeadersBinding(), uriInfoBinding());
  }

  private DopantBinding restServerDopantBinding(LatticeCodegenContext context) {
    LatticeCodegenUtils latticeCodegenUtils =
        new LatticeCodegenUtils(context.codeGenUtility().codegenUtil());
    TypeElement latticeAppElem = context.latticeAppTypeElement();
    return new DopantBinding(
        ClassName.get(RestServiceDopant.class),
        latticeCodegenUtils.getDopantImplName(latticeAppElem, RestServiceDopant.class));
  }

  private Binding httpHeadersBinding() {
    return new SimpleBinding(
        ClassName.get(HttpHeaders.class), null, new BindTo.Provider(CodeBlock.of("null"), REQUEST));
  }

  private Binding uriInfoBinding() {
    return new SimpleBinding(
        ClassName.get(UriInfo.class), null, new BindTo.Provider(CodeBlock.of("null"), REQUEST));
  }
}
