package flipkart.krystal.lattice.ext.rest.quarkus.restServer;

import static io.netty.buffer.Unpooled.wrappedBuffer;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.lattice.core.di.DependencyInjectionBinder.BindingKey;
import com.flipkart.krystal.lattice.core.doping.Dopant;
import com.flipkart.krystal.lattice.core.doping.DopantInitData;
import com.flipkart.krystal.lattice.core.execution.ThreadingStrategyDopant;
import com.flipkart.krystal.lattice.vajram.VajramDopant;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.serial.SerializableModel;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import flipkart.krystal.lattice.ext.rest.quarkus.StatusCodes;
import flipkart.krystal.lattice.ext.rest.quarkus.app.QuarkusApplicationDopant;
import flipkart.krystal.lattice.ext.rest.quarkus.restServer.QuarkusRestServerSpec.QuarkusRestServerSpecBuilder;
import io.quarkus.vertx.utils.NoBoundChecksBuffer;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class QuarkusRestServerDopant
    implements Dopant<RestService, QuarkusRestServerConfig> {
  static final String REST_SERVER_DOPANT_TYPE = "krystal.lattice.restServer.quarkus";

  private final VajramDopant vajramDopant;
  private final QuarkusRestServerConfig config;
  private final ThreadingStrategyDopant threadingStrategyDopant;
  private final RestService restService;
  private final QuarkusApplicationDopant quarkusApplicationDopant;

  @Inject
  protected QuarkusRestServerDopant(QuarkusRestDopantInitData initData) {
    this.config = initData.config();
    this.restService = initData.annotation();
    this.threadingStrategyDopant = initData.threadingStrategyDopant();
    this.vajramDopant = initData.vajramDopant();
    this.quarkusApplicationDopant = initData.quarkusApplicationDopant();
  }

  public static QuarkusRestServerSpecBuilder quarkusRestServer() {
    return QuarkusRestServerSpec.builder();
  }

  @Override
  public void start() {
    Vertx vertx = quarkusApplicationDopant.vertx();
    Router router = Router.router(vertx);
    vertx
        .createHttpServer()
        .requestHandler(router)
        .listen(
            config.port(),
            result -> {
              if (result.succeeded()) {
                log.info("Server started on port {} ", config.port());
              } else {
                log.error("", result.cause());
              }
            });
    addRoutes(router);
  }

  protected abstract void addRoutes(Router router);

  protected String getPathPrefix() {
    String pathPrefix = restService.pathPrefix();
    return pathPrefix.isEmpty() ? "" : "/" + pathPrefix;
  }

  protected <RespT> void executeHttpRequest(
      RoutingContext routingContext,
      Function<byte[], ImmutableRequest<RespT>> requestDeserializer) {
    Lease<? extends ExecutorService> lease;
    try {
      lease = threadingStrategyDopant.getExecutorService();
    } catch (LeaseUnavailableException e) {
      log.error("Could not lease out single thread executor. Aborting request", e);
      routingContext.fail(StatusCodes.LEASE_UNAVAILABLE.code());
      return;
    }
    HttpServerRequest httpRequest = routingContext.request();
    HttpServerResponse httpResponse = routingContext.response();
    ExecutorService executorService = lease.get();
    if (!(executorService instanceof SingleThreadExecutor singleThreadExecutor)) {
      throw new UnsupportedOperationException(
          "Expected 'SingleThreadExecutor'. Found " + executorService.getClass());
    }
    Map<BindingKey, Object> seedMap = getRequestSeeds();
    CompletionStage<Buffer> body = httpRequest.body().toCompletionStage();
    body.whenComplete(
        (buffer, readError) -> {
          if (buffer == null || readError != null) {
            routingContext.fail(INTERNAL_SERVER_ERROR.code());
            return;
          }
          ImmutableRequest<RespT> request = requestDeserializer.apply(buffer.getBytes());
          executorService.submit(
              () -> {
                Closeable requestScope = threadingStrategyDopant.openRequestScope(seedMap);
                try (KrystexVajramExecutor executor =
                    vajramDopant.createExecutor(singleThreadExecutor)) {
                  executor
                      .execute(request)
                      .whenComplete(
                          (response, throwable) -> {
                            if (throwable != null || response == null) {
                              routingContext.fail(INTERNAL_SERVER_ERROR.code());
                              return;
                            }

                            try {
                              if (response instanceof SerializableModel serializableResponse) {
                                httpResponse
                                    .putHeader("Content-type", "application/json")
                                    .end(
                                        new NoBoundChecksBuffer(
                                            wrappedBuffer(serializableResponse._serialize())));
                              } else {
                                routingContext.fail(INTERNAL_SERVER_ERROR.code());
                              }
                            } catch (Throwable e) {
                              routingContext.fail(INTERNAL_SERVER_ERROR.code());
                            } finally {
                              try {
                                requestScope.close();
                              } catch (IOException e) {
                                log.error("Unable to close request scope");
                              }
                            }
                          });
                }
              });
        });
  }

  private Map<BindingKey, Object> getRequestSeeds() {
    return Map.of();
  }

  protected record QuarkusRestDopantInitData(
      RestService annotation,
      QuarkusRestServerConfig config,
      QuarkusRestServerSpec spec,
      VajramDopant vajramDopant,
      ThreadingStrategyDopant threadingStrategyDopant,
      QuarkusApplicationDopant quarkusApplicationDopant)
      implements DopantInitData<RestService, QuarkusRestServerConfig, QuarkusRestServerSpec> {

    @Inject
    public QuarkusRestDopantInitData {}
  }
}
