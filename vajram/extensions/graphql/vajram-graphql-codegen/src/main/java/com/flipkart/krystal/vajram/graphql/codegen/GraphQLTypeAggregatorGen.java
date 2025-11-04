package com.flipkart.krystal.vajram.graphql.codegen;

import static com.flipkart.krystal.codegen.common.models.Constants.IMMUT_SUFFIX;
import static com.flipkart.krystal.model.PlainJavaObject.POJO;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.REQUEST_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants._INTERNAL_FACETS_CLASS;
import static com.flipkart.krystal.vajram.graphql.api.AbstractGraphQLEntity.DEFAULT_ENTITY_ID_FIELD;
import static com.flipkart.krystal.vajram.graphql.codegen.GraphQlFetcherType.TYPE_AGGREGATOR;
import static com.flipkart.krystal.vajram.graphql.codegen.SchemaReaderUtil.DATA_FETCHER;
import static com.flipkart.krystal.vajram.graphql.codegen.SchemaReaderUtil.GRAPHQL_AGGREGATOR;
import static com.flipkart.krystal.vajram.graphql.codegen.SchemaReaderUtil.REFERENCE_FETCHER;
import static com.flipkart.krystal.vajram.graphql.codegen.SchemaReaderUtil.getDirectiveArgumentString;
import static java.util.Map.entry;
import static javax.lang.model.element.Modifier.*;
import static javax.lang.model.element.Modifier.FINAL;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.model.IfAbsent.IfAbsentThen;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.*;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import com.flipkart.krystal.vajram.graphql.api.GraphQLUtils;
import com.flipkart.krystal.vajram.graphql.api.VajramExecutionStrategy;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlResponseJson;
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
public class GraphQLTypeAggregatorGen implements CodeGenerator {

  public static final String GRAPHQL_RESPONSE = "_GQlFields";
  private static final AnnotationSpec IF_ABSENT_FAIL =
      AnnotationSpec.builder(IfAbsent.class)
          .addMember("value", "$T.$L", IfAbsentThen.class, "FAIL")
          .build();

  private final CodeGenUtility util;
  private final SchemaReaderUtil schemaReaderUtil;
  private final GraphQlCodeGenUtil graphQlCodeGenUtil;

  public GraphQLTypeAggregatorGen(CodeGenUtility util) {
    this.util = util;
    this.graphQlCodeGenUtil = new GraphQlCodeGenUtil(util);
    this.schemaReaderUtil = graphQlCodeGenUtil.schemaReaderUtil();
  }

  public void generate() {
    Map<GraphQLTypeName, ObjectTypeDefinition> entityTypes = schemaReaderUtil.entityTypes();
    util.note("******** generating typeAggregators **********");
    entityTypes.forEach(
        (entityName, entityTypeDefinition) -> {
          ClassName className = getAggregatorName(entityName);
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
                              schemaReaderUtil.getPackageNameForType(entityName),
                              entityName.value())))
                  .addAnnotation(Vajram.class)
                  .addAnnotation(AnnotationSpec.builder(Slf4j.class).build())
                  .addTypes(createFacetDefinitions(entityName))
                  .addMethods(getInputResolvers(entityName, entityTypeDefinition))
                  .addMethod(outputLogic(entityName));
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

  private ClassName getAggregatorName(GraphQLTypeName typeName) {
    return ClassName.get(
        schemaReaderUtil.getPackageNameForType(typeName), typeName.value() + GRAPHQL_AGGREGATOR);
  }

