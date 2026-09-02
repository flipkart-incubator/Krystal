package com.flipkart.krystal.vajram.graphql.schema;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import graphql.language.Argument;
import graphql.language.Directive;
import graphql.language.DirectivesContainer;
import graphql.language.ScalarTypeDefinition;
import graphql.language.StringValue;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Resolves GraphQL SDL field/argument types to their corresponding {@link TypeName}, applying
 * Krystal's Errable/List/NonNull wrapping conventions and built-in-scalar + {@code @javaType}
 * directive resolution. Schema/protocol-agnostic and shared between server-side (graphql execution)
 * and client-side (request facade) codegen.
 */
public final class SharedTypeNameResolver {

  private static final String JAVA_TYPE_DIRECTIVE = "javaType";
  private static final String PACKAGE_NAME_DIR_ARG = "packageName";
  private static final String CLASS_NAME_DIR_ARG = "className";
  private static final String SUB_PACKAGE_DIRECTIVE = "subPackage";
  private static final String NAME_DIR_ARG = "name";

  private final TypeDefinitionRegistry typeDefinitionRegistry;
  private final String rootPackageName;

  public SharedTypeNameResolver(
      TypeDefinitionRegistry typeDefinitionRegistry, String rootPackageName) {
    this.typeDefinitionRegistry = typeDefinitionRegistry;
    this.rootPackageName = rootPackageName;
  }

  /** T → Errable&lt;T&gt;; [T]! → Errable&lt;List&lt;T&gt;&gt;; T! → T; etc. */
  public TypeName toTypeNameForField(
      GraphQlTypeDecorator type, Function<PlainType, ClassName> leafResolver) {
    ClassName errable = ClassName.get("com.flipkart.krystal.data", "Errable");
    if (type instanceof PlainType plainType) {
      return ParameterizedTypeName.get(errable, leafResolver.apply(plainType));
    } else if (type instanceof WrappedType wrappedType) {
      return switch (wrappedType.wrapperType()) {
        case NONNULL -> toNonNullTypeName(wrappedType.innerType(), leafResolver);
        case LIST ->
            ParameterizedTypeName.get(
                errable,
                ParameterizedTypeName.get(
                    ClassName.get(List.class),
                    toTypeNameForField(wrappedType.innerType(), leafResolver)));
      };
    }
    throw new IllegalArgumentException("Unknown fieldType: " + type);
  }

  /**
   * Raw Java type for a GraphQL field type, ignoring all nullability - {@code Errable} is not
   * applied.
   */
  public TypeName toFacetTypeName(
      GraphQlTypeDecorator type, Function<PlainType, ClassName> leafResolver) {
    if (type instanceof PlainType plainType) {
      return leafResolver.apply(plainType);
    } else if (type instanceof WrappedType wrappedType) {
      return switch (wrappedType.wrapperType()) {
        case NONNULL -> toFacetTypeName(wrappedType.innerType(), leafResolver);
        case LIST ->
            ParameterizedTypeName.get(
                ClassName.get(List.class), toFacetTypeName(wrappedType.innerType(), leafResolver));
      };
    }
    throw new IllegalArgumentException("Unknown type: " + type);
  }

  private TypeName toNonNullTypeName(
      GraphQlTypeDecorator type, Function<PlainType, ClassName> leafResolver) {
    if (type instanceof PlainType plainType) {
      return leafResolver.apply(plainType);
    } else if (type instanceof WrappedType wrappedType) {
      return switch (wrappedType.wrapperType()) {
        case NONNULL -> toNonNullTypeName(wrappedType.innerType(), leafResolver); // flatten !!
        case LIST ->
            ParameterizedTypeName.get(
                ClassName.get(List.class),
                toTypeNameForField(wrappedType.innerType(), leafResolver));
      };
    }
    throw new IllegalArgumentException("Unknown type: " + type);
  }

