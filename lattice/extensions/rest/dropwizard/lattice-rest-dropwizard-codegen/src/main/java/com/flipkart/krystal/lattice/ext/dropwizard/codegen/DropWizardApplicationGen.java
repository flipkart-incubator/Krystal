package com.flipkart.krystal.lattice.ext.dropwizard.codegen;

import static com.flipkart.krystal.lattice.ext.dropwizard.codegen.DwCodegenUtil.getDwAppClassName;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.models.CodeGenUtility.AnnotationInfo;
import com.flipkart.krystal.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.flipkart.krystal.lattice.codegen.LatticeCodegenUtils;
import com.flipkart.krystal.lattice.codegen.spi.LatticeCodeGeneratorProvider;
import com.flipkart.krystal.lattice.core.LatticeApp;
import com.flipkart.krystal.lattice.ext.guice.GuiceFramework;
import com.flipkart.krystal.lattice.ext.rest.dropwizard.DropwizardRestApplication;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.JavaFile.Builder;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Bootstrap;
import jakarta.inject.Singleton;
import java.util.Map.Entry;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;
import ru.vyarus.dropwizard.guice.GuiceBundle;

@AutoService(LatticeCodeGeneratorProvider.class)
public class DropWizardApplicationGen implements LatticeCodeGeneratorProvider {

  @Override
  public CodeGenerator create(LatticeCodegenContext c) {
    return new CodeGenerator() {
      private final LatticeCodegenContext context = c;
      private final CodeGenUtility util = c.codeGenUtility().codegenUtil();
      private final LatticeCodegenUtils latticeUtil = new LatticeCodegenUtils(util);

      @Override
      public void generate() {
        AnnotationInfo<LatticeApp> annotationInfo =
            util.getAnnotationInfo(context.latticeAppTypeElement(), LatticeApp.class);
        if (!isApplicable(annotationInfo)) {
          return;
        }
        validate(annotationInfo);

        ClassName dwAppClassName = getDwAppClassName(context);
        TypeSpec.Builder classBuilder =
            util.classBuilder(
                    dwAppClassName.simpleName(),
                    context.latticeAppTypeElement().getQualifiedName().toString())
                .addAnnotation(Singleton.class)
                .superclass(DropwizardRestApplication.class)
                .addField(GuiceFramework.class, "guiceFramework", PRIVATE, FINAL);
        classBuilder.addMethod(constructor());
        classBuilder.addMethod(initMethod());
        Builder javaFile = JavaFile.builder(dwAppClassName.packageName(), classBuilder.build());
        util.generateSourceFile(
            dwAppClassName.canonicalName(), javaFile.build(), context.latticeAppTypeElement());
      }

      private MethodSpec constructor() {
        return latticeUtil
            .constructorOverride(DropwizardRestApplication.class)
            .addParameter(GuiceFramework.class, "guiceFramework")
            .addStatement("this.guiceFramework = guiceFramework")
            .build();
      }

      private MethodSpec initMethod() {
        return MethodSpec.methodBuilder("initialize")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .addParameter(
                ParameterizedTypeName.get(Bootstrap.class, Configuration.class), "bootstrap")
            .addCode(
"""
    bootstrap.addBundle($T.builder()
        .injectorFactory(
            (stage, modules) -> {
              return guiceFramework.getValueProvider().injector();
            })
        .build());
""",
                GuiceBundle.class)
            .build();
      }

      private void validate(AnnotationInfo<LatticeApp> annotationInfo) {
        CodeGenUtility util = context.codeGenUtility().codegenUtil();
        TypeMirror depInjectionFramework =
            util.getTypeFromAnnotationMember(context.latticeApp()::dependencyInjectionFramework);
        if (!util.isRawAssignable(depInjectionFramework, GuiceFramework.class)) {
          util.processingEnv()
              .getMessager()
              .printMessage(
                  Kind.ERROR,
                  "Lattice-Dropwizard currently only supports Guice for dependency injection. Found "
                      + depInjectionFramework,
                  context.latticeAppTypeElement(),
                  annotationInfo.mirror(),
                  annotationInfo.mirror().getElementValues().entrySet().stream()
                      .filter(
                          e ->
                              e.getKey()
                                  .getSimpleName()
                                  .contentEquals("dependencyInjectionFramework"))
                      .map(Entry::getValue)
                      .findAny()
                      .orElse(null));
        }
      }

      private boolean isApplicable(AnnotationInfo<LatticeApp> annotationInfo) {
        return context.codegenPhase() == CodegenPhase.FINAL && annotationInfo != null;
      }
    };
  }
}
