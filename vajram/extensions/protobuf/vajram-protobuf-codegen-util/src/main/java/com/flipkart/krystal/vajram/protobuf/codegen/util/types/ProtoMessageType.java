package com.flipkart.krystal.vajram.protobuf.codegen.util.types;

import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.codegen.common.datatypes.CodeGenType;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.CodeBlock;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/** A {@link CodeGenType} for a protobuf message type referenced by canonical class name. */
@EqualsAndHashCode
@ToString
public final class ProtoMessageType implements CodeGenType {

  public static ProtoMessageType create(String canonicalClassName) {
    return new ProtoMessageType(canonicalClassName);
  }

  @Getter private final String canonicalClassName;

  private ProtoMessageType(String canonicalClassName) {
    this.canonicalClassName = canonicalClassName;
  }

  @Override
  public TypeMirror typeMirror(ProcessingEnvironment processingEnv) {
    return requireNonNull(processingEnv.getElementUtils().getTypeElement(canonicalClassName))
        .asType();
  }

  @Override
  public CodeBlock defaultValueExpr(ProcessingEnvironment processingEnv)
      throws IllegalArgumentException {
    return CodeBlock.of("$T.getDefaultInstance()", typeMirror(processingEnv));
  }

  @Override
  public ImmutableList<CodeGenType> typeParameters() {
    return ImmutableList.of();
  }
}
