package com.flipkart.krystal.vajram.codegen.common.datatypes;

import static com.flipkart.krystal.vajram.codegen.common.datatypes.CodeGenTypeUtils.box;
import static com.flipkart.krystal.vajram.codegen.common.datatypes.StandardJavaType.standardTypesByCanonicalName;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.CodeBlock;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
public final class JavaCodeGenType implements CodeGenType {

  static JavaCodeGenType create(
      String canonicalClassName, ImmutableList<CodeGenType> typeParameters) {
    return new JavaCodeGenType(canonicalClassName, typeParameters);
  }

  @Getter private final String canonicalClassName;
  @Getter private final ImmutableList<CodeGenType> typeParameters;

  private JavaCodeGenType(String canonicalClassName, ImmutableList<CodeGenType> typeParameters) {
    this.canonicalClassName = canonicalClassName;
    this.typeParameters = typeParameters;
  }

  @Override
  public TypeMirror javaModelType(ProcessingEnvironment processingEnv) {
    TypeElement typeElement = processingEnv.getElementUtils().getTypeElement(canonicalClassName);
    if (typeElement == null) {
      throw new IllegalArgumentException(
          "Could not find typeElement for canonical class name '%s'".formatted(canonicalClassName));
    }
    return processingEnv
        .getTypeUtils()
        .getDeclaredType(
            typeElement,
            typeParameters.stream()
                .map(t -> box(t.javaModelType(processingEnv), processingEnv))
                .toArray(TypeMirror[]::new));
  }

  @SuppressWarnings("unchecked")
  @Override
  public CodeGenType rawType() {
    return new JavaCodeGenType(canonicalClassName, ImmutableList.of());
  }

  @Override
  public CodeBlock defaultValueExpr(ProcessingEnvironment processingEnv)
      throws IllegalArgumentException {
    StandardJavaType standardTypeInfo = standardTypesByCanonicalName.get(canonicalClassName);
    if (standardTypeInfo != null) {
      return standardTypeInfo.defaultValueExpr(processingEnv);
    }
    throw new IllegalArgumentException("No default value for non standard type %s".formatted(this));
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
