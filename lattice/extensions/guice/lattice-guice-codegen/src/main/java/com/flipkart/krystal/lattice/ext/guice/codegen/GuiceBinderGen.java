package com.flipkart.krystal.lattice.ext.guice.codegen;

import static com.flipkart.krystal.codegen.common.models.CodeGenUtility.lowerCaseFirstChar;
import static com.flipkart.krystal.codegen.common.models.Constants.EMPTY_CODE_BLOCK;
import static com.flipkart.krystal.datatypes.Trilean.FALSE;
import static com.flipkart.krystal.datatypes.Trilean.TRUE;
import static com.squareup.javapoet.CodeBlock.joining;
import static java.util.Map.entry;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.datatypes.Trilean;
import com.flipkart.krystal.lattice.codegen.spi.BindingsProvider;
import com.flipkart.krystal.lattice.codegen.spi.BindingsProvider.BindTo.Provider;
import com.flipkart.krystal.lattice.codegen.spi.BindingsProvider.Binding;
import com.flipkart.krystal.lattice.codegen.spi.BindingsProvider.BindingScope;
import com.flipkart.krystal.lattice.codegen.spi.BindingsProvider.DopantBinding;
import com.flipkart.krystal.lattice.codegen.spi.BindingsProvider.ProviderBinding;
import com.flipkart.krystal.lattice.codegen.spi.BindingsProvider.SimpleBinding;
import com.flipkart.krystal.lattice.codegen.spi.DepInjectBinderGen;
import com.flipkart.krystal.lattice.codegen.spi.LatticeAppCodeGenAttrsProvider;
import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.flipkart.krystal.lattice.core.Application;
import com.flipkart.krystal.lattice.ext.guice.GuiceModuleBinder;
import com.flipkart.krystal.lattice.ext.guice.servlet.GuiceServletModuleBinder;
import com.google.auto.service.AutoService;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Names;
import com.google.inject.servlet.RequestScoped;
import com.google.inject.util.Providers;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.CodeBlock.Builder;
import jakarta.inject.Singleton;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@AutoService(DepInjectBinderGen.class)
public final class GuiceBinderGen implements DepInjectBinderGen {

  @Override
  public CodeBlock getBinderCreationCode(LatticeCodegenContext context) {
    TypeElement dependencyInjectionBinder = getDependencyInjectionBinder(context);

    if (!isApplicable(context)) {
      throw new UnsupportedOperationException();
    }
    ServiceLoader<BindingsProvider> bindingGens =
        ServiceLoader.load(BindingsProvider.class, this.getClass().getClassLoader());
    return CodeBlock.builder()
        .addNamed(
            """
return new $guiceModuleBinder:T(
    $customBinderCreator:L
    new $abstractModule:T() {
      @$override:T
      protected void configure() {
$generatedBindings:L
      }

$generatedProviders:L
    });
""",
            Map.ofEntries(
                entry(
                    "customBinderCreator",
                    userDefinedDepInjectBinderMethod(context).isPresent()
                        ? CodeBlock.of("super.getDependencyInjectionBinder().getRootModule(),")
                        : CodeBlock.builder().build()),
                entry("guiceModuleBinder", dependencyInjectionBinder),
                entry("abstractModule", AbstractModule.class),
                entry("override", Override.class),
                entry("generatedBindings", getBindingsCode(context)),
                entry("generatedProviders", getProviderMethods(context, bindingGens))))
        .build();
  }

