package com.flipkart.krystal.vajram.samples.greeting;

import com.flipkart.krystal.vajram.ExecutionContextMap;
import com.flipkart.krystal.vajram.inputs.InputValues;
import com.flipkart.krystal.vajram.inputs.ValueOrError;
//import com.flipkart.krystal.vajram.samples.greeting.GreetingInputUtil.AllInputs;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Set;

// Auto-generated and managed by Krystal
public final class GreetingVajramImpl extends GreetingVajram {

//  @Override
//  public String executeCompute(ExecutionContextMap executionContext) {
//    return createGreetingMessage(
//        new AllInputs(
//            executionContext.getValue("user_id"),
//            executionContext.getValue("user_info"),
//            executionContext.getValue("log", null),
//            executionContext.getValue("analytics_event_sink", null)));
//  }

  @Override
  public ImmutableList<InputValues> resolveInputOfDependency(
      String dependency,
      ImmutableSet<String> resolvableInputs,
      ExecutionContextMap executionContext) {
    switch (dependency) {
      case "user_info" -> {
        if (Set.of("user_id").equals(resolvableInputs)) {
          String userId = super.userIdForUserService(executionContext.getValue("user_id"));
          return ImmutableList.of(
              new InputValues(ImmutableMap.of("user_id", new ValueOrError<>(userId))));
        }
      }
    }
    throw new IllegalArgumentException();
  }
}
