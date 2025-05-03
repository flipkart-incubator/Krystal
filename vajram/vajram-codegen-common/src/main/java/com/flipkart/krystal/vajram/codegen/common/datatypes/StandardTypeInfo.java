package com.flipkart.krystal.vajram.codegen.common.datatypes;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.CodeBlock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import lombok.Getter;

@Getter
enum StandardTypeInfo {
  BOOLEAN(
      TypeKind.BOOLEAN,
      CodeBlock.of("true"),
      boolean.class.getCanonicalName(),
      Boolean.class.getCanonicalName()),
  INT(
      TypeKind.INT,
      CodeBlock.of("0"),
      int.class.getCanonicalName(),
      Integer.class.getCanonicalName()),
  BYTE(
      TypeKind.BYTE,
      CodeBlock.of("0"),
      byte.class.getCanonicalName(),
      Byte.class.getCanonicalName()),
  SHORT(
      TypeKind.SHORT,
      CodeBlock.of("0"),
      short.class.getCanonicalName(),
      Short.class.getCanonicalName()),
  LONG(
      TypeKind.LONG,
      CodeBlock.of("0L"),
      long.class.getCanonicalName(),
      Long.class.getCanonicalName()),
  CHAR(
      TypeKind.CHAR,
      CodeBlock.of("0"),
      char.class.getCanonicalName(),
      Character.class.getCanonicalName()),
  FLOAT(
      TypeKind.FLOAT,
      CodeBlock.of("0.0f"),
      float.class.getCanonicalName(),
      Float.class.getCanonicalName()),
  DOUBLE(
      TypeKind.DOUBLE,
      CodeBlock.of("0.0d"),
      double.class.getCanonicalName(),
      Double.class.getCanonicalName()),
  VOID(
      TypeKind.VOID,
      CodeBlock.of("null"),
      processingEnv -> processingEnv.getTypeUtils().getNoType(TypeKind.VOID),
      void.class.getCanonicalName(),
      Void.class.getCanonicalName()),
  LIST(TypeKind.DECLARED, CodeBlock.of("$T.of()", List.class), List.class.getCanonicalName()),
  MAP(TypeKind.DECLARED, CodeBlock.of("$T.of()", Map.class), Map.class.getCanonicalName()),
  ;

  private final TypeKind typeKind;
  private final CodeBlock defaultValueExpr;
  private final Function<ProcessingEnvironment, TypeMirror> javaModelType;
  private final ImmutableList<String> canonicalClassNames;

  StandardTypeInfo(
      TypeKind typeKind,
      CodeBlock defaultValueExpr,
      String canonicalClassName,
      String... canonicalClassNames) {
    this(
        typeKind,
        defaultValueExpr,
        processingEnv -> {
          if (typeKind.isPrimitive()) {
            return processingEnv.getTypeUtils().getPrimitiveType(typeKind);
          }
          return processingEnv.getElementUtils().getTypeElement(canonicalClassName).asType();
        },
        canonicalClassName,
        canonicalClassNames);
  }

  StandardTypeInfo(
      TypeKind typeKind,
      CodeBlock defaultValueExpr,
      Function<ProcessingEnvironment, TypeMirror> javaModelType,
      String canonicalClassName,
      String... canonicalClassNames) {
    this.typeKind = typeKind;
    this.defaultValueExpr = defaultValueExpr;
    this.javaModelType = javaModelType;
    this.canonicalClassNames =
        ImmutableList.<String>builder().add(canonicalClassName).add(canonicalClassNames).build();
  }

  public TypeMirror javaModelType(ProcessingEnvironment processingEnv) {
    return javaModelType.apply(processingEnv);
  }
}
