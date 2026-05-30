package com.flipkart.krystal.codegen.common.spi;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.model.ModelProtocol;
import com.squareup.javapoet.CodeBlock;
import javax.lang.model.type.TypeMirror;

public interface ModelProtocolConfigProvider {
  ModelProtocolConfig getConfig();

  interface ModelProtocolConfig {
    ModelProtocol modelProtocol();

    default CodeBlock createDeserializationExpression(
        CodeBlock valueExpression, TypeMirror type, CodeGenUtility util) {
      throw new UnsupportedOperationException(
          modelProtocol() + " does not deserialization expression for " + type);
    }

    default CodeBlock createSerializationExpression(
        CodeBlock valueExpression, TypeMirror type, CodeGenUtility util) {
      throw new UnsupportedOperationException(
          modelProtocol() + " does not deserialization expression");
    }
  }
}
