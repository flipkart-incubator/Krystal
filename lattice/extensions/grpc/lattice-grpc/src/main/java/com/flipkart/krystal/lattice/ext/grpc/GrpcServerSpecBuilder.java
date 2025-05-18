package com.flipkart.krystal.lattice.ext.grpc;

import com.flipkart.krystal.lattice.core.DopantSpecBuilder;
import com.flipkart.krystal.lattice.core.SpecBuilders;
import com.flipkart.krystal.lattice.core.vajram.VajramGraphSpecBuilder;
import io.grpc.Context;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.checkerframework.checker.nullness.qual.Nullable;

@Data
@EqualsAndHashCode(callSuper = false)
public class GrpcServerSpecBuilder
    extends DopantSpecBuilder<GrpcServer, GrpcServerConfig, GrpcServerSpec> {
  public Context.Key<String> requestIdKey = Context.key("X-Request-Id");

  public static GrpcServerSpecBuilder create() {
    return new GrpcServerSpecBuilder();
  }

  @Override
  protected void configure(SpecBuilders allSpecBuilders) {
    VajramGraphSpecBuilder vajramGraphSpecBuilder =
        allSpecBuilders.getSpecBuilder(VajramGraphSpecBuilder.class);
    vajramGraphSpecBuilder.configureKryonExecutor(
        configBuilder -> configBuilder.executorId(requestIdKey().get()));
  }

  @SuppressWarnings("ClassEscapesDefinedScope")
  @Override
  public GrpcServerSpec build(
      @Nullable GrpcServer annotation, @Nullable GrpcServerConfig configuration) {
    return new GrpcServerSpec(requestIdKey);
  }

  @Override
  public Class<GrpcServer> getAnnotationType() {
    return GrpcServer.class;
  }

  @Override
  public Class<GrpcServerConfig> getConfigurationType() {
    return GrpcServerConfig.class;
  }

  @Override
  public String dopantType() {
    return GrpcServerConfig.DOPANT_TYPE;
  }

  private GrpcServerSpecBuilder() {}
}
