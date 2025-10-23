package com.flipkart.krystal.vajram.sql.codegen;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.IOVajramDef;
import com.flipkart.krystal.vajram.TraitRoot;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.codegen.common.models.VajramCodeGenUtility;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfo;
import com.flipkart.krystal.vajram.codegen.common.spi.VajramCodeGenContext;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.InputResolvers;
import com.flipkart.krystal.vajram.facets.resolution.SimpleInputResolver;
import com.flipkart.krystal.vajram.sql.SqlQuery;
import com.flipkart.krystal.vajram.sql.r2dbc.SQLRead;
import com.flipkart.krystal.vajram.sql.r2dbc.SQLRead_Req;
import com.google.common.collect.ImmutableCollection;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import io.r2dbc.spi.Result;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

final class SqlTraitCodeGenerator implements CodeGenerator {
  private final VajramCodeGenContext codeGenContext;
  private final VajramCodeGenUtility util;

  SqlTraitCodeGenerator(VajramCodeGenContext codeGenContext) {
    this.codeGenContext = codeGenContext;
    this.util = codeGenContext.util();
    System.out.println("Initialized SqlTraitCodeGenerator");
  }

  @Override
  public void generate() {
    if (!isApplicable()) {
      return;
    }

    VajramInfo vajramInfo = codeGenContext.vajramInfo();
    TypeElement traitElement = vajramInfo.vajramClassElem();

    System.out.println("Generating SQL Vajram for trait: " + traitElement.getSimpleName());
    // Extract SQL query from annotation
    String sqlQuery = extractSqlQuery(traitElement);
    if (sqlQuery == null) {
      return; // No @Sql annotation found
    }

    // Extract returnType parameter (e.g., List<User> from GetUserTrait<List<User>>)
    //Instead of extracting from GetUserTrait, extract from TraitRoot generic type which is implemented by trait
    //Get the trait root type
    List<? extends TypeMirror> traitRootType = traitElement.getInterfaces();
    System.out.println("Trait root type: " + traitRootType.get(0));
    TypeMirror returnType = extractGenericType(traitElement);
    //validate return type and Extract entity type from returnType
    TypeMirror entityType = extractEntityType(returnType);
    if (entityType == null) {
      return;
    }
    // Generate the implementation class with same packagename as of trait
    String packageName = util.codegenUtil().getPackageName(traitElement);
    String implClassName = traitElement.getSimpleName().toString() + "_sql";

    TypeSpec implClass = generateSqlVajram(traitElement, implClassName, sqlQuery, returnType, entityType, packageName);

    // Build JavaFile with static imports
    String facetsClassName = implClassName + "_Fac";
    JavaFile javaFile = JavaFile.builder(packageName, implClass)
        .addStaticImport(IfAbsent.IfAbsentThen.class, "FAIL")
        .addStaticImport(InputResolvers.class, "*")
        .addStaticImport(ClassName.get(packageName, facetsClassName), "*")
        .indent("  ")
        .build();

    try {
      javaFile.writeTo(util.codegenUtil().processingEnv().getFiler());
    } catch (Exception e) {
      util.codegenUtil().error("Failed to write generated file: " + e.getMessage(), traitElement);
    }
  }

  private TypeMirror extractEntityType(TypeMirror returnType) {
    if (returnType == null) {
      util.codegenUtil().error("Return type cannot be null for the trait element");
      return null;
    }else if (returnType.getKind() != TypeKind.DECLARED) {
      util.codegenUtil().error("Return type must be a declared type.");
      return null;
    }
    DeclaredType declaredType = (DeclaredType) returnType;
    //Check if the return type is exactly List<XYZ> where XYZ can be of any class
    TypeMirror listRawType = CodeGenUtility.
        getTypeElement("java.util.List", util.codegenUtil().processingEnv()).asType();

    System.out.println("Declared type: "+ declaredType);
    System.out.println("RawType type: "+ listRawType);
    boolean isList = util.processingEnv().getTypeUtils().
        isSameType(util.processingEnv().getTypeUtils().erasure(declaredType), util.processingEnv().getTypeUtils().erasure(listRawType));
    if (!isList) {
      util.codegenUtil().error("Return type must be a List type.");
      return null;
    }
    List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
    if (typeArguments.size() == 1) {
      return typeArguments.get(0);
    } else {
      // Error for parameterized type with no arguments (e.g., List)
      util.codegenUtil().error("Parameterized return type must have one type arguments.");
      return null;
    }
  }

