package com.flipkart.krystal.lattice.ext.grpc;

import static com.flipkart.krystal.lattice.ext.grpc.GrpcServerDopant.DOPANT_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.flipkart.krystal.lattice.core.doping.DopantType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@DopantType(DOPANT_TYPE)
@Inherited
@Target(TYPE)
@Retention(RUNTIME)
public @interface GrpcServer {
  String serverName();

  GrpcService[] services();
}
