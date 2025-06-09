package com.flipkart.krystal.lattice.ext.grpc;

import static com.flipkart.krystal.lattice.ext.grpc.GrpcServerDopant.DOPANT_TYPE;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.lattice.core.di.DependencyInjectionBinder;
import com.flipkart.krystal.lattice.core.di.DependencyInjectionBinder.BindingKey;
import com.flipkart.krystal.lattice.core.di.DependencyInjectionBinder.BindingKey.AnnotationType;
import com.flipkart.krystal.lattice.core.doping.Dopant;
import com.flipkart.krystal.lattice.core.doping.DopantType;
import com.flipkart.krystal.lattice.core.execution.ThreadingStrategyDopant;
import com.flipkart.krystal.lattice.core.headers.Header;
import com.flipkart.krystal.lattice.core.headers.SimpleHeader;
import com.flipkart.krystal.lattice.vajram.VajramDopant;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import io.grpc.BindableService;
import io.grpc.Context;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import jakarta.inject.Inject;
import java.io.Closeable;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@DopantType(DOPANT_TYPE)
@Slf4j
public abstract class GrpcServerDopant implements Dopant<GrpcServer, GrpcServerConfig> {

  public static final String DOPANT_TYPE = "krystal.lattice.grpcServer";
  private final ThreadingStrategyDopant threadingStrategyDopant;
  private final GrpcServerSpec grpcServerSpec;

  public static GrpcServerSpecBuilder grpc() {
    return new GrpcServerSpecBuilder();
  }

  private final GrpcServerConfig config;
  private final GrpcServer annotation;
  private final VajramDopant vajramDopant;
  private final StandardHeadersInterceptor headerInterceptor;

  private @MonotonicNonNull Server server;

  @Inject
  protected GrpcServerDopant(
      GrpcInitData initData,
      StandardHeadersInterceptor headerInterceptor,
      VajramDopant vajramDopant,
      ThreadingStrategyDopant threadingStrategyDopant) {
    this.grpcServerSpec = initData.spec();
    this.annotation = initData.annotation();
    this.config = initData.config();
    this.vajramDopant = vajramDopant;
    this.headerInterceptor = headerInterceptor;
    this.threadingStrategyDopant = threadingStrategyDopant;
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
    Lease<? extends ExecutorService> lease;
    try {
      lease = threadingStrategyDopant.getExecutorService();
    } catch (LeaseUnavailableException e) {
      log.error("Could not lease out single thread executor. Aborting request", e);
      responseObserver.onError(getUnknownInternalError());
      return;
    }
    ExecutorService executorService = lease.get();
    if (!(executorService instanceof SingleThreadExecutor singleThreadExecutor)) {
      throw new UnsupportedOperationException(
          "Expected 'SingleThreadExecutor'. Found " + executorService.getClass());
    }
    Map<BindingKey, Object> seedMap = getRequestSeeds();
    executorService.execute(
        () -> {
          Closeable requestScope = threadingStrategyDopant.openRequestScope(seedMap);
          try (KrystexVajramExecutor executor = vajramDopant.createExecutor(singleThreadExecutor)) {
            executor
                .execute(request)
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
                        try {
                          requestScope.close();
                        } catch (IOException e) {
                          log.error("Unable to close request scope");
                        }
                      }
                    });
          }
        });
  }

  private Map<BindingKey, Object> getRequestSeeds() {
    Map<BindingKey, Object> seedMap = new LinkedHashMap<>();
    addHeader(grpcServerSpec.requestIdContextKey(), seedMap);
    addHeader(grpcServerSpec.acceptHeaderContextKey(), seedMap);
    return seedMap;
  }

  private void addHeader(Context.Key<String> key, Map<BindingKey, Object> map) {
    String headerValue = key.get();
    if (headerValue != null) {
      String headerKey = key.toString();
      map.put(
          new AnnotationType(Header.class, DependencyInjectionBinder.named(headerKey)),
          new SimpleHeader(headerKey, headerValue));
    }
  }

  private static @NonNull IllegalArgumentException getUnknownInternalError() {
    return new IllegalArgumentException("Unknown internal error");
  }

  @Override
  public void tryMainMethodExit() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }
}
