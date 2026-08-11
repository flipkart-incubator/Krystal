package com.flipkart.krystal.vajram.graphql.api.model;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.vajram.graphql.api.errors.DefaultGraphQLErrorInfo;
import com.flipkart.krystal.vajram.graphql.api.errors.ErrorCollector;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlValue.ListValue;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlValue.ObjectValue;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlValue.SingleValue;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public sealed class GraphQlEntity implements GraphQlObject permits GraphQlOperationEntity {

  private final Map<String, GraphQlValue> graphql_values;
  private @MonotonicNonNull Map<String, Object> graphql_data;

  GraphQlEntity(Map<String, GraphQlValue> graphql_values) {
    this.graphql_values = unmodifiableMap(graphql_values);
  }

  @Override
  public Map<String, Object> graphql_data() {
    if (graphql_data == null) {
      Map<String, Object> _data = new LinkedHashMap<>();
      graphql_values.forEach((alias, value) -> _data.put(alias, graphql_data(value)));
      graphql_data = _data;
    }
    return graphql_data;
  }

  private @Nullable Object graphql_data(GraphQlValue graphQlValue) {
    Object value = graphQlValue.errable().value();
    if (value == null) {
      return null;
    }
    if (graphQlValue instanceof ListValue listValue) {
      List<Object> data = new ArrayList<>();
      List<SingleValue> valueList =
          // The null case was handled at the beginning of the method
          requireNonNull(listValue.errable().value());
      valueList.forEach(singleValue -> data.add(graphql_data(singleValue)));
      return data;
    } else if (value instanceof GraphQlEntity nestedObject) {
      return nestedObject.graphql_data();
    } else {
      return value;
    }
  }

  @Override
  public void _collectErrors(ErrorCollector errorCollector, List<Object> path) {
    graphql_values.forEach(
        (alias, value) -> {
          List<Object> newPath = new ArrayList<>(path);
          newPath.add(alias);
          _collectErrors(value, errorCollector, newPath);
        });
  }

  private static void _collectErrors(
      GraphQlValue graphQlValue, ErrorCollector errorCollector, List<Object> path) {
    if (graphQlValue instanceof SingleValue singleValue) {
      singleValue
          .errable()
          .errorOpt()
          .ifPresent(
              throwable -> errorCollector.addError(new DefaultGraphQLErrorInfo(path, throwable)));
      if (graphQlValue instanceof ObjectValue objectValue) {
        objectValue
            .errable()
            .valueOpt()
            .ifPresent(graphQlObjectMap -> graphQlObjectMap._collectErrors(errorCollector, path));
      }
    } else if (graphQlValue instanceof ListValue listValue) {
      listValue
          .errable()
          .valueOpt()
          .ifPresent(
              singleValues -> {
                for (int i = 0; i < singleValues.size(); i++) {
                  List<Object> newPathWithIndex = new ArrayList<>(path);
                  newPathWithIndex.add(i);
                  _collectErrors(singleValues.get(i), errorCollector, newPathWithIndex);
                }
              });
    }
  }

  @SuppressWarnings("unchecked")
  public static class GraphQlObjectMapBuilder<T extends GraphQlObjectMapBuilder<T>> {

    protected final Map<String, GraphQlValue> graphql_data = new LinkedHashMap<>();

    public T addField(String alias, GraphQlValue value) {
      graphql_data.put(alias, value);
      return (T) this;
    }

    public GraphQlEntity build() {
      return new GraphQlEntity(graphql_data);
    }
  }
}
