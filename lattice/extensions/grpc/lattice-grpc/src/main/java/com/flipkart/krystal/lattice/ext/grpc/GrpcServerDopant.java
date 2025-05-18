package com.flipkart.krystal.lattice.ext.grpc;

import static com.flipkart.krystal.lattice.ext.grpc.GrpcServerConfig.DOPANT_TYPE;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.ThreadPerRequestExecutorsPool;
import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.lattice.core.Dopant;
import com.flipkart.krystal.lattice.core.annos.DopantType;
import com.flipkart.krystal.lattice.core.vajram.VajramGraphDopant;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import io.grpc.BindableService;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@DopantType(DOPANT_TYPE)
@Slf4j
public abstract class GrpcServerDopant
    implements Dopant<GrpcServer, GrpcServerConfig, GrpcServerSpec> {

  private final GrpcServerConfig config;
  private final GrpcServer annotation;
  private final VajramGraphDopant vajramGraphDopant;
  private final HeaderServerInterceptor headerInterceptor;
  private final ThreadPerRequestExecutorsPool executorPool;

  private @MonotonicNonNull Server server;

  protected GrpcServerDopant(
      GrpcServerSpec spec,
      GrpcServerConfig config,
      GrpcServer annotation,
      VajramGraphDopant vajramGraphDopant,
      HeaderServerInterceptor headerInterceptor) {
    this.config = config;
    this.annotation = annotation;
    this.vajramGraphDopant = vajramGraphDopant;
    this.headerInterceptor = headerInterceptor;
    this.executorPool =
        new ThreadPerRequestExecutorsPool(
            annotation.serverName() + "-ThreadPerRequestExecutorsPool",
            config.maxApplicationThreads());
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
  public <ProtoT extends @Nullable Message, RespT> void executeRpc(
      StreamObserver<@Nullable ProtoT> responseObserver,
      ImmutableRequest<RespT> request,
      Function<@Nullable RespT, ProtoT> protoConverter) {
    Lease<@NonNull SingleThreadExecutor> lease;
    try {
      lease = executorPool.lease();
    } catch (LeaseUnavailableException e) {
      log.error("Could not lease out single thread executor. Aborting request", e);
      responseObserver.onError(getUnknownInternalError());
      return;
    }
    try (KrystexVajramExecutor executor = vajramGraphDopant.createExecutor(lease.get())) {
      executor
          .execute(request)
          .whenComplete(
              (response, throwable) -> {
                if (throwable != null) {
                  responseObserver.onError(throwable);
                } else {
                  ProtoT proto = protoConverter.apply(response);
                  if (proto == null) {
                    responseObserver.onError(getUnknownInternalError());
                  } else {
                    responseObserver.onNext(proto);
                  }
                }
                responseObserver.onCompleted();
                lease.close();
              });
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
    executorPool.close();
  }
}
