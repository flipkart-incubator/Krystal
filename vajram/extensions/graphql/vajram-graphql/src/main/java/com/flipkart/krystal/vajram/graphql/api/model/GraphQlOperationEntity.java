package com.flipkart.krystal.vajram.graphql.api.model;

import java.util.LinkedHashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class GraphQlOperationEntity extends GraphQlEntity implements GraphQlOperation {

  private final @Nullable Map<Object, Object> extensions;

  public GraphQlOperationEntity(Map<String, GraphQlValue> graphql_values) {
    this(graphql_values, null);
  }

  public GraphQlOperationEntity(
      Map<String, GraphQlValue> graphql_values, @Nullable Map<Object, Object> extensions) {
    super(graphql_values);
    this.extensions = extensions;
  }

  @Override
  public @Nullable Map<Object, Object> graphql_extensions() {
    return extensions;
  }

  public static class GraphQlOperationObjectMapBuilder
      extends GraphQlObjectMapBuilder<GraphQlOperationObjectMapBuilder> {

    private @Nullable Map<Object, Object> extensions;

    public GraphQlOperationObjectMapBuilder addExtension(Object key, Object value) {
      if (extensions == null) {
        extensions = new LinkedHashMap<>();
      }
      extensions.put(key, value);
      return this;
    }

    public GraphQlOperationEntity build() {
      return new GraphQlOperationEntity(graphql_data, extensions);
    }
  }
}
