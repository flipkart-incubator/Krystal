package com.flipkart.krystal.vajram.lang.rust.resolve;

import com.flipkart.krystal.vajram.lang.rust.ast.Completion;
import com.flipkart.krystal.vajram.lang.rust.ast.ComputedFacet;
import com.flipkart.krystal.vajram.lang.rust.ast.Dependency;
import com.flipkart.krystal.vajram.lang.rust.ast.DependencyInvocation;
import com.flipkart.krystal.vajram.lang.rust.ast.OutputBlock;
import com.flipkart.krystal.vajram.lang.rust.ast.VajramFile;
import com.flipkart.krystal.vajram.lang.rust.diag.Diagnostics;
import com.flipkart.krystal.vajram.lang.rust.system.SystemVajram;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Cross-file registry of every Vajram in a compilation, keyed by name. Makes multi-file compilation
 * possible: a {@code .vajram} file can depend on a Vajram defined in a different file, and {@code
 * package_decl}/{@code imports_decl} on their own don't tell us which file that is - we need to
 * have seen every file in the source root first.
 */
public final class SymbolTable {

  private final Map<String, VajramFile> filesByVajramName = new HashMap<>();

  public static SymbolTable build(List<VajramFile> files, Diagnostics diagnostics) {
    SymbolTable table = new SymbolTable();
    for (VajramFile file : files) {
      String name = file.vajram().name();
      VajramFile existing = table.filesByVajramName.putIfAbsent(name, file);
      if (existing != null) {
        diagnostics.error(
            file.vajram().location(),
            "Duplicate vajram name '" + name + "' also defined in " + existing.sourcePath());
      }
    }
    return table;
  }

  public Optional<VajramFile> resolve(String vajramName) {
    return Optional.ofNullable(filesByVajramName.get(vajramName));
  }

  public @Nullable VajramFile lookup(String vajramName) {
    return filesByVajramName.get(vajramName);
  }

  public Completion completionOf(VajramFile file) {
    return completionOf(file, new java.util.HashSet<>());
  }

  private Completion completionOf(VajramFile file, java.util.Set<String> resolving) {
    if (!resolving.add(file.vajram().name())) {
      return Completion.NOW;
    }
    Completion completion = declaredCompletion(file);
    for (ComputedFacet facet : file.vajram().computedFacets()) {
      if (facet instanceof Dependency dependency) {
        completion =
            max(completion, completionOfInvocation(file, dependency.invocation(), resolving));
      }
    }
    if (file.vajram().outputBlock() instanceof OutputBlock.Delegate delegate) {
      completion = max(completion, completionOfInvocation(file, delegate.invocation(), resolving));
    }
    resolving.remove(file.vajram().name());
    return completion;
  }

  private Completion declaredCompletion(VajramFile file) {
    if (file.vajram().outputBlock() instanceof OutputBlock.Logic logic) {
      return logic.later() ? Completion.LATER : logic.soon() ? Completion.SOON : Completion.NOW;
    }
    return Completion.NOW;
  }

  private Completion completionOfInvocation(
      VajramFile caller, DependencyInvocation invocation, java.util.Set<String> resolving) {
    VajramFile callee = lookup(invocation.vajramName());
    Completion completion;
    if (callee != null) {
      completion = completionOf(callee, resolving);
    } else {
      completion =
          SystemVajram.lookup(invocation.vajramName(), caller.imports())
              .map(SystemVajram::completion)
              .orElse(Completion.NOW);
    }
    return invocation.fanout() ? max(completion, Completion.SOON) : completion;
  }

  private static Completion max(Completion first, Completion second) {
    return first.ordinal() >= second.ordinal() ? first : second;
  }
}
