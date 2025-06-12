package com.flipkart.krystal.lattice.ext.rest.quarkus.codegen;

import static com.flipkart.krystal.codegen.common.models.CodeGenUtility.lowerCaseFirstChar;
import static com.flipkart.krystal.codegen.common.models.CodegenPhase.FINAL;
import static com.flipkart.krystal.vajram.json.Json.JSON;
import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.codegen.common.models.Constants;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.flipkart.krystal.lattice.codegen.LatticeCodegenUtils;
import com.flipkart.krystal.lattice.codegen.spi.LatticeCodeGeneratorProvider;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfoLite;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import flipkart.krystal.lattice.ext.rest.quarkus.restServer.QuarkusRestServerDopant;
import flipkart.krystal.lattice.ext.rest.quarkus.restServer.RestService;
import java.util.List;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

@AutoService(LatticeCodeGeneratorProvider.class)
public class QuarkusRestServerDopantGenProvider implements LatticeCodeGeneratorProvider {

  @Override
  public CodeGenerator create(LatticeCodegenContext latticeCodegenContext) {
    return new QuarkusRestServerDopantGen(latticeCodegenContext);
  }

  static class QuarkusRestServerDopantGen implements CodeGenerator {

    private final LatticeCodegenContext context;
    private final CodeGenUtility util;

    public QuarkusRestServerDopantGen(LatticeCodegenContext context) {
      this.context = context;
      this.util = context.codeGenUtility().codegenUtil();
    }

    @Override
    public void generate() {
      CodegenPhase codegenPhase = context.codegenPhase();
      if (!FINAL.equals(codegenPhase)) {
        util.note(
            "Skipping Quarkus Rest Server dopant impl generation because this is not codegen phase: "
                + FINAL);
        return;
      }
      TypeElement latticeAppElem = context.latticeAppTypeElement();
      String packageName =
          util.processingEnv()
              .getElementUtils()
              .getPackageOf(latticeAppElem)
              .getQualifiedName()
              .toString();

      LatticeCodegenUtils latticeCodegenUtils = new LatticeCodegenUtils(util);
      String dopantImplName =
          latticeCodegenUtils.getDopantImplName(latticeAppElem, QuarkusRestServerDopant.class);
      TypeSpec.Builder classBuilder =
          util.classBuilder(dopantImplName)
              .addModifiers(Modifier.FINAL)
              .superclass(QuarkusRestServerDopant.class);
      classBuilder.addMethod(
          latticeCodegenUtils.dopantConstructorOverride(QuarkusRestServerDopant.class).build());
      RestService restService = latticeAppElem.getAnnotation(RestService.class);
      addRoutes(restService, classBuilder);
      util.generateSourceFile(
          ClassName.get(packageName, dopantImplName).canonicalName(),
          JavaFile.builder(packageName, classBuilder.build()).build(),
          latticeAppElem);
    }

    private void addRoutes(RestService restService, TypeSpec.Builder classBuilder) {
      List<? extends TypeMirror> vajramTypes =
          util.getTypesFromAnnotationMember(restService::resourceVajrams);
      MethodSpec.Builder methodSpec =
          MethodSpec.overriding(util.getMethod(QuarkusRestServerDopant.class, "addRoutes", 1));
      String pathPrefix = restService.pathPrefix().isEmpty() ? "" : "/" + restService.pathPrefix();
      for (TypeMirror vajramType : vajramTypes) {
        TypeElement vajramElement =
            (TypeElement) requireNonNull(util.processingEnv().getTypeUtils().asElement(vajramType));
        VajramInfoLite vajramInfoLite =
            context.codeGenUtility().computeVajramInfoLite(vajramElement);
        String packageName =
            util.processingEnv()
                .getElementUtils()
                .getPackageOf(vajramElement)
                .getQualifiedName()
                .toString();
        String vajramId = vajramInfoLite.vajramId().id();
        methodSpec.addCode(
            """
              router
                  .post($S)
                  .handler(
                      routingContext -> executeHttpRequest(routingContext, $T::new));
            """,
            pathPrefix + "/" + lowerCaseFirstChar(vajramId),
            ClassName.get(
                packageName,
                vajramId
                    + Constants.REQUEST_SUFFIX
                    + Constants.IMMUT_SUFFIX
                    + JSON.modelClassesSuffix()));
      }
      classBuilder.addMethod(methodSpec.build());
    }
  }
}
