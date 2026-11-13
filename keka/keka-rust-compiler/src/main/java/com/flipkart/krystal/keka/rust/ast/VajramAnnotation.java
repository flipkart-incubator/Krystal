package com.flipkart.krystal.keka.rust.ast;

import java.util.List;

/** An annotation on a Vajram declaration, retaining named arguments. */
public record VajramAnnotation(String name, List<Argument> arguments) {

  /** Whether any of {@code annotations} has this name. */
  public static boolean has(List<VajramAnnotation> annotations, String name) {
    return annotations.stream().anyMatch(annotation -> annotation.name().equals(name));
  }

  /** The names of {@code annotations}, in order. */
  public static List<String> names(List<VajramAnnotation> annotations) {
    return annotations.stream().map(VajramAnnotation::name).toList();
  }

  /** A named annotation argument. */
  public record Argument(String name, Expr value) {}
}
