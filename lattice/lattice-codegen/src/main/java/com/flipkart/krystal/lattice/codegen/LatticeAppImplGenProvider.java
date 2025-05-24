package com.flipkart.krystal.lattice.codegen;

import static com.flipkart.krystal.lattice.codegen.LatticeCodegenConstants.LATTICE_APP_IMPL_SUFFIX;
import static com.squareup.javapoet.TypeName.VOID;
import static java.util.Objects.requireNonNull;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec.Builder;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
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
              .map(Provider::get)
              .filter(s -> s.isApplicable(context))
              .toList();
      if (services.size() > 1) {
        throw util.errorAndThrow(
            "Found more than one Service Providers for 'DepInjectBinderGenProvider'. This is not allowed");
      } else if (services.isEmpty()) {
        throw util.errorAndThrow(
            "Could not find any Service Providers for 'DepInjectBinderGenProvider'");
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

      Builder classBuilder =
          util.classBuilder(latticeAppImplClassName.simpleName())
              .superclass(latticeApp.asType())
              .addMethod(
                  MethodSpec.methodBuilder("main")
                      .addModifiers(PUBLIC, STATIC)
                      .returns(VOID)
                      .addParameter(ParameterSpec.builder(String[].class, "_args").build())
                      .addException(Exception.class)
                      .addNamedCode(
                          "new $implClass:T().init(_args);",
                          Map.of("implClass", latticeAppImplClassName))
                      .build())
              .addMethod(
                  MethodSpec.overriding(
                          requireNonNull(
                              util.getMethod(latticeApp, "getDependencyInjectionBinder", 0)
                                  .orElse(null)))
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

    private boolean isApplicable() {
      if (!CodegenPhase.FINAL.equals(context.codegenPhase())) {
        util.note("Skipping Lattice App Impl codegen current phase is not FINAL");
        return false;
      }
      return true;
    }
  }
}
