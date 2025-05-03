package com.flipkart.krystal.vajram.protobuf3.codegen.types;

import com.flipkart.krystal.vajram.codegen.common.datatypes.CodeGenDataType;
import com.google.protobuf.MessageLiteOrBuilder;
import com.squareup.javapoet.CodeBlock;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public final class Proto3DataType implements CodeGenDataType {

  static Proto3DataType create(ProcessingEnvironment processingEnv, String canonicalClassName) {
    return new Proto3DataType(processingEnv, canonicalClassName);
  }

  private final ProcessingEnvironment processingEnv;

  @Getter private final String canonicalClassName;

  private Proto3DataType(ProcessingEnvironment processingEnv, String canonicalClassName) {
    this.processingEnv = processingEnv;
    this.canonicalClassName = canonicalClassName;
  }

  @Override
  public TypeMirror javaModelType() {
    return processingEnv.getElementUtils().getTypeElement(canonicalClassName).asType();
  }

  @Override
  public CodeGenDataType rawType() {
    return this;
  }

  @Override
  public CodeBlock defaultValueExpr() throws IllegalArgumentException {
    return CodeBlock.of("$T.getDefaultInstance()", javaModelType());
  }
}
