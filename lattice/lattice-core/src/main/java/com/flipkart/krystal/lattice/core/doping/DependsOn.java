package com.flipkart.krystal.lattice.core.doping;

public @interface DependsOn {
  Class<? extends Dopant>[] mandatory();

  Class<? extends Dopant>[] optional();
}
