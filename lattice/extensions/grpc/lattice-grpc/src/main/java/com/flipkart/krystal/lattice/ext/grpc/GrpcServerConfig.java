package com.flipkart.krystal.lattice.ext.grpc;

import com.flipkart.krystal.lattice.core.DopantConfig;
import com.flipkart.krystal.lattice.core.annos.DopantType;

@DopantType(GrpcServerConfig.DOPANT_TYPE)
public record GrpcServerConfig(int port, int maxApplicationThreads) implements DopantConfig {
  public static final String DOPANT_TYPE = "krystal.lattice.grpcServer";

  @Override
  public String _dopantType() {
    return DOPANT_TYPE;
  }
}
