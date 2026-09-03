package com.flipkart.krystal.vajram.graphql.client.codegen;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;
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
        CompileTestSupport.compile(SCHEMA, operationRoot, account, variables);

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
        CompileTestSupport.compile(SCHEMA, operationRoot, variables);

    assertThat(
            diagnostics.stream()
                .filter(d -> d.getKind() == Kind.ERROR)
                .anyMatch(d -> d.getMessage(null).contains("doesNotExistOnQuery")))
        .isTrue();
  }

  @Test
  void operationRootWithoutModelRootAnnotation_failsCompilationWithoutCrashing()
      throws IOException {
    // Regression test: the operation root is missing @ModelRoot - this must be reported as a
    // clean diagnostic rather than crashing the annotation processor (e.g. a NullPointerException
    // from response-wrapper generation reading the annotation's `pure()` member).
    String operationRoot =
        """
        package com.example.accounts3;

        import com.flipkart.krystal.model.Model;
        import com.flipkart.krystal.vajram.graphql.client.api.FieldArg;
        import com.flipkart.krystal.vajram.graphql.client.api.GraphQlOpRequest;

        @GraphQlOpRequest(schemaFilePath = "Schema.graphqls")
        public interface GetAccountOperation extends Model {
          @FieldArg(name = "id", useVariable = "id")
          Account account();
        }
        """;
    String account =
        """
        package com.example.accounts3;

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
        }
        """;
    String variables =
        """
        package com.example.accounts3;

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
        CompileTestSupport.compile(SCHEMA, operationRoot, account, variables);

    assertThat(
            diagnostics.stream()
                .filter(d -> d.getKind() == Kind.ERROR)
                .anyMatch(d -> d.getMessage(null).contains("@ModelRoot")))
        .isTrue();
  }
}
