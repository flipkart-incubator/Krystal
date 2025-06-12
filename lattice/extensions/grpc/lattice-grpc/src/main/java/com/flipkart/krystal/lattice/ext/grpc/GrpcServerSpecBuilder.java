package com.flipkart.krystal.lattice.ext.grpc;

import static com.flipkart.krystal.lattice.core.headers.StandardHeaderNames.REQUEST_ID;

import com.flipkart.krystal.lattice.core.doping.DopantSpecBuilder;
import io.grpc.Context;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.checkerframework.checker.nullness.qual.Nullable;

@Data
@EqualsAndHashCode(callSuper = false)
public class GrpcServerSpecBuilder
    implements DopantSpecBuilder<GrpcServer, GrpcServerConfig, GrpcServerSpec> {
  private final Context.Key<String> requestIdContextKey = Context.key(REQUEST_ID);

  GrpcServerSpecBuilder() {}

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
