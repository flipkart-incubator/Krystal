package com.flipkart.krystal.vajram.sql.codegen;

import com.flipkart.krystal.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.vajram.TraitRoot;
import com.flipkart.krystal.vajram.codegen.common.models.VajramCodeGenUtility;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfo;
import com.flipkart.krystal.vajram.codegen.common.spi.VajramCodeGenContext;
import com.flipkart.krystal.vajram.sql.annotations.SqlQuery;
import com.flipkart.krystal.vajram.sql.annotations.SqlUpdate;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

public abstract class AbstractSqlCodeGenerator implements CodeGenerator {

  protected final VajramCodeGenContext codeGenContext;
  protected final VajramCodeGenUtility util;

  protected AbstractSqlCodeGenerator(VajramCodeGenContext codeGenContext) {
    this.codeGenContext = codeGenContext;
    this.util = codeGenContext.util();
  }

  /**
   * Checks if this generator is applicable to the current code generation context. By default,
   * checks for MODELS phase, interface type, and SQL annotations.
   */
  protected boolean isApplicable() {
    if (!CodegenPhase.MODELS.equals(codeGenContext.codegenPhase())) {
      return false;
    }

    VajramInfo vajramInfo = codeGenContext.vajramInfo();
    TypeElement element = vajramInfo.vajramClassElem();

    // Check if it's a trait (interface)
    if (!element.getKind().isInterface()) {
      return false;
    }

    // Check if it has @SqlQuery or @SqlUpdate annotation
    return element.getAnnotation(SqlQuery.class) != null
        || element.getAnnotation(SqlUpdate.class) != null;
  }

  /**
   * Extracts the SQL query string from @SqlQuery annotation.
   *
   * @return SQL query string or null if annotation not present
   */
  protected String extractSqlQuery(TypeElement element) {
    SqlQuery sqlAnnotation = element.getAnnotation(SqlQuery.class);
    if (sqlAnnotation != null) {
      return sqlAnnotation.value();
    }
    return null;
  }

  /**
   * Extracts the SQL update/insert/delete string from @SqlUpdate annotation.
   *
   * @return SQL statement string or null if annotation not present
   */
  protected String extractSqlUpdate(TypeElement element) {
    SqlUpdate sqlAnnotation = element.getAnnotation(SqlUpdate.class);
    if (sqlAnnotation != null) {
      return sqlAnnotation.value();
    }
    return null;
  }

