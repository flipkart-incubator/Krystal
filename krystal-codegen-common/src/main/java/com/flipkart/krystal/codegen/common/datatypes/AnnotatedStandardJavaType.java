package com.flipkart.krystal.codegen.common.datatypes;

import static com.flipkart.krystal.codegen.common.datatypes.StandardJavaType.standardTypesByCanonicalName;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.CodeBlock;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;

public record AnnotatedStandardJavaType(StandardJavaType standardJavaType, TypeMirror typeMirror)
    implements CodeGenType {

  public static @Nullable CodeGenType from(
      TypeMirror typeMirror,
      List<CodeGenType> typeParameters,
      ProcessingEnvironment processingEnvironment) {
    if (typeParameters.isEmpty()) {
      String typeString = null;
      if (typeMirror instanceof PrimitiveType primitiveType) {
        typeString = primitiveType.toString();
      } else if (processingEnvironment.getTypeUtils().asElement(typeMirror)
          instanceof QualifiedNameable qualifiedNameable) {
        typeString = qualifiedNameable.getQualifiedName().toString();
      }
      if (typeString != null) {
        StandardJavaType standardType = standardTypesByCanonicalName.get(typeString);
        if (standardType != null) {
          List<? extends AnnotationMirror> annotationMirrors = typeMirror.getAnnotationMirrors();
          if (annotationMirrors.isEmpty()) {
            return standardType;
          } else {
            return new AnnotatedStandardJavaType(standardType, typeMirror);
          }
        }
      }
    }
    return null;
  }

  @Override
  public String canonicalClassName() {
    return standardJavaType.canonicalClassName();
  }

  @Override
  public ImmutableList<CodeGenType> typeParameters() {
    return standardJavaType.typeParameters();
  }

  @Override
  public TypeMirror typeMirror(ProcessingEnvironment processingEnv) {
    return typeMirror;
  }

  @Override
  public CodeBlock defaultValueExpr(ProcessingEnvironment processingEnv) {
    return standardJavaType.defaultValueExpr();
  }
}
