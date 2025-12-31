package com.flipkart.krystal.lattice.ext.rest.quarkus.codegen;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.flipkart.krystal.lattice.codegen.spi.LatticeCodeGeneratorProvider;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import io.quarkus.runtime.QuarkusApplication;
import jakarta.inject.Inject;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;

@AutoService(LatticeCodeGeneratorProvider.class)
public class QuarkusAppGenProvider implements LatticeCodeGeneratorProvider {

  @Override
  public CodeGenerator create(LatticeCodegenContext latticeCodegenContext) {
    return () -> {
      CodeGenUtility util = latticeCodegenContext.codeGenUtility().codegenUtil();
      CodegenPhase codegenPhase = latticeCodegenContext.codegenPhase();
      if (!CodegenPhase.FINAL.equals(codegenPhase)) {
        util.note(
            "Skipping Quarkus App generation because this is not codegen phase: "
                + CodegenPhase.FINAL);
        return;
      }
      TypeElement latticeAppTypeElement = latticeCodegenContext.latticeAppTypeElement();
      String packageName =
          util.processingEnv()
              .getElementUtils()
              .getPackageOf(latticeAppTypeElement)
              .getQualifiedName()
              .toString();

      Name appClassName = latticeAppTypeElement.getSimpleName();
      String quarkusAppClassName = appClassName + "_QuarkusApp";
      TypeName appType = TypeName.get(latticeAppTypeElement.asType());
      util.generateSourceFile(
          ClassName.get(packageName, quarkusAppClassName).canonicalName(),
          JavaFile.builder(
                  packageName,
                  util.classBuilder(
                          quarkusAppClassName, latticeAppTypeElement.getQualifiedName().toString())
                      .addModifiers(PUBLIC)
                      .addSuperinterface(QuarkusApplication.class)
                      .addField(appType, "latticeApp", PRIVATE, FINAL)
                      .addMethod(
                          constructorBuilder()
                              .addModifiers(PUBLIC)
                              .addAnnotation(Inject.class)
                              .addParameter(appType, "latticeApp")
                              .addStatement("this.latticeApp = latticeApp")
                              .build())
                      .addMethod(
                          MethodSpec.overriding(util.getMethod(QuarkusApplication.class, "run", 1))
                              .addStatement("return latticeApp.run(args)")
                              .build())
                      .build())
              .build(),
          latticeAppTypeElement);
    };
  }
}
