package com.flipkart.krystal.vajram.sql.codegen;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfo;
import com.flipkart.krystal.vajram.codegen.common.spi.VajramCodeGenContext;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.InputResolvers;
import com.flipkart.krystal.vajram.facets.resolution.SimpleInputResolver;
import com.flipkart.krystal.vajram.sql.SQLResult;
import com.flipkart.krystal.vajram.sql.r2dbc.SQLRead;
import com.flipkart.krystal.vajram.sql.r2dbc.SQLRead_Req;
import com.flipkart.krystal.vajram.sql.r2dbc.SQLWrite;
import com.flipkart.krystal.vajram.sql.r2dbc.SQLWrite_Req;
import com.google.common.collect.ImmutableCollection;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Code generator for SQL Traits that generates ComputeVajramDef implementations wrapping SQLRead or
 * SQLWrite vajrams.
 *
 * <p>This generator creates vajrams for traits annotated with @SqlQuery and @SqlUpdate.
 */
final class DefaultSqlTraitVajramGenerator extends AbstractSqlCodeGenerator {

  // Constants
  private static final String IMPL_SUFFIX = "_Sql";
  private static final String FACETS_SUFFIX = "_Fac";

  DefaultSqlTraitVajramGenerator(VajramCodeGenContext codeGenContext) {
    super(codeGenContext);
  }

  @Override
  public void generate() {
    if (!isApplicable()) {
      return;
    }

    VajramInfo vajramInfo = codeGenContext.vajramInfo();
    TypeElement traitElement = vajramInfo.vajramClassElem();

    // Determine if this is a query or update
    String sqlQuery = extractSqlQuery(traitElement);
    String sqlUpdate = extractSqlUpdate(traitElement);

    boolean isQuery = sqlQuery != null;

    if (sqlQuery == null && sqlUpdate == null) {
      return;
    } else if (sqlQuery != null && sqlUpdate != null) {
      throw util.codegenUtil()
          .errorAndThrow("Trait cannot have both @SqlQuery and @SqlUpdate", traitElement);
    }

    String sqlStatement = isQuery ? sqlQuery : sqlUpdate;
    TypeMirror returnType = extractGenericType(traitElement);

    if (returnType == null) {
      throw util.codegenUtil()
          .errorAndThrow("Could not extract return type from TraitRoot", traitElement);
    }
    TypeMirror entityType = extractEntityTypeFromList(returnType);
    if (entityType == null) {
      throw util.codegenUtil()
          .errorAndThrow("Could not extract entity type from TraitRoot", traitElement);
    }
    validateTraitElement(traitElement, returnType, entityType, isQuery);
    // Generate the implementation class
    String packageName = util.codegenUtil().getPackageName(traitElement);
    String implClassName = traitElement.getSimpleName().toString() + IMPL_SUFFIX;

    TypeSpec implClass =
        generateComputeVajram(
            traitElement, implClassName, sqlStatement, returnType, entityType, isQuery);

    // Build JavaFile with static imports
    String facetsClassName = implClassName + FACETS_SUFFIX;
    JavaFile javaFile =
        JavaFile.builder(packageName, implClass)
            .addStaticImport(IfAbsent.IfAbsentThen.class, "FAIL")
            .addStaticImport(InputResolvers.class, "*")
            .addStaticImport(ClassName.get(packageName, facetsClassName), "*")
            .indent("  ")
            .build();

    try {
      javaFile.writeTo(util.codegenUtil().processingEnv().getFiler());
    } catch (Exception e) {
      throw util.codegenUtil()
          .errorAndThrow("Failed to write generated file: " + e.getMessage(), traitElement);
    }
  }

  private TypeSpec generateComputeVajram(
      TypeElement traitElement,
      String implClassName,
      String sqlStatement,
      TypeMirror returnType,
      TypeMirror entityType,
      boolean isQuery) {

    TypeName returnTypeName = TypeName.get(returnType);

    // Create class builder extending ComputeVajramDef<ReturnType>
    TypeSpec.Builder classBuilder =
        TypeSpec.classBuilder(implClassName)
            .addModifiers(PUBLIC, ABSTRACT)
            .superclass(
                ParameterizedTypeName.get(ClassName.get(ComputeVajramDef.class), returnTypeName))
            .addSuperinterface(ClassName.get(traitElement))
            .addAnnotation(
                AnnotationSpec.builder(SuppressWarnings.class)
                    .addMember("value", "$S", "initialization.field.uninitialized")
                    .build())
            .addAnnotation(InvocableOutsideGraph.class)
            .addAnnotation(Vajram.class);

    // Generate _Inputs inner class
    TypeSpec inputsClass = generateInputsClass(traitElement);
    classBuilder.addType(inputsClass);

    // Generate _InternalFacets inner class
    TypeSpec internalFacetsClass = generateInternalFacetsClass(isQuery);
    classBuilder.addType(internalFacetsClass);

    // Generate getSimpleInputResolvers method
    MethodSpec resolversMethod = generateGetSimpleInputResolvers(sqlStatement, entityType, isQuery);
    classBuilder.addMethod(resolversMethod);

    // Generate @Output method
    MethodSpec outputMethod = generateOutputMethod(traitElement, returnType, entityType, isQuery);
    classBuilder.addMethod(outputMethod);

    return classBuilder.build();
  }

