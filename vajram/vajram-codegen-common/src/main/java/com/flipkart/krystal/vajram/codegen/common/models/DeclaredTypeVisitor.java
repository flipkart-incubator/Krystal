package com.flipkart.krystal.vajram.codegen.common.models;

import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.datatypes.JavaType;
import com.google.common.collect.ImmutableMap;
import javax.annotation.processing.ProcessingEnvironment;
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
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.AbstractTypeVisitor14;

public class DeclaredTypeVisitor<T> extends AbstractTypeVisitor14<DataType<T>, Void> {

  private final Utils util;
  private final Element element;
  private final ImmutableMap<Class<?>, String> disallowedTypes;

  public DeclaredTypeVisitor(Utils util, Element element) {
    this(util, element, ImmutableMap.of());
  }

  public DeclaredTypeVisitor(
      Utils util, Element element, ImmutableMap<Class<?>, String> disallowedTypes) {
    this.util = util;
    this.element = element;
    this.disallowedTypes = disallowedTypes;
  }

  @Override
  public DataType<T> visitDeclared(DeclaredType t, Void inputDef) {
    String disallowedMessage = util.getDisallowedMessage(t, disallowedTypes);
    if (disallowedMessage != null) {
      util.error(disallowedMessage, element);
    }
    Element elementOfType = t.asElement();
    if (elementOfType instanceof QualifiedNameable qualifiedNameable) {
      return JavaType.create(
          qualifiedNameable.getQualifiedName().toString(),
          t.getTypeArguments().stream().map(this::visit).toArray(DataType<?>[]::new));
    }
    throw util.errorAndThrow("Could not infer data type for type " + elementOfType, element);
  }

  @Override
  public DataType<T> visitPrimitive(PrimitiveType t, Void unused) {
    PrimitiveType withoutAnnotations =
        util.processingEnv().getTypeUtils().getPrimitiveType(t.getKind());
    return JavaType.create(withoutAnnotations.toString());
  }

  @Override
  public DataType<T> visitArray(ArrayType t, Void unused) {
    throw uoe("Array types are not supported by Krystal. Use collections instead.");
  }

  @Override
  public DataType<T> visitTypeVariable(TypeVariable t, Void unused) {
    throw uoe();
  }

  @Override
  public DataType<T> visitNull(NullType t, Void unused) {
    throw uoe();
  }

  @Override
  public DataType<T> visitIntersection(IntersectionType t, Void unused) {
    throw uoe();
  }

  @Override
  public DataType<T> visitError(ErrorType t, Void unused) {
    throw uoe();
  }

  @Override
  public DataType<T> visitWildcard(WildcardType t, Void unused) {
    throw uoe();
  }

  @Override
  public DataType<T> visitExecutable(ExecutableType t, Void unused) {
    throw uoe();
  }

  @Override
  public DataType<T> visitNoType(NoType t, Void unused) {
    throw uoe();
  }

  @Override
  public DataType<T> visitUnion(UnionType t, Void unused) {
    throw uoe();
  }

  private static UnsupportedOperationException uoe(String message) {
    return new UnsupportedOperationException(message);
  }

  private static UnsupportedOperationException uoe() {
    return new UnsupportedOperationException();
  }
}
