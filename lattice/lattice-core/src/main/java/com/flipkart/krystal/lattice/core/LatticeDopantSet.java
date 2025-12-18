package com.flipkart.krystal.lattice.core;

import com.flipkart.krystal.lattice.core.doping.Dopant;
import com.flipkart.krystal.lattice.core.execution.ThreadingStrategyDopant;
import com.flipkart.krystal.lattice.krystex.KrystexDopant;
import com.flipkart.krystal.lattice.vajram.VajramDopant;
import com.google.common.collect.ImmutableList;
import java.util.List;

public record LatticeDopantSet(ImmutableList<Dopant> dopants) {

  public LatticeDopantSet(Dopant... dopants) {
    this(ImmutableList.copyOf(dopants));
  }
}
