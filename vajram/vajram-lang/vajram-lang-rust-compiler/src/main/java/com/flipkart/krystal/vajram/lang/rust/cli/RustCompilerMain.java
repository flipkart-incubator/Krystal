package com.flipkart.krystal.vajram.lang.rust.cli;

import com.flipkart.krystal.vajram.lang.rust.ast.VajramFile;
import com.flipkart.krystal.vajram.lang.rust.codegen.Naming;
import com.flipkart.krystal.vajram.lang.rust.codegen.RustEmitter;
import com.flipkart.krystal.vajram.lang.rust.diag.Diagnostic;
import com.flipkart.krystal.vajram.lang.rust.diag.Diagnostics;
import com.flipkart.krystal.vajram.lang.rust.parse.AstBuilder;
import com.flipkart.krystal.vajram.lang.rust.plugin.AnnotationProcessorRunner;
import com.flipkart.krystal.vajram.lang.rust.resolve.Resolver;
import com.flipkart.krystal.vajram.lang.rust.resolve.SymbolTable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * CLI entry point: compiles every {@code .vajram} file under {@code --src} into idiomatic async
 * Rust under {@code --out}. Kept as a plain runnable {@code main()} for V1 - no Gradle task/plugin
 * wiring, matching the (currently empty) sibling {@code vajram-lang-compiler} module which also has
 * none.
 */
public final class RustCompilerMain {

  /** Rust artifact target selected by the CLI or the programmatic compiler API. */
  public enum Target {
    NATIVE,
    WASM;

    static Target parse(String value) {
      return switch (value) {
        case "native" -> NATIVE;
        case "wasm" -> WASM;
        default -> throw new IllegalArgumentException("Unknown Rust target: " + value);
      };
    }

    String resourceDirectory() {
      return this == NATIVE ? "native" : "wasm";
    }
  }

  private RustCompilerMain() {}

  public static void main(String[] args) throws IOException {
    Path srcDir = null;
    Path outDir = null;
    Target target = Target.NATIVE;
    for (int i = 0; i < args.length - 1; i++) {
      if ("--src".equals(args[i])) {
        srcDir = Path.of(args[i + 1]);
      } else if ("--out".equals(args[i])) {
        outDir = Path.of(args[i + 1]);
      } else if ("--target".equals(args[i])) {
        try {
          target = Target.parse(args[i + 1]);
        } catch (IllegalArgumentException e) {
          System.err.println(e.getMessage());
          System.err.println(
              "Usage: RustCompilerMain --src <dir> --out <dir> [--target native|wasm]");
          System.exit(2);
          return;
        }
      }
    }
    if (srcDir == null || outDir == null) {
      System.err.println("Usage: RustCompilerMain --src <dir> --out <dir> [--target native|wasm]");
      System.exit(2);
      return;
    }
    boolean ok = compile(srcDir, outDir, target);
    System.exit(ok ? 0 : 1);
  }

  /** Returns {@code true} on success (no diagnostic errors). Also used directly by tests. */
  public static boolean compile(Path srcDir, Path outDir) throws IOException {
    return compile(srcDir, outDir, Target.NATIVE);
  }

  /** Returns {@code true} on success (no diagnostic errors) for the selected Rust target. */
  public static boolean compile(Path srcDir, Path outDir, Target target) throws IOException {
    Diagnostics diagnostics = new Diagnostics();
    List<VajramFile> files = parseAll(srcDir, diagnostics);

    SymbolTable symbolTable = SymbolTable.build(files, diagnostics);
    Resolver.validate(files, symbolTable, diagnostics, target);

    if (diagnostics.hasErrors()) {
      printDiagnostics(diagnostics);
      return false;
    }

    Files.createDirectories(outDir);
    RustEmitter emitter = new RustEmitter(symbolTable);
    ModuleTree moduleTree = new ModuleTree();
    Map<Path, List<VajramFile>> filesBySource = new LinkedHashMap<>();
    for (VajramFile file : files) {
      filesBySource.computeIfAbsent(file.sourcePath(), unused -> new ArrayList<>()).add(file);
    }
    for (List<VajramFile> sourceFiles : filesBySource.values()) {
      VajramFile firstFile = sourceFiles.get(0);
      List<String> dir = firstFile.packageSegments().stream().map(Naming::toSnakeCase).toList();
      String moduleName = Naming.sourceModuleName(firstFile.sourcePath());
      moduleTree.register(dir, moduleName);
      Path modulePath = outDir.resolve(joinPath(dir)).resolve(moduleName + ".rs");
      writeFile(modulePath, emitter.emit(sourceFiles));
    }
    moduleTree.writeModDeclarations(outDir, target);
    copyPrelude(outDir, target);
    AnnotationProcessorRunner.process(files, outDir, symbolTable, diagnostics, target);
    if (target == Target.WASM && !Files.exists(outDir.resolve("wasm_dispatch.rs"))) {
      writeFile(outDir.resolve("wasm_dispatch.rs"), "");
    }
    runRustfmtBestEffort(outDir);
    printDiagnostics(diagnostics);
    return !diagnostics.hasErrors();
  }

