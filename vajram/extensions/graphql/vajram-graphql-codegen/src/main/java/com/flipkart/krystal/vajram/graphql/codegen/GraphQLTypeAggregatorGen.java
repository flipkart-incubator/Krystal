package com.flipkart.krystal.vajram.graphql.codegen;

import static com.flipkart.krystal.vajram.codegen.common.models.Constants._INTERNAL_FACETS_CLASS;
import static com.flipkart.krystal.vajram.graphql.codegen.SchemaReaderUtil.DATA_FETCHER;
import static com.flipkart.krystal.vajram.graphql.codegen.SchemaReaderUtil.GRAPHQL_AGGREGATOR;
import static com.flipkart.krystal.vajram.graphql.codegen.SchemaReaderUtil.REFERENCE_FETCHER;
import static com.flipkart.krystal.vajram.graphql.codegen.SchemaReaderUtil.entityTypeToReferenceFetcher;
import static com.flipkart.krystal.vajram.graphql.codegen.SchemaReaderUtil.getDirectiveArgumentString;
import static com.flipkart.krystal.vajram.graphql.codegen.SchemaReaderUtil.reverseEntityTypeToFieldResolverMap;
import static javax.lang.model.element.Modifier.*;
import static javax.lang.model.element.Modifier.FINAL;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.model.IfAbsent.IfAbsentThen;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.*;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import com.flipkart.krystal.vajram.graphql.api.AbstractGraphQLEntity;
import com.flipkart.krystal.vajram.graphql.api.GraphQLUtils;
import com.flipkart.krystal.vajram.graphql.api.VajramExecutionStrategy;
import com.squareup.javapoet.*;
import com.squareup.javapoet.TypeSpec.Builder;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategyParameters;
import graphql.language.*;
import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import javax.tools.JavaFileObject;
import lombok.extern.slf4j.Slf4j;

/**
 * This plugin generates the type aggregators for the entities in the schema Type aggregators are
 * the vajrams that are responsible for aggregating the data from the data fetcher vajrams. We
 * needed type aggregators as we aren't using graphql traversal of entities, instead we are using
 * Krystal for graph traversal.
 */
@Slf4j
public class GraphQLTypeAggregatorGen implements CodeGenerator {

  private static final String GRAPHQL_RESPONSE = "GraphQLResponse";
  private static final AnnotationSpec IF_ABSENT_FAIL =
      AnnotationSpec.builder(IfAbsent.class)
          .addMember("value", "$T.$L", IfAbsentThen.class, "FAIL")
          .build();

  private final CodeGenUtility util;
  private final SchemaReaderUtil schemaReaderUtil;

  public GraphQLTypeAggregatorGen(CodeGenUtility util) {
    this.util = util;
    this.schemaReaderUtil = new SchemaReaderUtil(util);
  }

