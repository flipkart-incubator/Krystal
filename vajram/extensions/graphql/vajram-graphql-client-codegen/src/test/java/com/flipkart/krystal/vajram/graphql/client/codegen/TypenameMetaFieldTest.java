package com.flipkart.krystal.vajram.graphql.client.codegen;

import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.vajram.graphql.client.codegen.CompileTestSupport.CompileResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.Test;

/**
 * Verifies {@link GraphQlFacadeProcessor} support for the {@code __typename} GraphQL introspection
 * meta-field, which is valid on any composite type and has no schema {@code FieldDefinition} -
 * unlike ordinary fields, it's selected via {@code @Field(name = "__typename")} without needing a
 * matching schema field.
 */
class TypenameMetaFieldTest {

  private static final String SCHEMA =
      """
      schema @rootPackage(name: "com.example.typename") { query: Query }
      type Query { order(id: ID!): Order }
      type Order { id: ID! }
      """;

  private static final String OPERATION_ROOT =
      """
      package com.example.typename;

      import com.flipkart.krystal.model.Model;
      import com.flipkart.krystal.model.ModelRoot;
      import com.flipkart.krystal.model.SupportedModelProtocol;
      import com.flipkart.krystal.vajram.graphql.client.api.Field;
      import com.flipkart.krystal.vajram.graphql.client.api.FieldArg;
      import com.flipkart.krystal.vajram.graphql.client.api.GraphQlOpRequest;
      import com.flipkart.krystal.vajram.json.Json;

      import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

      @GraphQlOpRequest(schemaFilePath = "Schema.graphqls")
      @ModelRoot(type = RESPONSE)
      @SupportedModelProtocol(Json.class)
      public interface GetOrderOp extends Model {
        @FieldArg(name = "id", useVariable = "id")
        OrderWithTypename order();

        @Field(name = "__typename")
        String queryTypename();
      }
      """;

  private static final String VARIABLES =
      """
      package com.example.typename;

      import com.flipkart.krystal.model.IfAbsent;
      import com.flipkart.krystal.model.Model;
      import com.flipkart.krystal.model.ModelRoot;
      import com.flipkart.krystal.model.SupportedModelProtocol;
      import com.flipkart.krystal.vajram.graphql.client.api.ForGraphQlOpReq;
      import com.flipkart.krystal.vajram.json.Json;

      import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
      import static com.flipkart.krystal.model.ModelRoot.ModelType.REQUEST;

      @ForGraphQlOpReq(GetOrderOp.class)
      @ModelRoot(type = REQUEST)
      @SupportedModelProtocol(Json.class)
      public interface GetOrderVariables extends Model {
        @IfAbsent(FAIL)
        String id();
      }
      """;

  private static String orderWithTypename(String extraAnnotationsOnTypename) {
    return """
        package com.example.typename;

        import com.flipkart.krystal.model.Model;
        import com.flipkart.krystal.model.ModelRoot;
        import com.flipkart.krystal.model.SupportedModelProtocol;
        import com.flipkart.krystal.vajram.graphql.client.api.Field;
        import com.flipkart.krystal.vajram.graphql.client.api.FieldArg;
        import com.flipkart.krystal.vajram.graphql.client.api.GraphQlRequest;
        import com.flipkart.krystal.vajram.json.Json;

        import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

        @GraphQlRequest
        @ModelRoot(type = RESPONSE)
        @SupportedModelProtocol(Json.class)
        public interface OrderWithTypename extends Model {
          String id();

          %s
          @Field(name = "__typename")
          String typename();
        }
        """
        .formatted(extraAnnotationsOnTypename);
  }

  @Test
  void typenameField_isSelectedWithoutSchemaFieldDefinition() throws IOException {
    CompileResult result =
        CompileTestSupport.compile(
            new GraphQlFacadeProcessor(), SCHEMA, OPERATION_ROOT, orderWithTypename(""), VARIABLES);

    assertThat(result.diagnostics().stream().filter(d -> d.getKind() == Kind.ERROR).toList())
        .isEmpty();

    Path file = result.outputDir().resolve("com/example/typename/GetOrderOp_SpecReq.java");
    assertThat(Files.exists(file)).as("generated file %s should exist", file).isTrue();
    String generated = Files.readString(file);
    assertThat(generated).contains("order(id: $id) { id typename: __typename }");
    assertThat(generated).contains("queryTypename: __typename");
  }

  @Test
  void fieldArgOnTypename_failsCompilation() throws IOException {
    List<Diagnostic<? extends JavaFileObject>> diagnostics =
        CompileTestSupport.compile(
            SCHEMA,
            OPERATION_ROOT,
            orderWithTypename("@FieldArg(name = \"bogus\", useVariable = \"id\")"),
            VARIABLES);

    assertThat(
            diagnostics.stream()
                .filter(d -> d.getKind() == Kind.ERROR)
                .anyMatch(d -> d.getMessage(null).contains("__typename")))
        .isTrue();
  }
}
