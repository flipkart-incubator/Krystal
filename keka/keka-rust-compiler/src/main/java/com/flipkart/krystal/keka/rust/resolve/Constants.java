package com.flipkart.krystal.keka.rust.resolve;

import com.flipkart.krystal.keka.rust.ast.Expr;
import com.flipkart.krystal.keka.rust.ast.SourceLocation;
import com.flipkart.krystal.keka.rust.ast.TypeRef;
import com.flipkart.krystal.keka.rust.diag.Diagnostics;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Compile-time constants: evaluates constant expressions (annotation arguments, parameter and field
 * defaults) and checks them against a declared type (a primitive, {@code string}, or a list of
 * those).
 */
final class Constants {

  static final Set<String> INTEGRAL =
      Set.of("int", "int32", "int64", "int128", "uint32", "uint64", "uint128");
  static final Set<String> FLOATING = Set.of("float", "float32", "float64", "double");
  static final Set<String> PRIMITIVES = new HashSet<>();

  static {
    PRIMITIVES.addAll(INTEGRAL);
    PRIMITIVES.addAll(FLOATING);
    PRIMITIVES.add("string");
    PRIMITIVES.add("bool");
  }

  private Constants() {}

  static boolean isSupportedType(TypeRef type) {
    if (type.name().equals("List")) {
      return type.typeArgs().size() == 1 && isSupportedType(type.typeArgs().get(0));
    }
    return type.typeArgs().isEmpty() && PRIMITIVES.contains(type.name());
  }

  static String describe(TypeRef type) {
    return type.name().equals("List") && type.typeArgs().size() == 1
        ? "[" + describe(type.typeArgs().get(0)) + "]"
        : type.name();
  }

  static void checkValue(
      Expr expression,
      TypeRef type,
      String what,
      SourceLocation location,
      Diagnostics diagnostics) {
    Optional<Constant> value = evaluate(expression, what, location, diagnostics);
    if (value.isPresent() && !conforms(value.get(), type)) {
      diagnostics.error(location, what + " must be a constant of type '" + describe(type) + "'");
    }
  }

  sealed interface Constant {
    record Text(String value) implements Constant {}

    record Whole(BigInteger value) implements Constant {}

    record Flag(boolean value) implements Constant {}

    record Items(List<Constant> values) implements Constant {}

    /** A bare name such as an enum constant; no declarable type accepts one yet. */
    record Name(String value) implements Constant {}
  }

  static Optional<Constant> evaluate(
      Expr expression, String what, SourceLocation location, Diagnostics diagnostics) {
    if (expression instanceof Expr.StringLiteral literal) {
      String text = literal.javaText();
      return Optional.of(new Constant.Text(text.substring(1, text.length() - 1)));
    }
    if (expression instanceof Expr.IntLiteral literal) {
      return Optional.of(new Constant.Whole(new BigInteger(literal.text())));
    }
    if (expression instanceof Expr.BoolLiteral literal) {
      return Optional.of(new Constant.Flag(literal.value()));
    }
    if (expression instanceof Expr.VarUse use) {
      return Optional.of(new Constant.Name(use.name()));
    }
    if (expression instanceof Expr.Array array) {
      List<Constant> items = new ArrayList<>();
      for (Expr element : array.elements()) {
        Optional<Constant> item = evaluate(element, what, location, diagnostics);
        if (item.isEmpty()) {
          return Optional.empty();
        }
        items.add(item.get());
      }
      return Optional.of(new Constant.Items(items));
    }
    if (expression instanceof Expr.BinaryOp binary) {
      Optional<Constant> left = evaluate(binary.left(), what, location, diagnostics);
      Optional<Constant> right = evaluate(binary.right(), what, location, diagnostics);
      if (left.isEmpty() || right.isEmpty()) {
        return Optional.empty();
      }
      boolean plus = binary.operator().equals("+");
      if (plus && left.get() instanceof Constant.Text a && right.get() instanceof Constant.Text b) {
        return Optional.of(new Constant.Text(a.value() + b.value()));
      }
      if (left.get() instanceof Constant.Whole a && right.get() instanceof Constant.Whole b) {
        return Optional.of(
            new Constant.Whole(plus ? a.value().add(b.value()) : a.value().multiply(b.value())));
      }
      diagnostics.error(
          location, what + " applies '" + binary.operator() + "' to operands it does not support");
      return Optional.empty();
    }
    diagnostics.error(location, what + " is not a compile-time constant");
    return Optional.empty();
  }

  static boolean conforms(Constant value, TypeRef type) {
    if (type.name().equals("List")) {
      return value instanceof Constant.Items items
          && items.values().stream().allMatch(item -> conforms(item, type.typeArgs().get(0)));
    }
    String name = type.name();
    if (name.equals("string")) {
      return value instanceof Constant.Text;
    }
    if (name.equals("bool")) {
      return value instanceof Constant.Flag;
    }
    if (value instanceof Constant.Whole whole) {
      return FLOATING.contains(name) || (INTEGRAL.contains(name) && inRange(whole.value(), name));
    }
    return false;
  }

  static boolean inRange(BigInteger value, String type) {
    int bits =
        switch (type) {
          case "int32" -> 31;
          case "uint32" -> 32;
          case "int", "int64" -> 63;
          case "uint64" -> 64;
          case "int128" -> 127;
          default -> 128;
        };
    return value.signum() >= 0 && value.bitLength() <= bits;
  }
}