  /**
   * Extracts the generic type parameter from TraitRoot<T> interface. For example, if trait extends
   * TraitRoot<List<User>>, returns List<User>.
   *
   * @param element The trait element to extract from
   * @return The generic type T from TraitRoot<T>, or null if not found
   */
  protected TypeMirror extractGenericType(TypeElement element) {
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

  /**
   * Extracts entity type from List<T> return type. If returnType is List<T>, returns T. Otherwise
   * returns returnType itself.
   *
   * <p>Examples:
   *
   * <ul>
   *   <li>List<User> -> User
   *   <li>User -> User
   *   <li>List<String> -> String
   * </ul>
   *
   * @param returnType The return type to extract from
   * @return The entity type T if List<T>, otherwise the returnType itself, or null if invalid
   */
  protected TypeMirror extractEntityTypeFromList(TypeMirror returnType) {
    boolean isList = isListType(returnType);
    if (!isList) {
      return returnType;
    }
    DeclaredType declaredType = (DeclaredType) returnType;
    List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
    if (typeArguments.size() == 1) {
      return typeArguments.get(0);
    }

    return null;
  }

  /**
   * Checks if the given type is java.util.List.
   *
   * @param typeMirror The type to check
   * @return true if the type is List, false otherwise
   */
  protected boolean isListType(TypeMirror typeMirror) {
    // use codegenutil isSameType function
    return util.codegenUtil().isSameRawType(typeMirror, java.util.List.class);
  }

  /**
   * Finds an inner class by name within an outer class.
   *
   * @param outerClass The outer class to search in
   * @param innerClassName The name of the inner class to find
   * @return The TypeElement of the inner class, or null if not found
   */
  protected TypeElement findInnerClass(TypeElement outerClass, String innerClassName) {
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
   *
   * @param element The type element to extract fields from
   * @return List of field elements
   */
  protected List<VariableElement> getFields(TypeElement element) {
    List<VariableElement> fields = new ArrayList<>();
    for (Element enclosed : element.getEnclosedElements()) {
      if (enclosed.getKind() == ElementKind.FIELD) {
        fields.add((VariableElement) enclosed);
      }
    }
    return fields;
  }

  /**
   * Validates that the trait element has required structure for SQL code generation. Checks for
   * _Inputs class with parameters field of type List<Object>.
   *
   * @param traitElement The trait element to validate
   * @throws RuntimeException if validation fails
   */
  protected void validateInputsClass(TypeElement traitElement) {
    TypeElement inputsElement = findInnerClass(traitElement, "_Inputs");
    if (inputsElement == null) {
      throw util.codegenUtil()
          .errorAndThrow("Sql trait must have a static _Inputs inner class", traitElement);
    }

    // Check for required "parameters" field
    boolean hasParameters = false;
    for (VariableElement field : getFields(inputsElement)) {
      if (field.getSimpleName().toString().equals("parameters")) {
        hasParameters = true;
        boolean isList = isListType(field.asType());
        if (!isList) {
          throw util.codegenUtil()
              .errorAndThrow(
                  "Sql trait _Inputs must contain a 'parameters' field of type List<Object>",
                  inputsElement);
        } else {
          DeclaredType declaredType = (DeclaredType) field.asType();
          List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
          if (!typeArguments.isEmpty()
              && !typeArguments.get(0).toString().equals("java.lang.Object")) {
            throw util.codegenUtil()
                .errorAndThrow(
                    "Sql trait _Inputs must contain a 'parameters' field of type List<Object>",
                    inputsElement);
          }
        }
        break;
      }
    }

    if (!hasParameters) {
      throw util.codegenUtil()
          .errorAndThrow(
              "Sql trait _Inputs must contain a 'parameters' field of type List<Object>",
              inputsElement);
    }
  }

  /**
   * Validates that the entity type is not a parameterized type. SQL entity types should be simple
   * classes, not generic types.
   *
   * @param entityType The entity type to validate
   * @param traitElement The trait element for error reporting
   * @throws RuntimeException if entity type is parameterized
   */
  protected void validateEntityType(TypeMirror entityType, TypeElement traitElement) {
    if (entityType.getKind() == TypeKind.DECLARED) {
      DeclaredType declaredType = (DeclaredType) entityType;
      List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
      if (!typeArguments.isEmpty()) {
        throw util.codegenUtil()
            .errorAndThrow(
                "Sql trait entity type must not be a parameterized type (e.g., List<T>, Map<K,V>)",
                traitElement);
      }
    } else if (entityType.getKind() != TypeKind.DECLARED && !isPrimitiveOrWrapper(entityType)) {
      throw util.codegenUtil()
          .errorAndThrow(
              "Sql trait entity type must be a declared type or primitive wrapper", traitElement);
    }
  }

  /**
   * Checks if a type is a primitive or primitive wrapper type.
   *
   * @param type The type to check
   * @return true if primitive or wrapper, false otherwise
   */
  protected boolean isPrimitiveOrWrapper(TypeMirror type) {
    if (type.getKind().isPrimitive()) {
      return true;
    }

    String typeName = type.toString();
    return typeName.equals("java.lang.String")
        || typeName.equals("java.lang.Long")
        || typeName.equals("java.lang.Integer")
        || typeName.equals("java.lang.Double")
        || typeName.equals("java.lang.Float")
        || typeName.equals("java.lang.Boolean")
        || typeName.equals("java.lang.Short")
        || typeName.equals("java.lang.Byte")
        || typeName.equals("java.lang.Character")
        || typeName.equals("java.math.BigDecimal")
        || typeName.equals("java.time.LocalDate")
        || typeName.equals("java.time.LocalDateTime");
  }

  protected void validateTraitElement(
      TypeElement traitElement, TypeMirror returnType, TypeMirror entityType, boolean isQuery) {
    TypeElement inputsElement = findInnerClass(traitElement, "_Inputs");
    if (inputsElement == null) {
      throw util.codegenUtil()
          .errorAndThrow("Sql trait must have a static _Inputs inner class", traitElement);
    }

    // Check for required "parameters" field
    boolean hasParameters = false;
    for (VariableElement field : getFields(inputsElement)) {
      if (field.getSimpleName().toString().equals("parameters")) {
        hasParameters = true;
        boolean isList = isListType(field.asType());
        if (!isList) {
          throw util.codegenUtil()
              .errorAndThrow(
                  "@Sql trait _Inputs must contain a 'parameters' field of type List<Object>",
                  inputsElement);
        } else {
          DeclaredType declaredType = (DeclaredType) field.asType();
          List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
          if (typeArguments.isEmpty()
              || !typeArguments.get(0).toString().equals("java.lang.Object")) {
            throw util.codegenUtil()
                .errorAndThrow(
                    "Sql trait _Inputs must contain a 'parameters' field of type List<Object>",
                    inputsElement);
          }
        }
        break;
      }
    }
    if (!hasParameters) {
      throw util.codegenUtil()
          .errorAndThrow(
              "Sql trait _Inputs must contain a 'parameters' field of type List<Object>",
              inputsElement);
    }
    if (isQuery) {
      // For queries, ensure returnType is List<T> or T
      // check if T is not parameterized type
      if (entityType.getKind() == TypeKind.DECLARED) {
        DeclaredType declaredType = (DeclaredType) entityType;
        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        if (!typeArguments.isEmpty()) {
          throw util.codegenUtil()
              .errorAndThrow(
                  "Sql trait with @SqlQuery must have return type as List<T> or T and T must not be parameterized type",
                  traitElement);
        }
      } else {
        throw util.codegenUtil()
            .errorAndThrow(
                "Sql trait with @SqlQuery must have return type as List<T> or T and T must not be parameterized type",
                traitElement);
      }
    } else {
      // For updates, ensure returnType is Long
      TypeName returnTypeName = TypeName.get(returnType);
      if (!returnTypeName.equals(TypeName.LONG)
          && !returnTypeName.equals(ClassName.get(Long.class))) {
        throw util.codegenUtil()
            .errorAndThrow("Sql trait with @SqlUpdate must have return type as Long", traitElement);
      }
    }
  }
}