  /**
   * Default leaf-type resolution: built-in scalars, {@code @javaType}-directive-mapped scalars, and
   * otherwise the object/enum type's default generated class name ({@code
   * packageNameFor(type).SimpleName}). Does NOT apply any entity-id substitution - callers with
   * such server-only concerns should wrap this and override as needed.
   */
  public ClassName defaultLeafClassName(PlainType plainType) {
    String graphQlTypeName = plainType.graphQlType().getName();

    ClassName scalarJavaType = getJavaTypeForScalar(graphQlTypeName);
    if (scalarJavaType != null) {
      return scalarJavaType;
    }

    return switch (graphQlTypeName) {
      case "String", "ID" -> ClassName.get(String.class);
      case "Int" -> ClassName.get(Integer.class);
      case "Boolean" -> ClassName.get(Boolean.class);
      // GraphqlJava handles graphql Floats using java Doubles
      // https://graphql-java.com/documentation/data-mapping/#scalars
      case "Float" -> ClassName.get(Double.class);
      default -> {
        GraphQLTypeName typeName = new GraphQLTypeName(graphQlTypeName);
        yield ClassName.get(getPackageNameForType(typeName), typeName.value());
      }
    };
  }

  /** Gets the Java type mapping for a scalar type from the {@code @javaType} directive. */
  public @Nullable ClassName getJavaTypeForScalar(String scalarName) {
    return typeDefinitionRegistry
        .getType(scalarName, ScalarTypeDefinition.class)
        .map(
            scalarDef ->
                extractClassNameFromJavaTypeDirective(
                    scalarDef, "Scalar '%s'".formatted(scalarName)))
        .orElse(null);
  }

  /** Extracts ClassName from a @javaType directive's packageName and className arguments. */
  public static @Nullable ClassName extractClassNameFromJavaTypeDirective(
      DirectivesContainer<?> directivesContainer, String contextDescription) {
    List<Directive> javaTypeDirectives = directivesContainer.getDirectives(JAVA_TYPE_DIRECTIVE);
    if (javaTypeDirectives.isEmpty()) {
      return null;
    }

    Directive directive = javaTypeDirectives.get(0);
    Argument packageNameArg = directive.getArgument(PACKAGE_NAME_DIR_ARG);
    Argument classNameArg = directive.getArgument(CLASS_NAME_DIR_ARG);

    if (packageNameArg == null || classNameArg == null) {
      throw new IllegalStateException(
          "%s has @javaType directive without required 'packageName' or 'className' argument"
              .formatted(contextDescription));
    }

    if (!(packageNameArg.getValue() instanceof StringValue packageNameValue)
        || !(classNameArg.getValue() instanceof StringValue classNameValue)) {
      throw new IllegalStateException(
          "%s @javaType directive: 'packageName' and 'className' must be String literals, got packageName: %s, className: %s"
              .formatted(
                  contextDescription,
                  packageNameArg.getValue().getClass().getSimpleName(),
                  classNameArg.getValue().getClass().getSimpleName()));
    }

    return ClassName.get(packageNameValue.getValue(), classNameValue.getValue());
  }

  public String getPackageNameForType(GraphQLTypeName graphQLTypeName) {
    var objectTypeDefinition =
        java.util.Objects.requireNonNull(
            typeDefinitionRegistry.types().get(graphQLTypeName.value()),
            () -> "Could not find type definition for type: " + graphQLTypeName);
    String subPackage =
        getDirectiveArgumentString(objectTypeDefinition, SUB_PACKAGE_DIRECTIVE, NAME_DIR_ARG)
            .orElse(graphQLTypeName.value().toLowerCase(Locale.ROOT))
            .trim();
    if (!subPackage.isEmpty()) {
      subPackage = "." + subPackage;
    }
    return rootPackageName + subPackage;
  }

  private static java.util.Optional<String> getDirectiveArgumentString(
      DirectivesContainer<?> element, String directiveName, String argName) {
    List<Directive> directives = element.getDirectives(directiveName);
    if (directives.isEmpty()) {
      return java.util.Optional.empty();
    }
    Argument argument = directives.get(0).getArgument(argName);
    return argument == null
        ? java.util.Optional.empty()
        : java.util.Optional.ofNullable((StringValue) argument.getValue())
            .map(StringValue::getValue);
  }
}
