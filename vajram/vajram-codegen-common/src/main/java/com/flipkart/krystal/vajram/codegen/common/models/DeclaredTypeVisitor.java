package com.flipkart.krystal.vajram.codegen.common.models;

import static com.flipkart.krystal.vajram.codegen.common.models.Utils.getDisallowedMessage;

import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.datatypes.JavaType;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
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

class DeclaredTypeVisitor<T> extends AbstractTypeVisitor14<DataType<T>, Void> {

  private final ProcessingEnvironment processingEnv;
  private final Utils util;
  private final Element element;

  DeclaredTypeVisitor(Utils util, Element element) {
    this.util = util;
    this.processingEnv = util.processingEnv();
    this.element = element;
  }

  @Override
  public DataType<T> visitDeclared(DeclaredType t, Void inputDef) {
    String disallowedMessage = getDisallowedMessage(t, processingEnv);
    if (disallowedMessage != null) {
      util.error(disallowedMessage, element);
    }
    return JavaType.create(
        t.asElement().toString(), t.getTypeArguments().stream().map(this::visit).toList());
  }

  @Override
  public DataType<T> visitPrimitive(PrimitiveType t, Void unused) {
    return JavaType.create(t.toString(), List.of());
  }

  @Override
  public DataType<T> visitArray(ArrayType t, Void unused) {
    throw uoe();
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

  private static UnsupportedOperationException uoe() {
    return new UnsupportedOperationException();
  }
}
