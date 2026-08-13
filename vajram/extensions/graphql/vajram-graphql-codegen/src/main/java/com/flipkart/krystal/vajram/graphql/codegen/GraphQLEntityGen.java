package com.flipkart.krystal.vajram.graphql.codegen;

import static com.flipkart.krystal.vajram.graphql.api.Constants.Directives.DATA_FETCHER;
import static com.flipkart.krystal.vajram.graphql.codegen.CodeGenConstants.IF_ABSENT_FAIL;
import static com.flipkart.krystal.vajram.graphql.codegen.GraphQlCodeGenUtil.GRAPHQL_FIELDS_SUFFIX;
import static com.flipkart.krystal.vajram.graphql.codegen.SchemaReaderUtil.fieldSpecFromField;
import static com.flipkart.krystal.vajram.graphql.codegen.SchemaReaderUtil.isMultiFieldDataFetcher;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static java.util.Objects.requireNonNullElse;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.vajram.graphql.api.Constants.Directives;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlResponse;
import com.squareup.javapoet.*;
import com.squareup.javapoet.TypeSpec.Builder;
import graphql.language.*;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.NonNull;

class GraphQLEntityGen implements CodeGenerator {

  private final CodeGenUtility util;
  private final GraphQlCodeGenUtil graphQlCodeGenUtil;
  private final SchemaReaderUtil schemaReaderUtil;

  public GraphQLEntityGen(CodeGenUtility util, File schemaFile) {
    this.util = util;
    this.graphQlCodeGenUtil = new GraphQlCodeGenUtil(schemaFile);
    this.schemaReaderUtil = graphQlCodeGenUtil.schemaReaderUtil();
  }

  @Override
  public void generate() {
    TypeDefinitionRegistry typeDefinitionRegistry = schemaReaderUtil.typeDefinitionRegistry();
    util.note("Generating enum models for types : %s".formatted(typeDefinitionRegistry.types()));
    for (TypeDefinition<?> typeDefinition : typeDefinitionRegistry.types().values()) {
      if (typeDefinition instanceof EnumTypeDefinition enumTypeDefinition) {
        GraphQLTypeName graphQLTypeName = GraphQLTypeName.of(typeDefinition);
        try {
          Builder typeSpec = TypeSpec.enumBuilder(graphQLTypeName.value()).addModifiers(PUBLIC);
          enumTypeDefinition
              .getEnumValueDefinitions()
              .forEach(
                  enumValueDefinition -> typeSpec.addEnumConstant(enumValueDefinition.getName()));
          typeSpec.addJavadoc(getDescription(enumTypeDefinition));
          util.generateSourceFile(
              schemaReaderUtil.typeClassName(graphQLTypeName).canonicalName(),
              JavaFile.builder(
                      schemaReaderUtil.typeClassName(graphQLTypeName).packageName(),
                      typeSpec.build())
                  .build()
                  .toString(),
              null);
        } catch (Throwable e) {
          util.error(
              "Could not generate id models and data models for type '%s' due to error '%s'"
                  .formatted(graphQLTypeName, getStackTraceAsString(e)));
        }
      }
    }

    Map<GraphQLTypeName, @NonNull ObjectTypeDefinition> aggregatableTypes =
        schemaReaderUtil.graphQLObjectTypes();
    util.note(
        "Evaluating '%s' to generate GraphQl Field Models where needed (i.e. for multiField data fetchers)"
            .formatted(aggregatableTypes));
    aggregatableTypes.forEach(
        (graphQLTypeName, entityTypeDefinition) -> {
          try {
            // Generate aggregate response models only for explicit multi-field fetchers.

            Map<ClassName, List<FieldDefinition>> fieldDefinitions = new HashMap<>();

            for (FieldDefinition fieldDefinition : entityTypeDefinition.getFieldDefinitions()) {
              if (!fieldDefinition.getDirectives(DATA_FETCHER).isEmpty()
                  && isMultiFieldDataFetcher(fieldDefinition)) {
                fieldDefinitions
                    .computeIfAbsent(
                        schemaReaderUtil.getDataFetcherClassName(fieldDefinition),
                        _k -> new ArrayList<>())
                    .add(fieldDefinition);
              }
            }
            // A multiField dataFetcher always returns its aggregate response, including when it
            // supplies just one GraphQL field.
            fieldDefinitions.forEach(
                (dataFetcherName, fieldDefinitionList) -> {
                  ClassName className =
                      ClassName.get(
                          dataFetcherName.packageName(),
                          dataFetcherName.simpleName() + GRAPHQL_FIELDS_SUFFIX);
                  generateResponseModel(className, fieldDefinitionList, graphQLTypeName, false);
                });
          } catch (Throwable e) {
            util.error(
                "Could not generate GraphQl Fields Models for type '%s' due to error '%s'"
                    .formatted(graphQLTypeName, getStackTraceAsString(e)));
          }
        });

    schemaReaderUtil
        .graphQLObjectTypes()
        .forEach(
            (graphQLTypeName, typeDefinition) -> {
              if (!schemaReaderUtil.hasOwnIdentity(typeDefinition)) {
                return;
              }
              try {
                List<FieldDefinition> idFields =
                    typeDefinition.getFieldDefinitions().stream()
                        .filter(field -> field.hasDirective(Directives.ID_FIELD))
                        .filter(field -> field.getInputValueDefinitions().isEmpty())
                        .toList();
                if (idFields.isEmpty()) {
                  return;
                }
                ClassName className =
                    ClassName.get(
                        schemaReaderUtil.getPackageNameForType(graphQLTypeName),
                        graphQLTypeName.value() + GraphQlCodeGenUtil.GRAPHQL_ID_SUFFIX);
                generateResponseModel(className, idFields, graphQLTypeName, true);
              } catch (Throwable e) {
                util.error(
                    "Could not generate GraphQl Core Model for type '%s' due to error '%s'"
                        .formatted(graphQLTypeName, getStackTraceAsString(e)));
              }
            });
  }

