package com.flipkart.krystal.vajram.protobuf.codegen.util.types;

import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.codegen.common.datatypes.CodeGenType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.squareup.javapoet.CodeBlock;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;
import lombok.Getter;

public enum StandardProtoType implements CodeGenType {
  ;

  public static final ImmutableMap<String, StandardProtoType> standardProtoTypesByCanonicalName;

  static {
    Map<String, StandardProtoType> collector = new LinkedHashMap<>();

    for (StandardProtoType standardProtoType : StandardProtoType.values()) {
      collector.put(standardProtoType.canonicalClassName(), standardProtoType);
    }
    standardProtoTypesByCanonicalName = ImmutableMap.copyOf(collector);
  }

  @SuppressWarnings("ImmutableEnumChecker")
  private final CodeBlock defaultValueExpr;

  @Getter private final String canonicalClassName;

  StandardProtoType(CodeBlock defaultValueExpr, String canonicalClassName) {
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
