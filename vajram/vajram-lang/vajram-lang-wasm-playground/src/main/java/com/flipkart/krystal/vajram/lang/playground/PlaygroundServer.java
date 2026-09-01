package com.flipkart.krystal.vajram.lang.playground;

import com.flipkart.krystal.vajram.lang.rust.cli.RustCompilerMain;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Local-only server that compiles virtual Vajram sources and serves browser-executed WASM
 * artifacts.
 */
public final class PlaygroundServer {

  private static final Object COMPILER_LOCK = new Object();
  private static final Pattern PUBLIC_FUNCTION =
      Pattern.compile("pub (?:async )?fn outside_process_([a-zA-Z0-9_]+)\\(([^)]*)\\)");
  private static final Pattern DIAGNOSTIC =
      Pattern.compile("(ERROR|WARNING) (.*?):(\\d+):(\\d+): (.*)");
  private final HttpServer server;
  private final Path workDir;
  private final Map<String, Path> artifacts = new HashMap<>();

  private PlaygroundServer(int port, Path workDir) throws IOException {
    this.workDir = workDir;
    Files.createDirectories(workDir);
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
    server.createContext("/api/compile", this::compile);
    server.createContext("/api/artifacts/", this::artifact);
    server.createContext("/api/samples", this::samples);
    server.createContext("/api/spec", this::specification);
    server.createContext("/api/health", exchange -> json(exchange, 200, Map.of("status", "ok")));
    server.createContext("/", this::staticAsset);
    server.setExecutor(Executors.newCachedThreadPool());
  }

  public static void main(String[] args) throws Exception {
    int port = 8787;
    for (int index = 0; index < args.length - 1; index++) {
      if ("--port".equals(args[index])) {
        port = Integer.parseInt(args[index + 1]);
      }
    }
    if (args.length == 1 && "--check".equals(args[0])) {
      prerequisiteCheck();
      return;
    }
    prerequisiteCheck();
    PlaygroundServer playground =
        new PlaygroundServer(
            port, Path.of(System.getProperty("java.io.tmpdir"), "vajram-playground"));
    playground.start();
    System.out.println("Vajram WASM Playground: http://127.0.0.1:" + port + "/");
  }

  private static void prerequisiteCheck() throws IOException, InterruptedException {
    requireCommand(List.of("cargo", "--version"), "Rust/Cargo");
    requireCommand(List.of("rustup", "target", "list", "--installed"), "Rust wasm target");
    String targets = commandOutput(List.of("rustup", "target", "list", "--installed"));
    if (!targets.contains("wasm32-unknown-unknown")) {
      throw new IllegalStateException("Missing Rust target wasm32-unknown-unknown");
    }
    requireCommand(List.of("wasm-bindgen", "--version"), "wasm-bindgen CLI");
  }

  private static void requireCommand(List<String> command, String name)
      throws IOException, InterruptedException {
    Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
    if (process.waitFor() != 0) {
      throw new IllegalStateException("Missing prerequisite: " + name);
    }
  }

  private static String commandOutput(List<String> command)
      throws IOException, InterruptedException {
    Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    if (process.waitFor() != 0) {
      throw new IllegalStateException("Command failed: " + String.join(" ", command));
    }
    return output;
  }

  private void start() {
    server.start();
  }

