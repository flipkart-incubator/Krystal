package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello;

import com.flipkart.krystal.vajram.ExecutionContextMap;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello.HelloInputUtils.EnrichedRequest;

// Auto generated and managed by Krystal
public class HelloVajramImpl extends HelloVajram {

  @Override
  public String executeNonBlocking(ExecutionContextMap executionContext) {
    return super.greet(
        new EnrichedRequest(
            new HelloRequest(
                executionContext.getValue("name"), executionContext.optValue("greeting"))));
  }
}
