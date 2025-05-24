package com.flipkart.krystal.lattice.ext.grpc;

import com.flipkart.krystal.lattice.core.doping.DopantInitData;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public record GrpcInitData(GrpcServer annotation, GrpcServerConfig config, GrpcServerSpec spec)
    implements DopantInitData<GrpcServer, GrpcServerConfig, GrpcServerSpec> {
  @Inject
  public GrpcInitData {}
}
