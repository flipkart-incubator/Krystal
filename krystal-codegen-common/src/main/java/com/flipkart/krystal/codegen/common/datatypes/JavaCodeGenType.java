package com.flipkart.krystal.codegen.common.datatypes;

import static com.flipkart.krystal.codegen.common.datatypes.StandardJavaType.standardTypesByCanonicalName;

import com.flipkart.krystal.codegen.common.models.CodeGenerationException;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.CodeBlock;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
public final class JavaCodeGenType implements CodeGenType {

  static JavaCodeGenType create(
      TypeMirror typeMirror,
      List<CodeGenType> typeParameters,
      ProcessingEnvironment processingEnvironment) {
    return new JavaCodeGenType(typeMirror, typeParameters, processingEnvironment);
  }

  @Getter private final String canonicalClassName;
  @Getter private final ImmutableList<CodeGenType> typeParameters;
  @Getter private final TypeMirror typeMirror;

  private JavaCodeGenType(
      TypeMirror typeMirror,
      List<CodeGenType> typeParameters,
      ProcessingEnvironment processingEnvironment) {
    Element element = processingEnvironment.getTypeUtils().asElement(typeMirror);
    if (!(element instanceof TypeElement typeElement)) {
      throw new IllegalArgumentException(
          "JavaCodeGenType supports only declared types, but got '%s' (kind=%s, element=%s)"
              .formatted(typeMirror, typeMirror.getKind(), element));
    }
    this.canonicalClassName = typeElement.getQualifiedName().toString();
    this.typeMirror = typeMirror;
    this.typeParameters = ImmutableList.copyOf(typeParameters);
  }

  @Override
  public TypeMirror typeMirror(ProcessingEnvironment processingEnv) {
    return typeMirror;
  }

  @Override
  public CodeBlock defaultValueExpr(ProcessingEnvironment processingEnv)
      throws CodeGenerationException {
    if (isEnum(processingEnv)) {
      Element element = processingEnv.getTypeUtils().asElement(typeMirror);
      List<VariableElement> fields = ElementFilter.fieldsIn(element.getEnclosedElements());
      for (VariableElement field : fields) {
        // Filter specifically for ENUM_CONSTANT
        if (field.getKind() == ElementKind.ENUM_CONSTANT) {
          return CodeBlock.of("$T.$L", typeMirror, field.getSimpleName());
        }
      }
      throw new CodeGenerationException(
          "No default value for enum type since enum is empty - at least one value is needed to default to '%s'"
              .formatted(this));
    }

    StandardJavaType standardTypeInfo = standardTypesByCanonicalName.get(canonicalClassName);
    if (standardTypeInfo != null) {
      CodeBlock defaultValueExpr = standardTypeInfo.defaultValueExpr(processingEnv);
      if (defaultValueExpr != null) {
        return defaultValueExpr;
      }
    }
    throw new CodeGenerationException(
        "No default value expression available for type '%s'".formatted(this));
  }

  private boolean isEnum(ProcessingEnvironment processingEnv) {
    Element element = processingEnv.getTypeUtils().asElement(typeMirror);
    if (element == null) {
      return false;
    }
    return ElementKind.ENUM.equals(element.getKind());
  }

  @Override
  public String toString() {
    return canonicalClassName()
        + (typeParameters.isEmpty()
            ? ""
            : "<"
                + typeParameters.stream().map(Objects::toString).collect(Collectors.joining(", "))
                + ">");
  }
}
