package flipkart.krystal.lattice.ext.rest.quarkus.restServer;

import static com.flipkart.krystal.lattice.core.headers.StandardHeaderNames.REQUEST_ID;
import static io.netty.buffer.Unpooled.wrappedBuffer;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;

import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig.KryonExecutorConfigBuilder;
import com.flipkart.krystal.lattice.core.di.Bindings;
import com.flipkart.krystal.lattice.core.di.Bindings.BindingsBuilder;
import com.flipkart.krystal.lattice.core.doping.Dopant;
import com.flipkart.krystal.lattice.core.headers.Header;
import com.flipkart.krystal.lattice.core.headers.SingleValueHeader;
import com.flipkart.krystal.lattice.rest.RestService;
import com.flipkart.krystal.lattice.rest.RestServiceDopant;
import com.flipkart.krystal.lattice.rest.api.status.HttpResponseStatusException;
import com.flipkart.krystal.serial.SerializableModel;
import com.flipkart.krystal.tags.Names;
import flipkart.krystal.lattice.ext.rest.quarkus.app.QuarkusApplicationDopant;
import flipkart.krystal.lattice.ext.rest.quarkus.restServer.QuarkusRestServerSpec.QuarkusRestServerSpecBuilder;
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
public final class QuarkusRestServerDopant implements Dopant<RestService, QuarkusRestServerConfig> {
  static final String REST_SERVER_DOPANT_TYPE = "krystal.lattice.restServer.quarkus";

  private final QuarkusRestServerConfig config;
  private final RestService restService;
  private final QuarkusApplicationDopant quarkusApplicationDopant;
  private final RestServiceDopant restServiceDopant;
  private final List<AutoCloseable> closeables = new ArrayList<>();

  @Inject
  QuarkusRestServerDopant(
      RestService restService,
      QuarkusRestServerConfig config,
      RestServiceDopant restServiceDopant,
      QuarkusApplicationDopant quarkusApplicationDopant) {
    this.config = config;
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

    HttpServerOptions httpServerOptions = new HttpServerOptions();
    httpServerOptions.setPort(config.port());
    vertx
        .createHttpServer(httpServerOptions)
        .requestHandler(jaxRsRequestHandler(vertx))
        .listen(
            result -> {
              if (result.succeeded()) {
                log.info("Server started on port {} ", config.port());
              } else {
                log.error("", result.cause());
              }
            });
  }

  private Handler<HttpServerRequest> jaxRsRequestHandler(Vertx vertx) {
    VertxResteasyDeployment deployment = new VertxResteasyDeployment();
    deployment.start();

    closeables.add(deployment::stop);
    VertxRegistry registry = deployment.getRegistry();
    restServiceDopant.getResources().forEach(registry::addSingletonResource);
    return new VertxRequestHandler(vertx, deployment, restService.pathPrefix());
  }

  protected <RespT> void executeHttpRequest(
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
                return;
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
