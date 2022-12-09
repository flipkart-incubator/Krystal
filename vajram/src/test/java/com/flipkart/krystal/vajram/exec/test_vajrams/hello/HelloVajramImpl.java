package com.flipkart.krystal.vajram.exec.test_vajrams.hello;

import com.flipkart.krystal.vajram.ExecutionContextMap;
import com.flipkart.krystal.vajram.exec.test_vajrams.hello.InputUtils.AllInputs;
import java.util.Optional;

// Auto generated and managed by Krystal
public class HelloVajramImpl extends HelloVajram {

  @Override
  public String executeNonBlocking(ExecutionContextMap executionContext) {
    Object nameValue = executionContext.getValue("name");
    String name;
    if (nameValue instanceof HelloRequest.Builder builder) {
      name = builder.name();
    } else if (nameValue instanceof Optional<?> optional && optional.isPresent()) {
      name = (String) optional.get();
    } else {
      throw new IllegalArgumentException(
          "Unsupported type %s for input %s in vajram %s"
              .formatted(nameValue.getClass(), "name", "GreetVajram"));
    }
    AllInputs allInputs = new AllInputs(new HelloRequest(name));
    return super.greet(allInputs);
  }
}
