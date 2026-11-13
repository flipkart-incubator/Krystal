package com.flipkart.krystal.keka.rust.resolve;

import com.flipkart.krystal.keka.rust.ast.Expr;
import com.flipkart.krystal.keka.rust.ast.ModelDef;
import com.flipkart.krystal.keka.rust.ast.ModelFile;
import com.flipkart.krystal.keka.rust.ast.SourceLocation;
import com.flipkart.krystal.keka.rust.ast.TypeRef;
import com.flipkart.krystal.keka.rust.ast.VajramFile;
import com.flipkart.krystal.keka.rust.codegen.TypeMapper;
import com.flipkart.krystal.keka.rust.diag.Diagnostics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Checks model declarations (field names and types, containment, defaults, identity fields) and the
 * use of {@code new Model(...)}.
 */
final class ModelValidator {

  private ModelValidator() {}

  static void validate(List<VajramFile> files, SymbolTable table, Diagnostics diagnostics) {
    for (ModelFile file : table.models()) {
      validateModel(file.model(), table, diagnostics);
    }
    checkContainmentCycles(table, diagnostics);
    for (VajramFile file : files) {
      SourceLocation location = file.vajram().location();
      ExprWalker.walk(file.vajram(), expr -> checkConstruction(expr, table, location, diagnostics));
    }
  }

  private static void validateModel(ModelDef model, SymbolTable table, Diagnostics diagnostics) {
    Set<String> names = new HashSet<>();
    for (ModelDef.FieldDecl field : model.fields()) {
      if (!names.add(field.name())) {
        diagnostics.error(
            field.location(),
            "Model '" + model.name() + "' declares field '" + field.name() + "' more than once");
      }
      boolean typeOk = checkType(model, field, field.type(), table, diagnostics);
      if (typeOk && field.defaultValue() != null) {
        checkDefault(model, field, diagnostics);
      }
      if (typeOk && field.isIdField()) {
        checkIdField(model, field, table, diagnostics);
      }
    }
  }

  /** Reports problems with a field's type; returns whether the type is usable. */
  private static boolean checkType(
      ModelDef model,
      ModelDef.FieldDecl field,
      TypeRef type,
      SymbolTable table,
      Diagnostics diagnostics) {
    String where = "field '" + field.name() + "' of model '" + model.name() + "'";
    if (type.soon() || type.later()) {
      diagnostics.error(
          field.location(),
          "The type of " + where + " cannot be soon or later ('~'): it holds data");
      return false;
    }
    if (type.grouperType() || type.name().contains("#")) {
      diagnostics.error(field.location(), "The type of " + where + " cannot be a generated type");
      return false;
    }
    int expectedArguments = expectedTypeArguments(type.name());
    if (type.typeArgs().size() != expectedArguments) {
      diagnostics.error(
          field.location(),
          "Type '"
              + type.name()
              + "' of "
              + where
              + (expectedArguments == 0
                  ? " takes no type arguments"
                  : " takes " + expectedArguments + " type argument(s)"));
      return false;
    }
    if (!isKnownType(type.name(), table)) {
      diagnostics.error(field.location(), "Unknown type '" + type.name() + "' for " + where);
      return false;
    }
    boolean ok = true;
    for (TypeRef argument : type.typeArgs()) {
      ok &= checkType(model, field, argument, table, diagnostics);
    }
    if (ok && type.name().equals("Map") && !ModelTraits.isHash(table, type.typeArgs().get(0))) {
      diagnostics.error(
          field.location(),
          "The key type of " + where + " must be hashable (not a float, set or map)");
      return false;
    }
    if (ok && type.name().equals("Set") && !ModelTraits.isHash(table, type.typeArgs().get(0))) {
      diagnostics.error(
          field.location(),
          "The element type of " + where + " must be hashable (not a float, set or map)");
      return false;
    }
    return ok;
  }

  private static int expectedTypeArguments(String name) {
    return switch (name) {
      case "List", "Set" -> 1;
      case "Map" -> 2;
      default -> 0;
    };
  }

  private static boolean isKnownType(String name, SymbolTable table) {
    return (TypeMapper.isPrimitive(name) && !name.equals("void"))
        || expectedTypeArguments(name) > 0
        || table.model(name) != null;
  }

  private static void checkDefault(
      ModelDef model, ModelDef.FieldDecl field, Diagnostics diagnostics) {
    TypeRef plain =
        new TypeRef(field.type().name(), field.type().typeArgs(), false, false, false, false);
    String what = "default of field '" + field.name() + "' of model '" + model.name() + "'";
    if (!Constants.isSupportedType(plain)) {
      diagnostics.error(
          field.location(),
          "The "
              + what
              + " is not supported: defaults are limited to primitives, 'string' and"
              + " lists of those");
      return;
    }
    Constants.checkValue(field.defaultValue(), plain, what, field.location(), diagnostics);
  }

