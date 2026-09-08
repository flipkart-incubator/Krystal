package com.flipkart.krystal.vajram.lang.rust.plugin;

import com.flipkart.krystal.vajram.lang.rust.ast.Completion;
import com.flipkart.krystal.vajram.lang.rust.ast.InputDecl;
import com.flipkart.krystal.vajram.lang.rust.ast.VajramFile;
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
    source.append("        eprintln!(\"usage: <program> <vajram-name>\");\n");
    source.append("        std::process::exit(2);\n");
    source.append("    });\n");
    source.append("    let arguments: Vec<String> = std::env::args().skip(2).collect();\n");
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

  private static void emitCase(StringBuilder source, VajramFile target, Completion completion) {
    String modulePath =
        target.packageSegments().stream()
            .map(Naming::toSnakeCase)
            .reduce((a, b) -> a + "::" + b)
            .orElse("");
    String module = modulePath + "::" + Naming.toSnakeCase(target.vajram().name());
    StringBuilder inputFields = new StringBuilder();
    for (int i = 0; i < target.vajram().inputs().size(); i++) {
      if (i > 0) {
        inputFields.append(", ");
      }
      InputDecl input = target.vajram().inputs().get(i);
      inputFields
          .append(input.name())
          .append(": ")
          .append(cliInputValue(input, i, target.vajram().name()));
    }
    String inputs =
        module
            + "::"
            + Naming.capitalize(target.vajram().name())
            + "Inputs { "
            + inputFields
            + " }";
    source.append("        \"").append(target.vajram().name()).append("\" => {\n");
    if (completion == Completion.NOW) {
      source
          .append("            let output = ")
          .append(module)
          .append("::call(")
          .append(inputs)
          .append(");\n");
      source.append("            println!(\"{}\", output);\n");
    } else {
      source.append(
          "            let runtime = tokio::runtime::Builder::new_current_thread().enable_all().build().unwrap();\n");
      source.append("            let local = tokio::task::LocalSet::new();\n");
      source.append("            local.block_on(&runtime, async {\n");
      source
          .append("                let output = ")
          .append(module)
          .append("::call(")
          .append(inputs)
          .append(").await;\n");
      source.append("                println!(\"{}\", output);\n");
      source.append("            });\n");
    }
    source.append("        }\n");
  }

  private static boolean isCliInput(InputDecl input) {
    return !input.type().errable()
        && ("string".equals(input.type().name()) || "int".equals(input.type().name()));
  }

  private static String cliInputValue(InputDecl input, int index, String vajramName) {
    String argument =
        "arguments.get("
            + index
            + ").unwrap_or_else(|| { eprintln!(\"missing input "
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
