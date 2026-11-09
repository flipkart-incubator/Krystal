package com.flipkart.krystal.keka.rust.resolve;

import com.flipkart.krystal.keka.rust.ast.Callers;
import com.flipkart.krystal.keka.rust.ast.Completion;
import com.flipkart.krystal.keka.rust.ast.ComputedFacet;
import com.flipkart.krystal.keka.rust.ast.DepInputResolver;
import com.flipkart.krystal.keka.rust.ast.Dependency;
import com.flipkart.krystal.keka.rust.ast.DependencyInvocation;
import com.flipkart.krystal.keka.rust.ast.Expr;
import com.flipkart.krystal.keka.rust.ast.Field;
import com.flipkart.krystal.keka.rust.ast.OutputBlock;
import com.flipkart.krystal.keka.rust.ast.Statement;
import com.flipkart.krystal.keka.rust.ast.TypeRef;
import com.flipkart.krystal.keka.rust.ast.VajramDef;
import com.flipkart.krystal.keka.rust.ast.VajramFile;
import com.flipkart.krystal.keka.rust.cli.RustCompilerMain.Target;
import com.flipkart.krystal.keka.rust.diag.Diagnostics;
import com.flipkart.krystal.keka.rust.system.SystemVajram;
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
          checkCallType(file, dependency, table, diagnostics);
          facetFanout.put(dependency.name(), dependency.fanout());
        }
      }
      if (vajram.outputBlock() instanceof OutputBlock.Delegate delegate) {
        checkInvocation(file, delegate.invocation(), table, diagnostics, target);
      }
      checkInferredTypes(vajram, diagnostics);
      if (checkOutputCompletionDeclared(vajram, diagnostics)) {
        checkCompletion(file, table, diagnostics);
        checkErrability(file, table, diagnostics);
      }
    }
  }

  /**
   * A Vajram's declared output type is the only source of truth for when it completes (see {@link
   * SymbolTable#completionOf}) - so it must be at least as late as its body requires (its own
   * output block's {@code ~}/{@code ~~} marker, or the declared completion of any dependency or
   * delegate it calls): a {@code `now} Vajram cannot depend on a {@code ~}/{@code ~~} one, and a
   * {@code ~} Vajram cannot depend on a {@code ~~} one. Keka never blocks a thread to reconcile
   * this, so a mismatch is a compile error rather than something silently inferred/promoted.
   */
  private static void checkCompletion(VajramFile file, SymbolTable table, Diagnostics diagnostics) {
    Completion declared = table.completionOf(file);
    Completion required = table.inferredCompletion(file);
    if (required.compareTo(declared) > 0) {
      diagnostics.error(
          file.vajram().location(),
          "Vajram '"
              + file.vajram().name()
              + "' is declared "
              + describe(declared)
              + " but its output block, or a dependency/delegate it calls, completes "
              + describe(required)
              + " - a vajram cannot complete earlier than what it depends on; declare it as"
              + " 'out "
              + file.vajram().outputType().name()
              + (required == Completion.LATER ? "~~" : "")
              + "' instead");
    }
  }

  /**
   * A Vajram is errable iff it declares {@code `now T?} or completes {@code ~}/{@code ~~} (see
   * {@link SymbolTable#isErrable}). A non-errable ({@code `now T}) Vajram can't fail, so it can
   * neither {@code throw} nor ignore the failure of an errable dependency: it must declare that
   * facet {@code T?} and handle it.
   */
  private static void checkErrability(VajramFile file, SymbolTable table, Diagnostics diagnostics) {
    VajramDef vajram = file.vajram();
    if (vajram.outputType().errable() && !vajram.outputAnnotations().contains("now")) {
      diagnostics.error(
          vajram.location(),
          "Vajram '"
              + vajram.name()
              + "' declares 'out "
              + vajram.outputType().name()
              + "?' without 'now' - the '?' is redundant because every soon/later vajram is"
              + " errable; write 'out "
              + vajram.outputType().name()
              + "' (or 'out `now "
              + vajram.outputType().name()
              + "?' for a sync errable vajram)");
    }
    if (table.isErrable(file)) {
      return;
    }
    if (vajram.outputBlock() instanceof OutputBlock.Logic logic
        && logic.body().statements().stream().anyMatch(Statement.Throw.class::isInstance)) {
      diagnostics.error(
          vajram.location(),
          "Vajram '"
              + vajram.name()
              + "' is non-errable ('out `now "
              + vajram.outputType().name()
              + "') so it cannot 'throw' - declare 'out "
              + vajram.outputType().name()
              + "?' or make it async");
    }
    for (ComputedFacet facet : vajram.computedFacets()) {
      if (facet instanceof Dependency dependency
          && !dependency.type().errable()
          && table.isErrableCallee(file, dependency.invocation().vajramName())) {
        diagnostics.error(
            dependency.location(),
            "Dependency '"
                + dependency.name()
                + "' calls errable '"
                + dependency.invocation().vajramName()
                + "' from non-errable vajram '"
                + vajram.name()
                + "' - declare it as '"
                + dependency.type().name()
                + "? "
                + dependency.name()
                + "' and handle the error, or make '"
                + vajram.name()
                + "' errable");
      }
    }
    if (vajram.outputBlock() instanceof OutputBlock.Delegate delegate
        && table.isErrableCallee(file, delegate.invocation().vajramName())) {
      diagnostics.error(
          delegate.invocation().location(),
          "Vajram '"
              + vajram.name()
              + "' delegates to errable '"
              + delegate.invocation().vajramName()
              + "' but is non-errable - declare 'out "
              + vajram.outputType().name()
              + "?'");
    }
  }

  private static String describe(Completion completion) {
    return switch (completion) {
      case NOW -> "'now'";
      case SOON -> "soon ('~')";
      case LATER -> "later ('~~')";
    };
  }

  /**
   * A Vajram's output completion is {@code `now}, {@code T~} (soon) or {@code T~~} (later); with no
   * marker it defaults to soon. At most one marker may be given. Returns whether that holds.
   */
  private static boolean checkOutputCompletionDeclared(VajramDef vajram, Diagnostics diagnostics) {
    TypeRef output = vajram.outputType();
    int declared =
        (vajram.outputAnnotations().contains("now") ? 1 : 0)
            + (output.soon() ? 1 : 0)
            + (output.later() ? 1 : 0);
    if (declared <= 1) {
      return true;
    }
    diagnostics.error(
        vajram.location(),
        "Vajram '"
            + vajram.name()
            + "' declares conflicting output completions - use at most one of 'out `now "
            + output.name()
            + "', 'out "
            + output.name()
            + "~' or 'out "
            + output.name()
            + "~~' (plain 'out "
            + output.name()
            + "' means soon)");
    return false;
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

  /**
   * {@code Call<Req, Resp>} pairs a dependency's response with its request (one pair per call, or a
   * list of pairs with {@code Call<Req, Resp>...} on a fanout). The callee must be a generated
   * (non-system) vajram, and {@code Req} is {@code _} (inferred) or {@code callee#req}.
   */
  private static void checkCallType(
      VajramFile file, Dependency dependency, SymbolTable table, Diagnostics diagnostics) {
    TypeRef type = dependency.type();
    if (!"Call".equals(type.name())) {
      return;
    }
    String callee = dependency.invocation().vajramName();
    if (type.typeArgs().size() != 2) {
      diagnostics.error(dependency.location(), "'Call' takes two type arguments: Call<Req, Resp>");
    } else if (!type.typeArgs().get(0).name().equals("_")
        && !type.typeArgs().get(0).name().equals(callee + "#req")) {
      diagnostics.error(
          dependency.location(),
          "The request type of 'Call' for '" + callee + "' must be '_' or '" + callee + "#req'");
    }
    if (table.lookup(callee) == null) {
      diagnostics.error(
          dependency.location(),
          "'Call<...>' needs a request type, which system vajram '" + callee + "' doesn't have");
    }
  }

  /** {@code _} (an inferred type) is only allowed as the request type of a {@code Call}. */
  private static void checkInferredTypes(VajramDef vajram, Diagnostics diagnostics) {
    checkNoInferred(vajram.outputType(), vajram, diagnostics);
    vajram.inputs().forEach(input -> checkNoInferred(input.type(), vajram, diagnostics));
    vajram
        .injections()
        .forEach(injection -> checkNoInferred(injection.type(), vajram, diagnostics));
    for (ComputedFacet facet : vajram.computedFacets()) {
      TypeRef type = facet instanceof Field field ? field.type() : ((Dependency) facet).type();
      if ("Call".equals(type.name()) && type.typeArgs().size() == 2) {
        checkNoInferred(type.typeArgs().get(1), vajram, diagnostics);
      } else {
        checkNoInferred(type, vajram, diagnostics);
      }
    }
  }

  private static void checkNoInferred(TypeRef type, VajramDef vajram, Diagnostics diagnostics) {
    if (type.name().equals("_")) {
      diagnostics.error(
          vajram.location(),
          "'_' can only be used as the request type of 'Call<_, T>' in vajram '"
              + vajram.name()
              + "'");
    }
    type.typeArgs().forEach(arg -> checkNoInferred(arg, vajram, diagnostics));
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
                  + "' is not supported for the wasm target: "
                  + systemVajram.get().unsupportedTargetReason());
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
