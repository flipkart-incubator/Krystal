package com.flipkart.krystal.vajram.graphql.codegen;

import static com.flipkart.krystal.vajram.graphql.codegen.SchemaReaderUtil.DATA_FETCHER;
import static com.flipkart.krystal.vajram.graphql.codegen.SchemaReaderUtil.GRAPHQL_AGGREGATOR;
import static com.flipkart.krystal.vajram.graphql.codegen.SchemaReaderUtil.REFERENCE_FETCHER;
import static com.flipkart.krystal.vajram.graphql.codegen.SchemaReaderUtil.entityTypeToReferenceFetcher;
import static com.flipkart.krystal.vajram.graphql.codegen.SchemaReaderUtil.getDataFetcherArgs;
import static com.flipkart.krystal.vajram.graphql.codegen.SchemaReaderUtil.getDirectiveArgumentString;
import static com.flipkart.krystal.vajram.graphql.codegen.SchemaReaderUtil.getEntityTypes;
import static com.flipkart.krystal.vajram.graphql.codegen.SchemaReaderUtil.getRefFetcherArgs;
import static com.flipkart.krystal.vajram.graphql.codegen.SchemaReaderUtil.reverseEntityTypeToFieldResolverMap;
import static javax.lang.model.element.Modifier.*;
import static javax.lang.model.element.Modifier.FINAL;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.*;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import com.flipkart.krystal.vajram.graphql.api.Entity;
import com.squareup.javapoet.*;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategyParameters;
import graphql.language.*;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.io.*;
import java.util.*;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;

/**
 * This plugin generates the type aggregators for the entities in the schema Type aggregators are
 * the vajrams that are responsible for aggregating the data from the data fetcher vajrams. We
 * needed type aggregators as we aren't using graphql traversal of entities, instead we are using
 * Krystal for graph traversal.
 */
@Slf4j
public class TypeAggregatorAutoGenerator {

  @SuppressWarnings("SpellCheckingInspection")
  public static final String PACKAGE_NAME_ENTITY = "com.flipkart.fkentity";

  public static final String GRAPHQL_RESPONSE = "GraphQLResponse";
  public static final ClassName schemaReaderUtil =
      ClassName.get("com.flipkart.fkEntities.typeAggregatorGenerator", "SchemaReaderUtil");
  public static final ClassName graphQlUtils =
      ClassName.bestGuess("com.flipkart.fkMobileApi.dal.utils.GraphQLUtils");

  private static final AnnotationSpec IF_ABSENT_FAIL =
      AnnotationSpec.builder(IfAbsent.class)
          .addMember("value", "$T.$L", IfAbsent.IfAbsentThen.class, "FAIL")
          .build();
  public static final ClassName VAJRAM_EXECUTION_STRATEGY =
      ClassName.get("com.flipkart.krystal.vajram.graphql.api", "VajramExecutionStrategy");

