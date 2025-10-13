package com.flipkart.krystal.vajram.graphql.api;

import static com.google.common.collect.Maps.filterKeys;

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

  public final Map<String, Object> _values() {
    return _values;
  }

  public final Map<String, Object> _asMap() {
    Map<String, Object> map = new LinkedHashMap<>();
    _values.forEach(
        (s, o) -> {
          List<Object> graphQlModels = new ArrayList<>();
          if (o instanceof List list) {
            for (Object listElement : list) {
              if (listElement instanceof AbstractGraphQlModel<?> graphQlModel) {
                graphQlModels.add(graphQlModel._asMap());
              } else {
                graphQlModels.add(listElement);
              }
            }
            map.put(s, graphQlModels);
          } else {
            map.put(
                s, o instanceof AbstractGraphQlModel<?> graphQlModel ? graphQlModel._asMap() : o);
          }
        });
    return map;
  }

  public final Map<String, Object> _errorsAsMap() {
    Map<String, Object> map = null;
    if (!_errors.isEmpty()) {
      map = new LinkedHashMap<>(_errors);
    }
    for (Entry<String, Object> entry : _values.entrySet()) {
      String s = entry.getKey();
      Object o = entry.getValue();
      if (o instanceof AbstractGraphQlModel<?> graphQlModel) {
        Map<String, Object> errorsAsMap = graphQlModel._errorsAsMap();
        if (!errorsAsMap.isEmpty()) {
          if (map == null) {
            map = new LinkedHashMap<>();
          }
          map.put(s, errorsAsMap);
        }
      }
    }
    return Optional.ofNullable(map).orElse(ImmutableMap.of());
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

  protected abstract T _new();

  public final Map<String, List<Throwable>> _errors() {
    return _errors;
  }
}
