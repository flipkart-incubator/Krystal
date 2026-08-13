package com.flipkart.krystal.vajram.graphql.api.model;

import com.flipkart.krystal.data.Errable;
import java.util.List;

public sealed interface GraphQlValue {
  boolean isNullable();

  Errable<?> errable();

  sealed interface SingleValue extends GraphQlValue {}

  record ScalarValue(Errable<?> errable, boolean isNullable) implements SingleValue {

    public ScalarValue(Object value, boolean isNullable) {
      this(Errable.withValue(value), isNullable);
    }
  }

  record ObjectValue(Errable<GraphQlEntity> errable, boolean isNullable) implements SingleValue {

    public ObjectValue(GraphQlEntity value, boolean isNullable) {
      this(Errable.withValue(value), isNullable);
    }
  }

  record ListValue(Errable<List<SingleValue>> errable, boolean isNullable) implements GraphQlValue {

    public ListValue(List<SingleValue> values, boolean isNullable) {
      this(Errable.withValue(values), isNullable);
    }
  }
}
