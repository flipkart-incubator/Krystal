package com.flipkart.krystal.lattice.ext.guice.codegen;

import static com.flipkart.krystal.codegen.common.models.CodegenPhase.FINAL;
import static com.flipkart.krystal.codegen.common.models.Constants.EMPTY_CODE_BLOCK;
import static com.flipkart.krystal.datatypes.Trilean.FALSE;
import static com.flipkart.krystal.datatypes.Trilean.TRUE;
import static com.flipkart.krystal.lattice.codegen.LatticeCodegenUtils.getDiBindingContainerName;
import static com.flipkart.krystal.lattice.codegen.spi.di.BindingsContainer.getBindingContainers;
import static com.flipkart.krystal.lattice.ext.guice.codegen.GuiceBinderGen.isGuiceBinderConfigured;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.datatypes.Trilean;
import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.flipkart.krystal.lattice.codegen.spi.LatticeAppCodeGenAttrsProvider;
import com.flipkart.krystal.lattice.codegen.spi.LatticeCodeGeneratorProvider;
import com.flipkart.krystal.lattice.codegen.spi.di.Binding;
import com.flipkart.krystal.lattice.codegen.spi.di.BindingsContainer;
import com.flipkart.krystal.lattice.codegen.spi.di.ImplTypeBinding;
import com.flipkart.krystal.lattice.codegen.spi.di.NullBinding;
import com.flipkart.krystal.lattice.codegen.spi.di.ProviderMethod;
import com.google.auto.service.AutoService;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.servlet.RequestScoped;
import com.google.inject.util.Providers;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec.Builder;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.lang.model.element.TypeElement;
import lombok.SneakyThrows;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

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
      CodeGenUtility util = context.codeGenUtility().codegenUtil();
      Map<String, List<BindingsContainer>> bindingContainers = getBindingContainers(context);
      TypeElement latticeAppTypeElement = context.latticeAppTypeElement();
      for (Entry<String, List<BindingsContainer>> entry : bindingContainers.entrySet()) {
        String identifier = entry.getKey();
        ClassName moduleClassName = getDiBindingContainerName(context, identifier);
        List<BindingsContainer> bindingsContainers = entry.getValue();
        List<Binding> bindings =
            bindingsContainers.stream()
                .map(BindingsContainer::bindings)
                .flatMap(Collection::stream)
                .toList();
        Builder classBuilder =
            util.classBuilder(
                    moduleClassName.simpleName(),
                    latticeAppTypeElement.getQualifiedName().toString())
                .addModifiers(PUBLIC)
                // Add vetoed to prevent jakarta CDI from picking up the producer methods
                .superclass(AbstractModule.class)
                .addMethod(
                    MethodSpec.overriding(
                            util.getMethod(
                                AbstractModule.class,
                                AbstractModule.class.getDeclaredMethod("configure").getName(),
                                0))
                        .addCode(configureMethodCode(context, bindings))
                        .build())
                .addMethods(getProviderMethods(bindings));
        util.generateSourceFile(
            moduleClassName.canonicalName(),
            JavaFile.builder(moduleClassName.packageName(), classBuilder.build()).build(),
            latticeAppTypeElement);
      }
    }

    private boolean isApplicable() {
      return FINAL.equals(context.codegenPhase()) && isGuiceBinderConfigured(context);
    }

    private CodeBlock configureMethodCode(LatticeCodegenContext context, List<Binding> bindings) {
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

      for (Binding binding : bindings) {
        if (binding instanceof ImplTypeBinding implTypeBinding) {
          codeBlock.addStatement(
              """
                           bind($T.class)
                               .to($T.class)
                               .in($T.class)
                """,
              implTypeBinding.parentType(),
              implTypeBinding.childType(),
              getScopeAnnotation(implTypeBinding.scope()));
        } else if (binding instanceof NullBinding nullBinding) {
          ClassName scopeAnnotation = getScopeAnnotation(nullBinding.scope());
          CodeBlock annotatedWith = nullBinding.qualifierExpression();
          codeBlock.addStatement(
              """
                           // Actual values will be set when the RequestScope is opened
                           bind($T.class)
                                $L.toProvider($T.of($L))
                                $L
                """,
              nullBinding.bindFrom(),
              annotatedWith != null
                  ? CodeBlock.of(
                      """
                            .annotatedWith($L)
            """,
                      annotatedWith)
                  : EMPTY_CODE_BLOCK,
              Providers.class,
              CodeBlock.of("null"),
              scopeAnnotation != null
                  ? CodeBlock.of(
                      """
                                .in($T.class)
                """,
                      scopeAnnotation)
                  : EMPTY_CODE_BLOCK);
        }
      }
      return codeBlock.build();
    }

    private List<MethodSpec> getProviderMethods(List<Binding> bindingGens) {
      List<MethodSpec> providers = new ArrayList<>();
      for (Binding binding : bindingGens) {
        if (binding instanceof ProviderMethod providerMethod) {
          @Nullable ClassName scopeAnnotation = getScopeAnnotation(providerMethod.scope());
          MethodSpec.Builder builder =
              MethodSpec.methodBuilder(providerMethod.identifierName())
                  .returns(providerMethod.boundType())
                  .addParameters(providerMethod.dependencies())
                  .addAnnotation(Provides.class)
                  .addCode(providerMethod.providingLogic());
          if (scopeAnnotation != null) {
            builder.addAnnotation(scopeAnnotation);
          }
          builder.addAnnotations(providerMethod.annotations());
          providers.add(builder.build());
        }
      }
      return providers;
    }

    private static @PolyNull ClassName getScopeAnnotation(@PolyNull AnnotationSpec scope) {
      if (scope == null) {
        return null;
      }
      TypeName type = scope.type;
      if (type.equals(ClassName.get(jakarta.enterprise.context.RequestScoped.class))) {
        return ClassName.get(RequestScoped.class);
      }
      if (type.equals(ClassName.get(jakarta.enterprise.context.ApplicationScoped.class))) {
        return ClassName.get(Singleton.class);
      }
      return (ClassName) scope.type;
    }
  }
}
