package com.flipkart.krystal.vajram.lang.rust.resolve;

import com.flipkart.krystal.vajram.lang.rust.ast.Callers;
import com.flipkart.krystal.vajram.lang.rust.ast.ComputedFacet;
import com.flipkart.krystal.vajram.lang.rust.ast.Dependency;
import com.flipkart.krystal.vajram.lang.rust.ast.DependencyInvocation;
import com.flipkart.krystal.vajram.lang.rust.ast.OutputBlock;
import com.flipkart.krystal.vajram.lang.rust.ast.VajramDef;
import com.flipkart.krystal.vajram.lang.rust.ast.VajramFile;
import com.flipkart.krystal.vajram.lang.rust.diag.Diagnostics;
import com.flipkart.krystal.vajram.lang.rust.system.SystemVajram;
import java.util.List;

/**
 * Validates that every dependency invocation refers to a Vajram that actually exists in the
 * compilation, and honors named {@code callers} declarations.
 */
public final class Resolver {

  private Resolver() {}

  public static void validate(List<VajramFile> files, SymbolTable table, Diagnostics diagnostics) {
    for (VajramFile file : files) {
      VajramDef vajram = file.vajram();
      for (ComputedFacet facet : vajram.computedFacets()) {
        if (facet instanceof Dependency dependency) {
          checkInvocation(file, dependency.invocation(), table, diagnostics);
        }
      }
      if (vajram.outputBlock() instanceof OutputBlock.Delegate delegate) {
        checkInvocation(file, delegate.invocation(), table, diagnostics);
      }
    }
  }

  private static void checkInvocation(
      VajramFile caller,
      DependencyInvocation invocation,
      SymbolTable table,
      Diagnostics diagnostics) {
    VajramFile callee = table.lookup(invocation.vajramName());
    if (callee == null) {
      var systemVajram = SystemVajram.lookup(invocation.vajramName(), caller.imports());
      if (systemVajram.isPresent()) {
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
