package com.flipkart.krystal.lattice.ext.grpc;

import com.flipkart.krystal.vajram.VajramDef;

public @interface GrpcService {
  String serviceName();

  String doc();

  Class<? extends VajramDef<?>>[] rpcVajrams();
}
