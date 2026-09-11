package com.flipkart.krystal.vajram.graphql.client.codegen;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Processor;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/**
 * Shared in-process {@code javac} harness for annotation-processor tests in this module - runs
 * {@link GraphQlFacadeProcessor} (or a custom processor) over in-memory source strings and a schema
 * file, and returns the resulting diagnostics.
 */
final class CompileTestSupport {

  private CompileTestSupport() {}

  static List<Diagnostic<? extends JavaFileObject>> compile(String schema, String... sources)
      throws IOException {
    return compile(new GraphQlFacadeProcessor(), schema, sources).diagnostics();
  }

  record CompileResult(List<Diagnostic<? extends JavaFileObject>> diagnostics, Path outputDir) {}

  static CompileResult compile(Processor processor, String schema, String... sources)
      throws IOException {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

    Path moduleRoot = Files.createTempDirectory("graphql-facade-validation-test");
    moduleRoot.toFile().deleteOnExit();
    Files.writeString(moduleRoot.resolve("Schema.graphqls"), schema);

    List<JavaFileObject> sourceFiles = new ArrayList<>();
    for (String source : sources) {
      sourceFiles.add(new StringSourceFile(source));
    }

    Path outputDir = Files.createTempDirectory("graphql-facade-validation-test-out");
    outputDir.toFile().deleteOnExit();
    JavaCompiler.CompilationTask task =
        compiler.getTask(
            new StringWriter(),
            null,
            diagnostics,
            List.of(
                "-proc:only",
                "-classpath",
                System.getProperty("java.class.path"),
                "-s",
                outputDir.toString(),
                "-Akrystal.codegen.phase=MODELS",
                "-Akrystal.codegen.moduleRootPath=" + moduleRoot),
            null,
            sourceFiles);
    task.setProcessors(List.of(processor));
    task.call();
    return new CompileResult(diagnostics.getDiagnostics(), outputDir);
  }

  private static final class StringSourceFile extends SimpleJavaFileObject {
    private final String content;

    StringSourceFile(String content) {
      super(
          URI.create(
              "string:///" + extractTypeName(content).replace('.', '/') + Kind.SOURCE.extension),
          Kind.SOURCE);
      this.content = content;
    }

    private static String extractTypeName(String content) {
      String pkg =
          content.lines().filter(l -> l.trim().startsWith("package ")).findFirst().orElse("");
      String pkgName = pkg.replace("package", "").replace(";", "").trim();
      String typeName =
          content
              .lines()
              .filter(l -> l.trim().startsWith("public interface "))
              .findFirst()
              .map(l -> l.trim().split("\\s+")[2])
              .orElse("Unknown");
      return (pkgName.isEmpty() ? "" : pkgName + ".") + typeName;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
      return content;
    }
  }
}