  private TypeSpec generateSqlVajram(
      TypeElement traitElement,
      String implClassName,
      String sqlQuery,
      TypeMirror returnType,
      TypeMirror entityType,
      String packageName) {

    //if returnType is ParameterizedType of List<GenericType>, extract GenericType

    TypeName entityTypeName = TypeName.get(entityType);
    TypeName returnTypeName = TypeName.get(returnType);

    // Validate _Inputs structure
    validateInputsClass(traitElement);

    // Create class builder extending IOVajramDef<List<GenericType>>
    TypeSpec.Builder classBuilder = TypeSpec.classBuilder(implClassName)
        .addModifiers(PUBLIC, ABSTRACT)
        .superclass(ParameterizedTypeName.get(
            ClassName.get(IOVajramDef.class), returnTypeName))
        .addSuperinterface(ClassName.get(traitElement))
        .addAnnotation(
            AnnotationSpec.builder(SuppressWarnings.class)
                .addMember("value", "$S", "initialization.field.uninitialized")
                .build())
        .addAnnotation(InvocableOutsideGraph.class)
        .addAnnotation(Vajram.class);

    // Generate _Inputs inner class
    TypeSpec inputsClass = generateInputsClass(traitElement, sqlQuery);
    classBuilder.addType(inputsClass);

    // Generate _InternalFacets inner class
    TypeSpec internalFacetsClass = generateInternalFacetsClass();
    classBuilder.addType(internalFacetsClass);

    // Generate getSimpleInputResolvers method
    MethodSpec resolversMethod = generateGetSimpleInputResolvers(sqlQuery);
    classBuilder.addMethod(resolversMethod);

    // Generate @Output method
    MethodSpec outputMethod = generateOutputMethod(traitElement, entityTypeName);
    classBuilder.addMethod(outputMethod);

    return classBuilder.build();
  }

  private void validateInputsClass(TypeElement traitElement) {
    TypeElement inputsElement = findInnerClass(traitElement, "_Inputs");
    if (inputsElement == null) {
      util.codegenUtil().error("@Sql trait must have a static _Inputs inner class", traitElement);
      return;
    }

    // Check for required "parameters" field
    boolean hasParameters = false;
    for (VariableElement field : getFields(inputsElement)) {
      if (field.getSimpleName().toString().equals("parameters")) {
        // Validate it's of type List<Object>
        TypeMirror fieldType = field.asType();
        String typeString = fieldType.toString();
        if (typeString.contains("List") || typeString.contains("java.util.List")) {
          hasParameters = true;
          break;
        }
      }
    }

    if (!hasParameters) {
      util.codegenUtil().error(
          "@Sql trait _Inputs must contain a 'parameters' field of type List<Object>",
          inputsElement);
    }
  }

  private TypeSpec generateInputsClass(TypeElement traitElement, String sqlQuery) {
    TypeSpec.Builder inputsBuilder = TypeSpec.classBuilder("_Inputs")
        .addModifiers(STATIC);

    // Find existing _Inputs class in trait
    TypeElement inputsElement = findInnerClass(traitElement, "_Inputs");
    
    if (inputsElement != null) {
      // Copy fields from trait's _Inputs
      for (VariableElement field : getFields(inputsElement)) {
        FieldSpec.Builder fieldBuilder = FieldSpec.builder(
            TypeName.get(field.asType()),
            field.getSimpleName().toString());

        // Copy annotations
        for (AnnotationMirror annotation : field.getAnnotationMirrors()) {
          fieldBuilder.addAnnotation(AnnotationSpec.get(annotation));
        }
        if (field.getSimpleName().toString().equals("query")) {
          fieldBuilder.initializer("$S", sqlQuery);
        }
        inputsBuilder.addField(fieldBuilder.build());
      }
    }

//    // Add query field
//    inputsBuilder.addField(
//        FieldSpec.builder(String.class, "query")
//            .addAnnotation(
//                AnnotationSpec.builder(IfAbsent.class)
//                    .addMember("value", "$T.FAIL", IfAbsent.IfAbsentThen.class)
//                    .build())
//            .build());

    return inputsBuilder.build();
  }

  private TypeSpec generateInternalFacetsClass() {
    TypeSpec.Builder facetsBuilder = TypeSpec.classBuilder("_InternalFacets")
        .addModifiers(STATIC);

    // Add queryResult field with @Dependency on SQLRead
    facetsBuilder.addField(
        FieldSpec.builder(Result.class, "queryResult")
            .addAnnotation(
                AnnotationSpec.builder(IfAbsent.class)
                    .addMember("value", "$T.FAIL", IfAbsent.IfAbsentThen.class)
                    .build())
            .addAnnotation(
                AnnotationSpec.builder(Dependency.class)
                    .addMember("onVajram", "$T.class", SQLRead.class)
                    .build())
            .build());

    // Add optional RowMapper field
    facetsBuilder.addField(
        FieldSpec.builder(ClassName.get("com.flipkart.krystal.vajram.sql.r2dbc", "RowMapper"), "resultMapper")
            .addAnnotation(Inject.class)
            .build());

    return facetsBuilder.build();
  }

