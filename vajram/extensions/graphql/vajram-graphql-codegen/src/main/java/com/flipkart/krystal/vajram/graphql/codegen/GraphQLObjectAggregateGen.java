package com.flipkart.krystal.vajram.graphql.codegen;

import static com.flipkart.krystal.codegen.common.models.Constants.EMPTY_CODE_BLOCK;
import static com.flipkart.krystal.model.PlainJavaObject.POJO;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.IMMUT_REQUEST_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants._INTERNAL_FACETS_CLASS;
import static com.flipkart.krystal.vajram.graphql.codegen.CodeGenConstants.IF_ABSENT_FAIL;
import static com.flipkart.krystal.vajram.graphql.codegen.GraphQlFetcherType.INHERIT_ID_FROM_ARGS;
import static com.flipkart.krystal.vajram.graphql.codegen.GraphQlFetcherType.INHERIT_ID_FROM_PARENT;
import static com.flipkart.krystal.vajram.graphql.codegen.SchemaReaderUtil.getDirectiveArgumentString;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static java.util.Map.entry;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.Failure;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.FanoutCommand;
import com.flipkart.krystal.vajram.facets.One2OneCommand;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import com.flipkart.krystal.vajram.graphql.api.Constants.DirectiveArgs;
import com.flipkart.krystal.vajram.graphql.api.Constants.Directives;
import com.flipkart.krystal.vajram.graphql.api.Constants.Facets;
import com.flipkart.krystal.vajram.graphql.api.execution.GraphQLUtils;
import com.flipkart.krystal.vajram.graphql.api.execution.VajramExecutionStrategy;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlEntity;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlOperationEntity;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlValue;
import com.flipkart.krystal.vajram.graphql.api.traits.GraphQlOperationAggregate;
import com.google.common.collect.ImmutableMap;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategyParameters;
import graphql.execution.MergedField;
import graphql.language.FieldDefinition;
import graphql.language.InputValueDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.language.TypeDefinition;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
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

  public GraphQLObjectAggregateGen(CodeGenUtility util, File schemaFile) {
    this.util = util;
    this.graphQlCodeGenUtil = new GraphQlCodeGenUtil(schemaFile);
    this.schemaReaderUtil = graphQlCodeGenUtil.schemaReaderUtil();
  }

  public void generate() {
    Map<GraphQLTypeName, ObjectTypeDefinition> aggregatableTypes =
        schemaReaderUtil.aggregatableTypes();
    util.note(
        "Generating typeAggregators for aggregatable types : '%s'".formatted(aggregatableTypes));
    aggregatableTypes.forEach(
        (objectTypeName, typeDefinition) -> {
          try {
            ClassName aggregatorName = schemaReaderUtil.getAggregatorName(objectTypeName);
            TypeSpec.Builder typeAggregator =
                util.classBuilder(aggregatorName.simpleName(), "")
                    .addModifiers(PUBLIC)
                    .addModifiers(ABSTRACT)
                    .superclass(
                        ParameterizedTypeName.get(
                            ClassName.get(ComputeVajramDef.class),
                            asVajramReturnType(objectTypeName)))
                    .addAnnotation(Vajram.class)
                    .addTypes(createFacetDefinitions(typeDefinition))
                    .addMethods(getInputResolvers(objectTypeName, typeDefinition))
                    .addMethod(outputLogic(objectTypeName));
            if (schemaReaderUtil.isOperationType(objectTypeName)) {
              typeAggregator.addSuperinterface(
                  ParameterizedTypeName.get(
                      ClassName.get(GraphQlOperationAggregate.class),
                      asVajramReturnType(objectTypeName)));
            }
            schemaReaderUtil
                .typeToFetcherToFields()
                .get(objectTypeName)
                .forEach(
                    (fetcher, graphQlFieldSpecs) -> {
                      if (!(fetcher instanceof VajramFetcher vajramFetcher)) {
                        return;
                      }
                      String facetName = getFacetName(vajramFetcher, graphQlFieldSpecs);
                      typeAggregator.addField(
                          FieldSpec.builder(
                                  ParameterizedTypeName.get(Set.class, String.class),
                                  facetName + "_FIELDS",
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

            util.generateSourceFile(
                aggregatorName.canonicalName(),
                JavaFile.builder(aggregatorName.packageName(), typeAggregator.build())
                    .build()
                    .toString(),
                null);
            util.note("Successfully Create source file %s".formatted(aggregatorName));
          } catch (Throwable e) {
            util.error(
                "Error generating GraphQl Object Aggregator for object type '%s' due to exception '%s'"
                    .formatted(objectTypeName, getStackTraceAsString(e)));
          }
        });
  }

  private ClassName asVajramReturnType(GraphQLTypeName objectTypeName) {
    if (schemaReaderUtil.isOperationType(objectTypeName)) {
      return ClassName.get(GraphQlOperationEntity.class);
    }
    return ClassName.get(GraphQlEntity.class);
  }

  private List<TypeSpec> createFacetDefinitions(ObjectTypeDefinition typeDefinition) {
    GraphQLTypeName typeName = GraphQLTypeName.of(typeDefinition);

    TypeSpec.Builder inputs = TypeSpec.interfaceBuilder("_Inputs").addModifiers(STATIC);

    if (schemaReaderUtil.hasEntityId(typeDefinition)) {
      inputs.addMethod(
          MethodSpec.methodBuilder(Facets.ENTITY_ID)
              .returns(schemaReaderUtil.entityIdClassName(typeName))
              .addModifiers(ABSTRACT, PUBLIC)
              .addAnnotation(IF_ABSENT_FAIL)
              .build());
    }
    inputs.addMethod(
        MethodSpec.methodBuilder(Facets.EXECUTION_CONTEXT)
            .returns(ExecutionContext.class)
            .addModifiers(ABSTRACT, PUBLIC)
            .addAnnotation(IF_ABSENT_FAIL)
            .build());
    inputs.addMethod(
        MethodSpec.methodBuilder(Facets.EXECUTION_STRATEGY)
            .returns(ClassName.get(VajramExecutionStrategy.class))
            .addModifiers(ABSTRACT, PUBLIC)
            .addAnnotation(IF_ABSENT_FAIL)
            .build());
    inputs.addMethod(
        MethodSpec.methodBuilder(Facets.EXECUTION_STRATEGY_PARAMS)
            .returns(ExecutionStrategyParameters.class)
            .addModifiers(ABSTRACT, PUBLIC)
            .addAnnotation(IF_ABSENT_FAIL)
            .build());

    TypeSpec.Builder internalFacets =
        TypeSpec.interfaceBuilder(_INTERNAL_FACETS_CLASS).addModifiers(STATIC);

    Map<Fetcher, List<GraphQlFieldSpec>> fetcherToFields =
        schemaReaderUtil.typeToFetcherToFields().getOrDefault(typeName, Map.of());
    for (Entry<Fetcher, List<GraphQlFieldSpec>> entry : fetcherToFields.entrySet()) {
      if (entry.getKey() instanceof VajramFetcher fetcher) {
        List<GraphQlFieldSpec> fields = entry.getValue();
        boolean hasArgs =
            !fields.isEmpty()
                && !fields.get(0).fieldDefinition().getInputValueDefinitions().isEmpty();
        AnnotationSpec.Builder fetcherDepAnnotation =
            AnnotationSpec.builder(Dependency.class)
                .addMember("onVajram", "$T.class", fetcher.vajramClassName());
        if (hasArgs) {
          fetcherDepAnnotation.addMember("canFanout", "true");
        }
        internalFacets.addMethod(
            MethodSpec.methodBuilder(getFacetName(fetcher, fields))
                .returns(getFetcherResponseType(fetcher, fields))
                .addModifiers(ABSTRACT, PUBLIC)
                .addAnnotation(fetcherDepAnnotation.build())
                .build());
      }
    }

    for (Entry<GraphQlFieldSpec, ClassName> fieldToTypeAggregator :
        schemaReaderUtil
            .entityTypeToFieldToTypeAggregator()
            .getOrDefault(typeName, Map.of())
            .entrySet()) {
      GraphQlFieldSpec fieldSpec = fieldToTypeAggregator.getKey();
      ClassName typeAggregatorClassName = fieldToTypeAggregator.getValue();

      boolean typeAggHasArgs = !fieldSpec.fieldDefinition().getInputValueDefinitions().isEmpty();
      AnnotationSpec.Builder depAnnotation =
          AnnotationSpec.builder(Dependency.class)
              .addMember("onVajram", "$T.class", typeAggregatorClassName);
      if (isGraphQlList(fieldSpec) || typeAggHasArgs) {
        depAnnotation.addMember("canFanout", "true");
      }

      internalFacets.addMethod(
          MethodSpec.methodBuilder(fieldSpec.fieldName())
              .returns(asVajramReturnType(fieldSpec))
              .addModifiers(ABSTRACT, PUBLIC)
              .addAnnotation(depAnnotation.build())
              .build());
    }

    return List.of(inputs.build(), internalFacets.build());
  }

  private ClassName asVajramReturnType(GraphQlFieldSpec fieldSpec) {
    return ClassName.get(GraphQlEntity.class);
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
      case MULTI_FIELD_DATA_FETCHER, SINGLE_FIELD_DATA_FETCHER, ID_FETCHER ->
          fetcher.vajramClassName().simpleName();
      default -> fields.get(0).fieldName();
    };
  }

  private TypeName getFetcherResponseType(
      VajramFetcher fetcher, List<GraphQlFieldSpec> fieldsDeRef) {
    ClassName fetcherClassName = fetcher.vajramClassName();
    TypeName responseType;
    if (fetcher.type() != GraphQlFetcherType.MULTI_FIELD_DATA_FETCHER && !fieldsDeRef.isEmpty()) {
      GraphQlFieldSpec fieldSpec = fieldsDeRef.get(0);
      FieldDefinition fieldDefinition = fieldSpec.fieldDefinition();
      Optional<TypeDefinition> typeDefinition =
          schemaReaderUtil.typeDefinitionRegistry().getType(fieldDefinition.getType());
      if (typeDefinition.isPresent() && schemaReaderUtil.hasEntityId(typeDefinition.get())) {
        ClassName entityIdClassName =
            schemaReaderUtil.entityIdClassName(GraphQLTypeName.of(typeDefinition.get()));
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

  private MethodSpec outputLogic(GraphQLTypeName objectTypeName) {
    TypeDefinition objectTypeDefinition =
        schemaReaderUtil.typeDefinitionRegistry().getType(objectTypeName.value()).orElseThrow();
    boolean isEntity = objectTypeDefinition.hasDirective(Directives.ENTITY);
    boolean isOpType = schemaReaderUtil.isOperationType(objectTypeName);
    String entityIdFieldName = schemaReaderUtil.getEntityIdFieldName(objectTypeDefinition);
    ClassName returnType = asVajramReturnType(objectTypeName);

    MethodSpec.Builder builder =
        MethodSpec.methodBuilder("output")
            .addAnnotation(Output.class)
            .addModifiers(STATIC)
            .returns(returnType);

    if (isEntity) {
      builder.addParameter(schemaReaderUtil.entityIdClassName(objectTypeName), Facets.ENTITY_ID);
    }

    // Add non-ID data fetcher parameters
    schemaReaderUtil
        .typeToFetcherToFields()
        .getOrDefault(objectTypeName, Map.of())
        .forEach(
            (fetcher, fields) -> {
              if (!(fetcher instanceof VajramFetcher vajramFetcher)
                  || vajramFetcher.type().equals(GraphQlFetcherType.ID_FETCHER)) {
                return;
              }
              builder.addParameter(
                  ParameterizedTypeName.get(
                      ClassName.get(Errable.class), getFetcherResponseType(vajramFetcher, fields)),
                  getFacetName(vajramFetcher, fields));
            });

    // Collect arg-bearing list ID fetcher parameters (for slice sizes)
    Map<String, String> argListFieldToIdFetcherFacet = new LinkedHashMap<>();
    Map<String, TypeName> argListIdFetcherToResponseType = new LinkedHashMap<>();
    schemaReaderUtil
        .typeToFetcherToFields()
        .getOrDefault(objectTypeName, Map.of())
        .forEach(
            (fetcher, fields) -> {
              if (!(fetcher instanceof VajramFetcher vajramFetcher)
                  || !vajramFetcher.type().equals(GraphQlFetcherType.ID_FETCHER)) {
                return;
              }
              if (fields.isEmpty()) {
                return;
              }
              boolean hasArgs =
                  !fields.get(0).fieldDefinition().getInputValueDefinitions().isEmpty();
              boolean isList = isGraphQlList(fields.get(0));
              if (hasArgs && isList) {
                String facetName = getFacetName(vajramFetcher, fields);
                TypeName responseType = getFetcherResponseType(vajramFetcher, fields);
                builder.addParameter(
                    ParameterizedTypeName.get(
                        ClassName.get(FanoutDepResponses.class),
                        getRequestClassName(vajramFetcher.vajramClassName()),
                        responseType),
                    facetName);
                argListFieldToIdFetcherFacet.put(fields.get(0).fieldName(), facetName);
                argListIdFetcherToResponseType.put(facetName, responseType);
              }
            });

    // Add type aggregator parameters
    Map<GraphQlFieldSpec, ClassName> typeAggregators =
        schemaReaderUtil.entityTypeToFieldToTypeAggregator().getOrDefault(objectTypeName, Map.of());
    for (Entry<GraphQlFieldSpec, ClassName> entry : typeAggregators.entrySet()) {
      GraphQlFieldSpec key = entry.getKey();
      ClassName value = entry.getValue();
      boolean hasArgs = !key.fieldDefinition().getInputValueDefinitions().isEmpty();
      if (hasArgs) {
        builder.addParameter(
            ParameterizedTypeName.get(
                ClassName.get(FanoutDepResponses.class),
                getRequestClassName(value),
                asVajramReturnType(key)),
            key.fieldName());
      } else {
        builder.addParameter(
            isGraphQlList(key)
                ? ParameterizedTypeName.get(
                    ClassName.get(FanoutDepResponses.class),
                    getRequestClassName(value),
                    asVajramReturnType(key))
                : ParameterizedTypeName.get(ClassName.get(Errable.class), asVajramReturnType(key)),
            key.fieldName());
      }
    }

    builder.addParameter(ExecutionContext.class, "graphql_executionContext");
    builder.addParameter(VajramExecutionStrategy.class, "graphql_executionStrategy");
    builder.addParameter(ExecutionStrategyParameters.class, "graphql_executionStrategyParams");

    // Initialize builder (GraphQlOperationObjectMapBuilder for op types, GraphQlObjectMapBuilder
    // otherwise)
    ClassName builderType =
        isOpType
            ? ClassName.get(GraphQlOperationEntity.class)
                .nestedClass("GraphQlOperationObjectMapBuilder")
            : ClassName.get(GraphQlEntity.class).nestedClass("GraphQlObjectMapBuilder");
    builder.addNamedCode(
        """
        $builderType:T _builder = new $builderType:T();
        """,
        Map.of("builderType", builderType));

    // Build fieldName → aliases map from execution context
    builder.addNamedCode(
        """
        $map:T<$string:T, $list:T<$string:T>> _fieldNameToAliases = $graphQLUtils:T.computeFieldNameToAliases(graphql_executionStrategyParams);
        """,
        Map.ofEntries(
            entry("map", Map.class),
            entry("string", String.class),
            entry("list", List.class),
            entry("graphQLUtils", GraphQLUtils.class)));

    // __typename: same value for all aliases
    builder.addNamedCode(
        """
        if (_fieldNameToAliases.containsKey("__typename")) {
          for ($string:T _alias : _fieldNameToAliases.get("__typename")) {
            _builder.addField(_alias, new $scalarValue:T($errable:T.withValue($typeName:S), true));
          }
        }
        """,
        Map.ofEntries(
            entry("string", String.class),
            entry("scalarValue", GraphQlValue.ScalarValue.class),
            entry("errable", Errable.class),
            entry("typeName", objectTypeName.value())));

    // Entity ID field: same value for all aliases
    if (isEntity) {
      builder.addNamedCode(
          """
          if (_fieldNameToAliases.containsKey($entityIdField:S)) {
            for ($string:T _alias : _fieldNameToAliases.get($entityIdField:S)) {
              _builder.addField(_alias, new $scalarValue:T($errable:T.withValue($entityId:L), true));
            }
          }
          """,
          Map.ofEntries(
              entry("string", String.class),
              entry("entityIdField", entityIdFieldName),
              entry("scalarValue", GraphQlValue.ScalarValue.class),
              entry("errable", Errable.class),
              entry("entityId", Facets.ENTITY_ID)));
    }

    // Data fetcher fields: arg-less, same value for all aliases that map to each fieldname
    schemaReaderUtil
        .typeToFetcherToFields()
        .getOrDefault(objectTypeName, Map.of())
        .forEach(
            (fetcher, fields) -> {
              if (!(fetcher instanceof VajramFetcher vajramFetcher)
                  || vajramFetcher.type().equals(GraphQlFetcherType.ID_FETCHER)) {
                return;
              }
              String facetName = getFacetName(vajramFetcher, fields);
              boolean isMultiField =
                  vajramFetcher.type() == GraphQlFetcherType.MULTI_FIELD_DATA_FETCHER;
              for (GraphQlFieldSpec fieldSpec : fields) {
                String fieldName = fieldSpec.fieldName();
                boolean isListField = isGraphQlList(fieldSpec);
                if (isListField) {
                  if (isMultiField) {
                    builder.addNamedCode(
                        """
                        if (_fieldNameToAliases.containsKey($fieldName:S)) {
                          $errable:T<$list:T<$singleValue:T>> _$fieldName:L_items =
                              $facetName:L.mapToValue($failure:T::cast, $errable:T::nil, _v -> {
                                $list:T<$singleValue:T> _items = new $arrayList:T<>();
                                for (Object _s : _v.$fieldName:L()) {
                                  _items.add(new $scalarValue:T($errable:T.withValue(_s), true));
                                }
                                return $errable:T.withValue(_items);
                              });
                          for ($string:T _alias : _fieldNameToAliases.get($fieldName:S)) {
                            _builder.addField(_alias, new $listValue:T(_$fieldName:L_items, true));
                          }
                        }
                        """,
                        Map.ofEntries(
                            entry("fieldName", fieldName),
                            entry("errable", Errable.class),
                            entry("list", List.class),
                            entry("singleValue", GraphQlValue.SingleValue.class),
                            entry("arrayList", ArrayList.class),
                            entry("scalarValue", GraphQlValue.ScalarValue.class),
                            entry("listValue", GraphQlValue.ListValue.class),
                            entry("facetName", facetName),
                            entry("failure", Failure.class),
                            entry("string", String.class)));
                  } else {
                    builder.addNamedCode(
                        """
                        if (_fieldNameToAliases.containsKey($fieldName:S)) {
                          $errable:T<$list:T<$singleValue:T>> _$fieldName:L_items =
                              $facetName:L.mapToValue($failure:T::cast, $errable:T::nil, _list -> {
                                $list:T<$singleValue:T> _items = new $arrayList:T<>();
                                for (Object _s : _list) {
                                  _items.add(new $scalarValue:T($errable:T.withValue(_s), true));
                                }
                                return $errable:T.withValue(_items);
                              });
                          for ($string:T _alias : _fieldNameToAliases.get($fieldName:S)) {
                            _builder.addField(_alias, new $listValue:T(_$fieldName:L_items, true));
                          }
                        }
                        """,
                        Map.ofEntries(
                            entry("fieldName", fieldName),
                            entry("errable", Errable.class),
                            entry("list", List.class),
                            entry("singleValue", GraphQlValue.SingleValue.class),
                            entry("arrayList", ArrayList.class),
                            entry("scalarValue", GraphQlValue.ScalarValue.class),
                            entry("listValue", GraphQlValue.ListValue.class),
                            entry("facetName", facetName),
                            entry("failure", Failure.class),
                            entry("string", String.class)));
                  }
                } else {
                  if (isMultiField) {
                    builder.addNamedCode(
                        """
                        if (_fieldNameToAliases.containsKey($fieldName:S)) {
                          for ($string:T _alias : _fieldNameToAliases.get($fieldName:S)) {
                            _builder.addField(_alias, new $scalarValue:T($facetName:L.mapToValue($failure:T::cast, $errable:T::nil, _v -> $errable:T.withValue(_v.$fieldName:L())), true));
                          }
                        }
                        """,
                        Map.ofEntries(
                            entry("fieldName", fieldName),
                            entry("scalarValue", GraphQlValue.ScalarValue.class),
                            entry("facetName", facetName),
                            entry("failure", Failure.class),
                            entry("errable", Errable.class),
                            entry("string", String.class)));
                  } else {
                    builder.addNamedCode(
                        """
                        if (_fieldNameToAliases.containsKey($fieldName:S)) {
                          for ($string:T _alias : _fieldNameToAliases.get($fieldName:S)) {
                            _builder.addField(_alias, new $scalarValue:T($facetName:L, true));
                          }
                        }
                        """,
                        Map.ofEntries(
                            entry("fieldName", fieldName),
                            entry("scalarValue", GraphQlValue.ScalarValue.class),
                            entry("facetName", facetName),
                            entry("string", String.class)));
                  }
                }
              }
            });

    // Type aggregator fields
    typeAggregators.forEach(
        (fieldSpec, aggregatorClassName) -> {
          boolean hasArgs = !fieldSpec.fieldDefinition().getInputValueDefinitions().isEmpty();
          String fieldName = fieldSpec.fieldName();
          if (hasArgs) {
            if (isGraphQlList(fieldSpec)) {
              // Arg-bearing list: match each alias positionally to its response slice,
              // using the corresponding ID fetcher to determine the slice size per alias.
              String idFetcherFacet = argListFieldToIdFetcherFacet.get(fieldName);
              TypeName idListType = argListIdFetcherToResponseType.get(idFetcherFacet);
              builder.addNamedCode(
                  """
                  if (_fieldNameToAliases.containsKey($fieldName:S)) {
                    $list:T<$string:T> _$fieldName:L_aliases = _fieldNameToAliases.get($fieldName:S);
                    int _$fieldName:L_offset = 0;
                    for (int _i = 0; _i < _$fieldName:L_aliases.size(); _i++) {
                      $string:T _alias = _$fieldName:L_aliases.get(_i);
                      $errable:T<$idListType:T> _idsErrable = $idFetcherFacet:L.requestResponsePairs().get(_i).response();
                      if (_idsErrable.valueOpt().isPresent()) {
                        int _count = _idsErrable.valueOpt().get().size();
                        $list:T<$singleValue:T> _items = new $arrayList:T<>();
                        for (int _j = _$fieldName:L_offset; _j < _$fieldName:L_offset + _count; _j++) {
                          $fieldName:L.requestResponsePairs().get(_j).response().handle(
                              _f -> _items.add(new $objectValue:T(_f.cast(), true)),
                              _v -> _items.add(new $objectValue:T($errable:T.withValue(_v), true)));
                        }
                        _builder.addField(_alias, new $listValue:T($errable:T.withValue(_items), true));
                        _$fieldName:L_offset += _count;
                      }
                    }
                  }
                  """,
                  Map.ofEntries(
                      entry("fieldName", fieldName),
                      entry("string", String.class),
                      entry("list", List.class),
                      entry("errable", Errable.class),
                      entry("idListType", idListType),
                      entry("idFetcherFacet", idFetcherFacet),
                      entry("singleValue", GraphQlValue.SingleValue.class),
                      entry("arrayList", ArrayList.class),
                      entry("objectValue", GraphQlValue.ObjectValue.class),
                      entry("listValue", GraphQlValue.ListValue.class)));
            } else {
              // Arg-bearing non-list: match each alias positionally to its response
              builder.addNamedCode(
                  """
                  if (_fieldNameToAliases.containsKey($fieldName:S)) {
                    $list:T<$string:T> _$fieldName:L_aliases = _fieldNameToAliases.get($fieldName:S);
                    for (int _i = 0; _i < _$fieldName:L_aliases.size() && _i < $fieldName:L.requestResponsePairs().size(); _i++) {
                      $string:T _alias = _$fieldName:L_aliases.get(_i);
                      $fieldName:L.requestResponsePairs().get(_i).response().handle(
                          _failure -> _builder.addField(_alias, new $objectValue:T(_failure.cast(), true)),
                          _val -> _builder.addField(_alias, new $objectValue:T($errable:T.withValue(_val), true)));
                    }
                  }
                  """,
                  Map.ofEntries(
                      entry("fieldName", fieldName),
                      entry("string", String.class),
                      entry("list", List.class),
                      entry("errable", Errable.class),
                      entry("objectValue", GraphQlValue.ObjectValue.class)));
            }
          } else if (isGraphQlList(fieldSpec)) {
            // Arg-less list: build items once, assign same value to all aliases
            builder.addNamedCode(
                """
                if (_fieldNameToAliases.containsKey($fieldName:S)) {
                  $list:T<$singleValue:T> _$fieldName:L_items = new $arrayList:T<>();
                  for ($errable:T<$gqlObjectMap:T> _e : $fieldName:L.responses()) {
                    _$fieldName:L_items.add(new $objectValue:T(_e, true));
                  }
                  for ($string:T _alias : _fieldNameToAliases.get($fieldName:S)) {
                    _builder.addField(_alias, new $listValue:T($errable:T.withValue(_$fieldName:L_items), true));
                  }
                }
                """,
                Map.ofEntries(
                    entry("fieldName", fieldName),
                    entry("string", String.class),
                    entry("list", List.class),
                    entry("singleValue", GraphQlValue.SingleValue.class),
                    entry("arrayList", ArrayList.class),
                    entry("errable", Errable.class),
                    entry("gqlObjectMap", GraphQlEntity.class),
                    entry("objectValue", GraphQlValue.ObjectValue.class),
                    entry("listValue", GraphQlValue.ListValue.class)));
          } else {
            // Arg-less non-list: same value for all aliases
            builder.addNamedCode(
                """
                if (_fieldNameToAliases.containsKey($fieldName:S)) {
                  for ($string:T _alias : _fieldNameToAliases.get($fieldName:S)) {
                    _builder.addField(_alias, new $objectValue:T($fieldName:L, true));
                  }
                }
                """,
                Map.ofEntries(
                    entry("fieldName", fieldName),
                    entry("string", String.class),
                    entry("objectValue", GraphQlValue.ObjectValue.class)));
          }
        });

    builder.addStatement("return _builder.build()");

    return builder.build();
  }

  private List<MethodSpec> getInputResolvers(
      GraphQLTypeName entityType, ObjectTypeDefinition typeDefinition) {
    List<MethodSpec> methodSpecList = new ArrayList<>();

    schemaReaderUtil
        .typeToFetcherToFields()
        .getOrDefault(entityType, Map.of())
        .forEach(
            (fetcher, fields) -> {
              if (fetcher instanceof VajramFetcher vajramFetcher) {
                methodSpecList.add(
                    createFetcherInputResolver(vajramFetcher, fields, entityType, typeDefinition));
              }
            });

    schemaReaderUtil
        .entityTypeToFieldToTypeAggregator()
        .getOrDefault(entityType, Map.of())
        .forEach(
            (field, typeAggregatorClass) -> {
              @Nullable Fetcher fetcher =
                  schemaReaderUtil
                      .entityTypeToFieldToFetcher()
                      .getOrDefault(entityType, Map.of())
                      .get(field);
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

    ClassName vajramReqClass = getRequestClassName(fetcher.vajramClassName());
    boolean isParentOpType = schemaReaderUtil.operationTypes().containsKey(parentTypeName);
    boolean parentTypeHasEntityId = !isParentOpType;

    String facetName = getFacetName(fetcher, fields);
    boolean hasArgs =
        !fields.isEmpty() && !fields.get(0).fieldDefinition().getInputValueDefinitions().isEmpty();
    String fieldName = !fields.isEmpty() ? fields.get(0).fieldName() : facetName;

    if (hasArgs && fetcher.type() != GraphQlFetcherType.MULTI_FIELD_DATA_FETCHER) {
      // Arg-bearing fields use alias-based fanout: one request per alias
      List<CodeBlock> depInputSetterCode = new ArrayList<>();
      if (parentTypeHasEntityId) {
        depInputSetterCode.add(
            CodeBlock.of(
                ".$L($L)", schemaReaderUtil.getEntityIdFieldName(parentTypeDef), Facets.ENTITY_ID));
      }
      for (InputValueDefinition inputValueDefinition :
          fields.get(0).fieldDefinition().getInputValueDefinitions()) {
        String argName = inputValueDefinition.getName();
        depInputSetterCode.add(
            CodeBlock.of(
                ".$L($L.getExecutionStepInfo().getArgument($S))",
                argName,
                Facets.EXECUTION_STRATEGY_PARAMS + "_new",
                argName));
      }
      ClassName reqPojoClass =
          ClassName.get(
              fetcher.vajramClassName().packageName(),
              fetcher.vajramClassName().simpleName()
                  + IMMUT_REQUEST_SUFFIX
                  + POJO.modelClassesSuffix());
      ClassName reqImmutClass =
          ClassName.get(
              fetcher.vajramClassName().packageName(),
              fetcher.vajramClassName().simpleName() + IMMUT_REQUEST_SUFFIX);
      MethodSpec.Builder fanoutBuilder =
          MethodSpec.methodBuilder(facetName)
              .addAnnotation(
                  AnnotationSpec.builder(Resolve.class)
                      .addMember(
                          "dep",
                          "$T.$L_n",
                          getFacetClassName(schemaReaderUtil.getAggregatorName(parentTypeName)),
                          facetName)
                      .build())
              .addModifiers(STATIC)
              .returns(
                  ParameterizedTypeName.get(
                      ClassName.get(FanoutCommand.class), reqImmutClass.nestedClass("Builder")))
              .addParameter(ExecutionContext.class, Facets.EXECUTION_CONTEXT)
              .addParameter(VajramExecutionStrategy.class, Facets.EXECUTION_STRATEGY)
              .addParameter(ExecutionStrategyParameters.class, Facets.EXECUTION_STRATEGY_PARAMS);
      if (parentTypeHasEntityId) {
        fanoutBuilder.addParameter(
            schemaReaderUtil.entityIdClassName(parentTypeName), Facets.ENTITY_ID);
      }
      fanoutBuilder.addNamedCode(
          """
          $list:T<$reqBuilder:T> _reqs = new $arrayList:T<>();
          for ($mapEntry:T<$string:T, $mergedField:T> _aliasEntry :
              $executionStrategyParams:L.getFields().getSubFields().entrySet()) {
            if (!$fieldName:S.equals(_aliasEntry.getValue().getSingleField().getName())) {
              continue;
            }
            var graphql_executionStrategyParams_new =
                graphql_executionStrategy.newParametersForFieldExecution(
                    graphql_executionContext, graphql_executionStrategyParams,
                    _aliasEntry.getValue());
            _reqs.add($reqPojoClass:T._builder()
                $depInputSetterCode:L);
          }
          if (_reqs.isEmpty()) {
            return $fanoutCommand:T.skipFanout($facetName:S);
          }
          return $fanoutCommand:T.executeFanoutWith(_reqs);
          """,
          Map.ofEntries(
              entry("list", List.class),
              entry("reqBuilder", reqImmutClass.nestedClass("Builder")),
              entry("arrayList", ArrayList.class),
              entry("mapEntry", Map.Entry.class),
              entry("string", String.class),
              entry("mergedField", MergedField.class),
              entry("executionStrategyParams", Facets.EXECUTION_STRATEGY_PARAMS),
              entry("fieldName", fieldName),
              entry("reqPojoClass", reqPojoClass),
              entry(
                  "depInputSetterCode",
                  depInputSetterCode.stream().collect(CodeBlock.joining("\n"))),
              entry("fanoutCommand", FanoutCommand.class),
              entry("facetName", facetName)));
      return fanoutBuilder.build();
    }

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

    if (fetcher.type() != GraphQlFetcherType.MULTI_FIELD_DATA_FETCHER && !fields.isEmpty()) {
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
                        getFacetClassName(schemaReaderUtil.getAggregatorName(parentTypeName)),
                        facetName)
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
                facetName,
                Facets.EXECUTION_STRATEGY_PARAMS,
                // Arg-bearing single-field fetchers are handled via the alias-fanout path above,
                // so no fields reaching here have args — newParams would be a dead variable.
                EMPTY_CODE_BLOCK,
                One2OneCommand.class,
                ClassName.get(
                    fetcher.vajramClassName().packageName(),
                    fetcher.vajramClassName().simpleName()
                        + IMMUT_REQUEST_SUFFIX
                        + POJO.modelClassesSuffix()),
                depInputSetterCode.stream().collect(CodeBlock.joining("\n")),
                One2OneCommand.class,
                facetName);
    if (parentTypeHasEntityId) {
      methodBuilder.addParameter(
          schemaReaderUtil.entityIdClassName(parentTypeName), Facets.ENTITY_ID);
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
      if (fetcherType == INHERIT_ID_FROM_ARGS) {
        // This means the user has declared that the arguments must contain exactly one ID which is
        // same as the id of the entity type of the field
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
      } else if (fetcherType == INHERIT_ID_FROM_PARENT) {
        throw util.errorAndThrow(
            "Entity field '%s' cannot inherit id from parent when parent '%s' is an operation type"
                .formatted(fieldName, parentTypeName.value()));
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
    boolean hasArgs = !fieldSpec.fieldDefinition().getInputValueDefinitions().isEmpty();
    if (hasArgs && fetcherType == GraphQlFetcherType.ID_FETCHER) {
      return createAliasBasedTypeAggregatorInputResolver(
          fetcher,
          parentTypeName,
          fieldSpec,
          typeAggregatorClass,
          entityIdFacetName,
          canFanout,
          fieldName);
    }
    if (hasArgs && fetcherType == INHERIT_ID_FROM_ARGS) {
      return createInheritIdFromArgsAliasResolver(
          parentTypeName,
          typeAggregatorClass,
          fieldName,
          schemaReaderUtil.entityIdClassName(fieldTypeName),
          entityIdFieldName);
    }

    @Nullable CodeBlock entityIdAccessCode =
        switch (fetcherType) {
          case ID_FETCHER ->
              canFanout
                  ? CodeBlock.of("$L.valueOpt().get()", entityIdFacetName)
                  : CodeBlock.of("_nonNil");
          case INHERIT_ID_FROM_ARGS ->
              CodeBlock.of(
                  "new $T($L.getExecutionStepInfo().getArgument($S))",
                  schemaReaderUtil.entityIdClassName(fieldTypeName),
                  Facets.EXECUTION_STRATEGY_PARAMS + "_new",
                  entityIdFieldName);
          case INHERIT_ID_FROM_PARENT -> CodeBlock.of(Facets.ENTITY_ID);
          default -> null;
        };
    ClassName depReqImmutType =
        ClassName.get(
            typeAggregatorClass.packageName(),
            typeAggregatorClass.simpleName() + IMMUT_REQUEST_SUFFIX);
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
                        getFacetClassName(schemaReaderUtil.getAggregatorName(parentTypeName)),
                        fieldName)
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
    return $fetcherFacetName:L.mapToValue(
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
                    schemaReaderUtil.entityIdClassName(typeName), entityIdFacetName));
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

  /**
   * Generates the type aggregator resolver for {@code @inferIdFromArgs} fields with arguments,
   * using alias-based fanout: one type-aggregator request per alias, with the entity ID extracted
   * from each alias's own execution args.
   */
  private MethodSpec createInheritIdFromArgsAliasResolver(
      GraphQLTypeName parentTypeName,
      ClassName typeAggregatorClass,
      String fieldName,
      ClassName entityIdClassName,
      String entityIdFieldName) {

    ClassName depReqImmutType =
        ClassName.get(
            typeAggregatorClass.packageName(),
            typeAggregatorClass.simpleName() + IMMUT_REQUEST_SUFFIX);
    ClassName depReqImmutPojoType =
        ClassName.get(
            depReqImmutType.packageName(),
            depReqImmutType.simpleName() + POJO.modelClassesSuffix());

    return MethodSpec.methodBuilder(fieldName)
        .addAnnotation(
            AnnotationSpec.builder(Resolve.class)
                .addMember(
                    "dep",
                    "$T.$L_n",
                    getFacetClassName(schemaReaderUtil.getAggregatorName(parentTypeName)),
                    fieldName)
                .build())
        .addModifiers(STATIC)
        .returns(
            ParameterizedTypeName.get(
                ClassName.get(FanoutCommand.class), depReqImmutType.nestedClass("Builder")))
        .addParameter(ExecutionContext.class, Facets.EXECUTION_CONTEXT)
        .addParameter(VajramExecutionStrategy.class, Facets.EXECUTION_STRATEGY)
        .addParameter(ExecutionStrategyParameters.class, Facets.EXECUTION_STRATEGY_PARAMS)
        .addNamedCode(
            """
            if (!$graphqlUtils:T.isFieldQueried($fieldName:S, $executionStrategyParams:L)) {
              return $fanoutCommand:T.skipFanout($fieldNotQueried:S);
            }
            $list:T<$mapEntry:T<$string:T, $mergedField:T>> _aliases =
                $executionStrategyParams:L.getFields().getSubFields().entrySet().stream()
                    .filter(e -> $fieldName:S.equals(e.getValue().getSingleField().getName()))
                    .toList();
            $list:T<$depReqBuilder:T> _reqs = new $arrayList:T<>();
            for ($mapEntry:T<$string:T, $mergedField:T> _aliasEntry : _aliases) {
              var graphql_executionStrategyParams_new =
                  graphql_executionStrategy.newParametersForFieldExecution(
                      graphql_executionContext, graphql_executionStrategyParams,
                      _aliasEntry.getValue());
              var _entityId = new $entityIdClass:T(
                  graphql_executionStrategyParams_new.getExecutionStepInfo().getArgument(
                      $entityIdField:S));
              _reqs.add($depReqPojoType:T._builder()
                  .$entityIdFacet:L(_entityId)
                  .graphql_executionContext(graphql_executionContext)
                  .graphql_executionStrategy(graphql_executionStrategy)
                  .graphql_executionStrategyParams(graphql_executionStrategyParams_new));
            }
            if (_reqs.isEmpty()) {
              return $fanoutCommand:T.skipFanout($fieldNoIds:S);
            }
            return $fanoutCommand:T.executeFanoutWith(_reqs);
            """,
            Map.ofEntries(
                entry("graphqlUtils", GraphQLUtils.class),
                entry("fieldName", fieldName),
                entry("fieldNotQueried", "'" + fieldName + "' not queried"),
                entry("fieldNoIds", "'" + fieldName + "' no IDs"),
                entry("fanoutCommand", FanoutCommand.class),
                entry("executionStrategyParams", Facets.EXECUTION_STRATEGY_PARAMS),
                entry("list", List.class),
                entry("mapEntry", Map.Entry.class),
                entry("string", String.class),
                entry("mergedField", MergedField.class),
                entry("depReqBuilder", depReqImmutType.nestedClass("Builder")),
                entry("arrayList", ArrayList.class),
                entry("depReqPojoType", depReqImmutPojoType),
                entry("entityIdClass", entityIdClassName),
                entry("entityIdField", entityIdFieldName),
                entry("entityIdFacet", Facets.ENTITY_ID)))
        .build();
  }

  /**
   * Generates the type aggregator resolver method for fields with arguments, using alias-based
   * fanout: one ID-fetcher request per alias, then one type-aggregator request per alias result.
   */
  private MethodSpec createAliasBasedTypeAggregatorInputResolver(
      Fetcher fetcher,
      GraphQLTypeName parentTypeName,
      GraphQlFieldSpec fieldSpec,
      ClassName typeAggregatorClass,
      String entityIdFacetName,
      boolean isList,
      String fieldName) {

    ClassName depReqImmutType =
        ClassName.get(
            typeAggregatorClass.packageName(),
            typeAggregatorClass.simpleName() + IMMUT_REQUEST_SUFFIX);
    ClassName depReqImmutPojoType =
        ClassName.get(
            depReqImmutType.packageName(),
            depReqImmutType.simpleName() + POJO.modelClassesSuffix());

    TypeName fetcherResponseType =
        fetcher instanceof VajramFetcher vajramFetcher
            ? getFetcherResponseType(vajramFetcher, List.of(fieldSpec))
            : TypeName.OBJECT;
    ClassName fetcherReqClass =
        fetcher instanceof VajramFetcher vajramFetcher
            ? getRequestClassName(vajramFetcher.vajramClassName())
            : ClassName.get(Object.class);

    MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder(fieldName)
            .addAnnotation(
                AnnotationSpec.builder(Resolve.class)
                    .addMember(
                        "dep",
                        "$T.$L_n",
                        getFacetClassName(schemaReaderUtil.getAggregatorName(parentTypeName)),
                        fieldName)
                    .build())
            .addModifiers(STATIC)
            .returns(
                ParameterizedTypeName.get(
                    ClassName.get(FanoutCommand.class), depReqImmutType.nestedClass("Builder")))
            .addParameter(ExecutionContext.class, Facets.EXECUTION_CONTEXT)
            .addParameter(VajramExecutionStrategy.class, Facets.EXECUTION_STRATEGY)
            .addParameter(ExecutionStrategyParameters.class, Facets.EXECUTION_STRATEGY_PARAMS);

    if (!entityIdFacetName.isBlank()) {
      methodBuilder.addParameter(
          ParameterizedTypeName.get(
              ClassName.get(FanoutDepResponses.class), fetcherReqClass, fetcherResponseType),
          entityIdFacetName);
    }

    methodBuilder.addNamedCode(
        """
        if (!$graphqlUtils:T.isFieldQueried($fieldName:S, $executionStrategyParams:L)) {
          return $fanoutCommand:T.skipFanout($fieldNotQueried:S);
        }
        $list:T<$mapEntry:T<$string:T, $mergedField:T>> _aliases =
            $executionStrategyParams:L.getFields().getSubFields().entrySet().stream()
                .filter(e -> $fieldName:S.equals(e.getValue().getSingleField().getName()))
                .toList();
        $list:T<$depReqBuilder:T> _reqs = new $arrayList:T<>();
        """,
        Map.ofEntries(
            entry("graphqlUtils", GraphQLUtils.class),
            entry("fieldName", fieldName),
            entry("fieldNotQueried", "'" + fieldName + "' not queried"),
            entry("fanoutCommand", FanoutCommand.class),
            entry("executionStrategyParams", Facets.EXECUTION_STRATEGY_PARAMS),
            entry("list", List.class),
            entry("mapEntry", Map.Entry.class),
            entry("string", String.class),
            entry("mergedField", MergedField.class),
            entry("depReqBuilder", depReqImmutType.nestedClass("Builder")),
            entry("arrayList", ArrayList.class)));

    if (isList) {
      // List entity: for each alias, iterate all IDs returned by the ID fetcher
      methodBuilder.addNamedCode(
          """
          for (int _i = 0; _i < _aliases.size(); _i++) {
            $mapEntry:T<$string:T, $mergedField:T> _aliasEntry = _aliases.get(_i);
            $errable:T<$fetcherResponse:T> _idsErrable = $entityIdFacet:L.requestResponsePairs().get(_i).response();
            if (_idsErrable.valueOpt().isEmpty()) {
              continue;
            }
            var graphql_executionStrategyParams_new =
                graphql_executionStrategy.newParametersForFieldExecution(
                    graphql_executionContext, graphql_executionStrategyParams,
                    _aliasEntry.getValue());
            for (var _entityId : _idsErrable.valueOpt().get()) {
              _reqs.add($depReqPojoType:T._builder()
                  .$entityIdFacetName:L(_entityId)
                  .graphql_executionContext(graphql_executionContext)
                  .graphql_executionStrategy(graphql_executionStrategy)
                  .graphql_executionStrategyParams(graphql_executionStrategyParams_new));
            }
          }
          """,
          Map.ofEntries(
              entry("mapEntry", Map.Entry.class),
              entry("string", String.class),
              entry("mergedField", MergedField.class),
              entry("errable", Errable.class),
              entry("fetcherResponse", fetcherResponseType),
              entry("entityIdFacet", entityIdFacetName),
              entry("depReqPojoType", depReqImmutPojoType),
              entry("entityIdFacetName", Facets.ENTITY_ID)));
    } else {
      // Single entity: for each alias, create one request using the alias's ID
      methodBuilder.addNamedCode(
          """
          for (int _i = 0; _i < _aliases.size(); _i++) {
            $mapEntry:T<$string:T, $mergedField:T> _aliasEntry = _aliases.get(_i);
            $errable:T<$fetcherResponse:T> _idErrable = $entityIdFacet:L.requestResponsePairs().get(_i).response();
            if (_idErrable.valueOpt().isEmpty()) {
              continue;
            }
            var graphql_executionStrategyParams_new =
                graphql_executionStrategy.newParametersForFieldExecution(
                    graphql_executionContext, graphql_executionStrategyParams,
                    _aliasEntry.getValue());
            _reqs.add($depReqPojoType:T._builder()
                .$entityIdFacetName:L(_idErrable.valueOpt().get())
                .graphql_executionContext(graphql_executionContext)
                .graphql_executionStrategy(graphql_executionStrategy)
                .graphql_executionStrategyParams(graphql_executionStrategyParams_new));
          }
          """,
          Map.ofEntries(
              entry("mapEntry", Map.Entry.class),
              entry("string", String.class),
              entry("mergedField", MergedField.class),
              entry("errable", Errable.class),
              entry("fetcherResponse", fetcherResponseType),
              entry("entityIdFacet", entityIdFacetName),
              entry("depReqPojoType", depReqImmutPojoType),
              entry("entityIdFacetName", Facets.ENTITY_ID)));
    }

    methodBuilder.addCode(
        """
        if (_reqs.isEmpty()) {
          return $T.skipFanout($S);
        }
        return $T.executeFanoutWith(_reqs);
        """,
        FanoutCommand.class,
        "'" + fieldName + "' no IDs",
        FanoutCommand.class);

    return methodBuilder.build();
  }
}
