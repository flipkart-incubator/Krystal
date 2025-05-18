package com.flipkart.krystal.lattice.samples.proto3.sampleProtoService.app;

import com.flipkart.krystal.lattice.core.Application;
import com.flipkart.krystal.lattice.core.LatticeApp;
import com.flipkart.krystal.lattice.core.LatticeAppBootstrap;
import com.flipkart.krystal.lattice.core.vajram.VajramGraphSpecBuilder;
import com.flipkart.krystal.lattice.ext.grpc.GrpcServer;
import com.flipkart.krystal.lattice.ext.grpc.GrpcServerSpecBuilder;
import com.flipkart.krystal.lattice.ext.grpc.GrpcService;
import com.flipkart.krystal.lattice.samples.proto3.sampleProtoService.GuiceBinderModule;
import com.flipkart.krystal.lattice.samples.proto3.sampleProtoService.Proto3LatticeSample;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;

@GrpcServer(
    serverName = "LatticeSampleAppGrpc",
    services = {
      @GrpcService(
          serviceName = "SampleGrpcService",
          vajrams = Proto3LatticeSample.class,
          doc = "A nice Service"),
      @GrpcService(
          serviceName = "AnotherSampleGrpcService",
          vajrams = Proto3LatticeSample.class,
          doc = "A super service"),
    })
@LatticeApp
public class LatticeSampleApplication extends Application {

  @Override
  public GuiceBinderModule getDependencyInjectionBinder() {
    return new GuiceBinderModule();
  }

  @Override
  public void bootstrap(LatticeAppBootstrap bootstrap) {
    bootstrap
        .addDopant(
            VajramGraphSpecBuilder.create()
                .graphBuilder(
                    VajramKryonGraph.builder()
                        .loadFromPackage(
                            "com.flipkart.krystal.lattice.samples.proto3.sampleProtoService.app")
                        .loadClasses(Proto3LatticeSample.class)))
        .addDopant(GrpcServerSpecBuilder.create());
  }
}
