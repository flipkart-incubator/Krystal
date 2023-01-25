package com.flipkart.krystal.krystex.decoration;

public final class TerminateDecoration implements LogicDecoratorCommand {

  private static final TerminateDecoration INSTANCE = new TerminateDecoration();

  public static TerminateDecoration instance() {
    return INSTANCE;
  }

  private TerminateDecoration() {}
}
