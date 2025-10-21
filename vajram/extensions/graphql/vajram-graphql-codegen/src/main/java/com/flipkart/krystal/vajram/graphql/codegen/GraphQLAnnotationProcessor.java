package com.flipkart.krystal.vajram.graphql.codegen;

import static com.flipkart.krystal.codegen.common.models.Constants.CODEGEN_PHASE_KEY;
import static com.google.common.base.Throwables.getStackTraceAsString;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.vajram.codegen.common.models.VajramCodeGenUtility;
import com.flipkart.krystal.vajram.graphql.api.GraphQLFetcher;
import com.google.auto.service.AutoService;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
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
public class GraphQLAnnotationProcessor extends AbstractProcessor {

  boolean generated;

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    VajramCodeGenUtility vajramGenUtil =
        new VajramCodeGenUtility(
            processingEnv,
            GraphQLTypeAggregatorGen.class,
            processingEnv.getOptions().get(CODEGEN_PHASE_KEY));
    CodeGenUtility util = vajramGenUtil.codegenUtil();
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
        new GraphQLTypeAggregatorGen(vajramGenUtil).generate();
      } catch (Exception e) {
        util.error("[GraphQL Codegen Exception] " + getStackTraceAsString(e));
      }
      try {
        new GraphQLEntityModelGen(util).generate();
      } catch (Exception e) {
        util.error("[GraphQL Codegen Exception] " + getStackTraceAsString(e));
      }
      generated = true;
    }
    return false;
  }
}