  private TypeSpec generateInputsClass(TypeElement traitElement) {
    TypeSpec.Builder inputsBuilder = TypeSpec.classBuilder("_Inputs").addModifiers(STATIC);

    // Find existing _Inputs class in trait
    TypeElement inputsElement = findInnerClass(traitElement, "_Inputs");

    if (inputsElement != null) {
      // Copy fields from trait's _Inputs
      for (VariableElement field : getFields(inputsElement)) {
        FieldSpec.Builder fieldBuilder =
            FieldSpec.builder(TypeName.get(field.asType()), field.getSimpleName().toString());

        // Copy annotations
        for (AnnotationMirror annotation : field.getAnnotationMirrors()) {
          fieldBuilder.addAnnotation(AnnotationSpec.get(annotation));
        }

        inputsBuilder.addField(fieldBuilder.build());
      }
    }

    return inputsBuilder.build();
  }

  private TypeSpec generateInternalFacetsClass(boolean isQuery) {
    TypeSpec.Builder facetsBuilder = TypeSpec.classBuilder("_InternalFacets").addModifiers(STATIC);

    // Add queryResult field with @Dependency on SQLRead or SQLWrite
    ClassName dependencyClass =
        isQuery ? ClassName.get(SQLRead.class) : ClassName.get(SQLWrite.class);

    facetsBuilder.addField(
        FieldSpec.builder(ClassName.get(SQLResult.class), "queryResult")
            .addAnnotation(
                AnnotationSpec.builder(IfAbsent.class)
                    .addMember("value", "$T.FAIL", IfAbsent.IfAbsentThen.class)
                    .build())
            .addAnnotation(
                AnnotationSpec.builder(Dependency.class)
                    .addMember("onVajram", "$T.class", dependencyClass)
                    .build())
            .build());

    return facetsBuilder.build();
  }

  private MethodSpec generateGetSimpleInputResolvers(
      String sqlStatement, TypeMirror entityType, boolean isQuery) {

    CodeBlock.Builder codeBuilder = CodeBlock.builder();

    if (isQuery) {
      codeBuilder.add(
          """
          return resolve(
              dep(
                  queryResult_s,
                  depInput($T.selectQuery_s).usingValueAsResolver(() -> $S),
                  depInput($T.parameters_s).usingAsIs(parameters_s).asResolver(),
                  depInput($T.resultType_s).usingValueAsResolver(() -> $T.class)))""",
          ClassName.get(SQLRead_Req.class),
          sqlStatement,
          ClassName.get(SQLRead_Req.class),
          ClassName.get(SQLRead_Req.class),
          TypeName.get(entityType));
    } else {
      // For updates, use SQLWrite
      codeBuilder.add(
          """
          return resolve(
              dep(
                  queryResult_s,
                  depInput($T.query_s).usingValueAsResolver(() -> $S),
                  depInput($T.parameters_s).usingAsIs(parameters_s).asResolver()))""",
          ClassName.get(SQLWrite_Req.class),
          sqlStatement,
          ClassName.get(SQLWrite_Req.class));
    }

    return MethodSpec.methodBuilder("getSimpleInputResolvers")
        .addAnnotation(Override.class)
        .addAnnotation(NonNull.class)
        .addModifiers(PUBLIC)
        .returns(
            ParameterizedTypeName.get(
                ClassName.get(ImmutableCollection.class),
                WildcardTypeName.subtypeOf(ClassName.get(SimpleInputResolver.class))))
        .addStatement(codeBuilder.build())
        .build();
  }

  private MethodSpec generateOutputMethod(
      TypeElement traitElement, TypeMirror returnType, TypeMirror entityType, boolean isQuery) {

    String methodName = isQuery ? "get" : "result";
    TypeName returnTypeName = TypeName.get(returnType);

    MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder(methodName)
            .addAnnotation(Output.class)
            .addModifiers(PUBLIC, STATIC)
            .returns(returnTypeName)
            .addParameter(ClassName.get(SQLResult.class), "queryResult");

    if (isQuery) {
      // For queries, check if return type is List<T> or just T

      if (!entityType.equals(returnType)) {
        // List<User> case - return the entire list
        methodBuilder.addStatement("return ($T) queryResult.rows()", returnTypeName);
      } else {
        // Single User case - return first element or null
        methodBuilder
            .beginControlFlow("if (queryResult.rows().isEmpty())")
            .addStatement("return null")
            .endControlFlow()
            .addStatement(
                "$T users = ($T) queryResult.rows()",
                ParameterizedTypeName.get(ClassName.get(List.class), returnTypeName),
                ParameterizedTypeName.get(ClassName.get(List.class), returnTypeName))
            .addStatement("return users.get(0)");
      }
    } else {
      // For updates, return rowsUpdated count
      methodBuilder.addStatement("return queryResult.rowsUpdated()");
    }

    return methodBuilder.build();
  }
}
