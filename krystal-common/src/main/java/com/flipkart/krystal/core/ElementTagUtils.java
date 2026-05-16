package com.flipkart.krystal.core;

import java.lang.annotation.Annotation;
import java.util.Collection;

public interface ElementTagUtils<T extends Annotation> {

  @SuppressWarnings("unchecked")
  T resolve(Collection<Annotation> annotations);

  /**
   * Returns 1 if a1 has precedence of a2, -1 if a2 has precedence of a1, and 0 if they have equal
   * precedence
   */
  int compare(Annotation a1, Annotation a2);
}
