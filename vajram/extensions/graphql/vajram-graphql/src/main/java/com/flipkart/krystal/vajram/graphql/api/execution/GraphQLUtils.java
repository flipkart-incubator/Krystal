package com.flipkart.krystal.vajram.graphql.api.execution;

import com.flipkart.krystal.vajram.graphql.api.errors.GraphQLErrorInfo;
import graphql.ErrorClassification;
import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.execution.ExecutionStrategyParameters;
import graphql.execution.MergedField;
import graphql.execution.MergedSelectionSet;
import graphql.language.SourceLocation;
import graphql.scalars.ExtendedScalars;
import graphql.schema.idl.RuntimeWiring;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;

@UtilityClass
public class GraphQLUtils {

  public static RuntimeWiring buildRuntimeWarning() {
    RuntimeWiring.Builder builder = RuntimeWiring.newRuntimeWiring();
    builder.scalar(ExtendedScalars.Object);
    return builder.build();
  }

  /**
   * Converts a GraphQLErrorInfo to a GraphQLError.
   *
   * <p>This method provides the abstraction layer between our internal error representation and the
   * GraphQL-specific error format. It creates a generic GraphQLError implementation that delegates
   * to the error map from GraphQLErrorInfo.
   *
   * @param errorInfo The error info to convert
   * @return A GraphQLError instance
   */
  public static GraphQLError convertToGraphQlError(GraphQLErrorInfo errorInfo) {
    return new GraphQLErrorFromMap(errorInfo.getErrors());
  }

  /**
   * Generic GraphQLError implementation backed by a map.
   *
   * <p>This implementation delegates all method calls to the underlying error map, making it
   * flexible and easy to extend without modifying conversion logic.
   */
  private record GraphQLErrorFromMap(Map<String, Object> errorMap) implements GraphQLError {

    @Override
    public String getMessage() {
      Object message = errorMap.get("message");
      return message != null ? message.toString() : "Unknown error";
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SourceLocation> getLocations() {
      Object locations = errorMap.get("locations");
      return locations instanceof List<?> ? (List<SourceLocation>) locations : List.of();
    }

    @Override
    @SuppressWarnings("unchecked")
    public ErrorClassification getErrorType() {
      Object classification = errorMap.get("errorType");
      return classification instanceof ErrorClassification
          ? (ErrorClassification) classification
          : ErrorType.DataFetchingException;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable List<Object> getPath() {
      Object path = errorMap.get("path");
      return path instanceof List ? (List<Object>) path : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable Map<String, Object> getExtensions() {
      Object extensions = errorMap.get("extensions");
      return extensions instanceof Map ? (Map<String, Object>) extensions : null;
    }

    @Override
    public Map<String, Object> toSpecification() {
      // Return the error map as-is for maximum flexibility
      return new LinkedHashMap<>(errorMap);
    }
  }

  /**
   * Returns true if the given field name is queried (by response key/alias OR by actual field
   * name), allowing correct detection even when the field is requested under an alias.
   */
  public static boolean isFieldQueried(String fieldName, ExecutionStrategyParameters params) {
    MergedSelectionSet fields = params.getFields();
    // Check by actual field name to handle aliased requests
    for (MergedField subField : fields.getSubFields().values()) {
      if (subField.getName().equals(fieldName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if any of the given field names are queried (by response key/alias OR by actual
   * field name), allowing correct detection even when a field is requested under an alias.
   */
  public static boolean isAnyFieldQueried(
      Set<String> fieldNames, ExecutionStrategyParameters params) {
    MergedSelectionSet fields = params.getFields();
    // Check by actual field name to handle aliased requests
    for (MergedField subField : fields.getSubFields().values()) {
      if (fieldNames.contains(subField.getName())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the merged field for a GraphQL field name, irrespective of its response-key alias. Use
   * this only when the exact alias and the args past to the field with that alias does not matter.
   */
  public static @Nullable MergedField getSubFieldByName(
      String fieldName, ExecutionStrategyParameters params) {
    for (MergedField subField : params.getFields().getSubFields().values()) {
      if (subField.getName().equals(fieldName)) {
        return subField;
      }
    }
    return null;
  }

  public static Map<String, List<String>> computeFieldNameToAliases(
      ExecutionStrategyParameters graphql_executionStrategyParams) {
    Map<String, List<String>> _fieldNameToAliases = new LinkedHashMap<>();
    graphql_executionStrategyParams
        .getFields()
        .getSubFields()
        .forEach(
            (alias, mergedField) ->
                _fieldNameToAliases
                    .computeIfAbsent(
                        mergedField.getSingleField().getName(), _k -> new ArrayList<>())
                    .add(alias));
    return _fieldNameToAliases;
  }
}
