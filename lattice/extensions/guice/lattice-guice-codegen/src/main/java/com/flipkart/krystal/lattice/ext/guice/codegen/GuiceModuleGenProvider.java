package com.flipkart.krystal.lattice.ext.guice.codegen;

import static com.flipkart.krystal.codegen.common.models.CodeGenUtility.lowerCaseFirstChar;
import static com.flipkart.krystal.codegen.common.models.CodegenPhase.FINAL;
import static com.flipkart.krystal.codegen.common.models.Constants.EMPTY_CODE_BLOCK;
import static com.flipkart.krystal.datatypes.Trilean.FALSE;
import static com.flipkart.krystal.datatypes.Trilean.TRUE;
import static com.flipkart.krystal.lattice.ext.guice.codegen.GuiceBinderGen.isGuiceBinderConfigured;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.datatypes.Trilean;
import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.flipkart.krystal.lattice.codegen.spi.BindingsProvider;
import com.flipkart.krystal.lattice.codegen.spi.BindingsProvider.BindTo;
import com.flipkart.krystal.lattice.codegen.spi.BindingsProvider.Binding;
import com.flipkart.krystal.lattice.codegen.spi.BindingsProvider.BindingScope;
import com.flipkart.krystal.lattice.codegen.spi.BindingsProvider.DopantBinding;
import com.flipkart.krystal.lattice.codegen.spi.BindingsProvider.ProviderMethod;
import com.flipkart.krystal.lattice.codegen.spi.BindingsProvider.SimpleBinding;
import com.flipkart.krystal.lattice.codegen.spi.LatticeAppCodeGenAttrsProvider;
import com.flipkart.krystal.lattice.codegen.spi.LatticeCodeGeneratorProvider;
import com.google.auto.service.AutoService;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.servlet.RequestScoped;
import com.google.inject.util.Providers;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec.Builder;
import com.squareup.javapoet.TypeSpec;
import jakarta.enterprise.inject.Vetoed;
import jakarta.inject.Singleton;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.lang.model.element.TypeElement;
import lombok.SneakyThrows;
import org.checkerframework.checker.nullness.qual.Nullable;

@AutoService(LatticeCodeGeneratorProvider.class)
public class GuiceModuleGenProvider implements LatticeCodeGeneratorProvider {

  @Override
  public CodeGenerator create(LatticeCodegenContext latticeCodegenContext) {
    return new GuiceModuleGen(latticeCodegenContext);
  }

  private record GuiceModuleGen(LatticeCodegenContext context) implements CodeGenerator {

    @SneakyThrows
    @Override
    public void generate() {
      if (!isApplicable()) {
        return;
      }
      ServiceLoader<BindingsProvider> bindingGens =
          ServiceLoader.load(BindingsProvider.class, this.getClass().getClassLoader());
      CodeGenUtility util = context.codeGenUtility().codegenUtil();
      TypeElement latticeAppTypeElement = context.latticeAppTypeElement();
      ClassName moduleClassName = getModuleClassName(context);
      TypeSpec.Builder classBuilder =
          util.classBuilder(moduleClassName.simpleName())
              .addModifiers(PUBLIC)
              // Add vetoed to prevent jakarta CDI from picking up the producer methods
              .addAnnotation(Vetoed.class)
              .superclass(AbstractModule.class)
              .addMethod(
                  MethodSpec.overriding(
                          util.getMethod(
                              AbstractModule.class,
                              AbstractModule.class.getDeclaredMethod("configure").getName(),
                              0))
                      .addCode(getBindingsCode(context))
                      .build())
              .addMethods(getProviderMethods(context, bindingGens));
      util.generateSourceFile(
          moduleClassName.canonicalName(),
          JavaFile.builder(moduleClassName.packageName(), classBuilder.build()).build(),
          latticeAppTypeElement);
    }

    private boolean isApplicable() {
      return FINAL.equals(context.codegenPhase()) && isGuiceBinderConfigured(context);
    }

    private CodeBlock getBindingsCode(LatticeCodegenContext context) {
      CodeGenUtility util = context.codeGenUtility().codegenUtil();
      CodeBlock.Builder codeBlock = CodeBlock.builder();
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
            if (simpleBinding.bindTo() instanceof BindTo.Provider provider) {
              Class<? extends Annotation> scopeAnnotation = getScopeAnnotation(provider.scope());
              CodeBlock annotatedWith = simpleBinding.qualifier();
              codeBlock.addStatement(
                  """
                               // Actual values will be set when the RequestScope is opened
                               bind($T.class)
                                    $L.toProvider($T.of($L))
                                    $L
                    """,
                  simpleBinding.bindFrom(),
                  annotatedWith != null
                      ? CodeBlock.of(
                          """
                                    .annotatedWith($L)
                    """,
                          annotatedWith)
                      : EMPTY_CODE_BLOCK,
                  Providers.class,
                  provider.bindToCode(),
                  scopeAnnotation != null
                      ? CodeBlock.of(
                          """
                                        .in($T.class)
                        """,
                          scopeAnnotation)
                      : EMPTY_CODE_BLOCK);
            }
          }
        }
      }
      return codeBlock.build();
    }

    private List<MethodSpec> getProviderMethods(
        LatticeCodegenContext context, ServiceLoader<BindingsProvider> bindingGens) {
      List<MethodSpec> providers = new ArrayList<>();
      for (BindingsProvider bindingsProvider : bindingGens) {
        for (Binding binding : bindingsProvider.bindings(context)) {
          if (binding instanceof ProviderMethod providerMethod) {
            Class<? extends Annotation> scopeAnnotation =
                getScopeAnnotation(providerMethod.scope());
            MethodSpec.Builder builder =
                MethodSpec.methodBuilder(lowerCaseFirstChar(providerMethod.name()))
                    .returns(providerMethod.boundType())
                    .addParameters(
                        providerMethod.dependencies().stream().map(Builder::build).toList())
                    .addAnnotation(Provides.class)
                    .addCode(providerMethod.providingLogic());
            if (scopeAnnotation != null) {
              builder.addAnnotation(scopeAnnotation);
            }
            providers.add(builder.build());
          }
        }
      }
      return providers;
    }

    private static @Nullable Class<? extends Annotation> getScopeAnnotation(BindingScope scope) {
      return switch (scope) {
        case NO_SCOPE -> null;
        case REQUEST -> RequestScoped.class;
        case SINGLETON -> Singleton.class;
      };
    }
  }

  static ClassName getModuleClassName(LatticeCodegenContext context) {
    return ClassName.get(
        context.codeGenUtility().codegenUtil().getPackageName(context.latticeAppTypeElement()),
        context.latticeAppTypeElement().getSimpleName().toString() + "_Module");
  }
}
