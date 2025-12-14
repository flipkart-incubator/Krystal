package com.flipkart.krystal.lattice.codegen;

import static com.flipkart.krystal.lattice.codegen.LatticeCodegenUtils.LATTICE_APP_IMPL_SUFFIX;
import static com.squareup.javapoet.TypeName.VOID;
import static java.util.Objects.requireNonNull;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.lattice.codegen.spi.LatticeAppImplContributor;
import com.flipkart.krystal.lattice.codegen.spi.LatticeCodeGeneratorProvider;
import com.flipkart.krystal.lattice.codegen.spi.di.DepInjectBinderGen;
import com.flipkart.krystal.lattice.core.LatticeApplication;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import javax.lang.model.element.TypeElement;

@AutoService(LatticeCodeGeneratorProvider.class)
public final class LatticeAppImplGenProvider implements LatticeCodeGeneratorProvider {

  @Override
  public CodeGenerator create(LatticeCodegenContext latticeCodegenContext) {
    return new LatticeAppImplGen(latticeCodegenContext);
  }

  public static final class LatticeAppImplGen implements CodeGenerator {

    private final LatticeCodegenContext context;
    private final CodeGenUtility util;
    private final DepInjectBinderGen depInjectBinderGen;

    public LatticeAppImplGen(LatticeCodegenContext context) {
      this.context = context;
      this.util = context.codeGenUtility().codegenUtil();
      List<DepInjectBinderGen> services =
          ServiceLoader.load(DepInjectBinderGen.class, this.getClass().getClassLoader()).stream()
              .map(ServiceLoader.Provider::get)
              .filter(s -> s.isApplicable(context))
              .toList();
      if (services.size() > 1) {
        throw util.errorAndThrow(
            "Found more than one Service Providers for 'DepInjectBinderGen'. This is not allowed");
      } else if (services.isEmpty()) {
        throw util.errorAndThrow("Could not find any Service Providers for 'DepInjectBinderGen'");
      }
      this.depInjectBinderGen = services.get(0);
    }

    @Override
    public void generate() {
      if (!isApplicable()) {
        return;
      }
      TypeElement latticeApp = context.latticeAppTypeElement();
      String packageName =
          util.processingEnv()
              .getElementUtils()
              .getPackageOf(latticeApp)
              .getQualifiedName()
              .toString();
      ClassName latticeAppImplClassName =
          ClassName.get(
              packageName, latticeApp.getSimpleName().toString() + LATTICE_APP_IMPL_SUFFIX);

      TypeSpec.Builder classBuilder =
          util.classBuilder(
                  latticeAppImplClassName.simpleName(), latticeApp.getQualifiedName().toString())
              .addModifiers(PUBLIC)
              .superclass(latticeApp.asType())
              .addMethod(getMainMethod(latticeAppImplClassName, latticeApp, context))
              .addMethods(
                  ServiceLoader.load(
                          LatticeAppImplContributor.class, this.getClass().getClassLoader())
                      .stream()
                      .map(ServiceLoader.Provider::get)
                      .map(contrib -> contrib.methods(context))
                      .flatMap(List::stream)
                      .toList())
              .addFields(
                  ServiceLoader.load(
                          LatticeAppImplContributor.class, this.getClass().getClassLoader())
                      .stream()
                      .map(ServiceLoader.Provider::get)
                      .map(contrib -> contrib.fields(context))
                      .flatMap(List::stream)
                      .toList())
              .addAnnotations(getTypeAnnotations())
              .addMethod(
                  MethodSpec.overriding(
                          requireNonNull(
                              util.getMethod(
                                  LatticeApplication.class, "getDependencyInjectionBinder", 0)))
                      .returns(
                          TypeName.get(
                              util.getTypeFromAnnotationMember(
                                  context.latticeApp()::dependencyInjectionBinder)))
                      .addCode("$L", depInjectBinderGen.getBinderCreationCode(context))
                      .build());

      StringWriter writer = new StringWriter();
      try {
        JavaFile.builder(packageName, classBuilder.build()).build().writeTo(writer);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      util.generateSourceFile(
          latticeAppImplClassName.canonicalName(), writer.toString(), latticeApp);
    }

    private List<AnnotationSpec> getTypeAnnotations() {
      return ServiceLoader.load(LatticeAppImplContributor.class, this.getClass().getClassLoader())
          .stream()
          .map(ServiceLoader.Provider::get)
          .map(c -> c.classAnnotations(context))
          .flatMap(Collection::stream)
          .toList();
    }

    private MethodSpec getMainMethod(
        ClassName latticeAppImplClassName, TypeElement latticeApp, LatticeCodegenContext context) {
      ServiceLoader<LatticeAppImplContributor> contributors =
          ServiceLoader.load(LatticeAppImplContributor.class, this.getClass().getClassLoader());
      Map<? extends Class<?>, Optional<MethodSpec>> mainMethods =
          contributors.stream()
              .map(ServiceLoader.Provider::get)
              .collect(
                  Collectors.toMap(
                      (LatticeAppImplContributor latticeAppImplContributor) ->
                          (Class<?>) latticeAppImplContributor.getClass(),
                      c -> Optional.ofNullable(c.mainMethod(context))));
      List<MethodSpec> mainMethodsList =
          mainMethods.values().stream().filter(Optional::isPresent).map(Optional::get).toList();
      if (mainMethodsList.size() > 1) {
        util.error(
            "More than main method providers found for Lattice App impl. Providers are : "
                + mainMethods.keySet(),
            latticeApp);
        return mainMethodsList.get(0);
      } else if (mainMethodsList.size() == 1) {
        return mainMethodsList.get(0);
      } else {
        return MethodSpec.methodBuilder("main")
            .addModifiers(PUBLIC, STATIC)
            .returns(VOID)
            .addParameter(ParameterSpec.builder(String[].class, "_args").build())
            .addException(Exception.class)
            .addNamedCode(
                "new $implClass:T().init(_args);", Map.of("implClass", latticeAppImplClassName))
            .build();
      }
    }

    private boolean isApplicable() {
      if (!CodegenPhase.FINAL.equals(context.codegenPhase())) {
        util.note("Skipping Lattice App Impl codegen current phase is not FINAL");
        return false;
      }
      return true;
    }
  }
}
