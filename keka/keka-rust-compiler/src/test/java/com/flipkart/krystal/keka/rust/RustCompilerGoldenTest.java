package com.flipkart.krystal.keka.rust;

import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.keka.rust.ast.Callers.Caller;
import com.flipkart.krystal.keka.rust.cli.RustCompilerMain;
import com.flipkart.krystal.keka.rust.codegen.Naming;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Compiles every {@code .keka} file in {@code keka-samples} (the real, cargo-built samples) and
 * checks the generated Rust against a checked-in "golden" copy under {@code
 * src/test/resources/expected/}.
 *
 * <p>If a golden file doesn't exist yet, this test writes one and fails with instructions to review
 * and commit it - the standard low-maintenance way to bootstrap/update golden tests without
 * hand-authoring expected Rust text.
 */
class RustCompilerGoldenTest {

  private static final Path FIXTURES_DIR = Path.of("../keka-samples/src/main/keka");
  private static final Path EXPECTED_DIR = Path.of("src/test/resources/expected");
  private static final boolean UPDATE_GOLDENS = Boolean.getBoolean("updateRustGoldens");

  @Test
  void compilesAllFixturesWithoutErrors(@TempDir Path outDir) throws IOException {
    boolean ok = RustCompilerMain.compile(FIXTURES_DIR, outDir);
    assertThat(ok).as("compilation should succeed with no diagnostic errors").isTrue();
  }

  @Test
  void generatedRustMatchesGoldenFiles(@TempDir Path outDir) throws IOException {
    boolean ok = RustCompilerMain.compile(FIXTURES_DIR, outDir);
    assertThat(ok).isTrue();

    List<Path> generated = listRsFiles(outDir);
    List<String> missingGolden = new java.util.ArrayList<>();
    List<String> mismatched = new java.util.ArrayList<>();
    for (Path file : generated) {
      Path relative = outDir.relativize(file);
      Path golden = EXPECTED_DIR.resolve(relative);
      String actual = Files.readString(file);
      if (!Files.exists(golden)) {
        Files.createDirectories(golden.getParent());
        Files.writeString(golden, actual);
        missingGolden.add(relative.toString());
        continue;
      }
      String expected = Files.readString(golden);
      if (!expected.equals(actual)) {
        if (UPDATE_GOLDENS) {
          Files.writeString(golden, actual);
        } else {
          mismatched.add(relative.toString());
        }
      }
    }
    if (!missingGolden.isEmpty() && !UPDATE_GOLDENS) {
      throw new AssertionError(
          "Wrote new golden file(s), review and commit them: " + missingGolden);
    }
    assertThat(mismatched).as("generated Rust drifted from golden files").isEmpty();
  }

  @Test
  void everySampleVajramHasAGoldenFile() throws IOException {
    List<String> missing = new java.util.ArrayList<>();
    try (Stream<Path> paths = Files.walk(FIXTURES_DIR)) {
      for (Path source : paths.filter(p -> p.toString().endsWith(".keka")).toList()) {
        Path relativeDir = FIXTURES_DIR.relativize(source).getParent();
        Path golden =
            EXPECTED_DIR.resolve(relativeDir).resolve(Naming.sourceModuleName(source) + ".rs");
        if (!Files.exists(golden)) {
          missing.add(FIXTURES_DIR.relativize(source) + " -> " + EXPECTED_DIR.relativize(golden));
        }
      }
    }
    assertThat(missing).as("sample vajrams without a golden file").isEmpty();
  }

