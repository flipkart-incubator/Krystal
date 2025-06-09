package com.flipkart.krystal.lattice.ext.grpc;

import com.flipkart.krystal.lattice.core.doping.DopantSpec;
import com.flipkart.krystal.lattice.core.headers.StandardHeaderNames;
import io.grpc.Context;

record GrpcServerSpec(Context.Key<String> requestIdContextKey)
    implements DopantSpec<GrpcServer, GrpcServerConfig, GrpcServerDopant> {
  private static final Context.Key<String> ACCEPT_HEADER_CONTEXT_KEY =
      Context.key(StandardHeaderNames.ACCEPT);

  public Context.Key<String> acceptHeaderContextKey() {
    return ACCEPT_HEADER_CONTEXT_KEY;
  }

  @Override
  public Class<GrpcServerDopant> dopantClass() {
    return GrpcServerDopant.class;
  }
}
