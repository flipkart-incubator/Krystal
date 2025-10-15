package com.flipkart.krystal.vajram.graphql.codegen;

import static com.google.common.base.Throwables.getStackTraceAsString;

import com.flipkart.krystal.vajram.codegen.common.models.CodeGenUtility;
import com.google.auto.service.AutoService;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
public class GraphQLAnnotationProcessor extends AbstractProcessor {

  boolean generated;

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    CodeGenUtility util = new CodeGenUtility(processingEnv, GraphQLTypeAggregatorGen.class);
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
        new GraphQLTypeAggregatorGen(util).generate();
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
