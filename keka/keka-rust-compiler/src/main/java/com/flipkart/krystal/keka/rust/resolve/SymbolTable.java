package com.flipkart.krystal.keka.rust.resolve;

import com.flipkart.krystal.keka.rust.ast.AnnotationDef;
import com.flipkart.krystal.keka.rust.ast.AnnotationDefFile;
import com.flipkart.krystal.keka.rust.ast.Completion;
import com.flipkart.krystal.keka.rust.ast.ComputedFacet;
import com.flipkart.krystal.keka.rust.ast.Dependency;
import com.flipkart.krystal.keka.rust.ast.DependencyInvocation;
import com.flipkart.krystal.keka.rust.ast.Expr;
import com.flipkart.krystal.keka.rust.ast.ModelDef;
import com.flipkart.krystal.keka.rust.ast.ModelFile;
import com.flipkart.krystal.keka.rust.ast.OutputBlock;
import com.flipkart.krystal.keka.rust.ast.SourceLocation;
import com.flipkart.krystal.keka.rust.ast.TypeRef;
import com.flipkart.krystal.keka.rust.ast.VajramAnnotation;
import com.flipkart.krystal.keka.rust.ast.VajramFile;
import com.flipkart.krystal.keka.rust.diag.Diagnostics;
import com.flipkart.krystal.keka.rust.system.SystemVajram;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Cross-file registry of every Vajram in a compilation, keyed by name. Makes multi-file compilation
 * possible: a {@code .keka} file can depend on a Vajram defined in a different file, and {@code
 * package_decl}/{@code imports_decl} on their own don't tell us which file that is - we need to
 * have seen every file in the source root first.
 */
public final class SymbolTable {

  private final Map<String, VajramFile> filesByVajramName = new HashMap<>();
  private final Map<String, AnnotationDefFile> annotationDefsByName = new HashMap<>();
  private final Map<String, ModelFile> modelsByName = new java.util.LinkedHashMap<>();

  /** Annotations the language itself defines; they cannot be redefined by user code. */
  private static final Map<String, AnnotationDef> BUILT_IN_ANNOTATIONS =
      Map.of(
          ModelDef.ID_FIELD,
          new AnnotationDef(
              ModelDef.ID_FIELD,
              List.of(
                  new VajramAnnotation(
                      AnnotationValidator.TARGET_META_ANNOTATION,
                      List.of(
                          new VajramAnnotation.Argument(
                              "kinds", new Expr.Array(List.of(new Expr.VarUse("FIELD", false))))))),
              List.of(),
              new SourceLocation("<built-in>", 1, 1)));

  public static SymbolTable build(List<VajramFile> files, Diagnostics diagnostics) {
    return build(files, List.of(), diagnostics);
  }

  public static SymbolTable build(
      List<VajramFile> files, List<AnnotationDefFile> annotationDefs, Diagnostics diagnostics) {
    return build(files, annotationDefs, List.of(), diagnostics);
  }

  public static SymbolTable build(
      List<VajramFile> files,
      List<AnnotationDefFile> annotationDefs,
      List<ModelFile> models,
      Diagnostics diagnostics) {
    SymbolTable table = new SymbolTable();
    for (ModelFile file : models) {
      String name = file.model().name();
      ModelFile existing = table.modelsByName.putIfAbsent(name, file);
      if (existing != null) {
        diagnostics.error(
            file.model().location(),
            "Duplicate model name '" + name + "' also defined in " + existing.sourcePath());
      }
    }
    for (AnnotationDefFile file : annotationDefs) {
      String name = file.definition().name();
      AnnotationDefFile existing = table.annotationDefsByName.putIfAbsent(name, file);
      if (BUILT_IN_ANNOTATIONS.containsKey(name)) {
        diagnostics.error(
            file.definition().location(),
            "Annotation name '" + name + "' is built into the language and cannot be redefined");
      } else if (existing != null) {
        diagnostics.error(
            file.definition().location(),
            "Duplicate annotation name '" + name + "' also defined in " + existing.sourcePath());
      }
    }
    for (VajramFile file : files) {
      String name = file.vajram().name();
      if (table.modelsByName.containsKey(name)) {
        diagnostics.error(
            file.vajram().location(),
            "Vajram name '" + name + "' clashes with the model of the same name");
      }
      VajramFile existing = table.filesByVajramName.putIfAbsent(name, file);
      if (existing != null) {
        diagnostics.error(
            file.vajram().location(),
            "Duplicate vajram name '" + name + "' also defined in " + existing.sourcePath());
      }
    }
    return table;
  }

