package com.flipkart.krystal.codegen.common.spi;

import com.flipkart.krystal.model.ModelProtocol;
import com.squareup.javapoet.CodeBlock;

public interface ModelProtocolConfigProvider {
  ModelProtocolConfig getConfig();

  record ModelProtocolConfig(
      Class<? extends ModelProtocol> modelProtocolType,
      String modelClassesSuffix,
      CodeBlock httpContentType) {}
}
