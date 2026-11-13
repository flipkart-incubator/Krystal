package com.flipkart.krystal.keka.rust.resolve;

import com.flipkart.krystal.keka.rust.ast.ModelDef;
import com.flipkart.krystal.keka.rust.ast.TypeRef;
import java.util.HashSet;
import java.util.Set;

/**
 * Whether values of a type can be compared for equality ({@code Eq}) and hashed ({@code Hash}).
 * Floats are neither; sets and maps can be compared but not hashed. An entity is always comparable
 * and hashable, since it uses only its {@code `idField} fields (which are validated to be hashable
 * separately).
 */
public final class ModelTraits {

  private ModelTraits() {}

  public static boolean isEq(SymbolTable table, ModelDef model) {
    return model.isEntity()
        || model.fields().stream().allMatch(f -> isEq(table, f.type(), new HashSet<>()));
  }

  public static boolean isHash(SymbolTable table, ModelDef model) {
    return model.isEntity()
        || model.fields().stream().allMatch(f -> isHash(table, f.type(), new HashSet<>()));
  }

  public static boolean isEq(SymbolTable table, TypeRef type) {
    return isEq(table, type, new HashSet<>());
  }

  public static boolean isHash(SymbolTable table, TypeRef type) {
    return isHash(table, type, new HashSet<>());
  }

  private static boolean isEq(SymbolTable table, TypeRef type, Set<String> visiting) {
    String name = type.name();
    if (isFloat(name)) {
      return false;
    }
    ModelDef model = table.model(name);
    if (model != null) {
      if (model.isEntity()) {
        return true;
      }
      if (!visiting.add(name)) {
        return true;
      }
      boolean result =
          model.fields().stream().allMatch(field -> isEq(table, field.type(), visiting));
      visiting.remove(name);
      return result;
    }
    return type.typeArgs().stream().allMatch(argument -> isEq(table, argument, visiting));
  }

  private static boolean isHash(SymbolTable table, TypeRef type, Set<String> visiting) {
    String name = type.name();
    if (isFloat(name) || name.equals("Set") || name.equals("Map")) {
      return false;
    }
    ModelDef model = table.model(name);
    if (model != null) {
      if (model.isEntity()) {
        return true;
      }
      if (!visiting.add(name)) {
        return true;
      }
      boolean result =
          model.fields().stream().allMatch(field -> isHash(table, field.type(), visiting));
      visiting.remove(name);
      return result;
    }
    return type.typeArgs().stream().allMatch(argument -> isHash(table, argument, visiting));
  }

  private static boolean isFloat(String name) {
    return name.equals("float")
        || name.equals("float32")
        || name.equals("float64")
        || name.equals("double");
  }
}
