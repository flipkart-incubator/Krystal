package com.flipkart.krystal.lattice.ext.cdi.codegen;

import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.flipkart.krystal.lattice.codegen.spi.di.DepInjectBinderGen;
import com.flipkart.krystal.lattice.core.LatticeApplication;
import com.flipkart.krystal.lattice.ext.cdi.CdiFramework;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.CodeBlock;
import java.util.Optional;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import org.checkerframework.checker.nullness.qual.NonNull;

@AutoService(DepInjectBinderGen.class)
public final class CdiBinderGen implements DepInjectBinderGen {

  @Override
  public CodeBlock getBinderCreationCode(LatticeCodegenContext context) {
    if (!isApplicable(context)) {
      throw new UnsupportedOperationException();
    }
    return CodeBlock.builder()
        .addStatement("return new $T()", getDependencyInjectionBinder(context))
        .build();
  }

  static @NonNull TypeElement getDependencyInjectionBinder(LatticeCodegenContext context) {
    CodeGenUtility util = context.codeGenUtility().codegenUtil();

    TypeElement dependencyInjectionBinder =
        (TypeElement)
            requireNonNull(
                util.processingEnv()
                    .getTypeUtils()
                    .asElement(
                        util.getTypeFromAnnotationMember(
                            context.latticeApp()::dependencyInjectionFramework)));
    return dependencyInjectionBinder;
  }

  @Override
  public boolean isApplicable(LatticeCodegenContext context) {
    boolean isCdiApp =
        getDependencyInjectionBinder(context)
            .equals(
                context
                    .codeGenUtility()
                    .processingEnv()
                    .getElementUtils()
                    .getTypeElement(CdiFramework.class.getCanonicalName()));
    if (isCdiApp) {
      userDefinedDepInjectorMethod(context)
          .ifPresent(
              userDefinedDepInjectBinderMethod ->
                  context
                      .codeGenUtility()
                      .codegenUtil()
                      .error(
                          "CDI doesn't support user defined dependency injection binder",
                          userDefinedDepInjectBinderMethod));
    }
    return isCdiApp;
  }

  private Optional<ExecutableElement> userDefinedDepInjectorMethod(LatticeCodegenContext context) {
    TypeElement typeElement = context.latticeAppTypeElement();
    CodeGenUtility util = context.codeGenUtility().codegenUtil();
    try {
      return util.getMethod(
          typeElement, LatticeApplication.class.getMethod("getDependencyInjector").getName(), 0);
    } catch (Exception e) {
      throw util.errorAndThrow(
          "LatticeApplication.class has no 'getDependencyInjectionBinder' method. "
              + "This should not happen. Possibly there are incompatible versions in the classpath.");
    }
  }
}
