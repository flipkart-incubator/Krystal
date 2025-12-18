package com.flipkart.krystal.lattice.core.doping;

import java.lang.annotation.Annotation;

/**
 * A "dopant" is used to add capabilities to a lattice application and to control how the
 * application is executed.
 */
public interface Dopant<A extends Annotation, C extends DopantConfig> {

  default void start(String... commandLineArgs) throws Exception {
    start();
  }

  default void start() throws Exception {}

  /**
   * Invoked just before existing the application main method. The dopant might choose to block in
   * this method to prevent the application from exiting. If this method returns for all dopants of
   * the lattice application, then the lattice app exits.
   *
   * <p>The exit code of the lattice app is 0 if this method returns 0 for all dopants.
   *
   * <p>If any of the dopants return a non-zero exit code, then the lattice app returns the non-zero
   * exit code of the dopant which is last in order among all dopants.
   *
   * @throws InterruptedException if thread is interrupted when this method blocks
   * @return the exit code for this dopant. Non-zero exit codes are considered errors.
   */
  default int tryApplicationExit() throws InterruptedException {
    return 0;
  }
}
