package com.flipkart.krystal.vajram.graphql.codegen;

import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.codegen.common.spi.ModelsCodeGenContext;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlResponseJson;
import com.squareup.javapoet.ClassName;
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;

public class GraphQlTypeModelGen implements CodeGenerator {

  private final ModelsCodeGenContext codeGenContext;

  public GraphQlTypeModelGen(ModelsCodeGenContext codeGenContext) {
    this.codeGenContext = codeGenContext;
  }

  @Override
  public void generate() {
    if (!isApplicable()) {
      return;
    }
    TypeElement modelRootType = codeGenContext.modelRootType();
    CodeGenUtility util = codeGenContext.util();

    ClassName immutClassName = util.getImmutClassName(modelRootType);
    String packageName = immutClassName.packageName();
    ClassName immutableModelName =
        ClassName.get(
            packageName,
            immutClassName.simpleName() + GraphQlResponseJson.INSTANCE.modelClassesSuffix());

    // Extract and validate model methods
    List<ExecutableElement> modelMethods = util.extractAndValidateModelMethods(modelRootType);
  }

  private boolean isApplicable() {
    CodeGenUtility util = codeGenContext.util();
    SupportedModelProtocols supportedModelProtocols =
        codeGenContext.modelRootType().getAnnotation(SupportedModelProtocols.class);
    // Check if Json is mentioned in the annotation value
    return supportedModelProtocols != null
        && util.getTypesFromAnnotationMember(supportedModelProtocols::value).stream()
            .map(typeMirror -> util.processingEnv().getTypeUtils().asElement(typeMirror))
            .filter(elem -> elem instanceof QualifiedNameable)
            .map(QualifiedNameable.class::cast)
            .map(element -> requireNonNull(element).getQualifiedName().toString())
            .anyMatch(GraphQlResponseJson.class.getCanonicalName()::equals);
  }
}
