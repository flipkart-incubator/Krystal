package com.flipkart.krystal.vajram.graphql.codegen;

import static com.flipkart.krystal.vajram.codegen.common.models.Constants.CODEGEN_PHASE_KEY;

import com.google.auto.service.AutoService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

@SupportedAnnotationTypes("com.flipkart.krystal.model.ModelRoot")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
public class GraphQLAggregatorGenerator extends AbstractProcessor {

  public static final String GRAPHQL_SCHEMA_EXTENSION = ".graphqls";

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    try {
      FileObject fileObject =
          processingEnv.getFiler().getResource(StandardLocation.SOURCE_PATH, "", "graphql_schemas");
      File graphqlsDir = new File(fileObject.toUri());
      if (!graphqlsDir.exists()) {
        return true;
      }
      String[] graphqlSchemaFileNames =
          graphqlsDir.list((dir, name) -> name.endsWith(GRAPHQL_SCHEMA_EXTENSION));
      if (graphqlSchemaFileNames == null) {
        return true;
      }
      for (String graphqlSchemaFileName : graphqlSchemaFileNames) {
        File graphqlSchemaFile = new File(graphqlsDir, graphqlSchemaFileName);
        if (!graphqlSchemaFile.exists()) {
          return true;
        }
        byte[] graphQLSchemaContent = Files.readAllBytes(graphqlSchemaFile.toPath());
      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return false;
  }
}
