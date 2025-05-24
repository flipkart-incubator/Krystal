module flipkart.krystal.lattice.ext.grpc.codegen {
  requires com.google.auto.service;
  requires com.google.common;
  requires com.squareup.javapoet;
  requires flipkart.krystal.lattice.codegen;
  requires flipkart.krystal.lattice.ext.grpc;
  requires flipkart.krystal.vajram.codegen.common;
  requires java.compiler;
  requires static lombok;
  requires flipkart.krystal.codegen.common;
  requires org.checkerframework.checker.qual;
  requires jakarta.inject;
  requires flipkart.krystal.vajram.ext.protobuf.codegen;
  requires io.grpc;
  requires com.google.protobuf;
  requires flipkart.krystal.vajram.ext.protobuf;
  requires io.grpc.stub;
  requires flipkart.krystal.lattice.core;
}