  private static @NonNull TypeElement getDependencyInjectionBinder(LatticeCodegenContext context) {
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

  private CodeBlock getBindingsCode(LatticeCodegenContext context) {
    CodeGenUtility util = context.codeGenUtility().codegenUtil();
    Builder codeBlock = CodeBlock.builder();
    List<LatticeAppCodeGenAttrsProvider> providers =
        StreamSupport.stream(
                ServiceLoader.load(
                        LatticeAppCodeGenAttrsProvider.class, this.getClass().getClassLoader())
                    .spliterator(),
                false)
            .toList();
    Set<Trilean> collect =
        providers.stream()
            .map(p -> p.get(context).needsRequestScopedHeaders())
            .collect(Collectors.toSet());
    if (collect.contains(TRUE)) {
      if (collect.contains(FALSE)) {
        util.error(
            "Conflicting lattice app attribute values found for attribute "
                + "'needsRequestScopedHeaders'. Found both TRUE and FALSE from providers: "
                + providers);
      }
    }

    for (BindingsProvider bindingsProvider :
        ServiceLoader.load(BindingsProvider.class, this.getClass().getClassLoader())) {
      for (Binding binding : bindingsProvider.bindings(context)) {
        if (binding instanceof DopantBinding dopantBinding) {
          codeBlock.addStatement(
              """
           bind($T.class)
               .to($T.class)
               .in($T.class)
""",
              dopantBinding.dopantType(),
              dopantBinding.dopantImplType(),
              Singleton.class);
        } else if (binding instanceof SimpleBinding simpleBinding) {
          if (simpleBinding.bindTo() instanceof Provider provider) {
            CodeBlock named = simpleBinding.named();
            Class<? extends Annotation> scopeAnnotation = getScopeAnnotation(provider.scope());
            codeBlock.addStatement(
                """
           // Actual values will be set when the RequestScope is opened
           bind($T.class)
                $L.toProvider($T.of($L))
                $L
""",
                simpleBinding.bindFrom(),
                named != null
                    ? CodeBlock.of(
                        """
                .annotatedWith($T.named($L))
""", Names.class, named)
                    : EMPTY_CODE_BLOCK,
                Providers.class,
                provider.bindToCode(),
                scopeAnnotation != null
                    ? CodeBlock.of("""
                .in($T.class)
""", scopeAnnotation)
                    : EMPTY_CODE_BLOCK);
          }
        }
      }
    }
    return codeBlock.build();
  }

  private CodeBlock getProviderMethods(
      LatticeCodegenContext context, ServiceLoader<BindingsProvider> bindingGens) {
    Builder codeBlock = CodeBlock.builder();
    for (BindingsProvider bindingsProvider : bindingGens) {
      for (Binding binding : bindingsProvider.bindings(context)) {
        if (binding instanceof ProviderBinding providerBinding) {
          codeBlock.addNamed(
              """
      @$provides:T
      $scope:L
      $boundType:T $name:L($args:L){
        $provisionLogic:L
      }
""",
              Map.ofEntries(
                  entry("provides", Provides.class),
                  entry("scope", getScope(providerBinding.scope())),
                  entry("boundType", providerBinding.boundType()),
                  entry("name", lowerCaseFirstChar(providerBinding.name())),
                  entry("args", providerBinding.dependencies().stream().collect(joining(","))),
                  entry("provisionLogic", providerBinding.providingLogic())));
        }
      }
    }
    return codeBlock.build();
  }

  private static CodeBlock getScope(BindingScope scope) {
    Class<? extends Annotation> scopeAnnotation = getScopeAnnotation(scope);
    return scopeAnnotation == null
        ? CodeBlock.builder().build()
        : CodeBlock.of("@$T", scopeAnnotation);
  }

  private static @Nullable Class<? extends Annotation> getScopeAnnotation(BindingScope scope) {
    return switch (scope) {
      case NO_SCOPE -> null;
      case APP_LOGIC_SCOPE -> RequestScoped.class;
      case SINGLETON -> Singleton.class;
    };
  }

  private Optional<ExecutableElement> userDefinedDepInjectBinderMethod(
      LatticeCodegenContext context) {
    TypeElement typeElement = context.latticeAppTypeElement();
    CodeGenUtility util = context.codeGenUtility().codegenUtil();
    try {
      return util.getMethod(
          typeElement, Application.class.getMethod("getDependencyInjectionBinder").getName(), 0);
    } catch (Exception e) {
      throw util.errorAndThrow(
          "Application class has no 'getDependencyInjectionBinder' method. "
              + "This should not happen. Possibly there are incompatible versions in the classpath.");
    }
  }
}
