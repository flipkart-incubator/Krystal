package com.flipkart.krystal.vajram.graphql.api;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetValues;
import com.google.common.collect.ImmutableList;
import graphql.execution.ExecutionStrategyParameters;
import graphql.language.Directive;
import graphql.language.Field;
import graphql.language.ObjectTypeDefinition;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.language.TypeDefinition;
import graphql.scalars.ExtendedScalars;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
public class GraphQLUtils {

  public static RuntimeWiring buildRuntimeWarning() {
    RuntimeWiring.Builder builder = RuntimeWiring.newRuntimeWiring();
    builder.scalar(ExtendedScalars.Object);
    return builder.build();
  }

  public static boolean isFieldQueriedInTheNestedType(
      String nestedField, ExecutionStrategyParameters params) {
    String[] nestedFieldArray = nestedField.split("\\.");
    return pathMatchesInTheFields(
        nestedFieldArray, params.getField().getSingleField().getSelectionSet(), 0);
  }

  public static boolean isFieldQueriedInTheNestedType(
      Set<String> fieldSupported, ExecutionStrategyParameters params) {
    for (String value : fieldSupported) {
      String[] nestedFieldArray = value.split("\\.");
      if (pathMatchesInTheFields(
          nestedFieldArray, params.getField().getSingleField().getSelectionSet(), 0)) {
        return true;
      }
    }
    return false;
  }

  private static boolean pathMatchesInTheFields(
      String[] nestedFieldArray, SelectionSet field, int index) {
    if (index >= nestedFieldArray.length) {
      return true;
    }
    for (Selection selection : field.getSelections()) {
      Field selectedField = (Field) selection;
      if (selectedField.getName().equals(nestedFieldArray[index])) {
        return pathMatchesInTheFields(nestedFieldArray, selectedField.getSelectionSet(), ++index);
      }
    }
    return false;
  }

  public static void handleErrable(
      String fieldName, Errable<?> errable, AbstractGraphQlModel<?> model) {
    errable.errorOpt().ifPresent(error -> model._putError(fieldName, ImmutableList.of(error)));
    errable
        .valueOpt()
        .ifPresent(
            value -> {
              if (model._putValue(fieldName, value) != null) {
                throw new IllegalArgumentException(
                    "Attempt to set '%s' after it was already set. Old value: %s, New value: %s"
                        .formatted(fieldName, model._values().get(fieldName), value));
              }
            });
  }

  public static <T> void handleErrable(
      String fieldName, Collection<Errable<T>> errables, AbstractGraphQlModel<?> model) {
    List<Object> values = new ArrayList<>();
    List<Throwable> errors = new ArrayList<>();
    for (Errable<T> errable : errables) {
      errable.errorOpt().ifPresent(errors::add);
      errable.valueOpt().ifPresent(values::add);
    }
    model._putError(fieldName, errors);
    if (model._putValue(fieldName, values) != null) {
      throw new IllegalArgumentException(
          "Attempt to set '%s' after it was already set. Old value: %s, New value: %s"
              .formatted(fieldName, model._values().get(fieldName), values));
    }
  }
}
