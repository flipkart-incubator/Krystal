module flipkart.krystal.vajram.ext.protobuf {
  exports com.flipkart.krystal.vajram.protobuf3;

  requires com.google.protobuf;
  requires transitive flipkart.krystal.common;
  requires transitive flipkart.krystal.vajram.ext.protobuf.util;
  requires static lombok;
  requires com.google.common;
  requires org.checkerframework.checker.qual;
}
