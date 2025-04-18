package com.flipkart.krystal.krystex.dependencydecoration;

import com.flipkart.krystal.krystex.Decorator;
import com.flipkart.krystal.krystex.kryon.KryonCommandResponse;

@FunctionalInterface
public non-sealed interface DependencyDecorator extends Decorator {

  <R extends KryonCommandResponse> VajramInvocation<R> decorateDependency(
      VajramInvocation<R> invocationToDecorate);

  DependencyDecorator NO_OP =
      new DependencyDecorator() {
        @Override
        public <R extends KryonCommandResponse> VajramInvocation<R> decorateDependency(
            VajramInvocation<R> invocationToDecorate) {
          return invocationToDecorate;
        }
      };
}
