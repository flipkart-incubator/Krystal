package com.flipkart.krystal.krystex.decoration;

public non-sealed interface FlushableDecorator extends Decorator {
  void flushDecorator(FlushCommand flushCommand);
}
