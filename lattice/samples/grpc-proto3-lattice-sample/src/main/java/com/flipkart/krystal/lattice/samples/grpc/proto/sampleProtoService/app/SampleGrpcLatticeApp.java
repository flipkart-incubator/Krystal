package com.flipkart.krystal.lattice.samples.grpc.proto.sampleProtoService.app;

import static com.flipkart.krystal.lattice.core.execution.ThreadingStrategy.NATIVE_THREAD_PER_REQUEST;
import static com.flipkart.krystal.lattice.core.execution.ThreadingStrategyDopant.threadingStrategy;
import static com.flipkart.krystal.lattice.ext.grpc.GrpcServerDopant.grpc;
import static com.flipkart.krystal.lattice.vajram.VajramDopant.vajramGraph;

import com.flipkart.krystal.lattice.core.Application;
import com.flipkart.krystal.lattice.core.LatticeApp;
import com.flipkart.krystal.lattice.core.LatticeAppBootstrap;
import com.flipkart.krystal.lattice.ext.grpc.GrpcServer;
import com.flipkart.krystal.lattice.ext.grpc.GrpcService;
import com.flipkart.krystal.lattice.ext.guice.GuiceModuleBinder;
import com.flipkart.krystal.lattice.ext.guice.servlet.GuiceServletModuleBinder;
import com.flipkart.krystal.lattice.samples.grpc.proto.sampleProtoService.Proto3LatticeSample;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;

@GrpcServer(
    serverName = "LatticeSampleAppGrpc",
    services = {
      @GrpcService(
          serviceName = "SampleGrpcService",
          rpcVajrams = Proto3LatticeSample.class,
          doc = "A nice Service"),
      @GrpcService(
          serviceName = "AnotherSampleGrpcService",
          rpcVajrams = Proto3LatticeSample.class,
          doc = "A super service"),
    })
@LatticeApp(
    name = "LatticeSampleApplication",
    description = "A sample Lattice Application",
    dependencyInjectionBinder = GuiceServletModuleBinder.class)
public abstract class SampleGrpcLatticeApp extends Application {

  @Override
  public void bootstrap(LatticeAppBootstrap app) {
    app.dopeWith(threadingStrategy().threadingStrategy(NATIVE_THREAD_PER_REQUEST))
        .dopeWith(
            vajramGraph()
                .graphBuilder(
                    VajramKryonGraph.builder()
                        .loadFromPackage(
                            "com.flipkart.krystal.lattice.samples.proto3.sampleProtoService.app")
                        .loadClasses(Proto3LatticeSample.class)))
        .dopeWith(grpc());
  }

  @Override
  public GuiceModuleBinder getDependencyInjectionBinder() {
    return new GuiceModuleBinder(new CustomGuiceModule());
  }
}
