import com.flipkart.krystal.codegen.common.spi.ModelsCodeGeneratorProvider;
import com.flipkart.krystal.vajram.protobuf3.codegen.ModelsProto3GenProvider;
import com.flipkart.krystal.vajram.protobuf3.codegen.ModelsProto3SchemaGenProvider;

module flipkart.krystal.vajram.ext.protobuf.codegen {
  exports com.flipkart.krystal.vajram.protobuf3.codegen;

  requires flipkart.krystal.codegen.common;
  requires flipkart.krystal.vajram.ext.protobuf;
  requires com.google.auto.service;
  requires org.slf4j;
  requires com.google.common;
  requires org.checkerframework.checker.qual;
  requires com.squareup.javapoet;
  requires static lombok;
  requires com.google.protobuf;
  requires flipkart.krystal.lattice.core;
  requires flipkart.krystal.vajram.codegen.common;
  requires flipkart.krystal.lattice.vajram.sdk;

  provides ModelsCodeGeneratorProvider with
      ModelsProto3GenProvider,
      ModelsProto3SchemaGenProvider;
}
