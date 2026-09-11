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
 * Verifies (via in-process annotation processing) that {@link GraphQlFacadeProcessor} spreads
 * {@code @GraphQlFragment} supertypes as GraphQL fragments, and enforces that fragment membership
 * is declared consistently on both the fragment's own declaration and the extends-clause reference.
 */
class GraphQlFragmentTest {

  private static final String SCHEMA =
      """
      schema @rootPackage(name: "com.example.orders") { query: Query }
      type Query { order(id: ID!): Order }
      type Order { id: ID! state: String orderItemNames: [String] }
      """;

  private static final String FRAGMENT_DECLARATION =
      """
      package com.example.orders;

      import com.flipkart.krystal.model.Model;
      import com.flipkart.krystal.model.ModelRoot;
      import com.flipkart.krystal.model.SupportedModelProtocol;
      import com.flipkart.krystal.vajram.graphql.client.api.GraphQlFragment;
      import com.flipkart.krystal.vajram.graphql.client.api.GraphQlRequest;
      import com.flipkart.krystal.vajram.json.Json;
      import java.util.List;

      import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

      @GraphQlFragment
      @GraphQlRequest
      @ModelRoot(type = RESPONSE)
      @SupportedModelProtocol(Json.class)
      public interface OrderFieldsFragment extends Model {
        String state();
        List<String> orderItemNames();
      }
      """;

  private static final String OPERATION_ROOT =
      """
      package com.example.orders;

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
      public interface GetOrderOp extends Model {
        @FieldArg(name = "id", useVariable = "id")
        OrderWithFragment order();
      }
      """;

  private static final String VARIABLES =
      """
      package com.example.orders;

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

  private static String orderWithFragment(String extendsClause) {
    return """
        package com.example.orders;

        import com.flipkart.krystal.model.Model;
        import com.flipkart.krystal.model.ModelRoot;
        import com.flipkart.krystal.model.SupportedModelProtocol;
        import com.flipkart.krystal.vajram.graphql.client.api.GraphQlFragment;
        import com.flipkart.krystal.vajram.graphql.client.api.GraphQlRequest;
        import com.flipkart.krystal.vajram.json.Json;

        import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

        @GraphQlRequest
        @ModelRoot(type = RESPONSE)
        @SupportedModelProtocol(Json.class)
        public interface OrderWithFragment %s {
        }
        """
        .formatted(extendsClause);
  }

  @Test
  void fragmentDeclaredOnBothSides_isSpreadAndDefined() throws IOException {
    CompileResult result =
        CompileTestSupport.compile(
            new GraphQlFacadeProcessor(),
            SCHEMA,
            OPERATION_ROOT,
            FRAGMENT_DECLARATION,
            orderWithFragment("extends Model, @GraphQlFragment OrderFieldsFragment"),
            VARIABLES);

    assertThat(result.diagnostics().stream().filter(d -> d.getKind() == Kind.ERROR).toList())
        .isEmpty();

    String generated = readGeneratedFacade(result.outputDir(), "GetOrderOp_SpecReq");
    assertThat(generated)
        .contains("fragment OrderFieldsFragment on Order { state orderItemNames }");
    assertThat(generated).contains("order(id: $id) { ...OrderFieldsFragment }");
  }

  @Test
  void fragmentDeclaredOnlyOnFragmentItself_failsCompilation() throws IOException {
    List<Diagnostic<? extends JavaFileObject>> diagnostics =
        CompileTestSupport.compile(
            SCHEMA,
            OPERATION_ROOT,
            FRAGMENT_DECLARATION,
            // extends-clause reference is missing @GraphQlFragment.
            orderWithFragment("extends Model, OrderFieldsFragment"),
            VARIABLES);

    assertThat(
            diagnostics.stream()
                .filter(d -> d.getKind() == Kind.ERROR)
                .anyMatch(d -> d.getMessage(null).contains("@GraphQlFragment")))
        .isTrue();
  }

  @Test
  void fragmentDeclaredOnlyOnExtendsClause_failsCompilation() throws IOException {
    String fragmentWithoutAnnotation = FRAGMENT_DECLARATION.replace("@GraphQlFragment\n", "");

    List<Diagnostic<? extends JavaFileObject>> diagnostics =
        CompileTestSupport.compile(
            SCHEMA,
            OPERATION_ROOT,
            fragmentWithoutAnnotation,
            orderWithFragment("extends Model, @GraphQlFragment OrderFieldsFragment"),
            VARIABLES);

    assertThat(
            diagnostics.stream()
                .filter(d -> d.getKind() == Kind.ERROR)
                .anyMatch(d -> d.getMessage(null).contains("@GraphQlFragment")))
        .isTrue();
  }

  @Test
  void extendsPlainInterfaceWithoutGraphQlFragment_failsCompilation() throws IOException {
    String plainInterface =
        """
        package com.example.orders;

