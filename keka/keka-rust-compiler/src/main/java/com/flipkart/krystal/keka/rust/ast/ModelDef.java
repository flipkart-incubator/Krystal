package com.flipkart.krystal.keka.rust.ast;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Grammar rule {@code model_def}. A model with {@code `idField} fields is an <i>entity</i>: its
 * equality and hash use only those fields. A model without any is a <i>value</i>, compared field by
 * field.
 */
public record ModelDef(
    String name,
    List<VajramAnnotation> annotations,
    List<FieldDecl> fields,
    SourceLocation location) {

  /** The built-in annotation that marks a field as part of an entity's identity. */
  public static final String ID_FIELD = "idField";

  /**
   * One field. {@code type} may be errable ({@code T?}: a value, an error or nil); {@code
   * defaultValue} is a constant expression, or null when the field has no default.
   */
  public record FieldDecl(
      List<VajramAnnotation> annotations,
      TypeRef type,
      String name,
      @Nullable Expr defaultValue,
      SourceLocation location) {

    public boolean isIdField() {
      return VajramAnnotation.has(annotations, ID_FIELD);
    }
  }

  public List<FieldDecl> idFields() {
    return fields.stream().filter(FieldDecl::isIdField).toList();
  }

  public boolean isEntity() {
    return fields.stream().anyMatch(FieldDecl::isIdField);
  }
}
