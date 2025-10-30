package com.flipkart.krystal.vajram.graphql.api;

import graphql.execution.ExecutionStrategyParameters;
import graphql.language.Field;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.scalars.ExtendedScalars;
import graphql.schema.idl.RuntimeWiring;
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
}
