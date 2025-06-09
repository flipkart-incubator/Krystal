package com.flipkart.krystal.lattice.ext.grpc;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/** A interceptor to handle server header. */
@Singleton
@Slf4j
public class StandardHeadersInterceptor implements ServerInterceptor {

  private final Context.Key<String> requestIdContextKey;
  private final Metadata.Key<String> requestIdMetadataKey;

  private final Context.Key<String> acceptHeaderContextKey;
  private final Metadata.Key<String> acceptMetadataKey;

  @Inject
  StandardHeadersInterceptor(GrpcServerSpec grpcServerSpec) {
    this.requestIdContextKey = grpcServerSpec.requestIdContextKey();
    this.requestIdMetadataKey =
        Metadata.Key.of(requestIdContextKey.toString(), Metadata.ASCII_STRING_MARSHALLER);

    this.acceptHeaderContextKey = grpcServerSpec.acceptHeaderContextKey();
    this.acceptMetadataKey =
        Metadata.Key.of(acceptHeaderContextKey.toString(), Metadata.ASCII_STRING_MARSHALLER);
  }

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call,
      final Metadata requestHeaders,
      ServerCallHandler<ReqT, RespT> next) {
    // Extract header value
    String requestId = requestHeaders.get(requestIdMetadataKey);
    log.info("requestId received from client: {}", requestId);

    String acceptHeader = requestHeaders.get(acceptMetadataKey);
    log.info("Accept header received from client: {}", acceptHeader);

    // Put into gRPC context
    Context contextWithHeader =
        Context.current()
            .withValue(requestIdContextKey, requestId)
            .withValue(acceptHeaderContextKey, acceptHeader);

    return Contexts.interceptCall(contextWithHeader, call, requestHeaders, next);
  }
}
