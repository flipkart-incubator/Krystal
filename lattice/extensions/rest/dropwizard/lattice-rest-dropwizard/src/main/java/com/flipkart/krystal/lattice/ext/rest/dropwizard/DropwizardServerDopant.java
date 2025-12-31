package com.flipkart.krystal.lattice.ext.rest.dropwizard;

import com.flipkart.krystal.lattice.core.doping.DopantType;
import com.flipkart.krystal.lattice.core.doping.DopantWithConfig;
import com.flipkart.krystal.lattice.ext.rest.RestService;
import jakarta.inject.Inject;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.Nullable;

@DopantType(DropwizardServerDopant.DOPANT_TYPE)
public final class DropwizardServerDopant
    implements DopantWithConfig<DropwizardServerDopantConfig> {

  public static final String DOPANT_TYPE = "krystal.lattice.DropwizardServerDopant";

  private final @Nullable RestService restService;
  private final DropwizardServerDopantConfig config;
  private final DropwizardRestApplication app;

  @Inject
  public DropwizardServerDopant(
      @Nullable RestService restService,
      DropwizardServerDopantConfig config,
      DropwizardRestApplication app) {
    this.restService = restService;
    this.config = config;
    this.app = app;
  }

  @Override
  public void start(String... commandLineArgs) throws Exception {
    String[] dwArgs;
    if (restService != null) {
      dwArgs = Stream.concat(Stream.of("server"), config.args().stream()).toArray(String[]::new);
    } else {
      dwArgs = config.args().toArray(String[]::new);
    }
    app.run(dwArgs);
  }

  @Override
  public int tryApplicationExit() {
    try {
      app.awaitTermination();
    } catch (Throwable e) {
      return 1;
    }
    return 0;
  }
}
