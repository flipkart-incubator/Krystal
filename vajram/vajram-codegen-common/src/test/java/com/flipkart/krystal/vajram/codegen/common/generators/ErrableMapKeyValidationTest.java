package com.flipkart.krystal.vajram.codegen.common.generators;

import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.codegen.common.processors.ModelGenProcessor;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;

/**
 * Verifies (via in-process annotation processing) that a {@code @ModelRoot} field of type {@code
 * Map<Errable<K>, V>} is rejected at compile time - {@code Errable} is not allowed as a Map key
 * type, since it is not a valid hash/equality key (it can represent absence/failure).
 */
class ErrableMapKeyValidationTest {

  @Test
  void mapWithErrableKey_failsCompilationWithExpectedMessage() {
    String source =
        """
        package com.flipkart.krystal.vajram.codegen.common.generators.testfixtures;

        import com.flipkart.krystal.data.Errable;
        import com.flipkart.krystal.model.Model;
        import com.flipkart.krystal.model.ModelRoot;
        import java.util.Map;

        import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

        @ModelRoot(type = RESPONSE)
        public interface BadMapKeyModel extends Model {
          Map<Errable<String>, String> badField();
        }
        """;

    List<Diagnostic<? extends JavaFileObject>> diagnostics = compile(source);

    assertThat(
            diagnostics.stream()
                .filter(d -> d.getKind() == Kind.ERROR)
                .anyMatch(
                    d -> d.getMessage(null).contains("Errable is not allowed as a Map key type")))
        .isTrue();
  }

  @Test
  void mapWithStringKey_compilesWithoutErrableKeyError() {
    String source =
        """
        package com.flipkart.krystal.vajram.codegen.common.generators.testfixtures;

        import com.flipkart.krystal.data.Errable;
        import com.flipkart.krystal.model.Model;
        import com.flipkart.krystal.model.ModelRoot;
        import java.util.Map;

        import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

        @ModelRoot(type = RESPONSE)
        public interface GoodMapKeyModel extends Model {
          Map<String, Errable<String>> goodField();
        }
        """;

    List<Diagnostic<? extends JavaFileObject>> diagnostics = compile(source);

    assertThat(
            diagnostics.stream()
                .filter(d -> d.getKind() == Kind.ERROR)
                .anyMatch(
                    d -> d.getMessage(null).contains("Errable is not allowed as a Map key type")))
        .isFalse();
  }

  private static List<Diagnostic<? extends JavaFileObject>> compile(String source) {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    JavaFileObject sourceFile = new StringSourceFile(source);
    try {
      Path outputDir = Files.createTempDirectory("errable-map-key-validation-test");
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
                  "-Akrystal.codegen.phase=MODELS"),
              null,
              List.of(sourceFile));
      task.setProcessors(List.of(new ModelGenProcessor()));
      task.call();
      return diagnostics.getDiagnostics();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static final class StringSourceFile extends SimpleJavaFileObject {
    private final String content;

    StringSourceFile(String content) {
      super(
          java.net.URI.create(
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
