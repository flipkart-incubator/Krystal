package com.flipkart.krystal.keka.rust.ast;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Grammar rule {@code annotation_def}: a custom annotation, e.g. {@code `type table(string name,
 * bool indexed = false)}, with the meta-annotations written before it (e.g. {@code `target}).
 */
public record AnnotationDef(
    String name,
    List<VajramAnnotation> metaAnnotations,
    List<Parameter> parameters,
    SourceLocation location) {

  /**
   * One parameter. {@code type} is a primitive or {@code string}, or {@code List<...>} of those;
   * {@code defaultValue} is a constant expression, or null when the parameter is required.
   */
  public record Parameter(
      String name, TypeRef type, @Nullable Expr defaultValue, SourceLocation location) {}
}
