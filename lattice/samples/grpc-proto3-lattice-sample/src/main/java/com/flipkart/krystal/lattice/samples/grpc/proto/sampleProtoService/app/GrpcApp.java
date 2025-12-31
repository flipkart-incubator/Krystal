package com.flipkart.krystal.lattice.samples.grpc.proto.sampleProtoService.app;

import static com.flipkart.krystal.lattice.core.execution.ThreadingStrategy.POOLED_NATIVE_THREAD_PER_REQUEST;
import static com.flipkart.krystal.lattice.ext.grpc.GrpcServerDopant.grpcServer;

import com.flipkart.krystal.lattice.core.LatticeApp;
import com.flipkart.krystal.lattice.core.LatticeApplication;
import com.flipkart.krystal.lattice.core.doping.DopeWith;
import com.flipkart.krystal.lattice.core.execution.ThreadingStrategySpec;
import com.flipkart.krystal.lattice.core.execution.ThreadingStrategySpec.ThreadingStrategySpecBuilder;
import com.flipkart.krystal.lattice.ext.grpc.GrpcServer;
import com.flipkart.krystal.lattice.ext.grpc.GrpcServerSpec.GrpcServerSpecBuilder;
import com.flipkart.krystal.lattice.ext.grpc.GrpcService;
import com.flipkart.krystal.lattice.ext.guice.GuiceFramework;
import com.flipkart.krystal.lattice.ext.guice.servlet.GuiceServletFramework;
import com.flipkart.krystal.lattice.krystex.KrystexDopantSpec;
import com.flipkart.krystal.lattice.krystex.KrystexDopantSpec.KrystexDopantSpecBuilder;
import com.flipkart.krystal.lattice.samples.grpc.proto.sampleProtoService.Proto3LatticeSample;
import com.flipkart.krystal.lattice.vajram.VajramDopantSpec;
import com.flipkart.krystal.lattice.vajram.VajramDopantSpec.VajramDopantSpecBuilder;
import com.flipkart.krystal.vajramexecutor.krystex.VajramGraph;

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
    description = "A sample Lattice Application",
    dependencyInjectionFramework = GuiceServletFramework.class)
public abstract class GrpcApp extends LatticeApplication {

  @DopeWith
  public static ThreadingStrategySpecBuilder threadingStrategySpec() {
    return ThreadingStrategySpec.builder().threadingStrategy(POOLED_NATIVE_THREAD_PER_REQUEST);
  }

  @DopeWith
  public static VajramDopantSpecBuilder vajramDopantSpecBuilder() {
    return VajramDopantSpec.builder()
        .vajramGraphBuilder(
            VajramGraph.builder()
                .loadFromPackage(
                    "com.flipkart.krystal.lattice.samples.proto3.sampleProtoService.app")
                .loadClasses(Proto3LatticeSample.class));
  }

  @DopeWith
  static KrystexDopantSpecBuilder krystex() {
    return KrystexDopantSpec.builder();
  }

  @DopeWith
  public static GrpcServerSpecBuilder grpcServerDopantSpecBuilder() {
    return grpcServer();
  }

  @Override
  public GuiceFramework getDependencyInjector() {
    return new GuiceFramework(this, new CustomGuiceModule());
  }
}