  @Test
  void emittedRustPassesRustcSyntaxCheckWhenAvailable(@TempDir Path tempDir) throws Exception {
    // A real `cargo build` (rather than a bare `rustc` invocation) since the generated crate now
    // depends on the `bumpalo` arena crate for its output arena.
    Assumptions.assumeTrue(commandAvailable("cargo"), "cargo is not available on PATH");
    Assumptions.assumeTrue(supportsRust2024(), "rustc does not support the Rust 2024 edition");
    Path cargoDir = tempDir.resolve("cargo");
    Path sourceDir = tempDir.resolve("vajram");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("hello.keka"),
        """
        package smoke;
        vajram hello(int32 count) out `now string inject (ConsoleWriter console) {
          { console.println("hello"); "hello" }
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, cargoDir.resolve("src"))).isTrue();
    Files.writeString(
        cargoDir.resolve("Cargo.toml"),
        """
        [package]
        name = "smoke"
        version = "0.1.0"
        edition = "2024"

        [dependencies]
        futures = "0.3"
        tokio = { version = "1", features = ["rt", "fs"] }
        reqwest = "0.12"
        bumpalo = "3"
        """);

    Process process =
        new ProcessBuilder("cargo", "build")
            .directory(cargoDir.toFile())
            .redirectErrorStream(true)
            .start();
    String output = new String(process.getInputStream().readAllBytes());
    assertThat(process.waitFor()).as(output).isZero();
  }

  @Test
  void permitsCrossCompletionDependenciesAndDefersAsyncDependencyUse(@TempDir Path tempDir)
      throws IOException {
    Path sourceDir = tempDir.resolve("source");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("source.keka"),
        """
        package lifecycle;
        vajram laterLeaf() out string~~ { ~~ { "leaf" } }
        """);
    Files.writeString(
        sourceDir.resolve("caller.keka"),
        """
        package lifecycle;
        vajram nowCaller() out string~~ {
          string value = laterLeaf();
          { value }
        }
        """);
    Path outDir = tempDir.resolve("out");
    assertThat(RustCompilerMain.compile(sourceDir, outDir)).isTrue();
    String caller = Files.readString(outDir.resolve("lifecycle/caller.rs"));
    assertThat(caller).contains("pub async fn call");
    assertThat(caller).contains("let _value_fut = async move {");
    assertThat(caller).contains("let value = _value_fut.clone().await;");
  }

  @Test
  void outsideProcessProcessorGeneratesAggregatingEntrypoint(@TempDir Path tempDir)
      throws IOException {
    Path sourceDir = tempDir.resolve("vajram");
    Path outDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("first.keka"),
        """
        package external;
        vajram first(string input) out `now string permit callers `outsideProcess public {
          { input }
        }
        """);
    Files.writeString(
        sourceDir.resolve("second.keka"),
        """
        package external;
        vajram second() out `now string permit callers `outsideProcess public {
          { "second" }
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir)).isTrue();
    String main = Files.readString(outDir.resolve("main.rs"));
    assertThat(main).contains("match _vajram.as_str()");
    assertThat(main).contains("usage: <program> <vajram-name> [--input value]...");
    assertThat(main).contains(".strip_prefix(\"--\")");
    assertThat(main).contains(".get(\"input\")");
    assertThat(main).contains("\"first\" =>");
    assertThat(main).contains("\"second\" =>");
    assertThat(main).contains("external::first::first::call(");
    assertThat(main).contains("FirstInputs {");
    assertThat(main).contains("AppContext::new");
    assertThat(main).contains("external::second::second::call(").contains("SecondInputs {}");
  }

  @Test
  void outsideProcessAcceptsRepeatedFlagListInputs(@TempDir Path tempDir) throws IOException {
    Path sourceDir = tempDir.resolve("vajram");
    Path outDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("join.keka"),
        """
        package external;
        vajram join(List<string> names, List<int> counts) out `now string permit callers `outsideProcess public {
          { "x" }
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir)).isTrue();
    assertThat(Files.readString(outDir.resolve("main.rs")))
        .contains(".or_default().push(_value)")
        .contains("names: _arena.alloc(_inputs.get(\"names\").map(")
        .contains(".get(\"counts\")")
        .contains(".parse::<i64>()");
  }

  @Test
  void wasmTargetGeneratesTypedOutsideProcessDispatchers(@TempDir Path tempDir) throws IOException {
    Path sourceDir = tempDir.resolve("vajram");
    Path outDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("dispatch.keka"),
        """
        package external;
        vajram greet(string name, int count) out `now string permit callers `outsideProcess public {
          { name }
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir, RustCompilerMain.Target.WASM)).isTrue();
    assertThat(Files.exists(outDir.resolve("main.rs"))).isFalse();
    assertThat(Files.readString(outDir.resolve("lib.rs"))).contains("pub mod wasm_dispatch;");
    assertThat(Files.readString(outDir.resolve("wasm_dispatch.rs")))
        .contains("use wasm_bindgen::prelude::*;")
        .contains("#[wasm_bindgen]")
        .contains("pub fn outside_process_greet(name: String, count: i64) -> String")
        .contains("name: _arena.alloc(name)")
        .contains("count: _arena.alloc(count)");
    assertThat(Files.readString(outDir.resolve("keka_rt/mod.rs")))
        .contains("WASM prelude")
        .contains("wasm_bindgen_futures::JsFuture")
        .doesNotContain("tokio::")
        .doesNotContain("every generated crate's");
  }

  @Test
  void preservesAnnotationsForEachNamedCaller() {
    var diagnostics = new com.flipkart.krystal.keka.rust.diag.Diagnostics();
    var parsed =
        new com.flipkart.krystal.keka.rust.parse.AstBuilder(diagnostics)
            .build(
                Path.of("callers.keka"),
                """
                package test;
                vajram target() out `now void permit callers `first callerA, `second callerB {
                  { }
                }
                """);

    assertThat(diagnostics.hasErrors()).as(diagnostics.all().toString()).isFalse();
    var file = parsed.orElseThrow();
    assertThat(file.vajram().callers())
        .isInstanceOf(com.flipkart.krystal.keka.rust.ast.Callers.Named.class);
    var named = (com.flipkart.krystal.keka.rust.ast.Callers.Named) file.vajram().callers();
    assertThat(named.callers())
        .containsExactly(
            new Caller(
                List.of(
                    new com.flipkart.krystal.keka.rust.ast.VajramAnnotation("first", List.of())),
                "callerA"),
            new Caller(
                List.of(
                    new com.flipkart.krystal.keka.rust.ast.VajramAnnotation("second", List.of())),
                "callerB"));
  }

  @Test
  void preservesProviderAnnotationOnVajram() {
    var diagnostics = new com.flipkart.krystal.keka.rust.diag.Diagnostics();
    var parsed =
        new com.flipkart.krystal.keka.rust.parse.AstBuilder(diagnostics)
            .build(
                Path.of("provider.keka"),
                """
                package test;
                `provider(forScope = REQUEST) vajram database() out `now Database { { new Database() } }
                """);

    assertThat(diagnostics.hasErrors()).as(diagnostics.all().toString()).isFalse();
    var annotation = parsed.orElseThrow().vajram().annotations().get(0);
    assertThat(annotation.name()).isEqualTo("provider");
    assertThat(annotation.arguments())
        .singleElement()
        .satisfies(
            argument -> {
              assertThat(argument.name()).isEqualTo("forScope");
              assertThat(argument.value())
                  .isEqualTo(new com.flipkart.krystal.keka.rust.ast.Expr.VarUse("REQUEST", false));
            });
  }

  @Test
  void parsesOrderedFieldsAndArrayExpressions() {
    var diagnostics = new com.flipkart.krystal.keka.rust.diag.Diagnostics();
    var parsed =
        new com.flipkart.krystal.keka.rust.parse.AstBuilder(diagnostics)
            .build(
                Path.of("fields.keka"),
                """
                package test;
                vajram fields() out `now void {
                  string values = ["one", "two"];
                  { }
                }
                """);

    assertThat(diagnostics.hasErrors()).as(diagnostics.all().toString()).isFalse();
    var file = parsed.orElseThrow();
    assertThat(file.vajram().computedFacets())
        .singleElement()
        .isInstanceOf(com.flipkart.krystal.keka.rust.ast.Field.class);
    var field = (com.flipkart.krystal.keka.rust.ast.Field) file.vajram().computedFacets().get(0);
    assertThat(field.value()).isInstanceOf(com.flipkart.krystal.keka.rust.ast.Expr.Array.class);
  }

  @Test
  void createsSingletonProvidersAndPropagatesContextForInjections(@TempDir Path tempDir)
      throws IOException {
    Path sourceDir = tempDir.resolve("vajram");
    Path outDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("injected.keka"),
        """
        package injections;
        vajram leaf() out `now string inject (string prefix) {
          { prefix }
        }
        vajram parent() out `now string {
          string value = leaf();
          { value }
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir)).isTrue();
    String generated = Files.readString(outDir.resolve("injections/injected.rs"));
    assertThat(generated)
        .contains("pub struct Leaf_Injections")
        .contains("prefix: Rc<dyn crate::keka_rt::Provider<String>>")
        .contains("InjectionKey::new(\"string\", &[])")
        .contains("fn instance<I: crate::keka_rt::Injector + 'static>")
        .contains("let _deps = Leaf_Injections::instance(_context);")
        .contains("::leaf::call(")
        .contains("_arena,")
        .contains("_context,")
        .contains("_deps.prefix.get()");
  }

  @Test
  void usesFullyQualifiedImportedTypeForInjectionKeys(@TempDir Path tempDir) throws IOException {
    Path sourceDir = tempDir.resolve("vajram");
    Path outDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("injected.keka"),
        """
        package injections;
        import type ConsoleWriter from lang.process;
        vajram logger() out `now void inject (ConsoleWriter writer) {
          { writer.println("hello") }
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir)).isTrue();
    assertThat(Files.readString(outDir.resolve("injections/injected.rs")))
        .contains("InjectionKey::new(\"lang.process.ConsoleWriter\", &[])");
  }

  @Test
  void emitsBareExpressionStatementsInLogicBlocks(@TempDir Path tempDir) throws IOException {
    Path sourceDir = tempDir.resolve("vajram");
    Path outDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("logger.keka"),
        """
        package test;
        vajram logger(string message) out `now void {
          { Console.log(message); }
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir)).isTrue();
    assertThat(Files.readString(outDir.resolve("test/logger.rs")))
        .contains("Console.log(_inputs.message);");
  }

  @Test
  void rejectsConflictingOutputCompletionDeclarations(@TempDir Path tempDir) throws IOException {
    for (String out :
        List.of(
            "string",
            "string~",
            "`now string",
            "string?",
            "`now string?",
            "`now string~",
            "`now string?~",
            "`now string~?",
            "string~~",
            "`now string~~",
            "`now string?~~")) {
      Path source = Files.createDirectories(tempDir.resolve("src" + out.hashCode()));
      Files.writeString(
          source.resolve("v.keka"),
          "package completion;\nvajram v() out " + out + " { { \"x\" } }\n");
      // Rejected: conflicting markers, and a redundant `?` on a soon/later (non-`now) output.
      boolean ok =
          (!out.startsWith("`now") && !out.equals("string?"))
              || out.equals("`now string")
              || out.equals("`now string?");
      assertThat(RustCompilerMain.compile(source, tempDir.resolve("out" + out.hashCode())))
          .as(out)
          .isEqualTo(ok);
    }
  }

  @Test
  void onlyBatchAndReqGroupersAreDefined(@TempDir Path tempDir) throws IOException {
    for (String grouper : List.of("batch", "req", "mod", "foo")) {
      Path source = Files.createDirectories(tempDir.resolve("src" + grouper));
      Files.writeString(
          source.resolve("v.keka"),
          "package g;\nvajram v(`batched string a) out `now void { Map<#"
              + grouper
              + ", string> m = #"
              + grouper
              + "(); { } }\n");
      boolean ok = grouper.equals("batch") || grouper.equals("req");
      assertThat(RustCompilerMain.compile(source, tempDir.resolve("out" + grouper)))
          .as(grouper)
          .isEqualTo(ok);
    }
  }

  @Test
  void callPairsRequestsOfGeneratedVajrams(@TempDir Path tempDir) throws IOException {
    String callee = "vajram leaf(string x) out string { ~{ yield x; } }\n";
    String[][] cases = {
      {"ok", "Call<_, string>... r = ...leaf(x =... xs);", "true"},
      {"okExplicit", "Call<leaf#req, string>... r = ...leaf(x =... xs);", "true"},
      {"handled", "Call<_, string?>... r = ...leaf(x =... xs);", "true"},
      {"wrongReq", "Call<other#req, string>... r = ...leaf(x =... xs);", "false"},
      {"single", "Call<_, string> r = leaf(x = a);", "true"},
      {"singleHandled", "Call<_, string?> r = leaf(x = a);", "true"},
      {"fanoutMismatch", "Call<_, string> r = ...leaf(x =... xs);", "false"},
      {"strayInfer", "_... r = ...leaf(x =... xs);", "false"},
    };
    for (String[] c : cases) {
      Path source = Files.createDirectories(tempDir.resolve("src" + c[0]));
      Files.writeString(
          source.resolve("v.keka"),
          "package c;\n"
              + callee
              + "vajram v(string a) out string {\n  string... xs = [a];\n  "
              + c[1]
              + "\n  ~{ yield a; }\n}\n");
      assertThat(RustCompilerMain.compile(source, tempDir.resolve("out" + c[0])))
          .as(c[0])
          .isEqualTo(Boolean.parseBoolean(c[2]));
    }
  }

  @Test
  void generatedTypesNeedADefinedPermittingVajramWithBatchedInputsForBatch(@TempDir Path tempDir)
      throws IOException {
    String vajrams =
        "vajram open(string x) out `now string { { x } }\n"
            + "vajram batchy(`batched string x) out `now string { { x } }\n"
            + "vajram closed(string x) out `now string permit callers somebodyElse { { x } }\n";
    String[][] cases = {
      {"req", "open#req r", "true"},
      {"batchFromOutside", "batchy#batch b", "false"},
      {"bareBatchWithoutBatched", "Map<#batch, string> m", "false"},
      {"undefined", "ghost#req r", "false"},
      {"batchWithoutBatched", "open#batch b", "false"},
      {"notPermitted", "closed#req r", "false"},
      {"bareReq", "#req r", "true"},
    };
    for (String[] c : cases) {
      Path source = Files.createDirectories(tempDir.resolve("src" + c[0]));
      Files.writeString(
          source.resolve("v.keka"),
          "package g;\n"
              + vajrams
              + "vajram user("
              + c[1]
              + ") out `now void { { } }\n"
              + "vajram batchedUser(`batched string x, #batch own, batchedUser#batch same)"
              + " out `now void { { } }\n");
      assertThat(RustCompilerMain.compile(source, tempDir.resolve("out" + c[0])))
          .as(c[0])
          .isEqualTo(Boolean.parseBoolean(c[2]));
    }
  }

  @Test
  void callerCannotCompleteEarlierThanItsDependency(@TempDir Path tempDir) throws IOException {
    String[][] cases = {
      {"`now string", "string", "false"},
      {"`now string", "string~", "false"},
      {"`now string", "string~~", "false"},
      {"string", "string~~", "false"},
      {"string", "`now string", "true"},
      {"string~~", "string~", "true"},
    };
    for (int i = 0; i < cases.length; i++) {
      Path source = Files.createDirectories(tempDir.resolve("src" + i));
      Files.writeString(
          source.resolve("v.keka"),
          "package dep;\nvajram leaf() out "
              + cases[i][1]
              + " { "
              + (cases[i][1].endsWith("~~") ? "~~ " : cases[i][1].endsWith("~") ? "~ " : "")
              + "{ \"x\" } }\nvajram caller() out "
              + cases[i][0]
              + " { string v = leaf(); { v } }\n");
      assertThat(RustCompilerMain.compile(source, tempDir.resolve("out" + i)))
          .as(String.join(" <- ", cases[i]))
          .isEqualTo(Boolean.parseBoolean(cases[i][2]));
    }
  }

  @Test
  void rejectsScalarFanoutFieldsAndMismatchedDependencyCardinality(@TempDir Path tempDir)
      throws IOException {
    Path scalarSource = tempDir.resolve("scalar");
    Files.createDirectories(scalarSource);
    Files.writeString(
        scalarSource.resolve("scalar.keka"),
        """
        package fanout;
        vajram scalar() out `now void {
          string... values = "not a collection";
          { }
        }
        """);
    assertThat(RustCompilerMain.compile(scalarSource, tempDir.resolve("scalar-out"))).isFalse();

    Path mismatchSource = tempDir.resolve("mismatch");
    Files.createDirectories(mismatchSource);
    Files.writeString(
        mismatchSource.resolve("leaf.keka"),
        """
        package fanout;
        vajram leaf() out `now string { { "leaf" } }
        """);
    Files.writeString(
        mismatchSource.resolve("parent.keka"),
        """
        package fanout;
        vajram parent() out `now void {
          string... values = leaf();
          { }
        }
        """);
    assertThat(RustCompilerMain.compile(mismatchSource, tempDir.resolve("mismatch-out"))).isFalse();
  }

  @Test
  void lowersReadFileAsStringToTokioUtf8Io(@TempDir Path tempDir) throws IOException {
    Path sourceDir = tempDir.resolve("vajram");
    Path outDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("reader.keka"),
        """
        package system;
        import vajram readFileAsString from lang.fileSystem;
        vajram reader(string filePath) out string permit callers public {
          string content = readFileAsString(path = filePath);
          out ~ { content }
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir)).isTrue();
    String reader = Files.readString(outDir.resolve("system/reader.rs"));
    assertThat(reader).contains("tokio::fs::read_to_string(_inputs.filePath.as_str())");
    assertThat(reader).contains("let _content_fut = async move {");
  }

  @Test
  void lowersCallHttpToReqwestNonBlockingGet(@TempDir Path tempDir) throws IOException {
    Path sourceDir = tempDir.resolve("vajram");
    Path outDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("fetcher.keka"),
        """
        package system;
        import vajram callHttp from lang.net;
        vajram fetcher(string requestUrl) out string permit callers public {
          string body = callHttp(url = requestUrl);
          out ~ { body }
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir)).isTrue();
    String fetcher = Files.readString(outDir.resolve("system/fetcher.rs"));
    assertThat(fetcher)
        .contains("reqwest::Client::new()")
        .contains(".get(_inputs.requestUrl.as_str())")
        .contains(".header(reqwest::header::USER_AGENT,");
    assertThat(fetcher)
        .contains(".text()")
        .contains(".map_err(|_e| VajramError::from(_e.to_string()))");
    assertThat(fetcher).contains("let _body_fut = async move {");
  }

  @Test
  void lowersCallHttpToBrowserFetchOnWasmTarget(@TempDir Path tempDir) throws IOException {
    Path sourceDir = tempDir.resolve("vajram");
    Path outDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("fetcher.keka"),
        """
        package system;
        import vajram callHttp from lang.net;
        vajram fetcher(string requestUrl) out string permit callers public {
          string body = callHttp(url = requestUrl);
          out ~ { body }
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir, RustCompilerMain.Target.WASM)).isTrue();
    String fetcher = Files.readString(outDir.resolve("system/fetcher.rs"));
    assertThat(fetcher)
        .contains("crate::keka_rt::fetch_text(_inputs.requestUrl.as_str())")
        .contains(".await");
  }

  @Test
  void rejectsReadFileAsStringForWasmWithoutEmittingTokioFilesystemIo(@TempDir Path tempDir)
      throws IOException {
    Path sourceDir = tempDir.resolve("vajram");
    Path outDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("reader.keka"),
        """
        package system;
        import vajram readFileAsString from lang.fileSystem;
        vajram reader(string filePath) out `now string {
          out readFileAsString(path = filePath);
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir, RustCompilerMain.Target.WASM)).isFalse();
    assertThat(Files.exists(outDir.resolve("system/reader.rs"))).isFalse();
  }

  @Test
  void wasmTargetGeneratesAsyncDispatcherForAsyncGraphs(@TempDir Path tempDir) throws IOException {
    Path sourceDir = tempDir.resolve("vajram");
    Path outDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("dispatch.keka"),
        """
        package external;
        vajram greet(string name) out string permit callers `outsideProcess public {
          ~ { name }
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir, RustCompilerMain.Target.WASM)).isTrue();
    assertThat(Files.readString(outDir.resolve("wasm_dispatch.rs")))
        .contains("pub async fn outside_process_greet(name: String) -> String")
        .contains("::call(")
        .contains(".await")
        .contains(".into_iter()");
  }

  @Test
  void startsIndependentFacetsBeforeTheirComputedFieldTasks(@TempDir Path tempDir)
      throws IOException {
    Path sourceDir = tempDir.resolve("vajram");
    Path outDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("graph.keka"),
        """
        package graph;
        vajram firstLeaf() out string { ~ { "first" } }
        vajram secondLeaf() out string { ~ { "second" } }
        vajram parent() out string {
          string first = firstLeaf();
          string second = secondLeaf();
          string firstValue = first + "";
          string secondValue = second + "";
          out ~ { firstValue }
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir)).isTrue();
    String parent = Files.readString(outDir.resolve("graph/graph.rs"));
    // `first`/`second` are independent, and each is wrapped as a `Shared` future so it can be
    // awaited by exactly the facet(s) that need it. `firstValue` only clones+awaits `_first_fut`
    // (never `_second_fut`), and `secondValue` only clones+awaits `_second_fut` - so neither of
    // them can be blocked by the other's dependency, unlike a level-grouped join.
    assertThat(parent).contains("let _first_fut = async move {");
    assertThat(parent).contains("let _second_fut = async move {");
    assertThat(parent)
        .contains(
            "        let _firstValue_needs_first = _first_fut.clone();\n"
                + "        let _firstValue_fut = async move {\n"
                + "            let first = _firstValue_needs_first.await;\n")
        .contains(
            "        let _secondValue_needs_second = _second_fut.clone();\n"
                + "        let _secondValue_fut = async move {\n"
                + "            let second = _secondValue_needs_second.await;\n")
        // Per-input entries are bound from the awaited facet vectors inside the batch loop.
        .contains("let first = match Clone::clone(&first[__i])")
        .contains("Ok(&*_arena.alloc(first.to_string() + \"\"))")
        .doesNotContain("_secondValue_needs_first")
        .doesNotContain("_firstValue_needs_second")
        .contains(
            "let (first, second, firstValue, secondValue) = futures::join!(_first_fut.clone(),"
                + " _second_fut.clone(), _firstValue_fut.clone(), _secondValue_fut.clone());");
    assertThat(parent.indexOf("let _second_fut ="))
        .isLessThan(parent.indexOf("let _firstValue_fut ="));
  }

  @Test
  void eachFacetAwaitsOnlyItsOwnDependenciesNotUnrelatedSiblings(@TempDir Path tempDir)
      throws IOException {
    Path sourceDir = tempDir.resolve("vajram");
    Path outDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("diamond.keka"),
        """
        package diamond;
        vajram leafA() out string { ~ { "a" } }
        vajram leafB() out string { ~ { "b" } }
        vajram leafC() out string { ~ { "c" } }
        vajram diamond() out string {
          string a = leafA();
          string b = leafB();
          string c = leafC();
          string combined = a + b;
          out ~ { combined }
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir)).isTrue();
    String diamond = Files.readString(outDir.resolve("diamond/diamond.rs"));
    // `combined` depends on both `a` and `b`, but not on the unrelated independent `c` - it must
    // clone+await exactly `_a_fut` and `_b_fut`, and never reference `_c_fut` at all. This is the
    // case the old level-grouped `join!` got wrong: `c` would have shared a level with `a`/`b`
    // and `combined` would have been forced to wait on it regardless.
    assertThat(diamond).contains("let _a_fut = async move {");
    assertThat(diamond).contains("let _b_fut = async move {");
    assertThat(diamond).contains("let _c_fut = async move {");
    assertThat(diamond)
        .contains("let _combined_needs_a = _a_fut.clone();")
        .contains("let _combined_needs_b = _b_fut.clone();")
        .contains("let _combined_fut = async move {\n")
        .contains("let a = _combined_needs_a.await;\n")
        .contains("let b = _combined_needs_b.await;\n")
        .contains("let a = match Clone::clone(&a[__i])")
        .contains("Ok(&*_arena.alloc(a.to_string() + b))");
    String combinedBody =
        diamond.substring(
            diamond.indexOf("let _combined_fut ="), diamond.indexOf("let (a, b, c, combined)"));
    assertThat(combinedBody).doesNotContain("_c_fut");
    assertThat(diamond)
        .contains(
            "let (a, b, c, combined) = futures::join!(_a_fut.clone(), _b_fut.clone(),"
                + " _c_fut.clone(), _combined_fut.clone());");
  }

  @Test
  void lowersStringConcatenationWithStringInputsToRustStringSlices(@TempDir Path tempDir)
      throws IOException {
    Path sourceDir = tempDir.resolve("vajram");
    Path outDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("hello.keka"),
        """
        package hello;
        vajram greet(string name) out `now string {
          { "Hello, " + name }
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir)).isTrue();
    assertThat(Files.readString(outDir.resolve("hello/hello.rs")))
        .contains("\"Hello, \".to_string() + _inputs.name.as_str()");
  }

  @Test
  void lowersChainedStringConcatenationWithTrailingStringLiterals(@TempDir Path tempDir)
      throws IOException {
    Path sourceDir = tempDir.resolve("vajram");
    Path outDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("hello.keka"),
        """
        package hello;
        vajram greet(string name) out `now string {
          { "Hello, " + name + "!" }
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir)).isTrue();
    assertThat(Files.readString(outDir.resolve("hello/hello.rs")))
        .contains("\"Hello, \".to_string() + _inputs.name.as_str() + \"!\"");
  }

  @Test
  void lowersNumericPrimitiveAdditionWithDereferencedBoundaryValues(@TempDir Path tempDir)
      throws IOException {
    Path sourceDir = tempDir.resolve("vajram");
    Path outDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("math.keka"),
        """
        package math;
        vajram sumInts(int left, int right) out `now int { { left + right } }
        vajram sumFloats(float left, float right) out `now float { { left + right } }
        vajram sumDoubles(double left, double right) out `now double { { left + right } }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir)).isTrue();
    String generated = Files.readString(outDir.resolve("math/math.rs"));
    assertThat(generated).contains("_arena.alloc(*_inputs.left + *_inputs.right)");
  }

  @Test
  void lowersConcatStringsToRustStringJoin(@TempDir Path tempDir) throws IOException {
    Path sourceDir = tempDir.resolve("vajram");
    Path outDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("join.keka"),
        """
        package system;
        import vajram concatStrings from lang.strings;
        vajram join(string separator) out `now string {
          string values = ["one", "two"];
          out concatStrings(strings = values; separator = separator);
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir)).isTrue();
    String join = Files.readString(outDir.resolve("system/join.rs"));
    assertThat(join)
        .contains("values")
        .contains("value.as_str()")
        .contains("collect::<Vec<_>>()")
        .contains("join(_inputs.separator.as_str())");
  }

  @Test
  void emitsOneRustFileForMultipleVajramsInOneSourceFile(@TempDir Path tempDir) throws IOException {
    Path sourceDir = tempDir.resolve("vajram");
    Path outDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("combined.keka"),
        """
        package combined;
        vajram leaf() out `now string { { "leaf" } }

        vajram caller() out `now string {
          string value = leaf();
          { value }
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir)).isTrue();
    assertThat(Files.readString(outDir.resolve("combined/mod.rs"))).contains("pub mod combined;");
    assertThat(Files.exists(outDir.resolve("combined/leaf.rs"))).isFalse();
    assertThat(Files.exists(outDir.resolve("combined/caller.rs"))).isFalse();
    assertThat(Files.readString(outDir.resolve("combined/combined.rs")))
        .contains("pub struct LeafInputs")
        .contains("pub struct CallerInputs")
        .contains("crate::combined::combined::leaf::call(")
        .contains("crate::combined::combined::leaf::LeafInputs {}")
        .doesNotContain("call_one");
  }

  @Test
  void batchesFanoutDependencyInputsIntoOneCalleeInvocation(@TempDir Path tempDir)
      throws IOException {
    Path sourceDir = tempDir.resolve("vajram");
    Path outDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("fanout.keka"),
        """
        package fanout;
        vajram leaf(string value) out `now string { { value } }
        vajram parent() out `now List<string> {
          string... values = ["one", "two"];
          string... results = leaf(value =... values);
          { results }
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir)).isTrue();
    String generated = Files.readString(outDir.resolve("fanout/fanout.rs"));
    assertThat(generated)
        .contains("leaf::call(")
        .contains("values")
        .contains(".into_iter()")
        .contains(".map(|_it|")
        .contains("LeafInputs {")
        .contains("value: _arena.alloc(_it)")
        .doesNotContain("futures::future::join_all");
  }

  @Test
  void parsesAnnotationDefinitions() {
    var diagnostics = new com.flipkart.krystal.keka.rust.diag.Diagnostics();
    var parsed =
        new com.flipkart.krystal.keka.rust.parse.AstBuilder(diagnostics)
            .buildSource(
                Path.of("defs.keka"),
                """
                package test;
                `target(kinds = [VAJRAM]) `type table(string name, bool indexed = false, [int32] sizes = [1, 2 * 3],)
                `type marker()
                `type flag
                vajram v() out `now void { { } }
                """);

    assertThat(diagnostics.hasErrors()).as(diagnostics.all().toString()).isFalse();
    assertThat(parsed.vajrams()).hasSize(1);
    var table = parsed.annotationDefs().get(0).definition();
    assertThat(table.name()).isEqualTo("table");
    assertThat(table.metaAnnotations()).extracting(a -> a.name()).containsExactly("target");
    assertThat(table.parameters())
        .extracting(p -> p.name())
        .containsExactly("name", "indexed", "sizes");
    assertThat(table.parameters().get(0).defaultValue()).isNull();
    assertThat(table.parameters().get(2).type().name()).isEqualTo("List");
    assertThat(parsed.annotationDefs().get(1).definition().parameters()).isEmpty();
    assertThat(parsed.annotationDefs().get(2).definition().name()).isEqualTo("flag");
    assertThat(parsed.annotationDefs().get(2).definition().parameters()).isEmpty();
  }

  @Test
  void annotationArgumentsMustBeConstantSyntax() {
    for (String argument : List.of("foo()", "new Foo()", "a.b()", "not true", "#req")) {
      var diagnostics = new com.flipkart.krystal.keka.rust.diag.Diagnostics();
      new com.flipkart.krystal.keka.rust.parse.AstBuilder(diagnostics)
          .buildSource(
              Path.of("args.keka"),
              "package test;\n`tag(value = " + argument + ") vajram v() out `now void { { } }\n");
      assertThat(diagnostics.hasErrors()).as(argument).isTrue();
    }
  }

  @Test
  void validatesUsesOfDefinedAnnotations() {
    String definitions =
        "package test;\n"
            + "`target(kinds = [VAJRAM]) `type table(string name, bool indexed = false, [int32] sizes = [1])\n"
            + "`target(kinds = [INPUT]) `type column(string name)\n"
            + "`type untargeted()\n";
    record Case(String use, String expectedError) {}
    for (Case testCase :
        List.of(
            new Case("`table(name = \"a\" + \"b\", sizes = [2 * 3, 4])", null),
            new Case("`table(name = \"a\", indexed = true)", null),
            new Case("`table(indexed = true)", "missing required parameter 'name'"),
            new Case("`table(name = \"a\", color = \"red\")", "has no parameter 'color'"),
            new Case("`table(name = \"a\", name = \"b\")", "more than once"),
            new Case("`table(name = 5)", "must be a constant of type 'string'"),
            new Case(
                "`table(name = \"a\", sizes = [\"x\"])", "must be a constant of type '[int32]'"),
            new Case(
                "`table(name = \"a\", sizes = [3000000000])",
                "must be a constant of type '[int32]'"),
            new Case("`table(name = SOME_NAME)", "must be a constant of type 'string'"),
            new Case("`table(name = \"a\" * 2)", "operands it does not support"),
            new Case("`column(name = \"a\")", "cannot be applied to a vajram"),
            new Case("`untargeted()", "allowed nowhere"),
            new Case("`unknownBuiltin(anything = 1)", null))) {
      var diagnostics = new com.flipkart.krystal.keka.rust.diag.Diagnostics();
      var parsed =
          new com.flipkart.krystal.keka.rust.parse.AstBuilder(diagnostics)
              .buildSource(
                  Path.of("uses.keka"),
                  definitions + testCase.use() + " vajram v() out `now void { { } }\n");
      assertThat(diagnostics.hasErrors()).as(diagnostics.all().toString()).isFalse();
      var table =
          com.flipkart.krystal.keka.rust.resolve.SymbolTable.build(
              parsed.vajrams(), parsed.annotationDefs(), diagnostics);
      com.flipkart.krystal.keka.rust.resolve.Resolver.validate(
          parsed.vajrams(), table, diagnostics);
      if (testCase.expectedError() == null) {
        assertThat(diagnostics.hasErrors()).as(testCase.use() + diagnostics.all()).isFalse();
      } else {
        assertThat(diagnostics.all().toString())
            .as(testCase.use())
            .contains(testCase.expectedError());
      }
    }
  }

  @Test
  void checksAnnotationTargetsAtEverySite() {
    String template =
        """
        @@PACKAGE@@ package test;
        `target(kinds = [PACKAGE]) `type pkgTag
        `target(kinds = [VAJRAM]) `type vajTag
        `target(kinds = [INPUT]) `type inTag
        `target(kinds = [OUTPUT]) `type outTag
        `target(kinds = [FACET]) `type facetTag
        `target(kinds = [DEPENDENCY]) `type depTag
        `target(kinds = [CALLERS]) `type callersTag
        `target(kinds = [INJECTION]) `type injTag
        vajram other(string a) out `now string { { a } }
        @@VAJRAM@@ vajram v(@@INPUT@@ string a) out @@OUTPUT@@ `now string
            inject (@@INJECTION@@ Logger logger) permit callers @@CALLERS@@ public {
          @@FACET@@ string x = "a";
          @@DEPENDENCY@@ string d = other(a = "x");
          { d }
        }
        """;
    java.util.Map<String, String> correct =
        java.util.Map.of(
            "PACKAGE", "`pkgTag",
            "VAJRAM", "`vajTag",
            "INPUT", "`inTag",
            "OUTPUT", "`outTag",
            "FACET", "`facetTag",
            "DEPENDENCY", "`depTag",
            "CALLERS", "`callersTag",
            "INJECTION", "`injTag");
    java.util.Map<String, String> siteNames =
        java.util.Map.of(
            "PACKAGE", "a package",
            "VAJRAM", "a vajram",
            "INPUT", "an input",
            "OUTPUT", "an output",
            "FACET", "a facet",
            "DEPENDENCY", "a dependency",
            "CALLERS", "a callers declaration",
            "INJECTION", "an injection");
    // Every annotation at its own site is accepted.
    assertThat(diagnoseSource(fill(template, correct)).all().toString()).isEqualTo("[]");
    // A wrong-target annotation at any one site is rejected, naming that site.
    for (String site : correct.keySet()) {
      java.util.Map<String, String> wrong = new java.util.HashMap<>(correct);
      wrong.put(site, site.equals("INPUT") ? "`facetTag" : "`inTag");
      assertThat(diagnoseSource(fill(template, wrong)).all().toString())
          .as(site)
          .contains("cannot be applied to " + siteNames.get(site));
    }
  }

  @Test
  void parsesModels() {
    var diagnostics = new com.flipkart.krystal.keka.rust.diag.Diagnostics();
    var parsed =
        new com.flipkart.krystal.keka.rust.parse.AstBuilder(diagnostics)
            .buildSource(
                Path.of("models.keka"),
                """
                package test;
                import model Money from other.pkg;
                model Money(int64 amountMinor; string currency = "USD";)
                model Account(
                  `idField string id;
                  `range(min = 0) int32 tier = 1 * 2;
                  string? nickname;
                  List<Money> history
                )
                model Empty()
                vajram v() out `now void {
                  Account a = new Account(id = "x"; history = []);
                  { }
                }
                """);

    assertThat(diagnostics.hasErrors()).as(diagnostics.all().toString()).isFalse();
    assertThat(parsed.models())
        .extracting(m -> m.model().name())
        .containsExactly("Money", "Account", "Empty");
    var account = parsed.models().get(1).model();
    assertThat(account.isEntity()).isTrue();
    assertThat(account.idFields()).extracting(f -> f.name()).containsExactly("id");
    assertThat(parsed.models().get(0).model().isEntity()).isFalse();
    assertThat(account.fields().get(1).annotations())
        .extracting(a -> a.name())
        .containsExactly("range");
    assertThat(account.fields().get(1).defaultValue())
        .isInstanceOf(com.flipkart.krystal.keka.rust.ast.Expr.BinaryOp.class);
    assertThat(account.fields().get(2).type().errable()).isTrue();
    assertThat(account.fields().get(3).type().name()).isEqualTo("List");
    assertThat(parsed.models().get(2).model().fields()).isEmpty();
    var field =
        (com.flipkart.krystal.keka.rust.ast.Field)
            parsed.vajrams().get(0).vajram().computedFacets().get(0);
    assertThat(field.value()).isInstanceOf(com.flipkart.krystal.keka.rust.ast.Expr.NewModel.class);
    var newModel = (com.flipkart.krystal.keka.rust.ast.Expr.NewModel) field.value();
    assertThat(newModel.arguments()).extracting(a -> a.name()).containsExactly("id", "history");
  }

  @Test
  void validatesModels() {
    record Case(String source, String expectedError) {}
    String money = "model Money(int64 amountMinor; string currency = \"USD\")\n";
    String vajram = "vajram v() out `now void { { } }\n";
    for (Case testCase :
        List.of(
            new Case(
                money
                    + "model Account(`idField string id; Money balance; string? nick; List<Money> hist; Map<string, Money> m; int32 t = 1 + 2; float64 f = 3)\n"
                    + vajram,
                null),
            new Case("model A(string x; string x)\n" + vajram, "declares field 'x' more than once"),
            new Case("model A(Foo x)\n" + vajram, "Unknown type 'Foo'"),
            new Case("model A(#req x)\n" + vajram, "cannot be a generated type"),
            new Case("model A(List<string, string> x)\n" + vajram, "takes 1 type argument"),
            new Case("model A(string<int32> x)\n" + vajram, "takes no type arguments"),
            new Case("model A(int32 x = \"s\")\n" + vajram, "must be a constant of type 'int32'"),
            new Case(
                money + "model A(Money m = 5)\n" + vajram, "defaults are limited to primitives"),
            new Case("model A(B b)\nmodel B(A a)\n" + vajram, "contains itself (A -> B -> A)"),
            new Case("model A(A? self)\n" + vajram, "contains itself (A -> A)"),
            new Case("model A(List<A> children)\n" + vajram, null),
            new Case("model A(`idField float64 id)\n" + vajram, "must be hashable"),
            new Case("model A(`idField string? id)\n" + vajram, "cannot be errable"),
            new Case("model A(`idField Map<string, string> id)\n" + vajram, "must be hashable"),
            new Case("model A(Map<float64, string> m)\n" + vajram, "must be hashable"),
            new Case("model A(string x)\nmodel A(string y)\n" + vajram, "Duplicate model name 'A'"),
            new Case("model v(string x)\n" + vajram, "clashes with the model of the same name"),
            new Case("`idField model A(string x)\n" + vajram, "cannot be applied to a model"),
            new Case("`type idField()\n" + vajram, "built into the language"),
            new Case(
                "vajram w(`idField string a) out `now void { { } }\n",
                "cannot be applied to an input"),
            new Case(
                money + "vajram w() out `now void { Money m = new Money(amountMinor = 1); { } }\n",
                null),
            new Case(
                money
                    + "vajram w() out `now void { Money m = new Money(amountMinor = 1; color = 2); { } }\n",
                "has no field 'color'"),
            new Case(
                money
                    + "vajram w() out `now void { Money m = new Money(amountMinor = 1; amountMinor = 2); { } }\n",
                "is set twice"),
            new Case(
                money
                    + "vajram w() out `now void { Money m = new Money(currency = \"EUR\"); { } }\n",
                "Field 'amountMinor' of model 'Money' has no default"),
            new Case(
                money + "vajram w() out `now void { Money m = new Money(5); { } }\n",
                "Create model 'Money' with named fields"),
            new Case(
                money + "vajram w() out `now void { Money m = new Money(); { } }\n",
                "Field 'amountMinor' of model 'Money' has no default"),
            new Case(
                "model D(int32 a = 1; string b = \"x\")\nvajram w() out `now void { D d = new D(); { } }\n",
                null),
            new Case(
                "vajram w() out `now void { Nope n = new Nope(a = 1); { } }\n",
                "Unknown model 'Nope'"))) {
      var diagnostics = diagnoseSource("package test;\n" + testCase.source());
      if (testCase.expectedError() == null) {
        assertThat(diagnostics.all().toString()).as(testCase.source()).isEqualTo("[]");
      } else {
        assertThat(diagnostics.all().toString())
            .as(testCase.source())
            .contains(testCase.expectedError());
      }
    }
  }

  private static String fill(String template, java.util.Map<String, String> values) {
    String result = template;
    for (var entry : values.entrySet()) {
      result = result.replace("@@" + entry.getKey() + "@@", entry.getValue());
    }
    return result;
  }

  private static com.flipkart.krystal.keka.rust.diag.Diagnostics diagnoseSource(String source) {
    var diagnostics = new com.flipkart.krystal.keka.rust.diag.Diagnostics();
    var parsed =
        new com.flipkart.krystal.keka.rust.parse.AstBuilder(diagnostics)
            .buildSource(Path.of("sites.keka"), source);
    var table =
        com.flipkart.krystal.keka.rust.resolve.SymbolTable.build(
            parsed.vajrams(), parsed.annotationDefs(), parsed.models(), diagnostics);
    com.flipkart.krystal.keka.rust.resolve.Resolver.validate(parsed.vajrams(), table, diagnostics);
    return diagnostics;
  }

  @Test
  void rejectsInvalidAnnotationDefinitions() {
    record Case(String definition, String expectedError) {}
    for (Case testCase :
        List.of(
            new Case("`type a(string x, string x)", "more than once"),
            new Case("`type a(Foo x)", "unsupported type 'Foo'"),
            new Case("`type a(int32 x = \"s\")", "must be a constant of type 'int32'"),
            new Case("`type a(int32 x = foo)", "must be a constant of type 'int32'"),
            new Case("`target(kinds = [NOWHERE]) `type a()", "Unknown annotation target"),
            new Case("`target(where = [VAJRAM]) `type a()", "takes one parameter"),
            new Case("`type a() `type a()", "Duplicate annotation name 'a'"))) {
      var diagnostics = new com.flipkart.krystal.keka.rust.diag.Diagnostics();
      var parsed =
          new com.flipkart.krystal.keka.rust.parse.AstBuilder(diagnostics)
              .buildSource(
                  Path.of("bad.keka"),
                  "package test;\n"
                      + testCase.definition()
                      + "\nvajram v() out `now void { { } }\n");
      var table =
          com.flipkart.krystal.keka.rust.resolve.SymbolTable.build(
              parsed.vajrams(), parsed.annotationDefs(), diagnostics);
      com.flipkart.krystal.keka.rust.resolve.Resolver.validate(
          parsed.vajrams(), table, diagnostics);
      assertThat(diagnostics.all().toString())
          .as(testCase.definition())
          .contains(testCase.expectedError());
    }
  }

  private static boolean commandAvailable(String command) {
    try {
      Process process = new ProcessBuilder(command, "--version").start();
      return process.waitFor() == 0;
    } catch (IOException e) {
      return false;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  private static boolean supportsRust2024() {
    try {
      Process process = new ProcessBuilder("rustc", "--version").redirectErrorStream(true).start();
      String version = new String(process.getInputStream().readAllBytes());
      if (process.waitFor() != 0) {
        return false;
      }
      String[] parts = version.split("\\s+");
      String[] numbers = parts.length > 1 ? parts[1].split("\\.") : new String[0];
      return numbers.length >= 2
          && Integer.parseInt(numbers[0]) == 1
          && Integer.parseInt(numbers[1]) >= 85;
    } catch (IOException e) {
      return false;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  private static List<Path> listRsFiles(Path dir) throws IOException {
    try (Stream<Path> paths = Files.walk(dir)) {
      return paths
          .filter(p -> p.toString().endsWith(".rs"))
          .sorted(Comparator.naturalOrder())
          .toList();
    }
  }
}
