package com.flipkart.krystal.vajram.graphql.client.codegen;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
 * Verifies (via in-process annotation processing) that {@link GraphQlFacadeProcessor} reports
 * compile errors for invalid {@code @FieldArg} bindings and unrecognized schema fields.
 */
class FieldArgValidationTest {

  private static final String SCHEMA =
      """
      schema @rootPackage(name: "com.example.accounts") { query: Query }
      type Query { account(id: ID!): Account }
      type Account { id: ID! name: String }
      """;

  @Test
  void useVariableReferencingNonexistentFieldFailsCompilation() throws IOException {
    String operationRoot =
        """
        package com.example.accounts;

        import com.flipkart.krystal.model.Model;
        import com.flipkart.krystal.model.ModelRoot;
        import com.flipkart.krystal.model.SupportedModelProtocol;
        import com.flipkart.krystal.vajram.graphql.client.api.FieldArg;
        import com.flipkart.krystal.vajram.graphql.client.api.GraphQlOpRequest;
        import com.flipkart.krystal.vajram.json.Json;

        import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

        @GraphQlOpRequest(schemaFilePath = "Schema.graphqls")
        @ModelRoot(type = RESPONSE)
        @SupportedModelProtocol(Json.class)
        public interface GetAccountOperation extends Model {
          @FieldArg(name = "id", useVariable = "doesNotExist")
          Account account();
        }
        """;
    String account =
        """
        package com.example.accounts;

        import com.flipkart.krystal.model.Model;
        import com.flipkart.krystal.model.ModelRoot;
        import com.flipkart.krystal.model.SupportedModelProtocol;
        import com.flipkart.krystal.vajram.graphql.client.api.GraphQlRequest;
        import com.flipkart.krystal.vajram.json.Json;

        import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

        @GraphQlRequest
        @ModelRoot(type = RESPONSE)
        @SupportedModelProtocol(Json.class)
        public interface Account extends Model {
          String id();
          String name();
        }
        """;
    String variables =
        """
        package com.example.accounts;

        import com.flipkart.krystal.model.IfAbsent;
        import com.flipkart.krystal.model.Model;
        import com.flipkart.krystal.model.ModelRoot;
        import com.flipkart.krystal.model.SupportedModelProtocol;
        import com.flipkart.krystal.vajram.graphql.client.api.ForGraphQlOpReq;
        import com.flipkart.krystal.vajram.json.Json;

        import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
        import static com.flipkart.krystal.model.ModelRoot.ModelType.REQUEST;

        @ForGraphQlOpReq(GetAccountOperation.class)
        @ModelRoot(type = REQUEST)
        @SupportedModelProtocol(Json.class)
        public interface GetAccountVariables extends Model {
          @IfAbsent(FAIL)
          String id();
        }
        """;

    List<Diagnostic<? extends JavaFileObject>> diagnostics =
        compile(SCHEMA, operationRoot, account, variables);

    assertThat(
            diagnostics.stream()
                .filter(d -> d.getKind() == Kind.ERROR)
                .anyMatch(d -> d.getMessage(null).contains("doesNotExist")))
        .isTrue();
  }

  @Test
  void unrecognizedSchemaFieldFailsCompilation() throws IOException {
    String operationRoot =
        """
        package com.example.accounts2;

        import com.flipkart.krystal.model.Model;
        import com.flipkart.krystal.model.ModelRoot;
        import com.flipkart.krystal.model.SupportedModelProtocol;
        import com.flipkart.krystal.vajram.graphql.client.api.Field;
        import com.flipkart.krystal.vajram.graphql.client.api.GraphQlOpRequest;
        import com.flipkart.krystal.vajram.json.Json;

        import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

        @GraphQlOpRequest(schemaFilePath = "Schema.graphqls")
        @ModelRoot(type = RESPONSE)
        @SupportedModelProtocol(Json.class)
        public interface GetAccountOperation extends Model {
          @Field(name = "doesNotExistOnQuery")
          String account();
        }
        """;
    String variables =
        """
        package com.example.accounts2;

        import com.flipkart.krystal.model.Model;
        import com.flipkart.krystal.model.ModelRoot;
        import com.flipkart.krystal.model.SupportedModelProtocol;
        import com.flipkart.krystal.vajram.graphql.client.api.ForGraphQlOpReq;
        import com.flipkart.krystal.vajram.json.Json;

        import static com.flipkart.krystal.model.ModelRoot.ModelType.REQUEST;

        @ForGraphQlOpReq(GetAccountOperation.class)
        @ModelRoot(type = REQUEST)
        @SupportedModelProtocol(Json.class)
        public interface GetAccountVariables extends Model {
        }
        """;

    List<Diagnostic<? extends JavaFileObject>> diagnostics =
        compile(SCHEMA, operationRoot, variables);

    assertThat(
            diagnostics.stream()
                .filter(d -> d.getKind() == Kind.ERROR)
                .anyMatch(d -> d.getMessage(null).contains("doesNotExistOnQuery")))
        .isTrue();
  }

  private static List<Diagnostic<? extends JavaFileObject>> compile(
      String schema, String... sources) throws IOException {
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
    task.setProcessors(List.of(new GraphQlFacadeProcessor()));
    task.call();
    return diagnostics.getDiagnostics();
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
