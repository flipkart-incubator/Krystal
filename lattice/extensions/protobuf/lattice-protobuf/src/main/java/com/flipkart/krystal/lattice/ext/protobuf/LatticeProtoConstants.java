package com.flipkart.krystal.lattice.ext.protobuf;

import static com.flipkart.krystal.lattice.core.headers.StandardHeaderNames.ACCEPT;
import static com.flipkart.krystal.vajram.protobuf3.Protobuf3.PROTOBUF_3;

import com.flipkart.krystal.lattice.core.headers.Header;
import com.flipkart.krystal.lattice.core.headers.SimpleHeader;
import lombok.experimental.UtilityClass;

@UtilityClass
public class LatticeProtoConstants {

  public static final Header PROTOBUF_ACCEPT_HEADER =
      new SimpleHeader(ACCEPT, PROTOBUF_3.contentType());
}
