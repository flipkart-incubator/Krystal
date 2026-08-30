package com.flipkart.krystal.vajram.lang.rust.codegen;

import com.flipkart.krystal.vajram.lang.rust.ast.TypeRef;
import java.util.Map;

/**
 * Maps vajram-lang types to Rust types. {@code T?} (errable) becomes {@code Result<T, VajramError>}
 * since Rust's {@code Result} + native {@code ?} operator is the idiomatic analog of vajram-lang's
 * errable operator. {@code T~} (soon) is handled at the call-site/signature level (as an {@code
 * async fn} / {@code .await}), not as a wrapper type, since Rust futures aren't nameable the way a
 * struct field type needs to be.
 */
public final class TypeMapper {

  private static final Map<String, String> PRIMITIVES =
      Map.of(
          "string", "String",
          "int", "i64",
          "float", "f32",
          "double", "f64",
          "bool", "bool",
          "void", "()");

  private static final Map<String, String> COLLECTIONS =
      Map.of(
          "Set", "std::collections::HashSet",
          "List", "Vec",
          "Map", "std::collections::HashMap");

  private TypeMapper() {}

  public static String toRustValueType(TypeRef type) {
    String base = baseName(type);
    String withGenerics =
        type.typeArgs().isEmpty()
            ? base
            : base
                + "<"
                + type.typeArgs().stream()
                    .map(TypeMapper::toRustValueType)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("")
                + ">";
    return type.errable() ? "Result<" + withGenerics + ", VajramError>" : withGenerics;
  }

  /**
   * Every value that crosses a generated Vajram boundary uses shared, local-task-safe ownership.
   */
  public static String toRustOwnedType(TypeRef type) {
    return "Rc<" + toRustValueType(type) + ">";
  }

  public static String toRustReturnType(TypeRef type) {
    TypeRef valueType =
        new TypeRef(type.name(), type.typeArgs(), type.grouperType(), false, type.soon());
    String ownedValue = toRustOwnedType(valueType);
    return type.errable() ? "Result<" + ownedValue + ", VajramError>" : ownedValue;
  }

  private static String baseName(TypeRef type) {
    if (type.grouperType()) {
      // `#mod`/`#batch` used as a type refers to the facet-group key type; the compiler doesn't
      // synthesize it, it just names it consistently with the value-position mapping below.
      return capitalize(type.name()) + "Key";
    }
    String primitive = PRIMITIVES.get(type.name());
    if (primitive != null) {
      return primitive;
    }
    String collection = COLLECTIONS.get(type.name());
    return collection != null ? collection : type.name();
  }

  private static String capitalize(String s) {
    return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }
}
