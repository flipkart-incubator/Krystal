package com.flipkart.krystal.lattice.ext.grpc;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.flipkart.krystal.lattice.core.annos.DopantType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@DopantType(GrpcServerConfig.DOPANT_TYPE)
@Inherited
@Target(TYPE)
@Retention(RUNTIME)
public @interface GrpcServer {
  String serverName();

  GrpcService[] services();
}
