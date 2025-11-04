package com.flipkart.krystal.vajram.graphql.api;

import graphql.ErrorClassification;
import graphql.GraphQLError;
import graphql.language.SourceLocation;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a field-level error in a GraphQL response. This interface extends the standard {@link
 * GraphQLError} from graphql-java to provide structured error information for fields that failed
 * during execution.
 */
public interface GraphQLFieldError extends GraphQLError {

  /**
   * Returns the field path where the error occurred. For example, ["order", "items", 0, "price"]
   * represents an error in order.items[0].price
   */
  List<Object> getFieldPath();

  /**
   * Returns the field name where the error occurred at the current level. This is typically the
   * last element of the field path.
   */
  String getFieldName();

  /** Returns the original throwable that caused this error, if available. */
  @Nullable Throwable getOriginalError();

  /**
   * Creates a new GraphQLFieldError from a throwable.
   *
   * @param fieldName The name of the field where the error occurred
   * @param error The throwable that caused the error
   * @return A new GraphQLFieldError instance
   */
  static GraphQLFieldError fromThrowable(String fieldName, Throwable error) {
    return new DefaultGraphQLFieldError(fieldName, error);
  }

  /**
   * Creates a new GraphQLFieldError with a custom message.
   *
   * @param fieldName The name of the field where the error occurred
   * @param message The error message
   * @return A new GraphQLFieldError instance
   */
  static GraphQLFieldError fromMessage(String fieldName, String message) {
    return new DefaultGraphQLFieldError(fieldName, message, null);
  }

  /**
   * Creates a new GraphQLFieldError with a custom message and field path.
   *
   * @param fieldPath The path to the field where the error occurred
   * @param message The error message
   * @return A new GraphQLFieldError instance
   */
  static GraphQLFieldError fromPath(List<Object> fieldPath, String message) {
    return new DefaultGraphQLFieldError(fieldPath, message, null);
  }

  /** Default implementation of GraphQLFieldError */
  class DefaultGraphQLFieldError implements GraphQLFieldError {
    private final List<Object> fieldPath;
    private final String fieldName;
    private final String message;
    private final @Nullable Throwable originalError;

    DefaultGraphQLFieldError(String fieldName, Throwable error) {
      this(List.of(fieldName), error.getMessage(), error);
    }

    DefaultGraphQLFieldError(String fieldName, String message, @Nullable Throwable error) {
      this(List.of(fieldName), message, error);
    }

    DefaultGraphQLFieldError(
        List<Object> fieldPath, String message, @Nullable Throwable originalError) {
      this.fieldPath = List.copyOf(fieldPath);
      this.fieldName =
          fieldPath.isEmpty() ? "" : String.valueOf(fieldPath.get(fieldPath.size() - 1));
      this.message = message != null ? message : "Unknown error";
      this.originalError = originalError;
    }

    @Override
    public List<Object> getFieldPath() {
      return fieldPath;
    }

    @Override
    public String getFieldName() {
      return fieldName;
    }

    @Override
    public @Nullable Throwable getOriginalError() {
      return originalError;
    }

    @Override
    public String getMessage() {
      return message;
    }

    @Override
    public List<SourceLocation> getLocations() {
      return null; // Field errors don't have source locations from the query
    }

    @Override
    public ErrorClassification getErrorType() {
      return ErrorClassification.errorClassification("DataFetchingError");
    }

    @Override
    public Map<String, Object> getExtensions() {
      // Per GraphQL spec, extensions should contain additional error metadata
      // Common format: {"code": "ERROR_CODE", ...}
      Map<String, Object> extensions = new java.util.LinkedHashMap<>();

      // Add error code (standard GraphQL practice)
      if (originalError != null) {
        extensions.put("code", originalError.getClass().getSimpleName().toUpperCase());
      } else {
        extensions.put("code", "UNKNOWN_ERROR");
      }

      // Optional: Add exception class name for debugging
      if (originalError != null) {
        extensions.put("exception", originalError.getClass().getName());
      }

      return extensions;
    }

    @Override
    public String toString() {
      return String.format("GraphQLFieldError{fieldPath=%s, message='%s'}", fieldPath, message);
    }
  }
}
