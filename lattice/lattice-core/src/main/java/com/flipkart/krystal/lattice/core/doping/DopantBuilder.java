package com.flipkart.krystal.lattice.core.doping;

public interface DopantBuilder<D extends Dopant> {
  D _build();

  /**
   * A dopant builder may depend on other dopant builders by declaring them via the {@link
   * DependsOn} annotation on the dopant class. This method is invoked with the dopant builders that
   * this dopant builder depends on, one at a time. This dopant builder may choose to modify the
   * dependency dopant builder. This feature allows one dopant to dictate the configuration of
   * another dopant that it depends on.
   *
   * <p>If dopant builders dependency declarations have a cycle, a build time error is thrown.
   *
   * @param dopantBuilders the builders of dopants that this dopant depends on
   */
  default void _configure(DopantBuilder<?> dopantBuilders) {}
}