  private static void checkIdField(
      ModelDef model, ModelDef.FieldDecl field, SymbolTable table, Diagnostics diagnostics) {
    if (field.type().errable()) {
      diagnostics.error(
          field.location(),
          "Id field '"
              + field.name()
              + "' of model '"
              + model.name()
              + "' cannot be errable ('?')");
    } else if (!ModelTraits.isHash(table, field.type())) {
      diagnostics.error(
          field.location(),
          "Id field '"
              + field.name()
              + "' of model '"
              + model.name()
              + "' must be hashable: floats, sets, maps and models containing them cannot be ids");
    }
  }

  /**
   * A model cannot contain itself through plain or errable fields (it would have infinite size); a
   * {@code List}, {@code Set} or {@code Map} breaks the cycle.
   */
  private static void checkContainmentCycles(SymbolTable table, Diagnostics diagnostics) {
    Map<String, List<String>> contains = new HashMap<>();
    for (ModelFile file : table.models()) {
      List<String> targets = new ArrayList<>();
      for (ModelDef.FieldDecl field : file.model().fields()) {
        if (table.model(field.type().name()) != null) {
          targets.add(field.type().name());
        }
      }
      contains.put(file.model().name(), targets);
    }
    Set<String> done = new HashSet<>();
    for (ModelFile file : table.models()) {
      detectCycle(file.model(), contains, new ArrayList<>(), done, table, diagnostics);
    }
  }

  private static void detectCycle(
      ModelDef model,
      Map<String, List<String>> contains,
      List<String> path,
      Set<String> done,
      SymbolTable table,
      Diagnostics diagnostics) {
    if (done.contains(model.name())) {
      return;
    }
    int at = path.indexOf(model.name());
    if (at >= 0) {
      List<String> cycle = new ArrayList<>(path.subList(at, path.size()));
      cycle.add(model.name());
      diagnostics.error(
          model.location(),
          "Model '"
              + model.name()
              + "' contains itself ("
              + String.join(" -> ", cycle)
              + "); break the cycle with a List, Set or Map");
      done.addAll(cycle);
      return;
    }
    path.add(model.name());
    for (String next : contains.getOrDefault(model.name(), List.of())) {
      ModelDef nextModel = table.model(next);
      if (nextModel != null) {
        detectCycle(nextModel, contains, path, done, table, diagnostics);
      }
    }
    path.remove(path.size() - 1);
    done.add(model.name());
  }

  private static void checkConstruction(
      Expr expr, SymbolTable table, SourceLocation location, Diagnostics diagnostics) {
    if (expr instanceof Expr.NewModel newModel) {
      ModelDef model = table.model(newModel.name());
      if (model == null) {
        diagnostics.error(location, "Unknown model '" + newModel.name() + "' in 'new'");
        return;
      }
      Set<String> given = new HashSet<>();
      for (Expr.NewModel.Argument argument : newModel.arguments()) {
        if (model.fields().stream().noneMatch(field -> field.name().equals(argument.name()))) {
          diagnostics.error(
              location, "Model '" + model.name() + "' has no field '" + argument.name() + "'");
        } else if (!given.add(argument.name())) {
          diagnostics.error(
              location,
              "Field '" + argument.name() + "' of model '" + model.name() + "' is set twice");
        }
      }
      checkRequiredFields(model, given, location, diagnostics);
    } else if (expr instanceof Expr.Call call && call.isNew() && !call.isSpecial()) {
      ModelDef model = table.model(call.name());
      if (model == null) {
        return;
      }
      if (!call.args().isEmpty() || call.lambda() != null) {
        diagnostics.error(
            location,
            "Create model '"
                + model.name()
                + "' with named fields: new "
                + model.name()
                + "(field = value; ...)");
        return;
      }
      checkRequiredFields(model, Set.of(), location, diagnostics);
    }
  }

  private static void checkRequiredFields(
      ModelDef model, Set<String> given, SourceLocation location, Diagnostics diagnostics) {
    for (ModelDef.FieldDecl field : model.fields()) {
      if (!given.contains(field.name())
          && field.defaultValue() == null
          && !field.type().errable()) {
        diagnostics.error(
            location,
            "Field '"
                + field.name()
                + "' of model '"
                + model.name()
                + "' has no default and must be set (an errable 'T?' field defaults to nil)");
      }
    }
  }
}
