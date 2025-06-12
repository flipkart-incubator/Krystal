package com.flipkart.krystal.codegen.common.spi;

import com.flipkart.krystal.serial.SerdeProtocol;

public interface ModelProtocolConfigProvider {
  ModelProtocolConfig getConfig();

  record ModelProtocolConfig(SerdeProtocol serdeProtocol) {}
}
