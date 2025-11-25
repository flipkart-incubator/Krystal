package com.flipkart.krystal.lattice.ext.grpc;

import static com.flipkart.krystal.lattice.ext.grpc.GrpcServerDopant.DOPANT_TYPE;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig.KryonExecutorConfigBuilder;
import com.flipkart.krystal.lattice.core.di.Bindings;
import com.flipkart.krystal.lattice.core.di.Bindings.BindingsBuilder;
import com.flipkart.krystal.lattice.core.doping.Dopant;
import com.flipkart.krystal.lattice.core.doping.DopantType;
import com.flipkart.krystal.lattice.core.headers.Header;
import com.flipkart.krystal.lattice.core.headers.SingleValueHeader;
import com.flipkart.krystal.lattice.vajram.VajramDopant;
import com.flipkart.krystal.lattice.vajram.VajramRequestExecutionContext;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.tags.Names;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import io.grpc.BindableService;
import io.grpc.Context.Key;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@DopantType(DOPANT_TYPE)
@Slf4j
public abstract class GrpcServerDopant implements Dopant<GrpcServer, GrpcServerConfig> {

  public static final String DOPANT_TYPE = "krystal.lattice.grpcServer";
  private final GrpcServerSpec grpcServerSpec;

  public static GrpcServerSpec.GrpcServerSpecBuilder grpcServer() {
    return new GrpcServerSpec.GrpcServerSpecBuilder();
  }

  private final GrpcServerConfig config;
  private final GrpcServer annotation;
  private final VajramDopant vajramDopant;
  private final StandardHeadersInterceptor headerInterceptor;

  private @MonotonicNonNull Server server;

  @Inject
  protected GrpcServerDopant(GrpcInitData initData) {
    this.grpcServerSpec = initData.spec();
    this.annotation = initData.annotation();
    this.config = initData.config();
    this.vajramDopant = initData.vajramDopant();
    this.headerInterceptor = initData.headerInterceptor();
  }

  @Override
  public void start() throws IOException {
    log.info("****** GrpcServerDopant : Starting GrpcServer {} ****** ", annotation.serverName());
    ExecutorService executor = Executors.newFixedThreadPool(8);
    ServerBuilder<?> serverBuilder =
        Grpc.newServerBuilderForPort(config.port(), InsecureServerCredentials.create())
            .executor(executor)
            .intercept(headerInterceptor);
    serviceDefinitions().forEach(serverBuilder::addService);
    var server = serverBuilder.build().start();
    log.info("************************************************");
    log.info("****** GrpcServerDopant : Started GrpcServer {}", annotation.serverName());
    log.info("************************************************");

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                  log.error(
                      "*** shutting down gRPC server '{}' since JVM is shutting down",
                      annotation.serverName());
                  try {
                    server.shutdown().awaitTermination(30, SECONDS);
                  } catch (InterruptedException e) {
                    server.shutdownNow();
                    log.error("", e);
                  } finally {
                    executor.shutdown();
                  }
                  log.error("*** server shut down ***");
                }));
    this.server = server;
  }

  protected abstract ImmutableList<BindableService> serviceDefinitions();

  @SuppressWarnings("unchecked")
  public <RespT, RespProtoT extends @Nullable Message> void executeRpc(
      ImmutableRequest<RespT> request,
      StreamObserver<@Nullable RespProtoT> responseObserver,
      Function<@Nullable RespT, RespProtoT> protoConverter) {
    Bindings seedMap = getRequestSeeds();
    KryonExecutorConfigBuilder configBuilder = KryonExecutorConfig.builder();
    String requestId = grpcServerSpec.requestIdContextKey().get();
    if (requestId != null) {
      configBuilder.executorId(requestId);
    }
    try {
      vajramDopant
          .executeRequest(
              VajramRequestExecutionContext.<RespT>builder()
                  .vajramRequest(request)
                  .requestScopeSeeds(seedMap)
                  .executorConfigBuilder(configBuilder)
                  .build())
          .whenComplete(
              (response, throwable) -> {
                try {
                  if (throwable != null) {
                    responseObserver.onError(throwable);
                  } else {
                    RespProtoT proto = protoConverter.apply(response);
                    if (proto == null) {
                      responseObserver.onError(getUnknownInternalError());
                    } else {
                      responseObserver.onNext(proto);
                    }
                  }
                } catch (Throwable e) {
                  responseObserver.onError(e);
                } finally {
                  responseObserver.onCompleted();
                }
              });
    } catch (LeaseUnavailableException e) {
      log.error("Could not lease out single thread executor. Aborting request", e);
      responseObserver.onError(new StatusException(Status.RESOURCE_EXHAUSTED));
      return;
    }
  }

  private Bindings getRequestSeeds() {
    BindingsBuilder bindings = Bindings.builder();
    addHeader(grpcServerSpec.acceptHeaderContextKey(), bindings);
    return bindings.build();
  }

  private void addHeader(Key<String> key, BindingsBuilder bindings) {
    String headerValue = key.get();
    if (headerValue != null) {
      String headerKey = key.toString();
      bindings.bind(
          Header.class, Names.named(headerKey), new SingleValueHeader(headerKey, headerValue));
    }
  }

  private static IllegalArgumentException getUnknownInternalError() {
    return new IllegalArgumentException("Unknown internal error");
  }

  @Override
  public void tryMainMethodExit() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  @Singleton
  protected record GrpcInitData(
      GrpcServer annotation,
      GrpcServerConfig config,
      GrpcServerSpec spec,
      StandardHeadersInterceptor headerInterceptor,
      VajramDopant vajramDopant) {
    @Inject
    public GrpcInitData {}
  }
}