  private static void printDiagnostics(Diagnostics diagnostics) {
    for (Diagnostic diagnostic : diagnostics.all()) {
      System.err.println(diagnostic);
    }
  }

  private static List<VajramFile> parseAll(Path srcDir, Diagnostics diagnostics)
      throws IOException {
    List<VajramFile> files = new ArrayList<>();
    if (!Files.isDirectory(srcDir)) {
      return files;
    }
    try (Stream<Path> paths = Files.walk(srcDir)) {
      List<Path> vajramFiles =
          paths.filter(p -> p.toString().endsWith(".vajram")).sorted().toList();
      for (Path path : vajramFiles) {
        String source = Files.readString(path);
        files.addAll(new AstBuilder(diagnostics).buildAll(path, source));
      }
    }
    return files;
  }

  private static Path joinPath(List<String> segments) {
    Path p = Path.of("");
    for (String segment : segments) {
      p = p.resolve(segment);
    }
    return p;
  }

  private static void writeFile(Path path, String contents) throws IOException {
    Files.createDirectories(path.getParent());
    Files.writeString(path, contents);
  }

  private static void copyPrelude(Path outDir, Target target) throws IOException {
    Path preludeDir = outDir.resolve("vajram_rt");
    Files.createDirectories(preludeDir);
    for (String resource : List.of("mod.rs", "errable.rs", "error.rs")) {
      try (InputStream in =
          RustCompilerMain.class.getResourceAsStream(
              "/rust-prelude/" + target.resourceDirectory() + "/" + resource)) {
        if (in == null) {
          throw new UncheckedIOException(new IOException("Missing bundled resource: " + resource));
        }
        Files.write(preludeDir.resolve(resource), in.readAllBytes());
      }
    }
  }

  /** Best-effort: if `rustfmt` isn't installed, generated code is still valid, just unformatted. */
  private static void runRustfmtBestEffort(Path outDir) {
    try (Stream<Path> paths = Files.walk(outDir)) {
      List<String> rsFiles =
          paths.filter(p -> p.toString().endsWith(".rs")).map(Path::toString).toList();
      if (rsFiles.isEmpty()) {
        return;
      }
      List<String> command = new ArrayList<>(List.of("rustfmt", "--edition", "2024"));
      command.addAll(rsFiles);
      Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
      process.getInputStream().readAllBytes();
      process.waitFor();
    } catch (IOException | InterruptedException e) {
      System.err.println(
          "Note: rustfmt not run (" + e.getMessage() + "); output left unformatted.");
    }
  }

  /** Tracks package-directory nesting so we can emit `mod.rs`/`lib.rs` declarations. */
  private static final class ModuleTree {
    private final Map<List<String>, Set<String>> childDirs = new HashMap<>();
    private final Map<List<String>, Set<String>> modulesByDir = new HashMap<>();

    void register(List<String> dir, String moduleName) {
      for (int i = 0; i < dir.size(); i++) {
        List<String> parent = dir.subList(0, i);
        childDirs.computeIfAbsent(parent, k -> new HashSet<>()).add(dir.get(i));
      }
      modulesByDir.computeIfAbsent(dir, k -> new HashSet<>()).add(moduleName);
    }

    void writeModDeclarations(Path outDir, Target target) throws IOException {
      Set<List<String>> allDirs = new HashSet<>(childDirs.keySet());
      allDirs.addAll(modulesByDir.keySet());
      for (List<String> dir : allDirs) {
        StringBuilder sb = new StringBuilder();
        if (dir.isEmpty()) {
          sb.append("pub mod vajram_rt;\n");
          if (target == Target.WASM) {
            sb.append("pub mod wasm_dispatch;\n");
          }
        }
        for (String child : childDirs.getOrDefault(dir, Set.of()).stream().sorted().toList()) {
          sb.append("pub mod ").append(child).append(";\n");
        }
        for (String module : modulesByDir.getOrDefault(dir, Set.of()).stream().sorted().toList()) {
          sb.append("pub mod ").append(module).append(";\n");
        }
        Path file = outDir.resolve(joinPath(dir)).resolve(dir.isEmpty() ? "lib.rs" : "mod.rs");
        writeFile(file, sb.toString());
      }
    }
  }
}
