package com.flipkart.krystal.vajram.exec.test_vajrams.hello;

import com.flipkart.krystal.vajram.ExecutionContext;
import com.flipkart.krystal.vajram.RequestBuilder;
import com.flipkart.krystal.vajram.exec.test_vajrams.hello.InputUtils.AllInputs;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;

// Auto generated and managed by Krystal
public class HelloVajramImpl extends HelloVajram {

  @Override
  public ImmutableList<String> executeNonBlocking(ExecutionContext executionContext) {
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
    return ImmutableList.of(super.greet(allInputs));
  }

  @Override
  public ImmutableList<RequestBuilder<?>> resolveInputOfDependency(
      String dependency, ImmutableSet<String> resolvableInputs, ExecutionContext executionContext) {
    throw new UnsupportedOperationException();
  }
}
