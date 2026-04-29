package com.flipkart.krystal.codegen.common.spi;

import com.flipkart.krystal.model.ModelProtocol;

public interface ModelProtocolConfigProvider {
  ModelProtocolConfig getConfig();

  record ModelProtocolConfig(ModelProtocol modelProtocol) {}
}
