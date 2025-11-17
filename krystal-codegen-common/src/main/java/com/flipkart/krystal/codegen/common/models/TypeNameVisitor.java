package com.flipkart.krystal.codegen.common.models;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
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

/**
 * An enhanced version of the type visitor inside {@link TypeName#get(TypeMirror)} method. This
 * visitor makes sure that any type annotations encountered during visitation are transferred to the
 * returned {@link TypeName}s. (This feature is currently missing in javapoet. See <a
 * href="https://github.com/square/javapoet/issues/685">issue</a>)
 */
public class TypeNameVisitor extends AbstractTypeVisitor14<TypeName, Void> {

  private final boolean boxTypes;

  public TypeNameVisitor() {
    this(false);
  }

  public TypeNameVisitor(boolean boxTypes) {
    this.boxTypes = boxTypes;
  }

  @Override
  public TypeName visitDeclared(DeclaredType t, Void inputDef) {
    ClassName rawType = addTypeAnnotations(t, ClassName.get((TypeElement) t.asElement()));
    TypeMirror enclosingType = t.getEnclosingType();
    TypeName enclosing =
        (enclosingType.getKind() != TypeKind.NONE)
                && !t.asElement().getModifiers().contains(Modifier.STATIC)
            ? this.visit(enclosingType)
            : null;
    if (t.getTypeArguments().isEmpty() && !(enclosing instanceof ParameterizedTypeName)) {
      return rawType;
    }

    List<TypeName> typeArgumentNames = new ArrayList<>();
    for (TypeMirror mirror : t.getTypeArguments()) {
      typeArgumentNames.add(this.visit(mirror));
    }
    return enclosing instanceof ParameterizedTypeName
        ? addTypeAnnotations(
            t,
            ((ParameterizedTypeName) enclosing)
                .nestedClass(rawType.simpleName(), typeArgumentNames))
        : ParameterizedTypeName.get(rawType, typeArgumentNames.toArray(TypeName[]::new));
  }

  @Override
  public TypeName visitPrimitive(PrimitiveType t, Void unused) {
    return addTypeAnnotations(t, TypeName.get(t));
  }

  @Override
  public TypeName visitArray(ArrayType t, Void unused) {
    return addTypeAnnotations(t, ArrayTypeName.of(this.visit(t.getComponentType())));
  }

  @Override
  public TypeName visitTypeVariable(TypeVariable t, Void unused) {
    Element element = t.asElement();
    if (element instanceof TypeParameterElement typeParameterElement) {
      return addTypeAnnotations(t, TypeVariableName.get(typeParameterElement));
    } else {
      return addTypeAnnotations(t, TypeName.get(t));
    }
  }

  @Override
  public TypeName visitNull(NullType t, Void unused) {
    return addTypeAnnotations(t, TypeName.get(t));
  }

  @Override
  public TypeName visitIntersection(IntersectionType t, Void unused) {
    // Not yet supported by javapoet
    return addTypeAnnotations(t, TypeName.get(t));
  }

  @Override
  public TypeName visitError(ErrorType t, Void unused) {
    return visitDeclared(t, unused);
  }

  @Override
  public TypeName visitWildcard(WildcardType t, Void unused) {
    TypeMirror superBound = t.getSuperBound();
    TypeMirror extendsBound = t.getExtendsBound();
    WildcardTypeName result;
    if (superBound != null) {
      result = WildcardTypeName.supertypeOf(this.visit(superBound));
    } else if (extendsBound != null) {
      result = WildcardTypeName.subtypeOf(this.visit(extendsBound));
    } else {
      result = WildcardTypeName.subtypeOf(Object.class);
    }
    return addTypeAnnotations(t, result);
  }

  @Override
  public TypeName visitExecutable(ExecutableType t, Void unused) {
    // Not supported by JavaPoet
    return addTypeAnnotations(t, TypeName.get(t));
  }

  @Override
  public TypeName visitNoType(NoType t, Void unused) {
    return addTypeAnnotations(t, TypeName.get(t));
  }

  @Override
  public TypeName visitUnion(UnionType t, Void unused) {
    // Not supported by JavaPoet
    return addTypeAnnotations(t, TypeName.get(t));
  }

  private ClassName addTypeAnnotations(TypeMirror from, ClassName to) {
    return (ClassName) addTypeAnnotations(from, (TypeName) to);
  }

  private TypeName addTypeAnnotations(TypeMirror from, TypeName to) {
    List<AnnotationSpec> annotations =
        from.getAnnotationMirrors().stream().map(AnnotationSpec::get).toList();
    to = boxTypes ? to.box() : to;
    if (annotations.isEmpty()) {
      return to;
    } else {
      return to.annotated(annotations);
    }
  }
}
