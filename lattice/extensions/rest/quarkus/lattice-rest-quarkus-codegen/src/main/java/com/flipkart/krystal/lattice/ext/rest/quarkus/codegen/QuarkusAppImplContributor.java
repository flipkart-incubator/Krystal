package com.flipkart.krystal.lattice.ext.rest.quarkus.codegen;

import static com.squareup.javapoet.TypeName.VOID;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.flipkart.krystal.lattice.codegen.spi.LatticeAppImplContributor;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;
import java.util.List;
import javax.lang.model.element.TypeElement;
import org.checkerframework.checker.nullness.qual.Nullable;

@AutoService(LatticeAppImplContributor.class)
public class QuarkusAppImplContributor implements LatticeAppImplContributor {

  @Override
  public @Nullable MethodSpec mainMethod(LatticeCodegenContext context) {
    TypeElement latticeApp = context.latticeAppTypeElement();
    String packageName =
        context
            .codeGenUtility()
            .processingEnv()
            .getElementUtils()
            .getPackageOf(latticeApp)
            .getQualifiedName()
            .toString();

    return MethodSpec.methodBuilder("main")
        .addModifiers(PUBLIC, STATIC)
        .returns(VOID)
        .addParameter(ParameterSpec.builder(String[].class, "_args").build())
        .addException(Exception.class)
        .addStatement(
            "$T.run($T.class, _args)",
            Quarkus.class,
            ClassName.get(packageName, latticeApp.getSimpleName() + "_QuarkusApp"))
        .build();
  }

  @Override
  public List<AnnotationSpec> classAnnotations(LatticeCodegenContext context) {
    return List.of(AnnotationSpec.builder(QuarkusMain.class).build());
  }
}
