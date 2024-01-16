package com.flipkart.krystal.vajram.codegen;

import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.datatypes.JavaType;
import java.util.List;
import java.util.Optional;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.AbstractTypeVisitor14;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;

class DeclaredTypeVisitor extends AbstractTypeVisitor14<DataType<?>, Void> {
  private final ProcessingEnvironment processingEnv;
  private final Utils util;
  private final boolean ignoreFirstOptional;
  private final Element element;

  @NotOnlyInitialized private final DeclaredTypeVisitor subVisitor;

  DeclaredTypeVisitor(Utils util, boolean ignoreFirstOptional, Element element) {
    this.util = util;
    this.processingEnv = util.getProcessingEnv();
    this.ignoreFirstOptional = ignoreFirstOptional;
    this.element = element;
    if (ignoreFirstOptional) {
      this.subVisitor = new DeclaredTypeVisitor(util, false, element);
    } else {
      this.subVisitor = this;
    }
  }

  @Override
  public DataType<?> visitDeclared(DeclaredType t, Void inputDef) {
    boolean optional = isOptional(t, processingEnv);
    if (optional) {
      TypeMirror optionalOf = t.getTypeArguments().get(0);
      if (isOptional(optionalOf, processingEnv)) {
        util.error("Optional<Optional<..>> is not supported", element);
      }
      if (ignoreFirstOptional) {
        return subVisitor.visit(optionalOf);
      }
    }
    return JavaType.create(
        t.asElement().toString(), t.getTypeArguments().stream().map(subVisitor::visit).toList());
  }

  @Override
  public DataType<?> visitPrimitive(PrimitiveType t, Void unused) {
    return JavaType.create(t.toString(), List.of());
  }

  @Override
  public DataType<?> visitArray(ArrayType t, Void unused) {
    throw uoe();
  }

  @Override
  public DataType<?> visitTypeVariable(TypeVariable t, Void unused) {
    throw uoe();
  }

  @Override
  public DataType<?> visitNull(NullType t, Void unused) {
    throw uoe();
  }

  @Override
  public DataType<?> visitIntersection(IntersectionType t, Void unused) {
    throw uoe();
  }

  @Override
  public DataType<?> visitError(ErrorType t, Void unused) {
    throw uoe();
  }

  @Override
  public DataType<?> visitWildcard(WildcardType t, Void unused) {
    throw uoe();
  }

  @Override
  public DataType<?> visitExecutable(ExecutableType t, Void unused) {
    throw uoe();
  }

  @Override
  public DataType<?> visitNoType(NoType t, Void unused) {
    throw uoe();
  }

  @Override
  public DataType<?> visitUnion(UnionType t, Void unused) {
    throw uoe();
  }

  static boolean isOptional(TypeMirror t, ProcessingEnvironment processingEnv) {
    Element e = processingEnv.getTypeUtils().asElement(t);
    if (e == null) {
      return false;
    }
    Name name =
        (e instanceof QualifiedNameable qualifiedNameable)
            ? qualifiedNameable.getQualifiedName()
            : e.getSimpleName();
    return name.contentEquals(Optional.class.getName());
  }

  private static UnsupportedOperationException uoe() {
    return new UnsupportedOperationException();
  }
}
