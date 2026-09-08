package com.flipkart.krystal.vajram.lang.rust.ast;

import java.util.List;

/** Declares which Vajrams may invoke a Vajram and metadata attached to that access declaration. */
public sealed interface Callers {

  List<String> annotations();

  /** {@code permit callers `annotation public}. */
  record Public(List<String> annotations) implements Callers {}

  /** {@code permit callers `annotationA vajramA, `annotationB vajramB}. */
  record Named(List<Caller> callers) implements Callers {

    @Override
    public List<String> annotations() {
      return callers.stream().flatMap(caller -> caller.annotations().stream()).toList();
    }
  }

  /** One named caller and the annotations attached directly to it. */
  record Caller(List<String> annotations, String name) {}
}
