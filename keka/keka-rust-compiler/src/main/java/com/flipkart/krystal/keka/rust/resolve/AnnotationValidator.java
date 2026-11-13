package com.flipkart.krystal.keka.rust.resolve;

import com.flipkart.krystal.keka.rust.ast.AnnotationDef;
import com.flipkart.krystal.keka.rust.ast.AnnotationDefFile;
import com.flipkart.krystal.keka.rust.ast.ComputedFacet;
import com.flipkart.krystal.keka.rust.ast.Dependency;
import com.flipkart.krystal.keka.rust.ast.DependencyInvocation;
import com.flipkart.krystal.keka.rust.ast.Expr;
import com.flipkart.krystal.keka.rust.ast.Field;
import com.flipkart.krystal.keka.rust.ast.InjectionDecl;
import com.flipkart.krystal.keka.rust.ast.InputDecl;
import com.flipkart.krystal.keka.rust.ast.ModelDef;
import com.flipkart.krystal.keka.rust.ast.ModelFile;
import com.flipkart.krystal.keka.rust.ast.OutputBlock;
import com.flipkart.krystal.keka.rust.ast.SourceLocation;
import com.flipkart.krystal.keka.rust.ast.VajramAnnotation;
import com.flipkart.krystal.keka.rust.ast.VajramDef;
import com.flipkart.krystal.keka.rust.ast.VajramFile;
import com.flipkart.krystal.keka.rust.diag.Diagnostics;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Checks custom annotation definitions, and the use of defined annotations.
 *
 * <p>Annotation arguments and parameter defaults are compile-time constants (the grammar already
 * limits them to constant syntax); here they are evaluated and checked against the declared
 * parameter types. Annotations with no definition (the built-in ones such as {@code `batched} or
 * {@code `outsideProcess}) are not checked.
 */
final class AnnotationValidator {

  /** Where an annotation may be written; used by the {@code `target} meta-annotation. */
  static final Set<String> TARGETS =
      Set.of(
          "VAJRAM",
          "INPUT",
          "OUTPUT",
          "FACET",
          "DEPENDENCY",
          "INJECTION",
          "CALLERS",
          "PACKAGE",
          "MODEL",
          "FIELD");

  static final String TARGET_META_ANNOTATION = "target";
  private static final String TARGET_KINDS = "kinds";

  private AnnotationValidator() {}

  static void validate(List<VajramFile> files, SymbolTable table, Diagnostics diagnostics) {
    for (AnnotationDefFile file : table.annotationDefinitions()) {
      validateDefinition(file.definition(), diagnostics);
    }
    for (VajramFile file : files) {
      validateSites(file, table, diagnostics);
    }
    for (ModelFile file : table.models()) {
      ModelDef model = file.model();
      check(table, diagnostics, "PACKAGE", model.location(), file.packageAnnotations());
      check(table, diagnostics, "MODEL", model.location(), model.annotations());
      for (ModelDef.FieldDecl field : model.fields()) {
        check(table, diagnostics, "FIELD", field.location(), field.annotations());
      }
    }
  }

  /** Checks the annotations written at every site of one vajram against their definitions. */
  private static void validateSites(VajramFile file, SymbolTable table, Diagnostics diagnostics) {
    VajramDef vajram = file.vajram();
    SourceLocation vajramLocation = vajram.location();
    check(table, diagnostics, "PACKAGE", vajramLocation, file.packageAnnotations());
    check(table, diagnostics, "VAJRAM", vajramLocation, vajram.annotations());
    for (InputDecl input : vajram.inputs()) {
      check(table, diagnostics, "INPUT", vajramLocation, input.annotations());
    }
    check(table, diagnostics, "OUTPUT", vajramLocation, vajram.outputAnnotations());
    for (InjectionDecl injection : vajram.injections()) {
      check(table, diagnostics, "INJECTION", vajramLocation, injection.annotations());
    }
    if (vajram.callers() != null) {
      check(table, diagnostics, "CALLERS", vajramLocation, vajram.callers().annotations());
    }
    for (ComputedFacet facet : vajram.computedFacets()) {
      if (facet instanceof Field field) {
        check(table, diagnostics, "FACET", field.location(), field.annotations());
      } else if (facet instanceof Dependency dependency) {
        check(table, diagnostics, "DEPENDENCY", dependency.location(), dependency.annotations());
        for (DependencyInvocation.AnnotatedBlock block :
            dependency.invocation().extraAnnotatedBlocks()) {
          check(table, diagnostics, "DEPENDENCY", dependency.location(), block.annotations());
        }
      }
    }
    if (vajram.outputBlock() instanceof OutputBlock.Logic logic) {
      check(table, diagnostics, "OUTPUT", vajramLocation, logic.annotations());
    } else if (vajram.outputBlock() instanceof OutputBlock.Delegate delegate) {
      for (DependencyInvocation.AnnotatedBlock block :
          delegate.invocation().extraAnnotatedBlocks()) {
        check(table, diagnostics, "OUTPUT", vajramLocation, block.annotations());
      }
    }
  }

