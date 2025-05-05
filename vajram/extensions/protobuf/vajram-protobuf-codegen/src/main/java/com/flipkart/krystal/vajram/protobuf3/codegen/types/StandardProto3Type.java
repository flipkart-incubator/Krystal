package com.flipkart.krystal.vajram.protobuf3.codegen.types;

import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.vajram.codegen.common.datatypes.CodeGenType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import com.squareup.javapoet.CodeBlock;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;
import lombok.Getter;

public enum StandardProto3Type implements CodeGenType {
  BYTE_STRING(CodeBlock.of("$T.EMPTY", ByteString.class), ByteString.class.getCanonicalName());

  static final ImmutableMap<String, StandardProto3Type> standardProto3TypesByCanonicalName;

  static {
    Map<String, StandardProto3Type> collector = new LinkedHashMap<>();

    for (StandardProto3Type standardProto3Type : StandardProto3Type.values()) {
      collector.put(standardProto3Type.canonicalClassName(), standardProto3Type);
    }
    standardProto3TypesByCanonicalName = ImmutableMap.copyOf(collector);
  }

  @SuppressWarnings("ImmutableEnumChecker")
  private final CodeBlock defaultValueExpr;

  @Getter private final String canonicalClassName;

  StandardProto3Type(CodeBlock defaultValueExpr, String canonicalClassName) {
    this.defaultValueExpr = defaultValueExpr;
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
  public CodeBlock defaultValueExpr(ProcessingEnvironment processingEnv) {
    return defaultValueExpr;
  }

  @Override
  public ImmutableList<CodeGenType> typeParameters() {
    return ImmutableList.of();
  }
}
