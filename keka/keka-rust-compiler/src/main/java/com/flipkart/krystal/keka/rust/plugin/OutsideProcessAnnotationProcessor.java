package com.flipkart.krystal.keka.rust.plugin;

import com.flipkart.krystal.keka.rust.ast.Completion;
import com.flipkart.krystal.keka.rust.ast.Dependency;
import com.flipkart.krystal.keka.rust.ast.InputDecl;
import com.flipkart.krystal.keka.rust.ast.OutputBlock;
import com.flipkart.krystal.keka.rust.ast.VajramFile;
import com.flipkart.krystal.keka.rust.cli.RustCompilerMain.Target;
import com.flipkart.krystal.keka.rust.codegen.Naming;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Generates a process entry point which dispatches public {@code `outsideProcess} Vajrams by name.
 */
public final class OutsideProcessAnnotationProcessor implements VajramAnnotationProcessor {

  private static final String ANNOTATION = "outsideProcess";

  @Override
  public Set<String> supportedAnnotations() {
    return Set.of(ANNOTATION);
  }

  @Override
  public ProcessingMode processingMode() {
    return ProcessingMode.AGGREGATING;
  }

  @Override
  public void process(AnnotationProcessorContext context) throws IOException {
    List<VajramFile> targets =
        context.matchingVajrams().stream()
            .filter(
                file -> {
                  if (file.vajram().callers()
                      instanceof com.flipkart.krystal.keka.rust.ast.Callers.Public) {
                    return true;
                  }
                  context.error(
                      file.vajram().location(),
                      "`outsideProcess must annotate a public callers declaration");
                  return false;
                })
            .filter(
                file -> {
                  if (supportsOutsideProcessDi(file, context)
                      && file.vajram().inputs().stream()
                          .allMatch(OutsideProcessAnnotationProcessor::isCliInput)) {
                    return true;
                  }
                  context.error(
                      file.vajram().location(),
                      "`outsideProcess supports string, numeric and List of string/numeric inputs plus ConsoleWriter injections or errable inputs");
                  return false;
                })
            .sorted(java.util.Comparator.comparing(file -> file.vajram().name()))
            .toList();
    if (targets.isEmpty()) {
      return;
    }

    if (context.target() == Target.WASM) {
      emitWasmDispatcher(context, targets);
      return;
    }

    StringBuilder source = new StringBuilder();
    source.append("mod keka_rt;\n");
    java.util.stream.Stream.concat(
            context.compilationVajrams().stream().map(VajramFile::packageSegments),
            context.symbolTable().models().stream()
                .map(com.flipkart.krystal.keka.rust.ast.ModelFile::packageSegments))
        .filter(segments -> !segments.isEmpty())
        .map(segments -> Naming.toSnakeCase(segments.get(0)))
        .distinct()
        .sorted()
        .forEach(module -> source.append("pub mod ").append(module).append(";\n"));
    source.append("\nfn main() {\n");
    source.append("    let _vajram = std::env::args().nth(1).unwrap_or_else(|| {\n");
    source.append("        eprintln!(\"usage: <program> <vajram-name> [--input value]...\");\n");
    source.append("        std::process::exit(2);\n");
    source.append("    });\n");
    source.append("    let mut _arguments = std::env::args().skip(2);\n");
    source.append(
        "    let mut _inputs: std::collections::HashMap<String, Vec<String>> = std::collections::HashMap::new();\n");
    source.append("    let _arena = crate::keka_rt::Arena::new();\n");
    source.append(
        "    let _app_context = crate::keka_rt::AppContext::new(std::rc::Rc::new(crate::keka_rt::DefaultInjector::default()));\n");
    source.append("    while let Some(_flag) = _arguments.next() {\n");
    source.append(
        "        let _input = _flag.strip_prefix(\"--\").filter(|_input| !_input.is_empty()).unwrap_or_else(|| {\n");
    source.append(
        "            eprintln!(\"expected input flag in the form --input value, got {}\", _flag);\n");
    source.append("            std::process::exit(2);\n");
    source.append("        });\n");
    source.append("        let _value = _arguments.next().unwrap_or_else(|| {\n");
    source.append("            eprintln!(\"missing value for input {}\", _input);\n");
    source.append("            std::process::exit(2);\n");
    source.append("        });\n");
    source.append("        _inputs.entry(_input.to_owned()).or_default().push(_value);\n");
    source.append("    }\n");
    source.append("    match _vajram.as_str() {\n");
    for (VajramFile target : targets) {
      emitCase(source, target, context.symbolTable().completionOf(target), context);
    }
    source.append("        _ => {\n");
    source.append(
        "            eprintln!(\"Vajram {} has not been declared to be called from outside-process\", _vajram);\n");
    source.append("            std::process::exit(2);\n");
    source.append("        }\n");
    source.append("    }\n");
    source.append("}\n");
    context.writeFile(Path.of("main.rs"), source.toString());
  }

  private static void emitWasmDispatcher(
      AnnotationProcessorContext context, List<VajramFile> targets) throws IOException {
    List<VajramFile> wasmTargets =
        targets.stream()
            .filter(
                file -> {
                  if (file.vajram().inputs().stream().anyMatch(i -> isListInput(i))) {
                    context.error(
                        file.vajram().location(),
                        "WASM `outsideProcess` does not support List inputs");
                    return false;
                  }
                  if (isWasmOutput(file)) {
                    return true;
                  }
                  context.error(
                      file.vajram().location(),
                      "WASM `outsideProcess` supports string, int, or void outputs");
                  return false;
                })
            .toList();
    if (wasmTargets.isEmpty()) {
      return;
    }

    StringBuilder source = new StringBuilder();
    source.append("use wasm_bindgen::prelude::*;\n");
    source.append("use std::rc::Rc;\n");
    for (VajramFile target : wasmTargets) {
      emitWasmCase(source, target, context.symbolTable().completionOf(target), context);
    }
    context.writeFile(Path.of("wasm_dispatch.rs"), source.toString());
  }

  private static void emitWasmCase(
      StringBuilder source,
      VajramFile target,
      Completion completion,
      AnnotationProcessorContext context) {
    String module =
        target.packageSegments().stream()
                .map(Naming::toSnakeCase)
                .reduce((a, b) -> a + "::" + b)
                .map(path -> "crate::" + path)
                .orElse("crate")
            + "::"
            + Naming.sourceModuleName(target.sourcePath())
            + "::"
            + Naming.toSnakeCase(target.vajram().name());
    String functionName = "outside_process_" + Naming.toSnakeCase(target.vajram().name());
    String parameters =
        target.vajram().inputs().stream()
            .map(input -> input.name() + ": " + wasmInputType(input))
            .collect(java.util.stream.Collectors.joining(", "));
    String fields =
        target.vajram().inputs().stream()
            .map(input -> input.name() + ": _arena.alloc(" + input.name() + ")")
            .collect(java.util.stream.Collectors.joining(", "));
    source.append("\n#[wasm_bindgen]\n");
    source
        .append("pub ")
        .append(completion.isAsync() ? "async " : "")
        .append("fn ")
        .append(functionName)
        .append("(")
        .append(parameters)
        .append(") -> String {\n");
    source.append("    let _arena = crate::keka_rt::Arena::new();\n");
    source.append(
        "    let _app_context = crate::keka_rt::AppContext::new(Rc::new(crate::keka_rt::DefaultInjector::default()));\n");
    source
        .append("    let _output = ")
        .append(module)
        .append("::call(vec![")
        .append(module)
        .append("::")
        .append(Naming.capitalize(target.vajram().name()))
        .append("Inputs { ")
        .append(fields)
        .append(" }], &_arena, &_app_context")
        .append(")")
        .append(completion.isAsync() ? ".await" : "")
        .append(".into_iter().next().expect(\"outside-process Vajram result\");\n");
    if (context.symbolTable().isErrable(target)) {
      source.append(
          "    let _output = match _output { Ok(_value) => _value, Err(_error) => return format!(\"error: {}\", _error) };\n");
    }
    switch (target.vajram().outputType().name()) {
      case "string" -> source.append("    (*_output).clone()\n");
      case "int" -> source.append("    _output.to_string()\n");
      case "void" -> source.append("    String::new()\n");
      default -> throw new IllegalStateException("Validated unsupported WASM output");
    }
    source.append("}\n");
  }

  private static void emitCase(
      StringBuilder source,
      VajramFile target,
      Completion completion,
      AnnotationProcessorContext context) {
    String modulePath =
        target.packageSegments().stream()
            .map(Naming::toSnakeCase)
            .reduce((a, b) -> a + "::" + b)
            .orElse("");
    String module =
        modulePath
            + "::"
            + Naming.sourceModuleName(target.sourcePath())
            + "::"
            + Naming.toSnakeCase(target.vajram().name());
    StringBuilder inputFields = new StringBuilder();
    for (int i = 0; i < target.vajram().inputs().size(); i++) {
      if (i > 0) {
        inputFields.append(", ");
      }
      InputDecl input = target.vajram().inputs().get(i);
      inputFields
          .append(input.name())
          .append(": ")
          .append(cliInputValue(input, target.vajram().name()));
    }
    String inputs =
        module
            + "::"
            + Naming.capitalize(target.vajram().name())
            + "Inputs { "
            + inputFields
            + " }]";
    source.append("        \"").append(target.vajram().name()).append("\" => {\n");
    source.append("            for _input in _inputs.keys() {\n");
    source.append("                if ![");
    for (int i = 0; i < target.vajram().inputs().size(); i++) {
      if (i > 0) {
        source.append(", ");
      }
      source.append("\"").append(target.vajram().inputs().get(i).name()).append("\"");
    }
    source.append("].contains(&_input.as_str()) {\n");
    source
        .append("                    eprintln!(\"unknown input {} for ")
        .append(target.vajram().name())
        .append("\", _input);\n");
    source.append("                    std::process::exit(2);\n");
    source.append("                }\n");
    source.append("            }\n");
    if (completion == Completion.NOW) {
      source
          .append("            let _output = ")
          .append(module)
          .append("::call(vec![")
          .append(inputs)
          .append(", &_arena, &_app_context")
          .append(").into_iter().next().expect(\"outside-process Vajram result\");\n");
      appendCliUnwrap(source, target, context, "            ");
      source.append(
          isVoidOutput(target)
              ? "            let _ = _output;\n"
              : "            println!(\"{}\", _output);\n");
    } else {
      source.append(
          "            let _runtime = tokio::runtime::Builder::new_current_thread().enable_all().build().unwrap();\n");
      source.append("            let _local = tokio::task::LocalSet::new();\n");
      source.append("            _local.block_on(&_runtime, async {\n");
      source
          .append("                let _output = ")
          .append(module)
          .append("::call(vec![")
          .append(inputs)
          .append(", &_arena, &_app_context")
          .append(").await.into_iter().next().expect(\"outside-process Vajram result\");\n");
      appendCliUnwrap(source, target, context, "                ");
      source.append(
          isVoidOutput(target)
              ? "                let _ = _output;\n"
              : "                println!(\"{}\", _output);\n");
      source.append("            });\n");
    }
    source.append("        }\n");
  }

  /** An errable Vajram yields a {@code Result}: print its error to stderr and exit non-zero. */
  private static void appendCliUnwrap(
      StringBuilder source, VajramFile target, AnnotationProcessorContext context, String indent) {
    if (context.symbolTable().isErrable(target)) {
      source
          .append(indent)
          .append(
              "let _output = match _output { Ok(_value) => _value, Err(_error) => { eprintln!(\"error: {}\", _error); std::process::exit(1) } };\n");
    }
  }

  /** A {@code void} Vajram yields {@code ()}, which has nothing to print. */
  private static boolean isVoidOutput(VajramFile target) {
    return "void".equals(target.vajram().outputType().name());
  }

  private static boolean isCliInput(InputDecl input) {
    return !input.type().errable()
        && (isScalar(input.type())
            || (isListInput(input) && isScalar(input.type().typeArgs().get(0))));
  }

  private static boolean isScalar(com.flipkart.krystal.keka.rust.ast.TypeRef type) {
    return !type.errable()
        && type.typeArgs().isEmpty()
        && ("string".equals(type.name()) || rustNumericType(type.name()) != null);
  }

  private static boolean isListInput(InputDecl input) {
    return "List".equals(input.type().name()) && input.type().typeArgs().size() == 1;
  }

  private static boolean supportsOutsideProcessDi(
      VajramFile file, AnnotationProcessorContext context) {
    return requiredInjections(file, context, new java.util.HashSet<>()).stream()
        .allMatch(injection -> "ConsoleWriter".equals(injection.type().name()));
  }

  private static List<com.flipkart.krystal.keka.rust.ast.InjectionDecl> requiredInjections(
      VajramFile file, AnnotationProcessorContext context, Set<VajramFile> visited) {
    if (!visited.add(file)) {
      return List.of();
    }
    List<com.flipkart.krystal.keka.rust.ast.InjectionDecl> injections =
        new java.util.ArrayList<>(file.vajram().injections());
    for (var facet : file.vajram().computedFacets()) {
      if (facet instanceof Dependency dependency) {
        addInjections(dependency.invocation().vajramName(), context, visited, injections);
      }
    }
    if (file.vajram().outputBlock() instanceof OutputBlock.Delegate delegate) {
      addInjections(delegate.invocation().vajramName(), context, visited, injections);
    }
    return injections;
  }

  private static void addInjections(
      String vajramName,
      AnnotationProcessorContext context,
      Set<VajramFile> visited,
      List<com.flipkart.krystal.keka.rust.ast.InjectionDecl> injections) {
    VajramFile callee = context.symbolTable().lookup(vajramName);
    if (callee != null) {
      injections.addAll(requiredInjections(callee, context, visited));
    }
  }

  private static boolean isWasmOutput(VajramFile file) {
    return !file.vajram().outputType().errable()
        && ("string".equals(file.vajram().outputType().name())
            || "int".equals(file.vajram().outputType().name())
            || "void".equals(file.vajram().outputType().name()));
  }

  private static String wasmInputType(InputDecl input) {
    return "string".equals(input.type().name()) ? "String" : "i64";
  }

  private static String cliInputValue(InputDecl input, String vajramName) {
    if (isListInput(input)) {
      // Repeated flag; an omitted flag is an empty list.
      return "_arena.alloc(_inputs.get(\""
          + input.name()
          + "\").map(|_values| _values.iter().map(|_e| "
          + cliScalar("_e", input.type().typeArgs().get(0), input.name(), vajramName)
          + ").collect::<Vec<_>>()).unwrap_or_default())";
    }
    String argument =
        "_inputs.get(\""
            + input.name()
            + "\").map(|_values| { if _values.len() > 1 { eprintln!(\"input "
            + input.name()
            + " was specified more than once\"); std::process::exit(2) } &_values[0] })"
            + ".unwrap_or_else(|| { eprintln!(\"missing input "
            + input.name()
            + " for "
            + vajramName
            + "\"); std::process::exit(2) })";
    return "_arena.alloc(" + cliScalar(argument, input.type(), input.name(), vajramName) + ")";
  }

  private static String cliScalar(
      String argument,
      com.flipkart.krystal.keka.rust.ast.TypeRef type,
      String inputName,
      String vajramName) {
    if ("string".equals(type.name())) {
      return argument + ".to_owned()";
    }
    return argument
        + ".parse::<"
        + rustNumericType(type.name())
        + ">().unwrap_or_else(|_| { eprintln!(\"invalid numeric input "
        + inputName
        + " for "
        + vajramName
        + "\"); std::process::exit(2) })";
  }

  private static String rustNumericType(String type) {
    return switch (type) {
      case "int" -> "i64";
      case "int32" -> "i32";
      case "int64" -> "i64";
      case "int128" -> "i128";
      case "uint32" -> "u32";
      case "uint64" -> "u64";
      case "uint128" -> "u128";
      case "float", "float32" -> "f32";
      case "float64", "double" -> "f64";
      default -> null;
    };
  }
}