  private static void check(
      SymbolTable table,
      Diagnostics diagnostics,
      String site,
      SourceLocation location,
      List<VajramAnnotation> uses) {
    for (VajramAnnotation use : uses) {
      AnnotationDef definition = table.annotationDefinition(use.name());
      if (definition != null) {
        validateUse(use, definition, site, location, diagnostics);
      }
    }
  }

  private static String describeSite(String site) {
    return switch (site) {
      case "INPUT" -> "an input";
      case "OUTPUT" -> "an output";
      case "INJECTION" -> "an injection";
      case "CALLERS" -> "a callers declaration";
      case "FACET" -> "a facet";
      case "DEPENDENCY" -> "a dependency";
      case "PACKAGE" -> "a package";
      case "MODEL" -> "a model";
      case "FIELD" -> "a model field";
      default -> "a vajram";
    };
  }

  private static void validateDefinition(AnnotationDef definition, Diagnostics diagnostics) {
    Set<String> names = new HashSet<>();
    for (AnnotationDef.Parameter parameter : definition.parameters()) {
      if (!names.add(parameter.name())) {
        diagnostics.error(
            parameter.location(),
            "Annotation '"
                + definition.name()
                + "' declares parameter '"
                + parameter.name()
                + "' more than once");
      }
      if (!Constants.isSupportedType(parameter.type())) {
        diagnostics.error(
            parameter.location(),
            "Parameter '"
                + parameter.name()
                + "' of annotation '"
                + definition.name()
                + "' has unsupported type '"
                + Constants.describe(parameter.type())
                + "' - annotation parameters are primitives, 'string', or lists of those");
      } else if (parameter.defaultValue() != null) {
        Constants.checkValue(
            parameter.defaultValue(),
            parameter.type(),
            "default of parameter '" + parameter.name() + "'",
            parameter.location(),
            diagnostics);
      }
    }
    for (VajramAnnotation meta : definition.metaAnnotations()) {
      if (meta.name().equals(TARGET_META_ANNOTATION)) {
        targetsOf(definition, diagnostics);
      }
    }
  }

  private static void validateUse(
      VajramAnnotation use,
      AnnotationDef definition,
      String site,
      SourceLocation location,
      Diagnostics diagnostics) {
    Set<String> targets = targetsOf(definition, new Diagnostics());
    if (!targets.contains(site)) {
      diagnostics.error(
          location,
          "Annotation '"
              + use.name()
              + "' cannot be applied to "
              + describeSite(site)
              + "; "
              + (targets.isEmpty()
                  ? "it declares no '`target', so it is allowed nowhere"
                  : "it targets " + targets));
    }
    Set<String> given = new HashSet<>();
    for (VajramAnnotation.Argument argument : use.arguments()) {
      AnnotationDef.Parameter parameter = parameterOf(definition, argument.name());
      if (parameter == null) {
        diagnostics.error(
            location, "Annotation '" + use.name() + "' has no parameter '" + argument.name() + "'");
        continue;
      }
      if (!given.add(argument.name())) {
        diagnostics.error(
            location,
            "Annotation '"
                + use.name()
                + "' passes parameter '"
                + argument.name()
                + "' more than once");
        continue;
      }
      if (Constants.isSupportedType(parameter.type())) {
        Constants.checkValue(
            argument.value(),
            parameter.type(),
            "argument '" + argument.name() + "' of annotation '" + use.name() + "'",
            location,
            diagnostics);
      }
    }
    for (AnnotationDef.Parameter parameter : definition.parameters()) {
      if (parameter.defaultValue() == null && !given.contains(parameter.name())) {
        diagnostics.error(
            location,
            "Annotation '"
                + use.name()
                + "' is missing required parameter '"
                + parameter.name()
                + "'");
      }
    }
  }

  /**
   * The targets named by the definition's {@code `target(kinds = [...])}. A definition without one
   * has no targets: the annotation is allowed nowhere. Problems are reported to {@code
   * diagnostics}.
   */
  private static Set<String> targetsOf(AnnotationDef definition, Diagnostics diagnostics) {
    for (VajramAnnotation meta : definition.metaAnnotations()) {
      if (!meta.name().equals(TARGET_META_ANNOTATION)) {
        continue;
      }
      Set<String> targets = new LinkedHashSet<>();
      for (VajramAnnotation.Argument argument : meta.arguments()) {
        if (!argument.name().equals(TARGET_KINDS)
            || !(argument.value() instanceof Expr.Array array)) {
          diagnostics.error(
              definition.location(),
              "'`target' takes one parameter, 'kinds', a list of " + TARGETS);
          continue;
        }
        for (Expr element : array.elements()) {
          if (element instanceof Expr.VarUse use && TARGETS.contains(use.name())) {
            targets.add(use.name());
          } else {
            diagnostics.error(
                definition.location(),
                "Unknown annotation target in '`target'; expected one of " + TARGETS);
          }
        }
      }
      if (meta.arguments().isEmpty()) {
        diagnostics.error(definition.location(), "'`target' needs 'kinds', a list of " + TARGETS);
      }
      return targets;
    }
    return Set.of();
  }

  private static AnnotationDef.@Nullable Parameter parameterOf(
      AnnotationDef definition, String name) {
    return definition.parameters().stream()
        .filter(parameter -> parameter.name().equals(name))
        .findFirst()
        .orElse(null);
  }
}
