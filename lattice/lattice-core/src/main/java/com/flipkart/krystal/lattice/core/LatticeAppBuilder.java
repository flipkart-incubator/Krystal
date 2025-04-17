package com.flipkart.krystal.lattice.core;

public interface LatticeAppBuilder {
  /**
   * Adds a {@link Dopant} to the lattice application. A "dopant" is used to add capabilities to the
   * krystal graph hosted by a lattice application or to control how the graph is executed.
   *
   * @param dopant the dopant to add
   * @return this builder
   */
  LatticeAppBuilder add(Dopant dopant);

  /**
   * Builds the lattice application.
   *
   * @return the built lattice application
   */
  LatticeApplication build();
}
