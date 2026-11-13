package com.flipkart.krystal.keka.rust.codegen;

import com.flipkart.krystal.keka.rust.ast.Expr;
import com.flipkart.krystal.keka.rust.ast.ModelDef;
import com.flipkart.krystal.keka.rust.ast.TypeRef;
import com.flipkart.krystal.keka.rust.resolve.ModelTraits;
import com.flipkart.krystal.keka.rust.resolve.SymbolTable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Emits Rust for models. A value model derives its comparison traits. An entity compares and hashes
 * by its {@code `idField} fields only. Field names keep their Keka spelling (with {@code
 * non_snake_case} allowed), so a member access in Keka is a plain transliteration.
 */
final class ModelEmitter {

  private ModelEmitter() {}

  static void emitModel(RustWriter w, ModelDef model, SymbolTable table) {
    boolean entity = model.isEntity();
    List<String> derives = new ArrayList<>(List.of("Debug", "Clone"));
    if (!entity) {
      derives.add("PartialEq");
      if (ModelTraits.isEq(table, model)) {
        derives.add("Eq");
      }
      if (ModelTraits.isHash(table, model)) {
        derives.add("Hash");
      }
    }
    w.line("#[derive(" + String.join(", ", derives) + ")]");
    w.line("#[allow(non_snake_case)]");
    w.openBlock("pub struct " + model.name());
    for (ModelDef.FieldDecl field : model.fields()) {
      w.line("pub " + field.name() + ": " + TypeMapper.toRustValueType(field.type()) + ",");
    }
    w.closeBlock();
    if (entity) {
      emitEntityIdentity(w, model);
    }
    w.blank();
  }

  private static void emitEntityIdentity(RustWriter w, ModelDef model) {
    List<ModelDef.FieldDecl> ids = model.idFields();
    String equal =
        ids.stream()
            .map(id -> "self." + id.name() + " == other." + id.name())
            .collect(Collectors.joining(" && "));
    w.openBlock("impl PartialEq for " + model.name());
    w.openBlock("fn eq(&self, other: &Self) -> bool");
    w.line(equal);
    w.closeBlock();
    w.closeBlock();
    w.line("impl Eq for " + model.name() + " {}");
    w.openBlock("impl std::hash::Hash for " + model.name());
    w.openBlock("fn hash<H: std::hash::Hasher>(&self, state: &mut H)");
    for (ModelDef.FieldDecl id : ids) {
      w.line("self." + id.name() + ".hash(state);");
    }
    w.closeBlock();
    w.closeBlock();
  }

  /** Rust for a constant default of a (non-errable) field type. */
  static String emitConstant(Expr expr, TypeRef type) {
    if (expr instanceof Expr.StringLiteral literal) {
      return literal.javaText() + ".to_string()";
    }
    if (expr instanceof Expr.IntLiteral literal) {
      return isFloat(type.name())
          ? "(" + literal.text() + " as " + TypeMapper.toRustValueType(type) + ")"
          : literal.text();
    }
    if (expr instanceof Expr.BoolLiteral literal) {
      return Boolean.toString(literal.value());
    }
    if (expr instanceof Expr.Array array) {
      TypeRef element = type.typeArgs().isEmpty() ? type : type.typeArgs().get(0);
      return "vec!["
          + array.elements().stream()
              .map(item -> emitConstant(item, element))
              .collect(Collectors.joining(", "))
          + "]";
    }
    if (expr instanceof Expr.BinaryOp binary) {
      String left = emitConstant(binary.left(), type);
      String right = emitConstant(binary.right(), type);
      if (binary.operator().equals("+") && type.name().equals("string")) {
        return "(" + left + " + &" + right + ")";
      }
      return "(" + left + " " + binary.operator() + " " + right + ")";
    }
    throw new IllegalStateException("Unhandled constant: " + expr.getClass());
  }

  private static boolean isFloat(String name) {
    return name.equals("float")
        || name.equals("float32")
        || name.equals("float64")
        || name.equals("double");
  }
}
