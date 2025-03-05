package com.flipkart.krystal.vajram.samples.greet;

public interface AnalyticsEventSink {

  void pushEvent(String eventType, GreetingEvent greetingEvent);
}
