package com.flipkart.krystal.vajram.graphql.codegen;

import static com.flipkart.krystal.codegen.common.models.CodegenPhase.MODELS;
import static com.flipkart.krystal.codegen.common.models.Constants.CODEGEN_PHASE_KEY;
import static com.google.common.base.Throwables.getStackTraceAsString;

import com.flipkart.krystal.codegen.common.models.AbstractKrystalAnnoProcessor;
import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.models.RunOnlyWhenCodegenPhaseIs;
import com.google.auto.service.AutoService;
import java.util.Set;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

@SupportedAnnotationTypes({
  "com.flipkart.krystal.vajram.graphql.api.GraphQLFetcher",
  "com.flipkart.krystal.annos.Generated"
})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
@SupportedOptions(CODEGEN_PHASE_KEY)
@RunOnlyWhenCodegenPhaseIs(MODELS)
public class GraphQLAnnotationProcessor extends AbstractKrystalAnnoProcessor {

  boolean generated;

  @Override
  protected boolean processImpl(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    CodeGenUtility util = codeGenUtil();
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
    if (!generated) {
      try {
        new GraphQLTypeAggregatorGen(codeGenUtil()).generate();
      } catch (Exception e) {
        util.error("[GraphQL Codegen Exception] " + getStackTraceAsString(e));
      }
      try {
        new GraphQLEntityGen(util).generate();
      } catch (Exception e) {
        util.error("[GraphQL Codegen Exception] " + getStackTraceAsString(e));
      }
      generated = true;
    }
    return false;
  }
}
