package com.flipkart.krystal.vajram.lang.rust.plugin;

import com.flipkart.krystal.vajram.lang.rust.ast.Completion;
import com.flipkart.krystal.vajram.lang.rust.ast.InputDecl;
import com.flipkart.krystal.vajram.lang.rust.ast.VajramFile;
import com.flipkart.krystal.vajram.lang.rust.cli.RustCompilerMain.Target;
import com.flipkart.krystal.vajram.lang.rust.codegen.Naming;
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
                      instanceof com.flipkart.krystal.vajram.lang.rust.ast.Callers.Public) {
                    return true;
                  }
                  context.error(
                      file.vajram().location(),
                      "`outsideProcess must annotate a public callers declaration");
                  return false;
                })
            .filter(
                file -> {
                  if (file.vajram().injections().isEmpty()
                      && file.vajram().inputs().stream()
                          .allMatch(OutsideProcessAnnotationProcessor::isCliInput)) {
                    return true;
                  }
                  context.error(
                      file.vajram().location(),
                      "`outsideProcess supports string and int inputs but no injections or errable inputs");
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
    source.append("mod vajram_rt;\n");
    context.compilationVajrams().stream()
        .map(VajramFile::packageSegments)
        .filter(segments -> !segments.isEmpty())
        .map(segments -> Naming.toSnakeCase(segments.get(0)))
        .distinct()
        .sorted()
        .forEach(module -> source.append("pub mod ").append(module).append(";\n"));
    source.append("\nfn main() {\n");
    source.append("    let vajram = std::env::args().nth(1).unwrap_or_else(|| {\n");
    source.append("        eprintln!(\"usage: <program> <vajram-name> [--input value]...\");\n");
    source.append("        std::process::exit(2);\n");
    source.append("    });\n");
    source.append("    let mut arguments = std::env::args().skip(2);\n");
    source.append("    let mut inputs = std::collections::HashMap::new();\n");
    source.append("    while let Some(flag) = arguments.next() {\n");
    source.append(
        "        let input = flag.strip_prefix(\"--\").filter(|input| !input.is_empty()).unwrap_or_else(|| {\n");
    source.append(
        "            eprintln!(\"expected input flag in the form --input value, got {}\", flag);\n");
    source.append("            std::process::exit(2);\n");
    source.append("        });\n");
    source.append("        let value = arguments.next().unwrap_or_else(|| {\n");
    source.append("            eprintln!(\"missing value for input {}\", input);\n");
    source.append("            std::process::exit(2);\n");
    source.append("        });\n");
    source.append("        if inputs.insert(input.to_owned(), value).is_some() {\n");
    source.append("            eprintln!(\"input {} was specified more than once\", input);\n");
    source.append("            std::process::exit(2);\n");
    source.append("        }\n");
    source.append("    }\n");
    source.append("    match vajram.as_str() {\n");
    for (VajramFile target : targets) {
      emitCase(source, target, context.symbolTable().completionOf(target));
    }
    source.append("        _ => {\n");
    source.append("            eprintln!(\"unknown outside-process Vajram: {}\", vajram);\n");
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
      emitWasmCase(source, target, context.symbolTable().completionOf(target));
    }
    context.writeFile(Path.of("wasm_dispatch.rs"), source.toString());
  }

  private static void emitWasmCase(StringBuilder source, VajramFile target, Completion completion) {
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
            .map(input -> input.name() + ": Rc::new(" + input.name() + ")")
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
    source
        .append("    let output = ")
        .append(module)
        .append("::call(vec![")
        .append(module)
        .append("::")
        .append(Naming.capitalize(target.vajram().name()))
        .append("Inputs { ")
        .append(fields)
        .append(" }])")
        .append(completion.isAsync() ? ".await" : "")
        .append(".into_iter().next().expect(\"outside-process Vajram result\");\n");
    switch (target.vajram().outputType().name()) {
      case "string" -> source.append("    (*output).clone()\n");
      case "int" -> source.append("    output.to_string()\n");
      case "void" -> source.append("    String::new()\n");
      default -> throw new IllegalStateException("Validated unsupported WASM output");
    }
    source.append("}\n");
  }

  private static void emitCase(StringBuilder source, VajramFile target, Completion completion) {
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
    source.append("            for input in inputs.keys() {\n");
    source.append("                if ![");
    for (int i = 0; i < target.vajram().inputs().size(); i++) {
      if (i > 0) {
        source.append(", ");
      }
      source.append("\"").append(target.vajram().inputs().get(i).name()).append("\"");
    }
    source.append("].contains(&input.as_str()) {\n");
    source
        .append("                    eprintln!(\"unknown input {} for ")
        .append(target.vajram().name())
        .append("\", input);\n");
    source.append("                    std::process::exit(2);\n");
    source.append("                }\n");
    source.append("            }\n");
    if (completion == Completion.NOW) {
      source
          .append("            let output = ")
          .append(module)
          .append("::call(vec![")
          .append(inputs)
          .append(").into_iter().next().expect(\"outside-process Vajram result\");\n");
      source.append("            println!(\"{}\", output);\n");
    } else {
      source.append(
          "            let runtime = tokio::runtime::Builder::new_current_thread().enable_all().build().unwrap();\n");
      source.append("            let local = tokio::task::LocalSet::new();\n");
      source.append("            local.block_on(&runtime, async {\n");
      source
          .append("                let output = ")
          .append(module)
          .append("::call(vec![")
          .append(inputs)
          .append(").await.into_iter().next().expect(\"outside-process Vajram result\");\n");
      source.append("                println!(\"{}\", output);\n");
      source.append("            });\n");
    }
    source.append("        }\n");
  }

  private static boolean isCliInput(InputDecl input) {
    return !input.type().errable()
        && ("string".equals(input.type().name()) || "int".equals(input.type().name()));
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
    String argument =
        "inputs.get(\""
            + input.name()
            + "\").unwrap_or_else(|| { eprintln!(\"missing input "
            + input.name()
            + " for "
            + vajramName
            + "\"); std::process::exit(2) })";
    if ("string".equals(input.type().name())) {
      return "std::rc::Rc::new(" + argument + ".to_owned())";
    }
    return "std::rc::Rc::new("
        + argument
        + ".parse::<i64>().unwrap_or_else(|_| { eprintln!(\"invalid int input "
        + input.name()
        + " for "
        + vajramName
        + "\"); std::process::exit(2) }))";
  }
}