        public interface PlainMarker {
        }
        """;

    List<Diagnostic<? extends JavaFileObject>> diagnostics =
        CompileTestSupport.compile(
            SCHEMA,
            OPERATION_ROOT,
            plainInterface,
            orderWithFragment("extends Model, PlainMarker"),
            VARIABLES);

    assertThat(
            diagnostics.stream()
                .filter(d -> d.getKind() == Kind.ERROR)
                .anyMatch(
                    d ->
                        d.getMessage(null).contains("PlainMarker")
                            && d.getMessage(null).contains("@GraphQlFragment")))
        .isTrue();
  }

  @Test
  void invalidGraphQlFragmentName_failsCompilation() throws IOException {
    String fragmentWithInvalidName =
        FRAGMENT_DECLARATION.replace(
            "@GraphQlFragment\n", "@GraphQlFragment(name = \"bad-name\")\n");

    List<Diagnostic<? extends JavaFileObject>> diagnostics =
        CompileTestSupport.compile(
            SCHEMA,
            OPERATION_ROOT,
            fragmentWithInvalidName,
            orderWithFragment("extends Model, @GraphQlFragment OrderFieldsFragment"),
            VARIABLES);

    assertThat(
            diagnostics.stream()
                .filter(d -> d.getKind() == Kind.ERROR)
                .anyMatch(d -> d.getMessage(null).contains("bad-name")))
        .isTrue();
  }

  @Test
  void sameFragmentNameFromDifferentInterfaces_failsCompilation() throws IOException {
    // Two distinct interfaces both explicitly named "OrderFieldsFragment" and both used against
    // `Order` - same name and same type condition, but different declaring interfaces.
    String secondFragmentDeclaration =
        """
        package com.example.orders;

        import com.flipkart.krystal.model.Model;
        import com.flipkart.krystal.model.ModelRoot;
        import com.flipkart.krystal.model.SupportedModelProtocol;
        import com.flipkart.krystal.vajram.graphql.client.api.GraphQlFragment;
        import com.flipkart.krystal.vajram.graphql.client.api.GraphQlRequest;
        import com.flipkart.krystal.vajram.json.Json;

        import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

        @GraphQlFragment(name = "OrderFieldsFragment")
        @GraphQlRequest
        @ModelRoot(type = RESPONSE)
        @SupportedModelProtocol(Json.class)
        public interface OtherOrderFieldsFragment extends Model {
          String state();
        }
        """;

    String twoFragmentsOperationRoot =
        """
        package com.example.orders;

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
        public interface GetOrderOp extends Model {
          @FieldArg(name = "id", useVariable = "id")
          OrderWithFragment order();

          @com.flipkart.krystal.vajram.graphql.client.api.Field(name = "order")
          @FieldArg(name = "id", useVariable = "id")
          OrderWithOtherFragment order2();
        }
        """;

    String orderWithOtherFragment =
        """
        package com.example.orders;

        import com.flipkart.krystal.model.Model;
        import com.flipkart.krystal.model.ModelRoot;
        import com.flipkart.krystal.model.SupportedModelProtocol;
        import com.flipkart.krystal.vajram.graphql.client.api.GraphQlFragment;
        import com.flipkart.krystal.vajram.graphql.client.api.GraphQlRequest;
        import com.flipkart.krystal.vajram.json.Json;

        import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

        @GraphQlRequest
        @ModelRoot(type = RESPONSE)
        @SupportedModelProtocol(Json.class)
        public interface OrderWithOtherFragment
            extends Model, @GraphQlFragment OtherOrderFieldsFragment {}
        """;

    List<Diagnostic<? extends JavaFileObject>> diagnostics =
        CompileTestSupport.compile(
            SCHEMA,
            twoFragmentsOperationRoot,
            FRAGMENT_DECLARATION,
            secondFragmentDeclaration,
            orderWithFragment("extends Model, @GraphQlFragment OrderFieldsFragment"),
            orderWithOtherFragment,
            VARIABLES);

    assertThat(
            diagnostics.stream()
                .filter(d -> d.getKind() == Kind.ERROR)
                .anyMatch(
                    d ->
                        d.getMessage(null).contains("OrderFieldsFragment")
                            && d.getMessage(null).contains("declared by two different")))
        .isTrue();
  }

  private static String readGeneratedFacade(Path outputDir, String simpleName) throws IOException {
    Path file = outputDir.resolve("com/example/orders/" + simpleName + ".java");
    assertThat(Files.exists(file)).as("generated file %s should exist", file).isTrue();
    return Files.readString(file);
  }
}
