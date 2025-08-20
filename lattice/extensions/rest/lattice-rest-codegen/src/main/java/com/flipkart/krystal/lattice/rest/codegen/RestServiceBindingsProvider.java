package com.flipkart.krystal.lattice.rest.codegen;

import static com.flipkart.krystal.codegen.common.models.CodeGenUtility.lowerCaseFirstChar;
import static com.flipkart.krystal.lattice.codegen.spi.BindingsProvider.BindingScope.REQUEST;
import static com.flipkart.krystal.lattice.codegen.spi.BindingsProvider.BindingScope.SINGLETON;
import static com.flipkart.krystal.lattice.rest.codegen.JakartaRestServiceResourceGenProvider.resourceClasses;

import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.flipkart.krystal.lattice.codegen.LatticeCodegenUtils;
import com.flipkart.krystal.lattice.codegen.spi.BindingsProvider;
import com.flipkart.krystal.lattice.rest.JakartaRestResources;
import com.flipkart.krystal.lattice.rest.RestService;
import com.flipkart.krystal.lattice.rest.RestServiceDopant;
import com.google.auto.service.AutoService;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterSpec;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;
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
        restServerDopantBinding(context),
        httpHeadersBinding(),
        uriInfoBinding(),
        jakartaResources(context));
  }

  private Binding jakartaResources(LatticeCodegenContext context) {
    List<ClassName> resourceClassNames = resourceClasses(context);
    BiMap<ClassName, String> paramNames = HashBiMap.create();
    for (ClassName resourceClassName : resourceClassNames) {
      String prefix = lowerCaseFirstChar(resourceClassName.simpleName());
      String paramName = prefix;
      int i = 1;
      while (paramNames.containsValue(paramName)) {
        paramName = prefix + "_" + i;
        i++;
      }
      paramNames.put(resourceClassName, paramName);
    }
    return new ProviderMethod(
        "jakartaRestResources",
        ClassName.get(JakartaRestResources.class),
        paramNames.entrySet().stream()
            .map(e -> ParameterSpec.builder(e.getKey(), e.getValue()))
            .toList(),
        CodeBlock.of(
            "new $T($L);",
            JakartaRestResources.class,
            paramNames.values().stream()
                .map(c -> CodeBlock.of("$L", c))
                .collect(CodeBlock.joining(", "))),
        SINGLETON);
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
