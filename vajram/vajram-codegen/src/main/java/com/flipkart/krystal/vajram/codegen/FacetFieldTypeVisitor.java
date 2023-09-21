package com.flipkart.krystal.vajram.codegen;

import static com.flipkart.krystal.datatypes.TypeUtils.getDataType;

import com.flipkart.krystal.datatypes.DataType;
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
import javax.tools.Diagnostic.Kind;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;

class FacetFieldTypeVisitor extends AbstractTypeVisitor14<DataType<?>, Void> {
  private final ProcessingEnvironment processingEnv;
  private final boolean root;
  private final Element element;

  @NotOnlyInitialized private final FacetFieldTypeVisitor subVisitor;

  FacetFieldTypeVisitor(ProcessingEnvironment processingEnv, boolean root, Element element) {
    this.processingEnv = processingEnv;
    this.root = root;
    this.element = element;
    if (root) {
      this.subVisitor = new FacetFieldTypeVisitor(processingEnv, false, element);
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
        processingEnv
            .getMessager()
            .printMessage(Kind.ERROR, "Optional<Optional<..>> is not supported", element);
      }
      if (root) {
        return subVisitor.visit(optionalOf);
      }
    }
    return getDataType(
        t.asElement().toString(), t.getTypeArguments().stream().map(subVisitor::visit).toList());
  }

  @Override
  public DataType<?> visitPrimitive(PrimitiveType t, Void unused) {
    return getDataType(t.toString(), List.of());
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
