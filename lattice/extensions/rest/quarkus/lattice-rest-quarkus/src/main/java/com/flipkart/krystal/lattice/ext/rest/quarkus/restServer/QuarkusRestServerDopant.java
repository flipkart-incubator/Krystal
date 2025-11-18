package com.flipkart.krystal.lattice.ext.rest.quarkus.restServer;

import static com.flipkart.krystal.lattice.core.headers.StandardHeaderNames.REQUEST_ID;
import static io.netty.buffer.Unpooled.wrappedBuffer;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;

import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig.KryonExecutorConfigBuilder;
import com.flipkart.krystal.lattice.core.di.Bindings;
import com.flipkart.krystal.lattice.core.di.Bindings.BindingsBuilder;
import com.flipkart.krystal.lattice.core.doping.SimpleDopant;
import com.flipkart.krystal.lattice.core.headers.Header;
import com.flipkart.krystal.lattice.core.headers.SingleValueHeader;
import com.flipkart.krystal.lattice.ext.quarkus.app.QuarkusApplicationDopant;
import com.flipkart.krystal.lattice.ext.rest.RestService;
import com.flipkart.krystal.lattice.ext.rest.RestServiceDopant;
import com.flipkart.krystal.lattice.ext.rest.api.status.HttpResponseStatusException;
import com.flipkart.krystal.lattice.ext.rest.config.RestServerConfig;
import com.flipkart.krystal.lattice.ext.rest.quarkus.restServer.QuarkusRestServerSpec.QuarkusRestServerSpecBuilder;
import com.flipkart.krystal.serial.SerializableModel;
import com.flipkart.krystal.tags.Names;
import io.quarkus.vertx.utils.NoBoundChecksBuffer;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jboss.resteasy.plugins.server.vertx.VertxRegistry;
import org.jboss.resteasy.plugins.server.vertx.VertxRequestHandler;
import org.jboss.resteasy.plugins.server.vertx.VertxResteasyDeployment;

@Slf4j
public final class QuarkusRestServerDopant implements SimpleDopant {
  static final String QUARKUS_REST_SERVER_DOPANT_TYPE = "krystal.lattice.restServer.quarkus";

  private final RestService restService;
  private final QuarkusApplicationDopant quarkusApplicationDopant;
  private final RestServiceDopant restServiceDopant;
  private final List<AutoCloseable> closeables = new ArrayList<>();

  @Inject
  QuarkusRestServerDopant(
      RestService restService,
      RestServiceDopant restServiceDopant,
      QuarkusApplicationDopant quarkusApplicationDopant) {
    this.restService = restService;
    this.quarkusApplicationDopant = quarkusApplicationDopant;
    this.restServiceDopant = restServiceDopant;
  }

  public static QuarkusRestServerSpecBuilder quarkusRestServer() {
    return QuarkusRestServerSpec.builder();
  }

  @Override
  public void start() {
    Vertx vertx = quarkusApplicationDopant.vertx();

    startServer(
        restServiceDopant.config().applicationServer(),
        vertx,
        restServiceDopant.allApplicationRestResources());
    RestServerConfig adminServer = restServiceDopant.config().adminServer();
    if (adminServer != null) {
      startServer(adminServer, vertx, restServiceDopant.allAdminRestResources());
    }
  }

  private void startServer(RestServerConfig restServerConfig, Vertx vertx, List<?> restResources) {
    HttpServerOptions httpServerOptions = new HttpServerOptions();

    httpServerOptions.setPort(restServerConfig.port());
    vertx
        .createHttpServer(httpServerOptions)
        .requestHandler(jaxRsRequestHandler(vertx, restResources))
        .listen(
            result -> {
              if (result.succeeded()) {
                log.info(
                    "Server '{}' started on port {} ",
                    restServerConfig.name(),
                    restServerConfig.port());
              } else {
                log.error("Could not start server {}", restServerConfig.name(), result.cause());
              }
            });
  }

  private Handler<HttpServerRequest> jaxRsRequestHandler(Vertx vertx, List<?> serverResources) {
    VertxResteasyDeployment deployment = new VertxResteasyDeployment();
    deployment.start();

    closeables.add(deployment::stop);
    VertxRegistry registry = deployment.getRegistry();
    serverResources.forEach(registry::addSingletonResource);
    return new VertxRequestHandler(vertx, deployment, restService.pathPrefix());
  }

  private <RespT> void executeHttpRequest(
      RoutingContext routingContext, CompletionStage<ImmutableRequest<RespT>> requestFuture) {

    HttpServerResponse httpResponse = routingContext.response();

    String requestId = routingContext.request().getHeader(REQUEST_ID);
    Bindings seedMap = getRequestScopeSeedBindings(routingContext, requestId);

    requestFuture
        .thenCompose(
            request -> {
              KryonExecutorConfigBuilder configBuilder = KryonExecutorConfig.builder();
              if (requestId != null) {
                configBuilder.executorId(requestId);
              }
              return restServiceDopant.executeHttpRequest(request, seedMap, configBuilder);
            })
        .thenAccept(
            response -> {
              try {
                if (response == null) {
                  httpResponse.end();
                } else if (response instanceof byte[] bytes) {
                  httpResponse.end(new NoBoundChecksBuffer(wrappedBuffer(bytes)));
                } else if (response instanceof SerializableModel serializableResponse) {
                  httpResponse
                      .putHeader(
                          CONTENT_TYPE, serializableResponse._serdeProtocol().defaultContentType())
                      .end(
                          new NoBoundChecksBuffer(
                              wrappedBuffer(serializableResponse._serialize())));
                } else {
                  log.error(
                      "Executing vajram request of type {} returned a non-serializable response model of type {}",
                      requestFuture.toCompletableFuture().join().getClass(),
                      response.getClass());
                  routingContext.fail(INTERNAL_SERVER_ERROR.code());
                }
              } catch (Throwable e) {
                routingContext.fail(INTERNAL_SERVER_ERROR.code());
              }
            })
        .whenComplete(
            (response, throwable) -> {
              if (throwable != null) {
                if (throwable instanceof HttpResponseStatusException statusException) {
                  routingContext.fail(statusException.status().statusCode());
                } else {
                  routingContext.fail(INTERNAL_SERVER_ERROR.code());
                }
              }
            });
  }

  @Override
  public void tryMainMethodExit() {
    closeables.forEach(
        autoCloseable -> {
          try {
            autoCloseable.close();
          } catch (Exception e) {
            log.error("Could not perform cleanup of closeable", e);
          }
        });
  }

  private Bindings getRequestScopeSeedBindings(
      RoutingContext routingContext, @Nullable String requestId) {
    BindingsBuilder seeds = Bindings.builder();
    seeds.bind(RoutingContext.class, routingContext);
    if (requestId != null) {
      seeds.bind(
          Header.class, Names.named(REQUEST_ID), new SingleValueHeader(REQUEST_ID, requestId));
    }
    return seeds.build();
  }
}
