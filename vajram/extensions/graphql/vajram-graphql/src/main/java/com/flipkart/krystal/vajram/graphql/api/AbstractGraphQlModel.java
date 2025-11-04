package com.flipkart.krystal.vajram.graphql.api;

import static com.google.common.collect.Maps.filterKeys;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNullElse;

import com.google.common.collect.ImmutableMap;
import java.util.*;
import java.util.Map.Entry;
import lombok.ToString;

@ToString
public abstract class AbstractGraphQlModel<T extends AbstractGraphQlModel<T>> {

  protected final Map<String, Object> _values;
  protected final Map<String, List<Throwable>> _errors = new LinkedHashMap<>();

  protected AbstractGraphQlModel() {
    this(new LinkedHashMap<>());
  }

  private AbstractGraphQlModel(Map<String, Object> _values) {
    this._values = _values;
  }

  protected abstract T _new();

  public final Map<String, Object> _values() {
    return unmodifiableMap(_values);
  }

  public final Map<String, Object> _valuesAsMap() {
    final Map<String, Object> map = new LinkedHashMap<>(_values.size());
    _values.forEach(
        (s, o) -> {
          List<Object> graphQlModels = new ArrayList<>();
          if (o instanceof List list) {
            for (Object listElement : list) {
              if (listElement instanceof AbstractGraphQlModel<?> graphQlModel) {
                graphQlModels.add(graphQlModel._valuesAsMap());
              } else {
                graphQlModels.add(listElement);
              }
            }
            map.put(s, graphQlModels);
          } else {
            map.put(
                s,
                o instanceof AbstractGraphQlModel<?> graphQlModel
                    ? graphQlModel._valuesAsMap()
                    : o);
          }
        });
    return map;
  }

  public final Map<String, Object> _errorsAsMap() {
    Map<String, Object> map = null;
    if (!_errors.isEmpty()) {
      // Use GraphQL-compliant error format
      map = new LinkedHashMap<>(_graphqlErrorsAsMap());
    }
    for (Entry<String, Object> entry : _values.entrySet()) {
      String s = entry.getKey();
      Object value = entry.getValue();
      if (value instanceof AbstractGraphQlModel<?> graphQlModel) {
        Map<String, Object> errorsAsMap = graphQlModel._errorsAsMap();
        if (!errorsAsMap.isEmpty()) {
          if (map == null) {
            map = new LinkedHashMap<>();
          }
          map.put(s, errorsAsMap);
        }
      }
    }
    return requireNonNullElse(map, ImmutableMap.of());
  }

  public final T _deepCopy() {
    Map<String, Object> map = new LinkedHashMap<>();
    _values.forEach(
        (s, o) ->
            map.put(
                s,
                o instanceof AbstractGraphQlModel<?> graphQlModel ? graphQlModel._deepCopy() : o));
    T t = _new();
    t._values.putAll(map);
    return t;
  }

  public final T _filterFields(Set<String> fieldNames) {
    T t = _new();
    t._values.putAll(filterKeys(_values, fieldNames::contains));
    t._errors.putAll(filterKeys(_errors, fieldNames::contains));
    return t;
  }

  public final Map<String, List<Throwable>> _errors() {
    return unmodifiableMap(_errors);
  }

  public final void _putError(String fieldName, Throwable error) {
    _errors.computeIfAbsent(fieldName, _k -> new ArrayList<>()).add(error);
  }

  public final void _putErrors(String fieldName, List<Throwable> errors) {
    _errors.put(fieldName, unmodifiableList(errors));
  }

  /**
   * Returns all errors as GraphQLFieldError objects. Converts Throwable errors to GraphQLFieldError
   * on the fly.
   */
  public final Map<String, List<GraphQLFieldError>> _graphqlErrors() {
    Map<String, List<GraphQLFieldError>> graphqlErrors = new LinkedHashMap<>();
    _errors.forEach(
        (fieldName, errors) -> {
          List<GraphQLFieldError> fieldErrors =
              errors.stream()
                  .map(
                      error ->
                          error instanceof GraphQLFieldError gqlError
                              ? gqlError
                              : GraphQLFieldError.fromThrowable(fieldName, error))
                  .toList();
          graphqlErrors.put(fieldName, fieldErrors);
        });
    return unmodifiableMap(graphqlErrors);
  }

