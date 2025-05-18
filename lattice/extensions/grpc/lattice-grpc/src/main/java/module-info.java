module flipkart.krystal.lattice.extensions.grpc {
  exports com.flipkart.krystal.lattice.ext.grpc;

  requires flipkart.krystal.lattice.core;
  requires org.checkerframework.checker.qual;
  requires io.grpc;
  requires com.google.common;
  requires flipkart.krystal.common;
  requires com.google.protobuf;
  requires io.grpc.stub;
  requires jakarta.inject;
  requires flipkart.krystal.vajramexecutor.krystex;
  requires flipkart.krystal.vajram;
  requires org.slf4j;
  requires static lombok;
  requires io.grpc.protobuf;
  requires flipkart.krystal.vajram.extensions.protobuf;
  requires flipkart.krystal.krystex;
}
