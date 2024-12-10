package com.flipkart.krystal.vajram.samples.greeting;

public interface AnalyticsEventSink {

  void pushEvent(String eventType, GreetingEvent greetingEvent);
}
