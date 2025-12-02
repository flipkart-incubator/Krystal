package com.flipkart.krystal.krystex.dependencydecoration;

import com.flipkart.krystal.krystex.decoration.Decorator;
import com.flipkart.krystal.krystex.kryon.KryonCommandResponse;

@FunctionalInterface
public non-sealed interface DependencyDecorator extends Decorator {

  <R extends KryonCommandResponse> DependencyInvocation<R> decorateDependency(
      DependencyInvocation<R> invocationToDecorate);

  DependencyDecorator NO_OP =
      new DependencyDecorator() {
        @Override
        public <R extends KryonCommandResponse> DependencyInvocation<R> decorateDependency(
            DependencyInvocation<R> invocationToDecorate) {
          return invocationToDecorate;
        }
      };
}
