package com.flipkart.krystal.vajram.graphql.api.execution;

import com.flipkart.krystal.vajram.graphql.api.errors.GraphQLErrorInfo;
import com.google.common.collect.Sets;
import graphql.ErrorClassification;
import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.execution.ExecutionStrategyParameters;
import graphql.execution.MergedField;
import graphql.execution.MergedSelectionSet;
import graphql.language.SourceLocation;
import graphql.scalars.ExtendedScalars;
import graphql.schema.idl.RuntimeWiring;
import java.util.LinkedHashMap;
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
      return locations instanceof List ? (List<SourceLocation>) locations : null;
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
    public List<Object> getPath() {
      Object path = errorMap.get("path");
      return path instanceof List ? (List<Object>) path : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getExtensions() {
      Object extensions = errorMap.get("extensions");
      return extensions instanceof Map ? (Map<String, Object>) extensions : null;
    }

    @Override
    public Map<String, Object> toSpecification() {
      // Return the error map as-is for maximum flexibility
      return new LinkedHashMap<>(errorMap);
    }
  }

  public static boolean isFieldQueried(String fieldName, ExecutionStrategyParameters params) {
    MergedSelectionSet fields = params.getFields();
    MergedField field = params.getField();
    return fields.getSubFields().containsKey(fieldName)
        || (field != null && field.getSingleField().getName().equals(fieldName));
  }

  public static boolean isAnyFieldQueried(
      Set<String> fieldNames, ExecutionStrategyParameters params) {
    MergedSelectionSet fields = params.getFields();
    return !Sets.intersection(fields.getSubFields().keySet(), fieldNames).isEmpty()
        || fieldNames.contains(params.getField().getSingleField().getName());
  }
}
