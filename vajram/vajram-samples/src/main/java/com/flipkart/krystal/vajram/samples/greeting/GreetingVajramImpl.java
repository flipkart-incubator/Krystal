package com.flipkart.krystal.vajram.samples.greeting;

import com.flipkart.krystal.vajram.ExecutionContext;
import com.flipkart.krystal.vajram.RequestBuilder;
import com.flipkart.krystal.vajram.samples.greeting.GreetingVajramInputUtils.EnrichedRequest;
import com.flipkart.krystal.vajram.samples.greeting.GreetingVajramInputUtils.SessionInputs;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.logging.Logger;

// Auto-generated and managed by Krystal
public final class GreetingVajramImpl extends GreetingVajram {

  @Override
  public ImmutableList<String> executeNonBlocking(ExecutionContext executionContext) {
    GreetingVajramRequest _request =
        new GreetingVajramRequest(executionContext.getValue("user_id"));
    UserInfo userInfo = executionContext.getValue("user_info");
    Logger log = executionContext.getValue("log");
    AnalyticsEventSink analyticsEventSink = executionContext.getValue("analytics_event_sink");
    return ImmutableList.of(
        createGreetingMessage(
            new EnrichedRequest(_request, new SessionInputs(log, analyticsEventSink), userInfo)));
  }

  @Override
  public ImmutableList<RequestBuilder<?>> resolveInputOfDependency(
      String dependency, ImmutableSet<String> resolvableInputs, ExecutionContext executionContext) {
    switch (dependency) {
      case "user_info" -> {
        if (Set.of("user_id").equals(resolvableInputs)) {
          String user_id =
              super.userIdForUserService(
                  executionContext.<GreetingVajramRequest>getValue("user_id").userId());
          return ImmutableList.of(UserServiceVajramRequest.builder().userId(user_id));
        }
      }
    }
    throw new RuntimeException();
  }
}
