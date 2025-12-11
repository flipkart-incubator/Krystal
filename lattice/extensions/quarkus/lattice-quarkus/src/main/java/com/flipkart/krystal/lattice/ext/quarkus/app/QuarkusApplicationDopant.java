package com.flipkart.krystal.lattice.ext.quarkus.app;

import static com.flipkart.krystal.lattice.ext.quarkus.app.QuarkusApplicationDopant.APPLICATION_DOPANT_TYPE;

import com.flipkart.krystal.lattice.core.doping.DopantType;
import com.flipkart.krystal.lattice.core.doping.SimpleDopant;
import com.flipkart.krystal.lattice.ext.quarkus.app.QuarkusApplicationSpec.QuarkusApplicationSpecBuilder;
import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.Quarkus;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@DopantType(APPLICATION_DOPANT_TYPE)
@Singleton
@Unremovable
public final class QuarkusApplicationDopant implements SimpleDopant {
  static final String APPLICATION_DOPANT_TYPE = "krystal.lattice.quarkus.app";

  @Inject
  public QuarkusApplicationDopant() {}

  public static QuarkusApplicationSpecBuilder quarkusApplication() {
    return QuarkusApplicationSpec.builder();
  }

  @Override
  public void tryMainMethodExit() {
    Quarkus.waitForExit();
  }
}