  private List<TypeSpec> createFacetDefinitions(GraphQLTypeName entityName) {
    Builder inputs = TypeSpec.classBuilder("_Inputs").addModifiers(STATIC);
    inputs.addField(
        FieldSpec.builder(
                schemaReaderUtil.entityIdClassName(schemaReaderUtil.typeClassName(entityName)),
                "entityId")
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

    Builder internalFacets = TypeSpec.classBuilder(_INTERNAL_FACETS_CLASS).addModifiers(STATIC);

    for (Entry<Fetcher, List<GraphQlFieldSpec>> entry :
        schemaReaderUtil.entityTypeToFetcherToFields().get(entityName).entrySet()) {
      Fetcher fetcher = entry.getKey();
      List<GraphQlFieldSpec> fields = entry.getValue();
      internalFacets.addField(
          FieldSpec.builder(getFetcherResponseType(fetcher, fields), getFacetName(fetcher, fields))
              .addAnnotation(
                  AnnotationSpec.builder(Dependency.class)
                      .addMember("onVajram", "$T.class", fetcher.className())
                      .build())
              .build());
    }

    for (Entry<GraphQlFieldSpec, ClassName> fieldToTypeAggregator :
        schemaReaderUtil.entityTypeToFieldToTypeAggregator().get(entityName).entrySet()) {
      GraphQlFieldSpec fieldSpec = fieldToTypeAggregator.getKey();
      ClassName typeAggregatorClassName = fieldToTypeAggregator.getValue();

      AnnotationSpec.Builder depAnnotation =
          AnnotationSpec.builder(Dependency.class)
              .addMember("onVajram", "$T.class", typeAggregatorClassName);
      if (isGraphQlList(fieldSpec)) {
        depAnnotation.addMember("canFanout", "true");
      }
      internalFacets.addField(
          FieldSpec.builder(
                  graphQlCodeGenUtil.toTypeNameForField(
                      getDeclaredActualFieldType(fieldSpec), fieldSpec),
                  fieldSpec.fieldName())
              .addAnnotation(depAnnotation.build())
              .build());
    }

    return List.of(inputs.build(), internalFacets.build());
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
    if (currentType.isList()) {
      return true;
    }
    return false;
  }

  private static String getFacetName(Fetcher fetcher, List<GraphQlFieldSpec> fields) {
    return switch (fetcher.type()) {
      case MULTI_FIELD_DATA_FETCHER, ID_FETCHER -> fetcher.className().simpleName();
      default -> fields.get(0).fieldName();
    };
  }

  private TypeName getFetcherResponseType(Fetcher fetcher, List<GraphQlFieldSpec> fieldsDeRef) {
    ClassName fetcherClassName = fetcher.className();
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
              } else if (field.hasDirective(REFERENCE_FETCHER)) {
                dfToListOfFieldsDeRef
                    .computeIfAbsent(
                        schemaReaderUtil.getIdFetcherClassName(field), k -> new ArrayList<>())
                    .add(schemaReaderUtil.fieldSpecFromField(field, "", enclosingType));
              }
            });
    return dfToListOfFieldsDeRef;
  }

  private CodeBlock getFieldSetters(Fetcher fetcher, List<GraphQlFieldSpec> graphQlFieldSpecs) {
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

  private MethodSpec outputLogic(GraphQLTypeName entityName) {
    MethodSpec.Builder builder =
        MethodSpec.methodBuilder("output")
            .addAnnotation(Output.class)
            .addModifiers(STATIC)
            .returns(
                ClassName.get(
                    schemaReaderUtil.getPackageNameForType(entityName), entityName.value()));
    builder.addParameter(
        schemaReaderUtil.entityIdClassName(schemaReaderUtil.typeClassName(entityName)), "entityId");
    ClassName immutGQlRespJsonClassName =
        ClassName.get(
            schemaReaderUtil.getPackageNameForType(entityName),
            entityName.value()
                + "_"
                + IMMUT_SUFFIX
                + GraphQlResponseJson.INSTANCE.modelClassesSuffix());
    builder.addStatement(
        "$T.Builder entity = $T._builder().id(entityId)",
        immutGQlRespJsonClassName,
        immutGQlRespJsonClassName);
    schemaReaderUtil
        .entityTypeToFetcherToFields()
        .getOrDefault(entityName, Map.of())
        .forEach(
            (fetcher, fields) -> {
              if (fetcher.type().equals(GraphQlFetcherType.ID_FETCHER)) {
                // ID Fetchers are not needed in output logic
                return;
              }
              builder.addParameter(
                  ParameterizedTypeName.get(
                      ClassName.get(Errable.class), getFetcherResponseType(fetcher, fields)),
                  getFacetName(fetcher, fields));
              builder.addCode("$L", getFieldSetters(fetcher, fields));
            });
    schemaReaderUtil
        .entityTypeToFieldToTypeAggregator()
        .getOrDefault(entityName, Map.of())
        .forEach(
            (fieldSpec, aggregatorClassName) -> {
              Fetcher fetcher = new Fetcher(aggregatorClassName, TYPE_AGGREGATOR);
              boolean canFanout = isGraphQlList(fieldSpec);
              GraphQlTypeDecorator fieldType = getDeclaredActualFieldType(fieldSpec);
              builder.addParameter(
                  canFanout
                      ? ParameterizedTypeName.get(
                          ClassName.get(FanoutDepResponses.class),
                          getRequestClassName(aggregatorClassName),
                          graphQlCodeGenUtil.toTypeNameForField(fieldType, fieldSpec))
                      : ParameterizedTypeName.get(
                          ClassName.get(Errable.class),
                          graphQlCodeGenUtil.toTypeNameForField(fieldType, fieldSpec)),
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
            .graphql_executionStrategyParams(graphql_executionStrategyParams)
            ._build()""")
        .build();
  }

  private List<MethodSpec> getInputResolvers(
      GraphQLTypeName entityType, ObjectTypeDefinition entityTypeDefinition) {
    String entityIdentifierKey =
        getDirectiveArgumentString(entityTypeDefinition, "entity", "identifierKey")
            .orElse(DEFAULT_ENTITY_ID_FIELD);
    List<MethodSpec> methodSpecList = new ArrayList<>();

    schemaReaderUtil
        .entityTypeToFetcherToFields()
        .get(entityType)
        .forEach(
            (fetcher, fields) ->
                methodSpecList.add(
                    createFetcherInputResolver(fetcher, fields, entityType, entityIdentifierKey)));

    schemaReaderUtil
        .entityTypeToFieldToTypeAggregator()
        .get(entityType)
        .forEach(
            (field, vajramClass) ->
                methodSpecList.add(
                    createTypeAggregatorInputResolver(entityType, field, vajramClass)));
    return methodSpecList;
  }

  private MethodSpec createFetcherInputResolver(
      Fetcher fetcher,
      List<GraphQlFieldSpec> fields,
      GraphQLTypeName entityType,
      String entityIdField) {
    String vajramId = fetcher.className().simpleName();
    ClassName vajramReqClass = getRequestClassName(fetcher.className());

    ClassName entityClassName = schemaReaderUtil.typeClassName(entityType);
    ClassName entityIdClassName = schemaReaderUtil.entityIdClassName(entityClassName);
    String facetName = getFacetName(fetcher, fields);
    MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder(facetName)
            .addAnnotation(
                AnnotationSpec.builder(Resolve.class)
                    .addMember(
                        "dep",
                        "$T.$L_n",
                        getFacetClassName(getAggregatorName(entityType)),
                        facetName)
                    .addMember("depInputs", "$T.$L_n", vajramReqClass, entityIdField)
                    .build())
            .addModifiers(STATIC)
            .returns(
                ParameterizedTypeName.get(
                    ClassName.get(One2OneCommand.class), getRequestClassName(fetcher.className())))
            .addParameter(ExecutionContext.class, "graphql_executionContext")
            .addParameter(ExecutionStrategyParameters.class, "graphql_executionStrategyParams")
            .addParameter(entityIdClassName, "entityId")
            .addCode(
                """
            if ($T.isFieldQueriedInTheNestedType($L_FIELDS, $L)) {
              return $T.executeWith(
                    $T._builder()
                      .$L(entityId));
            } else {
              return $T.skipExecution($S);
            }
""",
                GraphQLUtils.class,
                vajramId,
                "graphql_executionStrategyParams",
                One2OneCommand.class,
                ClassName.get(
                    fetcher.className().packageName(),
                    fetcher.className().simpleName() + "_ReqImmutPojo"),
                entityIdField,
                One2OneCommand.class,
                vajramId);

    return methodBuilder.build();
  }

  private static ClassName getRequestClassName(ClassName vajramClass) {
    return ClassName.get(vajramClass.packageName(), vajramClass.simpleName() + "_Req");
  }

  private static ClassName getFacetClassName(ClassName aggregatorName) {
    return ClassName.get(aggregatorName.packageName(), aggregatorName.simpleName() + "_Fac");
  }

  private MethodSpec createTypeAggregatorInputResolver(
      GraphQLTypeName graphQLTypeName, GraphQlFieldSpec fieldSpec, ClassName vajramClass) {
    Fetcher fetcher =
        schemaReaderUtil.entityTypeToFieldToFetcher().get(graphQLTypeName).get(fieldSpec);
    if (fetcher == null) {
      throw util.errorAndThrow(
          "Could not find idFetcher for field "
              + fieldSpec
              + " in graphql Type: "
              + graphQLTypeName);
    }
    boolean canFanout = isGraphQlList(fieldSpec);
    TypeName fetcherResponseType = getFetcherResponseType(fetcher, List.of(fieldSpec));
    ClassName vajramReqClass = getRequestClassName(vajramClass);
    String fieldName = fieldSpec.fieldName();
    String fetcherFacetName = fetcher.className().simpleName();
    ClassName depReqImmutType =
        ClassName.get(
            vajramClass.packageName(), vajramClass.simpleName() + REQUEST_SUFFIX + IMMUT_SUFFIX);
    ClassName depReqImmutPojoType =
        ClassName.get(
            depReqImmutType.packageName(),
            depReqImmutType.simpleName() + POJO.modelClassesSuffix());
    MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder(fieldName)
            .addAnnotation(
                AnnotationSpec.builder(Resolve.class)
                    .addMember(
                        "dep",
                        "$T.$L_n",
                        getFacetClassName(getAggregatorName(graphQLTypeName)),
                        fieldName)
                    .addMember(
                        "depInputs",
                        "{$T.graphql_executionContext_n, $T.graphql_executionStrategy_n, $T.graphql_executionStrategyParams_n, $T.entityId_n}",
                        vajramReqClass,
                        vajramReqClass,
                        vajramReqClass,
                        vajramReqClass)
                    .build())
            .addModifiers(STATIC)
            .returns(
                ParameterizedTypeName.get(
                    ClassName.get(canFanout ? FanoutCommand.class : One2OneCommand.class),
                    depReqImmutType.nestedClass("Builder")))
            .addParameter(ExecutionContext.class, "graphql_executionContext")
            .addParameter(VajramExecutionStrategy.class, "graphql_executionStrategy")
            .addParameter(ExecutionStrategyParameters.class, "graphql_executionStrategyParams")
            .addParameter(
                ParameterizedTypeName.get(ClassName.get(Errable.class), fetcherResponseType),
                fetcherFacetName)
            .addNamedCode(
                """
    if ($graphqlUtils:T.isFieldQueriedInTheNestedType($fieldName:S, graphql_executionStrategyParams)
        && $fetcherFacetName:L.valueOpt().isPresent()) {
      try {
        $forLoopStart:L
        var _req = $reqPojoType:T._builder()
            .entityId(_entityId)
            .graphql_executionContext(graphql_executionContext)
            .graphql_executionStrategy(graphql_executionStrategy)
            .graphql_executionStrategyParams(
              graphql_executionStrategy.newParametersForFieldExecution(
                graphql_executionContext,
                graphql_executionStrategyParams,
                graphql_executionStrategyParams.getFields().getSubField($fieldName:S)));
        $forLoopEnd:L
        $execute:L
      } catch ($throwable:T e) {
        $skip:L
      }
    } else {
      $skip:L
    }
""",
                Map.ofEntries(
                    entry("graphqlUtils", GraphQLUtils.class),
                    entry("fieldName", fieldName),
                    entry("fetcherFacetName", fetcherFacetName),
                    entry(
                        "entityType",
                        graphQlCodeGenUtil.toTypeNameForField(
                            getDeclaredActualFieldType(fieldSpec), fieldSpec)),
                    entry("reqPojoType", depReqImmutPojoType),
                    entry("throwable", Throwable.class),
                    entry(
                        "forLoopStart",
                        canFanout
                            ? CodeBlock.of(
                                """
        $T<$T> _reqs = new $T<>();
        for (var _entityId : $L.valueOpt().get()) {
""",
                                List.class,
                                depReqImmutType.nestedClass("Builder"),
                                ArrayList.class,
                                fetcherFacetName)
                            : CodeBlock.of(
                                "var _entityId = $L.valueOpt().get();", fetcherFacetName)),
                    entry(
                        "forLoopEnd",
                        canFanout
                            ? CodeBlock.of(
                                """
        _reqs.add(_req);
        }
""", fetcherFacetName)
                            : CodeBlock.of("")),
                    entry(
                        "execute",
                        canFanout
                            ? CodeBlock.of(
                                "return $T.executeFanoutWith(_reqs);", FanoutCommand.class)
                            : CodeBlock.of("return $T.executeWith(_req);", One2OneCommand.class)),
                    entry(
                        "skip",
                        canFanout
                            ? CodeBlock.of(
                                "return $T.skipFanout($S);", FanoutCommand.class, fieldName)
                            : CodeBlock.of(
                                "return $T.skipExecution($S);", One2OneCommand.class, fieldName))));

    return methodBuilder.build();
  }
}