  /** The custom annotation definition with this name, or null if there is none. */
  public @Nullable AnnotationDef annotationDefinition(String name) {
    AnnotationDef builtIn = BUILT_IN_ANNOTATIONS.get(name);
    if (builtIn != null) {
      return builtIn;
    }
    AnnotationDefFile file = annotationDefsByName.get(name);
    return file == null ? null : file.definition();
  }

  /** The model with this name, or null if there is none. */
  public @Nullable ModelDef model(String name) {
    ModelFile file = modelsByName.get(name);
    return file == null ? null : file.model();
  }

  public @Nullable ModelFile modelFile(String name) {
    return modelsByName.get(name);
  }

  /** Every model, in declaration order across files. */
  public List<ModelFile> models() {
    return new ArrayList<>(modelsByName.values());
  }

  public List<AnnotationDefFile> annotationDefinitions() {
    return new ArrayList<>(annotationDefsByName.values());
  }

  public Optional<VajramFile> resolve(String vajramName) {
    return Optional.ofNullable(filesByVajramName.get(vajramName));
  }

  public @Nullable VajramFile lookup(String vajramName) {
    return filesByVajramName.get(vajramName);
  }

  /**
   * A Vajram's completion is authoritatively its own declared output type: {@code out `now T} is
   * {@link Completion#NOW}, {@code out T~} and plain {@code out T} are {@link Completion#SOON},
   * {@code out T~~} is {@link Completion#LATER}. It is deliberately *not* inferred from the
   * Vajram's body - a caller deciding whether to {@code .await} a dependency needs a simple,
   * non-recursive answer, and Keka never blocks a thread to reconcile an actually-async body with a
   * sync-declared signature. {@link #inferredCompletion} validates the body actually honors this
   * contract.
   */
  public Completion completionOf(VajramFile file) {
    TypeRef output = file.vajram().outputType();
    if (output.later()) {
      return Completion.LATER;
    }
    boolean now = VajramAnnotation.has(file.vajram().outputAnnotations(), "now");
    return now ? Completion.NOW : Completion.SOON;
  }

  /**
   * Whether a Vajram yields one {@code Result} per input in its batch: it declared {@code out T?},
   * or it completes {@code ~}/{@code ~~} (async work, e.g. IO, can always fail - so every async
   * Vajram is errable whether or not it says {@code ?}). A {@code `now} Vajram without {@code ?}
   * can't fail (see {@link Resolver}).
   */
  public boolean isErrable(VajramFile file) {
    return file.vajram().outputType().errable() || completionOf(file).isAsync();
  }

  /** {@link #isErrable} for the callee named in {@code caller}'s invocation (incl. system ones). */
  public boolean isErrableCallee(VajramFile caller, String vajramName) {
    VajramFile callee = lookup(vajramName);
    if (callee != null) {
      return isErrable(callee);
    }
    return SystemVajram.lookup(vajramName, caller.imports())
        .map(system -> system.completion().isAsync())
        .orElse(false);
  }

  /**
   * The completion actually required by a Vajram's body: its output block's own {@code ~}/{@code
   * ~~} marker, or any dependency/delegate call to a Vajram (or system Vajram) whose own completion
   * is async. Used only to validate (see {@link Resolver}) that a Vajram declaring a sync ({@code
   * out T}) type doesn't secretly require async execution - that mismatch is a compile error, not
   * something silently promoted.
   */
  public Completion inferredCompletion(VajramFile file) {
    Completion completion = Completion.NOW;
    if (file.vajram().outputBlock() instanceof OutputBlock.Logic logic) {
      completion =
          logic.later() ? Completion.LATER : logic.soon() ? Completion.SOON : Completion.NOW;
    }
    for (ComputedFacet facet : file.vajram().computedFacets()) {
      if (facet instanceof Dependency dependency) {
        completion = max(completion, completionOfInvocation(file, dependency.invocation()));
      }
    }
    if (file.vajram().outputBlock() instanceof OutputBlock.Delegate delegate) {
      completion = max(completion, completionOfInvocation(file, delegate.invocation()));
    }
    return completion;
  }

  private Completion completionOfInvocation(VajramFile caller, DependencyInvocation invocation) {
    VajramFile callee = lookup(invocation.vajramName());
    Completion completion;
    if (callee != null) {
      completion = completionOf(callee);
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
