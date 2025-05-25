package com.flipkart.krystal.lattice.ext.protobuf;

import com.flipkart.krystal.lattice.core.headers.Header;
import com.flipkart.krystal.lattice.core.headers.SimpleHeader;
import com.flipkart.krystal.lattice.core.headers.StandardHeaderNames;
import lombok.experimental.UtilityClass;

@UtilityClass
public class LatticeProtoConstants {

  public static final String PROTOBUF_CONTENT_TYPE = "application/protobuf";

  public static final Header PROTOBUF_ACCEPT_HEADER =
      new SimpleHeader(StandardHeaderNames.ACCEPT, PROTOBUF_CONTENT_TYPE);
}
