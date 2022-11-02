package com.flipkart.krystal.vajram.samples.greeting;

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
  }
}
