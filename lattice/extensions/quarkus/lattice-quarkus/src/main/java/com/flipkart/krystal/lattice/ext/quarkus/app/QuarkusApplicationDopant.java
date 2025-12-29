package com.flipkart.krystal.lattice.ext.quarkus.app;

import static com.flipkart.krystal.lattice.ext.quarkus.app.QuarkusApplicationDopant.APPLICATION_DOPANT_TYPE;

import com.flipkart.krystal.lattice.core.doping.DopantType;
import com.flipkart.krystal.lattice.core.doping.SimpleDopant;
import io.quarkus.runtime.Quarkus;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@DopantType(APPLICATION_DOPANT_TYPE)
@Singleton
public final class QuarkusApplicationDopant implements SimpleDopant {
  static final String APPLICATION_DOPANT_TYPE = "krystal.lattice.quarkus.app";

  @Inject
  public QuarkusApplicationDopant() {}

  @Override
  public int tryApplicationExit() {
    Quarkus.waitForExit();
    return 0;
  }
}
