package com.flipkart.krystal.lattice.core.doping;

import java.lang.annotation.Annotation;

/**
 * A "dopant" is used to add capabilities to a lattice application and to control how the
 * application is executed.
 */
public interface Dopant<A extends Annotation, C extends DopantConfig> {

  default void start() throws Exception {}

  /**
   * Invoked just before existing the main method. Dopant might choose to block in this method to
   * prevent the application from exiting. If this method returns for all dopants, then the
   * application is terminated.
   *
   * @throws InterruptedException
   */
  default void tryMainMethodExit() throws InterruptedException {}
}
