package com.flipkart.krystal.lattice.ext.grpc;

import com.flipkart.krystal.lattice.core.Dopant;
import com.flipkart.krystal.lattice.core.DopantSpec;
import io.grpc.Context;

public record GrpcServerSpec(Context.Key<String> requestIdKey)
    implements DopantSpec<GrpcServer, GrpcServerConfig, GrpcServerSpec> {
  @Override
  public Class<? extends Dopant<?, ?, ?>> dopantClass() {
    return GrpcServerDopant.class;
  }
}
