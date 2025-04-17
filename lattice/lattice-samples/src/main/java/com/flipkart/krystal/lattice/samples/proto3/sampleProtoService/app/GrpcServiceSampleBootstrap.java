package com.flipkart.krystal.lattice.samples.proto3.sampleProtoService.app;

import com.flipkart.krystal.lattice.core.LatticeAppBootstrap;
import com.flipkart.krystal.lattice.core.LatticeAppBuilder;
import java.util.List;

public final class GrpcServiceSampleBootstrap implements LatticeAppBootstrap {

  @Override
  public void bootstrap(LatticeAppBuilder appBuilder) {
//    appBuilder.add(new GuiceDopant(new GrpcSampleGuiceModule()));
  }
}
