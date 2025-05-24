package com.flipkart.krystal.lattice.ext.grpc;

import com.flipkart.krystal.lattice.core.doping.Dopant;
import com.flipkart.krystal.lattice.core.doping.DopantSpec;
import com.flipkart.krystal.lattice.core.headers.StandardHeaderKeys;
import io.grpc.Context;

record GrpcServerSpec(Context.Key<String> requestIdContextKey)
    implements DopantSpec<GrpcServer, GrpcServerConfig, GrpcServerSpec> {
  private static final Context.Key<String> ACCEPT_HEADER_CONTEXT_KEY =
      Context.key(StandardHeaderKeys.ACCEPT);

  public Context.Key<String> acceptHeaderContextKey() {
    return ACCEPT_HEADER_CONTEXT_KEY;
  }

  @Override
  public Class<GrpcServerDopant> dopantClass() {
    return GrpcServerDopant.class;
  }
}
