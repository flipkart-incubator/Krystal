package com.flipkart.krystal.lattice.ext.rest.dropwizard;

import static com.flipkart.krystal.data.Errable.nil;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.Failure;
import com.flipkart.krystal.data.Nil;
import com.flipkart.krystal.data.NonNil;
import com.flipkart.krystal.lattice.ext.rest.RestServiceDopant;
import com.flipkart.krystal.lattice.ext.rest.config.RestServiceDopantConfig;
import com.flipkart.krystal.lattice.ext.rest.visualization.StaticKrystalGraphResource;
import io.dropwizard.core.Application;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Environment;
import jakarta.inject.Inject;
import org.eclipse.jetty.server.Server;

public abstract class DropwizardRestApplication extends Application<Configuration> {

  private final RestServiceDopant restServiceDopant;
  private final RestServiceDopantConfig restServiceDopantConfig;
  private final StaticKrystalGraphResource krystalGraphResource;
  private final FlowPublisherMessageBodyWriter flowPublisherMessageBodyWriter;

  private Errable<Server> server = nil();

  @Inject
  public DropwizardRestApplication(
      RestServiceDopant restServiceDopant,
      RestServiceDopantConfig restServiceDopantConfig,
      StaticKrystalGraphResource krystalGraphResource,
      FlowPublisherMessageBodyWriter flowPublisherMessageBodyWriter) {
    this.restServiceDopant = restServiceDopant;
    this.restServiceDopantConfig = restServiceDopantConfig;
    this.krystalGraphResource = krystalGraphResource;
    this.flowPublisherMessageBodyWriter = flowPublisherMessageBodyWriter;
  }

  @Override
  public void run(Configuration configuration, Environment environment) {
    boolean serveStaticKrystalGraph =
        restServiceDopantConfig.applicationServer().serveStaticKrystalGraph();
    environment
        .lifecycle()
        .addServerLifecycleListener(server -> this.server = Errable.withValue(server));
    restServiceDopant.allApplicationResources().forEach(environment.jersey()::register);
    if (serveStaticKrystalGraph) {
      environment.jersey().register(krystalGraphResource);
    }
    environment.jersey().register(flowPublisherMessageBodyWriter);
  }

  @SuppressWarnings("BusyWait")
  public void awaitTermination() throws Throwable {
    while (server instanceof Nil<Server>) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
    if (server instanceof Nil<Server>) {
      return;
    } else if (server instanceof Failure<Server> failure) {
      throw failure.error();
    } else if (server instanceof NonNil<Server> success) {
      success.value().join();
    }
  }
}
