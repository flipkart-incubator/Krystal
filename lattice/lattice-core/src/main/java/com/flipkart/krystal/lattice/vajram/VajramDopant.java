package com.flipkart.krystal.lattice.vajram;

import com.flipkart.krystal.lattice.core.doping.DopantType;
import com.flipkart.krystal.lattice.core.doping.SimpleDopant;
import com.flipkart.krystal.vajramexecutor.krystex.VajramGraph;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@DopantType(VajramDopant.DOPANT_TYPE)
public final class VajramDopant implements SimpleDopant {

  static final String DOPANT_TYPE = "krystal.lattice.vajram";

  @Getter(onMethod_ = {@Produces, @Singleton})
  private final VajramGraph vajramGraph;

  @Inject
  VajramDopant(VajramDopantSpec vajramDopantSpec) {
    this.vajramGraph = vajramDopantSpec.vajramGraphBuilder().build();
  }
}
