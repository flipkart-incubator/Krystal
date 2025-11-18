package com.flipkart.krystal.vajram.graphql.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link GraphQLErrorInfo} that builds a GraphQL-compliant error map.
 *
 * <p>This implementation stores error information and converts it to a map format that follows the
 * GraphQL error specification.
 */
public class DefaultGraphQLErrorInfo implements GraphQLErrorInfo {

  private final List<String> messages = new ArrayList<>();
  private final List<Object> path;
  private final Throwable throwable;

  /**
   * Creates a new error info with the given path and throwable.
   *
   * @param path The GraphQL path where the error occurred
   * @param throwable The throwable that caused the error
   */
  public DefaultGraphQLErrorInfo(List<Object> path, Throwable throwable) {
    this.path = new ArrayList<>(path);
    this.throwable = throwable;
    if (throwable != null && throwable.getMessage() != null) {
      this.messages.add(throwable.getMessage());
    }
  }

  /**
   * Creates a new error info with the given path and message.
   *
   * @param path The GraphQL path where the error occurred
   * @param message The error message
   */
  public DefaultGraphQLErrorInfo(List<Object> path, String message) {
    this.path = new ArrayList<>(path);
    this.throwable = null;
    if (message != null) {
      this.messages.add(message);
    }
  }

  @Override
  public void addMessage(String message) {
    if (message != null) {
      messages.add(message);
    }
  }

  @Override
  public Map<String, Object> getErrors() {
    Map<String, Object> errorMap = new LinkedHashMap<>();

    // Add message (combine all messages if multiple)
    if (!messages.isEmpty()) {
      errorMap.put("message", messages.size() == 1 ? messages.get(0) : String.join("; ", messages));
    } else {
      errorMap.put("message", "Unknown error");
    }

    // Add path
    if (path != null && !path.isEmpty()) {
      errorMap.put("path", new ArrayList<>(path));
    }

    // Always add extensions with error code and exception details
    Map<String, Object> extensions = new LinkedHashMap<>();

    if (throwable != null) {
      // Add error code based on exception type
      String errorCode = getErrorCode(throwable);
      extensions.put("code", errorCode);

      // Add exception simple name only
      extensions.put("exception", throwable.getClass().getSimpleName());
    } else {
      // Even without throwable, provide a default error code
      extensions.put("code", "INTERNAL_ERROR");
    }

    errorMap.put("extensions", extensions);

    return errorMap;
  }

  private String getErrorCode(Throwable throwable) {
    // Map common exceptions to error codes
    String className = throwable.getClass().getSimpleName();

    if (className.contains("NotFound") || className.contains("NoSuchElement")) {
      return "NOT_FOUND";
    } else if (className.contains("IllegalArgument") || className.contains("Validation")) {
      return "BAD_REQUEST";
    } else if (className.contains("Unauthorized") || className.contains("Authentication")) {
      return "UNAUTHORIZED";
    } else if (className.contains("Forbidden") || className.contains("Access")) {
      return "FORBIDDEN";
    } else if (className.contains("Timeout")) {
      return "TIMEOUT";
    } else {
      return "INTERNAL_ERROR";
    }
  }
}
