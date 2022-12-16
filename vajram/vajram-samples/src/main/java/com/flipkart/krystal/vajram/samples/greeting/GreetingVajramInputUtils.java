package com.flipkart.krystal.vajram.samples.greeting;

import com.flipkart.krystal.vajram.ExecutionContextMap;
import java.util.logging.Logger;

// Auto-generated and managed by Krystal
final class GreetingVajramInputUtils {

  record SessionInputs(Logger log, AnalyticsEventSink analyticsEventSink) {}

  record EnrichedRequest(
      GreetingVajramRequest _request, SessionInputs _sessionInputs, UserInfo userInfo) {

    public Logger log() {
      return _sessionInputs().log();
    }

    public String userId() {
      return _request().userId();
    }

    public AnalyticsEventSink analyticsEventSink() {
      return _sessionInputs().analyticsEventSink();
    }

    static EnrichedRequest from(ExecutionContextMap executionContext) {
      return new EnrichedRequest(
          GreetingVajramRequest.builder().userId(executionContext.getValue("user_id")).build(),
          new SessionInputs(
              executionContext.getValue("log"), executionContext.getValue("analytics_event_sink")),
          executionContext.getValue("user_info"));
    }
  }
}
