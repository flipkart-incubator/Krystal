package com.flipkart.krystal.lattice.ext.cdi.codegen;

import static com.flipkart.krystal.codegen.common.models.CodeGenUtility.lowerCaseFirstChar;
import static com.flipkart.krystal.codegen.common.models.CodegenPhase.FINAL;
import static com.flipkart.krystal.lattice.codegen.LatticeCodegenUtils.getDiBindingContainerName;
import static com.flipkart.krystal.lattice.codegen.spi.di.BindingsContainer.getBindingContainers;
import static com.flipkart.krystal.lattice.ext.cdi.codegen.CdiBinderGen.getDependencyInjectionBinder;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.flipkart.krystal.lattice.codegen.spi.LatticeCodeGeneratorProvider;
import com.flipkart.krystal.lattice.codegen.spi.di.Binding;
import com.flipkart.krystal.lattice.codegen.spi.di.BindingsContainer;
import com.flipkart.krystal.lattice.codegen.spi.di.ImplTypeBinding;
import com.flipkart.krystal.lattice.codegen.spi.di.NullBinding;
import com.flipkart.krystal.lattice.codegen.spi.di.ProviderMethod;
import com.flipkart.krystal.lattice.ext.cdi.CdiFramework;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec.Builder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import javax.lang.model.element.TypeElement;
import lombok.SneakyThrows;

@AutoService(LatticeCodeGeneratorProvider.class)
public class CdiModuleGenProvider implements LatticeCodeGeneratorProvider {

  @Override
  public CodeGenerator create(LatticeCodegenContext latticeCodegenContext) {
    return new CdiModuleGen(latticeCodegenContext);
  }

  private record CdiModuleGen(LatticeCodegenContext context) implements CodeGenerator {

    @SneakyThrows
    @Override
    public void generate() {
      if (!isApplicable()) {
        return;
      }
      Map<String, List<BindingsContainer>> bindingContainers = getBindingContainers(context);
      CodeGenUtility util = context.codeGenUtility().codegenUtil();
      TypeElement latticeAppTypeElement = context.latticeAppTypeElement();
      for (Entry<String, List<BindingsContainer>> entry : bindingContainers.entrySet()) {
        String identifier = entry.getKey();
        List<Binding> bindings =
            entry.getValue().stream()
                .map(BindingsContainer::bindings)
                .flatMap(List::stream)
                .toList();
        ClassName moduleClassName = getDiBindingContainerName(context, identifier);
        Builder classBuilder =
            util.classBuilder(
                    moduleClassName.simpleName(),
                    latticeAppTypeElement.getQualifiedName().toString())
                .addAnnotation(ApplicationScoped.class)
                .addModifiers(PUBLIC)
                .addMethods(getProviderMethods(bindings));
        util.generateSourceFile(
            moduleClassName.canonicalName(),
            JavaFile.builder(moduleClassName.packageName(), classBuilder.build()).build(),
            latticeAppTypeElement);
      }
    }

    private boolean isApplicable() {
      return FINAL.equals(context.codegenPhase())
          && getDependencyInjectionBinder(context)
              .equals(
                  context
                      .codeGenUtility()
                      .processingEnv()
                      .getElementUtils()
                      .getTypeElement(CdiFramework.class.getCanonicalName()));
    }

    private List<MethodSpec> getProviderMethods(List<Binding> bindings) {
      List<MethodSpec> providers = new ArrayList<>();
      for (Binding binding : bindings) {
        if (binding instanceof NullBinding) {
          // Null bindings are used for Guice-like DI systems which need explicit null bindings for
          // request scoped objects to be able to provide seeds at runtime. CDI doesn't need this.
          continue;
        } else if (binding instanceof ImplTypeBinding implTypeBinding
            && implTypeBinding.isSoleImpl()) {
          // If this is the sole impl binding, we don't need to provide it as a producer method. It
          // will be auto discovered
          continue;
        }
        MethodSpec.Builder methodBuilder =
            MethodSpec.methodBuilder(lowerCaseFirstChar(binding.identifierName()))
                .addAnnotation(Produces.class);
        if (binding instanceof ImplTypeBinding implTypeBinding) {
          methodBuilder.returns(implTypeBinding.parentType());
          ClassName childType = implTypeBinding.childType();
          AnnotationSpec scopeAnnotation = implTypeBinding.scope();
          String varName = lowerCaseFirstChar(childType.simpleName());
          methodBuilder.addParameter(childType, varName);
          if (scopeAnnotation != null) {
            methodBuilder.addAnnotation(scopeAnnotation);
          }
          methodBuilder.addStatement("return $L", varName);
        } else if (binding instanceof ProviderMethod providerMethod) {
          AnnotationSpec scopeAnnotation = providerMethod.scope();
          methodBuilder
              .returns(providerMethod.boundType())
              .addParameters(providerMethod.dependencies())
              .addCode(providerMethod.providingLogic());
          if (scopeAnnotation != null) {
            methodBuilder.addAnnotation(scopeAnnotation);
          }
          methodBuilder.addAnnotations(providerMethod.annotations());
        } else {
          throw new UnsupportedOperationException("Unsupported binding type " + binding);
        }
        for (BindingCodeTransformer bindingCodeTransformer :
            ServiceLoader.load(BindingCodeTransformer.class, this.getClass().getClassLoader())) {
          bindingCodeTransformer.transform(methodBuilder);
        }
        providers.add(methodBuilder.build());
      }
      return providers;
    }
  }
}
