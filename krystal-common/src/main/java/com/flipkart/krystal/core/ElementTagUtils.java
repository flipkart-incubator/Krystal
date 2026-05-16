package com.flipkart.krystal.core;

import com.flipkart.krystal.annos.ElementTagUtilityOf;
import com.flipkart.krystal.annos.Transitive;
import java.lang.annotation.Annotation;
import java.util.Collection;

/**
 * ElementTagUtils are used to handle annotations on elements in the Krystal graph. Example use
 * cases include: when an element infers multiple non-repeatable {@link Transitive} annotations from
 * different sources (dependencies, for example)
 *
 * <p>All implementations of this interface must have a public no-arg constructor
 *
 * <p>All implementations of this interface must be annotated with @{@link ElementTagUtilityOf}
 *
 * @param <T> The annotation type handled by this ElementTagUtils
 */
public interface ElementTagUtils<T extends Annotation> {

  /**
   * If an element (for example a vajram) infers multiple non-repeatable {@link Transitive}
   * annotations from different sources (dependencies, for example), this method is used to resolve
   * the conflict to infer a final, effective annotation.
   *
   * <p>This means that a transitive annotation must adhere to a <a
   * href="https://en.wikipedia.org/wiki/Total_order">total order</a> so that conflicts between any
   * two annotations of that type can be resolved unambiguously to a single annotation. If such a
   * total order is not possible to define for an annotation type, then that annotation type must
   * not be declared as a {@link Transitive} annotation.
   *
   * @param annotations the annotations of type T which need to conflict-resolved
   * @return The final, effective annotation of type T
   * @throws IllegalArgumentException if any of the annotations is not of type T
   * @implNote This method accepts {@code Collection<Annotation>} rather than {@code Collection<T>}
   *     for convenience of classes which create an instance of this type using reflection.
   */
  @SuppressWarnings("unchecked")
  T resolve(Collection<Annotation> annotations) throws IllegalArgumentException;

  /**
   * Returns 1 if a1 has precedence over a2, -1 if a2 has precedence over a1, and 0 if they have
   * equal precedence
   */
  int compare(Annotation a1, Annotation a2);
}
