package com.flipkart.krystal.vajram.graphql.codegen;

import static com.flipkart.krystal.codegen.common.models.CodegenPhase.MODELS;
import static com.flipkart.krystal.codegen.common.models.Constants.CODEGEN_PHASE_KEY;
import static com.flipkart.krystal.codegen.common.models.Constants.MODULE_ROOT_PATH_KEY;
import static com.google.common.base.Throwables.getStackTraceAsString;

import com.flipkart.krystal.codegen.common.models.AbstractKrystalAnnoProcessor;
import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.models.RunOnlyWhenCodegenPhaseIs;
import com.google.auto.service.AutoService;
import java.io.File;
import java.util.Set;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
@SupportedOptions({CODEGEN_PHASE_KEY, MODULE_ROOT_PATH_KEY})
@RunOnlyWhenCodegenPhaseIs(MODELS)
public class GraphQLAnnotationProcessor extends AbstractKrystalAnnoProcessor {

  boolean generated;

  @Override
  protected boolean processImpl(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (this.generated) {
      return false;
    }
    CodeGenUtility util = codeGenUtil();
    File schemaFile = GraphQlCodeGenUtil.getSchemaFile(util);
    if (!schemaFile.exists()) {
      util.note("Schema.graphqls not found. Skipping GraphQl Code Generation");
      return false;
    }
    util.note("Annotations: " + annotations);
    for (TypeElement annotation : annotations) {
      util.note(
          "Annotation Processor: "
              + getClass()
              + "; Annotation:"
              + annotation
              + "; Classes: "
              + roundEnv.getElementsAnnotatedWith(annotation));
    }
    try {
      new GraphQLObjectAggregateGen(codeGenUtil(), schemaFile).generate();
    } catch (Exception e) {
      util.error("[GraphQL Codegen Exception] " + getStackTraceAsString(e));
    }
    try {
      new GraphQLEntityGen(util, schemaFile).generate();
    } catch (Exception e) {
      util.error("[GraphQL Codegen Exception] " + getStackTraceAsString(e));
    }
    generated = true;
    return false;
  }
}
