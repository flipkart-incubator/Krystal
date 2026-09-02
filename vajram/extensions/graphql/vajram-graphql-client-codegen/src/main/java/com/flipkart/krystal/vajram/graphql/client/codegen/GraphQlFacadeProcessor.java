package com.flipkart.krystal.vajram.graphql.client.codegen;

import static com.flipkart.krystal.codegen.common.models.CodegenPhase.MODELS;
import static com.flipkart.krystal.codegen.common.models.Constants.CODEGEN_PHASE_KEY;
import static com.flipkart.krystal.codegen.common.models.Constants.MODULE_ROOT_PATH_KEY;
import static com.google.common.base.Throwables.getStackTraceAsString;

import com.flipkart.krystal.codegen.common.models.AbstractKrystalAnnoProcessor;
import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.models.RunOnlyWhenCodegenPhaseIs;
import com.google.auto.service.AutoService;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;

/**
 * Generates a {@code <OperationRoot>QueryFacade} class for every {@code @ForGraphQlOpReq}
 * -annotated variables model. Unlike the model-protocol codegen classes in {@code
 * vajram-graphql-codegen}, this is a standalone annotation processor - no model protocol is
 * involved, since the generated facade is a plain utility class, not a model.
 */
@SupportedAnnotationTypes("com.flipkart.krystal.vajram.graphql.client.api.ForGraphQlOpReq")
@AutoService(Processor.class)
@SupportedOptions({CODEGEN_PHASE_KEY, MODULE_ROOT_PATH_KEY})
@RunOnlyWhenCodegenPhaseIs(MODELS)
public class GraphQlFacadeProcessor extends AbstractKrystalAnnoProcessor {

  /** Memoizes parsed schemas per resolved schema-file path, across rounds. */
  private final Map<String, GraphQlFacadeGen.SchemaContext> schemaCache = new HashMap<>();

  @Override
  protected void processImpl(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    CodeGenUtility util = codeGenUtil();
    for (TypeElement annotation : annotations) {
      for (TypeElement variablesModel :
          ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(annotation))) {
        try {
          new GraphQlFacadeGen(util, schemaCache).generate(variablesModel);
        } catch (Exception e) {
          util.error(
              "[GraphQL Request Facade Codegen Exception] " + getStackTraceAsString(e),
              variablesModel);
        }
      }
    }
  }
}
