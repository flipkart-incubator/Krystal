package com.flipkart.krystal.vajram.graphql.api.model;

import java.util.LinkedHashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class GraphQlOperationImpl extends GraphQlObjectImpl implements GraphQlOperation {

  private final @Nullable Map<Object, Object> extensions;

  public GraphQlOperationImpl(Map<String, GraphQlValue> graphql_values) {
    this(graphql_values, null);
  }

  public GraphQlOperationImpl(
      Map<String, GraphQlValue> graphql_values, @Nullable Map<Object, Object> extensions) {
    super(graphql_values);
    this.extensions = extensions;
  }

  @Override
  public @Nullable Map<Object, Object> graphql_extensions() {
    return extensions;
  }

  public static class GraphQlOperationImplBuilder
      extends GraphQlObjectImplBuilder<GraphQlOperationImplBuilder> {

    private @Nullable Map<Object, Object> extensions;

    public GraphQlOperationImplBuilder addExtension(Object key, Object value) {
      if (extensions == null) {
        extensions = new LinkedHashMap<>();
      }
      extensions.put(key, value);
      return this;
    }

    public GraphQlOperationImpl build() {
      return new GraphQlOperationImpl(graphql_data, extensions);
    }
  }
}