  /**
   * Returns errors as a GraphQL-compliant map structure for inclusion in responses. The format
   * follows the GraphQL spec with message, path, and extensions.
   */
  public final Map<String, Object> _graphqlErrorsAsMap() {
    Map<String, List<Map<String, Object>>> errorsByField = new LinkedHashMap<>();
    _errors.forEach(
        (fieldName, errors) -> {
          List<Map<String, Object>> fieldErrors =
              errors.stream()
                  .map(
                      error -> {
                        GraphQLFieldError gqlError =
                            error instanceof GraphQLFieldError ge
                                ? ge
                                : GraphQLFieldError.fromThrowable(fieldName, error);
                        Map<String, Object> errorMap = new LinkedHashMap<>();
                        errorMap.put("message", gqlError.getMessage());
                        errorMap.put("path", gqlError.getFieldPath());
                        if (gqlError.getExtensions() != null
                            && !gqlError.getExtensions().isEmpty()) {
                          errorMap.put("extensions", gqlError.getExtensions());
                        }
                        return errorMap;
                      })
                  .toList();
          errorsByField.put(fieldName, fieldErrors);
        });
    return unmodifiableMap(errorsByField);
  }

  /**
   * Returns errors as a flat list conforming to the GraphQL specification. According to the spec
   * (https://spec.graphql.org/October2021/#sec-Errors), errors should be returned as a list at the
   * top level of the response.
   *
   * <p>Each error contains:
   *
   * <ul>
   *   <li>message (required): A description of the error
   *   <li>path (optional): Path to the field that caused the error
   *   <li>extensions (optional): Additional error information
   * </ul>
   *
   * <p>This method recursively collects errors from nested entities and builds proper paths
   * including field names and array indices, per GraphQL spec:
   *
   * <ul>
   *   <li>Simple field: ["fieldName"]
   *   <li>Nested field: ["parent", "child", "field"]
   *   <li>Array item: ["parent", 0, "field"]
   * </ul>
   *
   * @return A flat list of GraphQL-compliant error maps
   */
  public final List<Map<String, Object>> _graphqlErrorsAsList() {
    List<Map<String, Object>> errorList = new ArrayList<>();
    _collectErrorsRecursively(errorList, new ArrayList<>());
    return unmodifiableList(errorList);
  }

  /**
   * Recursively collects errors from this entity and all nested entities. Builds proper
   * GraphQL-compliant paths with field names and array indices.
   *
   * @param errorList The list to add errors to
   * @param pathPrefix The current path prefix (e.g., ["order", "items", 0])
   */
  private void _collectErrorsRecursively(
      List<Map<String, Object>> errorList, List<Object> pathPrefix) {
    // Collect errors from this entity
    _errors.forEach(
        (fieldName, errors) -> {
          for (Throwable error : errors) {
            // Build full path: pathPrefix + fieldName
            List<Object> fullPath = new ArrayList<>(pathPrefix);
            fullPath.add(fieldName);

            GraphQLFieldError gqlError =
                error instanceof GraphQLFieldError ge
                    ? ge
                    : GraphQLFieldError.fromPathWithThrowable(fullPath, error);

            Map<String, Object> errorMap = new LinkedHashMap<>();
            errorMap.put("message", gqlError.getMessage());
            errorMap.put("path", fullPath);
            if (gqlError.getExtensions() != null && !gqlError.getExtensions().isEmpty()) {
              errorMap.put("extensions", gqlError.getExtensions());
            }
            errorList.add(errorMap);
          }
        });

    // Recursively collect errors from nested entities
    for (Entry<String, Object> entry : _values.entrySet()) {
      String fieldName = entry.getKey();
      Object value = entry.getValue();

      if (value instanceof AbstractGraphQlModel<?> nestedEntity) {
        // Single nested entity: add field name to path
        List<Object> nestedPath = new ArrayList<>(pathPrefix);
        nestedPath.add(fieldName);
        nestedEntity._collectErrorsRecursively(errorList, nestedPath);

      } else if (value instanceof List<?> list) {
        // Array of entities: add field name and index to path
        for (int i = 0; i < list.size(); i++) {
          Object item = list.get(i);
          if (item instanceof AbstractGraphQlModel<?> nestedEntity) {
            List<Object> nestedPath = new ArrayList<>(pathPrefix);
            nestedPath.add(fieldName);
            nestedPath.add(i);
            nestedEntity._collectErrorsRecursively(errorList, nestedPath);
          }
        }
      }
    }
  }

  final Object _putValue(String fieldName, Object value) {
    return _values.put(fieldName, value);
  }
}
