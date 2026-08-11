package com.flipkart.krystal.vajram.graphql.api.model;

import com.flipkart.krystal.data.Errable;
import java.util.List;

public sealed interface GraphQlValue {
  boolean isNullable();

  Errable<?> errable();

  sealed interface SingleValue extends GraphQlValue {}

  record ScalarValue(Errable<?> errable, boolean isNullable) implements SingleValue {}

  record ObjectValue(Errable<GraphQlEntity> errable, boolean isNullable) implements SingleValue {}

  record ListValue(Errable<List<SingleValue>> errable, boolean isNullable)
      implements GraphQlValue {}
}