  void generateTypeAggregators(List<File> files) {

    SchemaParser schemaParser = new SchemaParser();
    TypeDefinitionRegistry typeDefinitionRegistry = new TypeDefinitionRegistry();
    files.forEach(file -> typeDefinitionRegistry.merge(schemaParser.parse(file)));
    SchemaReaderUtil.setFieldVajramsForEachEntity(typeDefinitionRegistry);

    Map<EntityTypeName, ObjectTypeDefinition> entityTypes = getEntityTypes(typeDefinitionRegistry);

    entityTypes.forEach(
        (entityName, entityTypeDefinition) -> {
          ClassName className = getAggregatorName(entityName);
          Map<String, List<GraphQlFieldSpec>> refToFieldMap =
              getDfToListOfFieldsDeRef(entityTypeDefinition);
          TypeSpec.Builder typeAggregator =
              TypeSpec.classBuilder(className)
                  .addModifiers(PUBLIC)
                  .addModifiers(ABSTRACT)
                  .superclass(
                      ParameterizedTypeName.get(
                          ClassName.get(ComputeVajramDef.class),
                          ClassName.get(PACKAGE_NAME_ENTITY, entityName.value())))
                  .addAnnotation(Vajram.class)
                  .addAnnotation(AnnotationSpec.builder(Slf4j.class).build())
                  .addTypes(getFacetDefinitions(entityName))
                  .addMethods(getInputResolvers(entityName, entityTypeDefinition))
                  .addMethod(outputLogic(entityName, entityTypeDefinition));
          refToFieldMap.forEach(
              (vajramClass, graphQlFieldSpecs) -> {
                String vajramId = vajramIdFromClassName(vajramClass);
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

          try {
            javaFile.writeToFile(outputDir);
          } catch (Exception e) {
            log.error("", e);
          }
        });
  }

  private static ClassName getAggregatorName(EntityTypeName entityName) {
    return ClassName.get(
        "com.flipkart.fkMobileApi.dal.typeAggregators." + entityName.value().toLowerCase(),
        entityName.value() + GRAPHQL_AGGREGATOR);
  }

  private static List<TypeSpec> getFacetDefinitions(EntityTypeName entityName) {
    TypeSpec.Builder _inputs = TypeSpec.classBuilder("_Inputs").addModifiers(STATIC);
    _inputs.addField(
        FieldSpec.builder(ClassName.get(PACKAGE_NAME_ENTITY, entityName.value()), "entity")
            .addAnnotation(IF_ABSENT_FAIL)
            .build());
    _inputs.addField(
        FieldSpec.builder(ExecutionContext.class, "graphql_executionContext")
            .addAnnotation(IF_ABSENT_FAIL)
            .build());
    _inputs.addField(
        FieldSpec.builder(VAJRAM_EXECUTION_STRATEGY, "graphql_executionStrategy")
            .addAnnotation(IF_ABSENT_FAIL)
            .build());
    _inputs.addField(
        FieldSpec.builder(ExecutionStrategyParameters.class, "graphql_executionStrategyParams")
            .addAnnotation(IF_ABSENT_FAIL)
            .build());

    TypeSpec.Builder _internalFacets =
        TypeSpec.classBuilder("_InternalFacets").addModifiers(STATIC);
    //    _internalFacets.addField(
    //        FieldSpec.builder(MetricRegistry.class, "metricRegistry")
    //            .addAnnotation(
    //                AnnotationSpec.builder(Named.class)
    //                    .addMember("value", "$S", "metric_registry")
    //                    .build())
    //            .addAnnotation(Inject.class)
    //            .addAnnotation(IF_ABSENT_FAIL)
    //            .build());

    for (Map.Entry<ClassName, List<GraphQlFieldSpec>> entry :
        reverseEntityTypeToFieldResolverMap.get(entityName).entrySet()) {
      ClassName vajramClass = entry.getKey();
      String facetName = vajramClass.simpleName();
      String vajramId = vajramIdFromClassName(vajramClass.simpleName());
      _internalFacets.addField(
          FieldSpec.builder(getResponseType(facetName, entry.getValue()), vajramId)
              .addAnnotation(
                  AnnotationSpec.builder(Dependency.class)
                      .addMember("onVajram", "$T.class", vajramClass)
                      .build())
              .build());
    }

    for (ClassName refFetcherVajram : entityTypeToReferenceFetcher.get(entityName).values()) {
      String vajramId = vajramIdFromClassName(refFetcherVajram.canonicalName());
      _internalFacets.addField(
          FieldSpec.builder(String.class, vajramId)
              .addAnnotation(
                  AnnotationSpec.builder(Dependency.class)
                      .addMember("onVajram", "$T.class", refFetcherVajram)
                      .build())
              .build());
    }

    return List.of(_inputs.build(), _internalFacets.build());
  }

  private static com.squareup.javapoet.TypeName getResponseType(
      String df, List<GraphQlFieldSpec> fieldsDeRef) {
    com.squareup.javapoet.TypeName responseType;
    if (fieldsDeRef.size() == 1) {
      responseType = fieldsDeRef.get(0).fieldType();
    } else {
      responseType =
          ClassName.get(PACKAGE_NAME_ENTITY, vajramIdFromClassName(df) + GRAPHQL_RESPONSE);
    }
    return responseType;
  }

  private static Map<String, List<GraphQlFieldSpec>> getDfToListOfFieldsDeRef(
      ObjectTypeDefinition fieldDefinition) {
    Map<String, List<GraphQlFieldSpec>> dfToListOfFieldsDeRef = new HashMap<>();

    fieldDefinition
        .getFieldDefinitions()
        .forEach(
            field -> {
              if (field.hasDirective(DATA_FETCHER)) {
                dfToListOfFieldsDeRef
                    .computeIfAbsent(
                        getDataFetcherArgs(field).canonicalName(), k -> new ArrayList<>())
                    .add(GraphQlFieldSpec.fromField(field));
              } else if (field.hasDirective(REFERENCE_FETCHER)) {
                dfToListOfFieldsDeRef
                    .computeIfAbsent(
                        getRefFetcherArgs(field).canonicalName(), k -> new ArrayList<>())
                    .add(GraphQlFieldSpec.fromField(field));
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

  private static String reverseCamelCase(String s) {
    return s.substring(0, 1).toLowerCase() + s.substring(1);
  }

  private static String camelCase(String s) {
    return s.substring(0, 1).toUpperCase() + s.substring(1);
  }

  @Nonnull
  private static MethodSpec outputLogic(
      EntityTypeName entityName, ObjectTypeDefinition entityTypeDefinition) {
    MethodSpec.Builder builder =
        MethodSpec.methodBuilder("output")
            .addAnnotation(
                AnnotationSpec.builder(SuppressWarnings.class)
                    .addMember("value", "$S", "OptionalUsedAsFieldOrParameterType")
                    .build())
            .addAnnotation(Output.class)
            .addModifiers(STATIC)
            .returns(ClassName.get(PACKAGE_NAME_ENTITY, entityName.value()));
    builder.addParameter(ClassName.get(PACKAGE_NAME_ENTITY, entityName.value()), "entity");
    for (Map.Entry<String, List<GraphQlFieldSpec>> entry :
        getDfToListOfFieldsDeRef(entityTypeDefinition).entrySet()) {
      String dataFetcherCanonicalName = entry.getKey();
      String facetName = vajramIdFromClassName(dataFetcherCanonicalName);
      builder.addParameter(
          ParameterizedTypeName.get(
              ClassName.get(Optional.class),
              getResponseType(dataFetcherCanonicalName, entry.getValue())),
          facetName);
      builder.addCode("$L", getFieldSetters(facetName, entry.getValue()));
    }

    return builder.addStatement("return entity").build();
  }

  private static String vajramIdFromClassName(String dataFetcherCanonicalName) {
    return dataFetcherCanonicalName.substring(dataFetcherCanonicalName.lastIndexOf('.') + 1);
  }

  @Nonnull
  private static List<MethodSpec> getInputResolvers(
      EntityTypeName entityType, ObjectTypeDefinition entityTypeDefinition) {
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

  @Nonnull
  private static MethodSpec getAbstractInputResolverFieldMethod(
      ClassName vajramClass, EntityTypeName entityType, String entityIdentifierKey) {
    String vajramId = vajramIdFromClassName(vajramClass.canonicalName());
    ClassName vajramReqClass = getRequestClassName(vajramClass);
    MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder(vajramId)
            .addAnnotation(
                AnnotationSpec.builder(Resolve.class)
                    .addMember(
                        "dep",
                        "$T.$L_n",
                        getFacetClassName(getAggregatorName(entityType)),
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
            .addParameter(ClassName.get(PACKAGE_NAME_ENTITY, entityType.value()), "entity")
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
                graphQlUtils,
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

  private static MethodSpec getAbstractInputResolverForRefMethod(
      EntityTypeName entityTypeName, GraphQlFieldSpec fieldName, ClassName vajramClass) {
    String vajramId = vajramIdFromClassName(vajramClass.canonicalName());
    ClassName vajramReqClass = getRequestClassName(vajramClass);
    MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder(vajramId)
            .addAnnotation(
                AnnotationSpec.builder(Resolve.class)
                    .addMember(
                        "dep",
                        "$T.$L_n",
                        getFacetClassName(getAggregatorName(entityTypeName)),
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
            .addParameter(VAJRAM_EXECUTION_STRATEGY, "graphql_executionStrategy")
            .addParameter(ExecutionStrategyParameters.class, "graphql_executionStrategyParams")
            .addParameter(ParameterizedTypeName.get(Optional.class, String.class), vajramId)
            .addCode(
                """
                            if ($T.isFieldQueriedInTheNestedType($S, $L) && $L.isPresent()) {
                              try {
                                $T entity = ($T) $T.refFieldToEntity.get($S).newInstance();
                                entity.id($L.get());
                                return $T.executeWith($T._builder()
                                    .graphql_executionContext(graphql_executionContext)
                                    .graphql_executionStrategy(graphql_executionStrategy)
                                    .graphql_executionStrategyParams(
                                      graphql_executionStrategy.newParametersForFieldExecution(
                                        graphql_executionContext,
                                        $T.newParameters(graphql_executionStrategyParams).build(),
                                        graphql_executionStrategyParams.getFields().getSubField($S)))
                                    .entity(entity));
                              } catch ($T e) {
                                return $T.skipExecution($S);
                              }
                            } else {
                              return $T.skipExecution($S);
                            }
                """,
                ClassName.get("com.flipkart.fkMobileApi.dal.utils", "GraphQLUtils"),
                vajramId,
                "graphql_executionStrategyParams",
                vajramId,
                Entity.class,
                Entity.class,
                schemaReaderUtil,
                fieldName,
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
