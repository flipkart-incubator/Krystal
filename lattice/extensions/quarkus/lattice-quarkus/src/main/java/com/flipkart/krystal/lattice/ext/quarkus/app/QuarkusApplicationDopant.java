package com.flipkart.krystal.lattice.ext.quarkus.app;

import com.flipkart.krystal.lattice.core.doping.SimpleDopant;
import com.flipkart.krystal.lattice.ext.quarkus.app.QuarkusApplicationSpec.QuarkusApplicationSpecBuilder;
import io.quarkus.runtime.Quarkus;
import io.vertx.core.Vertx;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class QuarkusApplicationDopant implements SimpleDopant {
  static final String APPLICATION_DOPANT_TYPE = "krystal.lattice.quarkus.app";

  private final Vertx vertx;

  @Inject
  public QuarkusApplicationDopant() {
    this.vertx = CDI.current().select(Vertx.class).get();
  }

  public static QuarkusApplicationSpecBuilder quarkusApplication() {
    return QuarkusApplicationSpec.builder();
  }

  public Vertx vertx() {
    return vertx;
  }

  @Override
  public void tryMainMethodExit() {
    Quarkus.waitForExit();
  }
}