  private void generateResponseModel(
      ClassName className,
      List<FieldDefinition> fieldDefinitions,
      GraphQLTypeName enclosingType,
      boolean isIdModel) {
    Builder builder = TypeSpec.interfaceBuilder(className);
    for (FieldDefinition fieldDefinition : fieldDefinitions) {
      builder.addMethod(
          MethodSpec.methodBuilder(fieldDefinition.getName())
              .addModifiers(PUBLIC, ABSTRACT)
              .addAnnotation(IF_ABSENT_FAIL)
              .returns(
                  graphQlCodeGenUtil.toTypeNameForField(
                      fieldSpecFromField(fieldDefinition, "", enclosingType)))
              .build());
    }
    ClassName modelRootClassName = ClassName.get("com.flipkart.krystal.model", "ModelRoot");
    AnnotationSpec.Builder modelRoot =
        AnnotationSpec.builder(modelRootClassName).addMember("pure", "false");
    if (isIdModel) {
      modelRoot.addMember(
          "type",
          "{$T.$L, $T.$L}",
          modelRootClassName.nestedClass("ModelType"),
          "REQUEST",
          modelRootClassName.nestedClass("ModelType"),
          "RESPONSE");
    } else {
      modelRoot.addMember("type", "$T.$L", modelRootClassName.nestedClass("ModelType"), "RESPONSE");
    }
    builder
        .addModifiers(PUBLIC)
        .addSuperinterface(ClassName.get("com.flipkart.krystal.model", "Model"))
        .addAnnotation(modelRoot.build())
        .addAnnotation(
            AnnotationSpec.builder(ClassName.get(SupportedModelProtocol.class))
                .addMember("value", "$T.class", ClassName.get(GraphQlResponse.class))
                .build());
    util.generateSourceFile(
        className.canonicalName(),
        JavaFile.builder(className.packageName(), builder.build()).build().toString(),
        null);
  }

  private static String getDescription(AbstractDescribedNode<?> describedNode) {
    Description description = describedNode.getDescription();
    if (description == null) {
      return "";
    }
    return requireNonNullElse(description.getContent(), "");
  }
}
