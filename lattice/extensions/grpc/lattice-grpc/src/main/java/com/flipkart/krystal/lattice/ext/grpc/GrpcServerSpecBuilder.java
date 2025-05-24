package com.flipkart.krystal.lattice.ext.grpc;

import com.flipkart.krystal.lattice.core.doping.DopantSpecBuilder;
import com.flipkart.krystal.lattice.core.doping.SpecBuilders;
import com.flipkart.krystal.lattice.vajram.VajramDopantSpecBuilder;
import io.grpc.Context;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.checkerframework.checker.nullness.qual.Nullable;

@Data
@EqualsAndHashCode(callSuper = false)
public class GrpcServerSpecBuilder
    implements DopantSpecBuilder<GrpcServer, GrpcServerConfig, GrpcServerSpec> {
  public Context.Key<String> requestIdContextKey = Context.key("X-Request-Id");

  GrpcServerSpecBuilder() {}

  @Override
  public void _configure(SpecBuilders allSpecBuilders) {
    VajramDopantSpecBuilder vajramDopantSpecBuilder =
        allSpecBuilders.getSpecBuilder(VajramDopantSpecBuilder.class);
    vajramDopantSpecBuilder.configureKryonExecutor(
        configBuilder -> configBuilder.executorId(requestIdContextKey().get()));
  }

  @SuppressWarnings("ClassEscapesDefinedScope")
  @Override
  public GrpcServerSpec _buildSpec(
      @Nullable GrpcServer annotation, @Nullable GrpcServerConfig configuration) {
    return new GrpcServerSpec(requestIdContextKey);
  }

  @Override
  public Class<GrpcServer> _annotationType() {
    return GrpcServer.class;
  }

  @SuppressWarnings("ClassEscapesDefinedScope")
  @Override
  public Class<GrpcServerConfig> _configurationType() {
    return GrpcServerConfig.class;
  }

  @Override
  public String _dopantType() {
    return GrpcServerDopant.DOPANT_TYPE;
  }
}
