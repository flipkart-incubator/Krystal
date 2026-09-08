package com.flipkart.krystal.vajram.lang.rust.plugin;

import com.flipkart.krystal.vajram.lang.rust.ast.VajramFile;
import com.flipkart.krystal.vajram.lang.rust.diag.Diagnostics;
import com.flipkart.krystal.vajram.lang.rust.resolve.SymbolTable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/** Discovers annotation processors and invokes each in its declared processing mode. */
public final class AnnotationProcessorRunner {

  private AnnotationProcessorRunner() {}

  public static void process(
      List<VajramFile> files, Path outputDir, SymbolTable symbolTable, Diagnostics diagnostics)
      throws IOException {
    List<VajramAnnotationProcessor> processors = new ArrayList<>();
    processors.add(new OutsideProcessAnnotationProcessor());
    ServiceLoader.load(VajramAnnotationProcessor.class).forEach(processors::add);
    for (VajramAnnotationProcessor processor : processors) {
      List<VajramFile> matching =
          files.stream()
              .filter(
                  file ->
                      file.vajram().callers() != null
                          && file.vajram().callers().annotations().stream()
                              .anyMatch(processor.supportedAnnotations()::contains))
              .toList();
      if (matching.isEmpty()) {
        continue;
      }
      if (processor.processingMode() == ProcessingMode.ISOLATED) {
        for (VajramFile file : matching) {
          processor.process(
              new AnnotationProcessorContext(
                  List.of(file), files, outputDir, symbolTable, diagnostics));
        }
      } else {
        processor.process(
            new AnnotationProcessorContext(matching, files, outputDir, symbolTable, diagnostics));
      }
    }
  }
}