  public void generate() {
    Map<GraphQLTypeName, ObjectTypeDefinition> entityTypes =
        schemaReaderUtil.getEntityTypes(schemaReaderUtil.typeDefinitionRegistry());
    util.note("******** generating typeAggregators **********");
    entityTypes.forEach(
        (entityName, entityTypeDefinition) -> {
          ClassName className = getAggregatorName(entityName.value());
          Map<ClassName, List<GraphQlFieldSpec>> refToFieldMap =
              getDfToListOfFieldsDeRef(entityTypeDefinition);
          Builder typeAggregator =
              util.classBuilder(className.simpleName(), "")
                  .addModifiers(PUBLIC)
                  .addModifiers(ABSTRACT)
                  .superclass(
                      ParameterizedTypeName.get(
                          ClassName.get(ComputeVajramDef.class),
                          ClassName.get(
                              schemaReaderUtil.getPackageNameForType(entityName.value()),
                              entityName.value())))
                  .addAnnotation(Vajram.class)
                  .addAnnotation(AnnotationSpec.builder(Slf4j.class).build())
                  .addTypes(getFacetDefinitions(entityName))
                  .addMethods(getInputResolvers(entityName, entityTypeDefinition))
                  .addMethod(outputLogic(entityName, entityTypeDefinition));
          refToFieldMap.forEach(
              (vajramClass, graphQlFieldSpecs) -> {
                String vajramId = vajramClass.simpleName();
                typeAggregator.addField(
                    FieldSpec.builder(
                            ParameterizedTypeName.get(Set.class, String.class),
                            vajramId + "_FIELDS",
                            PRIVATE,
                            STATIC,
                            FINAL)
                        .initializer(
                            "$T.of($L)",
                            Set.class,
                            graphQlFieldSpecs.stream()
                                .map(f -> CodeBlock.of("$S", f.fieldName()))
                                .collect(CodeBlock.joining(",")))
                        .build());
              });
          JavaFile javaFile =
              JavaFile.builder(className.packageName(), typeAggregator.build()).build();

          StringWriter writer = new StringWriter();
          try {
            javaFile.writeTo(writer);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
          try {
            try {
              JavaFileObject requestFile =
                  util.processingEnv().getFiler().createSourceFile(className.canonicalName());
              util.note("Successfully Create source file %s".formatted(className));
              try (PrintWriter out = new PrintWriter(requestFile.openWriter())) {
                out.println(writer);
              }
            } catch (Exception e) {
              util.error(
                  "Error creating java file for className: %s. Error: %s".formatted(className, e));
            }
          } catch (Exception e) {
            StringWriter exception = new StringWriter();
            e.printStackTrace(new PrintWriter(exception));
            util.error(
                "Error while generating file for class %s. Exception: %s"
                    .formatted(className, exception));
          }
        });
  }

  private ClassName getAggregatorName(String typeName) {
    return ClassName.get(
        schemaReaderUtil.getPackageNameForType(typeName), typeName + GRAPHQL_AGGREGATOR);
  }

  private List<TypeSpec> getFacetDefinitions(GraphQLTypeName entityName) {
    TypeSpec.Builder inputs = TypeSpec.classBuilder("_Inputs").addModifiers(STATIC);
    inputs.addField(
        FieldSpec.builder(
                ClassName.get(
                    schemaReaderUtil.getPackageNameForType(entityName.value()), entityName.value()),
                "entity")
            .addAnnotation(IF_ABSENT_FAIL)
            .build());
    inputs.addField(
        FieldSpec.builder(ExecutionContext.class, "graphql_executionContext")
            .addAnnotation(IF_ABSENT_FAIL)
            .build());
    inputs.addField(
        FieldSpec.builder(ClassName.get(VajramExecutionStrategy.class), "graphql_executionStrategy")
            .addAnnotation(IF_ABSENT_FAIL)
            .build());
    inputs.addField(
        FieldSpec.builder(ExecutionStrategyParameters.class, "graphql_executionStrategyParams")
            .addAnnotation(IF_ABSENT_FAIL)
            .build());

    TypeSpec.Builder internalFacets =
        TypeSpec.classBuilder(_INTERNAL_FACETS_CLASS).addModifiers(STATIC);

    for (Entry<ClassName, List<GraphQlFieldSpec>> entry :
        reverseEntityTypeToFieldResolverMap.get(entityName).entrySet()) {
      ClassName vajramClass = entry.getKey();
      String vajramId = vajramClass.simpleName();
      internalFacets.addField(
          FieldSpec.builder(getResponseType(vajramClass, entry.getValue()), vajramId)
              .addAnnotation(
                  AnnotationSpec.builder(Dependency.class)
                      .addMember("onVajram", "$T.class", vajramClass)
                      .build())
              .build());
    }

    for (Entry<GraphQlFieldSpec, ClassName> map :
        entityTypeToReferenceFetcher.get(entityName).entrySet()) {
      GraphQlFieldSpec fieldSpec = map.getKey();
      ClassName refFetcherVajram = map.getValue();
      String vajramId = refFetcherVajram.simpleName();

      internalFacets.addField(
          FieldSpec.builder(
                  fieldSpec.fieldDefinition().getType() instanceof ListType
                      ? ParameterizedTypeName.get(List.class, String.class)
                      : ClassName.get(String.class),
                  vajramId)
              .addAnnotation(
                  AnnotationSpec.builder(Dependency.class)
                      .addMember("onVajram", "$T.class", refFetcherVajram)
                      .build())
              .build());
    }

    return List.of(inputs.build(), internalFacets.build());
  }

  private com.squareup.javapoet.TypeName getResponseType(
      ClassName df, List<GraphQlFieldSpec> fieldsDeRef) {
    com.squareup.javapoet.TypeName responseType;
    if (fieldsDeRef.size() == 1) {
      responseType = fieldsDeRef.get(0).fieldType();
    } else {
      responseType = ClassName.get(df.packageName(), df.simpleName() + GRAPHQL_RESPONSE);
    }
    return responseType;
  }

  private Map<ClassName, List<GraphQlFieldSpec>> getDfToListOfFieldsDeRef(
      ObjectTypeDefinition fieldDefinition) {
    Map<ClassName, List<GraphQlFieldSpec>> dfToListOfFieldsDeRef = new HashMap<>();

    fieldDefinition
        .getFieldDefinitions()
        .forEach(
            field -> {
              if (field.hasDirective(DATA_FETCHER)) {
                dfToListOfFieldsDeRef
                    .computeIfAbsent(
                        schemaReaderUtil.getDataFetcherArgs(field), k -> new ArrayList<>())
                    .add(schemaReaderUtil.fieldSpecFromField(field, ""));
              } else if (field.hasDirective(REFERENCE_FETCHER)) {
                dfToListOfFieldsDeRef
                    .computeIfAbsent(
                        schemaReaderUtil.getRefFetcherArgs(field), k -> new ArrayList<>())
                    .add(schemaReaderUtil.fieldSpecFromField(field, ""));
              }
            });
    return dfToListOfFieldsDeRef;
  }

  private static CodeBlock getFieldSetters(
      String facetName, List<GraphQlFieldSpec> graphQlFieldSpecs) {
    CodeBlock.Builder codeBlockBuilder = CodeBlock.builder();
    if (graphQlFieldSpecs.size() == 1) {
      codeBlockBuilder.addStatement(
          "$L.ifPresent(entity::$L)", facetName, graphQlFieldSpecs.get(0).fieldName());
      return codeBlockBuilder.build();
    } else {
      codeBlockBuilder.add("if($L.isPresent()) {", facetName);
      for (GraphQlFieldSpec graphQlFieldSpec : graphQlFieldSpecs) {
        codeBlockBuilder.addStatement(
            "entity.$L($L.get().get$L())",
            graphQlFieldSpec.fieldName(),
            facetName,
            camelCase(graphQlFieldSpec.fieldName()));
      }
    }
    codeBlockBuilder.add("}");

    return codeBlockBuilder.build();
  }

  private static String camelCase(String s) {
    return s.substring(0, 1).toUpperCase() + s.substring(1);
  }

  private MethodSpec outputLogic(
      GraphQLTypeName entityName, ObjectTypeDefinition entityTypeDefinition) {
    MethodSpec.Builder builder =
        MethodSpec.methodBuilder("output")
            .addAnnotation(
                AnnotationSpec.builder(SuppressWarnings.class)
                    .addMember("value", "$S", "OptionalUsedAsFieldOrParameterType")
                    .build())
            .addAnnotation(Output.class)
            .addModifiers(STATIC)
            .returns(
                ClassName.get(
                    schemaReaderUtil.getPackageNameForType(entityName.value()),
                    entityName.value()));
    builder.addParameter(
        ClassName.get(
            schemaReaderUtil.getPackageNameForType(entityName.value()), entityName.value()),
        "entity");
    for (Entry<ClassName, List<GraphQlFieldSpec>> entry :
        getDfToListOfFieldsDeRef(entityTypeDefinition).entrySet()) {
      ClassName dataFetcherClassName = entry.getKey();
      String vajramId = dataFetcherClassName.simpleName();
      builder.addParameter(
          ParameterizedTypeName.get(
              ClassName.get(Optional.class),
              getResponseType(dataFetcherClassName, entry.getValue())),
          vajramId);
      builder.addCode("$L", getFieldSetters(vajramId, entry.getValue()));
    }

    return builder.addStatement("return entity").build();
  }

  private List<MethodSpec> getInputResolvers(
      GraphQLTypeName entityType, ObjectTypeDefinition entityTypeDefinition) {
    String entityIdentifierKey =
        getDirectiveArgumentString(entityTypeDefinition, "entity", "identifierKey").orElseThrow();
    List<MethodSpec> methodSpecList = new ArrayList<>();

    reverseEntityTypeToFieldResolverMap
        .get(entityType)
        .forEach(
            (vajramClass, value) ->
                methodSpecList.add(
                    getAbstractInputResolverFieldMethod(
                        vajramClass, entityType, entityIdentifierKey)));

    entityTypeToReferenceFetcher
        .get(entityType)
        .forEach(
            (field, vajramClass) ->
                methodSpecList.add(
                    getAbstractInputResolverForRefMethod(entityType, field, vajramClass)));
    return methodSpecList;
  }

  private MethodSpec getAbstractInputResolverFieldMethod(
      ClassName vajramClass, GraphQLTypeName entityType, String entityIdentifierKey) {
    String vajramId = vajramClass.simpleName();
    ClassName vajramReqClass = getRequestClassName(vajramClass);
    MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder(vajramId)
            .addAnnotation(
                AnnotationSpec.builder(Resolve.class)
                    .addMember(
                        "dep",
                        "$T.$L_n",
                        getFacetClassName(getAggregatorName(entityType.value())),
                        vajramId)
                    .addMember(
                        "depInputs",
                        "{$T.rawVariables_n, $T.requestContext_n,$T.queryContext_n, $T.$L_n}",
                        vajramReqClass,
                        vajramReqClass,
                        vajramReqClass,
                        vajramReqClass,
                        entityIdentifierKey)
                    .build())
            .addModifiers(STATIC)
            .returns(
                ParameterizedTypeName.get(
                    ClassName.get(One2OneCommand.class), getRequestClassName(vajramClass)))
            .addParameter(ExecutionContext.class, "graphql_executionContext")
            .addParameter(ExecutionStrategyParameters.class, "graphql_executionStrategyParams")
            .addParameter(
                ClassName.get(
                    schemaReaderUtil.getPackageNameForType(entityType.value()), entityType.value()),
                "entity")
            .addCode(
"""
            if ($T.isFieldQueriedInTheNestedType($L_FIELDS, $L)) {
              return $T.executeWith($T._builder()
                  .rawVariables(graphql_executionContext.getExecutionInput().getRawVariables())
                  .requestContext(graphql_executionContext.getGraphQLContext().get("requestContext"))
                  .queryContext(graphql_executionContext.getGraphQLContext().get("queryContext"))
                  .$L(entity.id()));
            } else {
              return $T.skipExecution($S);
            }
""",
                GraphQLUtils.class,
                vajramId,
                "graphql_executionStrategyParams",
                One2OneCommand.class,
                ClassName.get(
                    vajramClass.packageName(), vajramClass.simpleName() + "_ReqImmutPojo"),
                entityIdentifierKey,
                One2OneCommand.class,
                vajramId);

    return methodBuilder.build();
  }

  private static ClassName getRequestClassName(ClassName aggregatorName) {
    return ClassName.get(aggregatorName.packageName(), aggregatorName.simpleName() + "_Req");
  }

  private static ClassName getFacetClassName(ClassName aggregatorName) {
    return ClassName.get(aggregatorName.packageName(), aggregatorName.simpleName() + "_Fac");
  }

  private MethodSpec getAbstractInputResolverForRefMethod(
      GraphQLTypeName graphQLTypeName, GraphQlFieldSpec fieldName, ClassName vajramClass) {
    String vajramId = vajramClass.simpleName();
    ClassName vajramReqClass = getRequestClassName(vajramClass);
    MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder(vajramId)
            .addAnnotation(
                AnnotationSpec.builder(Resolve.class)
                    .addMember(
                        "dep",
                        "$T.$L_n",
                        getFacetClassName(getAggregatorName(graphQLTypeName.value())),
                        vajramId)
                    .addMember(
                        "depInputs",
                        "{$T.graphql_executionContext_n, $T.graphql_executionStrategy_n, $T.graphql_executionStrategyParams_n, $T.entity_n}",
                        vajramReqClass,
                        vajramReqClass,
                        vajramReqClass,
                        vajramReqClass)
                    .build())
            .addModifiers(STATIC)
            .returns(
                ParameterizedTypeName.get(
                    ClassName.get(One2OneCommand.class), getRequestClassName(vajramClass)))
            .addParameter(ExecutionContext.class, "graphql_executionContext")
            .addParameter(ClassName.get(VajramExecutionStrategy.class), "graphql_executionStrategy")
            .addParameter(ExecutionStrategyParameters.class, "graphql_executionStrategyParams")
            .addParameter(ParameterizedTypeName.get(Optional.class, String.class), vajramId)
            .addCode(
                """
                            if ($T.isFieldQueriedInTheNestedType($S, $L) && $L.isPresent()) {
                              try {
                                $T _entity = new $T();
                                _entity.id($L.get());
                                return $T.executeWith($T._builder()
                                    .entity(_entity)
                                    .graphql_executionContext(graphql_executionContext)
                                    .graphql_executionStrategy(graphql_executionStrategy)
                                    .graphql_executionStrategyParams(
                                      graphql_executionStrategy.newParametersForFieldExecution(
                                        graphql_executionContext,
                                        $T.newParameters(graphql_executionStrategyParams).build(),
                                        graphql_executionStrategyParams.getFields().getSubField($S))));
                              } catch ($T e) {
                                return $T.skipExecution($S);
                              }
                            } else {
                              return $T.skipExecution($S);
                            }
                """,
                GraphQLUtils.class,
                vajramId,
                "graphql_executionStrategyParams",
                vajramId,
                AbstractGraphQLEntity.class,
                fieldName.fieldType(),
                vajramId,
                One2OneCommand.class,
                ClassName.get(
                    vajramClass.packageName(), vajramClass.simpleName() + "_ReqImmutPojo"),
                ExecutionStrategyParameters.class,
                fieldName,
                Throwable.class,
                One2OneCommand.class,
                vajramId,
                One2OneCommand.class,
                vajramId);

    return methodBuilder.build();
  }
}
