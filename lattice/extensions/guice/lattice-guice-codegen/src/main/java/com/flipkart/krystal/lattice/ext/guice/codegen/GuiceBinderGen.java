package com.flipkart.krystal.lattice.ext.guice.codegen;

import static java.util.Map.entry;
import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.flipkart.krystal.lattice.codegen.spi.DepInjectBinderGen;
import com.flipkart.krystal.lattice.core.LatticeApplication;
import com.flipkart.krystal.lattice.ext.guice.GuiceModuleBinder;
import com.flipkart.krystal.lattice.ext.guice.servlet.GuiceServletModuleBinder;
import com.google.auto.service.AutoService;
import com.google.inject.servlet.RequestScoped;
import com.squareup.javapoet.CodeBlock;
import java.util.Map;
import java.util.Optional;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import org.checkerframework.checker.nullness.qual.NonNull;

@AutoService(DepInjectBinderGen.class)
public final class GuiceBinderGen implements DepInjectBinderGen {

  @Override
  public CodeBlock getBinderCreationCode(LatticeCodegenContext context) {
    TypeElement dependencyInjectionBinder = getDependencyInjectionBinder(context);

    if (!isApplicable(context)) {
      throw new UnsupportedOperationException();
    }
    return CodeBlock.builder()
        .addNamed(
            """
return new $guiceModuleBinder:T(
    $customBinderCreator:L
    new $abstractModule:T());
""",
            Map.ofEntries(
                entry(
                    "customBinderCreator",
                    userDefinedDepInjectBinderMethod(context).isPresent()
                        ? CodeBlock.of("super.getDependencyInjectionBinder().getRootModule(),")
                        : CodeBlock.builder().build()),
                entry("guiceModuleBinder", dependencyInjectionBinder),
                entry("abstractModule", GuiceModuleGenProvider.getModuleClassName(context))))
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
                                context.latticeApp()::dependencyInjectionBinder)
                            .orElseThrow(() -> new AssertionError("Not possible"))));
    return dependencyInjectionBinder;
  }

  @Override
  public boolean isApplicable(LatticeCodegenContext context) {
    return isGuiceBinderConfigured(context);
  }

  static boolean isGuiceBinderConfigured(LatticeCodegenContext context) {
    return getDependencyInjectionBinder(context)
            .equals(
                context
                    .codeGenUtility()
                    .processingEnv()
                    .getElementUtils()
                    .getTypeElement(GuiceModuleBinder.class.getCanonicalName()))
        || getDependencyInjectionBinder(context)
            .equals(
                context
                    .codeGenUtility()
                    .processingEnv()
                    .getElementUtils()
                    .getTypeElement(GuiceServletModuleBinder.class.getCanonicalName()));
  }

  @Override
  public CodeBlock getRequestScope() {
    return CodeBlock.of("@$T", RequestScoped.class);
  }

  private Optional<ExecutableElement> userDefinedDepInjectBinderMethod(
      LatticeCodegenContext context) {
    TypeElement typeElement = context.latticeAppTypeElement();
    CodeGenUtility util = context.codeGenUtility().codegenUtil();
    try {
      return util.getMethod(
          typeElement, LatticeApplication.class.getMethod("getDependencyInjectionBinder").getName(), 0);
    } catch (Exception e) {
      throw util.errorAndThrow(
          "Application class has no 'getDependencyInjectionBinder' method. "
              + "This should not happen. Possibly there are incompatible versions in the classpath.");
    }
  }
}
