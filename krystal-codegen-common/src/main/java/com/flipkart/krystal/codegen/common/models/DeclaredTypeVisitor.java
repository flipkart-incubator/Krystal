package com.flipkart.krystal.codegen.common.models;

import com.flipkart.krystal.codegen.common.datatypes.CodeGenType;
import com.flipkart.krystal.codegen.common.datatypes.DataTypeRegistry;
import com.google.common.collect.ImmutableMap;
import javax.lang.model.element.Element;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.AbstractTypeVisitor14;
import org.checkerframework.checker.nullness.qual.Nullable;

public class DeclaredTypeVisitor extends AbstractTypeVisitor14<CodeGenType, Void> {

  private final CodeGenUtility util;
  private final @Nullable Element element;
  private final ImmutableMap<Class<?>, String> disallowedTypes;
  private final DataTypeRegistry dataTypeRegistry;

  public DeclaredTypeVisitor(CodeGenUtility util, @Nullable Element element) {
    this(util, element, ImmutableMap.of());
  }

  public DeclaredTypeVisitor(
      CodeGenUtility util,
      @Nullable Element element,
      ImmutableMap<Class<?>, String> disallowedTypes) {
    this.util = util;
    this.element = element;
    this.disallowedTypes = disallowedTypes;
    this.dataTypeRegistry = util.dataTypeRegistry();
  }

  @Override
  public CodeGenType visitDeclared(DeclaredType t, Void inputDef) {
    String disallowedMessage = util.getDisallowedMessage(t, disallowedTypes);
    if (disallowedMessage != null) {
      util.error(disallowedMessage, element);
    }
    Element elementOfType = t.asElement();
    if (elementOfType instanceof QualifiedNameable qualifiedNameable) {
      return dataTypeRegistry.create(
          util.processingEnv(),
          qualifiedNameable.getQualifiedName().toString(),
          t.getTypeArguments().stream().map(this::visit).toArray(CodeGenType[]::new));
    }
    throw util.errorAndThrow("Could not infer data type for type " + elementOfType, element);
  }

  @Override
  public CodeGenType visitPrimitive(PrimitiveType t, Void unused) {
    PrimitiveType withoutAnnotations =
        util.processingEnv().getTypeUtils().getPrimitiveType(t.getKind());
    return dataTypeRegistry.create(util.processingEnv(), withoutAnnotations.toString());
  }

  @Override
  public CodeGenType visitArray(ArrayType t, Void unused) {
    throw uoe("Array types are not supported by Krystal. Use collections instead.");
  }

  @Override
  public CodeGenType visitTypeVariable(TypeVariable t, Void unused) {
    throw uoe(t);
  }

  @Override
  public CodeGenType visitNull(NullType t, Void unused) {
    throw uoe(t);
  }

  @Override
  public CodeGenType visitIntersection(IntersectionType t, Void unused) {
    throw uoe(t);
  }

  @Override
  public CodeGenType visitError(ErrorType t, Void unused) {
    throw uoe(t);
  }

  @Override
  public CodeGenType visitWildcard(WildcardType t, Void unused) {
    throw uoe(t);
  }

  @Override
  public CodeGenType visitExecutable(ExecutableType t, Void unused) {
    throw uoe(t);
  }

  @Override
  public CodeGenType visitNoType(NoType t, Void unused) {
    throw uoe(t);
  }

  @Override
  public CodeGenType visitUnion(UnionType t, Void unused) {
    throw uoe(t);
  }

  private RuntimeException uoe(TypeMirror type) {
    if (TypeKind.ERROR.equals(type.getKind())) {
      return new CodeGenShortCircuitException(
          "Element : "
              + element
              + " has encountered a type of ERROR kind: "
              + type
              + ". Shortcuircuiting code gen. This codegen may be retried in the next round and will most possibly succeed.");
    } else {
      return uoe(type + " not supported by DeclaredTypeVisitor");
    }
  }

  private CodeValidationException uoe(String message) {
    return new CodeValidationException(message);
  }
}
