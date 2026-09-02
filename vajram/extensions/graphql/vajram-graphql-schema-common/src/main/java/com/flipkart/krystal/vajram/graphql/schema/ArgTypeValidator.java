package com.flipkart.krystal.vajram.graphql.schema;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import graphql.language.FieldDefinition;
import graphql.language.InputValueDefinition;
import graphql.language.NonNullType;
import java.util.Optional;
import java.util.function.Function;

/**
 * Resolves a {@link FieldDefinition}'s argument by name and validates its declared GraphQL type for
 * Java-type compatibility, for use by codegen that binds Java fields to GraphQL arguments (e.g. the
 * client-side request-facade codegen).
 */
public final class ArgTypeValidator {

  private final SharedTypeNameResolver typeNameResolver;

  public ArgTypeValidator(SharedTypeNameResolver typeNameResolver) {
    this.typeNameResolver = typeNameResolver;
  }

  /** Resolves an argument definition by name on the given field, if present. */
  public Optional<InputValueDefinition> resolveArg(FieldDefinition field, String argName) {
    return field.getInputValueDefinitions().stream()
        .filter(arg -> arg.getName().equals(argName))
        .findFirst();
  }

  /** Returns true if the argument's declared type is a {@link NonNullType}. */
  public boolean isMandatory(InputValueDefinition arg) {
    return arg.getType() instanceof NonNullType;
  }

  /**
   * The Java type Krystal's GraphQL codegen would generate for this argument's declared GraphQL
   * type (using default, non-entity-aware leaf resolution - GraphQL input/scalar arguments never
   * resolve to entity-id types).
   */
  public TypeName expectedJavaType(InputValueDefinition arg) {
    GraphQlTypeDecorator argType = GraphQlTypeDecorator.of(arg.getType());
    Function<PlainType, ClassName> leafResolver = typeNameResolver::defaultLeafClassName;
    return typeNameResolver.toFacetTypeName(argType, leafResolver);
  }

  /**
   * Validates that {@code javaFieldType} is type-compatible with {@code arg}'s declared GraphQL
   * type, and - if the argument is mandatory ({@code !}) - that {@code
   * javaFieldIsEffectivelyNonOptional} (i.e. the bound Java field is annotated
   * {@code @IfAbsent(FAIL)}) is {@code true}.
   *
   * @return an empty Optional if valid, or an error message describing the mismatch.
   */
  public Optional<String> validate(
      InputValueDefinition arg, TypeName javaFieldType, boolean javaFieldIsEffectivelyNonOptional) {
    if (isMandatory(arg) && !javaFieldIsEffectivelyNonOptional) {
      return Optional.of(
          "GraphQL argument '%s' is non-null (%s), so the bound Java field must be effectively non-optional (e.g. annotated @IfAbsent(FAIL))"
              .formatted(arg.getName(), arg.getType()));
    }
    TypeName expected = expectedJavaType(arg);
    if (!expected.toString().equals(javaFieldType.toString())) {
      return Optional.of(
          "GraphQL argument '%s' has declared type '%s' (Java type '%s'), but the bound field has type '%s'"
              .formatted(arg.getName(), arg.getType(), expected, javaFieldType));
    }
    return Optional.empty();
  }
}
