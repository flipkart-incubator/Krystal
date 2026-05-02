import com.flipkart.krystal.codegen.common.spi.ModelsCodeGeneratorProvider;
import com.flipkart.krystal.vajram.protobuf2024e.codegen.Proto2024eModelsGenProvider;
import com.flipkart.krystal.vajram.protobuf2024e.codegen.Proto2024eSchemaGenProvider;

module flipkart.krystal.vajram.ext.protobuf2024e.codegen {
  exports com.flipkart.krystal.vajram.protobuf2024e.codegen;

  requires flipkart.krystal.codegen.common;
  requires transitive flipkart.krystal.vajram.ext.protobuf2024e;
  requires transitive flipkart.krystal.vajram.ext.protobuf.codegen.util;
  requires com.google.auto.service;
  requires org.slf4j;
  requires com.google.common;
  requires org.checkerframework.checker.qual;
  requires com.squareup.javapoet;
  requires static lombok;
  requires com.google.protobuf;
  requires flipkart.krystal.vajram.codegen.common;

  provides ModelsCodeGeneratorProvider with
      Proto2024eModelsGenProvider,
      Proto2024eSchemaGenProvider;
}
