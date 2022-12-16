package com.flipkart.krystal.vajram.samples.greeting;

import com.flipkart.krystal.vajram.ExecutionContextMap;
import com.flipkart.krystal.vajram.inputs.InputValues;
import com.flipkart.krystal.vajram.inputs.SingleValue;
import com.flipkart.krystal.vajram.samples.greeting.GreetingVajramInputUtils.EnrichedRequest;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Set;

// Auto-generated and managed by Krystal
public final class GreetingVajramImpl extends GreetingVajram {

  @Override
  public String executeNonBlocking(ExecutionContextMap executionContext) {
    return createGreetingMessage(EnrichedRequest.from(executionContext));
  }

  @Override
  public ImmutableList<InputValues> resolveInputOfDependency(
      String dependency,
      ImmutableSet<String> resolvableInputs,
      ExecutionContextMap executionContext) {
    switch (dependency) {
      case "user_info" -> {
        if (Set.of("user_id").equals(resolvableInputs)) {
          String user_id =
              super.userIdForUserService(
                  executionContext.<GreetingVajramRequest>getValue("user_id").userId());
          return ImmutableList.of(
              new InputValues(ImmutableMap.of("user_id", new SingleValue<>(user_id))));
        }
      }
    }
    throw new IllegalArgumentException();
  }
}
