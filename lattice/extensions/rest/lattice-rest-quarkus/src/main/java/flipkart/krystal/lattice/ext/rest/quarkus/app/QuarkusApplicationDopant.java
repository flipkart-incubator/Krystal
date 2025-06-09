package flipkart.krystal.lattice.ext.rest.quarkus.app;

import static com.google.common.util.concurrent.Uninterruptibles.awaitUninterruptibly;
import static java.time.Duration.ofMinutes;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.lattice.core.doping.Dopant;
import com.flipkart.krystal.lattice.core.doping.DopantConfig.NoAnnotation;
import com.flipkart.krystal.lattice.core.doping.DopantConfig.NoConfiguration;
import com.flipkart.krystal.lattice.core.doping.SimpleDopant;
import com.google.common.util.concurrent.Uninterruptibles;
import flipkart.krystal.lattice.ext.rest.quarkus.app.QuarkusApplicationSpec.QuarkusApplicationSpecBuilder;
import io.quarkus.runtime.Quarkus;
import io.vertx.core.Vertx;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

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
