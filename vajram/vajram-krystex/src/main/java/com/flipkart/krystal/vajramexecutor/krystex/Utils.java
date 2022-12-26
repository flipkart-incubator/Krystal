package com.flipkart.krystal.vajramexecutor.krystex;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.flipkart.krystal.krystex.node.NodeInputs;
import com.flipkart.krystal.vajram.inputs.InputValues;
import com.flipkart.krystal.vajram.inputs.ValueOrError;
import java.util.Map.Entry;

public class Utils {

  public static InputValues toInputValues(NodeInputs nodeInputs) {
    return new InputValues(
        nodeInputs.values().entrySet().stream()
            .collect(
                toImmutableMap(
                    Entry::getKey,
                    e -> new ValueOrError<>(e.getValue().value(), e.getValue().failureReason()))));
  }

  static NodeInputs toNodeInputs(InputValues inputValues) {
    return new NodeInputs(
        inputValues.values().entrySet().stream()
            .collect(
                toImmutableMap(
                    Entry::getKey,
                    e ->
                        new com.flipkart.krystal.krystex.SingleValue<>(
                            e.getValue().value(), e.getValue().error()))));
  }

  public static ValueOrError<?> toSingleValue(
      com.flipkart.krystal.krystex.SingleValue<?> singleValue) {
    return new ValueOrError<>(singleValue.value(), singleValue.failureReason());
  }
}
