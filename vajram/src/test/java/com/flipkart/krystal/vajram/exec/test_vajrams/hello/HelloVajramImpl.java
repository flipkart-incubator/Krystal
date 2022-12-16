package com.flipkart.krystal.vajram.exec.test_vajrams.hello;

import com.flipkart.krystal.vajram.ExecutionContextMap;
import com.flipkart.krystal.vajram.exec.test_vajrams.hello.HelloInputUtils.EnrichedRequest;

// Auto generated and managed by Krystal
public class HelloVajramImpl extends HelloVajram {

  @Override
  public String executeNonBlocking(ExecutionContextMap executionContext) {
    Object nameValue = executionContext.getValue("name");
    String name;
    if (nameValue instanceof String string) {
      name = string;
    } else {
      throw new IllegalArgumentException(
          "Unsupported type %s for input %s in vajram %s"
              .formatted(nameValue.getClass(), "name", "GreetVajram"));
    }
    return super.greet(new EnrichedRequest(new HelloRequest(name)));
  }
}
