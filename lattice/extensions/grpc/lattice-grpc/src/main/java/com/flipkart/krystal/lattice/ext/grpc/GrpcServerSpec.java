package com.flipkart.krystal.lattice.ext.grpc;

import static com.flipkart.krystal.lattice.core.headers.StandardHeaderNames.REQUEST_ID;

import com.flipkart.krystal.lattice.core.doping.DopantSpec;
import com.flipkart.krystal.lattice.core.doping.DopantSpecBuilder;
import com.flipkart.krystal.lattice.core.headers.StandardHeaderNames;
import io.grpc.Context;
import lombok.Builder;

@Builder(buildMethodName = "_buildSpec")
public record GrpcServerSpec(Context.Key<String> requestIdContextKey)
    implements DopantSpec<GrpcServer, GrpcServerConfig, GrpcServerDopant> {

  private static final Context.Key<String> ACCEPT_HEADER_CONTEXT_KEY =
      Context.key(StandardHeaderNames.ACCEPT);

  public Context.Key<String> acceptHeaderContextKey() {
    return ACCEPT_HEADER_CONTEXT_KEY;
  }

  @Override
  public Class<GrpcServerDopant> dopantClass() {
    return GrpcServerDopant.class;
  }

  @Override
  public Class<GrpcServerConfig> _configurationType() {
    return GrpcServerConfig.class;
  }

  @Override
  public String _dopantType() {
    return GrpcServerDopant.DOPANT_TYPE;
  }

  public static final class GrpcServerSpecBuilder
      implements DopantSpecBuilder<GrpcServer, GrpcServerConfig, GrpcServerSpec> {

    GrpcServerSpecBuilder() {
      requestIdContextKey(Context.key(REQUEST_ID));
    }

    @Override
    public Class<GrpcServer> _annotationType() {
      return GrpcServer.class;
    }
  }
}
