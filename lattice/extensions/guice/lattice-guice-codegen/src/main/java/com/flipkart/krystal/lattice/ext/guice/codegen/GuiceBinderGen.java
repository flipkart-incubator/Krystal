package com.flipkart.krystal.lattice.ext.guice.codegen;

import static com.flipkart.krystal.lattice.codegen.LatticeCodegenUtils.getDiBindingContainerName;
import static com.flipkart.krystal.lattice.codegen.spi.di.BindingsContainer.getBindingContainers;
import static java.util.Map.entry;
import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.flipkart.krystal.lattice.codegen.spi.di.BindingsContainer;
import com.flipkart.krystal.lattice.codegen.spi.di.DepInjectBinderGen;
import com.flipkart.krystal.lattice.core.LatticeApplication;
import com.flipkart.krystal.lattice.ext.guice.GuiceInjectionProvider;
import com.flipkart.krystal.lattice.ext.guice.servlet.GuiceServletInjectionProvider;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.CodeBlock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

@AutoService(DepInjectBinderGen.class)
public final class GuiceBinderGen implements DepInjectBinderGen {

  @Override
  public CodeBlock getBinderCreationCode(LatticeCodegenContext context) {

    if (!isApplicable(context)) {
      throw new UnsupportedOperationException();
    }
    Map<String, List<BindingsContainer>> bindingContainers = getBindingContainers(context);
    return CodeBlock.builder()
        .addNamed(
"""
return new $guiceModuleBinder:T(
    this,
    $customBinderCreator:L
    $autoGenModules:L);
""",
            Map.ofEntries(
                entry(
                    "customBinderCreator",
                    userDefinedDepInjectBinderMethod(context).isPresent()
                        ? CodeBlock.of("super.getDependencyInjectionBinder().getRootModule(),")
                        : CodeBlock.builder().build()),
                entry("guiceModuleBinder", getDependencyInjectionBinder(context)),
                entry(
                    "autoGenModules",
                    bindingContainers.keySet().stream()
                        .map(id -> CodeBlock.of("new $T()", getDiBindingContainerName(context, id)))
                        .collect(CodeBlock.joining(",")))))
        .build();
  }

  static TypeElement getDependencyInjectionBinder(LatticeCodegenContext context) {
    CodeGenUtility util = context.codeGenUtility().codegenUtil();

    TypeElement dependencyInjectionBinder =
        (TypeElement)
            requireNonNull(
                util.processingEnv()
                    .getTypeUtils()
                    .asElement(
                        util.getTypeFromAnnotationMember(
                            context.latticeApp()::dependencyInjectionBinder)));
    return dependencyInjectionBinder;
  }

  @Override
  public boolean isApplicable(LatticeCodegenContext context) {
    return isGuiceBinderConfigured(context);
  }

  static boolean isGuiceBinderConfigured(LatticeCodegenContext context) {
    TypeElement dependencyInjectionBinder = getDependencyInjectionBinder(context);
    return dependencyInjectionBinder.equals(
            context
                .codeGenUtility()
                .processingEnv()
                .getElementUtils()
                .getTypeElement(GuiceInjectionProvider.class.getCanonicalName()))
        || dependencyInjectionBinder.equals(
            context
                .codeGenUtility()
                .processingEnv()
                .getElementUtils()
                .getTypeElement(GuiceServletInjectionProvider.class.getCanonicalName()));
  }

  private Optional<ExecutableElement> userDefinedDepInjectBinderMethod(
      LatticeCodegenContext context) {
    TypeElement typeElement = context.latticeAppTypeElement();
    CodeGenUtility util = context.codeGenUtility().codegenUtil();
    try {
      return util.getMethod(
          typeElement,
          LatticeApplication.class.getMethod("getDependencyInjectionBinder").getName(),
          0);
    } catch (Exception e) {
      throw util.errorAndThrow(
          "Application class has no 'getDependencyInjectionBinder' method. "
              + "This should not happen. Possibly there are incompatible versions in the classpath.");
    }
  }
}
