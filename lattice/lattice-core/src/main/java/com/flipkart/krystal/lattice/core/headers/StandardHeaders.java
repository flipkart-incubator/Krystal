package com.flipkart.krystal.lattice.core.headers;

import com.flipkart.krystal.serial.SerdeProtocol;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;

@UtilityClass
public class StandardHeaders {

  @UtilityClass
  public final class AcceptHeaders {
    public Header PROTOBUF = new SimpleHeader(StandardHeaderKeys.ACCEPT, "application/protobuf");
  }
}
