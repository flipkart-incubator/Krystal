package com.flipkart.krystal.vajram.lang.rust.plugin;

import com.flipkart.krystal.vajram.lang.rust.ast.SourceLocation;
import com.flipkart.krystal.vajram.lang.rust.ast.VajramFile;
import com.flipkart.krystal.vajram.lang.rust.cli.RustCompilerMain.Target;
import com.flipkart.krystal.vajram.lang.rust.diag.Diagnostics;
import com.flipkart.krystal.vajram.lang.rust.resolve.SymbolTable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Services and source views provided to an annotation processor invocation. */
public final class AnnotationProcessorContext {

  private final List<VajramFile> matchingVajrams;
  private final List<VajramFile> compilationVajrams;
  private final Path outputDir;
  private final SymbolTable symbolTable;
  private final Diagnostics diagnostics;
  private final Target target;

  public AnnotationProcessorContext(
      List<VajramFile> matchingVajrams,
      List<VajramFile> compilationVajrams,
      Path outputDir,
      SymbolTable symbolTable,
      Diagnostics diagnostics,
      Target target) {
    this.matchingVajrams = List.copyOf(matchingVajrams);
    this.compilationVajrams = List.copyOf(compilationVajrams);
    this.outputDir = outputDir;
    this.symbolTable = symbolTable;
    this.diagnostics = diagnostics;
    this.target = target;
  }

  public List<VajramFile> matchingVajrams() {
    return matchingVajrams;
  }

  public List<VajramFile> compilationVajrams() {
    return compilationVajrams;
  }

  public SymbolTable symbolTable() {
    return symbolTable;
  }

  public Target target() {
    return target;
  }

  public void error(SourceLocation location, String message) {
    diagnostics.error(location, message);
  }

  /** Writes a generated file below the compiler output directory. */
  public void writeFile(Path relativePath, String content) throws IOException {
    Path target = outputDir.resolve(relativePath).normalize();
    if (!target.startsWith(outputDir.normalize())) {
      throw new IOException(
          "Annotation processor attempted to write outside output: " + relativePath);
    }
    Files.createDirectories(target.getParent());
    Files.writeString(target, content);
  }
}
