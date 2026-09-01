package com.flipkart.krystal.vajram.lang.rust.ast;

import java.util.List;

/** An annotation on a Vajram declaration, retaining named arguments. */
public record VajramAnnotation(String name, List<Argument> arguments) {

  /** A named annotation argument. */
  public record Argument(String name, Expr value) {}
}