  private MethodSpec generateGetSimpleInputResolvers(String sqlQuery) {
    return MethodSpec.methodBuilder("getSimpleInputResolvers")
        .addAnnotation(Override.class)
        .addAnnotation(NonNull.class)
        .addModifiers(PUBLIC)
        .returns(ParameterizedTypeName.get(
            ClassName.get(ImmutableCollection.class),
            WildcardTypeName.subtypeOf(ClassName.get(SimpleInputResolver.class))))
        .addStatement(
            "return resolve(\n    dep(\n        queryResult_s,\n        depInput($T.selectQuery_s).usingValueAsResolver(() -> $S),\n        depInput($T.parameters_s).usingAsIs(parameters_s).asResolver()))",
            ClassName.get(SQLRead_Req.class), sqlQuery,
            ClassName.get(SQLRead_Req.class))
        .build();
  }

  private MethodSpec generateOutputMethod(TypeElement traitElement, TypeName genericTypeName) {
    String methodName = toMethodName(traitElement.getSimpleName().toString());
    TypeName returnType = ParameterizedTypeName.get(
        ClassName.get(CompletableFuture.class),
        ParameterizedTypeName.get(ClassName.get(List.class), genericTypeName));

    ClassName rowMapperClass = ClassName.get("com.flipkart.krystal.vajram.sql.r2dbc", "RowMapper");

    return MethodSpec.methodBuilder(methodName)
        .addAnnotation(Output.class)
        .addModifiers(PUBLIC, STATIC)
        .returns(returnType)
        .addParameter(Result.class, "queryResult")
        .addParameter(
            rowMapperClass.annotated(AnnotationSpec.builder(Nullable.class).build()),
            "resultMapper")
        .beginControlFlow("if (resultMapper != null)")
        .addStatement("return resultMapper.map(queryResult, $T.class).toFuture()", genericTypeName)
        .nextControlFlow("else")
        .addComment("TODO: Handle case when resultMapper is null")
        .endControlFlow()
        .addStatement("return null")
        .build();
  }

  private String toMethodName(String traitName) {
    // Convert GetUserTrait to getUser
    String name = traitName;
    if (name.endsWith("Trait")) {
      name = name.substring(0, name.length() - 5);
    }
    return Character.toLowerCase(name.charAt(0)) + name.substring(1);
  }

  private String extractSqlQuery(TypeElement element) {
    SqlQuery sqlAnnotation = element.getAnnotation(SqlQuery.class);
    if (sqlAnnotation != null) {
      return sqlAnnotation.value();
    }
    return null;
  }

  private TypeMirror extractGenericType(TypeElement element) {
    // Look for TraitRoot<T> in interfaces
    for (TypeMirror iface : element.getInterfaces()) {
      if (iface instanceof DeclaredType) {
        DeclaredType declaredType = (DeclaredType) iface;
        TypeElement ifaceElement = (TypeElement) declaredType.asElement();
        
        if (ifaceElement.getQualifiedName().toString().equals(TraitRoot.class.getName())) {
          List<? extends TypeMirror> typeArgs = declaredType.getTypeArguments();
          if (!typeArgs.isEmpty()) {
            return typeArgs.get(0);
          }
        }
      }
    }
    return null;
  }

  private TypeElement findInnerClass(TypeElement outerClass, String innerClassName) {
    for (var enclosed : outerClass.getEnclosedElements()) {
      if (enclosed instanceof TypeElement) {
        TypeElement typeElement = (TypeElement) enclosed;
        if (typeElement.getSimpleName().toString().equals(innerClassName)) {
          return typeElement;
        }
      }
    }
    return null;
  }

  /**
   * Extracts all field elements from a TypeElement.
   */
  private List<VariableElement> getFields(TypeElement element) {
    List<VariableElement> fields = new ArrayList<>();
    for (Element enclosed : element.getEnclosedElements()) {
      if (enclosed.getKind() == ElementKind.FIELD) {
        fields.add((VariableElement) enclosed);
      }
    }
    return fields;
  }

  private boolean isApplicable() {
    if (!CodegenPhase.MODELS.equals(codeGenContext.codegenPhase())) {
      return false;
    }

    VajramInfo vajramInfo = codeGenContext.vajramInfo();
    TypeElement element = vajramInfo.vajramClassElem();
    System.out.println("SqlTraitCodeGenerator checking element: " + element.getSimpleName());

    // Check if it's a trait (interface)
    if (!element.getKind().isInterface()) {
      return false;
    }

    // Check if it has @Sql annotation
    System.out.println("Checking if element is a trait: " + element.getKind().isInterface());
    System.out.println("Checking if element has @Sql annotation: " + element.getAnnotation(SqlQuery.class));
    return element.getAnnotation(SqlQuery.class) != null;
  }
}
