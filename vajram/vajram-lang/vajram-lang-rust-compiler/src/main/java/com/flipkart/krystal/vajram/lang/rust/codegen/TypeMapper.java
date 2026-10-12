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
      Map.ofEntries(
          Map.entry("string", "String"),
          Map.entry("bool", "bool"),
          Map.entry("void", "()"),
          Map.entry("int", "i64"),
          Map.entry("int32", "i32"),
          Map.entry("int64", "i64"),
          Map.entry("int128", "i128"),
          Map.entry("uint32", "u32"),
          Map.entry("uint64", "u64"),
          Map.entry("uint128", "u128"),
          Map.entry("float", "f32"),
          Map.entry("float32", "f32"),
          Map.entry("float64", "f64"),
          Map.entry("double", "f64"));

  private static final Map<String, String> COLLECTIONS =
      Map.of(
          "Set", "std::collections::HashSet",
          "List", "Vec",
          "Map", "std::collections::HashMap");

  private static final Map<String, String> RUNTIME_TYPES =
      Map.of("ConsoleWriter", "dyn crate::vajram_rt::ConsoleWriter");

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
   * Every value that crosses a generated Vajram boundary (by default) is borrowed out of some
   * arena: a reference tied to the given {@code lifetime}, not a refcounted handle. Callers pass
   * {@code "a"} for the callee's own arena/output lifetime, or {@code "i"} for the (independent)
   * lifetime of the {@code Inputs} struct's fields - keeping the two decoupled is what lets a
   * short-lived scratch arena back a {@code `local} facet's inputs without also shrinking the
   * arena/output lifetime of the call that consumes them. See {@link #toRustLocalType(TypeRef)} for
   * the {@code `local} facet case, which is a plain owned value instead.
   */
  public static String toRustOwnedType(TypeRef type, String lifetime) {
    return "&'" + lifetime + " " + toRustValueType(type);
  }

  /**
   * A {@code `local} facet's value never crosses this Vajram's own boundary, so it is just a plain
   * owned Rust value - freed by ordinary scope/drop rules when {@code call_one} returns, no arena
   * or reference involved.
   */
  public static String toRustLocalType(TypeRef type) {
    return toRustValueType(type);
  }

  public static String toRustReturnType(TypeRef type) {
    TypeRef valueType =
        new TypeRef(type.name(), type.typeArgs(), type.grouperType(), false, type.soon());
    String ownedValue = toRustOwnedType(valueType, "a");
    return type.errable() ? "Result<" + ownedValue + ", VajramError>" : ownedValue;
  }

  public static boolean isPrimitive(String name) {
    return PRIMITIVES.containsKey(name);
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
    if (collection != null) {
      return collection;
    }
    String runtimeType = RUNTIME_TYPES.get(type.name());
    return runtimeType != null ? runtimeType : type.name();
  }

  private static String capitalize(String s) {
    return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }
}
