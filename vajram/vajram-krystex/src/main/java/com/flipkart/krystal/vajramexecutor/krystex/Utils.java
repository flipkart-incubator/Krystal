package com.flipkart.krystal.vajramexecutor.krystex;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.flipkart.krystal.krystex.MultiValue;
import com.flipkart.krystal.krystex.SingleValue;
import com.flipkart.krystal.krystex.Value;
import com.flipkart.krystal.krystex.node.NodeInputs;
import com.flipkart.krystal.vajram.ValueOrError;
import com.flipkart.krystal.vajram.inputs.InputValues;
import java.util.Map.Entry;

public class Utils {

  public static InputValues toInputValues(NodeInputs nodeInputs) {
    return new InputValues(
        nodeInputs.values().entrySet().stream()
            .collect(toImmutableMap(Entry::getKey, e -> toValueOrError(e.getValue()))));
  }

  public static ValueOrError<?> toValueOrError(Value value) {
    if (value instanceof SingleValue<?> singleValue) {
      return new ValueOrError<>(singleValue.value(), singleValue.failureReason());
    } else if (value instanceof MultiValue<?> multiValue) {
      return new ValueOrError<>(
          multiValue.values().entrySet().stream()
              .collect(
                  toImmutableMap(
                      e2 -> toInputValues(e2.getKey()), e2 -> toValueOrError(e2.getValue()))));
    }
    throw new UnsupportedOperationException();
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
}
