package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello;

class HelloInputUtils {
  record EnrichedRequest(HelloRequest _request) {
    String name() {
      return _request().name();
    }
  }
}
