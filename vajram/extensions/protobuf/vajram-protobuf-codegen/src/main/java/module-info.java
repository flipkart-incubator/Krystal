import com.flipkart.krystal.vajram.codegen.common.spi.AllVajramsCodeGeneratorProvider;
import com.flipkart.krystal.vajram.codegen.common.spi.ModelsCodeGeneratorProvider;
import com.flipkart.krystal.vajram.codegen.common.spi.VajramCodeGeneratorProvider;
import com.flipkart.krystal.vajram.protobuf3.codegen.ModelsProto3GenProvider;
import com.flipkart.krystal.vajram.protobuf3.codegen.ModelsProto3SchemaGenProvider;
import com.flipkart.krystal.vajram.protobuf3.codegen.VajramModelsProto3SchemaGenProvider;
import com.flipkart.krystal.vajram.protobuf3.codegen.VajramProto3ServiceSchemaGenProvider;

module flipkart.krystal.lattice.protobuf {
  requires flipkart.krystal.vajram.codegen.common;
  requires flipkart.krystal.vajram.extensions.protobuf;
  requires com.google.auto.service;
  requires org.slf4j;
  requires com.google.common;
  requires org.checkerframework.checker.qual;
  requires com.squareup.javapoet;
  requires static lombok;
  requires com.google.protobuf;
  requires flipkart.krystal.lattice.core;

  provides VajramCodeGeneratorProvider with
      VajramModelsProto3SchemaGenProvider;
  provides ModelsCodeGeneratorProvider with
      ModelsProto3GenProvider,
      ModelsProto3SchemaGenProvider;
  provides AllVajramsCodeGeneratorProvider with
      VajramProto3ServiceSchemaGenProvider;
}
