package com.flipkart.krystal.lattice.ext.rest.quarkus.codegen;

import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.flipkart.krystal.lattice.codegen.LatticeCodegenUtils;
import com.flipkart.krystal.lattice.codegen.spi.BindingsProvider;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import flipkart.krystal.lattice.ext.rest.quarkus.restServer.QuarkusRestServerDopant;
import flipkart.krystal.lattice.ext.rest.quarkus.restServer.RestService;
import javax.lang.model.element.TypeElement;
import org.checkerframework.checker.nullness.qual.NonNull;

@AutoService(BindingsProvider.class)
public final class QuarkusRestBindingsProvider implements BindingsProvider {
  @Override
  public ImmutableList<Binding> bindings(LatticeCodegenContext context) {
    TypeElement latticeAppElem = context.latticeAppTypeElement();
    RestService restService = latticeAppElem.getAnnotation(RestService.class);
    if (restService == null) {
      return ImmutableList.of();
    }
    return ImmutableList.of(getDopantBinding(context));
  }

  private static @NonNull DopantBinding getDopantBinding(LatticeCodegenContext context) {
    LatticeCodegenUtils latticeCodegenUtils =
        new LatticeCodegenUtils(context.codeGenUtility().codegenUtil());
    TypeElement latticeAppElem = context.latticeAppTypeElement();
    String packageName =
        requireNonNull(
                context
                    .codeGenUtility()
                    .processingEnv()
                    .getElementUtils()
                    .getPackageOf(latticeAppElem))
            .getQualifiedName()
            .toString();
    return new DopantBinding(
        ClassName.get(QuarkusRestServerDopant.class),
        ClassName.get(
            packageName,
            latticeCodegenUtils.getDopantImplName(latticeAppElem, QuarkusRestServerDopant.class)));
  }
}
