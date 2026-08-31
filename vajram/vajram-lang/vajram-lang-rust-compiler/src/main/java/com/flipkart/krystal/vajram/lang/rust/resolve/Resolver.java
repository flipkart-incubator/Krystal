package com.flipkart.krystal.vajram.lang.rust.resolve;

import com.flipkart.krystal.vajram.lang.rust.ast.Callers;
import com.flipkart.krystal.vajram.lang.rust.ast.ComputedFacet;
import com.flipkart.krystal.vajram.lang.rust.ast.DepInputResolver;
import com.flipkart.krystal.vajram.lang.rust.ast.Dependency;
import com.flipkart.krystal.vajram.lang.rust.ast.DependencyInvocation;
import com.flipkart.krystal.vajram.lang.rust.ast.Expr;
import com.flipkart.krystal.vajram.lang.rust.ast.Field;
import com.flipkart.krystal.vajram.lang.rust.ast.OutputBlock;
import com.flipkart.krystal.vajram.lang.rust.ast.VajramDef;
import com.flipkart.krystal.vajram.lang.rust.ast.VajramFile;
import com.flipkart.krystal.vajram.lang.rust.cli.RustCompilerMain.Target;
import com.flipkart.krystal.vajram.lang.rust.diag.Diagnostics;
import com.flipkart.krystal.vajram.lang.rust.system.SystemVajram;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Validates that every dependency invocation refers to a Vajram that actually exists in the
 * compilation, and honors named {@code callers} declarations.
 */
public final class Resolver {

  private Resolver() {}

  public static void validate(List<VajramFile> files, SymbolTable table, Diagnostics diagnostics) {
    validate(files, table, diagnostics, Target.NATIVE);
  }

  public static void validate(
      List<VajramFile> files, SymbolTable table, Diagnostics diagnostics, Target target) {
    for (VajramFile file : files) {
      VajramDef vajram = file.vajram();
      Map<String, Boolean> facetFanout = new HashMap<>();
      for (ComputedFacet facet : vajram.computedFacets()) {
        if (facet instanceof Field field) {
          checkFieldFanout(field, facetFanout, diagnostics);
          facetFanout.put(field.name(), field.fanout());
        } else if (facet instanceof Dependency dependency) {
          checkInvocation(file, dependency.invocation(), table, diagnostics, target);
          checkDependencyFanout(dependency, diagnostics);
          facetFanout.put(dependency.name(), dependency.fanout());
        }
      }
      if (vajram.outputBlock() instanceof OutputBlock.Delegate delegate) {
        checkInvocation(file, delegate.invocation(), table, diagnostics, target);
      }
    }
  }

  private static void checkFieldFanout(
      Field field, Map<String, Boolean> facetFanout, Diagnostics diagnostics) {
    if (!field.fanout()) {
      return;
    }
    if (field.value() instanceof Expr.Array) {
      return;
    }
    if (field.value() instanceof Expr.VarUse varUse
        && Boolean.TRUE.equals(facetFanout.get(varUse.name()))) {
      return;
    }
    diagnostics.error(
        field.location(),
        "Fanout field '" + field.name() + "' requires an array or another fanout facet value");
  }

  private static void checkDependencyFanout(Dependency dependency, Diagnostics diagnostics) {
    boolean invocationFanout =
        dependency.invocation().fanout()
            || dependency.invocation().resolvers().stream().anyMatch(DepInputResolver::fanout);
    if (dependency.fanout() != invocationFanout) {
      diagnostics.error(
          dependency.location(),
          "Fanout dependency '"
              + dependency.name()
              + "' must have matching declaration and invocation/resolver cardinality");
    }
  }

  private static void checkInvocation(
      VajramFile caller,
      DependencyInvocation invocation,
      SymbolTable table,
      Diagnostics diagnostics,
      Target target) {
    VajramFile callee = table.lookup(invocation.vajramName());
    if (callee == null) {
      var systemVajram = SystemVajram.lookup(invocation.vajramName(), caller.imports());
      if (systemVajram.isPresent()) {
        if (!systemVajram.get().supports(target)) {
          diagnostics.error(
              invocation.location(),
              "System Vajram '"
                  + invocation.vajramName()
                  + "' is not supported for the wasm target: browser file-picker support is not bundled");
        }
        return;
      }
      diagnostics.error(
          invocation.location(),
          "Vajram '" + invocation.vajramName() + "' is not defined in this compilation");
      return;
    }
    Callers callers = callee.vajram().callers();
    if (callers instanceof Callers.Named named
        && named.callers().stream()
            .noneMatch(entry -> entry.name().equals(caller.vajram().name()))) {
      diagnostics.error(
          invocation.location(),
          "Vajram '"
              + invocation.vajramName()
              + "' does not permit being depended on by '"
              + caller.vajram().name()
              + "' (callers: "
              + named.callers().stream().map(Callers.Caller::name).toList()
              + ")");
    }
  }
}
