package com.flipkart.krystal.vajram.protobuf3.codegen.types;

import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.vajram.codegen.common.datatypes.CodeGenType;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.CodeBlock;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public final class Proto3MessageType implements CodeGenType {

  static Proto3MessageType create(String canonicalClassName) {
    return new Proto3MessageType(canonicalClassName);
  }

  @Getter private final String canonicalClassName;

  private Proto3MessageType(String canonicalClassName) {
    this.canonicalClassName = canonicalClassName;
  }

  @Override
  public TypeMirror javaModelType(ProcessingEnvironment processingEnv) {
    return requireNonNull(processingEnv.getElementUtils().getTypeElement(canonicalClassName))
        .asType();
  }

  @Override
  public CodeGenType rawType() {
    return this;
  }

  @Override
  public CodeBlock defaultValueExpr(ProcessingEnvironment processingEnv)
      throws IllegalArgumentException {
    return CodeBlock.of("$T.getDefaultInstance()", javaModelType(processingEnv));
  }

  @Override
  public ImmutableList<CodeGenType> typeParameters() {
    return ImmutableList.of();
  }
}
