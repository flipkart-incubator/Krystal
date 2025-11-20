package com.flipkart.krystal.vajram.graphql.api.execution;

import com.flipkart.krystal.vajram.graphql.api.errors.GraphQLErrorInfo;
import com.google.common.collect.Sets;
import graphql.GraphQLError;
import graphql.execution.ExecutionStrategyParameters;
import graphql.execution.MergedSelectionSet;
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
  private static class GraphQLErrorFromMap implements GraphQLError {
    private final java.util.Map<String, Object> errorMap;

    public GraphQLErrorFromMap(java.util.Map<String, Object> errorMap) {
      this.errorMap = errorMap;
    }

    @Override
    public String getMessage() {
      Object message = errorMap.get("message");
      return message != null ? message.toString() : "Unknown error";
    }

    @Override
    @SuppressWarnings("unchecked")
    public java.util.List<graphql.language.SourceLocation> getLocations() {
      Object locations = errorMap.get("locations");
      return locations instanceof java.util.List
          ? (java.util.List<graphql.language.SourceLocation>) locations
          : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public graphql.ErrorClassification getErrorType() {
      Object classification = errorMap.get("errorType");
      return classification instanceof graphql.ErrorClassification
          ? (graphql.ErrorClassification) classification
          : graphql.ErrorType.DataFetchingException;
    }

    @Override
    @SuppressWarnings("unchecked")
    public java.util.List<Object> getPath() {
      Object path = errorMap.get("path");
      return path instanceof java.util.List ? (java.util.List<Object>) path : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public java.util.Map<String, Object> getExtensions() {
      Object extensions = errorMap.get("extensions");
      return extensions instanceof java.util.Map
          ? (java.util.Map<String, Object>) extensions
          : null;
    }

    @Override
    public java.util.Map<String, Object> toSpecification() {
      // Return the error map as-is for maximum flexibility
      return new java.util.LinkedHashMap<>(errorMap);
    }
  }

  public static boolean isFieldQueried(String fieldName, ExecutionStrategyParameters params) {
    MergedSelectionSet fields = params.getFields();
    return fields.getSubFields().containsKey(fieldName)
        || params.getField().getSingleField().getName().equals(fieldName);
  }

  public static boolean isAnyFieldQueried(
      Set<String> fieldNames, ExecutionStrategyParameters params) {
    MergedSelectionSet fields = params.getFields();
    return !Sets.intersection(fields.getSubFields().keySet(), fieldNames).isEmpty()
        || fieldNames.contains(params.getField().getSingleField().getName());
  }
}
