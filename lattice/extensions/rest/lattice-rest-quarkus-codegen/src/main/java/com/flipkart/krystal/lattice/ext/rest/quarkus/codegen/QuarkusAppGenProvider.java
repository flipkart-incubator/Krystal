package com.flipkart.krystal.lattice.ext.rest.quarkus.codegen;

import static com.flipkart.krystal.codegen.common.models.CodegenPhase.FINAL;
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
import io.quarkus.runtime.QuarkusApplication;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;

@AutoService(LatticeCodeGeneratorProvider.class)
public class QuarkusAppGenProvider implements LatticeCodeGeneratorProvider {

  @Override
  public CodeGenerator create(LatticeCodegenContext latticeCodegenContext) {
    return () -> {
      CodeGenUtility util = latticeCodegenContext.codeGenUtility().codegenUtil();
      CodegenPhase codegenPhase = latticeCodegenContext.codegenPhase();
      if (!FINAL.equals(codegenPhase)) {
        util.note("Skipping Quarkus App generation because this is not codegen phase: " + FINAL);
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
      util.generateSourceFile(
          ClassName.get(packageName, quarkusAppClassName).canonicalName(),
          JavaFile.builder(
                  packageName,
                  util.classBuilder(quarkusAppClassName)
                      .addModifiers(PUBLIC)
                      .addSuperinterface(QuarkusApplication.class)
                      .addMethod(
                          MethodSpec.overriding(util.getMethod(QuarkusApplication.class, "run", 1))
                              .addStatement(
                                  "new $T().init(args)",
                                  ClassName.get(packageName, appClassName + "_Impl"))
                              .addStatement("return 0")
                              .build())
                      .build())
              .build(),
          latticeAppTypeElement);
    };
  }
}
