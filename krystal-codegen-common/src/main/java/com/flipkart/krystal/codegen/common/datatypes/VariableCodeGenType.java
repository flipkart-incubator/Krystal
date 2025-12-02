package com.flipkart.krystal.codegen.common.datatypes;

import com.flipkart.krystal.codegen.common.models.CodeGenerationException;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.CodeBlock;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import lombok.Getter;

public final class VariableCodeGenType implements CodeGenType {

  private final TypeVariable typeVariable;
  @Getter private final CodeGenType upperBound;

  public VariableCodeGenType(TypeVariable typeVariable, CodeGenType upperBound) {
    this.typeVariable = typeVariable;
    this.upperBound = upperBound;
  }

  @Override
  public String canonicalClassName() {
    return upperBound.canonicalClassName();
  }

  @Override
  public ImmutableList<CodeGenType> typeParameters() {
    return upperBound.typeParameters();
  }

  @Override
  public TypeMirror javaModelType(ProcessingEnvironment processingEnv) {
    return typeVariable;
  }

  @Override
  public CodeGenType rawType() {
    return upperBound.rawType();
  }

  @Override
  public CodeBlock defaultValueExpr(ProcessingEnvironment processingEnv)
      throws CodeGenerationException {
    try {
      return upperBound.defaultValueExpr(processingEnv);
    } catch (CodeGenerationException exception) {
      throw new CodeGenerationException(
          "No default value for type variable '%s'".formatted(typeVariable));
    }
  }
}
