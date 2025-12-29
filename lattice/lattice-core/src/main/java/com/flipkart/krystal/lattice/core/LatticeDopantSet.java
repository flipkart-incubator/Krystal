package com.flipkart.krystal.lattice.core;

import com.flipkart.krystal.lattice.core.doping.Dopant;
import com.google.common.collect.ImmutableList;

public record LatticeDopantSet(ImmutableList<Dopant> dopants) {

  public LatticeDopantSet(Dopant... dopants) {
    this(ImmutableList.copyOf(dopants));
  }
}
