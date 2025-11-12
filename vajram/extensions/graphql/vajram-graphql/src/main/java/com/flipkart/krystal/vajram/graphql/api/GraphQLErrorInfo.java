package com.flipkart.krystal.vajram.graphql.api;

import java.util.Map;

/**
 * Abstraction over GraphQL error information.
 *
 * <p>This interface provides a clean abstraction layer on top of GraphQLError,
 * allowing error information to be collected and managed independently of the
 * GraphQL-specific error format. This makes it easier to change error structures
 * without affecting the generated code.
 *
 * <p>Usage:
 * <pre>{@code
 * GraphQLErrorInfo errorInfo = new DefaultGraphQLErrorInfo();
 * errorInfo.addMessage("Failed to fetch data");
 * Map<String, Object> errorMap = errorInfo.getErrors();
 * }</pre>
 */
public interface GraphQLErrorInfo {
  
  /**
   * Adds an error message to this error info.
   *
   * @param message The error message to add
   */
  void addMessage(String message);
  
  /**
   * Returns the error information as a map suitable for GraphQL response.
   *
   * <p>The returned map follows the GraphQL error specification format:
   * <pre>{@code
   * {
   *   "message": "Error message",
   *   "path": ["field", "name"],
   *   "extensions": {
   *     "code": "ERROR_CODE",
   *     "exception": {...}
   *   }
   * }
   * }</pre>
   *
   * @return Map containing error information in GraphQL format
   */
  Map<String, Object> getErrors();
}

