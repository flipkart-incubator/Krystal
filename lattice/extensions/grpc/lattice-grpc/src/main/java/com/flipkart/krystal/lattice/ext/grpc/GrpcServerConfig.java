package com.flipkart.krystal.lattice.ext.grpc;

import static com.flipkart.krystal.lattice.ext.grpc.GrpcServerDopant.DOPANT_TYPE;

import com.flipkart.krystal.lattice.core.doping.DopantConfig;
import com.flipkart.krystal.lattice.core.doping.DopantType;

@DopantType(DOPANT_TYPE)
public record GrpcServerConfig(int port) implements DopantConfig {

  @Override
  public String _dopantType() {
    return DOPANT_TYPE;
  }
}