  private void compile(HttpExchange exchange) throws IOException {
    if (!"POST".equals(exchange.getRequestMethod())) {
      methodNotAllowed(exchange);
      return;
    }
    try {
      Map<String, Object> request = Json.object(readBody(exchange));
      List<Object> files = Json.array(request.get("files"));
      if (files.isEmpty()) {
        json(exchange, 400, Map.of("error", "At least one .vajram file is required"));
        return;
      }
      Path job = Files.createTempDirectory(workDir, "build-");
      Path sources = job.resolve("sources");
      for (Object entry : files) {
        Map<String, Object> file = Json.object(entry);
        String name = Json.string(file.get("name"));
        if (!name.matches("[A-Za-z0-9_./-]+\\.vajram") || name.contains("..")) {
          throw new IllegalArgumentException("Invalid Vajram file name: " + name);
        }
        Path target = sources.resolve(name).normalize();
        if (!target.startsWith(sources)) {
          throw new IllegalArgumentException("File path escapes source workspace");
        }
        Files.createDirectories(target.getParent());
        Files.writeString(target, Json.string(file.get("content")));
      }
      Path rust = job.resolve("rust/src");
      String diagnostics = invokeCompiler(sources, rust);
      if (!Files.exists(rust.resolve("wasm_dispatch.rs")) || diagnostics.contains("ERROR")) {
        json(exchange, 422, Map.of("diagnostics", diagnostics(diagnostics)));
        return;
      }
      Files.writeString(job.resolve("rust/Cargo.toml"), cargoManifest());
      CommandResult cargo =
          run(
              List.of("cargo", "build", "--release", "--target", "wasm32-unknown-unknown"),
              job.resolve("rust"));
      if (cargo.exitCode != 0) {
        json(
            exchange,
            422,
            Map.of("diagnostics", List.of(diagnostic("error", "", 1, 1, cargo.output))));
        return;
      }
      Path packageDir = job.resolve("pkg");
      Path wasm = job.resolve("rust/target/wasm32-unknown-unknown/release/vajram_playground.wasm");
      CommandResult bindings =
          run(
              List.of(
                  "wasm-bindgen",
                  "--target",
                  "web",
                  "--out-dir",
                  packageDir.toString(),
                  wasm.toString()),
              job);
      if (bindings.exitCode != 0) {
        json(
            exchange,
            422,
            Map.of("diagnostics", List.of(diagnostic("error", "", 1, 1, bindings.output))));
        return;
      }
      String id = UUID.randomUUID().toString();
      synchronized (artifacts) {
        artifacts.put(id, packageDir);
      }
      json(
          exchange,
          200,
          Map.of("artifact", id, "publicVajrams", publicVajrams(rust.resolve("wasm_dispatch.rs"))));
    } catch (IllegalArgumentException exception) {
      json(exchange, 400, Map.of("error", exception.getMessage()));
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      json(exchange, 500, Map.of("error", "Compilation interrupted"));
    } catch (Exception exception) {
      json(exchange, 500, Map.of("error", exception.getMessage()));
    }
  }

  private static String invokeCompiler(Path sources, Path output) throws IOException {
    synchronized (COMPILER_LOCK) {
      java.io.PrintStream original = System.err;
      ByteArrayOutputStream captured = new ByteArrayOutputStream();
      try (java.io.PrintStream stream =
          new java.io.PrintStream(captured, true, StandardCharsets.UTF_8)) {
        System.setErr(stream);
        RustCompilerMain.compile(sources, output, RustCompilerMain.Target.WASM);
      } finally {
        System.setErr(original);
      }
      return captured.toString(StandardCharsets.UTF_8);
    }
  }

  private static String cargoManifest() {
    return """
        [package]
        name = "vajram_playground"
        version = "0.1.0"
        edition = "2024"

        [lib]
        crate-type = ["cdylib"]

        [dependencies]
        futures = "0.3"
        wasm-bindgen = "0.2"
        wasm-bindgen-futures = "0.4"
        """;
  }

