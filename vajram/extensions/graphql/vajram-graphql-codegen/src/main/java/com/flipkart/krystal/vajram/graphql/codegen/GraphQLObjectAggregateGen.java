package com.flipkart.krystal.vajram.graphql.codegen;

import static com.flipkart.krystal.codegen.common.models.Constants.EMPTY_CODE_BLOCK;
import static com.flipkart.krystal.codegen.common.models.Constants.IMMUT_SUFFIX;
import static com.flipkart.krystal.model.PlainJavaObject.POJO;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.REQUEST_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants._INTERNAL_FACETS_CLASS;
import static com.flipkart.krystal.vajram.graphql.api.Constants.Directives.DATA_FETCHER;
import static com.flipkart.krystal.vajram.graphql.api.Constants.Directives.ID_FETCHER;
import static com.flipkart.krystal.vajram.graphql.api.Constants.GRAPHQL_AGGREGATOR_SUFFIX;
import static com.flipkart.krystal.vajram.graphql.codegen.CodeGenConstants.IF_ABSENT_FAIL;
import static com.flipkart.krystal.vajram.graphql.codegen.GraphQlFetcherType.INHERIT_ID_FROM_ARGS;
import static com.flipkart.krystal.vajram.graphql.codegen.GraphQlFetcherType.INHERIT_ID_FROM_PARENT;
import static com.flipkart.krystal.vajram.graphql.codegen.GraphQlFetcherType.TYPE_AGGREGATOR;
import static com.flipkart.krystal.vajram.graphql.codegen.SchemaReaderUtil.getDirectiveArgumentString;
import static java.util.Map.entry;
import static javax.lang.model.element.Modifier.*;
import static javax.lang.model.element.Modifier.FINAL;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.*;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import com.flipkart.krystal.vajram.graphql.api.Constants.DirectiveArgs;
import com.flipkart.krystal.vajram.graphql.api.Constants.Directives;
import com.flipkart.krystal.vajram.graphql.api.Constants.Facets;
import com.flipkart.krystal.vajram.graphql.api.execution.GraphQLUtils;
import com.flipkart.krystal.vajram.graphql.api.execution.VajramExecutionStrategy;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlResponseJson;
import com.flipkart.krystal.vajram.graphql.api.traits.GraphQlOperationAggregate;
import com.google.common.collect.ImmutableMap;
import com.squareup.javapoet.*;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec.Builder;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategyParameters;
import graphql.language.*;
import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import javax.tools.JavaFileObject;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This plugin generates the type aggregators for the entities in the schema Type aggregators are
 * the vajrams that are responsible for aggregating the data from the data fetcher vajrams. We
 * needed type aggregators as we aren't using graphql traversal of entities, instead we are using
 * Krystal for graph traversal.
 */
@Slf4j
public class GraphQLObjectAggregateGen implements CodeGenerator {

  public static final String GRAPHQL_RESPONSE = "_GQlFields";

  private final CodeGenUtility util;
  private final SchemaReaderUtil schemaReaderUtil;
  private final GraphQlCodeGenUtil graphQlCodeGenUtil;

  public GraphQLObjectAggregateGen(CodeGenUtility util) {
    this.util = util;
    this.graphQlCodeGenUtil = new GraphQlCodeGenUtil(util);
    this.schemaReaderUtil = graphQlCodeGenUtil.schemaReaderUtil();
  }

