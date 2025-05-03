package com.flipkart.krystal.vajram.codegen.common.datatypes;

import static com.flipkart.krystal.vajram.codegen.common.datatypes.CodeGenTypeUtils.box;
import static com.flipkart.krystal.vajram.codegen.common.datatypes.CodeGenTypeUtils.nativeTypeInfos;

import com.flipkart.krystal.datatypes.JavaType;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.CodeBlock;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.NonNull;

public record CodeGenJavaType(
    String canonicalClassName, ImmutableList<CodeGenDataType> typeParameters)
    implements CodeGenDataType {

  @Override
  public TypeMirror javaModelType(ProcessingEnvironment processingEnv) {
    StandardTypeInfo standardTypeInfo = nativeTypeInfos.get(canonicalClassName);
    if (standardTypeInfo != null) {
      return standardTypeInfo.javaModelType(processingEnv);
    }
    TypeElement typeElement = processingEnv.getElementUtils().getTypeElement(canonicalClassName);
    if (typeElement == null) {
      throw new IllegalArgumentException(
          "Could not find typeElement for canonical class name %s".formatted(canonicalClassName));
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
  public CodeGenDataType rawType() {
    return new CodeGenJavaType(canonicalClassName, ImmutableList.of());
  }

  @Override
  public CodeBlock defaultValueExpr() throws IllegalArgumentException {
    StandardTypeInfo standardTypeInfo = nativeTypeInfos.get(canonicalClassName);
    if (standardTypeInfo != null) {
      return standardTypeInfo.defaultValueExpr();
    }
    throw new IllegalArgumentException("No default value for non standard type %s".formatted(this));
  }

  @Override
  public String toString() {
    return canonicalClassName()
        + (typeParameters.isEmpty()
            ? ""
            : "< "
                + typeParameters.stream().map(Objects::toString).collect(Collectors.joining(", "))
                + ">");
  }
}