  private static CommandResult run(List<String> command, Path directory)
      throws IOException, InterruptedException {
    Process process =
        new ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true).start();
    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    return new CommandResult(process.waitFor(), output);
  }

  private static List<Map<String, Object>> publicVajrams(Path dispatcher) throws IOException {
    List<Map<String, Object>> publicVajrams = new ArrayList<>();
    Matcher matcher = PUBLIC_FUNCTION.matcher(Files.readString(dispatcher));
    while (matcher.find()) {
      List<Map<String, Object>> inputs = new ArrayList<>();
      String parameters = matcher.group(2).trim();
      if (!parameters.isEmpty()) {
        for (String parameter : parameters.split(", ")) {
          String[] parts = parameter.split(": ");
          inputs.add(Map.of("name", parts[0], "type", parts[1]));
        }
      }
      publicVajrams.add(
          Map.of(
              "name",
              matcher.group(1),
              "function",
              "outside_process_" + matcher.group(1),
              "inputs",
              inputs));
    }
    return publicVajrams;
  }

  private void artifact(HttpExchange exchange) throws IOException {
    String[] parts =
        exchange.getRequestURI().getPath().substring("/api/artifacts/".length()).split("/", 2);
    if (parts.length != 2 || !parts[1].matches("[A-Za-z0-9_.-]+")) {
      notFound(exchange);
      return;
    }
    Path artifact;
    synchronized (artifacts) {
      artifact = artifacts.get(parts[0]);
    }
    if (artifact == null) {
      notFound(exchange);
      return;
    }
    Path file = artifact.resolve(parts[1]).normalize();
    if (!file.startsWith(artifact) || !Files.isRegularFile(file)) {
      notFound(exchange);
      return;
    }
    bytes(exchange, 200, Files.readAllBytes(file), contentType(file.getFileName().toString()));
  }

  private void samples(HttpExchange exchange) throws IOException {
    Map<String, Object> all = new LinkedHashMap<>();
    for (String samplePath :
        resource("samples/index.txt").lines().filter(path -> !path.isBlank()).toList()) {
      if (!samplePath.matches("[A-Za-z0-9_./-]+\\.vajram") || samplePath.contains("..")) {
        throw new IOException("Invalid bundled sample path: " + samplePath);
      }
      String sampleName = samplePath.substring(0, samplePath.length() - ".vajram".length());
      all.put(sampleName, resource("samples/" + samplePath));
    }
    json(exchange, 200, all);
  }

  private void specification(HttpExchange exchange) throws IOException {
    bytes(
        exchange,
        200,
        resource("VAJRAM_LANGUAGE_SPEC.md").getBytes(StandardCharsets.UTF_8),
        "text/markdown; charset=utf-8");
  }

  private void staticAsset(HttpExchange exchange) throws IOException {
    String request = exchange.getRequestURI().getPath();
    String asset = "/".equals(request) ? "web/index.html" : "web" + request;
    try {
      bytes(exchange, 200, resource(asset).getBytes(StandardCharsets.UTF_8), contentType(asset));
    } catch (IllegalArgumentException exception) {
      notFound(exchange);
    }
  }

  private static String resource(String name) throws IOException {
    try (InputStream input = PlaygroundServer.class.getClassLoader().getResourceAsStream(name)) {
      if (input == null) {
        throw new IllegalArgumentException("Missing bundled resource: " + name);
      }
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static List<Map<String, Object>> diagnostics(String output) {
    List<Map<String, Object>> diagnostics = new ArrayList<>();
    for (String line : output.lines().toList()) {
      Matcher matcher = DIAGNOSTIC.matcher(line);
      if (matcher.matches()) {
        diagnostics.add(
            diagnostic(
                matcher.group(1).toLowerCase(),
                matcher.group(2),
                Integer.parseInt(matcher.group(3)),
                Integer.parseInt(matcher.group(4)),
                matcher.group(5)));
      }
    }
    if (diagnostics.isEmpty()) {
      diagnostics.add(
          diagnostic("error", "", 1, 1, output.isBlank() ? "Compilation failed" : output));
    }
    return diagnostics;
  }

  private static Map<String, Object> diagnostic(
      String severity, String file, int line, int column, String message) {
    return Map.of(
        "severity", severity, "file", file, "line", line, "column", column, "message", message);
  }

  private static void json(HttpExchange exchange, int status, Object value) throws IOException {
    bytes(
        exchange,
        status,
        Json.stringify(value).getBytes(StandardCharsets.UTF_8),
        "application/json; charset=utf-8");
  }

  private static void bytes(HttpExchange exchange, int status, byte[] body, String contentType)
      throws IOException {
    exchange.getResponseHeaders().set("Content-Type", contentType);
    exchange.getResponseHeaders().set("Cache-Control", "no-store");
    exchange.sendResponseHeaders(status, body.length);
    exchange.getResponseBody().write(body);
    exchange.close();
  }

  private static void methodNotAllowed(HttpExchange exchange) throws IOException {
    bytes(exchange, 405, new byte[0], "text/plain");
  }

  private static void notFound(HttpExchange exchange) throws IOException {
    bytes(exchange, 404, new byte[0], "text/plain");
  }

  private static String readBody(HttpExchange exchange) throws IOException {
    return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
  }

  private static String contentType(String name) {
    return name.endsWith(".js")
        ? "text/javascript; charset=utf-8"
        : name.endsWith(".css")
            ? "text/css; charset=utf-8"
            : name.endsWith(".wasm") ? "application/wasm" : "text/html; charset=utf-8";
  }

  private record CommandResult(int exitCode, String output) {}

  /** Small JSON codec keeps the standalone distribution free of an HTTP/JSON framework. */
  private static final class Json {
    private final String text;
    private int index;

    private Json(String text) {
      this.text = text;
    }

    static Map<String, Object> object(String text) {
      return object(new Json(text).value());
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> object(Object value) {
      if (!(value instanceof Map)) {
        throw new IllegalArgumentException("Expected JSON object");
      }
      return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    static List<Object> array(Object value) {
      if (!(value instanceof List)) {
        throw new IllegalArgumentException("Expected JSON array");
      }
      return (List<Object>) value;
    }

    static String string(Object value) {
      if (!(value instanceof String)) {
        throw new IllegalArgumentException("Expected JSON string");
      }
      return (String) value;
    }

    private Object value() {
      skip();
      char c = text.charAt(index);
      if (c == '{') return map();
      if (c == '[') return list();
      if (c == '"') return string();
      throw new IllegalArgumentException("Unsupported JSON value");
    }

    private Map<String, Object> map() {
      Map<String, Object> result = new LinkedHashMap<>();
      index++;
      skip();
      while (text.charAt(index) != '}') {
        String key = string();
        skip();
        expect(':');
        result.put(key, value());
        skip();
        if (text.charAt(index) == ',') {
          index++;
          skip();
        } else break;
      }
      expect('}');
      return result;
    }

    private List<Object> list() {
      List<Object> result = new ArrayList<>();
      index++;
      skip();
      while (text.charAt(index) != ']') {
        result.add(value());
        skip();
        if (text.charAt(index) == ',') {
          index++;
          skip();
        } else break;
      }
      expect(']');
      return result;
    }

    private String string() {
      expect('"');
      StringBuilder result = new StringBuilder();
      while (text.charAt(index) != '"') {
        char c = text.charAt(index++);
        if (c == '\\') {
          char escaped = text.charAt(index++);
          result.append(
              switch (escaped) {
                case 'n' -> '\n';
                case 'r' -> '\r';
                case 't' -> '\t';
                default -> escaped;
              });
        } else result.append(c);
      }
      index++;
      return result.toString();
    }

    private void expect(char expected) {
      skip();
      if (index >= text.length() || text.charAt(index++) != expected)
        throw new IllegalArgumentException("Invalid JSON");
    }

    private void skip() {
      while (index < text.length() && Character.isWhitespace(text.charAt(index))) index++;
    }

    static String stringify(Object value) {
      if (value instanceof String s)
        return '"' + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + '"';
      if (value instanceof Map<?, ?> map)
        return "{"
            + map.entrySet().stream()
                .map(e -> stringify(e.getKey().toString()) + ":" + stringify(e.getValue()))
                .collect(java.util.stream.Collectors.joining(","))
            + "}";
      if (value instanceof List<?> list)
        return "["
            + list.stream().map(Json::stringify).collect(java.util.stream.Collectors.joining(","))
            + "]";
      return value.toString();
    }
  }
}