  public void generate() {
    util.note("******** generating typeAggregators **********");
    schemaReaderUtil
        .aggregatableTypes()
        .forEach(
            (objectTypeName, typeDefinition) -> {
              ClassName className = getAggregatorName(objectTypeName);
              Map<ClassName, List<GraphQlFieldSpec>> refToFieldMap =
                  getDfToListOfFieldsDeRef(typeDefinition);
              TypeSpec.Builder typeAggregator =
                  util.classBuilder(className.simpleName(), "")
                      .addModifiers(PUBLIC)
                      .addModifiers(ABSTRACT)
                      .superclass(
                          ParameterizedTypeName.get(
                              ClassName.get(ComputeVajramDef.class),
                              asVajramReturnType(objectTypeName)))
                      .addAnnotation(Vajram.class)
                      .addAnnotation(AnnotationSpec.builder(Slf4j.class).build())
                      .addTypes(createFacetDefinitions(typeDefinition))
                      .addMethods(getInputResolvers(objectTypeName, typeDefinition))
                      .addMethod(outputLogic(objectTypeName));
              ObjectTypeDefinition queryType = schemaReaderUtil.queryType();
              if (queryType != null && queryType.getName().equals(objectTypeName.value())) {
                typeAggregator.addSuperinterface(
                    ParameterizedTypeName.get(
                        ClassName.get(GraphQlOperationAggregate.class),
                        asVajramReturnType(objectTypeName)));
              }
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
                      "Error creating java file for className: %s. Error: %s"
                          .formatted(className, e));
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

  private ClassName asVajramReturnType(GraphQLTypeName objectTypeName) {
    ClassName className = schemaReaderUtil.typeClassName(objectTypeName);
    return ClassName.get(className.packageName(), className.simpleName() + "_" + IMMUT_SUFFIX)
        .nestedClass("Builder");
  }

  private ClassName getAggregatorName(GraphQLTypeName typeName) {
    return ClassName.get(
        schemaReaderUtil.getPackageNameForType(typeName),
        typeName.value() + GRAPHQL_AGGREGATOR_SUFFIX);
  }

  private List<TypeSpec> createFacetDefinitions(ObjectTypeDefinition typeDefinition) {
    GraphQLTypeName typeName = GraphQLTypeName.of(typeDefinition);
    Optional<String> composingEntityType =
        getDirectiveArgumentString(
            typeDefinition, Directives.COMPOSED_TYPE, DirectiveArgs.IN_ENTITY);
    boolean isEntity = typeDefinition.hasDirective(Directives.ENTITY);
    Builder inputs = TypeSpec.classBuilder("_Inputs").addModifiers(STATIC);
    GraphQLTypeName composingEntityTypeName = null;
    if (isEntity) {
      composingEntityTypeName = typeName;
    } else if (composingEntityType.isPresent()) {
      composingEntityTypeName = GraphQLTypeName.of(composingEntityType.get());
    }
    if (composingEntityTypeName != null) {
      inputs.addField(
          FieldSpec.builder(
                  schemaReaderUtil.entityIdClassName(
                      schemaReaderUtil.typeClassName(composingEntityTypeName)),
                  Facets.ENTITY_ID)
              .addAnnotation(IF_ABSENT_FAIL)
              .build());
    }
    inputs.addField(
        FieldSpec.builder(ExecutionContext.class, Facets.EXECUTION_CONTEXT)
            .addAnnotation(IF_ABSENT_FAIL)
            .build());
    inputs.addField(
        FieldSpec.builder(ClassName.get(VajramExecutionStrategy.class), Facets.EXECUTION_STRATEGY)
            .addAnnotation(IF_ABSENT_FAIL)
            .build());
    inputs.addField(
        FieldSpec.builder(ExecutionStrategyParameters.class, Facets.EXECUTION_STRATEGY_PARAMS)
            .addAnnotation(IF_ABSENT_FAIL)
            .build());

    Builder internalFacets = TypeSpec.classBuilder(_INTERNAL_FACETS_CLASS).addModifiers(STATIC);

    Map<Fetcher, List<GraphQlFieldSpec>> fetcherToFields =
        schemaReaderUtil.typeToFetcherToFields().get(typeName);
    for (Entry<Fetcher, List<GraphQlFieldSpec>> entry : fetcherToFields.entrySet()) {
      if (entry.getKey() instanceof VajramFetcher fetcher) {
        List<GraphQlFieldSpec> fields = entry.getValue();
        internalFacets.addField(
            FieldSpec.builder(
                    getFetcherResponseType(fetcher, fields), getFacetName(fetcher, fields))
                .addAnnotation(
                    AnnotationSpec.builder(Dependency.class)
                        .addMember("onVajram", "$T.class", fetcher.vajramClassName())
                        .build())
                .build());
      }
    }

    for (Entry<GraphQlFieldSpec, ClassName> fieldToTypeAggregator :
        schemaReaderUtil.entityTypeToFieldToTypeAggregator().get(typeName).entrySet()) {
      GraphQlFieldSpec fieldSpec = fieldToTypeAggregator.getKey();
      ClassName typeAggregatorClassName = fieldToTypeAggregator.getValue();

      AnnotationSpec.Builder depAnnotation =
          AnnotationSpec.builder(Dependency.class)
              .addMember("onVajram", "$T.class", typeAggregatorClassName);
      if (isGraphQlList(fieldSpec)) {
        depAnnotation.addMember("canFanout", "true");
      }

      internalFacets.addField(
          FieldSpec.builder(asVajramReturnType(fieldSpec), fieldSpec.fieldName())
              .addAnnotation(depAnnotation.build())
              .build());
    }

    return List.of(inputs.build(), internalFacets.build());
  }

  private ClassName asVajramReturnType(GraphQlFieldSpec fieldSpec) {
    return asVajramReturnType(
        GraphQLTypeName.of(
            schemaReaderUtil
                .typeDefinitionRegistry()
                .getType(getDeclaredActualFieldType(fieldSpec).graphQlType())
                .orElseThrow()));
  }

  /**
   * Returns the declared field type for a graphql field. For example, if the field type is any of
   * {@code A}, {@code A!}, {@code [A]}, {@code [A]!}, {@code [A!]}, {@code [A!]!}, this method
   * returns {@code A}
   */
  private GraphQlTypeDecorator getDeclaredActualFieldType(GraphQlFieldSpec fieldSpec) {
    GraphQlTypeDecorator currentType = fieldSpec.fieldType();
    if (currentType.isNonNull()) {
      currentType = currentType.innerType();
    }
    if (currentType.isList()) {
      currentType = currentType.innerType();
    }
    if (currentType.isNonNull()) {
      currentType = currentType.innerType();
    }
    return currentType;
  }

  /**
   * Returns true if the field is of type {@code [A]}, {@code [A]!}, {@code [A!]} or {@code [A!]!}
   */
  private boolean isGraphQlList(GraphQlFieldSpec fieldSpec) {
    GraphQlTypeDecorator currentType = fieldSpec.fieldType();
    if (currentType.isNonNull()) {
      currentType = currentType.innerType();
    }
    return currentType.isList();
  }

  private static String getFacetName(VajramFetcher fetcher, List<GraphQlFieldSpec> fields) {
    return switch (fetcher.type()) {
      case MULTI_FIELD_DATA_FETCHER, ID_FETCHER -> fetcher.vajramClassName().simpleName();
      default -> fields.get(0).fieldName();
    };
  }

  private TypeName getFetcherResponseType(
      VajramFetcher fetcher, List<GraphQlFieldSpec> fieldsDeRef) {
    ClassName fetcherClassName = fetcher.vajramClassName();
    TypeName responseType;
    if (fieldsDeRef.size() == 1) {
      GraphQlFieldSpec fieldSpec = fieldsDeRef.get(0);
      FieldDefinition fieldDefinition = fieldSpec.fieldDefinition();
      Optional<TypeDefinition> typeDefinition =
          schemaReaderUtil.typeDefinitionRegistry().getType(fieldDefinition.getType());
      if (typeDefinition.isPresent()
          && typeDefinition.get().getDirectivesByName().containsKey("entity")) {
        GraphQLTypeName refEntityName = new GraphQLTypeName(typeDefinition.get().getName());
        ClassName entityIdClassName =
            schemaReaderUtil.entityIdClassName(schemaReaderUtil.typeClassName(refEntityName));
        GraphQlTypeDecorator innerType = fieldSpec.fieldType();
        boolean isInnerNonNull = false;
        if (innerType.isNonNull()) {
          innerType = innerType.innerType();
        }
        boolean isList = false;
        if (innerType.isList()) {
          innerType = innerType.innerType();
          isList = true;
        }
        if (innerType.isNonNull()) {
          isInnerNonNull = true;
        }
        if (isList) {
          responseType =
              ParameterizedTypeName.get(
                  ClassName.get(List.class),
                  isInnerNonNull
                      ? entityIdClassName
                      : entityIdClassName.annotated(
                          AnnotationSpec.builder(Nullable.class).build()));
        } else {
          responseType = entityIdClassName;
        }
      } else {
        responseType = graphQlCodeGenUtil.toTypeNameForField(fieldSpec.fieldType(), fieldSpec);
      }
    } else {
      responseType =
          ClassName.get(
              fetcherClassName.packageName(), fetcherClassName.simpleName() + GRAPHQL_RESPONSE);
    }
    return responseType;
  }

  private Map<ClassName, List<GraphQlFieldSpec>> getDfToListOfFieldsDeRef(
      ObjectTypeDefinition typeDefinition) {
    Map<ClassName, List<GraphQlFieldSpec>> dfToListOfFieldsDeRef = new HashMap<>();
    GraphQLTypeName enclosingType = GraphQLTypeName.of(typeDefinition);
    typeDefinition
        .getFieldDefinitions()
        .forEach(
            field -> {
              if (field.hasDirective(DATA_FETCHER)) {
                dfToListOfFieldsDeRef
                    .computeIfAbsent(
                        schemaReaderUtil.getDataFetcherClassName(field), k -> new ArrayList<>())
                    .add(schemaReaderUtil.fieldSpecFromField(field, "", enclosingType));
              } else if (field.hasDirective(ID_FETCHER)) {
                dfToListOfFieldsDeRef
                    .computeIfAbsent(
                        schemaReaderUtil.getIdFetcherClassName(field), k -> new ArrayList<>())
                    .add(schemaReaderUtil.fieldSpecFromField(field, "", enclosingType));
              }
            });
    return dfToListOfFieldsDeRef;
  }

  private CodeBlock getFieldSetters(
      VajramFetcher fetcher, List<GraphQlFieldSpec> graphQlFieldSpecs) {
    CodeBlock.Builder codeBlockBuilder = CodeBlock.builder();
    String facetName = getFacetName(fetcher, graphQlFieldSpecs);

    // Multiple fields from same fetcher: GetOrderItemNames returns {orderItemNames, name}
    for (GraphQlFieldSpec graphQlFieldSpec : graphQlFieldSpecs) {
      boolean canFanout = isGraphQlList(graphQlFieldSpec);

      if (TYPE_AGGREGATOR.equals(fetcher.type()) && canFanout) {
        // Fanout case: dummies.responses().handle(...)
        codeBlockBuilder.addNamed(
            """
            entity.$fieldName:L($errable:T.withValue($facetName:L.responses()));
            """,
            Map.ofEntries(
                entry("facetName", facetName),
                entry("fieldName", graphQlFieldSpec.fieldName()),
                entry("errable", Errable.class)));
      } else {
        // This covers:
        // - One-to-one type aggregator:
        // - Data fetcher returning single field
        // - Data fetcher returning multiple fields:
        //     Ex: GetOrderItemNames returns {orderItemNames, name}
        codeBlockBuilder.addNamed(
            """
            $facetName:L.handle(
                _failure -> entity.$fieldName:L(_failure.cast()),
                _nonNil -> entity.$fieldName:L(_nonNil.value()$fieldExtractor:L));

            """,
            Map.ofEntries(
                entry("facetName", facetName),
                entry("fieldName", graphQlFieldSpec.fieldName()),
                entry(
                    "fieldExtractor",
                    graphQlFieldSpecs.size() > 1
                        ? CodeBlock.of(".$L()", graphQlFieldSpec.fieldName())
                        : CodeBlock.of(""))));
      }
    }
    return codeBlockBuilder.build();
  }

  private MethodSpec outputLogic(GraphQLTypeName objectTypeName) {
    TypeDefinition objectTypeDefinition =
        schemaReaderUtil.typeDefinitionRegistry().getType(objectTypeName.value()).orElseThrow();
    boolean isEntity = objectTypeDefinition.hasDirective(Directives.ENTITY);
    String entityIdFieldName = schemaReaderUtil.getEntityIdFieldName(objectTypeDefinition);
    ClassName immutGQlRespJsonClassName = getImmutGQlRespJsonClassName(objectTypeName);
    MethodSpec.Builder builder =
        MethodSpec.methodBuilder("output")
            .addAnnotation(Output.class)
            .addModifiers(STATIC)
            .returns(asVajramReturnType(objectTypeName));
    if (isEntity) {
      builder.addParameter(
          schemaReaderUtil.entityIdClassName(schemaReaderUtil.typeClassName(objectTypeName)),
          Facets.ENTITY_ID);
    }
    builder.addStatement(
        "$T.Builder entity = $T._builder()$L",
        immutGQlRespJsonClassName,
        immutGQlRespJsonClassName,
        // Only entities have ids
        isEntity ? CodeBlock.of(".$L($L)", entityIdFieldName, Facets.ENTITY_ID) : EMPTY_CODE_BLOCK);
    schemaReaderUtil
        .typeToFetcherToFields()
        .getOrDefault(objectTypeName, Map.of())
        .forEach(
            (fetcher, fields) -> {
              if (!(fetcher instanceof VajramFetcher vajramFetcher)
                  || vajramFetcher.type().equals(GraphQlFetcherType.ID_FETCHER)) {
                // ID Fetchers are not needed in output logic
                return;
              }
              builder.addParameter(
                  ParameterizedTypeName.get(
                      ClassName.get(Errable.class), getFetcherResponseType(vajramFetcher, fields)),
                  getFacetName(vajramFetcher, fields));
              builder.addCode("$L", getFieldSetters(vajramFetcher, fields));
            });
    schemaReaderUtil
        .entityTypeToFieldToTypeAggregator()
        .getOrDefault(objectTypeName, Map.of())
        .forEach(
            (fieldSpec, aggregatorClassName) -> {
              VajramFetcher fetcher = new VajramFetcher(aggregatorClassName, TYPE_AGGREGATOR);
              boolean canFanout = isGraphQlList(fieldSpec);
              builder.addParameter(
                  canFanout
                      ? ParameterizedTypeName.get(
                          ClassName.get(FanoutDepResponses.class),
                          getRequestClassName(aggregatorClassName),
                          asVajramReturnType(fieldSpec))
                      : ParameterizedTypeName.get(
                          ClassName.get(Errable.class), asVajramReturnType(fieldSpec)),
                  fieldSpec.fieldName());
              builder.addCode("$L", getFieldSetters(fetcher, List.of(fieldSpec)));
            });
    builder.addParameter(ExecutionContext.class, "graphql_executionContext");
    builder.addParameter(VajramExecutionStrategy.class, "graphql_executionStrategy");
    builder.addParameter(ExecutionStrategyParameters.class, "graphql_executionStrategyParams");
    return builder
        .addStatement(
            """
        return entity
            .graphql_executionStrategy(graphql_executionStrategy)
            .graphql_executionContext(graphql_executionContext)
            .graphql_executionStrategyParams(graphql_executionStrategyParams)""")
        .build();
  }

  private ClassName getImmutGQlRespJsonClassName(GraphQLTypeName objectTypeName) {
    return ClassName.get(
        schemaReaderUtil.getPackageNameForType(objectTypeName),
        objectTypeName.value()
            + "_"
            + IMMUT_SUFFIX
            + GraphQlResponseJson.INSTANCE.modelClassesSuffix());
  }

  private List<MethodSpec> getInputResolvers(
      GraphQLTypeName entityType, ObjectTypeDefinition typeDefinition) {
    List<MethodSpec> methodSpecList = new ArrayList<>();

    schemaReaderUtil
        .typeToFetcherToFields()
        .get(entityType)
        .forEach(
            (fetcher, fields) -> {
              if (fetcher instanceof VajramFetcher vajramFetcher) {
                methodSpecList.add(
                    GraphQLObjectAggregateGen.this.createFetcherInputResolver(
                        vajramFetcher, fields, entityType, typeDefinition));
              }
            });

    schemaReaderUtil
        .entityTypeToFieldToTypeAggregator()
        .get(entityType)
        .forEach(
            (field, typeAggregatorClass) -> {
              @Nullable Fetcher fetcher =
                  schemaReaderUtil.entityTypeToFieldToFetcher().get(entityType).get(field);
              if (fetcher != null) {
                methodSpecList.add(
                    createTypeAggregatorInputResolver(
                        fetcher, entityType, typeDefinition, field, typeAggregatorClass));
              }
            });
    return methodSpecList;
  }

  private MethodSpec createFetcherInputResolver(
      VajramFetcher fetcher,
      List<GraphQlFieldSpec> fields,
      GraphQLTypeName parentTypeName,
      TypeDefinition parentTypeDef) {

    String vajramId = fetcher.vajramClassName().simpleName();
    ClassName vajramReqClass = getRequestClassName(fetcher.vajramClassName());
    boolean isParentOpType = schemaReaderUtil.operationTypes().containsKey(parentTypeName);
    boolean parentTypeHasEntityId = !isParentOpType;

    ClassName entityClassName = schemaReaderUtil.typeClassName(parentTypeName);
    ClassName entityIdClassName = schemaReaderUtil.entityIdClassName(entityClassName);
    String facetName = getFacetName(fetcher, fields);
    List<CodeBlock> depInputNames = new ArrayList<>();
    List<CodeBlock> depInputSetterCode = new ArrayList<>();
    if (parentTypeHasEntityId) {
      depInputNames.add(
          CodeBlock.of(
              "$T.$L_n", vajramReqClass, schemaReaderUtil.getEntityIdFieldName(parentTypeDef)));
      depInputSetterCode.add(
          CodeBlock.of(
              ".$L($L)", schemaReaderUtil.getEntityIdFieldName(parentTypeDef), Facets.ENTITY_ID));
    }

    if (fields.size() == 1) {
      for (InputValueDefinition inputValueDefinition :
          fields.get(0).fieldDefinition().getInputValueDefinitions()) {
        String argName = inputValueDefinition.getName();
        depInputNames.add(CodeBlock.of("$T.$L_n", vajramReqClass, argName));
        depInputSetterCode.add(
            CodeBlock.of(
                ".$L($L.getExecutionStepInfo().getArgument($S))",
                argName,
                Facets.EXECUTION_STRATEGY_PARAMS + "_new",
                argName));
      }
    }

    MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder(facetName)
            .addAnnotation(
                AnnotationSpec.builder(Resolve.class)
                    .addMember(
                        "dep",
                        "$T.$L_n",
                        getFacetClassName(getAggregatorName(parentTypeName)),
                        facetName)
                    .addMember(
                        "depInputs",
                        "$L",
                        depInputNames.stream().collect(CodeBlock.joining(",", "{", "}")))
                    .build())
            .addModifiers(STATIC)
            .returns(
                ParameterizedTypeName.get(
                    ClassName.get(One2OneCommand.class),
                    getRequestClassName(fetcher.vajramClassName())))
            .addParameter(ExecutionContext.class, Facets.EXECUTION_CONTEXT)
            .addParameter(VajramExecutionStrategy.class, Facets.EXECUTION_STRATEGY)
            .addParameter(ExecutionStrategyParameters.class, Facets.EXECUTION_STRATEGY_PARAMS)
            .addCode(
"""
            if ($T.isAnyFieldQueried($L_FIELDS, $L)) {
              $L
              return $T.executeWith(
                  $T._builder()
                      $L);
            } else {
              return $T.skipExecution($S);
            }
""",
                GraphQLUtils.class,
                vajramId,
                Facets.EXECUTION_STRATEGY_PARAMS,
                fields.size() == 1
                    ?
                    // Modify the strategy params only if exactly one field is fetched by this
                    // fetcher. If the fields are multiple (i.e. this is a
                    // MULTI_FIELD_DATA_FETCHER), then  modifying the strategy params has no
                    // utility.
                    CodeBlock.of(
"""
              var graphql_executionStrategyParams_new =
                  graphql_executionStrategy.newParametersForFieldExecution(
                      graphql_executionContext,
                      graphql_executionStrategyParams,
                      graphql_executionStrategyParams.getFields().getSubField($S));
""",
                        fields.get(0).fieldName())
                    : EMPTY_CODE_BLOCK,
                One2OneCommand.class,
                ClassName.get(
                    fetcher.vajramClassName().packageName(),
                    fetcher.vajramClassName().simpleName()
                        + REQUEST_SUFFIX
                        + IMMUT_SUFFIX
                        + POJO.modelClassesSuffix()),
                depInputSetterCode.stream().collect(CodeBlock.joining("\n")),
                One2OneCommand.class,
                vajramId);
    if (parentTypeHasEntityId) {
      methodBuilder.addParameter(entityIdClassName, Facets.ENTITY_ID);
    }
    return methodBuilder.build();
  }

  private static ClassName getRequestClassName(ClassName vajramClass) {
    return ClassName.get(vajramClass.packageName(), vajramClass.simpleName() + "_Req");
  }

  private static ClassName getFacetClassName(ClassName aggregatorName) {
    return ClassName.get(aggregatorName.packageName(), aggregatorName.simpleName() + "_Fac");
  }

  private MethodSpec createTypeAggregatorInputResolver(
      Fetcher fetcher,
      GraphQLTypeName parentTypeName,
      ObjectTypeDefinition parentTypeDefinition,
      GraphQlFieldSpec fieldSpec,
      ClassName typeAggregatorClass) {
    getDirectiveArgumentString(
        parentTypeDefinition, Directives.COMPOSED_TYPE, DirectiveArgs.IN_ENTITY);
    Optional<GraphQLTypeName> parentComposingEntityType =
        schemaReaderUtil.getComposingEntityType(parentTypeDefinition);

    boolean isParentOpType = schemaReaderUtil.operationTypes().containsKey(parentTypeName);
    boolean parentTypeHasEntityId = !isParentOpType;
    boolean canFanout = isGraphQlList(fieldSpec);
    ObjectTypeDefinition fieldTypeDef =
        (ObjectTypeDefinition)
            schemaReaderUtil
                .typeDefinitionRegistry()
                .getType(fieldSpec.fieldType().graphQlType())
                .orElseThrow(
                    () ->
                        new IllegalStateException(
                            "Could not find type "
                                + fieldSpec.fieldType().graphQlType()
                                + " of field "
                                + fieldSpec.fieldDefinition()));
    GraphQLTypeName fieldTypeName = GraphQLTypeName.of(fieldTypeDef);
    Optional<GraphQLTypeName> fieldComposingEntityType =
        schemaReaderUtil.getComposingEntityType(fieldTypeDef);
    ClassName vajramReqClass = getRequestClassName(typeAggregatorClass);
    String fieldName = fieldSpec.fieldName();
    String entityIdFacetName =
        fetcher instanceof VajramFetcher vajramFetcher
            ? vajramFetcher.vajramClassName().simpleName()
            : parentTypeHasEntityId ? Facets.ENTITY_ID : "";
    String entityIdFieldName = schemaReaderUtil.getEntityIdFieldName(fieldTypeDef);
    GraphQlFetcherType fetcherType = fetcher.type();
    if (isParentOpType) {
      if (fieldSpec.fieldDefinition().getInputValueDefinitions().size() != 1) {
        throw util.errorAndThrow(
            "Entity fields in operation types must contain exactly one argument: " + fieldName);
      } else {
        String argName = fieldSpec.fieldDefinition().getInputValueDefinitions().get(0).getName();
        if (!entityIdFieldName.equals(argName)) {
          throw util.errorAndThrow(
              "Entity field argument name '%s' in operation type '%s' does not match entity id '%s' of entity '%s'"
                  .formatted(
                      argName, parentTypeName.value(), entityIdFieldName, fieldTypeName.value()));
        }
      }
    } else if (fetcherType == INHERIT_ID_FROM_PARENT
        && !parentComposingEntityType.equals(fieldComposingEntityType)) {
      throw util.errorAndThrow(
          """
          Directive @inheritFromParent on field '%s' in type '%s' specifies that the \
          field type must be a @composedType whose 'inEntity' argument matches the parent entity of the field. \
          Expected: '%s', Found '%s'
          """
              .formatted(
                  fieldName,
                  parentTypeName.value(),
                  parentComposingEntityType.map(GraphQLTypeName::value).orElse(null),
                  fieldComposingEntityType.map(GraphQLTypeName::value).orElse(null)));
    }
    @Nullable CodeBlock entityIdAccessCode =
        switch (fetcherType) {
          case ID_FETCHER ->
              canFanout
                  ? CodeBlock.of("$L.valueOpt().get()", entityIdFacetName)
                  : CodeBlock.of("_nonNil.value()");
          case INHERIT_ID_FROM_ARGS ->
              CodeBlock.of(
                  "new $T($L.getExecutionStepInfo().getArgument($S))",
                  schemaReaderUtil.entityIdClassName(schemaReaderUtil.typeClassName(fieldTypeName)),
                  Facets.EXECUTION_STRATEGY_PARAMS + "_new",
                  entityIdFieldName);
          case INHERIT_ID_FROM_PARENT -> CodeBlock.of(Facets.ENTITY_ID);
          default -> null;
        };
    ClassName depReqImmutType =
        ClassName.get(
            typeAggregatorClass.packageName(),
            typeAggregatorClass.simpleName() + REQUEST_SUFFIX + IMMUT_SUFFIX);
    ClassName depReqImmutPojoType =
        ClassName.get(
            depReqImmutType.packageName(),
            depReqImmutType.simpleName() + POJO.modelClassesSuffix());
    Map<String, Object> args =
        Map.ofEntries(
            entry("graphqlUtils", GraphQLUtils.class),
            entry("fieldName", fieldName),
            entry("fetcherFacetName", entityIdFacetName),
            entry(
                "entityIdAccessCode",
                entityIdAccessCode != null ? entityIdAccessCode : EMPTY_CODE_BLOCK),
            entry(
                "entityType",
                graphQlCodeGenUtil.toTypeNameForField(
                    getDeclaredActualFieldType(fieldSpec), fieldSpec)),
            entry("reqPojoType", depReqImmutPojoType),
            entry("facet_entityId", Facets.ENTITY_ID),
            entry("throwable", Throwable.class),
            entry(
                "forLoopStart",
                entityIdAccessCode != null
                    ? canFanout
                        ? CodeBlock.of(
"""
        $T<$T> _reqs = new $T<>();
        for (var _entityId : $L) {\
""",
                            List.class,
                            depReqImmutType.nestedClass("Builder"),
                            ArrayList.class,
                            entityIdAccessCode)
                        : CodeBlock.of("var _entityId = $L;", entityIdAccessCode)
                    : EMPTY_CODE_BLOCK),
            entry(
                "forLoopEnd",
                canFanout
                    ? CodeBlock.of(
                        """
                        _reqs.add(_req);
                        }
                """,
                        entityIdFacetName)
                    : CodeBlock.of("")),
            entry(
                "execute",
                canFanout
                    ? CodeBlock.of("return $T.executeFanoutWith(_reqs);", FanoutCommand.class)
                    : CodeBlock.of("return $T.executeWith(_req);", One2OneCommand.class)),
            entry(
                "skip",
                canFanout
                    ? CodeBlock.of("$T.skipFanout", FanoutCommand.class)
                    : CodeBlock.of("$T.skipExecution", One2OneCommand.class)),
            entry("skipNotQueried", CodeBlock.of("'$L' not queried", fieldName)));
    MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder(fieldName)
            .addAnnotation(
                AnnotationSpec.builder(Resolve.class)
                    .addMember(
                        "dep",
                        "$T.$L_n",
                        getFacetClassName(getAggregatorName(parentTypeName)),
                        fieldName)
                    .addMember(
                        "depInputs",
                        "{$T.graphql_executionContext_n, $T.graphql_executionStrategy_n, $T.graphql_executionStrategyParams_n, $T.$L_n}",
                        vajramReqClass,
                        vajramReqClass,
                        vajramReqClass,
                        vajramReqClass,
                        Facets.ENTITY_ID)
                    .build())
            .addModifiers(STATIC)
            .returns(
                ParameterizedTypeName.get(
                    ClassName.get(canFanout ? FanoutCommand.class : One2OneCommand.class),
                    depReqImmutType.nestedClass("Builder")))
            .addParameter(ExecutionContext.class, Facets.EXECUTION_CONTEXT)
            .addParameter(VajramExecutionStrategy.class, Facets.EXECUTION_STRATEGY)
            .addParameter(ExecutionStrategyParameters.class, Facets.EXECUTION_STRATEGY_PARAMS)
            .addNamedCode(
"""
    if (!$graphqlUtils:T.isFieldQueried($fieldName:S, graphql_executionStrategyParams)){
      return $skip:L($skipNotQueried:S);
    }
""",
                args)
            .addNamedCode(
                fetcherType == INHERIT_ID_FROM_PARENT || fetcherType == INHERIT_ID_FROM_ARGS
                    ?
"""
      $requestBuildingLogic:L
"""
                    :
"""
    return $fetcherFacetName:L.map(
          _failure -> $skip:L("'$fetcherFacetName:L' failed with error.", _failure.error()),
          () -> $skip:L("'$fetcherFacetName:L' returned null"),
          _nonNil -> {
            $requestBuildingLogic:L
          });
""",
                ImmutableMap.<String, Object>builder()
                    .put(
                        "requestBuildingLogic",
                        CodeBlock.builder()
                            .addNamed(
"""

            var graphql_executionStrategyParams_new = graphql_executionStrategy.newParametersForFieldExecution(
                    graphql_executionContext,
                    graphql_executionStrategyParams,
                    graphql_executionStrategyParams.getFields().getSubField($fieldName:S));
            $forLoopStart:L
            var _req = $reqPojoType:T._builder()
                .$facet_entityId:L(_entityId)
                .graphql_executionContext(graphql_executionContext)
                .graphql_executionStrategy(graphql_executionStrategy)
                .graphql_executionStrategyParams(graphql_executionStrategyParams_new);
            $forLoopEnd:L
            $execute:L
""",
                                args)
                            .build())
                    .putAll(args)
                    .build());

    if (!entityIdFacetName.isBlank()) {
      if (fetcherType == INHERIT_ID_FROM_PARENT) {
        fieldComposingEntityType.ifPresent(
            typeName ->
                methodBuilder.addParameter(
                    schemaReaderUtil.entityIdClassName(schemaReaderUtil.typeClassName(typeName)),
                    entityIdFacetName));
      } else if (fetcher instanceof VajramFetcher vajramFetcher) {
        methodBuilder.addParameter(
            ParameterizedTypeName.get(
                ClassName.get(Errable.class),
                getFetcherResponseType(vajramFetcher, List.of(fieldSpec))),
            entityIdFacetName);
      }
    }
    return methodBuilder.build();
  }
}
